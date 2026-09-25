package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.reform.sscs.bulkscan.BaseFunctionalTest.generateRandomNino;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CREATE_TEST_CASE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FINAL_DECISION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_CASE_ONLY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberType.TRIBUNAL_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberType.TRIBUNAL_MEMBER_DISABILITY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberType.TRIBUNAL_MEMBER_MEDICAL;
import static uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler.getJsonCallbackForTest;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberComposition;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.exception.MessageProcessingException;
import uk.gov.hmcts.reform.sscs.model.hmc.message.HearingUpdate;
import uk.gov.hmcts.reform.sscs.model.hmc.message.HmcMessage;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.ListingStatus;
import uk.gov.hmcts.reform.sscs.service.HmcHearingApiService;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.ProcessHmcMessageServiceV2;

@Slf4j
@TestPropertySource(locations = "classpath:config/application_functional.properties")
@TestConstructor(autowireMode = AutowireMode.ALL)
@RequiredArgsConstructor
class IssueFinalDecisionCancelListingFunctionalTest extends AbstractFunctionalTest {

    private static final String CASE_DATA_JSON = "handlers/issuefinaldecision/readyToListWriteFinalDecisionCaseData.json";
    private static final String EVIDENCE_DOCUMENT_PDF = "evidence-document.pdf";
    private static final String PREVIEW_DOCUMENT_TYPE = "PREVIEW_DOCUMENT";
    private static final String SSCS_SERVICE_CODE = "BBA3";
    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1");

    private final ProcessHmcMessageServiceV2 processHmcMessageServiceV2;
    private final HmcHearingApiService hmcHearingApiService;

    static boolean isDeployedToLocalhost() {
        final String testUrl = System.getenv("TEST_URL");
        log.info("Test URL: {}", testUrl);
        return StringUtils.isBlank(testUrl) || LOCAL_HOSTS.contains(URI.create(testUrl).getHost());
    }

    @Test
    void givenJudgeOnlyCaseAwaitingListingInReadyToList_whenIssueFinalDecision_thenListingRequestIsCancelled() throws IOException, MessageProcessingException {

        final String hearingId = createCaseAwaitingListing();

        log.info("Case {}: setting a judge-only panel so the Ready to List judge-only rule applies", ccdCaseId);
        final SscsCaseDetails judgeOnlyCase = updatePanelMemberComposition(judgeOnlyPanel());
        assertThat(judgeOnlyCase.getData().getPanelMemberComposition().isJudgeOnly()).isTrue();

        issueFinalDecision(judgeOnlyCase);
        awaitHearingCancelled(hearingId);
    }

