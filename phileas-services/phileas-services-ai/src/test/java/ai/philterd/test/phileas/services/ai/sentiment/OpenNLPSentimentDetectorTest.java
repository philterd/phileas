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
package ai.philterd.test.phileas.services.ai.sentiment;

import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.services.SentimentDetector;
import ai.philterd.phileas.service.ai.sentiment.OpenNLPSentimentDetector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class OpenNLPSentimentDetectorTest {

    private static final Logger LOGGER = LogManager.getLogger(OpenNLPSentimentDetectorTest.class);

    private String getModelFilePath() {

        final ClassLoader classLoader = getClass().getClassLoader();
        return new File(classLoader.getResource("ner/en-sentiment.bin").getFile()).getAbsolutePath();

    }

    @Test
    public void sentimentPositive() throws Exception {

        final Policy policy = new Policy();
        policy.getConfig().getAnalysis().getSentiment().setEnabled(true);
        policy.getConfig().getAnalysis().getSentiment().setModel(getModelFilePath());

        final SentimentDetector sentimentDetector = new OpenNLPSentimentDetector();
        final String sentiment = sentimentDetector.classify(policy, "I had a great and wonderful day.");

        LOGGER.info("Sentiment detected as: {}", sentiment);
        Assertions.assertEquals("0", sentiment);

    }

    @Test
    public void sentimentNegative() throws Exception {

        final Policy policy = new Policy();
        policy.getConfig().getAnalysis().getSentiment().setEnabled(true);
        policy.getConfig().getAnalysis().getSentiment().setModel(getModelFilePath());

        final SentimentDetector sentimentDetector = new OpenNLPSentimentDetector();
        final String sentiment = sentimentDetector.classify(policy, "I had a bad and terrible day.");

        LOGGER.info("Sentiment detected as: {}", sentiment);
        Assertions.assertEquals("1", sentiment);

    }

}
