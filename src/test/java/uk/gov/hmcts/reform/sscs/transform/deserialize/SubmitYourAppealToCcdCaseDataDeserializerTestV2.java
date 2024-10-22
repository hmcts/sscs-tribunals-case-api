package uk.gov.hmcts.reform.sscs.transform.deserialize;

import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseDataV2;

import junitparams.JUnitParamsRunner;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;

@RunWith(JUnitParamsRunner.class)
public class SubmitYourAppealToCcdCaseDataDeserializerTestV2 extends AbstractSubmitYourAppealToCcdCaseDataDeserializerTest {

    @Override
    public SscsCaseData callConvertSyaToCcdCaseDataRelevantVersion(SyaCaseWrapper syaCaseWrapper, String region, RegionalProcessingCenter rpc, boolean caseAccessManagementEnabled) {
        return convertSyaToCcdCaseDataV2(syaCaseWrapper, region, rpc, caseAccessManagementEnabled, new SscsCaseData());
    }

    @Override
    public SscsCaseData callConvertSyaToCcdCaseDataRelevantVersion(SyaCaseWrapper syaCaseWrapper, boolean caseAccessManagementEnabled) {
        return convertSyaToCcdCaseDataV2(syaCaseWrapper, caseAccessManagementEnabled, new SscsCaseData());
    }
}
