package pitiupi.pitiukype.GUI;

import pitiupi.pitiukype.control.AudioController;
import pitiupi.pitiukype.control.PitiukypeUiSink;
import pitiupi.pitiukype.control.RoomController;
import pitiupi.pitiukype.control.VoiceController;
import pitiupi.pitiukype.model.Room;
import pitiupi.pitiukype.model.RoomMember;
import pitiupi.presentation.shell.ui.ShellUi;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Objects;

public final class PitiukypePanel extends JPanel implements PitiukypeUiSink {
    private final JTextField apelido = new JTextField(System.getProperty("user.name", "Usuario"), 14);
    private final JTextField nomeSala = new JTextField("Geral", 14);
    private final JButton criar = new JButton("Criar sala");
    private final JButton entrar = new JButton("Entrar na sala");
    private final JButton sair = new JButton("Sair da sala");

    private final JButton microfoneChamada = new JButton("Ativar microfone");
    private final JButton silenciarChamada = new JButton("Silenciar audio");
    private final JButton testarAudio = new JButton("Testar microfone local");

    private final JLabel tituloSala = new JLabel("Selecione uma sala");
    private final JLabel descricaoSala = ShellUi.mutedLabel("As salas aparecem automaticamente na rede local.");
    private final JLabel quantidade = ShellUi.mutedLabel("0 participantes");
    private final JLabel chamada = new JLabel("Fora de uma chamada");
    private final JLabel status = ShellUi.mutedLabel("Aguardando salas");

    private final DefaultListModel<Room> modeloSalas = new DefaultListModel<>();
    private final JList<Room> listaSalas = new JList<>(modeloSalas);
    private final DefaultListModel<RoomMember> modeloMembros = new DefaultListModel<>();
    private final JList<RoomMember> listaMembros = new JList<>(modeloMembros);

    private RoomController roomController;
    private AudioController audioController;
    private VoiceController voiceController;
    private String salaAtualId;
    private String pendenteId;
    private boolean atualizandoLista;

