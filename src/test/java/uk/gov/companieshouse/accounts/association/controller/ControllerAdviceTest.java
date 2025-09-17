package uk.gov.companieshouse.accounts.association.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.configuration.InterceptorConfig;
import uk.gov.companieshouse.accounts.association.configuration.WebSecurityConfig;
import uk.gov.companieshouse.accounts.association.exceptions.ForbiddenRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.service.AssociationsTransactionService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.EmailService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.accounts.association.service.client.CompanyClient;
import uk.gov.companieshouse.accounts.association.service.client.UserClient;
import uk.gov.companieshouse.accounts.association.utils.RequestContextUtil;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;

@Tag("unit-test")
@Import(WebSecurityConfig.class)
@WebMvcTest(UserCompanyAssociationsController.class)
class ControllerAdviceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private AssociationsTransactionService associationsTransactionService;

    @MockitoBean
    private UsersService usersService;

    @MockitoBean
    private InterceptorConfig interceptorConfig;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private StaticPropertyUtil staticPropertyUtil;

    @MockitoBean
    private CompanyService companyService;

    @Mock
    private RequestContextUtil requestContextUtil;

    // Mock external service client layer
    @MockitoBean
    private CompanyClient companyClient;
    @MockitoBean
    private UserClient userClient;

    private final TestDataManager testDataManager = TestDataManager.getInstance();

    @BeforeEach
    void setup() {
    }

    @Test
    void testNotFoundRuntimeError() throws Exception {
        Mockito.doThrow(new NotFoundRuntimeException("Couldn't find association", new Exception("Couldn't find association")))
                .when(associationsTransactionService).fetchAssociationsForUserAndPartialCompanyNumberAndStatuses(any(),any(),anySet(),anyInt(),anyInt());

        try (var staticMock = mockStatic(RequestContextUtil.class)) {
            staticMock.when(RequestContextUtil::getUser).thenReturn(testDataManager.fetchUserDtos("111").getFirst());

            mockMvc.perform(get("/associations")
                            .header("X-Request-Id", "theId123")
                            .header("ERIC-Identity", "111")
                            .header("ERIC-Identity-Type", "oauth2"))
                    .andExpect(status().isNotFound());
        }
    }


    @Test
    void testBadRequestRuntimeError() throws Exception {
        mockMvc.perform(get("/associations?page_index=-1")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity", "111")
                        .header("ERIC-Identity-Type", "oauth2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testConstraintViolationError() throws Exception {
        mockMvc.perform(get("/associations?company_number=&&")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity", "111")
                        .header("ERIC-Identity-Type", "oauth2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testOnInternalServerError() throws Exception {
        Mockito.doThrow(new NullPointerException("Couldn't find association"))
                .when(associationsTransactionService).fetchAssociationsForUserAndPartialCompanyNumberAndStatuses(any(),any(),anySet(),anyInt(),anyInt());
        try (var staticMock = mockStatic(RequestContextUtil.class)) {
            staticMock.when(RequestContextUtil::getUser).thenReturn(testDataManager.fetchUserDtos("111").getFirst());

        mockMvc.perform(get("/associations?company_number=123445")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity", "111")
                        .header("ERIC-Identity-Type", "oauth2"))
                .andDo(print())
                .andExpect(status().isInternalServerError());
        }
    }

    @Test
    void testOnInternalServerErrorRuntimeException() throws Exception {
        Mockito.doThrow(new InternalServerErrorRuntimeException("Couldn't find association", new Exception("Couldn't find association")))
                .when(associationsTransactionService).fetchAssociationsForUserAndPartialCompanyNumberAndStatuses(any(),any(),anySet(),anyInt(),anyInt());

        try (var staticMock = mockStatic(RequestContextUtil.class)) {
            staticMock.when(RequestContextUtil::getUser).thenReturn(testDataManager.fetchUserDtos("111").getFirst());

        mockMvc.perform(get("/associations")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity", "111")
                        .header("ERIC-Identity-Type", "oauth2"))
                .andExpect(status().isInternalServerError());
        }
    }

    @Test
    void testOnForbiddenRuntimeException() throws Exception {
        Mockito.doThrow(new ForbiddenRuntimeException("Forbidden", new Exception("Forbidden")))
                .when(associationsTransactionService).fetchAssociationsForUserAndPartialCompanyNumberAndStatuses(any(), any(), anySet(), anyInt(), anyInt());

        mockMvc.perform(get("/associations")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity", "111")
                        .header("ERIC-Identity-Type", "oauth2"))
                .andExpect(status().isForbidden());
    }

}

