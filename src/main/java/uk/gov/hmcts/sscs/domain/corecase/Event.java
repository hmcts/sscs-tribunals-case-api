package uk.gov.hmcts.sscs.domain.corecase;

import java.time.ZonedDateTime;
import java.util.Objects;
import javax.xml.bind.annotation.XmlTransient;

public class Event {

    private ZonedDateTime date;
    private ZonedDateTime dwpResponseDate;
    private EventType type;
    private String evidenceType;
    private String evidenceProvidedBy;
    private String contentKey;
    private Hearing hearing;
    private ZonedDateTime decisionLetterReceivedByDate;
    private ZonedDateTime adjournedLetterReceivedByDate;
    private ZonedDateTime adjournedDate;
    private ZonedDateTime hearingContactDate;
    private String placeholders;

    public Event(ZonedDateTime date, ZonedDateTime dwpResponseDate, EventType type,
                 String evidenceType, String contentKey) {
        this.date = date;
        this.dwpResponseDate = dwpResponseDate;
        this.type = type;
        this.evidenceType = evidenceType;
        this.contentKey = contentKey;
    }

    public Event(ZonedDateTime date, ZonedDateTime dwpResponseDate, EventType type,
                 String evidenceType, String evidenceProvidedBy, String contentKey,
                 Hearing hearing, ZonedDateTime decisionLetterReceivedByDate,
                 ZonedDateTime adjournedLetterReceivedByDate, ZonedDateTime adjournedDate,
                 ZonedDateTime hearingContactDate) {
        this.date = date;
        this.dwpResponseDate = dwpResponseDate;
        this.type = type;
        this.evidenceType = evidenceType;
        this.evidenceProvidedBy = evidenceProvidedBy;
        this.contentKey = contentKey;
        this.hearing = hearing;
        this.decisionLetterReceivedByDate = decisionLetterReceivedByDate;
        this.adjournedLetterReceivedByDate = adjournedLetterReceivedByDate;
        this.adjournedDate = adjournedDate;
        this.hearingContactDate = hearingContactDate;
    }

    public Event(ZonedDateTime date, EventType type) {
        this.date = date;
        this.type = type;
    }

    public ZonedDateTime getDate() {
        return date;
    }

    public void setDate(ZonedDateTime date) {
        this.date = date;
    }

    public ZonedDateTime getDwpResponseDate() {
        return dwpResponseDate;
    }

    public void setDwpResponseDate(ZonedDateTime dwpResponseDate) {
        this.dwpResponseDate = dwpResponseDate;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public String getEvidenceType() {
        return evidenceType;
    }

    public void setEvidenceType(String evidenceType) {
        this.evidenceType = evidenceType;
    }

    public String getContentKey() {
        return contentKey;
    }

    public void setContentKey(String contentKey) {
        this.contentKey = contentKey;
    }

    public Hearing getHearing() {
        return hearing;
    }

    public void setHearing(Hearing hearing) {
        this.hearing = hearing;
    }

    public ZonedDateTime getDecisionLetterReceivedByDate() {
        return decisionLetterReceivedByDate;
    }

    public void setDecisionLetterReceivedByDate(ZonedDateTime decisionLetterReceivedByDate) {
        this.decisionLetterReceivedByDate = decisionLetterReceivedByDate;
    }

    public String getEvidenceProvidedBy() {
        return evidenceProvidedBy;
    }

    public void setEvidenceProvidedBy(String evidenceProvidedBy) {
        this.evidenceProvidedBy = evidenceProvidedBy;
    }

    public ZonedDateTime getAdjournedLetterReceivedByDate() {
        return adjournedLetterReceivedByDate;
    }

    public void setAdjournedLetterReceivedByDate(ZonedDateTime adjournedLetterReceivedByDate) {
        this.adjournedLetterReceivedByDate = adjournedLetterReceivedByDate;
    }

    public ZonedDateTime getAdjournedDate() {
        return adjournedDate;
    }

    public void setAdjournedDate(ZonedDateTime adjournedDate) {
        this.adjournedDate = adjournedDate;
    }

    public ZonedDateTime getHearingContactDate() {
        return hearingContactDate;
    }

    public void setHearingContactDate(ZonedDateTime hearingContactDate) {
        this.hearingContactDate = hearingContactDate;
    }

    @XmlTransient
    public String getPlaceholders() {
        return placeholders;
    }

    public void setPlaceholders(String placeholders) {
        this.placeholders = placeholders;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Event)) {
            return false;
        }
        Event event = (Event) o;
        return Objects.equals(date, event.date)
                && Objects.equals(dwpResponseDate, event.dwpResponseDate)
                && type == event.type
                && Objects.equals(evidenceType, event.evidenceType)
                && Objects.equals(evidenceProvidedBy, event.evidenceProvidedBy)
                && Objects.equals(contentKey, event.contentKey)
                && Objects.equals(hearing, event.hearing)
                && Objects.equals(decisionLetterReceivedByDate, event.decisionLetterReceivedByDate)
                && Objects.equals(adjournedLetterReceivedByDate,
                    event.adjournedLetterReceivedByDate)
                && Objects.equals(adjournedDate, event.adjournedDate)
                && Objects.equals(hearingContactDate, event.hearingContactDate)
                && Objects.equals(placeholders, event.placeholders);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, dwpResponseDate, type, evidenceType, evidenceProvidedBy,
                contentKey, hearing, decisionLetterReceivedByDate, adjournedLetterReceivedByDate,
                adjournedDate, hearingContactDate, placeholders);
    }
}
