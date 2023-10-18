/*
 *     Copyright 2023 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.test.phileas.model.objects;

import com.google.gson.Gson;
import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.Alert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AlertTest {

    @Test
    public void alertJson1() {

        final Alert alert = new Alert("my-filter-policy", "my-strategy", "context", "documentid", FilterType.CREDIT_CARD.getType());

        final Gson gson = new Gson();
        final String json = gson.toJson(alert);

        Assertions.assertNotNull(json);

        System.out.println(json);

    }

}
