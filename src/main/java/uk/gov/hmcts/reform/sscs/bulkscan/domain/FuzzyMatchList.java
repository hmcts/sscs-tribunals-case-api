package uk.gov.hmcts.reform.sscs.bulkscan.domain;

import java.util.ArrayList;

public class FuzzyMatchList extends ArrayList<String> {
    @Override
    public boolean contains(Object o) {
        String paramStr = (String)o;
        for (String s : this) {
            if (paramStr.equalsIgnoreCase(s) || paramStr.toLowerCase().contains(s.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
