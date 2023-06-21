/*
 *     Copyright 2023 Philerd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.service.ai.onnx;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Tokens {

    private String[] tokens;
    private long[] ids;
    private long[] mask;
    private long[] types;

    public Tokens(String[] tokens, long[] ids, long[] mask, long[] types) {

        this.tokens = tokens;
        this.ids = ids;
        this.mask = mask;
        this.types = types;

    }

    /*public List<Tokens> chunk() {

        final List<Tokens> chunks = new LinkedList<>();

        int start = 1;

        for(int x = 510; x < tokens.length; x = x + 510) {

            final String[] tokens = Arrays.copyOfRange(getTokens(), start, x - 1);
            final long[] ids = Arrays.copyOfRange(getIds(), start, x - 1);
            final long[] mask = Arrays.copyOfRange(getMask(), start, x - 1);
            final long[] types = Arrays.copyOfRange(getTypes(), start, x - 1);

            start = x;

            chunks.add(new Tokens(tokens, ids, mask, types));

        }

        return chunks;

    }*/

    public String[] getTokens() {
        return tokens;
    }

    public long[] getIds() {
        return ids;
    }

    public long[] getMask() {
        return mask;
    }


    public long[] getTypes() {
        return types;
    }

}