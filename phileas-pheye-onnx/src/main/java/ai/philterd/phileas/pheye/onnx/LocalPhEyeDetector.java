/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phileas.pheye.onnx;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtProvider;
import ai.onnxruntime.OrtSession;
import ai.philterd.phileas.services.filters.ai.pheye.PhEyeDetector;
import ai.philterd.phileas.services.filters.ai.pheye.PhEyeSpan;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

/**
 * On-device GLiNER inference via ONNX Runtime. This is a Java port of the GLiNER 0.2.25
 * uni-encoder span pipeline (the recipe PhEye uses): build an entity prompt, tokenize prompt+text,
 * enumerate candidate word spans, run the ONNX model, then sigmoid + threshold + greedy
 * (flat, non-overlapping) decode, mapping word spans back to character offsets.
 *
 * <p><b>Model directory layout</b> (from {@code ./hub/publish.sh}):
 * <ul>
 *   <li>{@code model.onnx} (or {@code onnx/model.onnx}) — the exported GLiNER model</li>
 *   <li>{@code tokenizer.json} — the HuggingFace fast tokenizer</li>
 *   <li>{@code gliner_config.json} — span width, max length, prompt tokens</li>
 * </ul>
 *
 * <p><b>Parity:</b> a redaction model that decodes spans incorrectly leaks names, so this class is
 * parity-tested. {@code LocalPhEyeDetectorParityTest} verifies the pipeline mechanics against a
 * synthetic ONNX fixture, and {@code LocalPhEyeDetectorRealModelParityTest} confirms its spans match
 * Python {@code gliner.predict_entities} exactly on a real exported model. Both pass.
 */
public class LocalPhEyeDetector implements PhEyeDetector {

    // Default confidence threshold; the PhEyeFilter applies its own per-label thresholds on top.
    private static final double DEFAULT_THRESHOLD = 0.5;

    static final String THREADS_ENV = "PHEYE_ONNX_THREADS";
    static final String THREADS_PROPERTY = "pheye.onnx.threads";

    static final String DEVICE_ENV = "PHEYE_ONNX_DEVICE";
    static final String DEVICE_PROPERTY = "pheye.onnx.device";

    private static final Logger LOGGER = LogManager.getLogger(LocalPhEyeDetector.class);

    private final GlinerConfig config;
    private final HuggingFaceTokenizer tokenizer;
    private final OrtEnvironment ortEnvironment;
    private final OrtSession session;
    private final String device;

    public LocalPhEyeDetector(final Path modelDir) throws Exception {

        // Read first, so a bad value fails before anything is loaded.
        final int cuda = cudaDevice(System::getProperty, System::getenv);

        this.config = GlinerConfig.load(modelDir);

        if (!"whitespace".equals(config.wordsSplitterType)) {
            throw new IllegalArgumentException("Unsupported words_splitter_type '" + config.wordsSplitterType
                    + "'. Only 'whitespace' is supported by this detector.");
        }

        useCpuTokenizerLibrary(System::getenv, System::getProperty, System::setProperty);

        // DJL truncates to 512 tokens by default, which would leave the last text words with no tokens.
        // The Python gliner tokenizer does not truncate; text is bounded by max_len words instead.
        this.tokenizer = HuggingFaceTokenizer.newInstance(modelDir.resolve("tokenizer.json"), Map.of("truncation", "false"));

        final Path onnx = resolveOnnxPath(modelDir);
        this.ortEnvironment = OrtEnvironment.getEnvironment();
        OrtSession onCuda = null;
        if (cuda >= 0) {
            onCuda = createCudaSession(onnx, cuda);
        }
        this.session = onCuda != null ? onCuda : ortEnvironment.createSession(onnx.toString(), sessionOptions());
        this.device = onCuda != null ? "cuda:" + cuda : "cpu";
        LOGGER.info("Loaded the PhEye model in {} on {}.", modelDir, device);

    }