    @Test
    @EnabledIf("isDeployedToLocalhost")
    void givenCaseWithBookedHearingInTheFuture_whenIssueFinalDecision_thenHearingIsCancelled() throws IOException, MessageProcessingException {
        final String hearingId = createCaseAwaitingListing();

        log.info("Case {}: setting a full panel so only the future hearing rule can trigger the cancellation", ccdCaseId);
        final SscsCaseDetails panelCase = updatePanelMemberComposition(fullPanel());
        assertThat(panelCase.getData().getPanelMemberComposition().isJudgeOnly()).isFalse();

        sendHmcResponseMessageToBookHearing(hearingId);

        assertThatHearingBooked(hearingId);

        final Hearing bookedHearing = findHearing(findCaseById(ccdCaseId), hearingId);
        log.info("Case {}: hearing {} booked for {} at {}", ccdCaseId, hearingId, bookedHearing.getValue().getHearingDate(),
            bookedHearing.getValue().getVenue().getName());

        issueFinalDecision(findCaseById(ccdCaseId));

        awaitHearingCancelled(hearingId);
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

    private void assertThatHearingBooked(String hearingId) {
        log.info("Case {}: waiting for hearing {} to be booked with a venue and a future date", ccdCaseId, hearingId);
        defaultAwait().untilAsserted(() -> {
            final SscsCaseDetails bookedCase = findCaseById(ccdCaseId);
            assertThat(bookedCase.getState()).isEqualTo(State.HEARING.getId());
            final Hearing bookedHearing = findHearing(bookedCase, hearingId);
            assertThat(bookedHearing.getValue().getHearingStatus()).isEqualTo(HearingStatus.LISTED);
            assertThat(bookedHearing.getValue().getVenue()).isNotNull();
            assertThat(LocalDate.parse(bookedHearing.getValue().getHearingDate())).isAfter(LocalDate.now());
        });
    }

    private String createCaseAwaitingListing() throws IOException {

        createCaseFromJson(uploadCaseDocument(EVIDENCE_DOCUMENT_PDF, PREVIEW_DOCUMENT_TYPE, loadCaseDataJson()), CREATE_TEST_CASE);
        log.info("Case {}: created", ccdCaseId);

        log.info("Case {}: firing {} to send a listing request to HMC", ccdCaseId, READY_TO_LIST.getCcdType());
        updateCaseEvent(READY_TO_LIST, findCaseById(ccdCaseId));

        final String hearingId = awaitListingRequestCreated();

        final SscsCaseDetails awaitingListingCase = findCaseById(ccdCaseId);
        assertThat(awaitingListingCase.getState()).isEqualTo(State.READY_TO_LIST.getId());

        log.info("Case {}: in state {} with listing request {}", ccdCaseId, awaitingListingCase.getState(), hearingId);

        return hearingId;
    }

    private static String loadCaseDataJson() throws IOException {
        final LocalDate today = LocalDate.now();
        return getJsonCallbackForTest(CASE_DATA_JSON)
            .replace("NINO_TO_BE_REPLACED", generateRandomNino())
            .replace("MRN_DATE_TO_BE_REPLACED", today.minusDays(14).toString())
            .replace("CASE_CREATED_TO_BE_REPLACED", today.toString());
    }

    private static Hearing findHearing(final SscsCaseDetails caseDetails, final String hearingId) {
        return caseDetails.getData().getHearings().stream()
            .filter(hearing -> hearingId.equals(hearing.getValue().getHearingId()))
            .findFirst()
            .orElseThrow();
    }

    private SscsCaseDetails updatePanelMemberComposition(final PanelMemberComposition panelMemberComposition) {
        final SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
        caseDetails.getData().setPanelMemberComposition(panelMemberComposition);
        updateCaseEvent(UPDATE_CASE_ONLY, caseDetails);
        final SscsCaseDetails updatedCase = findCaseById(ccdCaseId);
        log.info("Case {}: panel member composition is now {} (judge only: {})", ccdCaseId,
            updatedCase.getData().getPanelMemberComposition(), updatedCase.getData().getPanelMemberComposition().isJudgeOnly());
        return updatedCase;
    }

    private void issueFinalDecision(final SscsCaseDetails caseDetails) {
        log.info("Case {}: firing {} from state {}", ccdCaseId, ISSUE_FINAL_DECISION.getCcdType(), caseDetails.getState());
        updateCaseEvent(ISSUE_FINAL_DECISION, caseDetails);
        log.info("Case {}: {} completed, case is now in state {}", ccdCaseId, ISSUE_FINAL_DECISION.getCcdType(),
            findCaseById(ccdCaseId).getState());
    }

    private void sendHmcResponseMessageToBookHearing(final String hearingId) throws MessageProcessingException {
        log.info("Case {}: simulating HMC topic message for hearing {} with status {} and listing status {}", ccdCaseId,
            hearingId, HmcStatus.LISTED, ListingStatus.FIXED);
        processHmcMessageServiceV2.processEventMessage(buildHmcMessage(hearingId, HmcStatus.LISTED, ListingStatus.FIXED));
    }

    private void sendHmcResponseMessageOnceHearingCancelled(final String hearingId) throws MessageProcessingException {
        log.info("Case {}: waiting for HMC to report hearing {} as {}", ccdCaseId, hearingId, HmcStatus.CANCELLED);
        defaultAwait().untilAsserted(() -> assertThat(
            hmcHearingApiService.getHearingRequest(hearingId).getRequestDetails().getStatus()).isEqualTo(HmcStatus.CANCELLED));
        log.info("Case {}: simulating HMC topic message for hearing {} with status {} and listing status {}", ccdCaseId,
            hearingId, HmcStatus.CANCELLED, ListingStatus.CNCL);
        processHmcMessageServiceV2.processEventMessage(buildHmcMessage(hearingId, HmcStatus.CANCELLED, ListingStatus.CNCL));
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

    private String awaitListingRequestCreated() {
        log.info("Case {}: waiting for a listing request to be added to the case", ccdCaseId);
        defaultAwait().untilAsserted(() -> {
            final Hearing latestHearing = findCaseById(ccdCaseId).getData().getLatestHearing();
            assertThat(latestHearing).isNotNull();
            assertThat(latestHearing.getValue().getHearingId()).isNotNull();
        });
        final String hearingId = findCaseById(ccdCaseId).getData().getLatestHearing().getValue().getHearingId();
        log.info("Case {}: listing request {} added to the case", ccdCaseId, hearingId);
        return hearingId;
    }

    private void awaitHearingCancelled(final String hearingId) throws MessageProcessingException {
        if (isDeployedToLocalhost()) {
            sendHmcResponseMessageOnceHearingCancelled(hearingId);
        }
        log.info("Case {}: waiting for hearing {} to be {} on the case", ccdCaseId, hearingId, HearingStatus.CANCELLED);
        hmcAwait().untilAsserted(() -> assertThat(
            findHearing(findCaseById(ccdCaseId), hearingId).getValue().getHearingStatus()).isEqualTo(HearingStatus.CANCELLED));
        log.info("Case {}: hearing {} is {} on the case", ccdCaseId, hearingId, HearingStatus.CANCELLED);
    }

    private static ConditionFactory hmcAwait() {
        return await().atMost(5, MINUTES).pollInterval(10, SECONDS);
    }

}