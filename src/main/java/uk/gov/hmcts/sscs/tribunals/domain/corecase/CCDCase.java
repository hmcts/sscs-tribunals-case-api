package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType(propOrder = {"appeal", "appellant", "appointee", "representative", "hearing"})
public class CCDCase {

    private Appeal appeal;
    private Appellant appellant;
    private Appointee appointee;
    private Representative representative;
    private Hearing hearing;

    public CCDCase() {

    }

    public CCDCase(Appeal appeal, Appellant appellant, Appointee appointee, Representative representative, Hearing hearing) {
        this.appeal = appeal;
        this.appellant = appellant;
        this.appointee = appointee;
        this.representative = representative;
        this.hearing = hearing;
    }

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

    @Override
    public String toString() {
        return "CCDCase{"
                + " appeal=" + appeal
                + ", appellant=" + appellant
                + ", appointee=" + appointee
                + ", representative=" + representative
                + ", hearing=" + hearing
                + '}';
    }
}
