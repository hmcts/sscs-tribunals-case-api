package uk.gov.hmcts.reform.sscs.service;

import static junit.framework.TestCase.assertNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase.POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase.REVIEW_BY_TCW;

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
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;


@RunWith(JUnitParamsRunner.class)
public class InterlocServiceTest {

    private InterlocService interlocService;
    private final PostponementRequestService postponementRequestService = new PostponementRequestService();
    private SscsCaseData sscsCaseData;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;


    @Before
    public void setUp() {
        openMocks(this);
        interlocService = new InterlocService(postponementRequestService);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
                .selectWhoReviewsCase(new DynamicList(new DynamicListItem("reviewByTcw", "Review by TCW"), null))
                .directionDueDate("01/02/2020")
                .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
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

        PreSubmitCallbackResponse<SscsCaseData> response = interlocService.processSendToInterloc(callback, sscsCaseData);

        assertEquals(Collections.EMPTY_SET, response.getErrors());

        assertEquals(selectWhoReviewsCase.getId(), response.getData().getInterlocReviewState().getCcdDefinition());
        assertEquals(LocalDate.now(), response.getData().getInterlocReferralDate());
        assertNull(response.getData().getSelectWhoReviewsCase());
        assertNull(response.getData().getDirectionDueDate());
    }

    @Test
    @Parameters({"APPELLANT, APPELLANT", "REPRESENTATIVE, REP", "JOINT_PARTY, JOINT_PARTY"})
    public void givenPostponementRequestInterlocSendToTcw_setsEvidenceHandledFlagToNoForDocumentSelected(PartyItemList originalSenderParty, UploadParty uploadParty) {
        setupDataForPostponementRequestInterlocSendToTcw(originalSenderParty);
        when(callback.isIgnoreWarnings()).thenReturn(true);

        PreSubmitCallbackResponse<SscsCaseData> response = interlocService.processSendToInterloc(callback, sscsCaseData);

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

    @Test
    public void givenPostponementRequestInterlocSendToTcw_thenReturnWarningMessageAboutPostponingHearing() {
        setupDataForPostponementRequestInterlocSendToTcw(PartyItemList.APPELLANT);

        PreSubmitCallbackResponse<SscsCaseData> response = interlocService.processSendToInterloc(callback, sscsCaseData);
        assertThat(response.getWarnings().size(), is(1));
        assertThat(response.getWarnings().iterator().next(), is("Are you sure you want to postpone the hearing?"));
    }

    @Test
    public void givenPostponementRequestInterlocSendToTcw_returnAnErrorIfNoOriginalSenderSelected() {
        sscsCaseData = sscsCaseData.toBuilder().selectWhoReviewsCase(new DynamicList(
                        new DynamicListItem(POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getId(), POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getLabel()),
                        Arrays.asList(new DynamicListItem(REVIEW_BY_JUDGE.getId(), REVIEW_BY_JUDGE.getLabel()),
                                new DynamicListItem(REVIEW_BY_TCW.getId(), REVIEW_BY_TCW.getLabel()),
                                new DynamicListItem(POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getId(), POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getLabel()))
                ))
                .originalSender(null).build();

        PreSubmitCallbackResponse<SscsCaseData> response = interlocService.processSendToInterloc(callback, sscsCaseData);

        assertEquals(1, response.getErrors().size());
        assertEquals("Must select original sender", response.getErrors().toArray()[0]);
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
}
