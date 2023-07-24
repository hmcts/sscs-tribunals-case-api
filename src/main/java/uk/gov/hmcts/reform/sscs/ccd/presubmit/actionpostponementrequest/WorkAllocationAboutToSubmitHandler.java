package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionpostponementrequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkAllocationAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.ACTION_POSTPONEMENT_REQUEST;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData sscsCaseData = caseDetails.getCaseData();

        Integer daysToHearing = Math.toIntExact(calculateDaysToHearing(sscsCaseData.getHearings()));

        //sscsCaseData.getWorkAllocationFields().setDaysToHearing(daysToHearing);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private Long calculateDaysToHearing(List<Hearing> hearings) {
        if (hearings != null) {
           Optional<LocalDate> nextHearingDate = hearings.stream()
                .map(h -> h.getValue().getHearingDate())
                .filter(s -> s!=null)
                .map(s -> LocalDate.parse(s))
                .filter(d -> !d.isBefore(LocalDate.now()))
                .min(LocalDate::compareTo);
           if(nextHearingDate.isPresent()) {
                return LocalDate.now().until(nextHearingDate.get(), ChronoUnit.DAYS);
           }
        }
        return null;
    }
}
