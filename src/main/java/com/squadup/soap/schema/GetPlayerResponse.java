package com.squadup.soap.schema;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "getPlayerResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"player"})
public class GetPlayerResponse {

    @XmlElement(required = true)
    private SoapPlayer player;

    public SoapPlayer getPlayer() { return player; }
    public void setPlayer(SoapPlayer player) { this.player = player; }
}
