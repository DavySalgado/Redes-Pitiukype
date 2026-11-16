package pitiupi.pitiukype.net;

import pitiupi.models.Messages.Message;
import pitiupi.pitiukype.model.Room;
import pitiupi.pitiukype.model.RoomMember;
import java.util.List;

public final class RoomMessage extends Message {
    private static final long serialVersionUID = 1L;

    //Anuncio é para informar aos outros que a sala foi criada, que está disponivel e os participantes
    //Batimento é para verificar se a pessoa ainda está la. Ex: Caso ela desligue a maquina ou a perca conxao da internet, ao nao receber batimento, é deslogada.
    // O resto é autoexplicativo
    public enum Tipo {
        ANUNCIO, PEDIR_ENTRADA, ENTRADA_ACEITA, ENTRADA_RECUSADA, BATIMENTO, SAIDA, ENCERRAMENTO
    }

    private final Tipo tipo;
    private final String salaId;
    private final String salaNome;
    private final String hostId;
    private final String hostIp;
    private final RoomMember emissor;
    private final String destinatarioId;
    private final List<RoomMember> participantes;
    private final long versao;
    private final String motivo;

    public RoomMessage(Tipo tipo, Room sala, RoomMember emissor,
                       String destinatarioId, String motivo) {
        super(false);
        this.tipo = tipo;
        this.salaId = sala.getId();
        this.salaNome = sala.getNome();
        this.hostId = sala.getHostId();
        this.hostIp = sala.getHostIp();
        this.emissor = emissor;
        this.destinatarioId = destinatarioId;
        this.participantes = List.copyOf(sala.getParticipantes());
        this.versao = sala.getVersao();
        this.motivo = motivo;
    }

    public Tipo getTipo() { return tipo; }
    public String getSalaId() { return salaId; }
    public String getSalaNome() { return salaNome; }
    public String getHostId() { return hostId; }
    public String getHostIp() { return hostIp; }
    public RoomMember getEmissor() { return emissor; }
    public String getDestinatarioId() { return destinatarioId; }
    public List<RoomMember> getParticipantes() { return participantes; }
    public long getVersao() { return versao; }
    public String getMotivo() { return motivo; }
}
