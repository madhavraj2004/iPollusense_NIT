package com.example.ipollusen;

public class RoomModel {
    private final String roomId; // Immutable Room ID
    private String roomName;
    private String roomDesc;
    private int length;
    private int breadth;

    // Constructor with dimensions
    public RoomModel(String roomId, String roomName, String roomDesc, int length, int breadth) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.roomDesc = roomDesc;
        this.length = length;
        this.breadth = breadth;
    }

    // Overloaded constructor without dimensions (defaulting to 0)
    public RoomModel(String roomId, String roomName, String roomDesc) {
        this(roomId, roomName, roomDesc, 0, 0);
    }

    public String getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public String getRoomDesc() { return roomDesc; }
    public int getLength() { return length; }
    public int getBreadth() { return breadth; }

    public void setRoomName(String roomName) { this.roomName = roomName; }
    public void setRoomDesc(String roomDesc) { this.roomDesc = roomDesc; }
    public void setLength(int length) { this.length = length; }
    public void setBreadth(int breadth) { this.breadth = breadth; }

    @Override
    public String toString() {
        return "RoomModel{" +
                "roomId='" + roomId + '\'' +
                ", roomName='" + roomName + '\'' +
                ", roomDesc='" + roomDesc + '\'' +
                ", length=" + length +
                ", breadth=" + breadth +
                '}';
    }
}
