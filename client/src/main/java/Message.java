import java.io.Serializable;
import java.util.ArrayList;

public class Message implements Serializable {
    static final long serialVersionUID = 42L;
    String type;
    String content;
    String username;
    String recipient;
    ArrayList<ArrayList<Integer>> cells;

    Message(String type, String content) {
        this.type = type;
        this.content = content;
    }

    Message(String type, String content, String username) {
        this.type = type;
        this.content = content;
        this.username = username;
    }

    Message(String type, String content, String username, ArrayList<ArrayList<Integer>> cells) {
        this.type = type;
        this.content = content;
        this.username = username;
        this.cells = cells;
    }
    public Message(String type, String content, String msgSender, String msgRecipient) { // individual message
        this.type = type;
        this.content = content;
        this.username = msgSender;
        this.recipient = msgRecipient;
    }

    public Message() {
    }
}
