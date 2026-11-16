package pitiupi.pitiukype.model;

import java.io.Serializable;

public final class RoomMember implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String nome;
    private final String ip;

    public RoomMember(String id, String nome, String ip) {
        this.id = id;
        this.nome = nome;
        this.ip = ip;
    }

    public String getId() { return id; }
    public String getNome() { return nome; }
    public String getIp() { return ip; }
}
