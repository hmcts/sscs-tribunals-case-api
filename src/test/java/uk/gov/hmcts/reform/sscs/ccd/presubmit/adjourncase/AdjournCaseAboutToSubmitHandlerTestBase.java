package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.PreviewDocumentService;

@ExtendWith(MockitoExtension.class)
abstract class AdjournCaseAboutToSubmitHandlerTestBase {

    protected static final String USER_AUTHORISATION = "Bearer token";
    protected static final String SPANISH = "Spanish";
    protected static final String OLD_DRAFT_DOC = "oldDraft.doc";

    @InjectMocks
    protected AdjournCaseAboutToSubmitHandler handler;

    @Mock
    protected Callback<SscsCaseData> callback;

    @Mock
    protected CaseDetails<SscsCaseData> caseDetails;

    @SuppressWarnings("unused")
    @Mock
    protected PreviewDocumentService previewDocumentService;

    protected SscsCaseData sscsCaseData;

    @BeforeEach
    protected void setUp() {
        ReflectionTestUtils.setField(handler, "isAdjournmentEnabled", true);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().build())
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .hearingRoute(HearingRoute.GAPS)
                .build())
            .build();
    }
}
