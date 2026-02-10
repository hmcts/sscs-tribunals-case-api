package uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessRequestAction.GRANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessRequestAction.REFUSE;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderUtility.getPostponementRequestStatus;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public class PlaceholderUtilityTest {
    private SscsCaseData caseData = buildCaseData();

    @Test
    public void whenNotAHearingPostponementRequest_thenGetPostponementRequestStatusReturnEmptyString() {
        String response = getPostponementRequestStatus(caseData);

        assertEquals("", getPostponementRequestStatus(caseData));
    }

    @Test
    public void givenAGrantedHearingPostponementRequest_GetPostponementRequestStatusReturnGrant() {
        caseData.setPostponementRequest(uk.gov.hmcts.reform.sscs.ccd.domain.PostponementRequest.builder().actionPostponementRequestSelected(GRANT.getValue()).build());

        assertEquals("grant", getPostponementRequestStatus(caseData));
    }

    @Test
    public void givenARefusedHearingPostponementRequest_GetPostponementRequestStatusReturnRefuse() {
        caseData.setPostponementRequest(uk.gov.hmcts.reform.sscs.ccd.domain.PostponementRequest.builder().actionPostponementRequestSelected(REFUSE.getValue()).build());

        assertEquals("refuse", getPostponementRequestStatus(caseData));
    }
}
