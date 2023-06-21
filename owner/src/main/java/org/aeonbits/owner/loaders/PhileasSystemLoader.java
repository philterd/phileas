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

package org.aeonbits.owner.loaders;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import static org.aeonbits.owner.util.Util.system;

/**
 * Allows specifying <code>system:properties</code> and <code>system:env</code> with the <code>@Sources</code> annotation.
 *
 * This class accesses <code>Util.system()</code> which is package accessible; for this reason this class cannot be moved
 * inside the package loaders.
 *
 * I think this class should be split into two separate classes:
 * one for system:properties and one for system:env.
 */
public class PhileasSystemLoader implements Loader {

    private static final String SYSTEM_PROPERTIES_URI = "system:properties";
    private static final String ENVIRONMENT_VARIABLES_URI = "system:env";

    public boolean accept(URI uri) {
        String path = uri.toString();
        return SYSTEM_PROPERTIES_URI.equals(path) || ENVIRONMENT_VARIABLES_URI.equals(path);
    }

    public void load(Properties result, URI uri) throws IOException {
        String path = uri.toString();
        if (SYSTEM_PROPERTIES_URI.equals(path))
            result.putAll(system().getProperties());
        if (ENVIRONMENT_VARIABLES_URI.equals(path)) {
            for(String key : system().getenv().keySet()) {
                final String name = key.toLowerCase().replace("philter_", "").replace("_", ".");
                result.put(name, system().getenv().get(key));
            }
        }
    }

    public String defaultSpecFor(String uriPrefix) {
        return null;
    }
}


