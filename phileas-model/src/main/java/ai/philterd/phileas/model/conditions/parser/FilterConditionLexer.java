/*
 *     Copyright 2023 Philerd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\16\u00a2\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3\5\3\5\3\5\3\5"+
		"\3\5\3\5\3\5\3\5\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\7\3\7\3\7\3\7\3\7\3"+
		"\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b"+
		"\3\b\3\b\3\b\3\b\3\b\5\ba\n\b\3\t\3\t\3\t\3\t\3\t\3\t\3\t\5\tj\n\t\3\n"+
		"\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3"+
		"\n\3\n\3\n\3\n\5\n\u0082\n\n\3\13\6\13\u0085\n\13\r\13\16\13\u0086\3\13"+
		"\3\13\6\13\u008b\n\13\r\13\16\13\u008c\5\13\u008f\n\13\3\f\3\f\3\f\3\f"+
		"\7\f\u0095\n\f\f\f\16\f\u0098\13\f\3\f\3\f\3\r\6\r\u009d\n\r\r\r\16\r"+
		"\u009e\3\r\3\r\2\2\16\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27"+
		"\r\31\16\3\2\6\4\2>>@@\6\2\f\f\17\17$$^^\4\2$$^^\5\2\13\f\17\17\"\"\2"+
		"\u00b4\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2"+
		"\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3"+
		"\2\2\2\2\31\3\2\2\2\3\33\3\2\2\2\5&\3\2\2\2\7,\3\2\2\2\t\61\3\2\2\2\13"+
		"<\3\2\2\2\rD\3\2\2\2\17`\3\2\2\2\21i\3\2\2\2\23\u0081\3\2\2\2\25\u0084"+
		"\3\2\2\2\27\u0090\3\2\2\2\31\u009c\3\2\2\2\33\34\7r\2\2\34\35\7q\2\2\35"+
		"\36\7r\2\2\36\37\7w\2\2\37 \7n\2\2 !\7c\2\2!\"\7v\2\2\"#\7k\2\2#$\7q\2"+
		"\2$%\7p\2\2%\4\3\2\2\2&\'\7v\2\2\'(\7q\2\2()\7m\2\2)*\7g\2\2*+\7p\2\2"+
		"+\6\3\2\2\2,-\7v\2\2-.\7{\2\2./\7r\2\2/\60\7g\2\2\60\b\3\2\2\2\61\62\7"+
		"e\2\2\62\63\7q\2\2\63\64\7p\2\2\64\65\7h\2\2\65\66\7k\2\2\66\67\7f\2\2"+
		"\678\7g\2\289\7p\2\29:\7e\2\2:;\7g\2\2;\n\3\2\2\2<=\7e\2\2=>\7q\2\2>?"+
		"\7p\2\2?@\7v\2\2@A\7g\2\2AB\7z\2\2BC\7v\2\2C\f\3\2\2\2DE\7e\2\2EF\7n\2"+
		"\2FG\7c\2\2GH\7u\2\2HI\7u\2\2IJ\7k\2\2JK\7h\2\2KL\7k\2\2LM\7e\2\2MN\7"+
		"c\2\2NO\7v\2\2OP\7k\2\2PQ\7q\2\2QR\7p\2\2R\16\3\2\2\2Sa\3\2\2\2TU\7R\2"+
		"\2UV\7G\2\2Va\7T\2\2WX\7r\2\2XY\7g\2\2Ya\7t\2\2Z[\7N\2\2[\\\7Q\2\2\\a"+
		"\7E\2\2]^\7n\2\2^_\7q\2\2_a\7e\2\2`S\3\2\2\2`T\3\2\2\2`W\3\2\2\2`Z\3\2"+
		"\2\2`]\3\2\2\2a\20\3\2\2\2bj\3\2\2\2cd\7C\2\2de\7P\2\2ej\7F\2\2fg\7c\2"+
		"\2gh\7p\2\2hj\7f\2\2ib\3\2\2\2ic\3\2\2\2if\3\2\2\2j\22\3\2\2\2k\u0082"+
		"\3\2\2\2l\u0082\t\2\2\2mn\7>\2\2n\u0082\7?\2\2op\7?\2\2p\u0082\7@\2\2"+
		"qr\7?\2\2r\u0082\7?\2\2st\7#\2\2t\u0082\7?\2\2uv\7u\2\2vw\7v\2\2wx\7c"+
		"\2\2xy\7t\2\2yz\7v\2\2z{\7u\2\2{|\7y\2\2|}\7k\2\2}~\7v\2\2~\u0082\7j\2"+
		"\2\177\u0080\7k\2\2\u0080\u0082\7u\2\2\u0081k\3\2\2\2\u0081l\3\2\2\2\u0081"+
		"m\3\2\2\2\u0081o\3\2\2\2\u0081q\3\2\2\2\u0081s\3\2\2\2\u0081u\3\2\2\2"+
		"\u0081\177\3\2\2\2\u0082\24\3\2\2\2\u0083\u0085\4\62;\2\u0084\u0083\3"+
		"\2\2\2\u0085\u0086\3\2\2\2\u0086\u0084\3\2\2\2\u0086\u0087\3\2\2\2\u0087"+
		"\u008e\3\2\2\2\u0088\u008a\7\60\2\2\u0089\u008b\4\62;\2\u008a\u0089\3"+
		"\2\2\2\u008b\u008c\3\2\2\2\u008c\u008a\3\2\2\2\u008c\u008d\3\2\2\2\u008d"+
		"\u008f\3\2\2\2\u008e\u0088\3\2\2\2\u008e\u008f\3\2\2\2\u008f\26\3\2\2"+
		"\2\u0090\u0096\7$\2\2\u0091\u0095\n\3\2\2\u0092\u0093\7^\2\2\u0093\u0095"+
		"\t\4\2\2\u0094\u0091\3\2\2\2\u0094\u0092\3\2\2\2\u0095\u0098\3\2\2\2\u0096"+
		"\u0094\3\2\2\2\u0096\u0097\3\2\2\2\u0097\u0099\3\2\2\2\u0098\u0096\3\2"+
		"\2\2\u0099\u009a\7$\2\2\u009a\30\3\2\2\2\u009b\u009d\t\5\2\2\u009c\u009b"+
		"\3\2\2\2\u009d\u009e\3\2\2\2\u009e\u009c\3\2\2\2\u009e\u009f\3\2\2\2\u009f"+
		"\u00a0\3\2\2\2\u00a0\u00a1\b\r\2\2\u00a1\32\3\2\2\2\f\2`i\u0081\u0086"+
		"\u008c\u008e\u0094\u0096\u009e\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}