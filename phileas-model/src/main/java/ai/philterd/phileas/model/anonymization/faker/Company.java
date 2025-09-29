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
/*
 * Copyright 2014 DiUS Computing
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phileas.model.anonymization.faker;

import ai.philterd.phileas.model.anonymization.faker.service.FakerIDN;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.join;

public class Company {

    private final Faker faker;

    protected Company(Faker faker) {
        this.faker = faker;
    }

    public String name() {
        return faker.fakeValuesService().resolve("company.name", this, faker);
    }

    public String suffix() {
        return faker.fakeValuesService().resolve("company.suffix", this, faker);
    }

    public String industry() {
        return faker.fakeValuesService().resolve("company.industry", this, faker);
    }

    public String profession() {
        return faker.fakeValuesService().resolve("company.profession", this, faker);
    }

    public String buzzword() {
        @SuppressWarnings("unchecked")
        List<List<String>> buzzwordLists = (List<List<String>>) faker.fakeValuesService().fetchObject("company.buzzwords");
        List<String> buzzwords = new ArrayList<String>();
        for (List<String> buzzwordList : buzzwordLists) {
            buzzwords.addAll(buzzwordList);
        }
        return buzzwords.get(faker.random().nextInt(buzzwords.size()));
    }

    /**
     * Generate a buzzword-laden catch phrase.
     */
    public String catchPhrase() {
        @SuppressWarnings("unchecked")
        List<List<String>> catchPhraseLists = (List<List<String>>) faker.fakeValuesService().fetchObject("company.buzzwords");
        return joinSampleOfEachList(catchPhraseLists, " ");
    }

    /**
     * When a straight answer won't do, BS to the rescue!
     */
    public String bs() {
        @SuppressWarnings("unchecked")
        List<List<String>> buzzwordLists = (List<List<String>>) faker.fakeValuesService().fetchObject("company.bs");
        return joinSampleOfEachList(buzzwordLists, " ");
    }

    /**
     * Generate a random company logo url in PNG format.
     */
    public String logo() {
        int number = faker.random().nextInt(13) + 1;
        return "https://pigment.github.io/fake-logos/logos/medium/color/" + number + ".png";
    }

    public String url() {
        return join(
                "www",
                ".",
                FakerIDN.toASCII(domainName()),
                ".",
                domainSuffix()
        );
    }

    private String domainName(){
        return StringUtils.deleteWhitespace(name().toLowerCase().replaceAll(",", "").replaceAll("'", ""));
    }

    private String domainSuffix() {
        return faker.fakeValuesService().resolve("internet.domain_suffix", this, faker);
    }

    private String joinSampleOfEachList(List<List<String>> listOfLists, String separator) {
        List<String> words = new ArrayList<String>();
        for (List<String> list : listOfLists) {
           words.add(list.get(faker.random().nextInt(list.size())));
        }
        return join(words, separator);
    }
}
