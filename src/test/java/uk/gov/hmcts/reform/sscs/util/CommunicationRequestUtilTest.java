package uk.gov.hmcts.reform.sscs.util;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;

class CommunicationRequestUtilTest {

    @Test
    public void testConstructorIsPrivate() throws NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        Constructor<CommunicationRequestUtil> constructor = CommunicationRequestUtil.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    void shouldReturnRepliesWithoutReviews() {
        CommunicationRequest requestWithReply = CommunicationRequest.builder()
            .value(CommunicationRequestDetails.builder()
                .requestReply(CommunicationRequestReply.builder().replyHasBeenActioned(YesNo.NO).build())
                .build())
            .build();

        CommunicationRequest requestWithActionedReply = CommunicationRequest.builder()
            .value(CommunicationRequestDetails.builder()
                .requestReply(CommunicationRequestReply.builder().replyHasBeenActioned(YesNo.YES).build())
                .build())
            .build();

        CommunicationRequest requestWithNoReply = CommunicationRequest.builder()
            .value(CommunicationRequestDetails.builder()
                .build())
            .build();

        List<CommunicationRequest> comms = List.of(requestWithReply, requestWithActionedReply, requestWithNoReply);

        List<CommunicationRequest> result = CommunicationRequestUtil.getRepliesWithoutReviews(comms);

        assertEquals(1, result.size());
        assertEquals(requestWithReply, result.getFirst());
    }

    @Test
    void shouldReturnOldestResponseDate() {
        CommunicationRequest request1 = CommunicationRequest.builder()
            .value(CommunicationRequestDetails.builder()
                .requestResponseDueDate(LocalDate.of(2023, 1, 1))
                .build())
            .build();

        CommunicationRequest request2 = CommunicationRequest.builder()
            .value(CommunicationRequestDetails.builder()
                .requestResponseDueDate(LocalDate.of(2022, 1, 1))
                .build())
            .build();

        List<CommunicationRequest> comms = List.of(request1, request2);

        LocalDate result = CommunicationRequestUtil.getOldestResponseDate(comms);

        assertEquals(LocalDate.of(2022, 1, 1), result);
    }

    @Test
    void shouldReturnNullWhenNoResponseDatesExist() {
        List<CommunicationRequest> comms = Collections.emptyList();

        LocalDate result = CommunicationRequestUtil.getOldestResponseDate(comms);

        assertNull(result);
    }

    @Test
    void shouldReturnOldestResponseProvidedDate() {
        CommunicationRequest request1 = CommunicationRequest.builder()
            .value(CommunicationRequestDetails.builder()
                .requestReply(CommunicationRequestReply.builder()
                    .replyDateTime(LocalDateTime.of(2023, 1, 1, 10, 0))
                    .build())
                .build())
            .build();

        CommunicationRequest request2 = CommunicationRequest.builder()
            .value(CommunicationRequestDetails.builder()
                .requestReply(CommunicationRequestReply.builder()
                    .replyDateTime(LocalDateTime.of(2022, 1, 1, 10, 0))
                    .build())
                .build())
            .build();

        List<CommunicationRequest> comms = List.of(request1, request2);

        LocalDate result = CommunicationRequestUtil.getOldestResponseProvidedDate(comms);

        assertEquals(LocalDate.of(2022, 1, 1), result);
    }

    @Test
    void shouldReturnRequestsWithoutReplies() {
        CommunicationRequest requestWithoutReply = CommunicationRequest.builder()
            .value(CommunicationRequestDetails.builder().build())
            .build();

        CommunicationRequest requestWithReply = CommunicationRequest.builder()
            .value(CommunicationRequestDetails.builder()
                .requestReply(CommunicationRequestReply.builder().build())
                .build())
            .build();

        List<CommunicationRequest> comms = List.of(requestWithoutReply, requestWithReply);

        List<CommunicationRequest> result = CommunicationRequestUtil.getRequestsWithoutReplies(comms);

        assertEquals(1, result.size());
        assertEquals(requestWithoutReply, result.getFirst());
    }

