package uk.gov.hmcts.reform.sscs.tyanotifications.service.coh;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
public class QuestionService {
    private final CohClient cohClient;
    private final IdamService idamService;

    public QuestionService(CohClient cohClient, IdamService idamService) {
        this.cohClient = cohClient;
        this.idamService = idamService;
    }

    public String getQuestionRequiredByDate(String onlineHearingId) {
        IdamTokens idamTokens = idamService.getIdamTokens();

        QuestionRounds questionRounds = cohClient.getQuestionRounds(
            idamTokens.getIdamOauth2Token(),
            idamTokens.getServiceAuthorization(),
            onlineHearingId
        );

        int currentQuestionRound = questionRounds.getCurrentQuestionRound();

        List<QuestionReferences> questionRefsForCurrentRound =
            questionRounds.getQuestionRounds().get(currentQuestionRound - 1).getQuestionReferences();
        if (questionRefsForCurrentRound != null && !questionRefsForCurrentRound.isEmpty()) {
            return questionRefsForCurrentRound.get(0).getDeadlineExpiryDate();
        } else {
            throw new IllegalStateException(
                "Cannot get questions required by date as question round has been published with no questions in it"
            );
        }
    }

    public QuestionRounds getQuestionRounds(String onlineHearingId) {
        IdamTokens idamTokens = idamService.getIdamTokens();

        return cohClient.getQuestionRounds(
            idamTokens.getIdamOauth2Token(),
            idamTokens.getServiceAuthorization(),
            onlineHearingId
        );
    }
}
