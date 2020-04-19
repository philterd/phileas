package com.mtnfog.phileas.model.objects;

public class GetFilterProfileResult {

    private String filterProfileJson;
    private boolean requiresReload;

    public GetFilterProfileResult(String filterProfileJson, boolean requiresReload) {

        this.filterProfileJson = filterProfileJson;
        this.requiresReload = requiresReload;

    }

    public String getFilterProfileJson() {
        return filterProfileJson;
    }

    public boolean isRequiresReload() {
        return requiresReload;
    }

}
