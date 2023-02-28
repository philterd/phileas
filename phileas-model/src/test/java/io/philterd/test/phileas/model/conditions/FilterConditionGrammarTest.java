package io.philterd.test.phileas.model.conditions;

import io.philterd.phileas.model.conditions.ParsedCondition;
import io.philterd.phileas.model.conditions.ParserListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class FilterConditionGrammarTest {

    private static final Logger LOGGER = LogManager.getLogger(FilterConditionGrammarTest.class);

    @Test
    public void testPopulationWithAND() {

        final ParserListener parserListener = new ParserListener();

        final List<ParsedCondition> conditions = parserListener.getTerminals("population < 2500 and population > 10000");

        Assertions.assertEquals(2, conditions.size());

        ParsedCondition parsedCondition = conditions.get(0);

        Assertions.assertEquals("population", parsedCondition.getField());
        Assertions.assertEquals("<", parsedCondition.getOperator());
        Assertions.assertEquals("2500", parsedCondition.getValue());

        parsedCondition = conditions.get(1);

        Assertions.assertEquals("population", parsedCondition.getField());
        Assertions.assertEquals(">", parsedCondition.getOperator());
        Assertions.assertEquals("10000", parsedCondition.getValue());

    }

    @Test
    public void testPopulation() {

        final ParserListener parserListener = new ParserListener();

        final List<ParsedCondition> conditions = parserListener.getTerminals("population < 2500");

        final ParsedCondition parsedCondition = conditions.get(0);

        Assertions.assertEquals("population", parsedCondition.getField());
        Assertions.assertEquals("<", parsedCondition.getOperator());
        Assertions.assertEquals("2500", parsedCondition.getValue());

    }

    @Test
    public void testToken() {

        final ParserListener parserListener = new ParserListener();

        final List<ParsedCondition> conditions = parserListener.getTerminals("token == \"test\"");

        ParsedCondition parsedCondition = conditions.get(0);

        Assertions.assertEquals("token", parsedCondition.getField());
        Assertions.assertEquals("==", parsedCondition.getOperator());
        Assertions.assertEquals("\"test\"", parsedCondition.getValue());

    }

    @Test
    public void testNerConditions1() {

        final ParserListener parserListener = new ParserListener();

        final List<ParsedCondition> conditions = parserListener.getTerminals("confidence != 0.5 and type != LOC");

        Assertions.assertEquals(2, conditions.size());

        ParsedCondition parsedCondition = conditions.get(0);

        Assertions.assertEquals("confidence", parsedCondition.getField());
        Assertions.assertEquals("!=", parsedCondition.getOperator());
        Assertions.assertEquals("0.5", parsedCondition.getValue());

        parsedCondition = conditions.get(1);

        Assertions.assertEquals("type", parsedCondition.getField());
        Assertions.assertEquals("!=", parsedCondition.getOperator());
        Assertions.assertEquals("LOC", parsedCondition.getValue());

    }

    @Test
    public void testNerConditions2() {

        final ParserListener parserListener = new ParserListener();

        final List<ParsedCondition> conditions = parserListener.getTerminals("confidence < 0.4 and type == PER");

        Assertions.assertEquals(2, conditions.size());

        ParsedCondition parsedCondition = conditions.get(0);

        Assertions.assertEquals("confidence", parsedCondition.getField());
        Assertions.assertEquals("<", parsedCondition.getOperator());
        Assertions.assertEquals("0.4", parsedCondition.getValue());

        parsedCondition = conditions.get(1);

        Assertions.assertEquals("type", parsedCondition.getField());
        Assertions.assertEquals("==", parsedCondition.getOperator());
        Assertions.assertEquals("PER", parsedCondition.getValue());

    }

}
