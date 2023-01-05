package uk.gov.hmcts.reform.sscs.ccd.presubmit.validsendtointerloc;

import static junit.framework.TestCase.assertNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.service.PostponementRequestService;

@RunWith(JUnitParamsRunner.class)
public class ValidSendToInterlocAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private ValidSendToInterlocAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    private SscsCaseData sscsCaseData;


    @Before
    public void setUp() {
        openMocks(this);

        handler = new ValidSendToInterlocAboutToSubmitHandler(new PostponementRequestService());

        when(callback.getEvent()).thenReturn(EventType.VALID_SEND_TO_INTERLOC);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
                .selectWhoReviewsCase(new DynamicList(new DynamicListItem("reviewByTcw", "Review by TCW"), null))
                .directionDueDate("01/02/2020")
                .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    @Parameters({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenANonHandleEvidenceEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    @Parameters({"REVIEW_BY_TCW", "REVIEW_BY_JUDGE"})
    public void setsEvidenceHandledFlagToNoForDocumentSelected(SelectWhoReviewsCase selectWhoReviewsCase) {

        sscsCaseData = sscsCaseData.toBuilder()
                .selectWhoReviewsCase(new DynamicList(
                        new DynamicListItem(selectWhoReviewsCase.getId(), selectWhoReviewsCase.getLabel()),
                        Arrays.asList(new DynamicListItem(REVIEW_BY_JUDGE.getId(), REVIEW_BY_JUDGE.getLabel()),
                                new DynamicListItem(REVIEW_BY_TCW.getId(), REVIEW_BY_TCW.getLabel()),
                                new DynamicListItem(POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getId(), POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getLabel()))
                ))
                .reissueArtifactUi(ReissueArtifactUi.builder()
                        .reissueFurtherEvidenceDocument(new DynamicList(new DynamicListItem(selectWhoReviewsCase.getId(),
                                selectWhoReviewsCase.getLabel()), null)).build())
                .build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(Collections.EMPTY_SET, response.getErrors());

        assertEquals(selectWhoReviewsCase.getId(), response.getData().getInterlocReviewState().getCcdDefinition());
        assertEquals(LocalDate.now(), response.getData().getInterlocReferralDate());
        assertNull(response.getData().getSelectWhoReviewsCase());
        assertNull(response.getData().getDirectionDueDate());
    }


    @Test
    public void givenPostponementRequestInterlocSendToTcw_thenReturnWarningMessageAboutPostponingHearing() {
        setupDataForPostponementRequestInterlocSendToTcw(PartyItemList.APPELLANT);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getWarnings().size(), is(1));
        assertThat(response.getWarnings().iterator().next(), is("Are you sure you want to postpone the hearing?"));
    }

    @Test
    @Parameters({"APPELLANT, APPELLANT", "REPRESENTATIVE, REP", "JOINT_PARTY, JOINT_PARTY"})
    public void givenPostponementRequestInterlocSendToTcw_setsEvidenceHandledFlagToNoForDocumentSelected(PartyItemList originalSenderParty, UploadParty uploadParty) {

        setupDataForPostponementRequestInterlocSendToTcw(originalSenderParty);
        when(callback.isIgnoreWarnings()).thenReturn(true);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(Collections.EMPTY_SET, response.getErrors());

        assertEquals(InterlocReviewState.REVIEW_BY_TCW, response.getData().getInterlocReviewState());
        assertEquals(InterlocReferralReason.REVIEW_POSTPONEMENT_REQUEST, response.getData().getInterlocReferralReason());
        assertEquals(LocalDate.now(), response.getData().getInterlocReferralDate());
        assertEquals(YES, sscsCaseData.getPostponementRequest().getUnprocessedPostponementRequest());
        assertNull(response.getData().getSelectWhoReviewsCase());
        assertNull(response.getData().getDirectionDueDate());
        assertNull(response.getData().getPostponementRequest().getPostponementRequestDetails());
        assertNull(response.getData().getPostponementRequest().getPostponementPreviewDocument());

        assertEquals(1, sscsCaseData.getSscsDocument().size());
        final SscsDocument document = sscsCaseData.getSscsDocument().get(0);
        assertEquals(DocumentType.POSTPONEMENT_REQUEST.getValue(), document.getValue().getDocumentType());
        assertEquals("example.pdf", document.getValue().getDocumentLink().getDocumentFilename());
        assertEquals(uploadParty.getValue(), document.getValue().getOriginalPartySender());
    }

    private void setupDataForPostponementRequestInterlocSendToTcw(PartyItemList originalSenderParty) {
        DynamicListItem value = new DynamicListItem(originalSenderParty.getCode(), originalSenderParty.getLabel());
        DynamicList originalSender = new DynamicList(value, Collections.singletonList(value));

        sscsCaseData = sscsCaseData.toBuilder()
                .selectWhoReviewsCase(new DynamicList(
                        new DynamicListItem(POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getId(), POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getLabel()),
                        Arrays.asList(new DynamicListItem(REVIEW_BY_JUDGE.getId(), REVIEW_BY_JUDGE.getLabel()),
                                new DynamicListItem(REVIEW_BY_TCW.getId(), REVIEW_BY_TCW.getLabel()),
                                new DynamicListItem(POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getId(), POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getLabel()))
                ))
                .postponementRequest(PostponementRequest.builder()
                        .postponementRequestDetails("Here are some details")
                        .postponementRequestHearingVenue("Venue 1")
                        .postponementPreviewDocument(DocumentLink.builder()
                                .documentBinaryUrl("http://example.com")
                                .documentFilename("example.pdf")
                                .build()).build())
                .originalSender(originalSender).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    @Parameters({"VALID_SEND_TO_INTERLOC", "ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE"})
    public void returnAnErrorIfNoSelectWhoReviewsCaseSelected(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);

        sscsCaseData = sscsCaseData.toBuilder().selectWhoReviewsCase(null).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals("Must select who reviews the appeal.", response.getErrors().toArray()[0]);
    }

    @Test
    @Parameters({"VALID_SEND_TO_INTERLOC", "ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE"})
    public void givenPostponementRequestInterlocSendToTcw_returnAnErrorIfNoOriginalSenderSelected(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);

        sscsCaseData = sscsCaseData.toBuilder().selectWhoReviewsCase(new DynamicList(
                        new DynamicListItem(POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getId(), POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getLabel()),
                        Arrays.asList(new DynamicListItem(REVIEW_BY_JUDGE.getId(), REVIEW_BY_JUDGE.getLabel()),
                                new DynamicListItem(REVIEW_BY_TCW.getId(), REVIEW_BY_TCW.getLabel()),
                                new DynamicListItem(POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getId(), POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getLabel()))
                ))
                .originalSender(null).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals("Must select original sender", response.getErrors().toArray()[0]);
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

}
