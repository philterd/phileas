package com.mtnfog.phileas.service.ai.onnx;

public class SpanEnd {

    private int index;
    private int characterEnd;

    public SpanEnd(int index, int characterEnd) {
        this.index = index;
        this.characterEnd = characterEnd;
    }

    @Override
    public String toString() {
        return "index: " + index + "; character end: " + characterEnd;
    }

    public int getIndex() {
        return index;
    }

    public int getCharacterEnd() {
        return characterEnd;
    }

}
