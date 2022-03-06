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



/**
 * For playing Tic Tac Toe in the console.
 */
public class Console {


    public Algorithms test;

    /**
     * Construct Console.
     */
    public Console(int instance, MqttClient sampleClient, String room, boolean myTurn, String enemy) {
        Board board = new Board();

;
        test = new Algorithms();

        //String topic2 = room + "/" + instance + "/" + enemy;
        //System.out.println(topic2);




       //Console ticTacToe = this;


        try {

            //Console ticTacToe = new Console();

            sampleClient.setCallback(new MqttCallback() {
                public void connectionLost(Throwable cause) {}

                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    System.out.println(topic + " says: \n" + message.toString());
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
                                int AIMove = makeMove(board);
                                System.out.println("Mossa AI: " + AIMove);

                                JSONObject json3 = new JSONObject();
                                json3.put("move", AIMove);

                                message = new MqttMessage(json3.toString().getBytes());
                                topic = room + "/" + instance + "/" + "trisser.bot2@gmail.com";

                                sampleClient.publish(topic, message);
                            }
                        }
                    }


                    if(!Objects.isNull(json.get("move"))){

                        String moveStr = json.get("move").toString() ;
                        int move = Integer.parseInt(moveStr) - 1;


                        System.out.println(board);
                        if (move < 0 || move >= board.BOARD_WIDTH* board.BOARD_WIDTH) {
                            System.out.println("\nInvalid move.");
                            System.out.println("\nThe index of the move must be between 0 and "
                                    + (board.BOARD_WIDTH * board.BOARD_WIDTH - 1) + ", inclusive.");
                        } else if (!board.move(move)) {
                            System.out.println("\nInvalid move.");
                            System.out.println("\nThe selected index must be blank.");
                            System.out.println(board);
                        }else{

                            System.out.println(board);
                            int AIMove = makeMove(board);
                            System.out.println("Mossa AI: " + AIMove);

                            json = new JSONObject();
                            json.put("move", AIMove);

                            message = new MqttMessage(json.toString().getBytes());
                            topic = room + "/" + instance + "/" + "trisser.bot2@gmail.com";
                            sampleClient.publish(topic, message);




                        }


                        if (board.isGameOver()) {
                            printWinner(board);
                        }

                    }else{
                        System.out.println("Nulla casso");
                    }
                }
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });


            String topic = room + "/" + instance + "/" + enemy;
            sampleClient.subscribe(topic);
            sampleClient.subscribe("broadcast");




            //Dico al server che sono online
            JSONObject json2 = new JSONObject();
            json2.put("trisser.bot2@gmail.com", "true");

            topic = "online/trisser.bot2@gmail.com";
            MqttMessage message = new MqttMessage(json2.toString().getBytes());
            sampleClient.publish(topic, message);







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
        System.exit(0);
    }



    public int makeMove(Board board){



        System.out.println("test board: \n" + board);
        test.alphaBetaAdvanced(board);
        System.out.println("\n" + board + "\n");
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
