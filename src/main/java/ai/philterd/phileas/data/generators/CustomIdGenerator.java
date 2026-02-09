/*
 * Copyright 2026 Philterd, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phileas.data.generators;

import java.util.Random;

/**
 * Generates custom IDs based on a pattern.
 */
public class CustomIdGenerator extends AbstractGenerator<String> {

    private final Random random;
    private final String pattern;

    /**
     * Creates a new custom ID generator.
     * @param pattern The pattern to use for generation.
     */
    public CustomIdGenerator(final String pattern) {
        this(new Random(), pattern);
    }

    /**
     * Creates a new custom ID generator.
     * @param random The {@link Random} to use.
     * @param pattern The pattern to use for generation.
     */
    public CustomIdGenerator(final Random random, final String pattern) {
        this.random = random;
        this.pattern = pattern;
    }

    @Override
    public String random() {
        if (pattern == null) {
            return null;
        }

        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < pattern.length(); i++) {
            final char c = pattern.charAt(i);

            if (Character.isDigit(c)) {
                sb.append(random.nextInt(10));
            } else if (Character.isLetter(c)) {
                if (Character.isUpperCase(c)) {
                    sb.append((char) ('A' + random.nextInt(26)));
                } else {
                    sb.append((char) ('a' + random.nextInt(26)));
                }
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    @Override
    public long poolSize() {
        if (pattern == null) {
            return 0;
        }

        double poolSize = 1;

        for (int i = 0; i < pattern.length(); i++) {
            final char c = pattern.charAt(i);
            if (Character.isDigit(c)) {
                poolSize *= 10;
            } else if (Character.isLetter(c)) {
                poolSize *= 26;
            }
        }

        return (long) poolSize;
    }

}
