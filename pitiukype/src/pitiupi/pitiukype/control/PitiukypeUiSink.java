package pitiupi.pitiukype.control;

import java.util.List;
import pitiupi.pitiukype.model.Room;

public interface PitiukypeUiSink {
    void mostrarStatus(String texto);
    void atualizarTesteAudio(boolean ativo);
    void atualizarEstadoVoz(boolean microfoneAtivo, boolean audioSilenciado);
    void atualizarSalas(List<Room> salas, String salaAtualId, String convitePendenteId);
}
