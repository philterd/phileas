package com.mtnfog.test.phileas.model.conditions;

import com.mtnfog.phileas.model.conditions.ParsedCondition;
import com.mtnfog.phileas.model.conditions.ParserListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class FilterConditionGrammarTest {

    private static final Logger LOGGER = LogManager.getLogger(FilterConditionGrammarTest.class);

    @Test
    public void testPopulationWithAND() {

        final ParserListener parserListener = new ParserListener();

        final List<ParsedCondition> conditions = parserListener.getTerminals("population < 2500 and population > 10000");

        Assert.assertEquals(2, conditions.size());

        ParsedCondition parsedCondition = conditions.get(0);

        Assert.assertEquals("population", parsedCondition.getField());
        Assert.assertEquals("<", parsedCondition.getOperator());
        Assert.assertEquals("2500", parsedCondition.getValue());

        parsedCondition = conditions.get(1);

        Assert.assertEquals("population", parsedCondition.getField());
        Assert.assertEquals(">", parsedCondition.getOperator());
        Assert.assertEquals("10000", parsedCondition.getValue());

    }

    @Test
    public void testPopulation() {

        final ParserListener parserListener = new ParserListener();

        final List<ParsedCondition> conditions = parserListener.getTerminals("population < 2500");

        final ParsedCondition parsedCondition = conditions.get(0);

        Assert.assertEquals("population", parsedCondition.getField());
        Assert.assertEquals("<", parsedCondition.getOperator());
        Assert.assertEquals("2500", parsedCondition.getValue());

    }

    @Test
    public void testToken() {

        final ParserListener parserListener = new ParserListener();

        final List<ParsedCondition> conditions = parserListener.getTerminals("token == \"test\"");

        ParsedCondition parsedCondition = conditions.get(0);

        Assert.assertEquals("token", parsedCondition.getField());
        Assert.assertEquals("==", parsedCondition.getOperator());
        Assert.assertEquals("\"test\"", parsedCondition.getValue());

    }

    @Test
    public void testNerConditions1() {

        final ParserListener parserListener = new ParserListener();

        final List<ParsedCondition> conditions = parserListener.getTerminals("confidence != 0.5 and type != LOC");

        Assert.assertEquals(2, conditions.size());

        ParsedCondition parsedCondition = conditions.get(0);

        Assert.assertEquals("confidence", parsedCondition.getField());
        Assert.assertEquals("!=", parsedCondition.getOperator());
        Assert.assertEquals("0.5", parsedCondition.getValue());

        parsedCondition = conditions.get(1);

        Assert.assertEquals("type", parsedCondition.getField());
        Assert.assertEquals("!=", parsedCondition.getOperator());
        Assert.assertEquals("LOC", parsedCondition.getValue());

    }

    @Test
    public void testNerConditions2() {

        final ParserListener parserListener = new ParserListener();

        final List<ParsedCondition> conditions = parserListener.getTerminals("confidence < 0.4 and type == PER");

        Assert.assertEquals(2, conditions.size());

        ParsedCondition parsedCondition = conditions.get(0);

        Assert.assertEquals("confidence", parsedCondition.getField());
        Assert.assertEquals("<", parsedCondition.getOperator());
        Assert.assertEquals("0.4", parsedCondition.getValue());

        parsedCondition = conditions.get(1);

        Assert.assertEquals("type", parsedCondition.getField());
        Assert.assertEquals("==", parsedCondition.getOperator());
        Assert.assertEquals("PER", parsedCondition.getValue());

    }

}
