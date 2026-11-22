package spike;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import com.google.gson.Gson;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/**
 * redact &lt;policy&gt; &lt;in&gt; &lt;out&gt;   redact a .docx/.xlsx; prints the span count
 * detect &lt;policy&gt; &lt;file&gt;      prints span count and texts
 * paragraphs &lt;file&gt;           ReadParagraphs as JSON (docx)
 * roundtrip &lt;in&gt; &lt;out&gt;        open and save unchanged with POI's XWPFDocument / XSSFWorkbook
 */
public final class Main {

    public static void main(final String[] args) throws Exception {
        switch (args[0]) {
            case "redact" -> {
                final Office.Filter filter = filter(args[1]);
                final byte[] in = Files.readAllBytes(Path.of(args[2]));
                final long t0 = System.nanoTime();
                final WordRedactor.Result r = docx(args[2]) ? WordRedactor.redact(in, filter) : XlsxRedactor.redact(in, filter);
                final long ms = (System.nanoTime() - t0) / 1_000_000;
                Files.write(Path.of(args[3]), r.document());
                System.out.println("RESULT " + r.spans().size() + " " + ms + "ms");
            }
            case "detect" -> {
                final Office.Filter filter = filter(args[1]);
                final byte[] in = Files.readAllBytes(Path.of(args[2]));
                final List<Office.OfficeSpan> spans = docx(args[2]) ? WordRedactor.detect(in, filter) : XlsxRedactor.detect(in, filter);
                System.out.println(new Gson().toJson(spans.stream().map(s -> List.of(s.paragraphIndex(), s.text())).toList()));
            }
            case "paragraphs" -> System.out.println("RESULT " + new Gson().toJson(WordRedactor.readParagraphs(Files.readAllBytes(Path.of(args[1])))));
            case "roundtrip" -> {
                final byte[] in = Files.readAllBytes(Path.of(args[1]));
                try (FileOutputStream out = new FileOutputStream(args[2])) {
                    if (docx(args[1])) {
                        try (XWPFDocument d = new XWPFDocument(new ByteArrayInputStream(in))) {
                            d.write(out);
                        }
                    } else {
                        try (XSSFWorkbook w = new XSSFWorkbook(new ByteArrayInputStream(in))) {
                            w.write(out);
                        }
                    }
                }
            }
            default -> throw new IllegalArgumentException(args[0]);
        }
    }

    private static boolean docx(final String path) {
        return path.toLowerCase().endsWith(".docx");
    }

    private static Office.Filter filter(final String policyPath) throws Exception {
        final Policy policy = new Gson().fromJson(Files.readString(Path.of(policyPath)), Policy.class);
        final PlainTextFilterService service = new PlainTextFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null);
        return text -> {
            try {
                return service.filter(policy, "spike", text);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
