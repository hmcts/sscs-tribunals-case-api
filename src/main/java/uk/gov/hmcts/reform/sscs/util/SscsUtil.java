package uk.gov.hmcts.reform.sscs.util;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.PostponeRequestTemplateBody;

@Slf4j
public class SscsUtil {

    private static final String TITLE = "Postponement Request";
    public static final String FILENAME = "Postponement Request.pdf";

    private SscsUtil() {
        //
    }

    public static List<DynamicListItem> getPartiesOnCase(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = new ArrayList<>();

        listOptions.add(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));

        if (sscsCaseData.isThereAJointParty()) {
            listOptions.add(new DynamicListItem(JOINT_PARTY.getCode(), JOINT_PARTY.getLabel()));
        }

        if (sscsCaseData.getAppeal().getRep() != null
                && equalsIgnoreCase(sscsCaseData.getAppeal().getRep().getHasRepresentative(), "yes")) {
            listOptions.add(new DynamicListItem(REPRESENTATIVE.getCode(), REPRESENTATIVE.getLabel()));
        }

        return listOptions;
    }

    public static List<DynamicListItem> getPartiesOnCaseWithDwpAndHmcts(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = getPartiesOnCase(sscsCaseData);

        listOptions.add(new DynamicListItem(DWP.getCode(), DWP.getLabel()));

        listOptions.add(new DynamicListItem(HMCTS.getCode(), HMCTS.getLabel()));

        return listOptions;
    }

    public static <T> List<T> mutableEmptyListIfNull(List<T> list) {
        return Optional.ofNullable(list).orElse(new ArrayList<>());
    }

    public static PreSubmitCallbackResponse<SscsCaseData> processPostponementRequestPdfAndSetPreviewDocument(String userAuthorisation,
                                                                                                             SscsCaseData sscsCaseData,
                                                                                                             PreSubmitCallbackResponse<SscsCaseData> response,
                                                                                                             GenerateFile generateFile,
                                                                                                             String templateId) {

        final String requestDetails = sscsCaseData.getPostponementRequest().getPostponementRequestDetails();

        if (isBlank(requestDetails)) {
            response.addError("Please enter request details to generate a postponement request document");
            return response;
        }

        GenerateFileParams params = GenerateFileParams.builder()
                .renditionOutputLocation(null)
                .templateId(templateId)
                .formPayload(PostponeRequestTemplateBody.builder().title(TITLE).text(requestDetails).build())
                .userAuthentication(userAuthorisation)
                .build();
        final String generatedFileUrl = generateFile.assemble(params);
        sscsCaseData.getPostponementRequest().setPostponementPreviewDocument(DocumentLink.builder()
                .documentFilename(FILENAME)
                .documentBinaryUrl(generatedFileUrl + "/binary")
                .documentUrl(generatedFileUrl)
                .build());

        return response;
    }
}
