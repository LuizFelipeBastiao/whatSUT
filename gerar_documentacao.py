"""
Gera um PDF de documentação técnica do WhatsUT
com diagramas inline desenhados via reportlab.
"""

from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import cm, mm
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY, TA_LEFT
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, PageBreak, Table, TableStyle,
    KeepTogether, Image
)
from reportlab.platypus.flowables import Flowable
from reportlab.graphics.shapes import (
    Drawing, Rect, String, Line, Polygon, Circle, Group
)
from reportlab.pdfgen.canvas import Canvas


# ════════════════════════════════════════════════════════════════
# Paleta de cores
# ════════════════════════════════════════════════════════════════
COR_FUNDO    = colors.HexColor("#FFFFFF")
COR_TITULO   = colors.HexColor("#1A1A2E")
COR_DESTAQUE = colors.HexColor("#6D28D9")
COR_DESTAQUE2= colors.HexColor("#EC4899")
COR_TEXTO    = colors.HexColor("#1F2937")
COR_SUBTEXTO = colors.HexColor("#6B7280")
COR_CARTAO   = colors.HexColor("#F3F4F6")
COR_BORDA    = colors.HexColor("#E5E7EB")
COR_CLIENTE  = colors.HexColor("#3B82F6")
COR_SERVIDOR = colors.HexColor("#10B981")
COR_REDE     = colors.HexColor("#F59E0B")


# ════════════════════════════════════════════════════════════════
# Estilos de texto
# ════════════════════════════════════════════════════════════════
styles = getSampleStyleSheet()

style_titulo_capa = ParagraphStyle(
    "TituloCapa", parent=styles["Title"],
    fontSize=36, leading=42, textColor=COR_TITULO,
    alignment=TA_CENTER, spaceAfter=10
)
style_subtitulo_capa = ParagraphStyle(
    "SubtituloCapa", parent=styles["Normal"],
    fontSize=16, leading=22, textColor=COR_DESTAQUE,
    alignment=TA_CENTER, spaceAfter=4
)
style_metadata_capa = ParagraphStyle(
    "MetaCapa", parent=styles["Normal"],
    fontSize=12, leading=18, textColor=COR_SUBTEXTO,
    alignment=TA_CENTER
)
style_h1 = ParagraphStyle(
    "H1", parent=styles["Heading1"],
    fontSize=22, leading=28, textColor=COR_TITULO,
    spaceBefore=12, spaceAfter=12, fontName="Helvetica-Bold"
)
style_h2 = ParagraphStyle(
    "H2", parent=styles["Heading2"],
    fontSize=16, leading=22, textColor=COR_DESTAQUE,
    spaceBefore=10, spaceAfter=8, fontName="Helvetica-Bold"
)
style_h3 = ParagraphStyle(
    "H3", parent=styles["Heading3"],
    fontSize=13, leading=18, textColor=COR_TITULO,
    spaceBefore=8, spaceAfter=4, fontName="Helvetica-Bold"
)
style_paragrafo = ParagraphStyle(
    "Paragrafo", parent=styles["Normal"],
    fontSize=10.5, leading=15, textColor=COR_TEXTO,
    alignment=TA_JUSTIFY, spaceAfter=6
)
style_lista = ParagraphStyle(
    "Lista", parent=style_paragrafo,
    leftIndent=14, bulletIndent=4, spaceAfter=3
)
style_codigo = ParagraphStyle(
    "Codigo", parent=styles["Code"],
    fontSize=9, leading=12, textColor=COR_TEXTO,
    backColor=COR_CARTAO, borderColor=COR_BORDA, borderWidth=0.5,
    borderPadding=6, leftIndent=4, rightIndent=4,
    spaceBefore=4, spaceAfter=8, fontName="Courier"
)
style_legenda = ParagraphStyle(
    "Legenda", parent=styles["Normal"],
    fontSize=9, leading=12, textColor=COR_SUBTEXTO,
    alignment=TA_CENTER, spaceAfter=10, fontName="Helvetica-Oblique"
)


