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
package ai.philterd.phileas.model.filtering;

import com.google.gson.Gson;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpanVector {

    private Map<Double, Double> vectorIndexes;
    private final transient Gson gson;

    public SpanVector() {
        this.vectorIndexes = new ConcurrentHashMap<>();
        this.gson = new Gson();
    }

    @Override
    public String toString() {
        return gson.toJson(this);
    }

    public Map<Double, Double> getVectorIndexes() {
        return vectorIndexes;
    }

    public void setVectorIndexes(Map<Double, Double> vectorIndexes) {
        this.vectorIndexes = vectorIndexes;
    }

}
