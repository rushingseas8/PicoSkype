package pffft;

import javax.sound.sampled.*;

public class AudioTest {
    public static void main() throws Exception {
        Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
        for(Mixer.Info mix : mixerInfo) {
            Mixer mixer = AudioSystem.getMixer(mix); // default mixer
            mixer.open();

            System.out.printf("Supported SourceDataLines of default mixer (%s):\n\n", mixer.getMixerInfo().getName());
            for(Line.Info info : mixer.getSourceLineInfo()) {
                if(SourceDataLine.class.isAssignableFrom(info.getLineClass())) {
                    SourceDataLine.Info info2 = (SourceDataLine.Info) info;
                    System.out.println(info2);
                    System.out.printf("  max buffer size: \t%d\n", info2.getMaxBufferSize());
                    System.out.printf("  min buffer size: \t%d\n", info2.getMinBufferSize());
                    AudioFormat[] formats = info2.getFormats();
                    System.out.println("  Supported Audio formats: ");
                    for(AudioFormat format : formats) {
                        System.out.println("    "+format);
                    }
                    System.out.println();
                } else {
                    System.out.println(info.toString());
                }
                System.out.println();
            }

            mixer.close();
        }
    }
}