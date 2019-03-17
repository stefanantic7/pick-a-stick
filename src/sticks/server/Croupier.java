package sticks.server;

import com.google.gson.JsonObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

public class Croupier {
    public static final int TCP_PORT = 9000;
    public static final int MAX_PLAYER_COUNT = 6; // Stapica ima koliko i igraca
    public static int ROUNDS = 9; // Broj rundi (M)

    private static Croupier instance = null;

    private AtomicInteger roundCounter = new AtomicInteger(0);
    private AtomicInteger currentPlayerIndex = new AtomicInteger(0);
    private AtomicInteger lastChosenStickIndex = new AtomicInteger(0);

    private CyclicBarrier allReadyBarrier;
    private CountDownLatch allGuessLatch;
    private CountDownLatch stickChosenLatch;
    private CountDownLatch newPartyLatch;
    private CyclicBarrier nextRoundBarrier;


    private Croupier() {
        this.allReadyBarrier = new CyclicBarrier(MAX_PLAYER_COUNT); // Barijera koja kaze da li su svi spremni
        this.allGuessLatch = new CountDownLatch(MAX_PLAYER_COUNT-1); // Omogucava da onaj koji izvlaci saceka da svi daju prognozu
        this.stickChosenLatch = new CountDownLatch(1); // Omogucava da svi cekaju da jedan izvuce stapic
        this.newPartyLatch = new CountDownLatch(1); // Svi ce sacekati da poseban thread spremi sve za novu partiju

        //Pre prelaska u drugu rundu treba sacekati da svi zavrse, a onda odrediti ko igra sledeci i inkrementirati rundu
        this.nextRoundBarrier = new CyclicBarrier(MAX_PLAYER_COUNT, new NextRoundThread(this));

        try {
            start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void start() throws IOException {

        this.getNewPartyLatch().countDown(); //starts a new party

        System.out.println("[Server]: Croupier is running...");

        //Promesamo pre pocetka
        StickUtils.reset();

        System.out.println("[Server]: Round number: 1");

        ServerSocket serverSocket = new ServerSocket(TCP_PORT);
        while (true) {
            Socket socket = serverSocket.accept();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                    socket.getOutputStream())), true);

            JsonObject response = new JsonObject();
            if(PlayerUtils.countActivePlayers() < MAX_PLAYER_COUNT) {
                String uuid = UUID.randomUUID().toString();
                PlayerUtils.addClient(uuid);

                response.addProperty("connection_status", "ok");
                response.addProperty("uuid", uuid);
                out.println(response.toString());
                new CroupierThread(this, socket, in, out, uuid);
            }
            else {
                response.addProperty("connection_status", "bad");
                response.addProperty("reason", "Table is full. Get out!");
                out.println(response.toString());
            }
        }

    }

    public AtomicInteger getRoundCounter() {
        return roundCounter;
    }

    public CyclicBarrier getNextRoundBarrier() {
        return nextRoundBarrier;
    }

    public void resetAllGuessLatch() {
        this.allGuessLatch = new CountDownLatch(MAX_PLAYER_COUNT-1);
    }

    public void resetStickChosenLatch() {
        this.stickChosenLatch = new CountDownLatch(1);
    }

    public void resetNewPartyLatch() {
        this.newPartyLatch = new CountDownLatch(1);
    }

    public CountDownLatch getNewPartyLatch() {
        return newPartyLatch;
    }

    public AtomicInteger getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public CyclicBarrier getAllReadyBarrier() {
        return allReadyBarrier;
    }

    public CountDownLatch getAllGuessLatch() {
        return allGuessLatch;
    }

    public CountDownLatch getStickChosenLatch() {
        return stickChosenLatch;
    }

    public AtomicInteger getLastChosenStickIndex() {
        return lastChosenStickIndex;
    }

    public static Croupier getInstance() {
        if (instance == null) {
            synchronized (Croupier.class) {
                if (instance == null) {
                    instance = new Croupier();
                }
            }
        }
        return instance;
    }

    public static void main(String[] args) {
        Croupier.getInstance();
    }

}
