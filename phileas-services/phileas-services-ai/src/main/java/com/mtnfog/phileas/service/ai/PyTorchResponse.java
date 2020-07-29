package com.mtnfog.phileas.service.ai;

import java.util.List;

public class PyTorchResponse {

    private String c;
    private String d;
    private int p;
    private List<PhileasSpan> spans;

    public String getC() {
        return c;
    }

    public void setC(String c) {
        this.c = c;
    }

    public String getD() {
        return d;
    }

    public void setD(String d) {
        this.d = d;
    }

    public int getP() {
        return p;
    }

    public void setP(int p) {
        this.p = p;
    }

    public List<PhileasSpan> getSpans() {
        return spans;
    }

    public void setSpans(List<PhileasSpan> spans) {
        this.spans = spans;
    }

}
