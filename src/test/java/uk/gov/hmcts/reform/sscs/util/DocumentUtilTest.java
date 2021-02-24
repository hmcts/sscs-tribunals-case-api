package uk.gov.hmcts.reform.sscs.util;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.AudioVideoUploadParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@RunWith(JUnitParamsRunner.class)
public class DocumentUtilTest {

    @Test
    @Parameters({"true,test.mp3", "true,test.mp4", "false,test.pdf"})
    public void isFileAMediaTest(boolean isMediaFile, String fileName) {
        Assert.assertEquals(isMediaFile, DocumentUtil.isFileAMedia(DocumentLink.builder().documentUrl("Test").documentFilename(fileName).build()));
    }

    @Test
    @Parameters(method = "generateData")
    public void getUploaderTest(List<String> roles, AudioVideoUploadParty uploader){
        Assert.assertEquals(uploader, DocumentUtil.getUploader(roles));
    }

    private Object[] generateData() {
        return new Object[]{
                new Object[] {Arrays.asList("caseworker-sscs-dwpresponsewriter", "caseworker-sscs", "caseworker-sscs-superuser"), AudioVideoUploadParty.DWP},
                new Object[] {Arrays.asList("caseworker-sscs", "caseworker-sscs-superuser", "caseworker-sscs-clerk"), AudioVideoUploadParty.CTSC},
                new Object[] {Arrays.asList("appellant"), AudioVideoUploadParty.APPELLANT},
                new Object[] {Arrays.asList("rep"), AudioVideoUploadParty.REP},
                new Object[] {Arrays.asList("jointParty"), AudioVideoUploadParty.JOINT_PARTY}
        };
    }
}
