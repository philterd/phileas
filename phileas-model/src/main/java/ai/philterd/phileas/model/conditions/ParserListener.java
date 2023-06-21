/*
 *     Copyright 2023 Philerd, LLC @ https://www.philterd.ai
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
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class ParserListener extends FilterConditionBaseListener {

    protected static final Logger LOGGER = LogManager.getLogger(ParserListener.class);

    private List<ParsedCondition> conditions;
    private Queue<String> terminals;

    public ParserListener() {

        this.conditions = new LinkedList<>();
        this.terminals = new LinkedBlockingQueue<>();

    }

    @Override
    public void visitTerminal(TerminalNode node) {

        terminals.add(node.getText());
        LOGGER.debug("Processing terminal node: [{}]", node.getText());

    }

    public static List<ParsedCondition> getTerminals(final String condition) {

        final CharStream codePointCharStream = CharStreams.fromString(condition);
        final FilterConditionLexer lexer = new FilterConditionLexer(codePointCharStream);
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final FilterConditionParser parser = new FilterConditionParser(tokens);

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

        if(StringUtils.equalsIgnoreCase(terminals.poll(), "AND")) {
            getConditions();
        }

        return conditions;

    }


}