package uk.gov.hmcts.reform.sscs.util;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.reference.data.model.Language;
import uk.gov.hmcts.reform.sscs.reference.data.service.SignLanguagesService;
import uk.gov.hmcts.reform.sscs.reference.data.service.VerbalLanguagesService;

@Component
@RequiredArgsConstructor
public class DynamicListLanguageUtil {
    private final SignLanguagesService signLanguagesService;
    private final VerbalLanguagesService verbalLanguagesService;

    public DynamicList generateInterpreterLanguageFields(DynamicList list) {
        if (isNull(list)) {
            list = new DynamicList(null, null);
        }

        List<DynamicListItem> interpreterLanguage = generateInterpreterLanguage();

        list.setListItems(interpreterLanguage);
        return list;
    }

    @NotNull
    private List<DynamicListItem> generateInterpreterLanguage() {
        List<Language> signLanguages = signLanguagesService.getSignLanguages();
        List<Language> verbalLanguages = verbalLanguagesService.getVerbalLanguages();

        return Stream.concat(signLanguages.stream(), verbalLanguages.stream())
            .map(this::getLanguageDynamicListItem)
            .sorted(Comparator.comparing(DynamicListItem::getLabel))
            .toList();
    }

    @NotNull
    public DynamicListItem getLanguageDynamicListItem(Language language) {
        String reference = language.getReference();
        String name = language.getNameEn();
        String dialectReference = language.getDialectReference();
        String mrdReference = language.getMrdReference();

        if (nonNull(dialectReference)) {
            reference = String.format("%s-%s", reference, dialectReference);
            name = language.getDialectEn();
        }

        if (nonNull(mrdReference)) {
            reference = mrdReference;
        }
        return new DynamicListItem(reference, name);
    }
}