# ════════════════════════════════════════════════════════════════
# DIAGRAMA 1 — Arquitetura geral (cliente / rede / servidor)
# ════════════════════════════════════════════════════════════════
def diagrama_arquitetura():
    d = Drawing(450, 240)

    # ─── Caixa Cliente A ─────────────────────────
    d.add(Rect(10, 80, 110, 130, fillColor=COR_CLIENTE,
               strokeColor=COR_TITULO, strokeWidth=1.5, rx=8, ry=8))
    d.add(String(65, 195, "Cliente A", textAnchor="middle",
                 fontName="Helvetica-Bold", fontSize=11, fillColor=colors.white))
    d.add(String(65, 175, "PrincipalCliente", textAnchor="middle",
                 fontName="Courier", fontSize=8, fillColor=colors.white))
    d.add(String(65, 160, "SessaoChat", textAnchor="middle",
                 fontName="Courier", fontSize=8, fillColor=colors.white))
    d.add(String(65, 145, "PainelChat", textAnchor="middle",
                 fontName="Courier", fontSize=8, fillColor=colors.white))
    d.add(Rect(20, 100, 90, 30, fillColor=colors.white,
               strokeColor=colors.white, rx=4, ry=4))
    d.add(String(65, 117, "Callback", textAnchor="middle",
                 fontName="Helvetica-Bold", fontSize=8, fillColor=COR_CLIENTE))
    d.add(String(65, 105, "(objeto remoto)", textAnchor="middle",
                 fontName="Helvetica", fontSize=7, fillColor=COR_SUBTEXTO))

    # ─── Caixa Cliente B ─────────────────────────
    d.add(Rect(10, 10, 110, 60, fillColor=COR_CLIENTE,
               strokeColor=COR_TITULO, strokeWidth=1.5, rx=8, ry=8))
    d.add(String(65, 55, "Cliente B", textAnchor="middle",
                 fontName="Helvetica-Bold", fontSize=11, fillColor=colors.white))
    d.add(String(65, 38, "(outra instância)", textAnchor="middle",
                 fontName="Helvetica", fontSize=8, fillColor=colors.white))
    d.add(String(65, 22, "mesmo código", textAnchor="middle",
                 fontName="Helvetica", fontSize=8, fillColor=colors.white))

    # ─── Caixa Rede / RMI ─────────────────────────
    d.add(Rect(160, 60, 130, 150, fillColor=COR_REDE,
               strokeColor=COR_TITULO, strokeWidth=1.5, rx=8, ry=8))
    d.add(String(225, 195, "Java RMI", textAnchor="middle",
                 fontName="Helvetica-Bold", fontSize=12, fillColor=colors.white))
    d.add(String(225, 180, "(sobre TCP)", textAnchor="middle",
                 fontName="Helvetica", fontSize=8, fillColor=colors.white))
    # Linhas representando "rede"
    for i, y in enumerate([155, 140, 125, 110, 95]):
        d.add(Line(170, y, 280, y, strokeColor=colors.white,
                   strokeDashArray=[3, 2], strokeWidth=0.8))
    d.add(String(225, 80, "porta 1099", textAnchor="middle",
                 fontName="Courier", fontSize=9, fillColor=colors.white))
    d.add(String(225, 68, "rmiregistry", textAnchor="middle",
                 fontName="Courier", fontSize=9, fillColor=colors.white))

    # ─── Caixa Servidor ───────────────────────────
    d.add(Rect(330, 30, 115, 180, fillColor=COR_SERVIDOR,
               strokeColor=COR_TITULO, strokeWidth=1.5, rx=8, ry=8))
    d.add(String(387, 195, "Servidor", textAnchor="middle",
                 fontName="Helvetica-Bold", fontSize=12, fillColor=colors.white))
    d.add(String(387, 178, "PrincipalServidor", textAnchor="middle",
                 fontName="Courier", fontSize=7.5, fillColor=colors.white))
    d.add(String(387, 165, "ServicoChatImpl", textAnchor="middle",
                 fontName="Courier", fontSize=7.5, fillColor=colors.white))
    # mini caixas internas
    d.add(Rect(340, 110, 95, 45, fillColor=colors.white, strokeColor=colors.white, rx=3, ry=3))
    d.add(String(387, 142, "ConcurrentHashMap", textAnchor="middle",
                 fontName="Helvetica-Bold", fontSize=7.5, fillColor=COR_SERVIDOR))
    d.add(String(387, 130, "usuarios", textAnchor="middle",
                 fontName="Courier", fontSize=7, fillColor=COR_TEXTO))
    d.add(String(387, 120, "grupos", textAnchor="middle",
                 fontName="Courier", fontSize=7, fillColor=COR_TEXTO))
    d.add(Rect(340, 50, 95, 50, fillColor=colors.white, strokeColor=colors.white, rx=3, ry=3))
    d.add(String(387, 88, "Thread Pools", textAnchor="middle",
                 fontName="Helvetica-Bold", fontSize=7.5, fillColor=COR_SERVIDOR))
    d.add(String(387, 76, "executorRetornoChamada", textAnchor="middle",
                 fontName="Courier", fontSize=6.5, fillColor=COR_TEXTO))
    d.add(String(387, 66, "agendador (heartbeat)", textAnchor="middle",
                 fontName="Courier", fontSize=6.5, fillColor=COR_TEXTO))

    # ─── Setas bidirecionais ──────────────────────
    # Cliente A <-> Rede
    d.add(Line(122, 145, 158, 145, strokeColor=COR_TITULO, strokeWidth=1.5))
    d.add(Polygon([158, 145, 152, 148, 152, 142],
                  fillColor=COR_TITULO, strokeColor=COR_TITULO))
    d.add(Polygon([122, 145, 128, 148, 128, 142],
                  fillColor=COR_TITULO, strokeColor=COR_TITULO))

    # Cliente B <-> Rede
    d.add(Line(122, 40, 158, 80, strokeColor=COR_TITULO, strokeWidth=1.5))
    d.add(Polygon([158, 80, 152, 82, 154, 76],
                  fillColor=COR_TITULO, strokeColor=COR_TITULO))

    # Rede <-> Servidor
    d.add(Line(292, 145, 328, 145, strokeColor=COR_TITULO, strokeWidth=1.5))
    d.add(Polygon([328, 145, 322, 148, 322, 142],
                  fillColor=COR_TITULO, strokeColor=COR_TITULO))
    d.add(Polygon([292, 145, 298, 148, 298, 142],
                  fillColor=COR_TITULO, strokeColor=COR_TITULO))

    return d


# ════════════════════════════════════════════════════════════════
# DIAGRAMA 2 — Sequência de mensagem em grupo
# ════════════════════════════════════════════════════════════════
def diagrama_sequencia_mensagem():
    d = Drawing(450, 360)

    # Cabeçalhos das 3 colunas (atores)
    atores = [
        (65,  "Cliente Alice", COR_CLIENTE),
        (225, "Servidor",      COR_SERVIDOR),
        (385, "Cliente Bob",   COR_CLIENTE),
    ]
    for x, nome, cor in atores:
        d.add(Rect(x - 50, 330, 100, 24, fillColor=cor,
                   strokeColor=COR_TITULO, strokeWidth=1, rx=4, ry=4))
        d.add(String(x, 338, nome, textAnchor="middle",
                     fontName="Helvetica-Bold", fontSize=10, fillColor=colors.white))
        # Linha vertical (life-line)
        d.add(Line(x, 25, x, 325, strokeColor=COR_SUBTEXTO,
                   strokeDashArray=[2, 2], strokeWidth=0.6))

    # Setas de mensagens (de cima para baixo)
    eventos = [
        # (y, x_inicio, x_fim, texto_principal, texto_secundario)
        (305, 65,  225, "1. enviarMensagemGrupo(\"oi\")", "via stub RMI"),
        (260, 225, 225, "2. valida e grava no histórico", "(executa no servidor)"),
        (215, 225, 225, "3. dispararRetornoChamada(...)", "submete ao ExecutorService"),
        (170, 225, 385, "4. aoReceberMensagem(msg)", "callback RMI"),
        (125, 385, 385, "5. SwingUtilities.invokeLater", "renderiza no Swing"),
        (75,  385, 385, "6. balão aparece na tela", ""),
    ]

    for y, xi, xf, texto, sub in eventos:
        if xi == xf:
            # Mesma coluna: caixa de processamento à direita
            d.add(Rect(xi + 6, y - 8, 8, 16, fillColor=COR_DESTAQUE,
                       strokeColor=COR_DESTAQUE))
            d.add(String(xi + 22, y + 1, texto, textAnchor="start",
                         fontName="Helvetica-Bold", fontSize=8.5, fillColor=COR_TITULO))
            if sub:
                d.add(String(xi + 22, y - 10, sub, textAnchor="start",
                             fontName="Helvetica-Oblique", fontSize=7.5, fillColor=COR_SUBTEXTO))
        else:
            d.add(Line(xi, y, xf, y, strokeColor=COR_TITULO, strokeWidth=1.2))
            if xf > xi:
                d.add(Polygon([xf, y, xf - 7, y + 4, xf - 7, y - 4],
                              fillColor=COR_TITULO, strokeColor=COR_TITULO))
                tx = (xi + xf) / 2
            else:
                d.add(Polygon([xf, y, xf + 7, y + 4, xf + 7, y - 4],
                              fillColor=COR_TITULO, strokeColor=COR_TITULO))
                tx = (xi + xf) / 2
            d.add(String(tx, y + 3, texto, textAnchor="middle",
                         fontName="Helvetica-Bold", fontSize=8.5, fillColor=COR_TITULO))
            if sub:
                d.add(String(tx, y - 9, sub, textAnchor="middle",
                             fontName="Helvetica-Oblique", fontSize=7.5, fillColor=COR_SUBTEXTO))

    return d


