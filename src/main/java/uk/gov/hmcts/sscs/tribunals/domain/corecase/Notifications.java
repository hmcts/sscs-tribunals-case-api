package uk.gov.hmcts.sscs.tribunals.domain.corecase;

public class Notifications {

    private String email;

    private String sms;

    private String mobile;

    public Notifications() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSms() {
        return sms;
    }

    public void setSms(String sms) {
        this.sms = sms;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    @Override
    public String toString() {
        return "Notifications{"
                +      "email='" + email + '\''
                +      ", sms='" + sms + '\''
                +      ", mobile='" + mobile + '\''
                +      '}';
    }
}
