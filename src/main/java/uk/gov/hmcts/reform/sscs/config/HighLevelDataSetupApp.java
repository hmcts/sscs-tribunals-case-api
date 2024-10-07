package uk.gov.hmcts.reform.sscs.config;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import uk.gov.hmcts.befta.dse.ccd.CcdEnvironment;
import uk.gov.hmcts.befta.dse.ccd.CcdRoleConfig;
import uk.gov.hmcts.befta.dse.ccd.DataLoaderToDefinitionStore;
import uk.gov.hmcts.befta.util.BeftaUtils;

public class HighLevelDataSetupApp extends DataLoaderToDefinitionStore {

    private static final Logger logger = LoggerFactory.getLogger(HighLevelDataSetupApp.class);

    private static final CcdRoleConfig[] CCD_ROLES = {
        new CcdRoleConfig("citizen", "PUBLIC"),
        new CcdRoleConfig("caseworker", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-systemupdate", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-anonymouscitizen", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-callagent", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-judge", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-judge-salaried", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-clerk", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-dwpresponsewriter", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-registrar", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-superuser", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-teamleader", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-panelmember", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-bulkscan", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-pcqextractor", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-hmrcresponsewriter", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-ibcaresponsewriter", "PUBLIC"),
        new CcdRoleConfig("caseworker-wa-task-configuration", "PUBLIC"),
        new CcdRoleConfig("caseworker-ras-validation", "PUBLIC"),
        new CcdRoleConfig("GS_profile", "PUBLIC")
    };

    private final CcdEnvironment environment;

    public HighLevelDataSetupApp(CcdEnvironment dataSetupEnvironment) {
        super(dataSetupEnvironment);
        environment = dataSetupEnvironment;
    }

    public static void main(String[] args) throws Throwable {
        DataLoaderToDefinitionStore.main(HighLevelDataSetupApp.class, args);
    }

    @Override
    public void addCcdRoles() {
        for (CcdRoleConfig roleConfig : CCD_ROLES) {
            try {
                logger.info("\n\nAdding CCD Role {}.", roleConfig);
                addCcdRole(roleConfig);
                logger.info("\n\nAdded CCD Role {}.", roleConfig);
            } catch (Exception e) {
                logger.error("\n\nCouldn't add CCD Role {} - Exception: {}.\n\n", roleConfig, e);

                throw e;
            }
        }
    }

    @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 1000))
    protected void doLoadTestData() {
        try {
            super.doLoadTestData();
        } catch (Exception e) {
            logger.error("\n\nCouldn't load test data - Exception: {}.\n\n", e);

            throw e;
        }
    }

    @Override
    protected List<String> getAllDefinitionFilesToLoadAt(String definitionsPath) {
        if (environment.name().toUpperCase().equals(CcdEnvironment.PREVIEW.name())) {
            return List.of(
                    String.format("definitions/bulkscan/CCD_BulkScanningDefinition_%s.xlsx", "PR"),
                    String.format("definitions/benefit/CCD_SSCSDefinition_%s.xlsx", "PR")
            );
        } else {
            return List.of(
                    String.format("definitions/benefit/CCD_SSCSDefinition_%s.xlsx", environment.name().toUpperCase())
            );
        }
    }

    @Override
    public void createRoleAssignments() {
        // Do not create role assignments.
        BeftaUtils.defaultLog("Will NOT create role assignments!");
    }

    @Override
    protected boolean shouldTolerateDataSetupFailure() {
        return false;
    }
}
