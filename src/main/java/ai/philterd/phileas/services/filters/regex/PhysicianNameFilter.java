/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phileas.services.filters.regex;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.filters.rules.regex.RegexFilter;
import ai.philterd.phileas.services.Analyzer;
import ai.philterd.phileas.model.objects.FilterPattern;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Position;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.policy.Policy;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PhysicianNameFilter extends RegexFilter {

    // The percentage of letters each candidate string must contain.
    private static final double LETTER_PERCENTAGE_THRESHOLD = 0.75;

    private final List<String> preNominals;

    private final List<String> postNominals;
    private final List<String> postNominalsLowerCase;
    private final List<String> postNominalsWithoutPunctuaction;

    public PhysicianNameFilter(FilterConfiguration filterConfiguration) {
        super(FilterType.PHYSICIAN_NAME, filterConfiguration);

        // TODO: Set the contextual terms.
        this.contextualTerms = new HashSet<>();

        this.preNominals = new LinkedList<>();
        populatePreNominals();

        this.postNominals = new LinkedList<>();
        populatePostNominals();

        this.postNominalsLowerCase = postNominals.stream().map(String::toLowerCase).collect(Collectors.toList());
        this.postNominalsWithoutPunctuaction = postNominalsLowerCase.stream().map(x -> x.replaceAll("\\.", "")).collect(Collectors.toList());

    }

    @Override
    public FilterResult filter(Policy policy, String context, int piece, String input) throws Exception {

        // \b([A-Z][A-Za-z'\s+]+)(,|\s)?([A-Z][A-Za-z'\s+]+(,|\s))?([A-Z][A-Za-z'\s+]+(,|\s)?(MD|PhD))\b

        final List<Span> spans = new LinkedList<>();

        final Map<Position, String> ngrams = getNgramsUpToLength(input, 5);

        for(final String candidate : ngrams.values()) {

            if (endsWithPostNominal(candidate) || startsWithPreNominal(candidate)) {

                // The candidate has to be some percentage of ASCII letters.
                if (letterPercentage(candidate) > LETTER_PERCENTAGE_THRESHOLD) {

                    // Use this text as a literal regex pattern.
                    final Pattern candidatePattern = Pattern.compile(Pattern.quote(candidate), Pattern.CASE_INSENSITIVE);
                    final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(candidatePattern, 0.90).build();
                    this.analyzer = new Analyzer(contextualTerms, filterPattern);

                    final List<Span> patternSpans = findSpans(policy, analyzer, input, context);

                    spans.addAll(patternSpans);

                }

            }

        }

        final List<Span> droppedOverlappingSpans = Span.dropOverlappingSpans(spans);

        return new FilterResult(context, droppedOverlappingSpans);

    }

    private double letterPercentage(String text) {

        int count = 0;
        text = text.replaceAll("\\p{Punct}", "");

        for(int i = 0; i < text.length() ; i++) {

            if(Character.isAlphabetic(text.charAt(i))) {
                count++;
            }

        }

        return (double) count / (double) text.length();

    }

    private boolean startsWithPreNominal(String text) {
        return preNominals.stream().anyMatch(entry -> text.startsWith(entry + " "));
    }

    private boolean endsWithPostNominal(String text) {
        return postNominals.stream().anyMatch(entry -> text.endsWith(" " + entry) || text.endsWith("," + entry));
    }

    private void populatePreNominals() {

        this.preNominals.clear();
        this.preNominals.add("DR.");
        this.preNominals.add("DR");
        this.preNominals.add("DOCTOR");
        this.preNominals.add("Doctor");
        this.preNominals.add("Dr.");

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