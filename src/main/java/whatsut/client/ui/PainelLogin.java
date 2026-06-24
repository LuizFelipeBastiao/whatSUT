package whatsut.client.ui;

import whatsut.client.SessaoChat;
import whatsut.common.UsuarioDTO;

import javax.swing.*;
import java.awt.*;

/**
 * Tela de login/cadastro do WhatsUT — design moderno com gradiente.
 */
public class PainelLogin extends JPanel {

    // ── Cores (herdadas do tema do servidor para consistência) ────────────────
    static final Color BG_A    = new Color(0x1A1A2E);
    static final Color BG_B    = new Color(0x16213E);
    static final Color CARD    = new Color(0x0F3460);
    static final Color ACCENT  = new Color(0x533483);
    static final Color ACCENT2 = new Color(0xE94560);
    static final Color TEXT    = new Color(0xEEEEEE);
    static final Color TEXT2   = new Color(0x9999AA);
    static final Color SUCCESS = new Color(0x4CAF50);

    private final SessaoChat sessao;
    private final Runnable aoLogarComSucesso;

    private JTextField tfHost, tfPorta, tfNomeUsuario, tfNomeExibicao;
    private JPasswordField tfSenha;
    private JTabbedPane abas;
    private JLabel lblStatus;

    public PainelLogin(SessaoChat sessao, Runnable aoLogarComSucesso) {
        this.sessao            = sessao;
        this.aoLogarComSucesso = aoLogarComSucesso;
        setLayout(new BorderLayout());
        setBackground(BG_A);
        construirUI();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        // Gradiente de fundo
        GradientPaint gp = new GradientPaint(0, 0, BG_A, getWidth(), getHeight(), BG_B);
        g2.setPaint(gp);
        g2.fillRect(0, 0, getWidth(), getHeight());
    }

    private void construirUI() {
        // Centraliza o card
        JPanel centro = new JPanel(new GridBagLayout());
        centro.setOpaque(false);
        add(centro, BorderLayout.CENTER);

        JPanel card = construirCard();
        centro.add(card);
    }

