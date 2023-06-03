package ai.philterd.services.pdf.model;

import ai.philterd.phileas.model.objects.Span;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

public class RedactedRectangle {

    private PDRectangle pdRectangle;
    private Span span;

    public RedactedRectangle(PDRectangle pdRectangle, Span span) {

        this.pdRectangle = pdRectangle;
        this.span = span;

    }

    public Span getSpan() {
        return span;
    }

    public PDRectangle getPdRectangle() {
        return pdRectangle;
    }

}

