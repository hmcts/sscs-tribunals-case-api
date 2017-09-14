package uk.gov.hmcts.sscs.tribunals.domain.corecase;

public class Name {

    private String title;

    private String last;

    private String middle;

    private String first;

    public Name() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLast() {
        return last;
    }

    public void setLast(String last) {
        this.last = last;
    }

    public String getMiddle() {
        return middle;
    }

    public void setMiddle(String middle) {
        this.middle = middle;
    }

    public String getFirst() {
        return first;
    }

    public void setFirst(String first) {
        this.first = first;
    }

    @Override
    public String toString() {
        return "Name{"
                +      "title='" + title + '\''
                +      ", last='" + last + '\''
                +      ", middle='" + middle + '\''
                +      ", first='" + first + '\''
                +      '}';
    }
}
