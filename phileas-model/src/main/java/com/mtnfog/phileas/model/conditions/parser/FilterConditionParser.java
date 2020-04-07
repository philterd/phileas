// Generated from FilterCondition.g4 by ANTLR 4.7.2
package com.mtnfog.phileas.model.conditions.parser;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class FilterConditionParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.7.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, TYPE=6, AND=7, COMPARATOR=8, NUMBER=9, 
		WORD=10, WS=11;
	public static final int
		RULE_expression = 0;
	private static String[] makeRuleNames() {
		return new String[] {
			"expression"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'population'", "'token'", "'type'", "'confidence'", "'context'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, "TYPE", "AND", "COMPARATOR", "NUMBER", 
			"WORD", "WS"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "FilterCondition.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public FilterConditionParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class ExpressionContext extends ParserRuleContext {
		public TerminalNode COMPARATOR() { return getToken(FilterConditionParser.COMPARATOR, 0); }
		public TerminalNode NUMBER() { return getToken(FilterConditionParser.NUMBER, 0); }
		public TerminalNode WORD() { return getToken(FilterConditionParser.WORD, 0); }
		public TerminalNode TYPE() { return getToken(FilterConditionParser.TYPE, 0); }
		public TerminalNode AND() { return getToken(FilterConditionParser.AND, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public ExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FilterConditionListener ) ((FilterConditionListener)listener).enterExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterConditionListener ) ((FilterConditionListener)listener).exitExpression(this);
		}
	}

	public final ExpressionContext expression() throws RecognitionException {
		ExpressionContext _localctx = new ExpressionContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_expression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(38);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,5,_ctx) ) {
			case 1:
				{
				}
				break;
			case 2:
				{
				setState(3);
				match(T__0);
				setState(4);
				match(COMPARATOR);
				setState(5);
				match(NUMBER);
				setState(8);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
				case 1:
					{
					setState(6);
					match(AND);
					setState(7);
					expression();
					}
					break;
				}
				}
				break;
			case 3:
				{
				setState(10);
				match(T__1);
				setState(11);
				match(COMPARATOR);
				setState(12);
				match(WORD);
				setState(15);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
				case 1:
					{
					setState(13);
					match(AND);
					setState(14);
					expression();
					}
					break;
				}
				}
				break;
			case 4:
				{
				setState(17);
				match(T__2);
				setState(18);
				match(COMPARATOR);
				setState(19);
				match(TYPE);
				setState(22);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,2,_ctx) ) {
				case 1:
					{
					setState(20);
					match(AND);
					setState(21);
					expression();
					}
					break;
				}
				}
				break;
			case 5:
				{
				setState(24);
				match(T__3);
				setState(25);
				match(COMPARATOR);
				setState(26);
				match(NUMBER);
				setState(29);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
				case 1:
					{
					setState(27);
					match(AND);
					setState(28);
					expression();
					}
					break;
				}
				}
				break;
			case 6:
				{
				setState(31);
				match(T__4);
				setState(32);
				match(COMPARATOR);
				setState(33);
				match(WORD);
				setState(36);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,4,_ctx) ) {
				case 1:
					{
					setState(34);
					match(AND);
					setState(35);
					expression();
					}
					break;
				}
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\r+\4\2\t\2\3\2\3"+
		"\2\3\2\3\2\3\2\3\2\5\2\13\n\2\3\2\3\2\3\2\3\2\3\2\5\2\22\n\2\3\2\3\2\3"+
		"\2\3\2\3\2\5\2\31\n\2\3\2\3\2\3\2\3\2\3\2\5\2 \n\2\3\2\3\2\3\2\3\2\3\2"+
		"\5\2\'\n\2\5\2)\n\2\3\2\2\2\3\2\2\2\2\63\2(\3\2\2\2\4)\3\2\2\2\5\6\7\3"+
		"\2\2\6\7\7\n\2\2\7\n\7\13\2\2\b\t\7\t\2\2\t\13\5\2\2\2\n\b\3\2\2\2\n\13"+
		"\3\2\2\2\13)\3\2\2\2\f\r\7\4\2\2\r\16\7\n\2\2\16\21\7\f\2\2\17\20\7\t"+
		"\2\2\20\22\5\2\2\2\21\17\3\2\2\2\21\22\3\2\2\2\22)\3\2\2\2\23\24\7\5\2"+
		"\2\24\25\7\n\2\2\25\30\7\b\2\2\26\27\7\t\2\2\27\31\5\2\2\2\30\26\3\2\2"+
		"\2\30\31\3\2\2\2\31)\3\2\2\2\32\33\7\6\2\2\33\34\7\n\2\2\34\37\7\13\2"+
		"\2\35\36\7\t\2\2\36 \5\2\2\2\37\35\3\2\2\2\37 \3\2\2\2 )\3\2\2\2!\"\7"+
		"\7\2\2\"#\7\n\2\2#&\7\f\2\2$%\7\t\2\2%\'\5\2\2\2&$\3\2\2\2&\'\3\2\2\2"+
		"\')\3\2\2\2(\4\3\2\2\2(\5\3\2\2\2(\f\3\2\2\2(\23\3\2\2\2(\32\3\2\2\2("+
		"!\3\2\2\2)\3\3\2\2\2\b\n\21\30\37&(";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}