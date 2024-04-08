package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isNoOrNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public class HandlerUtils {
    private HandlerUtils() {
        // empty
    }

    public static boolean isANewJointParty(Callback<SscsCaseData> callback, SscsCaseData caseData) {
        boolean wasNotAlreadyJointParty = false;
        CaseDetails oldCaseDetails = callback.getCaseDetailsBefore().orElse(null);
        if (oldCaseDetails != null) {
            SscsCaseData oldCaseData = (SscsCaseData) oldCaseDetails.getCaseData();
            if (isNoOrNull(oldCaseData.getJointParty().getHasJointParty())) {
                wasNotAlreadyJointParty = true;
            }
        } else {
            wasNotAlreadyJointParty = true;
        }
        return wasNotAlreadyJointParty && isYes(caseData.getJointParty().getHasJointParty());
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
}
