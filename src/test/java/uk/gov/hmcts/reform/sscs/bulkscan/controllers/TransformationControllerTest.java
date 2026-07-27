package uk.gov.hmcts.reform.sscs.bulkscan.controllers;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.handlers.CcdCallbackHandler;
import uk.gov.hmcts.reform.sscs.bulkscan.domain.transformation.CaseCreationDetails;
import uk.gov.hmcts.reform.sscs.bulkscan.domain.transformation.SuccessfulTransformationResponse;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptionhandlers.ResponseExceptionHandler;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptions.ForbiddenException;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptions.InvalidExceptionRecordException;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptions.UnauthorizedException;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;

@ExtendWith(MockitoExtension.class)
public class TransformationControllerTest {

    @Mock
    private CcdCallbackHandler ccdCallbackHandler;

    @Mock
    private AuthorisationService authService;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        TransformationController transformationController = new TransformationController(authService, ccdCallbackHandler);
        mockMvc = standaloneSetup(transformationController)
            .setControllerAdvice(new ResponseExceptionHandler())
            .build();
    }

    private final Map<String, Object> hmctsServiceIdMap = new HashMap<>() {
        {
            put("HMCTSServiceId", "BBA3");
        }
    };

    private final Map<String, Map<String, Object>> supplementaryDataRequestMap = new HashMap<>() {
        {
            put("$set", hmctsServiceIdMap);
        }
    };

    //FIXME: update after bulk scan auto case creation is switch on
    @ParameterizedTest
    @ValueSource(strings = {"/transform-exception-record", "/transform-scanned-data"})
    public void should_return_case_data_if_transformation_succeeded(String url) throws Exception {
        doNothing().when(authService).assertIsAllowedToHandleCallback(any());

        SuccessfulTransformationResponse transformationResult = getSuccessfulTransformationResponse();

        given(ccdCallbackHandler.handle(any())).willReturn(transformationResult);

        sendRequest(url)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.case_creation_details.case_type_id").value("case-type-id"))
            .andExpect(jsonPath("$.case_creation_details.event_id").value("event-id"))
            .andExpect(jsonPath("$.case_creation_details.case_data.person1_first_name").value("George"))
            .andExpect(jsonPath("$.warnings[0]").value("warning-1"))
            .andExpect(jsonPath("$.warnings[1]").value("warning-2"));
    }

    @NotNull
    private SuccessfulTransformationResponse getSuccessfulTransformationResponse() {
        Map<String, Object> pairs = new HashMap<>();

        pairs.put("person1_first_name", "George");

        return new SuccessfulTransformationResponse(
            new CaseCreationDetails(
                "case-type-id",
                "event-id",
                pairs
            ),
            asList(
                "warning-1",
                "warning-2"
            ),
            supplementaryDataRequestMap
        );
    }

    //FIXME: update after bulk scan auto case creation is switch on
    @ParameterizedTest
    @ValueSource(strings = {"/transform-exception-record", "/transform-scanned-data"})
    public void should_return_422_with_errors_if_transformation_failed(String url) throws Exception {
        given(ccdCallbackHandler.handle(any()))
            .willThrow(new InvalidExceptionRecordException(
                asList(
                    "error-1",
                    "error-2"
                )
            ));

        sendRequest(url)
            .andDo(print())
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.errors[0]").value("error-1"))
            .andExpect(jsonPath("$.errors[1]").value("error-2"));
    }

    //FIXME: delete after bulk scan auto case creation is switch on
    @ParameterizedTest
    @MethodSource("exceptionsAndStatuses")
    public void should_return_proper_status_codes_for_auth_exceptions_when_transforming_scanned_data(RuntimeException exc, HttpStatus status) throws Exception {
        doThrow(exc).when(authService).assertIsAllowedToHandleCallback(any());

        sendRequest("/transform-exception-record").andExpect(status().is(status.value()));
    }

    @ParameterizedTest
    @MethodSource("exceptionsAndStatuses")
    public void new_endpoint_should_return_proper_status_codes_for_auth_exceptions_when_transforming_scanned_data(RuntimeException exc, HttpStatus status) throws Exception {
        doThrow(exc).when(authService).assertIsAllowedToHandleCallback(any());

        sendRequest("/transform-scanned-data").andExpect(status().is(status.value()));
    }

    private ResultActions sendRequest(String url) throws Exception {
        return mockMvc
            .perform(
                post(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            );
    }

    private static Stream<Arguments> exceptionsAndStatuses() {
        return Stream.of(
            Arguments.of(new UnauthorizedException("unauthorized"), HttpStatus.UNAUTHORIZED),
            Arguments.of(new ForbiddenException("forbidden"), HttpStatus.FORBIDDEN),
            Arguments.of(new InvalidTokenException("invalid-token"), HttpStatus.UNAUTHORIZED)
        );
    }
}
