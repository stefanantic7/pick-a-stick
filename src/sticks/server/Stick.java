package sticks.server;

public class Stick {
    private boolean success;
    private int id;

    public Stick(int id, boolean success) {
        this.id = id;
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getId() {
        return id;
    }
}
