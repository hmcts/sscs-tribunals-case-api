package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatelistingrequirements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingInterpreter;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.ReservedToMember;
import uk.gov.hmcts.reform.sscs.client.JudicialRefDataApi;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.client.JudicialMemberAppointments;
import uk.gov.hmcts.reform.sscs.model.client.JudicialRefDataUsersRequest;
import uk.gov.hmcts.reform.sscs.model.client.JudicialRefDataUsersResponse;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUser;
import uk.gov.hmcts.reform.sscs.reference.data.model.Language;
import uk.gov.hmcts.reform.sscs.reference.data.service.SignLanguagesService;
import uk.gov.hmcts.reform.sscs.reference.data.service.VerbalLanguagesService;

@RunWith(MockitoJUnitRunner.class)
public class UpdateListingRequirementsHandlerTest {

    private static final String IDAM_OAUTH2_TOKEN = "TestOauth2Token";
    private static final String SERVICE_AUTHORIZATION = "TestServiceAuthorization";

    @Mock
    private IdamService idamService;
    @Mock
    private SignLanguagesService signLanguagesService;
    @Mock
    private VerbalLanguagesService verbalLanguagesService;
    @Mock
    private JudicialRefDataApi judicialRefData;
    @InjectMocks
    private UpdateListingRequirementsHandler updateListingRequirementsHandler;

    @Test
    public void generateInterpreterLanguageFields() {
        Language signLanguage = new Language("sign-mkn", "Makaton", null, null, null);
        given(signLanguagesService.getSignLanguages()).willReturn(List.of(signLanguage));

        Language verbalLanguage = new Language("fre", "French", null, null, null);
        given(verbalLanguagesService.getVerbalLanguages()).willReturn(List.of(verbalLanguage));

        OverrideFields overrideFields = OverrideFields.builder()
            .appellantInterpreter(HearingInterpreter.builder()
                .interpreterLanguage(new DynamicList(null,null))
                .build())
            .build();

        updateListingRequirementsHandler.generateInterpreterLanguageFields(overrideFields);

        List<DynamicListItem> result = overrideFields.getAppellantInterpreter().getInterpreterLanguage().getListItems();

        assertThat(result)
            .hasSize(2)
            .extracting("code","label")
            .containsExactlyInAnyOrder(tuple("sign-mkn","Makaton"), tuple("fre","French"));
    }

    @Test
    public void generateInterpreterLanguageFieldsInterpreterLanguageNull() {
        Language signLanguage = new Language("sign-mkn", "Makaton", null, null, null);
        given(signLanguagesService.getSignLanguages()).willReturn(List.of(signLanguage));

        Language verbalLanguage = new Language("fre", "French", null, null, null);
        given(verbalLanguagesService.getVerbalLanguages()).willReturn(List.of(verbalLanguage));

        OverrideFields overrideFields = OverrideFields.builder()
            .appellantInterpreter(HearingInterpreter.builder()
                .interpreterLanguage(null)
                .build())
            .build();

        updateListingRequirementsHandler.generateInterpreterLanguageFields(overrideFields);

        List<DynamicListItem> result = overrideFields.getAppellantInterpreter().getInterpreterLanguage().getListItems();

        assertThat(result)
            .hasSize(2)
            .extracting("code","label")
            .containsExactlyInAnyOrder(tuple("sign-mkn","Makaton"), tuple("fre","French"));
    }

    @Test
    public void generateInterpreterLanguageFieldsAppellantInterpreterNull() {
        Language signLanguage = new Language("sign-mkn", "Makaton", null, null, null);
        given(signLanguagesService.getSignLanguages()).willReturn(List.of(signLanguage));

        Language verbalLanguage = new Language("fre", "French", null, null, null);
        given(verbalLanguagesService.getVerbalLanguages()).willReturn(List.of(verbalLanguage));

        OverrideFields overrideFields = OverrideFields.builder()
            .appellantInterpreter(null)
            .build();

        updateListingRequirementsHandler.generateInterpreterLanguageFields(overrideFields);

        List<DynamicListItem> result = overrideFields.getAppellantInterpreter().getInterpreterLanguage().getListItems();

        assertThat(result)
            .hasSize(2)
            .extracting("code","label")
            .containsExactlyInAnyOrder(tuple("sign-mkn","Makaton"), tuple("fre","French"));
    }

