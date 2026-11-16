package io.github.hussainmd.certwatcher.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import io.github.hussainmd.certwatcher.check.CertificateCheckService;
import io.github.hussainmd.certwatcher.check.CertificateStatus;
import io.github.hussainmd.certwatcher.check.Severity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CertificateController.class)
class CertificateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CertificateCheckService checkService;

    @Test
    void listsLatestResults() throws Exception {
        when(checkService.latest()).thenReturn(List.of(result("a.example:443", Severity.OK)));

        mockMvc.perform(get("/api/v1/certificates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].target").value("a.example:443"))
                .andExpect(jsonPath("$[0].daysUntilExpiry").value(90));
    }

    @Test
    void filtersBySeverity() throws Exception {
        when(checkService.latest()).thenReturn(List.of(
                result("a.example:443", Severity.OK),
                result("b.example:443", Severity.WARNING)));

        mockMvc.perform(get("/api/v1/certificates").param("severity", "WARNING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].target").value("b.example:443"));
    }

    private static CertificateStatus result(String target, Severity severity) {
        return new CertificateStatus(
                target, "CN=" + target, "CN=Test CA", Instant.parse("2026-04-01T00:00:00Z"), 90L, severity, null);
    }
}
