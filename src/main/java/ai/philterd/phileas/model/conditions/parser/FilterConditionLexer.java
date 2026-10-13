// Generated from FilterCondition.g4 by ANTLR 4.13.2
package ai.philterd.phileas.model.conditions.parser;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class FilterConditionLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, TYPE=6, AND=7, COMPARATOR=8, NUMBER=9, 
		WORD=10, WS=11;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "T__4", "TYPE", "AND", "COMPARATOR", 
			"NUMBER", "WORD", "WS"
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


	public FilterConditionLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "FilterCondition.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\u0004\u0000\u000b\u0092\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002"+
		"\u0001\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002"+
		"\u0004\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002"+
		"\u0007\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0003\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0003\u0005M\b"+
		"\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0003\u0006U\b\u0006\u0001\u0007\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0003"+
		"\u0007r\b\u0007\u0001\b\u0004\bu\b\b\u000b\b\f\bv\u0001\b\u0001\b\u0004"+
		"\b{\b\b\u000b\b\f\b|\u0003\b\u007f\b\b\u0001\t\u0001\t\u0001\t\u0001\t"+
		"\u0005\t\u0085\b\t\n\t\f\t\u0088\t\t\u0001\t\u0001\t\u0001\n\u0004\n\u008d"+
		"\b\n\u000b\n\f\n\u008e\u0001\n\u0001\n\u0000\u0000\u000b\u0001\u0001\u0003"+
		"\u0002\u0005\u0003\u0007\u0004\t\u0005\u000b\u0006\r\u0007\u000f\b\u0011"+
		"\t\u0013\n\u0015\u000b\u0001\u0000\u0004\u0002\u0000<<>>\u0004\u0000\n"+
		"\n\r\r\"\"\\\\\u0002\u0000\"\"\\\\\u0001\u0000  \u00a2\u0000\u0001\u0001"+
		"\u0000\u0000\u0000\u0000\u0003\u0001\u0000\u0000\u0000\u0000\u0005\u0001"+
		"\u0000\u0000\u0000\u0000\u0007\u0001\u0000\u0000\u0000\u0000\t\u0001\u0000"+
		"\u0000\u0000\u0000\u000b\u0001\u0000\u0000\u0000\u0000\r\u0001\u0000\u0000"+
		"\u0000\u0000\u000f\u0001\u0000\u0000\u0000\u0000\u0011\u0001\u0000\u0000"+
		"\u0000\u0000\u0013\u0001\u0000\u0000\u0000\u0000\u0015\u0001\u0000\u0000"+
		"\u0000\u0001\u0017\u0001\u0000\u0000\u0000\u0003\"\u0001\u0000\u0000\u0000"+
		"\u0005(\u0001\u0000\u0000\u0000\u0007-\u0001\u0000\u0000\u0000\t8\u0001"+
		"\u0000\u0000\u0000\u000bL\u0001\u0000\u0000\u0000\rT\u0001\u0000\u0000"+
		"\u0000\u000fq\u0001\u0000\u0000\u0000\u0011t\u0001\u0000\u0000\u0000\u0013"+
		"\u0080\u0001\u0000\u0000\u0000\u0015\u008c\u0001\u0000\u0000\u0000\u0017"+
		"\u0018\u0005p\u0000\u0000\u0018\u0019\u0005o\u0000\u0000\u0019\u001a\u0005"+
		"p\u0000\u0000\u001a\u001b\u0005u\u0000\u0000\u001b\u001c\u0005l\u0000"+
		"\u0000\u001c\u001d\u0005a\u0000\u0000\u001d\u001e\u0005t\u0000\u0000\u001e"+
		"\u001f\u0005i\u0000\u0000\u001f \u0005o\u0000\u0000 !\u0005n\u0000\u0000"+
		"!\u0002\u0001\u0000\u0000\u0000\"#\u0005t\u0000\u0000#$\u0005o\u0000\u0000"+
		"$%\u0005k\u0000\u0000%&\u0005e\u0000\u0000&\'\u0005n\u0000\u0000\'\u0004"+
		"\u0001\u0000\u0000\u0000()\u0005t\u0000\u0000)*\u0005y\u0000\u0000*+\u0005"+
		"p\u0000\u0000+,\u0005e\u0000\u0000,\u0006\u0001\u0000\u0000\u0000-.\u0005"+
		"c\u0000\u0000./\u0005o\u0000\u0000/0\u0005n\u0000\u000001\u0005f\u0000"+
		"\u000012\u0005i\u0000\u000023\u0005d\u0000\u000034\u0005e\u0000\u0000"+
		"45\u0005n\u0000\u000056\u0005c\u0000\u000067\u0005e\u0000\u00007\b\u0001"+
		"\u0000\u0000\u000089\u0005c\u0000\u00009:\u0005o\u0000\u0000:;\u0005n"+
		"\u0000\u0000;<\u0005t\u0000\u0000<=\u0005e\u0000\u0000=>\u0005x\u0000"+
		"\u0000>?\u0005t\u0000\u0000?\n\u0001\u0000\u0000\u0000@A\u0005P\u0000"+
		"\u0000AB\u0005E\u0000\u0000BM\u0005R\u0000\u0000CD\u0005p\u0000\u0000"+
		"DE\u0005e\u0000\u0000EM\u0005r\u0000\u0000FG\u0005L\u0000\u0000GH\u0005"+
		"O\u0000\u0000HM\u0005C\u0000\u0000IJ\u0005l\u0000\u0000JK\u0005o\u0000"+
		"\u0000KM\u0005c\u0000\u0000L@\u0001\u0000\u0000\u0000LC\u0001\u0000\u0000"+
		"\u0000LF\u0001\u0000\u0000\u0000LI\u0001\u0000\u0000\u0000M\f\u0001\u0000"+
		"\u0000\u0000NO\u0005A\u0000\u0000OP\u0005N\u0000\u0000PU\u0005D\u0000"+
		"\u0000QR\u0005a\u0000\u0000RS\u0005n\u0000\u0000SU\u0005d\u0000\u0000"+
		"TN\u0001\u0000\u0000\u0000TQ\u0001\u0000\u0000\u0000U\u000e\u0001\u0000"+
		"\u0000\u0000Vr\u0007\u0000\u0000\u0000WX\u0005<\u0000\u0000Xr\u0005=\u0000"+
		"\u0000YZ\u0005>\u0000\u0000Zr\u0005=\u0000\u0000[\\\u0005=\u0000\u0000"+
		"\\r\u0005=\u0000\u0000]^\u0005!\u0000\u0000^r\u0005=\u0000\u0000_`\u0005"+
		"s\u0000\u0000`a\u0005t\u0000\u0000ab\u0005a\u0000\u0000bc\u0005r\u0000"+
		"\u0000cd\u0005t\u0000\u0000de\u0005s\u0000\u0000ef\u0005w\u0000\u0000"+
		"fg\u0005i\u0000\u0000gh\u0005t\u0000\u0000hr\u0005h\u0000\u0000ij\u0005"+
		"i\u0000\u0000jr\u0005s\u0000\u0000kl\u0005i\u0000\u0000lm\u0005s\u0000"+
		"\u0000mn\u0005 \u0000\u0000no\u0005n\u0000\u0000op\u0005o\u0000\u0000"+
		"pr\u0005t\u0000\u0000qV\u0001\u0000\u0000\u0000qW\u0001\u0000\u0000\u0000"+
		"qY\u0001\u0000\u0000\u0000q[\u0001\u0000\u0000\u0000q]\u0001\u0000\u0000"+
		"\u0000q_\u0001\u0000\u0000\u0000qi\u0001\u0000\u0000\u0000qk\u0001\u0000"+
		"\u0000\u0000r\u0010\u0001\u0000\u0000\u0000su\u000209\u0000ts\u0001\u0000"+
		"\u0000\u0000uv\u0001\u0000\u0000\u0000vt\u0001\u0000\u0000\u0000vw\u0001"+
		"\u0000\u0000\u0000w~\u0001\u0000\u0000\u0000xz\u0005.\u0000\u0000y{\u0002"+
		"09\u0000zy\u0001\u0000\u0000\u0000{|\u0001\u0000\u0000\u0000|z\u0001\u0000"+
		"\u0000\u0000|}\u0001\u0000\u0000\u0000}\u007f\u0001\u0000\u0000\u0000"+
		"~x\u0001\u0000\u0000\u0000~\u007f\u0001\u0000\u0000\u0000\u007f\u0012"+
		"\u0001\u0000\u0000\u0000\u0080\u0086\u0005\"\u0000\u0000\u0081\u0085\b"+
		"\u0001\u0000\u0000\u0082\u0083\u0005\\\u0000\u0000\u0083\u0085\u0007\u0002"+
		"\u0000\u0000\u0084\u0081\u0001\u0000\u0000\u0000\u0084\u0082\u0001\u0000"+
		"\u0000\u0000\u0085\u0088\u0001\u0000\u0000\u0000\u0086\u0084\u0001\u0000"+
		"\u0000\u0000\u0086\u0087\u0001\u0000\u0000\u0000\u0087\u0089\u0001\u0000"+
		"\u0000\u0000\u0088\u0086\u0001\u0000\u0000\u0000\u0089\u008a\u0005\"\u0000"+
		"\u0000\u008a\u0014\u0001\u0000\u0000\u0000\u008b\u008d\u0007\u0003\u0000"+
		"\u0000\u008c\u008b\u0001\u0000\u0000\u0000\u008d\u008e\u0001\u0000\u0000"+
		"\u0000\u008e\u008c\u0001\u0000\u0000\u0000\u008e\u008f\u0001\u0000\u0000"+
		"\u0000\u008f\u0090\u0001\u0000\u0000\u0000\u0090\u0091\u0006\n\u0000\u0000"+
		"\u0091\u0016\u0001\u0000\u0000\u0000\n\u0000LTqv|~\u0084\u0086\u008e\u0001"+
		"\u0006\u0000\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}