package com.mtnfog.phileas.model.objects;

public class PostFilterResult {

    private boolean isPostFiltered = false;

    public PostFilterResult(boolean isPostFiltered) {
        this.isPostFiltered = isPostFiltered;
    }

    public boolean isPostFiltered() {
        return isPostFiltered;
    }

    public void setPostFiltered(boolean postFiltered) {
        isPostFiltered = postFiltered;
    }

}