    /**
     * A session on the CUDA device, or null when it cannot be had: ONNX Runtime is the CPU-only package, or CUDA
     * or the device is not available. The model then runs on the CPU.
     */
    private OrtSession createCudaSession(final Path onnx, final int cuda) {

        if (!OrtEnvironment.getAvailableProviders().contains(OrtProvider.CUDA)) {
            LOGGER.warn("{} or {} asks for CUDA, but this ONNX Runtime has no CUDA provider; running on the CPU. "
                    + "Use com.microsoft.onnxruntime:onnxruntime_gpu instead of onnxruntime for CUDA.",
                    DEVICE_ENV, DEVICE_PROPERTY);
            return null;
        }

        OrtSession.SessionOptions options = null;
        try {
            options = sessionOptions();
            options.addCUDA(cuda);
            return ortEnvironment.createSession(onnx.toString(), options);
        } catch (final Exception e) {
            if (options != null) {
                options.close();
            }
            LOGGER.warn("Could not use CUDA device {}; running on the CPU: {}", cuda, e.toString());
            return null;
        }

    }

    /**
     * Has DJL load the CPU build of its tokenizer library, which is bundled, unless {@code RUST_FLAVOR} is set.
     * On a host where DJL finds CUDA it otherwise picks a CUDA build and downloads it from publish.djl.ai, which
     * fails without network access and stops the model loading, whatever {@value #DEVICE_ENV} says. Tokenizing
     * runs on the CPU either way.
     */
    static void useCpuTokenizerLibrary(final UnaryOperator<String> environment, final UnaryOperator<String> properties,
                                       final BiConsumer<String, String> setProperty) {
        final String flavor = "RUST_FLAVOR";
        final String fromEnvironment = environment.apply(flavor);
        final String fromProperty = properties.apply(flavor);
        if ((fromEnvironment == null || fromEnvironment.isBlank()) && (fromProperty == null || fromProperty.isBlank())) {
            setProperty.accept(flavor, "cpu");
        }
    }

    /** Where the model runs: {@code cpu} or {@code cuda:<device>}. */
    String device() {
        return device;
    }

    /**
     * The CUDA device asked for, or -1 for the CPU: the system property, else the environment variable. {@code cpu}
     * or unset is the CPU, {@code cuda} device 0, and {@code cuda:<n>} device n.
     */
    static int cudaDevice(final UnaryOperator<String> properties, final UnaryOperator<String> environment) {
        String value = properties.apply(DEVICE_PROPERTY);
        String name = DEVICE_PROPERTY;
        if (value == null || value.isBlank()) {
            value = environment.apply(DEVICE_ENV);
            name = DEVICE_ENV;
        }
        if (value == null || value.isBlank() || value.strip().equalsIgnoreCase("cpu")) {
            return -1;
        }
        final String device = value.strip().toLowerCase(Locale.ROOT);
        if (device.equals("cuda")) {
            return 0;
        }
        if (device.matches("cuda:\\d{1,9}")) {
            return Integer.parseInt(device.substring(5));
        }
        throw new IllegalArgumentException(name + " must be cpu, cuda, or cuda:<device number>, not '" + value + "'.");
    }

    private static OrtSession.SessionOptions sessionOptions() throws Exception {
        final OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        // ONNX Runtime otherwise starts a thread per physical core of the host, ignoring a container's
        // CPU set or quota. Spinning off keeps idle threads from busy-waiting on CPUs shared with other work.
        options.setIntraOpNumThreads(threads(System::getProperty, System::getenv, Runtime.getRuntime().availableProcessors()));
        options.addConfigEntry("session.intra_op.allow_spinning", "0");
        return options;
    }

    /** The intra-op thread count: the system property, else the environment variable, else the available processors. */
    static int threads(final UnaryOperator<String> properties, final UnaryOperator<String> environment,
                       final int availableProcessors) {
        String value = properties.apply(THREADS_PROPERTY);
        String name = THREADS_PROPERTY;
        if (value == null || value.isBlank()) {
            value = environment.apply(THREADS_ENV);
            name = THREADS_ENV;
        }
        if (value == null || value.isBlank()) {
            return availableProcessors;
        }
        try {
            final int threads = Integer.parseInt(value.trim());
            if (threads > 0) {
                return threads;
            }
        } catch (final NumberFormatException e) {
            // Reported below.
        }
        throw new IllegalArgumentException(name + " must be a positive whole number, not '" + value + "'.");
    }

    private static Path resolveOnnxPath(final Path modelDir) {
        final Path nested = modelDir.resolve("onnx").resolve("model.onnx");
        if (Files.exists(nested)) {
            return nested;
        }
        return modelDir.resolve("model.onnx");
    }

