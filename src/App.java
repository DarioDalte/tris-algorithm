import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import javax.mail.*;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Properties;
import java.util.Scanner;


/**bot that, through the MQTT protocol, is able to play multiple games of tic-tac-toe simultaneously
 *
 * @author D'Alterio Dario
 * @author Shaukat Arslan
 * @author Jacopo Beltrami
 * @author Navraj Singh
 * @author Marzola Alessandro
 *
 * @version 1.1
 */
public class App extends Thread {

    /* Attributes */
    private static AlphaBetaAdvanced AI = new AlphaBetaAdvanced();
    private static MqttClient client;
    private static String userName;
    private static String pwd;
    private static int games;
    private static ArrayList<String> rooms;
    private static ArrayList<ArrayList<Board>> boards  = new ArrayList<>();
    private static ArrayList<ArrayList<Boolean>> starts = new ArrayList<>();
    private static String myName = "trisser.bot2@gmail.com";
    private JSONObject JSONmessage;
    private String topic;
    private static int lose;
    private static int win;
    private static int draw;
    private static boolean thereIsEnemy = true;
    private static boolean emailError = true;


    /* Constructor */
    public App(JSONObject JSONmessage, String topic) {
        this.JSONmessage = JSONmessage;
        this.topic = topic;
    }

    /**
     * Start multiple threads to initialize different room simultaneously
     * @param i room number (eg 0, 1, 2)
     * */
    public static void initializeRoom(int i){
        Thread thread1 = new Thread () {
            public void run () {
                String myTopic;
                for (int x = 0; x < starts.get(i).size(); x++) {
                    if(starts.get(i).get(x)){
                        myTopic = rooms.get(i) + "/" + x + "/" + myName;
                        makeMove(boards.get(i).get(x), myTopic);
                    }
                }
            }
        };
        thread1.start();
    }

    /** Thread that will manage the opponent's moves */
    @Override
    public void run() {

            boolean error = false;
            int instance;

            if(!topic.equals("broadcast")){
                String[] topicParts = topic.split("/");
                String room = topicParts[0];
                String myTopic = room + "/" + topicParts[1] + "/" + myName;
                instance = Integer.parseInt(topicParts[1]);

                int roomInstance = rooms.indexOf(room);
                if(!Objects.isNull(JSONmessage.get("move"))){
                    String moveStr = JSONmessage.get("move").toString();
                    int move = Integer.parseInt(moveStr) - 1;
                    if (move < 0 || move >= 9) {
                        System.out.println("\nMossa del nemico invalida");
                    } else if (!boards.get(roomInstance).get(instance).move(move)) {
                        System.out.println("\nMossa del nemico invalida");
                        error = true;
                    }else{
                        if(!boards.get(roomInstance).get(instance).isGameOver()){
                            makeMove(boards.get(roomInstance).get(instance), myTopic);
                        }
                    }
                }

                //Check Game over in a board
                if (boards.get(roomInstance).get(instance).isGameOver() && !error) {
                    gameOver(boards.get(roomInstance).get(instance));
                }
            }
    }


