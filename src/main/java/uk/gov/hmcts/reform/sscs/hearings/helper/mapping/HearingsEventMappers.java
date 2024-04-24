package uk.gov.hmcts.reform.sscs.hearings.helper.mapping;

import static uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason.*;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.hearings.model.single.hearing.HearingGetResponse;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;

@Slf4j
public final class HearingsEventMappers {

    public static final List<CancellationReason> DORMANT_CANCELLATION_REASONS = List.of(WITHDRAWN, STRUCK_OUT, LAPSED);

    private HearingsEventMappers() {
    }

    public static EventType cancelledHandler(HearingGetResponse response, SscsCaseData caseData) {
        if (shouldCaseBeDormant(response)) {
            return EventType.DORMANT;
        } else if (HearingsWindowMapping.isCasePostponed(caseData)) {
            return EventType.POSTPONED;
        } else {
            return EventType.UPDATE_CASE_ONLY;
        }
    }

    private static boolean shouldCaseBeDormant(HearingGetResponse response) {
        return Optional.ofNullable(response.getRequestDetails().getCancellationReasonCodes())
            .orElse(Collections.emptyList()).stream()
            .filter(Objects::nonNull)
            .anyMatch(DORMANT_CANCELLATION_REASONS::contains);
    }
}
