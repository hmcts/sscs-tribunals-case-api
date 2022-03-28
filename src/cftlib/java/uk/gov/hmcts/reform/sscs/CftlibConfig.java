package uk.gov.hmcts.reform.sscs;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.rse.ccd.lib.api.CFTLib;
import uk.gov.hmcts.rse.ccd.lib.api.CFTLibConfigurer;

@Component
public class CftlibConfig implements CFTLibConfigurer {

    @Override
    public void configure(CFTLib lib) throws Exception {
        lib.createIdamUser("a@b.com",
                "caseworker",
                "caseworker-sscs",
                "caseworker-sscs-superuser",
                "caseworker-sscs-clerk",
                "caseworker-sscs-systemupdate",
                "caseworker-sscs-judge",
                "caseworker-sscs-dwpresponsewriter",
                "caseworker-sscs-registrar"
        );
        lib.createRoles(
                "caseworker-sscs-superuser",
                "caseworker-sscs-clerk",
                "caseworker-sscs-systemupdate",
                "caseworker-sscs-judge",
                "caseworker-sscs-dwpresponsewriter",
                "caseworker-sscs-registrar",
                "caseworker-sscs-callagent",
                "caseworker-sscs-teamleader",
                "caseworker-sscs-panelmember",
                "caseworker-sscs-bulkscan",
                "caseworker-sscs-anonymouscitizen",
                "caseworker-sscs-pcqextractor",
                "citizen",
                "caseworker-sscs"
        );
        var def = Files.readAllBytes(Path.of("sscs-ccd-definitions/releases/CCD_SSCSDefinition_vdev_PROD.xlsx"));
        lib.importDefinition(def);
    }
}
