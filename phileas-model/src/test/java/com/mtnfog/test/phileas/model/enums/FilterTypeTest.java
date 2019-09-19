package com.mtnfog.test.phileas.model.enums;

import com.mtnfog.phileas.model.enums.FilterType;
import org.junit.Assert;
import org.junit.Test;

public class FilterTypeTest {

    @Test
    public void test1() {

        FilterType filterType = FilterType.CREDIT_CARD;
        Assert.assertEquals("credit-card", filterType.getType());
        Assert.assertEquals(filterType.getType(), filterType.toString());

    }

}
