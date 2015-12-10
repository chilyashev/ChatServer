package server;

/**
 * Created by Mihail Chilyashev on 11/7/15.
 * All rights reserved, unless otherwise noted.
 */
public class Message {

    public static final int TYPE_INFO = 0x0;
    public static final int TYPE_ERROR = 0x1;
    public static final int TYPE_MESSAGE = 0x2;

    final static String[] colors = {"lightblue", "red", "grey"};

    // TODO: type, sender, receiver
    private String payload;

    private int type;

    public Message(int type) {
        this.type = type;
    }

    public Message(int type, String payload) {
        this.payload = payload;
        this.type = type;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getColor() {
        return colors[type]; // TODO: това е малоумно. Да се преправи да не е малоумно
    }

    @Override
    public String toString() {
        return payload;
    }
}
