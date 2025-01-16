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

import ai.philterd.phileas.model.services.AnonymizationCacheService;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Collection;
import java.util.LinkedList;

public class VinAnonymizationService extends AbstractAnonymizationService {

    private final Collection<String> randomVins = new LinkedList<>();

    public VinAnonymizationService(AnonymizationCacheService anonymizationCacheService) {
        super(anonymizationCacheService);

        randomVins.add("1GTEC19V95Z351087");
        randomVins.add("JH4DC53835S819649");
        randomVins.add("1FTHF25H2KLB08097");
        randomVins.add("1GCHC23K48F214887");
        randomVins.add("1FTPX14V57KD21696");
        randomVins.add("1HD1CGP105K438188");
        randomVins.add("1GKDG15H2J7538956");
        randomVins.add("2HSFBGTR0HC038203");
        randomVins.add("1G4HR54K25U123439");
        randomVins.add("1B7GL22Z01S332538");
        randomVins.add("3D7KR26L39G511860");
        randomVins.add("1FDXF46F23EB93853");
        randomVins.add("1HTSMABK9YH257093");
        randomVins.add("4TARN13P4SZ372202");
        randomVins.add("1FDXF82K7FVA12308");
        randomVins.add("5N3AA08C35N865588");
        randomVins.add("3GTP1VEJ6EG369040");
        randomVins.add("1GCDT19Z0N0104303");
        randomVins.add("1GDHR33J4MF703467");
        randomVins.add("4S3BMJG67B2227285");
        randomVins.add("1GTSKVE07AZ150094");
        randomVins.add("1GTEC19V95Z351087");
        randomVins.add("1GTSKVE07AZ150094");
        randomVins.add("1M1K195Y0VM078514");
        randomVins.add("1GBJ6C1G09F425832");

    }

    @Override
    public String anonymize(String token) {

        return randomVins.stream()
                .skip((int) (randomVins.size() * Math.random()))
                .findFirst().orElse(RandomStringUtils.random(17));

    }

}
