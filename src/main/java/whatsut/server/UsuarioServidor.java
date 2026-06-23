package whatsut.server;

import whatsut.common.RetornoChamadaCliente;
import whatsut.common.UsuarioDTO;
import whatsut.common.StatusUsuario;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo interno de usuário no servidor.
 * Contém o hash da senha (BCrypt) e a referência ao callback RMI.
 */
public class UsuarioServidor {

    private final String nomeUsuario;
    private String nomeExibicao;
    private String hashSenha;            // BCrypt hash — NUNCA a senha em texto puro
    private StatusUsuario status;
    private Instant ultimoAcesso;
    private boolean banido;
    private String motivoBan;
    private boolean adminServidor;
    private Instant criadoEm;
    private RetornoChamadaCliente retornoChamada; // referência remota para push de eventos
    private Instant ultimoHeartbeat;
    private List<String> idsGrupos;      // grupos dos quais é membro

    public UsuarioServidor(String nomeUsuario, String nomeExibicao, String hashSenha) {
        this.nomeUsuario   = nomeUsuario;
        this.nomeExibicao  = nomeExibicao;
        this.hashSenha     = hashSenha;
        this.status        = StatusUsuario.OFFLINE;
        this.criadoEm      = Instant.now();
        this.ultimoAcesso  = Instant.now();
        this.idsGrupos     = new ArrayList<>();
    }

    /** Converte para DTO (sem dados sensíveis). */
    public UsuarioDTO paraDTO() {
        UsuarioDTO dto = new UsuarioDTO(nomeUsuario, nomeExibicao, status);
        dto.setUltimoAcesso(ultimoAcesso);
        dto.setEhAdmin(adminServidor);
        return dto;
    }

    public boolean estaOnline() {
        return retornoChamada != null && status != StatusUsuario.OFFLINE;
    }

    // Getters e Setters
    public String getNomeUsuario()                            { return nomeUsuario; }
    public String getNomeExibicao()                           { return nomeExibicao; }
    public void setNomeExibicao(String d)                     { this.nomeExibicao = d; }
    public String getHashSenha()                              { return hashSenha; }
    public void setHashSenha(String h)                        { this.hashSenha = h; }
    public StatusUsuario getStatus()                          { return status; }
    public void setStatus(StatusUsuario s)                    { this.status = s; }
    public Instant getUltimoAcesso()                          { return ultimoAcesso; }
    public void setUltimoAcesso(Instant t)                    { this.ultimoAcesso = t; }
    public boolean isBanido()                                 { return banido; }
    public void setBanido(boolean b)                          { this.banido = b; }
    public String getMotivoBan()                              { return motivoBan; }
    public void setMotivoBan(String r)                        { this.motivoBan = r; }
    public boolean isAdminServidor()                          { return adminServidor; }
    public void setAdminServidor(boolean a)                   { this.adminServidor = a; }
    public Instant getCriadoEm()                              { return criadoEm; }
    public RetornoChamadaCliente getRetornoChamada()          { return retornoChamada; }
    public void setRetornoChamada(RetornoChamadaCliente c)    { this.retornoChamada = c; }
    public Instant getUltimoHeartbeat()                       { return ultimoHeartbeat; }
    public void setUltimoHeartbeat(Instant t)                 { this.ultimoHeartbeat = t; }
    public List<String> getIdsGrupos()                        { return idsGrupos; }
}
