package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.service.PreviewDocumentService;

@ExtendWith(MockitoExtension.class)
class AdjournCaseAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
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

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "isAdjournmentEnabled", true);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().build())
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .hearingRoute(HearingRoute.GAPS)
                .build())
            .build();
    }

    @Nested
    class Main {

        @BeforeEach
        void setup() {
            when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
            when(callback.getCaseDetails()).thenReturn(caseDetails);
            when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        }

        @DisplayName("Given draft adjournment notice already exists on case, then overwrite existing draft")
        @Test
        void givenAdjournmentNoticeAlreadyExistsOnCase_thenOverwriteExistingDraft() {
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
        void givenAdjournmentEventWithLanguageInterpreterRequiredAndCaseHasExistingInterpreter_overwriteExistingInterpreter() {
            callback.getCaseDetails().getCaseData().getAdjournment().setInterpreterRequired(YES);
            callback.getCaseDetails().getCaseData().getAdjournment().setInterpreterLanguage(SPANISH);
            callback.getCaseDetails().getCaseData().getAppeal().setHearingOptions(HearingOptions.builder()
                .languageInterpreter(NO.getValue())
                .languages("French")
                .build());

            PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

            assertThat(response.getData().getAppeal().getHearingOptions().getLanguageInterpreter()).isEqualTo(YES.getValue());
            assertThat(response.getData().getAppeal().getHearingOptions().getLanguages()).isEqualTo(SPANISH);
        }

        @DisplayName("Given an adjournment event with language interpreter required and interpreter language set, "
            + "then do not display error")
        @Test
        void givenAdjournmentEventWithLanguageInterpreterRequiredAndLanguageSet_thenDoNotDisplayError() {
            callback.getCaseDetails().getCaseData().getAdjournment().setInterpreterRequired(YES);
            callback.getCaseDetails().getCaseData().getAdjournment().setInterpreterLanguage(SPANISH);

            PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

            assertThat(response.getData().getAppeal().getHearingOptions().getLanguageInterpreter()).isEqualTo(YES.getValue());
            assertThat(response.getData().getAppeal().getHearingOptions().getLanguages()).isEqualTo(SPANISH);
        }


        @DisplayName("When adjournment is disabled and case is LA, then should not send any messages")
        @Test
        void givenFeatureFlagDisabled_thenNoMessageIsSent() {
            ReflectionTestUtils.setField(handler, "isAdjournmentEnabled", false);
            sscsCaseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);

            PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

            verifyNoInteractions(hearingMessageHelper);

            assertThat(response.getErrors()).isEmpty();
        }

        @DisplayName("When adjournment is enabled and case is LA and case cannot be listed right away "
            + "and no directions are being made, then should send a new hearing request in hearings API")
        @Test
        void givenCaseCannotBeListedRightAwayAndNoDirectionsBeingMade_thenNewHearingRequestSent() {
            PreSubmitCallbackResponse<SscsCaseData> response = cannotBeListedAndNoDirectionsGiven();

            assertHearingCreatedAndAdjournmentInProgress(response);
        }

        @DisplayName("When adjournment is enabled and case is LA and case can be listed right away "
            + "then should send a new hearing request in hearings API")
        @Test
        void givenCanBeListedRightAway_thenNewHearingRequestSent() {
            PreSubmitCallbackResponse<SscsCaseData> response = canBeListed();

            assertHearingCreatedAndAdjournmentInProgress(response);
        }

        private void assertHearingCreatedAndAdjournmentInProgress(PreSubmitCallbackResponse<SscsCaseData> response) {
            verify(hearingMessageHelper, times(1))
                .sendListAssistCreateHearingMessage(sscsCaseData.getCcdCaseId());

            assertThat(response.getErrors()).isEmpty();
            assertThat(response.getData().getAdjournment().getIsAdjournmentInProgress()).isEqualTo(YES);
        }

        @DisplayName("When adjournment is enabled and case is LA and case cannot be listed right away "
            + "and directions are being made, then should not send any messages")
        @Test
        void givenCaseCannotBeListedRightAwayAndDirectionsAreBeingMade_thenNoMessagesSent() {
            PreSubmitCallbackResponse<SscsCaseData> response = cannotBeListedAndDirectionsGiven();

            verifyNoInteractions(hearingMessageHelper);

            assertThat(response.getErrors()).isEmpty();
        }
    }

    @Nested
    class Other {

        @DisplayName("Given a non callback type, then return false")
        @ParameterizedTest
        @ValueSource(strings = {"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
        void givenNonCallbackType_thenReturnFalse(CallbackType callbackType) {
            assertThat(handler.canHandle(callbackType, callback)).isFalse();
        }

        @DisplayName("Throws exception if it cannot handle the appeal")
        @Test
        void givenCannotHandleAppeal_thenThrowsException() {
            when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
            assertThatThrownBy(() -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION))
                .isInstanceOf(IllegalStateException.class);
        }

        @DisplayName("Given a non adjourn case event, then return false")
        @Test
        void givenNonAdjournCaseEvent_thenReturnFalse() {
            when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
            assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
        }

        @DisplayName("Given caseDetails is null, then return false")
        @Test
        void givenCaseDetailsIsNull_thenReturnFalse() {
            assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
        }

        @DisplayName("Given caseData is null, then return false")
        @Test
        void givenCaseDataIsNull_thenReturnFalse() {
            lenient().when(callback.getCaseDetails()).thenReturn(caseDetails);
            assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
        }
    }


    private PreSubmitCallbackResponse<SscsCaseData> cannotBeListedAndNoDirectionsGiven() {
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);
        sscsCaseData.getAdjournment().setCanCaseBeListedRightAway(NO);
        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(NO);

        return handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    private PreSubmitCallbackResponse<SscsCaseData> canBeListed() {
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);
        sscsCaseData.getAdjournment().setCanCaseBeListedRightAway(YES);
        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(NO);

        return handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    private PreSubmitCallbackResponse<SscsCaseData> cannotBeListedAndDirectionsGiven() {
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);
        sscsCaseData.getAdjournment().setCanCaseBeListedRightAway(NO);
        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(YES);

        return handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }
}
