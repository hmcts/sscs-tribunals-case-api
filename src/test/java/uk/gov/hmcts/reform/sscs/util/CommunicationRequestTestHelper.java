package uk.gov.hmcts.reform.sscs.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequestDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequestReply;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequestTopic;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

public final class CommunicationRequestTestHelper {

    private CommunicationRequestTestHelper() {
    }

    public static CommunicationRequest buildCustomCommRequest(String message, String username, int requestDateTimeOffset, int responseDueDateOffset) {
        return CommunicationRequest.builder()
            .value(
                CommunicationRequestDetails.builder()
                    .requestTopic(CommunicationRequestTopic.APPEAL_TYPE)
                    .requestMessage(message)
                    .requestDateTime(LocalDateTime.now().plusYears(requestDateTimeOffset))
                    .requestUserName(username)
                    .requestResponseDueDate(LocalDate.now().plusYears(responseDueDateOffset))
                    .build()
            ).build();
    }

    public static CommunicationRequest buildCommRequest() {
        return CommunicationRequest.builder()
            .value(
                CommunicationRequestDetails.builder()
                    .requestTopic(CommunicationRequestTopic.APPEAL_TYPE)
                    .requestMessage("message")
                    .requestDateTime(LocalDateTime.now())
                    .requestUserName("username")
                    .requestResponseDueDate(LocalDate.now())
                    .build()
            ).build();
    }

    public static DynamicListItem dlItemFromCommRequest(CommunicationRequest ftaCommunication) {
        return new DynamicListItem(ftaCommunication.getId(),
            ftaCommunication.getValue().getRequestTopic().getValue() + " - "
                + ftaCommunication.getValue().getRequestDateTime()
                .format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm")) + " - "
                + ftaCommunication.getValue().getRequestUserName());
    }

    public static CommunicationRequest buildCommRequestWithoutReply() {
        return CommunicationRequest.builder().value(
                CommunicationRequestDetails.builder()
                    .requestUserName("some user")
                    .requestDateTime(LocalDateTime.now())
                    .requestTopic(CommunicationRequestTopic.APPEAL_TYPE)
                    .build())
            .build();
    }

    public static CommunicationRequest buildCommRequestWithReply() {
        return CommunicationRequest.builder()
            .value(
                CommunicationRequestDetails.builder()
                    .requestUserName("some user")
                    .requestDateTime(LocalDateTime.now())
                    .requestTopic(CommunicationRequestTopic.APPEAL_TYPE)
                    .requestMessage("some request message")
                    .requestReply(CommunicationRequestReply.builder()
                        .replyDateTime(LocalDateTime.now())
                        .replyMessage("some reply message")
                        .replyHasBeenActionedByFta(YesNo.NO)
                        .replyHasBeenActionedByTribunal(YesNo.NO)
                        .build())
                    .build())
            .build();
    }

    public static CommunicationRequest buildCommRequestWithNoActionReply() {
        return CommunicationRequest.builder()
            .value(
                CommunicationRequestDetails.builder()
                    .requestUserName("some user")
                    .requestDateTime(LocalDateTime.now())
                    .requestTopic(CommunicationRequestTopic.APPEAL_TYPE)
                    .requestMessage("some request message")
                    .requestReply(CommunicationRequestReply.builder()
                        .replyDateTime(LocalDateTime.now())
                        .replyMessage("some reply message")
                        .replyHasBeenActionedByFta(null)
                        .replyHasBeenActionedByTribunal(null)
                        .build())
                    .build())
            .build();
    }

    public static CommunicationRequest buildCommRequestWithActionedReply(boolean ftaToAction) {
        CommunicationRequestReply.CommunicationRequestReplyBuilder replyBuilder = CommunicationRequestReply.builder()
            .replyDateTime(LocalDateTime.now());
        if (ftaToAction) {
            replyBuilder.replyHasBeenActionedByFta(YesNo.YES);
        } else {
            replyBuilder.replyHasBeenActionedByTribunal(YesNo.YES);
        }
        return CommunicationRequest.builder().value(
                CommunicationRequestDetails.builder()
                    .requestUserName("some user")
                    .requestDateTime(LocalDateTime.now())
                    .requestTopic(CommunicationRequestTopic.APPEAL_TYPE)
                    .requestReply(replyBuilder.build())
                    .build())
            .build();
    }

    public static CommunicationRequest buildCommRequestNotActionedResponseDateOffset(int offset, boolean ftaToAction) {
        CommunicationRequestReply.CommunicationRequestReplyBuilder replyBuilder = CommunicationRequestReply.builder()
            .replyDateTime(LocalDateTime.now().plusYears(offset));
        if (ftaToAction) {
            replyBuilder.replyHasBeenActionedByFta(YesNo.NO);
        } else {
            replyBuilder.replyHasBeenActionedByTribunal(YesNo.NO);
        }
        return CommunicationRequest.builder()
            .value(CommunicationRequestDetails.builder()
                .requestReply(replyBuilder.build())
                .build())
            .build();
    }

    public static CommunicationRequest buildCommRequestWithId(String id) {
        return CommunicationRequest.builder()
            .id(id)
            .value(CommunicationRequestDetails.builder().build())
            .build();
    }
}
