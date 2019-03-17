package sticks.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BrokenBarrierException;

public class PlayerThread extends Thread{

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String uuid;

    private Croupier croupier;

    private boolean lastSuccessGuess = true;
    public PlayerThread(Croupier croupier, Socket socket, BufferedReader in, PrintWriter out, String uuid) {
        this.croupier = croupier;
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.uuid = uuid;

        start();
    }


    private void guess() {
        JsonObject request = new JsonObject();

        request.addProperty("action", "guess");
        out.println(request);
        System.out.println("[Server]: Requested guess from uuid: "+uuid);
        try {
            String response = in.readLine();
            System.out.println("[Client] "+uuid+": "+response);
            JsonObject responseJson = new JsonParser().parse(response).getAsJsonObject();
            this.lastSuccessGuess = responseJson.get("success").getAsBoolean();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.croupier.getAllGuessLatch().countDown();
    }
    private void choose() {
        JsonObject request = new JsonObject();

        request.addProperty("action", "choose");
        request.addProperty("less_then", StickUtils.count());
        out.println(request);
        System.out.println("[Server]: Requested choice([0<=x<"+StickUtils.count()+"]) from uuid: "+uuid);
        try {
            String response = in.readLine();
            System.out.println("[Client] "+uuid+": "+response);
            JsonObject responseJson = new JsonParser().parse(response).getAsJsonObject();
            this.croupier.getLastChosenStickIndex().set(responseJson.get("chosen").getAsInt());
            Stick stick = StickUtils.pick(responseJson.get("chosen").getAsInt());
            System.out.println("[Server]: Client "+uuid+" choose stick with id: "+stick.getId());
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.croupier.getStickChosenLatch().countDown();
    }
    @Override
    public void run() {
        //newParty

        while(Croupier.round_counter.get() < Croupier.ROUNDS) {

            System.out.println("[Server]: "+uuid+" has entered in new round");
            //Cekaj da se sve pripremi za ovu partiju
            try {
                this.croupier.getNewPartyLatch().await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //barijera da svh 6 dodju
            try {
                this.croupier.getAllReadyBarrier().await();
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
            System.out.println("[Server]: "+uuid+" has started");

            //svi prognozu osim jednog
            String currentPlayerUuid = PlayerUtils.getByIndex(croupier.getCurrentPlayerIndex().get());
            if(!this.uuid.equals(currentPlayerUuid)) {
                guess();
            }

            //Sacekaj da svi daju prognozu
            try {
                this.croupier.getAllGuessLatch().await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //taj jedan da izvuce stapic
            if(this.uuid.equals(currentPlayerUuid)) {
                choose();
            }

            //barijera dok ga izvuce, svi cekaju
            try {
                this.croupier.getStickChosenLatch().await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //Daj poene onima koji su pogadjali i koji su pogodili
            if(!this.uuid.equals(currentPlayerUuid)
                    && this.lastSuccessGuess==StickUtils.getLastPickedStick().isSuccess()) {
                int points = PlayerUtils.incrementPointsTo(uuid);
                System.out.println("[Server]: "+this.uuid+" has "+points+" points");
            }

            //Ako je izvukao los
            if(this.uuid.equals(currentPlayerUuid) && !StickUtils.getLastPickedStick().isSuccess()) {
                System.out.println("[Server]: "+this.uuid+" chose bad stick");
                //-1 jer ce se tek kasnije inkrementirati brojac ( u drugom threadu koji resetuje sve potrebno za novu rundu )
                if(Croupier.round_counter.get() < Croupier.ROUNDS -1) { //nece poceti novu partiju ako je poslednja runda (M-ta runda)
                    System.out.println("[Server]: New party has started");
                }
                //New party
                this.croupier.resetNewPartyLatch();
                new Thread(()->{
                    //promesaj stapice,
                    StickUtils.reset();

                    //Opet kreni od prvog
                    this.croupier.getCurrentPlayerIndex().set(0);
                    //restartuj latcheve
                    this.croupier.resetAllGuessLatch();
                    this.croupier.resetStickChosenLatch();
                    this.croupier.getNewPartyLatch().countDown();
                }).start();
                PlayerUtils.removeClient(uuid);

                //wait to increment round counter
                try {
                    this.croupier.getNextRoundBarrier().await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
                break;
            }

            //wait to increment round counter
            try {
                this.croupier.getNextRoundBarrier().await();
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }


        }

        JsonObject request = new JsonObject();
        request.addProperty("action", "quit");
        out.println(request.toString());
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
