import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.ArrayList;
import java.util.Properties;

import javax.mail.*;
import javax.mail.Flags.Flag;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;

public class ReadMail {
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

    public static void check(String host, String storeType, String user, String password) {
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

            store.connect(host, user, password);

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






            inbox.close(false);
            store.close();

        } catch (Exception e) {
            System.out.println("Non ci sono email");
        }

    }

    public static void main(String[] args) {

        String host = "imap.gmail.com";
        String mailStoreType = "imap";
        String username = "trisser.bot2@gmail.com";
        String password = "trisserbot2!";

        check(host, mailStoreType, username, password);

    }
}
