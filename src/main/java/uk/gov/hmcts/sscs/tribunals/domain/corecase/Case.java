package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;

import java.util.Arrays;

@JsonRootName(value = "case")
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class Case {

    private Hearing hearing;

    private String region;

    private Id id;

    private String benefitType;

    private DwpTimeExtension dwpTimeExtension;

    private Evidence[] evidence;

    private Mrn mrn;

    private Appellant appellant;

    public Case() {
    }

    public Hearing getHearing() {
        return hearing;
    }

    public void setHearing(Hearing hearing) {
        this.hearing = hearing;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Id getId() {
        return id;
    }

    public void setId(Id id) {
        this.id = id;
    }

    public String getBenefitType() {
        return benefitType;
    }

    public void setBenefitType(String benefitType) {
        this.benefitType = benefitType;
    }

    public DwpTimeExtension getDwpTimeExtension() {
        return dwpTimeExtension;
    }

    public void setDwpTimeExtension(DwpTimeExtension dwpTimeExtension) {
        this.dwpTimeExtension = dwpTimeExtension;
    }

    public Evidence[] getEvidence() {
        return evidence;
    }

    public void setEvidence(Evidence[] evidence) {
        this.evidence = evidence;
    }

    public Mrn getMrn() {
        return mrn;
    }

    public void setMrn(Mrn mrn) {
        this.mrn = mrn;
    }

    public Appellant getAppellant() {
        return appellant;
    }

    public void setAppellant(Appellant appellant) {
        this.appellant = appellant;
    }

    @Override
    public String toString() {
        return "Case{"
                +       "hearing=" + hearing
                +       ", region='" + region + '\''
                +       ", id=" + id
                +       ", benefitType='" + benefitType + '\''
                +       ", dwpTimeExtension=" + dwpTimeExtension
                +       ", evidence=" + Arrays.toString(evidence)
                +       ", mrn=" + mrn
                +       ", appellant=" + appellant
                +       '}';
    }
}
