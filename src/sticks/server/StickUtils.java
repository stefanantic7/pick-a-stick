package sticks.server;

import sticks.server.models.Stick;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class StickUtils {
    private static ArrayList<Stick> sticks = new ArrayList<>();
    private static Random random = new Random();
    private static Stick lastPickedStick = null;
    private static AtomicInteger autoId = new AtomicInteger(0);

    public static synchronized void reset() {
        int selectWrong = random.nextInt(Croupier.MAX_PLAYER_COUNT);
        sticks = new ArrayList<>();
        for(int i=0;i<Croupier.MAX_PLAYER_COUNT;i++) {
            int newId = autoId.incrementAndGet();
            if(i==selectWrong) {
                sticks.add(new Stick(newId, false));
                System.out.println("[Server]: ID of wrong stick for the next party: " + newId);
                continue;
            }
            sticks.add(new Stick(newId, true));
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
