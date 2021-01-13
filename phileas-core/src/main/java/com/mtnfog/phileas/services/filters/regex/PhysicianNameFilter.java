package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Analyzer;
import com.mtnfog.phileas.model.objects.FilterPattern;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.IgnoredPattern;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.AnonymizationService;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PhysicianNameFilter extends RegexFilter {

    private final List<String> preNominals;

    private final List<String> postNominals;
    private final List<String> postNominalsLowerCase;
    private final List<String> postNominalsWithoutPunctuaction;

    public PhysicianNameFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, AlertService alertService, Set<String> ignored, Set<String> ignoredFiles, List<IgnoredPattern> ignoredPatterns, Crypto crypto, int windowSize) {
        super(FilterType.PHYSICIAN_NAME, strategies, anonymizationService, alertService, ignored, ignoredFiles, ignoredPatterns, crypto, windowSize);

        // \b[A-Z][A-Za-z'\s+]+,[A-Z][A-Za-z'\s+]+,(MD|PhD)\b

        // TODO: Set the pattern here.
        final Pattern bitcoinPattern = Pattern.compile("\\b[13][a-km-zA-HJ-NP-Z1-9]{25,34}\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern bitcoin1 = new FilterPattern.FilterPatternBuilder(bitcoinPattern, 0.90).build();

        // TODO: Set the contextual terms.
        this.contextualTerms = new HashSet<>();

        this.preNominals = new LinkedList<>();
        this.preNominals.add("Dr.");
        this.preNominals.add("Doctor");

        this.postNominals = new LinkedList<>();
        populatePostNominals();

        this.postNominalsLowerCase = postNominals.stream().map(String::toLowerCase).collect(Collectors.toList());
        this.postNominalsWithoutPunctuaction = postNominalsLowerCase.stream().map(x -> x.replaceAll("\\.", "")).collect(Collectors.toList());

    }

    @Override
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, int piece, String input) throws Exception {

        // \b([A-Z][A-Za-z'\s+]+)(,|\s)?([A-Z][A-Za-z'\s+]+(,|\s))?([A-Z][A-Za-z'\s+]+(,|\s)?(MD|PhD))\b

        final List<Span> spans = new LinkedList<>();

        // Do prenominals  ----------------------------------------------------------

        for(final String preNominal : preNominals) {

            final Pattern pattern = Pattern.compile("\\b" + preNominal + "\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(pattern, 0.90).build();
            this.analyzer = new Analyzer(contextualTerms, filterPattern);

            final List<Span> preNominalSpans = findSpans(filterProfile, analyzer, input, context, documentId);

            if(preNominalSpans.size() > 0) {
                System.out.println("preNominalSpans: " + preNominalSpans.size());
            }

            for(final Span span : preNominalSpans) {

                final int start = span.getCharacterStart();

                final String[] tokens = input.split(" ");
                final int count = tokens.length;

                final String text;

                if(tokens.length >= 3) {
                    text = StringUtils.joinWith(" ", tokens[0], tokens[1], tokens[2]);
                } else if(tokens.length >= 2) {
                    text = StringUtils.joinWith(" ", tokens[0], tokens[1]);
                } else {
                    // Not a name.
                    text = StringUtils.joinWith(" ", tokens[0], tokens[1]);
                }

                // TODO: Update the span text.
                span.setText(text);
                span.setCharacterStart(span.get);

            }

            spans.addAll(preNominalSpans);

        }

        // Do postnominals ----------------------------------------------------------

        for(final String postNominal : postNominals) {

            final Pattern pattern = Pattern.compile("\\b" + postNominal + "\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(pattern, 0.90).build();
            this.analyzer = new Analyzer(contextualTerms, filterPattern);

            final List<Span> postNominalSpans = findSpans(filterProfile, analyzer, input, context, documentId);

            if(postNominalSpans.size() > 0) {
                System.out.println("postNominalSpans: " + postNominalSpans.size());
            }

            for(final Span span : postNominalSpans) {

                final int start = span.getCharacterStart();

                final String[] tokens = input.split(" ");
                final int count = tokens.length;

                if(tokens.length >= 4) {
                    System.out.println(StringUtils.joinWith(" ", tokens[count - 5], tokens[count - 4], tokens[count - 3], tokens[count - 2], tokens[count - 1]));
                } else if(tokens.length >= 3) {
                    System.out.println(StringUtils.joinWith(" ", tokens[count - 4], tokens[count - 3], tokens[count - 2], tokens[count - 1]));
                } else if(tokens.length >= 2) {
                    System.out.println(StringUtils.joinWith(" ", tokens[count - 3], tokens[count - 2], tokens[count - 1]));
                }

            }

            // TODO: Remove stop words.

            spans.addAll(postNominalSpans);

        }

        return new FilterResult(context, documentId, spans);

    }

    private void populatePostNominals() {

        this.postNominals.clear();
        this.postNominals.add("AuD");
        this.postNominals.add("DC");
        this.postNominals.add("DDS");
        this.postNominals.add("DMD");
        this.postNominals.add("DO");
        this.postNominals.add("OD");
        this.postNominals.add("DPM");
        this.postNominals.add("DPT");
        this.postNominals.add("DScPT");
        this.postNominals.add("DSN");
        this.postNominals.add("DVM");
        this.postNominals.add("ENT");
        this.postNominals.add("GP");
        this.postNominals.add("GYN");
        this.postNominals.add("MD");
        this.postNominals.add("MS");
        this.postNominals.add("OB/GYN");
        this.postNominals.add("PharmD");
        this.postNominals.add("FAAEM");
        this.postNominals.add("FAAFP");
        this.postNominals.add("FACS");
        this.postNominals.add("FFR");
        this.postNominals.add("FRCPSC");
        this.postNominals.add("MRCOG");
        this.postNominals.add("MRCS");
        this.postNominals.add("DD");
        this.postNominals.add("DEd");
        this.postNominals.add("EdD");
        this.postNominals.add("DPA");
        this.postNominals.add("DPH");
        this.postNominals.add("DPhil");
        this.postNominals.add("PhD");
        this.postNominals.add("FFPHM");
        this.postNominals.add("JD");
        this.postNominals.add("PhD");
        this.postNominals.add("PSYCH");
        this.postNominals.add("ScD");
        this.postNominals.add("SScD");
        this.postNominals.add("ThD");
        this.postNominals.add("D.D.S.");
        this.postNominals.add("D.M.D.");
        this.postNominals.add("D.O.");
        this.postNominals.add("D.P.M.");
        this.postNominals.add("F.A.A.D.");
        this.postNominals.add("F.A.A.E.M.");
        this.postNominals.add("F.A.A.F.P.");
        this.postNominals.add("F.A.C.C.");
        this.postNominals.add("F.A.C.E.");
        this.postNominals.add("F.A.C.E.P.");
        this.postNominals.add("F.A.C.G.");
        this.postNominals.add("F.A.C.F.A.S.");
        this.postNominals.add("F.A.C.O.G.");
        this.postNominals.add("F.A.C.O.S.");
        this.postNominals.add("F.A.C.P.");
        this.postNominals.add("F.A.C.C.P.");
        this.postNominals.add("F.A.C.S.");
        this.postNominals.add("F.A.S.P.S.");
        this.postNominals.add("F.H.M.");
        this.postNominals.add("F.I.C.S.");
        this.postNominals.add("F.S.C.A.I.");
        this.postNominals.add("F.S.T.S.");
        this.postNominals.add("M.B.A.");
        this.postNominals.add("M.D.");
        this.postNominals.add("M.P.H.");
        this.postNominals.add("N.P.");
        this.postNominals.add("O.D.");
        this.postNominals.add("P.A.");
        this.postNominals.add("Ph.D.");
        this.postNominals.add("Psy.D.");
        this.postNominals.add("ABPP");
        this.postNominals.add("BLS-I");
        this.postNominals.add("CGC");
        this.postNominals.add("CNA");
        this.postNominals.add("CHES");
        this.postNominals.add("CPG");
        this.postNominals.add("MCHES");
        this.postNominals.add("MPH");
        this.postNominals.add("MHI");
        this.postNominals.add("CDAL");
        this.postNominals.add("CHSE");
        this.postNominals.add("CHSE-A");
        this.postNominals.add("CHSOS");
        this.postNominals.add("CHPA");
        this.postNominals.add("CHPE");
        this.postNominals.add("CHPSE");
        this.postNominals.add("CHP");
        this.postNominals.add("CIC");
        this.postNominals.add("CHPLN");
        this.postNominals.add("CHSE");
        this.postNominals.add("CHSS");
        this.postNominals.add("CST");
        this.postNominals.add("CTBS");
        this.postNominals.add("CVA[30]");
        this.postNominals.add("CWOCN");
        this.postNominals.add("CWCN");
        this.postNominals.add("COCN");
        this.postNominals.add("CCCN");
        this.postNominals.add("CWON");
        this.postNominals.add("DVM");
        this.postNominals.add("DACVIM");
        this.postNominals.add("DC");
        this.postNominals.add("D.D.S.");
        this.postNominals.add("DDS");
        this.postNominals.add("Psy.D.");
        this.postNominals.add("PsyD");
        this.postNominals.add("D.M.D.");
        this.postNominals.add("DMD");
        this.postNominals.add("M.D.");
        this.postNominals.add("MD");
        this.postNominals.add("DNP");
        this.postNominals.add("OTD");
        this.postNominals.add("OD");
        this.postNominals.add("D.O.");
        this.postNominals.add("DO");
        this.postNominals.add("DPT");
        this.postNominals.add("D.P.M.");
        this.postNominals.add("EMD");
        this.postNominals.add("NBC-HIS");
        this.postNominals.add("NREMR");
        this.postNominals.add("EMT-B");
        this.postNominals.add("NREMT");
        this.postNominals.add("EMT-I/85");
        this.postNominals.add("EMT-I/99");
        this.postNominals.add("NRAEMT");
        this.postNominals.add("EMT-P");
        this.postNominals.add("NRP");
        this.postNominals.add("CCP-C");
        this.postNominals.add("FP-C");
        this.postNominals.add("CP-C");
        this.postNominals.add("MICP");
        this.postNominals.add("LP");
        this.postNominals.add("LPN LVN");
        this.postNominals.add("RN");
        this.postNominals.add("APN APRN");
        this.postNominals.add("CMT");
        this.postNominals.add("LMT");
        this.postNominals.add("LCMT");
        this.postNominals.add("RM");
        this.postNominals.add("LM");
        this.postNominals.add("CM");
        this.postNominals.add("CPM");
        this.postNominals.add("CNM");
        this.postNominals.add("CRNA");
        this.postNominals.add("NP");
        this.postNominals.add("MPA");
        this.postNominals.add("MT-BC");
        this.postNominals.add("OT");
        this.postNominals.add("COTA");
        this.postNominals.add("PA-C");
        this.postNominals.add("RVT");
        this.postNominals.add("PT");
        this.postNominals.add("PTA");
        this.postNominals.add("CPH");
        this.postNominals.add("MLS");
        this.postNominals.add("MT");
        this.postNominals.add("MLT");
        this.postNominals.add("LPC");
        this.postNominals.add("LCPC");
        this.postNominals.add("LAC");
        this.postNominals.add("LMSW");
        this.postNominals.add("LCSW");
        this.postNominals.add("TEM");
        this.postNominals.add("LVT");
        this.postNominals.add("QCSW");
        this.postNominals.add("CTBS");
        this.postNominals.add("CHt");
        this.postNominals.add("CHA");
        this.postNominals.add("CAHA");
        this.postNominals.add("NBCCH");
        this.postNominals.add("NBCCH-PS");
        this.postNominals.add("NBCDCH");
        this.postNominals.add("NBCDCH-PS");
        this.postNominals.add("NBCFCH");
        this.postNominals.add("NBCFCH-PS");
        this.postNominals.add("L.Ac.");
        this.postNominals.add("Dipl.Ac.");
        this.postNominals.add("Dipl.O.M.");
        this.postNominals.add("DABFM");
        this.postNominals.add("DABVLM");
        this.postNominals.add("FAAFP");
        this.postNominals.add("FASPEN");
        this.postNominals.add("AGSF");
        this.postNominals.add("FAAN");
        this.postNominals.add("FASHP");
        this.postNominals.add("FACEP");
        this.postNominals.add("FAsMA");
        this.postNominals.add("AFAsMA");
        this.postNominals.add("CAsP");
        this.postNominals.add("AME");
        this.postNominals.add("SAME");
        this.postNominals.add("FAAP");
        this.postNominals.add("FACC");
        this.postNominals.add("FACD");
        this.postNominals.add("FACE");
        this.postNominals.add("FACP");
        this.postNominals.add("FACPh");
        this.postNominals.add("FACS");
        this.postNominals.add("FACOFP");
        this.postNominals.add("FACOG");
        this.postNominals.add("FHAMES");
        this.postNominals.add("PharmD");
        this.postNominals.add("CPhT");
        this.postNominals.add("R.Ph.");
        this.postNominals.add("RPh");
        this.postNominals.add("RRT");
        this.postNominals.add("RRT-NPS");
        this.postNominals.add("RRT-SDS");
        this.postNominals.add("RRT-ACCS");
        this.postNominals.add("RP");
        this.postNominals.add("RCP");
        this.postNominals.add("CRTT");
        this.postNominals.add("CRT");
        this.postNominals.add("CPT");
        this.postNominals.add("CPT");
        this.postNominals.add("ATC");
        this.postNominals.add("CMA");
        this.postNominals.add("NDTR");
        this.postNominals.add("DTR");
        this.postNominals.add("RDH");
        this.postNominals.add("RD");
        this.postNominals.add("RDN");
        this.postNominals.add("RDMS");
        this.postNominals.add("RVT");
        this.postNominals.add("RDCS");
        this.postNominals.add("RMSKS");

    }

}