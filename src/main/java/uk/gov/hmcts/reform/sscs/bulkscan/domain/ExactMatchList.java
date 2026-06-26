package uk.gov.hmcts.reform.sscs.bulkscan.domain;

import java.util.ArrayList;

public class ExactMatchList extends ArrayList<String> {
    @Override
    public boolean contains(Object o) {
        String paramStr = (String)o;
        for (String s : this) {
            if (paramStr.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }
}
