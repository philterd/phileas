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
			null, "'population'", "'token'", "'\"'", "'type'", "'confidence'"
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\r\u0089\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\4\3\4\3\5\3\5\3\5\3\5\3\5\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3"+
		"\6\3\6\3\6\3\6\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\5\7"+
		"J\n\7\3\b\3\b\3\b\3\b\3\b\3\b\3\b\5\bS\n\b\3\t\3\t\3\t\3\t\3\t\3\t\3\t"+
		"\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\5\ti\n\t\3\n\6\n"+
		"l\n\n\r\n\16\nm\3\n\3\n\6\nr\n\n\r\n\16\ns\5\nv\n\n\3\13\3\13\3\13\3\13"+
		"\7\13|\n\13\f\13\16\13\177\13\13\3\13\3\13\3\f\6\f\u0084\n\f\r\f\16\f"+
		"\u0085\3\f\3\f\2\2\r\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27"+
		"\r\3\2\6\4\2>>@@\6\2\f\f\17\17$$^^\4\2$$^^\5\2\13\f\17\17\"\"\2\u009a"+
		"\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2"+
		"\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2"+
		"\3\31\3\2\2\2\5$\3\2\2\2\7*\3\2\2\2\t,\3\2\2\2\13\61\3\2\2\2\rI\3\2\2"+
		"\2\17R\3\2\2\2\21h\3\2\2\2\23k\3\2\2\2\25w\3\2\2\2\27\u0083\3\2\2\2\31"+
		"\32\7r\2\2\32\33\7q\2\2\33\34\7r\2\2\34\35\7w\2\2\35\36\7n\2\2\36\37\7"+
		"c\2\2\37 \7v\2\2 !\7k\2\2!\"\7q\2\2\"#\7p\2\2#\4\3\2\2\2$%\7v\2\2%&\7"+
		"q\2\2&\'\7m\2\2\'(\7g\2\2()\7p\2\2)\6\3\2\2\2*+\7$\2\2+\b\3\2\2\2,-\7"+
		"v\2\2-.\7{\2\2./\7r\2\2/\60\7g\2\2\60\n\3\2\2\2\61\62\7e\2\2\62\63\7q"+
		"\2\2\63\64\7p\2\2\64\65\7h\2\2\65\66\7k\2\2\66\67\7f\2\2\678\7g\2\289"+
		"\7p\2\29:\7e\2\2:;\7g\2\2;\f\3\2\2\2<J\3\2\2\2=>\7R\2\2>?\7G\2\2?J\7T"+
		"\2\2@A\7r\2\2AB\7g\2\2BJ\7t\2\2CD\7N\2\2DE\7Q\2\2EJ\7E\2\2FG\7n\2\2GH"+
		"\7q\2\2HJ\7e\2\2I<\3\2\2\2I=\3\2\2\2I@\3\2\2\2IC\3\2\2\2IF\3\2\2\2J\16"+
		"\3\2\2\2KS\3\2\2\2LM\7C\2\2MN\7P\2\2NS\7F\2\2OP\7c\2\2PQ\7p\2\2QS\7f\2"+
		"\2RK\3\2\2\2RL\3\2\2\2RO\3\2\2\2S\20\3\2\2\2Ti\3\2\2\2Ui\t\2\2\2VW\7>"+
		"\2\2Wi\7?\2\2XY\7?\2\2Yi\7@\2\2Z[\7?\2\2[i\7?\2\2\\]\7#\2\2]i\7?\2\2^"+
		"_\7u\2\2_`\7v\2\2`a\7c\2\2ab\7t\2\2bc\7v\2\2cd\7u\2\2de\7y\2\2ef\7k\2"+
		"\2fg\7v\2\2gi\7j\2\2hT\3\2\2\2hU\3\2\2\2hV\3\2\2\2hX\3\2\2\2hZ\3\2\2\2"+
		"h\\\3\2\2\2h^\3\2\2\2i\22\3\2\2\2jl\4\62;\2kj\3\2\2\2lm\3\2\2\2mk\3\2"+
		"\2\2mn\3\2\2\2nu\3\2\2\2oq\7\60\2\2pr\4\62;\2qp\3\2\2\2rs\3\2\2\2sq\3"+
		"\2\2\2st\3\2\2\2tv\3\2\2\2uo\3\2\2\2uv\3\2\2\2v\24\3\2\2\2w}\7$\2\2x|"+
		"\n\3\2\2yz\7^\2\2z|\t\4\2\2{x\3\2\2\2{y\3\2\2\2|\177\3\2\2\2}{\3\2\2\2"+
		"}~\3\2\2\2~\u0080\3\2\2\2\177}\3\2\2\2\u0080\u0081\7$\2\2\u0081\26\3\2"+
		"\2\2\u0082\u0084\t\5\2\2\u0083\u0082\3\2\2\2\u0084\u0085\3\2\2\2\u0085"+
		"\u0083\3\2\2\2\u0085\u0086\3\2\2\2\u0086\u0087\3\2\2\2\u0087\u0088\b\f"+
		"\2\2\u0088\30\3\2\2\2\f\2IRhmsu{}\u0085\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}