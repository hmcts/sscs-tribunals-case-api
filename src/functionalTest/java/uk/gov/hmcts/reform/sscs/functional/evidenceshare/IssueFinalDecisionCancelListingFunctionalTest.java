package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CREATE_RESPONSE_RECEIVED_TEST_CASE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FINAL_DECISION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_CASE_ONLY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberType.TRIBUNAL_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberType.TRIBUNAL_MEMBER_DISABILITY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberType.TRIBUNAL_MEMBER_MEDICAL;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberComposition;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.exception.MessageProcessingException;
import uk.gov.hmcts.reform.sscs.model.hmc.message.HearingUpdate;
import uk.gov.hmcts.reform.sscs.model.hmc.message.HmcMessage;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.ListingStatus;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.ProcessHmcMessageServiceV2;

@Slf4j
@TestPropertySource(locations = "classpath:config/application_functional.properties")
@TestConstructor(autowireMode = AutowireMode.ALL)
@RequiredArgsConstructor
class IssueFinalDecisionCancelListingFunctionalTest extends AbstractFunctionalTest {

    private static final String CASE_DATA_JSON = "handlers/issuefinaldecision/readyToListWriteFinalDecisionCaseData.json";
    private static final String EVIDENCE_DOCUMENT_PDF = "evidence-document.pdf";
    private static final String PREVIEW_DOCUMENT_TYPE = "PREVIEW_DOCUMENT";
    private static final String HEARING_ENDPOINT = "/hearing";
    private static final String SSCS_SERVICE_CODE = "BBA3";

    private final CcdService ccdService;
    private final ObjectMapper mapper;
    private final ProcessHmcMessageServiceV2 processHmcMessageServiceV2;

    @Value("${hmc.url}")
    private String hmcUrl;

    private WireMock hmcWireMock;

    @BeforeEach
    void setUpHmcWireMock() {
        final URI hmcUri = URI.create(hmcUrl);
        hmcWireMock = new WireMock(hmcUri.getHost(), hmcUri.getPort());
    }

    @Test
    void givenJudgeOnlyCaseAwaitingListingInReadyToList_whenIssueFinalDecision_thenListingRequestIsCancelled() throws IOException, MessageProcessingException {

        final String hearingId = createCaseAwaitingListing();

        log.info("Case {}: setting a judge-only panel so the Ready to List judge-only rule applies", ccdCaseId);
        final SscsCaseDetails judgeOnlyCase = updatePanelMemberComposition(judgeOnlyPanel());
        assertThat(judgeOnlyCase.getData().getPanelMemberComposition().isJudgeOnly()).isTrue();

        issueFinalDecision(judgeOnlyCase);

        awaitCancellationRequested(hearingId);
    }

    @Test
    void givenCaseWithBookedHearingInTheFuture_whenIssueFinalDecision_thenHearingIsCancelled() throws IOException, MessageProcessingException {
        final String hearingId = createCaseAwaitingListing();

        log.info("Case {}: setting a full panel so only the future hearing rule can trigger the cancellation", ccdCaseId);
        final SscsCaseDetails panelCase = updatePanelMemberComposition(fullPanel());
        assertThat(panelCase.getData().getPanelMemberComposition().isJudgeOnly()).isFalse();

        sendHmcMessageToBookHearing(hearingId);

        log.info("Case {}: waiting for hearing {} to be booked with a venue and a future date", ccdCaseId, hearingId);
        defaultAwait().untilAsserted(() -> {
            final SscsCaseDetails bookedCase = findCaseById(ccdCaseId);
            assertThat(bookedCase.getState()).isEqualTo(State.HEARING.getId());
            final Hearing bookedHearing = findHearing(bookedCase, hearingId);
            assertThat(bookedHearing.getValue().getHearingStatus()).isEqualTo(HearingStatus.LISTED);
            assertThat(bookedHearing.getValue().getVenue()).isNotNull();
            assertThat(LocalDate.parse(bookedHearing.getValue().getHearingDate())).isAfter(today());
        });
        final Hearing bookedHearing = findHearing(findCaseById(ccdCaseId), hearingId);
        log.info("Case {}: hearing {} booked for {} at {}", ccdCaseId, hearingId, bookedHearing.getValue().getHearingDate(),
            bookedHearing.getValue().getVenue().getName());

        issueFinalDecision(findCaseById(ccdCaseId));

        awaitCancellationRequested(hearingId);
    }

    private static PanelMemberComposition judgeOnlyPanel() {
        return PanelMemberComposition.builder().panelCompositionJudge(TRIBUNAL_JUDGE.getReference()).build();
    }

    private static PanelMemberComposition fullPanel() {
        return PanelMemberComposition
            .builder()
            .panelCompositionJudge(TRIBUNAL_JUDGE.getReference())
            .panelCompositionMemberMedical1(TRIBUNAL_MEMBER_MEDICAL.getReference())
            .panelCompositionDisabilityAndFqMember(List.of(TRIBUNAL_MEMBER_DISABILITY.getReference()))
            .build();
    }

