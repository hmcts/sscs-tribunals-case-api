package uk.gov.hmcts.sscs.tribunals.domain.corecase;

public class Id {

    private String ucn;

    private String tya;

    private String gaps2;

    public Id() {
    }

    public String getUcn() {
        return ucn;
    }

    public void setUcn(String ucn) {
        this.ucn = ucn;
    }

    public String getTya() {
        return tya;
    }

    public void setTya(String tya) {
        this.tya = tya;
    }

    public String getGaps2() {
        return gaps2;
    }

    public void setGaps2(String gaps2) {
        this.gaps2 = gaps2;
    }

    @Override
    public String toString() {
        return "Id{"
                +    "ucn='" + ucn + '\''
                +    ", tya='" + tya + '\''
                +    ", gaps2='" + gaps2 + '\''
                +    '}';
    }
}
