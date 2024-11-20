package uk.gov.hmcts.reform.sscs.tyanotifications.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.boot.jdbc.SchemaManagement;
import org.springframework.boot.jdbc.SchemaManagementProvider;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcOperations;

@Configuration
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", matchIfMissing = true)
public class LegacyFlywayAutoConfiguration {

    @Bean
    @DependsOnDatabaseInitialization
    @Primary
    SchemaManagementProvider flywayDefaultDdlModeProvider(ObjectProvider<Flyway> flyways) {
        return new SchemaManagementProvider() {
            @Override
            public SchemaManagement getSchemaManagement(DataSource dataSource) {
                return SchemaManagement.MANAGED;
            }
        };
    }

    @Bean(initMethod = "migrate")
    Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure().baselineOnMigrate(true).dataSource(dataSource).load();
        flyway.migrate();
        return flyway;
    }

    @Bean
    FlywayMigrationInitializer flywayInitializer(Flyway flyway) {
        return new FlywayMigrationInitializer(flyway, null);
    }

    /**
     * Additional configuration to ensure that {@link JdbcOperations} beans depend
     * on the {@code flywayInitializer} bean.
     */
    @Configuration
    @ConditionalOnClass(JdbcOperations.class)
    @ConditionalOnBean(JdbcOperations.class)
    protected static class FlywayInitializerJdbcOperationsDependencyConfiguration implements BeanFactoryPostProcessor {

        @Override
        public void postProcessBeanFactory(@NotNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
            if (beanFactory instanceof BeanDefinitionRegistry registry) {
                String[] beanNamesForType = beanFactory.getBeanNamesForType(JdbcOperations.class, false, false);
                for (String beanName : beanNamesForType) {
                    RootBeanDefinition beanDefinition = (RootBeanDefinition) registry.getBeanDefinition(beanName);
                    beanDefinition.setDependsOn("flywayInitializer");
                }
            }
        }
    }
}