    private String createCaseAwaitingListing() throws IOException {

        final SscsCaseDetails createdCase = ccdService.createCase(
            buildCaseData(CASE_DATA_JSON, data -> uploadCaseDocument(EVIDENCE_DOCUMENT_PDF, PREVIEW_DOCUMENT_TYPE, data)),
            CREATE_RESPONSE_RECEIVED_TEST_CASE.getCcdType(), "Issue final decision cancel listing",
            "Issue final decision cancel listing functional test", getIdamTokens());
        ccdCaseId = String.valueOf(createdCase.getId());
        log.info("Case {}: created in state {}", ccdCaseId, createdCase.getState());

        final String hearingId = ccdCaseId;


        log.info("Case {}: firing {} to send a listing request to HMC", ccdCaseId, READY_TO_LIST.getCcdType());
        updateCaseEvent(READY_TO_LIST, findCaseById(ccdCaseId));


        awaitListingRequestCreated(hearingId);

        final SscsCaseDetails awaitingListingCase = findCaseById(ccdCaseId);
        assertThat(awaitingListingCase.getState()).isEqualTo(State.READY_TO_LIST.getId());
        final Hearing awaitingListingHearing = findHearing(awaitingListingCase, hearingId);
        assertThat(awaitingListingHearing.getValue().getStart()).isNull();
        assertThat(awaitingListingHearing.getValue().getVenue()).isNull();
        log.info("Case {}: in state {} with listing request {} awaiting listing (no date or venue)", ccdCaseId,
            awaitingListingCase.getState(), hearingId);

        return hearingId;
    }

    private SscsCaseDetails updatePanelMemberComposition(final PanelMemberComposition panelMemberComposition) {
        final SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
        caseDetails.getData().setPanelMemberComposition(panelMemberComposition);
        updateCaseEvent(UPDATE_CASE_ONLY, caseDetails, "update panel member composition functional test",
            "is judge only panel composition: %s".formatted(panelMemberComposition.isJudgeOnly()));
        final SscsCaseDetails updatedCase = findCaseById(ccdCaseId);
        log.info("Case {}: panel member composition is now {} (judge only: {})", ccdCaseId,
            updatedCase.getData().getPanelMemberComposition(), updatedCase.getData().getPanelMemberComposition().isJudgeOnly());
        return updatedCase;
    }

    private void issueFinalDecision(final SscsCaseDetails caseDetails) {
        log.info("Case {}: firing {} from state {}", ccdCaseId, ISSUE_FINAL_DECISION.getCcdType(), caseDetails.getState());
        updateCaseEvent(ISSUE_FINAL_DECISION, caseDetails, "issue final decision functional test", "issue final decision event");
        log.info("Case {}: {} completed, case is now in state {}", ccdCaseId, ISSUE_FINAL_DECISION.getCcdType(),
            findCaseById(ccdCaseId).getState());
    }

    private void sendHmcMessageToBookHearing(final String hearingId) throws MessageProcessingException {
        log.info("Case {}: simulating HMC topic message for hearing {} with status {} and listing status {}", ccdCaseId,
            hearingId, HmcStatus.LISTED, ListingStatus.FIXED);
        processHmcMessageServiceV2.processEventMessage(buildHmcMessage(hearingId));
    }

    private HmcMessage buildHmcMessage(final String hearingId) {
        return HmcMessage
            .builder()
            .hmctsServiceCode(SSCS_SERVICE_CODE)
            .caseId(Long.valueOf(ccdCaseId))
            .hearingId(hearingId)
            .hearingUpdate(HearingUpdate.builder().hmcStatus(HmcStatus.LISTED).listingStatus(ListingStatus.FIXED).build())
            .build();
    }

    private void awaitListingRequestCreated(final String hearingId) {
        log.info("Case {}: waiting for listing request {} to be added to the case", ccdCaseId, hearingId);
        defaultAwait().untilAsserted(() -> assertThat(findCaseById(ccdCaseId).getData().getHearings())
            .isNotEmpty()
            .anySatisfy(hearing -> assertThat(hearing.getValue().getHearingId()).isEqualTo(hearingId)));
        log.info("Case {}: listing request {} added to the case", ccdCaseId, hearingId);
    }

    private void awaitCancellationRequested(final String hearingId) throws JsonProcessingException {
        final String otherCancellationReason = mapper.writeValueAsString(CancellationReason.OTHER);
        log.info("Case {}: waiting for HMC to receive DELETE {}/{} with reason {}", ccdCaseId, HEARING_ENDPOINT, hearingId,
            otherCancellationReason);
        defaultAwait().untilAsserted(() -> assertThat(hmcWireMock.find(
            deleteRequestedFor(urlEqualTo(HEARING_ENDPOINT + "/" + hearingId)).withRequestBody(
                containing(otherCancellationReason)))).hasSize(1));
        log.info("Case {}: HMC received the cancellation request for hearing {}", ccdCaseId, hearingId);
    }


}
