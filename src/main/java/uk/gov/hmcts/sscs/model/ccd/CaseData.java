package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseData {
    private String caseReference;
    private Appeal appeal;
    private List<Hearing> hearings;
    private Evidence evidence;
    private List<DwpTimeExtension> dwpTimeExtension;
    private List<Events> events;

    public String getCaseReference() {
        return caseReference;
    }

    public void setCaseReference(String caseReference) {
        this.caseReference = caseReference;
    }

    public Appeal getAppeal() {
        return appeal;
    }

    public void setAppeal(Appeal appeal) {
        this.appeal = appeal;
    }

    public List<Hearing> getHearings() {
        return hearings;
    }

    public void setHearings(List<Hearing> hearings) {
        this.hearings = hearings;
    }

    public Evidence getEvidence() {
        return evidence;
    }

    public void setEvidence(Evidence evidence) {
        this.evidence = evidence;
    }

    public List<DwpTimeExtension> getDwpTimeExtension() {
        return dwpTimeExtension;
    }

    public void setDwpTimeExtension(List<DwpTimeExtension> dwpTimeExtension) {
        this.dwpTimeExtension = dwpTimeExtension;
    }

    public List<Events> getEvents() {
        return events;
    }

    public void setEvents(List<Events> events) {
        this.events = events;
    }

    @Override
    public String toString() {
        return "CaseData{" +
                "caseReference='" + caseReference + '\'' +
                ", appeal=" + appeal +
                ", hearings=" + hearings +
                ", evidence=" + evidence +
                ", dwpTimeExtension=" + dwpTimeExtension +
                ", events=" + events +
                '}';
    }
}
