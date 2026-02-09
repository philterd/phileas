// Generated from FilterCondition.g4 by ANTLR 4.13.2
package ai.philterd.phileas.model.conditions.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.RuntimeMetaData;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.VocabularyImpl;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class FilterConditionLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, TYPE=7, AND=8, COMPARATOR=9, 
		NUMBER=10, WORD=11, WS=12;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "TYPE", "AND", "COMPARATOR", 
			"NUMBER", "WORD", "WS"
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
		"\u0004\u0000\f\u00a6\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002\u0001"+
		"\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004"+
		"\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007"+
		"\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b"+
		"\u0007\u000b\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000"+
		"\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004"+
		"\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0006\u0001\u0006\u0001\u0006"+
		"\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006"+
		"\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0003\u0006_\b\u0006"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0003\u0007h\b\u0007\u0001\b\u0001\b\u0001\b\u0001\b\u0001"+
		"\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001"+
		"\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001"+
		"\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0003\b\u0086\b\b\u0001\t\u0004"+
		"\t\u0089\b\t\u000b\t\f\t\u008a\u0001\t\u0001\t\u0004\t\u008f\b\t\u000b"+
		"\t\f\t\u0090\u0003\t\u0093\b\t\u0001\n\u0001\n\u0001\n\u0001\n\u0005\n"+
		"\u0099\b\n\n\n\f\n\u009c\t\n\u0001\n\u0001\n\u0001\u000b\u0004\u000b\u00a1"+
		"\b\u000b\u000b\u000b\f\u000b\u00a2\u0001\u000b\u0001\u000b\u0000\u0000"+
		"\f\u0001\u0001\u0003\u0002\u0005\u0003\u0007\u0004\t\u0005\u000b\u0006"+
		"\r\u0007\u000f\b\u0011\t\u0013\n\u0015\u000b\u0017\f\u0001\u0000\u0004"+
		"\u0002\u0000<<>>\u0004\u0000\n\n\r\r\"\"\\\\\u0002\u0000\"\"\\\\\u0001"+
		"\u0000  \u00b9\u0000\u0001\u0001\u0000\u0000\u0000\u0000\u0003\u0001\u0000"+
		"\u0000\u0000\u0000\u0005\u0001\u0000\u0000\u0000\u0000\u0007\u0001\u0000"+
		"\u0000\u0000\u0000\t\u0001\u0000\u0000\u0000\u0000\u000b\u0001\u0000\u0000"+
		"\u0000\u0000\r\u0001\u0000\u0000\u0000\u0000\u000f\u0001\u0000\u0000\u0000"+
		"\u0000\u0011\u0001\u0000\u0000\u0000\u0000\u0013\u0001\u0000\u0000\u0000"+
		"\u0000\u0015\u0001\u0000\u0000\u0000\u0000\u0017\u0001\u0000\u0000\u0000"+
		"\u0001\u0019\u0001\u0000\u0000\u0000\u0003$\u0001\u0000\u0000\u0000\u0005"+
		"*\u0001\u0000\u0000\u0000\u0007/\u0001\u0000\u0000\u0000\t:\u0001\u0000"+
		"\u0000\u0000\u000bB\u0001\u0000\u0000\u0000\r^\u0001\u0000\u0000\u0000"+
		"\u000fg\u0001\u0000\u0000\u0000\u0011\u0085\u0001\u0000\u0000\u0000\u0013"+
		"\u0088\u0001\u0000\u0000\u0000\u0015\u0094\u0001\u0000\u0000\u0000\u0017"+
		"\u00a0\u0001\u0000\u0000\u0000\u0019\u001a\u0005p\u0000\u0000\u001a\u001b"+
		"\u0005o\u0000\u0000\u001b\u001c\u0005p\u0000\u0000\u001c\u001d\u0005u"+
		"\u0000\u0000\u001d\u001e\u0005l\u0000\u0000\u001e\u001f\u0005a\u0000\u0000"+
		"\u001f \u0005t\u0000\u0000 !\u0005i\u0000\u0000!\"\u0005o\u0000\u0000"+
		"\"#\u0005n\u0000\u0000#\u0002\u0001\u0000\u0000\u0000$%\u0005t\u0000\u0000"+
		"%&\u0005o\u0000\u0000&\'\u0005k\u0000\u0000\'(\u0005e\u0000\u0000()\u0005"+
		"n\u0000\u0000)\u0004\u0001\u0000\u0000\u0000*+\u0005t\u0000\u0000+,\u0005"+
		"y\u0000\u0000,-\u0005p\u0000\u0000-.\u0005e\u0000\u0000.\u0006\u0001\u0000"+
		"\u0000\u0000/0\u0005c\u0000\u000001\u0005o\u0000\u000012\u0005n\u0000"+
		"\u000023\u0005f\u0000\u000034\u0005i\u0000\u000045\u0005d\u0000\u0000"+
		"56\u0005e\u0000\u000067\u0005n\u0000\u000078\u0005c\u0000\u000089\u0005"+
		"e\u0000\u00009\b\u0001\u0000\u0000\u0000:;\u0005c\u0000\u0000;<\u0005"+
		"o\u0000\u0000<=\u0005n\u0000\u0000=>\u0005t\u0000\u0000>?\u0005e\u0000"+
		"\u0000?@\u0005x\u0000\u0000@A\u0005t\u0000\u0000A\n\u0001\u0000\u0000"+
		"\u0000BC\u0005c\u0000\u0000CD\u0005l\u0000\u0000DE\u0005a\u0000\u0000"+
		"EF\u0005s\u0000\u0000FG\u0005s\u0000\u0000GH\u0005i\u0000\u0000HI\u0005"+
		"f\u0000\u0000IJ\u0005i\u0000\u0000JK\u0005c\u0000\u0000KL\u0005a\u0000"+
		"\u0000LM\u0005t\u0000\u0000MN\u0005i\u0000\u0000NO\u0005o\u0000\u0000"+
		"OP\u0005n\u0000\u0000P\f\u0001\u0000\u0000\u0000Q_\u0001\u0000\u0000\u0000"+
		"RS\u0005P\u0000\u0000ST\u0005E\u0000\u0000T_\u0005R\u0000\u0000UV\u0005"+
		"p\u0000\u0000VW\u0005e\u0000\u0000W_\u0005r\u0000\u0000XY\u0005L\u0000"+
		"\u0000YZ\u0005O\u0000\u0000Z_\u0005C\u0000\u0000[\\\u0005l\u0000\u0000"+
		"\\]\u0005o\u0000\u0000]_\u0005c\u0000\u0000^Q\u0001\u0000\u0000\u0000"+
		"^R\u0001\u0000\u0000\u0000^U\u0001\u0000\u0000\u0000^X\u0001\u0000\u0000"+
		"\u0000^[\u0001\u0000\u0000\u0000_\u000e\u0001\u0000\u0000\u0000`h\u0001"+
		"\u0000\u0000\u0000ab\u0005A\u0000\u0000bc\u0005N\u0000\u0000ch\u0005D"+
		"\u0000\u0000de\u0005a\u0000\u0000ef\u0005n\u0000\u0000fh\u0005d\u0000"+
		"\u0000g`\u0001\u0000\u0000\u0000ga\u0001\u0000\u0000\u0000gd\u0001\u0000"+
		"\u0000\u0000h\u0010\u0001\u0000\u0000\u0000i\u0086\u0001\u0000\u0000\u0000"+
		"j\u0086\u0007\u0000\u0000\u0000kl\u0005<\u0000\u0000l\u0086\u0005=\u0000"+
		"\u0000mn\u0005>\u0000\u0000n\u0086\u0005=\u0000\u0000op\u0005=\u0000\u0000"+
		"p\u0086\u0005=\u0000\u0000qr\u0005!\u0000\u0000r\u0086\u0005=\u0000\u0000"+
		"st\u0005s\u0000\u0000tu\u0005t\u0000\u0000uv\u0005a\u0000\u0000vw\u0005"+
		"r\u0000\u0000wx\u0005t\u0000\u0000xy\u0005s\u0000\u0000yz\u0005w\u0000"+
		"\u0000z{\u0005i\u0000\u0000{|\u0005t\u0000\u0000|\u0086\u0005h\u0000\u0000"+
		"}~\u0005i\u0000\u0000~\u0086\u0005s\u0000\u0000\u007f\u0080\u0005i\u0000"+
		"\u0000\u0080\u0081\u0005s\u0000\u0000\u0081\u0082\u0005 \u0000\u0000\u0082"+
		"\u0083\u0005n\u0000\u0000\u0083\u0084\u0005o\u0000\u0000\u0084\u0086\u0005"+
		"t\u0000\u0000\u0085i\u0001\u0000\u0000\u0000\u0085j\u0001\u0000\u0000"+
		"\u0000\u0085k\u0001\u0000\u0000\u0000\u0085m\u0001\u0000\u0000\u0000\u0085"+
		"o\u0001\u0000\u0000\u0000\u0085q\u0001\u0000\u0000\u0000\u0085s\u0001"+
		"\u0000\u0000\u0000\u0085}\u0001\u0000\u0000\u0000\u0085\u007f\u0001\u0000"+
		"\u0000\u0000\u0086\u0012\u0001\u0000\u0000\u0000\u0087\u0089\u000209\u0000"+
		"\u0088\u0087\u0001\u0000\u0000\u0000\u0089\u008a\u0001\u0000\u0000\u0000"+
		"\u008a\u0088\u0001\u0000\u0000\u0000\u008a\u008b\u0001\u0000\u0000\u0000"+
		"\u008b\u0092\u0001\u0000\u0000\u0000\u008c\u008e\u0005.\u0000\u0000\u008d"+
		"\u008f\u000209\u0000\u008e\u008d\u0001\u0000\u0000\u0000\u008f\u0090\u0001"+
		"\u0000\u0000\u0000\u0090\u008e\u0001\u0000\u0000\u0000\u0090\u0091\u0001"+
		"\u0000\u0000\u0000\u0091\u0093\u0001\u0000\u0000\u0000\u0092\u008c\u0001"+
		"\u0000\u0000\u0000\u0092\u0093\u0001\u0000\u0000\u0000\u0093\u0014\u0001"+
		"\u0000\u0000\u0000\u0094\u009a\u0005\"\u0000\u0000\u0095\u0099\b\u0001"+
		"\u0000\u0000\u0096\u0097\u0005\\\u0000\u0000\u0097\u0099\u0007\u0002\u0000"+
		"\u0000\u0098\u0095\u0001\u0000\u0000\u0000\u0098\u0096\u0001\u0000\u0000"+
		"\u0000\u0099\u009c\u0001\u0000\u0000\u0000\u009a\u0098\u0001\u0000\u0000"+
		"\u0000\u009a\u009b\u0001\u0000\u0000\u0000\u009b\u009d\u0001\u0000\u0000"+
		"\u0000\u009c\u009a\u0001\u0000\u0000\u0000\u009d\u009e\u0005\"\u0000\u0000"+
		"\u009e\u0016\u0001\u0000\u0000\u0000\u009f\u00a1\u0007\u0003\u0000\u0000"+
		"\u00a0\u009f\u0001\u0000\u0000\u0000\u00a1\u00a2\u0001\u0000\u0000\u0000"+
		"\u00a2\u00a0\u0001\u0000\u0000\u0000\u00a2\u00a3\u0001\u0000\u0000\u0000"+
		"\u00a3\u00a4\u0001\u0000\u0000\u0000\u00a4\u00a5\u0006\u000b\u0000\u0000"+
		"\u00a5\u0018\u0001\u0000\u0000\u0000\n\u0000^g\u0085\u008a\u0090\u0092"+
		"\u0098\u009a\u00a2\u0001\u0006\u0000\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}