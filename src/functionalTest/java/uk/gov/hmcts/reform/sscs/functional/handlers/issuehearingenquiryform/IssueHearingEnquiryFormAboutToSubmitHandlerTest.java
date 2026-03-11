package uk.gov.hmcts.reform.sscs.functional.handlers.issuehearingenquiryform;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.HEF_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_JUDGE;

import java.io.IOException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "CM_OTHER_PARTY_CONFIDENTIALITY_ENABLED", matches = "true")
class IssueHearingEnquiryFormAboutToSubmitHandlerTest extends BaseHandler {

    private static final String BASE_CASE_DATA = "handlers/issuehearingenquiryform/issueHearingEnquiryFormCallback.json";
    private static final String RESPONSE_DUE_DATE = LocalDate.now().plusDays(21).toString();

    @Test
    void shouldHandleIssueHearingEnquiryFormAboutToSubmit() throws IOException {
        final Callback<SscsCaseData> sscsCaseDataCallback = deserializeCallbackData(BASE_CASE_DATA);
        assertThat(sscsCaseDataCallback.getCaseDetails().getCaseData().getDirectionDueDate()).isNull();
        assertThat(sscsCaseDataCallback.getCaseDetails().getCaseData().getInterlocReviewState()).isNull();

        final SscsCaseData caseData = callAboutToSubmitEndpoint(sscsCaseDataCallback);

        assertThatCaseUpdatedWithHEFData(caseData);
    }

    @Test
    void shouldResetDirectionDueDateTo21DaysInFuture() throws IOException {
        final Callback<SscsCaseData> sscsCaseDataCallback = deserializeCallbackData(BASE_CASE_DATA);
        sscsCaseDataCallback.getCaseDetails().getCaseData().setDirectionDueDate("2025-05-24");
        sscsCaseDataCallback.getCaseDetails().getCaseData().setInterlocReviewState(REVIEW_BY_JUDGE);

        final SscsCaseData caseData = callAboutToSubmitEndpoint(sscsCaseDataCallback);

        assertThatCaseUpdatedWithHEFData(caseData);
    }

    private static void assertThatCaseUpdatedWithHEFData(SscsCaseData caseData) {
        assertThat(caseData.getInterlocReviewState()).isEqualTo(HEF_ISSUED);
        assertThat(caseData.getDirectionDueDate()).isEqualTo(RESPONSE_DUE_DATE);
    }

}
