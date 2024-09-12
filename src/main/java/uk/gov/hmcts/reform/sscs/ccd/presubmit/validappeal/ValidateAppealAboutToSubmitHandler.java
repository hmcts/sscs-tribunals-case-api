package uk.gov.hmcts.reform.sscs.ccd.presubmit.validappeal;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.*;

import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import static uk.gov.hmcts.reform.sscs.ccd.validation.sscscasedata.AppealValidator.IS_NOT_A_VALID_POSTCODE;
import static uk.gov.hmcts.reform.sscs.service.CaseCodeService.*;

import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;

import uk.gov.hmcts.reform.sscs.ccd.validation.sscscasedata.AppealPostcodeHelper;
import uk.gov.hmcts.reform.sscs.ccd.validation.sscscasedata.AppealValidator;
import uk.gov.hmcts.reform.sscs.exception.CaseManagementLocationService;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.ResponseEventsAboutToSubmit;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;

import uk.gov.hmcts.reform.sscs.helper.SscsDataHelper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ValidateAppealAboutToSubmitHandler extends ResponseEventsAboutToSubmit implements PreSubmitCallbackHandler<SscsCaseData> {

    private final AppealValidator appealValidator;
    private final SscsDataHelper sscsDataHelper;
    private final IdamService idamService;
    private final CcdService ccdService;
    private final DwpAddressLookupService dwpAddressLookupService;
    private final AppealPostcodeHelper appealPostcodeHelper;
    private final CaseManagementLocationService caseManagementLocationService;
    private final boolean caseAccessManagementFeature;
    private static final String LOGSTR_VALIDATION_ERRORS = "Errors found while validating exception record id {} - {}";
    private static final String LOGSTR_VALIDATION_WARNING = "Warnings found while validating exception record id {} - {}";

    public ValidateAppealAboutToSubmitHandler(CcdService ccdService,
                                              AppealValidator appealValidator,
                                              AppealPostcodeHelper appealPostcodeHelper,
                                              SscsDataHelper sscsDataHelper,
                                              IdamService idamService,
                                              DwpAddressLookupService dwpAddressLookupService,
                                              CaseManagementLocationService caseManagementLocationService,
                                              @Value("${feature.case-access-management.enabled}") boolean caseAccessManagementFeature) { ////check if feature toggle exists, if does remove.
        this.ccdService = ccdService;
        this.appealPostcodeHelper = appealPostcodeHelper;
        this.appealValidator = appealValidator;
        this.sscsDataHelper = sscsDataHelper;
        this.idamService = idamService;
        this.dwpAddressLookupService = dwpAddressLookupService;
        this.caseManagementLocationService = caseManagementLocationService;
        this.caseAccessManagementFeature = caseAccessManagementFeature;

    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.VALID_APPEAL
                && nonNull(callback.getCaseDetails())
                && nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        //Set digital flag on legacy cases
        if ((sscsCaseData.getCreatedInGapsFrom() == null
                || VALID_APPEAL.getId().equalsIgnoreCase(sscsCaseData.getCreatedInGapsFrom()))) {
            sscsCaseData.setCreatedInGapsFrom(READY_TO_LIST.getId());
        }

        log.info("Processing validation and update request for SSCS exception record id {}", callback.getCaseDetails().getId());

        if (null != callback.getCaseDetails().getCaseData().getInterlocReviewState()) {
            callback.getCaseDetails().getCaseData().setInterlocReviewState(InterlocReviewState.NONE);
        }

        setUnsavedFieldsOnCallback(callback);

        FormType formType = callback.getCaseDetails().getCaseData().getFormType();
        Map<String, Object> appealData = new HashMap<>();
        sscsDataHelper.addSscsDataToMap(appealData,
                callback.getCaseDetails().getCaseData().getAppeal(),
                callback.getCaseDetails().getCaseData().getSscsDocument(),
                callback.getCaseDetails().getCaseData().getSubscriptions(),
                formType,
                callback.getCaseDetails().getCaseData().getChildMaintenanceNumber(),
                callback.getCaseDetails().getCaseData().getOtherParties()
        );

        boolean ignoreMrnValidation = false;
        if (callback.getEvent() != null && (EventType.DIRECTION_ISSUED.equals(callback.getEvent())
                || EventType.DIRECTION_ISSUED_WELSH.equals(callback.getEvent()))
                && callback.getCaseDetails().getCaseData().getDirectionTypeDl() != null) {
            ignoreMrnValidation = StringUtils.equals(DirectionType.APPEAL_TO_PROCEED.toString(),
                    callback.getCaseDetails().getCaseData().getDirectionTypeDl().getValue().getCode());
        }

        //only keep errors and warning for caseResponse
        CaseResponse caseValidationResponse = validateValidationRecord(appealData, ignoreMrnValidation);

        PreSubmitCallbackResponse<SscsCaseData> validationErrorResponse = convertWarningsToErrors(callback.getCaseDetails().getCaseData(), caseValidationResponse); //convertWarn.. can be in tribs.

        if (validationErrorResponse != null) {
            log.info(LOGSTR_VALIDATION_ERRORS, callback.getCaseDetails().getId(), ".");
            return validationErrorResponse;
        } else {
            log.info("Appeal {} validated successfully", callback.getCaseDetails().getId());

            PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(callback.getCaseDetails().getCaseData());

            if (caseValidationResponse.getWarnings() != null) {
                preSubmitCallbackResponse.addWarnings(caseValidationResponse.getWarnings());
            }

            IdamTokens tokens = idamService.getIdamTokens();

            checkForMatches(caseValidationResponse.getTransformedCase(), tokens);

            return preSubmitCallbackResponse;
        }
    }

    public Map<String, Object> checkForMatches(Map<String, Object> sscsCaseData, IdamTokens token) {
        Appeal appeal = (Appeal) sscsCaseData.get("appeal");
        String nino = "";
        if (appeal != null && appeal.getAppellant() != null
                && appeal.getAppellant().getIdentity() != null && appeal.getAppellant().getIdentity().getNino() != null) {
            nino = appeal.getAppellant().getIdentity().getNino();
        }

        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();

        if (!StringUtils.isEmpty(nino)) {
            matchedByNinoCases = ccdService.findCaseBy("data.appeal.appellant.identity.nino", nino, token);
        }

        sscsCaseData = addAssociatedCases(sscsCaseData, matchedByNinoCases);
        return sscsCaseData;
    }

    private Map<String, Object> addAssociatedCases(Map<String, Object> sscsCaseData,
                                                   List<SscsCaseDetails> matchedByNinoCases) {
        List<CaseLink> associatedCases = new ArrayList<>();

        for (SscsCaseDetails sscsCaseDetails : matchedByNinoCases) {
            CaseLink caseLink = CaseLink.builder().value(
                    CaseLinkDetails.builder().caseReference(sscsCaseDetails.getId().toString()).build()).build();
            associatedCases.add(caseLink);

            String caseId = null != sscsCaseDetails.getId() ? sscsCaseDetails.getId().toString() : "N/A";
            log.info("Added associated case {}" + caseId);
        }
        if (associatedCases.size() > 0) {
            sscsCaseData.put("associatedCase", associatedCases);
            sscsCaseData.put("linkedCasesBoolean", "Yes");
        } else {
            sscsCaseData.put("linkedCasesBoolean", "No");
        }

        return sscsCaseData;
    }

    public CaseResponse validateValidationRecord(Map<String, Object> caseData, boolean ignoreMrnValidation) {
        Map<String, List<String>> errsWarns =
                appealValidator.validateAppeal(new HashMap<>(), caseData, ignoreMrnValidation, false, false);

        return CaseResponse.builder()
                .errors(errsWarns.get("errors"))
                .warnings(errsWarns.get("warnings"))
                .transformedCase(caseData)
                .build();
    }

    private void setUnsavedFieldsOnCallback(Callback<SscsCaseData> callback) {
        Appeal appeal = callback.getCaseDetails().getCaseData().getAppeal();
        callback.getCaseDetails().getCaseData().setCreatedInGapsFrom(READY_TO_LIST.getId());
        callback.getCaseDetails().getCaseData().setEvidencePresent(sscsDataHelper.hasEvidence(callback.getCaseDetails().getCaseData().getSscsDocument()));

        if (appeal != null && callback.getCaseDetails().getCaseData().getAppeal().getBenefitType() != null
                && isNotBlank(callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().getCode())) {
            String addressName = null;
            if (appeal.getMrnDetails() != null) {
                addressName = appeal.getMrnDetails().getDwpIssuingOffice();
            }
            String benefitCode = generateBenefitCode(callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().getCode(), addressName).orElse(EMPTY);

            String issueCode = generateIssueCode();

            callback.getCaseDetails().getCaseData().setBenefitCode(benefitCode);
            callback.getCaseDetails().getCaseData().setIssueCode(issueCode);
            callback.getCaseDetails().getCaseData().setCaseCode(generateCaseCode(benefitCode, issueCode));

            if (callback.getCaseDetails().getCaseData().getAppeal().getMrnDetails() != null
                    && callback.getCaseDetails().getCaseData().getAppeal().getMrnDetails().getDwpIssuingOffice() != null) {

                String dwpRegionCentre = dwpAddressLookupService.getDwpRegionalCenterByBenefitTypeAndOffice(
                        appeal.getBenefitType().getCode(),
                        appeal.getMrnDetails().getDwpIssuingOffice());

                callback.getCaseDetails().getCaseData().setDwpRegionalCentre(dwpRegionCentre);
            }

            String postcode = appealPostcodeHelper.resolvePostcode(appeal.getAppellant());
            String processingVenue = sscsDataHelper.findProcessingVenue(postcode, appeal.getBenefitType());

            if (isNotBlank(processingVenue)) {
                callback.getCaseDetails().getCaseData().setProcessingVenue(processingVenue);
                Optional<CaseManagementLocation> caseManagementLocationOptional = caseManagementLocationService
                        .retrieveCaseManagementLocation(processingVenue, callback.getCaseDetails().getCaseData().getRegionalProcessingCenter());

                caseManagementLocationOptional.ifPresent(caseManagementLocation ->
                        callback.getCaseDetails().getCaseData()
                                .setCaseManagementLocation(caseManagementLocation));
            }
            setCaseAccessManagementCategories(appeal, callback);
        } else {
            setUnknownCategory(callback);
        }

        setCaseAccessManagementFields(appeal, callback);
    }

    private void setCaseAccessManagementCategories(Appeal appeal, Callback<SscsCaseData> callback) {
        if (caseAccessManagementFeature) {
            Optional<Benefit> benefit = Benefit.getBenefitOptionalByCode(appeal.getBenefitType().getCode());
            benefit.ifPresent(
                    value -> callback.getCaseDetails().getCaseData().getCaseAccessManagementFields()
                            .setCategories(value));
        }
    }

    private void setCaseAccessManagementFields(Appeal appeal, Callback<SscsCaseData> callback) {
        if (caseAccessManagementFeature) {
            if (appeal != null && appeal.getAppellant() != null && appeal.getAppellant().getName() != null
                    && appeal.getAppellant().getName().getFirstName() != null && appeal.getAppellant().getName().getLastName() != null) {
                callback.getCaseDetails().getCaseData().getCaseAccessManagementFields().setCaseNames(appeal.getAppellant().getName().getFullNameNoTitle());
            }
            if (appeal != null && appeal.getBenefitType() != null) {
                FormType formType = callback.getCaseDetails().getCaseData().getFormType();
                Optional<Benefit> benefit = Benefit.getBenefitOptionalByCode(appeal.getBenefitType().getCode());
                String ogdType = isHmrcBenefit(benefit, formType) ? "HMRC" : "DWP";
                callback.getCaseDetails().getCaseData().getCaseAccessManagementFields().setOgdType(ogdType);
            }
        }
    }

    private boolean isHmrcBenefit(Optional<Benefit> benefit, FormType formType) {
        if (benefit.isEmpty()) {
            return FormType.SSCS5.equals(formType);
        }
        return SscsType.SSCS5.equals(benefit.get().getSscsType());
    }

    private void setUnknownCategory(Callback<SscsCaseData> callback) {
        FormType formType = callback.getCaseDetails().getCaseData().getFormType();
        if (formType != null) {
            DynamicListItem caseManagementCategoryItem = formType.equals(FormType.SSCS5)
                    ? new DynamicListItem("sscs5Unknown", "SSCS5 Unknown")
                    : new DynamicListItem("sscs12Unknown", "SSCS1/2 Unknown");
            callback.getCaseDetails().getCaseData().getCaseAccessManagementFields()
                    .setCaseManagementCategory(new DynamicList(
                            caseManagementCategoryItem,
                            List.of(caseManagementCategoryItem)));
        }
    }
    private PreSubmitCallbackResponse<SscsCaseData> convertWarningsToErrors(SscsCaseData caseData, CaseResponse caseResponse) {

        List<String> appendedWarningsAndErrors = new ArrayList<>();

        List<String> allWarnings = caseResponse.getWarnings();
        List<String> warningsThatAreNotErrors = getWarningsThatShouldNotBeErrors(caseResponse);
        List<String> filteredWarnings = emptyIfNull(allWarnings).stream()
                .filter(w -> !warningsThatAreNotErrors.contains(w))
                .collect(Collectors.toList());

        if (!isEmpty(filteredWarnings)) {
            log.info(LOGSTR_VALIDATION_WARNING, caseData.getCcdCaseId(), stringJoin(filteredWarnings));
            appendedWarningsAndErrors.addAll(filteredWarnings);
        }

        if (!isEmpty(caseResponse.getErrors())) {
            log.info(LOGSTR_VALIDATION_ERRORS, caseData.getCcdCaseId(), stringJoin(caseResponse.getErrors()));
            appendedWarningsAndErrors.addAll(caseResponse.getErrors());
        }

        if (!appendedWarningsAndErrors.isEmpty() || !warningsThatAreNotErrors.isEmpty()) {
            PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);

            preSubmitCallbackResponse.addErrors(appendedWarningsAndErrors);
            preSubmitCallbackResponse.addWarnings(warningsThatAreNotErrors);
            return preSubmitCallbackResponse;
        }
        return null;
    }

    private String stringJoin(List<String> messages) {
        return String.join(". ", messages);
    }

    private List<String> getWarningsThatShouldNotBeErrors(CaseResponse caseResponse) {
        return emptyIfNull(caseResponse.getWarnings()).stream()
                .filter(warning -> warning.endsWith(IS_NOT_A_VALID_POSTCODE))
                .collect(Collectors.toList());
    }

}
