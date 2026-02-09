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

import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class AbstractGeneratorTest {

    private static class TestGenerator extends AbstractGenerator<String> {
        @Override
        public String random() { return null; }
        @Override
        public long poolSize() { return 0; }
        public List<String> testLoadNames(String path) throws IOException {
            return loadNames(path);
        }
    }

    @Test
    public void testLoadNames() throws IOException {
        TestGenerator generator = new TestGenerator();
        List<String> names = generator.testLoadNames("/first-names.txt");
        assertNotNull(names);
        assertFalse(names.isEmpty());
        for (String name : names) {
            assertFalse(name.trim().isEmpty());
        }
    }

    @Test(expected = IOException.class)
    public void testLoadNamesNotFound() throws IOException {
        TestGenerator generator = new TestGenerator();
        generator.testLoadNames("/non-existent.txt");
    }
}
