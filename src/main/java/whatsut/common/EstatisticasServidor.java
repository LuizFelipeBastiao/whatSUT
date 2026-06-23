package whatsut.common;

import java.io.Serializable;
import java.time.Instant;

/** Estatísticas em tempo real do servidor. */
public class EstatisticasServidor implements Serializable {
    private static final long serialVersionUID = 1L;

    private int usuariosOnline;
    private int totalUsuarios;
    private int totalGrupos;
    private int mensagensEntregues;
    private long bytesTransferidos;
    private Instant inicioServidor;
    private int usuariosBanidos;

    public EstatisticasServidor() {}

    public int getUsuariosOnline()              { return usuariosOnline; }
    public void setUsuariosOnline(int v)        { this.usuariosOnline = v; }
    public int getTotalUsuarios()               { return totalUsuarios; }
    public void setTotalUsuarios(int v)         { this.totalUsuarios = v; }
    public int getTotalGrupos()                 { return totalGrupos; }
    public void setTotalGrupos(int v)           { this.totalGrupos = v; }
    public int getMensagensEntregues()          { return mensagensEntregues; }
    public void setMensagensEntregues(int v)    { this.mensagensEntregues = v; }
    public long getBytesTransferidos()          { return bytesTransferidos; }
    public void setBytesTransferidos(long v)    { this.bytesTransferidos = v; }
    public Instant getInicioServidor()          { return inicioServidor; }
    public void setInicioServidor(Instant v)    { this.inicioServidor = v; }
    public int getUsuariosBanidos()             { return usuariosBanidos; }
    public void setUsuariosBanidos(int v)       { this.usuariosBanidos = v; }
}
