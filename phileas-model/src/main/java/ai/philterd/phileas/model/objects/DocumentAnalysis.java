package ai.philterd.phileas.model.objects;

public class DocumentAnalysis {

    private DocumentType documentType;
    private String document;

    public DocumentAnalysis() {

    }

    public DocumentAnalysis(DocumentType documentType, String document) {
        this.documentType = documentType;
        this.document = document;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;

    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

}
