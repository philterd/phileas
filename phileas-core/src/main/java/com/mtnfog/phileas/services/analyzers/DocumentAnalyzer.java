package com.mtnfog.phileas.services.analyzers;

import com.mtnfog.phileas.model.objects.DocumentAnalysis;
import com.mtnfog.phileas.model.objects.DocumentType;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Locale;

public class DocumentAnalyzer {

    public DocumentAnalysis analyze(List<String> lines) {
        return run(StringUtils.join(lines, " "));
    }

    public DocumentAnalysis analyze(String text) {
        return run(text);
    }

    private DocumentAnalysis run(String text) {

        final DocumentAnalysis documentAnalysis = new DocumentAnalysis();

        final String lowerCase = text.toLowerCase();

        // B2540 - Form 2540
        if(lowerCase.contains("Form 2540".toLowerCase()) || lowerCase.contains("SUBPOENA FOR RULE 2004 EXAMINATION".toLowerCase())) {
            documentAnalysis.setDocumentType(DocumentType.SUBPOENA);
            documentAnalysis.setDocument("Form 2540");
        } else

        // B2550 - Form 2550
        if(lowerCase.contains("Form 2550".toLowerCase()) || lowerCase.contains("SUBPOENA TO APPEAR AND TESTIFY".toLowerCase())) {
            documentAnalysis.setDocumentType(DocumentType.SUBPOENA);
            documentAnalysis.setDocument("Form 2550");
        } else

        // B2560 - Form 2560
        if(lowerCase.contains("Form 2560".toLowerCase()) || lowerCase.contains("SUBPOENA TO TESTIFY AT A DEPOSITION".toLowerCase())) {
            documentAnalysis.setDocumentType(DocumentType.SUBPOENA);
            documentAnalysis.setDocument("Form 2560");
        } else

        // B2570 - Form 2570
        if(lowerCase.contains("Form 2570".toLowerCase()) || lowerCase.contains("SUBPOENA TO PRODUCE DOCUMENTS".toLowerCase())) {
            documentAnalysis.setDocumentType(DocumentType.SUBPOENA);
            documentAnalysis.setDocument("Form 2570");
        } else

        // AO 88
        if(lowerCase.contains("AO 88".toLowerCase()) || lowerCase.contains("SUBPOENA TO APPEAR AND TESTIFY AT A HEARING OR TRIAL IN A CIVIL ACTION".toLowerCase())) {
            documentAnalysis.setDocumentType(DocumentType.SUBPOENA);
            documentAnalysis.setDocument("AO 88");
        } else

        // AO 88A
        if(lowerCase.contains("AO 88A".toLowerCase()) || lowerCase.contains("SUBPOENA TO TESTIFY AT A DEPOSITION IN A CIVIL ACTION".toLowerCase())) {
            documentAnalysis.setDocumentType(DocumentType.SUBPOENA);
            documentAnalysis.setDocument("AO 88A");
        } else

        // AO 88B
        if(lowerCase.contains("AO 88B".toLowerCase()) || lowerCase.contains("SUBPOENA TO PRODUCE DOCUMENTS, INFORMATION, OR OBJECTS".toLowerCase())) {
            documentAnalysis.setDocumentType(DocumentType.SUBPOENA);
            documentAnalysis.setDocument("AO 88B");
        } else

        // AO 89
        if(lowerCase.contains("AO 89".toLowerCase()) || lowerCase.contains("SUBPOENA TO TESTIFY AT A HEARING OR TRIAL IN A CRIMINAL CASE".toLowerCase())) {
            documentAnalysis.setDocumentType(DocumentType.SUBPOENA);
            documentAnalysis.setDocument("AO 89");
        } else

        // AO 90
        if(lowerCase.contains("AO 90".toLowerCase()) || lowerCase.contains("SUBPOENA TO TESTIFY AT A DEPOSITION IN A CRIMINAL CASE".toLowerCase())) {
            documentAnalysis.setDocumentType(DocumentType.SUBPOENA);
            documentAnalysis.setDocument("AO 90");
        } else

        // AO 110
        if(lowerCase.contains("AO 110".toLowerCase()) || lowerCase.contains("SUBPOENA TO TESTIFY BEFORE A GRAND JURY".toLowerCase())) {
            documentAnalysis.setDocumentType(DocumentType.SUBPOENA);
            documentAnalysis.setDocument("AO 110");
        }

        return documentAnalysis;

    }

}