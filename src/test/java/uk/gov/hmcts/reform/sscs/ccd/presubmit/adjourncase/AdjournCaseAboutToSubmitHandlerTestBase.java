package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCasePanelMembersExcluded;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberExclusions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUserBase;
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

    @Mock
    protected ListAssistHearingMessageHelper hearingMessageHelper;

    @SuppressWarnings("unused")
    @Mock
    protected PreviewDocumentService previewDocumentService;

    protected SscsCaseData sscsCaseData;

    @BeforeEach
    protected void setUp() {
        ReflectionTestUtils.setField(handler, "isAdjournmentEnabled", true);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .adjournment(Adjournment.builder()
                .panelMembersExcluded(AdjournCasePanelMembersExcluded.NO).build())
            .appeal(Appeal.builder().build())
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .hearingRoute(HearingRoute.GAPS)
                .build())
            .build();
    }

    protected PreSubmitCallbackResponse<SscsCaseData> cannotBeListedAndNoDirectionsGiven() {
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);
        sscsCaseData.getAdjournment().setCanCaseBeListedRightAway(NO);
        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(NO);

        return handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    protected PreSubmitCallbackResponse<SscsCaseData> canBeListed() {
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);
        sscsCaseData.getAdjournment().setCanCaseBeListedRightAway(YES);
        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(NO);

        return handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    protected PreSubmitCallbackResponse<SscsCaseData> cannotBeListedAndDirectionsGiven() {
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);
        sscsCaseData.getAdjournment().setCanCaseBeListedRightAway(NO);
        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(YES);

        return handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    protected void panelMembersGetExcluded(boolean areExistingExclusions) {
        if (areExistingExclusions) {
            sscsCaseData.getSchedulingAndListingFields().setPanelMemberExclusions(PanelMemberExclusions.builder()
                .excludedPanelMembers(new ArrayList<>(Arrays.asList(JudicialUserBase.builder().idamId("1").build(), JudicialUserBase.builder().idamId("2").build()))).build());
        }

        sscsCaseData.getAdjournment().setPanelMembersExcluded(AdjournCasePanelMembersExcluded.YES);
        sscsCaseData.getAdjournment().setPanelMember1(JudicialUserBase.builder().idamId("1").build());
        sscsCaseData.getAdjournment().setPanelMember3(JudicialUserBase.builder().idamId("3").build());
        sscsCaseData.getAdjournment().setCanCaseBeListedRightAway(NO);
        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(NO);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }
}
