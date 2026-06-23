package whatsut.server;

import whatsut.common.GrupoDTO;
import whatsut.common.MensagemDTO;

import java.util.*;

/** Modelo interno de grupo de chat. */
public class GrupoServidor {

    private final String id;
    private String nome;
    private String descricao;
    private String nomeUsuarioAdmin;
    private final LinkedList<String> nomesMembros; // LinkedList = ordem de entrada
    private final List<String> solicitacoesPendentes;
    private boolean grupoPrivado;
    private GrupoDTO.AoAdminSair aoAdminSair;
    private final LinkedList<MensagemDTO> historico;
    private final int MAX_HISTORICO = 500;

    public GrupoServidor(String id, String nome, String nomeUsuarioAdmin,
                          boolean grupoPrivado, GrupoDTO.AoAdminSair aoAdminSair) {
        this.id                     = id;
        this.nome                   = nome;
        this.nomeUsuarioAdmin       = nomeUsuarioAdmin;
        this.grupoPrivado           = grupoPrivado;
        this.aoAdminSair            = aoAdminSair;
        this.nomesMembros           = new LinkedList<>();
        this.solicitacoesPendentes  = new ArrayList<>();
        this.historico              = new LinkedList<>();
        this.nomesMembros.add(nomeUsuarioAdmin);
    }

    /** Adiciona mensagem ao histórico (mantém limite). */
    public synchronized void adicionarMensagem(MensagemDTO msg) {
        historico.addLast(msg);
        if (historico.size() > MAX_HISTORICO) historico.removeFirst();
    }

    /** Retorna as últimas {@code limite} mensagens. */
    public synchronized List<MensagemDTO> obterHistorico(int limite) {
        int inicio = Math.max(0, historico.size() - limite);
        return new ArrayList<>(new ArrayList<>(historico).subList(inicio, historico.size()));
    }

    /** Converte para DTO (sem histórico de mensagens). */
    public synchronized GrupoDTO paraDTO() {
        GrupoDTO dto = new GrupoDTO(id, nome, nomeUsuarioAdmin);
        dto.setDescricao(descricao);
        dto.setNomesMembros(new ArrayList<>(nomesMembros));
        dto.setSolicitacoesPendentes(new ArrayList<>(solicitacoesPendentes));
        dto.setGrupoPrivado(grupoPrivado);
        dto.setAoAdminSair(aoAdminSair);
        dto.setQuantidadeMembros(nomesMembros.size());
        return dto;
    }

    /**
     * Lida com saída do admin. Retorna o novo admin ou null se o grupo deve ser deletado.
     */
    public synchronized String tratarSaidaDoAdmin() {
        nomesMembros.remove(nomeUsuarioAdmin);
        if (nomesMembros.isEmpty()) return null; // grupo vazio → deletar

        return switch (aoAdminSair) {
            case PROMOVER_MAIS_ANTIGO -> {
                String novoAdmin = nomesMembros.getFirst();
                nomeUsuarioAdmin = novoAdmin;
                yield novoAdmin;
            }
            case PROMOVER_ALEATORIO -> {
                List<String> lista = new ArrayList<>(nomesMembros);
                String novoAdmin = lista.get(new Random().nextInt(lista.size()));
                nomeUsuarioAdmin = novoAdmin;
                yield novoAdmin;
            }
            case DELETAR_GRUPO -> null;
        };
    }

    // Getters e Setters
    public String getId()                                  { return id; }
    public String getNome()                                { return nome; }
    public void setNome(String n)                          { this.nome = n; }
    public String getDescricao()                           { return descricao; }
    public void setDescricao(String d)                     { this.descricao = d; }
    public String getNomeUsuarioAdmin()                    { return nomeUsuarioAdmin; }
    public void setNomeUsuarioAdmin(String a)              { this.nomeUsuarioAdmin = a; }
    public LinkedList<String> getNomesMembros()            { return nomesMembros; }
    public List<String> getSolicitacoesPendentes()         { return solicitacoesPendentes; }
    public boolean isGrupoPrivado()                        { return grupoPrivado; }
    public GrupoDTO.AoAdminSair getAoAdminSair()           { return aoAdminSair; }
}
