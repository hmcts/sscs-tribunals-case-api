package uk.gov.hmcts.reform.sscs.ccd.presubmit.welsh;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocuments;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Service
public class UploadWelshDocumentsSubmittedCallbackHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final CcdService ccdService;
    private final IdamService idamService;

    @Autowired
    public UploadWelshDocumentsSubmittedCallbackHandler(CcdService ccdService, IdamService idamService) {
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");
        DynamicList welshDocuments = callback.getCaseDetails().getCaseData().getOriginalDocuments();
        return callbackType.equals(CallbackType.SUBMITTED)
                && callback.getEvent().equals(EventType.UPLOAD_WELSH_DOCUMENT);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        SscsCaseDetails sscsCaseDetails = updateCase(callback, caseData);

        return new PreSubmitCallbackResponse<>(sscsCaseDetails.getData());
    }

    private SscsCaseDetails updateCase(Callback<SscsCaseData> callback, SscsCaseData caseData) {

        List<SscsDocument> documents = new ArrayList<>();
        List<SscsWelshDocuments> sscsWelshDocumentsList = new ArrayList<>();
        List<SscsDocument> sscsDocuments =  Optional.ofNullable(caseData).map(SscsCaseData::getSscsDocument)
                .orElse(Collections.emptyList())
                .stream()
                .filter(a -> a.getValue().getDocumentTranslationStatus().equals(SscsDocumentTranslationStatus.TRANSLATION_REQUESTED))
                .collect(Collectors.toList());

        List<SscsWelshDocuments> sscsWelshDocuments =
                new ArrayList<>(Optional.ofNullable(caseData).map(SscsCaseData::getSscsWelshDocuments)
                .orElse(Collections.emptyList()));


        if(sscsDocuments.size() > 0) {
            for (SscsDocument sscsDocument : sscsDocuments) {
                SscsDocument _sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder()
                        .documentType(sscsDocument.getValue().getDocumentType())
                        .documentFileName(sscsDocument.getValue().getDocumentLink().getDocumentFilename())
                        .documentLink(sscsDocument.getValue().getDocumentLink())
                        .documentTranslationStatus(SscsDocumentTranslationStatus.TRANSLATION_COMPLETE)
                        .build()).build();

                documents.add(_sscsDocument);
            }
            caseData.setSscsDocument(documents);
        }

        if(sscsWelshDocuments.size() > 0) {
            for (SscsWelshDocuments sscsWelshDocument : sscsWelshDocuments) {
                SscsWelshDocuments _sscsWelshDocument =
                        sscsWelshDocument.builder().value(SscsWelshDocumentDetails.builder()
                        .documentType(sscsWelshDocument.getValue().getDocumentType())
                        .documentFileName(sscsWelshDocument.getValue().getDocumentLink().getDocumentFilename())
                        .documentLink(sscsWelshDocument.getValue().getDocumentLink())
                        .originalDocumentFileName(caseData.getOriginalDocuments().getValue().getCode())
                        .documentComment(sscsWelshDocument.getValue().getDocumentComment())
                        .documentLanguage(sscsWelshDocument.getValue().getDocumentLanguage())
                        .build()).build();

                sscsWelshDocumentsList.add(_sscsWelshDocument);
            }
            caseData.setSscsWelshDocuments(sscsWelshDocumentsList);
        }

        return ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                EventType.UPLOAD_WELSH_DOCUMENT.getCcdType(), "Update document translation status",
                "Update document translation status", idamService.getIdamTokens());
    }
}
