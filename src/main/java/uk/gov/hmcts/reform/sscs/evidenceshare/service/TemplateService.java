package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static org.apache.commons.lang3.StringUtils.stripToNull;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;
import uk.gov.hmcts.reform.sscs.evidenceshare.config.DocmosisTemplateConfig;

@Service
@Slf4j
@RequiredArgsConstructor
public class TemplateService {

    private final DocmosisTemplateConfig docmosisTemplateConfig;

    public Template findTemplate(SscsCaseData caseData) {
        if (caseData.getAppeal().getMrnDetails() != null
            && stripToNull(caseData.getAppeal().getMrnDetails().getMrnDate()) != null) {
            LocalDate mrnDate = LocalDate.parse(caseData.getAppeal().getMrnDetails().getMrnDate());
            if (mrnDate.plusDays(30).isBefore(LocalDate.now())) {
                return new Template(docmosisTemplateConfig.getTemplate().get(LanguagePreference.ENGLISH)
                    .get(DocumentType.DL16.getValue()).get("name"), DocumentType.DL16.getValue());
            }
            return new Template(docmosisTemplateConfig.getTemplate().get(LanguagePreference.ENGLISH)
                .get(DocumentType.DL6.getValue()).get("name"), DocumentType.DL6.getValue());
        }
        return null;
    }
}
