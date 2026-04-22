package uk.gov.hmcts.reform.sscs.ccd.presubmit.validsendtointerloc;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_SEND_TO_INTERLOC;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase.POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase.REVIEW_BY_TCW;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostponementRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.ReissueArtifactUi;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.UploadParty;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.service.AddNoteService;
import uk.gov.hmcts.reform.sscs.service.PostponementRequestService;

@ExtendWith(MockitoExtension.class)
class ValidSendToInterlocAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private PostponementRequestService postponementRequestService;
    @Mock
    private AddNoteService addNoteService;

    private Callback<SscsCaseData> callback;
    private CaseDetails<SscsCaseData> caseDetails;
    private SscsCaseData sscsCaseData;

    private ValidSendToInterlocAboutToSubmitHandler handler;

    @BeforeEach
    void setUp() {
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
                .selectWhoReviewsCase(
                        new DynamicList(new DynamicListItem("reviewByTcw", "Review by TCW"), null))
                .directionDueDate("01/02/2020")
                .build();
        caseDetails = new CaseDetails<>(123L, "SSCS", READY_TO_LIST, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), VALID_SEND_TO_INTERLOC, false);

        handler = new ValidSendToInterlocAboutToSubmitHandler(postponementRequestService, addNoteService, false);
    }

    @ParameterizedTest
    @CsvSource({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    void givenANonHandleEvidenceEvent_thenReturnFalse(EventType eventType) {
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), eventType, false);

        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertThat(handler.canHandle(callbackType, callback)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"REVIEW_BY_TCW", "REVIEW_BY_JUDGE"})
    void setsEvidenceHandledFlagToNoForDocumentSelected(SelectWhoReviewsCase selectWhoReviewsCase) {
        sscsCaseData = sscsCaseData.toBuilder()
                .selectWhoReviewsCase(new DynamicList(
                        new DynamicListItem(selectWhoReviewsCase.getId(), selectWhoReviewsCase.getLabel()),
                        Arrays.asList(new DynamicListItem(REVIEW_BY_JUDGE.getId(), REVIEW_BY_JUDGE.getLabel()),
                                new DynamicListItem(REVIEW_BY_TCW.getId(), REVIEW_BY_TCW.getLabel()),
                                new DynamicListItem(POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getId(),
                                        POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getLabel()))
                ))
                .reissueArtifactUi(ReissueArtifactUi.builder()
                        .reissueFurtherEvidenceDocument(new DynamicList(new DynamicListItem(selectWhoReviewsCase.getId(),
                                selectWhoReviewsCase.getLabel()), null)).build())
                .build();
        caseDetails = new CaseDetails<>(123L, "SSCS", READY_TO_LIST, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), VALID_SEND_TO_INTERLOC, false);

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getInterlocReferralDate()).isEqualTo(LocalDate.now());
        assertThat(response.getData().getSelectWhoReviewsCase()).isNull();
        assertThat(response.getData().getDirectionDueDate()).isNull();
        assertThat(response.getData().getInterlocReviewState()).isNotNull()
            .extracting(InterlocReviewState::getCcdDefinition)
            .isEqualTo(selectWhoReviewsCase.getId());
        verify(addNoteService).addNote(eq(USER_AUTHORISATION), eq(response.getData()), eq(null));
    }


    @Test
    void givenPostponementRequestInterlocSendToTcw_thenReturnWarningMessageAboutPostponingHearing() {
        var caseData = setupDataForPostponementRequestInterlocSendToTcw(PartyItemList.APPELLANT);
        caseDetails = new CaseDetails<>(123L, "SSCS", READY_TO_LIST, caseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), VALID_SEND_TO_INTERLOC, false);

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings()).hasSize(1);
        assertThat(response.getWarnings().iterator().next()).isEqualTo("Are you sure you want to postpone the hearing?");
    }

    @ParameterizedTest
    @CsvSource({"APPELLANT", "REPRESENTATIVE", "JOINT_PARTY"})
    void givenPostponementRequestInterlocSendToTcw_setsEvidenceHandledFlagToNoForDocumentSelected(
            PartyItemList originalSenderParty) {
        var caseData = setupDataForPostponementRequestInterlocSendToTcw(originalSenderParty);
        caseData.setTempNoteDetail("Test Note");
        caseDetails = new CaseDetails<>(123L, "SSCS", READY_TO_LIST, caseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), VALID_SEND_TO_INTERLOC, true);

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        final UploadParty expectedUploadParty = originalSenderParty == PartyItemList.REPRESENTATIVE
            ? UploadParty.REP
            : UploadParty.fromValue(originalSenderParty.getCode());

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getInterlocReferralDate()).isEqualTo(LocalDate.now());
        assertThat(response.getData().getSelectWhoReviewsCase()).isNull();
        assertThat(response.getData().getDirectionDueDate()).isNull();
        verify(postponementRequestService).processPostponementRequest(any(), eq(expectedUploadParty), eq(Optional.empty()));
        verify(addNoteService).addNote(eq(USER_AUTHORISATION), eq(response.getData()), eq("Test Note"));
    }

    @ParameterizedTest
    @CsvSource({"VALID_SEND_TO_INTERLOC", "ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE"})
    void returnAnErrorIfNoSelectWhoReviewsCaseSelected(EventType eventType) {
        sscsCaseData = sscsCaseData.toBuilder().selectWhoReviewsCase(null).build();
        caseDetails = new CaseDetails<>(123L, "SSCS", READY_TO_LIST, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), eventType, true);

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).hasSize(1).contains("Must select who reviews the appeal.");
    }

    @ParameterizedTest
    @CsvSource({"VALID_SEND_TO_INTERLOC", "ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE"})
    void givenPostponementRequestInterlocSendToTcw_returnAnErrorIfNoOriginalSenderSelected(EventType eventType) {
        sscsCaseData = sscsCaseData.toBuilder().selectWhoReviewsCase(new DynamicList(
                        new DynamicListItem(POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getId(),
                                POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getLabel()),
                        Arrays.asList(new DynamicListItem(REVIEW_BY_JUDGE.getId(), REVIEW_BY_JUDGE.getLabel()),
                                new DynamicListItem(REVIEW_BY_TCW.getId(), REVIEW_BY_TCW.getLabel()),
                                new DynamicListItem(POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getId(),
                                        POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getLabel()))
                ))
                .originalSender(null).build();
        caseDetails = new CaseDetails<>(123L, "SSCS", READY_TO_LIST, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), eventType, false);

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).hasSize(1).contains("Must select original sender");
    }

    @Test
    void givenSelectWhoReviewsCaseWithNullValue_thenReturnError() {
        sscsCaseData = sscsCaseData.toBuilder()
            .selectWhoReviewsCase(new DynamicList(null, null))
            .build();
        caseDetails = new CaseDetails<>(123L, "SSCS", READY_TO_LIST, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), VALID_SEND_TO_INTERLOC, false);

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).hasSize(1).contains("Must select who reviews the appeal.");
    }

    @Test
    void givenSelectWhoReviewsCaseWithBlankCode_thenReturnError() {
        sscsCaseData = sscsCaseData.toBuilder()
            .selectWhoReviewsCase(new DynamicList(new DynamicListItem("", "label"), null))
            .build();
        caseDetails = new CaseDetails<>(123L, "SSCS", READY_TO_LIST, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), VALID_SEND_TO_INTERLOC, false);

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).hasSize(1).contains("Must select who reviews the appeal.");
    }

    @Test
    void givenCmConfidentialityEnabledAndConfidentialityReferralAndMissingSelectedParty_thenReturnError() {
        handler = new ValidSendToInterlocAboutToSubmitHandler(postponementRequestService, addNoteService, true);
        sscsCaseData = sscsCaseData.toBuilder()
            .interlocReferralReason(InterlocReferralReason.CONFIDENTIALITY)
            .build();
        caseDetails = new CaseDetails<>(123L, "SSCS", READY_TO_LIST, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), VALID_SEND_TO_INTERLOC, false);

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).hasSize(1).contains("Must select party");
    }

    @Test
    void givenCmConfidentialityEnabledAndConfidentialityReferralAndPartySelected_thenSucceed() {
        handler = new ValidSendToInterlocAboutToSubmitHandler(postponementRequestService, addNoteService, true);
        sscsCaseData = sscsCaseData.toBuilder()
            .interlocReferralReason(InterlocReferralReason.CONFIDENTIALITY)
            .build();
        sscsCaseData.getExtendedSscsCaseData().setSelectedConfidentialityParty(
            new DynamicList(new DynamicListItem("appellant", "Appellant"), null)
        );
        caseDetails = new CaseDetails<>(123L, "SSCS", READY_TO_LIST, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), VALID_SEND_TO_INTERLOC, false);

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getInterlocReferralDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void givenCmConfidentialityEnabledAndNonConfidentialityReferral_thenNoPartyCheckRequired() {
        handler = new ValidSendToInterlocAboutToSubmitHandler(postponementRequestService, addNoteService, true);
        sscsCaseData = sscsCaseData.toBuilder()
            .interlocReferralReason(InterlocReferralReason.COMPLEX_CASE)
            .build();
        caseDetails = new CaseDetails<>(123L, "SSCS", READY_TO_LIST, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), VALID_SEND_TO_INTERLOC, false);

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getInterlocReferralDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void throwsExceptionIfItCannotHandleTheAppeal() {
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), APPEAL_RECEIVED, false);

        assertThatThrownBy(() -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION))
                .isInstanceOf(IllegalStateException.class);
    }

    private SscsCaseData setupDataForPostponementRequestInterlocSendToTcw(
            PartyItemList originalSenderParty) {
        final DynamicListItem value = new DynamicListItem(originalSenderParty.getCode(), originalSenderParty.getLabel());
        final DynamicList originalSender = new DynamicList(value, Collections.singletonList(value));

        return SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
                .selectWhoReviewsCase(
                        new DynamicList(new DynamicListItem("reviewByTcw", "Review by TCW"), null))
                .directionDueDate("01/02/2020")
                .selectWhoReviewsCase(new DynamicList(
                        new DynamicListItem(POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getId(),
                                POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getLabel()),
                        Arrays.asList(new DynamicListItem(REVIEW_BY_JUDGE.getId(), REVIEW_BY_JUDGE.getLabel()),
                                new DynamicListItem(REVIEW_BY_TCW.getId(), REVIEW_BY_TCW.getLabel()),
                                new DynamicListItem(POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getId(),
                                        POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getLabel()))
                ))
                .postponementRequest(PostponementRequest.builder()
                        .postponementRequestDetails("Here are some details")
                        .postponementRequestHearingVenue("Venue 1")
                        .postponementPreviewDocument(DocumentLink.builder()
                                .documentBinaryUrl("http://example.com")
                                .documentFilename("example.pdf")
                                .build()).build())
                .originalSender(originalSender).build();
    }
}
