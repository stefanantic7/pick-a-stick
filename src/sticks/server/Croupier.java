package sticks.server;

import com.google.gson.JsonObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

//zavrsi igru kad se sve zavrsi (pobi sve)
//Posalji pobedniku poruku
public class Croupier {
    public static final int TCP_PORT = 9000;
    public static final int MAX_PLAYER_COUNT = 3;
    public static int ROUNDS = 5;

    private static Croupier instance = null;

    private AtomicInteger roundCounter = new AtomicInteger(0);

    private AtomicInteger currentPlayerIndex = new AtomicInteger(0);
    private AtomicInteger lastChosenStickIndex = new AtomicInteger(0);

    private CyclicBarrier allReadyBarrier;
    private CountDownLatch allGuessLatch;
    private CountDownLatch stickChosenLatch;
    private CountDownLatch newPartyLatch;
    private CyclicBarrier nextRoundBarrier;
    private ServerSocket serverSocket;


    private Croupier() {
        start();
    }

    private void start() {
        this.allReadyBarrier = new CyclicBarrier(MAX_PLAYER_COUNT);
        this.allGuessLatch = new CountDownLatch(MAX_PLAYER_COUNT-1);
        this.stickChosenLatch = new CountDownLatch(1);
        this.newPartyLatch = new CountDownLatch(1);
        this.nextRoundBarrier = new CyclicBarrier(MAX_PLAYER_COUNT, ()->{
            //Prelazi se u sledecu rundu kad svi sve zavrse

            int roundNumber = getRoundCounter().incrementAndGet();
            if(getRoundCounter().get() < Croupier.ROUNDS) { //nece poceti novu partiju ako je poslednja runda (M-ta runda)
                System.out.println("[Server]: Round number: " + (roundNumber+1)); // Count from zero
            }

            if(this.getCurrentPlayerIndex().incrementAndGet() == MAX_PLAYER_COUNT) {
                this.getCurrentPlayerIndex().set(0);
            }

            resetAllGuessLatch();
            resetStickChosenLatch();

        });

        StickUtils.reset();

        this.getNewPartyLatch().countDown(); //starts a new party

        System.out.println("[Server]: Croupier is running...");
        System.out.println("[Server]: Round number: 1");

        Socket socket = null;
        try {
            serverSocket = new ServerSocket(TCP_PORT);
            while (true) {
                socket = serverSocket.accept();
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
                    new PlayerThread(this, socket, in, out, uuid);
                }
                else {
                    response.addProperty("connection_status", "bad");
                    out.println(response.toString());
                }


            }
        } catch (SocketException ex) {
            try {
                if(socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("[Server]: close socket"); //Server socket will be automatically closed when game is finished.
        } catch (IOException ex) {
            ex.printStackTrace();
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
