package pitiupi.pitiukype.net;

import pitiupi.models.Messages.Message;

public final class PingMessage extends Message {
    private static final long serialVersionUID = 1L;
    private final String tipo;
    private final String ipOrigem;

    public PingMessage(String tipo, String ipOrigem) {
        super(false);
        this.tipo = tipo;
        this.ipOrigem = ipOrigem;
    }

    public String getTipo() { return tipo; }
    public String getIpOrigem() { return ipOrigem; }
}
