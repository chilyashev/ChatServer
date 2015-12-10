package server.messaging;

/**
 * Клас, описващ типовете съобщения, на които може да реагира сървърът.
 */
public class ChatMessage {
    public static final int TYPE_INVALID = 0x0;
    public static final int TYPE_PING = 0x1;
    /**
     * Изпращане на чат съобщение от един потребител към друг
     */
    public static final int TYPE_MSG = 0x2;
    public static final int TYPE_EXIT = 0x3;
    /**
     * Вземане на списък с приятели
     */
    public static final int TYPE_GET_FRIENDS = 0x4;
    /**
     * Вход в системата
     */
    public static final int TYPE_LOGIN = 0x5;
    /**
     * Добавяне на чат клиент
     */
    public static final int TYPE_REGISTER_READER = 0x6;

    public static final int LAST_TYPE = 0x6;

    private int type;
    private int receiverId;
    private int senderId;
    private String message;
    private String senderName;

    /**
     * Проверява дали съобщението е валидно
     * @throws InvalidMessageException Ако съобщението не е валидно
     */
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
