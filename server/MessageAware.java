package server;

/**
 * Created by Mihail Chilyashev on 11/7/15.
 * All rights reserved, unless otherwise noted.
 */
public interface MessageAware {
    void onMessage(Message message);
    void sessionStatusChange(boolean closed);
}
