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
package ai.philterd.phileas.services.anonymization;

import org.apache.commons.collections4.CollectionUtils;

import ai.philterd.phileas.services.context.ContextService;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class CountyAnonymizationService extends AbstractAnonymizationService {

    public CountyAnonymizationService(final ContextService contextService, final Random random, final List<String> candidates) {
        super(contextService, random, candidates);
    }

    public CountyAnonymizationService(final ContextService contextService, final Random random) {
        super(contextService, random);
    }

    public CountyAnonymizationService(final ContextService contextService) {
        super(contextService);
    }

    private static final List<String> COUNTIES = new LinkedList<>();

    static {

        COUNTIES.add("Beaver");
        COUNTIES.add("Ohio");
        COUNTIES.add("Tallahatchie");
        COUNTIES.add("Braxton");
        COUNTIES.add("Orange");
        COUNTIES.add("Lemhi");
        COUNTIES.add("Wagoner");
        COUNTIES.add("Osage");
        COUNTIES.add("Rensselaer");
        COUNTIES.add("Meeker");
        COUNTIES.add("Stark");
        COUNTIES.add("McCone");
        COUNTIES.add("Clarion");
        COUNTIES.add("Spotsylvania");
        COUNTIES.add("Accomack");
        COUNTIES.add("Dauphin");
        COUNTIES.add("Jim Hogg");
        COUNTIES.add("Prince Edward");
        COUNTIES.add("Greenville");
        COUNTIES.add("Tillman");
        COUNTIES.add("Ravalli");
        COUNTIES.add("Santa Rosa");
        COUNTIES.add("Wyandot");
        COUNTIES.add("Box Butte");
        COUNTIES.add("Milwaukee");
        COUNTIES.add("Trinity");
        COUNTIES.add("Kleberg");
        COUNTIES.add("Ritchie");
        COUNTIES.add("Rockland");
        COUNTIES.add("Miami-Dade");
        COUNTIES.add("Keya Paha");
        COUNTIES.add("McCulloch");
        COUNTIES.add("Meade");
        COUNTIES.add("Collin");
        COUNTIES.add("Utah");
        COUNTIES.add("Breathitt");
        COUNTIES.add("Allen Parish");
        COUNTIES.add("Refugio");
        COUNTIES.add("Jim Wells");
        COUNTIES.add("Torrance");
        COUNTIES.add("Lunenburg");
        COUNTIES.add("Otsego");
        COUNTIES.add("Bryan");
        COUNTIES.add("Nueces");
        COUNTIES.add("Decatur");
        COUNTIES.add("Sibley");
        COUNTIES.add("Candler");
        COUNTIES.add("Del Norte");
        COUNTIES.add("Aleutians East");
        COUNTIES.add("Humboldt");
        COUNTIES.add("Cheboygan");
        COUNTIES.add("Tom Green");
        COUNTIES.add("Hodgeman");
        COUNTIES.add("Benzie");
        COUNTIES.add("Kidder");
        COUNTIES.add("Burleigh");
        COUNTIES.add("Berrien");
        COUNTIES.add("St. Lucie");
        COUNTIES.add("Harnett");
        COUNTIES.add("Sublette");
        COUNTIES.add("Traverse");
        COUNTIES.add("Caldwell Parish");
        COUNTIES.add("Walworth");
        COUNTIES.add("Kalamazoo");
        COUNTIES.add("Hamilton");
        COUNTIES.add("Yellow Medicine");
        COUNTIES.add("Mora");
        COUNTIES.add("Sherman");
        COUNTIES.add("Bethel");
        COUNTIES.add("Charles City");
        COUNTIES.add("Daniels");
        COUNTIES.add("Washington");
        COUNTIES.add("Dearborn");
        COUNTIES.add("Solano");
        COUNTIES.add("Conejos");
        COUNTIES.add("Elk");
        COUNTIES.add("Harris");
        COUNTIES.add("Fremont");
        COUNTIES.add("Addison");
        COUNTIES.add("LaGrange");
        COUNTIES.add("Sarasota");
        COUNTIES.add("Schuyler");
        COUNTIES.add("Bacon");
        COUNTIES.add("Brookings");
        COUNTIES.add("Androscoggin");
        COUNTIES.add("Forrest");
        COUNTIES.add("Smith");
        COUNTIES.add("Milam");
        COUNTIES.add("McClain");
        COUNTIES.add("Labette");
        COUNTIES.add("Powhatan");
        COUNTIES.add("Musselshell");
        COUNTIES.add("Coosa");
        COUNTIES.add("Kootenai");
        COUNTIES.add("Parker");
        COUNTIES.add("Mitchell");
        COUNTIES.add("Niobrara");
        COUNTIES.add("Miller");
        COUNTIES.add("Bingham");
        COUNTIES.add("Borden");

    }

    @Override
    public ContextService getContextService() {
        return contextService;
    }

    @Override
    public String anonymize(final String token) {

        if(CollectionUtils.isNotEmpty(candidates)) {
            return candidates.get(random.nextInt(candidates.size()));
        }

        final int randomInt = generateInteger(0, COUNTIES.size() - 1);

        final String anonymized = COUNTIES.get(randomInt);

        // Make sure the anonymized and the token aren't the same since it's a small pool.
        if(StringUtils.equalsIgnoreCase(token, anonymized)) {
            return anonymize(token);
        }

        return anonymized;

    }

}
