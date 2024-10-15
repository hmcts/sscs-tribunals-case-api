package uk.gov.hmcts.reform.sscs.helper.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.reference.data.service.HearingDurationsService;
import uk.gov.hmcts.reform.sscs.reference.data.service.SessionCategoryMapService;
import uk.gov.hmcts.reform.sscs.reference.data.service.SignLanguagesService;
import uk.gov.hmcts.reform.sscs.reference.data.service.VerbalLanguagesService;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

@ExtendWith(MockitoExtension.class)
public class HearingsMappingBase {

    public static final String CASE_CREATED = "2022-04-01";

    @Mock
    public HearingDurationsService hearingDurations;

    @Mock
    public SessionCategoryMapService sessionCategoryMaps;

    @Mock
    public VerbalLanguagesService verbalLanguages;

    @Mock
    public SignLanguagesService signLanguages;

    @Mock
    public ReferenceDataServiceHolder refData;

    public SscsCaseData caseData;

    protected HearingsMappingBase() {

    }

    public static final long CASE_ID = 1625080769409918L;

    public static final String ARRAY_SPLIT_REGEX = "\\s*\\|\\s*";

    public static final String CASE_NAME_PUBLIC = "Case Name Public";

    public static final String CASE_NAME_INTERNAL = "Case Name Internal";

    public static final String BENEFIT_CODE = "002";

    public static final String CHILD_SUPPORT_BENEFIT_CODE = "022";

    public static final String ISSUE_CODE = "DD";

    public static final String REGION = "Test Region";

    public static final String EPIMS_ID = "239585";

    public static final String EX_UI_URL = "http://localhost:3455";

    public static final int DURATION_FACE_TO_FACE = 60;

    public static final int DURATION_INTERPRETER = 75;

    public static final int DURATION_PAPER = 40;

    @BeforeEach
    public void setUpCaseData() {
        caseData = SscsCaseData.builder()
            .benefitCode(BENEFIT_CODE)
            .issueCode(ISSUE_CODE)
            .appeal(Appeal.builder()
                .hearingOptions(HearingOptions.builder()
                    .build())
                .build())
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .overrideFields(OverrideFields.builder()
                    .build())
                .build())
            .build();
    }

    @NotNull
    public static List<String> splitCsvParamArray(String expected) {
        List<String> paramArray = new ArrayList<>(List.of(expected.split(ARRAY_SPLIT_REGEX)));
        paramArray.removeAll(Arrays.asList("", null));
        return paramArray;
    }
}
