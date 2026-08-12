package com.eneik.production.dto;

import java.util.List;

public class SearchResponse {
    private List<MaterialDTO> items;
    private long total;
    private int page;
    private int limit;
    private int pages;

    public SearchResponse() {}

    public SearchResponse(List<MaterialDTO> items, long total, int page, int limit, int pages) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.limit = limit;
        this.pages = pages;
    }

    public List<MaterialDTO> getItems() {
        return items;
    }

    public void setItems(List<MaterialDTO> items) {
        this.items = items;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }
}
