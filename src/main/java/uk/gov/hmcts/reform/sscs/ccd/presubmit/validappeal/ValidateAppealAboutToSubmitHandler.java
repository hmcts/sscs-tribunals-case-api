package uk.gov.hmcts.reform.sscs.ccd.presubmit.validappeal;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.tika.utils.StringUtils.EMPTY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;
import static uk.gov.hmcts.reform.sscs.service.CaseCodeService.generateBenefitCode;
import static uk.gov.hmcts.reform.sscs.service.CaseCodeService.generateCaseCode;
import static uk.gov.hmcts.reform.sscs.service.CaseCodeService.generateIssueCode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseManagementLocation;
import uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.FormType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsType;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.ResponseEventsAboutToSubmit;
import uk.gov.hmcts.reform.sscs.ccd.validation.helper.AppealPostcodeHelper;
import uk.gov.hmcts.reform.sscs.ccd.validation.helper.SscsDataHelper;
import uk.gov.hmcts.reform.sscs.service.CaseManagementLocationService;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;

@Component
@Slf4j
public class ValidateAppealAboutToSubmitHandler extends ResponseEventsAboutToSubmit implements PreSubmitCallbackHandler<SscsCaseData> {

    private final SyaAppealValidator appealValidator;
    private final SscsDataHelper sscsDataHelper;
    private final DwpAddressLookupService dwpAddressLookupService;
    private final AppealPostcodeHelper appealPostcodeHelper;
    private final CaseManagementLocationService caseManagementLocationService;

    public ValidateAppealAboutToSubmitHandler(SyaAppealValidator appealValidator,
                                              AppealPostcodeHelper appealPostcodeHelper,
                                              SscsDataHelper sscsDataHelper,
                                              DwpAddressLookupService dwpAddressLookupService,
                                              CaseManagementLocationService caseManagementLocationService) {
        this.appealPostcodeHelper = appealPostcodeHelper;
        this.appealValidator = appealValidator;
        this.sscsDataHelper = sscsDataHelper;
        this.dwpAddressLookupService = dwpAddressLookupService;
        this.caseManagementLocationService = caseManagementLocationService;

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

        return appealValidator.validateAppeal(callback.getCaseDetails(), appealData, ignoreMrnValidation);
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
        Optional<Benefit> benefit = Benefit.getBenefitOptionalByCode(appeal.getBenefitType().getCode());
        benefit.ifPresent(
                value -> callback.getCaseDetails().getCaseData().getCaseAccessManagementFields()
                        .setCategories(value));
    }

    private void setCaseAccessManagementFields(Appeal appeal, Callback<SscsCaseData> callback) {
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
}
