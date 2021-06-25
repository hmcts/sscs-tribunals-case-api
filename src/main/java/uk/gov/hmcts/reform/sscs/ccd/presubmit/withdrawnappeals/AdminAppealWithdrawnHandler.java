package uk.gov.hmcts.reform.sscs.ccd.presubmit.withdrawnappeals;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.WITHDRAWAL_RECEIVED;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
public class AdminAppealWithdrawnHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType != null && callback != null && callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent().equals(EventType.ADMIN_APPEAL_WITHDRAWN);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setDwpState(WITHDRAWAL_RECEIVED.getId());

        SscsDocumentDetails documentDetails = caseData.getWithdrawalDocument();
        if (nonNull(documentDetails)) {
            addToSScsDocuments(caseData, documentDetails);
        }

        return new PreSubmitCallbackResponse<>(caseData);
    }

    private void addToSScsDocuments(SscsCaseData caseData, SscsDocumentDetails documentDetails) {
        if (isEmpty(documentDetails.getDocumentDateAdded())
                && (nonNull(documentDetails.getDocumentLink()) || nonNull(documentDetails.getEditedDocumentLink()))) {
            documentDetails.setDocumentDateAdded(LocalDate.now().toString());
        }
        List<SscsDocument> allDocuments = new ArrayList<>(ofNullable(caseData.getSscsDocument()).orElse(emptyList()));
        allDocuments.add(SscsDocument.builder().value(documentDetails).build());
        caseData.setSscsDocument(allDocuments);
        caseData.setWithdrawalDocument(null);
    }
}
