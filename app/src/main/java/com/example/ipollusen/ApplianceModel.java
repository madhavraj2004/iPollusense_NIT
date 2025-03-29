package com.example.ipollusen;

import java.io.Serializable;

public class ApplianceModel implements Serializable {
    private String applianceId;
    private String applianceName;
    private String applianceState;
    private boolean isSmart;

    public ApplianceModel(String applianceId, String applianceName, String applianceState, boolean isSmart) {
        this.applianceId = applianceId;
        this.applianceName = applianceName;
        this.applianceState = applianceState;
        this.isSmart = isSmart;
    }

    public String getApplianceId() { return applianceId; }
    public String getApplianceName() { return applianceName; }
    public String getApplianceState() { return applianceState; }
    public boolean isSmart() { return isSmart; }
}
