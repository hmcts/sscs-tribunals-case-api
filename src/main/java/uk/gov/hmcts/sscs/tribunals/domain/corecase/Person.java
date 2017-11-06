package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import javax.xml.bind.annotation.XmlElement;

abstract class Person {

    protected Name name;

    protected Address address;

    protected String phone;

    protected String email;

    @XmlElement
    public Name getName() { return name; }

    public void setName(Name name) { this.name = name; }

    @XmlElement
    public Address getAddress() { return address; }

    public void setAddress(Address address) { this.address = address; }

    @XmlElement
    public String getPhone() { return phone; }

    public void setPhone(String phone) { this.phone = phone; }

    @XmlElement
    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }
}
