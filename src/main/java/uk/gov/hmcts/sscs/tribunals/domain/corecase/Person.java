package uk.gov.hmcts.sscs.tribunals.domain.corecase;

abstract class Person {

    protected Name name;

    protected Address address;

    protected String phone;

    public Name getName() { return name; }

    public void setName(Name name) { this.name = name; }

    public Address getAddress() { return address; }

    public void setAddress(Address address) { this.address = address; }

    public String getPhone() { return phone; }

    public void setPhone(String phone) { this.phone = phone; }
}
