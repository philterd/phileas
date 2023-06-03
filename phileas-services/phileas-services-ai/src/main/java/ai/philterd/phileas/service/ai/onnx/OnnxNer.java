package ai.philterd.phileas.service.ai.onnx;

import ai.philterd.phileas.model.objects.Entity;

import opennlp.dl.InferenceOptions;
import opennlp.tools.sentdetect.SentenceDetector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.List;
import java.util.Map;

public class OnnxNer {

    private static final Logger LOGGER = LogManager.getLogger(OnnxNer.class);

    private final Inference inference;

    public OnnxNer(File model, File vocab, final Map<Integer, String> id2Labels, SentenceDetector sentenceDetector) throws Exception {

        this.inference = new Inference(model, vocab, id2Labels, sentenceDetector);

    }

    public List<Entity> find(final String tokens, final String context, final String documentId) throws Exception {

        return inference.predict(tokens, context, documentId);

    }

}
