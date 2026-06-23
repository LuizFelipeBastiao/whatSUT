package whatsut.common;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Representa uma mensagem de chat (privada ou em grupo).
 */
public class MensagemDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Tipo { TEXTO, ARQUIVO, SISTEMA }

    private String id;
    private String nomeRemetente;
    private String nomeExibicaoRemetente;
    private String conteudo;          // texto da mensagem
    private Tipo tipo;
    private Instant momento;
    private String idDestino;         // nome de usuário (privado) ou id do grupo (grupo)
    private boolean ehGrupo;

    // Para mensagens de arquivo
    private String nomeArquivo;
    private byte[] dadosArquivo;
    private long tamanhoArquivo;

    public MensagemDTO() {}

    public static MensagemDTO texto(String remetente, String nomeExibicaoRemetente,
                                     String idDestino, boolean ehGrupo, String texto) {
        MensagemDTO m = new MensagemDTO();
        m.id = UUID.randomUUID().toString();
        m.nomeRemetente = remetente;
        m.nomeExibicaoRemetente = nomeExibicaoRemetente;
        m.idDestino = idDestino;
        m.ehGrupo = ehGrupo;
        m.conteudo = texto;
        m.tipo = Tipo.TEXTO;
        m.momento = Instant.now();
        return m;
    }

    public static MensagemDTO arquivo(String remetente, String nomeExibicaoRemetente,
                                       String idDestinoUsuario, String nomeArquivo,
                                       byte[] dadosArquivo) {
        MensagemDTO m = new MensagemDTO();
        m.id = UUID.randomUUID().toString();
        m.nomeRemetente = remetente;
        m.nomeExibicaoRemetente = nomeExibicaoRemetente;
        m.idDestino = idDestinoUsuario;
        m.ehGrupo = false;
        m.tipo = Tipo.ARQUIVO;
        m.nomeArquivo = nomeArquivo;
        m.dadosArquivo = dadosArquivo;
        m.tamanhoArquivo = dadosArquivo != null ? dadosArquivo.length : 0;
        m.conteudo = "[Arquivo: " + nomeArquivo + " (" + formatarTamanho(m.tamanhoArquivo) + ")]";
        m.momento = Instant.now();
        return m;
    }

    public static MensagemDTO sistema(String conteudo, String idDestino, boolean ehGrupo) {
        MensagemDTO m = new MensagemDTO();
        m.id = UUID.randomUUID().toString();
        m.nomeRemetente = "SYSTEM";
        m.nomeExibicaoRemetente = "Sistema";
        m.idDestino = idDestino;
        m.ehGrupo = ehGrupo;
        m.conteudo = conteudo;
        m.tipo = Tipo.SISTEMA;
        m.momento = Instant.now();
        return m;
    }

    private static String formatarTamanho(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }

    // Getters e Setters
    public String getId()                          { return id; }
    public String getNomeRemetente()               { return nomeRemetente; }
    public String getNomeExibicaoRemetente()       { return nomeExibicaoRemetente; }
    public String getConteudo()                    { return conteudo; }
    public void setConteudo(String c)              { this.conteudo = c; }
    public Tipo getTipo()                          { return tipo; }
    public Instant getMomento()                    { return momento; }
    public String getIdDestino()                   { return idDestino; }
    public boolean isEhGrupo()                     { return ehGrupo; }
    public String getNomeArquivo()                 { return nomeArquivo; }
    public byte[] getDadosArquivo()                { return dadosArquivo; }
    public long getTamanhoArquivo()                { return tamanhoArquivo; }
}
