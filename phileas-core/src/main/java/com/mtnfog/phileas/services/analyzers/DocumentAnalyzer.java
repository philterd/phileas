package com.mtnfog.phileas.services.analyzers;

import com.mtnfog.phileas.model.objects.DocumentAnalysis;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class DocumentAnalyzer {

    public DocumentAnalysis analyze(List<String> lines) {
        return run(StringUtils.join(lines, " "));
    }

    public DocumentAnalysis analyze(String text) {
        return run(text);
    }

    private DocumentAnalysis run(String text) {

        final DocumentAnalysis documentAnalysis = new DocumentAnalysis();

        // Analyze the document.

        return documentAnalysis;

    }

}