    private  static String getTextFromMessage(Message message) throws Exception{
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


    /**
     * Take last unread mail with object equals to 'GAME', interprets all its data and last delete this mail.
     * */
    public static void getEmail(){
        String host = "imap.gmail.com";
        String mailStoreType = "imap";
        //Mail data
        String username = "trisser.bot2@gmail.com";
        String password = "trisserbot2!";

        try{
            // create properties
            Properties properties = new Properties();
            System.out.println("\nLeggo la mail...");

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
            SearchTerm searchTerm = new AndTerm(new SubjectTerm("GAME"), new BodyTerm("GAME"));
            Message[] messages = inbox.search(searchTerm);
            messages[0].setFlag(Flags.Flag.DELETED, true);

            int i = 0;
            Message message = messages[i];
            String result = getTextFromMessage(message);

            inbox.close(false);
            store.close();

            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(result);

            //Take from the email the number of games you need to play
            games = Integer.parseInt(json.get("room_instance").toString());


            //Take from the email the username and password to connect to the broker
            userName = json.get("user").toString();
            pwd = json.get("pwd").toString();


            //Take the rooms from the email, I will need them later to subscribe to the topic
            rooms = (ArrayList<String>) json.get("rooms");


            //Remove all the room which not contain my name
            for(i = 0; i< rooms.size(); i++){
                if(rooms.get(i).contains("trisser.bot2@gmail.com")){
                    rooms.set(i, rooms.get(i).replaceAll("\\r\\n|\\r|\\n", ""));
                }else{
                    rooms.remove(i);
                }
            }

            System.out.println("\nMail letta.\n");
            emailError = false;
        }catch (Exception e){//The email does not exist or has already been read
            System.out.print("Errore nella lettura della mail, vuoi riprovare? (s/n): ");
            Scanner chooseScanner = new Scanner(System.in);
            String choose = chooseScanner.nextLine();
            if(!choose.equals("s")){
                System.out.println("Ok esco...");
                System.exit(0);
            }
        }

    }

    /**
     * This function will sett all the option for the connection to the broker and then try to connect to it.
     * After the connection will set a callback to receive all messages sent by the server
     *
     * @param broker ip of the broker to which our client will have to connect to
     * @param PubId unique identifier of our bot
     * */
    public static void connectClient(String broker, String PubId){
        try{

            //----------- STAR BROKER CONNECTION -----------
            MemoryPersistence persistence = new MemoryPersistence();
            client = new MqttClient(broker, PubId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setConnectionTimeout(60);
            connOpts.setKeepAliveInterval(60);
            connOpts.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
            connOpts.setMaxInflight(1000000);
            connOpts.setUserName(userName);
            connOpts.setPassword(pwd.toCharArray());

            System.out.println("Mi connetto al broker: " + broker);
            client.connect(connOpts);
            System.out.println("Connesso!");
            //----------- END BROKER CONNECTION -----------

            //Inform the server that i im online
            JSONObject json2 = new JSONObject();
            json2.put("trisser.bot2@gmail.com", "true");

            String topic = "online/trisser.bot2@gmail.com";
            MqttMessage msg = new MqttMessage(json2.toString().getBytes());
            msg.setQos(1);
            client.publish(topic, msg);

            client.subscribe("broadcast"); //Subscribe to broadcast topic

            //Callback to receive all messages sent by the server
            client.setCallback(new MqttCallback() {
                public void connectionLost(Throwable cause) {}

                public void messageArrived(String topic, MqttMessage message) throws Exception {

                    String msg = message.toString();
                    JSONParser parser = new JSONParser();
                    JSONObject json = (JSONObject) parser.parse(msg);

                    //Server says Game Start
                    if(!Objects.isNull(json.get("game")) && thereIsEnemy){
                        System.out.println("\nInizio!");
                        System.out.println("\nSto giocando...");
                        for (int i = 0; i < rooms.size(); i++) {
                            initializeRoom(i);
                        }
                    }

                    //the server says that someone did not connect, so i will remove it from my rooms array
                    if(!Objects.isNull(json.get("not_connected"))){
                        for (int i = 0; i < rooms.size(); i++) {
                            if(rooms.get(i).contains(json.get("not_connected").toString())){
                                rooms.remove(i);
                            }
                        }
                        //There are no bot
                        if(rooms.isEmpty() && thereIsEnemy){
                            thereIsEnemy = false;
                            System.out.println("Nessun nemico si è connesso");
                            System.out.println("Aspetto la classifica...");
                        }

                    }

                    //Server says the result so the game is over.
                    if(!Objects.isNull(json.get("1"))){
                        System.out.println("\nClassifica: \n" + msg);
                        System.out.println("\nPartite Vinte: " + win);
                        System.out.println("Partite Perse: " + lose);
                        System.out.println("Partite Pareggiate: " + draw);
                        System.exit(0);
                    }

                   if(!topic.equals("broadcast")){
                       new App(json, topic).start(); //Start a thread which will manage the enemy move.
                   }
                }
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    /**
     * This function will initialize the two arraylist used to manage all the matches: starts and boards.
     * The two arraylist will contain n arraylist as many as there are rooms.
     *
     * <ul>
     *     <li>
     *       an internal starts' arraylist will contain n flag (true or false) as many as there are the room instances.
     *       true means: in this room i will start and false means: in this room enemy will start.
     *     </li>
     *     <li>
     *         an internal boards' arraylist will contain n tic-tac-toe board as many as there are the room instances.
     *     </li>
     * </ul>
     * */
    public static void initializeArrays(){
        try {
            String topic;
            boolean start; //start = true means that the bot will start in even-numbered instances.
            String roomEnemy;

            ArrayList<Board> boardsTemp = new ArrayList<>();
            ArrayList<Boolean> startsTemp = new ArrayList<>();

            for (String room : rooms) {

                String[] partsRoom = room.split("_");
                roomEnemy = partsRoom[0];
                start = false;

                if(partsRoom[0].equals("trisser.bot2@gmail.com")){ //Eg. trisser.bot2@gmail.com_trisser.bot3@gmail.com
                    roomEnemy = partsRoom[1];
                    start = true;
                }

                for(int y = 0; y < games; y++){
                    topic = room + "/" + y + "/" + roomEnemy; //Eg. trisser.bot2@gmail.com_trisser.bot3@gmail.com/2/trisser.bot3@gmail.com
                    client.subscribe(topic);

                    if(start){
                        if(y%2 == 0){
                            startsTemp.add(true);
                            boardsTemp.add(new Board(Board.State.X));
                        }else{
                            startsTemp.add(false);
                            boardsTemp.add(new Board(Board.State.O));
                        }
                    }else{
                        if(y%2 != 0){
                            startsTemp.add(true);
                            boardsTemp.add(new Board(Board.State.X));
                        }else{
                            startsTemp.add(false);
                            boardsTemp.add(new Board(Board.State.O));
                        }
                    }
                }

                ArrayList clone = (ArrayList)startsTemp.clone();
                starts.add(clone);

                clone = (ArrayList)boardsTemp.clone();
                boards.add(clone);

                boardsTemp.clear();
                startsTemp.clear();
            }

            System.out.println("\nAspetto l'inizio...\n");

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    /** This function will call the AI to make the perfect move.
     *
     * @param board tic-tac-toe playing field
     * @param myTopic topic on which the AI move will be sent
     * */
    public static void makeMove(Board board, String myTopic){

        AI.run(board.getTurn(), board, Double.POSITIVE_INFINITY); //Run the AI that will set the column and row selected.
        int AIMove;

        //Convert the AI move coordinates. Eg. the move (0, 2) (0 = row, 2 = column) will be converted in: 3
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
        try{
            JSONObject JSONmove = new JSONObject();
            JSONmove.put("move", Integer.toString(AIMove));
            MqttMessage move = new MqttMessage(JSONmove.toString().getBytes());
            move.setQos(0);
            client.publish(myTopic, move);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }


    /**This function increase the results variable
     *
     * @param board tic-tac-toe playing field
     * */
    public static void gameOver (Board board) {
        Board.State winner = board.getWinner();

        if (winner == Board.State.Blank) {//The game is a draw
            draw++;
        } else {
            if(winner.toString().equals("X")){//Our bot win the game
                win++;
            }else{//Our bot lost the game
                lose++;
            }
        }
    }



    public static void main(String[] args) {

        Scanner myObj = new Scanner(System.in);
        System.out.print("Indirizzo ip: ");

        String ip = myObj.nextLine();  // Read broker ip
        String broker = "tcp://" + ip + ":1883";

        System.out.print("\nInserisci un Pub Id: ");
        String PubId = myObj.nextLine(); // Read our pub id, different than other bot

        while(emailError){
            getEmail();
        }
        connectClient(broker, PubId);
        initializeArrays();
    }

}