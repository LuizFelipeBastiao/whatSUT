# WhatsUT — Sistema de Comunicação Interpessoal com RMI Java

> Trabalho Acadêmico — UTFPR  
> Protocolo: Java RMI | Criptografia: BCrypt | UI: Swing dark theme

---

## Arquitetura

```
whatsut/
├── common/           ← DTOs e interfaces RMI (compartilhadas)
│   ├── ChatService.java        ← Interface principal do servidor
│   ├── ClientCallback.java     ← Interface de callback (push → cliente)
│   ├── MessageDTO.java
│   ├── UserDTO.java / UserStatus.java
│   ├── GroupDTO.java
│   ├── ServerConfig.java
│   └── ServerStats.java
├── server/           ← Implementação do servidor
│   ├── ChatServiceImpl.java    ← Implementação da interface RMI
│   ├── ServerUser.java         ← Modelo interno (contém hash BCrypt)
│   ├── ServerGroup.java        ← Modelo interno de grupo
│   ├── ServerAdminUI.java      ← Interface gráfica de administração
│   └── ServerMain.java         ← Ponto de entrada do servidor
└── client/           ← Aplicação cliente
    ├── ChatSession.java        ← Gerencia conexão RMI e sessão
    ├── ClientCallbackImpl.java ← Implementação do callback (recebe push)
    ├── ClientMain.java         ← Janela principal (troca Login↔Chat)
    └── ui/
        ├── LoginPanel.java     ← Tela de login/cadastro
        └── ChatPanel.java      ← Tela principal de chat
```

---

## Requisitos atendidos

| # | Requisito | Implementação |
|---|-----------|---------------|
| 1 | **Autenticação criptografada** | BCrypt (fator 12) em `ChatServiceImpl.register()` e `login()` |
| 2 | **Lista de usuários** | `getUsers()` + callback `onUserListUpdated()` em tempo real |
| 3 | **Lista de grupos** | `getGroups()` + painel lateral na `ChatPanel` |
| 3b| **Aprovação pelo criador** | Fluxo `requestJoinGroup → onJoinRequest → approveJoinRequest` via callback |
| 4 | **Chat privado** | `sendPrivateMessage()` com histórico |
| 4 | **Chat em grupo** | `sendGroupMessage()` com broadcast para todos os membros |
| 5 | **Envio de arquivos** | `sendFile()` + `onFileReceived()` com dialog de salvamento |
| 6 | **Ban de usuário** | `banUser()` (admin servidor) e `banFromGroup()` (admin grupo) |
| 6 | **Saída do admin** | Políticas: promover mais antigo, aleatório ou deletar grupo |
| ★ | **RMI Callbacks** | `ClientCallback` chamada pelo servidor para todos os eventos |
| ★ | **Interface admin servidor** | `ServerAdminUI` com abas: Usuários, Grupos, Config, Stats, Log |
| ★ | **Configuração do servidor** | `ServerConfig` + `getServerConfig/updateServerConfig` via RMI |

---

## Como compilar e executar

### Pré-requisitos
- Java 17+
- Maven 3.6+

### Build
```bash
cd whatsut
mvn clean package
```

### Servidor
```bash
# Inicia o rmiregistry + servidor + painel admin
java -cp target/whatsut-server.jar whatsut.server.ServerMain

# Credenciais padrão do admin:
#   usuário: admin
#   senha:   admin123
```

### Cliente (múltiplas instâncias)
```bash
java -cp target/whatsut-server.jar whatsut.client.ClientMain
```

---

## Pattern RMI Callback — detalhe técnico

O padrão **Observer sobre RMI** é o coração do sistema:

```
Cliente A                  Servidor                    Cliente B
    |                          |                            |
    |-- login(user,pass,cb) -->|                            |
    |                          |-- cb.ping() ----------->  |  ← heartbeat
    |                          |                            |
    |-- sendPrivateMessage() ->|                            |
    |                          |-- cb.onMessageReceived() ->|  ← callback!
    |                          |                            |
```

1. No login, o cliente passa sua referência remota `ClientCallbackImpl` ao servidor.  
2. O servidor armazena essa referência em `ServerUser.callback`.  
3. Para qualquer evento (mensagem, atualização de grupo, ban...) o servidor chama o método na referência remota do cliente.  
4. O cliente recebe a chamada em uma thread separada e atualiza a UI via `SwingUtilities.invokeLater()`.

---

## Diagramas UML

Gerados inline como SVG interativo:
- **Diagrama de Sequência**: fluxo login, chat, callback, arquivo
- **Diagrama de Atividades**: autenticação, tipos de chat, grupos, logout
- **Diagrama de Colaboração**: objetos e interfaces do sistema

---

## Criptografia

Implementada com **BCrypt** (biblioteca `at.favre.lib:bcrypt`):

```java
// Cadastro — gera hash da senha
String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());

// Login — verifica sem comparar strings
BCrypt.Result r = BCrypt.verifyer().verify(password.toCharArray(), user.getPasswordHash());
if (!r.verified) return null;
```

- Fator de custo **12** (~300ms de CPU, resiste a ataques de dicionário)
- A senha **nunca** é armazenada em texto puro
- O hash é específico por usuário (salt embutido no hash BCrypt)
