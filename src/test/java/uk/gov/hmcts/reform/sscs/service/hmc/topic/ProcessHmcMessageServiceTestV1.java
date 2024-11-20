package uk.gov.hmcts.reform.sscs.service.hmc.topic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
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
import uk.gov.hmcts.reform.sscs.model.hmc.message.HmcMessage;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingGetResponse;
import uk.gov.hmcts.reform.sscs.service.CcdCaseService;

@ExtendWith(MockitoExtension.class)
class ProcessHmcMessageServiceTestV1 extends AbstractProcessHmcMessageServiceTest {

    @BeforeEach
    void setUp() {
        super.setUp();
        processHmcMessageService = new ProcessHmcMessageServiceV1(hmcHearingApiService, ccdCaseService, hearingUpdateService, processHmcMessageHelper);
    }

    @Override
    void givenWillReturn(CcdCaseService ccdCaseService, UpdateCcdCaseService updateCcdCaseService, Long caseId, SscsCaseDetails sscsCaseDetails, IdamService idamService) throws GetCaseException {
        given(ccdCaseService.getCaseDetails(CASE_ID))
            .willReturn(sscsCaseDetails);
    }

    @Override
    void callProcessEventMessage(ProcessHmcMessageService processHmcMessageService, HmcMessage hmcMessage) throws CaseException, MessageProcessingException {
        processHmcMessageService.processEventMessage(hmcMessage);
    }

    @Override
    void assertThatCall(UpdateCcdCaseService updateCcdCaseService, SscsCaseDetails sscsCaseDetails, DwpState dwpState) {
        assertThat(sscsCaseDetails.getData().getDwpState()).isEqualTo(DwpState.HEARING_DATE_ISSUED);
    }

    @Override
    void verifyUpdateCaseDataCalledCorrectlyForHmcStatus(CcdCaseService ccdCaseService, UpdateCcdCaseService updateCcdCaseService,
                                                         SscsCaseData caseData, HmcStatus hmcStatus, HearingGetResponse hearingGetResponse) throws UpdateCaseException {
        String ccdUpdateDescription = hmcStatus.getCcdUpdateDescription().formatted(HEARING_ID);
        verify(updateCcdCaseService, never()).updateCaseV2DynamicEvent(anyLong(), any(), any());
        verify(ccdCaseService, times(1))
            .updateCaseData(caseData,
                            hmcStatus.getEventMapper().apply(hearingGetResponse, caseData),
                            hmcStatus.getCcdUpdateSummary(),
                            ccdUpdateDescription);
    }
}
