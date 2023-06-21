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
// Generated from FilterCondition.g4 by ANTLR 4.8
package com.mtnfog.phileas.model.conditions.parser;
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