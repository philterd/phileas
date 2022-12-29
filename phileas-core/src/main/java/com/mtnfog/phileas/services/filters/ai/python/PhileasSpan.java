package com.mtnfog.phileas.services.filters.ai.python;

import com.google.gson.Gson;

public class PhileasSpan {

    private String text;
    private String tag;
    private double score;
    private int start;
    private int end;

    public PhileasSpan() {

    }

    public PhileasSpan(String text, String tag, double score, int start, int end) {

        this.text = text;
        this.tag = tag;
        this.score = score;
        this.start = start;
        this.end = end;

    }

    @Override
    public String toString() {

        final Gson gson = new Gson();
        return gson.toJson(this);

    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

}