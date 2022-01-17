package com.mtnfog.phileas.service.ai;

public class SpanEnd {

    private int index;
    private int characterEnd;

    public SpanEnd(int index, int characterEnd) {
        this.index = index;
        this.characterEnd = characterEnd;
    }

    public int getIndex() {
        return index;
    }

    public int getCharacterEnd() {
        return characterEnd;
    }

}
