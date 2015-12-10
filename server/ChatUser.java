package server;

import java.util.ArrayList;

/**
 * Клас, описващ потребител в базата от данни.
 */
public class ChatUser {
    /**
     * id на потребиля
     */
    public int id;
    /**
     * Име
     */
    public String name;
    /**
     * Хора, с които може да пише
     */
    public ArrayList<ChatUser> friends;

    public ChatUser() {
    }

    public ChatUser(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<ChatUser> getFriends() {
        return friends;
    }

    public void setFriends(ArrayList<ChatUser> friends) {
        this.friends = friends;
    }
}
