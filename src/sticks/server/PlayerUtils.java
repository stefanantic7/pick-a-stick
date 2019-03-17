package sticks.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PlayerUtils {
    private static HashMap<String, AtomicInteger> clients = new HashMap<>();
    private static ArrayList<String> table = new ArrayList<>();

    public static synchronized void addClient(String uuid) {
        clients.put(uuid, new AtomicInteger(0));
        table.add(uuid);
    }

    public static synchronized void removeClient(String uuid) {
        clients.remove(uuid);
        table.remove(uuid);
    }

    public static HashMap<String, AtomicInteger> getPlayers() {
        return clients;
    }

    public static String getByIndex(int index) {
        return table.get(index);
    }

    public static synchronized int incrementPointsTo(String uuid) {
        return clients.get(uuid).incrementAndGet();
    }
}
