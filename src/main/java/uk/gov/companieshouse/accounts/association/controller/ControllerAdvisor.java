package uk.gov.companieshouse.accounts.association.controller;

import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.ParsingUtil.parseJsonFrom;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil.APPLICATION_NAMESPACE;
import static uk.gov.companieshouse.service.rest.err.Err.invalidPathBuilderWithLocation;
import static uk.gov.companieshouse.service.rest.err.Err.serviceErrBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.ForbiddenRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.service.rest.err.Errors;

@ControllerAdvice
public class ControllerAdvisor extends ResponseEntityExceptionHandler {

    private <T extends Exception> Errors mapThrownExceptionsToErrors(final T exception, final HttpServletRequest request){
        final var url = request.getRequestURL().toString();
        final var queryParams = Objects.nonNull(request.getQueryString()) ? "?" + request.getQueryString() : "";
        final Map<String, Object> contextMap = Map.of("url", url, "query-parameters", queryParams);

        LOGGER.errorContext(getXRequestId(), exception.getMessage(), exception, contextMap);

        return new Errors(serviceErrBuilder().withError(exception.getMessage()).build());
    }

    @ExceptionHandler(NotFoundRuntimeException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public Errors onNotFoundRuntimeException(final NotFoundRuntimeException exception, final HttpServletRequest request) {
        return mapThrownExceptionsToErrors(exception, request);
    }

    @ExceptionHandler(BadRequestRuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Errors onBadRequestRuntimeException(final BadRequestRuntimeException exception, final HttpServletRequest request) {
        return mapThrownExceptionsToErrors(exception, request);
    }

    @ExceptionHandler(InternalServerErrorRuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Errors onInternalServerErrorRuntimeException(final InternalServerErrorRuntimeException exception, final HttpServletRequest request) {
        return mapThrownExceptionsToErrors(exception, request);
    }

    @ExceptionHandler(ForbiddenRuntimeException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    public Errors onForbiddenRuntimeException(final ForbiddenRuntimeException exception, final HttpServletRequest request) {
        return mapThrownExceptionsToErrors(exception, request);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Errors onException(final Exception exception, final HttpServletRequest request) {
        return mapThrownExceptionsToErrors(exception, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Errors onConstraintViolationException(final ConstraintViolationException exception) {
        final var errors = new Errors();
        exception.getConstraintViolations()
                .stream()
                .map(constraintViolation -> String.format("%s %s", Optional.of(constraintViolation.getInvalidValue()).orElse(" "), constraintViolation.getMessage()))
                .map(error -> invalidPathBuilderWithLocation(APPLICATION_NAMESPACE).withError(error).build())
                .forEach(errors::addError);

        LOGGER.errorContext(getXRequestId(), String.format("Validation Failed with [%s]", parseJsonFrom(errors, "")), exception, null);

        return errors;
    }
}