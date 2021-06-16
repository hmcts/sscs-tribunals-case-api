package uk.gov.hmcts.reform.sscs.util;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.util.AudioVideoEvidenceUtil.setHasUnprocessedAudioVideoEvidenceFlag;

import java.util.List;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.AudioVideoEvidence;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

public class AudioVideoEvidenceUtilTest {

    @Test
    public void shouldSetFlagToYes() {
        SscsCaseData caseData = SscsCaseData.builder().audioVideoEvidence(List.of(AudioVideoEvidence.builder().build())).build();
        setHasUnprocessedAudioVideoEvidenceFlag(caseData);
        assertEquals(YesNo.YES, caseData.getHasUnprocessedAudioVideoEvidence());
    }

    @Test
    public void shouldSetFlagToNoProvidedNullEvidenceList() {
        SscsCaseData caseData = SscsCaseData.builder().build();
        setHasUnprocessedAudioVideoEvidenceFlag(caseData);
        assertEquals(YesNo.NO, caseData.getHasUnprocessedAudioVideoEvidence());
    }

    @Test
    public void shouldSetFlagToNoProvidedEmptyEvidenceList() {
        SscsCaseData caseData = SscsCaseData.builder().audioVideoEvidence(List.of()).build();
        setHasUnprocessedAudioVideoEvidenceFlag(caseData);
        assertEquals(YesNo.NO, caseData.getHasUnprocessedAudioVideoEvidence());
    }
}
