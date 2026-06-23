package whatsut.server;

import at.favre.lib.crypto.bcrypt.BCrypt;
import whatsut.common.*;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Implementação do serviço RMI de chat.
 *
 * Design:
 * - Usuários e grupos armazenados em memória (ConcurrentHashMap — thread-safe).
 * - Callbacks disparados em thread pool separada para não bloquear o chamador RMI.
 * - BCrypt para hash de senhas (fator de custo 12).
 * - Heartbeat monitor verifica clientes mortos a cada 60s.
 */
public class ServicoChatImpl extends UnicastRemoteObject implements ServicoChat {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(ServicoChatImpl.class.getName());

    // Armazenamento em memória
    private final ConcurrentHashMap<String, UsuarioServidor> usuarios = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, GrupoServidor>   grupos   = new ConcurrentHashMap<>();
    // Histórico de mensagens privadas: chave = "usuario1:usuario2" (ordenado lexicograficamente)
    private final ConcurrentHashMap<String, LinkedList<MensagemDTO>> historicosPrivados
            = new ConcurrentHashMap<>();

    private ConfiguracaoServidor configuracao = new ConfiguracaoServidor();
    private final Instant horaInicio = Instant.now();
    private int totalMensagens = 0;
    private long totalBytes    = 0;

    // Thread pool para disparar callbacks sem bloquear o chamador
    private final ExecutorService executorRetornoChamada =
            Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "trabalhador-retorno-chamada");
                t.setDaemon(true);
                return t;
            });

    // Heartbeat monitor
    private final ScheduledExecutorService agendador =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "monitor-heartbeat");
                t.setDaemon(true);
                return t;
            });

    public ServicoChatImpl() throws RemoteException {
        super();
        // Cria usuário admin padrão
        criarUsuarioAdmin();
        // Inicia monitor de heartbeat (verifica a cada 60s)
        agendador.scheduleAtFixedRate(this::verificarHeartbeats, 60, 60,
                TimeUnit.SECONDS);
        log.info("ServicoChat iniciado com sucesso.");
    }

    private void criarUsuarioAdmin() {
        String hash = BCrypt.withDefaults().hashToString(12, "admin123".toCharArray());
        UsuarioServidor admin = new UsuarioServidor("admin", "Administrador", hash);
        admin.setAdminServidor(true);
        usuarios.put("admin", admin);
        log.info("Usuário admin criado (senha padrão: admin123)");
    }

    // ── Autenticação ─────────────────────────────────────────────────────────

    @Override
    public synchronized boolean cadastrar(String nomeUsuario, String nomeExibicao,
                                           String senha) throws RemoteException {
        if (nomeUsuario == null || nomeUsuario.isBlank() || senha == null
                || senha.length() < 4) {
            return false;
        }
        if (usuarios.containsKey(nomeUsuario.toLowerCase())) return false;

        String hash = BCrypt.withDefaults().hashToString(12, senha.toCharArray());
        UsuarioServidor usuario = new UsuarioServidor(nomeUsuario.toLowerCase(), nomeExibicao, hash);
        usuarios.put(nomeUsuario.toLowerCase(), usuario);
        log.info("Novo usuário registrado: " + nomeUsuario);

        // Notifica todos os online sobre novo usuário
        transmitirListaUsuarios();
        return true;
    }

    @Override
    public UsuarioDTO entrar(String nomeUsuario, String senha,
                              RetornoChamadaCliente retornoChamada) throws RemoteException {
        UsuarioServidor usuario = usuarios.get(nomeUsuario.toLowerCase());
        if (usuario == null) return null;
        if (usuario.isBanido()) {
            log.warning("Login bloqueado — usuário banido: " + nomeUsuario);
            return null;
        }

        BCrypt.Result resultado = BCrypt.verifyer()
                .verify(senha.toCharArray(), usuario.getHashSenha());
        if (!resultado.verified) return null;

        usuario.setRetornoChamada(retornoChamada);
        usuario.setStatus(StatusUsuario.ONLINE);
        usuario.setUltimoAcesso(Instant.now());
        usuario.setUltimoHeartbeat(Instant.now());

        log.info("Login: " + nomeUsuario);

        // Notifica todos sobre a mudança de status
        transmitirMudancaStatus(nomeUsuario, StatusUsuario.ONLINE);
        transmitirListaUsuarios();

        return usuario.paraDTO();
    }

    @Override
    public void sair(String nomeUsuario) throws RemoteException {
        UsuarioServidor usuario = usuarios.get(nomeUsuario);
        if (usuario == null) return;
        usuario.setRetornoChamada(null);
        usuario.setStatus(StatusUsuario.OFFLINE);
        usuario.setUltimoAcesso(Instant.now());
        log.info("Logout: " + nomeUsuario);
        transmitirMudancaStatus(nomeUsuario, StatusUsuario.OFFLINE);
        transmitirListaUsuarios();
    }

    @Override
    public boolean alterarSenha(String nomeUsuario, String senhaAtual,
                                 String novaSenha) throws RemoteException {
        UsuarioServidor usuario = usuarios.get(nomeUsuario);
        if (usuario == null) return false;
        BCrypt.Result r = BCrypt.verifyer()
                .verify(senhaAtual.toCharArray(), usuario.getHashSenha());
        if (!r.verified) return false;
        String novoHash = BCrypt.withDefaults().hashToString(12, novaSenha.toCharArray());
        usuario.setHashSenha(novoHash);
        return true;
    }

    // ── Presença ──────────────────────────────────────────────────────────────

    @Override
    public List<UsuarioDTO> obterUsuarios(String usuarioSolicitante) throws RemoteException {
        List<UsuarioDTO> lista = new ArrayList<>();
        for (UsuarioServidor u : usuarios.values()) {
            if (!u.isBanido()) lista.add(u.paraDTO());
        }
        lista.sort(Comparator.comparing(UsuarioDTO::getNomeExibicao));
        return lista;
    }

    @Override
    public void definirStatus(String nomeUsuario, StatusUsuario status) throws RemoteException {
        UsuarioServidor usuario = usuarios.get(nomeUsuario);
        if (usuario != null) {
            usuario.setStatus(status);
            transmitirMudancaStatus(nomeUsuario, status);
        }
    }

    // ── Chat privado ──────────────────────────────────────────────────────────

    @Override
    public void enviarMensagemPrivada(String deUsuario, String paraUsuario,
                                       MensagemDTO mensagem) throws RemoteException {
        UsuarioServidor alvo = usuarios.get(paraUsuario);
        armazenarMensagemPrivada(deUsuario, paraUsuario, mensagem);
        totalMensagens++;

        if (alvo != null && alvo.estaOnline()) {
            dispararRetornoChamada(() -> {
                try { alvo.getRetornoChamada().aoReceberMensagem(mensagem); }
                catch (RemoteException e) { tratarClienteMorto(paraUsuario); }
            });
        }
    }

    @Override
    public void enviarArquivo(String deUsuario, String paraUsuario,
                               MensagemDTO mensagemArquivo) throws RemoteException {
        if (!configuracao.isPermitirTransferenciaArquivos()) {
            throw new RemoteException("Transferência de arquivos desabilitada pelo servidor.");
        }
        if (mensagemArquivo.getDadosArquivo() != null
                && mensagemArquivo.getDadosArquivo().length > configuracao.getTamanhoMaxArquivo()) {
            throw new RemoteException("Arquivo excede o tamanho máximo permitido ("
                    + configuracao.getTamanhoMaxArquivo() / (1024 * 1024) + " MB).");
        }

        UsuarioServidor alvo = usuarios.get(paraUsuario);
        totalBytes += mensagemArquivo.getTamanhoArquivo();

        if (alvo != null && alvo.estaOnline()) {
            dispararRetornoChamada(() -> {
                try { alvo.getRetornoChamada().aoReceberArquivo(mensagemArquivo); }
                catch (RemoteException e) { tratarClienteMorto(paraUsuario); }
            });
        }
    }

    @Override
    public List<MensagemDTO> obterHistoricoPrivado(String usuario1, String usuario2,
                                                    int limite) throws RemoteException {
        String chave = chaveHistorico(usuario1, usuario2);
        LinkedList<MensagemDTO> historico = historicosPrivados.getOrDefault(chave, new LinkedList<>());
        int inicio = Math.max(0, historico.size() - limite);
        return new ArrayList<>(new ArrayList<>(historico).subList(inicio, historico.size()));
    }

    // ── Grupos ────────────────────────────────────────────────────────────────

    @Override
    public List<GrupoDTO> obterGrupos(String usuarioSolicitante) throws RemoteException {
        List<GrupoDTO> lista = new ArrayList<>();
        for (GrupoServidor g : grupos.values()) lista.add(g.paraDTO());
        lista.sort(Comparator.comparing(GrupoDTO::getNome));
        return lista;
    }

    @Override
    public GrupoDTO criarGrupo(String nomeUsuarioCriador, String nomeGrupo,
                                String descricao, boolean grupoPrivado,
                                GrupoDTO.AoAdminSair aoAdminSair) throws RemoteException {
        String id = UUID.randomUUID().toString();
        GrupoServidor grupo = new GrupoServidor(id, nomeGrupo, nomeUsuarioCriador,
                grupoPrivado, aoAdminSair);
        grupo.setDescricao(descricao);
        grupos.put(id, grupo);

        UsuarioServidor criador = usuarios.get(nomeUsuarioCriador);
        if (criador != null) criador.getIdsGrupos().add(id);

        log.info("Grupo criado: " + nomeGrupo + " por " + nomeUsuarioCriador);
        transmitirListaGrupos();
        return grupo.paraDTO();
    }

    @Override
    public void solicitarEntradaGrupo(String nomeUsuario, String idGrupo) throws RemoteException {
        GrupoServidor grupo = grupos.get(idGrupo);
        if (grupo == null) throw new RemoteException("Grupo não encontrado.");
        if (grupo.getNomesMembros().contains(nomeUsuario)) return;

        if (!grupo.isGrupoPrivado()) {
            // Grupo público: entra diretamente
            adicionarMembroAoGrupo(nomeUsuario, grupo);
        } else {
            // Grupo privado: enfileira pedido e notifica admin
            if (!grupo.getSolicitacoesPendentes().contains(nomeUsuario)) {
                grupo.getSolicitacoesPendentes().add(nomeUsuario);
            }
            UsuarioServidor admin = usuarios.get(grupo.getNomeUsuarioAdmin());
            UsuarioServidor solicitante = usuarios.get(nomeUsuario);
            if (admin != null && admin.estaOnline() && solicitante != null) {
                String exibicao = solicitante.getNomeExibicao();
                dispararRetornoChamada(() -> {
                    try {
                        admin.getRetornoChamada().aoReceberSolicitacao(
                                idGrupo, grupo.getNome(), nomeUsuario, exibicao);
                    } catch (RemoteException e) { tratarClienteMorto(admin.getNomeUsuario()); }
                });
            }
        }
    }

    @Override
    public void aprovarSolicitacao(String nomeUsuarioAdmin, String idGrupo,
                                    String nomeSolicitante) throws RemoteException {
        GrupoServidor grupo = grupos.get(idGrupo);
        if (grupo == null) return;
        if (!grupo.getNomeUsuarioAdmin().equals(nomeUsuarioAdmin)) return;

        grupo.getSolicitacoesPendentes().remove(nomeSolicitante);
        adicionarMembroAoGrupo(nomeSolicitante, grupo);

        UsuarioServidor solicitante = usuarios.get(nomeSolicitante);
        if (solicitante != null && solicitante.estaOnline()) {
            GrupoDTO dto = grupo.paraDTO();
            dispararRetornoChamada(() -> {
                try { solicitante.getRetornoChamada().aoAprovarSolicitacao(dto); }
                catch (RemoteException e) { tratarClienteMorto(nomeSolicitante); }
            });
        }
    }

    @Override
    public void negarSolicitacao(String nomeUsuarioAdmin, String idGrupo,
                                  String nomeSolicitante) throws RemoteException {
        GrupoServidor grupo = grupos.get(idGrupo);
        if (grupo == null) return;
        if (!grupo.getNomeUsuarioAdmin().equals(nomeUsuarioAdmin)) return;

        grupo.getSolicitacoesPendentes().remove(nomeSolicitante);

        UsuarioServidor solicitante = usuarios.get(nomeSolicitante);
        if (solicitante != null && solicitante.estaOnline()) {
            dispararRetornoChamada(() -> {
                try { solicitante.getRetornoChamada().aoNegarSolicitacao(idGrupo, grupo.getNome()); }
                catch (RemoteException e) { tratarClienteMorto(nomeSolicitante); }
            });
        }
    }

    @Override
    public void enviarMensagemGrupo(String deUsuario, String idGrupo,
                                     MensagemDTO mensagem) throws RemoteException {
        GrupoServidor grupo = grupos.get(idGrupo);
        if (grupo == null) return;
        if (!grupo.getNomesMembros().contains(deUsuario)) return;

        grupo.adicionarMensagem(mensagem);
        totalMensagens++;

        // Entrega a todos os membros (exceto o remetente)
        for (String membro : grupo.getNomesMembros()) {
            if (membro.equals(deUsuario)) continue;
            UsuarioServidor u = usuarios.get(membro);
            if (u != null && u.estaOnline()) {
                dispararRetornoChamada(() -> {
                    try { u.getRetornoChamada().aoReceberMensagem(mensagem); }
                    catch (RemoteException e) { tratarClienteMorto(u.getNomeUsuario()); }
                });
            }
        }
    }

    @Override
    public List<MensagemDTO> obterHistoricoGrupo(String idGrupo, int limite)
            throws RemoteException {
        GrupoServidor grupo = grupos.get(idGrupo);
        if (grupo == null) return Collections.emptyList();
        return grupo.obterHistorico(limite);
    }

    @Override
    public void sairDoGrupo(String nomeUsuario, String idGrupo) throws RemoteException {
        GrupoServidor grupo = grupos.get(idGrupo);
        if (grupo == null) return;

        if (grupo.getNomeUsuarioAdmin().equals(nomeUsuario)) {
            tratarSaidaDoAdmin(grupo);
        } else {
            grupo.getNomesMembros().remove(nomeUsuario);
            UsuarioServidor u = usuarios.get(nomeUsuario);
            if (u != null) u.getIdsGrupos().remove(idGrupo);

            MensagemDTO msgSis = MensagemDTO.sistema(
                    usuarios.get(nomeUsuario) != null
                            ? usuarios.get(nomeUsuario).getNomeExibicao() + " saiu do grupo."
                            : nomeUsuario + " saiu do grupo.",
                    idGrupo, true);
            grupo.adicionarMensagem(msgSis);
            transmitirParaGrupo(grupo, msgSis, nomeUsuario);
            transmitirAtualizacaoGrupo(grupo);
        }
    }

    @Override
    public void banirDoGrupo(String nomeUsuarioAdmin, String idGrupo,
                              String nomeAlvo) throws RemoteException {
        GrupoServidor grupo = grupos.get(idGrupo);
        if (grupo == null) return;
        if (!grupo.getNomeUsuarioAdmin().equals(nomeUsuarioAdmin)) return;

        grupo.getNomesMembros().remove(nomeAlvo);
        UsuarioServidor alvo = usuarios.get(nomeAlvo);
        if (alvo != null) {
            alvo.getIdsGrupos().remove(idGrupo);
            if (alvo.estaOnline()) {
                dispararRetornoChamada(() -> {
                    try {
                        alvo.getRetornoChamada().aoSairDoGrupo(idGrupo,
                                "Você foi banido do grupo pelo administrador.");
                    } catch (RemoteException e) { tratarClienteMorto(nomeAlvo); }
                });
            }
        }

        MensagemDTO msgSis = MensagemDTO.sistema(
                (alvo != null ? alvo.getNomeExibicao() : nomeAlvo)
                        + " foi removido do grupo.", idGrupo, true);
        grupo.adicionarMensagem(msgSis);
        transmitirParaGrupo(grupo, msgSis, null);
        transmitirAtualizacaoGrupo(grupo);
    }

    @Override
    public void transferirAdminGrupo(String adminAtual, String idGrupo,
                                       String novoAdmin) throws RemoteException {
        GrupoServidor grupo = grupos.get(idGrupo);
        if (grupo == null) return;
        if (!grupo.getNomeUsuarioAdmin().equals(adminAtual)) return;
        if (!grupo.getNomesMembros().contains(novoAdmin)) return;

        grupo.setNomeUsuarioAdmin(novoAdmin);
        UsuarioServidor novoAdminUsuario = usuarios.get(novoAdmin);
        String exibicao = novoAdminUsuario != null ? novoAdminUsuario.getNomeExibicao() : novoAdmin;
        MensagemDTO msgSis = MensagemDTO.sistema(
                exibicao + " é o novo administrador do grupo.", idGrupo, true);
        grupo.adicionarMensagem(msgSis);
        transmitirParaGrupo(grupo, msgSis, null);
        transmitirAtualizacaoGrupo(grupo);
    }

    @Override
    public void deletarGrupo(String nomeUsuarioAdmin, String idGrupo) throws RemoteException {
        GrupoServidor grupo = grupos.get(idGrupo);
        if (grupo == null) return;
        if (!grupo.getNomeUsuarioAdmin().equals(nomeUsuarioAdmin)) return;

        // Notifica todos os membros
        for (String membro : grupo.getNomesMembros()) {
            UsuarioServidor u = usuarios.get(membro);
            if (u != null && u.estaOnline()) {
                dispararRetornoChamada(() -> {
                    try {
                        u.getRetornoChamada().aoSairDoGrupo(idGrupo,
                                "O grupo foi encerrado pelo administrador.");
                    } catch (RemoteException e) { tratarClienteMorto(u.getNomeUsuario()); }
                });
            }
            if (u != null) u.getIdsGrupos().remove(idGrupo);
        }
        grupos.remove(idGrupo);
        transmitirListaGrupos();
    }

    // ── Moderação global ──────────────────────────────────────────────────────

    @Override
    public void banirUsuario(String nomeUsuarioAdmin, String nomeAlvo,
                              String motivo) throws RemoteException {
        UsuarioServidor admin = usuarios.get(nomeUsuarioAdmin);
        if (admin == null || !admin.isAdminServidor()) return;

        UsuarioServidor alvo = usuarios.get(nomeAlvo);
        if (alvo == null) return;

        alvo.setBanido(true);
        alvo.setMotivoBan(motivo);

        if (alvo.estaOnline()) {
            dispararRetornoChamada(() -> {
                try { alvo.getRetornoChamada().aoSerBanidoDoServidor(motivo); }
                catch (RemoteException e) { /* ignorar */ }
            });
            alvo.setRetornoChamada(null);
            alvo.setStatus(StatusUsuario.OFFLINE);
        }
        log.warning("Usuário banido: " + nomeAlvo + " | Motivo: " + motivo);
        transmitirListaUsuarios();
    }

    @Override
    public void desbanirUsuario(String nomeUsuarioAdmin, String nomeAlvo)
            throws RemoteException {
        UsuarioServidor admin = usuarios.get(nomeUsuarioAdmin);
        if (admin == null || !admin.isAdminServidor()) return;

        UsuarioServidor alvo = usuarios.get(nomeAlvo);
        if (alvo != null) {
            alvo.setBanido(false);
            alvo.setMotivoBan(null);
        }
        transmitirListaUsuarios();
    }

    @Override
    public List<UsuarioDTO> obterUsuariosBanidos(String nomeUsuarioAdmin) throws RemoteException {
        UsuarioServidor admin = usuarios.get(nomeUsuarioAdmin);
        if (admin == null || !admin.isAdminServidor()) return Collections.emptyList();

        List<UsuarioDTO> banidos = new ArrayList<>();
        for (UsuarioServidor u : usuarios.values()) {
            if (u.isBanido()) banidos.add(u.paraDTO());
        }
        return banidos;
    }

    // ── Configurações do servidor ─────────────────────────────────────────────

    @Override
    public ConfiguracaoServidor obterConfiguracao(String nomeUsuarioAdmin) throws RemoteException {
        UsuarioServidor admin = usuarios.get(nomeUsuarioAdmin);
        if (admin == null || !admin.isAdminServidor()) return null;
        return configuracao;
    }

    @Override
    public void atualizarConfiguracao(String nomeUsuarioAdmin, ConfiguracaoServidor novaConfiguracao)
            throws RemoteException {
        UsuarioServidor admin = usuarios.get(nomeUsuarioAdmin);
        if (admin == null || !admin.isAdminServidor()) return;
        this.configuracao = novaConfiguracao;
        log.info("Configurações do servidor atualizadas por " + nomeUsuarioAdmin);
    }

    @Override
    public EstatisticasServidor obterEstatisticas(String nomeUsuarioAdmin) throws RemoteException {
        UsuarioServidor admin = usuarios.get(nomeUsuarioAdmin);
        if (admin == null || !admin.isAdminServidor()) return null;

        EstatisticasServidor stats = new EstatisticasServidor();
        stats.setTotalUsuarios((int) usuarios.values().stream().filter(u -> !u.isBanido()).count());
        stats.setUsuariosOnline((int) usuarios.values().stream().filter(UsuarioServidor::estaOnline).count());
        stats.setTotalGrupos(grupos.size());
        stats.setMensagensEntregues(totalMensagens);
        stats.setBytesTransferidos(totalBytes);
        stats.setInicioServidor(horaInicio);
        stats.setUsuariosBanidos((int) usuarios.values().stream().filter(UsuarioServidor::isBanido).count());
        return stats;
    }

    // ── Heartbeat ─────────────────────────────────────────────────────────────

    @Override
    public void manterConectado(String nomeUsuario) throws RemoteException {
        UsuarioServidor u = usuarios.get(nomeUsuario);
        if (u != null) u.setUltimoHeartbeat(Instant.now());
    }

    // ── Helpers internos ──────────────────────────────────────────────────────

    private void adicionarMembroAoGrupo(String nomeUsuario, GrupoServidor grupo) {
        if (!grupo.getNomesMembros().contains(nomeUsuario)) {
            grupo.getNomesMembros().add(nomeUsuario);
        }
        UsuarioServidor u = usuarios.get(nomeUsuario);
        if (u != null && !u.getIdsGrupos().contains(grupo.getId()))
            u.getIdsGrupos().add(grupo.getId());

        // Mensagem de sistema para o grupo
        String exibicao = (u != null) ? u.getNomeExibicao() : nomeUsuario;
        MensagemDTO msgSis = MensagemDTO.sistema(exibicao + " entrou no grupo.",
                grupo.getId(), true);
        grupo.adicionarMensagem(msgSis);
        transmitirParaGrupo(grupo, msgSis, nomeUsuario);

        // Notifica o novo membro
        if (u != null && u.estaOnline()) {
            GrupoDTO dto = grupo.paraDTO();
            dispararRetornoChamada(() -> {
                try { u.getRetornoChamada().aoEntrarNoGrupo(dto); }
                catch (RemoteException e) { tratarClienteMorto(nomeUsuario); }
            });
        }
        transmitirAtualizacaoGrupo(grupo);
    }

    private void tratarSaidaDoAdmin(GrupoServidor grupo) {
        String novoAdmin = grupo.tratarSaidaDoAdmin();
        if (novoAdmin == null) {
            // Deletar grupo
            String idGrupo = grupo.getId();
            for (String membro : grupo.getNomesMembros()) {
                UsuarioServidor u = usuarios.get(membro);
                if (u != null) {
                    u.getIdsGrupos().remove(idGrupo);
                    if (u.estaOnline()) {
                        dispararRetornoChamada(() -> {
                            try {
                                u.getRetornoChamada().aoSairDoGrupo(idGrupo,
                                        "O grupo foi encerrado (admin saiu).");
                            } catch (RemoteException e) { /* ignorar */ }
                        });
                    }
                }
            }
            grupos.remove(idGrupo);
            transmitirListaGrupos();
        } else {
            // Novo admin promovido
            UsuarioServidor novoAdminUsuario = usuarios.get(novoAdmin);
            String exibicao = novoAdminUsuario != null ? novoAdminUsuario.getNomeExibicao() : novoAdmin;
            MensagemDTO msgSis = MensagemDTO.sistema(
                    "Admin anterior saiu. " + exibicao + " é o novo administrador.",
                    grupo.getId(), true);
            grupo.adicionarMensagem(msgSis);
            transmitirParaGrupo(grupo, msgSis, null);
            transmitirAtualizacaoGrupo(grupo);
        }
    }

    private void transmitirParaGrupo(GrupoServidor grupo, MensagemDTO msg, String usuarioExcluir) {
        for (String membro : grupo.getNomesMembros()) {
            if (membro.equals(usuarioExcluir)) continue;
            UsuarioServidor u = usuarios.get(membro);
            if (u != null && u.estaOnline()) {
                dispararRetornoChamada(() -> {
                    try { u.getRetornoChamada().aoReceberMensagem(msg); }
                    catch (RemoteException e) { tratarClienteMorto(u.getNomeUsuario()); }
                });
            }
        }
    }

    private void transmitirAtualizacaoGrupo(GrupoServidor grupo) {
        GrupoDTO dto = grupo.paraDTO();
        for (String membro : grupo.getNomesMembros()) {
            UsuarioServidor u = usuarios.get(membro);
            if (u != null && u.estaOnline()) {
                dispararRetornoChamada(() -> {
                    try { u.getRetornoChamada().aoAtualizarGrupo(dto); }
                    catch (RemoteException e) { tratarClienteMorto(u.getNomeUsuario()); }
                });
            }
        }
    }

    private void transmitirListaGrupos() {
        // Atualiza lista de grupos para todos os online
        for (UsuarioServidor u : usuarios.values()) {
            if (u.estaOnline()) {
                try {
                    List<GrupoDTO> listaGrupos = obterGrupos(u.getNomeUsuario());
                    // Reutiliza aoAtualizarGrupo com null para indicar "lista atualizada"
                    // ou usa aoReceberMensagem com mensagem de sistema
                } catch (RemoteException e) { /* ignorar */ }
            }
        }
    }

    private void transmitirListaUsuarios() {
        List<UsuarioDTO> lista;
        try { lista = obterUsuarios("system"); }
        catch (RemoteException e) { return; }

        for (UsuarioServidor u : usuarios.values()) {
            if (u.estaOnline()) {
                List<UsuarioDTO> listaFinal = lista;
                dispararRetornoChamada(() -> {
                    try { u.getRetornoChamada().aoAtualizarListaUsuarios(listaFinal); }
                    catch (RemoteException e2) { tratarClienteMorto(u.getNomeUsuario()); }
                });
            }
        }
    }

    private void transmitirMudancaStatus(String nomeUsuario, StatusUsuario status) {
        for (UsuarioServidor u : usuarios.values()) {
            if (u.estaOnline() && !u.getNomeUsuario().equals(nomeUsuario)) {
                dispararRetornoChamada(() -> {
                    try { u.getRetornoChamada().aoMudarStatusUsuario(nomeUsuario, status); }
                    catch (RemoteException e) { tratarClienteMorto(u.getNomeUsuario()); }
                });
            }
        }
    }

    private void armazenarMensagemPrivada(String usuario1, String usuario2, MensagemDTO msg) {
        String chave = chaveHistorico(usuario1, usuario2);
        historicosPrivados.computeIfAbsent(chave, k -> new LinkedList<>()).addLast(msg);
        // Manter limite
        LinkedList<MensagemDTO> h = historicosPrivados.get(chave);
        while (h.size() > configuracao.getLimiteHistoricoMensagens()) h.removeFirst();
    }

    private String chaveHistorico(String a, String b) {
        return a.compareTo(b) < 0 ? a + ":" + b : b + ":" + a;
    }

    private void dispararRetornoChamada(Runnable r) {
        executorRetornoChamada.submit(r);
    }

    private void tratarClienteMorto(String nomeUsuario) {
        UsuarioServidor u = usuarios.get(nomeUsuario);
        if (u == null) return;
        log.warning("Cliente morto detectado: " + nomeUsuario + ". Fazendo logout forçado.");
        u.setRetornoChamada(null);
        u.setStatus(StatusUsuario.OFFLINE);
        u.setUltimoAcesso(Instant.now());
        transmitirMudancaStatus(nomeUsuario, StatusUsuario.OFFLINE);
        transmitirListaUsuarios();
    }

    private void verificarHeartbeats() {
        Instant limite = Instant.now().minusSeconds(
                configuracao.getTimeoutSessaoMinutos() * 60L);
        for (UsuarioServidor u : usuarios.values()) {
            if (u.estaOnline() && u.getUltimoHeartbeat() != null
                    && u.getUltimoHeartbeat().isBefore(limite)) {
                log.info("Sessão expirada por inatividade: " + u.getNomeUsuario());
                // Testa se o callback está vivo
                try { u.getRetornoChamada().verificar(); }
                catch (RemoteException e) { tratarClienteMorto(u.getNomeUsuario()); }
            }
        }
    }

    // Acesso ao mapa interno para a UI do servidor
    public ConcurrentHashMap<String, UsuarioServidor> getMapaUsuarios()  { return usuarios; }
    public ConcurrentHashMap<String, GrupoServidor>   getMapaGrupos()    { return grupos; }
    public ConfiguracaoServidor getConfiguracao() { return configuracao; }
}
