package uk.gov.hmcts.sscs.domain.corecase;

import java.util.Objects;

public class Subscription {

    private Boolean isEmailSubscribe;
    private Boolean isMobileSubscribe;
    private String reason;

    public Subscription() {
    }

    public Subscription(Boolean isEmailSubscribe, Boolean isMobileSubscribe, String reason) {
        this.isEmailSubscribe = isEmailSubscribe;
        this.isMobileSubscribe = isMobileSubscribe;
        this.reason = reason;
    }

    public Boolean isEmailSubscribe() {
        return isEmailSubscribe;
    }

    public void setEmailSubscribe(Boolean isEmailSubscribe) {
        this.isEmailSubscribe = isEmailSubscribe;
    }

    public Boolean isMobileSubscribe() {
        return isMobileSubscribe;
    }

    public void setMobileSubscribe(Boolean isMobileSubscribe) {
        this.isMobileSubscribe = isMobileSubscribe;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "Subscription{"
                + " isEmailSubscribe=" + isEmailSubscribe
                + ", isMobileSubscribe=" + isMobileSubscribe
                + ", reason=" + reason
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Subscription subscription = (Subscription) o;
        return Objects.equals(isEmailSubscribe, subscription.isEmailSubscribe)
                && Objects.equals(isMobileSubscribe, subscription.isMobileSubscribe)
                && Objects.equals(reason, subscription.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isEmailSubscribe, isMobileSubscribe, reason);
    }
}