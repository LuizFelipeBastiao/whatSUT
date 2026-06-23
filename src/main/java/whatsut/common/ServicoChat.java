package whatsut.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Interface principal do Servidor RMI — ServicoChat.
 *
 * Exposta via rmiregistry; o cliente obtém o stub e chama estes
 * métodos remotamente. Para eventos "de volta" ao cliente,
 * o servidor usa a interface {@link RetornoChamadaCliente}.
 */
public interface ServicoChat extends Remote {

    // ── Autenticação ─────────────────────────────────────────────

    /**
     * Registra um novo usuário (cadastro).
     * Senha é transmitida em texto puro aqui e hasheada no servidor com BCrypt.
     * @return true se cadastro bem-sucedido
     */
    boolean cadastrar(String nomeUsuario, String nomeExibicao, String senha)
            throws RemoteException;

    /**
     * Realiza login. Vincula o callback do cliente à sessão.
     * @return UsuarioDTO com dados do usuário ou null se credenciais inválidas
     */
    UsuarioDTO entrar(String nomeUsuario, String senha, RetornoChamadaCliente retornoChamada)
            throws RemoteException;

    /** Encerra a sessão do usuário logado. */
    void sair(String nomeUsuario) throws RemoteException;

    /** Altera a senha do usuário (requer senha atual). */
    boolean alterarSenha(String nomeUsuario, String senhaAtual,
                          String novaSenha) throws RemoteException;

    // ── Presença ─────────────────────────────────────────────────

    /** Retorna lista de usuários (online e offline). */
    List<UsuarioDTO> obterUsuarios(String usuarioSolicitante) throws RemoteException;

    /** Atualiza o status do usuário. */
    void definirStatus(String nomeUsuario, StatusUsuario status) throws RemoteException;

    // ── Chat privado ─────────────────────────────────────────────

    /** Envia mensagem de texto privada. */
    void enviarMensagemPrivada(String deUsuario, String paraUsuario,
                                MensagemDTO mensagem) throws RemoteException;

    /** Envia arquivo em chat privado. */
    void enviarArquivo(String deUsuario, String paraUsuario,
                        MensagemDTO mensagemArquivo) throws RemoteException;

    /** Recupera histórico de mensagens privadas (últimas N). */
    List<MensagemDTO> obterHistoricoPrivado(String usuario1, String usuario2, int limite)
            throws RemoteException;

    // ── Grupos ───────────────────────────────────────────────────

    /** Lista todos os grupos disponíveis (para entrar ou já membro). */
    List<GrupoDTO> obterGrupos(String usuarioSolicitante) throws RemoteException;

    /**
     * Cria um novo grupo. O criador torna-se automaticamente o admin.
     * @return GrupoDTO criado (com ID gerado pelo servidor)
     */
    GrupoDTO criarGrupo(String nomeUsuarioCriador, String nomeGrupo,
                         String descricao, boolean grupoPrivado,
                         GrupoDTO.AoAdminSair aoAdminSair)
            throws RemoteException;

    /**
     * Solicita entrada em um grupo.
     * Se o grupo for público: entrada imediata.
     * Se for privado: envia notificação ao admin via callback.
     */
    void solicitarEntradaGrupo(String nomeUsuario, String idGrupo) throws RemoteException;

    /** Admin aprova solicitação de entrada. */
    void aprovarSolicitacao(String nomeUsuarioAdmin, String idGrupo,
                             String nomeSolicitante) throws RemoteException;

    /** Admin rejeita solicitação de entrada. */
    void negarSolicitacao(String nomeUsuarioAdmin, String idGrupo,
                           String nomeSolicitante) throws RemoteException;

    /** Envia mensagem para o grupo. */
    void enviarMensagemGrupo(String deUsuario, String idGrupo,
                              MensagemDTO mensagem) throws RemoteException;

    /** Recupera histórico de mensagens do grupo (últimas N). */
    List<MensagemDTO> obterHistoricoGrupo(String idGrupo, int limite)
            throws RemoteException;

    /** Usuário sai do grupo voluntariamente. */
    void sairDoGrupo(String nomeUsuario, String idGrupo) throws RemoteException;

    /**
     * Admin bane usuário do grupo.
     * Se o banido for o próprio admin: invoca lógica de sucessão.
     */
    void banirDoGrupo(String nomeUsuarioAdmin, String idGrupo,
                       String nomeAlvo) throws RemoteException;

    /** Admin transfere a administração para outro membro. */
    void transferirAdminGrupo(String adminAtual, String idGrupo,
                                String novoAdmin) throws RemoteException;

    /** Admin deleta o grupo. */
    void deletarGrupo(String nomeUsuarioAdmin, String idGrupo) throws RemoteException;

    // ── Moderação global (admin do servidor) ─────────────────────

    /**
     * Ban global de usuário — só pode ser feito por admin do servidor.
     * Desconecta o usuário imediatamente via callback e o marca como banido.
     */
    void banirUsuario(String nomeUsuarioAdmin, String nomeAlvo, String motivo)
            throws RemoteException;

    /** Remove ban de usuário. */
    void desbanirUsuario(String nomeUsuarioAdmin, String nomeAlvo)
            throws RemoteException;

    /** Lista todos os usuários banidos (somente para admin). */
    List<UsuarioDTO> obterUsuariosBanidos(String nomeUsuarioAdmin) throws RemoteException;

    // ── Configurações do servidor ────────────────────────────────

    /** Retorna configurações atuais do servidor (somente admin). */
    ConfiguracaoServidor obterConfiguracao(String nomeUsuarioAdmin) throws RemoteException;

    /** Atualiza configurações do servidor (somente admin). */
    void atualizarConfiguracao(String nomeUsuarioAdmin, ConfiguracaoServidor configuracao)
            throws RemoteException;

    /** Retorna estatísticas em tempo real do servidor. */
    EstatisticasServidor obterEstatisticas(String nomeUsuarioAdmin) throws RemoteException;

    // ── Heartbeat ────────────────────────────────────────────────

    /** Mantém sessão viva e atualiza timestamp de atividade. */
    void manterConectado(String nomeUsuario) throws RemoteException;
}
