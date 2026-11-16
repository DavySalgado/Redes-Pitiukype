package pitiupi.pitiukype.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.util.function.Consumer;

public final class AudioMonitor {
    private static final AudioFormat FORMATO = new AudioFormat(16000, 16, 1, true, false);
    private static final int BYTES_POR_BLOCO = 640; // 20 ms de PCM mono, 16 bits, 16 kHz

    private volatile boolean ativo;
    private volatile TargetDataLine microfone;
    private volatile SourceDataLine altoFalante;
    private Thread thread;

    public synchronized boolean iniciar(Consumer<String> aoEncerrar) {
        if (thread != null && thread.isAlive()) return false;
        ativo = true;
        thread = new Thread(() -> executar(aoEncerrar), "pitiukype-audio-local");
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    public boolean estaAtivo() {
        return ativo;
    }

    public void parar() {
        ativo = false;
        TargetDataLine entrada = microfone;
        SourceDataLine saida = altoFalante;
        if (entrada != null) entrada.close();
        if (saida != null) saida.close();
    }

    private void executar(Consumer<String> aoEncerrar) {
        TargetDataLine entrada = null;
        SourceDataLine saida = null;
        String resultado = "Teste de microfone encerrado.";
        try {
            DataLine.Info infoEntrada = new DataLine.Info(TargetDataLine.class, FORMATO);
            DataLine.Info infoSaida = new DataLine.Info(SourceDataLine.class, FORMATO);

            entrada = (TargetDataLine) AudioSystem.getLine(infoEntrada);
            saida = (SourceDataLine) AudioSystem.getLine(infoSaida);
            entrada.open(FORMATO, BYTES_POR_BLOCO * 8);
            saida.open(FORMATO, BYTES_POR_BLOCO * 8);

            microfone = entrada;
            altoFalante = saida;
            if (ativo) {
                entrada.start();
                saida.start();
                byte[] bloco = new byte[BYTES_POR_BLOCO];

                while (ativo) {
                    int lidos = entrada.read(bloco, 0, bloco.length);
                    int enviados = 0;
                    while (ativo && enviados < lidos) {
                        int escritos = saida.write(bloco, enviados, lidos - enviados);
                        if (escritos <= 0) break;
                        enviados += escritos;
                    }
                }
            }
        } catch (LineUnavailableException | IllegalArgumentException erro) {
            if (ativo) resultado = "Microfone ou saida indisponivel: " + erro.getMessage();
        } catch (RuntimeException erro) {
            if (ativo) resultado = "Falha no teste de audio: " + erro.getMessage();
        } finally {
            ativo = false;
            if (entrada != null) entrada.close();
            if (saida != null) saida.close();
            microfone = null;
            altoFalante = null;
            aoEncerrar.accept(resultado);
        }
    }
}
