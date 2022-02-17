package TicTacToe;

import ArtificialIntelligence.Algorithms;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.Objects;
import java.util.Scanner;

/**
 * For playing Tic Tac Toe in the console.
 */
public class Console {

    private Board board;
    private Scanner sc = new Scanner(System.in);

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

                if (!tryAgain()) {
                    break;
                }
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
    private void printWinner () {
        Board.State winner = board.getWinner();

        System.out.println("\n" + board + "\n");

        if (winner == Board.State.Blank) {
            System.out.println("The TicTacToe is a Draw.");
        } else {
            System.out.println("Player " + winner.toString() + " wins!");
        }
    }

    /**
     * Reset the game if the player wants to play again.
     * @return      true if the player wants to play again
     */
    private boolean tryAgain () {
        if (promptTryAgain()) {
            board.reset();
            System.out.println("Started new game.");
            System.out.println("X's turn.");
            return true;
        }

        return false;
    }

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

    public static void main(String[] args) {
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
                            System.out.println("\n" + ticTacToe.board + "\n");
                            Algorithms.alphaBetaAdvanced(ticTacToe.board);
                            System.out.println("\n" + ticTacToe.board + "\n");
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
