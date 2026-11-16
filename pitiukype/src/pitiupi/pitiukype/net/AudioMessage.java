package pitiupi.pitiukype.net;

import pitiupi.models.Messages.Message;
import java.util.Arrays;

public final class AudioMessage extends Message {
    private static final long serialVersionUID = 1L;
    public static final int TAMANHO_PCM = 640;

    private final String salaId;
    private final String emissorId;
    private final long sequencia;
    private final byte[] audioPcm;

    public AudioMessage(String salaId, String emissorId, long sequencia, byte[] audioPcm) {
        super(false);
        if (audioPcm == null || audioPcm.length != TAMANHO_PCM) {
            throw new IllegalArgumentException("Um pacote deve conter 640 bytes de PCM");
        }
        this.salaId = salaId;
        this.emissorId = emissorId;
        this.sequencia = sequencia;
        this.audioPcm = Arrays.copyOf(audioPcm, audioPcm.length);
    }

    public String getSalaId() { return salaId; }
    public String getEmissorId() { return emissorId; }
    public long getSequencia() { return sequencia; }
    public byte[] getAudioPcm() { return Arrays.copyOf(audioPcm, audioPcm.length); }
}
