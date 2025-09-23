package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static java.time.LocalDate.now;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.DWP_CHALLENGE_VALIDITY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.REINSTATE_APPEAL;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.AWAITING_ADMIN_ACTION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.AWAITING_INFORMATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.NONE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_TCW;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.WELSH_TRANSLATION;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.AddNoteService;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;

@Service
@Slf4j
public class InterlocServiceHandler extends EventToFieldPreSubmitCallbackHandler {

    private final AddNoteService addNoteService;
    private final DwpDocumentService documentService;

    public InterlocServiceHandler(AddNoteService addNoteService, DwpDocumentService documentService) {
        super(createMappings());
        this.addNoteService = addNoteService;
        this.documentService = documentService;
    }

    private static Map<EventType, String> createMappings() {
        Map<EventType, String> eventTypeToSecondaryStatus = new HashMap<>();
        eventTypeToSecondaryStatus.put(EventType.TCW_DIRECTION_ISSUED, AWAITING_INFORMATION.getCcdDefinition());
        eventTypeToSecondaryStatus.put(EventType.JUDGE_DIRECTION_ISSUED, AWAITING_INFORMATION.getCcdDefinition());
        eventTypeToSecondaryStatus.put(EventType.TCW_REFER_TO_JUDGE, REVIEW_BY_JUDGE.getCcdDefinition());
        eventTypeToSecondaryStatus.put(EventType.NON_COMPLIANT, REVIEW_BY_TCW.getCcdDefinition());
        eventTypeToSecondaryStatus.put(EventType.DRAFT_TO_NON_COMPLIANT, REVIEW_BY_TCW.getCcdDefinition());
        eventTypeToSecondaryStatus.put(EventType.NON_COMPLIANT_SEND_TO_INTERLOC, REVIEW_BY_TCW.getCcdDefinition());
        eventTypeToSecondaryStatus.put(REINSTATE_APPEAL, AWAITING_ADMIN_ACTION.getCcdDefinition());
        eventTypeToSecondaryStatus.put(EventType.TCW_DECISION_APPEAL_TO_PROCEED, NONE.getCcdDefinition());
        eventTypeToSecondaryStatus.put(EventType.JUDGE_DECISION_APPEAL_TO_PROCEED, NONE.getCcdDefinition());
        eventTypeToSecondaryStatus.put(EventType.SEND_TO_ADMIN, AWAITING_ADMIN_ACTION.getCcdDefinition());
        eventTypeToSecondaryStatus.put(EventType.DWP_CHALLENGE_VALIDITY, REVIEW_BY_TCW.getCcdDefinition());
        eventTypeToSecondaryStatus.put(EventType.DWP_REQUEST_TIME_EXTENSION, REVIEW_BY_TCW.getCcdDefinition());
        return eventTypeToSecondaryStatus;
    }

    protected SscsCaseData updateCaseData(SscsCaseData sscsCaseData, String newValue, EventType eventType,
                                          String userAuth) {
        if (sscsCaseData.isLanguagePreferenceWelsh()) {
            log.info("Case({}): Setting Welsh next review field to {}", sscsCaseData.getCcdCaseId(), newValue);
            sscsCaseData.setWelshInterlocNextReviewState(newValue);
            sscsCaseData.setInterlocReviewState(WELSH_TRANSLATION);
            log.info("Case({}): Set interloc review  field to {}",
                    sscsCaseData.getCcdCaseId(), sscsCaseData.getInterlocReviewState());
        } else {
            log.info("Case({}): Setting interloc review field to {}", sscsCaseData.getCcdCaseId(), newValue);
            InterlocReviewState interlocState = Arrays.stream(InterlocReviewState.values())
                .filter(x -> x.getCcdDefinition().equals(newValue))
                .findFirst()
                .orElse(null);
            sscsCaseData.setInterlocReviewState(interlocState);
        }
        setInterlocReferralDate(sscsCaseData, eventType);
        clearDirectionDueDate(sscsCaseData, eventType);
        addNoteService.addNote(userAuth, sscsCaseData, sscsCaseData.getTempNoteDetail());

        if (REINSTATE_APPEAL.equals(eventType)) {
            sscsCaseData.setOutcome("reinstated");
        }

        if (EventType.DWP_CHALLENGE_VALIDITY.equals(eventType)) {
            documentService.addToDwpDocuments(
                    sscsCaseData, sscsCaseData.getDwpChallengeValidityDocument(), DWP_CHALLENGE_VALIDITY
            );
            sscsCaseData.setDwpChallengeValidityDocument(null);
        }
        return sscsCaseData;
    }

    private void setInterlocReferralDate(SscsCaseData newSscsCaseData, EventType eventType) {
        if (eventType.equals(EventType.NON_COMPLIANT)
                || eventType.equals(EventType.DRAFT_TO_NON_COMPLIANT)
                || eventType.equals(EventType.NON_COMPLIANT_SEND_TO_INTERLOC)
                || eventType.equals(EventType.TCW_REFER_TO_JUDGE)
                || eventType.equals(EventType.DWP_REQUEST_TIME_EXTENSION)
                || eventType.equals(EventType.DWP_CHALLENGE_VALIDITY)
                || eventType.equals(REINSTATE_APPEAL)) {
            log.info("Setting interloc referral date to {} for caseId {}", now(), newSscsCaseData.getCcdCaseId());
            newSscsCaseData.setInterlocReferralDate(now());
        }
    }

    private void clearDirectionDueDate(SscsCaseData newSscsCaseData, EventType eventType) {
        if (eventType.equals(EventType.NON_COMPLIANT)
                || eventType.equals(EventType.DRAFT_TO_NON_COMPLIANT)
                || eventType.equals(EventType.NON_COMPLIANT_SEND_TO_INTERLOC)
                || eventType.equals(REINSTATE_APPEAL)) {
            log.info("Clearing direction due date for caseId {}", newSscsCaseData.getCcdCaseId());
            newSscsCaseData.setDirectionDueDate(null);
        }
    }
}
