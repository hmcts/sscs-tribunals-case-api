package uk.gov.hmcts.reform.sscs.util;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingInterpreter;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.reference.data.model.Language;
import uk.gov.hmcts.reform.sscs.reference.data.service.SignLanguagesService;
import uk.gov.hmcts.reform.sscs.reference.data.service.VerbalLanguagesService;

@Component
@RequiredArgsConstructor
public class UpdateListingRequirementsUtil {
    private final SignLanguagesService signLanguagesService;
    private final VerbalLanguagesService verbalLanguagesService;

    public void generateInterpreterLanguageFields(OverrideFields overrideFields) {
        if (isNull(overrideFields.getAppellantInterpreter())
            || isNull(overrideFields.getAppellantInterpreter().getInterpreterLanguage())) {
            overrideFields.setAppellantInterpreter(HearingInterpreter.builder()
                .interpreterLanguage(new DynamicList(null, null))
                .build());
        }

        List<DynamicListItem> interpreterLanguage = generateInterpreterLanguage();

        overrideFields.getAppellantInterpreter().getInterpreterLanguage()
            .setListItems(interpreterLanguage);
    }

    @NotNull
    private List<DynamicListItem> generateInterpreterLanguage() {
        List<Language> signLanguages = signLanguagesService.getSignLanguages();
        List<Language> verbalLanguages = verbalLanguagesService.getVerbalLanguages();

        return Stream.concat(signLanguages.stream(), verbalLanguages.stream())
            .map(this::getLanguageDynamicListItem)
            .sorted(Comparator.comparing(DynamicListItem::getLabel))
            .collect(Collectors.toList());
    }

    @NotNull
    private DynamicListItem getLanguageDynamicListItem(Language language) {
        String reference = language.getReference();
        String name = language.getNameEn();

        if (nonNull(language.getDialectReference())) {
            reference = String.format("%s-%s", language.getReference(), language.getDialectReference());
            name = language.getDialectEn();
        }
        return new DynamicListItem(reference, name);
    }
}
