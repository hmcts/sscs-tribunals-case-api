package uk.gov.hmcts.reform.sscs.functional.ccd;

import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseDataV1;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;

@TestPropertySource(locations = "classpath:config/application_functional.properties")
@ContextConfiguration(initializers = CreateAndUpdateCaseInCcdTestV1.Initializer.class)
@SpringBootTest
public class CreateAndUpdateCaseInCcdTestV1 extends AbstractCreateAndUpdateCaseInCcdTest {

    @Override
    public SscsCaseData callConvertSyaToCcdCaseDataRelevantVersion(SyaCaseWrapper syaCaseWrapper, String region, RegionalProcessingCenter rpc, boolean caseAccessManagementEnabled) {
        return convertSyaToCcdCaseDataV1(syaCaseWrapper, region, rpc, caseAccessManagementEnabled);
    }

    @Override
    public SscsCaseData callConvertSyaToCcdCaseDataRelevantVersion(SyaCaseWrapper syaCaseWrapper, boolean caseAccessManagementEnabled) {
        return convertSyaToCcdCaseDataV1(syaCaseWrapper, caseAccessManagementEnabled);
    }
}
