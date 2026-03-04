package utils;

import javax.sound.sampled.*;
import java.io.File;

public class AudioRecorder {
    private TargetDataLine line;
    private AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;

    public void start(File destFile) {
        new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

                line = (TargetDataLine) AudioSystem.getLine(info);

                if (line.isOpen()) line.close();

                line.open(format);
                line.start();

                System.out.println("🎙 Enregistrement forcé démarré...");
                AudioInputStream ais = new AudioInputStream(line);
                AudioSystem.write(ais, fileType, destFile);

            } catch (LineUnavailableException e) {
                System.err.println("❌ Erreur : Le micro est déjà utilisé par une autre application (Teams, Discord, ou une ancienne instance Java).");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    public void stop() {
        if (line != null && line.isOpen()) {
            line.stop();
            line.flush();
            line.close();
            System.out.println("✅ Micro libéré avec succès.");
        }
    }
}