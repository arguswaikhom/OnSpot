package com.crown.onspot.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.Timestamp;

public class StatusRecord implements Parcelable {
    public static final Parcelable.Creator<StatusRecord> CREATOR = new Parcelable.Creator<StatusRecord>() {
        @Override
        public StatusRecord createFromParcel(Parcel source) {
            return new StatusRecord(source);
        }

        @Override
        public StatusRecord[] newArray(int size) {
            return new StatusRecord[size];
        }
    };
    private Status status;
    private Timestamp timestamp;

    public StatusRecord() {
    }

    public StatusRecord(Status status, Timestamp timestamp) {
        this.status = status;
        this.timestamp = timestamp;
    }

    protected StatusRecord(Parcel in) {
        int tmpStatus = in.readInt();
        this.status = tmpStatus == -1 ? null : Status.values()[tmpStatus];
        this.timestamp = in.readParcelable(Timestamp.class.getClassLoader());
    }

    public Status getStatus() {
        return status;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.status == null ? -1 : this.status.ordinal());
        dest.writeParcelable(this.timestamp, flags);
    }

    public enum Status {
        ORDERED, ACCEPTED, PREPARING, ON_THE_WAY, DELIVERED, CANCELED
    }
}