package pitiupi.pitiukype.control;

import pitiupi.pitiukype.net.AudioMessage;

public interface VoiceTransport {
    boolean enviarAudio(AudioMessage mensagem, String destinoIp);
}
