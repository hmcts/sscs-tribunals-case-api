package uk.gov.hmcts.sscs.domain.corecase;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;
import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.json.CcdCaseDeserializer;

@XmlRootElement
@XmlType(propOrder = {"appeal", "appellant", "appointee", "representative", "hearing"})
@JsonDeserialize(using = CcdCaseDeserializer.class)
public class CcdCase {

    private Appeal appeal;
    private Appellant appellant;
    private Appointee appointee;
    private Representative representative;
    private Hearing hearing;
    private String caseReference;
    private Status status;
    private List<Event> events;
    private ReasonsForAppealing reasonsForAppealing;
    private SmsNotify smsNotify;
    private Boolean isAppointee;

    public CcdCase() {
    }

    public CcdCase(Appeal appeal, Appellant appellant, Appointee appointee,
                   Representative representative, Hearing hearing) {
        this.appeal = appeal;
        this.appellant = appellant;
        this.appointee = appointee;
        this.representative = representative;
        this.hearing = hearing;
    }

    public CcdCase(Appeal appeal, Appellant appellant, Appointee appointee,
                   Representative representative, Hearing hearing, String caseReference,
                   Status status, List<Event> events) {
        this.appeal = appeal;
        this.appellant = appellant;
        this.appointee = appointee;
        this.representative = representative;
        this.hearing = hearing;
        this.caseReference = caseReference;
        this.status = status;
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

    public Hearing getHearing() {
        return hearing;
    }

    public void setHearing(Hearing hearing) {
        this.hearing = hearing;
    }

    @XmlTransient
    public String getCaseReference() {
        return caseReference;
    }

    public void setCaseReference(String caseReference) {
        this.caseReference = caseReference;
    }

    @XmlTransient
    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    @XmlTransient
    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @XmlTransient
    public ReasonsForAppealing getReasonsForAppealing() {
        return reasonsForAppealing;
    }

    public void setReasonsForAppealing(ReasonsForAppealing reasonsForAppealing) {
        this.reasonsForAppealing = reasonsForAppealing;
    }

    @XmlTransient
    public SmsNotify getSmsNotify() {
        return smsNotify;
    }

    public void setSmsNotify(SmsNotify smsNotify) {
        this.smsNotify = smsNotify;
    }

    @XmlTransient
    public Boolean getIsAppointee() {
        return isAppointee;
    }

    public void setIsAppointee(Boolean isAppointee) {
        this.isAppointee = isAppointee;
    }

    @Override
    public String toString() {
        return "CcdCase{"
                + " appeal=" + appeal
                + ", appellant=" + appellant
                + ", appointee=" + appointee
                + ", representative=" + representative
                + ", hearing=" + hearing
                + ", caseReference=" + caseReference
                + ", status=" + status
                + ", events=" + events
                + ", reasonsForAppealing=" + reasonsForAppealing
                + ", smsNotify=" + smsNotify
                + ", isAppointee=" + isAppointee
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
                && Objects.equals(hearing, ccdCase.hearing)
                && Objects.equals(caseReference, ccdCase.caseReference)
                && status == ccdCase.status
                && Objects.equals(events, ccdCase.events)
                && Objects.equals(reasonsForAppealing, ccdCase.reasonsForAppealing)
                && Objects.equals(smsNotify, ccdCase.smsNotify)
                && Objects.equals(isAppointee, ccdCase.isAppointee);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appeal, appellant, appointee, representative, hearing, caseReference,
                status, events, reasonsForAppealing, smsNotify, isAppointee);
    }

}
