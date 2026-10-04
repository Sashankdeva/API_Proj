package com.squadup.soap.schema;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "searchPlayersRequest")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"query", "maxResults"})
public class SearchPlayersRequest {

    @XmlElement(required = true)
    private String query;

    private Integer maxResults;

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public Integer getMaxResults() { return maxResults; }
    public void setMaxResults(Integer maxResults) { this.maxResults = maxResults; }
}
