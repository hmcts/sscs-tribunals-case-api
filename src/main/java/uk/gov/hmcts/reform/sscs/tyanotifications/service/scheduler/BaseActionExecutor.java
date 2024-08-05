package uk.gov.hmcts.reform.sscs.tyanotifications.service.scheduler;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.getNotificationByEvent;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationUtils.buildSscsCaseDataWrapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobExecutor;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.exception.NotificationServiceException;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationService;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.RetryNotificationService;

public abstract class BaseActionExecutor<T> implements JobExecutor<T> {
    protected static final Logger LOG = getLogger(BaseActionExecutor.class);
    protected final NotificationService notificationService;
    protected final CcdService ccdService;
    protected final UpdateCcdCaseService updateCcdCaseService;
    protected final IdamService idamService;
    private final SscsCaseCallbackDeserializer deserializer;
    private final RetryNotificationService retryNotificationService;

    BaseActionExecutor(NotificationService notificationService, RetryNotificationService retryNotificationService, CcdService ccdService, UpdateCcdCaseService updateCcdCaseService, IdamService idamService, SscsCaseCallbackDeserializer deserializer) {
        this.notificationService = notificationService;
        this.retryNotificationService = retryNotificationService;
        this.ccdService = ccdService;
        this.updateCcdCaseService = updateCcdCaseService;
        this.idamService = idamService;
        this.deserializer = deserializer;
    }

    @Override
    public void execute(String jobId, String jobGroup, String eventId, T payload) {

        long caseId = getCaseId(payload);
        int retry = getRetry(payload);
        try {
            LOG.info("Scheduled event: {} triggered for case id: {}", eventId, caseId);

            IdamTokens idamTokens = idamService.getIdamTokens();

            SscsCaseDetails caseDetails = ccdService.getByCaseId(caseId, idamTokens);

            if (caseDetails != null) {

                //The deserialiser does things the ccd find method doesn't do. e.g. sorts collections,
                // notifications relies on events being sorted. If there are multiple hearings on the case
                // the notification should refer to the latest one.
                Callback<SscsCaseData> callback = deserializer.deserialize(buildCcdNode(caseDetails, eventId));

                NotificationSscsCaseDataWrapper wrapper = buildSscsCaseDataWrapper(
                    callback.getCaseDetails().getCaseData(),
                    null,
                    getNotificationByEvent(eventId),
                    callback.getCaseDetails().getState());

                NotificationWrapper notificationWrapper = getWrapper(wrapper, payload);

                try {
                    notificationService.manageNotificationAndSubscription(notificationWrapper, true);
                    if (wrapper.getNotificationEventType().isReminder()) {
                        updateCase(caseId, wrapper, idamTokens);
                    }
                } catch (NotificationServiceException e) {
                    LOG.info("Gov notify error, Notification event {} is rescheduled for case id {}", eventId, caseId);
                    retryNotificationService.rescheduleIfHandledGovNotifyErrorStatus(retry + 1, notificationWrapper, e);
                    throw e;
                }
            } else {
                LOG.warn("Case id: {} could not be found for event: {}", caseId, eventId);
            }
        } catch (Exception exc) {
            LOG.error("Failed to process job [" + jobId + "] for case [" + caseId + "] and event [" + eventId + "]", exc);
        }
    }

    private String buildCcdNode(SscsCaseDetails caseDetails, String jobName) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        JsonNode jsonNode = mapper.valueToTree(caseDetails);
        ObjectNode node2 = (ObjectNode) jsonNode;
        ObjectNode node = JsonNodeFactory.instance.objectNode();

        node2.set("case_data", jsonNode.get("data"));
        node2.remove("data");

        node.set("case_details", node2);
        node = node.put("event_id", jobName);

        return mapper.writeValueAsString(node);
    }

    protected abstract void updateCase(Long caseId, NotificationSscsCaseDataWrapper wrapper, IdamTokens idamTokens);

    protected abstract NotificationWrapper getWrapper(NotificationSscsCaseDataWrapper wrapper, T payload);

    protected abstract long getCaseId(T payload);

    protected abstract int getRetry(T payload);

}
