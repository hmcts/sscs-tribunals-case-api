package uk.gov.hmcts.reform.sscs.ccd.presubmit.caseupdated;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class CaseUpdatedAboutToSubmitHandlerTest extends AbstractedCaseUpdatedAboutToSubmitHandlerTest {

    @Override
    boolean getAddLinkToOtherAssociatedCasesV2() {
        return false;
    }

    @Override
    void verifyUpdateCcdCaseServiceIsCalledWithExpectedValues(UpdateCcdCaseService updateCcdCaseService, SscsCaseDetails matchingCase1, SscsCaseDetails matchingCase2) {
        // do nothing as we are not testing the v2 version
    }
}
