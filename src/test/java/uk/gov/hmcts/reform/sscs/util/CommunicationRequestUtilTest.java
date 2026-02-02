package uk.gov.hmcts.reform.sscs.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestTestHelper.buildCommRequest;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestTestHelper.buildCommRequestNotActionedResponseDateOffset;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestTestHelper.buildCommRequestWithActionedReply;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestTestHelper.buildCommRequestWithNoActionReply;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestTestHelper.buildCommRequestWithReply;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestTestHelper.buildCommRequestWithoutReply;
import static uk.gov.hmcts.reform.sscs.util.CommunicationRequestTestHelper.buildCustomCommRequest;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.utility.calendar.BusinessDaysCalculatorService;

class CommunicationRequestUtilTest {

    @Mock
    private BusinessDaysCalculatorService businessDaysCalculatorService;

    @BeforeEach
    void setUp() {
        openMocks(this);
    }

    @Test
    void testConstructorIsPrivate() throws NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        Constructor<CommunicationRequestUtil> constructor = CommunicationRequestUtil.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    void shouldReturnRepliesWithoutReviews() {
        CommunicationRequest requestWithNoActionReply = buildCommRequestWithNoActionReply();

        CommunicationRequest requestWithFtaUnactionedReply = buildCommRequestNotActionedResponseDateOffset(0, true);

        CommunicationRequest requestWithTribunalUnactionedReply = buildCommRequestNotActionedResponseDateOffset(0, false);

        CommunicationRequest requestWithTribunalActionedReply = buildCommRequestWithActionedReply(false);

        CommunicationRequest requestWithFtaActionedReply = buildCommRequestWithActionedReply(true);

        CommunicationRequest requestWithNoReply = buildCommRequestWithoutReply();

        List<CommunicationRequest> comms = List.of(requestWithNoActionReply, requestWithTribunalUnactionedReply,
            requestWithFtaUnactionedReply, requestWithTribunalActionedReply, requestWithFtaActionedReply, requestWithNoReply);

        List<CommunicationRequest> result = CommunicationRequestUtil.getRepliesWithoutReviews(comms);

        assertEquals(2, result.size());
        assertEquals(requestWithTribunalUnactionedReply, result.getFirst());
        assertEquals(requestWithFtaUnactionedReply, result.getLast());
    }

    @Test
    void shouldReturnOldestResponseDate() {
        CommunicationRequest request1 = buildCustomCommRequest("", "", 0, 0);
        CommunicationRequest request2 = buildCustomCommRequest("", "", 0, 1);

        List<CommunicationRequest> comms = List.of(request1, request2);

        LocalDate result = CommunicationRequestUtil.getOldestResponseDate(comms);

        assertEquals(LocalDate.now(), result);
    }

    @Test
    void shouldReturnNullWhenNoResponseDatesExist() {
        List<CommunicationRequest> comms = Collections.emptyList();

        LocalDate result = CommunicationRequestUtil.getOldestResponseDate(comms);

        assertNull(result);
    }

    @Test
    void shouldReturnOldestResponseProvidedDate() {
        CommunicationRequest request1 = buildCommRequestNotActionedResponseDateOffset(0, true);
        CommunicationRequest request2 = buildCommRequestNotActionedResponseDateOffset(1, true);

        List<CommunicationRequest> comms = List.of(request1, request2);

        LocalDate result = CommunicationRequestUtil.getOldestResponseProvidedDate(comms);

        assertEquals(LocalDate.now(), result);
    }

    @Test
    void shouldReturnRequestsWithoutReplies() {
        CommunicationRequest requestWithoutReply = buildCommRequestWithoutReply();

        CommunicationRequest requestWithReply = buildCommRequestWithReply();

        List<CommunicationRequest> comms = List.of(requestWithoutReply, requestWithReply);

        List<CommunicationRequest> result = CommunicationRequestUtil.getRequestsWithoutReplies(comms);

        assertEquals(1, result.size());
        assertEquals(requestWithoutReply, result.getFirst());
    }

    @Test
    void shouldGetCommunicationRequestById() {
        CommunicationRequest request = buildCommRequest();

        List<CommunicationRequest> comms = List.of(request);

        CommunicationRequest result = CommunicationRequestUtil.getCommunicationRequestFromId(request.getId(), comms);

        assertEquals(request, result);
    }

    @Test
    void shouldThrowExceptionWhenCommunicationRequestNotFound() {
        List<CommunicationRequest> comms = Collections.emptyList();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            CommunicationRequestUtil.getCommunicationRequestFromId("1", comms)
        );

