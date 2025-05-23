package uk.gov.hmcts.reform.sscs.bulkscan.service;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Objects.isNull;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.apache.commons.lang3.ArrayUtils.contains;
import static org.apache.commons.lang3.RegExUtils.replaceAll;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.rightPad;
import static org.apache.commons.lang3.StringUtils.stripToEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.ATTENDANCE_ALLOWANCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.BEREAVEMENT_BENEFIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.BEREAVEMENT_SUPPORT_PAYMENT_SCHEME;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CARERS_ALLOWANCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.DLA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.ESA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.IIDB;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.INCOME_SUPPORT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.INDUSTRIAL_DEATH_BENEFIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.JSA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.MATERNITY_ALLOWANCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.PENSION_CREDIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.PIP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.RETIREMENT_PENSION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.SOCIAL_FUND;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.UC;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.findBenefitByDescription;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.findBenefitByShortName;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.values;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.BoundExtractedResult;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsType;

@Service
@Slf4j
public class FuzzyMatcherService {
    private static final int THRESHOLD_MATCH_SCORE = 90;
    private static final int MAX_FUZZY_SEARCH_LENGTH = 4;

    private static final List<String> EXACT_WORDS_THAT_WILL_NOT_CAUSE_A_MATCH = List.of(
        "support",
        "allowance",
        "benefit",
        "pension",
        ""
    );

    private static final Set<Pair<String, Benefit>> EXACT_WORDS_THAT_WILL_CAUSE_A_MATCH =
        ImmutableSet.<Pair<String, Benefit>>builder()
            .addAll(
                Set.of(
                    Pair.of("AA", ATTENDANCE_ALLOWANCE),
                    Pair.of("IS", INCOME_SUPPORT),
                    Pair.of("SF", SOCIAL_FUND),
                    Pair.of("PC", PENSION_CREDIT),
                    Pair.of("RP", RETIREMENT_PENSION),
                    Pair.of("BB", BEREAVEMENT_BENEFIT),
                    Pair.of("CA", CARERS_ALLOWANCE),
                    Pair.of("MA", MATERNITY_ALLOWANCE),
                    Pair.of("IIDB", IIDB),
                    Pair.of("BSPS", BEREAVEMENT_SUPPORT_PAYMENT_SCHEME),
                    Pair.of("Credit", UC),
                    Pair.of("IDB", INDUSTRIAL_DEATH_BENEFIT)
                ))
            .addAll(addBenefitShortNamesThatHaveAcronyms())
            .build();

    private static final Set<Pair<String, Benefit>> CONTAINS_STRING = Set.of(
        Pair.of("personal", PIP),
        Pair.of("independence", PIP),
        Pair.of("universal", UC),
        Pair.of("employment", ESA),
        Pair.of("attendance", ATTENDANCE_ALLOWANCE),
        Pair.of("disability", DLA),
        Pair.of("living", DLA),
        Pair.of("livi", DLA),
        Pair.of("livin", DLA),
        Pair.of("income", INCOME_SUPPORT),
        Pair.of("inco", INCOME_SUPPORT),
        Pair.of("incom", INCOME_SUPPORT),
        Pair.of("death", INDUSTRIAL_DEATH_BENEFIT),
        Pair.of("deat", INDUSTRIAL_DEATH_BENEFIT),
        Pair.of("retirement", RETIREMENT_PENSION),
        Pair.of("reti", RETIREMENT_PENSION),
        Pair.of("retir", RETIREMENT_PENSION),
        Pair.of("retire", RETIREMENT_PENSION),
        Pair.of("retirem", RETIREMENT_PENSION),
        Pair.of("retireme", RETIREMENT_PENSION),
        Pair.of("retiremen", RETIREMENT_PENSION),
        Pair.of("injuries", IIDB),
        Pair.of("disablement", IIDB),
        Pair.of("iidb", IIDB),
        Pair.of("job", JSA),
        Pair.of("seeker", JSA),
        Pair.of("seeker's", JSA),
        Pair.of("seekers", JSA),
        Pair.of("social", SOCIAL_FUND),
        Pair.of("fund", SOCIAL_FUND),
        Pair.of("carer's", CARERS_ALLOWANCE),
        Pair.of("care", CARERS_ALLOWANCE),
        Pair.of("carers", CARERS_ALLOWANCE),
        Pair.of("maternity", MATERNITY_ALLOWANCE),
        Pair.of("mate", MATERNITY_ALLOWANCE),
        Pair.of("mater", MATERNITY_ALLOWANCE),
        Pair.of("matern", MATERNITY_ALLOWANCE),
        Pair.of("materni", MATERNITY_ALLOWANCE),
        Pair.of("maternit", MATERNITY_ALLOWANCE),
        Pair.of("bsps", BEREAVEMENT_SUPPORT_PAYMENT_SCHEME)
    );

    private static final Set<Pair<String, Benefit>> FUZZY_CHOICES =
        ImmutableSet.<Pair<String, Benefit>>builder()
            .addAll(getBenefitShortNameAndDescriptionFuzzyChoices())
            .build()
            .stream()
            .filter(pair -> pair.getLeft().length() >= MAX_FUZZY_SEARCH_LENGTH)
            .collect(toUnmodifiableSet());

    public String matchBenefitType(String caseId, String ocrBenefitValue) {
        return wordExcludedFromFuzzySearch(caseId, ocrBenefitValue)
            .flatMap(code -> benefitByExactMatchOrFuzzySearch(caseId, code))
            .map(Benefit::getShortName)
            .orElse(ocrBenefitValue);
    }

