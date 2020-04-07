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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\r\u008f\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\4\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3"+
		"\5\3\5\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7"+
		"\3\7\3\7\3\7\3\7\3\7\5\7P\n\7\3\b\3\b\3\b\3\b\3\b\3\b\3\b\5\bY\n\b\3\t"+
		"\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3"+
		"\t\3\t\5\to\n\t\3\n\6\nr\n\n\r\n\16\ns\3\n\3\n\6\nx\n\n\r\n\16\ny\5\n"+
		"|\n\n\3\13\3\13\3\13\3\13\7\13\u0082\n\13\f\13\16\13\u0085\13\13\3\13"+
		"\3\13\3\f\6\f\u008a\n\f\r\f\16\f\u008b\3\f\3\f\2\2\r\3\3\5\4\7\5\t\6\13"+
		"\7\r\b\17\t\21\n\23\13\25\f\27\r\3\2\6\4\2>>@@\6\2\f\f\17\17$$^^\4\2$"+
		"$^^\5\2\13\f\17\17\"\"\2\u00a0\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t"+
		"\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2"+
		"\2\2\25\3\2\2\2\2\27\3\2\2\2\3\31\3\2\2\2\5$\3\2\2\2\7*\3\2\2\2\t/\3\2"+
		"\2\2\13:\3\2\2\2\rO\3\2\2\2\17X\3\2\2\2\21n\3\2\2\2\23q\3\2\2\2\25}\3"+
		"\2\2\2\27\u0089\3\2\2\2\31\32\7r\2\2\32\33\7q\2\2\33\34\7r\2\2\34\35\7"+
		"w\2\2\35\36\7n\2\2\36\37\7c\2\2\37 \7v\2\2 !\7k\2\2!\"\7q\2\2\"#\7p\2"+
		"\2#\4\3\2\2\2$%\7v\2\2%&\7q\2\2&\'\7m\2\2\'(\7g\2\2()\7p\2\2)\6\3\2\2"+
		"\2*+\7v\2\2+,\7{\2\2,-\7r\2\2-.\7g\2\2.\b\3\2\2\2/\60\7e\2\2\60\61\7q"+
		"\2\2\61\62\7p\2\2\62\63\7h\2\2\63\64\7k\2\2\64\65\7f\2\2\65\66\7g\2\2"+
		"\66\67\7p\2\2\678\7e\2\289\7g\2\29\n\3\2\2\2:;\7e\2\2;<\7q\2\2<=\7p\2"+
		"\2=>\7v\2\2>?\7g\2\2?@\7z\2\2@A\7v\2\2A\f\3\2\2\2BP\3\2\2\2CD\7R\2\2D"+
		"E\7G\2\2EP\7T\2\2FG\7r\2\2GH\7g\2\2HP\7t\2\2IJ\7N\2\2JK\7Q\2\2KP\7E\2"+
		"\2LM\7n\2\2MN\7q\2\2NP\7e\2\2OB\3\2\2\2OC\3\2\2\2OF\3\2\2\2OI\3\2\2\2"+
		"OL\3\2\2\2P\16\3\2\2\2QY\3\2\2\2RS\7C\2\2ST\7P\2\2TY\7F\2\2UV\7c\2\2V"+
		"W\7p\2\2WY\7f\2\2XQ\3\2\2\2XR\3\2\2\2XU\3\2\2\2Y\20\3\2\2\2Zo\3\2\2\2"+
		"[o\t\2\2\2\\]\7>\2\2]o\7?\2\2^_\7?\2\2_o\7@\2\2`a\7?\2\2ao\7?\2\2bc\7"+
		"#\2\2co\7?\2\2de\7u\2\2ef\7v\2\2fg\7c\2\2gh\7t\2\2hi\7v\2\2ij\7u\2\2j"+
		"k\7y\2\2kl\7k\2\2lm\7v\2\2mo\7j\2\2nZ\3\2\2\2n[\3\2\2\2n\\\3\2\2\2n^\3"+
		"\2\2\2n`\3\2\2\2nb\3\2\2\2nd\3\2\2\2o\22\3\2\2\2pr\4\62;\2qp\3\2\2\2r"+
		"s\3\2\2\2sq\3\2\2\2st\3\2\2\2t{\3\2\2\2uw\7\60\2\2vx\4\62;\2wv\3\2\2\2"+
		"xy\3\2\2\2yw\3\2\2\2yz\3\2\2\2z|\3\2\2\2{u\3\2\2\2{|\3\2\2\2|\24\3\2\2"+
		"\2}\u0083\7$\2\2~\u0082\n\3\2\2\177\u0080\7^\2\2\u0080\u0082\t\4\2\2\u0081"+
		"~\3\2\2\2\u0081\177\3\2\2\2\u0082\u0085\3\2\2\2\u0083\u0081\3\2\2\2\u0083"+
		"\u0084\3\2\2\2\u0084\u0086\3\2\2\2\u0085\u0083\3\2\2\2\u0086\u0087\7$"+
		"\2\2\u0087\26\3\2\2\2\u0088\u008a\t\5\2\2\u0089\u0088\3\2\2\2\u008a\u008b"+
		"\3\2\2\2\u008b\u0089\3\2\2\2\u008b\u008c\3\2\2\2\u008c\u008d\3\2\2\2\u008d"+
		"\u008e\b\f\2\2\u008e\30\3\2\2\2\f\2OXnsy{\u0081\u0083\u008b\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}