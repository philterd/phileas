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
package ai.philterd.phileas.data;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates a CSV of generated data.
 */
public class CsvGenerator {

    private final Map<String, DataGenerator.Generator<?>> columns;
    private String delimiter = ",";
    private boolean useQuotes = false;

    /**
     * Creates a new CSV generator.
     */
    public CsvGenerator() {
        this.columns = new LinkedHashMap<>();
    }

    /**
     * Sets the delimiter for the CSV.
     * @param delimiter The delimiter.
     * @return The {@link CsvGenerator} instance.
     */
    public CsvGenerator withDelimiter(final String delimiter) {
        this.delimiter = delimiter;
        return this;
    }

    /**
     * Sets whether or not to use quotes around all values.
     * @param useQuotes Whether or not to use quotes.
     * @return The {@link CsvGenerator} instance.
     */
    public CsvGenerator withQuotes(final boolean useQuotes) {
        this.useQuotes = useQuotes;
        return this;
    }

    /**
     * Adds a column to the CSV.
     * @param name The name of the column.
     * @param generator The generator for the column's data.
     * @return The {@link CsvGenerator} instance.
     */
    public CsvGenerator addColumn(final String name, final DataGenerator.Generator<?> generator) {
        columns.put(name, generator);
        return this;
    }

    /**
     * Generates the CSV data.
     * @param writer The {@link Writer} to write the CSV data to.
     * @param rows The number of rows to generate.
     */
    public void generate(final Writer writer, final int rows) {
        final PrintWriter printWriter = new PrintWriter(writer);

        // Write the header
        final String header = String.join(delimiter, columns.keySet().stream().map(this::escapeCsv).toList());
        printWriter.println(header);

        // Write the rows
        for (int i = 0; i < rows; i++) {
            final StringBuilder row = new StringBuilder();
            int colIndex = 0;
            for (final DataGenerator.Generator<?> generator : columns.values()) {
                final Object value = generator.random();
                row.append(escapeCsv(String.valueOf(value)));
                if (colIndex < columns.size() - 1) {
                    row.append(delimiter);
                }
                colIndex++;
            }
            printWriter.println(row);
        }

        printWriter.flush();
    }

    private String escapeCsv(String value) {
        if (useQuotes || value.contains(delimiter) || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

}
