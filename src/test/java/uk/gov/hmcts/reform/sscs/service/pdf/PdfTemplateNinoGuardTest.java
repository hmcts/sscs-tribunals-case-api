package uk.gov.hmcts.reform.sscs.service.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Every NINO render in a PDF template must sit inside a hideNino guard.
 *
 * <p>The Welsh templates are hand-duplicated markup rather than a translation lookup, so each
 * guard has to be added twice. Three renders had escaped: the Welsh half of
 * evidenceDescriptionWelsh, the English half of personalStatementWelsh, and the only render in
 * onlineHearingSummary. A spot-check would not have found them, because in each case a sibling
 * render in the same file was correctly guarded.
 *
 * <p>This walks the guard structure rather than counting, so a guard around some unrelated block
 * cannot satisfy it.
 */
class PdfTemplateNinoGuardTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");
    private static final String NINO_RENDER = "pdfSummary.appealDetails.nino";
    private static final String GUARD_SUBJECT = "hideNino";

    @Test
    void everyNinoRenderSitsInsideAHideNinoGuard() throws IOException {
        List<String> unguarded = new ArrayList<>();
        int rendersChecked = 0;
        int templatesScanned = 0;

        try (Stream<Path> templates = Files.list(TEMPLATES)) {
            for (Path template : templates.filter(p -> p.toString().endsWith(".html")).toList()) {
                templatesScanned++;
                List<String> lines = Files.readAllLines(template);
                Deque<Boolean> openGuards = new ArrayDeque<>();

                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);

                    if (line.contains("{% if ")) {
                        openGuards.push(line.contains(GUARD_SUBJECT));
                    }

                    if (line.contains(NINO_RENDER) && !line.contains("{% if ")) {
                        rendersChecked++;
                        if (!openGuards.contains(Boolean.TRUE)) {
                            unguarded.add(template.getFileName() + " line " + (i + 1));
                        }
                    }

                    if (line.contains("{% endif %}") && !openGuards.isEmpty()) {
                        openGuards.pop();
                    }
                }
            }
        }

        // Without these the test passes when the templates are renamed or moved, which is the
        // failure mode a guard test is least able to notice about itself.
        assertThat(templatesScanned)
                .as("no templates found under %s - has the directory moved?", TEMPLATES)
                .isPositive();
        assertThat(rendersChecked)
                .as("no NINO renders found in any template - has the field been renamed?")
                .isPositive();

        assertThat(unguarded)
                .as("NINO rendered outside a hideNino guard, so it prints for child-support appeals")
                .isEmpty();
    }
}
