package com.mtnfog.test.phileas.model.enums;

import com.mtnfog.phileas.model.enums.FilterType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FilterTypeTest {

    @Test
    public void test1() {

        FilterType filterType = FilterType.CREDIT_CARD;
        Assertions.assertEquals("credit-card", filterType.getType());
        Assertions.assertEquals(filterType.getType(), filterType.toString());

    }

}
