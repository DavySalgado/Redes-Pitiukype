package pitiupi.pitiukype.model;

import java.util.List;
import java.util.UUID;

public final class Room {
    private final String id;
    private final String nome;
    private final String hostId;
    private final String hostIp;
    private final List<RoomMember> participantes;
    private final long versao;
    private final long ultimoAnuncio;

    public Room(String id, String nome, String hostId, String hostIp, List<RoomMember> participantes, long versao, long ultimoAnuncio) {
        this.id = id;
        this.nome = nome;
        this.hostId = hostId;
        this.hostIp = hostIp;
        this.participantes = List.copyOf(participantes);
        this.versao = versao;
        this.ultimoAnuncio = ultimoAnuncio;
    }

    public static Room criar(String nome, RoomMember organizador, long agora) {
        return new Room(UUID.randomUUID().toString(), nome, organizador.getId(), organizador.getIp(), List.of(organizador), 1, agora);
    }

    public Room atualizar(List<RoomMember> membros, long novaVersao, long agora) {
        return new Room(id, nome, hostId, hostIp, membros, novaVersao, agora);
    }

    public Room vistoAgora(long agora) {
        return atualizar(participantes, versao, agora);
    }

    public String getId() { return id; }
    public String getNome() { return nome; }
    public String getHostId() { return hostId; }
    public String getHostIp() { return hostIp; }
    public List<RoomMember> getParticipantes() { return participantes; }
    public long getVersao() { return versao; }
    public long getUltimoAnuncio() { return ultimoAnuncio; }
}
