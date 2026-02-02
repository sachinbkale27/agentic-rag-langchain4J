package com.example.agentic.rag.model;

/**
 * Route a user query to the most relevant datasource
 */
public class RouteQuery {
    /**
     * Datasource to route to: "vectorstore" or "web_search"
     */
    private String datasource;

    public RouteQuery() {
    }

    public RouteQuery(String datasource) {
        this.datasource = datasource;
    }

    public String getDatasource() {
        return datasource;
    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }
}
