package com.example.ipollusen;

import java.io.Serializable;

public class RoomModel implements Serializable {  // ✅ Ensuring Serializable
    private String roomId;
    private String roomName;
    private String roomDesc;

    // ✅ Constructor for fetching rooms (includes roomId)
    public RoomModel(String roomId, String roomName, String roomDesc) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.roomDesc = roomDesc;
    }

    // ✅ Constructor for manually creating rooms (without roomId)
    public RoomModel(String roomName, String roomDesc) {
        this.roomName = roomName;
        this.roomDesc = roomDesc;
        this.roomId = null;  // ID will be assigned when stored in the database
    }

    // ✅ Getter Methods
    public String getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public String getRoomDesc() { return roomDesc; }

    // ✅ Setter Methods
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public void setRoomDesc(String roomDesc) { this.roomDesc = roomDesc; }
}
