package server;

import java.util.ArrayList;

/**
 * Created by Mihail Chilyashev on 11/9/15.
 * All rights reserved, unless otherwise noted.
 */
public class ChatUser {
    public int id;
    public String name;
    public ArrayList<ChatUser> friends;

    public ChatUser() {
    }

    public ChatUser(int id, String name) {
        this.id = id;
        this.name = name;
    }

}
