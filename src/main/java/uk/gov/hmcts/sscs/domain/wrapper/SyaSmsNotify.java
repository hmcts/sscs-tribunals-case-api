package uk.gov.hmcts.reform.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SyaSmsNotify {

    @JsonProperty("wantsSMSNotifications")
    private Boolean wantsSmsNotifications;

    private String smsNumber;

    private Boolean useSameNumber;

    public SyaSmsNotify() {
        // For Json
    }

    public Boolean isWantsSmsNotifications() {
        return wantsSmsNotifications;
    }

    public void setWantsSmsNotifications(Boolean wantsSmsNotifications) {
        this.wantsSmsNotifications = wantsSmsNotifications;
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
                + " wantsSMSNotifications=" + wantsSmsNotifications
                + ", smsNumber='" + smsNumber + '\''
                + ", useSameNumber=" + useSameNumber
                + '}';
    }
}
