package sticks.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import sticks.enums.ActionRequest;
import sticks.enums.RequestProperty;
import sticks.server.models.Stick;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BrokenBarrierException;

public class CroupierThread extends Thread{

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String uuid;

    private Croupier croupier;

    private boolean lastSuccessGuess = true;
    public CroupierThread(Croupier croupier, Socket socket, BufferedReader in, PrintWriter out, String uuid) {
        this.croupier = croupier;
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.uuid = uuid;

        start();
    }


    private void guess() throws IOException {
        JsonObject request = new JsonObject();
        request.addProperty(RequestProperty.action.toString(), ActionRequest.guess.toString());
        out.println(request);

        System.out.println("[Server]: Requested guess from uuid: "+uuid);

        String response = in.readLine();
        System.out.println("[Client] "+uuid+": "+response);
        JsonObject responseJson = new JsonParser().parse(response).getAsJsonObject();
        this.lastSuccessGuess = responseJson.get("success").getAsBoolean();
        this.croupier.getAllGuessLatch().countDown();
    }
    private void choose() throws IOException {
        JsonObject request = new JsonObject();
        request.addProperty(RequestProperty.action.toString(), ActionRequest.choose.toString());
        request.addProperty(RequestProperty.less_than.toString(), StickUtils.count());
        out.println(request);

        System.out.println("[Server]: Requested choice([0<=x<"+StickUtils.count()+"]) from uuid: "+uuid);
        String response = in.readLine();
        System.out.println("[Client] "+uuid+": "+response);
        JsonObject responseJson = new JsonParser().parse(response).getAsJsonObject();
        this.croupier.getLastChosenStickIndex().set(responseJson.get("chosen").getAsInt());
        Stick stick = StickUtils.pick(responseJson.get("chosen").getAsInt());
        System.out.println("[Server]: Client "+uuid+" choose stick with id: "+stick.getId());

        this.croupier.getStickChosenLatch().countDown();
    }

    private void play() throws IOException, InterruptedException, BrokenBarrierException {
        while(this.croupier.getRoundCounter().get() < Croupier.ROUNDS) {

            System.out.println("[Server]: "+uuid+" has entered in new round");
            //Cekaj da se sve pripremi za ovu partiju
            this.croupier.getNewPartyLatch().await();


            //barijera da svh 6 dodju
            this.croupier.getAllReadyBarrier().await();
            System.out.println("[Server]: "+uuid+" has started");

            //svi prognozu osim jednog
            String currentPlayerUuid = PlayerUtils.getByIndex(croupier.getCurrentPlayerIndex().get());
            if(!this.uuid.equals(currentPlayerUuid)) {
                guess();
            }

            //Sacekaj da svi daju prognozu
            this.croupier.getAllGuessLatch().await();

            //taj jedan da izvuce stapic
            if(this.uuid.equals(currentPlayerUuid)) {
                choose();
            }

            //barijera dok ga izvuce, svi cekaju
            this.croupier.getStickChosenLatch().await();

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
                if(this.croupier.getRoundCounter().get() < Croupier.ROUNDS -1) { //nece poceti novu partiju ako je poslednja runda (M-ta runda)
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
                this.croupier.getNextRoundBarrier().await();

                break;
            }

            //wait to increment round counter
            this.croupier.getNextRoundBarrier().await();

        }
    }

    private void handle() throws IOException, BrokenBarrierException, InterruptedException {
        play();

        if(PlayerUtils.getTheBestPlayer().getUuid().equals(this.uuid)) {
            System.out.println("[Socket]: The best player is: "+this.uuid);
            JsonObject request = new JsonObject();
            request.addProperty(RequestProperty.action.toString(), ActionRequest.lets_party.toString());
            out.println(request.toString());
        }

        JsonObject request = new JsonObject();
        request.addProperty(RequestProperty.action.toString(), ActionRequest.quit.toString());
        out.println(request.toString());

        in.close();
        out.close();
        socket.close();

        //Pobednicki thread ce se iskoristiti za reset igre, tako da nova igra moze poceti ponovo
        if(this.uuid.equals(PlayerUtils.getTheBestPlayer().getUuid())) {
            //Izbacimo preostale
            PlayerUtils.removeAll();
            //promesamo
            StickUtils.reset();

            //Resetujemo round counter
            this.croupier.getRoundCounter().set(0);
            //Resetujemo index igraca koji je sledeci na redu
            this.croupier.getCurrentPlayerIndex().set(0);

            //Resetujemo latcheve
            this.croupier.resetAllGuessLatch();
            this.croupier.resetStickChosenLatch();
            this.croupier.getNewPartyLatch().countDown();

            System.out.println();
            System.out.println();
            System.out.println("[Server]: Waiting for new players...");
        }
    }

    @Override
    public void run() {
        try {
            handle();
        } catch (IOException | InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }
}
