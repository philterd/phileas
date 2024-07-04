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
package ai.philterd.phileas.services.filters.ai.python;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface PyTorchRestService {

    // curl -X POST "http://localhost:18080/process?c=none&d=7da7b0ebc6cbd98ac39b02c06e3848e2&p=0" -H "Content-Type: text/plain" -d 'John went to Paris.'
    // {"c": "none", "d": "7da7b0ebc6cbd98ac39b02c06e3848e2", "p": "0", "spans": [{"text": "John", "tag": "PER", "score": 0.6359418034553528, "start": 0, "end": 4}, {"text": "Paris.", "tag": "LOC", "score": 0.8183994889259338, "start": 13, "end": 19}]}

    @POST("process")
    @Headers({"Content-Type: text/plain", "Accept: application/json"})
    Call<PyTorchResponse> process(@Query("c") String context, @Query("d") String documentId, @Query("p") int piece, @Body String text);

}