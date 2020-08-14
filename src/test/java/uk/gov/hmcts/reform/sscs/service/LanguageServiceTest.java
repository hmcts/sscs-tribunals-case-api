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
    public void givenASignPalantypistKey_ThenExtractTheInterpreterDescription() {
        String intepreterDescription = service.getInterpreterDescriptionForLanguageKey("signPalantypist");
        Assert.assertNotNull(intepreterDescription);
        Assert.assertEquals("a sign language interpreter (Sign Palantypist/Speech to TX)", intepreterDescription);
    }

    @Test
    public void givenASignLanguageOthersKey_ThenExtractTheInterpreterDescription() {
        String intepreterDescription = service.getInterpreterDescriptionForLanguageKey("signLanguage (Others");
        Assert.assertNotNull(intepreterDescription);
        Assert.assertEquals("a sign language interpreter (Others)", intepreterDescription);
    }

    @Test
    public void givenASignLanguageVisualFrameKey_ThenExtractTheInterpreterDescription() {
        String intepreterDescription = service.getInterpreterDescriptionForLanguageKey("signVisualFrame");
        Assert.assertNotNull(intepreterDescription);
        Assert.assertEquals("a sign language interpreter (Visual Frame)", intepreterDescription);
    }

    @Test
    public void givenASignLanguageSseKey_ThenExtractTheInterpreterDescription() {
        String intepreterDescription = service.getInterpreterDescriptionForLanguageKey("signSse");
        Assert.assertNotNull(intepreterDescription);
        Assert.assertEquals("a sign language interpreter (SSE)", intepreterDescription);
    }

    @Test
    public void givenASignLanguageRelayKey_ThenExtractTheInterpreterDescription() {
        String intepreterDescription = service.getInterpreterDescriptionForLanguageKey("signRelay");
        Assert.assertNotNull(intepreterDescription);
        Assert.assertEquals("a sign language interpreter (Relay)", intepreterDescription);
    }

    @Test
    public void givenASignLanguageNoteTakerKey_ThenExtractTheInterpreterDescription() {
        String intepreterDescription = service.getInterpreterDescriptionForLanguageKey("signNoteTaker");
        Assert.assertNotNull(intepreterDescription);
        Assert.assertEquals("a sign language interpreter (Note Taker)", intepreterDescription);
    }

    @Test
    public void givenASignLanguageManualAlphabetKey_ThenExtractTheInterpreterDescription() {
        String intepreterDescription = service.getInterpreterDescriptionForLanguageKey("signManualAlphabet");
        Assert.assertNotNull(intepreterDescription);
        Assert.assertEquals("a sign language interpreter (Manual Alphabet)", intepreterDescription);
    }

    @Test
    public void givenASignLanguageMakatonKey_ThenExtractTheInterpreterDescription() {
        String intepreterDescription = service.getInterpreterDescriptionForLanguageKey("signMakaton");
        Assert.assertNotNull(intepreterDescription);
        Assert.assertEquals("a sign language interpreter (Makaton)", intepreterDescription);
    }

    @Test
    public void givenASignLanguageLipspeakerKey_ThenExtractTheInterpreterDescription() {
        String intepreterDescription = service.getInterpreterDescriptionForLanguageKey("signLipSpeaker");
        Assert.assertNotNull(intepreterDescription);
        Assert.assertEquals("a sign language interpreter (Lip Speaker)", intepreterDescription);
    }

    @Test
    public void givenASignLanguageInternationalSignKey_ThenExtractTheInterpreterDescription() {
        String intepreterDescription = service.getInterpreterDescriptionForLanguageKey("signInternationalSign");
        Assert.assertNotNull(intepreterDescription);
        Assert.assertEquals("a sign language interpreter (International Sign)", intepreterDescription);
    }

    @Test
    public void givenASignLanguageDeafBlindKey_ThenExtractTheInterpreterDescription() {
        String intepreterDescription = service.getInterpreterDescriptionForLanguageKey("signDeafBlind");
        Assert.assertNotNull(intepreterDescription);
        Assert.assertEquals("a sign language interpreter (Deaf Blind ‑ Hands on)", intepreterDescription);
    }

    @Test
    public void givenASignLanguageBslKey_ThenExtractTheInterpreterDescription() {
        String intepreterDescription = service.getInterpreterDescriptionForLanguageKey("signBsl");
        Assert.assertNotNull(intepreterDescription);
        Assert.assertEquals("a sign language interpreter (BSL)", intepreterDescription);
    }

    @Test
    public void givenASignLanguageAmericanSignKey_ThenExtractTheInterpreterDescription() {
        String intepreterDescription = service.getInterpreterDescriptionForLanguageKey("signAmericanSign");
        Assert.assertNotNull(intepreterDescription);
        Assert.assertEquals("a sign language interpreter (American Sign)", intepreterDescription);
    }

    @Test
    public void givenAValidNonSignLanguageKey_ThenExtractTheInterpreterDescription() {
        String intepreterDescription = service.getInterpreterDescriptionForLanguageKey("senegalFrench");
        Assert.assertNotNull(intepreterDescription);
        Assert.assertEquals("an interpreter in Senegal (French) Olof Dialect", intepreterDescription);
    }

    @Test
    public void givenAnInvalidLanguageKey_ThenReturnNullInterpreterDescription() {
        String languageName = service.getInterpreterDescriptionForLanguageKey("something");
        Assert.assertNull(languageName);
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