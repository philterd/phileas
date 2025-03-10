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
package ai.philterd.phileas.processors.structured.fhir;

import ai.philterd.phileas.model.policy.Crypto;
import ai.philterd.phileas.model.utils.Encryption;
import org.hl7.fhir.r4.model.StringType;

import java.util.LinkedList;
import java.util.List;

public abstract class AbstractFhirDocumentProcessor {

    protected List<StringType> encryptList(List<StringType> list, Crypto crypto) throws Exception {

        final List<StringType> encryptedList = new LinkedList<>();

        for(StringType st : list) {

            final String encryptedValue = Encryption.encrypt(st.getValueAsString(), crypto);

            final StringType clone = st.copy();
            clone.setValueAsString(encryptedValue);

        }

        return encryptedList;

    }

}
