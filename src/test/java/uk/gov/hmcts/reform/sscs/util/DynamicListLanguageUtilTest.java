package uk.gov.hmcts.reform.sscs.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.reference.data.model.Language;
import uk.gov.hmcts.reform.sscs.reference.data.service.SignLanguagesService;
import uk.gov.hmcts.reform.sscs.reference.data.service.VerbalLanguagesService;

@RunWith(MockitoJUnitRunner.class)
public class DynamicListLanguageUtilTest {

    @Mock
    private SignLanguagesService signLanguagesService;
    @Mock
    private VerbalLanguagesService verbalLanguagesService;
    @InjectMocks
    private DynamicListLanguageUtil dynamicListLanguageUtil;

    @Test
    public void shouldGenerateInterpreterLanguageFields() {
        Language signLanguage = new Language("sign-mkn", "Makaton", null, null, null, null);
        given(signLanguagesService.getSignLanguages()).willReturn(List.of(signLanguage));

        Language verbalLanguage = new Language("fre", "French", null, null, null, null);
        given(verbalLanguagesService.getVerbalLanguages()).willReturn(List.of(verbalLanguage));

        DynamicList list = dynamicListLanguageUtil.generateInterpreterLanguageFields(null);

        List<DynamicListItem> result = list.getListItems();

        assertThat(result)
                .hasSize(2)
                .extracting("code", "label")
                .containsExactlyInAnyOrder(tuple("sign-mkn", "Makaton"), tuple("fre", "French"));
    }

    @Test
    public void shouldGenerateInterpreterLanguageFields_WhenInterpreterLanguageNull() {
        Language signLanguage = new Language("sign-mkn", "Makaton", null, null, null, null);
        given(signLanguagesService.getSignLanguages()).willReturn(List.of(signLanguage));

        Language verbalLanguage = new Language("fre", "French", null, null, null, null);
        given(verbalLanguagesService.getVerbalLanguages()).willReturn(List.of(verbalLanguage));

        DynamicList list = dynamicListLanguageUtil.generateInterpreterLanguageFields(null);

        List<DynamicListItem> result = list.getListItems();

        assertThat(result)
                .hasSize(2)
                .extracting("code", "label")
                .containsExactlyInAnyOrder(tuple("sign-mkn", "Makaton"), tuple("fre", "French"));
    }

    @Test
    public void shouldGenerateInterpreterLanguageFields_WhenAppellantInterpreterNull() {
        Language signLanguage = new Language("sign-mkn", "Makaton", null, null, null, null);
        given(signLanguagesService.getSignLanguages()).willReturn(List.of(signLanguage));

        Language verbalLanguage = new Language("fre", "French", null, null, null, null);
        given(verbalLanguagesService.getVerbalLanguages()).willReturn(List.of(verbalLanguage));

        DynamicList list = dynamicListLanguageUtil.generateInterpreterLanguageFields(null);

        List<DynamicListItem> result = list.getListItems();

        assertThat(result)
                .hasSize(2)
                .extracting("code", "label")
                .containsExactlyInAnyOrder(tuple("sign-mkn", "Makaton"), tuple("fre", "French"));
    }

    @Test
    public void shouGenerateInterpreterLanguageFieldsWithMrdReference() {
        List<Language> languages = List.of(
                new Language("reference-1", "name-1", "mrdReference-1", null, "mrdReference-1", null),
                new Language("reference-2", "name-2", "mrdReference-2", null, "mrdReference-2", null),
                new Language("reference-3", "name-3", "mrdReference-3", null, "mrdReference-3", null),
                new Language("reference-4", "name-4", "mrdReference-4", null, "mrdReference-4", null),
                new Language("reference-5", "name-5", "mrdReference-5", null, "mrdReference-5", null)
        );
        given(signLanguagesService.getSignLanguages()).willReturn(languages);
        DynamicList list = dynamicListLanguageUtil.generateInterpreterLanguageFields(null);
        List<DynamicListItem> result = list.getListItems();

        List<String> referenceList = languages.stream().map(Language::getMrdReference).collect(Collectors.toList());
        List<String> nameList = languages.stream().map(Language::getNameEn).collect(Collectors.toList());

        List<String> resultCodeList = result.stream().map(DynamicListItem::getCode).collect(Collectors.toList());
        List<String> resultLabelList = result.stream().map(DynamicListItem::getLabel).collect(Collectors.toList());

        assertThat(referenceList).containsAll(resultCodeList);
        assertThat(nameList).containsAll(resultLabelList);
    }

    @Test
    public void shouldReturnNameAsDialect_WhenDialectReferenceIsNonNull() {
        Language language = new Language(
                "reference-1",
                "name-1",
                "mrdReference-1",
                "dialect-1",
                "dialectRef-1",
                null);

        DynamicListItem result = dynamicListLanguageUtil.getLanguageDynamicListItem(language);

        assertThat(result.getLabel().equals("dialectRef-1"));

    }

    @Test
    public void shouldReturnNewDynamicList_WhenDynamicListIsNullForGenerateInterpreterLanguageFields() {
        DynamicList list = dynamicListLanguageUtil.generateInterpreterLanguageFields(null);

        assertThat(list.getListItems().size() == 0);
    }
}
