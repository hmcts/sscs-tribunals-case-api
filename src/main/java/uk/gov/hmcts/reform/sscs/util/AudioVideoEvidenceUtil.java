package uk.gov.hmcts.reform.sscs.util;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AudioVideoEvidence;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class AudioVideoEvidenceUtil {

    private AudioVideoEvidenceUtil() {
        //
    }

    public static DocumentType getDocumentType(String filename) {
        if (filename.toLowerCase().contains("mp3")) {
            return DocumentType.AUDIO_DOCUMENT;
        } else if (filename.toLowerCase().contains("mp4")) {
            return DocumentType.VIDEO_DOCUMENT;
        }
        return null;
    }

    public static String getDocumentTypeValue(String filename) {
        if (filename != null) {
            DocumentType type = getDocumentType(filename);
            if (type != null) {
                return type.getValue();
            }
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

    public static boolean isValidAudioVideoDocumentType(String documentType) {
        return APPELLANT_EVIDENCE.getValue().equals(documentType)
                || JOINT_PARTY_EVIDENCE.getValue().equals(documentType)
                || REPRESENTATIVE_EVIDENCE.getValue().equals(documentType)
                || DWP_EVIDENCE.getValue().equals(documentType)
                || HMCTS_EVIDENCE.getValue().equals(documentType);
    }

    public static String getOriginalSender(String documentType) {
        switch (documentType) {
            case "jointPartyEvidence":
                return "Joint party";
            case "representativeEvidence":
                return "Representative";
            case "dwpEvidence":
                return "DWP";
            case "hmctsEvidence":
                return "HMCTS";
            default: return "Appellant";
        }
    }

    public static List<String> addedEvidenceTypes(List<AudioVideoEvidence> previousEvidence, List<AudioVideoEvidence> evidence) {
        Map<String, Optional<String>> existingDocumentTypes = null;
        if (previousEvidence != null) {
            existingDocumentTypes = previousEvidence.stream().collect(
                    Collectors.toMap(e -> e.getId(), e -> Optional.ofNullable(e.getValue().getDocumentType())));
        }

        return addedDocumentTypes(existingDocumentTypes, evidence);
    }

    public static List<String> addedDocumentTypes(Map<String, Optional<String>> existingEvidenceTypes, List<AudioVideoEvidence> evidence) {
        if (evidence != null) {
            return evidence.stream()
                    .filter(e -> isNewDocumentOrTypeChanged(existingEvidenceTypes, e))
                    .map(e -> e.getValue().getDocumentType())
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private static boolean isNewDocumentOrTypeChanged(Map<String, Optional<String>> existingEvidenceTypes, AudioVideoEvidence evidence) {
        if (existingEvidenceTypes != null) {
            if (existingEvidenceTypes.containsKey(evidence.getId())) {
                return !StringUtils.equals(evidence.getValue().getDocumentType(),
                        existingEvidenceTypes.get(evidence.getId()).orElse(null));
            }
        }
        return true;
    }
}
