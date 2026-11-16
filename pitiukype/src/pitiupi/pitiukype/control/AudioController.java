
package pitiupi.pitiukype.control;

import pitiupi.pitiukype.audio.AudioMonitor;

public final class AudioController {

    private final AudioMonitor monitor = new AudioMonitor();

    private final PitiukypeUiSink interfaceUsuario;

    public AudioController(PitiukypeUiSink interfaceUsuario) {
        this.interfaceUsuario = interfaceUsuario;
    }

    public void alternarTeste() {

        if (monitor.estaAtivo()) {
            parar();
            return;
        }

        boolean iniciou = monitor.iniciar(mensagem -> {
            interfaceUsuario.atualizarTesteAudio(false);
            interfaceUsuario.mostrarStatus(mensagem);
        });

        if (iniciou) {
            interfaceUsuario.atualizarTesteAudio(true);
            interfaceUsuario.mostrarStatus(
                "Monitorando microfone local. Use fones de ouvido."
            );
        } else {
            interfaceUsuario.mostrarStatus(
                "O teste anterior ainda esta finalizando."
            );
        }
    }

    public void parar() {
        monitor.parar();
        interfaceUsuario.atualizarTesteAudio(false);
    }
}
