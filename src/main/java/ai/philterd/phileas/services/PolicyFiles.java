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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the files a policy names (dictionary {@code files}, a filter's {@code ignoredFiles}, a strategy's
 * {@code mappingFiles}, and the top-level {@code ignored[].files}). A file must be a regular file, since a device
 * such as {@code /dev/zero} never ends and a named pipe can block forever. When {@code policy.files.restricted} is
 * on, it must also be inside one of {@code policy.files.directories} after symbolic links and {@code ..} are
 * resolved. When {@code policy.files.max.bytes} is above zero, a larger file is refused, and the limit is enforced
 * while reading, not only checked beforehand.
 */
public final class PolicyFiles {

    private static final Logger LOGGER = LogManager.getLogger(PolicyFiles.class);

    private static final PolicyFiles UNRESTRICTED = new PolicyFiles(false, List.of(), 0);

    /** The longest array the JVM reliably allocates, a little under {@code Integer.MAX_VALUE}. */
    private static final long MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8L;

    private final boolean restricted;
    private final List<Path> directories;
    private final long maxBytes;

    private PolicyFiles(final boolean restricted, final List<Path> directories, final long maxBytes) {
        this.restricted = restricted;
        this.directories = directories;
        this.maxBytes = maxBytes;
    }

    /** Reads files as the configuration allows. */
    public static PolicyFiles of(final PhileasConfiguration configuration) {
        final List<Path> directories = new ArrayList<>();
        for (final String directory : configuration.policyFilesDirectories()) {
            try {
                directories.add(Path.of(directory).toRealPath());
            } catch (final IOException ex) {
                // An allowed directory that does not exist allows nothing; it is not an error to configure one.
                LOGGER.warn("Allowed policy file directory {} could not be resolved and allows no files.", directory);
            }
        }
        return new PolicyFiles(configuration.policyFilesRestricted(), List.copyOf(directories),
                Math.max(0, configuration.policyFilesMaxBytes()));
    }

    /** Reads files without the directory restriction or a size limit, but still only regular files. */
    public static PolicyFiles unrestricted() {
        return UNRESTRICTED;
    }

    /**
     * Checks a file a policy names, without reading it.
     * @param field Where the policy names the file, for the message, such as {@code identifiers.dictionaries[0].files}.
     * @param path The path as the policy gives it.
     * @return The resolved path, or null when the directories are not restricted and the file does not exist, in which
     * case each caller keeps its own handling of a missing file.
     * @throws InvalidPolicyFileException if the policy may not read the file.
     */
    public Path check(final String field, final String path) {

        final Path real;
        try {
            real = Path.of(path).toRealPath();
        } catch (final NoSuchFileException ex) {
            if (!restricted) {
                return null;
            }
            throw unreadable(field, path);
        } catch (final IOException | RuntimeException ex) {
            throw unreadable(field, path);
        }

        if (restricted && directories.stream().noneMatch(real::startsWith)) {
            throw unreadable(field, path);
        }
        if (!Files.isRegularFile(real, LinkOption.NOFOLLOW_LINKS)) {
            throw unreadable(field, path);
        }
        if (maxBytes > 0) {
            try {
                if (Files.size(real) > maxBytes) {
                    throw tooLarge(field, path);
                }
            } catch (final IOException ex) {
                throw unreadable(field, path);
            }
        }
        return real;

    }

    /**
     * Reads the lines of a file a policy names, as {@link Files#readAllLines(Path, Charset)} does, after
     * {@link #check(String, String)}. The size limit is enforced while reading.
     * @return The lines, or null when the directories are not restricted and the file does not exist.
     * @throws InvalidPolicyFileException if the policy may not read the file.
     * @throws IOException if the file cannot be read.
     */
    public List<String> readLines(final String field, final String path) throws IOException {

        final Path real = check(field, path);
        if (real == null) {
            return null;
        }

        final byte[] bytes;
        try (InputStream in = Files.newInputStream(real)) {
            if (maxBytes > 0 && maxBytes < MAX_ARRAY_LENGTH) {
                // One byte past the limit, so a file that grew after the check is still refused.
                bytes = in.readNBytes((int) maxBytes + 1);
                if (bytes.length > maxBytes) {
                    throw tooLarge(field, path);
                }
            } else {
                // No limit, or one larger than an array can hold: read it all, as Files.readAllLines does.
                bytes = in.readAllBytes();
            }
        }

        // Decoded strictly, as Files.readAllLines does, so a file that is not valid in the charset fails with a
        // CharacterCodingException rather than loading with replacement characters.
        final String text = Charset.defaultCharset().newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();

        final List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(text))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                lines.add(line);
            }
        }
        return lines;

    }

    /** The same message whether the file is missing, outside the allowed directories, or not a regular file. */
    private InvalidPolicyFileException unreadable(final String field, final String path) {
        return new InvalidPolicyFileException("The file '" + path + "' named by " + field + " cannot be read: a policy may "
                + "name only an existing regular file" + (restricted ? " inside an allowed directory." : "."));
    }

    private InvalidPolicyFileException tooLarge(final String field, final String path) {
        return new InvalidPolicyFileException("The file '" + path + "' named by " + field + " is larger than the limit of "
                + maxBytes + " bytes.");
    }

}
