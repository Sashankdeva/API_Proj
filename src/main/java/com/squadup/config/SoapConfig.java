package com.squadup.config;

import com.squadup.soap.schema.SoapNamespace;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

/**
 * SOAP web service configuration.
 *
 * Spring Boot's web-services auto-configuration enables Spring WS and registers its
 * MessageDispatcherServlet at {@code spring.webservices.path} (/soap, see application.properties).
 * Adding {@code @EnableWs} here would switch that auto-configuration off.
 *
 * The bean named "players" below publishes a WSDL built from the XSD at
 * {@code /soap/players.wsdl}. Load that URL into SoapUI to test the service.
 */
@Configuration
public class SoapConfig {

    @Bean(name = "players")
    public DefaultWsdl11Definition playersWsdl(XsdSchema playersSchema) {
        DefaultWsdl11Definition wsdl = new DefaultWsdl11Definition();
        wsdl.setPortTypeName("PlayersPort");
        wsdl.setLocationUri("/soap");
        wsdl.setTargetNamespace(SoapNamespace.URI);
        wsdl.setSchema(playersSchema);
        return wsdl;
    }

    @Bean
    public XsdSchema playersSchema() {
        return new SimpleXsdSchema(new ClassPathResource("xsd/players.xsd"));
    }
}
