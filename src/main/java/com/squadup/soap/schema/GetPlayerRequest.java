package com.squadup.soap.schema;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "getPlayerRequest")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"username"})
public class GetPlayerRequest {

    @XmlElement(required = true)
    private String username;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
