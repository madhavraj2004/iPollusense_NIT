package com.example.ipollusen;

import androidx.lifecycle.ViewModel;

public class RoomDetailsViewModel extends ViewModel {
    private String roomName = "";
    private String roomDesc = "";
    private String length = "";
    private String breadth = "";
    private String nodeIds = "";
    private String applianceJsonData = null;

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getRoomDesc() {
        return roomDesc;
    }

    public void setRoomDesc(String roomDesc) {
        this.roomDesc = roomDesc;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
    }

    public String getBreadth() {
        return breadth;
    }

    public void setBreadth(String breadth) {
        this.breadth = breadth;
    }

    public String getNodeIds() {
        return nodeIds;
    }

    public void setNodeIds(String nodeIds) {
        this.nodeIds = nodeIds;
    }

    public String getApplianceJsonData() {
        return applianceJsonData;
    }

    public void setApplianceJsonData(String applianceJsonData) {
        this.applianceJsonData = applianceJsonData;
    }
}