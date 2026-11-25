package com.northbridge.mobile.model;

import java.util.ArrayList;
import java.util.List;

public class TransactionPage {
    private List<Transaction> content = new ArrayList<>();
    private int page;
    private int size;
    private long total;

    public TransactionPage() {
    }

    public TransactionPage(List<Transaction> content, int page, int size, long total) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.total = total;
    }

    public List<Transaction> getContent() { return content; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotal() { return total; }

    public boolean hasMore() {
        return (long) (page + 1) * size < total;
    }
}
