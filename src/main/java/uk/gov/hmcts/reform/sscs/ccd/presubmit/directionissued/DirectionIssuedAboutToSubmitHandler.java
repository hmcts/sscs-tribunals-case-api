package uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.AWAITING_ADMIN_ACTION;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.AWAITING_INFORMATION;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.NONE;
import static uk.gov.hmcts.reform.sscs.helper.SscsHelper.getPreValidStates;
import static uk.gov.hmcts.reform.sscs.util.DocumentUtil.isFileAPdf;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.model.dwp.OfficeMapping;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.ServiceRequestExecutor;
import uk.gov.hmcts.reform.sscs.util.DateTimeUtils;

@Service
@Slf4j
public class DirectionIssuedAboutToSubmitHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final FooterService footerService;
    private final ServiceRequestExecutor serviceRequestExecutor;
    private final String bulkScanEndpoint;
    private final DwpAddressLookupService dwpAddressLookupService;
    private final int dwpResponseDueDays;

    @Autowired
    public DirectionIssuedAboutToSubmitHandler(FooterService footerService, ServiceRequestExecutor serviceRequestExecutor,
                                               @Value("${bulk_scan.url}") String bulkScanUrl,
                                               @Value("${bulk_scan.validateEndpoint}") String validateEndpoint,
                                               DwpAddressLookupService dwpAddressLookupService,
                                               @Value("${dwp.response.due.days}") int dwpResponseDueDays) {
        this.footerService = footerService;
        this.serviceRequestExecutor = serviceRequestExecutor;
        this.bulkScanEndpoint = String.format("%s%s", trimToEmpty(bulkScanUrl), trimToEmpty(validateEndpoint));
        this.dwpAddressLookupService = dwpAddressLookupService;
        this.dwpResponseDueDays = dwpResponseDueDays;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
                && (callback.getEvent() == EventType.DIRECTION_ISSUED
                || callback.getEvent() == EventType.DIRECTION_ISSUED_WELSH)
                && nonNull(callback.getCaseDetails())
                && nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData caseData = caseDetails.getCaseData();

        SscsDocumentTranslationStatus documentTranslationStatus = caseData.isLanguagePreferenceWelsh() && callback.getEvent() == EventType.DIRECTION_ISSUED ? SscsDocumentTranslationStatus.TRANSLATION_REQUIRED : null;
        log.info("DocumentTranslationStatus is {},  for case id : {}", documentTranslationStatus, caseData.getCcdCaseId());

        return validateDirectionType(caseData)
                .or(()        -> validateDirectionDueDate(caseData))
                .orElseGet(() -> validateForPdfAndCreateCallbackResponse(callback, caseDetails, caseData, documentTranslationStatus));
    }

    private void updateDwpRegionalCentre(SscsCaseData caseData) {
        Appeal appeal = caseData.getAppeal();

        if (appeal != null && appeal.getBenefitType() != null && (appeal.getMrnDetails() == null || appeal.getMrnDetails().getDwpIssuingOffice() == null || appeal.getMrnDetails().getDwpIssuingOffice().isEmpty())) {
            Optional<OfficeMapping> defaultOfficeMapping = dwpAddressLookupService.getDefaultDwpMappingByBenefitType(appeal.getBenefitType().getCode());
            if (defaultOfficeMapping.isPresent()) {
                String defaultDwpIssuingOffice = defaultOfficeMapping.get().getMapping().getCcd();
                // set default dwp office and regional centre
                if (appeal.getMrnDetails() == null) {
                    caseData.getAppeal().setMrnDetails(MrnDetails.builder().dwpIssuingOffice(defaultDwpIssuingOffice).build());
                } else {
                    caseData.getAppeal().getMrnDetails().setDwpIssuingOffice(defaultDwpIssuingOffice);
                }
                log.info("Update Case {} default DWP Issuing Office {}", caseData.getCcdCaseId(), defaultDwpIssuingOffice);
            }
        }
        if (appeal != null && appeal.getBenefitType() != null && appeal.getMrnDetails() != null && appeal.getMrnDetails().getDwpIssuingOffice() != null) {

            caseData.setDwpRegionalCentre(dwpAddressLookupService.getDwpRegionalCenterByBenefitTypeAndOffice(appeal.getBenefitType().getCode(),
                    appeal.getMrnDetails().getDwpIssuingOffice()));

        }
    }

    private Optional<PreSubmitCallbackResponse<SscsCaseData>> validateDirectionType(SscsCaseData caseData) {
        if (caseData.getDirectionTypeDl() == null || caseData.getDirectionTypeDl().getValue() == null) {
            PreSubmitCallbackResponse<SscsCaseData> errorResponse = new PreSubmitCallbackResponse<>(caseData);
            errorResponse.addError("Direction Type cannot be empty");
            return Optional.of(errorResponse);
        }
        return Optional.empty();
    }

    private Optional<PreSubmitCallbackResponse<SscsCaseData>> validateDirectionDueDate(SscsCaseData caseData) {
        if (DirectionType.PROVIDE_INFORMATION.toString().equals(caseData.getDirectionTypeDl().getValue().getCode())
                && isBlank(caseData.getDirectionDueDate())) {
            PreSubmitCallbackResponse<SscsCaseData> errorResponse = new PreSubmitCallbackResponse<>(caseData);
            errorResponse.addError("Please populate the direction due date");
            return Optional.of(errorResponse);
        }
        return Optional.empty();
    }

    private SscsCaseData updateCaseAfterExtensionRefused(SscsCaseData caseData, String interlocReviewState, State state) {
        caseData.setHmctsDwpState("sentToDwp");
        caseData.setDateSentToDwp(LocalDate.now().toString());
        caseData.setDwpDueDate(DateTimeUtils.generateDwpResponseDueDate(dwpResponseDueDays));
        caseData.setInterlocReviewState(interlocReviewState);
        caseData.setState(state);

        return caseData;
    }

    @NotNull
    private SscsCaseData updateCaseForDirectionType(CaseDetails<SscsCaseData> caseDetails, SscsCaseData caseData, SscsDocumentTranslationStatus documentTranslationStatus) {

        if (DirectionType.PROVIDE_INFORMATION.toString().equals(caseData.getDirectionTypeDl().getValue().getCode())) {

            caseData.setInterlocReviewState(AWAITING_INFORMATION.getId());

        } else if (getPreValidStates().contains(caseDetails.getState())
                && DirectionType.APPEAL_TO_PROCEED.toString().equals(caseData.getDirectionTypeDl().getValue().getCode())) {
            caseData.setDateSentToDwp(LocalDate.now().toString());
            caseData.setDwpDueDate(DateTimeUtils.generateDwpResponseDueDate(dwpResponseDueDays));
            caseData.setInterlocReviewState(AWAITING_ADMIN_ACTION.getId());
            updateDwpRegionalCentre(caseData);

            //Set digital flag on legacy cases
            if (caseData.getCreatedInGapsFrom() == null || VALID_APPEAL.getId().equalsIgnoreCase(caseData.getCreatedInGapsFrom())) {
                caseData.setCreatedInGapsFrom(READY_TO_LIST.getId());
            }
        } else if (DirectionType.REFUSE_EXTENSION.toString().equals(caseData.getDirectionTypeDl().getValue().getCode())
                && ExtensionNextEvent.SEND_TO_LISTING.toString().equals(caseData.getExtensionNextEventDl().getValue().getCode())) {
            caseData = updateCaseAfterExtensionRefused(caseData, AWAITING_ADMIN_ACTION.getId(), State.RESPONSE_RECEIVED);

        } else if (DirectionType.REFUSE_EXTENSION.toString().equals(caseData.getDirectionTypeDl().getValue().getCode())
                && ExtensionNextEvent.SEND_TO_VALID_APPEAL.toString().equals(caseData.getExtensionNextEventDl().getValue().getCode())) {
            caseData = updateCaseAfterExtensionRefused(caseData, null, State.WITH_DWP);

        } else if (DirectionTypeItemList.GRANT_REINSTATEMENT.getCode().equals(caseData.getDirectionTypeDl().getValue().getCode())) {
            caseData = updateCaseAfterReinstatementGranted(caseData, documentTranslationStatus);

        } else if (DirectionTypeItemList.REFUSE_REINSTATEMENT.getCode().equals(caseData.getDirectionTypeDl().getValue().getCode())) {
            caseData = updateCaseAfterReinstatementRefused(caseData, documentTranslationStatus);

        } else if (!SscsDocumentTranslationStatus.TRANSLATION_REQUIRED.equals(documentTranslationStatus)
            && DirectionTypeItemList.GRANT_URGENT_HEARING.getCode().equals(caseData.getDirectionTypeDl().getValue().getCode())) {
            caseData = updateCaseAfterUrgentHearingGranted(caseData);

        } else if (!SscsDocumentTranslationStatus.TRANSLATION_REQUIRED.equals(documentTranslationStatus)
            && DirectionTypeItemList.REFUSE_URGENT_HEARING.getCode().equals(caseData.getDirectionTypeDl().getValue().getCode())) {
            caseData = updateCaseAfterUrgentHearingRefused(caseData);

        } else {
            caseData.setInterlocReviewState(null);
        }
        return caseData;
    }

    private SscsCaseData updateCaseAfterReinstatementGranted(SscsCaseData caseData, SscsDocumentTranslationStatus documentTranslationStatus) {

        if (!SscsDocumentTranslationStatus.TRANSLATION_REQUIRED.equals(documentTranslationStatus)) {
            caseData.setReinstatementOutcome(RequestOutcome.GRANTED);
            caseData.setDwpState(DwpState.REINSTATEMENT_GRANTED.getId());

            updateStateIfInterLockReviewState(caseData);

            log.info("Case ID {} reinstatement granted on {}", caseData.getCcdCaseId(), LocalDate.now().toString());

        } else {
            log.info("Case ID {} reinstatement granted held pending Direction Translation {}", caseData.getCcdCaseId(), LocalDate.now().toString());
        }
        return caseData;
    }



    private SscsCaseData updateCaseAfterReinstatementRefused(SscsCaseData caseData, SscsDocumentTranslationStatus documentTranslationStatus) {

        if (!SscsDocumentTranslationStatus.TRANSLATION_REQUIRED.equals(documentTranslationStatus)) {
            caseData.setReinstatementOutcome(RequestOutcome.REFUSED);
            caseData.setDwpState(DwpState.REINSTATEMENT_REFUSED.getId());
            log.info("Case ID {} reinstatement refused on {}", caseData.getCcdCaseId(), LocalDate.now().toString());
        } else {
            log.info("Case ID {} reinstatement refused held pending Direction Translation {}", caseData.getCcdCaseId(), LocalDate.now().toString());
        }
        return caseData;
    }

    private SscsCaseData updateCaseAfterUrgentHearingGranted(SscsCaseData caseData) {

        caseData.setUrgentHearingOutcome(RequestOutcome.GRANTED.getValue());
        caseData.setInterlocReviewState(AWAITING_ADMIN_ACTION.getId());
        log.info("Case ID {} urgent hearing granted on {}", caseData.getCcdCaseId(), LocalDate.now().toString());
        return caseData;
    }

    private SscsCaseData updateCaseAfterUrgentHearingRefused(SscsCaseData caseData) {

        caseData.setUrgentHearingOutcome(RequestOutcome.REFUSED.getValue());
        caseData.setUrgentCase("No");
        caseData.setInterlocReviewState(NONE.getId());
        log.info("Case ID {} urgent hearing refused on {}", caseData.getCcdCaseId(), LocalDate.now().toString());
        return caseData;
    }

    private void updateStateIfInterLockReviewState(SscsCaseData caseData) {
        State previousState = caseData.getPreviousState();

        if (previousState != null && !State.INTERLOCUTORY_REVIEW_STATE.getId().equals(previousState.getId())) {
            caseData.setState(previousState);
        } else {
            caseData.setState(State.INTERLOCUTORY_REVIEW_STATE);
            caseData.setInterlocReviewState(AWAITING_ADMIN_ACTION.getId());
        }
    }

    @NotNull
    private PreSubmitCallbackResponse<SscsCaseData> validateForPdfAndCreateCallbackResponse(
            Callback<SscsCaseData> callback, CaseDetails<SscsCaseData> caseDetails, SscsCaseData caseData, SscsDocumentTranslationStatus documentTranslationStatus) {

        final PreSubmitCallbackResponse<SscsCaseData> sscsCaseDataPreSubmitCallbackResponse =
                new PreSubmitCallbackResponse<>(caseData);

        DocumentLink url = null;

        if (nonNull(caseData.getPreviewDocument()) && callback.getEvent() == EventType.DIRECTION_ISSUED) {
            url = caseData.getPreviewDocument();
        } else if (caseData.getSscsInterlocDirectionDocument() != null && callback.getEvent() == EventType.DIRECTION_ISSUED) {

            url = caseData.getSscsInterlocDirectionDocument().getDocumentLink();
            caseData.setDateAdded(caseData.getSscsInterlocDirectionDocument().getDocumentDateAdded());

            if (!isFileAPdf(caseData.getSscsInterlocDirectionDocument().getDocumentLink())) {
                sscsCaseDataPreSubmitCallbackResponse.addError("You need to upload PDF documents only");
                return sscsCaseDataPreSubmitCallbackResponse;
            }
        }

        if (isNull(url) && callback.getEvent() != EventType.DIRECTION_ISSUED_WELSH) {
            sscsCaseDataPreSubmitCallbackResponse.addError("You need to upload a PDF document");
            return sscsCaseDataPreSubmitCallbackResponse;
        }

        if (!SscsDocumentTranslationStatus.TRANSLATION_REQUIRED.equals(documentTranslationStatus)) {

            if (DirectionType.PROVIDE_INFORMATION.toString().equals(caseData.getDirectionTypeDl().getValue().getCode())) {
                caseData.setInterlocReviewState(AWAITING_INFORMATION.getId());

            } else if (getPreValidStates().contains(caseDetails.getState()) && DirectionType.APPEAL_TO_PROCEED.toString().equals(caseData.getDirectionTypeDl().getValue().getCode())) {
                caseData.setDateSentToDwp(LocalDate.now().toString());
                caseData.setDwpDueDate(DateTimeUtils.generateDwpResponseDueDate(dwpResponseDueDays));
                caseData.setInterlocReviewState(AWAITING_ADMIN_ACTION.getId());

            } else if (DirectionType.REFUSE_EXTENSION.toString().equals(caseData.getDirectionTypeDl().getValue().getCode())
                    && ExtensionNextEvent.SEND_TO_LISTING.toString().equals(caseData.getExtensionNextEventDl().getValue().getCode())) {
                updateCaseAfterExtensionRefused(caseData, AWAITING_ADMIN_ACTION.getId(), State.RESPONSE_RECEIVED);

            } else if (DirectionType.REFUSE_EXTENSION.toString().equals(caseData.getDirectionTypeDl().getValue().getCode())
                    && ExtensionNextEvent.SEND_TO_VALID_APPEAL.toString().equals(caseData.getExtensionNextEventDl().getValue().getCode())) {
                updateCaseAfterExtensionRefused(caseData, null, State.WITH_DWP);

            } else {
                caseData.setInterlocReviewState(null);
            }

        }
        return buildResponse(callback, caseDetails, caseData, sscsCaseDataPreSubmitCallbackResponse, url, documentTranslationStatus);
    }

    private PreSubmitCallbackResponse<SscsCaseData> buildResponse(Callback<SscsCaseData> callback,
                                                                  CaseDetails<SscsCaseData> caseDetails,
                                                                  SscsCaseData caseData,
                                                                  PreSubmitCallbackResponse<SscsCaseData> sscsCaseDataPreSubmitCallbackResponse,
                                                                  DocumentLink url,
                                                                  SscsDocumentTranslationStatus documentTranslationStatus) {

        caseData = updateCaseForDirectionType(caseDetails, caseData, documentTranslationStatus);


        if (callback.getEvent() == EventType.DIRECTION_ISSUED) {
            footerService.createFooterAndAddDocToCase(url, caseData, DocumentType.DIRECTION_NOTICE,
                    Optional.ofNullable(caseData.getDateAdded()).orElse(LocalDate.now())
                            .format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                    caseData.getDateAdded(), null, documentTranslationStatus);
        }

        if (!SscsDocumentTranslationStatus.TRANSLATION_REQUIRED.equals(documentTranslationStatus)) {

            State beforeState = callback.getCaseDetailsBefore().map(e -> e.getState()).orElse(null);
            clearTransientFields(caseData, beforeState);

            if (shouldSetDwpState(caseData)) {
                caseData.setDwpState(DwpState.DIRECTION_ACTION_REQUIRED.getId());
            }

            caseData.setTimeExtensionRequested("No");

            if (caseDetails.getState().equals(State.INTERLOCUTORY_REVIEW_STATE) && caseData.getDirectionTypeDl() != null && StringUtils.equals(DirectionType.APPEAL_TO_PROCEED.toString(), caseData.getDirectionTypeDl().getValue().getCode())) {
                PreSubmitCallbackResponse<SscsCaseData> response = serviceRequestExecutor.post(callback, bulkScanEndpoint);
                sscsCaseDataPreSubmitCallbackResponse.addErrors(response.getErrors());
            }
        } else {
            caseData.setInterlocReviewState(InterlocReviewState.WELSH_TRANSLATION.getId());
            caseData.setTranslationWorkOutstanding("Yes");
            clearBasicTransientFields(caseData);
            log.info("Set the InterlocReviewState to {},  for case id : {}", caseData.getInterlocReviewState(), caseData.getCcdCaseId());

            if (caseDetails.getState().equals(State.INTERLOCUTORY_REVIEW_STATE)
                    && caseData.getDirectionTypeDl() != null
                    && StringUtils.equals(DirectionType.APPEAL_TO_PROCEED.toString(), caseData.getDirectionTypeDl().getValue().getCode())) {

                PreSubmitCallbackResponse<SscsCaseData> response = serviceRequestExecutor.post(callback, bulkScanEndpoint);
                sscsCaseDataPreSubmitCallbackResponse.addErrors(response.getErrors());

            }
            log.info("Saved the new interloc direction document for case id: " + caseData.getCcdCaseId());
        }
        return sscsCaseDataPreSubmitCallbackResponse;
    }

    private boolean shouldSetDwpState(SscsCaseData caseData) {
        return isNull(caseData.getReinstatementOutcome())
                || (!caseData.getReinstatementOutcome().equals(RequestOutcome.GRANTED)
                && !caseData.getReinstatementOutcome().equals(RequestOutcome.REFUSED));
    }

}
