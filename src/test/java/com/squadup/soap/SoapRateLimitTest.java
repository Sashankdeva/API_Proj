package com.squadup.soap;

import com.squadup.dto.AuthDtos.LoginRequest;
import com.squadup.dto.AuthDtos.RegisterRequest;
import com.squadup.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** A logged-in caller may only make a few SOAP calls per minute (lowered to 2 here). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "squadup.rate-limit.soap-call=2")
@ActiveProfiles("test")
class SoapRateLimitTest {

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private AuthService authService;

    @Test
    void callsAboveTheLimitGetASoapFault() {
        authService.register(new RegisterRequest("soap_busy", "soap_busy@test.com", "secret123"));
        String token = authService.login(new LoginRequest("soap_busy", "secret123")).token();

        assertThat(call(token).getBody()).doesNotContain("Too many requests");
        assertThat(call(token).getBody()).doesNotContain("Too many requests");

        ResponseEntity<String> third = call(token);
        assertThat(third.getBody()).contains("Fault").contains("Too many requests");
    }

    private ResponseEntity<String> call(String token) {
        String envelope = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                    <soapenv:Header/>
                    <soapenv:Body>
                        <p:getPlayerRequest xmlns:p="http://squadup.com/soap/players">
                            <p:username>soap_busy</p:username>
                        </p:getPlayerRequest>
                    </soapenv:Body>
                </soapenv:Envelope>""";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        headers.setBearerAuth(token);
        return http.postForEntity("/soap", new HttpEntity<>(envelope, headers), String.class);
    }
}
