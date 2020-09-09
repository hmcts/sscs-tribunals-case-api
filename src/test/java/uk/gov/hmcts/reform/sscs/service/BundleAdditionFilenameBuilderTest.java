package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;

public class BundleAdditionFilenameBuilderTest {

    BundleAdditionFilenameBuilder bundleAdditionFilenameBuilder = new BundleAdditionFilenameBuilder();


    @Test
    public void shouldBuildFileName() {
        String result = bundleAdditionFilenameBuilder.build(DocumentType.APPELLANT_EVIDENCE, "A", "2020-08-29T00:00:00.000");
        assertEquals("Addition A - Appellant evidence received on 29-08-2020", result);
    }

    @Test
    public void shouldBuildFileNameWithNoBundleAddition() {
        String result = bundleAdditionFilenameBuilder.build(DocumentType.APPELLANT_EVIDENCE, null, "2020-08-29T00:00:00.000");
        assertEquals("Appellant evidence received on 29-08-2020", result);
    }

}