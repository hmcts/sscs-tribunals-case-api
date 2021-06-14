package uk.gov.hmcts.reform.sscs.ccd.util;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class ObjectMapperTestUtil {

    private ObjectMapperTestUtil() {
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static <T> T convertStringToObject(String data, Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(data, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String convertObjectToJsonString(final Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (Exception e) {
            System.out.println(" Error message is +++++ " + e.getLocalizedMessage());
            e.printStackTrace();

            throw new RuntimeException(e);
        }
    }


}
