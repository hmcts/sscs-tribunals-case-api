package uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType.APPEAL_TO_PROCEED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType.CONFIDENTIALITY_GRANTED_SEND_TO_ADMIN;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType.CONFIDENTIALITY_REFUSED_SEND_TO_ADMIN;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType.GRANT_EXTENSION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType.GRANT_REINSTATEMENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType.GRANT_URGENT_HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType.ISSUE_AND_SEND_TO_ADMIN;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType.PROVIDE_INFORMATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType.REFUSE_EXTENSION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType.REFUSE_HEARING_RECORDING_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType.REFUSE_REINSTATEMENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType.REFUSE_URGENT_HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DIRECTION_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.WITH_DWP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued.ExtensionNextEventItemList.NO_FURTHER_ACTION;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued.ExtensionNextEventItemList.SEND_TO_LISTING;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued.ExtensionNextEventItemList.SEND_TO_VALID_APPEAL;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.JUDGE;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.SUPER_USER;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.TCW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitCode;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.HmcHearingType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.JointParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.RequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.reference.data.model.ConfidentialityType;

@ExtendWith(MockitoExtension.class)
class DirectionIssuedAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private IdamService idamService;

    @Mock
    private UserDetails userDetails;

    private Callback<SscsCaseData> callback;
    private CaseDetails<SscsCaseData> caseDetails;
    private SscsCaseData sscsCaseData;

    private DirectionIssuedAboutToStartHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DirectionIssuedAboutToStartHandler(false, false, idamService);
        lenient().when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);
        lenient().when(userDetails.hasRole(SUPER_USER)).thenReturn(true);
        sscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build())
                .build();
        caseDetails = new CaseDetails<>(1234L, "SSCS", WITH_DWP, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), DIRECTION_ISSUED, false);
    }

    @Test
    void givenANonHandleEvidenceEvent_thenReturnFalse() {
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), APPEAL_RECEIVED, false);

        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @ParameterizedTest
    @CsvSource({"ABOUT_TO_SUBMIT", "SUBMITTED"})
    void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @ParameterizedTest
    @CsvSource({"ABOUT_TO_START", "MID_EVENT"})
    void givenAValidCallbackType_thenReturnTrue(CallbackType callbackType) {
        assertTrue(handler.canHandle(callbackType, callback));
    }

    @Test
    void givenValidAppeal_populateExtensionNextEventDropdown() {
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), DIRECTION_ISSUED, false);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(SEND_TO_LISTING.getCode(), SEND_TO_LISTING.getLabel()));
        listOptions.add(new DynamicListItem(NO_FURTHER_ACTION.getCode(), NO_FURTHER_ACTION.getLabel()));
        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(expected, response.getData().getExtensionNextEventDl());
        assertEquals(2, response.getData().getExtensionNextEventDl().getListItems().size());
        assertNull(response.getData().getHmcHearingType());
        assertEquals(NO, response.getData().getExtendedSscsCaseData().getSelectNextHmcHearingType());
    }

    @Test
    void givenValidAppealWithExtensionNextEventAlreadyPopulated_thenAutoSelectExtensionNextEventValue() {
        sscsCaseData = SscsCaseData.builder().extensionNextEventDl(new DynamicList(NO_FURTHER_ACTION.getCode()))
                .appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build())
                .build();
        caseDetails = new CaseDetails<>(1234L, "SSCS", WITH_DWP, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), DIRECTION_ISSUED, false);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(SEND_TO_LISTING.getCode(), SEND_TO_LISTING.getLabel()));
        listOptions.add(new DynamicListItem(NO_FURTHER_ACTION.getCode(), NO_FURTHER_ACTION.getLabel()));
        DynamicList expected = new DynamicList(
                new DynamicListItem(NO_FURTHER_ACTION.getCode(), NO_FURTHER_ACTION.getCode()), listOptions);
        assertEquals(expected, response.getData().getExtensionNextEventDl());
        assertEquals(2, response.getData().getExtensionNextEventDl().getListItems().size());
    }

    @ParameterizedTest
    @CsvSource({"INCOMPLETE_APPLICATION", "INCOMPLETE_APPLICATION_INFORMATION_REQUESTED", "INTERLOCUTORY_REVIEW_STATE"})
    void givenNonValidAppeal_populateExtensionNextEventDropdown(State state) {
        caseDetails = new CaseDetails<>(1234L, "SSCS", state, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), DIRECTION_ISSUED, false);
        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(SEND_TO_LISTING.getCode(), SEND_TO_LISTING.getLabel()));
        listOptions.add(new DynamicListItem(NO_FURTHER_ACTION.getCode(), NO_FURTHER_ACTION.getLabel()));
        listOptions.add(new DynamicListItem(SEND_TO_VALID_APPEAL.getCode(), SEND_TO_VALID_APPEAL.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(expected, response.getData().getExtensionNextEventDl());
        assertEquals(3, response.getData().getExtensionNextEventDl().getListItems().size());
    }

    @Test
    void givenAppealWithTimeExtension_populateDirectionTypeDropdown() {
        sscsCaseData.setTimeExtensionRequested("Yes");

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(APPEAL_TO_PROCEED.toString(), APPEAL_TO_PROCEED.getLabel()));
        listOptions.add(new DynamicListItem(PROVIDE_INFORMATION.toString(), PROVIDE_INFORMATION.getLabel()));
        listOptions.add(new DynamicListItem(ISSUE_AND_SEND_TO_ADMIN.toString(), ISSUE_AND_SEND_TO_ADMIN.getLabel()));
        listOptions.add(new DynamicListItem(GRANT_EXTENSION.toString(), GRANT_EXTENSION.getLabel()));
        listOptions.add(new DynamicListItem(REFUSE_EXTENSION.toString(), REFUSE_EXTENSION.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(expected, response.getData().getDirectionTypeDl());
        assertEquals(5, response.getData().getDirectionTypeDl().getListItems().size());
    }

    @Test
    void givenAppealWithConfidentialityReferralAndFeatureFlagEnabled_populateDirectionTypeDropdown() {
        handler = new DirectionIssuedAboutToStartHandler(false, true, idamService);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.CONFIDENTIALITY);

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(APPEAL_TO_PROCEED.toString(), APPEAL_TO_PROCEED.getLabel()));
        listOptions.add(new DynamicListItem(PROVIDE_INFORMATION.toString(), PROVIDE_INFORMATION.getLabel()));
        listOptions.add(new DynamicListItem(ISSUE_AND_SEND_TO_ADMIN.toString(), ISSUE_AND_SEND_TO_ADMIN.getLabel()));
        listOptions.add(new DynamicListItem(CONFIDENTIALITY_GRANTED_SEND_TO_ADMIN.toString(),
            CONFIDENTIALITY_GRANTED_SEND_TO_ADMIN.getLabel()));
        listOptions.add(new DynamicListItem(CONFIDENTIALITY_REFUSED_SEND_TO_ADMIN.toString(),
            CONFIDENTIALITY_REFUSED_SEND_TO_ADMIN.getLabel()));

        when(idamService.getUserDetails(anyString())).thenReturn(userDetails);
        when(userDetails.getRoles()).thenReturn(List.of(SUPER_USER.getValue(), TCW.getValue()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(expected, response.getData().getDirectionTypeDl());
        assertEquals(5, response.getData().getDirectionTypeDl().getListItems().size());
    }

    @Test
    void givenAppealWithConfidentialityReferralAndFeatureFlagEnabledAndNonAuthorisedUser_doNotPopulateConfidentialityDirectionOptions() {
        handler = new DirectionIssuedAboutToStartHandler(false, true, idamService);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.CONFIDENTIALITY);

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(APPEAL_TO_PROCEED.toString(), APPEAL_TO_PROCEED.getLabel()));
        listOptions.add(new DynamicListItem(PROVIDE_INFORMATION.toString(), PROVIDE_INFORMATION.getLabel()));
        listOptions.add(new DynamicListItem(ISSUE_AND_SEND_TO_ADMIN.toString(), ISSUE_AND_SEND_TO_ADMIN.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(expected, response.getData().getDirectionTypeDl());
        assertEquals(3, response.getData().getDirectionTypeDl().getListItems().size());
    }

    private static Stream<Arguments> authorisedRoles() {
        return Stream.of(
            Arguments.of(SUPER_USER.getValue()),
            Arguments.of(TCW.getValue()),
            Arguments.of(JUDGE.getValue())
        );
    }

    @ParameterizedTest
    @MethodSource("authorisedRoles")
    void givenConfidentialityReferralAndFeatureFlagEnabled_eachAuthorisedRoleAloneShowsConfidentialityOptions(final String role) {
        handler = new DirectionIssuedAboutToStartHandler(false, true, idamService);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.CONFIDENTIALITY);
        when(idamService.getUserDetails(anyString())).thenReturn(userDetails);
        when(userDetails.getRoles()).thenReturn(List.of(role));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDirectionTypeDl().getListItems())
            .hasSize(5)
            .extracting(DynamicListItem::getCode)
            .contains(CONFIDENTIALITY_GRANTED_SEND_TO_ADMIN.toString(), CONFIDENTIALITY_REFUSED_SEND_TO_ADMIN.toString());
    }

    @Test
    void givenConfidentialityReferralAndFeatureFlagEnabled_nullUserDetailsDoesNotShowConfidentialityOptions() {
        handler = new DirectionIssuedAboutToStartHandler(false, true, idamService);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.CONFIDENTIALITY);
        when(idamService.getUserDetails(anyString())).thenReturn(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDirectionTypeDl().getListItems())
            .hasSize(3)
            .extracting(DynamicListItem::getCode)
            .doesNotContain(CONFIDENTIALITY_GRANTED_SEND_TO_ADMIN.toString(), CONFIDENTIALITY_REFUSED_SEND_TO_ADMIN.toString());
    }

    @Test
    void givenConfidentialityReferralAndFeatureFlagEnabled_emptyRolesListDoesNotShowConfidentialityOptions() {
        handler = new DirectionIssuedAboutToStartHandler(false, true, idamService);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.CONFIDENTIALITY);
        when(idamService.getUserDetails(anyString())).thenReturn(userDetails);
        when(userDetails.getRoles()).thenReturn(Collections.emptyList());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDirectionTypeDl().getListItems())
            .hasSize(3)
            .extracting(DynamicListItem::getCode)
            .doesNotContain(CONFIDENTIALITY_GRANTED_SEND_TO_ADMIN.toString(), CONFIDENTIALITY_REFUSED_SEND_TO_ADMIN.toString());
    }

    @Test
    void givenConfidentialityReferralAndFeatureFlagEnabled_unrecognisedRoleDoesNotShowConfidentialityOptions() {
        handler = new DirectionIssuedAboutToStartHandler(false, true, idamService);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.CONFIDENTIALITY);
        when(idamService.getUserDetails(anyString())).thenReturn(userDetails);
        when(userDetails.getRoles()).thenReturn(List.of("caseworker-sscs"));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDirectionTypeDl().getListItems())
            .hasSize(3)
            .extracting(DynamicListItem::getCode)
            .doesNotContain(CONFIDENTIALITY_GRANTED_SEND_TO_ADMIN.toString(), CONFIDENTIALITY_REFUSED_SEND_TO_ADMIN.toString());
    }

    @Test
    void givenAppealWithConfidentialityReferralAndFeatureFlagDisabled_doNotPopulateDirectionTypeDropdown() {
        handler = new DirectionIssuedAboutToStartHandler(false, false, idamService);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.CONFIDENTIALITY);

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(APPEAL_TO_PROCEED.toString(), APPEAL_TO_PROCEED.getLabel()));
        listOptions.add(new DynamicListItem(PROVIDE_INFORMATION.toString(), PROVIDE_INFORMATION.getLabel()));
        listOptions.add(new DynamicListItem(ISSUE_AND_SEND_TO_ADMIN.toString(), ISSUE_AND_SEND_TO_ADMIN.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(expected, response.getData().getDirectionTypeDl());
        assertEquals(3, response.getData().getDirectionTypeDl().getListItems().size());
    }

    @Test
    void givenAppealWithoutConfidentialityReferralAndFeatureFlagEnaled_doNotPopulateDirectionTypeDropdown() {
        handler = new DirectionIssuedAboutToStartHandler(false, true, idamService);

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(APPEAL_TO_PROCEED.toString(), APPEAL_TO_PROCEED.getLabel()));
        listOptions.add(new DynamicListItem(PROVIDE_INFORMATION.toString(), PROVIDE_INFORMATION.getLabel()));
        listOptions.add(new DynamicListItem(ISSUE_AND_SEND_TO_ADMIN.toString(), ISSUE_AND_SEND_TO_ADMIN.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(expected, response.getData().getDirectionTypeDl());
        assertEquals(3, response.getData().getDirectionTypeDl().getListItems().size());
    }

    @Test
    void givenAnyBenefitCodeAppeal_populateDirectionTypeDropdown() {
        for (BenefitCode benefitCode : BenefitCode.values()) {
            sscsCaseData.setBenefitCode(String.valueOf(benefitCode.getCcdReference()));

            List<DynamicListItem> listOptions = new ArrayList<>();
            listOptions.add(new DynamicListItem(APPEAL_TO_PROCEED.toString(), APPEAL_TO_PROCEED.getLabel()));
            listOptions.add(new DynamicListItem(PROVIDE_INFORMATION.toString(), PROVIDE_INFORMATION.getLabel()));
            listOptions.add(new DynamicListItem(ISSUE_AND_SEND_TO_ADMIN.toString(), ISSUE_AND_SEND_TO_ADMIN.getLabel()));

            PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

            DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
            assertEquals(expected, response.getData().getDirectionTypeDl());
            assertEquals(3, response.getData().getDirectionTypeDl().getListItems().size());
        }
    }

    @Test
    void givenNonSpecificBenefitCodeAppeal_doNotPopulateIssueAndSendToAdmin() {
        sscsCaseData.setBenefitCode("001");

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(APPEAL_TO_PROCEED.toString(), APPEAL_TO_PROCEED.getLabel()));
        listOptions.add(new DynamicListItem(PROVIDE_INFORMATION.toString(), PROVIDE_INFORMATION.getLabel()));
        listOptions.add(new DynamicListItem(ISSUE_AND_SEND_TO_ADMIN.toString(), ISSUE_AND_SEND_TO_ADMIN.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(expected, response.getData().getDirectionTypeDl());
        assertEquals(3, response.getData().getDirectionTypeDl().getListItems().size());
    }

    @Test
    void givenAppealWithReinstatementRequest_populateDirectionTypeDropdown() {
        handler = new DirectionIssuedAboutToStartHandler(false, false, idamService);
        sscsCaseData.setReinstatementOutcome(RequestOutcome.IN_PROGRESS);

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(APPEAL_TO_PROCEED.toString(), APPEAL_TO_PROCEED.getLabel()));
        listOptions.add(new DynamicListItem(PROVIDE_INFORMATION.toString(), PROVIDE_INFORMATION.getLabel()));
        listOptions.add(new DynamicListItem(ISSUE_AND_SEND_TO_ADMIN.toString(), ISSUE_AND_SEND_TO_ADMIN.getLabel()));
        listOptions.add(new DynamicListItem(GRANT_REINSTATEMENT.toString(), GRANT_REINSTATEMENT.getLabel()));
        listOptions.add(new DynamicListItem(REFUSE_REINSTATEMENT.toString(), REFUSE_REINSTATEMENT.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(expected, response.getData().getDirectionTypeDl());
        assertEquals(5, response.getData().getDirectionTypeDl().getListItems().size());
    }

    @Test
    void givenAppealWithUrgentHearingEnabledAndUrgentCaseYes_populateDirectionTypeDropdown() {
        callback.getCaseDetails().getCaseData().setUrgentCase("Yes");

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(APPEAL_TO_PROCEED.toString(), APPEAL_TO_PROCEED.getLabel()));
        listOptions.add(new DynamicListItem(PROVIDE_INFORMATION.toString(), PROVIDE_INFORMATION.getLabel()));
        listOptions.add(new DynamicListItem(ISSUE_AND_SEND_TO_ADMIN.toString(), ISSUE_AND_SEND_TO_ADMIN.getLabel()));
        listOptions.add(new DynamicListItem(GRANT_URGENT_HEARING.toString(), GRANT_URGENT_HEARING.getLabel()));
        listOptions.add(new DynamicListItem(REFUSE_URGENT_HEARING.toString(), REFUSE_URGENT_HEARING.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(expected, response.getData().getDirectionTypeDl());
        assertEquals(5, response.getData().getDirectionTypeDl().getListItems().size());
    }

    @Test
    void givenAppealWithUrgentHearingEnabledAndUrgentCaseNo_populateDirectionTypeDropdown() {
        callback.getCaseDetails().getCaseData().setUrgentCase("No");

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(APPEAL_TO_PROCEED.toString(), APPEAL_TO_PROCEED.getLabel()));
        listOptions.add(new DynamicListItem(PROVIDE_INFORMATION.toString(), PROVIDE_INFORMATION.getLabel()));
        listOptions.add(new DynamicListItem(ISSUE_AND_SEND_TO_ADMIN.toString(), ISSUE_AND_SEND_TO_ADMIN.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(expected, response.getData().getDirectionTypeDl());
        assertEquals(3, response.getData().getDirectionTypeDl().getListItems().size());
    }

    @Test
    void givenValidAppealWithTimeExtensionAndDirectionTypeAlreadyPopulated_thenAutoSelectDirectionTypeValue() {
        sscsCaseData = SscsCaseData.builder().timeExtensionRequested("Yes")
                .directionTypeDl(new DynamicList(GRANT_EXTENSION.toString()))
                .appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build())
                .build();
        caseDetails = new CaseDetails<>(1234L, "SSCS", WITH_DWP, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), DIRECTION_ISSUED, false);

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(APPEAL_TO_PROCEED.toString(), APPEAL_TO_PROCEED.getLabel()));
        listOptions.add(new DynamicListItem(PROVIDE_INFORMATION.toString(), PROVIDE_INFORMATION.getLabel()));
        listOptions.add(new DynamicListItem(ISSUE_AND_SEND_TO_ADMIN.toString(), ISSUE_AND_SEND_TO_ADMIN.getLabel()));
        listOptions.add(new DynamicListItem(GRANT_EXTENSION.toString(), GRANT_EXTENSION.getLabel()));
        listOptions.add(new DynamicListItem(REFUSE_EXTENSION.toString(), REFUSE_EXTENSION.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList expected = new DynamicList(
                new DynamicListItem(GRANT_EXTENSION.toString(), GRANT_EXTENSION.toString()), listOptions);
        assertEquals(expected, response.getData().getDirectionTypeDl());
        assertEquals(5, response.getData().getDirectionTypeDl().getListItems().size());
    }

    @Test
    void givenAppealWithNoTimeExtension_populateDirectionTypeDropdown() {
        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(APPEAL_TO_PROCEED.toString(), APPEAL_TO_PROCEED.getLabel()));
        listOptions.add(new DynamicListItem(PROVIDE_INFORMATION.toString(), PROVIDE_INFORMATION.getLabel()));
        listOptions.add(new DynamicListItem(ISSUE_AND_SEND_TO_ADMIN.toString(), ISSUE_AND_SEND_TO_ADMIN.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(expected, response.getData().getDirectionTypeDl());
        assertEquals(3, response.getData().getDirectionTypeDl().getListItems().size());
    }

    @Test
    void givenAppealWithHearingRecordingRequestOutstanding_populateDirectionTypeDropdownWithRefuseHearingRecordingRequest() {
        handler = new DirectionIssuedAboutToStartHandler(false, false, idamService);
        sscsCaseData.getSscsHearingRecordingCaseData().setHearingRecordingRequestOutstanding(YES);

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(APPEAL_TO_PROCEED.toString(), APPEAL_TO_PROCEED.getLabel()));
        listOptions.add(new DynamicListItem(PROVIDE_INFORMATION.toString(), PROVIDE_INFORMATION.getLabel()));
        listOptions.add(new DynamicListItem(ISSUE_AND_SEND_TO_ADMIN.toString(), ISSUE_AND_SEND_TO_ADMIN.getLabel()));
        listOptions.add(new DynamicListItem(
                REFUSE_HEARING_RECORDING_REQUEST.toString(), REFUSE_HEARING_RECORDING_REQUEST.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(4, response.getData().getDirectionTypeDl().getListItems().size());
        assertEquals(expected, response.getData().getDirectionTypeDl());
    }

    @Test
    void givenAppealWithNoHearingRecordingRequestOutstanding_doNotPopulateDirectionTypeDropdownWithRefuseHearingRecordingRequest() {
        handler = new DirectionIssuedAboutToStartHandler(false, false, idamService);
        sscsCaseData.getSscsHearingRecordingCaseData().setHearingRecordingRequestOutstanding(NO);

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(APPEAL_TO_PROCEED.toString(), APPEAL_TO_PROCEED.getLabel()));
        listOptions.add(new DynamicListItem(PROVIDE_INFORMATION.toString(), PROVIDE_INFORMATION.getLabel()));
        listOptions.add(new DynamicListItem(ISSUE_AND_SEND_TO_ADMIN.toString(), ISSUE_AND_SEND_TO_ADMIN.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(3, response.getData().getDirectionTypeDl().getListItems().size());
        assertEquals(expected, response.getData().getDirectionTypeDl());
    }

    @Test
    void givenAValidCallbackType_thenClearTheConfidentialityFields() {
        handler = new DirectionIssuedAboutToStartHandler(false, false, idamService);
        sscsCaseData.setConfidentialityType(ConfidentialityType.CONFIDENTIAL.getCode());
        sscsCaseData.setSendDirectionNoticeToFTA(YES);
        sscsCaseData.setSendDirectionNoticeToRepresentative(YES);
        sscsCaseData.setSendDirectionNoticeToOtherPartyRep(YES);
        sscsCaseData.setSendDirectionNoticeToOtherPartyAppointee(YES);
        sscsCaseData.setSendDirectionNoticeToOtherParty(YES);
        sscsCaseData.setSendDirectionNoticeToJointParty(YES);
        sscsCaseData.setSendDirectionNoticeToAppellantOrAppointee(YES);

        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertNull(sscsCaseData.getConfidentialityType());
        assertNull(sscsCaseData.getSendDirectionNoticeToFTA());
        assertNull(sscsCaseData.getSendDirectionNoticeToRepresentative());
        assertNull(sscsCaseData.getSendDirectionNoticeToOtherPartyRep());
        assertNull(sscsCaseData.getSendDirectionNoticeToOtherPartyAppointee());
        assertNull(sscsCaseData.getSendDirectionNoticeToOtherParty());
        assertNull(sscsCaseData.getSendDirectionNoticeToJointParty());
        assertNull(sscsCaseData.getSendDirectionNoticeToAppellantOrAppointee());
    }

    @Test
    void givenAValidCallbackType_thenVerifyAllPartiesOnTheCase() {
        handler = new DirectionIssuedAboutToStartHandler(false, false, idamService);

        Appointee otherPartyAppointee = Appointee.builder()
                .id("2").name(Name.builder().firstName("Henry").lastName("Smith").build()).build();
        Representative otherPartyRepresentative = Representative.builder()
                .id("3").name(Name.builder().firstName("Wendy").lastName("Smith").build())
                .hasRepresentative(YES.getValue()).build();
        JointParty jointParty = JointParty.builder().hasJointParty(YES).build();
        Representative representative = Representative.builder().hasRepresentative("yes").build();
        sscsCaseData.getAppeal().setRep(representative);
        sscsCaseData.setJointParty(jointParty);
        CcdValue<OtherParty> otherParty = CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                        .id("1").name(Name.builder().firstName("Harry").lastName("Kane").build())
                        .isAppointee(YES.getValue()).appointee(otherPartyAppointee).rep(otherPartyRepresentative)
                        .build())
                .build();
        sscsCaseData.setOtherParties(Collections.singletonList(otherParty));

        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(YES, sscsCaseData.getHasRepresentative());
        assertEquals(YES, sscsCaseData.getHasOtherPartyRep());
        assertEquals(YES, sscsCaseData.getHasOtherPartyAppointee());
        assertEquals(YES, sscsCaseData.getHasOtherParties());
        assertEquals(YES, sscsCaseData.getHasJointParty());
    }

    @Test
    void givenAValidCallbackType_NoAdditionalPartiesForOtherParty() {
        handler = new DirectionIssuedAboutToStartHandler(false, false, idamService);

        CcdValue<OtherParty> otherParty = CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                        .id("1").name(Name.builder().firstName("Harry").lastName("Kane").build())
                        .isAppointee(YES.getValue()).build()).build();
        sscsCaseData.setOtherParties(Collections.singletonList(otherParty));

        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(NO, sscsCaseData.getHasRepresentative());
        assertEquals(NO, sscsCaseData.getHasOtherPartyRep());
        assertEquals(NO, sscsCaseData.getHasOtherPartyAppointee());
        assertEquals(YES, sscsCaseData.getHasOtherParties());
        assertEquals(NO, sscsCaseData.getHasJointParty());
    }

    @Test
    void givenNonDefaultSelectHmcHearingTypeNo_whenValueAboutToStart() {
        sscsCaseData.getExtendedSscsCaseData().setSelectNextHmcHearingType(YES);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(SEND_TO_LISTING.getCode(), SEND_TO_LISTING.getLabel()));
        listOptions.add(new DynamicListItem(NO_FURTHER_ACTION.getCode(), NO_FURTHER_ACTION.getLabel()));
        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(expected, response.getData().getExtensionNextEventDl());
        assertEquals(2, response.getData().getExtensionNextEventDl().getListItems().size());
        assertEquals(NO, response.getData().getExtendedSscsCaseData().getSelectNextHmcHearingType());
        assertNull(response.getData().getHmcHearingType());
    }

    @Test
    void givenNullSelectHmcHearingTypeNo_whenNullAboutToStart() {
        sscsCaseData.getExtendedSscsCaseData().setSelectNextHmcHearingType(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(SEND_TO_LISTING.getCode(), SEND_TO_LISTING.getLabel()));
        listOptions.add(new DynamicListItem(NO_FURTHER_ACTION.getCode(), NO_FURTHER_ACTION.getLabel()));
        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(expected, response.getData().getExtensionNextEventDl());
        assertEquals(2, response.getData().getExtensionNextEventDl().getListItems().size());
        assertEquals(NO, response.getData().getExtendedSscsCaseData().getSelectNextHmcHearingType());
        assertNull(response.getData().getHmcHearingType());
    }

    @Test
    void givenConfidentialityFlagAndNullUserDetails_doNotPopulateConfidentialityDirections() {
        handler = new DirectionIssuedAboutToStartHandler(false, true, idamService);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.CONFIDENTIALITY);
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDirectionTypeDl().getListItems()).hasSize(3);
        assertThat(response.getData().getDirectionTypeDl().getListItems())
                .noneMatch(item -> item.getCode().equals(CONFIDENTIALITY_GRANTED_SEND_TO_ADMIN.toString()));
    }

    @ParameterizedTest
    @CsvSource({
        "SUPER_USER",
        "TCW",
        "JUDGE"
    })
    void givenConfidentialityFlagAndAuthorisedRole_populateConfidentialityDirections(String roleName) {
        handler = new DirectionIssuedAboutToStartHandler(false, true, idamService);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.CONFIDENTIALITY);
        when(idamService.getUserDetails(anyString())).thenReturn(userDetails);

        String roleValue = switch (roleName) {
            case "SUPER_USER" -> SUPER_USER.getValue();
            case "TCW" -> TCW.getValue();
            case "JUDGE" -> JUDGE.getValue();
            default -> throw new IllegalArgumentException("Unexpected role: " + roleName);
        };

        when(userDetails.getRoles()).thenReturn(List.of(roleValue));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDirectionTypeDl().getListItems()).hasSize(5);
        assertThat(response.getData().getDirectionTypeDl().getListItems())
                .anyMatch(item -> item.getCode().equals(CONFIDENTIALITY_GRANTED_SEND_TO_ADMIN.toString()));
        assertThat(response.getData().getDirectionTypeDl().getListItems())
                .anyMatch(item -> item.getCode().equals(CONFIDENTIALITY_REFUSED_SEND_TO_ADMIN.toString()));
    }

    @Test
    void givenConfidentialityFlagAndNoQualifyingRole_doNotPopulateConfidentialityDirections() {
        handler = new DirectionIssuedAboutToStartHandler(false, true, idamService);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.CONFIDENTIALITY);
        when(idamService.getUserDetails(anyString())).thenReturn(userDetails);
        when(userDetails.getRoles()).thenReturn(List.of("caseworker"));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDirectionTypeDl().getListItems()).hasSize(3);
        assertThat(response.getData().getDirectionTypeDl().getListItems())
                .noneMatch(item -> item.getCode().equals(CONFIDENTIALITY_GRANTED_SEND_TO_ADMIN.toString()));
    }

    @Test
    void givenMidEvent_ThenDoesNotWipeHmcHearingTypeOrSelect() {
        sscsCaseData.getExtendedSscsCaseData().setSelectNextHmcHearingType(YES);
        sscsCaseData.setHmcHearingType(HmcHearingType.DIRECTION_HEARINGS);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(SEND_TO_LISTING.getCode(), SEND_TO_LISTING.getLabel()));
        listOptions.add(new DynamicListItem(NO_FURTHER_ACTION.getCode(), NO_FURTHER_ACTION.getLabel()));
        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(expected, response.getData().getExtensionNextEventDl());
        assertEquals(2, response.getData().getExtensionNextEventDl().getListItems().size());
        assertEquals(YES, response.getData().getExtendedSscsCaseData().getSelectNextHmcHearingType());
        assertEquals(HmcHearingType.DIRECTION_HEARINGS, response.getData().getHmcHearingType());
    }
}
