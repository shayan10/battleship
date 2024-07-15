import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.Consumer;

public class Client extends Thread{
    Socket socketClient;
    ObjectOutputStream out;
    ObjectInputStream in;
    private Consumer<Serializable> callback;

    Client(Consumer<Serializable> call){ callback = call;}

    public void run() {

        try {
            socketClient= new Socket("127.0.0.1",5555);
            out = new ObjectOutputStream(socketClient.getOutputStream());
            in = new ObjectInputStream(socketClient.getInputStream());
            socketClient.setTcpNoDelay(true);
        }
        catch(Exception e) {}

        while(true) { //this loop is for exclusively checking the username--> might have to modify is for just message objects later tho
            try {
                Message message = (Message) in.readObject();
                callback.accept(message);
            }
            catch(Exception e) {}
//            try {
//                Object received = in.readObject();
//                if(received instanceof ArrayList){
//                    ArrayList<Message>activeUsers = (ArrayList<Message>) received;
//                    callback.accept(activeUsers);
//                }
//                else{
//                    Message getMessage = (Message)received;
//                    //System.out.println("Recieved from server " + "From: " + getMessage.userName + " Message: " + getMessage.userMessage + " Type: " + getMessage.messageType);
//                    callback.accept(getMessage);
//                }
//            }
//
//            catch(Exception e) {}
        }

    }

    public void send(Message data) {
        try {
            out.writeObject(data);
        } catch (IOException e) {
//             TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


}