        assertEquals("No communication request found with id: 1", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldAddCommunicationRequest(boolean isFtaCommunicationAndWorkAllocationEnabled) throws IOException {
        List<CommunicationRequest> comms = new java.util.ArrayList<>();
        CommunicationRequestTopic topic = CommunicationRequestTopic.MRN_REVIEW_DECISION_NOTICE_DETAILS;
        String question = "Test question";
        UserDetails userDetails = UserDetails.builder().name("Test User").build();
        when(businessDaysCalculatorService.getBusinessDay(any(LocalDate.class), anyInt())).thenReturn(LocalDate.now());
        CommunicationRequestUtil.addCommunicationRequest(businessDaysCalculatorService,
            comms, topic, question, userDetails, isFtaCommunicationAndWorkAllocationEnabled);

        assertEquals(1, comms.size());
        CommunicationRequest addedRequest = comms.getFirst();
        assertEquals(question, addedRequest.getValue().getRequestMessage());
        assertEquals(topic, addedRequest.getValue().getRequestTopic());
        assertEquals("Test User", addedRequest.getValue().getRequestUserName());
        assertNotNull(addedRequest.getValue().getRequestResponseDueDate());
        if (isFtaCommunicationAndWorkAllocationEnabled) {
            assertEquals("No", addedRequest.getValue().getTaskCreatedForRequest());
        } else {
            assertNull(addedRequest.getValue().getTaskCreatedForRequest());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldAddCommunicationRequestNoNameIfNull(boolean isFtaCommunicationAndWorkAllocationEnabled) throws IOException {
        List<CommunicationRequest> comms = new java.util.ArrayList<>();
        CommunicationRequestTopic topic = CommunicationRequestTopic.MRN_REVIEW_DECISION_NOTICE_DETAILS;
        String question = "Test question";
        when(businessDaysCalculatorService.getBusinessDay(any(LocalDate.class), anyInt())).thenReturn(LocalDate.now());
        CommunicationRequestUtil.addCommunicationRequest(businessDaysCalculatorService,
            comms, topic, question, null, isFtaCommunicationAndWorkAllocationEnabled);

        assertEquals(1, comms.size());
        CommunicationRequest addedRequest = comms.getFirst();
        assertEquals(question, addedRequest.getValue().getRequestMessage());
        assertEquals(topic, addedRequest.getValue().getRequestTopic());
        assertNull(addedRequest.getValue().getRequestUserName());
        assertNotNull(addedRequest.getValue().getRequestResponseDueDate());
        if (isFtaCommunicationAndWorkAllocationEnabled) {
            assertEquals("No", addedRequest.getValue().getTaskCreatedForRequest());
        } else {
            assertNull(addedRequest.getValue().getTaskCreatedForRequest());
        }
    }

    @ParameterizedTest
    @EnumSource(value = CommunicationRequestTopic.class)
    void shouldCreateDynamicListItemFromCommunicationRequest(CommunicationRequestTopic communicationRequestTopic) {
        CommunicationRequest request = CommunicationRequest.builder()
            .value(CommunicationRequestDetails.builder()
                .requestTopic(communicationRequestTopic)
                .requestDateTime(LocalDateTime.of(2023, 1, 1, 10, 0))
                .requestUserName("Test User")
                .requestUserRole("Test role")
                .build())
            .build();

        DynamicListItem result = CommunicationRequestUtil.getDlItemFromCommunicationRequest(request);

        assertEquals(request.getId(), result.getCode());
        assertEquals(communicationRequestTopic.getValue() + " - 01 January 2023, 10:00 - Test User - Test role", result.getLabel());
    }

    @Test
    void shouldReturnAllRequestsFromFtaAndTribunalCommunications() {
        CommunicationRequest ftaRequest = buildCustomCommRequest("", "", 0, 0);

        CommunicationRequest tribunalRequest = buildCustomCommRequest("", "", 1, 1);

        FtaCommunicationFields communicationFields = FtaCommunicationFields.builder()
            .ftaCommunications(List.of(ftaRequest))
            .tribunalCommunications(List.of(tribunalRequest))
            .build();

        List<CommunicationRequest> result = CommunicationRequestUtil.getAllRequests(communicationFields);

        assertEquals(2, result.size());
        assertEquals(tribunalRequest, result.getFirst());
        assertEquals(ftaRequest, result.getLast());
    }

    @Test
    void shouldReturnEmptyListWhenCommunicationFieldsIsNull() {
        List<CommunicationRequest> result = CommunicationRequestUtil.getAllRequests(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyListWhenBothFtaAndTribunalCommunicationsAreNull() {
        FtaCommunicationFields communicationFields = FtaCommunicationFields.builder().build();

        List<CommunicationRequest> result = CommunicationRequestUtil.getAllRequests(communicationFields);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldSetCommRequestDateFiltersCorrectly() {
        CommunicationRequest requestWithoutReply = buildCommRequestWithoutReply();
        requestWithoutReply.getValue().setRequestResponseDueDate(LocalDate.of(2023, 1, 5));
        CommunicationRequest requestWithReply = buildCommRequestWithReply();
        requestWithReply.getValue().getRequestReply().setReplyDateTime(LocalDateTime.of(2023, 1, 1, 10, 0));
        CommunicationRequest replyWithoutReview = buildCommRequestWithReply();
        requestWithReply.getValue().getRequestReply().setReplyDateTime(LocalDateTime.of(2022, 2, 2, 10, 0));
        CommunicationRequest replyWithReview = buildCommRequestWithActionedReply(true);
        replyWithReview.getValue().getRequestReply().setReplyDateTime(LocalDateTime.of(2022, 3, 3, 10, 0));

        FtaCommunicationFields communicationFields = FtaCommunicationFields.builder()
            .ftaCommunications(List.of(replyWithReview, replyWithoutReview, requestWithReply, requestWithoutReply))
            .tribunalCommunications(List.of(replyWithReview, replyWithoutReview, requestWithReply, requestWithoutReply))
            .build();

        CommunicationRequestUtil.setCommRequestFilters(communicationFields);

        assertEquals(LocalDate.of(2023, 1, 5), communicationFields.getFtaResponseDueDate());
        assertEquals(LocalDate.of(2023, 1, 5), communicationFields.getTribunalResponseDueDate());
        assertEquals(LocalDate.of(2022, 2, 2), communicationFields.getTribunalResponseProvidedDate());
        assertEquals(LocalDate.of(2022, 2, 2), communicationFields.getFtaResponseProvidedDate());
    }

    @Test
    void shouldHandleNullFieldsInsetCommRequestFilters() {
        FtaCommunicationFields communicationFields = FtaCommunicationFields.builder().build();

        CommunicationRequestUtil.setCommRequestFilters(communicationFields);

        assertNull(communicationFields.getFtaResponseProvidedDate());
        assertNull(communicationFields.getTribunalResponseProvidedDate());
        assertNull(communicationFields.getFtaResponseDueDate());
        assertNull(communicationFields.getTribunalResponseDueDate());
    }

    @Test
    void shouldSetCommRequestYesNoFiltersIfRequired() {
        CommunicationRequest requestWithoutReply = buildCommRequestWithoutReply();
        requestWithoutReply.getValue().setRequestResponseDueDate(LocalDate.of(2023, 1, 5));

        CommunicationRequest replyWithoutReview = buildCommRequestWithReply();
        replyWithoutReview.getValue().getRequestReply().setReplyDateTime(LocalDateTime.of(2022, 2, 2, 10, 0));

        FtaCommunicationFields communicationFields = FtaCommunicationFields.builder()
            .ftaCommunications(List.of(replyWithoutReview, requestWithoutReply))
            .tribunalCommunications(List.of(replyWithoutReview, requestWithoutReply))
            .build();

        CommunicationRequestUtil.setCommRequestFilters(communicationFields);
        assertEquals(YES, communicationFields.getAwaitingInfoFromFta());
        assertEquals(YES, communicationFields.getInfoProvidedByFta());
        assertEquals(YES, communicationFields.getInfoRequestFromFta());
        assertEquals(YES, communicationFields.getInfoRequestFromTribunal());
        assertEquals(YES, communicationFields.getAwaitingInfoFromTribunal());
        assertEquals(YES, communicationFields.getInfoProvidedByTribunal());
    }

    @Test
    void shouldWipeCommRequestYesNoFiltersIfRequired() {
        CommunicationRequest replyWithReview = buildCommRequestWithActionedReply(true);

        FtaCommunicationFields communicationFields = FtaCommunicationFields.builder()
            .ftaCommunications(List.of(replyWithReview))
            .awaitingInfoFromFta(YES)
            .infoProvidedByFta(YES)
            .infoRequestFromFta(YES)
            .infoRequestFromTribunal(YES)
            .awaitingInfoFromTribunal(YES)
            .infoProvidedByTribunal(YES)
            .build();

        CommunicationRequestUtil.setCommRequestFilters(communicationFields);
        assertNull(communicationFields.getAwaitingInfoFromFta());
        assertNull(communicationFields.getInfoProvidedByFta());
        assertNull(communicationFields.getInfoRequestFromFta());
        assertNull(communicationFields.getInfoRequestFromTribunal());
        assertNull(communicationFields.getAwaitingInfoFromTribunal());
        assertNull(communicationFields.getInfoProvidedByTribunal());
    }
}
