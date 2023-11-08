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
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class OpenNLPSentimentDetector implements SentimentDetector {

    private static final Logger LOGGER = LogManager.getLogger(OpenNLPSentimentDetector.class);

    @Override
    public String classify(final Policy policy, String input) throws Exception {

        String modelFileName = policy.getConfig().getAnalysis().getSentiment().getModel();

        final DocumentCategorizerME documentCategorizerME;

        // Is the model cached?
        if(ModelCache.getInstance().get(modelFileName) != null) {
            LOGGER.debug("Sentiment model retrieved from model cache.");
            documentCategorizerME = ModelCache.getInstance().get(modelFileName);
        } else {

            // Load the model and cache it.
            final InputStream is;

            // Load the NER filter for the given modelFile.
            // If the filename starts with "classpath:" it is in the jar's resources.
            if(modelFileName.startsWith("classpath:")) {
                final String classpathModelFileName = modelFileName.replace("classpath:", "");
                is = OpenNLPSentimentDetector.this.getClass().getClassLoader().getResourceAsStream(classpathModelFileName);
                final DoccatModel model = new DoccatModel(is);
                documentCategorizerME = new DocumentCategorizerME(model);
                ModelCache.getInstance().put(modelFileName, documentCategorizerME);
            } else {
                if(Files.exists(Paths.get(modelFileName))) {
                    is = new FileInputStream(modelFileName);
                    final DoccatModel model = new DoccatModel(is);
                    documentCategorizerME = new DocumentCategorizerME(model);
                    is.close();
                    ModelCache.getInstance().put(modelFileName, documentCategorizerME);
                    LOGGER.debug("Sentiment model loaded from disk and cached.");
                } else {
                    LOGGER.error("The sentiment model file does not exist: " + modelFileName);
                    documentCategorizerME = null;
                }
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
