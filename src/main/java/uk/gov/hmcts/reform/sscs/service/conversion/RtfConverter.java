package uk.gov.hmcts.reform.sscs.service.conversion;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

@Slf4j
@Component
public class RtfConverter extends WordDocumentConverter implements FileToPdfConverter {

    @Autowired
    public RtfConverter(OkHttpClient httpClient,
                                 @Value("${docmosis.convert.endpoint}") String endpoint,
                                 @Value("${docmosis.accessKey}") String accessKey) {
        super(httpClient, endpoint, accessKey);
    }

    public List<String> accepts() {
        return Lists.newArrayList(
                "text/rtf",
                "application/rtf"
        );
    }

    @Override
    public File convert(File file) throws IOException {
        BodyContentHandler handler = new BodyContentHandler();
        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        try (InputStream stream = new FileInputStream(file)) {
            parser.parse(stream, handler, metadata);
            String content = handler.toString();

            File textFile = Files.createTempFile(FilenameUtils.getBaseName(file.getName()), ".txt").toFile();
            textFile.deleteOnExit();

            FileWriter writer = new FileWriter(textFile);
            writer.write(content);
            writer.close();

            return super.convert(textFile);
        } catch (TikaException e) {
            throw new IOException(e);
        } catch (SAXException e) {
            throw new IOException(e);
        }
    }
}
