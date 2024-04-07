package com.hels.elements;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class MugParameters implements Parcelable {

    // The max index shouldn't exceed max number of ListView types
    public static final int TYPE_NOT_DEF = 100;
    public static final int TYPE_PAIRED = 0;
    public static final int TYPE_DISCOVERED = 1;
    public static final int TYPE_PAIRED_INFO = 3;

    private String mac = null;
    private String btName = null;
    private String mugName = null;
    private Float currentTemperature = -1.0f;
    private Integer targetTemperature = null;
    private Integer batteryCharge = null;
    private Boolean connected = false;
    private Integer type = TYPE_NOT_DEF;
    private Long mugColor = null;
    private Long dateTime = null;

    public MugParameters(String mac, String btName, String mugName, Long mugColor, Float currentTemperature, Integer targetTemperature, Integer batteryCharge,  Integer type, Boolean connected, Long dateTime) {
        this.mac = mac;
        this.btName = btName;
        this.mugName = mugName;
        this.currentTemperature = currentTemperature;
        this.targetTemperature = targetTemperature;
        this.batteryCharge = batteryCharge;
        this.connected = connected;
        this.type = type;
        this.mugColor = mugColor;
        this.dateTime = dateTime;
    }


    protected MugParameters(Parcel in) {
        mac = in.readString();
        btName = in.readString();
        mugName = in.readString();
        if(in.readByte() == 0) {
            currentTemperature = null;
        } else {
            currentTemperature = in.readFloat();
        }
        if(in.readByte() == 0) {
            targetTemperature = null;
        } else {
            targetTemperature = in.readInt();
        }
        if(in.readByte() == 0) {
            batteryCharge = null;
        } else {
            batteryCharge = in.readInt();
        }
        byte tmpConnected = in.readByte();
        connected = tmpConnected == 0 ? null : tmpConnected == 1;
        if(in.readByte() == 0) {
            type = null;
        } else {
            type = in.readInt();
        }
        if(in.readByte() == 0) {
            mugColor = null;
        } else {
            mugColor = in.readLong();
        }
        if(in.readByte() == 0) {
            dateTime = null;
        } else {
            dateTime = in.readLong();
        }

    }

    public static final Creator<MugParameters> CREATOR = new Creator<MugParameters>() {
        @Override
        public MugParameters createFromParcel(Parcel in) {
            return new MugParameters(in);
        }

        @Override
        public MugParameters[] newArray(int size) {
            return new MugParameters[size];
        }
    };

    public void setCurrentTemperature(Float t) {
        currentTemperature = t;
    }
    public Float getCurrentTemperature() {
        return currentTemperature;
    }

    public void setTargetTemperature(Integer t) {
        targetTemperature = t;
    }
    public Integer getTargetTemperature() {
        return targetTemperature;
    }

    public void setBatteryCharge(Integer p) {
        batteryCharge = p;
    }
    public Integer getBatteryCharge() {
        return batteryCharge;
    }

    public void setConnected(Boolean v) {

        connected = v;
        if(!connected) {
            currentTemperature = null;
            targetTemperature = null;
            batteryCharge = null;
        }
    }

    public Boolean getConnected() {
        return connected;
    }

    public void setMACAddress(String mac) {
        this.mac = mac;
    }

    public String getMACAddress() {
        return mac;
    }

    public String getMugName() {
        return mugName;
    }

    public String getBtName() { return btName; }

    public void setMugName(String mugName) {
        this.mugName = mugName;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public void setMugColor(Long mugColor) {
        this.mugColor = mugColor;
    }

    public Long getMugColor() {
        return mugColor;
    }

    public void setDateTime(Long dateTime) {
        this.dateTime = dateTime;
    }

    public Long getDateTime() {
        return this.dateTime;
    }

    public boolean checkForData() {
        if( (mugName != null) || (currentTemperature != null) ) return true;

        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(mac);
        parcel.writeString(btName);
        parcel.writeString(mugName);
        if(currentTemperature == null) {
            parcel.writeByte((byte) 0);
        } else {
            parcel.writeByte((byte) 1);
            parcel.writeFloat(currentTemperature);
        }
        if(targetTemperature == null) {
            parcel.writeByte((byte) 0);
        } else {
            parcel.writeByte((byte) 1);
            parcel.writeInt(targetTemperature);
        }
        if(batteryCharge == null) {
            parcel.writeByte((byte) 0);
        } else {
            parcel.writeByte((byte) 1);
            parcel.writeInt(batteryCharge);
        }
        parcel.writeByte((byte) (connected == null ? 0 : connected ? 1 : 2));
        if(type == null) {
            parcel.writeByte((byte) 0);
        } else {
            parcel.writeByte((byte) 1);
            parcel.writeInt(type);
        }
        if(mugColor == null) {
            parcel.writeByte((byte) 0);
        } else {
            parcel.writeByte((byte) 1);
            parcel.writeLong(mugColor);
        }
        if(dateTime == null) {
            parcel.writeByte((byte) 0);
        } else {
            parcel.writeByte((byte) 1);
            parcel.writeLong(dateTime);
        }

    }
}
