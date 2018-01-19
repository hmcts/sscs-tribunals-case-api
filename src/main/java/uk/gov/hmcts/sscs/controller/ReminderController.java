package uk.gov.hmcts.sscs.controller;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.sscs.domain.reminder.ReminderResponse;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.service.CcdService;

@RestController
public class ReminderController {

    private static final org.slf4j.Logger LOG = getLogger(ReminderController.class);

    private final CcdService service;

    @Autowired
    public ReminderController(CcdService service) {
        this.service = service;
    }

    @RequestMapping(value = "/reminder", method = POST, produces = APPLICATION_JSON_VALUE)
    public void reminder(@RequestBody ReminderResponse reminderResponse) throws CcdException {
        LOG.info("Reminder received from job scheduler: ", reminderResponse);
        service.createEvent(null, reminderResponse);
    }
}

