package com.crown.onspot.model;

import com.crown.onspot.utils.abstracts.ListItem;

public class Header extends ListItem {
    public static final int TYPE = 2;

    private String header;

    public Header(String header) {
        this.header = header;
    }

    public String getHeader() {
        return header;
    }

    @Override
    public int getItemType() {
        return TYPE;
    }
}
