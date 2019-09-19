package com.mtnfog.test.phileas.model.profile.filters.strategies.ai;

import com.mtnfog.phileas.model.profile.filters.strategies.ai.NerFilterStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class NerFilterStrategyTest {

    private static final Logger LOGGER = LogManager.getLogger(NerFilterStrategyTest.class);

    @Test
    public void evaluateCondition1() {

        final NerFilterStrategy strategy = new NerFilterStrategy();

        final Map<String, Object> attributes = new HashMap<>();
        attributes.put(NerFilterStrategy.CONFIDENCE, 0.5);

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "John Smith", "confidence > 0.25", attributes);

        Assert.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition2() {

        final NerFilterStrategy strategy = new NerFilterStrategy();

        final Map<String, Object> attributes = new HashMap<>();
        attributes.put(NerFilterStrategy.TYPE, "PER");

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "John Smith", "type == PER", attributes);

        Assert.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition3() {

        final NerFilterStrategy strategy = new NerFilterStrategy();

        final Map<String, Object> attributes = new HashMap<>();
        attributes.put(NerFilterStrategy.TYPE, "LOC");

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "John Smith", "type == PER", attributes);

        Assert.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition4() {

        final NerFilterStrategy strategy = new NerFilterStrategy();

        final Map<String, Object> attributes = new HashMap<>();
        attributes.put(NerFilterStrategy.CONFIDENCE, 0.5);

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "John Smith", "confidence == 0.5", attributes);

        Assert.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition5() {

        final NerFilterStrategy strategy = new NerFilterStrategy();

        final Map<String, Object> attributes = new HashMap<>();
        attributes.put(NerFilterStrategy.CONFIDENCE, 0.6);

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "John Smith", "confidence != 0.5", attributes);

        Assert.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition6() {

        final NerFilterStrategy strategy = new NerFilterStrategy();

        final Map<String, Object> attributes = new HashMap<>();
        attributes.put(NerFilterStrategy.CONFIDENCE, 0.5);

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "John Smith", "confidence != 0.5", attributes);

        Assert.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition7() {

        final NerFilterStrategy strategy = new NerFilterStrategy();

        final Map<String, Object> attributes = new HashMap<>();
        attributes.put(NerFilterStrategy.CONFIDENCE, 0.5);
        attributes.put(NerFilterStrategy.TYPE, "LOC");

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "John Smith", "confidence != 0.5 and type == PER", attributes);

        Assert.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition8() {

        final NerFilterStrategy strategy = new NerFilterStrategy();

        final Map<String, Object> attributes = new HashMap<>();
        attributes.put(NerFilterStrategy.CONFIDENCE, 0.5);
        attributes.put(NerFilterStrategy.TYPE, "PER");

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "John Smith", "confidence != 0.5 and type != LOC", attributes);

        Assert.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition9() {

        final NerFilterStrategy strategy = new NerFilterStrategy();

        final Map<String, Object> attributes = new HashMap<>();
        attributes.put(NerFilterStrategy.CONFIDENCE, 0.5);
        attributes.put(NerFilterStrategy.TYPE, "PER");

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "John Smith", "confidence > 0.4 and type == PER", attributes);

        Assert.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition10() {

        final NerFilterStrategy strategy = new NerFilterStrategy();

        final Map<String, Object> attributes = new HashMap<>();
        attributes.put(NerFilterStrategy.CONFIDENCE, 0.5);
        attributes.put(NerFilterStrategy.TYPE, "PER");

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "John Smith", "confidence < 0.4 and type == PER", attributes);

        Assert.assertFalse(conditionSatisfied);

    }

}
