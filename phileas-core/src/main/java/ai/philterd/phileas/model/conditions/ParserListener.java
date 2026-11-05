/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.model.conditions;

import ai.philterd.phileas.model.conditions.parser.FilterConditionBaseListener;
import ai.philterd.phileas.model.conditions.parser.FilterConditionLexer;
import ai.philterd.phileas.model.conditions.parser.FilterConditionParser;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class ParserListener extends FilterConditionBaseListener {

    protected static final Logger LOGGER = LogManager.getLogger(ParserListener.class);

    /** Why a condition may fail to parse, without repeating its text. */
    private static final String REASON = "a condition is a field (token, context, confidence, population, or type), "
            + "an operator, and a value, such as token == \"value\" or confidence > 0.5, and conditions are joined "
            + "with and. Values compared with token and context must be in double quotes.";

    private static final BaseErrorListener THROWING_ERROR_LISTENER = new BaseErrorListener() {

        @Override
        public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol, final int line,
                                final int charPositionInLine, final String msg, final RecognitionException e) {
            // ANTLR's message quotes the offending text, which can be a sensitive value, so it is not used.
            throw new InvalidConditionException(charPositionInLine, REASON);
        }

    };

    private final List<ParsedCondition> conditions;
    private final Queue<String> terminals;

    public ParserListener() {

        this.conditions = new LinkedList<>();
        this.terminals = new LinkedBlockingQueue<>();

    }

    @Override
    public void visitTerminal(final TerminalNode node) {

        terminals.add(node.getText());
        // The terminal's text can contain literal values from a policy condition, so it is not logged.
        LOGGER.debug("Processing a terminal node of the filter condition.");

    }

    /**
     * Parses a condition.
     * @param condition The condition.
     * @return The conditions it joins with {@code and}, in order.
     * @throws InvalidConditionException if the condition cannot be parsed.
     */
    public static List<ParsedCondition> getTerminals(final String condition) {

        final CharStream codePointCharStream = CharStreams.fromString(condition == null ? "" : condition);
        final FilterConditionLexer lexer = new FilterConditionLexer(codePointCharStream);
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final FilterConditionParser parser = new FilterConditionParser(tokens);

        // Fail on the first error rather than recovering from it and returning a partial parse. The
        // default listeners print the offending text to the console, so they are replaced.
        lexer.removeErrorListeners();
        lexer.addErrorListener(THROWING_ERROR_LISTENER);
        parser.removeErrorListeners();
        parser.addErrorListener(THROWING_ERROR_LISTENER);

        final ParseTree tree = parser.expression();
        final ParserListener parserListener = new ParserListener();

        final ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(parserListener, tree);

        return parserListener.getConditions();

    }

    private List<ParsedCondition> getConditions() {

        final ParsedCondition parsedCondition = new ParsedCondition();

        parsedCondition.setField(terminals.poll());
        parsedCondition.setOperator(terminals.poll());
        parsedCondition.setValue(terminals.poll());

        conditions.add(parsedCondition);

        if(Strings.CI.equals(terminals.poll(), "AND")) {
            getConditions();
        }

        return conditions;

    }

}