package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import java.util.Arrays;

public class Appellant {

    private Identity identity;

    private Supporter supporter;

    private Notifications notifications;

    private AppealReasons[] appealReasons;

    private Address address;

    private HearingOptions hearingOptions;

    private Name name;

    private String gpConsent;

    private HearingType hearingType;

    private String isAppointee;

    private Contact contact;

    private Representative representative;

    public Appellant() {
    }

    public Identity getIdentity() {
        return identity;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    public Supporter getSupporter() {
        return supporter;
    }

    public void setSupporter(Supporter supporter) {
        this.supporter = supporter;
    }

    public Notifications getNotifications() {
        return notifications;
    }

    public void setNotifications(Notifications notifications) {
        this.notifications = notifications;
    }

    public AppealReasons[] getAppealReasons() {
        return appealReasons;
    }

    public void setAppealReasons(AppealReasons[] appealReasons) {
        this.appealReasons = appealReasons;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public HearingOptions getHearingOptions() {
        return hearingOptions;
    }

    public void setHearingOptions(HearingOptions hearingOptions) {
        this.hearingOptions = hearingOptions;
    }

    public Name getName() {
        return name;
    }

    public void setName(Name name) {
        this.name = name;
    }

    public String getGpConsent() {
        return gpConsent;
    }

    public void setGpConsent(String gpConsent) {
        this.gpConsent = gpConsent;
    }

    public HearingType getHearingType() {
        return hearingType;
    }

    public void setHearingType(HearingType hearingType) {
        this.hearingType = hearingType;
    }

    public String getIsAppointee() {
        return isAppointee;
    }

    public void setIsAppointee(String isAppointee) {
        this.isAppointee = isAppointee;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public Representative getRepresentative() {
        return representative;
    }

    public void setRepresentative(Representative representative) {
        this.representative = representative;
    }

    @Override
    public String toString() {
        return "Appellant{"
                +  "identity=" + identity
                +  ", supporter=" + supporter
                +  ", notifications=" + notifications
                +  ", appealReasons=" + Arrays.toString(appealReasons)
                +  ", address=" + address
                +  ", hearingOptions=" + hearingOptions
                +  ", name=" + name
                +  ", gpConsent='" + gpConsent + '\''
                +  ", hearingType='" + hearingType + '\''
                +  ", isAppointee='" + isAppointee + '\''
                +  ", contact=" + contact
                +  ", representative=" + representative
                + '}';
    }
}
