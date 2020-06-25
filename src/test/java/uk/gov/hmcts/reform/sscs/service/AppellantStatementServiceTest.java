package uk.gov.hmcts.reform.sscs.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.util.DataFixtures.someStatement;

import io.github.artsok.ParameterizedRepeatedIfExceptionsTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.params.provider.ValueSource;
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

    /**
     * Repeated three times if test failed.
     * By default Exception.class will be handled in test
     */
    @ParameterizedRepeatedIfExceptionsTest
    @ValueSource(ints = {14, 15, 55, -10})
    public void reRunTest(int argument) throws IOException {
        System.out.println("here we go");
        if (argument != 100) {
            System.out.println("fail with" + argument);
            throw new IOException("Error in Test");
        } else {
            System.out.println("pass with" + argument);
            assert (true);
        }
    }

    @Test
    public void findsOnlineHearing() {
        long id = 1234L;
        when(onlineHearingService.getCcdCaseByIdentifier(someOnlineHearing)).thenReturn(Optional.of(SscsCaseDetails.builder().id(id).build()));
        when(storeAppellantStatementService.storePdf(eq(id), eq(someOnlineHearing), any(AppellantStatementPdfData.class)))
                .thenReturn(mock(MyaEventActionContext.class));
        Optional handled = appellantStatementService.handleAppellantStatement(someOnlineHearing, someStatement());

        assertThat(handled.isPresent(), is(true));
    }

    @Test
    public void generatesAndSavesPdf() {
        long id = 1234L;
        when(onlineHearingService.getCcdCaseByIdentifier(someOnlineHearing)).thenReturn(Optional.of(SscsCaseDetails.builder().id(id).build()));
        MyaEventActionContext myaEventActionContext = mock(MyaEventActionContext.class);
        when(storeAppellantStatementService.storePdf(eq(id), eq(someOnlineHearing), any(AppellantStatementPdfData.class)))
                .thenReturn(myaEventActionContext);
        Optional handled = appellantStatementService.handleAppellantStatement(someOnlineHearing, someStatement());

        assertThat(handled.isPresent(), is(true));
    }
}
