package uk.gov.hmcts.reform.sscs.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.util.DataFixtures.someStatement;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.service.pdf.MyaEventActionContext;
import uk.gov.hmcts.reform.sscs.service.pdf.StoreAppellantStatementService;
import uk.gov.hmcts.reform.sscs.service.pdf.data.AppellantStatementPdfData;

public class AppellantStatementServiceTest {

    private StoreAppellantStatementService storeAppellantStatementService;
    private OnlineHearingService onlineHearingService;
    private AppellantStatementService appellantStatementService;
    private String someOnlineHearing;

    @Before
    public void setUp() {
        storeAppellantStatementService = mock(StoreAppellantStatementService.class);
        onlineHearingService = mock(OnlineHearingService.class);

        appellantStatementService = new AppellantStatementService(
                storeAppellantStatementService, onlineHearingService
        );
        someOnlineHearing = "someOnlineHearing";
    }

    @Test
    public void cannotFindOnlineHearing() {
        when(onlineHearingService.getCcdCase(someOnlineHearing)).thenReturn(Optional.empty());
        Optional handled = appellantStatementService.handleAppellantStatement(someOnlineHearing, someStatement());

        assertThat(handled, is(Optional.empty()));
    }

    @Test
    public void findsOnlineHearing() {
        long id = 1234L;
        SscsCaseDetails caseDetails = SscsCaseDetails.builder().data(SscsCaseData.builder().build()).id(id).build();
        when(onlineHearingService.getCcdCaseByIdentifier(someOnlineHearing)).thenReturn(Optional.of(caseDetails));
        when(storeAppellantStatementService.storePdfAndUpdate(eq(id), eq(someOnlineHearing), any(AppellantStatementPdfData.class)))
                .thenReturn(mock(MyaEventActionContext.class));
        Optional handled = appellantStatementService.handleAppellantStatement(someOnlineHearing, someStatement());

        assertThat(handled.isPresent(), is(true));
    }

    @Test
    public void generatesAndSavesPdf() {
        long id = 1234L;
        SscsCaseDetails caseDetails = SscsCaseDetails.builder().data(SscsCaseData.builder().build()).id(id).build();
        when(onlineHearingService.getCcdCaseByIdentifier(someOnlineHearing)).thenReturn(Optional.of(caseDetails));
        MyaEventActionContext myaEventActionContext = mock(MyaEventActionContext.class);
        when(storeAppellantStatementService.storePdfAndUpdate(eq(id), eq(someOnlineHearing), any(AppellantStatementPdfData.class)))
                .thenReturn(myaEventActionContext);
        Optional handled = appellantStatementService.handleAppellantStatement(someOnlineHearing, someStatement());

        assertThat(handled.isPresent(), is(true));
    }
}
