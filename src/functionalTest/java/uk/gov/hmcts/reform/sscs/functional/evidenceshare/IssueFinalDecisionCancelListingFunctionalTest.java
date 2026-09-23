package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.reform.sscs.bulkscan.BaseFunctionalTest.generateRandomNino;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CREATE_RESPONSE_RECEIVED_TEST_CASE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FINAL_DECISION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_CASE_ONLY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberType.TRIBUNAL_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberType.TRIBUNAL_MEMBER_DISABILITY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberType.TRIBUNAL_MEMBER_MEDICAL;
import static uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler.getJsonCallbackForTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberComposition;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
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
@TestPropertySource(locations = "classpath:config/application_functional.properties", properties = "logging.level.uk.gov.hmcts.reform.sscs.functional.evidenceshare.IssueFinalDecisionCancelListingFunctionalTest=DEBUG")
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

        log.debug("Case {}: setting a judge-only panel so the Ready to List judge-only rule applies", ccdCaseId);
        final SscsCaseDetails judgeOnlyCase = updatePanelMemberComposition(
            PanelMemberComposition.builder().panelCompositionJudge(TRIBUNAL_JUDGE.getReference()).build());
        assertThat(judgeOnlyCase.getData().getPanelMemberComposition().isJudgeOnly()).isTrue();

        issueFinalDecision(judgeOnlyCase);

        awaitCancellationRequested(hearingId);
        sendHmcMessage(hearingId, HmcStatus.CANCELLED, ListingStatus.CNCL);
        awaitHearingCancelled(hearingId);

        log.info("Listing request {} cancelled for case id {}", hearingId, ccdCaseId);
    }

    @Test
    void givenCaseWithBookedHearingInTheFuture_whenIssueFinalDecision_thenHearingIsCancelled() throws IOException, MessageProcessingException {
        final String hearingId = createCaseAwaitingListing();

        log.debug("Case {}: setting a full panel so only the future hearing rule can trigger the cancellation", ccdCaseId);
        final SscsCaseDetails panelCase = updatePanelMemberComposition(fullPanel());
        assertThat(panelCase.getData().getPanelMemberComposition().isJudgeOnly()).isFalse();

        sendHmcMessage(hearingId, HmcStatus.LISTED, ListingStatus.FIXED);

        log.debug("Case {}: waiting for hearing {} to be booked with a venue and a future date", ccdCaseId, hearingId);
        hmcAwait().untilAsserted(() -> {
            final SscsCaseDetails bookedCase = findCaseById(ccdCaseId);
            assertThat(bookedCase.getState()).isEqualTo(State.HEARING.getId());
            final Hearing bookedHearing = findHearing(bookedCase, hearingId);
            assertThat(bookedHearing.getValue().getHearingStatus()).isEqualTo(HearingStatus.LISTED);
            assertThat(bookedHearing.getValue().getVenue()).isNotNull();
            assertThat(LocalDate.parse(bookedHearing.getValue().getHearingDate())).isAfter(today());
        });
        final Hearing bookedHearing = findHearing(findCaseById(ccdCaseId), hearingId);
        log.debug("Case {}: hearing {} booked for {} at {}", ccdCaseId, hearingId, bookedHearing.getValue().getHearingDate(),
            bookedHearing.getValue().getVenue().getName());

        issueFinalDecision(findCaseById(ccdCaseId));

        awaitCancellationRequested(hearingId);
        sendHmcMessage(hearingId, HmcStatus.CANCELLED, ListingStatus.CNCL);
        awaitHearingCancelled(hearingId);

        log.info("Booked hearing {} cancelled for case id {}", hearingId, ccdCaseId);
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
        final SscsCaseDetails createdCase = ccdService.createCase(buildCaseData(),
            CREATE_RESPONSE_RECEIVED_TEST_CASE.getCcdType(), "Issue final decision cancel listing",
            "Issue final decision cancel listing functional test", getIdamTokens());
        ccdCaseId = String.valueOf(createdCase.getId());
        log.debug("Case {}: created in state {}", ccdCaseId, createdCase.getState());

        final String hearingId = ccdCaseId;

        log.debug("Case {}: firing {} to send a listing request to HMC", ccdCaseId, READY_TO_LIST.getCcdType());
        updateCaseEvent(READY_TO_LIST, findCaseById(ccdCaseId));

        awaitListingRequestCreated(hearingId);

        final SscsCaseDetails awaitingListingCase = findCaseById(ccdCaseId);
        assertThat(awaitingListingCase.getState()).isEqualTo(State.READY_TO_LIST.getId());
        final Hearing awaitingListingHearing = findHearing(awaitingListingCase, hearingId);
        assertThat(awaitingListingHearing.getValue().getStart()).isNull();
        assertThat(awaitingListingHearing.getValue().getVenue()).isNull();
        log.debug("Case {}: in state {} with listing request {} awaiting listing (no date or venue)", ccdCaseId,
            awaitingListingCase.getState(), hearingId);

        return hearingId;
    }

    private SscsCaseDetails updatePanelMemberComposition(final PanelMemberComposition panelMemberComposition) {
        final SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
        caseDetails.getData().setPanelMemberComposition(panelMemberComposition);
        updateCaseEvent(UPDATE_CASE_ONLY, caseDetails);
        final SscsCaseDetails updatedCase = findCaseById(ccdCaseId);
        log.debug("Case {}: panel member composition is now {} (judge only: {})", ccdCaseId,
            updatedCase.getData().getPanelMemberComposition(), updatedCase.getData().getPanelMemberComposition().isJudgeOnly());
        return updatedCase;
    }

    private void issueFinalDecision(final SscsCaseDetails caseDetails) {
        log.debug("Case {}: firing {} from state {}", ccdCaseId, ISSUE_FINAL_DECISION.getCcdType(), caseDetails.getState());
        updateCaseEvent(ISSUE_FINAL_DECISION, caseDetails);
        log.debug("Case {}: {} completed, case is now in state {}", ccdCaseId, ISSUE_FINAL_DECISION.getCcdType(),
            findCaseById(ccdCaseId).getState());
    }

    private void sendHmcMessage(final String hearingId, final HmcStatus hmcStatus,
        final ListingStatus listingStatus) throws MessageProcessingException {
        log.debug("Case {}: simulating HMC topic message for hearing {} with status {} and listing status {}", ccdCaseId,
            hearingId, hmcStatus, listingStatus);
        processHmcMessageServiceV2.processEventMessage(buildHmcMessage(hearingId, hmcStatus, listingStatus));
    }

    private SscsCaseData buildCaseData() throws IOException {
        final String caseCreated = today().toString();
        String json = getJsonCallbackForTest(CASE_DATA_JSON)
            .replace("NINO_TO_BE_REPLACED", generateRandomNino())
            .replace("MRN_DATE_TO_BE_REPLACED", today().minusDays(14).toString())
            .replace("CASE_CREATED_TO_BE_REPLACED", caseCreated);
        json = uploadCaseDocument(EVIDENCE_DOCUMENT_PDF, PREVIEW_DOCUMENT_TYPE, json);
        return mapper.readValue(json, SscsCaseData.class);
    }

    private HmcMessage buildHmcMessage(final String hearingId, final HmcStatus hmcStatus, final ListingStatus listingStatus) {
        return HmcMessage
            .builder()
            .hmctsServiceCode(SSCS_SERVICE_CODE)
            .caseId(Long.valueOf(ccdCaseId))
            .hearingId(hearingId)
            .hearingUpdate(HearingUpdate.builder().hmcStatus(hmcStatus).listingStatus(listingStatus).build())
            .build();
    }

    private ConditionFactory hmcAwait() {
        return await().atMost(3, MINUTES).pollInterval(5, SECONDS);
    }

    private void awaitListingRequestCreated(final String hearingId) {
        log.debug("Case {}: waiting for listing request {} to be added to the case", ccdCaseId, hearingId);
        hmcAwait().untilAsserted(() -> assertThat(findCaseById(ccdCaseId).getData().getHearings())
            .isNotEmpty()
            .anySatisfy(hearing -> assertThat(hearing.getValue().getHearingId()).isEqualTo(hearingId)));
        log.debug("Case {}: listing request {} added to the case", ccdCaseId, hearingId);
    }

    private void awaitCancellationRequested(final String hearingId) throws JsonProcessingException {
        final String otherCancellationReason = mapper.writeValueAsString(CancellationReason.OTHER);
        log.debug("Case {}: waiting for HMC to receive DELETE {}/{} with reason {}", ccdCaseId, HEARING_ENDPOINT, hearingId,
            otherCancellationReason);
        hmcAwait().untilAsserted(() -> assertThat(hmcWireMock.find(
            deleteRequestedFor(urlEqualTo(HEARING_ENDPOINT + "/" + hearingId)).withRequestBody(
                containing(otherCancellationReason)))).hasSize(1));
        log.debug("Case {}: HMC received the cancellation request for hearing {}", ccdCaseId, hearingId);
    }

    private void awaitHearingCancelled(final String hearingId) {
        log.debug("Case {}: waiting for hearing {} to be marked {} on the case", ccdCaseId, hearingId, HearingStatus.CANCELLED);
        hmcAwait().untilAsserted(
            () -> assertThat(findHearing(findCaseById(ccdCaseId), hearingId).getValue().getHearingStatus()).isEqualTo(
                HearingStatus.CANCELLED));
        log.debug("Case {}: hearing {} is {} on the case", ccdCaseId, hearingId, HearingStatus.CANCELLED);
    }

    private Hearing findHearing(final SscsCaseDetails caseDetails, final String hearingId) {
        return caseDetails
            .getData()
            .getHearings()
            .stream()
            .filter(hearing -> hearingId.equals(hearing.getValue().getHearingId()))
            .findFirst()
            .orElseThrow();
    }
}
