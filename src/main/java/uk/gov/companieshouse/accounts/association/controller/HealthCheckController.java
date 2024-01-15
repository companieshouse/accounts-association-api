package uk.gov.companieshouse.accounts.association.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/associations")
public class HealthCheckController {

    @GetMapping("/healthcheck")
    public ResponseEntity<String> checking() {
        return ResponseEntity.ok().body("OK");
    }
}