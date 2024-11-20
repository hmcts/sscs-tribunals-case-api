package uk.gov.hmcts.reform.sscs.tyanotifications.service.coh;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

public class QuestionServiceTest {

    private CohClient cohClient;
    private IdamService idamService;
    private String someHearingId;
    private String expectedDate;
    private String authHeader;
    private String serviceAuthHeader;
    private IdamTokens idamTokens;

    @BeforeEach
    public void setUp() {
        cohClient = mock(CohClient.class);
        idamService = mock(IdamService.class);
        someHearingId = "someHearingId";
        expectedDate = "expectedDate";
        authHeader = "authHeader";
        serviceAuthHeader = "serviceAuthHeader";
        idamTokens = IdamTokens.builder().idamOauth2Token(authHeader).serviceAuthorization(serviceAuthHeader).build();

        when(idamService.getIdamTokens()).thenReturn(idamTokens);
    }

    @Test
    public void getsRequiredByDateForFirstRound() {
        when(cohClient.getQuestionRounds(authHeader, serviceAuthHeader, someHearingId))
            .thenReturn(new QuestionRounds(1, singletonList(
                new QuestionRound(Collections.singletonList(new QuestionReferences(expectedDate))))
            ));
        String questionRequiredByDate = new QuestionService(cohClient, idamService).getQuestionRequiredByDate(someHearingId);

        assertThat(questionRequiredByDate, is(expectedDate));
    }

    @Test
    public void getsRequiredByDateForSecondRound() {
        when(cohClient.getQuestionRounds(authHeader, serviceAuthHeader, someHearingId))
            .thenReturn(new QuestionRounds(2, asList(
                new QuestionRound(Collections.singletonList(new QuestionReferences("Different date"))),
                new QuestionRound(Collections.singletonList(new QuestionReferences(expectedDate))))
            ));
        String questionRequiredByDate = new QuestionService(cohClient, idamService).getQuestionRequiredByDate(someHearingId);

        assertThat(questionRequiredByDate, is(expectedDate));
    }

    @Test
    public void questionRoundsMustHaveAtLeastOneQuestion() {
        assertThrows(IllegalStateException.class, () -> {
            when(cohClient.getQuestionRounds(authHeader, serviceAuthHeader, someHearingId))
                .thenReturn(new QuestionRounds(1, singletonList(new QuestionRound(emptyList()))));

            new QuestionService(cohClient, idamService).getQuestionRequiredByDate(someHearingId);
        });
    }

    @Test
    public void getsQuestionRounds() {
        QuestionRounds expectedQuestionRounds = new QuestionRounds(1, singletonList(new QuestionRound(emptyList())));
        when(cohClient.getQuestionRounds(authHeader, serviceAuthHeader, someHearingId))
            .thenReturn(expectedQuestionRounds);

        QuestionRounds questionRounds = new QuestionService(cohClient, idamService).getQuestionRounds(someHearingId);

        assertThat(questionRounds, is(expectedQuestionRounds));
    }
}
