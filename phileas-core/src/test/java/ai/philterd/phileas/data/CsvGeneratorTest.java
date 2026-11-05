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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CsvGeneratorTest {

    @Test
    public void testGenerateCsv() throws IOException {
        DataGenerator dataGenerator = new DefaultDataGenerator();
        CsvGenerator csvGenerator = new CsvGenerator()
                .addColumn("First Name", dataGenerator.firstNames())
                .addColumn("Last Name", dataGenerator.surnames())
                .addColumn("Age", dataGenerator.age());

        StringWriter writer = new StringWriter();
        csvGenerator.generate(writer, 5);

        String csv = writer.toString();
        String[] lines = csv.split("\n");

        assertEquals(6, lines.length); // 1 header + 5 data rows
        assertEquals("First Name,Last Name,Age", lines[0].trim());

        for (int i = 1; i < lines.length; i++) {
            String[] cols = lines[i].split(",");
            assertEquals(3, cols.length);
            assertTrue(Integer.parseInt(cols[2].trim()) >= 0);
        }
    }

    @Test
    public void testGenerateCsvWithEscaping() {
        // Mock generator that returns a value with a comma
        DataGenerator.Generator<String> commaGenerator = new DataGenerator.Generator<>() {
            @Override
            public String random() {
                return "Doe, John";
            }

            @Override
            public long poolSize() {
                return 1;
            }
        };

        CsvGenerator csvGenerator = new CsvGenerator()
                .addColumn("Name", commaGenerator);

        StringWriter writer = new StringWriter();
        csvGenerator.generate(writer, 1);

        String csv = writer.toString();
        String[] lines = csv.split("\n");

        assertEquals(2, lines.length);
        assertEquals("\"Doe, John\"", lines[1].trim());
    }

    @Test
    public void testGenerateCsvWithCustomDelimiter() {
        DataGenerator.Generator<String> simpleGenerator = new DataGenerator.Generator<>() {
            @Override
            public String random() {
                return "value";
            }

            @Override
            public long poolSize() {
                return 1;
            }
        };

        CsvGenerator csvGenerator = new CsvGenerator()
                .withDelimiter(";")
                .addColumn("Header1", simpleGenerator)
                .addColumn("Header2", simpleGenerator);

        StringWriter writer = new StringWriter();
        csvGenerator.generate(writer, 1);

        String csv = writer.toString();
        String[] lines = csv.split("\n");

        assertEquals("Header1;Header2", lines[0].trim());
        assertEquals("value;value", lines[1].trim());
    }

    @Test
    public void testGenerateCsvWithQuotes() {
        DataGenerator.Generator<String> simpleGenerator = new DataGenerator.Generator<>() {
            @Override
            public String random() {
                return "value";
            }

            @Override
            public long poolSize() {
                return 1;
            }
        };

        CsvGenerator csvGenerator = new CsvGenerator()
                .withQuotes(true)
                .addColumn("Header1", simpleGenerator);

        StringWriter writer = new StringWriter();
        csvGenerator.generate(writer, 1);

        String csv = writer.toString();
        String[] lines = csv.split("\n");

        assertEquals("\"Header1\"", lines[0].trim());
        assertEquals("\"value\"", lines[1].trim());
    }

    @Test
    public void testGenerateCsvWithCustomDelimiterAndEscaping() {
        DataGenerator.Generator<String> specialGenerator = new DataGenerator.Generator<>() {
            @Override
            public String random() {
                return "value;with;delimiter";
            }

            @Override
            public long poolSize() {
                return 1;
            }
        };

        CsvGenerator csvGenerator = new CsvGenerator()
                .withDelimiter(";")
                .addColumn("Name", specialGenerator);

        StringWriter writer = new StringWriter();
        csvGenerator.generate(writer, 1);

        String csv = writer.toString();
        String[] lines = csv.split("\n");

        assertEquals("Name", lines[0].trim());
        assertEquals("\"value;with;delimiter\"", lines[1].trim());
    }

}
