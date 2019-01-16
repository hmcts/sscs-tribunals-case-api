package uk.gov.hmcts.reform.sscs.service;

import static java.util.Collections.singletonList;
import static org.springframework.http.MediaType.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.document.DocumentUploadClientApi;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.pdf.ByteArrayMultipartFile;
import uk.gov.hmcts.reform.sscs.exception.UnsupportedDocumentTypeException;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
@Slf4j
public class RoboticsJsonUploadService {
    private static final String DM_STORE_USER_ID = "sscs";
    private static final String S2S_TOKEN = "oauth2Token";
    private static final String ROBOTICS_JSON_FILENAME = "robotics_json.txt";

    private final CcdService ccdService;
    private final DocumentUploadClientApi documentUploadClientApi;
    private final AuthTokenGenerator authTokenGenerator;

    @Autowired
    RoboticsJsonUploadService(
            DocumentUploadClientApi documentUploadClientApi,
            AuthTokenGenerator authTokenGenerator,
            CcdService ccdService) {

        this.documentUploadClientApi = documentUploadClientApi;
        this.authTokenGenerator = authTokenGenerator;
        this.ccdService = ccdService;
    }

    public void updateCaseWithRoboticsJson(
            JSONObject roboticsJson,
            SscsCaseData caseData,
            SscsCaseDetails caseDetails,
            IdamTokens idamTokens) {

        ByteArrayMultipartFile file = ByteArrayMultipartFile.builder()
                .content(roboticsJson.toString().getBytes())
                .name(ROBOTICS_JSON_FILENAME)
                .contentType(TEXT_PLAIN)
                .build();

        log.info("Uploading Robotics JSON for case {}", caseDetails.getId());
        UploadResponse uploadResponse = uploadRoboticsJson(singletonList(file));

        log.info("Attaching Robotics JSON to case {}", caseDetails.getId());
        SscsCaseData updatedCaseData = attachRoboticsJsonToCaseData(caseData, uploadResponse);

        try {
            ccdService.updateCase(updatedCaseData, caseDetails.getId(),
                    "attachRoboticsJson", "", "", idamTokens);
        } catch (Exception e) {
            log.info("Failed to update ccd case with Robotics JSON but carrying on [" + caseDetails.getId() + "] ["
                    + caseData.getCaseReference() + "]", e);
        }

    }

    public SscsCaseData attachRoboticsJsonToCaseData(SscsCaseData caseData, UploadResponse uploadResponse) {
        DocumentLink documentLink = getDocumentLink(uploadResponse);

        log.info("Robotics JSON Document for CCD Case {} uploaded to {}", caseData.getCcdCaseId(), documentLink);

        SscsDocument roboticsJsonDocument = getRoboticsJsonDocument(documentLink);

        log.info("Adding Robotics JSON document to CCD Case Id {}", caseData.getCcdCaseId());

        List<SscsDocument> sscsDocumentList = updateCaseDataDocuments(caseData, roboticsJsonDocument);
        caseData.setSscsDocument(sscsDocumentList);

        return caseData;
    }

    private SscsDocument getRoboticsJsonDocument(DocumentLink documentLink) {
        SscsDocumentDetails roboticsJsonDocumentDetails = SscsDocumentDetails.builder()
                .documentFileName(ROBOTICS_JSON_FILENAME)
                .documentDateAdded(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE))
                .documentLink(documentLink)
                .build();

        return new SscsDocument(roboticsJsonDocumentDetails);
    }

    private DocumentLink getDocumentLink(UploadResponse uploadResponse) {
        final String href = uploadResponse.getEmbedded().getDocuments().get(0).links.binary.href;
        return DocumentLink.builder().documentUrl(href).build();
    }

    private List<SscsDocument> updateCaseDataDocuments(SscsCaseData caseData, SscsDocument roboticsJsonDocument) {
        List<SscsDocument> sscsDocumentList = caseData.getSscsDocument();
        if (CollectionUtils.isEmpty(sscsDocumentList)) {
            sscsDocumentList = singletonList(roboticsJsonDocument);
        } else {
            sscsDocumentList.add(roboticsJsonDocument);
        }
        return sscsDocumentList;
    }

    public UploadResponse uploadRoboticsJson(List<MultipartFile> files) {

        String serviceAuthorization = authTokenGenerator.generate();

        try {
            return documentUploadClientApi
                    .upload(S2S_TOKEN, serviceAuthorization, DM_STORE_USER_ID, files);
        } catch (Exception e) {
            log.info("Doc Store service failed to upload Robotics JSON", e);
            return null;
        }
    }

}
