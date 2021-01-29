package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadfurtherevidence;

import static java.util.Collections.emptyList;
import static java.util.Collections.sort;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
public class UploadFurtherEvidenceAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {


    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent().equals(EventType.UPLOAD_FURTHER_EVIDENCE);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        log.info("About to submit Upload Further Evidence caseID:  {}", sscsCaseData.getCcdCaseId());
        final PreSubmitCallbackResponse<SscsCaseData>  preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        if (isNotEmpty(sscsCaseData.getDraftFurtherEvidenceDocuments())) {
            sscsCaseData.getDraftFurtherEvidenceDocuments().forEach(doc -> {
                if (isBlank(doc.getValue().getDocumentType())) {
                    preSubmitCallbackResponse.addError("Please select a document type");
                }
                if (isBlank(doc.getValue().getDocumentFileName())) {
                    preSubmitCallbackResponse.addError("Please add a file name");
                }
                if (isNull(doc.getValue().getDocumentLink())) {
                    preSubmitCallbackResponse.addError("Please upload a file");
                }
            });
            if (isEmpty(preSubmitCallbackResponse.getErrors())) {
                List<SscsDocument> newDocuments = sscsCaseData.getDraftFurtherEvidenceDocuments().stream().map(doc ->
                        SscsDocument.builder().value(SscsDocumentDetails.builder()
                        .documentLink(doc.getValue().getDocumentLink())
                        .documentFileName(doc.getValue().getDocumentFileName())
                        .documentType(doc.getValue().getDocumentType())
                        .documentDateAdded(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
                        .build()).build()).collect(toList());
                List<SscsDocument> allDocuments = new ArrayList<>(ofNullable(sscsCaseData.getSscsDocument()).orElse(emptyList()));
                allDocuments.addAll(newDocuments);
                sort(newDocuments);
                sscsCaseData.setSscsDocument(allDocuments);
            }
        }
        if (isEmpty(preSubmitCallbackResponse.getErrors())) {
            sscsCaseData.setDraftFurtherEvidenceDocuments(null);
        }
        return preSubmitCallbackResponse;
    }

}
