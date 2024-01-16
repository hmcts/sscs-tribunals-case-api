package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.PreviewDocumentService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;
import uk.gov.hmcts.reform.sscs.service.VenueDataLoader;

@ExtendWith(MockitoExtension.class)
abstract class AdjournCaseAboutToSubmitHandlerTestBase {

    protected static final String USER_AUTHORISATION = "Bearer token";
    protected static final String OLD_DRAFT_DOC = "oldDraft.doc";

    @Mock
    protected AdjournCaseAboutToSubmitHandler handler;

    @Mock
    protected Callback<SscsCaseData> callback;

    @Mock
    protected CaseDetails<SscsCaseData> caseDetails;

    @SuppressWarnings("unused")
    @Mock
    protected PreviewDocumentService previewDocumentService;

    @Mock
    protected UserDetailsService userDetailsService;

    @Mock
    protected VenueDataLoader venueDataLoader;

    protected SscsCaseData sscsCaseData;

    @BeforeEach
    protected void setUp() {
        handler = new AdjournCaseAboutToSubmitHandler(previewDocumentService, userDetailsService, true);
        ReflectionTestUtils.setField(handler, "isAdjournmentEnabled", true);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().hearingOptions(HearingOptions.builder().build()).build())
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .hearingRoute(HearingRoute.GAPS)
                .overrideFields(OverrideFields.builder().build())
                .build())
            .build();
    }
}
