package com.mtnfog.philter.services.nlp.sentence;

import com.mtnfog.phileas.model.services.SentenceDetector;
import com.neovisionaries.i18n.LanguageCode;
import net.loomchild.segment.TextIterator;
import net.loomchild.segment.srx.SrxDocument;
import net.loomchild.segment.srx.SrxParser;
import net.loomchild.segment.srx.SrxTextIterator;
import net.loomchild.segment.srx.io.Srx2SaxParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SegmentSentenceDetector implements SentenceDetector {

    private static final Logger LOGGER = LogManager.getLogger(SegmentSentenceDetector.class);

    private static final LanguageCode languageCode = LanguageCode.en;

    private SrxDocument srxDocument;

    public SegmentSentenceDetector(String srx) {

        final InputStream inputStream = new ByteArrayInputStream(srx.getBytes(StandardCharsets.UTF_8));

        BufferedReader srxReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        Map<String, Object> parserParameters = new HashMap<>();
        parserParameters.put(Srx2SaxParser.VALIDATE_PARAMETER, true);
        SrxParser srxParser = new Srx2SaxParser(parserParameters);

        srxDocument = srxParser.parse(srxReader);

    }

    public SegmentSentenceDetector() {

        final String srx = "";
        final InputStream inputStream = new ByteArrayInputStream(srx.getBytes(StandardCharsets.UTF_8));

        BufferedReader srxReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        Map<String, Object> parserParameters = new HashMap<>();
        parserParameters.put(Srx2SaxParser.VALIDATE_PARAMETER, true);
        SrxParser srxParser = new Srx2SaxParser(parserParameters);

        srxDocument = srxParser.parse(srxReader);

    }

    @Override
    public List<String> detect(String input) {

        List<String> segments = new ArrayList<>();

        final TextIterator textIterator = new SrxTextIterator(srxDocument, languageCode.getAlpha3().toString(), input);

        while(textIterator.hasNext()) {

            segments.add(textIterator.next().trim());

        }

        return segments;

    }

}
