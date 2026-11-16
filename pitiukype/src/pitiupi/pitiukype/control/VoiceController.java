package pitiupi.pitiukype.control;

import pitiupi.pitiukype.audio.AudioCapture;
import pitiupi.pitiukype.audio.AudioPlayback;
import pitiupi.pitiukype.model.Room;
import pitiupi.pitiukype.model.RoomMember;
import pitiupi.pitiukype.net.AudioMessage;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class VoiceController {
    private final VoiceTransport rede;
    private final PitiukypeUiSink ui;
    private final String meuId;
    private final AudioCapture captura = new AudioCapture();
    private final AudioPlayback reproducao = new AudioPlayback();
    private final AtomicLong proximaSequencia = new AtomicLong();

    private volatile Room salaAtual;
    private volatile boolean saidaSilenciada;
    private volatile String fonteAtual;
    private volatile long ultimaSequencia = -1;

    public VoiceController(VoiceTransport rede, PitiukypeUiSink ui, String meuId) {
        this.rede = rede;
        this.ui = ui;
        this.meuId = meuId;
    }

    public void atualizarSala(Room novaSala) {
        Room anterior = salaAtual;
        String idAntigo = anterior == null ? null : anterior.getId();
        String idNovo = novaSala == null ? null : novaSala.getId();
        if (!Objects.equals(idAntigo, idNovo)) {
            captura.parar();
            reproducao.parar();
            fonteAtual = null;
            ultimaSequencia = -1;
            proximaSequencia.set(0);
            saidaSilenciada = false;
            salaAtual = novaSala;
            if (novaSala != null) {
                reproducao.iniciar(ui::mostrarStatus);
            }
            ui.atualizarEstadoVoz(false, false);
        } else {
            salaAtual = novaSala;
        }
    }

    public void alternarMicrofone() {
        if (captura.estaAtivo()) {
            captura.parar();
            ui.atualizarEstadoVoz(false, saidaSilenciada);
            return;
        }
        if (salaAtual == null) {
            ui.mostrarStatus("Entre em uma sala antes de ativar a voz.");
            return;
        }
        boolean iniciou = captura.iniciar(this::transmitirBloco, mensagem -> {
            ui.atualizarEstadoVoz(false, saidaSilenciada);
            ui.mostrarStatus(mensagem);
        });
        if (iniciou) {
            ui.atualizarEstadoVoz(true, saidaSilenciada);
            ui.mostrarStatus("Microfone da chamada ativado.");
        } else {
            ui.mostrarStatus("O microfone anterior ainda esta finalizando.");
        }
    }

    public void alternarAudio() {
        saidaSilenciada = !saidaSilenciada;
        if (saidaSilenciada) reproducao.limpar();
        ui.atualizarEstadoVoz(captura.estaAtivo(), saidaSilenciada);
    }

    private void transmitirBloco(byte[] pcm) {
        Room sala = salaAtual;
        if (sala == null) return;
        AudioMessage mensagem = new AudioMessage(
                sala.getId(), meuId, proximaSequencia.getAndIncrement(), pcm);
        for (RoomMember membro : sala.getParticipantes()) {
            if (!meuId.equals(membro.getId())) {
                rede.enviarAudio(mensagem, membro.getIp());
            }
        }
    }

    public void receber(AudioMessage mensagem) {
        Room sala = salaAtual;
        if (sala == null || mensagem == null || saidaSilenciada) return;
        if (!sala.getId().equals(mensagem.getSalaId())) return;
        if (mensagem.getEmissorId() == null || meuId.equals(mensagem.getEmissorId())) return;
        if (mensagem.getSequencia() < 0) return;
        boolean participante = sala.getParticipantes().stream()
                .anyMatch(m -> mensagem.getEmissorId().equals(m.getId()));
        if (!participante) return;
        if (fonteAtual == null) fonteAtual = mensagem.getEmissorId();
        if (!fonteAtual.equals(mensagem.getEmissorId())) return;
        if (mensagem.getSequencia() <= ultimaSequencia) return;
        byte[] pcm = mensagem.getAudioPcm();
        if (pcm.length != AudioMessage.TAMANHO_PCM) return;
        ultimaSequencia = mensagem.getSequencia();
        reproducao.adicionar(pcm);
    }

    public void parar() { atualizarSala(null); }
}
