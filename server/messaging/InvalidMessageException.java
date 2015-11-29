package server.messaging;

/**
 * Created by Mihail Chilyashev on 11/9/15.
 * All rights reserved, unless otherwise noted.
 */
public class InvalidMessageException extends Exception {
    public InvalidMessageException(String message) {
        super(message);
    }
}
