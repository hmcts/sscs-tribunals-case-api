package uk.gov.hmcts.sscs.service;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.sscs.domain.reminder.ReminderResponse;
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

    public void createEvent(ReminderResponse reminderResponse) throws CcdException {
        try {
            //Implement
            LOG.info("creteEvent needs implementation post MVP : " + reminderResponse.getEventId());
        } catch (Exception ex) {
            LOG.error("Error while creating event in ccd", ex);
            throw new CcdException("Error while creating event in ccd" + ex.getMessage());
        }
    }

    public CaseDetails createCase(CaseData caseData) throws CcdException {
        try {
            return createCoreCaseDataService.createCcdCase(caseData);
        } catch (Exception ex) {
            LOG.error("Error while creating case in ccd", ex);
            throw new CcdException("Error while creating case in ccd" + ex.getMessage());
        }
    }

    public CaseDetails updateCase(CaseData caseData, Long caseId, String eventId) throws CcdException {
        try {
            return updateCoreCaseDataService.updateCcdCase(caseData, caseId, eventId);
        } catch (Exception ex) {
            LOG.error("Error while updating case in ccd", ex);
            throw new CcdException("Error while updating case in ccd" + ex.getMessage());
        }
    }

    public CaseData findCcdCaseByAppealNumber(String appealNumber) throws CcdException {

        try {
            return readCoreCaseDataService.getCcdCaseDataByAppealNumber(appealNumber);
        } catch (Exception ex) {
            LOG.error("Error while getting case from ccd", ex);
            throw new CcdException("Error while getting case from ccd" + ex.getMessage());
        }
    }

    public String unsubscribe(String appealNumber) throws CcdException {

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
            LOG.error("Error while un subscribing details in ccd: ", ex);
            throw new CcdException("Error while updating case in ccd: " + ex.getMessage());
        }
        return benefitType != null ? benefitType.toLowerCase() : "";
    }

    public String updateSubscription(String appealNumber, SubscriptionRequest subscriptionRequest) throws CcdException {
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
            LOG.error("Error while updating subscription details in ccd: ", ex);
            throw new CcdException("Error while updating case in ccd: " + ex.getMessage());
        }
        return benefitType != null ? benefitType.toLowerCase() : "";
    }

    public CaseData findCcdCaseByAppealNumberAndSurname(String appealNumber, String surname) throws CcdException {
        CaseData caseData = findCcdCaseByAppealNumber(appealNumber);
        if (caseData == null) {
            LOG.info("Appeal not exists for appeal number: {}", appealNumber);
            throw new AppealNotFoundException(appealNumber);
        }
        return caseData.getAppeal() != null && caseData.getAppeal().getAppellant() != null
                && caseData.getAppeal().getAppellant().getName() != null
                && caseData.getAppeal().getAppellant().getName().getLastName() != null
                && caseData.getAppeal().getAppellant().getName().getLastName().equalsIgnoreCase(surname)
                ? caseData : null;
    }

    private CaseDetails findCcdCaseDetailsByAppealNumber(String appealNumber) throws CcdException {

        try {
            return readCoreCaseDataService.getCcdCaseDetailsByAppealNumber(appealNumber);
        } catch (Exception ex) {
            LOG.error("Error while getting case from ccd", ex);
            throw new CcdException("Error while getting case from ccd" + ex.getMessage());
        }
    }


}
