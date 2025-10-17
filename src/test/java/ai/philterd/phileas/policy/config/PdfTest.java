package ai.philterd.phileas.policy.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PdfTest {

    @Test
    public void canSetPdfRedactorOptions() {
        Pdf pdfConfig = new Pdf();

        pdfConfig.setRedactionColor("blue");
        pdfConfig.setReplacementFont("times");
        pdfConfig.setShowReplacement(true);
        pdfConfig.setScale(2.0f);
        pdfConfig.setDpi(300);
        pdfConfig.setCompressionQuality(0.5f);
        pdfConfig.setPreserveUnredactedPages(true);

        Assertions.assertEquals("blue", pdfConfig.getRedactionColor());
        Assertions.assertEquals("times", pdfConfig.getReplacementFont());
        Assertions.assertTrue(pdfConfig.getShowReplacement());
        Assertions.assertEquals(2.0f, pdfConfig.getScale());
        Assertions.assertEquals(300, pdfConfig.getDpi());
        Assertions.assertEquals(0.5f, pdfConfig.getCompressionQuality());
        Assertions.assertTrue(pdfConfig.getPreserveUnredactedPages());
    }

    @Test
    public void defaultsAreSetProperly() {
        Pdf pdfConfig = new Pdf();

        Assertions.assertEquals("black", pdfConfig.getRedactionColor());
        Assertions.assertEquals("helvetica", pdfConfig.getReplacementFont());
        Assertions.assertEquals(12, pdfConfig.getReplacementMaxFontSize());
        Assertions.assertEquals(0.25f, pdfConfig.getScale());
        Assertions.assertEquals(150, pdfConfig.getDpi());
        Assertions.assertEquals(1.0f, pdfConfig.getCompressionQuality());
        Assertions.assertFalse(pdfConfig.getPreserveUnredactedPages());
    }

}
