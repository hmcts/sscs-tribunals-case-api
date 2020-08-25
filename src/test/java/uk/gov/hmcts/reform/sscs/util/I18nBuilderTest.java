package uk.gov.hmcts.reform.sscs.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.ResourceManager;

public class I18nBuilderTest {

    I18nBuilder i18n;

    @Before
    public void setUp() {
        i18n = new I18nBuilder(new ResourceManager());
    }

    @Test
    public void canLoadI18n() throws IOException {
        Map i18nMap = i18n.build();

        assertThat(i18nMap.containsKey("tribunalView"), is(true));
    }

}
