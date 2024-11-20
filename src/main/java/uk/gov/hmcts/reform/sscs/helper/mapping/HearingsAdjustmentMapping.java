package uk.gov.hmcts.reform.sscs.helper.mapping;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.Adjustment.HEARING_LOOP;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.Adjustment.SIGN_LANGUAGE_INTERPRETER;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.Adjustment.STEP_FREE_WHEELCHAIR_ACCESS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.exception.InvalidMappingException;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.Adjustment;

public final class HearingsAdjustmentMapping {

    public static final String CCD_SIGN_LANGUAGE_INTERPRETER = "signLanguageInterpreter";
    public static final String CCD_HEARING_LOOP = "hearingLoop";
    public static final String CCD_DISABLED_ACCESS = "disabledAccess";

    private HearingsAdjustmentMapping() {

    }

    public static List<Adjustment> getIndividualsAdjustments(HearingOptions hearingOptions) throws InvalidMappingException {
        if (isNull(hearingOptions)) {
            return Collections.emptyList();
        }
        return getAdjustments(hearingOptions.getArrangements());
    }

    public static List<Adjustment> getAdjustments(List<String> ccdAdjustments) throws InvalidMappingException {
        List<Adjustment> adjustments = new ArrayList<>();
        if (isEmpty(ccdAdjustments)) {
            return adjustments;
        }

        for (String ccdAdjustment : ccdAdjustments) {
            Adjustment adjustment = getAdjustment(ccdAdjustment);
            adjustments.add(adjustment);
        }
        return adjustments;
    }

    public static Adjustment getAdjustment(String ccdAdjustment) throws InvalidMappingException {
        return switch (ccdAdjustment) {
            case CCD_SIGN_LANGUAGE_INTERPRETER -> SIGN_LANGUAGE_INTERPRETER;
            case CCD_HEARING_LOOP -> HEARING_LOOP;
            case CCD_DISABLED_ACCESS -> STEP_FREE_WHEELCHAIR_ACCESS;
            default -> throw new InvalidMappingException("The adjustment '%s' given cannot be mapped".formatted(
                ccdAdjustment
            ));
        };
    }
}
