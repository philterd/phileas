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
package ai.philterd.phileas.services.filters.ai.pheye;

import java.util.Collection;
import java.util.List;

/**
 * Produces raw GLiNER detections for a piece of text. Two implementations exist:
 * a remote detector that calls a PhEye service over HTTP, and a local detector
 * (provided by the optional {@code ai.philterd:phileas-pheye-onnx} module) that
 * runs a GLiNER model on-device via ONNX Runtime.
 *
 * <p>Implementations return the same {@link PhEyeSpan} contract regardless of how
 * detection is performed; the {@link PhEyeFilter} applies thresholds, windows, and
 * replacement strategies on top of the returned spans.
 */
public interface PhEyeDetector extends AutoCloseable {

    /**
     * Detect entities in the given text.
     *
     * @param text    the (already punctuation-normalized) text to scan
     * @param labels  the entity labels to detect; for a GLiNER model this is the detection prompt
     * @param context the document context identifier (passed through to a remote PhEye)
     * @param piece   the piece/window index (passed through to a remote PhEye)
     * @return the detected spans, or an empty list if none; never {@code null}
     */
    List<PhEyeSpan> detect(String text, Collection<String> labels, String context, int piece) throws Exception;

    /** Release any held resources (e.g. an ONNX session). The default is a no-op. */
    @Override
    default void close() throws Exception {
        // no-op by default
    }

}
