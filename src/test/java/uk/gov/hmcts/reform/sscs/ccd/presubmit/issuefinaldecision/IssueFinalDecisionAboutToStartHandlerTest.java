package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsFinalDecisionCaseData;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;

@ExtendWith(MockitoExtension.class)
public class IssueFinalDecisionAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private IssueFinalDecisionAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private DecisionNoticeService decisionNoticeService;

    private SscsCaseData sscsCaseData;

    @BeforeEach
    void setup() {
        handler = new IssueFinalDecisionAboutToStartHandler(decisionNoticeService, true, true);

        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                .writeFinalDecisionGeneratedDate("2018-01-01")
                .writeFinalDecisionGenerateNotice(NO)
                .writeFinalDecisionPreviewDocument(DocumentLink.builder().documentFilename("filename").build())
                .build())
            .build();
    }

    @Test
    void whenGenerateNoticeIsNo_thenDoNotGeneratePreviewNotice() {
        when(callback.getEvent()).thenReturn(EventType.ISSUE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        assertDoesNotThrow(() -> handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION));
    }
}
