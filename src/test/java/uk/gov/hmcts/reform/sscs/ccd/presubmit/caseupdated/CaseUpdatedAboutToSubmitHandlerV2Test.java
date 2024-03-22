package uk.gov.hmcts.reform.sscs.ccd.presubmit.caseupdated;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class CaseUpdatedAboutToSubmitHandlerV2Test extends AbstractedCaseUpdatedAboutToSubmitHandlerTest {

    @Captor
    private ArgumentCaptor<Consumer<SscsCaseData>> caseDetailsCaptor;

    @Override
    boolean getAddLinkToOtherAssociatedCasesV2() {
        return true;
    }

    @Override
    void verifyUpdateCcdCaseServiceIsCalledWithExpectedValues(UpdateCcdCaseService updateCcdCaseService, SscsCaseDetails matchingCase1, SscsCaseDetails matchingCase2) {
        verify(updateCcdCaseService).updateCaseV2(eq(12345678L), eq(EventType.UPDATE_CASE_ONLY.getCcdType()), any(), any(), any(IdamTokens.class), caseDetailsCaptor.capture());
        caseDetailsCaptor.getValue().accept(matchingCase1.getData());

        verify(updateCcdCaseService).updateCaseV2(eq(56765676L), eq(EventType.UPDATE_CASE_ONLY.getCcdType()), any(), any(), any(IdamTokens.class), caseDetailsCaptor.capture());
        caseDetailsCaptor.getValue().accept(matchingCase2.getData());
    }
}
