package whatsut.client.ui;

import whatsut.client.SessaoChat;
import whatsut.common.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;

/**
 * Tela principal de chat do WhatsUT.
 *
 * Layout: sidebar esquerda (usuários + grupos) | área de mensagens | painel de info direito.
 */
public class PainelChat extends JPanel {

    // ── Paleta ────────────────────────────────────────────────────────────────
    static final Color BG          = new Color(0x111827);
    static final Color SIDEBAR_BG  = new Color(0x1F2937);
    static final Color CARD        = new Color(0x374151);
    static final Color ACCENT      = new Color(0x2BA2D6);
    static final Color ACCENT2     = new Color(0xEC4899);
    static final Color BUBBLE_ME   = new Color(0x6D28D9);
    static final Color BUBBLE_OTHER= new Color(0x374151);
    static final Color BUBBLE_SYS  = new Color(0x1F2937);
    static final Color TEXT        = new Color(0xF9FAFB);
    static final Color TEXT2       = new Color(0x9CA3AF);
    static final Color ONLINE      = new Color(0x10B981);
    static final Color AUSENTE     = new Color(0xF59E0B);
    static final Color OFFLINE     = new Color(0x6B7280);

    private final SessaoChat sessao;
    private final Runnable aoSair;

    // Estado do chat ativo
    private String idChatAtivo;   // nomeUsuario ou idGrupo
    private boolean chatAtivoEhGrupo;

    // Componentes
    private JPanel areaMensagens;
    private JScrollPane scrollMensagens;
    private JTextField tfMensagem;
    private JLabel lblTituloChat, lblSubtituloChat;
    private JPanel painelListaUsuarios, painelListaGrupos;
    private JButton btnEnviarArquivo;

    // Cache local
    private final Map<String, UsuarioDTO> usuariosConhecidos = new LinkedHashMap<>();
    private final Map<String, GrupoDTO>   gruposConhecidos   = new LinkedHashMap<>();
    // Notificações não lidas
    private final Map<String, Integer>    naoLidas           = new HashMap<>();

    private static final DateTimeFormatter FMT_HORA =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    public PainelChat(SessaoChat sessao, Runnable aoSair) {
        this.sessao = sessao;
        this.aoSair = aoSair;
        setLayout(new BorderLayout());
        setBackground(BG);
        construirUI();
        registrarRetornosChamada();
        carregarDadosIniciais();
    }

    // ── Construção da UI ──────────────────────────────────────────────────────

    private void construirUI() {
        add(construirSidebar(), BorderLayout.WEST);
        add(construirAreaChat(), BorderLayout.CENTER);
    }

