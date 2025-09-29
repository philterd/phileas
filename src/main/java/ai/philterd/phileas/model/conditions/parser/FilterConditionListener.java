// Generated from FilterCondition.g4 by ANTLR 4.13.2
package ai.philterd.phileas.model.conditions.parser;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link FilterConditionParser}.
 */
public interface FilterConditionListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link FilterConditionParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(FilterConditionParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link FilterConditionParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(FilterConditionParser.ExpressionContext ctx);
}