package uk.gov.hmcts.reform.sscs.domain.pdf;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Value
@Builder
public class PdfWrapper {

    private SscsCaseData sscsCaseData;

    private Long ccdCaseId;

    private Boolean isSignLanguageInterpreterRequired;

    private Boolean isHearingLoopRequired;

    private Boolean isAccessibleHearingRoomRequired;

    private LocalDate currentDate;

    private String repFullName;

    private String englishBenefitName;

    private Boolean isNiPostCodeFeatureEnabled;

}
