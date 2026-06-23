package whatsut.client;

import whatsut.common.*;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.function.Consumer;

/**
 * Implementação do callback RMI no lado do cliente.
 *
 * O servidor detém uma referência remota a este objeto e chama seus métodos
 * para "empurrar" eventos ao cliente (mensagens, atualizações de lista, etc.).
 * Cada evento é redirecionado para um listener funcional na UI.
 */
public class RetornoChamadaClienteImpl extends UnicastRemoteObject implements RetornoChamadaCliente {
    private static final long serialVersionUID = 1L;

    // Listeners registrados pela UI
    private Consumer<MensagemDTO>      aoReceberMensagem;
    private Consumer<List<UsuarioDTO>> aoAtualizarListaUsuarios;
    private ConsumidorStatusMensagem   aoMudarStatusUsuario;
    private Consumer<GrupoDTO>         aoEntrarNoGrupo;
    private Consumer<String[]>         aoSairDoGrupo;       // [idGrupo, motivo]
    private Consumer<GrupoDTO>         aoAtualizarGrupo;
    private ConsumidorSolicitacaoEntrada aoReceberSolicitacao;
    private Consumer<GrupoDTO>         aoAprovarSolicitacao;
    private Consumer<String[]>         aoNegarSolicitacao;  // [idGrupo, nomeGrupo]
    private Consumer<String>           aoSerBanidoDoServidor;
    private Consumer<MensagemDTO>      aoReceberArquivo;

    @FunctionalInterface
    public interface ConsumidorStatusMensagem {
        void aceitar(String nomeUsuario, StatusUsuario status);
    }
    @FunctionalInterface
    public interface ConsumidorSolicitacaoEntrada {
        void aceitar(String idGrupo, String nomeGrupo, String nomeUsuario, String exibicao);
    }

    public RetornoChamadaClienteImpl() throws RemoteException { super(); }

    // ── Interface RMI ─────────────────────────────────────────────────────────

    @Override
    public void aoReceberMensagem(MensagemDTO mensagem) throws RemoteException {
        if (aoReceberMensagem != null) aoReceberMensagem.accept(mensagem);
    }

    @Override
    public void aoAtualizarListaUsuarios(List<UsuarioDTO> usuarios) throws RemoteException {
        if (aoAtualizarListaUsuarios != null) aoAtualizarListaUsuarios.accept(usuarios);
    }

    @Override
    public void aoMudarStatusUsuario(String nomeUsuario, StatusUsuario novoStatus)
            throws RemoteException {
        if (aoMudarStatusUsuario != null) aoMudarStatusUsuario.aceitar(nomeUsuario, novoStatus);
    }

    @Override
    public void aoEntrarNoGrupo(GrupoDTO grupo) throws RemoteException {
        if (aoEntrarNoGrupo != null) aoEntrarNoGrupo.accept(grupo);
    }

    @Override
    public void aoSairDoGrupo(String idGrupo, String motivo) throws RemoteException {
        if (aoSairDoGrupo != null)
            aoSairDoGrupo.accept(new String[]{idGrupo, motivo});
    }

    @Override
    public void aoAtualizarGrupo(GrupoDTO grupo) throws RemoteException {
        if (aoAtualizarGrupo != null) aoAtualizarGrupo.accept(grupo);
    }

    @Override
    public void aoReceberSolicitacao(String idGrupo, String nomeGrupo,
                                      String nomeSolicitante, String exibicaoSolicitante)
            throws RemoteException {
        if (aoReceberSolicitacao != null)
            aoReceberSolicitacao.aceitar(idGrupo, nomeGrupo, nomeSolicitante, exibicaoSolicitante);
    }

    @Override
    public void aoAprovarSolicitacao(GrupoDTO grupo) throws RemoteException {
        if (aoAprovarSolicitacao != null) aoAprovarSolicitacao.accept(grupo);
    }

    @Override
    public void aoNegarSolicitacao(String idGrupo, String nomeGrupo)
            throws RemoteException {
        if (aoNegarSolicitacao != null) aoNegarSolicitacao.accept(new String[]{idGrupo, nomeGrupo});
    }

    @Override
    public void aoSerBanidoDoServidor(String motivo) throws RemoteException {
        if (aoSerBanidoDoServidor != null) aoSerBanidoDoServidor.accept(motivo);
    }

    @Override
    public void aoReceberArquivo(MensagemDTO mensagemArquivo) throws RemoteException {
        if (aoReceberArquivo != null) aoReceberArquivo.accept(mensagemArquivo);
    }

    @Override
    public void verificar() throws RemoteException { /* responde ao heartbeat do servidor */ }

    // ── Registro de listeners ─────────────────────────────────────────────────

    public void setAoReceberMensagem(Consumer<MensagemDTO> l)             { this.aoReceberMensagem = l; }
    public void setAoAtualizarListaUsuarios(Consumer<List<UsuarioDTO>> l) { this.aoAtualizarListaUsuarios = l; }
    public void setAoMudarStatusUsuario(ConsumidorStatusMensagem l)       { this.aoMudarStatusUsuario = l; }
    public void setAoEntrarNoGrupo(Consumer<GrupoDTO> l)                  { this.aoEntrarNoGrupo = l; }
    public void setAoSairDoGrupo(Consumer<String[]> l)                    { this.aoSairDoGrupo = l; }
    public void setAoAtualizarGrupo(Consumer<GrupoDTO> l)                 { this.aoAtualizarGrupo = l; }
    public void setAoReceberSolicitacao(ConsumidorSolicitacaoEntrada l)   { this.aoReceberSolicitacao = l; }
    public void setAoAprovarSolicitacao(Consumer<GrupoDTO> l)             { this.aoAprovarSolicitacao = l; }
    public void setAoNegarSolicitacao(Consumer<String[]> l)               { this.aoNegarSolicitacao = l; }
    public void setAoSerBanidoDoServidor(Consumer<String> l)              { this.aoSerBanidoDoServidor = l; }
    public void setAoReceberArquivo(Consumer<MensagemDTO> l)              { this.aoReceberArquivo = l; }
}
