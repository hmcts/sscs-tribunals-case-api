package uk.gov.hmcts.reform.sscs.bulkscan.helper;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.bulkscan.validators.PostcodeValidator;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

@Component
@RequiredArgsConstructor
public class AppealPostcodeHelper {

    private final PostcodeValidator postcodeValidator;

    public String resolvePostCodeOrPort(Appellant appellant) {

        if (appellant == null || appellant.getAddress() == null) {
            return StringUtils.EMPTY;
        }

        if (YesNo.NO.equals(appellant.getAddress().getInMainlandUk())) {
            return appellant.getAddress().getPortOfEntry() == null ? StringUtils.EMPTY : appellant.getAddress().getPortOfEntry();
        }

        return Optional.ofNullable(appellant.getAppointee())
            .map(Appointee::getAddress)
            .map(Address::getPostcode)
            .filter(this::isValidPostcode)
            .orElse(Optional.ofNullable(appellant.getAddress())
                .map(Address::getPostcode)
                .filter(this::isValidPostcode)
                .orElse(StringUtils.EMPTY));
    }

    private boolean isValidPostcode(String postcode) {
        return postcodeValidator.isValidPostcodeFormat(postcode)
            && postcodeValidator.isValid(postcode);
    }

}
