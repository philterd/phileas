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

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.Entity;
import opennlp.dl.namefinder.NameFinderDL;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.util.Span;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Inference {

    private static final Logger LOGGER = LogManager.getLogger(Inference.class);

    private final Map<Integer, String> id2Labels;
    private final NameFinderDL nameFinderDL;

    public Inference(File model, File vocab, Map<Integer, String> id2Labels, SentenceDetector sentenceDetector) throws Exception {

        this.id2Labels = id2Labels;
        this.nameFinderDL = new NameFinderDL(model, vocab, id2Labels, sentenceDetector);

    }

    public List<Entity> predict(final String text, final String context, final String documentId) {

        final String[] tokens = text.split(" ");

        final long startTime = System.currentTimeMillis();
        final Span[] spans = nameFinderDL.find(tokens);
        final long endTime = System.currentTimeMillis();
        LOGGER.debug("Inference took {} ms. Found {} spans.", endTime - startTime, spans.length);

        final List<Entity> entities = new LinkedList<>();

        for(final Span span : spans) {

            final String spanText = (String) span.getCoveredText(text);

            // Create a span for this text.
            final Entity entity = new Entity(
                    span.getStart(),
                    span.getEnd(),
                    FilterType.PERSON,
                    context,
                    documentId,
                    spanText,
                    span.getProb());

            entities.add(entity);

        }

        return entities;

    }

}
