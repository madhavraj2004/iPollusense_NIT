package com.example.ipollusen;

public class RoomCardModel {
    private String roomId;
    private String roomName;
    private String roomDesc;
    private int length;
    private int breadth;
    private String nodeIds; // For display, if available

    public RoomCardModel(String roomId, String roomName, String roomDesc, int length, int breadth, String nodeIds) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.roomDesc = roomDesc;
        this.length = length;
        this.breadth = breadth;
        this.nodeIds = nodeIds;
    }

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

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public void setRoomDesc(String roomDesc) {
        this.roomDesc = roomDesc;
    }
}
