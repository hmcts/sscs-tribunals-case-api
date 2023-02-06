package uk.gov.hmcts.reform.sscs.service;

import java.io.IOException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LanguageServiceTest {

    private LanguageService service;

    @Before
    public void setup() throws IOException {
        service = new LanguageService();
    }

    @Test
    public void givenASignPalantypistKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("signPalantypist");
        Assert.assertNotNull(languageName);
        Assert.assertEquals("Sign Palantypist/Speech to TX", languageName);
    }

    @Test
    public void givenASignLanguageOthersKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("signLanguage (Others");
        Assert.assertNotNull(languageName);
        Assert.assertEquals("Sign Language (Others)", languageName);
    }

    @Test
    public void givenASignLanguageVisualFrameKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("signVisualFrame");
        Assert.assertNotNull(languageName);
        Assert.assertEquals("Sign (Visual Frame)", languageName);
    }

    @Test
    public void givenASignLanguageSseKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("signSse");
        Assert.assertNotNull(languageName);
        Assert.assertEquals("Sign (SSE)", languageName);
    }

    @Test
    public void givenASignLanguageRelayKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("signRelay");
        Assert.assertNotNull(languageName);
        Assert.assertEquals("Sign (Relay)", languageName);
    }

    @Test
    public void givenASignLanguageNoteTakerKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("signNoteTaker");
        Assert.assertNotNull(languageName);
        Assert.assertEquals("Sign (Note Taker)", languageName);
    }

    @Test
    public void givenASignLanguageManualAlphabetKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("signManualAlphabet");
        Assert.assertNotNull(languageName);
        Assert.assertEquals("Sign (Manual Alphabet)", languageName);
    }

    @Test
    public void givenASignLanguageMakatonKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("signMakaton");
        Assert.assertNotNull(languageName);
        Assert.assertEquals("Sign (Makaton)", languageName);
    }

    @Test
    public void givenASignLanguageLipspeakerKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("signLipSpeaker");
        Assert.assertNotNull(languageName);
        Assert.assertEquals("Sign (Lip Speaker)", languageName);
    }

    @Test
    public void givenASignLanguageInternationalSignKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("signInternationalSign");
        Assert.assertNotNull(languageName);
        Assert.assertEquals("Sign (International Sign)", languageName);
    }

    @Test
    public void givenASignLanguageDeafBlindKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("signDeafBlind");
        Assert.assertNotNull(languageName);
        Assert.assertEquals("Sign (Deaf Blind ‑ Hands on)", languageName);
    }

    @Test
    public void givenASignLanguageBslKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("signBsl");
        Assert.assertNotNull(languageName);
        Assert.assertEquals("Sign (BSL)", languageName);
    }

    @Test
    public void givenASignLanguageAmericanSignKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("signAmericanSign");
        Assert.assertNotNull(languageName);
        Assert.assertEquals("Sign (American Sign)", languageName);
    }

    @Test
    public void givenAValidNonSignLanguageKey_ThenExtractTheLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("senegalFrench");
        Assert.assertNotNull(languageName);
        Assert.assertEquals("Senegal (French) Olof Dialect", languageName);
    }

    @Test
    public void givenAnInvalidLanguageKey_ThenReturnNullLanguageName() {
        String languageName = service.getLanguageNameFromLanguageKey("something");
        Assert.assertNull(languageName);
    }
}
