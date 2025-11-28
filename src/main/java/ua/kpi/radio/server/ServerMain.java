package ua.kpi.radio.server;

import ua.kpi.radio.repo.Database;
import ua.kpi.radio.server.http.HttpServerLauncher;
import ua.kpi.radio.service.DemoDataInitializer;

public class ServerMain {

    public static void main(String[] args) {
        try {
            System.out.println("Initializing database...");
            Database.init();

            System.out.println("Seeding demo data (if needed)...");
            DemoDataInitializer demo = new DemoDataInitializer();
            demo.initDemoData();

            System.out.println("Starting HTTP server on port 8080...");
            HttpServerLauncher launcher = new HttpServerLauncher();
            launcher.start(8080);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
