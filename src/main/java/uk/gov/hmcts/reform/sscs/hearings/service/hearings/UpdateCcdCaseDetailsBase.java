package uk.gov.hmcts.reform.sscs.hearings.service.hearings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.hearings.service.exceptions.UpdateCcdCaseDetailsException;

@Slf4j
@Component
@RequiredArgsConstructor
public abstract class UpdateCcdCaseDetailsBase<D> {

    private final CcdClient ccdClient;
    private final SscsCcdConvertService sscsCcdConvertService;

    public SscsCaseDetails updateCase(Long caseId, String eventType, IdamTokens idamTokens, D dto) throws UpdateCcdCaseDetailsException {
        log.info("UpdateCaseV2 for caseId {} and eventType {}", caseId, eventType);
        StartEventResponse startEventResponse = ccdClient.startEvent(idamTokens, caseId, eventType);
        CaseDetails caseDetails = startEventResponse.getCaseDetails();
        var data = sscsCcdConvertService.getCaseData(caseDetails.getData());

        /**
         * @see uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer#deserialize(String)
         * setCcdCaseId & sortCollections are called above, so this functionality has been replicated here preserving existing logic
         */
        data.setCcdCaseId(caseId.toString());
        data.sortCollections();

        UpdateCcdCaseService.UpdateResult result = applyUpdate(data, dto);


        CaseDataContent caseDataContent = sscsCcdConvertService.getCaseDataContent(data, startEventResponse, result.summary(), result.description());

        return sscsCcdConvertService.getCaseDetails(ccdClient.submitEventForCaseworker(idamTokens, caseId, caseDataContent));
    }

    protected abstract UpdateCcdCaseService.UpdateResult applyUpdate(SscsCaseData data, D dto) throws UpdateCcdCaseDetailsException;
}
