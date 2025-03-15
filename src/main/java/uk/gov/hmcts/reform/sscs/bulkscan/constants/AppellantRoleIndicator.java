package uk.gov.hmcts.reform.sscs.bulkscan.constants;

import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.IS_ANOTHER_PARTY;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.IS_PAYING_PARENT;
import static uk.gov.hmcts.reform.sscs.bulkscan.constants.SscsConstants.IS_RECEIVING_PARENT;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Getter;
import uk.gov.hmcts.reform.sscs.ccd.domain.AppellantRole;


@Getter
public enum AppellantRoleIndicator {
    PAYING_PARENT(IS_PAYING_PARENT, AppellantRole.PAYING_PARENT),
    RECEIVING_PARENT(IS_RECEIVING_PARENT, AppellantRole.RECEIVING_PARENT),
    OTHER(IS_ANOTHER_PARTY, AppellantRole.OTHER);

    private final String indicatorString;
    private final AppellantRole role;

    AppellantRoleIndicator(String indicatorString, AppellantRole role) {
        this.indicatorString = indicatorString;
        this.role = role;
    }

    public static List<String> getAllIndicatorStrings() {
        return Arrays.stream(values()).map(AppellantRoleIndicator::getIndicatorString).collect(Collectors.toList());
    }

    public static Optional<AppellantRole> findByIndicatorString(String indicatorString) {
        return  Arrays.stream(values()).filter(v -> v.getIndicatorString().equals(indicatorString)).map(AppellantRoleIndicator::getRole).findFirst();
    }
}
