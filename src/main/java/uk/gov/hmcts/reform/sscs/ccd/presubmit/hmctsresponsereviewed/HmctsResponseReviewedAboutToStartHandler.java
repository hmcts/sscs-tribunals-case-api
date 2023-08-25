package uk.gov.hmcts.reform.sscs.ccd.presubmit.hmctsresponsereviewed;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.APPENDIX_12;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.AT_38;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.DWP_EVIDENCE_BUNDLE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.DWP_RESPONSE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.UCB;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase.REVIEW_BY_TCW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.model.dwp.OfficeMapping;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;

@Service
public class HmctsResponseReviewedAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final DwpAddressLookupService service;

    @Autowired
    public HmctsResponseReviewedAboutToStartHandler(DwpAddressLookupService service) {
        this.service = service;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
            && callback.getEvent() == EventType.HMCTS_RESPONSE_REVIEWED;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        setOfficeDropdowns(sscsCaseData);
        setDefaultFieldValues(sscsCaseData);
        setDwpDocuments(sscsCaseData);
        setSelectWhoReviewsCase(sscsCaseData);

        final PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        if (sscsCaseData.getCreatedInGapsFrom() == null || !sscsCaseData.getCreatedInGapsFrom().equals("readyToList")) {
            preSubmitCallbackResponse.addError("This event cannot be run for cases created in GAPS at valid appeal");
        }

        return preSubmitCallbackResponse;
    }

    protected void setDwpDocuments(SscsCaseData sscsCaseData) {
        if (isNotEmpty(sscsCaseData.getDwpDocuments())) {

            findDwpDocument(DWP_RESPONSE, sscsCaseData.getDwpDocuments().stream()).ifPresent(d -> {
                sscsCaseData.setDwpResponseDocument(new DwpResponseDocument(d.getValue().getDocumentLink(), d.getValue().getDocumentFileName()));
                if (d.getValue().getEditedDocumentLink() != null) {
                    sscsCaseData.setDwpEditedResponseDocument(new DwpResponseDocument(d.getValue().getEditedDocumentLink(), d.getValue().getDocumentFileName()));
                }
            });

            findDwpDocument(DWP_EVIDENCE_BUNDLE, sscsCaseData.getDwpDocuments().stream()).ifPresent(d -> {
                sscsCaseData.setDwpEvidenceBundleDocument(new DwpResponseDocument(d.getValue().getDocumentLink(), d.getValue().getDocumentFileName()));
                if (d.getValue().getEditedDocumentLink() != null) {
                    sscsCaseData.setDwpEditedEvidenceBundleDocument(new DwpResponseDocument(d.getValue().getEditedDocumentLink(), d.getValue().getDocumentFileName()));
                }
            });

            findDwpDocument(AT_38, sscsCaseData.getDwpDocuments().stream())
                    .ifPresent(d -> sscsCaseData.setDwpAT38Document(new DwpResponseDocument(d.getValue().getDocumentLink(), d.getValue().getDocumentFileName())));

            findDwpDocument(APPENDIX_12, sscsCaseData.getDwpDocuments().stream())
                    .ifPresent(d -> sscsCaseData.setAppendix12Doc(new DwpResponseDocument(d.getValue().getDocumentLink(), d.getValue().getDocumentFileName())));

            findDwpDocument(UCB, sscsCaseData.getDwpDocuments().stream())
                    .ifPresent(d -> sscsCaseData.setDwpUcbEvidenceDocument(d.getValue().getDocumentLink()));
        }
    }

    public void setOfficeDropdowns(SscsCaseData sscsCaseData) {

        Optional<OfficeMapping> selectedOfficeMapping = sscsCaseData.getAppeal().getMrnDetails() != null && sscsCaseData.getAppeal().getMrnDetails().getDwpIssuingOffice() != null && sscsCaseData.getAppeal().getBenefitType() != null
                ? service.getDwpMappingByOffice(sscsCaseData.getAppeal().getBenefitType().getCode(), sscsCaseData.getAppeal().getMrnDetails().getDwpIssuingOffice()) : Optional.empty();

        OfficeMapping[] offices = service.getDwpOfficeMappings(sscsCaseData.getAppeal().getBenefitType().getCode());
        List<DynamicListItem> listOptions = Arrays.stream(offices)
                .map(office -> new DynamicListItem(office.getMapping().getCcd(), office.getMapping().getCcd()))
                .toList();

        DynamicListItem selectedDynamicListItem = selectedOfficeMapping
                .flatMap(selectedOffice -> listOptions.stream().filter(office -> office.getCode().equals(selectedOffice.getMapping().getCcd())).findFirst()).orElse(new DynamicListItem(null, null));

        if (sscsCaseData.getDwpOriginatingOffice() == null) {
            sscsCaseData.setDwpOriginatingOffice(new DynamicList(selectedDynamicListItem, listOptions));
        }
        if (sscsCaseData.getDwpPresentingOffice() == null) {
            sscsCaseData.setDwpPresentingOffice(new DynamicList(selectedDynamicListItem, listOptions));
        }
    }

    public void setDefaultFieldValues(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getDwpIsOfficerAttending() == null) {
            sscsCaseData.setDwpIsOfficerAttending("No");
        }
        if (sscsCaseData.getDwpUcb() == null) {
            sscsCaseData.setDwpUcb("No");
        }
        if (sscsCaseData.getDwpPhme() == null) {
            sscsCaseData.setDwpPhme("No");
        }
        if (sscsCaseData.getDwpComplexAppeal() == null) {
            sscsCaseData.setDwpComplexAppeal("No");
        }
    }

    private Optional<DwpDocument> findDwpDocument(DwpDocumentType dwpDocumentType, Stream<DwpDocument> stream) {
        return stream.filter(d -> d.getValue().getDocumentType().equals(dwpDocumentType.getValue())).findFirst();
    }

    private void setSelectWhoReviewsCase(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(REVIEW_BY_TCW.getId(), REVIEW_BY_TCW.getLabel()));
        listOptions.add(new DynamicListItem(REVIEW_BY_JUDGE.getId(), REVIEW_BY_JUDGE.getLabel()));

        sscsCaseData.setSelectWhoReviewsCase(new DynamicList(new DynamicListItem("", ""), listOptions));
    }

}
