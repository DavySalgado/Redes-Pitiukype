package pitiupi.pitiukype.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.util.Arrays;
import java.util.function.Consumer;

public final class AudioCapture {
    private static final AudioFormat FORMATO = new AudioFormat(16000, 16, 1, true, false);
    private static final int BLOCO = 640;
    private volatile boolean ativo;
    private volatile TargetDataLine microfone;
    private Thread thread;

    public synchronized boolean iniciar(Consumer<byte[]> aoCapturar, Consumer<String> aoEncerrar) {
        if (thread != null && thread.isAlive()) return false;
        ativo = true;
        thread = new Thread(() -> executar(aoCapturar, aoEncerrar), "pitiukype-microfone-rede");
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    public boolean estaAtivo() { return ativo; }

    public void parar() {
        ativo = false;
        TargetDataLine linha = microfone;
        if (linha != null) linha.close(); 
    }

    private void executar(Consumer<byte[]> aoCapturar, Consumer<String> aoEncerrar) {
        TargetDataLine entrada = null;
        String resultado = "Microfone da chamada parado.";
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMATO);
            entrada = (TargetDataLine) AudioSystem.getLine(info);
            entrada.open(FORMATO, BLOCO * 8);
            microfone = entrada;
            if (!ativo) return;
            entrada.start();
            byte[] bloco = new byte[BLOCO];
            while (ativo) {
                int total = 0;
                while (ativo && total < BLOCO) {
                    int lidos = entrada.read(bloco, total, BLOCO - total);
                    if (lidos <= 0) break;
                    total += lidos;
                }
                if (ativo && total == BLOCO) aoCapturar.accept(Arrays.copyOf(bloco, BLOCO));
            }
        } catch (LineUnavailableException | IllegalArgumentException erro) {
            if (ativo) resultado = "Microfone indisponivel: " + erro.getMessage();
        } catch (RuntimeException erro) {
            if (ativo) resultado = "Erro ao capturar voz: " + erro.getMessage();
        } finally {
            ativo = false;
            if (entrada != null) entrada.close();
            microfone = null;
            aoEncerrar.accept(resultado);
        }
    }
}
