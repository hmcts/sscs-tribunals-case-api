package uk.gov.hmcts.reform.sscs.util;

import static uk.gov.hmcts.reform.sscs.util.DocumentUtil.stripUrl;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;


@RunWith(JUnitParamsRunner.class)
public class DocumentUtilTest {

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"true,test.mp3", "true,test.mp4", "false,test.pdf"})
    public void isFileAMediaTest(boolean isMediaFile, String fileName) {
        Assertions.assertEquals(isMediaFile, DocumentUtil.isFileAMedia(DocumentLink.builder().documentUrl("Test").documentFilename(fileName).build()));
    }

    @Test
    public void shouldStripBinaryUrlAndReturnId() {
        Assertions.assertEquals("6d3b7351-168e-42f6-90b7-d263000e1864", stripUrl("http://dm-store:5005/documents/6d3b7351-168e-42f6-90b7-d263000e1864/binary"));
    }

    @Test
    public void shouldReturnSameUrlIfNotInRightFormatMissingDocument() {
        String url = "http://dm-store:5005/6d3b7351-168e-42f6-90b7-d263000e1864/binary";
        Assertions.assertEquals(url, stripUrl(url));
    }

    @Test
    public void shouldReturnSameUrlIfNotInRightFormatMissingBinary() {
        String url = "http://dm-store:5005/documents/6d3b7351-168e-42f6-90b7-d263000e1864";
        Assertions.assertEquals(url, stripUrl(url));
    }

    @Test
    public void shouldReturnSameUrlIfNotInRightFormatMissingBoth() {
        String url = "http://dm-store:5005";
        Assertions.assertEquals(url, stripUrl(url));
    }
}
