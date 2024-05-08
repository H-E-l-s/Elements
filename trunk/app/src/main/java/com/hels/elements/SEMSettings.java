package com.hels.elements;

public class SEMSettings {

    public static final int TYPE_NOT_DEF = 100;
    public static final int TYPE_LOAD = 0;
//    public static final int TYPE_MUG_NAME = 1;
//    public static final int TYPE_MUG_COLOR = 2;

    private Integer loadOnTimems = null;
    private Integer loadPeriodms = null;
    private Integer loadCurrentma = null;

    private Integer type = TYPE_NOT_DEF;

    public SEMSettings(Integer loadOnTimems, Integer loadPeriodms, Integer loadCurrentma, Integer type) {
        this.loadOnTimems = loadOnTimems;
        this.loadPeriodms = loadPeriodms;
        this.loadCurrentma = loadCurrentma;
        this.type = type;
    }

    public void setLoadOnTime(Integer ms) { this.loadOnTimems = ms; }
    public Integer getLoadOnTime() { return this.loadOnTimems; }

    public void setLoadPeriod(Integer ms) { this.loadPeriodms = ms; }
    public Integer getLoadPeriod() { return this.loadPeriodms; }

    public void setLoadCurrent(Integer ma) { this.loadCurrentma = ma; }
    public Integer getLoadCurrent() { return this.loadCurrentma; }

    public void setType(Integer type) {
        this.type = type;
    }
    public Integer getType() {
        return type;
    }
}
