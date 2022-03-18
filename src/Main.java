

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.mail.*;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Properties;
import java.util.Scanner;



public class Main extends Thread {

    /* Attributi */
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


    /* Costruttore */
    public Main(JSONObject JSONmessage, String topic) {
        this.JSONmessage = JSONmessage;
        this.topic = topic;

    }

    /**@
     * Start multiple threads to initialize different room simultaneously
     * @param i room number (eg 0, 1, 2)
     * */
    public static void initializeThread(int i){
        Thread thread1 = new Thread () {
            public void run () {
                String myTopic;
                for (int x = 0; x < starts.get(i).size(); x++) {
                    if(starts.get(i).get(x)){
                        myTopic = rooms.get(i) + "/" + x + "/" + myName;
                        makeMove(boards.get(i).get(x), myTopic);
                        // System.out.println(boards.get(i).get(x));
                    }
                }
            }
        };
        thread1.start();
    }


    @Override
    public void run() {

            //System.out.println("\n" + topic + " says:\n" + JSONmessage + "\n");

            if(!Objects.isNull(JSONmessage.get("error")) && JSONmessage.get("player").equals("trisser.bot2@gmail.com")){
                String[] topicParts = topic.split("/");
                String room = topicParts[0];
                String instance = topicParts[1];
                System.out.println("Errore nella room " + room + "/" + instance);
            }


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
                        System.out.println("\nInvalid enemy move.");
                        System.out.println("\nThe index of the move must be between 1 and "
                                + (9) + ", inclusive.");
                    } else if (!boards.get(roomInstance).get(instance).move(move)) {
                        System.out.println("\nInvalid enemy move.");
                        error = true;
//                        System.out.println("\nThe selected index must be blank.");
//                        System.out.println(boards.get(instance));
                    }else{
                            //System.out.println(board);
                            if(!boards.get(roomInstance).get(instance).isGameOver()){
                                makeMove(boards.get(roomInstance).get(instance), myTopic);
                                //System.out.println(boards.get(roomInstance).get(instance));
                            }
                            //System.out.println("Mossa AI: " + AIMove);
                    }
                }
                if (boards.get(roomInstance).get(instance).isGameOver() && !error) {
                    printWinner(boards.get(roomInstance).get(instance));
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
     * Take last unread email and interprets all its data
     * */
    public static void getEmail(){
        //Dati connessione all'email
        String host = "imap.gmail.com";
        String mailStoreType = "imap";
        String username = "trisser.bot2@gmail.com";
        String password = "trisserbot2!";

        try{
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
            Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

            int i = 0;
            Message message = messages[i];
            message.setFlag(Flags.Flag.SEEN, true);
            String result = getTextFromMessage(message);


            inbox.close(false);
            store.close();

            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(result);



            //Prendo dall'email il numero di partite che bisogna giocare
            games = Integer.parseInt(json.get("room_instance").toString());


            //Prendo dall'email l'username e la password per conenttermi al broker
            userName = json.get("user").toString();
            pwd = json.get("pwd").toString();


            //Prendo dall'email le room, mi serviranno poi per iscrivermi alle topic
            rooms = (ArrayList<String>) json.get("rooms");


            for(i = 0; i< rooms.size(); i++){
                if(rooms.get(i).contains("trisser.bot2@gmail.com")){
                    rooms.set(i, rooms.get(i).replaceAll("\\r\\n|\\r|\\n", ""));
                }else{
                    rooms.remove(i);
                }
            }
            //System.out.println(rooms);
            //System.out.println(games);
            //System.out.println(username);
            //System.out.println(pwd);




            emailError = false;
        }catch (Exception e){
           // System.out.println(e.getMessage());
            System.out.print("Errore nella lettura della mail, vuoi riprovare? (s/n): ");
            Scanner chooseScanner = new Scanner(System.in);
            String choose = chooseScanner.nextLine();
            if(!choose.equals("s")){
                System.out.println("Ok esco...");
                System.exit(0);
            }
        }

    }

    //Connessione al broker
    public static void connectClient(){
        try{
            System.out.println("Mail letta.\n");

            int qos = 0;
            //-----------INIZIO CONNESSINE AL BROKER-----------
            Scanner myObj = new Scanner(System.in);  // Create a Scanner object
            System.out.print("Indirizzo ip: ");

            String ip = myObj.nextLine();  // Read user input
            String broker = "tcp://" + ip + ":1883";

            System.out.print("\nInserisci un Pub Id: ");

            String PubId = myObj.nextLine();

           // String PubId = "132.1.1.1";

            MemoryPersistence persistence = new MemoryPersistence();
            client = new MqttClient(broker, PubId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setConnectionTimeout(60);
            connOpts.setKeepAliveInterval(60);
            connOpts.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
            connOpts.setMaxInflight(1000000);

            //IN CASO DI PARTITA COL SERVER SCOMMENTARE USERNAME E PASSWORD
            //connOpts.setUserName(userName);
            //connOpts.setPassword(pwd.toCharArray());


            System.out.println("Mi connetto al broker: " + broker);
            client.connect(connOpts);
            System.out.println("Connesso!");
            //-----------FINE CONNESSINE AL BROKER-----------

            //Dico al server che sono online
            JSONObject json2 = new JSONObject();
            json2.put("trisser.bot2@gmail.com", "true");

            String topic = "online/trisser.bot2@gmail.com";
            MqttMessage msg = new MqttMessage(json2.toString().getBytes());
            msg.setQos(1);
            client.publish(topic, msg);

            client.subscribe("broadcast"); //Mi iscrivo alla topic broadcast

            //Setto la Callback per ricevere i messaggi
            client.setCallback(new MqttCallback() {
                public void connectionLost(Throwable cause) {}

                public void messageArrived(String topic, MqttMessage message) throws Exception {

                    //System.out.println("\n" + topic + " says:\n" + message + "\n");
                    String msg = message.toString();
                    JSONParser parser = new JSONParser();
                    JSONObject json = (JSONObject) parser.parse(msg);

                    if(!Objects.isNull(json.get("game")) && thereIsEnemy){
                        System.out.println("\nInizio!");
                        System.out.println("\nSto giocando...");
                            if(json.get("game").equals("start")){
                                for (int i = 0; i < rooms.size(); i++) {
                                    initializeThread(i);
                                }
                            }
                    }

                    if(!Objects.isNull(json.get("not_connected"))){
                        for (int i = 0; i < rooms.size(); i++) {
                            if(rooms.get(i).contains(json.get("not_connected").toString())){
                                rooms.remove(i);
                            }
                        }
                        if(rooms.isEmpty() && thereIsEnemy){
                            thereIsEnemy = false;
                            System.out.println("Nessun nemico si è connesso");
                            System.out.println("Aspetto la classifica...");
                        }

                    }

                    if(!Objects.isNull(json.get("1"))){
                        System.out.println("\nClassifica: \n" + msg);
                        System.out.println("\nPartite Vinte: " + win);
                        System.out.println("Partite Perse: " + lose);
                        System.out.println("Partite Pareggiate: " + draw);
                        System.exit(0);
                    }

                   new Main(json, topic).start();
                }
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });
        }catch (Exception e){
            System.out.println(e.getMessage());

        }
    }


    //Lancio i thread dicendogli se devono iniziare prima loro oppure no
    public static void startThread(){
        try {
            String topic;
            boolean start; //start = true significa che il bot inizierà nelle istanze pari.
            String roomEnemy;


            ArrayList<Board> boardsTemp = new ArrayList<>();
            ArrayList<Boolean> startsTemp = new ArrayList<>();

            for (String room : rooms) {

                //System.out.println(room);
               // JSONgames.put(room,)
                start = false;

                String[] partsRoom = room.split("_");
                roomEnemy = partsRoom[0];

                if(partsRoom[0].equals("trisser.bot2@gmail.com")){
                    roomEnemy = partsRoom[1];
                    start = true;
                }

                for(int y = 0; y < games; y++){
                    topic = room + "/" + y + "/" + roomEnemy;
                    //System.out.println(topic);
                    client.subscribe(topic);
                    //System.out.println(topic);


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
//            System.out.println("\n" + rooms);
//            System.out.println("\n" +boards);
//            System.out.println("\n" +starts);

            System.out.println("\nAspetto l'inizio...\n");
            //System.out.println(roomsTest.indexOf("TRISSER.bot3@gmail.caom_trisser.bot2@gmail.com"));


        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    public static void makeMove(Board board, String myTopic){

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
        try{
            JSONObject JSONmove = new JSONObject();
            JSONmove.put("move", Integer.toString(AIMove));
            MqttMessage move = new MqttMessage(JSONmove.toString().getBytes());
            move.setQos(1);
            client.publish(myTopic, move);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }



    public static void printWinner (Board board) {
        Board.State winner = board.getWinner();

        if (winner == Board.State.Blank) {
            //System.out.println("The TicTacToe is a Draw.");
            draw++;
        } else {
            //System.out.println("Player " + winner.toString() + " wins!");
            if(winner.toString().equals("X")){
                win++;
            }else{
                lose++;
            }
        }
        //System.exit(0);
    }



    public static void main(String[] args) {

        while(emailError){
            getEmail();
        }
        connectClient();
        startThread();
    }

}

