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
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, TYPE=8, AND=9, 
		COMPARATOR=10, NUMBER=11, WORD=12, WS=13;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "TYPE", "AND", 
			"COMPARATOR", "NUMBER", "WORD", "WS"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'population'", "'token'", "'type'", "'confidence'", "'context'", 
			"'classification'", "'sentiment'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, "TYPE", "AND", "COMPARATOR", 
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
		"\u0004\u0000\r\u00b2\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002\u0001"+
		"\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004"+
		"\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007"+
		"\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b"+
		"\u0007\u000b\u0002\f\u0007\f\u0001\u0000\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0003\u0007k\b\u0007\u0001"+
		"\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0003\bt\b\b\u0001"+
		"\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001"+
		"\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001"+
		"\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001"+
		"\t\u0003\t\u0092\b\t\u0001\n\u0004\n\u0095\b\n\u000b\n\f\n\u0096\u0001"+
		"\n\u0001\n\u0004\n\u009b\b\n\u000b\n\f\n\u009c\u0003\n\u009f\b\n\u0001"+
		"\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0005\u000b\u00a5\b\u000b\n"+
		"\u000b\f\u000b\u00a8\t\u000b\u0001\u000b\u0001\u000b\u0001\f\u0004\f\u00ad"+
		"\b\f\u000b\f\f\f\u00ae\u0001\f\u0001\f\u0000\u0000\r\u0001\u0001\u0003"+
		"\u0002\u0005\u0003\u0007\u0004\t\u0005\u000b\u0006\r\u0007\u000f\b\u0011"+
		"\t\u0013\n\u0015\u000b\u0017\f\u0019\r\u0001\u0000\u0004\u0002\u0000<"+
		"<>>\u0004\u0000\n\n\r\r\"\"\\\\\u0002\u0000\"\"\\\\\u0001\u0000  \u00c5"+
		"\u0000\u0001\u0001\u0000\u0000\u0000\u0000\u0003\u0001\u0000\u0000\u0000"+
		"\u0000\u0005\u0001\u0000\u0000\u0000\u0000\u0007\u0001\u0000\u0000\u0000"+
		"\u0000\t\u0001\u0000\u0000\u0000\u0000\u000b\u0001\u0000\u0000\u0000\u0000"+
		"\r\u0001\u0000\u0000\u0000\u0000\u000f\u0001\u0000\u0000\u0000\u0000\u0011"+
		"\u0001\u0000\u0000\u0000\u0000\u0013\u0001\u0000\u0000\u0000\u0000\u0015"+
		"\u0001\u0000\u0000\u0000\u0000\u0017\u0001\u0000\u0000\u0000\u0000\u0019"+
		"\u0001\u0000\u0000\u0000\u0001\u001b\u0001\u0000\u0000\u0000\u0003&\u0001"+
		"\u0000\u0000\u0000\u0005,\u0001\u0000\u0000\u0000\u00071\u0001\u0000\u0000"+
		"\u0000\t<\u0001\u0000\u0000\u0000\u000bD\u0001\u0000\u0000\u0000\rS\u0001"+
		"\u0000\u0000\u0000\u000fj\u0001\u0000\u0000\u0000\u0011s\u0001\u0000\u0000"+
		"\u0000\u0013\u0091\u0001\u0000\u0000\u0000\u0015\u0094\u0001\u0000\u0000"+
		"\u0000\u0017\u00a0\u0001\u0000\u0000\u0000\u0019\u00ac\u0001\u0000\u0000"+
		"\u0000\u001b\u001c\u0005p\u0000\u0000\u001c\u001d\u0005o\u0000\u0000\u001d"+
		"\u001e\u0005p\u0000\u0000\u001e\u001f\u0005u\u0000\u0000\u001f \u0005"+
		"l\u0000\u0000 !\u0005a\u0000\u0000!\"\u0005t\u0000\u0000\"#\u0005i\u0000"+
		"\u0000#$\u0005o\u0000\u0000$%\u0005n\u0000\u0000%\u0002\u0001\u0000\u0000"+
		"\u0000&\'\u0005t\u0000\u0000\'(\u0005o\u0000\u0000()\u0005k\u0000\u0000"+
		")*\u0005e\u0000\u0000*+\u0005n\u0000\u0000+\u0004\u0001\u0000\u0000\u0000"+
		",-\u0005t\u0000\u0000-.\u0005y\u0000\u0000./\u0005p\u0000\u0000/0\u0005"+
		"e\u0000\u00000\u0006\u0001\u0000\u0000\u000012\u0005c\u0000\u000023\u0005"+
		"o\u0000\u000034\u0005n\u0000\u000045\u0005f\u0000\u000056\u0005i\u0000"+
		"\u000067\u0005d\u0000\u000078\u0005e\u0000\u000089\u0005n\u0000\u0000"+
		"9:\u0005c\u0000\u0000:;\u0005e\u0000\u0000;\b\u0001\u0000\u0000\u0000"+
		"<=\u0005c\u0000\u0000=>\u0005o\u0000\u0000>?\u0005n\u0000\u0000?@\u0005"+
		"t\u0000\u0000@A\u0005e\u0000\u0000AB\u0005x\u0000\u0000BC\u0005t\u0000"+
		"\u0000C\n\u0001\u0000\u0000\u0000DE\u0005c\u0000\u0000EF\u0005l\u0000"+
		"\u0000FG\u0005a\u0000\u0000GH\u0005s\u0000\u0000HI\u0005s\u0000\u0000"+
		"IJ\u0005i\u0000\u0000JK\u0005f\u0000\u0000KL\u0005i\u0000\u0000LM\u0005"+
		"c\u0000\u0000MN\u0005a\u0000\u0000NO\u0005t\u0000\u0000OP\u0005i\u0000"+
		"\u0000PQ\u0005o\u0000\u0000QR\u0005n\u0000\u0000R\f\u0001\u0000\u0000"+
		"\u0000ST\u0005s\u0000\u0000TU\u0005e\u0000\u0000UV\u0005n\u0000\u0000"+
		"VW\u0005t\u0000\u0000WX\u0005i\u0000\u0000XY\u0005m\u0000\u0000YZ\u0005"+
		"e\u0000\u0000Z[\u0005n\u0000\u0000[\\\u0005t\u0000\u0000\\\u000e\u0001"+
		"\u0000\u0000\u0000]k\u0001\u0000\u0000\u0000^_\u0005P\u0000\u0000_`\u0005"+
		"E\u0000\u0000`k\u0005R\u0000\u0000ab\u0005p\u0000\u0000bc\u0005e\u0000"+
		"\u0000ck\u0005r\u0000\u0000de\u0005L\u0000\u0000ef\u0005O\u0000\u0000"+
		"fk\u0005C\u0000\u0000gh\u0005l\u0000\u0000hi\u0005o\u0000\u0000ik\u0005"+
		"c\u0000\u0000j]\u0001\u0000\u0000\u0000j^\u0001\u0000\u0000\u0000ja\u0001"+
		"\u0000\u0000\u0000jd\u0001\u0000\u0000\u0000jg\u0001\u0000\u0000\u0000"+
		"k\u0010\u0001\u0000\u0000\u0000lt\u0001\u0000\u0000\u0000mn\u0005A\u0000"+
		"\u0000no\u0005N\u0000\u0000ot\u0005D\u0000\u0000pq\u0005a\u0000\u0000"+
		"qr\u0005n\u0000\u0000rt\u0005d\u0000\u0000sl\u0001\u0000\u0000\u0000s"+
		"m\u0001\u0000\u0000\u0000sp\u0001\u0000\u0000\u0000t\u0012\u0001\u0000"+
		"\u0000\u0000u\u0092\u0001\u0000\u0000\u0000v\u0092\u0007\u0000\u0000\u0000"+
		"wx\u0005<\u0000\u0000x\u0092\u0005=\u0000\u0000yz\u0005=\u0000\u0000z"+
		"\u0092\u0005>\u0000\u0000{|\u0005=\u0000\u0000|\u0092\u0005=\u0000\u0000"+
		"}~\u0005!\u0000\u0000~\u0092\u0005=\u0000\u0000\u007f\u0080\u0005s\u0000"+
		"\u0000\u0080\u0081\u0005t\u0000\u0000\u0081\u0082\u0005a\u0000\u0000\u0082"+
		"\u0083\u0005r\u0000\u0000\u0083\u0084\u0005t\u0000\u0000\u0084\u0085\u0005"+
		"s\u0000\u0000\u0085\u0086\u0005w\u0000\u0000\u0086\u0087\u0005i\u0000"+
		"\u0000\u0087\u0088\u0005t\u0000\u0000\u0088\u0092\u0005h\u0000\u0000\u0089"+
		"\u008a\u0005i\u0000\u0000\u008a\u0092\u0005s\u0000\u0000\u008b\u008c\u0005"+
		"i\u0000\u0000\u008c\u008d\u0005s\u0000\u0000\u008d\u008e\u0005 \u0000"+
		"\u0000\u008e\u008f\u0005n\u0000\u0000\u008f\u0090\u0005o\u0000\u0000\u0090"+
		"\u0092\u0005t\u0000\u0000\u0091u\u0001\u0000\u0000\u0000\u0091v\u0001"+
		"\u0000\u0000\u0000\u0091w\u0001\u0000\u0000\u0000\u0091y\u0001\u0000\u0000"+
		"\u0000\u0091{\u0001\u0000\u0000\u0000\u0091}\u0001\u0000\u0000\u0000\u0091"+
		"\u007f\u0001\u0000\u0000\u0000\u0091\u0089\u0001\u0000\u0000\u0000\u0091"+
		"\u008b\u0001\u0000\u0000\u0000\u0092\u0014\u0001\u0000\u0000\u0000\u0093"+
		"\u0095\u000209\u0000\u0094\u0093\u0001\u0000\u0000\u0000\u0095\u0096\u0001"+
		"\u0000\u0000\u0000\u0096\u0094\u0001\u0000\u0000\u0000\u0096\u0097\u0001"+
		"\u0000\u0000\u0000\u0097\u009e\u0001\u0000\u0000\u0000\u0098\u009a\u0005"+
		".\u0000\u0000\u0099\u009b\u000209\u0000\u009a\u0099\u0001\u0000\u0000"+
		"\u0000\u009b\u009c\u0001\u0000\u0000\u0000\u009c\u009a\u0001\u0000\u0000"+
		"\u0000\u009c\u009d\u0001\u0000\u0000\u0000\u009d\u009f\u0001\u0000\u0000"+
		"\u0000\u009e\u0098\u0001\u0000\u0000\u0000\u009e\u009f\u0001\u0000\u0000"+
		"\u0000\u009f\u0016\u0001\u0000\u0000\u0000\u00a0\u00a6\u0005\"\u0000\u0000"+
		"\u00a1\u00a5\b\u0001\u0000\u0000\u00a2\u00a3\u0005\\\u0000\u0000\u00a3"+
		"\u00a5\u0007\u0002\u0000\u0000\u00a4\u00a1\u0001\u0000\u0000\u0000\u00a4"+
		"\u00a2\u0001\u0000\u0000\u0000\u00a5\u00a8\u0001\u0000\u0000\u0000\u00a6"+
		"\u00a4\u0001\u0000\u0000\u0000\u00a6\u00a7\u0001\u0000\u0000\u0000\u00a7"+
		"\u00a9\u0001\u0000\u0000\u0000\u00a8\u00a6\u0001\u0000\u0000\u0000\u00a9"+
		"\u00aa\u0005\"\u0000\u0000\u00aa\u0018\u0001\u0000\u0000\u0000\u00ab\u00ad"+
		"\u0007\u0003\u0000\u0000\u00ac\u00ab\u0001\u0000\u0000\u0000\u00ad\u00ae"+
		"\u0001\u0000\u0000\u0000\u00ae\u00ac\u0001\u0000\u0000\u0000\u00ae\u00af"+
		"\u0001\u0000\u0000\u0000\u00af\u00b0\u0001\u0000\u0000\u0000\u00b0\u00b1"+
		"\u0006\f\u0000\u0000\u00b1\u001a\u0001\u0000\u0000\u0000\n\u0000js\u0091"+
		"\u0096\u009c\u009e\u00a4\u00a6\u00ae\u0001\u0006\u0000\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}