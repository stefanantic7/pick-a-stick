package sticks.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.Socket;
import java.util.Random;

import static sticks.server.Croupier.TCP_PORT;

//Pokrenuti nekoliko instanci klijenta kako bi se sve lepo videlo u zasebnim konzolama
public class Player {

    private long enterAfterMillis = 0;

    private BufferedReader in;

    private PrintWriter out;

    public Player(long enterAfterMillis) {
        this.enterAfterMillis = enterAfterMillis;
        try {
            start();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    public void start() throws InterruptedException, IOException {
        System.out.println("[Client]: "+"will start after "+this.enterAfterMillis+" ms");
        Thread.sleep(this.enterAfterMillis);
        System.out.println("[Client]: entering...");

        // otvori socket prema drugom racunaru
        Socket socket = new Socket("127.0.0.1", TCP_PORT);

        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

        String response = in.readLine();
        System.out.println("[Server]: "+response);

        JsonObject responseJson = new JsonParser().parse(response).getAsJsonObject();
        String responseProperty = responseJson.get("connection_status").getAsString();

        if(responseProperty.equals("ok")) {
            //Ima mesta
            handleRequests();
        }
        else if (responseProperty.equals("bad")) {
            System.out.println("[Client]: Exit...");
        }
        else {
            System.out.println("[Client]: Connection status is not supported. Exit...");
        }

        in.close();
        out.close();
        socket.close();

    }

    public void handleRequests() throws IOException {
        Random random = new Random();

        while(true) {
            String request = this.in.readLine();
            System.out.println("[Server]: "+request);

            JsonObject requestJson = new JsonParser().parse(request).getAsJsonObject();
            JsonObject responseJson = new JsonObject();

            switch (requestJson.get("action").getAsString()) {
                case "guess":
                    responseJson.addProperty("success", random.nextBoolean());
                    out.println(responseJson.toString());
                    break;
                case "choose":
                    responseJson.addProperty("chosen", random.nextInt(requestJson.get("less_than").getAsInt()));
                    out.println(responseJson.toString());
                    break;
                case "lets_party":
                    System.out.println("[Client]: Partijam");
                    break;
                default:
                    return;
            }

        }
    }
    public static void main(String[] args) {
        new Player((long)(Math.random() * 1000));
    }
}
