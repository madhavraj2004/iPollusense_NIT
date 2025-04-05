package com.example.ipollusen;

public class RoomModel {
    private final String roomId;
    private String roomName;
    private String roomDesc;
    private int length;
    private int breadth;
    private String nodeIds;

    // Full constructor
    public RoomModel(String roomId, String roomName, String roomDesc, int length, int breadth, String nodeIds) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.roomDesc = roomDesc;
        this.length = length;
        this.breadth = breadth;
        this.nodeIds = nodeIds;
    }

    // Constructor without nodeIds
    public RoomModel(String roomId, String roomName, String roomDesc, int length, int breadth) {
        this(roomId, roomName, roomDesc, length, breadth, "");
    }

    // Constructor without dimensions or nodeIds
    public RoomModel(String roomId, String roomName, String roomDesc) {
        this(roomId, roomName, roomDesc, 0, 0, "");
    }

    // Getters and setters...

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

    public void setLength(int length) {
        this.length = length;
    }

    public void setBreadth(int breadth) {
        this.breadth = breadth;
    }

    public void setNodeIds(String nodeIds) {
        this.nodeIds = nodeIds;
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
                '}';
    }
}
