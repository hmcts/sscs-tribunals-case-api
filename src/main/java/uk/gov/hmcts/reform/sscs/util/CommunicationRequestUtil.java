package uk.gov.hmcts.reform.sscs.util;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequestDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CommunicationRequestTopic;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.FtaCommunicationFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.idam.UserRole;
import uk.gov.hmcts.reform.sscs.utility.calendar.BusinessDaysCalculatorService;

@Slf4j
public class CommunicationRequestUtil {
    private CommunicationRequestUtil() {
        //
    }

    public static List<CommunicationRequest> getRepliesWithoutReviews(List<CommunicationRequest> comms) {
        return Optional.ofNullable(comms)
            .orElse(Collections.emptyList())
            .stream()
            .filter((request -> request.getValue().getRequestReply() != null))
            .filter((request ->
                request.getValue().getRequestReply().getReplyHasBeenActionedByFta() == YesNo.NO
                    || request.getValue().getRequestReply().getReplyHasBeenActionedByTribunal() == YesNo.NO))
            .toList();
    }

    public static LocalDate getOldestResponseDate(List<CommunicationRequest> communicationRequests) {
        List<CommunicationRequest> sortedList = communicationRequests.stream()
            .filter(communicationRequest -> communicationRequest.getValue().getRequestReply() == null)
            .filter(communicationRequest -> communicationRequest.getValue().getRequestResponseDueDate() != null)
            .sorted(Comparator.comparing(communicationRequest ->
                communicationRequest.getValue().getRequestResponseDueDate()))
            .toList();
        return sortedList.isEmpty() ? null : sortedList
            .getFirst()
            .getValue()
            .getRequestResponseDueDate();
    }

    public static LocalDate getOldestResponseProvidedDate(List<CommunicationRequest> communicationRequests) {
        List<CommunicationRequest> sortedList = communicationRequests.stream()
            .filter(communicationRequest -> communicationRequest.getValue().getRequestReply() != null)
            .filter(communicationRequest -> communicationRequest.getValue().getRequestReply().getReplyDateTime() != null)
            .sorted(Comparator.comparing(communicationRequest ->
                communicationRequest.getValue().getRequestReply().getReplyDateTime()))
            .toList();
        return sortedList.isEmpty() ? null : sortedList
            .getFirst()
            .getValue()
            .getRequestReply()
            .getReplyDateTime()
            .toLocalDate();
    }

    public static List<CommunicationRequest> getRequestsWithoutReplies(List<CommunicationRequest> comms) {
        return Optional.ofNullable(comms).orElse(Collections.emptyList()).stream()
            .filter((request -> request.getValue().getRequestReply() == null))
            .toList();
    }

