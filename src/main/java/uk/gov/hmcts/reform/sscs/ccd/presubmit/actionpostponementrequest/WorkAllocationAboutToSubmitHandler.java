package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionpostponementrequest;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
public class WorkAllocationAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final boolean workAllocationFeature;

    public WorkAllocationAboutToSubmitHandler(@Value("${feature.work-allocation.enabled}") boolean workAllocationFeature) {
        this.workAllocationFeature = workAllocationFeature;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.ACTION_POSTPONEMENT_REQUEST
                && workAllocationFeature;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData sscsCaseData = caseDetails.getCaseData();

        sscsCaseData.getWorkAllocationFields().setDaysToHearing(
            calculateDaysToHearing(sscsCaseData.getHearings()));

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private Integer calculateDaysToHearing(List<Hearing> hearings) {
        if (hearings != null) {
            Optional<LocalDate> nextHearingDate = hearings.stream()
                .map(this::hearingDate)
                .filter(Objects::nonNull)
                .filter(d -> !d.isBefore(LocalDate.now()))
                .min(LocalDate::compareTo);
            if (nextHearingDate.isPresent()) {
                return Math.toIntExact(LocalDate.now().until(nextHearingDate.get(), ChronoUnit.DAYS));
            }
        }
        return null;
    }

    private LocalDate hearingDate(Hearing hearing) {
        if (hearing != null) {
            HearingDetails details = hearing.getValue();
            if (details != null && details.getHearingDate() != null) {
                return parseDate(details.getHearingDate());
            }
        }
        return null;
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
