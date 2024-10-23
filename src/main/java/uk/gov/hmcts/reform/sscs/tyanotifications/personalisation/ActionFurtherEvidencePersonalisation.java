package uk.gov.hmcts.reform.sscs.tyanotifications.personalisation;

import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationMappingConstants.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.*;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingRequestType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.LetterUtils;

@Component
@Slf4j
public class ActionFurtherEvidencePersonalisation extends Personalisation<CcdNotificationWrapper> {
    @Override
    protected Map<String, Object> create(NotificationSscsCaseDataWrapper responseWrapper, SubscriptionWithType subscriptionWithType) {
        Map<String, Object> personalisation = super.create(responseWrapper, subscriptionWithType);

        NotificationEventType notificationEventType = responseWrapper.getNotificationEventType();
        SscsCaseData caseData = responseWrapper.getNewSscsCaseData();

        personalisation.put(DOCUMENT_TYPE_NAME, getPostHearingDocumentType(notificationEventType, responseWrapper.getNewSscsCaseData().getPostHearing().getRequestType()));
        personalisation.put(SENDER_NAME, LetterUtils.getNameForSender(caseData));
        personalisation.put(FURTHER_EVIDENCE_ACTION, caseData.getPostHearing().getRequestType());
        personalisation.put(LETTER_CONTENT_TYPE, LetterUtils.getNotificationTypeForActionFurtherEvidence(responseWrapper, subscriptionWithType));

        return personalisation;
    }

    private static String getPostHearingDocumentType(NotificationEventType eventType, PostHearingRequestType requestType) {
        if (CORRECTION_REQUEST.equals(eventType)) {
            return DocumentType.CORRECTION_APPLICATION.getLabel();
        } else if (LIBERTY_TO_APPLY_REQUEST.equals(eventType)) {
            return DocumentType.LIBERTY_TO_APPLY_APPLICATION.getLabel();
        } else if (STATEMENT_OF_REASONS_REQUEST.equals(eventType)) {
            return DocumentType.STATEMENT_OF_REASONS_APPLICATION.getLabel();
        } else if (PERMISSION_TO_APPEAL_REQUEST.equals(eventType)) {
            return DocumentType.PERMISSION_TO_APPEAL_APPLICATION.getLabel();
        }

        return DocumentType.SET_ASIDE_APPLICATION.getLabel();
    }
}
