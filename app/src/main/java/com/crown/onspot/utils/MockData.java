package com.crown.onspot.utils;

import com.crown.onspot.model.Shop;
import com.crown.onspot.utils.abstracts.ListItem;

import java.util.ArrayList;
import java.util.List;

public class MockData {
    public static List<ListItem> shopList() {
        List<ListItem> list = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            list.add(new Shop());
        }
        return list;
    }
}
