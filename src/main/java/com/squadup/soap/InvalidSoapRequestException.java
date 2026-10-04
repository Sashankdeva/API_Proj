package com.squadup.soap;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

@SoapFault(faultCode = FaultCode.CLIENT)
public class InvalidSoapRequestException extends RuntimeException {

    public InvalidSoapRequestException(String message) {
        super(message);
    }
}