    @Override
    public List<PhEyeSpan> detect(final String text, final Collection<String> labels,
                                  final String context, final int piece) throws Exception {

        final List<String> labelList = new ArrayList<>(labels);
        if (labelList.isEmpty() || text == null || text.isBlank()) {
            return new ArrayList<>();
        }

        // 1. Split into words with char offsets, then truncate to the model's max length.
        List<WordsSplitter.Word> words = WordsSplitter.split(text);
        if (words.size() > config.maxLen) {
            words = words.subList(0, config.maxLen);
        }
        final int numWords = words.size();
        if (numWords == 0) {
            return new ArrayList<>();
        }

        // 2. Build the prompt word list: [<<ENT>> label]* <<SEP>> <text words...>
        //    (mirrors SpanProcessor.prepare_inputs; markerV0 prompt).
        final List<String> inputWords = new ArrayList<>();
        for (final String label : labelList) {
            inputWords.add(config.entToken);
            inputWords.add(label);
        }
        inputWords.add(config.sepToken);
        final int promptWordCount = inputWords.size();
        for (final WordsSplitter.Word w : words) {
            inputWords.add(w.text());
        }

        // 3. Tokenize pre-split (is_split_into_words=True). The tokenizer adds special tokens.
        final Encoding encoding = tokenizer.encode(inputWords.toArray(new String[0]));
        final long[] inputIds = encoding.getIds();
        final long[] attentionMask = encoding.getAttentionMask();
        final long[] wordIds = encoding.getWordIds(); // word index per token; -1 for special tokens
        final int seqLen = inputIds.length;

        // words_mask: first subtoken of each TEXT word gets its 1-based text-word index, else 0
        // (mirrors prepare_word_mask with skip_first_words=promptWordCount).
        final long[] wordsMask = new long[seqLen];
        long previousWordId = Long.MIN_VALUE;
        int maskedWords = 0;
        for (int t = 0; t < seqLen; t++) {
            final long wid = wordIds[t];
            if (wid >= promptWordCount && wid != previousWordId) {
                wordsMask[t] = wid - promptWordCount + 1;
                maskedWords++;
            } else {
                wordsMask[t] = 0;
            }
            previousWordId = wid;
        }

        // A word with no token would be scored from padding, producing spans that are not in the text.
        if (maskedWords != numWords) {
            throw new IllegalStateException("Tokenization covered " + maskedWords + " of " + numWords + " words.");
        }

        // 4. Enumerate candidate spans [i, i+width] for width in 0..maxWidth-1; mask out-of-range.
        final int spansPerWord = config.maxWidth;
        final int numSpans = numWords * spansPerWord;
        final long[][] spanIdx = new long[numSpans][2];
        // span_mask is a boolean tensor in the GLiNER ONNX signature (not int64); feeding the
        // wrong dtype makes ONNX Runtime reject the input.
        final boolean[] spanMask = new boolean[numSpans];
        int s = 0;
        for (int i = 0; i < numWords; i++) {
            for (int k = 0; k < spansPerWord; k++) {
                final int end = i + k;
                spanIdx[s][0] = i;
                spanIdx[s][1] = end;
                spanMask[s] = end < numWords;
                s++;
            }
        }

        // 5. Run the ONNX model.
        final float[][][] logits = runModel(inputIds, attentionMask, wordsMask, numWords, spanIdx, spanMask, numWords);

        // 6. Decode: sigmoid + threshold -> candidate spans, then greedy non-overlap by score.
        final List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < numWords; i++) {
            for (int k = 0; k < spansPerWord; k++) {
                final int end = i + k;
                if (end >= numWords) {
                    continue;
                }
                for (int c = 0; c < labelList.size(); c++) {
                    final double prob = sigmoid(logits[i][k][c]);
                    if (prob > DEFAULT_THRESHOLD) {
                        candidates.add(new Candidate(i, end, c, prob));
                    }
                }
            }
        }

        final List<Candidate> selected = greedyNonOverlap(candidates);

