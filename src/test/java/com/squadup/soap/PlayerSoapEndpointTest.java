package com.squadup.soap;

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

/**
 * Unit IV: SOAP web service. Posts real SOAP envelopes over HTTP, the same thing SoapUI does.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PlayerSoapEndpointTest {

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private AuthService authService;

    @Test
    void wsdlIsPublished() {
        String wsdl = http.getForObject("/soap/players.wsdl", String.class);
        assertThat(wsdl)
                .contains("wsdl:definitions")
                .contains("getPlayer")
                .contains("searchPlayers")
                .contains("PlayersPort");
    }

    @Test
    void getPlayerReturnsTheProfileWithoutEmail() {
        authService.register(new RegisterRequest("soap_hero", "soap_hero@test.com", "secret123"));

        ResponseEntity<String> response = call("""
                <p:getPlayerRequest xmlns:p="http://squadup.com/soap/players">
                    <p:username>soap_hero</p:username>
                </p:getPlayerRequest>""");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody())
                .contains("getPlayerResponse")
                .contains("soap_hero")
                .doesNotContain("soap_hero@test.com");
    }

    @Test
    void searchPlayersReturnsMatches() {
        authService.register(new RegisterRequest("soapy_one", "soapy_one@test.com", "secret123"));
        authService.register(new RegisterRequest("soapy_two", "soapy_two@test.com", "secret123"));

        ResponseEntity<String> response = call("""
                <p:searchPlayersRequest xmlns:p="http://squadup.com/soap/players">
                    <p:query>soapy</p:query>
                    <p:maxResults>5</p:maxResults>
                </p:searchPlayersRequest>""");

        assertThat(response.getBody())
                .containsPattern("totalMatches>2</")
                .contains("soapy_one", "soapy_two");
    }

    @Test
    void unknownPlayerProducesASoapFault() {
        ResponseEntity<String> response = call("""
                <p:getPlayerRequest xmlns:p="http://squadup.com/soap/players">
                    <p:username>nobody_at_all</p:username>
                </p:getPlayerRequest>""");

        assertThat(response.getStatusCode().value()).isEqualTo(500); // SOAP 1.1 faults use HTTP 500
        assertThat(response.getBody()).contains("Fault").contains("No player named 'nobody_at_all'");
    }

    private ResponseEntity<String> call(String payload) {
        String envelope = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                    <soapenv:Header/>
                    <soapenv:Body>%s</soapenv:Body>
                </soapenv:Envelope>""".formatted(payload);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        return http.postForEntity("/soap", new HttpEntity<>(envelope, headers), String.class);
    }
}