    public static CommunicationRequest getCommunicationRequestFromId(String id, List<CommunicationRequest> comms) {
        return Optional.ofNullable(comms)
            .orElse(Collections.emptyList())
            .stream()
            .filter(communicationRequest -> communicationRequest.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No communication request found with id: " + id));
    }

    public static void addCommunicationRequest(BusinessDaysCalculatorService service, List<CommunicationRequest> comms, CommunicationRequestTopic topic, String question, UserDetails userDetails) throws IOException {
        LocalDateTime now = ZonedDateTime.now(ZoneId.of("Europe/London")).toLocalDateTime();
        LocalDate dueDate = service.getBusinessDay(now.toLocalDate(), 2);
        comms.add(CommunicationRequest.builder()
            .value(CommunicationRequestDetails.builder()
                .requestMessage(question)
                .requestTopic(topic)
                .requestDateTime(now)
                .requestUserName(isNull(userDetails) ? null : userDetails.getName())
                .requestUserRole(getRoleName(userDetails))
                .requestResponseDueDate(dueDate)
                .build())
            .build());
        comms.sort(Comparator.comparing(communicationRequest ->
            ((CommunicationRequest) communicationRequest).getValue().getRequestDateTime()).reversed());
    }

    public static DynamicListItem getDlItemFromCommunicationRequest(CommunicationRequest communicationRequest) {
        return new DynamicListItem(communicationRequest.getId(), communicationRequest.getValue().toString());
    }

    public static String getRoleName(UserDetails userDetails) {
        return Optional.ofNullable(userDetails)
            .flatMap(details -> Arrays.stream(UserRole.values())
                .filter(details::hasRole)
                .map(UserRole::getLabel)
                .findFirst())
            .orElse(null);
    }

    public static List<CommunicationRequest> getAllRequests(FtaCommunicationFields communicationFields) {
        List<CommunicationRequest> allRequests = new ArrayList<>();
        if (communicationFields != null) {
            Optional.ofNullable(communicationFields.getFtaCommunications())
                .ifPresent(allRequests::addAll);
            Optional.ofNullable(communicationFields.getTribunalCommunications())
                .ifPresent(allRequests::addAll);
        }
        allRequests.sort(Comparator.comparing(communicationRequest ->
            ((CommunicationRequest) communicationRequest).getValue().getRequestDateTime()).reversed());
        return allRequests;
    }

    public static void setCommRequestFilters(FtaCommunicationFields ftaCommunicationFields) {
        List<CommunicationRequest> ftaCommsWithoutReplies = getRequestsWithoutReplies(ftaCommunicationFields.getFtaCommunications());
        if (!ftaCommsWithoutReplies.isEmpty()) {
            ftaCommunicationFields.setFtaResponseDueDate(getOldestResponseDate(ftaCommsWithoutReplies));
            ftaCommunicationFields.setAwaitingInfoFromFta(YES);
            ftaCommunicationFields.setInfoRequestFromTribunal(YES);
        } else {
            ftaCommunicationFields.setFtaResponseDueDate(null);
            ftaCommunicationFields.setAwaitingInfoFromFta(null);
            ftaCommunicationFields.setInfoRequestFromTribunal(null);
        }

        List<CommunicationRequest> tribunalCommsWithoutReplies = getRequestsWithoutReplies(ftaCommunicationFields.getTribunalCommunications());
        if (!tribunalCommsWithoutReplies.isEmpty()) {
            ftaCommunicationFields.setTribunalResponseDueDate(getOldestResponseDate(tribunalCommsWithoutReplies));
            ftaCommunicationFields.setAwaitingInfoFromTribunal(YES);
            ftaCommunicationFields.setInfoRequestFromFta(YES);
        } else {
            ftaCommunicationFields.setTribunalResponseDueDate(null);
            ftaCommunicationFields.setAwaitingInfoFromTribunal(null);
            ftaCommunicationFields.setInfoRequestFromFta(null);
        }

        List<CommunicationRequest> ftaCommsWithoutReviews = getRepliesWithoutReviews(ftaCommunicationFields.getFtaCommunications());
        if (!ftaCommsWithoutReviews.isEmpty()) {
            ftaCommunicationFields.setFtaResponseProvidedDate(getOldestResponseProvidedDate(ftaCommsWithoutReviews));
            ftaCommunicationFields.setInfoProvidedByFta(YES);
        } else {
            ftaCommunicationFields.setFtaResponseProvidedDate(null);
            ftaCommunicationFields.setInfoProvidedByFta(null);
        }

        List<CommunicationRequest> tribunalCommsWithoutReviews = getRepliesWithoutReviews(ftaCommunicationFields.getTribunalCommunications());
        if (!tribunalCommsWithoutReviews.isEmpty()) {
            ftaCommunicationFields.setTribunalResponseProvidedDate(getOldestResponseProvidedDate(tribunalCommsWithoutReviews));
            ftaCommunicationFields.setInfoProvidedByTribunal(YES);
        } else {
            ftaCommunicationFields.setTribunalResponseProvidedDate(null);
            ftaCommunicationFields.setInfoProvidedByTribunal(null);
        }
    }
}

