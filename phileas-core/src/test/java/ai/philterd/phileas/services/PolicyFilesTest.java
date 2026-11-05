/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.services;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.policy.InvalidPolicyFileException;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class PolicyFilesTest {

    @TempDir
    Path temp;

    private Path allowed;
    private Path terms;
    private Path secret;

    @BeforeEach
    public void createFiles() throws Exception {
        allowed = Files.createDirectories(temp.resolve("allowed"));
        terms = Files.writeString(allowed.resolve("terms.txt"), "Quorlan\nBexley\n");
        final Path outside = Files.createDirectories(temp.resolve("outside"));
        secret = Files.writeString(outside.resolve("secret.txt"), "hunter2\n");
    }

    private static PhileasConfiguration configuration(final String... properties) {
        final Properties p = new Properties();
        for (int i = 0; i < properties.length; i += 2) {
            p.setProperty(properties[i], properties[i + 1]);
        }
        return new PhileasConfiguration(p);
    }

    private PolicyFiles restricted(final long maxBytes) {
        return PolicyFiles.of(configuration("policy.files.restricted", "true", "policy.files.directories", allowed.toString(),
                "policy.files.max.bytes", Long.toString(maxBytes)));
    }

    private static String refused(final PolicyFiles files, final String path) {
        return Assertions.assertThrows(InvalidPolicyFileException.class, () -> files.readLines("the field", path)).getMessage();
    }

    @Test
    public void aFileInAnAllowedDirectoryIsRead() throws Exception {
        Assertions.assertEquals(List.of("Quorlan", "Bexley"), restricted(0).readLines("the field", terms.toString()));
    }

    @Test
    public void aFileOutsideTheDirectoriesIsRefused() {
        Assertions.assertEquals("The file '" + secret + "' named by the field cannot be read: a policy may name only an existing "
                + "regular file inside an allowed directory.", refused(restricted(0), secret.toString()));
    }

    @Test
    public void aSymbolicLinkOrTraversalOutOfAnAllowedDirectoryIsRefused() throws Exception {
        final Path link = Files.createSymbolicLink(allowed.resolve("link.txt"), secret);
        refused(restricted(0), link.toString());
        refused(restricted(0), allowed + "/../outside/secret.txt");
    }

    @Test
    public void aFileThatIsNotRegularIsRefusedWhetherOrNotRestricted() throws Exception {
        refused(restricted(0), "/dev/zero");
        refused(PolicyFiles.unrestricted(), "/dev/zero");
        refused(restricted(0), allowed.toString());
        refused(PolicyFiles.unrestricted(), allowed.toString());
        final Path link = Files.createSymbolicLink(allowed.resolve("zero"), Path.of("/dev/zero"));
        refused(restricted(0), link.toString());
    }

    @Test
    public void aFileOverTheLimitIsRefused() throws Exception {
        final Path large = Files.writeString(allowed.resolve("large.txt"), "x".repeat(200));
        Assertions.assertEquals("The file '" + large + "' named by the field is larger than the limit of 100 bytes.",
                refused(restricted(100), large.toString()));
        Assertions.assertEquals(List.of("Quorlan", "Bexley"), restricted(100).readLines("the field", terms.toString()));
    }

    @Test
    public void restrictedWithNoDirectoriesAllowsNoFile() {
        refused(PolicyFiles.of(configuration("policy.files.restricted", "true")), terms.toString());
    }

    @Test
    public void aMissingFileIsRefusedWhenRestrictedAndLeftToTheCallerOtherwise() throws Exception {
        final String missing = allowed.resolve("missing.txt").toString();
        refused(restricted(0), missing);
        Assertions.assertNull(PolicyFiles.unrestricted().readLines("the field", missing));
    }

    @Test
    public void theMessageDoesNotTellAMissingFileFromOneOutsideOrNotRegular() {
        final PolicyFiles files = restricted(0);
        final String missing = refused(files, allowed.resolve("missing.txt").toString()).replace(allowed.resolve("missing.txt").toString(), "P");
        Assertions.assertEquals(missing, refused(files, secret.toString()).replace(secret.toString(), "P"));
        Assertions.assertEquals(missing, refused(files, "/dev/zero").replace("/dev/zero", "P"));
    }

    @Test
    public void unrestrictedReadsAnyRegularFile() throws Exception {
        Assertions.assertEquals(List.of("hunter2"), PolicyFiles.unrestricted().readLines("the field", secret.toString()));
    }

    /** The four places a policy names a file, each pointing at the file outside the allowed directory. */
    @Test
    public void everyFileFieldIsCheckedWhenThePolicyIsLoaded() {
        final String path = secret.toString();
        final String[][] policies = {
                {"{\"identifiers\":{\"dictionaries\":[{\"files\":[\"" + path + "\"]}]}}", "identifiers.dictionaries[0].files"},
                {"{\"identifiers\":{\"ssn\":{\"ignoredFiles\":[\"" + path + "\"]}}}", "identifiers.ssn.ignoredFiles"},
                {"{\"identifiers\":{\"ssn\":{\"ssnFilterStrategies\":[{\"strategy\":\"MAP_REPLACE\",\"mappingFiles\":[\"" + path + "\"]}]}}}",
                        "identifiers.ssn.ssnFilterStrategies[0].mappingFiles"},
                {"{\"identifiers\":{\"ssn\":{}},\"ignored\":[{\"files\":[\"" + path + "\"]}]}", "ignored[0].files"}};

        final FilterPolicyLoader loader = new FilterPolicyLoader(configuration("policy.files.restricted", "true",
                "policy.files.directories", allowed.toString()), new SecureRandom(), null);

        for (final String[] c : policies) {
            final Policy policy = new Gson().fromJson(c[0], Policy.class);
            final InvalidPolicyFileException ex = Assertions.assertThrows(InvalidPolicyFileException.class,
                    () -> loader.getFiltersForPolicy(policy, new HashMap<>()), c[1]);
            Assertions.assertTrue(ex.getMessage().contains("named by " + c[1] + " "), ex.getMessage());
        }
    }

    @Test
    public void anAllowedDictionaryAndMappingFileAreUsed() throws Exception {
        final Path mappings = Files.writeString(allowed.resolve("mappings.tsv"), "123-45-6789\tSSN-ONE\n");
        final PhileasConfiguration restricted = configuration("policy.files.restricted", "true", "policy.files.directories", allowed.toString());
        final PlainTextFilterService service = new PlainTextFilterService(restricted, new DefaultContextService(), new InMemoryVectorService(), null);

        final Policy dictionary = new Gson().fromJson("{\"identifiers\":{\"dictionaries\":[{\"files\":[\"" + terms + "\"]}]}}", Policy.class);
        Assertions.assertEquals("Ask {{{REDACTED-custom-dictionary}}} today",
                service.filter(dictionary, "context", "Ask Quorlan today").getFilteredText());

        final Policy mapped = new Gson().fromJson("{\"identifiers\":{\"ssn\":{\"ssnFilterStrategies\":[{\"strategy\":\"MAP_REPLACE\","
                + "\"mappingFiles\":[\"" + mappings + "\"]}]}}}", Policy.class);
        Assertions.assertEquals("SSN SSN-ONE", service.filter(mapped, "context", "SSN 123-45-6789").getFilteredText());
    }

    @Test
    public void aFileThatIsNotValidTextFailsAsBefore() throws Exception {
        // Files.readAllLines failed on bytes not valid in the charset, rather than replacing them.
        final Path invalid = Files.write(allowed.resolve("invalid.txt"), new byte[]{'o', 'k', '\n', (byte) 0xC3, (byte) 0x28, '\n'});
        org.junit.jupiter.api.Assumptions.assumeTrue("UTF-8".equals(java.nio.charset.Charset.defaultCharset().name()));
        Assertions.assertThrows(java.nio.charset.CharacterCodingException.class, () -> restricted(0).readLines("the field", invalid.toString()));
    }

}
