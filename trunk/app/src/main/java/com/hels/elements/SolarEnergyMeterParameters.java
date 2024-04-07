package com.hels.elements;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class SolarEnergyMeterParameters implements Parcelable {

    // The max index shouldn't exceed max number of ListView types
    public static final int TYPE_NOT_DEF = 100;
    public static final int TYPE_PAIRED = 0;
    public static final int TYPE_DISCOVERED = 1;
    public static final int TYPE_PAIRED_INFO = 3;

    private String mac = null;
    private String btName = null;
    private String mugName = null;
    private Integer batSolarmV = null;
    private Integer batSolarmA = null;
    private Integer batSolarmW = null;
    private Integer batLoadmV = null;
    private Integer batLoadmA = null;
    private Integer batLoadmW = null;
    private Integer batCPUmV = null;
    private Boolean connected = false;
    private Integer type = TYPE_NOT_DEF;
    private Long mugColor = null;
    private Long dateTime = null;
    private Integer rssi = null;

    public SolarEnergyMeterParameters(String mac, String btName, String mugName, Long mugColor,
                                      Integer batVolts, Integer batAmps, Integer batmW,
                                      Integer loadVolts, Integer loadAmps, Integer loadmW,
                                      Integer batteryCharge, Integer type, Boolean connected, Long dateTime, Integer rssi) {
        this.mac = mac;
        this.btName = btName;
        this.mugName = mugName;
        this.batSolarmV = batVolts;
        this.batSolarmA = batAmps;
        this.batSolarmW = batmW;
        this.batLoadmV = loadVolts;
        this.batLoadmA = loadAmps;
        this.batLoadmW = loadmW;
        this.batCPUmV = batteryCharge;
        this.connected = connected;
        this.type = type;
        this.mugColor = mugColor;
        this.dateTime = dateTime;
        this.rssi = rssi;
    }


    protected SolarEnergyMeterParameters(Parcel in) {
        mac = in.readString();
        btName = in.readString();
        mugName = in.readString();

        if(in.readByte() == 0) {
            batSolarmV = null;
        } else {
            batSolarmV = in.readInt();
        }
        if(in.readByte() == 0) {
            batSolarmA = null;
        } else {
            batSolarmA = in.readInt();
        }
        if(in.readByte() == 0) {
            batSolarmW = null;
        } else {
            batSolarmW = in.readInt();
        }

        if(in.readByte() == 0) {
            batLoadmV = null;
        } else {
            batLoadmV = in.readInt();
        }
        if(in.readByte() == 0) {
            batLoadmA = null;
        } else {
            batLoadmA = in.readInt();
        }
        if(in.readByte() == 0) {
            batLoadmW = null;
        } else {
            batLoadmW = in.readInt();
        }

        if(in.readByte() == 0) {
            batCPUmV = null;
        } else {
            batCPUmV = in.readInt();
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

        if(in.readByte() == 0) {
            rssi = null;
        } else {
            rssi = in.readInt();
        }

    }

    public static final Creator<SolarEnergyMeterParameters> CREATOR = new Creator<SolarEnergyMeterParameters>() {
        @Override
        public SolarEnergyMeterParameters createFromParcel(Parcel in) {
            return new SolarEnergyMeterParameters(in);
        }

        @Override
        public SolarEnergyMeterParameters[] newArray(int size) {
            return new SolarEnergyMeterParameters[size];
        }
    };

    public void setBatteryAmps(Integer a) {
        batSolarmA = a;
    }
    public Integer getBatteryAmps() {
        return batSolarmA;
    }

    public void setBatteryVolts(Integer v) {
        batSolarmV = v;
    }
    public Integer getBatteryVolts() {
        return batSolarmV;
    }

    public void setBatterymW(Integer v) {
        batSolarmW = v;
    }
    public Integer getBatterymW() {
        return batSolarmW;
    }

    public void setLoadAmps(Integer a) {
        batLoadmA = a;
    }
    public Integer getLoadAmps() {
        return batLoadmA;
    }

    public void setLoadVolts(Integer v) {
        batLoadmV = v;
    }
    public Integer getLoadVolts() {
        return batLoadmV;
    }

    public void setLoadmW(Integer v) {
        batLoadmW = v;
    }
    public Integer getLoadmW() {
        return batLoadmW;
    }


    public void setCPUBatteryCharge(Integer p) {
        batCPUmV = p;
    }
    public Integer getCPUBatteryCharge() {
        return batCPUmV;
    }

    public void setConnected(Boolean v) {

        connected = v;
        if(!connected) {
            batSolarmV = null;
            batSolarmA = null;
            batCPUmV = null;
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

    public void setRSSI(Integer v) {
        rssi = v;
    }
    public Integer getRSSI() {
        return rssi;
    }


//    public boolean checkForData() {
//        if( (mugName != null) || (currentTemperature != null) ) return true;
//
//        return false;
//    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(mac);
        parcel.writeString(btName);
        parcel.writeString(mugName);

        if(batSolarmV == null) {
            parcel.writeByte((byte) 0);
        } else {
            parcel.writeByte((byte) 1);
            parcel.writeInt(batSolarmV);
        }
        if(batSolarmA == null) {
            parcel.writeByte((byte) 0);
        } else {
            parcel.writeByte((byte) 1);
            parcel.writeInt(batSolarmA);
        }
        if(batSolarmW == null) {
            parcel.writeByte((byte) 0);
        } else {
            parcel.writeByte((byte) 1);
            parcel.writeInt(batSolarmW);
        }

        if(batLoadmV == null) {
            parcel.writeByte((byte) 0);
        } else {
            parcel.writeByte((byte) 1);
            parcel.writeInt(batLoadmV);
        }
        if(batLoadmA == null) {
            parcel.writeByte((byte) 0);
        } else {
            parcel.writeByte((byte) 1);
            parcel.writeInt(batLoadmA);
        }
        if(batLoadmW == null) {
            parcel.writeByte((byte) 0);
        } else {
            parcel.writeByte((byte) 1);
            parcel.writeInt(batLoadmW);
        }

        if(batCPUmV == null) {
            parcel.writeByte((byte) 0);
        } else {
            parcel.writeByte((byte) 1);
            parcel.writeInt(batCPUmV);
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

        if(rssi == null) {
            parcel.writeByte((byte) 0);
        } else {
            parcel.writeByte((byte) 1);
            parcel.writeInt(rssi);
        }

    }
}
