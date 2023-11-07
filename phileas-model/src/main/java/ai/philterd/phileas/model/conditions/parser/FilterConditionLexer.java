// Generated from FilterCondition.g4 by ANTLR 4.8
package ai.philterd.phileas.model.conditions.parser;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class FilterConditionLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.8", RuntimeMetaData.VERSION); }

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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\17\u00ae\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2"+
		"\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3\5\3"+
		"\5\3\5\3\5\3\5\3\5\3\5\3\5\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\7\3\7\3\7"+
		"\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3"+
		"\b\3\b\3\b\3\b\3\b\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t"+
		"\5\tm\n\t\3\n\3\n\3\n\3\n\3\n\3\n\3\n\5\nv\n\n\3\13\3\13\3\13\3\13\3\13"+
		"\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13"+
		"\3\13\3\13\3\13\5\13\u008e\n\13\3\f\6\f\u0091\n\f\r\f\16\f\u0092\3\f\3"+
		"\f\6\f\u0097\n\f\r\f\16\f\u0098\5\f\u009b\n\f\3\r\3\r\3\r\3\r\7\r\u00a1"+
		"\n\r\f\r\16\r\u00a4\13\r\3\r\3\r\3\16\6\16\u00a9\n\16\r\16\16\16\u00aa"+
		"\3\16\3\16\2\2\17\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31"+
		"\16\33\17\3\2\6\4\2>>@@\6\2\f\f\17\17$$^^\4\2$$^^\5\2\13\f\17\17\"\"\2"+
		"\u00c0\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2"+
		"\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3"+
		"\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\3\35\3\2\2\2\5(\3\2\2\2\7.\3\2\2\2\t"+
		"\63\3\2\2\2\13>\3\2\2\2\rF\3\2\2\2\17U\3\2\2\2\21l\3\2\2\2\23u\3\2\2\2"+
		"\25\u008d\3\2\2\2\27\u0090\3\2\2\2\31\u009c\3\2\2\2\33\u00a8\3\2\2\2\35"+
		"\36\7r\2\2\36\37\7q\2\2\37 \7r\2\2 !\7w\2\2!\"\7n\2\2\"#\7c\2\2#$\7v\2"+
		"\2$%\7k\2\2%&\7q\2\2&\'\7p\2\2\'\4\3\2\2\2()\7v\2\2)*\7q\2\2*+\7m\2\2"+
		"+,\7g\2\2,-\7p\2\2-\6\3\2\2\2./\7v\2\2/\60\7{\2\2\60\61\7r\2\2\61\62\7"+
		"g\2\2\62\b\3\2\2\2\63\64\7e\2\2\64\65\7q\2\2\65\66\7p\2\2\66\67\7h\2\2"+
		"\678\7k\2\289\7f\2\29:\7g\2\2:;\7p\2\2;<\7e\2\2<=\7g\2\2=\n\3\2\2\2>?"+
		"\7e\2\2?@\7q\2\2@A\7p\2\2AB\7v\2\2BC\7g\2\2CD\7z\2\2DE\7v\2\2E\f\3\2\2"+
		"\2FG\7e\2\2GH\7n\2\2HI\7c\2\2IJ\7u\2\2JK\7u\2\2KL\7k\2\2LM\7h\2\2MN\7"+
		"k\2\2NO\7e\2\2OP\7c\2\2PQ\7v\2\2QR\7k\2\2RS\7q\2\2ST\7p\2\2T\16\3\2\2"+
		"\2UV\7u\2\2VW\7g\2\2WX\7p\2\2XY\7v\2\2YZ\7k\2\2Z[\7o\2\2[\\\7g\2\2\\]"+
		"\7p\2\2]^\7v\2\2^\20\3\2\2\2_m\3\2\2\2`a\7R\2\2ab\7G\2\2bm\7T\2\2cd\7"+
		"r\2\2de\7g\2\2em\7t\2\2fg\7N\2\2gh\7Q\2\2hm\7E\2\2ij\7n\2\2jk\7q\2\2k"+
		"m\7e\2\2l_\3\2\2\2l`\3\2\2\2lc\3\2\2\2lf\3\2\2\2li\3\2\2\2m\22\3\2\2\2"+
		"nv\3\2\2\2op\7C\2\2pq\7P\2\2qv\7F\2\2rs\7c\2\2st\7p\2\2tv\7f\2\2un\3\2"+
		"\2\2uo\3\2\2\2ur\3\2\2\2v\24\3\2\2\2w\u008e\3\2\2\2x\u008e\t\2\2\2yz\7"+
		">\2\2z\u008e\7?\2\2{|\7?\2\2|\u008e\7@\2\2}~\7?\2\2~\u008e\7?\2\2\177"+
		"\u0080\7#\2\2\u0080\u008e\7?\2\2\u0081\u0082\7u\2\2\u0082\u0083\7v\2\2"+
		"\u0083\u0084\7c\2\2\u0084\u0085\7t\2\2\u0085\u0086\7v\2\2\u0086\u0087"+
		"\7u\2\2\u0087\u0088\7y\2\2\u0088\u0089\7k\2\2\u0089\u008a\7v\2\2\u008a"+
		"\u008e\7j\2\2\u008b\u008c\7k\2\2\u008c\u008e\7u\2\2\u008dw\3\2\2\2\u008d"+
		"x\3\2\2\2\u008dy\3\2\2\2\u008d{\3\2\2\2\u008d}\3\2\2\2\u008d\177\3\2\2"+
		"\2\u008d\u0081\3\2\2\2\u008d\u008b\3\2\2\2\u008e\26\3\2\2\2\u008f\u0091"+
		"\4\62;\2\u0090\u008f\3\2\2\2\u0091\u0092\3\2\2\2\u0092\u0090\3\2\2\2\u0092"+
		"\u0093\3\2\2\2\u0093\u009a\3\2\2\2\u0094\u0096\7\60\2\2\u0095\u0097\4"+
		"\62;\2\u0096\u0095\3\2\2\2\u0097\u0098\3\2\2\2\u0098\u0096\3\2\2\2\u0098"+
		"\u0099\3\2\2\2\u0099\u009b\3\2\2\2\u009a\u0094\3\2\2\2\u009a\u009b\3\2"+
		"\2\2\u009b\30\3\2\2\2\u009c\u00a2\7$\2\2\u009d\u00a1\n\3\2\2\u009e\u009f"+
		"\7^\2\2\u009f\u00a1\t\4\2\2\u00a0\u009d\3\2\2\2\u00a0\u009e\3\2\2\2\u00a1"+
		"\u00a4\3\2\2\2\u00a2\u00a0\3\2\2\2\u00a2\u00a3\3\2\2\2\u00a3\u00a5\3\2"+
		"\2\2\u00a4\u00a2\3\2\2\2\u00a5\u00a6\7$\2\2\u00a6\32\3\2\2\2\u00a7\u00a9"+
		"\t\5\2\2\u00a8\u00a7\3\2\2\2\u00a9\u00aa\3\2\2\2\u00aa\u00a8\3\2\2\2\u00aa"+
		"\u00ab\3\2\2\2\u00ab\u00ac\3\2\2\2\u00ac\u00ad\b\16\2\2\u00ad\34\3\2\2"+
		"\2\f\2lu\u008d\u0092\u0098\u009a\u00a0\u00a2\u00aa\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}