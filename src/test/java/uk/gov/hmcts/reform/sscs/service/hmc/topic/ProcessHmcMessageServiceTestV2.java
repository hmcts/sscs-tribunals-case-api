package uk.gov.hmcts.reform.sscs.service.hmc.topic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.function.Function;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.exception.CaseException;
import uk.gov.hmcts.reform.sscs.exception.GetCaseException;
import uk.gov.hmcts.reform.sscs.exception.MessageProcessingException;
import uk.gov.hmcts.reform.sscs.exception.UpdateCaseException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.hmc.message.HmcMessage;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingGetResponse;
import uk.gov.hmcts.reform.sscs.service.CcdCaseService;

@ExtendWith(MockitoExtension.class)
class ProcessHmcMessageServiceTestV2 extends AbstractProcessHmcMessageServiceTest {

    @Captor
    ArgumentCaptor<Function<SscsCaseDetails, UpdateCcdCaseService.DynamicEventUpdateResult>> captor;

    @Override
    void givenWillReturn(CcdCaseService ccdCaseService, UpdateCcdCaseService updateCcdCaseService, Long caseId, SscsCaseDetails sscsCaseDetails, IdamService idamService) throws GetCaseException {
        given(idamService.getIdamTokens())
            .willReturn(IdamTokens.builder().build());
    }

    @Override
    void callProcessEventMessage(ProcessHmcMessageService processHmcMessageService, ProcessHmcMessageServiceV2 processHmcMessageServiceV2, HmcMessage hmcMessage) throws CaseException, MessageProcessingException {
        processHmcMessageServiceV2.processEventMessageV2(hmcMessage);
    }

    @Override
    void assertThatCall(UpdateCcdCaseService updateCcdCaseService, SscsCaseDetails sscsCaseDetails, DwpState dwpState) {
        verify(updateCcdCaseService).updateCaseV2DynamicEvent(any(), any(), captor.capture());

        captor.getValue().apply(sscsCaseDetails);

        assertThat(sscsCaseDetails.getData().getDwpState()).isEqualTo(DwpState.HEARING_DATE_ISSUED);
    }

    @Override
    void verifyUpdateCaseDataCalledCorrectlyForHmcStatus(CcdCaseService ccdCaseService, UpdateCcdCaseService updateCcdCaseService,
                                                         SscsCaseData caseData, HmcStatus hmcStatus, HearingGetResponse hearingGetResponse) throws UpdateCaseException {
        verify(ccdCaseService, never()).updateCaseData(any(), any(), any());
        verify(updateCcdCaseService).updateCaseV2DynamicEvent(any(), any(), captor.capture());

        UpdateCcdCaseService.DynamicEventUpdateResult dynamicEventUpdateResult = captor.getValue().apply(SscsCaseDetails.builder().data(caseData).build());

        String expectedEventType = hmcStatus.getEventMapper().apply(hearingGetResponse, caseData).getCcdType();

        assert dynamicEventUpdateResult.willCommit().equals(true);
        assert dynamicEventUpdateResult.eventType().equals(expectedEventType);
        assert dynamicEventUpdateResult.summary().equals(hmcStatus.getCcdUpdateSummary());

        String ccdUpdateDescription = String.format(hmcStatus.getCcdUpdateDescription(), HEARING_ID);
        assert dynamicEventUpdateResult.description().equals(ccdUpdateDescription);
    }
}
