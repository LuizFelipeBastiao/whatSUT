package whatsut.common;

import java.io.Serializable;
import java.time.Instant;

/**
 * Data Transfer Object para usuário — trafega via RMI.
 */
public class UsuarioDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nomeUsuario;
    private String nomeExibicao;
    private StatusUsuario status;
    private Instant ultimoAcesso;
    private boolean ehAdmin; // admin do servidor (pode banir usuários globais)

    public UsuarioDTO() {}

    public UsuarioDTO(String nomeUsuario, String nomeExibicao, StatusUsuario status) {
        this.nomeUsuario  = nomeUsuario;
        this.nomeExibicao = nomeExibicao;
        this.status       = status;
        this.ultimoAcesso = Instant.now();
    }

    public String getNomeUsuario()              { return nomeUsuario; }
    public void setNomeUsuario(String u)        { this.nomeUsuario = u; }
    public String getNomeExibicao()             { return nomeExibicao; }
    public void setNomeExibicao(String d)       { this.nomeExibicao = d; }
    public StatusUsuario getStatus()            { return status; }
    public void setStatus(StatusUsuario s)      { this.status = s; }
    public Instant getUltimoAcesso()            { return ultimoAcesso; }
    public void setUltimoAcesso(Instant t)      { this.ultimoAcesso = t; }
    public boolean isEhAdmin()                  { return ehAdmin; }
    public void setEhAdmin(boolean a)           { this.ehAdmin = a; }

    @Override
    public String toString() {
        return nomeExibicao + " (" + nomeUsuario + ") [" + status + "]";
    }
}
