package io.github.hussainmd.certwatcher.api;

import java.util.List;

import io.github.hussainmd.certwatcher.check.CertificateCheckService;
import io.github.hussainmd.certwatcher.check.CertificateStatus;
import io.github.hussainmd.certwatcher.check.Severity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/certificates")
public class CertificateController {

    private final CertificateCheckService checkService;

    public CertificateController(CertificateCheckService checkService) {
        this.checkService = checkService;
    }

    @GetMapping
    public List<CertificateStatus> list(@RequestParam(required = false) Severity severity) {
        return checkService.latest().stream()
                .filter(status -> severity == null || status.severity() == severity)
                .toList();
    }
}
