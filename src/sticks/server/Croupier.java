package sticks.server;

import com.google.gson.JsonObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

//Fiksuj ovo sa stapicima, nakon izvlacenja, gubi se stapic
//Napravi klasu ActivePlayer gde ces da cuvas sve o tom igracu
//shodno tome, apdejtuj PlayerUtils
//Daj mu po logu da znamo ko je kakav stapic izvukao
//zavrsi igru kad se sve zavrsi (pobi sve)
public class Croupier {
    public static final int TCP_PORT = 9000;
    public static final int MAX_PLAYER_COUNT = 3;
    public static int rounds = 15;
    public static final AtomicInteger round_counter = new AtomicInteger(0);

    private AtomicInteger currentPlayerIndex = new AtomicInteger(0);
    private AtomicReferenceArray<Stick> sticks = new AtomicReferenceArray<>(MAX_PLAYER_COUNT);
    private AtomicInteger lastChosenStickIndex = new AtomicInteger(0);

    private CyclicBarrier allReadyBarrier;
    private CountDownLatch allGuessLatch;
    private CountDownLatch stickChosenLatch;
    private CountDownLatch newPartyLatch;
    private CyclicBarrier nextRoundBarrier;
    public Croupier() {

        this.allReadyBarrier = new CyclicBarrier(MAX_PLAYER_COUNT);
        this.allGuessLatch = new CountDownLatch(MAX_PLAYER_COUNT-1);
        this.stickChosenLatch = new CountDownLatch(1);
        this.newPartyLatch = new CountDownLatch(1);
        this.nextRoundBarrier = new CyclicBarrier(MAX_PLAYER_COUNT+1);//+one for another thread

        new Thread(()-> {
            while(true) {
                if(this.nextRoundBarrier.getNumberWaiting()==MAX_PLAYER_COUNT) {
                    round_counter.incrementAndGet();
                    //ispisi rezultat
                    if(this.getCurrentPlayerIndex().incrementAndGet() == MAX_PLAYER_COUNT) {
                        this.getCurrentPlayerIndex().set(0);
                    }
                    try {
                        this.nextRoundBarrier.await(); // Notify all to continue
                    } catch (InterruptedException | BrokenBarrierException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        shuffleSticks();
        this.getNewPartyLatch().countDown(); //starts new party

        System.out.println("[Server]: Croupier is running...");
        try {
            @SuppressWarnings("resource")
            ServerSocket serverSocket = new ServerSocket(TCP_PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                        socket.getOutputStream())), true);

                JsonObject response = new JsonObject();
                if(PlayerUtils.getPlayers().size() < MAX_PLAYER_COUNT) {
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
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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

    public AtomicReferenceArray<Stick> getSticks() {
        return sticks;
    }

    public AtomicInteger getLastChosenStickIndex() {
        return lastChosenStickIndex;
    }

    public static void main(String[] args) {
        new Croupier();
    }

    private void shuffleSticks() {
        //promesaj stapice,
        Random r = new Random();
        int selectWrong = r.nextInt(MAX_PLAYER_COUNT);
        System.out.println("Los je: "+selectWrong);
        for(int i=0;i<getSticks().length();i++) {
            if(i==selectWrong) {
                getSticks().set(i, new Stick(false));
                continue;
            }
            getSticks().set(i, new Stick(true));
        }

    }

}