# ════════════════════════════════════════════════════════════════
# DIAGRAMA 3 — Padrão RMI Callback
# ════════════════════════════════════════════════════════════════
def diagrama_callback():
    d = Drawing(450, 230)

    # Cliente
    d.add(Rect(20, 80, 140, 110, fillColor=COR_CLIENTE,
               strokeColor=COR_TITULO, strokeWidth=1.5, rx=8, ry=8))
    d.add(String(90, 175, "Cliente", textAnchor="middle",
                 fontName="Helvetica-Bold", fontSize=12, fillColor=colors.white))
    d.add(Rect(30, 95, 120, 60, fillColor=colors.white,
               strokeColor=colors.white, rx=4, ry=4))
    d.add(String(90, 140, "RetornoChamadaCliente", textAnchor="middle",
                 fontName="Helvetica-Bold", fontSize=8, fillColor=COR_CLIENTE))
    d.add(String(90, 128, "Impl extends", textAnchor="middle",
                 fontName="Helvetica", fontSize=8, fillColor=COR_TEXTO))
    d.add(String(90, 116, "UnicastRemoteObject", textAnchor="middle",
                 fontName="Courier", fontSize=8, fillColor=COR_TEXTO))
    d.add(String(90, 104, "(É um objeto remoto!)", textAnchor="middle",
                 fontName="Helvetica-Oblique", fontSize=7, fillColor=COR_DESTAQUE))

    # Servidor
    d.add(Rect(290, 80, 140, 110, fillColor=COR_SERVIDOR,
               strokeColor=COR_TITULO, strokeWidth=1.5, rx=8, ry=8))
    d.add(String(360, 175, "Servidor", textAnchor="middle",
                 fontName="Helvetica-Bold", fontSize=12, fillColor=colors.white))
    d.add(Rect(300, 95, 120, 60, fillColor=colors.white,
               strokeColor=colors.white, rx=4, ry=4))
    d.add(String(360, 140, "UsuarioServidor", textAnchor="middle",
                 fontName="Helvetica-Bold", fontSize=8, fillColor=COR_SERVIDOR))
    d.add(String(360, 124, "guarda stub:", textAnchor="middle",
                 fontName="Helvetica", fontSize=8, fillColor=COR_TEXTO))
    d.add(String(360, 110, "retornoChamada", textAnchor="middle",
                 fontName="Courier", fontSize=8, fillColor=COR_TEXTO))

    # Seta 1 — Login
    d.add(Line(165, 180, 285, 180, strokeColor=COR_TITULO, strokeWidth=1.5))
    d.add(Polygon([285, 180, 278, 183, 278, 177],
                  fillColor=COR_TITULO, strokeColor=COR_TITULO))
    d.add(String(225, 195, "1. entrar(...) passa callback", textAnchor="middle",
                 fontName="Helvetica-Bold", fontSize=8.5, fillColor=COR_TITULO))
    d.add(String(225, 185, "como argumento RMI", textAnchor="middle",
                 fontName="Helvetica", fontSize=7.5, fillColor=COR_SUBTEXTO))

    # Seta 2 — Callback (servidor → cliente)
    d.add(Line(285, 60, 165, 60, strokeColor=COR_DESTAQUE2, strokeWidth=2))
    d.add(Polygon([165, 60, 172, 63, 172, 57],
                  fillColor=COR_DESTAQUE2, strokeColor=COR_DESTAQUE2))
    d.add(String(225, 70, "2. servidor chama métodos no cliente",
                 textAnchor="middle", fontName="Helvetica-Bold",
                 fontSize=8.5, fillColor=COR_DESTAQUE2))
    d.add(String(225, 48, "aoReceberMensagem, aoEntrarNoGrupo, ...",
                 textAnchor="middle", fontName="Courier",
                 fontSize=7.5, fillColor=COR_SUBTEXTO))
    d.add(String(225, 36, "(RMI ao contrário!)",
                 textAnchor="middle", fontName="Helvetica-Oblique",
                 fontSize=7, fillColor=COR_SUBTEXTO))

    # Caixa de destaque
    d.add(Rect(60, 5, 330, 24, fillColor=COR_CARTAO,
               strokeColor=COR_DESTAQUE, strokeWidth=1, rx=4, ry=4))
    d.add(String(225, 17, "Sem callback, o cliente teria que fazer polling — péssimo.",
                 textAnchor="middle", fontName="Helvetica-Bold",
                 fontSize=8.5, fillColor=COR_TITULO))
    d.add(String(225, 8, "Com callback, o servidor 'liga de volta' em tempo real.",
                 textAnchor="middle", fontName="Helvetica",
                 fontSize=7.5, fillColor=COR_SUBTEXTO))

    return d


# ════════════════════════════════════════════════════════════════
# DIAGRAMA 4 — Threads no servidor
# ════════════════════════════════════════════════════════════════
def diagrama_threads():
    d = Drawing(450, 320)

    d.add(String(225, 300, "Threads do servidor — quem faz o quê",
                 textAnchor="middle", fontName="Helvetica-Bold",
                 fontSize=12, fillColor=COR_TITULO))

    # Cartões de cada tipo de thread
    cartoes = [
        (15, 200, 130, 80, "Threads RMI (automáticas)",
         ["Criadas pelo Java", "1 por chamada remota", "ConcurrentHashMap evita race"]),
        (160, 200, 130, 80, "executorRetornoChamada",
         ["CachedThreadPool", "Dispara callbacks", "Não bloqueia o chamador"]),
        (305, 200, 130, 80, "agendador",
         ["SingleThreadScheduled", "Roda a cada 60s", "Mata clientes mortos"]),
    ]
    for x, y, w, h, titulo, bullets in cartoes:
        d.add(Rect(x, y, w, h, fillColor=COR_CARTAO,
                   strokeColor=COR_DESTAQUE, strokeWidth=1.5, rx=6, ry=6))
        d.add(String(x + w/2, y + h - 14, titulo, textAnchor="middle",
                     fontName="Helvetica-Bold", fontSize=8.5, fillColor=COR_DESTAQUE))
        for i, b in enumerate(bullets):
            d.add(String(x + 8, y + h - 32 - i * 13, "• " + b,
                         textAnchor="start", fontName="Helvetica",
                         fontSize=8, fillColor=COR_TEXTO))

    # Fluxo: sendGroupMessage
    d.add(String(225, 175, "Exemplo: alice envia mensagem ao grupo de 5 pessoas",
                 textAnchor="middle", fontName="Helvetica-Oblique",
                 fontSize=10, fillColor=COR_SUBTEXTO))

    # Quadradinhos de threads em ação
    # Thread RMI da alice
    d.add(Rect(20, 130, 100, 35, fillColor=COR_CLIENTE,
               strokeColor=COR_TITULO, rx=4, ry=4))
    d.add(String(70, 152, "Thread RMI", textAnchor="middle",
                 fontName="Helvetica-Bold", fontSize=8, fillColor=colors.white))
    d.add(String(70, 140, "(da alice)", textAnchor="middle",
                 fontName="Helvetica", fontSize=7.5, fillColor=colors.white))

    # Seta para o pool
    d.add(Line(125, 147, 165, 147, strokeColor=COR_TITULO, strokeWidth=1.2))
    d.add(Polygon([165, 147, 159, 150, 159, 144],
                  fillColor=COR_TITULO, strokeColor=COR_TITULO))
    d.add(String(145, 155, "submit", textAnchor="middle",
                 fontName="Courier", fontSize=7, fillColor=COR_SUBTEXTO))

    # Pool: 4 threads em paralelo
    for i in range(4):
        x = 170 + i * 28
        d.add(Rect(x, 130, 24, 35, fillColor=COR_DESTAQUE,
                   strokeColor=COR_TITULO, rx=3, ry=3))
        d.add(String(x + 12, 152, f"T{i+1}", textAnchor="middle",
                     fontName="Helvetica-Bold", fontSize=8, fillColor=colors.white))
        d.add(String(x + 12, 140, "msg", textAnchor="middle",
                     fontName="Helvetica", fontSize=7, fillColor=colors.white))

    # Setas para os clientes destino
    for i, nome in enumerate(["Bob", "Carol", "Dave", "Eve"]):
        x_origem = 170 + i * 28 + 12
        x_dest = 330 + (i % 2) * 55
        y_dest = 100 if i < 2 else 60
        d.add(Line(x_origem, 130, x_dest, y_dest + 15,
                   strokeColor=COR_DESTAQUE2, strokeWidth=0.8))
        d.add(Rect(x_dest - 22, y_dest, 55, 18, fillColor=COR_CLIENTE,
                   strokeColor=COR_TITULO, rx=3, ry=3))
        d.add(String(x_dest + 5, y_dest + 6, nome, textAnchor="middle",
                     fontName="Helvetica-Bold", fontSize=7.5, fillColor=colors.white))

    # Caixa de conclusão
    d.add(Rect(15, 10, 420, 30, fillColor=COR_CARTAO,
               strokeColor=COR_DESTAQUE, strokeWidth=1, rx=4, ry=4))
    d.add(String(225, 28, "Resultado: alice's sendGroupMessage retorna em milissegundos",
                 textAnchor="middle", fontName="Helvetica-Bold",
                 fontSize=8.5, fillColor=COR_TITULO))
    d.add(String(225, 16, "mesmo se um dos destinos estiver lento — os callbacks rolam em paralelo.",
                 textAnchor="middle", fontName="Helvetica",
                 fontSize=8, fillColor=COR_SUBTEXTO))

    return d


