package com.example.ipollusen;

public class Appliance {
    private String applianceId;
    private String name;
    private String state;
    private boolean isOn;
    private boolean isSmart;

    public Appliance(String applianceId, String name, String state, boolean isOn, boolean isSmart) {
        this.applianceId = applianceId;
        this.name = name;
        this.state = state;
        this.isOn = isOn;
        this.isSmart = isSmart;
    }

    public String getApplianceId() {
        return applianceId;
    }

    public String getName() {
        return name;
    }

    public String getState() {
        return state;
    }

    public boolean isOn() {
        return isOn;
    }

    public boolean isSmart() {
        return isSmart;
    }
}
