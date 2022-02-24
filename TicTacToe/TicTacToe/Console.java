package TicTacToe;

import ArtificialIntelligence.Algorithms;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.internet.MimeMultipart;
import java.util.Objects;
import java.util.Scanner;

import java.util.ArrayList;
import java.util.Properties;

import javax.mail.*;
import javax.mail.Flags.Flag;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;

/**
 * For playing Tic Tac Toe in the console.
 */
public class Console {

    private static Board board;
    private Scanner sc = new Scanner(System.in);
    private static boolean myTurn;

    /**
     * Construct Console.
     */
    private Console() {
       board = new Board();
    }

    /**
     * Begin the game.
     */
    private void play () {

        System.out.println("Starting a new game.");

        while (true) {
            printGameStatus();

            if (board.isGameOver()) {
                printWinner();

                //if (!tryAgain()) {
                  //  break;
                //}
                break;
            }
        }
    }

    /**
     * Handle the move to be played, either by the player or the AI.

    private void playMove () {
        if (board.getTurn() == Board.State.X) {
            getPlayerMove();
        } else {
            Algorithms.alphaBetaAdvanced(board);
        }
    } */

    /**
     * Print out the board and the player who's turn it is.
     */
    private void printGameStatus () {
        System.out.println("\n" + board + "\n");
        System.out.println(board.getTurn().name() + "'s turn.");
    }

    /**
     * For reading in and interpreting the move that the user types into the console.
     */
    private void getPlayerMove (int move) {

        if (move < 0 || move >= Board.BOARD_WIDTH* Board.BOARD_WIDTH) {
            System.out.println("\nInvalid move.");
            System.out.println("\nThe index of the move must be between 0 and "
                    + (Board.BOARD_WIDTH * Board.BOARD_WIDTH - 1) + ", inclusive.");
        } else if (!board.move(move)) {
            System.out.println("\nInvalid move.");
            System.out.println("\nThe selected index must be blank.");
        }
    }

    /**
     * Print out the winner of the game.
     */
    private static void printWinner () {
        Board.State winner = board.getWinner();



        if (winner == Board.State.Blank) {
            System.out.println("The TicTacToe is a Draw.");
        } else {
            System.out.println("Player " + winner.toString() + " wins!");
        }
        System.exit(0);
    }

    /**
     * Reset the game if the player wants to play again.
     * @return      true if the player wants to play again

    private boolean tryAgain () {
        if (promptTryAgain()) {
            board.reset();
            System.out.println("Started new game.");
            System.out.println("X's turn.");
            return true;
        }

        return false;
    } */

    /**
     * Ask the player if they want to play again.
     * @return      true if the player wants to play again
     */
    private boolean promptTryAgain () {
        while (true) {
            System.out.print("Would you like to start a new game? (Y/N): ");
            String response = sc.next();
            if (response.equalsIgnoreCase("y")) {
                return true;
            } else if (response.equalsIgnoreCase("n")) {
                return false;
            }
            System.out.println("Invalid input.");
        }
    }


    public static int makeMove(Console ticTacToe){


        Algorithms.alphaBetaAdvanced(ticTacToe.board);
        System.out.println("\n" + ticTacToe.board + "\n");
        int AIMove;

        if(Board.rowSelected == 0){
            if(Board.columnSelected == 0){
                AIMove = 1;
            }else if(Board.columnSelected == 1){
                AIMove = 2;
            }else{
                AIMove = 3;
            }
        }else if(Board.rowSelected == 1){
            if(Board.columnSelected == 0){
                AIMove = 4;
            }else if(Board.columnSelected == 1){
                AIMove = 5;
            }else{
                AIMove = 6;
            }
        }else{
            if(Board.columnSelected == 0){
                AIMove = 7;
            }else if(Board.columnSelected == 1){
                AIMove = 8;
            }else{
                AIMove = 9;
            }
        }

        return AIMove;
    }

    private static String getTextFromMessage(Message message) throws Exception{
        String result = "";
        if (message.isMimeType("text/plain")) {
            result = message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            result = getTextFromMimeMultipart(mimeMultipart);
        }
        return result;
    }

