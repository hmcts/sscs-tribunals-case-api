package uk.gov.hmcts.reform.sscs.helper.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason.OTHER;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.model.HearingWrapper;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingCancelRequestPayload;
import uk.gov.hmcts.reform.sscs.model.single.hearing.RequestDetails;

class HearingsRequestMappingTest extends HearingsMappingBase {

    @DisplayName("When a valid hearing wrapper is given buildHearingRequestDetails returns the correct Hearing Request")
    @Test
    void buildHearingRequestDetails() {
        SscsCaseData caseData = SscsCaseData.builder()
            .hearings(Arrays.asList(Hearing.builder()
                .value(HearingDetails.builder()
                    .versionNumber(1L)
                    .build())
                .build()))
            .build();
        HearingWrapper wrapper = HearingWrapper.builder()
                .caseData(caseData)
                .build();

        RequestDetails requestDetails = HearingsRequestMapping.buildHearingRequestDetails(wrapper);

        assertNotNull(requestDetails.getVersionNumber());
    }

    @DisplayName("When a valid hearing wrapper is given buildCancelHearingPayloadTest returns the correct Hearing Request Payload")
    @Test
    void buildCancelHearingPayloadTest() {
        HearingWrapper wrapper = HearingWrapper.builder()
                .cancellationReasons(List.of(OTHER))
                .build();
        HearingCancelRequestPayload result = HearingsRequestMapping.buildCancelHearingPayload(wrapper);

        assertThat(result).isNotNull();
        assertThat(result.getCancellationReasonCodes())
            .hasSize(1)
            .containsOnly(OTHER);
    }
}
