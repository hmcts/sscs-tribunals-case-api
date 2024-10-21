package uk.gov.hmcts.reform.sscs.ccd.presubmit.validappeal;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.springframework.util.CollectionUtils.isEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.callback.ValidationType.SYA_APPEAL;
import static uk.gov.hmcts.reform.sscs.ccd.validation.address.AddressValidator.IS_NOT_A_VALID_POSTCODE;
import static uk.gov.hmcts.reform.sscs.ccd.validation.appeal.PartyValidator.ninoExists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLinkDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.validation.address.AddressValidator;
import uk.gov.hmcts.reform.sscs.ccd.validation.appeal.AppealValidator;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;

@Component
@Slf4j
public class SyaAppealValidator extends AppealValidator {
    private static final String LOGSTR_VALIDATION_ERRORS = "Errors found while validating exception record id {} - {}";
    private static final String LOGSTR_VALIDATION_WARNING = "Warnings found while validating exception record id {} - {}";

    private final IdamService idamService;
    private final CcdService ccdService;

    public SyaAppealValidator(IdamService idamService,
                              CcdService ccdService,
                              DwpAddressLookupService dwpAddressLookupService,
                              AddressValidator addressValidator,
                              @Value("#{'${validation.titles}'.split(',')}") List<String> titles) {
        super(dwpAddressLookupService, addressValidator, SYA_APPEAL, titles);
        this.idamService = idamService;
        this.ccdService = ccdService;
    }

    public PreSubmitCallbackResponse<SscsCaseData> validateAppeal(CaseDetails<SscsCaseData> caseDetails, Map<String, Object> appealData, boolean ignoreMrnValidation) {

        Map<String, List<String>> errsWarns =
                validateAppeal(new HashMap<>(), appealData, ignoreMrnValidation, false, false);

        CaseResponse caseValidationResponse = CaseResponse.builder()
                .errors(errsWarns.get("errors"))
                .warnings(errsWarns.get("warnings"))
                .transformedCase(appealData)
                .build();

        PreSubmitCallbackResponse<SscsCaseData> validationErrorResponse = convertWarningsToErrors(caseDetails.getCaseData(), caseValidationResponse);

        if (validationErrorResponse != null) {
            log.info(LOGSTR_VALIDATION_ERRORS, caseDetails.getId(), ".");
            return validationErrorResponse;
        } else {
            log.info("Appeal {} validated successfully", caseDetails.getId());

            PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseDetails.getCaseData());

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
        String nino = ninoExists(appeal) ? appeal.getAppellant().getIdentity().getNino() : "";

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
