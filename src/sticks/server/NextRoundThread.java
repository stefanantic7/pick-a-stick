package sticks.server;

public class NextRoundThread implements Runnable {

    private Croupier croupier;

    public NextRoundThread(Croupier croupier) {
        this.croupier = croupier;
    }

    @Override
    public void run() {
        //Prelazi se u sledecu rundu kad svi sve zavrse

        int roundNumber = this.croupier.getRoundCounter().incrementAndGet();
        if(this.croupier.getRoundCounter().get() < Croupier.ROUNDS) { //nece poceti novu partiju ako je poslednja runda (M-ta runda)
            System.out.println("[Server]: Round number: " + (roundNumber+1)); // Count from zero
        }

        if(this.croupier.getCurrentPlayerIndex().incrementAndGet() == Croupier.MAX_PLAYER_COUNT) {
            this.croupier.getCurrentPlayerIndex().set(0);
        }

        this.croupier.resetAllGuessLatch();
        this.croupier.resetStickChosenLatch();
    }
}
