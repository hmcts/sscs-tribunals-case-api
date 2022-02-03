package uk.gov.hmcts.reform.sscs.domain.wrapper.pdf;

public class PdfAppealDetails {
    private final String title;
    private final String firstName;
    private final String surname;
    private final String nino;
    private final String caseReference;
    private final String dateCreated;
    private final boolean hideNino;
    private String welshDateCreated;

    public PdfAppealDetails(String title, String firstName, String surname, String nino, String caseReference,
                            String dateCreated, boolean hideNino, String welshDateCreated) {
        this(title, firstName, surname, nino, caseReference, dateCreated, hideNino);
        this.welshDateCreated = welshDateCreated;
    }

    public PdfAppealDetails(String title, String firstName, String surname, String nino, String caseReference, String dateCreated, boolean hideNino) {
        this.title = title;
        this.firstName = firstName;
        this.surname = surname;
        this.nino = nino;
        this.caseReference = caseReference;
        this.dateCreated = dateCreated;
        this.hideNino = hideNino;
    }

    public String getTitle() {
        return title;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getSurname() {
        return surname;
    }

    public String getNino() {
        return nino;
    }

    public String getCaseReference() {
        return caseReference;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public String getWelshDateCreated() {
        return welshDateCreated;
    }

    public boolean getHideNino() {
        return hideNino;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PdfAppealDetails that = (PdfAppealDetails) o;

        if (title != null ? !title.equals(that.title) : that.title != null) {
            return false;
        }
        if (firstName != null ? !firstName.equals(that.firstName) : that.firstName != null) {
            return false;
        }
        if (surname != null ? !surname.equals(that.surname) : that.surname != null) {
            return false;
        }
        if (nino != null ? !nino.equals(that.nino) : that.nino != null) {
            return false;
        }
        if (caseReference != null ? !caseReference.equals(that.caseReference) : that.caseReference != null) {
            return false;
        }
        return dateCreated != null ? dateCreated.equals(that.dateCreated) : that.dateCreated == null;
    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
        result = 31 * result + (surname != null ? surname.hashCode() : 0);
        result = 31 * result + (nino != null ? nino.hashCode() : 0);
        result = 31 * result + (caseReference != null ? caseReference.hashCode() : 0);
        result = 31 * result + (dateCreated != null ? dateCreated.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PdfAppealDetails{"
                + "title='" + title + '\''
                + ", firstName='" + firstName + '\''
                + ", surname='" + surname + '\''
                + ", nino='" + nino + '\''
                + ", caseReference='" + caseReference + '\''
                + ", dateCreated='" + dateCreated + '\''
                + '}';
    }
}
