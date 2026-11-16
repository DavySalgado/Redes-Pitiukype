package pitiupi.pitiukype.control;

import pitiupi.pitiukype.net.RoomMessage;

public interface RoomHost {
    String ipLocal();
    boolean enviarMulticast(RoomMessage mensagem);
    boolean enviarUnicast(RoomMessage mensagem, String destinoIp);
}
