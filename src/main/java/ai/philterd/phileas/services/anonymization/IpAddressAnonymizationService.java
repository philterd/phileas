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

import ai.philterd.phileas.services.context.ContextService;
import org.apache.commons.lang3.RandomStringUtils;

public class IpAddressAnonymizationService extends AbstractAnonymizationService {

    public IpAddressAnonymizationService(final ContextService contextService) {
        super(contextService);
    }

    @Override
    public ContextService getContextService() {
        return contextService;
    }

    @Override
    public String anonymize(String token) {

        if(token.contains(":")) {

            // IPv6

            return RandomStringUtils.secure().nextNumeric(4) + ":"
                    + RandomStringUtils.secure().nextNumeric(4) + ":"
                    + RandomStringUtils.secure().nextNumeric(4) + ":"
                    + RandomStringUtils.secure().nextNumeric(4) + ":"
                    + RandomStringUtils.secure().nextNumeric(4) + ":"
                    + RandomStringUtils.secure().nextNumeric(4) + ":"
                    + RandomStringUtils.secure().nextNumeric(4) + ":"
                    + RandomStringUtils.secure().nextNumeric(4);

        } else {

            // IPv4

            return RandomStringUtils.secure().nextNumeric(3) + "."
                    + RandomStringUtils.secure().nextNumeric(3) + "."
                    + RandomStringUtils.secure().nextNumeric(3) + "."
                    + RandomStringUtils.secure().nextNumeric(3);

        }

    }

}
