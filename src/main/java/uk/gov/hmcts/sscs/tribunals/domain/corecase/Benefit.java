package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum Benefit {

    UNIVERSAL_CREDIT("001", "Universal Credit"),
    PIP("002" ,"PIP"),
    INCOME_SUPPORT("077","Income Support"),
    CARERS_ALLOWANCE("070","Carers Allowance"),
    JOB_SEEKERS_ALLOWANCE("073", "Job Seekers Allowance");

    private final String code;
    private final String description;
    private static final Map<String, String> mMap = Collections.unmodifiableMap(initializeMapping());

    Benefit(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }


    public static String getDescriptionByCode(String code) {
        if (mMap == null) {
            initializeMapping();
        }
        if (mMap.containsKey(code)) {
            return mMap.get(code);
        }
        return null;
    }

    private static Map<String, String> initializeMapping() {
        Map<String, String> mMap = new HashMap<>();
        for (Benefit s : Benefit.values()) {
            mMap.put(s.code, s.description);
        }
        return mMap;
    }
}
