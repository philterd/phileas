package io.philterd.phileas.services.split;

import io.philterd.phileas.model.services.SplitService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class NewLineSplitService extends AbstractSplitService implements SplitService {

    private static final Logger LOGGER = LogManager.getLogger(NewLineSplitService.class);

    private static final String SEPARATOR = System.lineSeparator();

    @Override
    public List<String> split(final String input) {

      /*  final List<String> splits = new LinkedList<>();

        final List<Integer> indexes = new LinkedList<>();

        int index = input.indexOf(SEPARATOR);
        while (index >= 0) {
            indexes.add(index);
            LOGGER.info("Using index: {}", index);
            index = input.indexOf(SEPARATOR, index + 1);
        }

        int lastSplitLocation = 0;
        int splitLocation = 0;
        int counter = 1;

        for(final int i : indexes) {

            if(i >= suggestedSplitSize * counter) {
                splitLocation = indexes.get(counter - 1);
                LOGGER.info("i = {}, Location = {}", i, suggestedSplitSize * counter);
                LOGGER.info("Last split location = {}, splitLocation = {}", lastSplitLocation, splitLocation);
                final String s = input.substring(lastSplitLocation, splitLocation).trim();
                splits.add(s);
                LOGGER.info("\tSplit: {}", s);
                lastSplitLocation = splitLocation;
                counter++;
            }

        }

        return splits;


        // Simply splits the input based on a number of new line operators.
        // See https://stackoverflow.com/a/31060125/1428388
        // return Arrays.asList(input.split("\\R+", -1));
*/
        // This method is faster than the \R regex.
        // https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html#lines()
        // This method provides better performance than split("\R") by supplying elements lazily and by faster search of new line terminators.
        final List<String> splits = Arrays.asList(input.lines().toArray(String[]::new));

        final List<String> trimmedLines = splits.stream().map(String :: trim).collect(Collectors.toList());

        LOGGER.info("Split large input exceeding threshold into {} splits using new line split method.", splits.size());

        return clean(splits);

    }

    @Override
    public String getSeparator() {
        return SEPARATOR;
    }

    /*private String split(final String input, int lastSplitLocation) {

        // Starting at position suggestedSplitSize, go backward until a new line is found.
        // Then, starting at position suggestedSplitSize, go forward until a new line is found.
        // Use the split that is closest to the position of suggestedSplitSize.

        int preSplit = 0;
        int postSplit = 0;

        for(int i = suggestedSplitSize; i >= lastSplitLocation; i--) {

            if(StringUtils.equals(Character.toString(input.charAt(i)), System.lineSeparator())) {
                preSplit = i;
                break;
            }

        }

        for(int i = suggestedSplitSize; i <= input.length(); i++) {

            if(StringUtils.equals(Character.toString(input.charAt(i)), System.lineSeparator())) {
                postSplit = i;
                break;
            }

        }

        int splitLocation;

        if(suggestedSplitSize - preSplit <= postSplit - suggestedSplitSize) {
            splitLocation = preSplit;
        } else {
            splitLocation = postSplit;
        }

        return input.substring(lastSplitLocation, splitLocation - lastSplitLocation);

    }*/

}
