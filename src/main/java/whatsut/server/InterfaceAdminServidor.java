package whatsut.server;

import whatsut.common.*;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interface gráfica de administração do servidor WhatsUT.
 * Design: dark theme, painel com abas (Usuários, Grupos, Config, Stats, Log).
 */
public class InterfaceAdminServidor extends JFrame {

    // ── Paleta de cores ──────────────────────────────────────────────────────
    static final Color BG_DARK    = new Color(0x1A1A2E);
    static final Color BG_PANEL   = new Color(0x16213E);
    static final Color BG_CARD    = new Color(0x0F3460);
    static final Color ACCENT     = new Color(0x533483);
    static final Color ACCENT2    = new Color(0xE94560);
    static final Color TEXT_MAIN  = new Color(0xEEEEEE);
    static final Color TEXT_DIM   = new Color(0x9999AA);
    static final Color SUCCESS    = new Color(0x4CAF50);
    static final Color WARNING    = new Color(0xFF9800);
    static final Color DANGER     = new Color(0xF44336);

    private final ServicoChatImpl servico;

    // Componentes principais
    private JTabbedPane painelAbas;
    private JLabel barraStatus;
    private JTextArea areaLog;

    // Tabela de usuários
    private DefaultTableModel modeloTabelaUsuarios;
    private JTable tabelaUsuarios;

    // Tabela de grupos
    private DefaultTableModel modeloTabelaGrupos;
    private JTable tabelaGrupos;

    // Painel de estatísticas
    private JLabel lblOnline, lblTotal, lblGrupos, lblMensagens, lblTempoAtivo;

    // Campos de configuração
    private JTextField tfNomeServidor, tfMotd, tfMaxGrupo, tfTimeout;
    private JCheckBox cbTransferenciaArquivos, cbExigirAprovacao;

