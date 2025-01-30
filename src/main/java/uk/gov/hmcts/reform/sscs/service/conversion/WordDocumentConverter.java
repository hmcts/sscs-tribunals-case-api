package uk.gov.hmcts.reform.sscs.service.conversion;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WordDocumentConverter implements FileToPdfConverter {
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private final String endpoint;
    private final String accessKey;
    private final OkHttpClient httpClient;

    @Autowired
    public WordDocumentConverter(OkHttpClient httpClient,
                                 @Value("${docmosis.convert.endpoint}") String endpoint,
                                 @Value("${docmosis.accessKey}") String accessKey) {
        this.httpClient = httpClient;
        this.endpoint = endpoint;
        this.accessKey = accessKey;
    }

    @Override
    public List<String> accepts() {
        return Lists.newArrayList(
                "text/plain",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/x-tika-ooxml",
                "application/x-tika-msoffice"
        );
    }

    @Override
    public File convert(File file) throws IOException {
        final String convertedFileName = String.format("%s.pdf", FilenameUtils.getBaseName(file.getName()));
        final String originalFileName = file.getName();

        MultipartBody requestBody = new MultipartBody
                .Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("accessKey", accessKey)
                .addFormDataPart("outputName", convertedFileName)
                .addFormDataPart("file", originalFileName, RequestBody.create(file, MediaType.get(PDF_CONTENT_TYPE)))
                .build();

        final Request request = new Request.Builder()
                .header("Accept", PDF_CONTENT_TYPE)
                .url(endpoint)
                .method("POST", requestBody)
                .build();

        final Response response = httpClient.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException(String.format("Docmosis error (%s) converting: %s", response.code(), file.getName()));
        }

        final File convertedFile = Files.createTempFile(Paths.get("").toAbsolutePath(), "stitch-conversion", ".pdf").toFile();
        convertedFile.deleteOnExit();
        Files.write(convertedFile.toPath(), Objects.requireNonNull(response.body()).bytes());

        return convertedFile;
    }
}
