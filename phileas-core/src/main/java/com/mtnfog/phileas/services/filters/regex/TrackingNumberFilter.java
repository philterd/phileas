package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Analyzer;
import com.mtnfog.phileas.model.objects.FilterPattern;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class TrackingNumberFilter extends RegexFilter {

    public TrackingNumberFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, AlertService alertService, Set<String> ignored, Crypto crypto, int windowSize) {
        super(FilterType.TRACKING_NUMBER, strategies, anonymizationService, alertService, ignored, crypto, windowSize);

        // https://andrewkurochkin.com/blog/code-for-recognizing-delivery-company-by-track

        // FedEx
        final Pattern fedex1 = Pattern.compile("\\b[0-9]{20}\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern fedex1FilterPattern = new FilterPattern.FilterPatternBuilder(fedex1, 0.75).withClassification("fedex").build();

        final Pattern fedex2 = Pattern.compile("\\b[0-9]{15}\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern fedex2FilterPattern = new FilterPattern.FilterPatternBuilder(fedex2, 0.75).withClassification("fedex").build();

        final Pattern fedex3 = Pattern.compile("\\b[0-9]{12}\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern fedex3FilterPattern = new FilterPattern.FilterPatternBuilder(fedex3, 0.75).withClassification("fedex").build();

        final Pattern fedex4 = Pattern.compile("\\b[0-9]{22}\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern fedex4FilterPattern = new FilterPattern.FilterPatternBuilder(fedex4, 0.75).withClassification("fedex").build();

        // UPS

        final Pattern ups1 = Pattern.compile("\\b(1Z)[0-9A-Z]{16}\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern ups1FilterPattern = new FilterPattern.FilterPatternBuilder(ups1, 0.90).withClassification("ups").build();

        final Pattern ups2 = Pattern.compile("\\b(T)+[0-9A-Z]{10}\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern ups2FilterPattern = new FilterPattern.FilterPatternBuilder(ups2, 0.90).withClassification("ups").build();

        final Pattern ups3 = Pattern.compile("\\b[0-9]{9}\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern ups3FilterPattern = new FilterPattern.FilterPatternBuilder(ups3, 0.75).withClassification("ups").build();

        final Pattern ups4 = Pattern.compile("\\b[0-9]{26}\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern ups4FilterPattern = new FilterPattern.FilterPatternBuilder(ups4, 0.75).withClassification("ups").build();

        // USPS

        final Pattern usps1 = Pattern.compile("\\b(94|93|92|94|95)[0-9]{20}\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern usps1FilterPattern = new FilterPattern.FilterPatternBuilder(usps1, 0.90).withClassification("usps").build();

        final Pattern usps2 = Pattern.compile("\\b(94|93|92|94|95)[0-9]{22}\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern usps2FilterPattern = new FilterPattern.FilterPatternBuilder(usps2, 0.90).withClassification("usps").build();

        final Pattern usps3 = Pattern.compile("\\b(70|14|23|03)[0-9]{14}\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern usps3FilterPattern = new FilterPattern.FilterPatternBuilder(usps3, 0.90).withClassification("usps").build();

        final Pattern usps4 = Pattern.compile("\\b([A-Z]{2})[0-9]{9}([A-Z]{2})\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern usps4FilterPattern = new FilterPattern.FilterPatternBuilder(usps4, 0.90).withClassification("usps").build();

        final Pattern usps5 = Pattern.compile("\\b[0-9]{34}\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern usps5FilterPattern = new FilterPattern.FilterPatternBuilder(usps5, 0.75).withClassification("usps").build();

        final Pattern usps6 = Pattern.compile("\\b[0-9]{30}\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern usps6FilterPattern = new FilterPattern.FilterPatternBuilder(usps6, 0.75).withClassification("usps").build();

        final Pattern usps7 = Pattern.compile("\\b[0-9]{28}\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern usps7FilterPattern = new FilterPattern.FilterPatternBuilder(usps7, 0.75).withClassification("usps").build();

        final Pattern usps8 = Pattern.compile("\\b[0-9]{26}\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern usps8FilterPattern = new FilterPattern.FilterPatternBuilder(usps8, 0.75).withClassification("usps").build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("tracking");
        this.contextualTerms.add("shipment");
        this.contextualTerms.add("shipping");
        this.contextualTerms.add("mailing");
        this.contextualTerms.add("sent");
        this.contextualTerms.add("delivered");

        this.analyzer = new Analyzer(contextualTerms,
                fedex1FilterPattern, fedex2FilterPattern, fedex3FilterPattern, fedex4FilterPattern,
                ups1FilterPattern, ups2FilterPattern, ups3FilterPattern, ups4FilterPattern,
                usps1FilterPattern, usps2FilterPattern, usps3FilterPattern, usps4FilterPattern, usps5FilterPattern, usps6FilterPattern, usps7FilterPattern, usps8FilterPattern);

    }

    @Override
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(filterProfile, analyzer, input, context, documentId);

        return new FilterResult(context, documentId, spans);

    }

}