    @Test
    public void generateReservedToJudgeFields() {
        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder()
            .idamOauth2Token(IDAM_OAUTH2_TOKEN)
            .serviceAuthorization(SERVICE_AUTHORIZATION)
            .build());

        JudicialRefDataUsersResponse response = JudicialRefDataUsersResponse.builder()
            .judicialUsers(List.of(JudicialUser.builder()
                .personalCode("1234")
                .fullName("Test Person")
                .postNominals("Judge")
                .appointments(List.of(JudicialMemberAppointments.builder()
                    .appointment("Tribunal Judge")
                    .build()))
                .build()))
            .build();

        given(judicialRefData.getJudicialUsers(
            eq(IDAM_OAUTH2_TOKEN),
            eq(SERVICE_AUTHORIZATION),
            any(JudicialRefDataUsersRequest.class)))
            .willReturn(response);

        OverrideFields overrideFields = OverrideFields.builder()
            .reservedToJudge(ReservedToMember.builder()
                .reservedMember(new DynamicList(null,null))
                .build())
            .build();

        updateListingRequirementsHandler.generateReservedToJudgeFields(overrideFields);

        List<DynamicListItem> result = overrideFields.getReservedToJudge().getReservedMember().getListItems();

        assertThat(result)
            .hasSize(1)
            .extracting("code","label")
            .containsExactlyInAnyOrder(tuple("1234|84","Test Person Judge"));
    }

    @Test
    public void generateReservedToJudgeFieldsReservedToMemberNull() {
        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder()
            .idamOauth2Token(IDAM_OAUTH2_TOKEN)
            .serviceAuthorization(SERVICE_AUTHORIZATION)
            .build());

        JudicialRefDataUsersResponse response = JudicialRefDataUsersResponse.builder()
            .judicialUsers(List.of(JudicialUser.builder()
                .personalCode("1234")
                .fullName("Test Person")
                .postNominals("Judge")
                .appointments(List.of(JudicialMemberAppointments.builder()
                    .appointment("Tribunal Judge")
                    .build()))
                .build()))
            .build();

        given(judicialRefData.getJudicialUsers(
            eq(IDAM_OAUTH2_TOKEN),
            eq(SERVICE_AUTHORIZATION),
            any(JudicialRefDataUsersRequest.class)))
            .willReturn(response);

        OverrideFields overrideFields = OverrideFields.builder()
            .reservedToJudge(ReservedToMember.builder()
                .reservedMember(null)
                .build())
            .build();

        updateListingRequirementsHandler.generateReservedToJudgeFields(overrideFields);

        List<DynamicListItem> result = overrideFields.getReservedToJudge().getReservedMember().getListItems();

        assertThat(result)
            .hasSize(1)
            .extracting("code","label")
            .containsExactlyInAnyOrder(tuple("1234|84","Test Person Judge"));
    }

    @Test
    public void generateReservedToJudgeFieldsReservedToJudgeNull() {
        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder()
            .idamOauth2Token(IDAM_OAUTH2_TOKEN)
            .serviceAuthorization(SERVICE_AUTHORIZATION)
            .build());

        JudicialRefDataUsersResponse response = JudicialRefDataUsersResponse.builder()
            .judicialUsers(List.of(JudicialUser.builder()
                .personalCode("1234")
                .fullName("Test Person")
                .postNominals("Judge")
                .appointments(List.of(JudicialMemberAppointments.builder()
                    .appointment("Tribunal Judge")
                    .build()))
                .build()))
            .build();

        given(judicialRefData.getJudicialUsers(
            eq(IDAM_OAUTH2_TOKEN),
            eq(SERVICE_AUTHORIZATION),
            any(JudicialRefDataUsersRequest.class)))
            .willReturn(response);

        OverrideFields overrideFields = OverrideFields.builder()
            .reservedToJudge(null)
            .build();

        updateListingRequirementsHandler.generateReservedToJudgeFields(overrideFields);

        List<DynamicListItem> result = overrideFields.getReservedToJudge().getReservedMember().getListItems();

        assertThat(result)
            .hasSize(1)
            .extracting("code","label")
            .containsExactlyInAnyOrder(tuple("1234|84","Test Person Judge"));
    }

    @Test
    public void getLanguageDynamicListItem() {
        Language language = new Language("fre", "French", null, null, null);

        DynamicListItem result = updateListingRequirementsHandler.getLanguageDynamicListItem(language);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("fre");
        assertThat(result.getLabel()).isEqualTo("French");
    }

    @Test
    public void getLanguageDynamicListItemDialect() {
        Language language = new Language("luo", "Luo", "lah", "Acholi", null);

        DynamicListItem result = updateListingRequirementsHandler.getLanguageDynamicListItem(language);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("luo-lah");
        assertThat(result.getLabel()).isEqualTo("Acholi");
    }

    @Test
    public void getLanguages() {
        Language signLanguage = new Language("sign-mkn", "Makaton", null, null, null);
        given(signLanguagesService.getSignLanguages()).willReturn(List.of(signLanguage));

        Language verbalLanguage = new Language("fre", "French", null, null, null);
        given(verbalLanguagesService.getVerbalLanguages()).willReturn(List.of(verbalLanguage));

        List<Language> result = updateListingRequirementsHandler.getLanguages();

        assertThat(result)
            .hasSize(2)
            .containsExactlyInAnyOrder(signLanguage, verbalLanguage);
    }

    @Test
    public void generateReservedMembers() {
        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder()
            .serviceAuthorization(SERVICE_AUTHORIZATION)
            .idamOauth2Token(IDAM_OAUTH2_TOKEN)
            .build());

        JudicialRefDataUsersResponse response = JudicialRefDataUsersResponse.builder()
            .judicialUsers(List.of(JudicialUser.builder()
                .personalCode("1234")
                .fullName("Test Person")
                .postNominals("Judge")
                .appointments(List.of(JudicialMemberAppointments.builder()
                    .appointment("Tribunal Judge")
                    .build()))
                .build()))
            .build();

        given(judicialRefData.getJudicialUsers(
            eq(IDAM_OAUTH2_TOKEN),
            eq(SERVICE_AUTHORIZATION),
            any(JudicialRefDataUsersRequest.class)))
            .willReturn(response);

        List<DynamicListItem> result = updateListingRequirementsHandler.generateReservedMembers();

        assertThat(result)
            .hasSize(1)
            .extracting("code","label")
            .containsExactlyInAnyOrder(tuple("1234|84","Test Person Judge"));
    }

    @Test
    public void generateReservedMembersNullJudicialUsers() {
        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder()
            .idamOauth2Token(IDAM_OAUTH2_TOKEN)
            .serviceAuthorization(SERVICE_AUTHORIZATION)
            .build());

        given(judicialRefData.getJudicialUsers(
            eq(IDAM_OAUTH2_TOKEN),
            eq(SERVICE_AUTHORIZATION),
            any(JudicialRefDataUsersRequest.class)))
            .willReturn(JudicialRefDataUsersResponse.builder().build());

        List<DynamicListItem> result = updateListingRequirementsHandler.generateReservedMembers();

        assertThat(result).isEmpty();
    }

    @Test
    public void getJudicialMemberListItem() {
        JudicialUser judicialUser = JudicialUser.builder()
            .personalCode("1234")
            .appointments(List.of(JudicialMemberAppointments.builder()
                .appointment("Tribunal Judge")
                .build()))
            .fullName("Test Person")
            .postNominals("Judge")
            .build();
        DynamicListItem result = updateListingRequirementsHandler.getJudicialMemberListItem(judicialUser);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("1234|84");
        assertThat(result.getLabel()).isEqualTo("Test Person Judge");
    }

    @Test
    public void getJudicialMemberListItemNoAppointments() {
        JudicialUser judicialUser = JudicialUser.builder()
            .personalCode("1234")
            .fullName("Test Person")
            .postNominals("Judge")
            .build();
        DynamicListItem result = updateListingRequirementsHandler.getJudicialMemberListItem(judicialUser);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("1234|");
        assertThat(result.getLabel()).isEqualTo("Test Person Judge");
    }

    @Test
    public void getJudicialMemberListItemNoPostNominals() {
        JudicialUser judicialUser = JudicialUser.builder()
            .personalCode("1234")
            .fullName("Test Person")
            .appointments(List.of(JudicialMemberAppointments.builder()
                .appointment("Tribunal Judge")
                .build()))
            .build();
        DynamicListItem result = updateListingRequirementsHandler.getJudicialMemberListItem(judicialUser);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("1234|84");
        assertThat(result.getLabel()).isEqualTo("Test Person");
    }

}
