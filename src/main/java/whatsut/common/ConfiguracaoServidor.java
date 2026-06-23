package whatsut.common;

import java.io.Serializable;

/** Configurações do servidor — editáveis via interface de admin. */
public class ConfiguracaoServidor implements Serializable {
    private static final long serialVersionUID = 1L;

    private int maxUsuariosPorGrupo        = 100;
    private int tamanhoMaxArquivo          = 10 * 1024 * 1024; // 10 MB
    private int limiteHistoricoMensagens   = 500;
    private boolean permitirTransferenciaArquivos = true;
    private boolean exigirAprovacaoGrupos  = false;
    private String nomeServidor            = "WhatsUT Server";
    private String mensagemDia             = "Bem-vindo ao WhatsUT!";
    private int timeoutSessaoMinutos       = 30;

    public ConfiguracaoServidor() {}

    public int getMaxUsuariosPorGrupo()              { return maxUsuariosPorGrupo; }
    public void setMaxUsuariosPorGrupo(int v)        { this.maxUsuariosPorGrupo = v; }
    public int getTamanhoMaxArquivo()                { return tamanhoMaxArquivo; }
    public void setTamanhoMaxArquivo(int v)          { this.tamanhoMaxArquivo = v; }
    public int getLimiteHistoricoMensagens()         { return limiteHistoricoMensagens; }
    public void setLimiteHistoricoMensagens(int v)   { this.limiteHistoricoMensagens = v; }
    public boolean isPermitirTransferenciaArquivos() { return permitirTransferenciaArquivos; }
    public void setPermitirTransferenciaArquivos(boolean v) { this.permitirTransferenciaArquivos = v; }
    public boolean isExigirAprovacaoGrupos()         { return exigirAprovacaoGrupos; }
    public void setExigirAprovacaoGrupos(boolean v)  { this.exigirAprovacaoGrupos = v; }
    public String getNomeServidor()                  { return nomeServidor; }
    public void setNomeServidor(String v)            { this.nomeServidor = v; }
    public String getMensagemDia()                   { return mensagemDia; }
    public void setMensagemDia(String v)             { this.mensagemDia = v; }
    public int getTimeoutSessaoMinutos()             { return timeoutSessaoMinutos; }
    public void setTimeoutSessaoMinutos(int v)       { this.timeoutSessaoMinutos = v; }
}