    private static String getTextFromMimeMultipart(
            MimeMultipart mimeMultipart)  throws Exception{
        String result = "";
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result = result + "\n" + bodyPart.getContent();
                break; // without break same text appears twice in my tests
            } else if (bodyPart.isMimeType("text/html")) {
                String html = (String) bodyPart.getContent();
                result = result + "\n" + org.jsoup.Jsoup.parse(html).text();
            } else if (bodyPart.getContent() instanceof MimeMultipart){
                result = result + getTextFromMimeMultipart((MimeMultipart)bodyPart.getContent());
            }
        }
        return result;
    }

    public static void main(String[] args) {

        String host = "imap.gmail.com";
        String mailStoreType = "imap";
        String username = "trisser.bot2@gmail.com";
        String password = "trisserbot2!";


        try {

            // create properties
            Properties properties = new Properties();

            properties.put("mail.imap.host", host);
            properties.put("mail.imap.port", "993");
            properties.put("mail.imap.starttls.enable", "true");
            properties.put("mail.imap.ssl.trust", host);

            Session emailSession = Session.getDefaultInstance(properties);

            // create the imap store object and connect to the imap server
            Store store = emailSession.getStore("imaps");

            store.connect(host, username, password);

            // create the inbox object and open it
            Folder inbox = store.getFolder("Inbox");
            inbox.open(Folder.READ_WRITE);

            // retrieve the messages from the folder in an array and print it
            Message[] messages = inbox.search(new FlagTerm(new Flags(Flag.SEEN), false));
            System.out.println("messages.length---" + messages.length);

            int i = 0;
            Message message = messages[i];
            message.setFlag(Flag.SEEN, true);
            System.out.println("---------------------------------");
            System.out.println("Email Number " + (i + 1));
            System.out.println("Subject: " + message.getSubject());
            System.out.println("From: " + message.getFrom()[0]);
            System.out.println("Text: " + message.getContent().toString());
            System.out.println(message.getContentType());
            String result = getTextFromMessage(message);
            System.out.println(result);
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(result);
            ArrayList<String> rooms = (ArrayList<String>) json.get("rooms");
            String first_move = json.get("first_move").toString();
            System.out.println(first_move);

            for(i = 0; i< rooms.size(); i++){
                rooms.set(0, rooms.get(i).replaceAll("\\r\\n|\\r|\\n", " "));
            }
            System.out.println(rooms);


            if(first_move.equals("trisser.bot2@gmail.com")){
                System.out.println("si");
                myTurn = true;
            }else{
                System.out.println("no");
                myTurn = false;
            }



            inbox.close(false);
            store.close();

        } catch (Exception e) {
            System.out.println("Non ci sono email");
            System.exit(0);
        }



        Console ticTacToe = new Console();
        try {
            int qos = 1;
            //CONNESSINE AL BROKER
            String broker = "tcp://localhost:1883";
            String PubId = "127.0.0.1";

            MemoryPersistence persistence = new MemoryPersistence();
            MqttClient sampleClient = new MqttClient(broker, PubId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setConnectionTimeout(60);
            connOpts.setKeepAliveInterval(60);
            connOpts.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
            connOpts.setUserName("mqtt");
            connOpts.setPassword("test".toCharArray());
            System.out.println("Connecting to broker: " + broker);

            sampleClient.setCallback(new MqttCallback() {
                public void connectionLost(Throwable cause) {}

                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    //System.out.println(topic + " says: \n" + message.toString());
                    String msg = message.toString();
                    JSONParser parser = new JSONParser();
                    JSONObject json = (JSONObject) parser.parse(msg);


                    if(!Objects.isNull(json.get("move"))){

                        String moveStr = json.get("move").toString() ;
                        int move = Integer.parseInt(moveStr) - 1;


                        if (move < 0 || move >= Board.BOARD_WIDTH* Board.BOARD_WIDTH) {
                            System.out.println("\nInvalid move.");
                            System.out.println("\nThe index of the move must be between 0 and "
                                    + (Board.BOARD_WIDTH * Board.BOARD_WIDTH - 1) + ", inclusive.");
                        } else if (!ticTacToe.board.move(move)) {
                            System.out.println("\nInvalid move.");
                            System.out.println("\nThe selected index must be blank.");
                        }else{

                            System.out.println(ticTacToe.board);
                           int AIMove = makeMove(ticTacToe);
                           System.out.println("Mossa AI: " + AIMove);

                           json = new JSONObject();
                            json.put("move", AIMove);

                            message = new MqttMessage(json.toString().getBytes());
                            sampleClient.publish("topic", message);


                        }


                        if (board.isGameOver()) {
                            printWinner();
                        }

                    }else{
                        System.out.println("Nulla casso");
                    }
                }
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });


            sampleClient.connect(connOpts);
            sampleClient.subscribe("dalterio.dario@einaudicorreggio.it/1"); //STANZA 1, DA CAMBIARE DINAMICAMENTE
            sampleClient.subscribe("broadcast");

            System.out.println("Connected");


            String topic = "online/dalterio.dario@einaudicorreggio.it";
            String msg = "True";
            MqttMessage message = new MqttMessage(msg.getBytes());
            message.setQos(qos);
            sampleClient.publish(topic, message);
            System.out.println("Message published");



            if(myTurn){
                int AIMove = makeMove(ticTacToe);
                System.out.println("Mossa AI: " + AIMove);

                JSONObject json = new JSONObject();
                json.put("move", AIMove);

                message = new MqttMessage(json.toString().getBytes());
                sampleClient.publish("topic", message);
            }


        }catch(MqttException me) {
            System.out.println("Reason :"+ me.getReasonCode());
            System.out.println("Message :"+ me.getMessage());
            System.out.println("Local :"+ me.getLocalizedMessage());
            System.out.println("Cause :"+ me.getCause());
            System.out.println("Exception :"+ me);
            me.printStackTrace();
        }



        //ticTacToe.play();
    }

}
