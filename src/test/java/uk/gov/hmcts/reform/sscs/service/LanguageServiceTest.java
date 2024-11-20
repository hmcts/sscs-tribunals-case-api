package uk.gov.hmcts.reform.sscs.service;

import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LanguageServiceTest {

    private LanguageService service;

    @BeforeEach
    public void setup() throws IOException {
        service = new LanguageService();
    }

    @Test
    public void givenASignPalantypistKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("signPalantypist");
        Assertions.assertNotNull(languageName);
        Assertions.assertEquals("Sign Palantypist/Speech to TX", languageName);
    }

    @Test
    public void givenASignLanguageOthersKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("signLanguage (Others");
        Assertions.assertNotNull(languageName);
        Assertions.assertEquals("Sign Language (Others)", languageName);
    }

    @Test
    public void givenASignLanguageVisualFrameKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("signVisualFrame");
        Assertions.assertNotNull(languageName);
        Assertions.assertEquals("Sign (Visual Frame)", languageName);
    }

    @Test
    public void givenASignLanguageSseKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("signSse");
        Assertions.assertNotNull(languageName);
        Assertions.assertEquals("Sign (SSE)", languageName);
    }

    @Test
    public void givenASignLanguageRelayKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("signRelay");
        Assertions.assertNotNull(languageName);
        Assertions.assertEquals("Sign (Relay)", languageName);
    }

    @Test
    public void givenASignLanguageNoteTakerKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("signNoteTaker");
        Assertions.assertNotNull(languageName);
        Assertions.assertEquals("Sign (Note Taker)", languageName);
    }

    @Test
    public void givenASignLanguageManualAlphabetKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("signManualAlphabet");
        Assertions.assertNotNull(languageName);
        Assertions.assertEquals("Sign (Manual Alphabet)", languageName);
    }

    @Test
    public void givenASignLanguageMakatonKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("signMakaton");
        Assertions.assertNotNull(languageName);
        Assertions.assertEquals("Sign (Makaton)", languageName);
    }

    @Test
    public void givenASignLanguageLipspeakerKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("signLipSpeaker");
        Assertions.assertNotNull(languageName);
        Assertions.assertEquals("Sign (Lip Speaker)", languageName);
    }

    @Test
    public void givenASignLanguageInternationalSignKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("signInternationalSign");
        Assertions.assertNotNull(languageName);
        Assertions.assertEquals("Sign (International Sign)", languageName);
    }

    @Test
    public void givenASignLanguageDeafBlindKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("signDeafBlind");
        Assertions.assertNotNull(languageName);
        Assertions.assertEquals("Sign (Deaf Blind ‑ Hands on)", languageName);
    }

    @Test
    public void givenASignLanguageBslKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("signBsl");
        Assertions.assertNotNull(languageName);
        Assertions.assertEquals("Sign (BSL)", languageName);
    }

    @Test
    public void givenASignLanguageAmericanSignKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("signAmericanSign");
        Assertions.assertNotNull(languageName);
        Assertions.assertEquals("Sign (American Sign)", languageName);
    }

    @Test
    public void givenAValidNonSignLanguageKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("senegalFrench");
        Assertions.assertNotNull(languageName);
        Assertions.assertEquals("Senegal (French) Olof Dialect", languageName);
    }

    @Test
    public void givenAnInvalidLanguageKey_ThenReturnNullLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("something");
        Assertions.assertNull(languageName);
    }
}
