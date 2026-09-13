package org.gbq.jails;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class JailLogger {

    private static final String LOG_FILE_NAME = "jail_log.txt";
    private final File logFile;

    public JailLogger(File dataFolder) {
        logFile = new File(dataFolder, LOG_FILE_NAME);
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void logEvent(String event) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            writer.write("[" + timestamp + "] " + event);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
