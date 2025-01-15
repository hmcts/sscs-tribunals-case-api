package uk.gov.hmcts.reform.sscs.helper;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;

@Service
public class EmailHelper {

    private static final String ID_FORMAT = "%s_%s";

    public String generateUniqueEmailId(Appellant appellant) {
        String appellantLastName = appellant.getName().getLastName();
        String nino = appellant.getIdentity().getNino();
        return String.format(ID_FORMAT, appellantLastName, nino.substring(nino.length() - 3));
    }
}