# ════════════════════════════════════════════════════════════════
# DIAGRAMA 5 — Estrutura de pacotes
# ════════════════════════════════════════════════════════════════
def diagrama_pacotes():
    d = Drawing(450, 260)

    # Pacote common (no centro/topo)
    d.add(Rect(155, 175, 140, 70, fillColor=COR_REDE,
               strokeColor=COR_TITULO, strokeWidth=1.5, rx=6, ry=6))
    d.add(String(225, 230, "whatsut.common", textAnchor="middle",
                 fontName="Helvetica-Bold", fontSize=10, fillColor=colors.white))
    d.add(String(225, 215, "ServicoChat (interface)", textAnchor="middle",
                 fontName="Courier", fontSize=7.5, fillColor=colors.white))
    d.add(String(225, 205, "RetornoChamadaCliente", textAnchor="middle",
                 fontName="Courier", fontSize=7.5, fillColor=colors.white))
    d.add(String(225, 195, "UsuarioDTO, GrupoDTO,", textAnchor="middle",
                 fontName="Courier", fontSize=7.5, fillColor=colors.white))
    d.add(String(225, 185, "MensagemDTO, ...", textAnchor="middle",
                 fontName="Courier", fontSize=7.5, fillColor=colors.white))

    # Pacote server (esquerda baixo)
    d.add(Rect(20, 30, 170, 110, fillColor=COR_SERVIDOR,
               strokeColor=COR_TITULO, strokeWidth=1.5, rx=6, ry=6))
    d.add(String(105, 125, "whatsut.server", textAnchor="middle",
                 fontName="Helvetica-Bold", fontSize=10, fillColor=colors.white))
    items_s = ["PrincipalServidor", "ServicoChatImpl", "UsuarioServidor",
               "GrupoServidor", "InterfaceAdminServidor"]
    for i, it in enumerate(items_s):
        d.add(String(105, 110 - i * 13, "• " + it, textAnchor="middle",
                     fontName="Courier", fontSize=8, fillColor=colors.white))

    # Pacote client (direita baixo)
    d.add(Rect(260, 30, 170, 110, fillColor=COR_CLIENTE,
               strokeColor=COR_TITULO, strokeWidth=1.5, rx=6, ry=6))
    d.add(String(345, 125, "whatsut.client", textAnchor="middle",
                 fontName="Helvetica-Bold", fontSize=10, fillColor=colors.white))
    items_c = ["PrincipalCliente", "SessaoChat",
               "RetornoChamadaClienteImpl", "ui.PainelChat", "ui.PainelLogin"]
    for i, it in enumerate(items_c):
        d.add(String(345, 110 - i * 13, "• " + it, textAnchor="middle",
                     fontName="Courier", fontSize=8, fillColor=colors.white))

    # Setas de dependência
    d.add(Line(160, 145, 190, 175, strokeColor=COR_TITULO,
               strokeWidth=1, strokeDashArray=[3, 2]))
    d.add(Polygon([190, 175, 184, 170, 188, 168],
                  fillColor=COR_TITULO, strokeColor=COR_TITULO))
    d.add(String(155, 162, "usa", textAnchor="middle",
                 fontName="Helvetica-Oblique", fontSize=7.5, fillColor=COR_SUBTEXTO))

    d.add(Line(290, 145, 260, 175, strokeColor=COR_TITULO,
               strokeWidth=1, strokeDashArray=[3, 2]))
    d.add(Polygon([260, 175, 262, 168, 266, 170],
                  fillColor=COR_TITULO, strokeColor=COR_TITULO))
    d.add(String(295, 162, "usa", textAnchor="middle",
                 fontName="Helvetica-Oblique", fontSize=7.5, fillColor=COR_SUBTEXTO))

    # Título inferior
    d.add(String(225, 12, "common é compartilhado entre cliente e servidor — é o 'contrato' RMI",
                 textAnchor="middle", fontName="Helvetica-Oblique",
                 fontSize=8.5, fillColor=COR_SUBTEXTO))

    return d


# ════════════════════════════════════════════════════════════════
# Helpers
# ════════════════════════════════════════════════════════════════
def p(texto):
    return Paragraph(texto, style_paragrafo)

def code(texto):
    # Escape HTML em código
    texto = texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    # Em reportlab Code, preserve quebras de linha com <br/>
    texto = texto.replace("\n", "<br/>")
    return Paragraph(texto, style_codigo)

def li(texto):
    return Paragraph("• " + texto, style_lista)


