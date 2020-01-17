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
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
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

        caseData.setBundleConfiguration("sscs-demo-bundle.yaml");

        try {
            createBundle(caseData);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return preSubmitCallbackResponse;
    }

    public void createBundle(SscsCaseData caseData) throws IOException {
        final String json = jsonMapper.writeValueAsString(caseData);
        final RequestBody body = RequestBody.create(MediaType.get("application/json"), json);
        IdamTokens idamTokens = idamService.getIdamTokens();

        final Request request = new Request.Builder()
                .addHeader("ServiceAuthorization", idamTokens.getServiceAuthorization())
                .url("http://rpa-em-ccd-orchestrator:8060/api/new-bundle")
                .method("POST", body)
                .build();

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
    }
}
