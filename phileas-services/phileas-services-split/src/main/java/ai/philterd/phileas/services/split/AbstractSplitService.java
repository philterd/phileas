package ai.philterd.phileas.services.split;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractSplitService {

    protected List<String> clean(List<String> lines) {

        final List<String> trimmedLines = lines.stream().map(String :: trim).collect(Collectors.toList());

        // Remove empty strings.
        trimmedLines.removeAll(Arrays.asList("", null));

        return trimmedLines;

    }

}
