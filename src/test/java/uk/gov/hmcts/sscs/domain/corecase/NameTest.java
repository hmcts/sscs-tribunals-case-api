package uk.gov.hmcts.sscs.domain.corecase;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class NameTest {

    private Name name;

    @Before
    public void setup() {
        name = new Name("Mr", "Barry", "Smithy");
    }

    @Test
    public void generateInitialAfterFirstNameChanged() {
        name.setFirst("George");
        assertEquals("G", name.getInitial());
    }

    @Test
    public void getFullName() {
        assertEquals("Mr B Smithy", name.getFullName());
    }
}