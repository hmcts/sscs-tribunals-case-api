package uk.gov.hmcts.reform.sscs.helper.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DORMANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Postponement;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.model.single.hearing.CaseDetails;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingDetails;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingGetResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.RequestDetails;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;

class HearingsEventMappersTest {


    private HearingGetResponse response;

    private SscsCaseData caseData;

    @BeforeEach
    void setup() {
        response = HearingGetResponse.builder()
            .requestDetails(RequestDetails.builder().build())
            .hearingResponse(HearingResponse.builder().build())
            .hearingDetails(HearingDetails.builder().build())
            .caseDetails(CaseDetails.builder().build())
            .partyDetails(Collections.emptyList())
            .hearingResponse(HearingResponse.builder().build())
            .build();

        caseData = SscsCaseData.builder()
            .postponement(Postponement.builder()
                .unprocessedPostponement(YES)
                .postponementEvent(EventType.READY_TO_LIST)
                .build())
            .build();
    }


    @DisplayName("When the cancellation reason is valid, the EventType should be UPDATE_CASE_ONLY")
    @ParameterizedTest
    @EnumSource(
        value = CancellationReason.class,
        mode = EnumSource.Mode.EXCLUDE,
        names = {"WITHDRAWN", "STRUCK_OUT", "LAPSED"})
    void testCancelledHandler(CancellationReason value) {
        response.getRequestDetails().setCancellationReasonCodes(List.of(value));

        caseData.setPostponement(Postponement.builder().build());

        EventType result = HearingsEventMappers.cancelledHandler(response, caseData);

        assertThat(result).isEqualTo(EventType.UPDATE_CASE_ONLY);
    }


    @DisplayName("When the cancellation reason is valid for Dormant, the EventType should be Dormant")
    @ParameterizedTest
    @EnumSource(
        value = CancellationReason.class,
        mode = EnumSource.Mode.INCLUDE,
        names = {"WITHDRAWN", "STRUCK_OUT", "LAPSED"})
    void testCancelledHandlerDormant(CancellationReason value) {
        response.getRequestDetails().setCancellationReasonCodes(List.of(value));

        EventType result = HearingsEventMappers.cancelledHandler(response, caseData);

        assertThat(result).isEqualTo(DORMANT);
    }

    @DisplayName("When the case is postponed cancelledHandler returns the EventType Postponed")
    @Test
    void testCancelledHandlerRelisted() {
        caseData.getPostponement().setPostponementEvent(EventType.READY_TO_LIST);

        EventType result = HearingsEventMappers.cancelledHandler(response, caseData);

        assertThat(result).isEqualTo(EventType.POSTPONED);
    }
}
