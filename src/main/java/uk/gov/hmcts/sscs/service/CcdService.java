package uk.gov.hmcts.sscs.service;

import static gcardone.junidecode.Junidecode.unidecode;
import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.sscs.exception.AppealNotFoundException;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.model.ccd.CcdUtil;
import uk.gov.hmcts.sscs.model.ccd.Subscription;
import uk.gov.hmcts.sscs.model.tya.SubscriptionRequest;
import uk.gov.hmcts.sscs.service.ccd.CreateCoreCaseDataService;
import uk.gov.hmcts.sscs.service.ccd.ReadCoreCaseDataService;
import uk.gov.hmcts.sscs.service.ccd.UpdateCoreCaseDataService;

@Service
public class CcdService {
    private static final Logger LOG = getLogger(CcdService.class);
    private static final String YES = "yes";
    private static final String NO = "no";
    private static final String EMPTY_STRING = "";
    private static final String SUBSCRIPTION_UPDATED = "subscriptionUpdated";

    private ReadCoreCaseDataService readCoreCaseDataService;
    private CreateCoreCaseDataService createCoreCaseDataService;
    private UpdateCoreCaseDataService updateCoreCaseDataService;

    @Autowired
    CcdService(ReadCoreCaseDataService readCoreCaseDataService,
               CreateCoreCaseDataService createCoreCaseDataService,
               UpdateCoreCaseDataService updateCoreCaseDataService) {
        this.readCoreCaseDataService = readCoreCaseDataService;
        this.createCoreCaseDataService = createCoreCaseDataService;
        this.updateCoreCaseDataService = updateCoreCaseDataService;
    }

    public CaseDetails createCase(CaseData caseData) {
        try {
            return createCoreCaseDataService.createCcdCase(caseData);
        } catch (Exception ex) {
            throw logCcdException("Error while creating case in ccd", ex);
        }
    }

    public CaseDetails updateCase(CaseData caseData, Long caseId, String eventId) {
        try {
            return updateCoreCaseDataService.updateCcdCase(caseData, caseId, eventId);
        } catch (Exception ex) {
            throw logCcdException("Error while updating case in ccd", ex);
        }
    }

    public CaseData findCcdCaseByAppealNumber(String appealNumber) {
        try {
            return readCoreCaseDataService.getCcdCaseDataByAppealNumber(appealNumber);
        } catch (Exception ex) {
            throw logCcdException("Error while getting case from ccd", ex);
        }
    }

    public String unsubscribe(String appealNumber) {

        String benefitType = null;
        try {
            CaseDetails caseDetails = findCcdCaseDetailsByAppealNumber(appealNumber);

            if (caseDetails != null) {
                CaseData caseData = CcdUtil.getCaseData(caseDetails.getData());

                Subscription appellantSubscription = caseData.getSubscriptions().getAppellantSubscription();
                appellantSubscription.setEmail(EMPTY_STRING);
                appellantSubscription.setSubscribeEmail(NO);

                caseData.getSubscriptions().setAppellantSubscription(appellantSubscription);

                Long caseId = caseDetails.getId();
                updateCase(caseData, caseId, SUBSCRIPTION_UPDATED);
                benefitType = caseData.getAppeal().getBenefitType().getCode();
            }
        } catch (Exception ex) {
            throw logCcdException("Error while unsubscribing details in ccd", ex);
        }
        return benefitType != null ? benefitType.toLowerCase() : "";
    }

    public String updateSubscription(String appealNumber, SubscriptionRequest subscriptionRequest) {
        String benefitType = null;
        try {
            CaseDetails caseDetails = findCcdCaseDetailsByAppealNumber(appealNumber);

            if (caseDetails != null) {
                CaseData caseData = CcdUtil.getCaseData(caseDetails.getData());

                Subscription appellantSubscription = caseData.getSubscriptions().getAppellantSubscription();

                if (null != subscriptionRequest.getEmail()) {
                    appellantSubscription.setEmail(subscriptionRequest.getEmail());
                    appellantSubscription.setSubscribeEmail(YES);
                }

                caseData.getSubscriptions().setAppellantSubscription(appellantSubscription);

                Long caseId = caseDetails.getId();
                updateCase(caseData, caseId, SUBSCRIPTION_UPDATED);
                benefitType = caseData.getAppeal().getBenefitType().getCode();
            }
        } catch (Exception ex) {
            throw logCcdException("Error while updating subscription details in ccd", ex);
        }
        return benefitType != null ? benefitType.toLowerCase() : "";
    }

    public CaseData findCcdCaseByAppealNumberAndSurname(String appealNumber, String surname) {
        CaseData caseData = findCcdCaseByAppealNumber(appealNumber);
        if (caseData == null) {
            LOG.info("Appeal does not exist for appeal number: {}", appealNumber);
            throw new AppealNotFoundException(appealNumber);
        }
        return caseData.getAppeal() != null && caseData.getAppeal().getAppellant() != null
                && caseData.getAppeal().getAppellant().getName() != null
                && caseData.getAppeal().getAppellant().getName().getLastName() != null
                && compareSurnames(surname, caseData)
                ? caseData : null;
    }

    private boolean compareSurnames(String surname, CaseData caseData) {
        String caseDataSurname = unidecode(caseData.getAppeal().getAppellant().getName().getLastName())
                .replaceAll("[^a-zA-Z]", "");
        String unidecodeSurname = unidecode(surname).replaceAll("[^a-zA-Z]", "");
        return caseDataSurname.equalsIgnoreCase(unidecodeSurname);
    }

    private CaseDetails findCcdCaseDetailsByAppealNumber(String appealNumber) {
        try {
            return readCoreCaseDataService.getCcdCaseDetailsByAppealNumber(appealNumber);
        } catch (Exception ex) {
            throw logCcdException("Error while getting case from ccd", ex);
        }
    }

    private CcdException logCcdException(String message, Exception ex) {
        CcdException ccdException = new CcdException(message, ex);
        LOG.error(message, ccdException);
        return ccdException;
    }
}
