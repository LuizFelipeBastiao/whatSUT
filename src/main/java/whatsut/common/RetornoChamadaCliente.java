package whatsut.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Interface de Callback do Cliente — implementada pelo cliente,
 * chamada pelo servidor (pattern RMI Callback).
 *
 * O servidor mantém referências remotas a cada cliente conectado
 * e usa esta interface para "empurrar" eventos em tempo real,
 * sem o cliente precisar fazer polling.
 */
public interface RetornoChamadaCliente extends Remote {

    // ── Mensagens ────────────────────────────────────────────────
    /** Recebe uma nova mensagem (privada ou de grupo). */
    void aoReceberMensagem(MensagemDTO mensagem) throws RemoteException;

    // ── Presença de usuários ─────────────────────────────────────
    /** Lista atualizada de usuários online. */
    void aoAtualizarListaUsuarios(List<UsuarioDTO> usuarios) throws RemoteException;

    /** Um usuário específico mudou de status. */
    void aoMudarStatusUsuario(String nomeUsuario, StatusUsuario novoStatus) throws RemoteException;

    // ── Grupos ───────────────────────────────────────────────────
    /** Você foi adicionado/aprovado em um grupo. */
    void aoEntrarNoGrupo(GrupoDTO grupo) throws RemoteException;

    /** Você foi removido/banido de um grupo. */
    void aoSairDoGrupo(String idGrupo, String motivo) throws RemoteException;

    /** Dados de um grupo foram atualizados (nome, admin, membros...). */
    void aoAtualizarGrupo(GrupoDTO grupo) throws RemoteException;

    /** (Só para o admin) alguém pediu para entrar no grupo. */
    void aoReceberSolicitacao(String idGrupo, String nomeGrupo,
                               String nomeSolicitante, String nomeExibicaoSolicitante)
            throws RemoteException;

    /** Sua solicitação de entrada no grupo foi aprovada. */
    void aoAprovarSolicitacao(GrupoDTO grupo) throws RemoteException;

    /** Sua solicitação de entrada no grupo foi negada. */
    void aoNegarSolicitacao(String idGrupo, String nomeGrupo) throws RemoteException;

    // ── Moderação ────────────────────────────────────────────────
    /** Você foi banido do servidor pelo administrador. */
    void aoSerBanidoDoServidor(String motivo) throws RemoteException;

    // ── Transferência de arquivos ────────────────────────────────
    /** Recebe um arquivo de outro usuário em chat privado. */
    void aoReceberArquivo(MensagemDTO mensagemArquivo) throws RemoteException;

    // ── Heartbeat ────────────────────────────────────────────────
    /** Ping do servidor para verificar se o cliente ainda está vivo. */
    void verificar() throws RemoteException;
}