    @Test
    void shouldGetCommunicationRequestById() {
        CommunicationRequest request = CommunicationRequest.builder()
            .id("1")
            .value(CommunicationRequestDetails.builder().build())
            .build();

        List<CommunicationRequest> comms = List.of(request);

        CommunicationRequest result = CommunicationRequestUtil.getCommunicationRequestFromId("1", comms);

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

    @Test
    void shouldAddCommunicationRequest() {
        List<CommunicationRequest> comms = new java.util.ArrayList<>();
        CommunicationRequestTopic topic = CommunicationRequestTopic.MRN_REVIEW_DECISION_NOTICE_DETAILS;
        String question = "Test question";
        UserDetails userDetails = UserDetails.builder().name("Test User").build();

        CommunicationRequestUtil.addCommunicationRequest(comms, topic, question, userDetails);

        assertEquals(1, comms.size());
        CommunicationRequest addedRequest = comms.getFirst();
        assertEquals(question, addedRequest.getValue().getRequestMessage());
        assertEquals(topic, addedRequest.getValue().getRequestTopic());
        assertEquals("Test User", addedRequest.getValue().getRequestUserName());
        assertNotNull(addedRequest.getValue().getRequestResponseDueDate());
    }

    @Test
    void shouldAddCommunicationRequestNoNameIfNull() {
        List<CommunicationRequest> comms = new java.util.ArrayList<>();
        CommunicationRequestTopic topic = CommunicationRequestTopic.MRN_REVIEW_DECISION_NOTICE_DETAILS;
        String question = "Test question";

        CommunicationRequestUtil.addCommunicationRequest(comms, topic, question, null);

        assertEquals(1, comms.size());
        CommunicationRequest addedRequest = comms.getFirst();
        assertEquals(question, addedRequest.getValue().getRequestMessage());
        assertEquals(topic, addedRequest.getValue().getRequestTopic());
        assertNull(addedRequest.getValue().getRequestUserName());
        assertNotNull(addedRequest.getValue().getRequestResponseDueDate());
    }

    @ParameterizedTest
    @EnumSource(value = CommunicationRequestTopic.class)
    void shouldCreateDynamicListItemFromCommunicationRequest(CommunicationRequestTopic communicationRequestTopic) {
        CommunicationRequest request = CommunicationRequest.builder()
            .id("1")
            .value(CommunicationRequestDetails.builder()
                .requestTopic(communicationRequestTopic)
                .requestDateTime(LocalDateTime.of(2023, 1, 1, 10, 0))
                .requestUserName("Test User")
                .requestUserRole("Test role")
                .build())
            .build();

        DynamicListItem result = CommunicationRequestUtil.getDlItemFromCommunicationRequest(request);

        assertEquals("1", result.getCode());
        assertEquals(communicationRequestTopic.getValue() + " - 01 January 2023, 10:00 - Test User - Test role", result.getLabel());
    }

    @Test
    void shouldReturnAllRequestsFromFtaAndTribunalCommunications() {
        CommunicationRequest ftaRequest = CommunicationRequest.builder()
            .value(CommunicationRequestDetails.builder()
                .requestDateTime(LocalDateTime.of(2023, 1, 1, 10, 0))
                .build())
            .build();

        CommunicationRequest tribunalRequest = CommunicationRequest.builder()
            .value(CommunicationRequestDetails.builder()
                .requestDateTime(LocalDateTime.of(2022, 1, 1, 10, 0))
                .build())
            .build();

        FtaCommunicationFields communicationFields = FtaCommunicationFields.builder()
            .ftaCommunications(List.of(ftaRequest))
            .tribunalCommunications(List.of(tribunalRequest))
            .build();

        List<CommunicationRequest> result = CommunicationRequestUtil.getAllRequests(communicationFields);

        assertEquals(2, result.size());
        assertEquals(ftaRequest, result.get(0)); // Sorted by date descending
        assertEquals(tribunalRequest, result.get(1));
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
        CommunicationRequest requestWithoutReply = CommunicationRequest.builder()
            .value(CommunicationRequestDetails.builder()
                .requestResponseDueDate(LocalDate.of(2023, 1, 5))
                .build())
            .build();
        CommunicationRequest requestWithReply = CommunicationRequest.builder()
            .value(CommunicationRequestDetails.builder()
                .requestReply(CommunicationRequestReply.builder()
                    .replyDateTime(LocalDateTime.of(2023, 1, 1, 10, 0))
                    .replyHasBeenActioned(YesNo.NO)
                    .build())
                .build())
            .build();
        CommunicationRequest replyWithoutReview = CommunicationRequest.builder()
            .value(CommunicationRequestDetails.builder()
                .requestReply(CommunicationRequestReply.builder()
                    .replyDateTime(LocalDateTime.of(2022, 2, 2, 10, 0))
                    .replyHasBeenActioned(YesNo.NO)
                    .build())
                .build())
            .build();
        CommunicationRequest replyWithReview = CommunicationRequest.builder()
            .value(CommunicationRequestDetails.builder()
                .requestReply(CommunicationRequestReply.builder()
                    .replyDateTime(LocalDateTime.of(2022, 3, 3, 10, 0))
                    .replyHasBeenActioned(YesNo.YES)
                    .build())
                .build())
            .build();

        FtaCommunicationFields communicationFields = FtaCommunicationFields.builder()
            .ftaCommunications(List.of(replyWithReview, replyWithoutReview, requestWithReply, requestWithoutReply))
            .tribunalCommunications(List.of(replyWithReview, replyWithoutReview, requestWithReply, requestWithoutReply))
            .build();

        CommunicationRequestUtil.setCommRequestDateFilters(communicationFields);

        assertEquals(LocalDate.of(2023, 1, 5), communicationFields.getFtaResponseDueDate());
        assertEquals(LocalDate.of(2023, 1, 5), communicationFields.getTribunalResponseDueDate());
        assertEquals(LocalDate.of(2022, 2, 2), communicationFields.getTribunalResponseProvidedDate());
        assertEquals(LocalDate.of(2022, 2, 2), communicationFields.getFtaResponseProvidedDate());
    }

    @Test
    void shouldHandleNullFieldsInSetCommRequestDateFilters() {
        FtaCommunicationFields communicationFields = FtaCommunicationFields.builder().build();

        CommunicationRequestUtil.setCommRequestDateFilters(communicationFields);

        assertNull(communicationFields.getFtaResponseProvidedDate());
        assertNull(communicationFields.getTribunalResponseProvidedDate());
        assertNull(communicationFields.getFtaResponseDueDate());
        assertNull(communicationFields.getTribunalResponseDueDate());
    }
}
