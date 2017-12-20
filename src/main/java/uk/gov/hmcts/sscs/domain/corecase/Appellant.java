package uk.gov.hmcts.sscs.domain.corecase;

import java.time.LocalDate;
import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType(propOrder = {"nino", "adminGroup"})
public class Appellant extends Person {

    private String nino;

    private String adminGroup;

    private LocalDate dateOfBirth;

    public Appellant() { }

    public Appellant(Name name, Address address, String phone, String email, String nino,
                     String adminGroup) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.nino = nino;
        this.adminGroup = adminGroup;
    }

    public String getNino() {
        return nino;
    }

    public void setNino(String nino) {
        this.nino = nino;
    }

    public String getAdminGroup() {
        return adminGroup;
    }

    public void setAdminGroup(String adminGroup) {
        this.adminGroup = adminGroup;
    }

    @XmlTransient
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    @Override
    public String toString() {
        return "Appellant{"
                + " nino='" + nino + '\''
                + ", adminGroup='" + adminGroup + '\''
                + ", dateOfBirth=" + dateOfBirth
                + ", name=" + name
                + ", address=" + address
                + ", phone='" + phone + '\''
                + ", email='" + email + '\''
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Appellant)) {
            return false;
        }
        Appellant appellant = (Appellant) o;
        return Objects.equals(nino, appellant.nino)
                && Objects.equals(adminGroup, appellant.adminGroup)
                && Objects.equals(dateOfBirth, appellant.dateOfBirth)
                && Objects.equals(name, appellant.name)
                && Objects.equals(address, appellant.address)
                && Objects.equals(phone, appellant.phone)
                && Objects.equals(email, appellant.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nino, adminGroup, dateOfBirth, name, address, phone, email);
    }
}
