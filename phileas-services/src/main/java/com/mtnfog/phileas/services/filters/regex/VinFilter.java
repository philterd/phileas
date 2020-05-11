package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class VinFilter extends RegexFilter implements Serializable {

    private static final Pattern VIN_REGEX = Pattern.compile("\\b[A-HJ-NPR-Z0-9]{17}\\b", Pattern.CASE_INSENSITIVE);

    public VinFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, Set<String> ignored, Crypto crypto, int windowSize) {
        super(FilterType.VIN, strategies, anonymizationService, ignored, crypto, windowSize);
    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        final List<Span> spans = findSpans(filterProfile, VIN_REGEX, input, context, documentId);

        CollectionUtils.filter(spans, new Predicate() {

            @Override
            public boolean evaluate(Object object) {
                Span s = (Span) object;
                return isVinValid(input.substring(s.getCharacterStart(), s.getCharacterEnd()));
            }

        });

        return spans;

    }

    private boolean isVinValid(String vin) {

        int[] values = { 1, 2, 3, 4, 5, 6, 7, 8, 0, 1, 2, 3, 4, 5, 0, 7, 0, 9, 2, 3, 4, 5, 6, 7, 8, 9 };
        int[] weights = { 8, 7, 6, 5, 4, 3, 2, 10, 0, 9, 8, 7, 6, 5, 4, 3, 2 };

        String s = vin;
        s = s.replaceAll("-", "");
        s = s.replaceAll(" ", "");
        s = s.toUpperCase();
        if (s.length() != 17) {
            return false;
        }

        int sum = 0;
        for (int i = 0; i < 17; i++) {
            char c = s.charAt(i);
            int value;
            int weight = weights[i];

            // letter
            if (c >= 'A' && c <= 'Z') {
                value = values[c - 'A'];
                if (value == 0) {
                    return false;
                }
            }

            // number
            else if (c >= '0' && c <= '9') {
                value = c - '0';
            }

            // illegal character
            else {
                return false;
            }

            sum = sum + weight * value;

        }


        // check digit
        sum = sum % 11;
        char check = s.charAt(8);
        if (sum == 10 && check == 'X') {
            return true;
        } else if (sum == transliterate(check)) {
            return true;
        } else {
            return false;
        }

    }

    private int transliterate(char check){
        if(check == 'A' || check == 'J'){
            return 1;
        } else if(check == 'B' || check == 'K' || check == 'S'){
            return 2;
        } else if(check == 'C' || check == 'L' || check == 'T'){
            return 3;
        } else if(check == 'D' || check == 'M' || check == 'U'){
            return 4;
        } else if(check == 'E' || check == 'N' || check == 'V'){
            return 5;
        } else if(check == 'F' || check == 'W'){
            return 6;
        } else if(check == 'G' || check == 'P' || check == 'X'){
            return 7;
        } else if(check == 'H' || check == 'Y'){
            return 8;
        } else if(check == 'R' || check == 'Z'){
            return 9;
        } else if(Integer.valueOf(Character.getNumericValue(check)) != null) { //hacky but works
            return Character.getNumericValue(check);
        }
        return -1;
    }


}
