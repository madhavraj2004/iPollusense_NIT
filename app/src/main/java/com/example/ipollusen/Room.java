package com.example.ipollusen;

public class Room {
    private final String roomId;
    private final String roomName;
    private final String roomDesc;

    public Room(String roomId, String roomName, String roomDesc) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.roomDesc = roomDesc;
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
}
