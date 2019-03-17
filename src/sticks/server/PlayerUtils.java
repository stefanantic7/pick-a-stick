package sticks.server;

import java.util.ArrayList;

public class PlayerUtils {
    private static ArrayList<ActivePlayer> table = new ArrayList<>();

    public static synchronized void addClient(String uuid) {
        if(findByUuid(uuid) == null) {
            table.add(new ActivePlayer(uuid, 0));
        }
    }

    public static synchronized void removeClient(String uuid) {
        table.remove(findByUuid(uuid));
    }

    public static String getByIndex(int index) {
        return table.get(index).getUuid();
    }

    public static synchronized int countActivePlayers() {
        return table.size();
    }

    public static synchronized int incrementPointsTo(String uuid) {
        return findByUuid(uuid).incrementPoints();
    }

    private static ActivePlayer findByUuid(String uuid) {
        ActivePlayer player = null;
        for (ActivePlayer activePlayer : table) {
            if(activePlayer.getUuid().equals(uuid)) {
                player = activePlayer;
                break;
            }
        }
        return player;
    }
}
