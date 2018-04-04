package uk.gov.hmcts.sscs.model.ccd;

import static org.slf4j.LoggerFactory.getLogger;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import uk.gov.hmcts.sscs.exception.ApplicationErrorException;

public class CcdUtil {

    private static final Logger LOG = getLogger(CcdUtil.class);

    private CcdUtil() {

    }

    public static CaseData getCaseData(Object object) {

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        try {
            return mapper.convertValue(object, CaseData.class);
        } catch (Exception e) {
            ApplicationErrorException applicationErrorException = new ApplicationErrorException(e);
            LOG.error("Error occurred when CaseDetails are mapped into CaseData", applicationErrorException);
            throw applicationErrorException;
        }
    }
}
