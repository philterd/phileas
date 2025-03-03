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
package ai.philterd.phileas.services.policies;

import ai.philterd.phileas.model.configuration.PhileasConfiguration;
import ai.philterd.phileas.model.exceptions.api.BadRequestException;
import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.services.AbstractPolicyService;
import ai.philterd.phileas.model.services.PolicyCacheService;
import ai.philterd.phileas.model.services.PolicyService;
import ai.philterd.phileas.services.policies.cache.InMemoryPolicyCacheService;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class LocalPolicyService extends AbstractPolicyService implements PolicyService {

    private static final Logger LOGGER = LogManager.getLogger(LocalPolicyService.class);

    private static final String JSON_EXTENSION = ".json";
    private static final String YAML_EXTENSION = ".yaml";

    private final String policiesDirectory;
    private final PolicyCacheService policyCacheService;

    public LocalPolicyService(PhileasConfiguration phileasConfiguration) {
        
        this.policiesDirectory = phileasConfiguration.policiesDirectory();
        LOGGER.info("Looking for policies in {}", policiesDirectory);

        // Always use an in-memory cache when using a local policy service.
        this.policyCacheService = new InMemoryPolicyCacheService();

    }

    @Override
    public List<String> get() throws IOException {

        // This function never uses a cache.

        final List<String> names = new LinkedList<>();

        // Read the policies from the file system.
        final Collection<File> files = FileUtils.listFiles(new File(policiesDirectory), new String[]{"json", "yaml"}, false);

        for(final File file : files) {

            final String json = FileUtils.readFileToString(file, Charset.defaultCharset());

            final String name;

            if(file.getName().endsWith(JSON_EXTENSION)) {

                final JSONObject object = new JSONObject(json);
                name = object.getString("name");

            } else {

                final Yaml yaml = new Yaml();
                final Policy policy = yaml.loadAs(json, Policy.class);
                name = policy.getName();

            }

            names.add(name);

        }

        return names;

    }

    @Override
    public String get(String policyName) throws IOException {

        String policyContent = policyCacheService.get(policyName);

        if(policyContent == null) {

            // The policy wasn't found in the cache so look on the file system.

            final File file = new File(policiesDirectory, policyName + JSON_EXTENSION);

            if (file.exists()) {

                policyContent = FileUtils.readFileToString(file, Charset.defaultCharset());

                // Put it in the cache.
                policyCacheService.insert(policyName, policyContent);

            } else {

                // Look for a yaml file.

                final File yamlFile = new File(policiesDirectory, policyName + YAML_EXTENSION);

                if (yamlFile.exists()) {

                    policyContent = FileUtils.readFileToString(yamlFile, Charset.defaultCharset());

                    // Put it in the cache.
                    policyCacheService.insert(policyName, policyContent);

                } else {

                    throw new FileNotFoundException("Policy [" + policyName + "] does not exist.");

                }

            }

        }

        return policyContent;

    }

    @Override
    public Map<String, String> getAll() throws IOException {

        final Map<String, String> policies = new HashMap<>();

        // Read the policies from the file system.
        final Collection<File> files = FileUtils.listFiles(new File(policiesDirectory), new String[]{"json", "yaml"}, false);
        LOGGER.info("Found {} policies", files.size());

        for (final File file : files) {

            LOGGER.info("Loading policy {}", file.getAbsolutePath());
            final String policyContents = FileUtils.readFileToString(file, Charset.defaultCharset());

            final String name;

            if(file.getName().endsWith(JSON_EXTENSION)) {

                final JSONObject object = new JSONObject(policyContents);
                name = object.getString("name");

            } else {

                final Yaml yaml = new Yaml();
                final Policy policy = yaml.loadAs(policyContents, Policy.class);
                name = policy.getName();

            }

            policies.put(name, policyContents);
            LOGGER.info("Added policy named [{}]", name);

        }

        return policies;

    }

    @Override
    public void save(String policyJson) throws IOException {

        try {

            if(isYaml(policyJson)) {

                final Yaml yaml = new Yaml();
                System.out.println(policyJson);
                final Policy policy = yaml.loadAs(policyJson, Policy.class);
                final String name = policy.getName();

                final File file = new File(policiesDirectory, name + YAML_EXTENSION);

                FileUtils.writeStringToFile(file, policyJson, Charset.defaultCharset());

                // Put this policy into the cache.
                policyCacheService.insert(name, policyJson);

            } else {

                final JSONObject object = new JSONObject(policyJson);
                final String policyName = object.getString("name");

                final File file = new File(policiesDirectory, policyName + JSON_EXTENSION);

                FileUtils.writeStringToFile(file, policyJson, Charset.defaultCharset());

                // Put this policy into the cache.
                policyCacheService.insert(policyName, policyJson);

            }

        } catch (JSONException ex) {

            LOGGER.error("The provided policy is not valid.", ex);
            throw new BadRequestException("The provided policy is not valid.");

        }

    }

    @Override
    public void delete(String policyName) throws IOException {

        final File file = new File(policiesDirectory, policyName + JSON_EXTENSION);

        LOGGER.info("Deleting policy at: {}", file.getAbsolutePath());

        if(file.exists()) {

            if(!file.delete()) {
                throw new IOException("Unable to delete policy " + policyName + JSON_EXTENSION);
            }

            // Remove it from the cache.
            policyCacheService.remove(policyName);

        } else {
            throw new FileNotFoundException("Policy with name " + policyName + " does not exist.");
        }

    }

    private boolean isYaml(String str) {
System.out.println(str);
        final LoadSettings settings = LoadSettings.builder().build();
        final Load load = new Load(settings);

        try {
            load.loadFromString(str);
            return true;
        } catch (YamlEngineException e) {
            System.out.println(e.getMessage());
            return false;
        }

    }

}


