package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import java.util.function.Function;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipWriteFinalDecisionPreviewDecisionService;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.service.PipDecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.PipDecisionNoticeQuestionService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;
import uk.gov.hmcts.reform.sscs.service.VenueDataLoader;

public class IssueNoticeHandlerTest extends IssueNoticeHandlerTestBase {
    @Mock
    public GenerateFile generateFile;
    @Mock
    public UserDetailsService userDetailsService;
    @Mock
    public Function<LanguagePreference, String> templateId;
    @Mock
    public PipDecisionNoticeQuestionService decisionNoticeQuestionService;
    @Mock
    public PipDecisionNoticeOutcomeService decisionNoticeOutcomeService;
    @Mock
    public DocumentConfiguration documentConfiguration;
    @Mock
    private VenueDataLoader venueDataLoader;

    public IssueNoticeHandlerTest() {
        super("PIP");
    }

    @Override
    protected IssueNoticeHandler createIssueNoticeHandler(GenerateFile generateFile, UserDetailsService userDetailsService, Function<LanguagePreference, String> templateId) {
        return new PipWriteFinalDecisionPreviewDecisionService(generateFile, userDetailsService, decisionNoticeQuestionService, decisionNoticeOutcomeService, documentConfiguration, venueDataLoader);
    }
}
