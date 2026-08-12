package uk.gov.hmcts.reform.sscs.service.conversion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import org.apache.tika.Tika;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
public class FileToPdfConversionServiceTest {

    @Mock
    private FileToPdfConverter pdfConverter;
    private final Tika tika = new Tika();
    private final ImageConverter imageConverter = new ImageConverter();

    private FileToPdfConversionService conversionService;

    @BeforeEach
    public void setup() {
        conversionService = new FileToPdfConversionService(
                Lists.newArrayList(pdfConverter)
        );
    }

    @Test
    public void noHandler() throws IOException {
        when(pdfConverter.accepts()).thenReturn(Collections.emptyList());

        MultipartFile mpf = mock(MultipartFile.class);
        InputStream inputStream = mock(InputStream.class);
        List<MultipartFile> input = Lists.newArrayList(mpf);
        when(mpf.getOriginalFilename()).thenReturn("flying-pig.jpg");
        when(mpf.getInputStream()).thenReturn(inputStream);
        final List<MultipartFile> convert = conversionService.convert(input);
        assertEquals(input, convert);
    }

    @Test
    public void converterFound() throws IOException {
        File inputFile = new File(ClassLoader.getSystemResource("flying-pig.jpg").getPath());
        final String contentType = tika.detect(inputFile);
        File expected = imageConverter.convert(inputFile);
        when(pdfConverter.accepts()).thenReturn(Lists.newArrayList(contentType));
        when(pdfConverter.convert(any())).thenReturn(expected);
        MultipartFile mpf = mock(MultipartFile.class);
        when(mpf.getInputStream()).thenReturn(new FileInputStream(inputFile));
        when(mpf.getOriginalFilename()).thenReturn("flying-pig.jpg");
        List<MultipartFile> input = Lists.newArrayList(mpf);
        final List<MultipartFile> convert = conversionService.convert(input);
        assertEquals("flying-pig.pdf", convert.getFirst().getName());
    }

    @Test
    public void fluffTestsToKeepSonarHappyDuringAPainfulSpringBoot3Upgrade() throws IOException {
        File inputFile = new File(ClassLoader.getSystemResource("flying-pig.jpg").getPath());
        final String contentType = tika.detect(inputFile);
        File expected = imageConverter.convert(inputFile);
        when(pdfConverter.accepts()).thenReturn(Lists.newArrayList(contentType));
        when(pdfConverter.convert(any())).thenReturn(expected);
        MultipartFile mpf = mock(MultipartFile.class);
        when(mpf.getInputStream()).thenReturn(new FileInputStream(inputFile));
        when(mpf.getOriginalFilename()).thenReturn("flying-pig.jpg");
        List<MultipartFile> input = Lists.newArrayList(mpf);
        final List<MultipartFile> convert = conversionService.convert(input);

        assertEquals("application/pdf", convert.getFirst().getContentType());
        assertNotNull(convert.getFirst().getInputStream());
        assertEquals("flying-pig.pdf", convert.getFirst().getOriginalFilename());
        assertNotNull(convert.getFirst().getResource());
        assertFalse(convert.getFirst().isEmpty());
        assertEquals(33491, convert.getFirst().getSize());
        assertEquals(33491, convert.getFirst().getBytes().length);

        File tempFile = File.createTempFile("tempConversion", ".jpg");
        tempFile.deleteOnExit();
        convert.getFirst().transferTo(tempFile);
    }
}
