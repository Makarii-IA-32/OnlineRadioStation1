package ua.kpi.radio.domain;

public class User {
    private int id;
    private String displayName;
    private String sessionId;

    public User() {}

    public User(int id, String displayName, String sessionId) {
        this.id = id;
        this.displayName = displayName;
        this.sessionId = sessionId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}