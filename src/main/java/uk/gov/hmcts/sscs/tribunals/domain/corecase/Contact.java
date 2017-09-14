package uk.gov.hmcts.sscs.tribunals.domain.corecase;

public class Contact {

    private String phone;

    private String email;

    private String mobile;

    public Contact() {
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    @Override
    public String toString() {
        return "Contact{"
                +    "phone='" + phone + '\''
                +   ", email='" + email + '\''
                +   ", mobile='" + mobile + '\''
                +   '}';
    }
}
