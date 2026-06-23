package whatsut.server;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Ponto de entrada do servidor WhatsUT.
 * Inicia o registro RMI e exibe a interface gráfica de administração.
 */
public class PrincipalServidor {
    private static final Logger log = Logger.getLogger(PrincipalServidor.class.getName());
    public static final int PORTA_RMI = 1099;
    public static final String NOME_SERVICO = "WhatsUTService";

    public static void main(String[] args) {
        // Coleta todos os IPs disponíveis na máquina
        List<String> ips = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr.isLoopbackAddress() || addr instanceof java.net.Inet6Address) continue;
                    ips.add(addr.getHostAddress() + "  (" + ni.getDisplayName() + ")");
                }
            }
        } catch (Exception e) { /* ignora */ }

        if (ips.isEmpty()) ips.add("localhost");

        // Pergunta ao usuário qual IP usar
        String[] opcoes = ips.toArray(new String[0]);
        String escolha = (String) javax.swing.JOptionPane.showInputDialog(
                null,
                "Selecione o endereço IP que os clientes usarão para se conectar:",
                "Configurar IP do Servidor",
                javax.swing.JOptionPane.QUESTION_MESSAGE,
                null,
                opcoes,
                opcoes[0]);

        if (escolha == null) System.exit(0); // cancelou

        // Extrai só o IP (antes do espaço)
        String ip = escolha.split("\\s")[0];
        System.setProperty("java.rmi.server.hostname", ip);
        log.info("Hostname RMI definido como: " + ip);

        try {
            ServicoChatImpl servico = new ServicoChatImpl();
            Registry registro = LocateRegistry.createRegistry(PORTA_RMI);
            registro.rebind(NOME_SERVICO, servico);

            log.info("WhatsUT Server iniciado na porta " + PORTA_RMI);
            log.info("Serviço registrado como: " + NOME_SERVICO);

            // Inicia a interface gráfica de administração (Swing)
            javax.swing.SwingUtilities.invokeLater(() -> {
                InterfaceAdminServidor adminUI = new InterfaceAdminServidor(servico);
                adminUI.setVisible(true);
            });

        } catch (Exception e) {
            log.severe("Falha ao iniciar o servidor: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
