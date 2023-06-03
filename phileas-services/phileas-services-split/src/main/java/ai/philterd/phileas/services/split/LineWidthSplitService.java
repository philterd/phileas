package ai.philterd.phileas.services.split;

import ai.philterd.phileas.model.services.SplitService;
import org.apache.commons.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

public class LineWidthSplitService extends AbstractSplitService implements SplitService {

    private static final Logger LOGGER = LogManager.getLogger(LineWidthSplitService.class);

    private static final String SEPARATOR = System.lineSeparator();

    final int lineWidth;

    public LineWidthSplitService(int lineWidth) {
        this.lineWidth = lineWidth;
    }

    @Override
    public List<String> split(final String input) {

        final String wrapped = WordUtils.wrap(input, lineWidth);
        final List<String> lines = Arrays.asList(wrapped.lines().toArray(String[]::new));

        return clean(lines);

    }

    @Override
    public String getSeparator() {
        return SEPARATOR;
    }

}
