package uk.gov.hmcts.reform.sscs.model.hmc.reference;

import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.HANDLING_ERROR;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.HEARING_BOOKED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_CASE_ONLY;

import java.util.function.BiFunction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.helper.mapping.HearingsEventMappers;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingGetResponse;

@RequiredArgsConstructor
@Getter
public enum HmcStatus {
    HEARING_REQUESTED("Hearing requested", null, HearingStatus.AWAITING_LISTING, "", ""),
    AWAITING_LISTING("Awaiting listing", (response, caseData) -> UPDATE_CASE_ONLY, HearingStatus.AWAITING_LISTING, "Awaiting Listing ",
            "Hearing is waiting to be listed"),
    LISTED("Listed", (response, caseData) -> HEARING_BOOKED, HearingStatus.LISTED, "Hearing Listed",
            "New hearing %s has been listed and added to case"),
    UPDATE_REQUESTED("Update requested", null, null, "", ""),
    UPDATE_SUBMITTED("Update submitted", (response, caseData) -> HEARING_BOOKED, null, "Hearing Updated",
            "The hearing with id %s has been updated and has been updated on the case"),
    EXCEPTION("Exception", (response, caseData) -> HANDLING_ERROR, HearingStatus.EXCEPTION, "Hearing Exception",
            "An error has occurred when trying to process the hearing with id %s"),
    CANCELLATION_REQUESTED("Cancellation requested", null, null, "", ""),
    CANCELLATION_SUBMITTED("Cancellation submitted", null, null, "", ""),
    CANCELLED("Cancelled", HearingsEventMappers::cancelledHandler, HearingStatus.CANCELLED,
            "Hearing Cancelled.",
            "The hearing with id %s has been successfully cancelled"),
    AWAITING_ACTUALS("Awaiting Actuals", null, HearingStatus.AWAITING_ACTUALS, "", ""),
    COMPLETED("Completed", null, HearingStatus.COMPLETED, "", ""),
    ADJOURNED("Adjourned", null, HearingStatus.ADJOURNED, "", ""),
    NOT_FOUND("Not Found", null, null, "", "");

    private final String label;
    private final BiFunction<HearingGetResponse, SscsCaseData, EventType> eventMapper;
    private final HearingStatus hearingStatus;
    private final String ccdUpdateSummary;
    private final String ccdUpdateDescription;

}
