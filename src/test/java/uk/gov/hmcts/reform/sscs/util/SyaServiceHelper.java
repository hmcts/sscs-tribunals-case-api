package uk.gov.hmcts.reform.sscs.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;

public final class SyaServiceHelper {

    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private SyaServiceHelper() {
    }

    public static SyaCaseWrapper getSyaCaseWrapper() {
        URL resource = SyaServiceHelper.class.getClassLoader().getResource("json/sya.json");
        try {
            return mapper.readValue(Objects.requireNonNull(resource), SyaCaseWrapper.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static SyaCaseWrapper getSyaCaseWrapper(String name) {
        URL resource = SyaServiceHelper.class.getClassLoader().getResource(name);
        try {
            return mapper.readValue(resource, SyaCaseWrapper.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static RegionalProcessingCenter getRegionalProcessingCenter() {
        URL resource = SyaServiceHelper.class.getClassLoader().getResource("json/rpc.json");
        try {
            return mapper.readValue(Objects.requireNonNull(resource), RegionalProcessingCenter.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String asJsonString(final Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
