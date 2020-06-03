package uk.gov.hmcts.reform.sscs.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.Map;
import org.junit.Test;

public class I18nBuilderTest {
    @Test
    public void canLoadI18n() throws IOException {
        Map i18n = new I18nBuilder().build();

        assertThat(i18n.containsKey("tribunalView"), is(true));
    }

}
