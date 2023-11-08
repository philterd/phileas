/*
 *     Copyright 2023 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.service.ai.sentiment;

import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.services.SentimentDetector;
import ai.philterd.phileas.service.ai.models.ModelCache;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class OpenNLPSentimentDetector implements SentimentDetector {

    private static final Logger LOGGER = LogManager.getLogger(OpenNLPSentimentDetector.class);

    @Override
    public String classify(final Policy policy, String input) throws IOException {

        final String modelName = policy.getConfig().getAnalysis().getSentiment().getModel();

        final DocumentCategorizerME documentCategorizerME;

        // Is the model cached?
        if(ModelCache.getInstance().get(modelName) != null) {
            LOGGER.debug("Sentiment model retrieved from model cache.");
            documentCategorizerME = ModelCache.getInstance().get(modelName);
        } else {
            // Load the model and cache it.
            if(Files.exists(Paths.get(modelName))) {
                final InputStream is = new FileInputStream(modelName);
                final DoccatModel model = new DoccatModel(is);
                documentCategorizerME = new DocumentCategorizerME(model);
                is.close();
                ModelCache.getInstance().put(modelName, documentCategorizerME);
                LOGGER.debug("Sentiment model loaded from disk and cached.");
            } else {
                LOGGER.error("The sentiment model file does not exist: " + modelName);
                documentCategorizerME = null;
            }
        }

        if(documentCategorizerME != null) {
            // Run sentiment analysis on the input text.
            final double[] outcomes = documentCategorizerME.categorize(input.split(" "));
            return documentCategorizerME.getBestCategory(outcomes);
        }

        return null;

    }

}
