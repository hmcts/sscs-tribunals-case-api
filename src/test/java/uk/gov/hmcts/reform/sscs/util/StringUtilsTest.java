package uk.gov.hmcts.reform.sscs.util;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

public class StringUtilsTest {

    @Test
    public void testEmptyList() {
        String result = StringUtils.getGramaticallyJoinedStrings(new ArrayList<>());
        Assert.assertEquals("", result);
    }

    @Test
    public void testSingleValuedList() {
        String result = StringUtils.getGramaticallyJoinedStrings(Arrays.asList("one"));
        Assert.assertEquals("one", result);
    }

    @Test
    public void testTwoValuesInList() {
        String result = StringUtils.getGramaticallyJoinedStrings(Arrays.asList("one", "two"));
        Assert.assertEquals("one and two", result);
    }

    @Test
    public void testThreeValuesInList() {
        String result = StringUtils.getGramaticallyJoinedStrings(Arrays.asList("one", "two", "three"));
        Assert.assertEquals("one, two and three", result);
    }

    @Test
    public void testFourValuesInList() {
        String result = StringUtils.getGramaticallyJoinedStrings(Arrays.asList("one", "two", "three", "four"));
        Assert.assertEquals("one, two, three and four", result);
    }

}
