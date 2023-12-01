package uk.gov.companieshouse.authentication.controller;

import org.springframework.web.bind.annotation.ModelAttribute;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.authentication.AuthenticationServiceApplication;

public abstract class BaseController {

    protected static final Logger LOGGER = LoggerFactory
            .getLogger(AuthenticationServiceApplication.APPLICATION_NAME_SPACE);

    protected static final String ERROR_VIEW = "error";

    protected BaseController() {
    }

    @ModelAttribute("templateName")
    protected abstract String getTemplateName();
}

