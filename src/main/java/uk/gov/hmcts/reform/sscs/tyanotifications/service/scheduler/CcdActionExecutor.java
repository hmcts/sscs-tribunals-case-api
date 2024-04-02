package uk.gov.hmcts.reform.sscs.tyanotifications.service.scheduler;

import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static org.apache.commons.lang3.RegExUtils.replaceAll;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationService;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.RetryNotificationService;


public class CcdActionExecutor extends BaseActionExecutor<String> {

    public CcdActionExecutor(NotificationService notificationService,
                             RetryNotificationService retryNotificationService,
                             CcdService ccdService,
                             UpdateCcdCaseService updateCcdCaseService,
                             IdamService idamService,
                             SscsCaseCallbackDeserializer deserializer) {
        super(notificationService, retryNotificationService, ccdService, updateCcdCaseService, idamService, deserializer);
    }

    @Override
    protected void updateCase(Long caseId, NotificationSscsCaseDataWrapper wrapper, IdamTokens idamTokens) {
        updateCcdCaseService.updateCaseV2(caseId, wrapper.getNotificationEventType().getId(), "CCD Case", "Notification Service updated case", idamTokens, sscsCaseData -> {
        });
    }

    @Override
    protected NotificationWrapper getWrapper(NotificationSscsCaseDataWrapper wrapper, String payload) {
        return new CcdNotificationWrapper(wrapper);
    }

    @Override
    protected long getCaseId(String payload) {
        return parseLong(replaceAll(payload, ",.*", EMPTY));
    }

    @Override
    protected int getRetry(String payload) {
        String[] strings = payload.split(",");
        return strings.length > 1 ? parseInt(strings[1]) : 0;
    }
}
