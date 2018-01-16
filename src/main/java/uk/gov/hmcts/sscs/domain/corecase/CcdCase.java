package uk.gov.hmcts.sscs.domain.corecase;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.sscs.domain.corecase.EventType.*;
import static uk.gov.hmcts.sscs.model.AppConstants.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CcdCase {

    private Appeal appeal;
    private Appellant appellant;
    private Appointee appointee;
    private Representative representative;
    private List<Hearing> hearings;
    private String caseReference;
    private EventType eventType;
    private List<Event> events;
    private ReasonsForAppealing reasonsForAppealing;
    private SmsNotify smsNotify;
    private Boolean isAppointee;
    private String benefitType;
    private String appealStatus;

    public CcdCase() {
    }

    public CcdCase(Appeal appeal, Appellant appellant, Appointee appointee,
                   Representative representative, List<Hearing> hearings) {
        this.appeal = appeal;
        this.appellant = appellant;
        this.appointee = appointee;
        this.representative = representative;
        this.hearings = hearings;
    }

    public CcdCase(Appeal appeal, Appellant appellant, Appointee appointee,
                   Representative representative, List<Hearing> hearings, String caseReference,
                   EventType eventType, List<Event> events) {
        this.appeal = appeal;
        this.appellant = appellant;
        this.appointee = appointee;
        this.representative = representative;
        this.hearings = hearings;
        this.caseReference = caseReference;
        this.eventType = eventType;
        this.events = events;
    }

    public Appeal getAppeal() {
        return appeal;
    }

    public void setAppeal(Appeal appeal) {
        this.appeal = appeal;
    }

    public Appellant getAppellant() {
        return appellant;
    }

    public void setAppellant(Appellant appellant) {
        this.appellant = appellant;
    }

    public Appointee getAppointee() {
        return appointee;
    }

    public void setAppointee(Appointee appointee) {
        this.appointee = appointee;
    }

    public Representative getRepresentative() {
        return representative;
    }

    public void setRepresentative(Representative representative) {
        this.representative = representative;
    }

    //TODO: Assume there is one hearing until business decides how to handle multiple for robotics
    public Hearing getHearing() {
        if (hearings == null || hearings.isEmpty()) {
            return null;
        }
        return hearings.get(0);
    }

    public List<Hearing> getHearings() {
        return hearings;
    }

    public void setHearings(List<Hearing> hearings) {
        this.hearings = hearings;
    }

    public String getCaseReference() {
        return caseReference;
    }

    public void setCaseReference(String caseReference) {
        this.caseReference = caseReference;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public ReasonsForAppealing getReasonsForAppealing() {
        return reasonsForAppealing;
    }

    public void setReasonsForAppealing(ReasonsForAppealing reasonsForAppealing) {
        this.reasonsForAppealing = reasonsForAppealing;
    }

    public SmsNotify getSmsNotify() {
        return smsNotify;
    }

    public void setSmsNotify(SmsNotify smsNotify) {
        this.smsNotify = smsNotify;
    }

    public Boolean getIsAppointee() {
        return isAppointee;
    }

    public void setIsAppointee(Boolean isAppointee) {
        this.isAppointee = isAppointee;
    }

    public String getBenefitType() {
        return benefitType;
    }

    public void setBenefitType(String benefitType) {
        this.benefitType = benefitType;
    }

    public String getAppealStatus() {
        return appealStatus;
    }

    public void setAppealStatus(String appealStatus) {
        this.appealStatus = appealStatus;
    }

    public List<Event> getEvents() {
        if (events == null) {
            buildEvents();
        }

        return events;
    }

    private void buildEvents() {
        //TODO: Placeholder, build up events in a proper way. Refer to TYA API
        this.events = new ArrayList<>();
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public List<Event> buildLatestEvents() {
        List<Event> latestEvents = new ArrayList<>();
        List<Event> sortedEvents = sortedEvents(getEvents());

        for (Event event: sortedEvents) {
            if (event.getType().equals(EVIDENCE_RECEIVED)) {
                latestEvents.add(event);
            } else {
                latestEvents.add(event);
                break;
            }
        }

        List<Event> latestAppealEvents = latestEvents.stream().sorted((e1, e2)
            -> e2.getDate().compareTo(e1.getDate())).collect(toList());
        processExceptions(sortedEvents, latestAppealEvents);

        setLatestAppealStatus(latestAppealEvents);

        return latestAppealEvents;
    }

    private void setLatestAppealStatus(List<Event> latestEvents) {
        if (null != latestEvents && !latestEvents.isEmpty()) {
            for (Event latestEvent : latestEvents) {

                if (latestEvent.getType().getOrder() > 0) {
                    setAppealStatus(latestEvent.getType().name());
                    return;
                }
            }
        }
    }

    private void processExceptions(List<Event> sortedEvents, List<Event> latestAppealEvents) {
        if (null != sortedEvents && !sortedEvents.isEmpty()) {

            Event currentEvent = sortedEvents.get(0);

            if (isPastHearingBookedDate(currentEvent)) {
                setLatestAppealEventStatus(latestAppealEvents, PAST_HEARING_BOOKED);
            } else if (isNewHearingBookedEvent(sortedEvents)) {
                setLatestAppealEventStatus(latestAppealEvents, NEW_HEARING_BOOKED);
            } else if (isAppealClosed(currentEvent)) {
                setLatestAppealEventStatus(latestAppealEvents, CLOSED);
            } else if (isDwpRespondOverdue(currentEvent)) {
                setLatestAppealEventStatus(latestAppealEvents, DWP_RESPOND_OVERDUE);
            }
        }
    }

    private boolean isDwpRespondOverdue(Event currentEvent) {
        return currentEvent.getType() == APPEAL_RECEIVED
                && ZonedDateTime.now().isAfter(currentEvent.getDate().plusDays(MAX_DWP_RESPONSE_DAYS));
    }

    private boolean isAppealClosed(Event currentEvent) {
        return currentEvent.getType() == DORMANT
                && ZonedDateTime.now().isAfter(currentEvent.getDate().plusMonths(
                DORMANT_TO_CLOSED_DURATION_IN_MONTHS));
    }

    private boolean isPastHearingBookedDate(Event currentEvent) {
        return currentEvent.getType() == DWP_RESPOND
                && ZonedDateTime.now().isAfter(currentEvent.getDate().plusWeeks(
                PAST_HEARING_BOOKED_IN_WEEKS));
    }

    private boolean isNewHearingBookedEvent(List<Event> sortedEvents) {
        return sortedEvents.size() > 1
                && sortedEvents.get(0).getType() == HEARING_BOOKED
                && (sortedEvents.get(1).getType() == POSTPONED
                || sortedEvents.get(1).getType() == ADJOURNED);
    }

    public List<Event> buildHistoricalEvents() {
        List<Event> sortedEvents = sortedEvents(getEvents());

        List<Event> historicalEvents;

        if (buildLatestEvents().size() == 1 && buildLatestEvents().get(0).getType()
                == DWP_RESPOND_OVERDUE) {
            historicalEvents = new ArrayList<>(sortedEvents);
        } else {
            historicalEvents = sortedEvents.stream().skip(buildLatestEvents().size())
                    .collect(toList());
        }
        return historicalEvents.stream().sorted((e1,e2) ->
                e2.getDate().compareTo(e1.getDate())).collect(toList());
    }

    private void setLatestAppealEventStatus(List<Event> latestEvents, EventType eventType) {

        for (Event event: latestEvents) {
            if (event.getType().getOrder() > 0) {
                event.setType(eventType);
                break;
            }
        }
    }

    private List<Event> sortedEvents(List<Event> eventsToBeSorted) {
        return eventsToBeSorted.stream().sorted((e1,e2) ->
                e2.getDate().compareTo(e1.getDate())).collect(toList());
    }

    @Override
    public String toString() {
        return "CcdCase{"
                + " appeal=" + appeal
                + ", appellant=" + appellant
                + ", appointee=" + appointee
                + ", representative=" + representative
                + ", hearings=" + hearings
                + ", caseReference=" + caseReference
                + ", eventType=" + eventType
                + ", events=" + events
                + ", reasonsForAppealing=" + reasonsForAppealing
                + ", smsNotify=" + smsNotify
                + ", isAppointee=" + isAppointee
                + ", benefitType=" + benefitType
                + ", appealStatus=" + appealStatus
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CcdCase ccdCase = (CcdCase) o;
        return Objects.equals(appeal, ccdCase.appeal)
                && Objects.equals(appellant, ccdCase.appellant)
                && Objects.equals(appointee, ccdCase.appointee)
                && Objects.equals(representative, ccdCase.representative)
                && Objects.equals(hearings, ccdCase.hearings)
                && Objects.equals(caseReference, ccdCase.caseReference)
                && eventType == ccdCase.eventType
                && Objects.equals(events, ccdCase.events)
                && Objects.equals(reasonsForAppealing, ccdCase.reasonsForAppealing)
                && Objects.equals(smsNotify, ccdCase.smsNotify)
                && Objects.equals(isAppointee, ccdCase.isAppointee)
                && Objects.equals(benefitType, ccdCase.benefitType)
                && Objects.equals(appealStatus, ccdCase.appealStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appeal, appellant, appointee, representative, hearings, caseReference,
                eventType, events, reasonsForAppealing, smsNotify, isAppointee, benefitType,
                appealStatus);
    }
}
