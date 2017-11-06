package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.util.Arrays;

@XmlRootElement
@XmlType(propOrder = {"appeal", "appellant", "appointee", "representative", "hearing"})
public class CCDCase {

    private Appeal appeal;
    private Appellant appellant;
    private Appointee appointee;
    private Representative representative;
    private Hearing hearing;
    private Id id;
    private DwpTimeExtension dwpTimeExtension;
    private Evidence[] evidence;

    public CCDCase() {

    }

    public CCDCase(Appeal appeal, Appellant appellant, Appointee appointee, Representative representative, Hearing hearing) {
        this.appeal = appeal;
        this.appellant = appellant;
        this.appointee = appointee;
        this.representative = representative;
        this.hearing = hearing;
    }

    @XmlElement
    public Appeal getAppeal() { return appeal; }

    public void setAppeal(Appeal appeal) { this.appeal = appeal; }

    @XmlElement
    public Appellant getAppellant() { return appellant; }

    public void setAppellant(Appellant appellant) { this.appellant = appellant; }

    @XmlElement
    public Appointee getAppointee() { return appointee; }

    public void setAppointee(Appointee appointee) { this.appointee = appointee; }

    @XmlElement
    public Representative getRepresentative() { return representative; }

    public void setRepresentative(Representative representative) { this.representative = representative; }

    @XmlElement
    public Hearing getHearing() { return hearing; }

    public void setHearing(Hearing hearing) { this.hearing = hearing; }

    @XmlTransient
    public Id getId() { return id; }

    public void setId(Id id) { this.id = id; }

    @XmlTransient
    public DwpTimeExtension getDwpTimeExtension() { return dwpTimeExtension; }

    public void setDwpTimeExtension(DwpTimeExtension dwpTimeExtension) { this.dwpTimeExtension = dwpTimeExtension; }

    @XmlTransient
    public Evidence[] getEvidence() { return evidence; }

    public void setEvidence(Evidence[] evidence) { this.evidence = evidence; }

    @Override
    public String toString() {
        return "CCDCase{"
                + " appeal=" + appeal
                + ", appellant=" + appellant
                + ", appointee=" + appointee
                + ", representative=" + representative
                + ", hearing=" + hearing
                + ", id=" + id
                + ", dwpTimeExtension=" + dwpTimeExtension
                + ", evidence=" + Arrays.toString(evidence)
                + '}';
    }
}
