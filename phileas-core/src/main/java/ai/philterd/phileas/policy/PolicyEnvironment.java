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
package ai.philterd.phileas.policy;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The environment variables a policy may name: as {@code env:NAME} for a {@link Crypto} or {@link FPE} key, or as a
 * {@code ${NAME}} placeholder. An application that lets people author policies restricts them, so a policy author
 * cannot use a variable meant for something else, such as another policy's key, as their own.
 *
 * <p>The allowlist is process-wide. The application sets it once at startup with {@link #restrictTo(Collection)}.
 * Otherwise it is read from {@code policy.env.allowed} (an environment variable or system property of that name, a
 * comma-separated list) when present. When neither sets it, every variable may be named, as before.</p>
 */
public final class PolicyEnvironment {

    private static final String SETTING = "policy.env.allowed";

    // Null when unrestricted.
    private static volatile Set<String> allowed = fromSetting();

    private PolicyEnvironment() {
    }

    /**
     * Restricts the variables a policy may name to these. An empty collection allows none.
     * @param names The variable names.
     */
    public static void restrictTo(final Collection<String> names) {
        allowed = Set.copyOf(names);
    }

    /** Lets a policy name any variable. For tests and applications that do not restrict policies. */
    public static void unrestricted() {
        allowed = null;
    }

    /**
     * Whether a policy may name this variable.
     * @param name The variable name.
     * @return {@code true} if it is allowed.
     */
    public static boolean isAllowed(final String name) {
        final Set<String> current = allowed;
        return current == null || current.contains(name);
    }

    /**
     * Resolves a value that may name an environment variable as {@code env:NAME}. A value without the prefix is
     * returned as it is.
     * @param field What the value is, for the message, such as {@code crypto key}.
     * @param value The value from the policy.
     * @return The value, or the variable's value, or null if the variable is not set.
     * @throws InvalidPolicyEnvironmentException if the policy may not name the variable. The variable is not read, so
     * the message is the same whether or not it exists.
     */
    public static String resolve(final String field, final String value) {
        if (value == null || !value.startsWith("env:")) {
            return value;
        }
        final String name = value.substring(4);
        if (!isAllowed(name)) {
            throw new InvalidPolicyEnvironmentException("The " + field + " names the environment variable " + name
                    + ", which a policy may not use here.");
        }
        return System.getenv(name);
    }

    private static Set<String> fromSetting() {
        String value = System.getenv(SETTING);
        if (value == null || value.isBlank()) {
            value = System.getProperty(SETTING);
        }
        if (value == null) {
            return null;
        }
        return Arrays.stream(value.split(",")).map(String::strip).filter(name -> !name.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

}
