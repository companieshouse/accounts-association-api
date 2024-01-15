package uk.gov.companieshouse.accounts.association.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.companieshouse.accounts.association.AccountsAssociationServiceApplication;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.utils.CamelCaseSnakeCase;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.service.rest.err.Err;
import uk.gov.companieshouse.service.rest.err.Errors;

@org.springframework.web.bind.annotation.ControllerAdvice
public class ControllerAdvice extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AccountsAssociationServiceApplication.applicationNameSpace);
    public static final String X_REQUEST_ID = "X-Request-Id";

    private String getJsonStringFromErrors(String requestId, Errors errors) {

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(errors);
        }
        catch (IOException e) {
            LOG.errorContext(requestId, String.format("Fail to parse Errors object to JSON %s", e.getMessage()), e, null);
            return "";
        }
    }

    @ExceptionHandler(NotFoundRuntimeException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public Errors onNotFoundRuntimeException(NotFoundRuntimeException e, HttpServletRequest r) {
        String requestId = r.getHeader(X_REQUEST_ID);

        Map<String, Object> contextMap = new HashMap<>();
        contextMap.put("url", r.getRequestURL().toString());
        contextMap.put("query-parameters", r.getQueryString() != null ? "?" + r.getQueryString() : "");

        LOG.errorContext(requestId, e.getMessage(), null, contextMap);

        Errors errors = new Errors();
        errors.addError(Err.invalidBodyBuilderWithLocation(e.getFieldLocation()).withError(e.getMessage()).build());
        return errors;
    }

    @ExceptionHandler(BadRequestRuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Errors onBadRequestRuntimeException(BadRequestRuntimeException e, HttpServletRequest request) {
        String requestId = request.getHeader(X_REQUEST_ID);

        Map<String, Object> contextMap = new HashMap<>();
        contextMap.put("url", request.getRequestURL().toString());
        contextMap.put("query-parameters", request.getQueryString() != null ? "?" + request.getQueryString() : "");

        LOG.errorContext(requestId, e.getMessage(), null, contextMap);

        Errors errors = new Errors();
        errors.addError(Err.invalidBodyBuilderWithLocation("accounts_association_api").withError(e.getMessage()).build());
        return errors;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Errors onConstraintViolationException( ConstraintViolationException exception, HttpServletRequest request) {

        Errors errors = new Errors();
        for (ConstraintViolation<?> constraintViolation : exception.getConstraintViolations()) {
            final var location = CamelCaseSnakeCase.toSnakeCase(constraintViolation.getPropertyPath().toString());
            var errorMessage = getConstraintViolationExceptionErrorMessage( location );
            errors.addError(Err.invalidBodyBuilderWithLocation(location).withError(errorMessage).build());
        }

        String requestId = request.getHeader(X_REQUEST_ID);
        String errorsJsonString = getJsonStringFromErrors(requestId, errors);
        LOG.errorContext(requestId, String.format("Validation Failed with [%s]", errorsJsonString), exception, null );

        return errors;
    }

    private String getConstraintViolationExceptionErrorMessage(String location) {
        return switch (location) {
            case "update_association_status_for_user_and_company.arg0" -> "Please check the request and try again.";
            case "update_association_status_for_user_and_company.arg1" -> "Please check the request and try again.";
            default -> "One of the inputs is incorrectly formatted";
        };
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public void onException(Exception e, HttpServletRequest r) {
        String requestId = r.getHeader(X_REQUEST_ID);
        String msg = r.getRequestURL() + (r.getQueryString()!=null ? "?"+r.getQueryString() : "") + ". " + e.getMessage();
        LOG.errorContext(requestId, msg, e, null);
    }
}