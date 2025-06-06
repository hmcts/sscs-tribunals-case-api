package uk.gov.hmcts.reform.sscs.tyanotifications.tya;

import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.getNotificationByCcdEvent;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationUtils.buildSscsCaseDataWrapper;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationService;

@RestController
@Slf4j
public class ReminderTestController {

    private final NotificationService notificationService;
    private final AuthorisationService authorisationService;
    private final CcdService ccdService;
    private final SscsCaseCallbackDeserializer deserializer;
    private final IdamService idamService;

    public ReminderTestController(NotificationService notificationService,
                                  AuthorisationService authorisationService,
                                  CcdService ccdService,
                                  SscsCaseCallbackDeserializer deserializer,
                                  IdamService idamService) {
        this.notificationService = notificationService;
        this.authorisationService = authorisationService;
        this.ccdService = ccdService;
        this.deserializer = deserializer;
        this.idamService = idamService;
    }

    @Operation(summary = "Send reminder notification test endpoint",
        description = "Test endpoint to simulate the response that is received when reminder service executes"
    )
    @PostMapping(value = "/reminder", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    public void reminder(
        @RequestHeader(AuthorisationService.SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
        @RequestBody String message) {
        try {
            Callback<SscsCaseData> callback = deserializer.deserialize(message);

            CaseDetails<SscsCaseData> caseDetailsBefore = callback.getCaseDetailsBefore().orElse(null);

            NotificationSscsCaseDataWrapper notificationSscsCaseDataWrapper = buildSscsCaseDataWrapper(
                callback.getCaseDetails().getCaseData(),
                caseDetailsBefore != null ? caseDetailsBefore.getCaseData() : null,
                getNotificationByCcdEvent(callback.getEvent()),
                callback.getCaseDetails().getState());

            log.info("Test endpoint: Ccd Response received for case id: {} , {}", notificationSscsCaseDataWrapper.getNewSscsCaseData().getCcdCaseId(), notificationSscsCaseDataWrapper.getNotificationEventType());

            callback.getCaseDetails().getCreatedDate();
            authorisationService.authorise(serviceAuthHeader);
            notificationService.manageNotificationAndSubscription(new CcdNotificationWrapper(notificationSscsCaseDataWrapper), true);
        } catch (Exception e) {
            log.info("Exception thrown", e);
            throw e;
        }
    }
}