        // 7. Map word spans -> char offsets -> PhEyeSpan.
        final List<PhEyeSpan> spans = new ArrayList<>();
        for (final Candidate cand : selected) {
            final int startChar = words.get(cand.startWord).start();
            final int endChar = words.get(cand.endWord).end();

            final PhEyeSpan span = new PhEyeSpan();
            span.setStart(startChar);
            span.setEnd(endChar);
            span.setLabel(labelList.get(cand.classIndex));
            span.setText(text.substring(startChar, endChar));
            span.setScore(cand.score);
            spans.add(span);
        }

        return spans;

    }

    /**
     * Run the span model and return logits shaped [numWords][maxWidth][numClasses].
     *
     * <p>VERIFY against the exported model: the input tensor dtypes (int64 here) and the
     * raw {@code logits} output shape must match the gliner export. GLiNER's span model emits
     * logits of [batch, numWords, maxWidth, numClasses]; this method reshapes to that.
     */
    private float[][][] runModel(final long[] inputIds, final long[] attentionMask, final long[] wordsMask,
                                 final int textLength, final long[][] spanIdx, final boolean[] spanMask,
                                 final int numWords) throws Exception {

        final int seqLen = inputIds.length;
        final int numSpans = spanIdx.length;
        final int numClasses; // inferred from the output below

        final Map<String, OnnxTensor> inputs = new HashMap<>();
        try {

            inputs.put("input_ids", OnnxTensor.createTensor(ortEnvironment, new long[][]{inputIds}));
            inputs.put("attention_mask", OnnxTensor.createTensor(ortEnvironment, new long[][]{attentionMask}));
            inputs.put("words_mask", OnnxTensor.createTensor(ortEnvironment, new long[][]{wordsMask}));
            inputs.put("text_lengths", OnnxTensor.createTensor(ortEnvironment, new long[][]{{textLength}}));
            inputs.put("span_idx", OnnxTensor.createTensor(ortEnvironment, new long[][][]{spanIdx}));
            inputs.put("span_mask", OnnxTensor.createTensor(ortEnvironment, new boolean[][]{spanMask}));

            try (final OrtSession.Result result = session.run(inputs)) {

                final OnnxValue value = result.get("logits").orElseThrow(
                        () -> new IllegalStateException("ONNX model did not return a 'logits' output."));
                final float[] flat = flatten(((OnnxTensor) value).getFloatBuffer().array());

                // Reshape flat logits to [numWords][maxWidth][numClasses].
                numClasses = flat.length / (numWords * config.maxWidth);
                final float[][][] logits = new float[numWords][config.maxWidth][numClasses];
                int idx = 0;
                for (int i = 0; i < numWords; i++) {
                    for (int k = 0; k < config.maxWidth; k++) {
                        for (int c = 0; c < numClasses; c++) {
                            logits[i][k][c] = flat[idx++];
                        }
                    }
                }
                return logits;

            }

        } finally {
            for (final OnnxTensor tensor : inputs.values()) {
                tensor.close();
            }
        }

    }

    private static float[] flatten(final float[] array) {
        return array;
    }

    /**
     * Greedy flat NER: keep highest-scoring spans that do not overlap already-kept ones.
     * Package-private so it can be unit-tested directly (see {@code GreedyNonOverlapTest}).
     */
    static List<Candidate> greedyNonOverlap(final List<Candidate> candidates) {
        candidates.sort((a, b) -> Double.compare(b.score, a.score));
        final List<Candidate> kept = new ArrayList<>();
        for (final Candidate c : candidates) {
            boolean overlaps = false;
            for (final Candidate k : kept) {
                if (c.startWord <= k.endWord && k.startWord <= c.endWord) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) {
                kept.add(c);
            }
        }
        kept.sort((a, b) -> Integer.compare(a.startWord, b.startWord));
        return kept;
    }

    private static double sigmoid(final double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    /** The model's {@code max_len}: it examines no more text words in one call. */
    @Override
    public int maxWords() {
        return config.maxLen;
    }

    /** The model's {@code max_width}: the most words one span covers. */
    @Override
    public int maxWidth() {
        return config.maxWidth;
    }

    @Override
    public void close() throws Exception {
        if (session != null) {
            session.close();
        }
        if (tokenizer != null) {
            tokenizer.close();
        }
    }

    /** A scored candidate span over word indices (inclusive) for a given class. Package-private for testing. */
    record Candidate(int startWord, int endWord, int classIndex, double score) {}

}
