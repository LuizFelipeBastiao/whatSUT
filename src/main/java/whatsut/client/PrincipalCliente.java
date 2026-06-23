package whatsut.client;

import whatsut.client.ui.PainelChat;
import whatsut.client.ui.PainelLogin;

import java.net.InetAddress;
import javax.swing.*;
import java.awt.*;

/**
 * Janela principal do cliente WhatsUT.
 * Alterna entre a tela de Login e a tela de Chat.
 */
public class PrincipalCliente extends JFrame {

    private final SessaoChat sessao = new SessaoChat();
    private JPanel painelPrincipal;
    private CardLayout cardLayout;

    private static final String CARD_LOGIN = "login";
    private static final String CARD_CHAT  = "chat";

    public PrincipalCliente() {
        setTitle("WhatsUT");
        setSize(1100, 700);
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Ícone de aplicação (silencioso se o recurso não existir)
        java.net.URL urlIcone = getClass().getResource("/icon.png");
        if (urlIcone != null) {
            setIconImage(Toolkit.getDefaultToolkit().createImage(urlIcone));
        }

        cardLayout      = new CardLayout();
        painelPrincipal = new JPanel(cardLayout);

        // Painel de login
        PainelLogin painelLogin = new PainelLogin(sessao, this::mostrarChat);
        painelPrincipal.add(painelLogin, CARD_LOGIN);

        setContentPane(painelPrincipal);
        cardLayout.show(painelPrincipal, CARD_LOGIN);
    }

    private void mostrarChat() {
        // Remove o painel de chat anterior (se houver)
        Component antigo = null;
        for (Component c : painelPrincipal.getComponents()) {
            if (c.getName() != null && c.getName().equals(CARD_CHAT)) {
                antigo = c;
                break;
            }
        }
        if (antigo != null) painelPrincipal.remove(antigo);

        // Cria um novo painel de chat
        PainelChat painelChat = new PainelChat(sessao, this::mostrarLogin);
        painelChat.setName(CARD_CHAT);
        painelPrincipal.add(painelChat, CARD_CHAT);
        cardLayout.show(painelPrincipal, CARD_CHAT);

        setTitle("WhatsUT — " + sessao.getUsuarioAtual().getNomeExibicao());
    }

    private void mostrarLogin() {
        setTitle("WhatsUT");
        cardLayout.show(painelPrincipal, CARD_LOGIN);
    }

    public static void main(String[] args) {
        // Look and feel do sistema (melhor aparência no Windows/macOS)
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) { /* padrão */ }

        // Necessário para que o servidor consiga chamar callbacks neste cliente.
        // Usa o IP real da máquina para funcionar na rede local.
        try {
            String ipLocal = InetAddress.getLocalHost().getHostAddress();
            System.setProperty("java.rmi.server.hostname", ipLocal);
        } catch (Exception e) {
            System.setProperty("java.rmi.server.hostname", "localhost");
        }

        SwingUtilities.invokeLater(() -> {
            PrincipalCliente app = new PrincipalCliente();
            app.setVisible(true);
        });
    }
}
