package com.acenite;

import java.util.Timer;
import java.util.TimerTask;

public class AceniteAgent {

    private final String apiKey;

    public AceniteAgent(String apiKey) {
        this.apiKey = apiKey;
    }

    public void start() {
        System.out.println("Acenite agent starting...");
        System.out.println("API Key: " + apiKey);

        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                System.out.println("collecting metrics...");
            }
        }, 0, 5000);
    }
}