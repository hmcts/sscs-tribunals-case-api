package uk.gov.hmcts.reform.sscs.util;

import java.util.Arrays;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.AudioVideoUploadParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;


@RunWith(JUnitParamsRunner.class)
public class DocumentUtilTest {

    @Test
    @Parameters({"true,test.mp3", "true,test.mp4", "false,test.pdf"})
    public void isFileAMediaTest(boolean isMediaFile, String fileName) {
        Assert.assertEquals(isMediaFile, DocumentUtil.isFileAMedia(DocumentLink.builder().documentUrl("Test").documentFilename(fileName).build()));
    }

    @Test
    @Parameters(method = "generateData")
    public void getUploaderTest(UserDetails userDetails, AudioVideoUploadParty uploader) {
        Assert.assertEquals(uploader, DocumentUtil.getUploader(userDetails));
    }

    private Object[] generateData() {
        return new Object[] {
            new Object[] {UserDetails.builder().roles(Arrays.asList("caseworker-sscs-dwpresponsewriter", "caseworker-sscs", "caseworker-sscs-superuser")).build(), AudioVideoUploadParty.DWP},
            new Object[] {UserDetails.builder().roles(Arrays.asList("caseworker-sscs", "caseworker-sscs-superuser", "caseworker-sscs-clerk")).build(), AudioVideoUploadParty.CTSC},
            new Object[] {UserDetails.builder().roles(Arrays.asList("caseworker-sscs", "caseworker-sscs-superuser", "caseworker-sscs-systemuser")).build(), null},
            new Object[] {UserDetails.builder().build(), null},
            new Object[] {null, null},
        };
    }
}