    private final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    public InterfaceAdminServidor(ServicoChatImpl servico) {
        this.servico = servico;
        setTitle("WhatsUT — Painel de Administração");
        setSize(1000, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setBackground(BG_DARK);

        construirUI();
        iniciarTimerAtualizacao();
        adicionarLog("Servidor iniciado. Aguardando conexões...");
    }

    // ── Construção da UI ──────────────────────────────────────────────────────

    private void construirUI() {
        // Painel raiz
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(BG_DARK);
        setContentPane(raiz);

        raiz.add(construirCabecalho(), BorderLayout.NORTH);
        raiz.add(construirAbas(), BorderLayout.CENTER);
        raiz.add(construirBarraStatus(), BorderLayout.SOUTH);
    }

    private JPanel construirCabecalho() {
        JPanel cabecalho = new JPanel(new BorderLayout());
        cabecalho.setBackground(BG_PANEL);
        cabecalho.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel titulo = new JLabel("⚡ WhatsUT Admin Panel");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titulo.setForeground(TEXT_MAIN);
        cabecalho.add(titulo, BorderLayout.WEST);

        JLabel subtitulo = new JLabel("v1.0.0 — RMI Server");
        subtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitulo.setForeground(ACCENT);
        cabecalho.add(subtitulo, BorderLayout.EAST);

        cabecalho.add(new JSeparator(), BorderLayout.SOUTH);
        return cabecalho;
    }

    private JTabbedPane construirAbas() {
        painelAbas = new JTabbedPane();
        painelAbas.setBackground(BG_DARK);
        painelAbas.setForeground(TEXT_MAIN);
        painelAbas.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        painelAbas.addTab("👥 Usuários",     construirAbaUsuarios());
        painelAbas.addTab("💬 Grupos",       construirAbaGrupos());
        painelAbas.addTab("⚙ Config",        construirAbaConfig());
        painelAbas.addTab("📊 Estatísticas", construirAbaEstatisticas());
        painelAbas.addTab("📋 Log",          construirAbaLog());

        return painelAbas;
    }

    // ── Aba de usuários ───────────────────────────────────────────────────────

    private JPanel construirAbaUsuarios() {
        JPanel painel = painelEscuro(new BorderLayout(8, 8));
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = {"Usuário", "Nome", "Status", "Admin", "Banido", "Último acesso"};
        modeloTabelaUsuarios = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tabelaUsuarios = tabelaEstilizada(modeloTabelaUsuarios);
        painel.add(new JScrollPane(tabelaUsuarios), BorderLayout.CENTER);

        // Botões de ação
        JPanel acoes = painelEscuro(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton btnBanir     = botaoAcento("🚫 Banir",        DANGER);
        JButton btnDesbanir  = botaoAcento("✅ Desbanir",      SUCCESS);
        JButton btnAdmin     = botaoAcento("⭐ Tornar Admin",  ACCENT);
        JButton btnAtualizar = botaoAcento("🔄 Atualizar",     ACCENT2);

        btnBanir.addActionListener(e -> banirUsuarioSelecionado());
        btnDesbanir.addActionListener(e -> desbanirUsuarioSelecionado());
        btnAdmin.addActionListener(e -> alternarAdminSelecionado());
        btnAtualizar.addActionListener(e -> atualizarUsuarios());

        acoes.add(btnBanir);
        acoes.add(btnDesbanir);
        acoes.add(btnAdmin);
        acoes.add(btnAtualizar);

        painel.add(acoes, BorderLayout.SOUTH);
        atualizarUsuarios();
        return painel;
    }

    private void atualizarUsuarios() {
        modeloTabelaUsuarios.setRowCount(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM HH:mm")
                .withZone(ZoneId.systemDefault());
        for (UsuarioServidor u : servico.getMapaUsuarios().values()) {
            modeloTabelaUsuarios.addRow(new Object[]{
                u.getNomeUsuario(),
                u.getNomeExibicao(),
                u.getStatus().getRotulo(),
                u.isAdminServidor() ? "✓" : "",
                u.isBanido() ? "⛔ " + u.getMotivoBan() : "",
                u.getUltimoAcesso() != null ? fmt.format(u.getUltimoAcesso()) : "-"
            });
        }
    }

    private void banirUsuarioSelecionado() {
        int linha = tabelaUsuarios.getSelectedRow();
        if (linha < 0) return;
        String nomeUsuario = (String) modeloTabelaUsuarios.getValueAt(linha, 0);
        String motivo = JOptionPane.showInputDialog(this,
                "Motivo do ban para " + nomeUsuario + ":", "Banir Usuário",
                JOptionPane.WARNING_MESSAGE);
        if (motivo == null) return;
        try {
            servico.banirUsuario("admin", nomeUsuario, motivo);
            adicionarLog("Usuário banido: " + nomeUsuario + " | Motivo: " + motivo);
            atualizarUsuarios();
        } catch (Exception ex) {
            mostrarErro(ex.getMessage());
        }
    }

    private void desbanirUsuarioSelecionado() {
        int linha = tabelaUsuarios.getSelectedRow();
        if (linha < 0) return;
        String nomeUsuario = (String) modeloTabelaUsuarios.getValueAt(linha, 0);
        try {
            servico.desbanirUsuario("admin", nomeUsuario);
            adicionarLog("Ban removido: " + nomeUsuario);
            atualizarUsuarios();
        } catch (Exception ex) { mostrarErro(ex.getMessage()); }
    }

    private void alternarAdminSelecionado() {
        int linha = tabelaUsuarios.getSelectedRow();
        if (linha < 0) return;
        String nomeUsuario = (String) modeloTabelaUsuarios.getValueAt(linha, 0);
        UsuarioServidor u = servico.getMapaUsuarios().get(nomeUsuario);
        if (u == null) return;
        u.setAdminServidor(!u.isAdminServidor());
        adicionarLog("Admin " + (u.isAdminServidor() ? "concedido" : "revogado") + ": " + nomeUsuario);
        atualizarUsuarios();
    }

    // ── Aba de grupos ─────────────────────────────────────────────────────────

    private JPanel construirAbaGrupos() {
        JPanel painel = painelEscuro(new BorderLayout(8, 8));
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = {"ID (curto)", "Nome", "Admin", "Membros", "Privado", "Política saída"};
        modeloTabelaGrupos = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tabelaGrupos = tabelaEstilizada(modeloTabelaGrupos);
        painel.add(new JScrollPane(tabelaGrupos), BorderLayout.CENTER);

        JPanel acoes = painelEscuro(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton btnDeletar   = botaoAcento("🗑 Deletar Grupo", DANGER);
        JButton btnAtualizar = botaoAcento("🔄 Atualizar",     ACCENT2);

        btnDeletar.addActionListener(e -> deletarGrupoSelecionado());
        btnAtualizar.addActionListener(e -> atualizarGrupos());

        acoes.add(btnDeletar);
        acoes.add(btnAtualizar);
        painel.add(acoes, BorderLayout.SOUTH);

        atualizarGrupos();
        return painel;
    }

    private void atualizarGrupos() {
        modeloTabelaGrupos.setRowCount(0);
        for (GrupoServidor g : servico.getMapaGrupos().values()) {
            modeloTabelaGrupos.addRow(new Object[]{
                g.getId().substring(0, 8),
                g.getNome(),
                g.getNomeUsuarioAdmin(),
                g.getNomesMembros().size(),
                g.isGrupoPrivado() ? "🔒 Sim" : "🌐 Não",
                g.getAoAdminSair().name()
            });
        }
    }

    private void deletarGrupoSelecionado() {
        int linha = tabelaGrupos.getSelectedRow();
        if (linha < 0) return;
        String idCurto = (String) modeloTabelaGrupos.getValueAt(linha, 0);
        String nomeGrupo = (String) modeloTabelaGrupos.getValueAt(linha, 1);
        int confirma = JOptionPane.showConfirmDialog(this,
                "Deletar o grupo \"" + nomeGrupo + "\"?",
                "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirma != JOptionPane.YES_OPTION) return;

        // Encontra o ID completo
        servico.getMapaGrupos().entrySet().stream()
                .filter(e -> e.getKey().startsWith(idCurto))
                .findFirst().ifPresent(e -> {
                    try {
                        servico.deletarGrupo(e.getValue().getNomeUsuarioAdmin(), e.getKey());
                        adicionarLog("Grupo deletado pelo admin: " + nomeGrupo);
                        atualizarGrupos();
                    } catch (Exception ex) { mostrarErro(ex.getMessage()); }
                });
    }

    // ── Aba de configurações ──────────────────────────────────────────────────

    private JPanel construirAbaConfig() {
        JPanel painel = painelEscuro(new GridBagLayout());
        painel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 8, 6, 8);

        ConfiguracaoServidor cfg = servico.getConfiguracao();

        // Título
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel titulo = rotulo("⚙ Configurações do Servidor", 15, Font.BOLD, ACCENT);
        painel.add(titulo, gbc);

        // Campos
        gbc.gridwidth = 1;
        int linha = 1;

        tfNomeServidor = adicionarLinhaConfig(painel, gbc, linha++, "Nome do Servidor:", cfg.getNomeServidor());
        tfMotd         = adicionarLinhaConfig(painel, gbc, linha++, "Mensagem do Dia (MOTD):", cfg.getMensagemDia());
        tfMaxGrupo     = adicionarLinhaConfig(painel, gbc, linha++, "Máx. Usuários por Grupo:",
                String.valueOf(cfg.getMaxUsuariosPorGrupo()));
        tfTimeout      = adicionarLinhaConfig(painel, gbc, linha++, "Timeout de Sessão (min):",
                String.valueOf(cfg.getTimeoutSessaoMinutos()));

        gbc.gridx = 0; gbc.gridy = linha; gbc.gridwidth = 1;
        painel.add(rotulo("Transferência de Arquivos:", 13, Font.PLAIN, TEXT_MAIN), gbc);
        gbc.gridx = 1;
        cbTransferenciaArquivos = new JCheckBox();
        cbTransferenciaArquivos.setSelected(cfg.isPermitirTransferenciaArquivos());
        cbTransferenciaArquivos.setBackground(BG_PANEL);
        painel.add(cbTransferenciaArquivos, gbc);
        linha++;

        gbc.gridx = 0; gbc.gridy = linha; gbc.gridwidth = 1;
        painel.add(rotulo("Aprovação obrigatória para grupos:", 13, Font.PLAIN, TEXT_MAIN), gbc);
        gbc.gridx = 1;
        cbExigirAprovacao = new JCheckBox();
        cbExigirAprovacao.setSelected(cfg.isExigirAprovacaoGrupos());
        cbExigirAprovacao.setBackground(BG_PANEL);
        painel.add(cbExigirAprovacao, gbc);
        linha++;

        gbc.gridx = 0; gbc.gridy = linha; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        JButton btnSalvar = botaoAcento("💾 Salvar Configurações", SUCCESS);
        btnSalvar.addActionListener(e -> salvarConfig());
        painel.add(btnSalvar, gbc);

        return painel;
    }

    private JTextField adicionarLinhaConfig(JPanel painel, GridBagConstraints gbc,
                                             int linha, String textoRotulo, String valor) {
        gbc.gridx = 0; gbc.gridy = linha; gbc.gridwidth = 1;
        painel.add(rotulo(textoRotulo, 13, Font.PLAIN, TEXT_MAIN), gbc);
        gbc.gridx = 1;
        JTextField tf = new JTextField(valor, 20);
        estilizarCampoTexto(tf);
        painel.add(tf, gbc);
        return tf;
    }

    private void salvarConfig() {
        try {
            ConfiguracaoServidor cfg = servico.getConfiguracao();
            cfg.setNomeServidor(tfNomeServidor.getText());
            cfg.setMensagemDia(tfMotd.getText());
            cfg.setMaxUsuariosPorGrupo(Integer.parseInt(tfMaxGrupo.getText()));
            cfg.setTimeoutSessaoMinutos(Integer.parseInt(tfTimeout.getText()));
            cfg.setPermitirTransferenciaArquivos(cbTransferenciaArquivos.isSelected());
            cfg.setExigirAprovacaoGrupos(cbExigirAprovacao.isSelected());
            servico.atualizarConfiguracao("admin", cfg);
            adicionarLog("Configurações salvas.");
            JOptionPane.showMessageDialog(this, "Configurações salvas com sucesso!",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) { mostrarErro(e.getMessage()); }
    }

    // ── Aba de estatísticas ───────────────────────────────────────────────────

    private JPanel construirAbaEstatisticas() {
        JPanel painel = painelEscuro(new GridLayout(3, 2, 16, 16));
        painel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        lblOnline     = cartaoEstatistica(painel, "👥 Online Agora",     "0");
        lblTotal      = cartaoEstatistica(painel, "📋 Total Usuários",   "0");
        lblGrupos     = cartaoEstatistica(painel, "💬 Grupos Ativos",    "0");
        lblMensagens  = cartaoEstatistica(painel, "✉ Mensagens Enviadas","0");
        lblTempoAtivo = cartaoEstatistica(painel, "⏱ Tempo Ativo",       "0s");
        cartaoEstatistica(painel, "🔒 Versão do Protocolo", "RMI/Java 17");

        return painel;
    }

    private JLabel cartaoEstatistica(JPanel pai, String titulo, String valorInicial) {
        JPanel cartao = new JPanel(new BorderLayout(0, 8));
        cartao.setBackground(BG_CARD);
        cartao.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT, 1, true),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));

        JLabel lTitulo = rotulo(titulo, 12, Font.BOLD, TEXT_DIM);
        JLabel lValor  = rotulo(valorInicial, 28, Font.BOLD, TEXT_MAIN);

        cartao.add(lTitulo, BorderLayout.NORTH);
        cartao.add(lValor, BorderLayout.CENTER);
        pai.add(cartao);
        return lValor;
    }

    // ── Aba de log ────────────────────────────────────────────────────────────

    private JPanel construirAbaLog() {
        JPanel painel = painelEscuro(new BorderLayout(8, 8));
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        areaLog = new JTextArea();
        areaLog.setEditable(false);
        areaLog.setBackground(new Color(0x0D1117));
        areaLog.setForeground(new Color(0x00FF88));
        areaLog.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        areaLog.setCaretColor(new Color(0x00FF88));

        JScrollPane scroll = new JScrollPane(areaLog);
        scroll.setBorder(BorderFactory.createLineBorder(ACCENT, 1));
        painel.add(scroll, BorderLayout.CENTER);

        JButton btnLimpar = botaoAcento("🗑 Limpar Log", ACCENT2);
        btnLimpar.addActionListener(e -> areaLog.setText(""));
        JPanel sul = painelEscuro(new FlowLayout(FlowLayout.RIGHT));
        sul.add(btnLimpar);
        painel.add(sul, BorderLayout.SOUTH);

        return painel;
    }

    // ── Status bar ────────────────────────────────────────────────────────────

    private JPanel construirBarraStatus() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(BG_PANEL);
        barra.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));

