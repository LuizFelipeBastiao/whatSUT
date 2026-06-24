package whatsut.client;

import whatsut.client.ui.PainelChat;
import whatsut.client.ui.PainelLogin;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        // Em ambientes com várias interfaces (LAN + VPN como Radmin/Hamachi/ZeroTier)
        // o IP retornado por getLocalHost() é da LAN e fica inalcançável pelo servidor
        // remoto, o que faz os callbacks (lista de usuários online, novos grupos, etc.)
        // falharem silenciosamente. Por isso pedimos ao usuário qual IP usar.
        String ipEscolhido = escolherIpDeCallback();
        System.setProperty("java.rmi.server.hostname", ipEscolhido);

        SwingUtilities.invokeLater(() -> {
            PrincipalCliente app = new PrincipalCliente();
            app.setVisible(true);
        });
    }

    private static String escolherIpDeCallback() {
        List<String> ips = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr.isLoopbackAddress() || addr instanceof java.net.Inet6Address) continue;
                    ips.add(addr.getHostAddress() + "  (" + ni.getDisplayName() + ")");
                }
            }
        } catch (Exception ignored) { }

        if (ips.isEmpty()) {
            try { return InetAddress.getLocalHost().getHostAddress(); }
            catch (Exception e) { return "localhost"; }
        }

        String[] opcoes = ips.toArray(new String[0]);
        String escolha = (String) JOptionPane.showInputDialog(
                null,
                "Selecione o IP local pelo qual o servidor irá te alcançar (callbacks).\n"
                        + "Se você está conectando via Radmin/Hamachi/ZeroTier, escolha o IP da VPN.",
                "Configurar IP do Cliente",
                JOptionPane.QUESTION_MESSAGE,
                null,
                opcoes,
                opcoes[0]);

        if (escolha == null) System.exit(0);
        return escolha.split("\\s")[0];
    }
}
