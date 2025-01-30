package ai.philterd.test.phileas.model.policy.config;

import ai.philterd.phileas.model.policy.config.Pdf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PdfTest {

    @Test
    public void canSetPdfRedactorOptions() {
        Pdf pdfConfig = new Pdf();

        pdfConfig.setRedactionColor("black");
        pdfConfig.setReplacementFont("helvetica");
        pdfConfig.setShowReplacement(true);
        pdfConfig.setScale(1.0f);
        pdfConfig.setDpi(150);
        pdfConfig.setCompressionQuality(0.5f);

        Assertions.assertEquals("black", pdfConfig.getRedactionColor());
        Assertions.assertEquals("helvetica", pdfConfig.getReplacementFont());
        Assertions.assertTrue(pdfConfig.getShowReplacement());
        Assertions.assertEquals(1.0f, pdfConfig.getScale());
        Assertions.assertEquals(150, pdfConfig.getDpi());
        Assertions.assertEquals(0.5f, pdfConfig.getCompressionQuality());
    }

}
