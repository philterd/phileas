// Generated from FilterCondition.g4 by ANTLR 4.13.2
package ai.philterd.phileas.model.conditions.parser;

import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.RuntimeMetaData;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.VocabularyImpl;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.ParserATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.TerminalNode;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class FilterConditionParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, TYPE=7, AND=8, COMPARATOR=9, 
		NUMBER=10, WORD=11, WS=12;
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
			null, "'population'", "'token'", "'type'", "'confidence'", "'context'", 
			"'classification'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, "TYPE", "AND", "COMPARATOR", 
			"NUMBER", "WORD", "WS"
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
		public TerminalNode EOF() { return getToken(FilterConditionParser.EOF, 0); }
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
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(45);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case EOF:
				{
				}
				break;
			case T__0:
				{
				setState(3);
				match(T__0);
				setState(4);
				match(COMPARATOR);
				setState(5);
				match(NUMBER);
				setState(8);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==AND) {
					{
					setState(6);
					match(AND);
					setState(7);
					expression();
					}
				}

				}
				break;
			case T__1:
				{
				setState(10);
				match(T__1);
				setState(11);
				match(COMPARATOR);
				setState(12);
				match(WORD);
				setState(15);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==AND) {
					{
					setState(13);
					match(AND);
					setState(14);
					expression();
					}
				}

				}
				break;
			case T__2:
				{
				setState(17);
				match(T__2);
				setState(18);
				match(COMPARATOR);
				setState(19);
				match(TYPE);
				setState(22);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==AND) {
					{
					setState(20);
					match(AND);
					setState(21);
					expression();
					}
				}

				}
				break;
			case T__3:
				{
				setState(24);
				match(T__3);
				setState(25);
				match(COMPARATOR);
				setState(26);
				match(NUMBER);
				setState(29);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==AND) {
					{
					setState(27);
					match(AND);
					setState(28);
					expression();
					}
				}

				}
				break;
			case T__4:
				{
				setState(31);
				match(T__4);
				setState(32);
				match(COMPARATOR);
				setState(33);
				match(WORD);
				setState(36);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==AND) {
					{
					setState(34);
					match(AND);
					setState(35);
					expression();
					}
				}

				}
				break;
			case T__5:
				{
				setState(38);
				match(T__5);
				setState(39);
				match(COMPARATOR);
				setState(40);
				match(WORD);
				setState(43);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==AND) {
					{
					setState(41);
					match(AND);
					setState(42);
					expression();
					}
				}

				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(47);
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

	public static final String _serializedATN =
		"\u0004\u0001\f2\u0002\u0000\u0007\u0000\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0003\u0000\t\b\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0003\u0000\u0010"+
		"\b\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0003"+
		"\u0000\u0017\b\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0003\u0000\u001e\b\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0003\u0000%\b\u0000\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0003\u0000,\b\u0000\u0003\u0000.\b\u0000"+
		"\u0001\u0000\u0001\u0000\u0001\u0000\u0000\u0000\u0001\u0000\u0000\u0000"+
		"<\u0000-\u0001\u0000\u0000\u0000\u0002.\u0001\u0000\u0000\u0000\u0003"+
		"\u0004\u0005\u0001\u0000\u0000\u0004\u0005\u0005\t\u0000\u0000\u0005\b"+
		"\u0005\n\u0000\u0000\u0006\u0007\u0005\b\u0000\u0000\u0007\t\u0003\u0000"+
		"\u0000\u0000\b\u0006\u0001\u0000\u0000\u0000\b\t\u0001\u0000\u0000\u0000"+
		"\t.\u0001\u0000\u0000\u0000\n\u000b\u0005\u0002\u0000\u0000\u000b\f\u0005"+
		"\t\u0000\u0000\f\u000f\u0005\u000b\u0000\u0000\r\u000e\u0005\b\u0000\u0000"+
		"\u000e\u0010\u0003\u0000\u0000\u0000\u000f\r\u0001\u0000\u0000\u0000\u000f"+
		"\u0010\u0001\u0000\u0000\u0000\u0010.\u0001\u0000\u0000\u0000\u0011\u0012"+
		"\u0005\u0003\u0000\u0000\u0012\u0013\u0005\t\u0000\u0000\u0013\u0016\u0005"+
		"\u0007\u0000\u0000\u0014\u0015\u0005\b\u0000\u0000\u0015\u0017\u0003\u0000"+
		"\u0000\u0000\u0016\u0014\u0001\u0000\u0000\u0000\u0016\u0017\u0001\u0000"+
		"\u0000\u0000\u0017.\u0001\u0000\u0000\u0000\u0018\u0019\u0005\u0004\u0000"+
		"\u0000\u0019\u001a\u0005\t\u0000\u0000\u001a\u001d\u0005\n\u0000\u0000"+
		"\u001b\u001c\u0005\b\u0000\u0000\u001c\u001e\u0003\u0000\u0000\u0000\u001d"+
		"\u001b\u0001\u0000\u0000\u0000\u001d\u001e\u0001\u0000\u0000\u0000\u001e"+
		".\u0001\u0000\u0000\u0000\u001f \u0005\u0005\u0000\u0000 !\u0005\t\u0000"+
		"\u0000!$\u0005\u000b\u0000\u0000\"#\u0005\b\u0000\u0000#%\u0003\u0000"+
		"\u0000\u0000$\"\u0001\u0000\u0000\u0000$%\u0001\u0000\u0000\u0000%.\u0001"+
		"\u0000\u0000\u0000&\'\u0005\u0006\u0000\u0000\'(\u0005\t\u0000\u0000("+
		"+\u0005\u000b\u0000\u0000)*\u0005\b\u0000\u0000*,\u0003\u0000\u0000\u0000"+
		"+)\u0001\u0000\u0000\u0000+,\u0001\u0000\u0000\u0000,.\u0001\u0000\u0000"+
		"\u0000-\u0002\u0001\u0000\u0000\u0000-\u0003\u0001\u0000\u0000\u0000-"+
		"\n\u0001\u0000\u0000\u0000-\u0011\u0001\u0000\u0000\u0000-\u0018\u0001"+
		"\u0000\u0000\u0000-\u001f\u0001\u0000\u0000\u0000-&\u0001\u0000\u0000"+
		"\u0000./\u0001\u0000\u0000\u0000/0\u0005\u0000\u0000\u00010\u0001\u0001"+
		"\u0000\u0000\u0000\u0007\b\u000f\u0016\u001d$+-";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}