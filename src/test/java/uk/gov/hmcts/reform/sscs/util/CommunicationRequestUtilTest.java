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
        String username = "Test User";

        CommunicationRequestUtil.addCommunicationRequest(comms, topic, question, username);

        assertEquals(1, comms.size());
        CommunicationRequest addedRequest = comms.getFirst();
        assertEquals(question, addedRequest.getValue().getRequestMessage());
        assertEquals(topic, addedRequest.getValue().getRequestTopic());
        assertEquals("Test User", addedRequest.getValue().getRequestUserName());
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
                .build())
            .build();

        DynamicListItem result = CommunicationRequestUtil.getDlItemFromCommunicationRequest(request);

        assertEquals("1", result.getCode());
        assertEquals(communicationRequestTopic.getValue() + " - 01 January 2023, 10:00 - Test User", result.getLabel());
    }
}
