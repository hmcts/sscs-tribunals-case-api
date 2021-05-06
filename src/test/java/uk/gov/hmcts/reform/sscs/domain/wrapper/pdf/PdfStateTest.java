package uk.gov.hmcts.reform.sscs.domain.wrapper.pdf;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import org.junit.Test;

public class PdfStateTest {

    @Test
    public void has_correct_values() {

        assertEquals("ok", PdfState.OK.getPdfState());
        assertEquals("unreadable", PdfState.UNREADABLE.getPdfState());
        assertEquals("password_encrypted", PdfState.PASSWORD_ENCRYPTED.getPdfState());
        assertEquals("unknown", PdfState.UNKNOWN.getPdfState());

        assertEquals(PdfState.OK, PdfState.of("ok"));
        assertEquals(PdfState.UNREADABLE, PdfState.of("unreadable"));
        assertEquals(PdfState.PASSWORD_ENCRYPTED, PdfState.of("password_encrypted"));
        assertEquals(PdfState.UNKNOWN, PdfState.of("unknown"));

    }

    @Test
    public void should_throw_exception_when_name_unrecognised() {

        assertThatThrownBy(() -> PdfState.of("badState"))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("No PdfState mapped for [badState]")
            .hasNoCause();
    }

    @Test
    public void if_this_test_fails_it_is_because_it_needs_updating_with_your_changes() {
        assertEquals(4, PdfState.values().length);
    }

}
