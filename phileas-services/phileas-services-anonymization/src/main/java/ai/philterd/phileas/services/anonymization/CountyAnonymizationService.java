/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
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

import ai.philterd.phileas.model.services.AnonymizationCacheService;

import java.util.Collection;
import java.util.LinkedList;

public class CountyAnonymizationService extends AbstractAnonymizationService {

    private static final Collection<String> counties = new LinkedList<String>();

    public CountyAnonymizationService(AnonymizationCacheService anonymizationCacheService) {
        super(anonymizationCacheService);

        counties.add("Beaver");
        counties.add("Ohio");
        counties.add("Tallahatchie");
        counties.add("Braxton");
        counties.add("Orange");
        counties.add("Lemhi");
        counties.add("Wagoner");
        counties.add("Osage");
        counties.add("Rensselaer");
        counties.add("Meeker");
        counties.add("Stark");
        counties.add("McCone");
        counties.add("Clarion");
        counties.add("Spotsylvania");
        counties.add("Accomack");
        counties.add("Dauphin");
        counties.add("Jim Hogg");
        counties.add("Prince Edward");
        counties.add("Greenville");
        counties.add("Tillman");
        counties.add("Ravalli");
        counties.add("Santa Rosa");
        counties.add("Wyandot");
        counties.add("Box Butte");
        counties.add("Milwaukee");
        counties.add("Trinity");
        counties.add("Kleberg");
        counties.add("Ritchie");
        counties.add("Rockland");
        counties.add("Miami-Dade");
        counties.add("Keya Paha");
        counties.add("McCulloch");
        counties.add("Meade");
        counties.add("Collin");
        counties.add("Utah");
        counties.add("Breathitt");
        counties.add("Allen Parish");
        counties.add("Refugio");
        counties.add("Jim Wells");
        counties.add("Torrance");
        counties.add("Lunenburg");
        counties.add("Otsego");
        counties.add("Bryan");
        counties.add("Nueces");
        counties.add("Decatur");
        counties.add("Sibley");
        counties.add("Candler");
        counties.add("Del Norte");
        counties.add("Aleutians East");
        counties.add("Humboldt");
        counties.add("Cheboygan");
        counties.add("Tom Green");
        counties.add("Hodgeman");
        counties.add("Benzie");
        counties.add("Kidder");
        counties.add("Burleigh");
        counties.add("Berrien");
        counties.add("St. Lucie");
        counties.add("Harnett");
        counties.add("Sublette");
        counties.add("Traverse");
        counties.add("Caldwell Parish");
        counties.add("Walworth");
        counties.add("Kalamazoo");
        counties.add("Hamilton");
        counties.add("Yellow Medicine");
        counties.add("Mora");
        counties.add("Sherman");
        counties.add("Bethel");
        counties.add("Charles City");
        counties.add("Daniels");
        counties.add("Washington");
        counties.add("Dearborn");
        counties.add("Solano");
        counties.add("Conejos");
        counties.add("Elk");
        counties.add("Harris");
        counties.add("Fremont");
        counties.add("Addison");
        counties.add("LaGrange");
        counties.add("Sarasota");
        counties.add("Schuyler");
        counties.add("Bacon");
        counties.add("Brookings");
        counties.add("Androscoggin");
        counties.add("Forrest");
        counties.add("Smith");
        counties.add("Milam");
        counties.add("McClain");
        counties.add("Labette");
        counties.add("Powhatan");
        counties.add("Musselshell");
        counties.add("Coosa");
        counties.add("Kootenai");
        counties.add("Parker");
        counties.add("Mitchell");
        counties.add("Niobrara");
        counties.add("Miller");
        counties.add("Bingham");
        counties.add("Borden");

    }

    @Override
    public String anonymize(String token) {

        return counties.stream()
                .skip((int) (counties.size() * Math.random()))
                .findFirst().orElse("Harris");

    }

}
