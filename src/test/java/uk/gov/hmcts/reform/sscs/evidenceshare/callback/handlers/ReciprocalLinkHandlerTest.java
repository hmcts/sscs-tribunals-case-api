package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ASSOCIATE_CASE;
import static uk.gov.hmcts.reform.sscs.utility.StringUtils.getMaskedNino;

import ch.qos.logback.classic.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLinkDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.util.LogCaptureExtension;

@ExtendWith(MockitoExtension.class)
public class ReciprocalLinkHandlerTest {

    public static final String YES = "Yes";
    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CcdService ccdService;

    @Mock
    private IdamService idamService;

    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    private ReciprocalLinkHandler handler;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Captor
    private ArgumentCaptor<Consumer<SscsCaseDetails>> capture;

    @RegisterExtension
    private final LogCaptureExtension logCapture =
            new LogCaptureExtension(ReciprocalLinkHandler.class);

    HashMap<String, String> map = new HashMap<String, String>();

    @BeforeEach
    void setUp() {
        lenient().when(callback.getEvent()).thenReturn(EventType.VALID_APPEAL_CREATED);

        handler = new ReciprocalLinkHandler(ccdService, idamService, updateCcdCaseService);

        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().appellant(
                Appellant.builder().identity(Identity.builder().nino("AB00000Y").build()).build())
            .mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();

        lenient().when(callback.getCaseDetails()).thenReturn(caseDetails);
        lenient().when(caseDetails.getId()).thenReturn(7656765L);
        lenient().when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        map.put("case.appeal.appellant.identity.nino", "AB00000Y");
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"VALID_APPEAL_CREATED", "DRAFT_TO_VALID_APPEAL_CREATED",
        "NON_COMPLIANT", "DRAFT_TO_NON_COMPLIANT", "INCOMPLETE_APPLICATION_RECEIVED", "DRAFT_TO_INCOMPLETE_APPLICATION"})
    void givenAValidEvent_thenReturnTrue(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);

        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    void givenANonReciprocalLinkEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(SUBMITTED, callback));
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, names = {"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
    void givenANonReciprocalLinkCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    void givenAReciprocalLinkCallbackType_thenReturnTrue() {
        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    void givenAssociatedCase_thenAddReciprocalLinkToAssociatedCase() {

        SscsCaseDetails associatedCase1 = SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().build()).build();
        SscsCaseDetails associatedCase2 = SscsCaseDetails.builder().id(7656765L).data(sscsCaseData).build();
        List<SscsCaseDetails> associatedCaseList = new ArrayList<>();
        associatedCaseList.add(associatedCase1);
        associatedCaseList.add(associatedCase2);

        given(ccdService.findCaseBy(anyString(), anyString(), any())).willReturn(associatedCaseList);

        handler.handle(SUBMITTED, callback);

        verify(updateCcdCaseService).updateCaseV2(eq(12345678L), eq(ASSOCIATE_CASE.getCcdType()), eq("Associate case"), eq("Associated case added"), any(), capture.capture());

        capture.getValue().accept(associatedCase2);
        assertEquals("7656765", associatedCase2.getData().getAssociatedCase().get(0).getValue().getCaseReference());
        assertEquals(YES, associatedCase2.getData().getLinkedCasesBoolean());
        logCapture
                .assertLogContains("Nino " + getMaskedNino("AB00000Y"), Level.INFO)
                .assertLogDoesNotContain("AB00000Y", Level.INFO);
    }

    @Test
    void givenAssociatedCaseWithExistingAssociatedCase_thenAddReciprocalLinkToAssociatedCase() {

        List<CaseLink> caseLinks = new ArrayList<>();
        caseLinks.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("1").build()).build());
        SscsCaseDetails associatedCase1 = SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().associatedCase(caseLinks).build()).build();
        SscsCaseDetails associatedCase2 = SscsCaseDetails.builder().id(7656765L).data(sscsCaseData).build();
        List<SscsCaseDetails> associatedCaseList = new ArrayList<>();
        associatedCaseList.add(associatedCase1);
        associatedCaseList.add(associatedCase2);

        given(ccdService.findCaseBy(anyString(), anyString(), any())).willReturn(associatedCaseList);

        handler.handle(SUBMITTED, callback);

        verify(updateCcdCaseService).updateCaseV2(eq(12345678L), eq(ASSOCIATE_CASE.getCcdType()), eq("Associate case"), eq("Associated case added"), any(), capture.capture());
        capture.getValue().accept(associatedCase2);
        assertEquals("1", associatedCase2.getData().getAssociatedCase().get(0).getValue().getCaseReference());
        assertEquals("7656765", associatedCase2.getData().getAssociatedCase().get(1).getValue().getCaseReference());
        assertEquals(YES, associatedCase2.getData().getLinkedCasesBoolean());
    }

    @Test
    void givenMultipleAssociatedCases_thenAddReciprocalLinkToAllCases() {
        List<SscsCaseDetails> associatedCaseList = new ArrayList<>();
        SscsCaseDetails associatedCase1 = SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().build()).build();
        SscsCaseDetails associatedCase2 = SscsCaseDetails.builder().id(34343434L).data(SscsCaseData.builder().build()).build();
        SscsCaseDetails associatedCase3 = SscsCaseDetails.builder().id(7656765L).data(sscsCaseData).build();

        associatedCaseList.add(associatedCase1);
        associatedCaseList.add(associatedCase2);
        associatedCaseList.add(associatedCase3);

        given(ccdService.findCaseBy(anyString(), anyString(), any())).willReturn(associatedCaseList);

        handler.handle(SUBMITTED, callback);

        verify(updateCcdCaseService, times(2)).updateCaseV2(any(), eq(ASSOCIATE_CASE.getCcdType()), eq("Associate case"), eq("Associated case added"), any(), any());
        verify(updateCcdCaseService).updateCaseV2(eq(12345678L), eq(ASSOCIATE_CASE.getCcdType()), eq("Associate case"), eq("Associated case added"), any(), capture.capture());
        verify(updateCcdCaseService).updateCaseV2(eq(34343434L), eq(ASSOCIATE_CASE.getCcdType()), eq("Associate case"), eq("Associated case added"), any(), capture.capture());
        capture.getValue().accept(associatedCase3);
        assertEquals("7656765", associatedCase3.getData().getAssociatedCase().get(0).getValue().getCaseReference());
        assertEquals("7656765", associatedCase3.getData().getAssociatedCase().get(0).getValue().getCaseReference());
        assertEquals(YES, associatedCase3.getData().getLinkedCasesBoolean());
    }

    @Test
    void givenMoreThan10AssociatedCases_thenDoNotAddReciprocalLinkToAllCases() {
        List<SscsCaseDetails> associatedCaseList = new ArrayList<>();
        associatedCaseList.add(SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().build()).build());
        associatedCaseList.add(SscsCaseDetails.builder().id(56765671L).data(SscsCaseData.builder().build()).build());
        associatedCaseList.add(SscsCaseDetails.builder().id(56765672L).data(SscsCaseData.builder().build()).build());
        associatedCaseList.add(SscsCaseDetails.builder().id(56765673L).data(SscsCaseData.builder().build()).build());
        associatedCaseList.add(SscsCaseDetails.builder().id(56765674L).data(SscsCaseData.builder().build()).build());
        associatedCaseList.add(SscsCaseDetails.builder().id(56765675L).data(SscsCaseData.builder().build()).build());
        associatedCaseList.add(SscsCaseDetails.builder().id(56765676L).data(SscsCaseData.builder().build()).build());
        associatedCaseList.add(SscsCaseDetails.builder().id(56765677L).data(SscsCaseData.builder().build()).build());
        associatedCaseList.add(SscsCaseDetails.builder().id(56765678L).data(SscsCaseData.builder().build()).build());
        associatedCaseList.add(SscsCaseDetails.builder().id(56765679L).data(SscsCaseData.builder().build()).build());
        associatedCaseList.add(SscsCaseDetails.builder().id(7656765L).data(sscsCaseData).build());

        given(ccdService.findCaseBy(anyString(), anyString(), any())).willReturn(associatedCaseList);

        handler.handle(SUBMITTED, callback);

        verify(ccdService, times(0)).updateCase(any(), any(), eq(ASSOCIATE_CASE.getCcdType()), eq("Associate case"), eq("Associated case added"), any());
    }

    @Test
    void addNoAssociatedCases() {
        List<SscsCaseDetails> associatedCaseList = new ArrayList<>();

        given(ccdService.findCaseBy(anyString(), anyString(), any())).willReturn(associatedCaseList);

        handler.handle(SUBMITTED, callback);

        verify(ccdService, times(0)).updateCase(any(), any(), eq(ASSOCIATE_CASE.getCcdType()), eq("Associate case"), eq("Associated case added"), any());
    }

}