    // SIDEBAR
    private JPanel construirSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(260, 0));

        // Cabeçalho do sidebar
        JPanel cabecalho = new JPanel(new BorderLayout());
        cabecalho.setBackground(new Color(0x111827));
        cabecalho.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel quemSouEu = new JLabel("💬 " + sessao.getUsuarioAtual().getNomeExibicao());
        quemSouEu.setFont(new Font("Segoe UI", Font.BOLD, 14));
        quemSouEu.setForeground(TEXT);
        cabecalho.add(quemSouEu, BorderLayout.CENTER);

        JButton btnSair = botaoMini("Sair", ACCENT2);
        btnSair.addActionListener(e -> {
            sessao.sair();
            aoSair.run();
        });
        cabecalho.add(btnSair, BorderLayout.EAST);
        sidebar.add(cabecalho, BorderLayout.NORTH);

        // Abas: Usuários | Grupos
        JTabbedPane abas = new JTabbedPane();
        abas.setBackground(SIDEBAR_BG);
        abas.setForeground(TEXT2);
        abas.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        painelListaUsuarios = new JPanel();
        painelListaUsuarios.setLayout(new BoxLayout(painelListaUsuarios, BoxLayout.Y_AXIS));
        painelListaUsuarios.setBackground(SIDEBAR_BG);
        JScrollPane scUs = new JScrollPane(painelListaUsuarios);
        scUs.setBorder(null);
        scUs.setBackground(SIDEBAR_BG);

        painelListaGrupos = new JPanel();
        painelListaGrupos.setLayout(new BoxLayout(painelListaGrupos, BoxLayout.Y_AXIS));
        painelListaGrupos.setBackground(SIDEBAR_BG);
        JScrollPane scGr = new JScrollPane(painelListaGrupos);
        scGr.setBorder(null);
        scGr.setBackground(SIDEBAR_BG);

        abas.addTab("👥 Usuários", scUs);
        abas.addTab("💬 Grupos",   scGr);

        sidebar.add(abas, BorderLayout.CENTER);

        // Botão "Novo Grupo"
        JPanel rodapeSidebar = new JPanel(new FlowLayout(FlowLayout.CENTER));
        rodapeSidebar.setBackground(SIDEBAR_BG);
        JButton btnNovoGrupo = botaoMini("＋ Novo Grupo", ACCENT);
        btnNovoGrupo.addActionListener(e -> mostrarDialogoCriarGrupo());
        rodapeSidebar.add(btnNovoGrupo);
        sidebar.add(rodapeSidebar, BorderLayout.SOUTH);

        return sidebar;
    }

    // ÁREA DE CHAT
    private JPanel construirAreaChat() {
        JPanel area = new JPanel(new BorderLayout());
        area.setBackground(BG);

        // Cabeçalho do chat
        JPanel cabecalhoChat = new JPanel(new BorderLayout());
        cabecalhoChat.setBackground(SIDEBAR_BG);
        cabecalhoChat.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        JPanel infoChat = new JPanel();
        infoChat.setLayout(new BoxLayout(infoChat, BoxLayout.Y_AXIS));
        infoChat.setOpaque(false);
        lblTituloChat    = rotulo("Selecione uma conversa", 15, Font.BOLD, TEXT);
        lblSubtituloChat = rotulo("", 12, Font.PLAIN, TEXT2);
        infoChat.add(lblTituloChat);
        infoChat.add(lblSubtituloChat);
        cabecalhoChat.add(infoChat, BorderLayout.CENTER);

        // Botões do cabeçalho (visíveis ao selecionar chat de grupo)
        JPanel botoesCabecalho = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botoesCabecalho.setOpaque(false);
        JButton btnMembros = botaoMini("Membros", ACCENT);
        btnMembros.addActionListener(e -> mostrarMembrosGrupo());
        botoesCabecalho.add(btnMembros);
        cabecalhoChat.add(botoesCabecalho, BorderLayout.EAST);

        area.add(cabecalhoChat, BorderLayout.NORTH);

        // Área de mensagens
        areaMensagens = new JPanel();
        areaMensagens.setLayout(new BoxLayout(areaMensagens, BoxLayout.Y_AXIS));
        areaMensagens.setBackground(BG);
        areaMensagens.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        scrollMensagens = new JScrollPane(areaMensagens);
        scrollMensagens.setBorder(null);
        scrollMensagens.setBackground(BG);
        scrollMensagens.getVerticalScrollBar().setUnitIncrement(16);
        area.add(scrollMensagens, BorderLayout.CENTER);

        // Input de mensagem
        area.add(construirBarraEntrada(), BorderLayout.SOUTH);

        return area;
    }

    private JPanel construirBarraEntrada() {
        JPanel barra = new JPanel(new BorderLayout(8, 0));
        barra.setBackground(SIDEBAR_BG);
        barra.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        tfMensagem = new JTextField();
        tfMensagem.setBackground(CARD);
        tfMensagem.setForeground(TEXT);
        tfMensagem.setCaretColor(TEXT);
        tfMensagem.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tfMensagem.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        tfMensagem.addActionListener(e -> enviarMensagem());

        JButton btnEnviar = botaoArredondado("Enviar ➤", ACCENT);
        btnEnviar.addActionListener(e -> enviarMensagem());

        btnEnviarArquivo = botaoMini("📎", new Color(0x374151));
        btnEnviarArquivo.addActionListener(e -> enviarArquivo());
        btnEnviarArquivo.setEnabled(false);

        barra.add(btnEnviarArquivo, BorderLayout.WEST);
        barra.add(tfMensagem, BorderLayout.CENTER);
        barra.add(btnEnviar, BorderLayout.EAST);

        return barra;
    }

    // ── Callbacks RMI ────────────────────────────────────────────────────────

    private void registrarRetornosChamada() {
        var cb = sessao.getRetornoChamada();

        cb.setAoReceberMensagem(msg -> SwingUtilities.invokeLater(() -> {
            String chave = msg.isEhGrupo() ? msg.getIdDestino() : msg.getNomeRemetente();
            if (chave.equals(idChatAtivo)) {
                adicionarMensagem(msg);
            } else {
                // Incrementa não lidos e atualiza sidebar
                naoLidas.merge(chave, 1, Integer::sum);
                atualizarSidebars();
            }
        }));

        cb.setAoReceberArquivo(msg -> SwingUtilities.invokeLater(() -> {
            if (msg.getNomeRemetente().equals(idChatAtivo) || idChatAtivo == null) {
                adicionarMensagem(msg);
                // Oferecer salvar
                oferecerSalvarArquivo(msg);
            } else {
                naoLidas.merge(msg.getNomeRemetente(), 1, Integer::sum);
                atualizarSidebars();
                JOptionPane.showMessageDialog(this,
                        msg.getNomeExibicaoRemetente() + " enviou um arquivo: " + msg.getNomeArquivo(),
                        "Arquivo recebido", JOptionPane.INFORMATION_MESSAGE);
            }
        }));

        cb.setAoAtualizarListaUsuarios(usuarios -> SwingUtilities.invokeLater(() -> {
            usuariosConhecidos.clear();
            for (UsuarioDTO u : usuarios) {
                if (!u.getNomeUsuario().equals(sessao.getUsuarioAtual().getNomeUsuario()))
                    usuariosConhecidos.put(u.getNomeUsuario(), u);
            }
            atualizarListaUsuarios();
        }));

        cb.setAoMudarStatusUsuario((nomeUsuario, status) -> SwingUtilities.invokeLater(() -> {
            UsuarioDTO u = usuariosConhecidos.get(nomeUsuario);
            if (u != null) { u.setStatus(status); atualizarListaUsuarios(); }
        }));

        cb.setAoEntrarNoGrupo(grupo -> SwingUtilities.invokeLater(() -> {
            gruposConhecidos.put(grupo.getId(), grupo);
            atualizarListaGrupos();
            mostrarInfo("Você entrou no grupo: " + grupo.getNome());
        }));

        cb.setAoSairDoGrupo(arr -> SwingUtilities.invokeLater(() -> {
            String idGrupo = arr[0], motivo = arr[1];
            gruposConhecidos.remove(idGrupo);
            if (idGrupo.equals(idChatAtivo)) {
                idChatAtivo = null;
                lblTituloChat.setText("Selecione uma conversa");
                limparMensagens();
            }
            atualizarListaGrupos();
            mostrarInfo("Removido do grupo: " + motivo);
        }));

        cb.setAoAtualizarGrupo(grupo -> SwingUtilities.invokeLater(() -> {
            gruposConhecidos.put(grupo.getId(), grupo);
            if (grupo.getId().equals(idChatAtivo)) {
                lblSubtituloChat.setText(grupo.getQuantidadeMembros() + " membros");
            }
            atualizarListaGrupos();
        }));

        cb.setAoReceberSolicitacao((idGrupo, nomeGrupo, nomeUsuario, exibicao) ->
            SwingUtilities.invokeLater(() -> {
                int opt = JOptionPane.showConfirmDialog(this,
                        exibicao + " (" + nomeUsuario + ") quer entrar em \"" + nomeGrupo + "\".\nAprovar?",
                        "Solicitação de Entrada", JOptionPane.YES_NO_OPTION);
                try {
                    if (opt == JOptionPane.YES_OPTION)
                        sessao.getServico().aprovarSolicitacao(
                                sessao.getUsuarioAtual().getNomeUsuario(), idGrupo, nomeUsuario);
                    else
                        sessao.getServico().negarSolicitacao(
                                sessao.getUsuarioAtual().getNomeUsuario(), idGrupo, nomeUsuario);
                } catch (Exception e) { mostrarErro(e.getMessage()); }
            }));

        cb.setAoAprovarSolicitacao(grupo -> SwingUtilities.invokeLater(() -> {
            gruposConhecidos.put(grupo.getId(), grupo);
            atualizarListaGrupos();
            mostrarInfo("Sua entrada no grupo \"" + grupo.getNome() + "\" foi aprovada!");
        }));

        cb.setAoNegarSolicitacao(arr -> SwingUtilities.invokeLater(() ->
                mostrarInfo("Sua entrada no grupo \"" + arr[1] + "\" foi negada.")));

        cb.setAoSerBanidoDoServidor(motivo -> SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    "Você foi banido do servidor.\nMotivo: " + motivo,
                    "Banido", JOptionPane.ERROR_MESSAGE);
            sessao.sair();
            aoSair.run();
        }));
    }

    // ── Dados iniciais ────────────────────────────────────────────────────────

    private void carregarDadosIniciais() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    List<UsuarioDTO> usuarios = sessao.getServico().obterUsuarios(
                            sessao.getUsuarioAtual().getNomeUsuario());
                    List<GrupoDTO> grupos = sessao.getServico().obterGrupos(
                            sessao.getUsuarioAtual().getNomeUsuario());
                    SwingUtilities.invokeLater(() -> {
                        for (UsuarioDTO u : usuarios) {
                            if (!u.getNomeUsuario().equals(sessao.getUsuarioAtual().getNomeUsuario()))
                                usuariosConhecidos.put(u.getNomeUsuario(), u);
                        }
                        for (GrupoDTO g : grupos) {
                            if (g.getNomesMembros().contains(
                                    sessao.getUsuarioAtual().getNomeUsuario()))
                                gruposConhecidos.put(g.getId(), g);
                        }
                        atualizarListaUsuarios();
                        atualizarListaGrupos();
                    });
                } catch (Exception e) { /* ignorar */ }
                return null;
            }
        }.execute();
    }

    // ── Refresh de sidebars ───────────────────────────────────────────────────

    private void atualizarListaUsuarios() {
        painelListaUsuarios.removeAll();
        for (UsuarioDTO u : usuariosConhecidos.values()) {
            painelListaUsuarios.add(construirItemUsuario(u));
        }
        painelListaUsuarios.revalidate();
        painelListaUsuarios.repaint();
    }

    private void atualizarListaGrupos() {
        painelListaGrupos.removeAll();

        // Grupos do usuário atual
        for (GrupoDTO g : gruposConhecidos.values()) {
            painelListaGrupos.add(construirItemGrupo(g, true));
        }

        // Separador "Outros grupos"
        JLabel sep = rotulo("  Outros grupos disponíveis", 11, Font.BOLD, TEXT2);
        sep.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 0));
        painelListaGrupos.add(sep);

        // Busca grupos não participados
        new SwingWorker<List<GrupoDTO>, Void>() {
            @Override protected List<GrupoDTO> doInBackground() {
                try { return sessao.getServico().obterGrupos(
                        sessao.getUsuarioAtual().getNomeUsuario()); }
                catch (Exception e) { return Collections.emptyList(); }
            }
            @Override protected void done() {
                try {
                    for (GrupoDTO g : get()) {
                        if (!gruposConhecidos.containsKey(g.getId())) {
                            painelListaGrupos.add(construirItemGrupo(g, false));
                        }
                    }
                    painelListaGrupos.revalidate();
                    painelListaGrupos.repaint();
                } catch (Exception e) { /* ignorar */ }
            }
        }.execute();

        painelListaGrupos.revalidate();
        painelListaGrupos.repaint();
    }

    private void atualizarSidebars() {
        atualizarListaUsuarios();
        atualizarListaGrupos();
    }

    // ── Itens da sidebar ──────────────────────────────────────────────────────

    private JPanel construirItemUsuario(UsuarioDTO u) {
        JPanel item = new JPanel(new BorderLayout(8, 0));
        item.setBackground(idChatAtivo != null && idChatAtivo.equals(u.getNomeUsuario())
                ? new Color(0x2D3748) : SIDEBAR_BG);
        item.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        Color cor = switch (u.getStatus()) {
            case ONLINE  -> ONLINE;
            case AUSENTE -> AUSENTE;
            case OCUPADO -> new Color(0xEF4444);
            default      -> OFFLINE;
        };

        JLabel avatar = new JLabel("●");
        avatar.setForeground(cor);
        avatar.setFont(new Font("Segoe UI", Font.PLAIN, 10));

        JPanel areaNome = new JPanel();
        areaNome.setLayout(new BoxLayout(areaNome, BoxLayout.Y_AXIS));
        areaNome.setOpaque(false);
        areaNome.add(rotulo(u.getNomeExibicao(), 13, Font.BOLD, TEXT));
        areaNome.add(rotulo(u.getStatus().getRotulo(), 11, Font.PLAIN, TEXT2));

        item.add(avatar, BorderLayout.WEST);
        item.add(areaNome, BorderLayout.CENTER);

        int badge = naoLidas.getOrDefault(u.getNomeUsuario(), 0);
        if (badge > 0) {
            JLabel lBadge = new JLabel(String.valueOf(badge));
            lBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lBadge.setForeground(Color.WHITE);
            lBadge.setBackground(ACCENT2);
            lBadge.setOpaque(true);
            lBadge.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
            item.add(lBadge, BorderLayout.EAST);
        }

        item.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                abrirChatPrivado(u);
            }
            @Override public void mouseEntered(MouseEvent e) {
                item.setBackground(new Color(0x2D3748));
            }
            @Override public void mouseExited(MouseEvent e) {
                item.setBackground(idChatAtivo != null
                        && idChatAtivo.equals(u.getNomeUsuario())
                        ? new Color(0x2D3748) : SIDEBAR_BG);
            }
        });

        return item;
    }

    private JPanel construirItemGrupo(GrupoDTO g, boolean ehMembro) {
        JPanel item = new JPanel(new BorderLayout(8, 0));
        item.setBackground(idChatAtivo != null && idChatAtivo.equals(g.getId())
                ? new Color(0x2D3748) : SIDEBAR_BG);
        item.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        JLabel icone = rotulo(g.isGrupoPrivado() ? "🔒" : "💬", 16, Font.PLAIN, TEXT);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        info.add(rotulo(g.getNome(), 13, Font.BOLD, TEXT));
        info.add(rotulo(g.getQuantidadeMembros() + " membros" + (ehMembro ? "" : " · Entrar"),
                11, Font.PLAIN, TEXT2));

        item.add(icone, BorderLayout.WEST);
        item.add(info, BorderLayout.CENTER);

        int badge = naoLidas.getOrDefault(g.getId(), 0);
        if (badge > 0) {
            JLabel lBadge = new JLabel(String.valueOf(badge));
            lBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lBadge.setForeground(Color.WHITE);
            lBadge.setBackground(ACCENT);
            lBadge.setOpaque(true);
            lBadge.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
            item.add(lBadge, BorderLayout.EAST);
        }

        item.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (ehMembro) abrirChatGrupo(g);
                else solicitarEntradaGrupo(g);
            }
            @Override public void mouseEntered(MouseEvent e) { item.setBackground(new Color(0x2D3748)); }
            @Override public void mouseExited(MouseEvent e) {
                item.setBackground(idChatAtivo != null && idChatAtivo.equals(g.getId())
                        ? new Color(0x2D3748) : SIDEBAR_BG);
            }
        });

        return item;
    }

    // ── Abertura de chat ──────────────────────────────────────────────────────

    private void abrirChatPrivado(UsuarioDTO u) {
        idChatAtivo      = u.getNomeUsuario();
        chatAtivoEhGrupo = false;
        naoLidas.remove(u.getNomeUsuario());
        lblTituloChat.setText(u.getNomeExibicao());
        lblSubtituloChat.setText(u.getStatus().getRotulo());
        btnEnviarArquivo.setEnabled(true);
        limparMensagens();
        atualizarSidebars();

        // Carrega histórico
        new SwingWorker<List<MensagemDTO>, Void>() {
            @Override protected List<MensagemDTO> doInBackground() {
                try { return sessao.getServico().obterHistoricoPrivado(
                        sessao.getUsuarioAtual().getNomeUsuario(), u.getNomeUsuario(), 50); }
                catch (Exception e) { return Collections.emptyList(); }
            }
            @Override protected void done() {
                try { for (MensagemDTO m : get()) adicionarMensagem(m); }
                catch (Exception e) { /* ignorar */ }
            }
        }.execute();
    }

    private void abrirChatGrupo(GrupoDTO g) {
        idChatAtivo      = g.getId();
        chatAtivoEhGrupo = true;
        naoLidas.remove(g.getId());
        lblTituloChat.setText(g.getNome());
        lblSubtituloChat.setText(g.getQuantidadeMembros() + " membros");
        btnEnviarArquivo.setEnabled(false);
        limparMensagens();
        atualizarSidebars();

        new SwingWorker<List<MensagemDTO>, Void>() {
            @Override protected List<MensagemDTO> doInBackground() {
                try { return sessao.getServico().obterHistoricoGrupo(g.getId(), 50); }
                catch (Exception e) { return Collections.emptyList(); }
            }
            @Override protected void done() {
                try { for (MensagemDTO m : get()) adicionarMensagem(m); }
                catch (Exception e) { /* ignorar */ }
            }
        }.execute();
    }

    // ── Envio de mensagens ────────────────────────────────────────────────────

    private void enviarMensagem() {
        if (idChatAtivo == null) return;
        String texto = tfMensagem.getText().trim();
        if (texto.isEmpty()) return;
        tfMensagem.setText("");

        String meuUsuario  = sessao.getUsuarioAtual().getNomeUsuario();
        String meuExibicao = sessao.getUsuarioAtual().getNomeExibicao();
        MensagemDTO msg = MensagemDTO.texto(meuUsuario, meuExibicao, idChatAtivo, chatAtivoEhGrupo, texto);

        adicionarMensagem(msg); // otimismo: mostra imediatamente

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                try {
                    if (chatAtivoEhGrupo)
                        sessao.getServico().enviarMensagemGrupo(meuUsuario, idChatAtivo, msg);
                    else
                        sessao.getServico().enviarMensagemPrivada(meuUsuario, idChatAtivo, msg);
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> mostrarErro(e.getMessage()));
                }
                return null;
            }
        }.execute();
    }

    private void enviarArquivo() {
        if (idChatAtivo == null || chatAtivoEhGrupo) return;
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File f = fc.getSelectedFile();
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                try {
                    byte[] dados = Files.readAllBytes(f.toPath());
                    MensagemDTO msg = MensagemDTO.arquivo(
                            sessao.getUsuarioAtual().getNomeUsuario(),
                            sessao.getUsuarioAtual().getNomeExibicao(),
                            idChatAtivo, f.getName(), dados);
                    sessao.getServico().enviarArquivo(
                            sessao.getUsuarioAtual().getNomeUsuario(), idChatAtivo, msg);
                    SwingUtilities.invokeLater(() -> adicionarMensagem(msg));
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> mostrarErro(e.getMessage()));
                }
                return null;
            }
        }.execute();
    }

    // ── Renderização de mensagens ─────────────────────────────────────────────

    private void adicionarMensagem(MensagemDTO msg) {
        boolean ehEu = msg.getNomeRemetente().equals(
                sessao.getUsuarioAtual().getNomeUsuario());
        boolean ehSis = msg.getTipo() == MensagemDTO.Tipo.SISTEMA;

        JPanel balao = construirBalao(msg, ehEu, ehSis);
        areaMensagens.add(balao);
        areaMensagens.add(Box.createVerticalStrut(4));
        areaMensagens.revalidate();
        areaMensagens.repaint();

        // Scroll para baixo
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scrollMensagens.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    private JPanel construirBalao(MensagemDTO msg, boolean ehEu, boolean ehSis) {
        JPanel linha = new JPanel(new FlowLayout(
                ehSis ? FlowLayout.CENTER : (ehEu ? FlowLayout.RIGHT : FlowLayout.LEFT),
                0, 0));
        linha.setOpaque(false);
        linha.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        Color corFundo = ehSis ? BUBBLE_SYS : (ehEu ? BUBBLE_ME : BUBBLE_OTHER);

        JPanel balao = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(corFundo);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        balao.setLayout(new BoxLayout(balao, BoxLayout.Y_AXIS));
        balao.setOpaque(false);
        balao.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        int maxW = (int)(getWidth() * 0.65);
        balao.setMaximumSize(new Dimension(Math.max(maxW, 200), Integer.MAX_VALUE));

        // Nome do remetente (em chats de grupo)
        if (!ehEu && !ehSis && chatAtivoEhGrupo) {
            JLabel remetente = rotulo(msg.getNomeExibicaoRemetente(), 11, Font.BOLD, ACCENT);
            balao.add(remetente);
        }

        // Conteúdo
        String conteudo = msg.getConteudo();
        JTextArea ta = new JTextArea(conteudo);
        ta.setEditable(false);
        ta.setOpaque(false);
        ta.setForeground(ehSis ? TEXT2 : TEXT);
        ta.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        ta.setWrapStyleWord(true);
        ta.setLineWrap(true);
        ta.setMaximumSize(new Dimension(Math.max(maxW - 24, 180), Integer.MAX_VALUE));
        balao.add(ta);

        // Botão de download para arquivos
        if (msg.getTipo() == MensagemDTO.Tipo.ARQUIVO && msg.getDadosArquivo() != null) {
            JButton btnDl = botaoMini("⬇ Salvar arquivo", ACCENT);
            btnDl.addActionListener(e -> dialogoSalvarArquivo(msg));
            balao.add(Box.createVerticalStrut(4));
            balao.add(btnDl);
        }

        // Timestamp
        if (msg.getMomento() != null) {
            JLabel ts = rotulo(FMT_HORA.format(msg.getMomento()), 10, Font.PLAIN, TEXT2);
            ts.setAlignmentX(ehEu ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
            balao.add(ts);
        }

        linha.add(balao);
        return linha;
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    private void mostrarDialogoCriarGrupo() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Criar Novo Grupo", true);
        dlg.setSize(380, 360);
        dlg.setLocationRelativeTo(this);

        JPanel painel = new JPanel();
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));
        painel.setBackground(new Color(0x1F2937));
        painel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JTextField tfNome = campo("Nome do grupo");
        JTextField tfDesc = campo("Descrição (opcional)");
        JCheckBox cbPriv  = new JCheckBox("Exigir aprovação para entrar");
        cbPriv.setForeground(TEXT);
        cbPriv.setBackground(new Color(0x1F2937));

        JComboBox<GrupoDTO.AoAdminSair> cbPolitica = new JComboBox<>(GrupoDTO.AoAdminSair.values());

        painel.add(rotulo("Nome *", 13, Font.BOLD, TEXT2));
        painel.add(tfNome);
        painel.add(Box.createVerticalStrut(10));
        painel.add(rotulo("Descrição", 13, Font.BOLD, TEXT2));
        painel.add(tfDesc);
        painel.add(Box.createVerticalStrut(10));
        painel.add(cbPriv);
        painel.add(Box.createVerticalStrut(8));
        painel.add(rotulo("Política quando admin sai:", 12, Font.PLAIN, TEXT2));
        painel.add(cbPolitica);
        painel.add(Box.createVerticalStrut(16));

        JButton btnCriar = botaoArredondado("Criar Grupo", ACCENT);
        btnCriar.addActionListener(e -> {
            String nome = tfNome.getText().trim();
            if (nome.isEmpty()) return;
            try {
                GrupoDTO g = sessao.getServico().criarGrupo(
                        sessao.getUsuarioAtual().getNomeUsuario(),
                        nome, tfDesc.getText().trim(),
                        cbPriv.isSelected(),
                        (GrupoDTO.AoAdminSair) cbPolitica.getSelectedItem());
                gruposConhecidos.put(g.getId(), g);
                atualizarListaGrupos();
                dlg.dispose();
                abrirChatGrupo(g);
            } catch (Exception ex) { mostrarErro(ex.getMessage()); }
        });

        painel.add(btnCriar);
        dlg.setContentPane(painel);
        dlg.setVisible(true);
    }

    private void solicitarEntradaGrupo(GrupoDTO g) {
        int opt = JOptionPane.showConfirmDialog(this,
                "Solicitar entrada no grupo \"" + g.getNome() + "\"?",
                "Entrar no Grupo", JOptionPane.YES_NO_OPTION);
        if (opt != JOptionPane.YES_OPTION) return;
        try {
            sessao.getServico().solicitarEntradaGrupo(
                    sessao.getUsuarioAtual().getNomeUsuario(), g.getId());
            if (!g.isGrupoPrivado()) {
                // Entrada imediata
                gruposConhecidos.put(g.getId(), g);
                atualizarListaGrupos();
                abrirChatGrupo(g);
            } else {
                mostrarInfo("Solicitação enviada ao administrador do grupo.");
            }
        } catch (Exception ex) { mostrarErro(ex.getMessage()); }
    }

    private void mostrarMembrosGrupo() {
        if (idChatAtivo == null || !chatAtivoEhGrupo) return;
        GrupoDTO g = gruposConhecidos.get(idChatAtivo);
        if (g == null) return;

        boolean ehAdmin = g.getNomeUsuarioAdmin().equals(sessao.getUsuarioAtual().getNomeUsuario());
        JDialog dlg = new JDialog((Frame)SwingUtilities.getWindowAncestor(this),
                "Membros: " + g.getNome(), true);
        dlg.setSize(320, 400);
        dlg.setLocationRelativeTo(this);

        JPanel painel = new JPanel(new BorderLayout(0, 8));
        painel.setBackground(new Color(0x1F2937));
        painel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        DefaultListModel<String> modelo = new DefaultListModel<>();
        for (String m : g.getNomesMembros()) {
            UsuarioDTO u = usuariosConhecidos.get(m);
            String texto = (u != null ? u.getNomeExibicao() : m)
                    + (m.equals(g.getNomeUsuarioAdmin()) ? " 👑" : "");
            modelo.addElement(texto);
        }
        JList<String> lista = new JList<>(modelo);
        lista.setBackground(CARD);
        lista.setForeground(TEXT);
        lista.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        painel.add(new JScrollPane(lista), BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout());
        btns.setOpaque(false);

        if (ehAdmin) {
            JButton btnExpulsar = botaoMini("🚫 Remover", new Color(0xEF4444));
            btnExpulsar.addActionListener(e -> {
                int idx = lista.getSelectedIndex();
                if (idx < 0) return;
                String membroRaw = g.getNomesMembros().get(idx);
                if (membroRaw.equals(sessao.getUsuarioAtual().getNomeUsuario())) return;
                try {
                    sessao.getServico().banirDoGrupo(
                            sessao.getUsuarioAtual().getNomeUsuario(), g.getId(), membroRaw);
                    dlg.dispose();
                } catch (Exception ex) { mostrarErro(ex.getMessage()); }
            });
            btns.add(btnExpulsar);
        }

        JButton btnSair = botaoMini("🚪 Sair do Grupo", ACCENT2);
        btnSair.addActionListener(e -> {
            int c = JOptionPane.showConfirmDialog(dlg, "Sair do grupo?", "Confirmar",
                    JOptionPane.YES_NO_OPTION);
            if (c != JOptionPane.YES_OPTION) return;
            try {
                sessao.getServico().sairDoGrupo(
                        sessao.getUsuarioAtual().getNomeUsuario(), g.getId());
                dlg.dispose();
            } catch (Exception ex) { mostrarErro(ex.getMessage()); }
        });
        btns.add(btnSair);

        painel.add(btns, BorderLayout.SOUTH);
        dlg.setContentPane(painel);
        dlg.setVisible(true);
    }

    private void oferecerSalvarArquivo(MensagemDTO msg) {
        int opt = JOptionPane.showConfirmDialog(this,
                "Arquivo recebido: " + msg.getNomeArquivo() + "\nDeseja salvar?",
                "Arquivo Recebido", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) dialogoSalvarArquivo(msg);
    }

    private void dialogoSalvarArquivo(MensagemDTO msg) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(msg.getNomeArquivo()));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.write(fc.getSelectedFile().toPath(), msg.getDadosArquivo());
                mostrarInfo("Arquivo salvo com sucesso!");
            } catch (IOException e) { mostrarErro(e.getMessage()); }
        }
    }

    // ── Utilitários ───────────────────────────────────────────────────────────

    private void limparMensagens() {
        areaMensagens.removeAll();
        areaMensagens.revalidate();
        areaMensagens.repaint();
    }

    private void mostrarInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarErro(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    private JLabel rotulo(String t, int tamanho, int estilo, Color c) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", estilo, tamanho));
        l.setForeground(c);
        return l;
    }

    private JButton botaoMini(String t, Color bg) {
        JButton b = new JButton(t);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 11));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        return b;
    }

    private JButton botaoArredondado(String t, Color bg) {
        JButton b = new JButton(t);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        return b;
    }

    private JTextField campo(String placeholder) {
        JTextField tf = new JTextField(placeholder);
        tf.setBackground(new Color(0x374151));
        tf.setForeground(TEXT);
        tf.setCaretColor(TEXT);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT, 1),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        return tf;
    }
}
