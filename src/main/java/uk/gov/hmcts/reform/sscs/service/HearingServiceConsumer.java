package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.isNull;

import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberComposition;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.helper.mapping.OverridesMapping;
import uk.gov.hmcts.reform.sscs.helper.service.HearingsServiceHelper;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HmcUpdateResponse;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.UnusedFormalParameter", "PMD.TooManyMethods", "AvoidThrowingRawExceptionTypes"})
public class HearingServiceConsumer {
    private final ReferenceDataServiceHolder refData;
    private final OverridesMapping overridesMapping;

    public Consumer<SscsCaseData> getCreateHearingCaseDataConsumer(HmcUpdateResponse response, Long hearingRequestId) {
        return sscsCaseData -> updateCaseDataWithHearingResponse(response, hearingRequestId, sscsCaseData);

    }

    public Consumer<SscsCaseDetails> getCreateHearingCaseDetailsConsumerV2(PanelMemberComposition panelComposition,
                                                                           HmcUpdateResponse response,
                                                                           Long hearingRequestId,
                                                                           boolean isUpdateHearing) {
        return sscsCaseDetails -> {
            try {
                if (isUpdateHearing) {
                    log.info("case id {} is an update hearing request, not setting default listing values",
                            sscsCaseDetails.getData().getCcdCaseId());
                    if (isNull(sscsCaseDetails.getData().getSchedulingAndListingFields().getOverrideFields())) {
                        overridesMapping.setOverrideValues(sscsCaseDetails.getData(), refData);
                    }
                } else {
                    log.info("case id {} is a create hearing request, setting default listing values",
                            sscsCaseDetails.getData().getCcdCaseId());
                    overridesMapping.setDefaultListingValues(sscsCaseDetails.getData(), refData);
                }
                sscsCaseDetails.getData().setPanelMemberComposition(panelComposition);
                updateCaseDataWithHearingResponse(response, hearingRequestId, sscsCaseDetails.getData());
            } catch (ListingException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private void updateCaseDataWithHearingResponse(HmcUpdateResponse response, Long hearingRequestId, SscsCaseData caseData) {
        log.info("case id {} received hearing response from HMC for hearing request id {}",
                caseData.getCcdCaseId(), hearingRequestId);
        Hearing hearing = HearingsServiceHelper.getHearingById(hearingRequestId, caseData);
        log.info("case id {} found hearing {} for hearing request id {}",
                caseData.getCcdCaseId(), hearing, hearingRequestId);

        if (isNull(hearing)) {
            log.info("case id {} did not find hearing for hearing request id {}, creating new hearing",
                    caseData.getCcdCaseId(), hearingRequestId);
            hearing = HearingsServiceHelper.createHearing(hearingRequestId);
            HearingsServiceHelper.addHearing(hearing, caseData);
        }

        HearingsServiceHelper.updateHearingId(hearing, response);
        HearingsServiceHelper.updateVersionNumber(hearing, response);

        if (YesNo.isYes(caseData.getAdjournment().getAdjournmentInProgress())) {
            log.debug("Case Updated with AdjournmentInProgress to NO for Case ID {}", caseData.getCcdCaseId());
            caseData.getAdjournment().setAdjournmentInProgress(YesNo.NO);
        }
        log.info("case id {} updated case data with hearing response xx", caseData.getCcdCaseId());
    }
}
