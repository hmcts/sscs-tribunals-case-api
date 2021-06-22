package uk.gov.hmcts.reform.sscs.util;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AudioVideoEvidence;
import uk.gov.hmcts.reform.sscs.ccd.domain.AudioVideoEvidenceDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Slf4j
public class AudioVideoEvidenceUtil {

    private AudioVideoEvidenceUtil() {
        //
    }

    public static DocumentType getDocumentType(AudioVideoEvidenceDetails evidence) {
        if (evidence.getDocumentLink().getDocumentFilename().toLowerCase().contains("mp3")) {
            return DocumentType.AUDIO_DOCUMENT;
        } else if (evidence.getDocumentLink().getDocumentFilename().toLowerCase().contains("mp4")) {
            return DocumentType.VIDEO_DOCUMENT;
        }
        return null;
    }

    public static boolean isSelectedEvidence(AudioVideoEvidence evidence, SscsCaseData caseData) {
        return evidence.getValue().getDocumentLink().getDocumentUrl()
            .equals(caseData.getSelectedAudioVideoEvidence().getValue().getCode());
    }

    public static void setHasUnprocessedAudioVideoEvidenceFlag(SscsCaseData caseData) {
        if (isNull(caseData.getAudioVideoEvidence()) || isEmpty(caseData.getAudioVideoEvidence())) {
            caseData.setHasUnprocessedAudioVideoEvidence(NO);
        } else {
            caseData.setHasUnprocessedAudioVideoEvidence(YES);
        }
        log.info("HasUnprocessedAudioVideoEvidence flag has been set to {} for case id: {}",
            caseData.getHasUnprocessedAudioVideoEvidence(), caseData.getCcdCaseId());
    }
}
