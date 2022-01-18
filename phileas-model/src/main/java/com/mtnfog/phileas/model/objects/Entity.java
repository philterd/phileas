package com.mtnfog.phileas.model.objects;

import com.mtnfog.phileas.model.enums.FilterType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.LinkedList;
import java.util.List;

public class Entity {

    private int characterStart;
    private int characterEnd;
    private FilterType filterType = FilterType.PERSON;
    private String context;
    private String documentId;
    private String text;
    private double confidence;

    public Entity(int characterStart, int characterEnd, FilterType filterType, String context, String documentId, String text, double confidence) {

        this.characterStart = characterStart;
        this.characterEnd = characterEnd;
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
                + " filterType: " + filterType.getType() + "; "
                + " context: " + context + "; "
                + " documentId: " + documentId + "; "
                + " confidence: " + confidence + "; "
                + " text: " + text + "; "
                ;

    }

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof Entity)) {
            return false;
        }

        Entity other = (Entity) obj;

        EqualsBuilder builder = new EqualsBuilder();
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

    private static String getSpaces(int numberOfSpaces) {

        final StringBuilder sb = new StringBuilder();

        for(int x = 1; x <= numberOfSpaces; x ++) {
            sb.append(" ");
        }

        return sb.toString();

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

}
