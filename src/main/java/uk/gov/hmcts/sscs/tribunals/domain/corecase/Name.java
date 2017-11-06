package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType(propOrder = {"title", "initial", "surname"})
public class Name {

    private String title;

    private String first;

    private String surname;

    private String initial;

    public Name() {}

    public Name(String title, String first, String surname) {
        this.title = title;
        this.first = first;
        this.surname = surname;
        this.initial = first.substring(0,1);
    }

    @XmlElement
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @XmlTransient
    public String getFirst() {
        return first;
    }

    public void setFirst(String first) {
        this.first = first;
    }

    @XmlElement
    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    @XmlElement
    public String getInitial() { return initial; }

    @Override
    public String toString() {
        return "Name{"
                + " title='" + title + '\''
                + ", first='" + first + '\''
                + ", surname='" + surname + '\''
                + ", initial='" + initial + '\''
                + '}';
    }
}
