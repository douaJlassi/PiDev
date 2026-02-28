package utils;

import javax.sound.sampled.*;
import java.io.File;

public class AudioRecorder {
    private TargetDataLine line;
    private AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;

    public void start(File destFile) {
        new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(16000, 16, 1, true, false);                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();
                AudioInputStream ais = new AudioInputStream(line);
                AudioSystem.write(ais, fileType, destFile);
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
    }

    public void stop() {
        if (line != null) {
            line.stop();
            line.close();
        }
    }
}