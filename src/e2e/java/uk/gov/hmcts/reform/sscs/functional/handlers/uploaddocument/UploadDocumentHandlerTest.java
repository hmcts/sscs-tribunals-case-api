package uk.gov.hmcts.reform.sscs.functional.handlers.uploaddocument;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandlerTest;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@SpringBootTest
public class UploadDocumentHandlerTest extends BaseHandlerTest {

    @Test
    public void givenUploadDocumentEventIsTriggered_shouldUploadDocument() throws IOException {
        caseDetails = createCaseInWithDwpStateUsingGivenCallback("handlers/uploaddocument/uploadDocumentCallback.json");

        SscsCaseDetails actualCase = ccdService.updateCase(caseDetails.getCaseData(), caseDetails.getId(),
            EventType.UPLOAD_DOCUMENT.getCcdType(), CREATED_BY_FUNCTIONAL_TEST, CREATED_BY_FUNCTIONAL_TEST, idamTokens);
        assertEquals(2, actualCase.getData().getSscsDocument().size());
        assertEquals(State.WITH_DWP.getId(), actualCase.getState());
        assertEquals("feReceived", actualCase.getData().getDwpState());
    }

}

