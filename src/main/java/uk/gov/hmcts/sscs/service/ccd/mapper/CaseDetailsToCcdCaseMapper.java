package uk.gov.hmcts.sscs.service.ccd.mapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.sscs.domain.corecase.CcdCase;
import uk.gov.hmcts.sscs.exception.ApplicationErrorException;
import uk.gov.hmcts.sscs.model.ccd.CaseData;

@Service
public class CaseDetailsToCcdCaseMapper {

    public CcdCase map(CaseDetails caseDetails) {
        CaseData caseData = getCaseData(caseDetails.getData().get("case-data"));

        CcdCase ccdCase = new CcdCase();
        ccdCase.setCaseReference(caseData.getCaseReference());
        ccdCase.setBenefitType(getBenefitType(caseData));

        return  ccdCase;
    }


    private CaseData getCaseData(Object object) {

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        try {
            return mapper.convertValue(object, CaseData.class);
        } catch (Exception e) {
            throw new ApplicationErrorException("Error occurred when CaseDetails are mapped into CcdCase", e);
        }
    }

    private String getBenefitType(CaseData caseData) {
        return caseData.getAppeal().getBenefitType().getCode();
    }
}
