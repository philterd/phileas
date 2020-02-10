// Generated from FilterCondition.g4 by ANTLR 4.7.2
package com.mtnfog.phileas.model.conditions.parser;
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
	static { RuntimeMetaData.checkVersion("4.7.2", RuntimeMetaData.VERSION); }

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
			null, "'population'", "'token'", "'\"'", "'type'", "'confidence'", "'context'"
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\16\u0093\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\4\3\4\3\5\3\5\3\5\3\5\3\5\3\6\3\6\3\6\3\6\3\6"+
		"\3\6\3\6\3\6\3\6\3\6\3\6\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\b\3\b\3\b\3"+
		"\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\5\bT\n\b\3\t\3\t\3\t\3\t\3\t\3"+
		"\t\3\t\5\t]\n\t\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3"+
		"\n\3\n\3\n\3\n\3\n\3\n\3\n\5\ns\n\n\3\13\6\13v\n\13\r\13\16\13w\3\13\3"+
		"\13\6\13|\n\13\r\13\16\13}\5\13\u0080\n\13\3\f\3\f\3\f\3\f\7\f\u0086\n"+
		"\f\f\f\16\f\u0089\13\f\3\f\3\f\3\r\6\r\u008e\n\r\r\r\16\r\u008f\3\r\3"+
		"\r\2\2\16\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\3\2"+
		"\6\4\2>>@@\6\2\f\f\17\17$$^^\4\2$$^^\5\2\13\f\17\17\"\"\2\u00a4\2\3\3"+
		"\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2"+
		"\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3"+
		"\2\2\2\3\33\3\2\2\2\5&\3\2\2\2\7,\3\2\2\2\t.\3\2\2\2\13\63\3\2\2\2\r>"+
		"\3\2\2\2\17S\3\2\2\2\21\\\3\2\2\2\23r\3\2\2\2\25u\3\2\2\2\27\u0081\3\2"+
		"\2\2\31\u008d\3\2\2\2\33\34\7r\2\2\34\35\7q\2\2\35\36\7r\2\2\36\37\7w"+
		"\2\2\37 \7n\2\2 !\7c\2\2!\"\7v\2\2\"#\7k\2\2#$\7q\2\2$%\7p\2\2%\4\3\2"+
		"\2\2&\'\7v\2\2\'(\7q\2\2()\7m\2\2)*\7g\2\2*+\7p\2\2+\6\3\2\2\2,-\7$\2"+
		"\2-\b\3\2\2\2./\7v\2\2/\60\7{\2\2\60\61\7r\2\2\61\62\7g\2\2\62\n\3\2\2"+
		"\2\63\64\7e\2\2\64\65\7q\2\2\65\66\7p\2\2\66\67\7h\2\2\678\7k\2\289\7"+
		"f\2\29:\7g\2\2:;\7p\2\2;<\7e\2\2<=\7g\2\2=\f\3\2\2\2>?\7e\2\2?@\7q\2\2"+
		"@A\7p\2\2AB\7v\2\2BC\7g\2\2CD\7z\2\2DE\7v\2\2E\16\3\2\2\2FT\3\2\2\2GH"+
		"\7R\2\2HI\7G\2\2IT\7T\2\2JK\7r\2\2KL\7g\2\2LT\7t\2\2MN\7N\2\2NO\7Q\2\2"+
		"OT\7E\2\2PQ\7n\2\2QR\7q\2\2RT\7e\2\2SF\3\2\2\2SG\3\2\2\2SJ\3\2\2\2SM\3"+
		"\2\2\2SP\3\2\2\2T\20\3\2\2\2U]\3\2\2\2VW\7C\2\2WX\7P\2\2X]\7F\2\2YZ\7"+
		"c\2\2Z[\7p\2\2[]\7f\2\2\\U\3\2\2\2\\V\3\2\2\2\\Y\3\2\2\2]\22\3\2\2\2^"+
		"s\3\2\2\2_s\t\2\2\2`a\7>\2\2as\7?\2\2bc\7?\2\2cs\7@\2\2de\7?\2\2es\7?"+
		"\2\2fg\7#\2\2gs\7?\2\2hi\7u\2\2ij\7v\2\2jk\7c\2\2kl\7t\2\2lm\7v\2\2mn"+
		"\7u\2\2no\7y\2\2op\7k\2\2pq\7v\2\2qs\7j\2\2r^\3\2\2\2r_\3\2\2\2r`\3\2"+
		"\2\2rb\3\2\2\2rd\3\2\2\2rf\3\2\2\2rh\3\2\2\2s\24\3\2\2\2tv\4\62;\2ut\3"+
		"\2\2\2vw\3\2\2\2wu\3\2\2\2wx\3\2\2\2x\177\3\2\2\2y{\7\60\2\2z|\4\62;\2"+
		"{z\3\2\2\2|}\3\2\2\2}{\3\2\2\2}~\3\2\2\2~\u0080\3\2\2\2\177y\3\2\2\2\177"+
		"\u0080\3\2\2\2\u0080\26\3\2\2\2\u0081\u0087\7$\2\2\u0082\u0086\n\3\2\2"+
		"\u0083\u0084\7^\2\2\u0084\u0086\t\4\2\2\u0085\u0082\3\2\2\2\u0085\u0083"+
		"\3\2\2\2\u0086\u0089\3\2\2\2\u0087\u0085\3\2\2\2\u0087\u0088\3\2\2\2\u0088"+
		"\u008a\3\2\2\2\u0089\u0087\3\2\2\2\u008a\u008b\7$\2\2\u008b\30\3\2\2\2"+
		"\u008c\u008e\t\5\2\2\u008d\u008c\3\2\2\2\u008e\u008f\3\2\2\2\u008f\u008d"+
		"\3\2\2\2\u008f\u0090\3\2\2\2\u0090\u0091\3\2\2\2\u0091\u0092\b\r\2\2\u0092"+
		"\32\3\2\2\2\f\2S\\rw}\177\u0085\u0087\u008f\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}