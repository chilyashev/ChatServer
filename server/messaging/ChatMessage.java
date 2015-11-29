package server.messaging;

/**
 * Created by Mihail Chilyashev on 11/9/15.
 * All rights reserved, unless otherwise noted.
 */
public class ChatMessage {
    public static final int TYPE_INVALID = 0x1;
    public static final int TYPE_START = 0x1;
    public static final int TYPE_MSG = 0x2;
    public static final int TYPE_EXIT = 0x3;
    public static final int TYPE_GET_FRIENDS = 0x4;

    public static final int LAST_TYPE = 0x4;

    private int type;
    private int receiverId;
    private int senderId;
    private String message;
    private String senderName;

    public void validate() throws InvalidMessageException{
        if (type < TYPE_INVALID || type > LAST_TYPE){
            throw new InvalidMessageException("Invalid message type");
        }
        // TODO: друга валидация - id на получател, изпращач и т.н.
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
}
