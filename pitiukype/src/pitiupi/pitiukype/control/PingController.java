package pitiupi.pitiukype.control;

import pitiupi.control.ConnectionManager;
import pitiupi.pitiukype.net.PingMessage;
import pitiupi.presentation.shell.LocalNetworkInfo;
import java.net.InetAddress;
import java.net.UnknownHostException;

public final class PingController {
    private final ConnectionManager rede;
    private final PitiukypeUiSink ui;

    public PingController(ConnectionManager rede, PitiukypeUiSink ui) {
        this.rede = rede;
        this.ui = ui;
    }

    public void enviarPing(InetAddress destino) {
        String ip = LocalNetworkInfo.bestLocalIpv4();
        if (ip == null || ip.equals("—")) {
            ui.mostrarStatus("IP local indisponivel.");
            return;
        }
        boolean ok = rede.sendUdpUnicast(new PingMessage("PING", ip), destino);
        ui.mostrarStatus(ok ? "PING enviado para " + destino.getHostAddress() : "Erro ao enviar PING.");
    }

    public void receber(PingMessage mensagem) {
        if ("PONG".equals(mensagem.getTipo())) {
            ui.mostrarStatus("PONG recebido de " + mensagem.getIpOrigem());
        } else if ("PING".equals(mensagem.getTipo())) {
            try {
                InetAddress endereco = InetAddress.getByName(mensagem.getIpOrigem());
                rede.sendUdpUnicast(new PingMessage("PONG", LocalNetworkInfo.bestLocalIpv4()), endereco);
                ui.mostrarStatus("PING recebido de " + mensagem.getIpOrigem());
            } catch (UnknownHostException ex) {
                ui.mostrarStatus("IP de PING invalido.");
            }
        }
    }
}