        barraStatus = new JLabel("🟢 Servidor ativo na porta 1099");
        barraStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        barraStatus.setForeground(SUCCESS);
        barra.add(barraStatus, BorderLayout.WEST);

        JLabel versao = rotulo("WhatsUT Server v1.0.0", 11, Font.PLAIN, TEXT_DIM);
        barra.add(versao, BorderLayout.EAST);

        return barra;
    }

    // ── Timer de atualização automática ──────────────────────────────────────

    private void iniciarTimerAtualizacao() {
        java.util.Timer timer = new java.util.Timer("servidor-ui-atualizacao", true);
        timer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    atualizarUsuarios();
                    atualizarGrupos();
                    atualizarEstatisticas();
                });
            }
        }, 3000, 3000);
    }

    private void atualizarEstatisticas() {
        ConcurrentHashMap<String, UsuarioServidor> mapaUsuarios = servico.getMapaUsuarios();
        long online = mapaUsuarios.values().stream().filter(UsuarioServidor::estaOnline).count();
        long total  = mapaUsuarios.values().stream().filter(u -> !u.isBanido()).count();
        long grupos = servico.getMapaGrupos().size();

        lblOnline.setText(String.valueOf(online));
        lblTotal.setText(String.valueOf(total));
        lblGrupos.setText(String.valueOf(grupos));

        try {
            EstatisticasServidor stats = servico.obterEstatisticas("admin");
            if (stats != null) {
                lblMensagens.setText(String.valueOf(stats.getMensagensEntregues()));
                if (stats.getInicioServidor() != null) {
                    long segundos = java.time.Duration.between(
                            stats.getInicioServidor(), java.time.Instant.now()).getSeconds();
                    lblTempoAtivo.setText(formatarTempoAtivo(segundos));
                }
            }
        } catch (Exception e) { /* ignorar */ }
    }

    private String formatarTempoAtivo(long segundos) {
        long h = segundos / 3600, m = (segundos % 3600) / 60, s = segundos % 60;
        return String.format("%dh %02dm %02ds", h, m, s);
    }

    // ── Utilitários de UI ─────────────────────────────────────────────────────

    public void adicionarLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            String hora = FMT.format(java.time.Instant.now());
            areaLog.append("[" + hora + "] " + msg + "\n");
            areaLog.setCaretPosition(areaLog.getDocument().getLength());
        });
    }

    private JPanel painelEscuro(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(BG_PANEL);
        return p;
    }

    private JTable tabelaEstilizada(DefaultTableModel modelo) {
        JTable tabela = new JTable(modelo);
        tabela.setBackground(BG_CARD);
        tabela.setForeground(TEXT_MAIN);
        tabela.setGridColor(new Color(0x2A2A4A));
        tabela.setRowHeight(28);
        tabela.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabela.getTableHeader().setBackground(BG_PANEL);
        tabela.getTableHeader().setForeground(TEXT_DIM);
        tabela.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabela.setSelectionBackground(ACCENT);
        tabela.setSelectionForeground(Color.WHITE);
        return tabela;
    }

    private JButton botaoAcento(String texto, Color cor) {
        JButton btn = new JButton(texto);
        btn.setBackground(cor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        return btn;
    }

    private JLabel rotulo(String texto, int tamanho, int estilo, Color cor) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("Segoe UI", estilo, tamanho));
        l.setForeground(cor);
        return l;
    }

    private void estilizarCampoTexto(JTextField tf) {
        tf.setBackground(BG_CARD);
        tf.setForeground(TEXT_MAIN);
        tf.setCaretColor(TEXT_MAIN);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
    }

    private void mostrarErro(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    // Necessário para o status bar ficar correto
    @Override
    public void setVisible(boolean visivel) {
        super.setVisible(visivel);
        if (visivel) atualizarEstatisticas();
    }
}
