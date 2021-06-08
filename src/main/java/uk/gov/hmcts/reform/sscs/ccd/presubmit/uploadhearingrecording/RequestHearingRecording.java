package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadhearingrecording;

import java.util.List;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;

public class RequestHearingRecording implements CaseData {
    private DynamicList dwpRequestableHearingDetails;
    private DynamicList dwpRequestedHearingDetails;
    private DynamicList dwpReleasedHearingDetails;
    private List<HearingRecordingRequest> hearingRecordingRequests;

}
