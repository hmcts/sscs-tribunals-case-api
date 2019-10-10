package uk.gov.hmcts.reform.sscs.service.conversion;

import static pl.touk.throwing.ThrowingFunction.unchecked;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import uk.gov.hmcts.reform.sscs.service.exceptions.FileToPdfConversionException;

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
            return files.stream().parallel().map(unchecked(this::convert)).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("cannot convert files to PDF.", e);
            throw new FileToPdfConversionException("Cannot convert files to PDF.", e);
        }
    }

    private MultipartFile convert(MultipartFile f) throws IOException {
        String mimeType = tika.detect(f.getInputStream(), new Metadata());

        File tempFile = File.createTempFile("tempConversion", FilenameUtils.getExtension(f.getOriginalFilename()));
        tempFile.deleteOnExit();
        f.transferTo(tempFile);
        Optional<File> fileOptional = convertFile(mimeType, tempFile);
        if (!fileOptional.isPresent()) {
            return f;
        }
        return getMultipartFile(f, fileOptional.get());
    }

    private MultipartFile getMultipartFile(MultipartFile f, File file) throws IOException {
        String newMimeType = tika.detect(file);

        String extension =  FilenameUtils.getExtension(file.getName());
        final String fileName = String.format("%s.%s", FilenameUtils.getBaseName(f.getOriginalFilename()), extension);

        final DiskFileItem diskFileItem = new DiskFileItem(fileName, newMimeType, false, fileName, (int) file.length(), file);
        // This silliness is to avoid a null pointer exception.
        diskFileItem.getOutputStream();

        return new CommonsMultipartFile(diskFileItem);
    }

    private Optional<File> convertFile(String mimeType, File f) {
        return converters.stream()
                .filter(g -> g.accepts().contains(mimeType))
                .findFirst()
                .map(unchecked(g -> g.convert(f)));
    }
}
