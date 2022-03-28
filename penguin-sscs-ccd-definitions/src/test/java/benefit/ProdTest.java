package benefit;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.junit.Test;

public class ProdTest {
    ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    public void whenFixedListIdHasBeenRemovedShowWarningToDeveloper() throws Exception {

        List<FixedLists> fixedListOnPr = getFixedLists("./benefit/data/sheets/FixedLists");
        List<FixedLists> fixedListOnMaster = getFixedLists("../master/benefit/data/sheets/FixedLists");
        List<String> prIds = fixedListOnPr.stream().map(FixedLists::getId).distinct().collect(Collectors.toList());
        List<String> masterIds = fixedListOnMaster.stream().map(FixedLists::getId).distinct().collect(Collectors.toList());
        masterIds.removeAll(prIds);

        assertThat("Changes to fixed list Id's from Production cannot be done without a data migration", masterIds, is(List.of()));
    }

    private List<FixedLists> getFixedLists(String path) throws IOException {
        System.out.println("going to " + path);
        List<Path> nonprod = Files.walk(Paths.get(path))
                .filter(Files::isRegularFile)
                .filter(f -> !f.getFileName().toString().contains("nonprod"))
                .collect(Collectors.toList());
        return nonprod.stream()
                .flatMap(file -> Arrays.stream(toFixedLists(file)))
                .distinct()
                .collect(Collectors.toList());
    }

    private FixedLists[] toFixedLists(Path file) {
        try {
            String data = String.join("", Files.readAllLines(file));
            data = data.replace("${CCD_DEF_PIP_DECISION_NOTICE_QUESTIONS},  ${CCD_DEF_ESA_DECISION_NOTICE_QUESTIONS},  ${CCD_DEF_UC_DECISION_NOTICE_QUESTIONS},  ${CCD_DEF_LANGUAGES}, ", "");
            return mapper.readValue(data, FixedLists[].class);
        } catch (IOException e) {
            e.printStackTrace();
            return new FixedLists[0];
        }
    }

    @Getter
    @Setter
    @JsonIgnoreProperties
    @ToString
    static class FixedLists {
        public FixedLists() {
            // default constructor
        }

        @JsonProperty("ID")
        String id;
        @JsonProperty("ListElementCode")
        String listElementCode;
    }
}
