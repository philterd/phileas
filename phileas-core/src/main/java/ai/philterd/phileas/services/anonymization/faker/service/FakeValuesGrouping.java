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
package ai.philterd.phileas.services.anonymization.faker.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FakeValuesGrouping implements FakeValuesInterface {

    private List<FakeValues> fakeValuesList = new ArrayList<FakeValues>();

    public void add(FakeValues fakeValues) {
        fakeValuesList.add(fakeValues);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Map get(String key) {
        Map result = null;
        for (FakeValues fakeValues : fakeValuesList) {
            if (fakeValues.supportsPath(key)) {
                if (result != null) {
                    final Map newResult = fakeValues.get(key);
                    result.putAll(newResult);
                } else {
                    result = fakeValues.get(key);
                }
            }
        }
        return result;
    }
}
