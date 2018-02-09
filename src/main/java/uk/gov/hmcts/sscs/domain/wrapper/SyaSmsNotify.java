package uk.gov.hmcts.sscs.domain.wrapper;

public class SyaSmsNotify {

    private Boolean wantsSMSNotifications;

    private String smsNumber;

    private Boolean useSameNumber;

    public SyaSmsNotify() {
        // For Json
    }

    public Boolean isWantsSmsNotifications() {
        return wantsSMSNotifications;
    }

    public void setWantsSMSNotifications(Boolean wantsSMSNotifications) {
        this.wantsSMSNotifications = wantsSMSNotifications;
    }

    public String getSmsNumber() {
        return smsNumber;
    }

    public void setSmsNumber(String smsNumber) {
        this.smsNumber = smsNumber;
    }

    public Boolean isUseSameNumber() {
        return useSameNumber;
    }

    public void setUseSameNumber(Boolean useSameNumber) {
        this.useSameNumber = useSameNumber;
    }

    @Override
    public String toString() {
        return "SyaSmsNotify{"
                + " wantsSMSNotifications=" + wantsSMSNotifications
                + ", smsNumber='" + smsNumber + '\''
                + ", useSameNumber=" + useSameNumber
                + '}';
    }
}