    public PitiukypePanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(construirCabecalho(), BorderLayout.NORTH);
        add(construirConteudo(), BorderLayout.CENTER);
        add(construirBarraStatus(), BorderLayout.SOUTH);
        registrarEventos();
        atualizarBotoes();
    }

    public void setController(RoomController controller) {
        this.roomController = controller;
    }

    public void setAudioController(AudioController controller) {
        this.audioController = controller;
    }

    public void setVoiceController(VoiceController controller) {
        this.voiceController = controller;
    }

    private JPanel construirCabecalho() {
        JPanel topo = new JPanel(new BorderLayout(12, 0));
        JLabel titulo = new JLabel("Pitiukype - Salas de voz");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));
        topo.add(titulo, BorderLayout.WEST);

        JPanel usuario = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        usuario.add(new JLabel("Apelido:"));
        usuario.add(apelido);
        topo.add(usuario, BorderLayout.EAST);
        return topo;
    }

    private JSplitPane construirConteudo() {
        JSplitPane divisao = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                construirListaSalas(),
                construirDetalhesSala()
        );
        divisao.setResizeWeight(0.30);
        divisao.setDividerLocation(270);
        divisao.setContinuousLayout(true);
        return divisao;
    }

    private JPanel construirListaSalas() {
        JPanel esquerda = new JPanel(new BorderLayout(6, 8));
        esquerda.setMinimumSize(new Dimension(200, 240));
        esquerda.setPreferredSize(new Dimension(265, 400));
        esquerda.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel titulo = new JLabel("Salas disponiveis");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD));
        esquerda.add(titulo, BorderLayout.NORTH);

        listaSalas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaSalas.setVisibleRowCount(8);
        listaSalas.setCellRenderer(new SalaRenderer());
        JScrollPane scroll = new JScrollPane(listaSalas);
        esquerda.add(scroll, BorderLayout.CENTER);

        JPanel novaSala = new JPanel(new BorderLayout(0, 5));
        novaSala.setBorder(BorderFactory.createTitledBorder("Nova sala"));
        novaSala.add(nomeSala, BorderLayout.CENTER);
        novaSala.add(criar, BorderLayout.SOUTH);
        esquerda.add(novaSala, BorderLayout.SOUTH);
        return esquerda;
    }

    private JPanel construirDetalhesSala() {
        JPanel direita = new JPanel(new BorderLayout(6, 8));
        direita.setMinimumSize(new Dimension(350, 260));
        direita.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 8));

        JPanel cabecalho = new JPanel();
        cabecalho.setLayout(new BoxLayout(cabecalho, BoxLayout.Y_AXIS));
        chamada.setAlignmentX(Component.LEFT_ALIGNMENT);
        tituloSala.setAlignmentX(Component.LEFT_ALIGNMENT);
        descricaoSala.setAlignmentX(Component.LEFT_ALIGNMENT);
        quantidade.setAlignmentX(Component.LEFT_ALIGNMENT);
        tituloSala.setFont(tituloSala.getFont().deriveFont(Font.BOLD, 17f));
        for (JLabel rotulo : new JLabel[]{chamada, tituloSala, descricaoSala, quantidade}) {
            rotulo.setMaximumSize(new Dimension(Integer.MAX_VALUE, rotulo.getPreferredSize().height));
        }
        cabecalho.add(chamada);
        cabecalho.add(Box.createVerticalStrut(12));
        cabecalho.add(tituloSala);
        cabecalho.add(Box.createVerticalStrut(4));
        cabecalho.add(descricaoSala);
        cabecalho.add(Box.createVerticalStrut(5));
        cabecalho.add(quantidade);
        direita.add(cabecalho, BorderLayout.NORTH);

        listaMembros.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaMembros.setCellRenderer(new MembroRenderer());
        JScrollPane scroll = new JScrollPane(listaMembros);
        scroll.setBorder(BorderFactory.createTitledBorder("Participantes"));
        direita.add(scroll, BorderLayout.CENTER);

        JPanel comandos = new JPanel();
        comandos.setLayout(new BoxLayout(comandos, BoxLayout.Y_AXIS));

        JPanel acoesSala = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        acoesSala.add(entrar);
        acoesSala.add(sair);
        comandos.add(acoesSala);

        JPanel acoesVoz = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        acoesVoz.add(microfoneChamada);
        acoesVoz.add(silenciarChamada);
        comandos.add(acoesVoz);

        JPanel teste = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        teste.add(testarAudio);
        teste.add(ShellUi.mutedLabel("Teste somente local - use fones de ouvido"));
        comandos.add(teste);

        direita.add(comandos, BorderLayout.SOUTH);
        return direita;
    }

    private JPanel construirBarraStatus() {
        JPanel rodape = new JPanel(new BorderLayout(8, 0));
        rodape.setBorder(BorderFactory.createEmptyBorder(2, 4, 0, 4));
        rodape.add(status, BorderLayout.CENTER);
        rodape.add(ShellUi.mutedLabel("Autor: Davyzon Bonitinho de ipatinga"), BorderLayout.EAST);
        return rodape;
    }

    private void registrarEventos() {
        criar.addActionListener(e -> {
            if (roomController != null) roomController.criarSala(nomeSala.getText(), apelido.getText());
        });
        entrar.addActionListener(e -> {
            Room selecionada = listaSalas.getSelectedValue();
            if (roomController != null && selecionada != null) {
                roomController.entrarSala(selecionada.getId(), apelido.getText());
            }
        });
        sair.addActionListener(e -> {
            if (roomController != null) roomController.sairSala();
        });
        microfoneChamada.addActionListener(e -> {
            if (voiceController != null) voiceController.alternarMicrofone();
        });
        silenciarChamada.addActionListener(e -> {
            if (voiceController != null) voiceController.alternarAudio();
        });
        testarAudio.addActionListener(e -> {
            if (audioController != null) audioController.alternarTeste();
        });
        listaSalas.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !atualizandoLista) atualizarDetalhes();
        });
    }

    @Override
    public void atualizarTesteAudio(boolean ativo) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> atualizarTesteAudio(ativo));
            return;
        }
        testarAudio.setText(ativo ? "Parar teste local" : "Testar microfone local");
    }

    @Override
    public void atualizarEstadoVoz(boolean microfoneAtivo, boolean audioSilenciado) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> atualizarEstadoVoz(microfoneAtivo, audioSilenciado));
            return;
        }
        microfoneChamada.setText(microfoneAtivo ? "Desativar microfone" : "Ativar microfone");
        silenciarChamada.setText(audioSilenciado ? "Ativar audio" : "Silenciar audio");
    }

    @Override
    public void mostrarStatus(String texto) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> mostrarStatus(texto));
            return;
        }
        status.setText(texto);
        System.out.println("[Pitiukype] " + texto);
    }

    @Override
    public void atualizarSalas(List<Room> salas, String novoAtualId, String novoPendenteId) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> atualizarSalas(salas, novoAtualId, novoPendenteId));
            return;
        }

        Room anterior = listaSalas.getSelectedValue();
        String selecionadaId = anterior == null ? null : anterior.getId();
        if (novoAtualId != null && !Objects.equals(novoAtualId, salaAtualId)) {
            selecionadaId = novoAtualId;
        }
        salaAtualId = novoAtualId;
        pendenteId = novoPendenteId;

        atualizandoLista = true;
        try {
            modeloSalas.clear();
            int indice = -1;
            for (int i = 0; i < salas.size(); i++) {
                Room sala = salas.get(i);
                modeloSalas.addElement(sala);
                if (sala.getId().equals(selecionadaId)) indice = i;
            }
            if (indice >= 0) listaSalas.setSelectedIndex(indice);
            else if (!modeloSalas.isEmpty()) listaSalas.setSelectedIndex(0);
        } finally {
            atualizandoLista = false;
        }

        chamada.setText(salaAtualId != null ? "Em uma chamada"
                : pendenteId != null ? "Aguardando entrada..." : "Fora de uma chamada");
        apelido.setEditable(salaAtualId == null && pendenteId == null);
        criar.setEnabled(salaAtualId == null && pendenteId == null);
        atualizarDetalhes();
    }

    private void atualizarDetalhes() {
        Room sala = listaSalas.getSelectedValue();
        modeloMembros.clear();
        if (sala == null) {
            tituloSala.setText("Nenhuma sala selecionada");
            descricaoSala.setText("Crie uma sala ou aguarde os anuncios da rede.");
            quantidade.setText("0 participantes");
        } else {
            tituloSala.setText(sala.getNome());
            descricaoSala.setText("Organizador: " + sala.getHostIp());
            quantidade.setText(sala.getParticipantes().size() + " participante(s)");
            for (RoomMember membro : sala.getParticipantes()) modeloMembros.addElement(membro);
        }
        atualizarBotoes();
    }

    private void atualizarBotoes() {
        Room selecionada = listaSalas.getSelectedValue();
        entrar.setEnabled(selecionada != null && salaAtualId == null && pendenteId == null);
        sair.setEnabled(salaAtualId != null || pendenteId != null);
        sair.setText(pendenteId != null ? "Cancelar pedido" : "Sair da sala");
        microfoneChamada.setEnabled(salaAtualId != null);
        silenciarChamada.setEnabled(salaAtualId != null);
        testarAudio.setEnabled(salaAtualId == null && pendenteId == null);
    }

    private final class SalaRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                       boolean selecionado, boolean foco) {
            JLabel item = (JLabel) super.getListCellRendererComponent(list, value, index, selecionado, foco);
            if (value instanceof Room sala) {
                boolean minhaSala = sala.getId().equals(salaAtualId);
                item.setText(sala.getNome() + "  (" + sala.getParticipantes().size() + ")"
                        + (minhaSala ? "  [na chamada]" : ""));
                item.setToolTipText("Organizador: " + sala.getHostIp());
            }
            item.setBorder(BorderFactory.createEmptyBorder(7, 8, 7, 8));
            return item;
        }
    }

    private final class MembroRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                       boolean selecionado, boolean foco) {
            JLabel item = (JLabel) super.getListCellRendererComponent(list, value, index, selecionado, foco);
            if (value instanceof RoomMember membro) {
                Room sala = listaSalas.getSelectedValue();
                boolean organizador = sala != null && membro.getId().equals(sala.getHostId());
                item.setText(membro.getNome() + (organizador ? " (organizador)" : ""));
                item.setToolTipText("IP: " + membro.getIp());
            }
            item.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
            return item;
        }
    }
}
