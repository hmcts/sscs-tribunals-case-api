package uk.gov.hmcts.reform.sscs.ccd.presubmit.caseupdated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

public class CaseUpdatedAboutToStartHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private CaseUpdatedAboutToStartHandler handler;

    private SscsCaseData sscsCaseData;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @BeforeEach
    public void setUp() {
        openMocks(this);

        handler = new CaseUpdatedAboutToStartHandler();

        when(callback.getEvent()).thenReturn(EventType.CASE_UPDATED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("ccdId")
                .appeal(Appeal.builder()
                        .benefitType(BenefitType.builder()
                                .code("PIP")
                                .build())
                        .appellant(Appellant.builder()
                                .name(Name.builder().firstName("First").lastName("Last").build())
                                .address(Address.builder().line1("Line1").line2("Line2").postcode("CM120NS").build())
                                .identity(Identity.builder().nino("AB223344B").dob("1995-12-20").build())
                                .build())
                        .build())
                .benefitCode("002")
                .issueCode("DD")
                .build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    void test() {
        var result = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        var benefitSelection = result.getData().getAppeal().getBenefitType().getDescriptionSelection();

        assertThat(benefitSelection).isNotNull();
        assertThat(benefitSelection.getValue()).isNotNull();
        assertThat(benefitSelection.getValue().getCode()).isEqualTo("002");
    }
}
