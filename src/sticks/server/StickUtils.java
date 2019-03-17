package sticks.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class StickUtils {
    private static ArrayList<Stick> sticks = new ArrayList<>();
    private static Random random = new Random();
    private static Stick lastPickedStick = null;

    public static synchronized void shuffle() {
        Collections.shuffle(sticks);
    }

    public static synchronized void reset() {
        int selectWrong = random.nextInt(Croupier.MAX_PLAYER_COUNT);
        System.out.println("[Server]: Index of wrong stick: "+selectWrong);

        sticks = new ArrayList<>();
        for(int i=0;i<Croupier.MAX_PLAYER_COUNT;i++) {
            if(i==selectWrong) {
                sticks.add(new Stick(false));
                continue;
            }
            sticks.add(new Stick(true));
        }
    }

    public static synchronized Stick find(int index) {
        return sticks.get(index);
    }

    public static synchronized Stick pick(int index) {
        lastPickedStick = sticks.get(index);
        sticks.remove(index);
        return lastPickedStick;
    }

    public static synchronized Stick getLastPickedStick() {
        return lastPickedStick;
    }

    public static synchronized int count() {
        return sticks.size();
    }

}
