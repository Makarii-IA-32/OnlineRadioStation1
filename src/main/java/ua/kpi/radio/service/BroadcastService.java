package ua.kpi.radio.service;

/**
 * Зберігає стан ефіру (включено / виключено).
 * Поки що глобальний для всіх бітрейтів.
 */
public class BroadcastService {

    private static final BroadcastService INSTANCE = new BroadcastService();

    private boolean broadcasting = false;

    private BroadcastService() {}

    public static BroadcastService getInstance() {
        return INSTANCE;
    }

    public synchronized void startBroadcast() {
        broadcasting = true;
        System.out.println("Broadcast started");
    }

    public synchronized void stopBroadcast() {
        broadcasting = false;
        System.out.println("Broadcast stopped");
    }

    public synchronized boolean isBroadcasting() {
        return broadcasting;
    }
}
