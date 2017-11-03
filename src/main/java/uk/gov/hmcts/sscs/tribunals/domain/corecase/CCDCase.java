package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;

import java.util.Arrays;

@JsonRootName(value = "ccdcase")
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class CCDCase {

    private Appeal appeal;

    private Appellant appellant;

    private Appointee appointee;

    private Representative representative;

    private Hearing hearing;

    private Id id;

    private Benefit benefit;

    private DwpTimeExtension dwpTimeExtension;

    private Evidence[] evidence;

    public Appeal getAppeal() { return appeal; }

    public void setAppeal(Appeal appeal) { this.appeal = appeal; }

    public Appellant getAppellant() { return appellant; }

    public void setAppellant(Appellant appellant) { this.appellant = appellant; }

    public Appointee getAppointee() { return appointee; }

    public void setAppointee(Appointee appointee) { this.appointee = appointee; }

    public Representative getRepresentative() { return representative; }

    public void setRepresentative(Representative representative) { this.representative = representative; }

    public Hearing getHearing() { return hearing; }

    public void setHearing(Hearing hearing) { this.hearing = hearing; }

    public Id getId() { return id; }

    public void setId(Id id) { this.id = id; }

    public Benefit getBenefit() { return benefit; }

    public void setBenefit(Benefit benefit) { this.benefit = benefit; }

    public DwpTimeExtension getDwpTimeExtension() { return dwpTimeExtension; }

    public void setDwpTimeExtension(DwpTimeExtension dwpTimeExtension) { this.dwpTimeExtension = dwpTimeExtension; }

    public Evidence[] getEvidence() { return evidence; }

    public void setEvidence(Evidence[] evidence) { this.evidence = evidence; }

    @Override
    public String toString() {
        return "CCDCase{" + " appeal=" + appeal + ", appellant=" + appellant + ", appointee=" + appointee + ", representative=" + representative + ", hearing=" + hearing + ", id=" + id + ", benefit=" + benefit + ", dwpTimeExtension=" + dwpTimeExtension + ", evidence=" + Arrays.toString(evidence) + '}';
    }
}
