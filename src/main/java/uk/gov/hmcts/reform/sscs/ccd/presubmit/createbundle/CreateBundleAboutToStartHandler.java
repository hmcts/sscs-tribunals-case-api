package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DecisionType.STRIKE_OUT;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.EmailService;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.SscsPdfService;

@Service
@Slf4j
public class CreateBundleAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse;

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final IdamService idamService;
    private final OkHttpClient http;

    @Autowired
    public CreateBundleAboutToStartHandler(IdamService idamService, OkHttpClient http) {
        this.idamService = idamService;
        this.http = http;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_START
                && callback.getEvent() == EventType.CREATE_BUNDLE
                && Objects.nonNull(callback.getCaseDetails())
                && Objects.nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {

        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);

//        List<Bundle> bundles = new ArrayList<>();
//        caseData.setCaseBundles();

        caseData.setBundleConfiguration("sscs-demo-bundle.yaml");

//        try {
//            createBundle(callback);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        return preSubmitCallbackResponse;
    }

    public void createBundle(Callback<SscsCaseData> callback) throws IOException {

//        SscsBundlingDto sscsBundlingDto = createSscsBundlingDto(callback);
//        callback.getCaseDetails().getCaseData().setBundleConfiguration("sscs-demo-bundle.yaml");

        //WORKS: jsonMapper.writeValueAsString(callback.getCaseDetails())

//        final String json = jsonMapper.writeValueAsString(sscsBundlingDto);
        final String json2 = jsonMapper.writeValueAsString(callback.getCaseDetails());
        final RequestBody body = RequestBody.create(MediaType.get("application/json"), json2);
        IdamTokens idamTokens = idamService.getIdamTokens();

        final Request request = new Request.Builder()
                .addHeader("Authorization", idamTokens.getIdamOauth2Token())
                .addHeader("ServiceAuthorization", idamTokens.getServiceAuthorization())
                .url("http://localhost:4623/api/new-bundle")
                .post(body)
                .build();

        try {
            final Response response = http.newCall(request).execute();

        if (response.isSuccessful()) {
            System.out.println("Success");
//            return jsonMapper.readValue(response.body().byteStream(), DocumentTaskDTO.class);
//
//        } else {
//            throw new IOException("Unable to create bundle task: " + response.body().string());
        } else {
            System.out.println("Fail");

        }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    private Bundle bundleBuilder(Callback<SscsCaseData> callback) {
//        DwpResponseDocument responseDocument = callback.getCaseDetails().getCaseData().getDwpResponseDocument();
//        DwpResponseDocument evidenceDocument = callback.getCaseDetails().getCaseData().getDwpEvidenceBundleDocument();
//
//        List<BundleDocument> bundleDocuments = new ArrayList<>();
//        BundleDocument bundledResponse = BundleDocument.builder().value(BundleDocumentDetails.builder()
//                .name("Dwp response document").description("The description of dwp response document").sourceDocument(responseDocument.getDocumentLink()).sortIndex(0).build()).build();
//
//        BundleDocument bundledEvidence = BundleDocument.builder().value(BundleDocumentDetails.builder()
//                .name("Dwp evidence document").description("The description of dwp evidence document").sourceDocument(responseDocument.getDocumentLink()).sortIndex(1).build()).build();
//
//        bundleDocuments.add(bundledResponse);
//        bundleDocuments.add(bundledEvidence);
//
//        List<SscsDocument> sscsDocuments = callback.getCaseDetails().getCaseData().getSscsDocument();
//
//        for (SscsDocument doc : sscsDocuments) {
//            if (doc.getValue().getBundleAddition() != null) {
//
//                BundleDocument additionalDoc = BundleDocument.builder().value(BundleDocumentDetails.builder()
//                        .name(doc.getValue().getDocumentFileName()).description("Additional description").sourceDocument(doc.getValue().getDocumentLink()).sortIndex(sscsDocuments.size()).build()).build();
//
//                bundleDocuments.add(additionalDoc);
//            }
//        }
//
//        return Bundle.builder().value(
//                BundleDetails.builder().documents(bundleDocuments).build()).build();
//    }

//    private SscsBundlingDto createSscsBundlingDto(Callback<SscsCaseData> callback) {
//        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
//
//        SscsBundlingDto dto = SscsBundlingDto.builder()
//                .caseData(
//                        SscsBundlingDto2.builder()
//                        .bundleConfiguration("sscs-demo-bundle.yaml")
//                        .sscsDocument(convertDocs(caseData.getSscsDocument())).build()).build();
//
//        return dto;
//    }

//    private List<SscsDocumentBundlingDto> convertDocs(List<SscsDocument> sscsDocument) {
//        List<SscsDocumentBundlingDto> documents = new ArrayList<>();
//        for (SscsDocument doc : sscsDocument) {
//            SscsDocumentBundlingDto docDto =
//                    SscsDocumentBundlingDto.builder()
//                    .documentLink(doc.getValue().getDocumentLink())
//                            .documentName(doc.getValue().getDocumentFileName())
//                            .createdDatetime(doc.getValue().getDocumentDateAdded()).build();
//
//            documents.add(docDto);
//        }
//        return documents;
//    }
}
