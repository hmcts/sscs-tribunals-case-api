package uk.gov.hmcts.reform.sscs.functional.utilities.idam.annotations;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.sscs.functional.utilities.idam.LocalIdamService;
import uk.gov.hmcts.reform.sscs.functional.utilities.idam.model.User;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

public final class WithIdamUsersExtension implements BeforeAllCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NS =
        ExtensionContext.Namespace.create(WithIdamUsersExtension.class);

    private static final String STORE_USERS = "usersByEmail";

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        WithIdamUsers ann = context.getRequiredTestClass().getAnnotation(WithIdamUsers.class);
        if (ann == null) return;

        // Requires Spring test context to be active for this test class
        ApplicationContext spring = SpringExtension.getApplicationContext(context);
        LocalIdamService localIdamService = spring.getBean(LocalIdamService.class);

        IdamUsersConfig cfg = loadConfig(ann.config());

        Map<String, User> provisioned = new LinkedHashMap<>();
        for (String email : ann.emails()) {
            IdamUsersConfig.UserProfile p = cfg.merge(ann.profile(), email);

            User requested = User.builder()
                .email(email)
                .forename(p.forename())
                .surname(p.surname())
                .roles(List.copyOf(p.roles()))
                .build();

            // This is the Spring-created/provisioned user with tokens populated
            User createdOrExisting = localIdamService.withUser(requested);
            provisioned.put(email, createdOrExisting);
        }

        context.getStore(NS).put(STORE_USERS, provisioned);
    }

    @Override
    public boolean supportsParameter(ParameterContext pc, ExtensionContext ec) {
        if (!pc.isAnnotated(IdamUser.class) && !pc.isAnnotated(IdamTokensParam.class)) {
            return false;
        }

        Class<?> type = pc.getParameter().getType();
        return type.equals(User.class) || type.equals(IdamTokens.class);
    }

    @Override
    public Object resolveParameter(ParameterContext pc, ExtensionContext ec) {
        @SuppressWarnings("unchecked")
        Map<String, User> users = (Map<String, User>) ec.getStore(NS).get(STORE_USERS);

        requireNonNull(users, "No users were provisioned. Ensure @WithIdamUsers is present on the test class.");

        String email = "";
        if (pc.isAnnotated(IdamUser.class)) {
            email = pc.findAnnotation(IdamUser.class).orElseThrow().email();
        } else if (pc.isAnnotated(IdamTokensParam.class)) {
            email = pc.findAnnotation(IdamTokensParam.class).orElseThrow().email();
        }

        User u = selectUser(users, email);

        if (pc.isAnnotated(IdamTokensParam.class)) {
            return requireNonNull(u.tokens(), "Provisioned user has no tokens: " + u.email());
        }
        return u;
    }

    private static User selectUser(Map<String, User> users, String email) {
        if (email == null || email.isBlank()) {
            return users.values().iterator().next();
        }
        User u = users.get(email);
        if (u == null) {
            throw new ParameterResolutionException(
                "No provisioned user for email " + email + ". Available: " + users.keySet());
        }
        return u;
    }

    private static IdamUsersConfig loadConfig(String resourcePath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        try (InputStream is = WithIdamUsersExtension.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Cannot find config resource: " + resourcePath);
            }
            return mapper.readValue(is, IdamUsersConfig.class);
        }
    }
}
