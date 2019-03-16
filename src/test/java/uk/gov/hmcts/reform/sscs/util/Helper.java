package uk.gov.hmcts.reform.sscs.util;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class Helper {
    private Helper() {

    }

    public static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
