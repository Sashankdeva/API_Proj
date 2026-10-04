package com.squadup.soap.schema;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "searchPlayersResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"totalMatches", "player"})
public class SearchPlayersResponse {

    private long totalMatches;

    @XmlElement(name = "player")
    private List<SoapPlayerSummary> player = new ArrayList<>();

    public long getTotalMatches() { return totalMatches; }
    public void setTotalMatches(long totalMatches) { this.totalMatches = totalMatches; }

    public List<SoapPlayerSummary> getPlayer() { return player; }
}
