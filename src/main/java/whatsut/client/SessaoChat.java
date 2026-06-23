package whatsut.client;

import whatsut.common.*;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Mantém o estado da sessão RMI no cliente:
 * stub do serviço, usuário logado, callback e heartbeat.
 */
public class SessaoChat {

    private ServicoChat servico;
    private RetornoChamadaClienteImpl retornoChamada;
    private UsuarioDTO usuarioAtual;
    private Timer timerHeartbeat;

    private String host;
    private int porta;

    public SessaoChat() {}

    /**
     * Conecta ao registro RMI e retorna o stub do serviço.
     * @throws Exception se não conseguir conectar
     */
    public ServicoChat conectar(String host, int porta) throws Exception {
        this.host  = host;
        this.porta = porta;
        Registry registro = LocateRegistry.getRegistry(host, porta);
        servico = (ServicoChat) registro.lookup("WhatsUTService");
        return servico;
    }

    /**
     * Realiza login e inicia o heartbeat.
     */
    public UsuarioDTO entrar(String nomeUsuario, String senha) throws Exception {
        if (servico == null) throw new IllegalStateException("Não conectado ao servidor.");

        retornoChamada = new RetornoChamadaClienteImpl();
        usuarioAtual = servico.entrar(nomeUsuario, senha, retornoChamada);
        if (usuarioAtual == null) return null;

        iniciarHeartbeat();
        return usuarioAtual;
    }

    /**
     * Registra novo usuário.
     */
    public boolean cadastrar(String nomeUsuario, String nomeExibicao, String senha)
            throws Exception {
        if (servico == null) throw new IllegalStateException("Não conectado ao servidor.");
        return servico.cadastrar(nomeUsuario, nomeExibicao, senha);
    }

    /**
     * Encerra a sessão.
     */
    public void sair() {
        pararHeartbeat();
        if (servico != null && usuarioAtual != null) {
            try { servico.sair(usuarioAtual.getNomeUsuario()); }
            catch (Exception e) { /* ignorar */ }
        }
        usuarioAtual   = null;
        retornoChamada = null;
    }

    // ── Heartbeat ─────────────────────────────────────────────────────────────

    private void iniciarHeartbeat() {
        timerHeartbeat = new Timer("heartbeat", true);
        timerHeartbeat.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (servico != null && usuarioAtual != null)
                        servico.manterConectado(usuarioAtual.getNomeUsuario());
                } catch (Exception e) {
                    pararHeartbeat();
                }
            }
        }, 20_000, 20_000); // a cada 20s
    }

    private void pararHeartbeat() {
        if (timerHeartbeat != null) {
            timerHeartbeat.cancel();
            timerHeartbeat = null;
        }
    }

    // Getters
    public ServicoChat getServico()                            { return servico; }
    public RetornoChamadaClienteImpl getRetornoChamada()       { return retornoChamada; }
    public UsuarioDTO getUsuarioAtual()                        { return usuarioAtual; }
    public boolean estaLogado()                                { return usuarioAtual != null; }
}
