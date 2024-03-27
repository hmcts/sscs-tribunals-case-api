package uk.gov.hmcts.reform.sscs.tyanotifications.callback;

import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.SscsCaseDataWrapper;

public interface CallbackHandler {
    boolean canHandle(SscsCaseDataWrapper sscsCaseDataWrapper);

    void handle(SscsCaseDataWrapper sscsCaseDataWrapper);

    DispatchPriority getPriority();
}
