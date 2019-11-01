package uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued;

import static uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;


@Service
@Slf4j
public class DirectionIssuedAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_START
                && callback.getEvent() == EventType.DIRECTION_ISSUED
                && Objects.nonNull(callback.getCaseDetails())
                && Objects.nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        List<DynamicListItem> listOptions = new ArrayList<>();
        if (StringUtils.equalsAnyIgnoreCase(caseData.getDwpState(), "extensionRequested")) {
            listOptions.add(new DynamicListItem(GRANT_EXTENSION.getId(), GRANT_EXTENSION.getLabel()));
            listOptions.add(new DynamicListItem(DENY_EXTENSION.getId(), DENY_EXTENSION.getLabel()));
        } else {
            listOptions.add(new DynamicListItem(APPEAL_TO_PROCEED.getId(), APPEAL_TO_PROCEED.getLabel()));
            listOptions.add(new DynamicListItem(PROVIDE_INFORMATION.getId(), PROVIDE_INFORMATION.getLabel()));
        }

        caseData.setSelectDirectionType(new DynamicList(listOptions.get(0), listOptions));

        return new PreSubmitCallbackResponse<>(caseData);
    }

}
