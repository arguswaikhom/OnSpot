package com.crown.onspot.model;

import com.crown.onspot.utils.ListItemKey;
import com.crown.onspot.utils.abstracts.ListItem;

import java.util.List;

public class Order extends ListItem {
    private String orderId;
    private String businessRefId;
    private String businessDisplayName;
    private String customerId;
    private String customerDisplayName;
    private StatusRecord.Status status;
    private List<StatusRecord> statusRecord;
    private Long totalPrice;
    private Long finalPrice;
    private List<OrderItem> items;
    private Contact contact;
    private OSLocation destination;

    public Order() {
    }

    @Override
    public int getItemType() {
        return ListItemKey.ORDER;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public OSLocation getDestination() {
        return destination;
    }

    public void setDestination(OSLocation destination) {
        this.destination = destination;
    }

    public Long getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Long totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getBusinessRefId() {
        return businessRefId;
    }

    public void setBusinessRefId(String businessRefId) {
        this.businessRefId = businessRefId;
    }

    public String getBusinessDisplayName() {
        return businessDisplayName;
    }

    public void setBusinessDisplayName(String businessDisplayName) {
        this.businessDisplayName = businessDisplayName;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerDisplayName() {
        return customerDisplayName;
    }

    public void setCustomerDisplayName(String customerDisplayName) {
        this.customerDisplayName = customerDisplayName;
    }

    public StatusRecord.Status getStatus() {
        return status;
    }

    public void setStatus(StatusRecord.Status status) {
        this.status = status;
    }

    public List<StatusRecord> getStatusRecord() {
        return statusRecord;
    }

    public void setStatusRecord(List<StatusRecord> statusRecord) {
        this.statusRecord = statusRecord;
    }

    public Long getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(Long finalPrice) {
        this.finalPrice = finalPrice;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
}
