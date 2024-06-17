package uk.gov.hmcts.reform.sscs.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.util.SyaServiceHelper.getSyaCaseWrapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.config.CcdRequestDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@ExtendWith(MockitoExtension.class)
class SubmitTestAppealServiceTest {
    @Mock
    private CoreCaseDataApi coreCaseDataApi;
    @Mock
    private SubmitAppealService submitAppealService;
    @Mock
    private CcdClient ccdClient;
    @Mock
    private IdamService idamService;
    @Mock
    private SscsCcdConvertService sscsCcdConvertService;
    @Mock
    private CcdRequestDetails ccdRequestDetails;

    @InjectMocks
    private SubmitTestAppealService submitTestAppealService;

    @Test
    void testSubmitAppeal() {
        String userToken = "abcd";
        String caseType = "appeal";
        SyaCaseWrapper appealData = getSyaCaseWrapper();
        Appellant appellant = Appellant.builder()
                .identity(Identity.builder()
                        .nino("AB223344B")
                        .build())
                .build();
        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("56765676")
                .appeal(
                        Appeal.builder()
                                .appellant(appellant)
                                .build())
                .build();

        when(idamService.getIdamTokens())
                .thenReturn(IdamTokens.builder()
                        .build()
            );
        when(submitAppealService.convertAppealToSscsCaseData(appealData))
                .thenReturn(
                        sscsCaseData
            );
        when(submitAppealService.findEventType(sscsCaseData, false))
                .thenReturn(EventType.VALID_APPEAL);

        CaseDetails caseDetails = CaseDetails.builder()
                .id(12121L)
                .build();
        when(coreCaseDataApi.submitForCaseworker(
                any(),
                any(),
                any(),
                any(),
                any(),
                anyBoolean(),
                any()
                )).thenReturn(caseDetails);

        when(sscsCcdConvertService.getCaseDetails(caseDetails))
                .thenReturn(SscsCaseDetails.builder()
                        .id(caseDetails.getId())
                        .build()
            );

        Long caseId = submitTestAppealService.submitAppeal(appealData, userToken, caseType);
        assertNotNull(caseId);
        verify(ccdClient).startCaseForCaseworker(any(), eq(EventType.VALID_APPEAL.getCcdType()));
    }
}
