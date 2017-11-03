package uk.gov.hmcts.sscs.tribunals.domain.corecase;

public class Appellant extends Person {

    private String nino;

    private Notifications notifications;

    public String getNino() { return nino; }

    public void setNino(String nino) { this.nino = nino; }

    public Notifications getNotifications() { return notifications; }

    public void setNotifications(Notifications notifications) { this.notifications = notifications; }

    @Override
    public String toString() {
        return "Appellant{" + " notifications=" + notifications + ", name=" + name + ", address=" + address + ", phone='" + phone + '\'' + '}';
    }
}
