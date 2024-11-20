package uk.gov.hmcts.reform.sscs.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NormTest {

    @Test
    public void shouldRemoveWhiteSpaceFromPipNumber() {

        Assertions.assertEquals("DWP PIP (10)", Norm.dwpIssuingOffice("DWP PIP ( 10)"));
        Assertions.assertEquals("DWP PIP (10)", Norm.dwpIssuingOffice("DWP PIP (  10)"));
        Assertions.assertEquals("DWP PIP (10)", Norm.dwpIssuingOffice("DWP PIP ( 10 )"));
        Assertions.assertEquals("DWP PIP (10)", Norm.dwpIssuingOffice("DWP PIP (\t \t 10\t \t)"));
        Assertions.assertEquals("DWP PIP (10)", Norm.dwpIssuingOffice("DWP PIP (10)"));
    }

    @Test
    public void shouldKeepStringsUnchanged() {

        Assertions.assertEquals("ABC", Norm.dwpIssuingOffice("ABC"));
        Assertions.assertEquals("   ", Norm.dwpIssuingOffice("   "));
        Assertions.assertEquals("", Norm.dwpIssuingOffice(""));
    }

    @Test
    public void shouldHandleNull() {

        Assertions.assertEquals(null, Norm.dwpIssuingOffice(null));
    }
}
