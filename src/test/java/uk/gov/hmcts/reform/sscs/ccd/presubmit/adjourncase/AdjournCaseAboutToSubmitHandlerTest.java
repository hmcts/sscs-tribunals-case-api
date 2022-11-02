package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.service.PreviewDocumentService;

@RunWith(JUnitParamsRunner.class)
public class AdjournCaseAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    public static final String FRENCH = "French";
    public static final String SPANISH = "Spanish";
    public static final String OLD_DRAFT_DOC = "oldDraft.doc";

    @InjectMocks
    private AdjournCaseAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private ListAssistHearingMessageHelper hearingMessageHelper;

    @SuppressWarnings("unused")
    @Mock
    private PreviewDocumentService previewDocumentService;

    private SscsCaseData sscsCaseData;
    private AutoCloseable autoCloseable;

    @Before
    public void setUp() {
        autoCloseable = openMocks(this);
        ReflectionTestUtils.setField(handler, "isAdjournmentEnabled", true);

        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().build())
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .hearingRoute(HearingRoute.GAPS)
                .build())
            .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @After
    public void tearDown() throws Exception {
        autoCloseable.close();
    }

    @DisplayName("Given a non adjourn case event, then return false")
    @Test
    public void givenNonAdjournCaseEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @DisplayName("Given draft adjournment notice already exists on case, then overwrite existing draft")
    @Test
    public void givenAdjournmentNoticeAlreadyExistsOnCase_thenOverwriteExistingDraft() {
        SscsDocument doc = SscsDocument.builder().value(
            SscsDocumentDetails.builder()
                .documentFileName(OLD_DRAFT_DOC)
                .documentType(DRAFT_ADJOURNMENT_NOTICE.getValue())
                .build())
            .build();
        List<SscsDocument> docs = new ArrayList<>();
        docs.add(doc);
        callback.getCaseDetails().getCaseData().setSscsDocument(docs);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(previewDocumentService, times(1)).writePreviewDocumentToSscsDocument(
            sscsCaseData, DRAFT_ADJOURNMENT_NOTICE, null);
    }

    @DisplayName("Given an adjournment event with language interpreter required and case has existing interpreter, "
        + "then overwrite existing interpreter in hearing options")
    @Test
    public void givenAdjournmentEventWithLanguageInterpreterRequiredAndCaseHasExistingInterpreter_overwriteExistingInterpreter() {
        callback.getCaseDetails().getCaseData().setAdjournCaseInterpreterRequired(YES.getValue());
        callback.getCaseDetails().getCaseData().setAdjournCaseInterpreterLanguage(SPANISH);
        callback.getCaseDetails().getCaseData().getAppeal().setHearingOptions(HearingOptions.builder()
            .languageInterpreter(NO.getValue())
            .languages(FRENCH)
            .build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getAppeal().getHearingOptions().getLanguageInterpreter()).isEqualTo(YES.getValue());
        assertThat(response.getData().getAppeal().getHearingOptions().getLanguages()).isEqualTo(SPANISH);
    }

    @DisplayName("Given an adjournment event with language interpreter required and interpreter language set, "
        + "then do not display error")
    @Test
    public void givenAdjournmentEventWithLanguageInterpreterRequiredAndLanguageSet_thenDoNotDisplayError() {
        callback.getCaseDetails().getCaseData().setAdjournCaseInterpreterRequired(YES.getValue());
        callback.getCaseDetails().getCaseData().setAdjournCaseInterpreterLanguage(SPANISH);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getAppeal().getHearingOptions().getLanguageInterpreter()).isEqualTo(YES.getValue());
        assertThat(response.getData().getAppeal().getHearingOptions().getLanguages()).isEqualTo(SPANISH);
    }

    @DisplayName("Given a non callback type, then return false")
    @ParameterizedTest
    @ValueSource(strings = {"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    void givenNonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertThat(handler.canHandle(callbackType, callback)).isFalse();
    }

    @DisplayName("Throws exception if it cannot handle the appeal")
    @Test(expected = IllegalStateException.class)
    public void givenCannotHandleAppeal_thenThrowsException() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    @DisplayName("When adjournment is disabled and case is LA, then should not send any messages")
    @Test
    public void givenFeatureFlagDisabled_thenNoMessageIsSent() {
        ReflectionTestUtils.setField(handler, "isAdjournmentEnabled", false);
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(HearingRoute.LIST_ASSIST);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verifyNoInteractions(hearingMessageHelper);

        assertThat(response.getErrors()).isEmpty();
    }

    @DisplayName("When adjournment is enabled and case is LA and case cannot be listed right away "
        + "and no directions are being made, then should send a new hearing request in hearings API")
    @Test
    public void givenCaseCannotBeListedRightAwayAndNoDirectionsBeingMade_thenNewHearingRequestSent() {
        PreSubmitCallbackResponse<SscsCaseData> response = cannotBeListedAndNoDirectionsGiven();

        assertHearingCreated(response);
    }

    @DisplayName("When adjournment is enabled and case is LA and case can be listed right away "
        + "then should send a new hearing request in hearings API")
    @Test
    public void givenCanBeListedRightAway_thenNewHearingRequestSent() {
        PreSubmitCallbackResponse<SscsCaseData> response = canBeListed();

        assertHearingCreated(response);
    }

    @DisplayName("When adjournment is enabled and case is LA and case cannot be listed right away "
        + "and directions are being made, then should not send any messages")
    @Test
    public void givenCaseCannotBeListedRightAwayAndDirectionsAreBeingMade_thenNoMessagesSent() {
        PreSubmitCallbackResponse<SscsCaseData> response = cannotBeListedAndDirectionsGiven();

        verifyNoInteractions(hearingMessageHelper);

        assertThat(response.getErrors()).isEmpty();
    }

    private PreSubmitCallbackResponse<SscsCaseData> cannotBeListedAndNoDirectionsGiven() {
        return getResponseWithYesNoCanBeListedAndYesNoDirections(NO, NO);
    }

    private void assertHearingCreated(PreSubmitCallbackResponse<SscsCaseData> response) {
        verify(hearingMessageHelper, times(1))
            .sendListAssistCreateHearingMessage(sscsCaseData.getCcdCaseId());

        assertThat(response.getErrors()).isEmpty();
    }

    private PreSubmitCallbackResponse<SscsCaseData> canBeListed() {
        return getResponseWithYesNoCanBeListedAndYesNoDirections(YES, NO);
    }

    private PreSubmitCallbackResponse<SscsCaseData> cannotBeListedAndDirectionsGiven() {
        return getResponseWithYesNoCanBeListedAndYesNoDirections(NO, YES);
    }

    private PreSubmitCallbackResponse<SscsCaseData> getResponseWithYesNoCanBeListedAndYesNoDirections(
        YesNo canBeListedRightAway,
        YesNo directionsBeingMade
    ) {
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(HearingRoute.LIST_ASSIST);
        sscsCaseData.setAdjournCaseCanCaseBeListedRightAway(canBeListedRightAway.getValue());
        sscsCaseData.setAdjournCaseAreDirectionsBeingMadeToParties(directionsBeingMade.getValue());

        return handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }
}
