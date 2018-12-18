package uk.gov.hmcts.reform.sscs.util;

import org.junit.Assert;
import org.junit.Test;

public class NormTest {

    @Test
    public void shouldRemoveSpaceFromPipNumber() {

        Assert.assertEquals("DWP PIP (10)", Norm.dwpIssuingOffice("DWP PIP ( 10)"));
        Assert.assertEquals("DWP PIP (10)", Norm.dwpIssuingOffice("DWP PIP (  10)"));
        Assert.assertEquals("DWP PIP (10)", Norm.dwpIssuingOffice("DWP PIP ( 10 )"));
        Assert.assertEquals("DWP PIP (10)", Norm.dwpIssuingOffice("DWP PIP (\t \t 10\t \t)"));
        Assert.assertEquals("DWP PIP (10)", Norm.dwpIssuingOffice("DWP PIP (10)"));
        Assert.assertEquals("ABC", Norm.dwpIssuingOffice("ABC"));
    }
}
