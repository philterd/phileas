package com.mtnfog.phileas.model.objects;

import java.util.LinkedList;
import java.util.List;

public class DocumentAnalysis {

    private List<DocumentType> documentTypes;

    public DocumentAnalysis() {

        this.documentTypes = new LinkedList<>();

    }

    public List<DocumentType> getDocumentTypes() {
        return documentTypes;
    }

}