# ════════════════════════════════════════════════════════════════
# Cabeçalho e rodapé do PDF
# ════════════════════════════════════════════════════════════════
def cabecalho_rodape(canvas: Canvas, doc):
    canvas.saveState()

    # Não imprime cabeçalho na capa (página 1)
    if doc.page > 1:
        # Cabeçalho
        canvas.setStrokeColor(COR_BORDA)
        canvas.setLineWidth(0.5)
        canvas.line(2 * cm, A4[1] - 1.5 * cm, A4[0] - 2 * cm, A4[1] - 1.5 * cm)
        canvas.setFont("Helvetica", 8)
        canvas.setFillColor(COR_SUBTEXTO)
        canvas.drawString(2 * cm, A4[1] - 1.3 * cm,
                          "WhatsUT — Documentação Técnica")
        canvas.drawRightString(A4[0] - 2 * cm, A4[1] - 1.3 * cm,
                               "Sistemas Distribuídos — UTFPR")

    # Rodapé
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(COR_SUBTEXTO)
    canvas.drawCentredString(A4[0] / 2, 1.2 * cm, f"Página {doc.page}")
    canvas.restoreState()


# ════════════════════════════════════════════════════════════════
# Conteúdo do PDF
# ════════════════════════════════════════════════════════════════
def construir_conteudo():
    flow = []

    # ═══ CAPA ═══
    flow.append(Spacer(1, 5 * cm))
    flow.append(Paragraph("WhatsUT", style_titulo_capa))
    flow.append(Paragraph("Documentação Técnica do Sistema",
                          style_subtitulo_capa))
    flow.append(Spacer(1, 1.5 * cm))
    flow.append(Paragraph("Sistema de Comunicação Interpessoal", style_metadata_capa))
    flow.append(Paragraph("baseado em Java RMI", style_metadata_capa))
    flow.append(Spacer(1, 3 * cm))
    flow.append(Paragraph("Universidade Tecnológica Federal do Paraná",
                          style_metadata_capa))
    flow.append(Paragraph("Sistemas Distribuídos", style_metadata_capa))
    flow.append(PageBreak())

    # ═══ SUMÁRIO ═══
    flow.append(Paragraph("Sumário", style_h1))
    sumario = [
        ["1. Visão geral",                            "3"],
        ["2. Arquitetura em três camadas",            "3"],
        ["3. O que é RMI (explicação simplificada)",  "5"],
        ["4. As classes principais",                  "6"],
        ["5. Fluxo de uma mensagem em grupo",         "8"],
        ["6. O padrão Callback (RMI bidirecional)",   "9"],
        ["7. Threads — quem faz o quê",              "10"],
        ["8. Persistência e estado",                 "12"],
        ["9. Heartbeat e detecção de clientes mortos","12"],
        ["10. Como executar o sistema",              "13"],
    ]
    tbl = Table(sumario, colWidths=[14 * cm, 2 * cm])
    tbl.setStyle(TableStyle([
        ("FONTNAME", (0, 0), (-1, -1), "Helvetica"),
        ("FONTSIZE", (0, 0), (-1, -1), 11),
        ("TEXTCOLOR", (0, 0), (-1, -1), COR_TEXTO),
        ("TEXTCOLOR", (1, 0), (1, -1), COR_DESTAQUE),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
        ("LINEBELOW", (0, 0), (-1, -2), 0.3, COR_BORDA),
        ("ALIGN", (1, 0), (1, -1), "RIGHT"),
    ]))
    flow.append(tbl)
    flow.append(PageBreak())

    # ═══ 1. VISÃO GERAL ═══
    flow.append(Paragraph("1. Visão geral", style_h1))
    flow.append(p(
        "O <b>WhatsUT</b> é um sistema de chat estilo WhatsApp construído inteiramente "
        "em Java. Ele permite que vários usuários se cadastrem, façam login, troquem mensagens "
        "privadas, criem grupos com aprovação de entrada, enviem arquivos e sejam moderados "
        "por um administrador."
    ))
    flow.append(p(
        "O que torna o sistema interessante do ponto de vista de Sistemas Distribuídos é "
        "que ele <b>não usa HTTP, REST nem WebSockets</b>. A comunicação entre cliente e servidor "
        "é feita via <b>Java RMI (Remote Method Invocation)</b>, uma tecnologia que permite "
        "chamar métodos Java em outro processo — mesmo em outra máquina — como se fossem locais."
    ))
    flow.append(p(
        "Não há banco de dados: tudo fica em memória no servidor (estruturas thread-safe). "
        "A interface gráfica usa <b>Java Swing</b>, mas neste documento vamos focar no que "
        "acontece no <b>backend</b> e na <b>rede</b>."
    ))

    # ═══ 2. ARQUITETURA ═══
    flow.append(Paragraph("2. Arquitetura em três camadas", style_h1))
    flow.append(p(
        "O projeto é dividido em três pacotes Java, organizados por responsabilidade:"
    ))
    flow.append(diagrama_pacotes())
    flow.append(Paragraph(
        "Figura 1 — Pacotes do projeto e suas relações",
        style_legenda))
    flow.append(p(
        "<b>whatsut.common</b> é o coração do contrato RMI. Contém as <b>interfaces remotas</b> "
        "(<font face='Courier'>ServicoChat</font> e <font face='Courier'>RetornoChamadaCliente</font>) "
        "e as classes <b>DTO</b> (Data Transfer Object) — objetos simples que viajam pela rede "
        "serializados como bytes."
    ))
    flow.append(p(
        "<b>whatsut.server</b> contém a implementação do serviço e suas classes auxiliares "
        "(modelos internos de usuário/grupo, painel admin). Só roda no servidor."
    ))
    flow.append(p(
        "<b>whatsut.client</b> contém a aplicação do usuário final, incluindo o callback "
        "que <i>também é um objeto remoto</i> — o servidor liga de volta para ele."
    ))

    flow.append(Paragraph("Visão de execução", style_h2))
    flow.append(diagrama_arquitetura())
    flow.append(Paragraph(
        "Figura 2 — Como cliente e servidor se conectam pela rede",
        style_legenda))
    flow.append(p(
        "O servidor abre a porta <b>1099</b> (padrão do RMI) e publica seu serviço com o "
        "nome <font face='Courier'>WhatsUTService</font>. Cada cliente faz lookup nesse nome, "
        "recebe um <b>stub</b> (objeto proxy) e a partir daí chama métodos como se fossem locais."
    ))

    flow.append(PageBreak())

    # ═══ 3. O QUE É RMI ═══
    flow.append(Paragraph("3. O que é RMI (explicação simplificada)", style_h1))
    flow.append(p(
        "Imagine que você tem um método Java normal:"
    ))
    flow.append(code(
        "List<UsuarioDTO> usuarios = servico.obterUsuarios(\"luiz\");"
    ))
    flow.append(p(
        "Esse <font face='Courier'>servico</font> parece um objeto comum. Mas com RMI, ele "
        "<b>não existe na sua máquina</b> — ele vive no processo do servidor. O método "
        "\"viaja\" pela rede, executa lá, e o resultado volta. Para você, programador, parece "
        "uma chamada local."
    ))

    flow.append(Paragraph("Por baixo dos panos", style_h2))
    flow.append(p(
        "Quando você chama <font face='Courier'>servico.obterUsuarios(...)</font>, o RMI "
        "internamente faz quatro coisas:"
    ))
    flow.append(li("<b>Serializa</b> os argumentos — transforma os objetos em bytes (eles devem implementar <font face='Courier'>Serializable</font>)."))
    flow.append(li("Envia esses bytes pelo <b>socket TCP</b> até o servidor."))
    flow.append(li("No servidor, <b>desserializa</b> os bytes e executa o método de verdade."))
    flow.append(li("Serializa o retorno e manda de volta pelo mesmo caminho."))

    flow.append(p(
        "Você não escreve <font face='Courier'>Socket</font>, <font face='Courier'>OutputStream</font> "
        "ou protocolo binário em lugar nenhum. <b>O RMI esconde a rede atrás de uma interface Java.</b>"
    ))

    flow.append(Paragraph("Regras obrigatórias do RMI", style_h2))
    flow.append(li("A interface remota <b>estende</b> <font face='Courier'>java.rmi.Remote</font>."))
    flow.append(li("Todo método declara <font face='Courier'>throws RemoteException</font> (a rede pode falhar)."))
    flow.append(li("A implementação estende <font face='Courier'>UnicastRemoteObject</font>."))
    flow.append(li("Os DTOs trafegados implementam <font face='Courier'>Serializable</font>."))

    flow.append(p(
        "Veja no código do projeto (<font face='Courier'>common/ServicoChat.java</font>):"
    ))
    flow.append(code(
        "public interface ServicoChat extends Remote {\n"
        "    UsuarioDTO entrar(String nomeUsuario, String senha,\n"
        "                      RetornoChamadaCliente retornoChamada)\n"
        "            throws RemoteException;\n"
        "    void enviarMensagemGrupo(String deUsuario, String idGrupo,\n"
        "                             MensagemDTO mensagem)\n"
        "            throws RemoteException;\n"
        "    // ... outros métodos ...\n"
        "}"
    ))

    flow.append(PageBreak())

    # ═══ 4. CLASSES PRINCIPAIS ═══
    flow.append(Paragraph("4. As classes principais", style_h1))
    flow.append(p(
        "Vamos olhar quem é responsável por quê:"
    ))

    tabela_classes = [
        ["Classe", "Pacote", "O que faz"],
        ["PrincipalServidor",       "server", "main() do servidor. Abre o registry RMI e instancia o serviço."],
        ["ServicoChatImpl",         "server", "Implementa todos os métodos RMI. É o cérebro do servidor."],
        ["UsuarioServidor",         "server", "Modelo interno de usuário. Guarda hash BCrypt e o stub do callback."],
        ["GrupoServidor",           "server", "Modelo interno de grupo. Mantém membros, histórico e política de admin."],
        ["InterfaceAdminServidor",  "server", "Janela Swing de administração (usuários, grupos, configs, logs)."],
        ["ServicoChat",             "common", "Interface RMI principal. Contrato do servidor."],
        ["RetornoChamadaCliente",   "common", "Interface RMI do cliente. Servidor chama estes métodos para 'empurrar' eventos."],
        ["UsuarioDTO, GrupoDTO",    "common", "Objetos serializáveis que trafegam pela rede."],
        ["MensagemDTO",             "common", "Representa uma mensagem (texto, arquivo ou sistema)."],
        ["PrincipalCliente",        "client", "main() do cliente. Cria a janela e gerencia login↔chat."],
        ["SessaoChat",              "client", "Mantém conexão RMI, usuário logado e thread de heartbeat."],
        ["RetornoChamadaClienteImpl","client", "Implementação do callback. Recebe eventos do servidor."],
        ["PainelChat, PainelLogin", "client.ui", "Telas Swing (login, chat, criação de grupos)."],
    ]
    tbl = Table(tabela_classes, colWidths=[4.2 * cm, 2 * cm, 9.5 * cm])
    tbl.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), COR_DESTAQUE),
        ("TEXTCOLOR",  (0, 0), (-1, 0), colors.white),
        ("FONTNAME",   (0, 0), (-1, 0), "Helvetica-Bold"),
        ("FONTSIZE",   (0, 0), (-1, 0), 10),
        ("FONTNAME",   (0, 1), (0, -1), "Courier-Bold"),
        ("FONTNAME",   (1, 1), (1, -1), "Courier"),
        ("FONTSIZE",   (0, 1), (-1, -1), 8.5),
        ("VALIGN",     (0, 0), (-1, -1), "TOP"),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, COR_CARTAO]),
        ("GRID",       (0, 0), (-1, -1), 0.3, COR_BORDA),
        ("LEFTPADDING",(0, 0), (-1, -1), 6),
        ("RIGHTPADDING",(0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING",(0, 0), (-1, -1), 5),
    ]))
    flow.append(tbl)

    flow.append(Spacer(1, 0.5 * cm))
    flow.append(Paragraph("Por que dois modelos para usuário?", style_h3))
    flow.append(p(
        "<font face='Courier'>UsuarioDTO</font> trafega pela rede e é seguro mostrar para qualquer "
        "cliente: contém só nome, status e flag de admin. Já <font face='Courier'>UsuarioServidor</font> "
        "vive apenas no servidor e contém <b>informações sensíveis</b> que não devem sair daqui: o "
        "hash BCrypt da senha e a referência remota ao callback do cliente. O método "
        "<font face='Courier'>paraDTO()</font> converte um no outro filtrando esses campos."
    ))

    flow.append(PageBreak())

    # ═══ 5. FLUXO DE MENSAGEM EM GRUPO ═══
    flow.append(Paragraph("5. Fluxo de uma mensagem em grupo", style_h1))
    flow.append(p(
        "Vamos rastrear o que acontece quando <b>alice</b> envia \"oi\" em um grupo onde "
        "<b>bob</b> também é membro:"
    ))
    flow.append(diagrama_sequencia_mensagem())
    flow.append(Paragraph(
        "Figura 3 — Sequência de uma mensagem de grupo",
        style_legenda))

    flow.append(Paragraph("Passo a passo no código", style_h2))
    flow.append(p(
        "<b>1.</b> Em <font face='Courier'>PainelChat.enviarMensagem()</font>, o usuário aperta Enviar:"
    ))
    flow.append(code(
        "MensagemDTO msg = MensagemDTO.texto(meuUsuario, meuExibicao,\n"
        "                                     idChatAtivo, chatAtivoEhGrupo, texto);\n"
        "// ...\n"
        "sessao.getServico().enviarMensagemGrupo(meuUsuario, idChatAtivo, msg);"
    ))
    flow.append(p(
        "<b>2.</b> O RMI serializa <font face='Courier'>msg</font> e manda pelo socket. No servidor, "
        "<font face='Courier'>ServicoChatImpl.enviarMensagemGrupo</font> executa:"
    ))
    flow.append(code(
        "grupo.adicionarMensagem(mensagem);  // grava no histórico em memória\n"
        "for (String membro : grupo.getNomesMembros()) {\n"
        "    if (membro.equals(deUsuario)) continue; // pula o remetente\n"
        "    UsuarioServidor u = usuarios.get(membro);\n"
        "    if (u != null && u.estaOnline()) {\n"
        "        dispararRetornoChamada(() -> {\n"
        "            try { u.getRetornoChamada().aoReceberMensagem(mensagem); }\n"
        "            catch (RemoteException e) { tratarClienteMorto(...); }\n"
        "        });\n"
        "    }\n"
        "}"
    ))
    flow.append(p(
        "<b>3.</b> O <font face='Courier'>dispararRetornoChamada</font> joga o trabalho num "
        "<i>thread pool</i> — assim o método retorna rapidamente para alice, e os callbacks "
        "rodam em paralelo. No cliente de bob, "
        "<font face='Courier'>RetornoChamadaClienteImpl.aoReceberMensagem</font> chama o listener "
        "que foi registrado em <font face='Courier'>PainelChat</font>:"
    ))
    flow.append(code(
        "cb.setAoReceberMensagem(msg -> SwingUtilities.invokeLater(() -> {\n"
        "    String chave = msg.isEhGrupo() ? msg.getIdDestino() : msg.getNomeRemetente();\n"
        "    if (chave.equals(idChatAtivo)) {\n"
        "        adicionarMensagem(msg);  // desenha o balão\n"
        "    } else {\n"
        "        naoLidas.merge(chave, 1, Integer::sum);  // badge\n"
        "    }\n"
        "}));"
    ))

    flow.append(PageBreak())

    # ═══ 6. CALLBACK ═══
    flow.append(Paragraph("6. O padrão Callback (RMI bidirecional)", style_h1))
    flow.append(p(
        "Esse é o conceito mais importante do projeto. Sem ele, o cliente teria que "
        "ficar perguntando \"tem mensagem nova?\" a cada segundo — uma técnica chamada "
        "<b>polling</b>, que é lenta e desperdiça rede."
    ))
    flow.append(diagrama_callback())
    flow.append(Paragraph(
        "Figura 4 — Cliente é também um objeto remoto",
        style_legenda))

    flow.append(Paragraph("Como funciona", style_h2))
    flow.append(p(
        "<b>1.</b> No login, o cliente envia uma referência ao próprio callback:"
    ))
    flow.append(code(
        "// SessaoChat.entrar()\n"
        "retornoChamada = new RetornoChamadaClienteImpl();\n"
        "usuarioAtual = servico.entrar(nomeUsuario, senha, retornoChamada);"
    ))
    flow.append(p(
        "<b>2.</b> O servidor guarda essa referência no <font face='Courier'>UsuarioServidor</font>:"
    ))
    flow.append(code(
        "// ServicoChatImpl.entrar()\n"
        "usuario.setRetornoChamada(retornoChamada);"
    ))
    flow.append(p(
        "<b>3.</b> A partir daí, sempre que algo acontece (mensagem nova, alguém entrou "
        "no grupo, ban do admin...), o servidor <b>chama métodos no cliente</b> usando essa "
        "referência. RMI <i>ao contrário</i>."
    ))
    flow.append(p(
        "Os métodos disponíveis no callback estão em <font face='Courier'>RetornoChamadaCliente</font>:"
    ))
    flow.append(li("<font face='Courier'>aoReceberMensagem</font> — chegou mensagem nova"))
    flow.append(li("<font face='Courier'>aoAtualizarListaUsuarios</font> — lista de usuários mudou"))
    flow.append(li("<font face='Courier'>aoMudarStatusUsuario</font> — alguém ficou online/offline"))
    flow.append(li("<font face='Courier'>aoEntrarNoGrupo</font> — você foi adicionado a um grupo"))
    flow.append(li("<font face='Courier'>aoReceberSolicitacao</font> — alguém pediu para entrar (admin)"))
    flow.append(li("<font face='Courier'>aoAprovarSolicitacao</font> — sua entrada foi aprovada"))
    flow.append(li("<font face='Courier'>aoSerBanidoDoServidor</font> — você foi banido"))
    flow.append(li("<font face='Courier'>verificar</font> — heartbeat: \"você está vivo?\""))

    flow.append(PageBreak())

    # ═══ 7. THREADS ═══
    flow.append(Paragraph("7. Threads — quem faz o quê", style_h1))
    flow.append(p(
        "Sistemas distribuídos vivem de paralelismo. Veja onde threads são usadas no projeto:"
    ))
    flow.append(diagrama_threads())
    flow.append(Paragraph(
        "Figura 5 — Como as threads colaboram numa mensagem de grupo",
        style_legenda))

    flow.append(Paragraph("a) Threads automáticas do RMI", style_h2))
    flow.append(p(
        "Isso é <b>de graça</b>. Quando 10 clientes chamam <font face='Courier'>obterUsuarios()</font> "
        "ao mesmo tempo, o RMI cria 10 threads no servidor automaticamente. Por isso o projeto "
        "usa <font face='Courier'>ConcurrentHashMap</font> em vez de <font face='Courier'>HashMap</font>:"
    ))
    flow.append(code(
        "private final ConcurrentHashMap<String, UsuarioServidor> usuarios\n"
        "        = new ConcurrentHashMap<>();"
    ))

    flow.append(Paragraph("b) Pool de threads para callbacks", style_h2))
    flow.append(p(
        "Quando o servidor entrega uma mensagem a 10 membros do grupo, ele não pode esperar "
        "10 chamadas de rede sequencialmente. Em vez disso, joga cada callback num "
        "<font face='Courier'>CachedThreadPool</font>:"
    ))
    flow.append(code(
        "private final ExecutorService executorRetornoChamada =\n"
        "        Executors.newCachedThreadPool(r -> {\n"
        "            Thread t = new Thread(r, \"trabalhador-retorno-chamada\");\n"
        "            t.setDaemon(true);\n"
        "            return t;\n"
        "        });\n"
        "\n"
        "private void dispararRetornoChamada(Runnable r) {\n"
        "    executorRetornoChamada.submit(r);\n"
        "}"
    ))
    flow.append(p(
        "O <font face='Courier'>submit()</font> coloca a tarefa numa fila e retorna na hora. "
        "As threads do pool pegam e executam. Resultado: o método "
        "<font face='Courier'>enviarMensagemGrupo</font> retorna em milissegundos para "
        "alice, mesmo se um dos destinos estiver com a rede lenta."
    ))

    flow.append(Paragraph("c) Thread agendada do heartbeat", style_h2))
    flow.append(p(
        "Um <font face='Courier'>ScheduledExecutorService</font> dispara uma verificação a "
        "cada 60 segundos para identificar clientes que não respondem mais:"
    ))
    flow.append(code(
        "agendador.scheduleAtFixedRate(this::verificarHeartbeats, 60, 60,\n"
        "                              TimeUnit.SECONDS);"
    ))

    flow.append(Paragraph("d) Thread Timer no cliente", style_h2))
    flow.append(p(
        "Cada cliente avisa o servidor \"estou vivo\" a cada 20 segundos, em "
        "<font face='Courier'>SessaoChat.iniciarHeartbeat()</font>:"
    ))
    flow.append(code(
        "timerHeartbeat.scheduleAtFixedRate(new TimerTask() {\n"
        "    @Override public void run() {\n"
        "        servico.manterConectado(usuarioAtual.getNomeUsuario());\n"
        "    }\n"
        "}, 20_000, 20_000);"
    ))

    flow.append(Paragraph("e) SwingUtilities.invokeLater", style_h2))
    flow.append(p(
        "Quando um callback chega no cliente, ele roda numa thread do RMI — <b>não</b> "
        "na thread de UI do Swing. Tocar em componentes Swing fora da EDT (Event Dispatch "
        "Thread) causa bugs sutis. Por isso todos os handlers usam:"
    ))
    flow.append(code(
        "cb.setAoReceberMensagem(msg -> SwingUtilities.invokeLater(() -> {\n"
        "    // aqui é seguro mexer na UI\n"
        "    adicionarMensagem(msg);\n"
        "}));"
    ))

    flow.append(PageBreak())

    # ═══ 8. PERSISTÊNCIA E ESTADO ═══
    flow.append(Paragraph("8. Persistência e estado", style_h1))
    flow.append(p(
        "Não há banco de dados. <b>Tudo é mantido em memória</b> no servidor, em três estruturas "
        "principais (todas <font face='Courier'>ConcurrentHashMap</font>):"
    ))
    flow.append(li("<font face='Courier'>usuarios</font>: nome → <font face='Courier'>UsuarioServidor</font>"))
    flow.append(li("<font face='Courier'>grupos</font>: id → <font face='Courier'>GrupoServidor</font>"))
    flow.append(li("<font face='Courier'>historicosPrivados</font>: chave \"a:b\" → fila de mensagens"))

    flow.append(p(
        "A <b>consequência prática</b>: ao reiniciar o servidor, todos os usuários, grupos "
        "e mensagens são perdidos. Para um projeto acadêmico isso é OK, mas é uma limitação "
        "óbvia para produção. Adicionar persistência seria um próximo passo natural — "
        "salvando esses mapas em SQLite ou em arquivos JSON."
    ))

    # ═══ 9. HEARTBEAT ═══
    flow.append(Paragraph("9. Heartbeat e detecção de clientes mortos", style_h1))
    flow.append(p(
        "Um problema comum em sistemas RMI: o cliente fecha (ou perde a conexão) sem fazer "
        "logout. O servidor ainda tem a referência ao callback antigo, e tentar chamá-lo "
        "gera <font face='Courier'>RemoteException</font>."
    ))
    flow.append(p(
        "A solução é em duas pontas:"
    ))
    flow.append(p(
        "<b>Cliente (a cada 20s):</b> envia ping com "
        "<font face='Courier'>servico.manterConectado(nomeUsuario)</font>, que atualiza o "
        "<font face='Courier'>ultimoHeartbeat</font> no servidor."
    ))
    flow.append(p(
        "<b>Servidor (a cada 60s):</b> percorre todos os usuários online. Se algum não "
        "atualizou o heartbeat há mais de N minutos (configurável), o servidor tenta um "
        "<font face='Courier'>callback.verificar()</font>. Se a chamada falhar, "
        "<font face='Courier'>tratarClienteMorto()</font> remove o callback e marca offline."
    ))
    flow.append(code(
        "// ServicoChatImpl.verificarHeartbeats()\n"
        "for (UsuarioServidor u : usuarios.values()) {\n"
        "    if (u.estaOnline() && u.getUltimoHeartbeat().isBefore(limite)) {\n"
        "        try { u.getRetornoChamada().verificar(); }\n"
        "        catch (RemoteException e) { tratarClienteMorto(u.getNomeUsuario()); }\n"
        "    }\n"
        "}"
    ))

    flow.append(PageBreak())

    # ═══ 10. EXECUÇÃO ═══
    flow.append(Paragraph("10. Como executar o sistema", style_h1))

    flow.append(Paragraph("Pré-requisitos", style_h2))
    flow.append(li("Java 17 ou superior"))
    flow.append(li("Maven 3.6 ou superior"))

    flow.append(Paragraph("Compilação", style_h2))
    flow.append(code("mvn clean package"))
    flow.append(p(
        "Isso gera <font face='Courier'>target/whatsut-server.jar</font> — um JAR \"fat\" que "
        "contém todas as dependências (BCrypt, Gson) embutidas."
    ))

    flow.append(Paragraph("Servidor", style_h2))
    flow.append(code(
        "java -cp target/whatsut-server.jar whatsut.server.PrincipalServidor"
    ))
    flow.append(p(
        "Inicia o <font face='Courier'>rmiregistry</font> embutido na porta 1099, registra "
        "o serviço como <font face='Courier'>WhatsUTService</font> e abre o painel admin. "
        "Login padrão: <font face='Courier'>admin / admin123</font>."
    ))

    flow.append(Paragraph("Cliente (uma janela por usuário)", style_h2))
    flow.append(code(
        "java -cp target/whatsut-server.jar whatsut.client.PrincipalCliente"
    ))
    flow.append(p(
        "Você pode rodar várias instâncias para simular múltiplos usuários. Cada janela "
        "tem sua tela de login/cadastro, e depois a tela principal de chat com a sidebar "
        "(Usuários | Grupos)."
    ))

    flow.append(Paragraph("Para criar um grupo:", style_h2))
    flow.append(li("Usuário A: aba <b>Grupos</b> → botão <b>+ Novo Grupo</b> → preenche e cria"))
    flow.append(li("Usuário B: aba <b>Grupos</b> → seção <i>Outros grupos disponíveis</i> → clica no grupo"))
    flow.append(li("Usuário A: aceita o popup de solicitação"))
    flow.append(li("Ambos veem mensagens em tempo real (callbacks RMI)"))

    flow.append(Spacer(1, 1.5 * cm))

    # Caixa de conclusão
    caixa = Table([[
        Paragraph(
            "<b>Em resumo:</b> o WhatsUT mostra na prática como RMI esconde a complexidade "
            "de rede atrás de chamadas de método. As três ideias-chave para entender o "
            "projeto são (1) <b>interface remota</b> compartilhada entre cliente e servidor, "
            "(2) <b>registry</b> como agenda telefônica de serviços e (3) <b>callback RMI</b> "
            "para que o servidor possa empurrar eventos em tempo real, sem polling.",
            style_paragrafo
        )
    ]], colWidths=[16 * cm])
    caixa.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), COR_CARTAO),
        ("BOX", (0, 0), (-1, -1), 1.5, COR_DESTAQUE),
        ("LEFTPADDING", (0, 0), (-1, -1), 14),
        ("RIGHTPADDING", (0, 0), (-1, -1), 14),
        ("TOPPADDING", (0, 0), (-1, -1), 12),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 12),
    ]))
    flow.append(caixa)

    return flow


# ════════════════════════════════════════════════════════════════
# Build
# ════════════════════════════════════════════════════════════════
def main():
    saida = "WhatsUT-Documentacao.pdf"
    doc = SimpleDocTemplate(
        saida,
        pagesize=A4,
        leftMargin=2.0 * cm, rightMargin=2.0 * cm,
        topMargin=2.0 * cm, bottomMargin=2.0 * cm,
        title="WhatsUT — Documentação Técnica",
        author="UTFPR — Sistemas Distribuídos",
    )
    doc.build(construir_conteudo(),
              onFirstPage=cabecalho_rodape,
              onLaterPages=cabecalho_rodape)
    print(f"PDF gerado: {saida}")


if __name__ == "__main__":
    main()
