package uk.gov.hmcts.reform.sscs.service;

import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.exception.UnhandleableHearingStateException;
import uk.gov.hmcts.reform.sscs.exception.UpdateCaseException;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;

public interface HearingsService {

    void processHearingRequest(HearingRequest hearingRequest) throws UnhandleableHearingStateException,
            UpdateCaseException, ListingException;


}
