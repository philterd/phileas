/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
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
