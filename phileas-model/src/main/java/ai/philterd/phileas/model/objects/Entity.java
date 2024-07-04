/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.model.objects;

import ai.philterd.phileas.model.enums.FilterType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.LinkedList;
import java.util.List;

public class Entity {

    private final int characterStart;
    private final int characterEnd;
    private int tokenStart;
    private int tokenEnd;
    private FilterType filterType = FilterType.PERSON;
    private final String context;
    private final String documentId;
    private final String text;
    private final double confidence;

    public Entity(final int characterStart, final int characterEnd, final FilterType filterType, final String context,
                  final String documentId, final String text, final double confidence) {

        this.characterStart = characterStart;
        this.characterEnd = characterEnd;
        this.filterType = filterType;
        this.context = context;
        this.documentId = documentId;
        this.text = text;
        this.confidence = confidence;

    }

    public Entity(final int characterStart, final int characterEnd, final int tokenStart, final int tokenEnd,
                  final FilterType filterType, final String context, final String documentId, final String text,
                  final double confidence) {

        this.characterStart = characterStart;
        this.characterEnd = characterEnd;
        this.tokenStart = tokenStart;
        this.tokenEnd = tokenEnd;
        this.filterType = filterType;
        this.context = context;
        this.documentId = documentId;
        this.text = text;
        this.confidence = confidence;

    }

    /**
     * Combines entities in a list that are adjacent to each other.
     * @param entities A list of entities.
     * @return A list of entities where adjacent entities have been combined.
     */
    public static List<Entity> combineAdjacentEntities(final List<Entity> entities) {

        final int numberOfSpaces = 1;

        final List<Entity> combinedEntities = new LinkedList<>();
        final List<Entity> entitiesToRemove = new LinkedList<>();

        // [INFO ] 2022-01-18 08:43:17.580 [main] OnnxNerTest - characterStart: 1174;  characterEnd: 1182;  filterType: person;  context: context;  documentId: documentId;  confidence: 0.626160740852356;  text: Reverend;
        // [INFO ] 2022-01-18 08:43:17.581 [main] OnnxNerTest - characterStart: 1183;  characterEnd: 1187;  filterType: person;  context: context;  documentId: documentId;  confidence: 0.4564152657985687;  text: King;

        for(final Entity entity1 : entities) {

            for(final Entity entity2 : entities) {

                // Does entity2 start immediately after entity1?
                if((entity1.getCharacterEnd() + numberOfSpaces == entity2.getCharacterStart())
                    && (entity1.getFilterType() == entity2.getFilterType())) {

                    // Average the confidence.
                    final double confidence = (entity1.getConfidence() + entity2.getConfidence()) / 2.0;

                    // Make a new entity for the combined entity.
                    final Entity entity = new Entity(
                            entity1.getCharacterStart(),
                            entity2.getCharacterEnd(),
                            entity1.getFilterType(),
                            entity1.getContext(),
                            entity1.getDocumentId(),
                            entity1.getText() + getSpaces(numberOfSpaces) + entity2.getText(),
                            confidence);

                    // Add it to the list of entities.
                    combinedEntities.add(entity);

                    entitiesToRemove.add(entity1);
                    entitiesToRemove.add(entity2);

                    break;

                }

            }

        }

        entities.addAll(combinedEntities);
        entities.removeAll(entitiesToRemove);

        return entities;

    }

    @Override
    public String toString() {

        return "characterStart: " + characterStart + "; "
                + " characterEnd: " + characterEnd + "; "
                + " tokenStart: " + tokenStart + "; "
                + " tokenEnd: " + tokenEnd + "; "
                + " filterType: " + filterType.getType() + "; "
                + " context: " + context + "; "
                + " documentId: " + documentId + "; "
                + " confidence: " + confidence + "; "
                + " text: " + text + "; "
                ;

    }

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof Entity other)) {
            return false;
        }

        final EqualsBuilder builder = new EqualsBuilder();
        builder.append(characterStart, other.characterStart);
        builder.append(characterEnd, other.characterEnd);
        builder.append(filterType, other.filterType);
        builder.append(context, other.context);
        builder.append(documentId, other.documentId);
        builder.append(confidence, other.confidence);
        builder.append(text, other.text);

        return builder.isEquals();

    }

    @Override
    public int hashCode() {

        return new HashCodeBuilder(17, 37).
                append(characterStart).
                append(characterEnd).
                append(filterType).
                append(context).
                append(documentId).
                append(confidence).
                append(text).
                toHashCode();

    }

    private static String getSpaces(final int numberOfSpaces) {
        return " ".repeat(Math.max(0, numberOfSpaces));
    }

    public int getCharacterStart() {
        return characterStart;
    }

    public int getCharacterEnd() {
        return characterEnd;
    }

    public FilterType getFilterType() {
        return filterType;
    }

    public String getContext() {
        return context;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getText() {
        return text;
    }

    public double getConfidence() {
        return confidence;
    }

    public int getTokenStart() {
        return tokenStart;
    }

    public void setTokenStart(int tokenStart) {
        this.tokenStart = tokenStart;
    }

    public int getTokenEnd() {
        return tokenEnd;
    }

    public void setTokenEnd(int tokenEnd) {
        this.tokenEnd = tokenEnd;
    }

}
