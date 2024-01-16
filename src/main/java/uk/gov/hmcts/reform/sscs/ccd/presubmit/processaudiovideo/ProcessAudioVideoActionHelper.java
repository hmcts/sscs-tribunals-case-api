package uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo;

import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoActionDynamicListItems.*;

import java.util.ArrayList;
import java.util.List;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public final class ProcessAudioVideoActionHelper {

    private ProcessAudioVideoActionHelper() {
        //not called
    }

    static void setProcessAudioVideoActionDropdown(SscsCaseData sscsCaseData, boolean hasJudgeRole, boolean hasTcwRole, boolean hasSuperUserRole) {
        List<DynamicListItem> listOptions = populateListItems(hasJudgeRole, hasTcwRole, hasSuperUserRole);
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(listOptions.get(0), listOptions));
    }

    static List<DynamicListItem> populateListItems(boolean hasJudgeRole, boolean hasTcwRole, boolean hasSuperUserRole) {
        List<DynamicListItem> listOptions = new ArrayList<>();

        populateActionListWithItems(listOptions, ISSUE_DIRECTIONS_NOTICE);

        if (hasJudgeRole || hasSuperUserRole || hasTcwRole) {
            populateActionListWithItems(listOptions, ADMIT_EVIDENCE);
            populateActionListWithItems(listOptions, EXCLUDE_EVIDENCE);
        }

        if (!hasJudgeRole || hasSuperUserRole) {
            populateActionListWithItems(listOptions, SEND_TO_JUDGE);
        }

        if (hasTcwRole || hasJudgeRole || hasSuperUserRole) {
            populateActionListWithItems(listOptions, SEND_TO_ADMIN);
        }

        return listOptions;
    }

    static void populateActionListWithItems(List<DynamicListItem> listOptions,
                                            ProcessAudioVideoActionDynamicListItems... items) {
        for (ProcessAudioVideoActionDynamicListItems item : items) {
            listOptions.add(new DynamicListItem(item.getCode(), item.getLabel()));
        }

    }

    static void setSelectedAudioVideoEvidence(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = populateEvidenceListWithItems(sscsCaseData);
        sscsCaseData.setSelectedAudioVideoEvidence(new DynamicList(listOptions.get(0), listOptions));
    }

    static List<DynamicListItem> populateEvidenceListWithItems(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = new ArrayList<>();

        sscsCaseData.getAudioVideoEvidence().forEach(audioVideoEvidence ->
                listOptions.add(new DynamicListItem(audioVideoEvidence.getValue().getDocumentLink().getDocumentUrl(),
                        audioVideoEvidence.getValue().getFileName())));

        return listOptions;
    }
}
