package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import java.util.Objects;
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
        generateInitial();
    }

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
        generateInitial();
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    @XmlElement
    public String getInitial() {
        return initial;
    }

    public void generateInitial() {
        if (first != null) {
            initial = first.substring(0, 1);
        }
    }

    @XmlTransient
    public String getFullName() {
        return title + " " + initial + " " + surname;
    }

    @Override
    public String toString() {
        return "Name{"
                + " title='" + title + '\''
                + ", first='" + first + '\''
                + ", surname='" + surname + '\''
                + ", initial='" + initial + '\''
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Name)) {
            return false;
        }
        Name name = (Name) o;
        return Objects.equals(title, name.title)
                && Objects.equals(first, name.first)
                && Objects.equals(surname, name.surname)
                && Objects.equals(initial, name.initial);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, first, surname, initial);
    }
}
