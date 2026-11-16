package pitiupi.pitiukype.control;

import pitiupi.pitiukype.model.Room;
import pitiupi.pitiukype.model.RoomMember;
import pitiupi.pitiukype.net.RoomMessage;
import pitiupi.pitiukype.net.RoomMessage.Tipo;

import javax.swing.Timer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class RoomController {
    private static final int INTERVALO_MS = 3000;
    private static final long LIMITE_MS = 12000;
    private static final int MAX_MEMBROS = 8;
    private static final int MAX_TENTATIVAS = 4;

    private final RoomHost rede;
    private final PitiukypeUiSink ui;
    private final String meuId = UUID.randomUUID().toString();
    private final Map<String, Room> salas = new LinkedHashMap<>();
    private final Map<String, Long> ultimaAtividadeMembros = new HashMap<>();
    private final Timer temporizador;
    private Consumer<Room> ouvinteSala;

    private RoomMember eu;
    private String salaAtualId;
    private String entradaPendenteId;
    private int tentativasEntrada;

    public RoomController(RoomHost rede, PitiukypeUiSink ui) {
        this.rede = rede;
        this.ui = ui;
        temporizador = new Timer(INTERVALO_MS, e -> verificarPeriodicamente());
        temporizador.start();
        publicarInterface();
    }

    public String getMeuId() { return meuId; }

    public void setOuvinteSala(Consumer<Room> ouvinte) {
        this.ouvinteSala = ouvinte;
        if (ouvinte != null) ouvinte.accept(salas.get(salaAtualId));
    }

    private boolean prepararIdentidade(String apelido) {
        String ip = rede.ipLocal();
        if (ip == null || !ip.matches("(?:\\d{1,3}\\.){3}\\d{1,3}")) {
            ui.mostrarStatus("Selecione uma interface Wi-Fi/Cabo valida nas configuracoes.");
            return false;
        }
        String nome = apelido == null ? "" : apelido.trim();
        if (nome.isBlank() || nome.length() > 24) {
            ui.mostrarStatus("Use um apelido entre 1 e 24 caracteres.");
            return false;
        }
        eu = new RoomMember(meuId, nome, ip);
        return true;
    }

    public void criarSala(String nome, String apelido) {
        if (salaAtualId != null || entradaPendenteId != null) {
            ui.mostrarStatus("Saia da sala atual antes de criar outra.");
            return;
        }
        String titulo = nome == null ? "" : nome.trim();
        if (titulo.isBlank() || titulo.length() > 30) {
            ui.mostrarStatus("O nome da sala precisa ter entre 1 e 30 caracteres.");
            return;
        }
        if (!prepararIdentidade(apelido)) return;

        long agora = System.currentTimeMillis();
        Room sala = Room.criar(titulo, eu, agora);
        salas.put(sala.getId(), sala);
        salaAtualId = sala.getId();
        ultimaAtividadeMembros.put(meuId, agora);
        anunciarSala(sala);
        ui.mostrarStatus("Sala '" + titulo + "' criada.");
        publicarInterface();
    }

    public void entrarSala(String id, String apelido) {
        if (salaAtualId != null || entradaPendenteId != null) {
            ui.mostrarStatus("Voce ja esta em uma sala ou aguardando entrada.");
            return;
        }
        Room sala = salas.get(id);
        if (sala == null || System.currentTimeMillis() - sala.getUltimoAnuncio() > LIMITE_MS) {
            ui.mostrarStatus("Esta sala nao esta mais disponivel.");
            return;
        }
        if (!prepararIdentidade(apelido)) return;
        entradaPendenteId = id;
        tentativasEntrada = 0;
        tentarEntrada();
        publicarInterface();
    }

    private void tentarEntrada() {
        Room sala = salas.get(entradaPendenteId);
        if (sala == null) {
            cancelarEntrada("Sala indisponivel; tente novamente.");
            return;
        }
        if (tentativasEntrada >= MAX_TENTATIVAS) {
            cancelarEntrada("Sem resposta do organizador; tente novamente.");
            return;
        }
        tentativasEntrada++;
        RoomMessage pedido = new RoomMessage(Tipo.PEDIR_ENTRADA, sala, eu, null, null);
        boolean ok = rede.enviarUnicast(pedido, sala.getHostIp());
        ui.mostrarStatus(ok
                ? "Solicitando entrada em '" + sala.getNome() + "' (" + tentativasEntrada + "/" + MAX_TENTATIVAS + ")..."
                : "Falha ao enviar pedido, nova tentativa...");
    }

    private void cancelarEntrada(String motivo) {
        entradaPendenteId = null;
        tentativasEntrada = 0;
        ui.mostrarStatus(motivo);
        publicarInterface();
    }

    public void sairSala() {
        if (entradaPendenteId != null && salaAtualId == null) {
            cancelarEntrada("Pedido de entrada cancelado.");
            return;
        }
        Room sala = salas.get(salaAtualId);
        if (sala == null || eu == null) return;

        if (souOrganizador(sala)) {
            RoomMessage aviso = new RoomMessage(Tipo.ENCERRAMENTO, sala, eu, null, null);
            rede.enviarMulticast(aviso);
            for (RoomMember membro : sala.getParticipantes()) {
                if (!membro.getId().equals(meuId)) {
                    rede.enviarUnicast(aviso, membro.getIp());
                }
            }
            salas.remove(sala.getId());
            ultimaAtividadeMembros.clear();
            ui.mostrarStatus("Sala encerrada pelo organizador.");
        } else {
            RoomMessage saida = new RoomMessage(Tipo.SAIDA, sala, eu, null, null);
            rede.enviarUnicast(saida, sala.getHostIp());
            ui.mostrarStatus("Voce saiu de '" + sala.getNome() + "'.");
        }
        salaAtualId = null;
        publicarInterface();
    }

    public void receber(RoomMessage mensagem) {
        if (!mensagemValida(mensagem)) return;
        RoomMember emissor = mensagem.getEmissor();
        if (meuId.equals(emissor.getId())) return;
        long agora = System.currentTimeMillis();

        switch (mensagem.getTipo()) {
            case ANUNCIO -> receberAnuncio(mensagem, agora);
            case PEDIR_ENTRADA -> receberPedido(mensagem, agora);
            case ENTRADA_ACEITA -> receberAceite(mensagem, agora);
            case ENTRADA_RECUSADA -> {
                if (entradaPendenteId != null
                        && entradaPendenteId.equals(mensagem.getSalaId())
                        && meuId.equals(mensagem.getDestinatarioId())
                        && mensagem.getHostId().equals(emissor.getId())) {
                    cancelarEntrada("Entrada recusada: " + mensagem.getMotivo());
                }
            }
            case BATIMENTO -> receberBatimento(mensagem, agora);
            case SAIDA -> receberSaida(mensagem, agora);
            case ENCERRAMENTO -> {
                if (!mensagem.getHostId().equals(emissor.getId())) return;
                salas.remove(mensagem.getSalaId());
                if (mensagem.getSalaId().equals(salaAtualId)) {
                    salaAtualId = null;
                    ui.mostrarStatus("O organizador encerrou a sala.");
                }
                if (mensagem.getSalaId().equals(entradaPendenteId)) {
                    entradaPendenteId = null;
                }
                publicarInterface();
            }
        }
    }

    private boolean mensagemValida(RoomMessage m) {
        if (m == null || m.getTipo() == null || m.getEmissor() == null) return false;
        if (m.getSalaId() == null || m.getHostId() == null || m.getHostIp() == null) return false;
        if (m.getSalaNome() == null || m.getSalaNome().length() > 30) return false;
        if (m.getParticipantes() == null || m.getParticipantes().size() > MAX_MEMBROS) return false;
        RoomMember p = m.getEmissor();
        return p.getId() != null && p.getNome() != null && p.getIp() != null
                && p.getNome().length() <= 24 && p.getId().length() <= 80;
    }

    private Room construirSala(RoomMessage mensagem, long agora) {
        return new Room(mensagem.getSalaId(), mensagem.getSalaNome(),
                mensagem.getHostId(), mensagem.getHostIp(),
                mensagem.getParticipantes(), mensagem.getVersao(), agora);
    }

    private void receberAnuncio(RoomMessage mensagem, long agora) {
        if (!mensagem.getHostId().equals(mensagem.getEmissor().getId())) return;
        Room antigo = salas.get(mensagem.getSalaId());
        if (antigo != null && (!antigo.getHostId().equals(mensagem.getHostId())
                || mensagem.getVersao() < antigo.getVersao())) {
            return;
        }
        salas.put(mensagem.getSalaId(), construirSala(mensagem, agora));
        publicarInterface();
    }

    private void receberPedido(RoomMessage mensagem, long agora) {
        Room sala = salas.get(salaAtualId);
        if (sala == null || !souOrganizador(sala) || !sala.getId().equals(mensagem.getSalaId())) return;
        RoomMember convidado = mensagem.getEmissor();
        boolean jaParticipa = sala.getParticipantes().stream()
                .anyMatch(m -> m.getId().equals(convidado.getId()));
        if (!jaParticipa && sala.getParticipantes().size() >= MAX_MEMBROS) {
            rede.enviarUnicast(new RoomMessage(Tipo.ENTRADA_RECUSADA, sala, eu,
                    convidado.getId(), "Sala cheia"), convidado.getIp());
            return;
        }
        ultimaAtividadeMembros.put(convidado.getId(), agora);
        if (!jaParticipa) {
            List<RoomMember> membros = new ArrayList<>(sala.getParticipantes());
            membros.add(convidado);
            sala = sala.atualizar(membros, sala.getVersao() + 1, agora);
            salas.put(sala.getId(), sala);
            ui.mostrarStatus(convidado.getNome() + " entrou na sala.");
        }
        rede.enviarUnicast(new RoomMessage(Tipo.ENTRADA_ACEITA, sala, eu,
                convidado.getId(), null), convidado.getIp());
        anunciarSala(sala);
        publicarInterface();
    }

    private void receberAceite(RoomMessage mensagem, long agora) {
        if (entradaPendenteId == null || !entradaPendenteId.equals(mensagem.getSalaId())) return;
        if (!meuId.equals(mensagem.getDestinatarioId())) return;
        if (!mensagem.getHostId().equals(mensagem.getEmissor().getId())) return;
        Room original = salas.get(mensagem.getSalaId());
        if (original == null || !original.getHostId().equals(mensagem.getHostId())) return;

        salas.put(mensagem.getSalaId(), construirSala(mensagem, agora));
        salaAtualId = mensagem.getSalaId();
        entradaPendenteId = null;
        tentativasEntrada = 0;
        ui.mostrarStatus("Voce entrou em '" + mensagem.getSalaNome() + "'.");
        publicarInterface();
    }

    private void receberBatimento(RoomMessage mensagem, long agora) {
        Room sala = salas.get(salaAtualId);
        if (sala == null || !souOrganizador(sala) || !sala.getId().equals(mensagem.getSalaId())) return;
        boolean cadastrado = sala.getParticipantes().stream()
                .anyMatch(m -> m.getId().equals(mensagem.getEmissor().getId()));
        if (cadastrado) ultimaAtividadeMembros.put(mensagem.getEmissor().getId(), agora);
    }

    private void receberSaida(RoomMessage mensagem, long agora) {
        Room sala = salas.get(salaAtualId);
        if (sala == null || !souOrganizador(sala) || !sala.getId().equals(mensagem.getSalaId())) return;
        List<RoomMember> novaLista = sala.getParticipantes().stream()
                .filter(m -> !m.getId().equals(mensagem.getEmissor().getId())).toList();
        if (novaLista.size() == sala.getParticipantes().size()) return;
        ultimaAtividadeMembros.remove(mensagem.getEmissor().getId());
        sala = sala.atualizar(novaLista, sala.getVersao() + 1, agora);
        salas.put(sala.getId(), sala);
        anunciarSala(sala);
        publicarInterface();
        ui.mostrarStatus(mensagem.getEmissor().getNome() + " saiu da sala.");
    }

    private void verificarPeriodicamente() {
        long agora = System.currentTimeMillis();
        Room atual = salas.get(salaAtualId);
        if (atual != null && souOrganizador(atual)) {
            List<RoomMember> ativos = atual.getParticipantes().stream()
                    .filter(m -> m.getId().equals(meuId)
                            || agora - ultimaAtividadeMembros.getOrDefault(m.getId(), 0L) <= LIMITE_MS)
                    .toList();
            if (ativos.size() != atual.getParticipantes().size()) {
                atual = atual.atualizar(ativos, atual.getVersao() + 1, agora);
                salas.put(atual.getId(), atual);
                ui.mostrarStatus("Participantes inativos removidos da sala.");
            }
            salas.put(atual.getId(), atual.vistoAgora(agora));
            anunciarSala(atual);
        } else if (atual != null && eu != null) {
            rede.enviarUnicast(new RoomMessage(Tipo.BATIMENTO, atual, eu, null, null),
                    atual.getHostIp());
        }

        if (entradaPendenteId != null) tentarEntrada();

        List<String> expiradas = salas.values().stream()
                .filter(s -> !souOrganizador(s) && agora - s.getUltimoAnuncio() > LIMITE_MS)
                .map(Room::getId).toList();
        for (String id : expiradas) {
            salas.remove(id);
            if (id.equals(salaAtualId)) {
                salaAtualId = null;
                ui.mostrarStatus("Conexao com a sala perdida (sem anuncios UDP).");
            }
            if (id.equals(entradaPendenteId)) {
                entradaPendenteId = null;
                ui.mostrarStatus("Sala nao responde mais.");
            }
        }
        publicarInterface();
    }

    private boolean souOrganizador(Room sala) {
        return sala != null && meuId.equals(sala.getHostId());
    }

    private void anunciarSala(Room sala) {
        rede.enviarMulticast(new RoomMessage(Tipo.ANUNCIO, sala, eu, null, null));
    }

    private void publicarInterface() {
        List<Room> lista = salas.values().stream()
                .sorted(Comparator.comparing(Room::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList();
        ui.atualizarSalas(lista, salaAtualId, entradaPendenteId);
        if (ouvinteSala != null) ouvinteSala.accept(salas.get(salaAtualId));
    }
}
