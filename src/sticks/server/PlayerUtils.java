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

    public static synchronized void removeAll() {
        table = new ArrayList<>();
    }

    public static synchronized boolean exists(String uuid) {
        boolean exists = false;
        for (ActivePlayer player:table) {
            if(player.getUuid().equals(uuid)) {
                exists = true;
                break;
            }
        }
        return exists;
    }

    public static synchronized ActivePlayer getTheBestPlayer() {
        ActivePlayer theBestPlayer = null;
        if(table.size()>0) {
            theBestPlayer = table.get(0);
            for (ActivePlayer player : table) {
                if(player.getPoints()>theBestPlayer.getPoints()) {
                    theBestPlayer = player;
                }
            }
        }
        return theBestPlayer;
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
