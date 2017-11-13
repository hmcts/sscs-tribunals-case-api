package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import uk.gov.hmcts.sscs.service.json.CcdCaseDeserializer;

@XmlRootElement
@XmlType(propOrder = {"appeal", "appellant", "appointee", "representative", "hearing"})
@JsonDeserialize(using = CcdCaseDeserializer.class)
public class CcdCase {

    private Appeal appeal;
    private Appellant appellant;
    private Appointee appointee;
    private Representative representative;
    private Hearing hearing;

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

    @Override
    public String toString() {
        return "CcdCase{"
                + " appeal=" + appeal
                + ", appellant=" + appellant
                + ", appointee=" + appointee
                + ", representative=" + representative
                + ", hearing=" + hearing
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CcdCase)) {
            return false;
        }
        CcdCase ccdCase = (CcdCase) o;
        return Objects.equals(appeal, ccdCase.appeal)
                && Objects.equals(appellant, ccdCase.appellant)
                && Objects.equals(appointee, ccdCase.appointee)
                && Objects.equals(representative, ccdCase.representative)
                && Objects.equals(hearing, ccdCase.hearing);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appeal, appellant, appointee, representative, hearing);
    }
}
