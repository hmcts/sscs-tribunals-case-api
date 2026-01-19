package uk.gov.hmcts.reform.sscs.ccd.presubmit.overdueftaresponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestUtil.getCommunicationRequestFromId;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequestDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequestReply;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequestTopic;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaCommunicationFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.utility.calendar.BusinessDaysCalculatorService;

class OverdueFtaResponseAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private OverdueFtaResponseAboutToSubmitHandler handler;

    private SscsCaseData sscsCaseData;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private IdamService idamService;

    @Mock
    private BusinessDaysCalculatorService businessDaysCalculatorService;

    private final CommunicationRequest overdueCommunicationsRequest = CommunicationRequest.builder()
            .id("overDueRequestId")
            .value(
                    CommunicationRequestDetails.builder()
                            .requestTopic(CommunicationRequestTopic.APPEAL_TYPE)
                            .requestMessage("test message")
                            .requestDateTime(LocalDateTime.now().minusDays(2))
                            .requestUserName("test user")
                            .requestResponseDueDate(LocalDate.now().plusDays(3))
                            .build()
            ).build();

    private final CommunicationRequest overdueCommunicationsRequestWithTaskCreated = CommunicationRequest.builder()
            .id("overDueRequestIdWithTask")
            .value(
                    CommunicationRequestDetails.builder()
                            .requestTopic(CommunicationRequestTopic.APPEAL_TYPE)
                            .requestMessage("test message")
                            .requestDateTime(LocalDateTime.now().minusDays(2))
                            .requestUserName("test user")
                            .requestResponseDueDate(LocalDate.now().plusDays(3))
                            .taskCreatedForRequest(YesNo.YES)
                            .build()
            ).build();

    private final CommunicationRequest notOverdueCommunicationRequest = CommunicationRequest.builder()
            .id("notOverDueRequestId")
            .value(
                    CommunicationRequestDetails.builder()
                            .requestTopic(CommunicationRequestTopic.APPEAL_TYPE)
                            .requestMessage("test message")
                            .requestDateTime(LocalDateTime.now())
                            .requestUserName("test user")
                            .requestResponseDueDate(LocalDate.now().plusDays(3))
                            .build()
            ).build();

    private final CommunicationRequest respondedToRequest = CommunicationRequest.builder()
            .id("respondedToRequestId")
            .value(
                    CommunicationRequestDetails.builder()
                            .requestTopic(CommunicationRequestTopic.APPEAL_TYPE)
                            .requestMessage("test message")
                            .requestDateTime(LocalDateTime.now().minusDays(2))
                            .requestUserName("test user")
                            .requestResponseDueDate(LocalDate.now())
                            .requestReply(CommunicationRequestReply.builder().build())
                            .build()
            ).build();

    @BeforeEach
    void setUp() {
        openMocks(this);
        handler = new OverdueFtaResponseAboutToSubmitHandler(businessDaysCalculatorService);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();
        when(callback.getEvent()).thenReturn(EventType.OVERDUE_FTA_RESPONSE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }


    @Test
    void givenAValidAboutToSubmitEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    void givenAnInvalidAboutToSubmitEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    void givenAValidAboutToStartEvent_thenReturnFalse() {
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    void throwsExceptionIfItCannotHandle() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertThrows(IllegalStateException.class, () -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION));
    }

    @Test
    void givenAnUnrespondedToOverdueRequest_thenSetTaskCreatedFlag() throws IOException {
        sscsCaseData.setCommunicationFields(FtaCommunicationFields.builder()
                .ftaCommunications(List.of(overdueCommunicationsRequest))
                .build());

        given(businessDaysCalculatorService.getBusinessDayInPast(LocalDate.now(), 2))
                .willReturn(LocalDate.now().minusDays(2));

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getCommunicationFields().getWaTaskFtaCommunicationId()).isEqualTo("overDueRequestId");
        assertThat(getCommunicationRequestFromId("overDueRequestId", sscsCaseData.getCommunicationFields().getFtaCommunications()).getValue().getTaskCreatedForRequest()).isEqualTo(YesNo.YES);
    }

    @Test
    void givenAnUnrespondedToOverdueRequestThatAlreadyHasTaskCreatedFlagSet_thenDoNotChangeFlagOrSetLatestCommunicationId() {
        sscsCaseData.setCommunicationFields(FtaCommunicationFields.builder()
                .waTaskFtaCommunicationId("testId")
                .ftaCommunications(List.of(overdueCommunicationsRequestWithTaskCreated))
                .build());

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getCommunicationFields().getWaTaskFtaCommunicationId()).isEqualTo("testId");
        assertThat(getCommunicationRequestFromId("overDueRequestIdWithTask", sscsCaseData.getCommunicationFields().getFtaCommunications()).getValue().getTaskCreatedForRequest()).isEqualTo(YesNo.YES);
    }

    @Test
    void givenAnUnrespondedToRequestThatIsNotOverdue_thenDoNotSetTaskCreatedFlag() {
        sscsCaseData.setCommunicationFields(FtaCommunicationFields.builder()
                .ftaCommunications(List.of(notOverdueCommunicationRequest))
                .build());

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getCommunicationFields().getWaTaskFtaCommunicationId()).isNull();
        assertThat(getCommunicationRequestFromId("notOverDueRequestId", sscsCaseData.getCommunicationFields().getFtaCommunications()).getValue().getTaskCreatedForRequest()).isNull();
    }

    @Test
    void givenARespondedToRequest_thenDoNotSetTaskCreatedFlag() {
        sscsCaseData.setCommunicationFields(FtaCommunicationFields.builder()
                .waTaskFtaCommunicationId("testId")
                .ftaCommunications(List.of(respondedToRequest))
                .build());

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getCommunicationFields().getWaTaskFtaCommunicationId()).isEqualTo("testId");
        assertThat(getCommunicationRequestFromId("respondedToRequestId", sscsCaseData.getCommunicationFields().getFtaCommunications()).getValue().getTaskCreatedForRequest()).isNull();
    }
}
