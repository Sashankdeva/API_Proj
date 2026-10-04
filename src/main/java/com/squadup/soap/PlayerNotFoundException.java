package com.squadup.soap;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

/** Turned into a SOAP Fault (Client) by Spring WS instead of an HTTP 500 page. */
@SoapFault(faultCode = FaultCode.CLIENT)
public class PlayerNotFoundException extends RuntimeException {

    public PlayerNotFoundException(String message) {
        super(message);
    }
}
