package com.hels.elements;

public class MugSettings {

    public static final int TYPE_NOT_DEF = 100;
    public static final int TYPE_TEMP_TARGET = 0;
    public static final int TYPE_MUG_NAME = 1;
    public static final int TYPE_MUG_COLOR = 2;

    private Integer targetTemperature = null;
    private String mugName = null;
    private Long mugColor = 0L;

    private Integer type = TYPE_NOT_DEF;

    //public MugSettings(String mugName, Float targetTemperature, Long mugColor, Integer) {
    public MugSettings(String mugName, Integer type) {
        this.mugName = mugName;
        this.type = type;
    }

    public MugSettings(Integer targetTemperature, Integer type) {
        this.targetTemperature = targetTemperature;
        this.type = type;
    }

    public MugSettings(Long mugColor, Integer type) {
        this.mugColor = mugColor;
        this.type = type;
    }


    public void setTargetTemperature(Integer t) {
        targetTemperature = t;
    }
    public Integer getTargetTemperature() {
        return targetTemperature;
    }

    public void setMugName(String name) { mugName = name; }

    public String getMugName() {
        return mugName;
    }

    public void setMugColor(Long color) { mugColor = color; }
    public Long getMugColor() { return mugColor; }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getType() {
        return type;
    }
}
