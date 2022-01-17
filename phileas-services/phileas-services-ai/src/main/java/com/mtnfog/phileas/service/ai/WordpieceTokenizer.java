package com.mtnfog.phileas.service.ai;

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import opennlp.tools.util.Span;
import org.apache.commons.lang3.NotImplementedException;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class WordpieceTokenizer implements Tokenizer {

    private static final String CLS = "[CLS]";
    private static final String SEP = "[SEP]";
    private static final String UNKNOWN = "[UNK]";

    private Map<String, Integer> vocabulary;
    private int maxTokenLength = 50;

    public WordpieceTokenizer(Map<String, Integer> vocabulary) {
        this.vocabulary = vocabulary;
    }

    public WordpieceTokenizer(Map<String, Integer> vocabulary, int maxTokenLength) {
        this.vocabulary = vocabulary;
        this.maxTokenLength = maxTokenLength;
    }

    // https://www.tensorflow.org/text/guide/subwords_tokenizer#applying_wordpiece
    // https://cran.r-project.org/web/packages/wordpiece/vignettes/basic_usage.html

    @Override
    public Span[] tokenizePos(final String text) {
        throw new NotImplementedException();
    }

    @Override
    public String[] tokenize(final String text) {

        final List<String> tokens = new LinkedList<>();
        tokens.add(CLS);

        // Put spaces around punctuation.
        final String spacedPunctuation = text.replaceAll("\\p{Punct}+", " $0 ");

        // Split based on whitespace.
        final String[] split = WhitespaceTokenizer.INSTANCE.tokenize(spacedPunctuation);

        // For each resulting word, if the word is found in the WordPiece vocabulary, keep it as-is.
        // If not, starting from the beginning, pull off the biggest piece that is in the vocabulary,
        // and prefix "##" to the remaining piece. Repeat until the entire word is represented by
        // pieces from the vocabulary, if possible.
        for (final String token : split) {

            final char[] characters = token.toCharArray();

            if (characters.length <= maxTokenLength) {

                // To start, the substring is the whole token.
                int start = 0;
                int end;

                // Look at the token from the start.
                while (start < characters.length) {

                    end = characters.length;
                    boolean found = false;

                    // Look at the token from the end.
                    while (start < end) {

                        // The substring is the part of the token we are looking at now.
                        String substring = String.valueOf(characters, start, end - start);

                        // This is a substring so prefix it with ##.
                        if(start > 0) {
                            substring = "##" + substring;
                        }

                        // See if the substring is in the vocabulary.
                        if (vocabulary.containsKey(substring)) {

                            // It is in the vocabulary so add it to the list of tokens.
                            tokens.add(substring);

                            // Next time we can pick up where we left off.
                            start = end;
                            found = true;

                            break;

                        }

                        // Subtract 1 from the end to find the next longest piece in the vocabulary.
                        end--;

                    }

                    // If the word can't be represented by vocabulary pieces replace it with a specified "unknown" token.
                    if (!found) {
                        tokens.add(UNKNOWN);
                        break;
                    }

                    // Start the next characters where we just left off.
                    start = end;

                }

            } else {

                // If the token's length is greater than the max length just add [UNK] instead.
                tokens.add(UNKNOWN);

            }

        }

        tokens.add(SEP);

        return tokens.toArray(new String[0]);

    }

}
