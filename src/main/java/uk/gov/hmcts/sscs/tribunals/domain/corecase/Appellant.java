package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType(propOrder = {"nino", "adminGroup"})
public class Appellant extends Person {

    private String nino;

    private String adminGroup;

    public Appellant() { }

    public Appellant(Name name, Address address, String phone, String email, String nino, String adminGroup) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.nino = nino;
        this.adminGroup = adminGroup;
    }

    public String getNino() { return nino; }

    public void setNino(String nino) { this.nino = nino; }

    public String getAdminGroup() { return adminGroup; }

    public void setAdminGroup(String adminGroup) { this.adminGroup = adminGroup; }

    @Override
    public String toString() {
        return "Appellant{"
                + " nino='" + nino + '\''
                + ", adminGroup='" + adminGroup + '\''
                + ", name=" + name
                + ", address=" + address
                + ", phone='" + phone + '\''
                + ", email='" + email + '\''
                + '}';
    }
}
