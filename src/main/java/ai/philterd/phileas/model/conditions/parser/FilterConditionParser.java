// Generated from FilterCondition.g4 by ANTLR 4.13.2
package ai.philterd.phileas.model.conditions.parser;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class FilterConditionParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, TYPE=6, AND=7, COMPARATOR=8, NUMBER=9, 
		WORD=10, WS=11;
	public static final int
		RULE_expression = 0, RULE_condition = 1;
	private static String[] makeRuleNames() {
		return new String[] {
			"expression", "condition"
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

	@SuppressWarnings("CheckReturnValue")
	public static class ExpressionContext extends ParserRuleContext {
		public List<ConditionContext> condition() {
			return getRuleContexts(ConditionContext.class);
		}
		public ConditionContext condition(int i) {
			return getRuleContext(ConditionContext.class,i);
		}
		public TerminalNode EOF() { return getToken(FilterConditionParser.EOF, 0); }
		public List<TerminalNode> AND() { return getTokens(FilterConditionParser.AND); }
		public TerminalNode AND(int i) {
			return getToken(FilterConditionParser.AND, i);
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
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(4);
			condition();
			setState(9);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AND) {
				{
				{
				setState(5);
				match(AND);
				setState(6);
				condition();
				}
				}
				setState(11);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(12);
			match(EOF);
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

	@SuppressWarnings("CheckReturnValue")
	public static class ConditionContext extends ParserRuleContext {
		public TerminalNode COMPARATOR() { return getToken(FilterConditionParser.COMPARATOR, 0); }
		public TerminalNode NUMBER() { return getToken(FilterConditionParser.NUMBER, 0); }
		public TerminalNode WORD() { return getToken(FilterConditionParser.WORD, 0); }
		public TerminalNode TYPE() { return getToken(FilterConditionParser.TYPE, 0); }
		public ConditionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_condition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FilterConditionListener ) ((FilterConditionListener)listener).enterCondition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterConditionListener ) ((FilterConditionListener)listener).exitCondition(this);
		}
	}

	public final ConditionContext condition() throws RecognitionException {
		ConditionContext _localctx = new ConditionContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_condition);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(29);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__0:
				{
				setState(14);
				match(T__0);
				setState(15);
				match(COMPARATOR);
				setState(16);
				match(NUMBER);
				}
				break;
			case T__1:
				{
				setState(17);
				match(T__1);
				setState(18);
				match(COMPARATOR);
				setState(19);
				match(WORD);
				}
				break;
			case T__2:
				{
				setState(20);
				match(T__2);
				setState(21);
				match(COMPARATOR);
				setState(22);
				match(TYPE);
				}
				break;
			case T__3:
				{
				setState(23);
				match(T__3);
				setState(24);
				match(COMPARATOR);
				setState(25);
				match(NUMBER);
				}
				break;
			case T__4:
				{
				setState(26);
				match(T__4);
				setState(27);
				match(COMPARATOR);
				setState(28);
				match(WORD);
				}
				break;
			default:
				throw new NoViableAltException(this);
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
		"\u0004\u0001\u000b \u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0005\u0000\b\b\u0000\n\u0000\f\u0000\u000b"+
		"\t\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0003"+
		"\u0001\u001e\b\u0001\u0001\u0001\u0000\u0000\u0002\u0000\u0002\u0000\u0000"+
		"\"\u0000\u0004\u0001\u0000\u0000\u0000\u0002\u001d\u0001\u0000\u0000\u0000"+
		"\u0004\t\u0003\u0002\u0001\u0000\u0005\u0006\u0005\u0007\u0000\u0000\u0006"+
		"\b\u0003\u0002\u0001\u0000\u0007\u0005\u0001\u0000\u0000\u0000\b\u000b"+
		"\u0001\u0000\u0000\u0000\t\u0007\u0001\u0000\u0000\u0000\t\n\u0001\u0000"+
		"\u0000\u0000\n\f\u0001\u0000\u0000\u0000\u000b\t\u0001\u0000\u0000\u0000"+
		"\f\r\u0005\u0000\u0000\u0001\r\u0001\u0001\u0000\u0000\u0000\u000e\u000f"+
		"\u0005\u0001\u0000\u0000\u000f\u0010\u0005\b\u0000\u0000\u0010\u001e\u0005"+
		"\t\u0000\u0000\u0011\u0012\u0005\u0002\u0000\u0000\u0012\u0013\u0005\b"+
		"\u0000\u0000\u0013\u001e\u0005\n\u0000\u0000\u0014\u0015\u0005\u0003\u0000"+
		"\u0000\u0015\u0016\u0005\b\u0000\u0000\u0016\u001e\u0005\u0006\u0000\u0000"+
		"\u0017\u0018\u0005\u0004\u0000\u0000\u0018\u0019\u0005\b\u0000\u0000\u0019"+
		"\u001e\u0005\t\u0000\u0000\u001a\u001b\u0005\u0005\u0000\u0000\u001b\u001c"+
		"\u0005\b\u0000\u0000\u001c\u001e\u0005\n\u0000\u0000\u001d\u000e\u0001"+
		"\u0000\u0000\u0000\u001d\u0011\u0001\u0000\u0000\u0000\u001d\u0014\u0001"+
		"\u0000\u0000\u0000\u001d\u0017\u0001\u0000\u0000\u0000\u001d\u001a\u0001"+
		"\u0000\u0000\u0000\u001e\u0003\u0001\u0000\u0000\u0000\u0002\t\u001d";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}