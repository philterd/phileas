package com.mtnfog.phileas.service.ai;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.LongBuffer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Inference {

    private static final Logger LOGGER = LogManager.getLogger(Inference.class);

    private final OrtEnvironment env;
    private final OrtSession session;
    private final WordpieceTokenizer tokenizer;
    private final Map<String, Integer> vocabulary;
    private final Map<Integer, String> id2Labels;

    public Inference(File model, File vocab, Map<Integer, String> id2Labels) throws Exception {

        this.env = OrtEnvironment.getEnvironment();
        this.session = env.createSession(model.getPath(), new OrtSession.SessionOptions());
        this.vocabulary = loadVocab(vocab);
        this.tokenizer = new WordpieceTokenizer(vocabulary);
        this.id2Labels = id2Labels;

    }

    public List<Span> predict(final String text, final String context, final String documentId) throws Exception {

        // TODO: I don't think this is needed and can be deleted.
        // Replace all multiple spaces with a single space.
        //text = text.replaceAll("\\s+", " ");

        // The NER spans found in the input text.
        final List<Span> spans = new LinkedList<>();

        // The WordPiece tokenized text. This changes the spacing in the text.
        final Tokens tokens = tokenize(text);

        // The inputs to the ONNX model.
        final Map<String, OnnxTensor> inputs = new HashMap<>();
        inputs.put("input_ids", OnnxTensor.createTensor(env, LongBuffer.wrap(tokens.getIds()), new long[]{1, tokens.getIds().length}));
        inputs.put("attention_mask", OnnxTensor.createTensor(env, LongBuffer.wrap(tokens.getMask()), new long[]{1, tokens.getMask().length}));
        inputs.put("token_type_ids", OnnxTensor.createTensor(env, LongBuffer.wrap(tokens.getTypes()), new long[]{1, tokens.getTypes().length}));

        // The outputs from the model.
        final float[][][] v = (float[][][]) session.run(inputs).get(0).getValue();

        // Find consecutive B-PER and I-PER labels and combine the spans where necessary.
        // There are also B-LOC and I-LOC tags for locations that might be useful at some point.

        // Keep track of where the last span was so when there are multiple/duplicate
        // spans we can get the next one instead of the first one each time.
        int characterStart = 0;

        // We are looping over the vector for each word,
        // finding the index of the array that has the maximum value,
        // and then finding the token classification that corresponds to that index.
        for(int x = 0; x < v[0].length; x++) {

            final float[] arr = v[0][x];
            final int maxIndex = maxIndex(arr);
            final String classification = id2Labels.get(maxIndex);

            // TODO: Need to make sure this value is between 0 and 1?
            // Can we do thresholding without it between 0 and 1?
            final double confidence = arr[maxIndex] / 10;

            // Is this is the start of a person entity.
            if (StringUtils.equalsIgnoreCase(classification, "B-PER")) {

                final String spanText;

                // Find the end index of the span in the array (where the label is not I-PER).
                final SpanEnd spanEnd = findSpanEnd(v, x, id2Labels, tokens.getTokens());

                // If the end is -1 it means this is a single-span token.
                // If the end is != -1 it means this is a multi-span token.
                if(spanEnd.getIndex() != -1) {

                    final StringBuilder sb = new StringBuilder();

                    // We have to concatenate the tokens.
                    // Add each token in the array and separate them with a space.
                    // TODO: What if the entities are not separated by a single space in the original text?
                    for(int i = x; i <= spanEnd.getIndex(); i++) {
                        sb.append(tokens.getTokens()[i]);
                        sb.append(" ");
                    }

                    // This is the text of the span.
                    spanText = findByRegex(text, sb.toString().trim()).trim();

                } else {

                    // This is a single-token span so there is nothing else to do except grab the token.
                    spanText = tokens.getTokens()[x];

                }

                // This ignores other potential matches in the same sentence
                // by only taking the first occurrence.
                characterStart = text.indexOf(spanText, characterStart);
                final int characterEnd = spanEnd.getCharacterEnd();

                // Create a span for this text.
                final Span span = new Span();
                span.setFilterType(FilterType.PERSON);
                span.setConfidence(confidence);
                span.setContext(context);
                span.setDocumentId(documentId);
                span.setText(spanText);
                span.setCharacterStart(characterStart);
                span.setCharacterEnd(characterEnd);

                // Add it to the list of spans to return.
                spans.add(span);

                characterStart = characterEnd;

            }

        }

        return spans;

    }

    private String findByRegex(String text, String span) {

        final String regex = span.replaceAll(" ", "\\\\s+");
        final Pattern pattern = Pattern.compile(regex);
        final Matcher matcher = pattern.matcher(text);

        if(matcher.find()) {
            return matcher.group(0);
        }

        // For some reason the regex match wasn't found. Just return the original span.
        return span;

    }

    private SpanEnd findSpanEnd(float[][][] v, int startIndex, Map<Integer, String> id2Labels, String[] tokens) {

        // -1 means there is no follow-up token, so it is a single-token span.
        int index = -1;
        int characterEnd = 0;

        // Starts at the span start in the vector.
        // Looks at the next token to see if it is an I-PER.
        // Go until the next token is something other than I-PER.
        // When the next token is not I-PER, return the previous index.

        for(int x = startIndex + 1; x < v[0].length; x++) {

            // Get the next item.
            final float[] arr = v[0][x];

            // See if the next token has an I-PER label.
            final String nextTokenClassification = id2Labels.get(maxIndex(arr));

            if(!StringUtils.equalsIgnoreCase(nextTokenClassification, "I-PER")) {
                index = x - 1;
                break;
            }

        }

        // Find where the span ends based on the tokens.
        for(int x = 1; x <= index; x++) {
            characterEnd += tokens[x].length();
        }

        // Add the number of spaces that is the number of tokens.
        // (One space per token.)
        // TODO: What if there are multiple spaces separating each token?
        characterEnd += index - 1;

        return new SpanEnd(index, characterEnd);

    }

    private int maxIndex(float[] arr) {

        float max = Float.NEGATIVE_INFINITY;
        int index = -1;

        for(int x = 0; x < arr.length; x++) {
            if(arr[x] > max) {
                index = x;
                max = arr[x];
            }
        }

        return index;

    }

    public Tokens tokenize(String text) {

        final String[] tokens = tokenizer.tokenize(text);

        final int[] ids = new int[tokens.length];

        for(int x = 0; x < tokens.length; x++) {
            ids[x] = vocabulary.get(tokens[x]);
        }

        final long[] lids = Arrays.stream(ids).mapToLong(i -> i).toArray();

        final long[] mask = new long[ids.length];
        Arrays.fill(mask, 1);

        final long[] types = new long[ids.length];
        Arrays.fill(types, 0);

        return new Tokens(tokens, lids, mask, types);

    }

    private Map<String, Integer> loadVocab(File vocab) throws IOException {

        final Map<String, Integer> v = new HashMap<>();

        BufferedReader br = new BufferedReader(new FileReader(vocab.getPath()));
        String line = br.readLine();
        int x = 0;

        while(line != null) {

            line = br.readLine();
            x++;

            v.put(line, x);

        }

        return v;

    }

}