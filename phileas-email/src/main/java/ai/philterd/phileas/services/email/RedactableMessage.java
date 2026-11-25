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
package ai.philterd.phileas.services.email;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import java.io.InputStream;

/**
 * A MIME message that, when written, adds no header the input did not have. Jakarta Mail otherwise gives a message
 * a new {@code Message-ID} naming the local host, and a {@code Date} of the time of writing when it has none, which
 * would put back headers the policy removed.
 */
final class RedactableMessage extends MimeMessage {

    RedactableMessage(final Session session) {
        super(session);
    }

    RedactableMessage(final Session session, final InputStream input) throws MessagingException {
        super(session, input);
    }

    @Override
    protected void updateMessageID() {
        // Keep the input's Message-ID, or none.
    }

    @Override
    protected synchronized void updateHeaders() throws MessagingException {
        final boolean hadDate = getHeader("Date") != null;
        super.updateHeaders();
        if (!hadDate) {
            removeHeader("Date");
        }
    }

}
