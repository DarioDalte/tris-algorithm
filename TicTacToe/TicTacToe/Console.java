package TicTacToe;


import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.internet.MimeMultipart;

import java.util.Objects;
import java.util.Scanner;



/**
 * For playing Tic Tac Toe in the console.
 */
public class Console {

    public String myName = "trisser.bot2@gmail.com";

    /**
     * Construct Console.
     */
    public Console(int instance, String room, boolean myTurn, String enemy, int number) {
        Board board = new Board();
        System.out.println("istanza " + instance + "  " + myTurn);
        System.out.println("room " + room);
        System.out.println("enemy " + enemy);




        AlphaBetaAdvanced test = new AlphaBetaAdvanced();


        //String topic2 = room + "/" + instance + "/" + enemy;
        //System.out.println(topic2);


        try {
            String broker = "tcp://localhost:1883";

            String PubId = "134.0.0." + (instance + number);
            System.out.println(PubId);

            MemoryPersistence persistence = new MemoryPersistence();
            MqttClient sampleClient = new MqttClient(broker, PubId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setConnectionTimeout(60);
            connOpts.setKeepAliveInterval(60);
            connOpts.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);

            //IN CASO DI PARTITA COL SERVER SCOMMENTARE USERNAME E PASSWORD
            //connOpts.setUserName(userName);
            //connOpts.setPassword(pwd.toCharArray());

            System.out.println("Connecting to broker: " + broker);
            sampleClient.connect(connOpts);
            System.out.println("Connected");

            sampleClient.setCallback(new MqttCallback() {
                public void connectionLost(Throwable cause) {}

                public void messageArrived(String topic, MqttMessage message) throws Exception {
                   // System.out.println("\nistanza: " + instance);
                    //System.out.println(topic + " says: \n" + message.toString());
                    String msg = message.toString();
                    JSONParser parser = new JSONParser();
                    JSONObject json = (JSONObject) parser.parse(msg);

                    if(!Objects.isNull(json.get("not_connected"))){
                        if(enemy.equals(json.get("not_connected"))){
                            System.exit(0);
                        }
                    }

                    if(!Objects.isNull(json.get("game"))){
                        if(json.get("game").equals("start")){

                            if(myTurn){
                                int AIMove = makeMove(board, test);
                                System.out.println("Mossa AI: " + AIMove);

                                JSONObject json3 = new JSONObject();
                                json3.put("move", Integer.toString(AIMove));

                                message = new MqttMessage(json3.toString().getBytes());
                                topic = room + "/" + instance + "/" + myName;

                                sampleClient.publish(topic, message);
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
                            int AIMove = makeMove(board, test);
                            //System.out.println("Mossa AI: " + AIMove);

                            json = new JSONObject();
                            json.put("move", Integer.toString(AIMove));

                            message = new MqttMessage(json.toString().getBytes());
                            topic = room + "/" + instance + "/" + myName;
                            sampleClient.publish(topic, message);

                        }


                        if (board.isGameOver()) {
                            printWinner(board);
                        }

                    }else{
                        //System.out.println("Nulla casso");
                    }
                }
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });


            String topic = room + "/" + instance + "/" + enemy;
            sampleClient.subscribe(topic);
            System.out.println("istanza " + instance + "iscritto: " + topic);
            sampleClient.subscribe("broadcast");



        }catch(MqttException me) {
            System.out.println("Reason :"+ me.getReasonCode());
            System.out.println("Message :"+ me.getMessage());
            System.out.println("Local :"+ me.getLocalizedMessage());
            System.out.println("Cause :"+ me.getCause());
            System.out.println("Exception :"+ me);
            me.printStackTrace();
        }



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



    public int makeMove(Board board, AlphaBetaAdvanced test){

        test.run(board.getTurn(), board, Double.POSITIVE_INFINITY);
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




    public  void main(String[] args) {






        //ticTacToe.play();
    }

}
