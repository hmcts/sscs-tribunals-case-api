package uk.gov.hmcts.reform.sscs.util;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;


@RunWith(JUnitParamsRunner.class)
public class DocumentUtilTest {

    @Test
    @Parameters({"true,test.mp3", "true,test.mp4", "false,test.pdf"})
    public void isFileAMediaTest(boolean isMediaFile, String fileName) {
        Assert.assertEquals(isMediaFile, DocumentUtil.isFileAMedia(DocumentLink.builder().documentUrl("Test").documentFilename(fileName).build()));
    }
}
