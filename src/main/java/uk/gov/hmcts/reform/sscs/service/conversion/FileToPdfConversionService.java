package uk.gov.hmcts.reform.sscs.service.conversion;

import static pl.touk.throwing.ThrowingFunction.unchecked;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import uk.gov.hmcts.reform.sscs.exception.FileToPdfConversionException;

@Slf4j
@Service
public class FileToPdfConversionService {

    private final List<FileToPdfConverter> converters;

    private final Tika tika = new Tika();

    @Autowired
    public FileToPdfConversionService(List<FileToPdfConverter> converters) {
        this.converters = converters;
    }

    public List<MultipartFile> convert(List<MultipartFile> files) {
        try {
            return files.stream().parallel().map(unchecked(this::convert)).toList();
        } catch (Exception e) {
            log.error("cannot convert files to PDF.", e);
            throw new FileToPdfConversionException("Cannot convert files to PDF.", e);
        }
    }

    private MultipartFile convert(MultipartFile f) throws IOException {
        TikaConfig config = TikaConfig.getDefaultConfig();
        Detector detector = config.getDetector();
        Metadata metadata = new Metadata();
        metadata.set(HttpHeaders.CONTENT_TYPE, f.getOriginalFilename());
        try (InputStream is = f.getInputStream()) {
            TikaInputStream stream = TikaInputStream.get(is);
            String mimeType = detector.detect(stream, metadata).getBaseType().toString();

            Optional<File> fileOptional = convertFile(mimeType, f);

            if (!fileOptional.isPresent()) {
                return f;
            }
            return getMultipartFile(f, fileOptional.get());
        }
    }

    private MultipartFile getMultipartFile(MultipartFile f, File file) throws IOException {
        TikaConfig config = TikaConfig.getDefaultConfig();
        Detector detector = config.getDetector();
        Metadata metadata = new Metadata();
        metadata.set(HttpHeaders.CONTENT_TYPE, f.getOriginalFilename());
        String newMimeType = detector.detect(TikaInputStream.get(file), metadata).getBaseType().toString();

        String extension =  FilenameUtils.getExtension(file.getName());
        final String fileName = String.format("%s.%s", FilenameUtils.getBaseName(f.getOriginalFilename()), extension);

        final DiskFileItem diskFileItem = new DiskFileItem(fileName, newMimeType, false, fileName, (int) file.length(), file.getParentFile());

        // This shoddy library doesn't work as it should.
        // See https://stackoverflow.com/questions/8978290/org-apache-commons-fileupload-disk-diskfileitem-is-not-created-properly
        try (InputStream input =  new FileInputStream(file)) {
            try (OutputStream os = diskFileItem.getOutputStream()) {
                IOUtils.copy(input, os);
            }
        }

        return new CommonsMultipartFile(diskFileItem);
    }

    private Optional<File> convertFile(String mimeType, MultipartFile f) {
        return converters.stream()
                .filter(g -> g.accepts().contains(mimeType))
                .findFirst()
                .map(unchecked(g -> g.convert(transferToFile(f))));
    }

    private File transferToFile(MultipartFile f) throws IOException {
        String suffix = String.format(".%s", FilenameUtils.getExtension(f.getOriginalFilename()));
        File tempFile = Files.createTempFile(Paths.get("").toAbsolutePath(), "tempConversion", suffix).toFile();
        tempFile.deleteOnExit();
        f.transferTo(tempFile);
        return tempFile;
    }
}
