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
package ai.philterd.phileas.service.ai.models;

import opennlp.tools.doccat.DocumentCategorizerME;

import java.util.HashMap;
import java.util.Map;

public class ModelCache {

    private static ModelCache instance;

    private final Map<String, DocumentCategorizerME> cache;

    public static ModelCache getInstance() {

        if(instance == null) {
            instance = new ModelCache();
        }

        return instance;

    }

    public DocumentCategorizerME get(final String name) {
        return cache.get(name);
    }

    public void put(final String name, final DocumentCategorizerME model) {
        this.cache.put(name, model);
    }

    private ModelCache() {
        this.cache = new HashMap<>();
    }

}
