package utils;

import javax.sound.sampled.*;
import java.io.File;

public class AudioRecorder {
    private TargetDataLine line;
    private AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;

    public void start(File destFile) {
        new Thread(() -> {
            try {
                // --- SOLUTION : Format Voix Standard (16kHz est supporté par 99% des micros) ---
                AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

                // Si le format n'est pas supporté, on tente le format universel de base
                if (!AudioSystem.isLineSupported(info)) {
                    System.err.println("⚠️ Format 16kHz non supporté, tentative en 8kHz...");
                    format = new AudioFormat(8000.0f, 8, 1, true, false);
                    info = new DataLine.Info(TargetDataLine.class, format);
                }

                line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();

                System.out.println("🎙 Enregistrement démarré...");
                AudioInputStream ais = new AudioInputStream(line);
                AudioSystem.write(ais, fileType, destFile);

            } catch (Exception ex) {
                System.err.println("❌ Erreur Micro : " + ex.getMessage());
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