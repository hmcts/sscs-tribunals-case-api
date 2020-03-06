package uk.gov.hmcts.reform.sscs.ccd.presubmit.editbundle;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.BundleRequestExecutor;

@RunWith(JUnitParamsRunner.class)
public class EditBundleAboutToStartTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private EditBundleAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private BundleRequestExecutor bundleRequestExecutor;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        initMocks(this);
        handler = new EditBundleAboutToStartHandler(bundleRequestExecutor, "bundleUrl.com");

        when(callback.getEvent()).thenReturn(EventType.EDIT_BUNDLE);

        sscsCaseData = SscsCaseData.builder().build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(bundleRequestExecutor.post(any(), any())).thenReturn(new PreSubmitCallbackResponse<>(sscsCaseData));
    }

    @Test
    public void givenAValidEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenANonEditBundleEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenABundleSelectedToBeStitched_thenSetDefaultConfigDetails() {
        Bundle bundle = Bundle.builder().value(BundleDetails.builder().eligibleForStitching("Yes").build()).build();
        List<Bundle> bundles = new ArrayList<>();
        bundles.add(bundle);

        callback.getCaseDetails().getCaseData().setCaseBundles(bundles);
        BundleDetails bundleResult = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION).getData().getCaseBundles().get(0).getValue();

        assertEquals("SscsBundle.pdf", bundleResult.getFileName());
        assertEquals("SSCS-cover-page.docx", bundleResult.getCoverpageTemplate());
        assertEquals("Yes", bundleResult.getHasTableOfContents());
        assertEquals("Yes", bundleResult.getHasCoversheets());
        assertEquals("topCenter", bundleResult.getPaginationStyle());
        assertEquals("numberOfPages", bundleResult.getPageNumberFormat());
        assertNull(bundleResult.getStitchStatus());
    }

    @Test
    public void givenACaseWithMultipleBundles_thenSetDefaultConfigDetailsForSelectedBundle() {
        Bundle bundle1 = Bundle.builder().value(BundleDetails.builder().eligibleForStitching("Yes").build()).build();
        Bundle bundle2 = Bundle.builder().value(BundleDetails.builder().eligibleForStitching("No").build()).build();
        List<Bundle> bundles = new ArrayList<>();
        bundles.add(bundle1);
        bundles.add(bundle2);

        callback.getCaseDetails().getCaseData().setCaseBundles(bundles);
        PreSubmitCallbackResponse<SscsCaseData> result = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        BundleDetails bundleResult1 = result.getData().getCaseBundles().get(0).getValue();

        assertEquals("SscsBundle.pdf", bundleResult1.getFileName());
        assertEquals("SSCS-cover-page.docx", bundleResult1.getCoverpageTemplate());
        assertEquals("Yes", bundleResult1.getHasTableOfContents());
        assertEquals("Yes", bundleResult1.getHasCoversheets());
        assertEquals("topCenter", bundleResult1.getPaginationStyle());
        assertEquals("numberOfPages", bundleResult1.getPageNumberFormat());
        assertNull(bundleResult1.getStitchStatus());

        BundleDetails bundleResult2 = result.getData().getCaseBundles().get(1).getValue();

        assertNull(bundleResult2.getFileName());
        assertNull(bundleResult2.getCoverpageTemplate());
        assertNull(bundleResult2.getHasTableOfContents());
        assertNull(bundleResult2.getHasCoversheets());
        assertNull(bundleResult2.getPaginationStyle());
        assertNull(bundleResult2.getPageNumberFormat());
    }

    @Test
    public void givenEditBundleEvent_thenTriggerTheExternalEditBundleEvent() {
        Bundle bundle = Bundle.builder().value(BundleDetails.builder().eligibleForStitching("Yes").build()).build();
        List<Bundle> bundles = new ArrayList<>();
        bundles.add(bundle);

        callback.getCaseDetails().getCaseData().setCaseBundles(bundles);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(bundleRequestExecutor).post(callback, "bundleUrl.com/api/stitch-ccd-bundles");
    }

    @Test
    public void givenEditBundleEventWithNoAmendedBundleOptionSelectedAndIgnoreWarningsFalse_thenReturnAWarningToCaseworker() {
        Bundle bundle = Bundle.builder().value(BundleDetails.builder().eligibleForStitching("No").build()).build();
        List<Bundle> bundles = new ArrayList<>();
        bundles.add(bundle);

        when(callback.isIgnoreWarnings()).thenReturn(false);
        callback.getCaseDetails().getCaseData().setCaseBundles(bundles);
        PreSubmitCallbackResponse<SscsCaseData> result = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String warning = result.getWarnings().stream()
                .findFirst()
                .orElse("");
        assertEquals("No bundle selected to be amended. The stitched PDF will not be updated. Are you sure you want to continue?", warning);

        verifyNoInteractions(bundleRequestExecutor);
    }

    @Test
    public void givenEditBundleEventWithNoAmendedBundleOptionSelectedAndIgnoreWarningsTrue_thenTriggerTheExternalEditBundleEvent() {
        Bundle bundle = Bundle.builder().value(BundleDetails.builder().eligibleForStitching("No").build()).build();
        List<Bundle> bundles = new ArrayList<>();
        bundles.add(bundle);

        when(callback.isIgnoreWarnings()).thenReturn(true);
        callback.getCaseDetails().getCaseData().setCaseBundles(bundles);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(bundleRequestExecutor).post(callback, "bundleUrl.com/api/stitch-ccd-bundles");
    }

    @Test
    public void givenNoBundlesToEdit_thenReturnNull() {
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response);
    }
}