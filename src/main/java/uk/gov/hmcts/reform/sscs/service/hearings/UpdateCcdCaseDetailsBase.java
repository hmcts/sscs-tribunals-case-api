package uk.gov.hmcts.reform.sscs.service.hearings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public abstract class UpdateCcdCaseDetailsBase<T extends HearingRequest> {

    private final CcdClient ccdClient;
    private final SscsCcdConvertService sscsCcdConvertService;

    public SscsCaseDetails updateCase(Long caseId, String eventType, IdamTokens idamTokens, HearingRequest hearingRequest) throws ListingException {
        log.info("UpdateCaseV3 for caseId {} and eventType {}", caseId, eventType);
        StartEventResponse startEventResponse = ccdClient.startEvent(idamTokens, caseId, eventType);
        SscsCaseDetails caseDetails = sscsCcdConvertService.getCaseDetails(startEventResponse);
        SscsCaseData data = caseDetails.getData();

        /**
         * @see uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer#deserialize(String)
         * setCcdCaseId & sortCollections are called above, so this functionality has been replicated here preserving existing logic
         */
        data.setCcdCaseId(caseId.toString());
        data.sortCollections();

        UpdateCcdCaseService.UpdateResult result = applyUpdate(caseDetails, hearingRequest);

        CaseDataContent caseDataContent = sscsCcdConvertService.getCaseDataContent(data, startEventResponse, result.summary(), result.description());

        return sscsCcdConvertService.getCaseDetails(ccdClient.submitEventForCaseworker(idamTokens, caseId, caseDataContent));
    }

    protected abstract UpdateCcdCaseService.UpdateResult applyUpdate(SscsCaseDetails data, HearingRequest hearingRequest) throws ListingException;
}