    private Optional<Benefit> benefitByExactMatchOrFuzzySearch(String caseId, String code) {
        return benefitByExactMatchSearch(code)
            .or(() -> benefitByContainsString(caseId, code))
            .or(() -> benefitByFuzzySearch(caseId, code));
    }

    private Optional<Benefit> benefitByExactMatchSearch(String code) {
        return findBenefitByShortName(code)
            .or(() -> findBenefitByDescription(code))
            .or(() -> findBenefitByExactWord(code));
    }

    private Optional<? extends Benefit> benefitByContainsString(String caseId, String code) {
        List<Benefit> benefits = findBenefitsInTheContainsStringSet(code);
        Optional<Benefit> benefitOptional = (benefits.size() == 1) ? Optional.of(benefits.getFirst()) : Optional.empty();
        logInfoIfPresent(caseId, code, benefitOptional.orElse(null));
        logWarningIfMultipleContainsStringMatchesOnBenefits(caseId, code, benefits);
        return benefitOptional;
    }

    private void logInfoIfPresent(String caseId, String code, Benefit benefit) {
        if (!isNull(benefit)) {
            log.info("Search code {}, contains the word that matches the benefit {} for caseId {}", code, benefit.getShortName(), caseId);
        }
    }

    private void logWarningIfMultipleContainsStringMatchesOnBenefits(String caseId, String code, List<Benefit> benefits) {
        if (benefits.size() > 1) {
            log.warn("Search code {}, contains multiple matches to benefits {} for caseId {}", code, getBenefitNames(benefits), caseId);
        }
    }

    private List<Benefit> findBenefitsInTheContainsStringSet(String code) {
        return CONTAINS_STRING.stream()
            .filter(pair -> contains(lowerCase(code).split(" "), pair.getLeft()))
            .map(Pair::getRight)
            .distinct()
            .toList();
    }

    private String getBenefitNames(List<Benefit> benefits) {
        return benefits.stream().map(Benefit::getShortName).reduce("", (a, b) -> format("%s, %s", a, b));
    }

    private Optional<Benefit> benefitByFuzzySearch(String caseId, String code) {
        final BoundExtractedResult<Pair<String, Benefit>> result = runFuzzySearch(code);
        logMessage(caseId, code, result);
        return benefitBasedOnThreshold(result);
    }

    private BoundExtractedResult<Pair<String, Benefit>> runFuzzySearch(String code) {
        return FuzzySearch.extractOne(code, FUZZY_CHOICES, Pair::getLeft);
    }

    private Optional<String> wordExcludedFromFuzzySearch(String caseId, String code) {
        String searchCode = stripToEmpty(stripNonAlphaNumeric(code));
        boolean match = EXACT_WORDS_THAT_WILL_NOT_CAUSE_A_MATCH.contains(lowerCase(searchCode));
        logMessageIfExcludedFromFuzzySearch(caseId, code, match);
        return match ? empty() : Optional.of(searchCode);
    }

    private void logMessageIfExcludedFromFuzzySearch(String caseId, String code, boolean match) {
        if (match) {
            log.info("The word '{}' has matched the unknown word list. Cannot work out the benefit for caseId {}.", code, caseId);
        }
    }

    private static String stripNonAlphaNumeric(String code) {
        return replaceAll(code, "[^A-Za-z0-9 ]", "");
    }

    private Optional<Benefit> benefitBasedOnThreshold(BoundExtractedResult<Pair<String, Benefit>> extractedResult) {
        return (extractedResult.getScore() < THRESHOLD_MATCH_SCORE)
            ? empty() : Optional.of(extractedResult.getReferent().getRight());
    }

    private void logMessage(String caseId, String code, BoundExtractedResult<Pair<String, Benefit>> result) {
        log.info("Search code '{}' has a fuzzy match score of {} with '{}'. The threshold score is {}. {} the fuzzy match for caseId {}.",
            code, result.getScore(), result.getString(), THRESHOLD_MATCH_SCORE,
            result.getScore() < THRESHOLD_MATCH_SCORE ? "Not Using" : "Using", caseId);
    }

    private static Optional<Benefit> findBenefitByExactWord(String code) {
        return EXACT_WORDS_THAT_WILL_CAUSE_A_MATCH.stream()
            .filter(pair -> pair.getLeft().equalsIgnoreCase(code))
            .findFirst()
            .map(Pair::getRight);
    }

    private static Set<Pair<String, Benefit>> addBenefitShortNamesThatHaveAcronyms() {
        return stream(Benefit.values())
            .filter(Benefit::isHasAcronym)
            .map(b -> Pair.of(b.getShortName(), b))
            .collect(toUnmodifiableSet());
    }

    private static Set<Pair<String, Benefit>> getBenefitShortNameAndDescriptionFuzzyChoices() {
        return stream(values())
            .filter(e -> e.getSscsType().equals(SscsType.SSCS1))
            .flatMap(benefit -> Stream.of(
                Pair.of(rightPad(benefit.getShortName(), MAX_FUZZY_SEARCH_LENGTH), benefit),
                Pair.of(rightPad(benefit.getDescription(), MAX_FUZZY_SEARCH_LENGTH), benefit)
            ))
            .collect(toUnmodifiableSet());
    }

}