    private JPanel construirCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(420, 560));
        card.setBorder(BorderFactory.createEmptyBorder(32, 36, 32, 36));

        // Logo/título
        JLabel logo = new JLabel("💬", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 52));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(logo);

        card.add(Box.createVerticalStrut(4));

        JLabel titulo = centralizado("WhatsUT", 28, Font.BOLD, TEXT);
        card.add(titulo);

        JLabel sub = centralizado("Sistema de Comunicação Interpessoal", 12, Font.PLAIN, TEXT2);
        card.add(sub);

        card.add(Box.createVerticalStrut(20));

        // Conexão ao servidor
        JPanel painelConexao = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        painelConexao.setOpaque(false);

        tfHost  = campoPequeno("localhost", 8);
        tfPorta = campoPequeno("1099", 4);
        JButton btnConectar = botaoArredondadoPequeno("Conectar", ACCENT);
        btnConectar.addActionListener(e -> conectar());

        painelConexao.add(rotulo("Host:", 12, Font.PLAIN, TEXT2));
        painelConexao.add(tfHost);
        painelConexao.add(rotulo("Porta:", 12, Font.PLAIN, TEXT2));
        painelConexao.add(tfPorta);
        painelConexao.add(btnConectar);
        painelConexao.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        card.add(painelConexao);

        card.add(Box.createVerticalStrut(16));

        // Abas Login / Cadastro
        abas = new JTabbedPane();
        abas.setBackground(CARD);
        abas.setForeground(TEXT);
        abas.setFont(new Font("Segoe UI", Font.BOLD, 13));
        abas.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        abas.addTab("Entrar",    construirAbaLogin());
        abas.addTab("Cadastrar", construirAbaCadastro());
        abas.setOpaque(false);
        card.add(abas);

        card.add(Box.createVerticalStrut(12));

        // Status
        lblStatus = centralizado("", 12, Font.PLAIN, TEXT2);
        card.add(lblStatus);

        return card;
    }

    private JPanel construirAbaLogin() {
        JPanel p = painelFormulario();

        p.add(Box.createVerticalStrut(16));
        p.add(rotulo("Usuário", 13, Font.BOLD, TEXT2));
        p.add(Box.createVerticalStrut(4));
        tfNomeUsuario = campoGrande("seu_usuario");
        p.add(tfNomeUsuario);

        p.add(Box.createVerticalStrut(12));
        p.add(rotulo("Senha", 13, Font.BOLD, TEXT2));
        p.add(Box.createVerticalStrut(4));
        tfSenha = new JPasswordField();
        estilizarCampo(tfSenha);
        p.add(tfSenha);

        p.add(Box.createVerticalStrut(20));

        JButton btnEntrar = botaoArredondado("Entrar →", ACCENT2);
        btnEntrar.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnEntrar.addActionListener(e -> entrar());
        p.add(btnEntrar);

        // Enter para logar
        tfSenha.addActionListener(e -> entrar());

        return p;
    }

    private JPanel construirAbaCadastro() {
        JPanel p = painelFormulario();

        p.add(Box.createVerticalStrut(12));
        p.add(rotulo("Usuário (único)", 13, Font.BOLD, TEXT2));
        p.add(Box.createVerticalStrut(4));
        JTextField tfRegUsuario = campoGrande("usuario_unico");
        p.add(tfRegUsuario);

        p.add(Box.createVerticalStrut(10));
        p.add(rotulo("Nome de exibição", 13, Font.BOLD, TEXT2));
        p.add(Box.createVerticalStrut(4));
        tfNomeExibicao = campoGrande("Seu Nome");
        p.add(tfNomeExibicao);

        p.add(Box.createVerticalStrut(10));
        p.add(rotulo("Senha (mín. 4 caracteres)", 13, Font.BOLD, TEXT2));
        p.add(Box.createVerticalStrut(4));
        JPasswordField tfRegSenha = new JPasswordField();
        estilizarCampo(tfRegSenha);
        p.add(tfRegSenha);

        p.add(Box.createVerticalStrut(16));

        JButton btnReg = botaoArredondado("Criar Conta", SUCCESS);
        btnReg.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnReg.addActionListener(e -> cadastrar(tfRegUsuario, tfRegSenha));
        p.add(btnReg);

        return p;
    }

    // ── Ações ─────────────────────────────────────────────────────────────────

    private void conectar() {
        definirStatus("Conectando...", TEXT2);
        String host = tfHost.getText().trim();
        int porta;
        try { porta = Integer.parseInt(tfPorta.getText().trim()); }
        catch (NumberFormatException e) { definirStatus("Porta inválida.", ACCENT2); return; }

        new SwingWorker<Void, Void>() {
            Exception erro;
            @Override
            protected Void doInBackground() {
                try { sessao.conectar(host, porta); }
                catch (Exception e) { erro = e; }
                return null;
            }
            @Override
            protected void done() {
                if (erro != null) definirStatus("Erro: " + erro.getMessage(), ACCENT2);
                else definirStatus("✅ Conectado a " + host + ":" + porta, SUCCESS);
            }
        }.execute();
    }

    private void entrar() {
        String nomeUsuario = tfNomeUsuario.getText().trim();
        String senha       = new String(tfSenha.getPassword());
        if (nomeUsuario.isEmpty() || senha.isEmpty()) {
            definirStatus("Preencha usuário e senha.", ACCENT2);
            return;
        }
        definirStatus("Autenticando...", TEXT2);

        new SwingWorker<UsuarioDTO, Void>() {
            Exception erro;
            @Override
            protected UsuarioDTO doInBackground() {
                try { return sessao.entrar(nomeUsuario, senha); }
                catch (Exception e) { erro = e; return null; }
            }
            @Override
            protected void done() {
                try {
                    UsuarioDTO u = get();
                    if (erro != null) {
                        definirStatus("Erro: " + erro.getMessage(), ACCENT2);
                    } else if (u == null) {
                        definirStatus("❌ Credenciais inválidas ou usuário banido.", ACCENT2);
                    } else {
                        definirStatus("✅ Bem-vindo, " + u.getNomeExibicao() + "!", SUCCESS);
                        aoLogarComSucesso.run();
                    }
                } catch (Exception e) {
                    definirStatus("Erro inesperado: " + e.getMessage(), ACCENT2);
                }
            }
        }.execute();
    }

    private void cadastrar(JTextField tfUsuario, JPasswordField tfSenhaCad) {
        String nomeUsuario  = tfUsuario.getText().trim();
        String nomeExibicao = tfNomeExibicao.getText().trim();
        String senha        = new String(tfSenhaCad.getPassword());

        if (nomeUsuario.isEmpty() || nomeExibicao.isEmpty() || senha.length() < 4) {
            definirStatus("Preencha todos os campos (senha mín. 4 chars).", ACCENT2);
            return;
        }
        definirStatus("Criando conta...", TEXT2);

        new SwingWorker<Boolean, Void>() {
            Exception erro;
            @Override
            protected Boolean doInBackground() {
                try { return sessao.cadastrar(nomeUsuario, nomeExibicao, senha); }
                catch (Exception e) { erro = e; return false; }
            }
            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    if (erro != null) definirStatus("Erro: " + erro.getMessage(), ACCENT2);
                    else if (ok) {
                        definirStatus("✅ Conta criada! Faça login.", SUCCESS);
                        abas.setSelectedIndex(0);
                    } else {
                        definirStatus("❌ Usuário já existe.", ACCENT2);
                    }
                } catch (Exception e) {
                    definirStatus("Erro inesperado.", ACCENT2);
                }
            }
        }.execute();
    }

    // ── Utilitários de UI ─────────────────────────────────────────────────────

    private void definirStatus(String msg, Color cor) {
        SwingUtilities.invokeLater(() -> {
            lblStatus.setText(msg);
            lblStatus.setForeground(cor);
        });
    }

    private JPanel painelFormulario() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        return p;
    }

    private JLabel centralizado(String texto, int tamanho, int estilo, Color cor) {
        JLabel l = rotulo(texto, tamanho, estilo, cor);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    private JLabel rotulo(String texto, int tamanho, int estilo, Color cor) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("Segoe UI", estilo, tamanho));
        l.setForeground(cor);
        return l;
    }

    private JTextField campoGrande(String placeholder) {
        JTextField tf = new JTextField(placeholder);
        estilizarCampo(tf);
        return tf;
    }

    private JTextField campoPequeno(String texto, int cols) {
        JTextField tf = new JTextField(texto, cols);
        tf.setBackground(new Color(0x1A1A3E));
        tf.setForeground(TEXT);
        tf.setCaretColor(TEXT);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT, 1),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return tf;
    }

    private void estilizarCampo(JTextField tf) {
        tf.setBackground(new Color(0x1A1A3E));
        tf.setForeground(TEXT);
        tf.setCaretColor(TEXT);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
    }

    private JButton botaoArredondado(String texto, Color bg) {
        JButton btn = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(200, 40));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return btn;
    }

    private JButton botaoArredondadoPequeno(String texto, Color bg) {
        JButton btn = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Dimension d = new Dimension(100, 28);
        btn.setPreferredSize(d);
        btn.setMaximumSize(d);
        btn.setMinimumSize(d);
        return btn;
    }
}
