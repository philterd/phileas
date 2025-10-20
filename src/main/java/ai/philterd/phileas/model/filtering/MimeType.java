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
package ai.philterd.phileas.model.filtering;

/**
 * A mime type
 */
public enum MimeType {

    /**
     * text/plain
     */
    TEXT_PLAIN("text/plain"),

    /**
     * text/html
     */
    TEXT_HTML("text/html"),

    /**
     * application/pdf
     */
    APPLICATION_PDF("application/pdf"),

    /**
     * image/jpeg
     */
    IMAGE_JPEG("image/jpeg");

    private String value;

    MimeType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

}
