package pitiupi.pitiukype.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class AudioPlayback {
    private static final AudioFormat FORMATO = new AudioFormat(16000, 16, 1, true, false);
    private final ArrayBlockingQueue<byte[]> fila = new ArrayBlockingQueue<>(8);
    private volatile boolean ativo;
    private volatile SourceDataLine saida;
    private Thread thread;

    public synchronized boolean iniciar(Consumer<String> aoErro) {
        if (thread != null && thread.isAlive()) return false;
        ativo = true;
        thread = new Thread(() -> executar(aoErro), "pitiukype-reproducao-remota");
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    public void adicionar(byte[] bloco) {
        if (!ativo || bloco == null || bloco.length != 640) return;
        if (!fila.offer(bloco)) {
            fila.poll();
            fila.offer(bloco);
        }
    }

    public void limpar() { fila.clear(); }

    public void parar() {
        ativo = false;
        fila.clear();
        SourceDataLine linha = saida;
        if (linha != null) linha.close(); 
    }

    private void executar(Consumer<String> aoErro) {
        SourceDataLine linha = null;
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, FORMATO);
            linha = (SourceDataLine) AudioSystem.getLine(info);
            linha.open(FORMATO, 640 * 6);
            saida = linha;
            if (!ativo) return;
            linha.start();
            while (ativo) {
                byte[] bloco = fila.poll(100, TimeUnit.MILLISECONDS);
                if (bloco == null) continue;
                int escrito = 0;
                while (ativo && escrito < bloco.length) {
                    int n = linha.write(bloco, escrito, bloco.length - escrito);
                    if (n <= 0) break;
                    escrito += n;
                }
            }
        } catch (LineUnavailableException erro) {
            if (ativo) aoErro.accept("Saida de audio indisponivel: " + erro.getMessage());
        } catch (InterruptedException erro) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException erro) {
            if (ativo) aoErro.accept("Falha ao reproduzir voz: " + erro.getMessage());
        } finally {
            ativo = false;
            if (linha != null) linha.close();
            saida = null;
            fila.clear();
        }
    }
}
