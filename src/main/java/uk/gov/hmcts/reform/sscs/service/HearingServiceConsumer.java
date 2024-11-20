package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.isNull;

import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
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


    public Consumer<SscsCaseData> getCreateHearingCaseDataConsumer(HmcUpdateResponse response, Long hearingRequestId) {
        return sscsCaseData -> updateCaseDataWithHearingResponse(response, hearingRequestId, sscsCaseData);

    }

    public Consumer<SscsCaseDetails> getCreateHearingCaseDetailsConsumerV2(HmcUpdateResponse response, Long hearingRequestId, boolean isUpdateHearing) {
        return sscsCaseDetails -> {
            try {
                if (isUpdateHearing) {
                    OverridesMapping.setOverrideValues(sscsCaseDetails.getData(), refData);
                } else {
                    OverridesMapping.setDefaultListingValues(sscsCaseDetails.getData(), refData);
                }
                updateCaseDataWithHearingResponse(response, hearingRequestId, sscsCaseDetails.getData());
            } catch (ListingException e) {
                throw new RuntimeException(e);
            }
        };

    }

    private void updateCaseDataWithHearingResponse(HmcUpdateResponse response, Long hearingRequestId, SscsCaseData caseData) {
        Hearing hearing = HearingsServiceHelper.getHearingById(hearingRequestId, caseData);

        if (isNull(hearing)) {
            hearing = HearingsServiceHelper.createHearing(hearingRequestId);
            HearingsServiceHelper.addHearing(hearing, caseData);
        }

        HearingsServiceHelper.updateHearingId(hearing, response);
        HearingsServiceHelper.updateVersionNumber(hearing, response);

        if (refData.isAdjournmentFlagEnabled()
                && YesNo.isYes(caseData.getAdjournment().getAdjournmentInProgress())) {
            log.debug("Case Updated with AdjournmentInProgress to NO for Case ID {}", caseData.getCcdCaseId());
            caseData.getAdjournment().setAdjournmentInProgress(YesNo.NO);
        }
    }
}
