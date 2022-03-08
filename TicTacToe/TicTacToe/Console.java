package TicTacToe;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.util.Objects;





public class Console {


    private String myName = "trisser.bot2@gmail.com";
    private AlphaBetaAdvanced AI;
    private Board board;
    private int instance;
    private String room;
    private boolean myTurn;
    private String enemy;
    private int number;
    private MqttClient client;
    private String myTopic;
    private String username;
    private String pwd;



    public void clientConnection(){
        try{
            String broker = "tcp://localhost:1883";

            String PubId = "134.0.0." + (instance + number);
            //System.out.println(PubId);

            MemoryPersistence persistence = new MemoryPersistence();
            client = new MqttClient(broker, PubId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setConnectionTimeout(60);
            connOpts.setKeepAliveInterval(60);
            connOpts.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);

            //IN CASO DI PARTITA COL SERVER SCOMMENTARE USERNAME E PASSWORD
            //connOpts.setUserName(username);
            //connOpts.setPassword(pwd.toCharArray());

            // System.out.println("Connecting to broker: " + broker);
            client.connect(connOpts);
            //System.out.println("Connected");

            //ISCRIZIONE ALLE TOPIC
            client.subscribe("broadcast"); //Mi iscrivo alla topic broadcast
            //Mi iscrivo alla topic del nemico per ricevere le sue mosse.
            String topic = room + "/" + instance + "/" + enemy;
            client.subscribe(topic);


        }catch (Exception e){
            System.out.println(e.getMessage());
        }

    }

    public void setCallback(){
        client.setCallback(new MqttCallback() {
            public void connectionLost(Throwable cause) {}

            public void messageArrived(String topic, MqttMessage message) throws Exception {
                //System.out.println("\nistanza: " + instance);
                //System.out.println(topic + " says: \n" + message.toString());
                String msg = message.toString();
                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(msg);


                if(!Objects.isNull(json.get("game"))){
                    if(json.get("game").equals("start")){
                        if(myTurn){
                            int AIMove = makeMove(board, AI);
                            JSONObject JSONmove = new JSONObject();
                            JSONmove.put("move", Integer.toString(AIMove));
                            message = new MqttMessage(JSONmove.toString().getBytes());
                            client.publish(myTopic, message);
                        }
                    }
                }

                if(!Objects.isNull(json.get("move"))){
                    String moveStr = json.get("move").toString() ;
                    int move = Integer.parseInt(moveStr) - 1;
                    if (move < 0 || move >= board.BOARD_WIDTH* board.BOARD_WIDTH) {
                        System.out.println("\nInvalid move.");
                        System.out.println("\nThe index of the move must be between 0 and "
                                + (board.BOARD_WIDTH * board.BOARD_WIDTH - 1) + ", inclusive.");
                    } else if (!board.move(move)) {
                        System.out.println("\nInvalid move.");
                        System.out.println("\nThe selected index must be blank.");
                        System.out.println(board);
                    }else{
                        //System.out.println(board);
                        int AIMove = makeMove(board, AI);
                        //System.out.println("Mossa AI: " + AIMove);
                        JSONObject JSONmove = new JSONObject();
                        JSONmove.put("move", Integer.toString(AIMove));
                        message = new MqttMessage(JSONmove.toString().getBytes());
                        client.publish(myTopic, message);
                    }

                    if (board.isGameOver()) {
                        printWinner(board);
                    }

                }
            }
            public void deliveryComplete(IMqttDeliveryToken token) {}
        });

    }

    public Console(int instance, String room, boolean myTurn, String enemy, int number, String username, String pwd) {
        this.instance = instance;
        this.room = room;
        this.myTurn = myTurn;
        this.enemy = enemy;
        this.number = number;
        this.username = username;
        this.pwd = pwd;

        board = new Board();
        AI = new AlphaBetaAdvanced();
        this.myTopic = room + "/" + instance + "/" + myName;


//       System.out.println("---------" +
//                "\nRoom: " + room +
//                "\nIstanza " + instance +
//                "\nMio turno: " + myTurn  +
//                "\nNemico: " + enemy +
//                "\nMando i messaggi a: " + room + "/" + instance + "/" + myName +
//                "\nRicevo i messaggi su: " + room + "/" + instance + "/" + enemy +
//                "\n----------\n\n");

        clientConnection();
        setCallback();

    }


    /**
     * Print out the winner of the game.
     */
    private void printWinner (Board board) {
        Board.State winner = board.getWinner();

        if (winner == Board.State.Blank) {
            System.out.println("The TicTacToe is a Draw.");
        } else {
            System.out.println("Player " + winner.toString() + " wins!");
        }
        //System.exit(0);
    }



    public int makeMove(Board board, AlphaBetaAdvanced AI){

        AI.run(board.getTurn(), board, Double.POSITIVE_INFINITY);
        //System.out.println("\n" + board + "\n");
        int AIMove;

        if(board.rowSelected == 0){
            if(board.columnSelected == 0){
                AIMove = 1;
            }else if(board.columnSelected == 1){
                AIMove = 2;
            }else{
                AIMove = 3;
            }
        }else if(board.rowSelected == 1){
            if(board.columnSelected == 0){
                AIMove = 4;
            }else if(board.columnSelected == 1){
                AIMove = 5;
            }else{
                AIMove = 6;
            }
        }else{
            if(board.columnSelected == 0){
                AIMove = 7;
            }else if(board.columnSelected == 1){
                AIMove = 8;
            }else{
                AIMove = 9;
            }
        }

        return AIMove;
    }

}
