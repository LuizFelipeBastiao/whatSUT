package whatsut.common;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa um grupo de chat.
 */
public class GrupoDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum AoAdminSair { PROMOVER_MAIS_ANTIGO, PROMOVER_ALEATORIO, DELETAR_GRUPO }

    private String id;
    private String nome;
    private String descricao;
    private String nomeUsuarioAdmin;
    private List<String> nomesMembros;
    private List<String> solicitacoesPendentes; // nomes aguardando aprovação
    private boolean grupoPrivado;               // requer aprovação para entrar
    private AoAdminSair aoAdminSair;
    private Instant criadoEm;
    private int quantidadeMembros;

    public GrupoDTO() {
        this.nomesMembros          = new ArrayList<>();
        this.solicitacoesPendentes = new ArrayList<>();
        this.aoAdminSair           = AoAdminSair.PROMOVER_MAIS_ANTIGO;
        this.criadoEm              = Instant.now();
    }

    public GrupoDTO(String id, String nome, String nomeUsuarioAdmin) {
        this();
        this.id               = id;
        this.nome             = nome;
        this.nomeUsuarioAdmin = nomeUsuarioAdmin;
        this.nomesMembros.add(nomeUsuarioAdmin);
    }

    // Getters e Setters
    public String getId()                              { return id; }
    public void setId(String id)                       { this.id = id; }
    public String getNome()                            { return nome; }
    public void setNome(String nome)                   { this.nome = nome; }
    public String getDescricao()                       { return descricao; }
    public void setDescricao(String d)                 { this.descricao = d; }
    public String getNomeUsuarioAdmin()                { return nomeUsuarioAdmin; }
    public void setNomeUsuarioAdmin(String a)          { this.nomeUsuarioAdmin = a; }
    public List<String> getNomesMembros()              { return nomesMembros; }
    public void setNomesMembros(List<String> m)        { this.nomesMembros = m; }
    public List<String> getSolicitacoesPendentes()     { return solicitacoesPendentes; }
    public void setSolicitacoesPendentes(List<String> p) { this.solicitacoesPendentes = p; }
    public boolean isGrupoPrivado()                    { return grupoPrivado; }
    public void setGrupoPrivado(boolean p)             { this.grupoPrivado = p; }
    public AoAdminSair getAoAdminSair()                { return aoAdminSair; }
    public void setAoAdminSair(AoAdminSair o)          { this.aoAdminSair = o; }
    public Instant getCriadoEm()                       { return criadoEm; }
    public int getQuantidadeMembros()                  { return quantidadeMembros; }
    public void setQuantidadeMembros(int c)            { this.quantidadeMembros = c; }

    @Override
    public String toString() {
        return nome + " [" + quantidadeMembros + " membros]";
    }
}
