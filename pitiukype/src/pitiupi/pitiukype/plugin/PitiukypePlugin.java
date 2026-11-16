package pitiupi.pitiukype.plugin;

import pitiupi.api.plugin.PeerActionsRegistrar;
import pitiupi.api.plugin.PluginWorkspace;
import pitiupi.application.peeractions.PeerAction;
import pitiupi.application.peeractions.PeerActionTarget;
import pitiupi.application.peeractions.PeerTarget;
import pitiupi.control.ConnectionManager;
import pitiupi.models.Messages.Message;
import pitiupi.models.Plugin;
import pitiupi.pitiukype.GUI.PitiukypePanel;
import pitiupi.pitiukype.control.PingController;
import pitiupi.pitiukype.control.AudioController;
import pitiupi.pitiukype.control.VoiceController;
import pitiupi.pitiukype.control.VoiceTransport;
import pitiupi.pitiukype.control.RoomController;
import pitiupi.pitiukype.control.RoomHost;
import pitiupi.pitiukype.net.PingMessage;
import pitiupi.pitiukype.net.RoomMessage;
import pitiupi.pitiukype.net.AudioMessage;
import pitiupi.presentation.shell.LocalNetworkInfo;

import javax.swing.SwingUtilities;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public final class PitiukypePlugin extends Plugin implements RoomHost, VoiceTransport {
    private ConnectionManager connectionManager;
    private PluginWorkspace workspace;
    private PitiukypePanel painel;
    private PingController pingController;
    private RoomController roomController;
    private AudioController audioController;
    private VoiceController voiceController;

    @Override
    public void createPlugin(ConnectionManager connectionManager, PluginWorkspace workspace) {
        this.connectionManager = connectionManager;
        this.workspace = workspace;
        this.painel = new PitiukypePanel();
        this.pingController = new PingController(connectionManager, painel);
        this.roomController = new RoomController(this, painel);
        this.audioController = new AudioController(painel);
        this.voiceController = new VoiceController(this, painel, roomController.getMeuId());
        painel.setAudioController(audioController);
        painel.setVoiceController(voiceController);
        painel.setController(roomController);
        roomController.setOuvinteSala(sala -> {
            if (sala != null) audioController.parar();
            voiceController.atualizarSala(sala);
        });
        bindShellWorkspaceContent(painel);
        installPeerActions(workspace.peerActions());
    }

    @Override
    public void installPeerActions(PeerActionsRegistrar registrar) {
        registrar.register(PeerAction.of(
                "pitiukype.abrir",
                "Ver salas do Pitiukype",
                PeerActionTarget.ALL_PEERS,
                alvo -> workspace.openOrFocusTab(getIdentifier(), getDisplayName())
        ));
        registrar.register(PeerAction.of(
                "pitiukype.ping",
                "Testar (PING/PONG)",
                PeerActionTarget.SINGLE_PEER,
                alvo -> {
                    if (alvo instanceof PeerTarget.Single selecionado) {
                        workspace.openOrFocusTab(getIdentifier(), getDisplayName());
                        selecionado.peer().getInetAddress().ifPresent(pingController::enviarPing);
                    }
                }
        ));
    }

    @Override
    public List<Class<? extends Message>> getMessages() {
        return List.of(PingMessage.class, RoomMessage.class, AudioMessage.class);
    }

    @Override
    public void receiveMessage(Message message) {
        if (message instanceof AudioMessage audio && voiceController != null) {
            voiceController.receber(audio);
        } else if (message instanceof RoomMessage sala && roomController != null) {
            SwingUtilities.invokeLater(() -> roomController.receber(sala));
        } else if (message instanceof PingMessage ping && pingController != null) {
            SwingUtilities.invokeLater(() -> pingController.receber(ping));
        }
    }

    @Override
    public String ipLocal() {
        return LocalNetworkInfo.bestLocalIpv4();
    }

    @Override
    public boolean enviarMulticast(RoomMessage mensagem) {
        return connectionManager.sendUdpMulticast(mensagem);
    }

    @Override
    public boolean enviarUnicast(RoomMessage mensagem, String destinoIp) {
        // Segunda comp é o ipv4 literal
        if (destinoIp == null || !destinoIp.matches("(?:\\d{1,3}\\.){3}\\d{1,3}")) {
            return false;
        }
        try {
            InetAddress endereco = InetAddress.getByName(destinoIp);
            return connectionManager.sendUdpUnicast(mensagem, endereco);
        } catch (UnknownHostException | IllegalArgumentException erro) {
            return false;
        }
    }

    @Override
    public boolean enviarAudio(AudioMessage mensagem, String destinoIp) {
        if (destinoIp == null || !destinoIp.matches("(?:\\d{1,3}\\.){3}\\d{1,3}")) {
            return false;
        }
        try {
            return connectionManager.sendUdpUnicast(mensagem, InetAddress.getByName(destinoIp));
        } catch (UnknownHostException | IllegalArgumentException erro) {
            return false;
        }
    }

    @Override
    public void onShellTabClosed() {
        if (voiceController != null) voiceController.parar();
        if (audioController != null) audioController.parar();
        if (roomController != null) {
            SwingUtilities.invokeLater(roomController::sairSala);
        }
    }

    @Override public String getName() { return "Pitiukype"; }
    @Override public String getDisplayName() { return "Pitiukype - Salas de voz"; }
    @Override public String getAuthor() { return "Davy"; }
    @Override public String getVersion() { return "0.5.2"; }
    @Override public String getIdentifier() { return "pitiupi.pitiukype"; }
}
