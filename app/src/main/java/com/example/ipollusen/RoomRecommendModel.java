package com.example.ipollusen;

public class RoomRecommendModel {
    private final String roomId;
    private String roomName;
    private String roomDesc;
    private int length;
    private int breadth;
    private String nodeIds;
    private boolean hasNewRecommendation = false;
    // New field to store appliance JSON data.
    private String applianceData;
    private boolean responseFound; // New property to track play/pause state
    private String mapRoomUserId;
    // Full constructor including applianceData.
    public RoomRecommendModel(String roomId, String roomName, String roomDesc, int length, int breadth, String nodeIds, String applianceData) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.roomDesc = roomDesc;
        this.length = length;
        this.breadth = breadth;
        this.nodeIds = nodeIds;
        this.applianceData = applianceData;
        this.responseFound = false;
    }

    // Constructor without nodeIds and applianceData.
    public RoomRecommendModel(String roomId, String roomName, String roomDesc, int length, int breadth) {
        this(roomId, roomName, roomDesc, length, breadth, "", "");
    }

    // Constructor without dimensions, nodeIds, and applianceData.
    public RoomRecommendModel(String roomId, String roomName, String roomDesc) {
        this(roomId, roomName, roomDesc, 0, 0, "", "");
    }

    // Getters
    public String getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getRoomDesc() {
        return roomDesc;
    }

    public int getLength() {
        return length;
    }

    public int getBreadth() {
        return breadth;
    }

    public String getNodeIds() {
        return nodeIds;
    }

    public String getApplianceData() {
        return applianceData;
    }

    // Setters
    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public void setRoomDesc(String roomDesc) {
        this.roomDesc = roomDesc;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setBreadth(int breadth) {
        this.breadth = breadth;
    }

    public void setNodeIds(String nodeIds) {
        this.nodeIds = nodeIds;
    }

    public void setApplianceData(String applianceData) {
        this.applianceData = applianceData;
    }
    // New getters and setters for responseFound
    public boolean isResponseFound() {
        return responseFound;
    }
    public String getMapRoomUserId() {
        return mapRoomUserId;
    }
    private boolean hasExistingResponse;

    public boolean hasExistingResponse() {
        return hasExistingResponse;
    }

    public void setHasExistingResponse(boolean hasExistingResponse) {
        this.hasExistingResponse = hasExistingResponse;
    }
    // Setter for mapRoomUserId
    public void setMapRoomUserId(String mapRoomUserId) {
        this.mapRoomUserId = mapRoomUserId;
    }

    public void setResponseFound(boolean responseFound) {
        this.responseFound = responseFound;
    }
    @Override
    public String toString() {
        return "RoomModel{" +
                "roomId='" + roomId + '\'' +
                ", roomName='" + roomName + '\'' +
                ", roomDesc='" + roomDesc + '\'' +
                ", length=" + length +
                ", breadth=" + breadth +
                ", nodeIds='" + nodeIds + '\'' +
                ", applianceData='" + applianceData + '\'' +
                '}';
    }


    public boolean isHasNewRecommendation() {
        return hasNewRecommendation;
    }

    // Setter
    public void setHasNewRecommendation(boolean hasNewRecommendation) {
        this.hasNewRecommendation = hasNewRecommendation;
    }
}
