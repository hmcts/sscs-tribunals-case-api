package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.validsendtointerloc;

import static java.time.LocalDateTime.now;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.SelectWhoReviewsCase.POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW;
import static uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.SelectWhoReviewsCase.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.SelectWhoReviewsCase.REVIEW_BY_TCW;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_SEND_TO_INTERLOC;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;

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
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.SelectWhoReviewsCase;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.validsendtointerloc.ValidSendToInterlocAboutToSubmitHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostponementRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.ReissueArtifactUi;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.service.AddNoteService;
import uk.gov.hmcts.reform.sscs.service.PostponementRequestService;

@ExtendWith(MockitoExtension.class)
public class ValidSendToInterlocAboutToSubmitHandlerTest {

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
    public void setUp() {
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
                .selectWhoReviewsCase(
                        new DynamicList(new DynamicListItem("reviewByTcw", "Review by TCW"), null))
                .directionDueDate("01/02/2020")
                .build();
        caseDetails = new CaseDetails<>(123L, "SSCS", READY_TO_LIST, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), VALID_SEND_TO_INTERLOC, false);

        handler = new ValidSendToInterlocAboutToSubmitHandler(postponementRequestService, addNoteService);
    }

    @ParameterizedTest
    @CsvSource({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenANonHandleEvidenceEvent_thenReturnFalse(EventType eventType) {
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), eventType, false);

        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @ParameterizedTest
    @CsvSource({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @ParameterizedTest
    @CsvSource({"REVIEW_BY_TCW", "REVIEW_BY_JUDGE"})
    public void setsEvidenceHandledFlagToNoForDocumentSelected(SelectWhoReviewsCase selectWhoReviewsCase) {
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

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(Collections.EMPTY_SET, response.getErrors());
        assertEquals(LocalDate.now(), response.getData().getInterlocReferralDate());
        assertNull(response.getData().getSelectWhoReviewsCase());
        assertNull(response.getData().getDirectionDueDate());
        verify(addNoteService).addNote(eq(USER_AUTHORISATION), eq(response.getData()), eq(null));
    }


    @Test
    public void givenPostponementRequestInterlocSendToTcw_thenReturnWarningMessageAboutPostponingHearing() {
        var caseData = setupDataForPostponementRequestInterlocSendToTcw(PartyItemList.APPELLANT);
        var caseDetails = new CaseDetails<>(123L, "SSCS", READY_TO_LIST, caseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), VALID_SEND_TO_INTERLOC, false);

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings().size(), is(1));
        assertThat(response.getWarnings().iterator().next(),
                is("Are you sure you want to postpone the hearing?"));
    }

    @ParameterizedTest
    @CsvSource({"APPELLANT", "REPRESENTATIVE", "JOINT_PARTY"})
    public void givenPostponementRequestInterlocSendToTcw_setsEvidenceHandledFlagToNoForDocumentSelected(
            PartyItemList originalSenderParty) {
        var caseData = setupDataForPostponementRequestInterlocSendToTcw(originalSenderParty);
        caseData.setTempNoteDetail("Test Note");
        var caseDetails = new CaseDetails<>(123L, "SSCS", READY_TO_LIST, caseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), VALID_SEND_TO_INTERLOC, true);

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(Collections.EMPTY_SET, response.getErrors());
        assertEquals(LocalDate.now(), response.getData().getInterlocReferralDate());
        assertNull(response.getData().getSelectWhoReviewsCase());
        assertNull(response.getData().getDirectionDueDate());
        verify(addNoteService).addNote(eq(USER_AUTHORISATION), eq(response.getData()), eq("Test Note"));
    }

    @ParameterizedTest
    @CsvSource({"VALID_SEND_TO_INTERLOC", "ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE"})
    public void returnAnErrorIfNoSelectWhoReviewsCaseSelected(EventType eventType) {
        sscsCaseData = sscsCaseData.toBuilder().selectWhoReviewsCase(null).build();
        caseDetails = new CaseDetails<>(123L, "SSCS", READY_TO_LIST, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), eventType, true);

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains("Must select who reviews the appeal."));
    }

    @ParameterizedTest
    @CsvSource({"VALID_SEND_TO_INTERLOC", "ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE"})
    public void givenPostponementRequestInterlocSendToTcw_returnAnErrorIfNoOriginalSenderSelected(EventType eventType) {
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

        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains("Must select original sender"));
    }

    @Test
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), APPEAL_RECEIVED, false);

        assertThrows(IllegalStateException.class, () -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION));
    }

    private SscsCaseData setupDataForPostponementRequestInterlocSendToTcw(
            PartyItemList originalSenderParty) {
        DynamicListItem value = new DynamicListItem(originalSenderParty.getCode(), originalSenderParty.getLabel());
        DynamicList originalSender = new DynamicList(value, Collections.singletonList(value));

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
