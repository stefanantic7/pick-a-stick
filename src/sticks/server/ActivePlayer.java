package sticks.server;

import java.util.concurrent.atomic.AtomicInteger;

public class ActivePlayer {

    private String uuid;
    private AtomicInteger points;

    public ActivePlayer(String uuid, int points) {
        this.uuid = uuid;
        this.points = new AtomicInteger(points);
    }

    public String getUuid() {
        return uuid;
    }

    public int getPoints() {
        return this.points.get();
    }

    public int incrementPoints() {
        return this.points.incrementAndGet();
    }

    @Override
    public String toString() {
        return "Player "+uuid+" ("+points+"p)";
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ActivePlayer)) {
            return false;
        }

        return this.getUuid().equals(((ActivePlayer)obj).getUuid());
    }
}
