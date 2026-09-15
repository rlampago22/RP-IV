package br.edu.unipampa.usina.app;

import br.edu.unipampa.usina.alarmes.Alarme;
import br.edu.unipampa.usina.alarmes.AlarmeFacade;
import br.edu.unipampa.usina.alarmes.AlarmeFactory;
import br.edu.unipampa.usina.alarmes.AvaliadorFaixaSegura;
import br.edu.unipampa.usina.auditorialogs.AuditoriaSubscriber;
import br.edu.unipampa.usina.auditorialogs.RegistroAuditoria;
import br.edu.unipampa.usina.controlereator.ReatorFacade;
import br.edu.unipampa.usina.controlereator.Sensor;
import br.edu.unipampa.usina.infraestruturaeventos.AlarmeEmitido;
import br.edu.unipampa.usina.infraestruturaeventos.AlarmeReconhecido;
import br.edu.unipampa.usina.infraestruturaeventos.AlarmeResolvido;
import br.edu.unipampa.usina.infraestruturaeventos.EventBus;
import br.edu.unipampa.usina.infraestruturaeventos.EventoDominio;
import br.edu.unipampa.usina.infraestruturaeventos.FalhaSensorDetectada;
import br.edu.unipampa.usina.infraestruturaeventos.MedicaoRegistrada;
import br.edu.unipampa.usina.infraestruturaeventos.ObservacaoRegistrada;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

/**
 * Interface gráfica moderna e de alta fidelidade visual para a Central de Supervisão
 * do Reator Nuclear 01 (MVP de Análise e Projeto de Software).
 * Paleta SCADA Dark, animações suaves do núcleo e telemetria completa.
 */
public final class SistemaMvpUI extends JFrame {
    // Paleta de Cores Estilo SCADA Dark Industrial Moderno
    private static final Color BG_MAIN       = new Color(11, 15, 25);     // #0B0F19
    private static final Color BG_CARD       = new Color(19, 28, 46);     // #131C2E
    private static final Color BG_CARD_LIGHT = new Color(26, 38, 62);     // #1A263E
    private static final Color BORDER_SUBTLE = new Color(34, 49, 79);     // #22314F
    private static final Color BORDER_ACTIVE = new Color(56, 189, 248);    // #38BDF8

    private static final Color TEXT_PRIMARY  = new Color(248, 250, 252);   // #F8FAFC
    private static final Color TEXT_MUTED    = new Color(148, 163, 184);   // #94A3B8
    private static final Color TEXT_DIM      = new Color(100, 116, 139);   // #64748B

    private static final Color ACCENT_EMERALD = new Color(16, 185, 129);   // Normal / Seguro
    private static final Color ACCENT_CYAN    = new Color(6, 182, 212);    // Telemetria / Fluxo
    private static final Color ACCENT_AMBER   = new Color(245, 158, 11);   // Atenção / Observação
    private static final Color ACCENT_CRIMSON = new Color(239, 68, 68);    // Alarme / Crítico
    private static final Color ACCENT_PURPLE  = new Color(168, 85, 247);   // Exceção / Manutenção

    // Tipografia Segoe UI / Consolas com fallbacks
    private static final Font FONT_HEADER_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_CARD_TITLE   = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_LABEL        = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_NUMERIC      = new Font("Consolas", Font.BOLD, 16);
    private static final Font FONT_METRIC_VAL   = new Font("Consolas", Font.BOLD, 22);
    private static final Font FONT_TABLE        = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_BADGE        = new Font("Segoe UI", Font.BOLD, 11);

    private static final DateTimeFormatter HORA =
        DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final EventBus eventBus = new EventBus();
    private final RegistroAuditoria auditoria =
        new RegistroAuditoria(Path.of("dados", "auditoria.log"));
    private final AlarmeFacade alarmes;
    private final ReatorFacade reator;

    private final DefaultTableModel modeloEventos = new DefaultTableModel(
        new Object[] {"Hora", "Evento", "Origem", "Detalhes", "Integridade"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tabelaEventos = new JTable(modeloEventos);
    private final JTextArea areaAlarmes = new JTextArea();

    private final JLabel totalMedicoes = metricLabel("0", ACCENT_CYAN);
    private final JLabel totalAlarmes = metricLabel("0", ACCENT_CRIMSON);
    private final JLabel totalAuditoria = metricLabel("0", ACCENT_AMBER);
    private final JLabel integridade = metricLabel("OK", ACCENT_EMERALD);
    private final JLabel statusBadge = new JLabel("● SISTEMA OPERACIONAL ESTÁVEL");
    private final JLabel relogioDigital = new JLabel("--:--:--");

    private final ReactorCoreHUD painelReator = new ReactorCoreHUD();

    // 4 Sensores exigidos pelo RF-1 e UC01
    private final JSpinner temperatura = spinner(310.5, -50.0, 500.0, 0.5);
    private final JSpinner pressao = spinner(155.0, 0.0, 300.0, 0.5);
    private final JSpinner radiacao = spinner(2.4, 0.0, 50.0, 0.1);
    private final JSpinner fluxoResfriamento = spinner(1100.0, 200.0, 2500.0, 10.0);

    // Barras visuais de nível dos sensores
    private final SensorLevelBar barTemperatura = new SensorLevelBar(0.0, 350.0, 320.0, "°C", ACCENT_EMERALD);
    private final SensorLevelBar barPressao = new SensorLevelBar(0.0, 160.0, 150.0, "bar", ACCENT_AMBER);
    private final SensorLevelBar barRadiacao = new SensorLevelBar(0.0, 5.0, 4.0, "mSv/h", ACCENT_CRIMSON);
    private final SensorLevelBar barFluxo = new SensorLevelBar(500.0, 1500.0, 1400.0, "m³/h", ACCENT_CYAN);

    // Monitoramento contínuo em tempo real (UC01)
    private final Timer timerTempoReal;
    private boolean tempoRealAtivo = false;
    private final ModernButton botaoTempoReal;
    private final Random random = new Random();

    public SistemaMvpUI() {
        super("Central de Supervisão SCADA — Reator Nuclear 01");
        new AuditoriaSubscriber(eventBus, auditoria);
        alarmes = new AlarmeFacade(eventBus, new AvaliadorFaixaSegura(), new AlarmeFactory());
        reator = new ReatorFacade(eventBus);

        // Registro dos 4 sensores com limites seguros e faixas de atenção preventiva
        reator.registrarSensor(new Sensor(1L, "TEMPERATURA", "Celsius", 0.0, 350.0, 20.0, 320.0));
        reator.registrarSensor(new Sensor(2L, "PRESSAO", "bar", 0.0, 160.0, 10.0, 150.0));
        reator.registrarSensor(new Sensor(3L, "RADIACAO", "mSv/h", 0.0, 5.0, 0.1, 4.0));
        reator.registrarSensor(new Sensor(4L, "FLUXO_RESFRIAMENTO", "m3/h", 500.0, 1500.0, 600.0, 1400.0));

        eventBus.assinarTodos(this::receberEventoNaTela);

        botaoTempoReal = new ModernButton("Iniciar Tempo Real", IconType.PLAY, new Color(15, 118, 110), new Color(13, 148, 136));
        botaoTempoReal.addActionListener(this::toggleTempoReal);

        timerTempoReal = new Timer(1500, this::executarTickTempoReal);

        // Relógio digital em tempo real
        Timer relogioTimer = new Timer(1000, e -> relogioDigital.setText(HORA.format(Instant.now())));
        relogioTimer.start();
        relogioDigital.setText(HORA.format(Instant.now()));

        configurarJanela();
        montarInterface();
        atualizarMetricas();
    }

    private void configurarJanela() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Rectangle areaUtil = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int largura = Math.max(1020, Math.min(1380, areaUtil.width - 24));
        int altura = Math.max(680, Math.min(840, areaUtil.height - 24));
        setSize(largura, altura);
        setMinimumSize(new Dimension(1000, 660));
        setLocation(
            areaUtil.x + Math.max(0, (areaUtil.width - largura) / 2),
            areaUtil.y + Math.max(0, (areaUtil.height - altura) / 2)
        );
        getContentPane().setBackground(BG_MAIN);
    }

    private void montarInterface() {
        JPanel raiz = new JPanel(new BorderLayout(0, 14));
        raiz.setBackground(BG_MAIN);
        raiz.setBorder(new EmptyBorder(16, 20, 16, 20));

        raiz.add(cabecalho(), BorderLayout.NORTH);
        raiz.add(conteudo(), BorderLayout.CENTER);
        raiz.add(barraAcoes(), BorderLayout.SOUTH);

        setContentPane(raiz);
    }

    private JPanel cabecalho() {
        RoundedPanel painel = new RoundedPanel(14, BG_CARD, BORDER_SUBTLE);
        painel.setLayout(new BorderLayout(16, 0));
        painel.setBorder(new EmptyBorder(12, 18, 12, 18));

        // Título e Identificação da Estação
        JPanel esquerda = new JPanel();
        esquerda.setOpaque(false);
        esquerda.setLayout(new BoxLayout(esquerda, BoxLayout.Y_AXIS));

        JPanel linhaBadge = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        linhaBadge.setOpaque(false);

        JLabel tagEstacao = new JLabel("USINA NUCLEAR • BLOCO DE CONTROLE R-01");
        tagEstacao.setFont(new Font("Segoe UI", Font.BOLD, 11));
        tagEstacao.setForeground(ACCENT_CYAN);

        linhaBadge.add(tagEstacao);

        JLabel titulo = new JLabel("Central Digital de Supervisão & Telemetria");
        titulo.setFont(FONT_HEADER_TITLE);
        titulo.setForeground(TEXT_PRIMARY);

        esquerda.add(linhaBadge);
        esquerda.add(Box.createVerticalStrut(2));
        esquerda.add(titulo);

        // Lado direito: Relógio + Status Badge
        JPanel direita = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 4));
        direita.setOpaque(false);

        relogioDigital.setFont(new Font("Consolas", Font.BOLD, 17));
        relogioDigital.setForeground(TEXT_MUTED);

        statusBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusBadge.setOpaque(true);
        statusBadge.setBackground(new Color(6, 78, 59));
        statusBadge.setForeground(new Color(167, 243, 208));
        statusBadge.setBorder(new EmptyBorder(7, 14, 7, 14));

        direita.add(relogioDigital);
        direita.add(statusBadge);

        painel.add(esquerda, BorderLayout.WEST);
        painel.add(direita, BorderLayout.EAST);
        return painel;
    }

    private JPanel conteudo() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1.0;

        c.gridx = 0;
        c.weightx = 0.33;
        c.insets = new Insets(0, 0, 0, 14);
        painel.add(colunaTelemetria(), c);

        c.gridx = 1;
        c.weightx = 0.67;
        c.insets = new Insets(0, 0, 0, 0);
        painel.add(colunaEventos(), c);
        return painel;
    }

    private JPanel colunaTelemetria() {
        JPanel coluna = new JPanel();
        coluna.setOpaque(false);
        coluna.setLayout(new BoxLayout(coluna, BoxLayout.Y_AXIS));

        // Radar / Visualizador Gráfico do Reator
        painelReator.setAlignmentX(Component.LEFT_ALIGNMENT);
        coluna.add(painelReator);

        Component strut1 = Box.createVerticalStrut(10);
        ((JComponent) strut1).setAlignmentX(Component.LEFT_ALIGNMENT);
        coluna.add(strut1);

        // 4 Cards Modernos de Sensores
        coluna.add(sensorCard("TEMPERATURA NÚCLEO", "01", "°C", temperatura, barTemperatura, ACCENT_EMERALD));
        Component strut2 = Box.createVerticalStrut(8);
        ((JComponent) strut2).setAlignmentX(Component.LEFT_ALIGNMENT);
        coluna.add(strut2);

        coluna.add(sensorCard("PRESSÃO CIRCUITO", "02", "bar", pressao, barPressao, ACCENT_AMBER));
        Component strut3 = Box.createVerticalStrut(8);
        ((JComponent) strut3).setAlignmentX(Component.LEFT_ALIGNMENT);
        coluna.add(strut3);

        coluna.add(sensorCard("RADIAÇÃO CONTENÇÃO", "03", "mSv/h", radiacao, barRadiacao, ACCENT_CRIMSON));
        Component strut4 = Box.createVerticalStrut(8);
        ((JComponent) strut4).setAlignmentX(Component.LEFT_ALIGNMENT);
        coluna.add(strut4);

        coluna.add(sensorCard("FLUXO RESFRIAMENTO", "04", "m³/h", fluxoResfriamento, barFluxo, ACCENT_CYAN));

        return coluna;
    }

    private JPanel sensorCard(String nome, String idTag, String unidade, JSpinner spinner, SensorLevelBar bar, Color acento) {
        RoundedPanel card = new RoundedPanel(12, BG_CARD, BORDER_SUBTLE);
        card.setLayout(new BorderLayout(8, 6));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));
        card.setBorder(new EmptyBorder(8, 12, 8, 12));

        // Topo: Nome, Tag, Unidade e Spinner
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);

        JPanel info = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        info.setOpaque(false);

        JLabel tag = new JLabel("[" + idTag + "]");
        tag.setFont(new Font("Consolas", Font.BOLD, 11));
        tag.setForeground(acento);

        JLabel lblNome = new JLabel(nome);
        lblNome.setFont(FONT_CARD_TITLE);
        lblNome.setForeground(TEXT_PRIMARY);

        JLabel lblUnidade = new JLabel("(" + unidade + ")");
        lblUnidade.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblUnidade.setForeground(TEXT_MUTED);

        info.add(tag);
        info.add(lblNome);
        info.add(lblUnidade);

        estilizarSpinner(spinner);

        top.add(info, BorderLayout.WEST);
        top.add(spinner, BorderLayout.EAST);

        // Centro: Barra gráfica de nível
        card.add(top, BorderLayout.NORTH);
        card.add(bar, BorderLayout.CENTER);

        // Ouvinte no spinner para atualizar a barra em tempo real
        spinner.addChangeListener(e -> bar.setValor(numero(spinner)));
        bar.setValor(numero(spinner));

        return card;
    }

    private JPanel colunaEventos() {
        JPanel coluna = new JPanel(new BorderLayout(0, 10));
        coluna.setOpaque(false);

        coluna.add(painelKpis(), BorderLayout.NORTH);

        // Tabela Estilizada com Renderizadores Customizados
        tabelaEventos.setBackground(BG_CARD);
        tabelaEventos.setForeground(TEXT_PRIMARY);
        tabelaEventos.setGridColor(new Color(25, 36, 58));
        tabelaEventos.setShowVerticalLines(false);
        tabelaEventos.setIntercellSpacing(new Dimension(0, 1));
        tabelaEventos.setSelectionBackground(new Color(30, 58, 138));
        tabelaEventos.setSelectionForeground(TEXT_PRIMARY);
        tabelaEventos.setRowHeight(30);
        tabelaEventos.setFont(FONT_TABLE);
        tabelaEventos.getTableHeader().setBackground(BG_CARD_LIGHT);
        tabelaEventos.getTableHeader().setForeground(TEXT_PRIMARY);
        tabelaEventos.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabelaEventos.getTableHeader().setPreferredSize(new Dimension(0, 32));
        tabelaEventos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tabelaEventos.getColumnModel().getColumn(0).setPreferredWidth(68);
        tabelaEventos.getColumnModel().getColumn(1).setPreferredWidth(145);
        tabelaEventos.getColumnModel().getColumn(2).setPreferredWidth(95);
        tabelaEventos.getColumnModel().getColumn(3).setPreferredWidth(420);
        tabelaEventos.getColumnModel().getColumn(4).setPreferredWidth(85);

        // Renderizadores de badges com cores
        tabelaEventos.getColumnModel().getColumn(0).setCellRenderer(new CenterRenderer());
        tabelaEventos.getColumnModel().getColumn(1).setCellRenderer(new EventTypeBadgeRenderer());
        tabelaEventos.getColumnModel().getColumn(2).setCellRenderer(new OriginBadgeRenderer());
        tabelaEventos.getColumnModel().getColumn(4).setCellRenderer(new IntegrityBadgeRenderer());

        JScrollPane scroll = new JScrollPane(tabelaEventos);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_SUBTLE));
        scroll.getViewport().setBackground(BG_CARD);

        RoundedPanel painelEventos = new RoundedPanel(12, BG_CARD, BORDER_SUBTLE);
        painelEventos.setLayout(new BorderLayout(0, 8));
        painelEventos.setBorder(new EmptyBorder(10, 12, 10, 12));

        JLabel lblEventos = new JLabel("LINHA DO TEMPO DE EVENTOS DE DOMÍNIO (EDA / PUB-SUB)");
        lblEventos.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblEventos.setForeground(TEXT_MUTED);

        painelEventos.add(lblEventos, BorderLayout.NORTH);
        painelEventos.add(scroll, BorderLayout.CENTER);

        coluna.add(painelEventos, BorderLayout.CENTER);

        // Painel de Alarmes e Notificações Ativas
        areaAlarmes.setEditable(false);
        areaAlarmes.setRows(4);
        areaAlarmes.setLineWrap(true);
        areaAlarmes.setWrapStyleWord(true);
        areaAlarmes.setBackground(new Color(24, 15, 20));
        areaAlarmes.setForeground(new Color(254, 205, 211));
        areaAlarmes.setCaretColor(TEXT_PRIMARY);
        areaAlarmes.setFont(new Font("Consolas", Font.PLAIN, 12));
        areaAlarmes.setText("Nenhum alarme pendente nesta sessão.\n");

        JScrollPane alarmScroll = new JScrollPane(areaAlarmes);
        alarmScroll.setBorder(BorderFactory.createLineBorder(new Color(159, 18, 57, 120)));
        alarmScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        RoundedPanel painelAlarmes = new RoundedPanel(12, BG_CARD, BORDER_SUBTLE);
        painelAlarmes.setLayout(new BorderLayout(0, 8));
        painelAlarmes.setBorder(new EmptyBorder(10, 12, 10, 12));

        JPanel topoAlarme = new JPanel(new BorderLayout());
        topoAlarme.setOpaque(false);
        JLabel lblAlarme = new JLabel("GESTÃO DE ALARMES & NOTIFICAÇÕES (UC01 / RF-2)");
        lblAlarme.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblAlarme.setForeground(TEXT_MUTED);

        JPanel acoesAlarmes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acoesAlarmes.setOpaque(false);

        ModernButton btnReconhecer = new ModernButton("Validar / Reconhecer", IconType.CHECK, new Color(180, 83, 9), new Color(217, 119, 6));
        btnReconhecer.addActionListener(this::reconhecerAlarmeAcao);

        ModernButton btnResolver = new ModernButton("Normalizar / Encerrar", IconType.REFRESH, new Color(5, 150, 105), new Color(16, 185, 129));
        btnResolver.addActionListener(this::resolverAlarmeAcao);

        acoesAlarmes.add(btnReconhecer);
        acoesAlarmes.add(btnResolver);

        topoAlarme.add(lblAlarme, BorderLayout.WEST);
        topoAlarme.add(acoesAlarmes, BorderLayout.EAST);

        painelAlarmes.add(topoAlarme, BorderLayout.NORTH);
        painelAlarmes.add(alarmScroll, BorderLayout.CENTER);

        coluna.add(painelAlarmes, BorderLayout.SOUTH);
        return coluna;
    }

    private JPanel painelKpis() {
        JPanel painel = new JPanel(new GridLayout(1, 4, 10, 0));
        painel.setOpaque(false);
        painel.add(kpiCard("MEDIÇÕES", totalMedicoes, ACCENT_CYAN));
        painel.add(kpiCard("ALARMES CRÍTICOS", totalAlarmes, ACCENT_CRIMSON));
        painel.add(kpiCard("EVENTOS AUDITADOS", totalAuditoria, ACCENT_AMBER));
        painel.add(kpiCard("INTEGRIDADE SHA-256", integridade, ACCENT_EMERALD));
        return painel;
    }

    private JPanel kpiCard(String nome, JLabel valor, Color acento) {
        RoundedPanel card = new RoundedPanel(12, BG_CARD, BORDER_SUBTLE);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel titulo = new JLabel(nome);
        titulo.setForeground(TEXT_MUTED);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 10));

        valor.setForeground(acento);

        card.add(titulo, BorderLayout.NORTH);
        card.add(valor, BorderLayout.CENTER);
        return card;
    }

    private JPanel barraAcoes() {
        RoundedPanel painel = new RoundedPanel(14, BG_CARD, BORDER_SUBTLE);
        painel.setLayout(new BorderLayout(10, 0));
        painel.setBorder(new EmptyBorder(10, 14, 10, 14));

        JPanel ladoEsquerdo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        ladoEsquerdo.setOpaque(false);

        ModernButton btnNormal = new ModernButton("Cenário Normal", IconType.NONE, new Color(13, 148, 136), new Color(20, 184, 166));
        btnNormal.addActionListener(this::cenarioNormal);

        ModernButton btnObservacao = new ModernButton("Observação (Alt. 1)", IconType.NONE, new Color(180, 83, 9), new Color(217, 119, 6));
        btnObservacao.addActionListener(this::cenarioObservacao);

        ModernButton btnAnomalia = new ModernButton("Simular Anomalia", IconType.WARNING, new Color(185, 28, 28), new Color(220, 38, 38));
        btnAnomalia.addActionListener(this::cenarioAnomalia);

        ModernButton btnFalha = new ModernButton("Falha Sensor (Exceção)", IconType.BOLT, new Color(109, 40, 217), new Color(124, 58, 237));
        btnFalha.addActionListener(this::cenarioFalhaSensor);

        ladoEsquerdo.add(botaoTempoReal);
        ladoEsquerdo.add(btnNormal);
        ladoEsquerdo.add(btnObservacao);
        ladoEsquerdo.add(btnAnomalia);
        ladoEsquerdo.add(btnFalha);

        JPanel ladoDireito = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        ladoDireito.setOpaque(false);

        ModernButton btnRegistrar = new ModernButton("Registrar Leituras", IconType.NONE, new Color(2, 132, 199), new Color(14, 165, 233));
        btnRegistrar.addActionListener(this::registrarValores);

        ModernButton btnLog = new ModernButton("Abrir Log", IconType.FOLDER, BG_CARD_LIGHT, BORDER_SUBTLE);
        btnLog.addActionListener(this::abrirLog);

        ladoDireito.add(btnRegistrar);
        ladoDireito.add(btnLog);

        painel.add(ladoEsquerdo, BorderLayout.CENTER);
        painel.add(ladoDireito, BorderLayout.EAST);
        return painel;
    }

    private void toggleTempoReal(ActionEvent ignored) {
        tempoRealAtivo = !tempoRealAtivo;
        if (tempoRealAtivo) {
            timerTempoReal.start();
            botaoTempoReal.setText("Pausar Tempo Real");
            botaoTempoReal.setIconType(IconType.PAUSE);
            botaoTempoReal.setBackground(new Color(220, 38, 38));
        } else {
            timerTempoReal.stop();
            botaoTempoReal.setText("Iniciar Tempo Real");
            botaoTempoReal.setIconType(IconType.PLAY);
            botaoTempoReal.setBackground(new Color(15, 118, 110));
        }
    }

    private void executarTickTempoReal(ActionEvent ignored) {
        double t = numero(temperatura) + (random.nextDouble() - 0.49) * 0.8;
        double p = numero(pressao) + (random.nextDouble() - 0.49) * 0.6;
        double r = Math.max(0.1, numero(radiacao) + (random.nextDouble() - 0.49) * 0.1);
        double f = numero(fluxoResfriamento) + (random.nextDouble() - 0.49) * 15.0;

        temperatura.setValue(Math.round(t * 10.0) / 10.0);
        pressao.setValue(Math.round(p * 10.0) / 10.0);
        radiacao.setValue(Math.round(r * 10.0) / 10.0);
        fluxoResfriamento.setValue(Math.round(f * 10.0) / 10.0);

        registrarLeituras();
    }

    private void cenarioNormal(ActionEvent ignored) {
        temperatura.setValue(310.5);
        pressao.setValue(155.0);
        radiacao.setValue(2.4);
        fluxoResfriamento.setValue(1100.0);
        registrarLeituras();
    }

    private void cenarioObservacao(ActionEvent ignored) {
        temperatura.setValue(328.0);
        pressao.setValue(155.0);
        radiacao.setValue(2.4);
        fluxoResfriamento.setValue(1100.0);
        registrarLeituras();
    }

    private void cenarioAnomalia(ActionEvent ignored) {
        temperatura.setValue(372.0);
        pressao.setValue(155.0);
        radiacao.setValue(8.4);
        fluxoResfriamento.setValue(420.0);
        registrarLeituras();
    }

    private void cenarioFalhaSensor(ActionEvent ignored) {
        reator.simularFalhaSensor(2L, "Falha de comunicacao / timeout de telemetria");
        atualizarMetricas();
    }

    private void reconhecerAlarmeAcao(ActionEvent ignored) {
        var pendentes = alarmes.consultarAlarmesPendentes();
        if (pendentes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Não há alarmes ativos aguardando validação.", "Status", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        for (Alarme a : pendentes) {
            if (a.isAtivo()) {
                alarmes.reconhecerAlarme(a.getId(), "Operador de Reator", "Alerta validado no painel; equipe em ação corretiva.");
            }
        }
        statusBadge.setText("● ALARME RECONHECIDO (EM TRATAMENTO)");
        statusBadge.setBackground(new Color(120, 53, 15));
        statusBadge.setForeground(new Color(253, 230, 138));
        atualizarMetricas();
    }

    private void resolverAlarmeAcao(ActionEvent ignored) {
        var pendentes = alarmes.consultarAlarmesPendentes();
        if (pendentes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhum alarme pendente para encerramento.", "Status", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        for (Alarme a : pendentes) {
            alarmes.resolverAlarme(a.getId(), "Engenheiro de Seguranca", "Resfriamento restabelecido e parametros estabilizados.");
        }
        temperatura.setValue(310.5);
        pressao.setValue(155.0);
        radiacao.setValue(2.4);
        fluxoResfriamento.setValue(1100.0);
        registrarLeituras();

        statusBadge.setText("● SISTEMA OPERACIONAL ESTÁVEL");
        statusBadge.setBackground(new Color(6, 78, 59));
        statusBadge.setForeground(new Color(167, 243, 208));
        painelReator.setCritico(false);
        atualizarMetricas();
    }

    private void registrarValores(ActionEvent ignored) {
        registrarLeituras();
    }

    private void registrarLeituras() {
        try {
            reator.receberLeitura(1L, numero(temperatura));
            reator.receberLeitura(2L, numero(pressao));
            reator.receberLeitura(3L, numero(radiacao));
            reator.receberLeitura(4L, numero(fluxoResfriamento));
            atualizarMetricas();
        } catch (RuntimeException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Leitura Rejeitada", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void receberEventoNaTela(EventoDominio evento) {
        String origem = "SISTEMA";
        String detalhes = evento.resumo();

        if (evento instanceof MedicaoRegistrada medicao) {
            origem = "SENSOR " + medicao.sensorId();
        } else if (evento instanceof ObservacaoRegistrada obs) {
            origem = "OBSERVACAO " + obs.sensorId();
        } else if (evento instanceof FalhaSensorDetectada falha) {
            origem = "MANUTENÇÃO";
            if (areaAlarmes.getText().startsWith("Nenhum")) {
                areaAlarmes.setText("");
            }
            areaAlarmes.append(
                HORA.format(falha.ocorridoEm()) + " [ALERTA TÉCNICO] " + falha.resumo() + "\n"
            );
        } else if (evento instanceof AlarmeEmitido alarme) {
            origem = "ALARME " + alarme.alarmeId();
            if (areaAlarmes.getText().startsWith("Nenhum")) {
                areaAlarmes.setText("");
            }
            areaAlarmes.append(
                HORA.format(alarme.ocorridoEm()) + " [CRÍTICO] " + alarme.mensagem()
                    + " -> " + String.join(" / ", alarme.destinatarios()) + "\n"
            );
            statusBadge.setText("● ALARME CRÍTICO ATIVO");
            statusBadge.setBackground(new Color(127, 29, 29));
            statusBadge.setForeground(new Color(254, 202, 202));
            painelReator.setCritico(true);
        } else if (evento instanceof AlarmeReconhecido rec) {
            origem = "OPERADOR";
            areaAlarmes.append(
                HORA.format(rec.ocorridoEm()) + " [RECONHECIDO] Alarme " + rec.alarmeId() + " por " + rec.operador() + "\n"
            );
        } else if (evento instanceof AlarmeResolvido res) {
            origem = "ENCERRAMENTO";
            areaAlarmes.append(
                HORA.format(res.ocorridoEm()) + " [RESOLVIDO] Alarme " + res.alarmeId() + " -> " + res.solucao() + "\n"
            );
        }

        boolean integra = auditoria.verificarIntegridadeArquivo();
        modeloEventos.addRow(new Object[] {
            HORA.format(evento.ocorridoEm()),
            evento.tipo(),
            origem,
            detalhes,
            integra ? "OK" : "FALHA"
        });
        int ultima = modeloEventos.getRowCount() - 1;
        tabelaEventos.scrollRectToVisible(tabelaEventos.getCellRect(ultima, 0, true));
        atualizarMetricas();
    }

    private void atualizarMetricas() {
        totalMedicoes.setText(Integer.toString(reator.consultarHistorico().size()));
        totalAlarmes.setText(Integer.toString(alarmes.consultarAlarmes().size()));
        totalAuditoria.setText(Integer.toString(auditoria.consultar().size()));
        boolean ok = auditoria.verificarIntegridadeArquivo();
        integridade.setText(ok ? "OK" : "FALHA");
        integridade.setForeground(ok ? ACCENT_EMERALD : ACCENT_CRIMSON);

        if (!alarmes.temAlarmePendente()) {
            statusBadge.setText("● SISTEMA OPERACIONAL ESTÁVEL");
            statusBadge.setBackground(new Color(6, 78, 59));
            statusBadge.setForeground(new Color(167, 243, 208));
            painelReator.setCritico(false);
        } else if (!alarmes.temAlarmeCriticoAtivo()) {
            statusBadge.setText("● ALARME RECONHECIDO (EM TRATAMENTO)");
            statusBadge.setBackground(new Color(120, 53, 15));
            statusBadge.setForeground(new Color(253, 230, 138));
        }
    }

    private void abrirLog(ActionEvent ignored) {
        try {
            if (!java.nio.file.Files.exists(auditoria.getArquivo())) {
                JOptionPane.showMessageDialog(this, "Registre uma leitura antes de abrir o log.", "Auditoria Vazia", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Desktop.getDesktop().open(auditoria.getArquivo().toFile());
        } catch (IOException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Falha ao abrir log", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static double numero(JSpinner spinner) {
        return ((Number) spinner.getValue()).doubleValue();
    }

    private static JSpinner spinner(double valor, double min, double max, double passo) {
        return new JSpinner(new SpinnerNumberModel(valor, min, max, passo));
    }

    private static void estilizarSpinner(JSpinner spinner) {
        spinner.setPreferredSize(new Dimension(100, 30));
        spinner.setFont(FONT_NUMERIC);
        spinner.setBorder(BorderFactory.createLineBorder(BORDER_SUBTLE));
        Component editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor defaultEditor) {
            defaultEditor.getTextField().setBackground(BG_CARD_LIGHT);
            defaultEditor.getTextField().setForeground(TEXT_PRIMARY);
            defaultEditor.getTextField().setCaretColor(TEXT_PRIMARY);
            defaultEditor.getTextField().setHorizontalAlignment(SwingConstants.RIGHT);
        }
    }

    private static JLabel metricLabel(String texto, Color cor) {
        JLabel label = new JLabel(texto);
        label.setFont(FONT_METRIC_VAL);
        label.setForeground(cor);
        return label;
    }

    // =========================================================================
    // COMPONENTES GRÁFICOS CUSTOMIZADOS (SCADA HUD & DESIGN SYSTEM)
    // =========================================================================

    /** Painel com cantos arredondados e borda sutil antialiasada */
    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color bg;
        private final Color border;

        RoundedPanel(int radius, Color bg, Color border) {
            this.radius = radius;
            this.bg = bg;
            this.border = border;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            if (border != null) {
                g2.setColor(border);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Barra gráfica de telemetria SCADA com preenchimento proporcional */
    private static class SensorLevelBar extends JComponent {
        private final double min;
        private final double max;
        private final double atencao;
        private final String unidade;
        private final Color corPadrao;
        private double valor;

        SensorLevelBar(double min, double max, double atencao, String unidade, Color corPadrao) {
            this.min = min;
            this.max = max;
            this.atencao = atencao;
            this.unidade = unidade;
            this.corPadrao = corPadrao;
            setPreferredSize(new Dimension(100, 16));
            setMinimumSize(new Dimension(100, 16));
        }

        void setValor(double v) {
            this.valor = v;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Track fundo
            g2.setColor(new Color(15, 23, 42));
            g2.fillRoundRect(0, 3, w, h - 6, 6, 6);
            g2.setColor(BORDER_SUBTLE);
            g2.drawRoundRect(0, 3, w - 1, h - 7, 6, 6);

            // Preenchimento proporcional
            double ratio = Math.max(0.0, Math.min(1.0, (valor - min) / (max - min)));
            int fillW = (int) Math.round(ratio * (w - 2));

            Color corBarra = corPadrao;
            if (valor > max || valor < min) {
                corBarra = ACCENT_CRIMSON;
            } else if (valor > atencao) {
                corBarra = ACCENT_AMBER;
            }

            g2.setColor(corBarra);
            if (fillW > 0) {
                g2.fillRoundRect(1, 4, fillW, h - 8, 4, 4);
            }

            g2.dispose();
        }
    }

    /** Visualizador Gráfico HUD do Núcleo do Reator Nuclear (R-01) */
    private static final class ReactorCoreHUD extends JPanel {
        private boolean critico;
        private double anguloRotacao = 0;
        private final Timer animTimer;

        ReactorCoreHUD() {
            setOpaque(true);
            setBackground(BG_CARD);
            setBorder(BorderFactory.createLineBorder(BORDER_SUBTLE, 1));
            setPreferredSize(new Dimension(360, 170));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));

            // Animação contínua sutil do radar
            animTimer = new Timer(50, e -> {
                anguloRotacao = (anguloRotacao + 1.5) % 360.0;
                repaint();
            });
            animTimer.start();
        }

        void setCritico(boolean critico) {
            this.critico = critico;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int cx = 68;
            int cy = getHeight() / 2;
            Color cor = critico ? ACCENT_CRIMSON : ACCENT_EMERALD;

            // Halo externo com pulso
            g.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), critico ? 50 : 25));
            g.fillOval(cx - 52, cy - 52, 104, 104);

            // Anéis concêntricos HUD
            g.setStroke(new BasicStroke(1.5f));
            g.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 120));
            g.drawOval(cx - 44, cy - 44, 88, 88);

            g.setStroke(new BasicStroke(1.0f));
            g.setColor(BORDER_SUBTLE);
            g.drawOval(cx - 34, cy - 34, 68, 68);

            // Arcos rotativos de varredura
            g.setColor(cor);
            g.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(cx - 44, cy - 44, 88, 88, (int) anguloRotacao, 70);
            g.drawArc(cx - 44, cy - 44, 88, 88, (int) anguloRotacao + 180, 70);

            // Pontos das barras de controle
            g.setColor(TEXT_PRIMARY);
            for (int i = 0; i < 4; i++) {
                double rad = Math.toRadians(i * 90 + 45);
                int px = cx + (int) (22 * Math.cos(rad));
                int py = cy + (int) (22 * Math.sin(rad));
                g.fillOval(px - 3, py - 3, 6, 6);
            }

            // Identificador central
            g.setFont(new Font("Consolas", Font.BOLD, 15));
            g.setColor(TEXT_PRIMARY);
            g.drawString("R-01", cx - 18, cy + 5);

            // Painel Textual de Telemetria do Reator
            int tx = 132;
            g.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g.setColor(TEXT_PRIMARY);
            g.drawString("NÚCLEO DO REATOR", tx, 38);

            g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g.setColor(TEXT_MUTED);
            g.drawString("Vaso PWR • 4 Loops Térmicos", tx, 56);

            g.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g.setColor(cor);
            g.drawString(critico ? "● ALERTA GERAL / CRÍTICO" : "● OPERAÇÃO NOMINAL", tx, 82);

            g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g.setColor(TEXT_DIM);
            g.drawString("Potência: 3.411 MWth", tx, 106);
            g.drawString("Barras de Controle: 100%", tx, 124);

            g.dispose();
        }
    }

    public enum IconType {
        NONE, PLAY, PAUSE, WARNING, BOLT, CHECK, REFRESH, FOLDER
    }

    /** Ícones vetoriais desenhados via Java2D garantindo 100% de compatibilidade sem dependência de fontes Unicode */
    private static final class VectorIcon implements javax.swing.Icon {
        private final IconType type;
        private final int width = 14;
        private final int height = 14;

        VectorIcon(IconType type) {
            this.type = type;
        }

        @Override
        public int getIconWidth() {
            return type == IconType.NONE ? 0 : width;
        }

        @Override
        public int getIconHeight() {
            return type == IconType.NONE ? 0 : height;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (type == IconType.NONE) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            int cx = x;
            int cy = y;

            switch (type) {
                case PLAY -> {
                    g2.setColor(Color.WHITE);
                    int[] px = {cx + 2, cx + 12, cx + 2};
                    int[] py = {cy + 1, cy + 7, cy + 13};
                    g2.fillPolygon(px, py, 3);
                }
                case PAUSE -> {
                    g2.setColor(Color.WHITE);
                    g2.fillRect(cx + 2, cy + 2, 3, 10);
                    g2.fillRect(cx + 8, cy + 2, 3, 10);
                }
                case WARNING -> {
                    // Triângulo de advertência âmbar vibrante com exclamação
                    int[] px = {cx + 7, cx + 14, cx};
                    int[] py = {cy + 1, cy + 13, cy + 13};
                    g2.setColor(new Color(254, 240, 138));
                    g2.fillPolygon(px, py, 3);
                    g2.setColor(new Color(153, 27, 27));
                    g2.fillRect(cx + 6, cy + 5, 2, 4);
                    g2.fillRect(cx + 6, cy + 10, 2, 2);
                }
                case BOLT -> {
                    // Raio elétrico amarelo de alta voltagem
                    int[] px = {cx + 8, cx + 3, cx + 8, cx + 5, cx + 13, cx + 7};
                    int[] py = {cy + 1, cy + 7, cy + 7, cy + 13, cy + 6, cy + 6};
                    g2.setColor(new Color(253, 224, 71));
                    g2.fillPolygon(px, py, 6);
                }
                case CHECK -> {
                    // Marca de validação branca
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(cx + 2, cy + 7, cx + 5, cy + 11);
                    g2.drawLine(cx + 5, cy + 11, cx + 12, cy + 3);
                }
                case REFRESH -> {
                    // Seta circular de normalização/reset
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawArc(cx + 1, cy + 2, 10, 10, 50, 270);
                    int[] px = {cx + 7, cx + 12, cx + 8};
                    int[] py = {cy + 1, cy + 3, cy + 6};
                    g2.fillPolygon(px, py, 3);
                }
                case FOLDER -> {
                    // Ícone de pasta amarela
                    g2.setColor(new Color(253, 224, 71));
                    g2.fillRect(cx + 1, cy + 2, 5, 2);
                    g2.fillRect(cx + 1, cy + 4, 12, 7);
                    g2.setColor(new Color(202, 138, 4));
                    g2.drawRect(cx + 1, cy + 4, 11, 6);
                }
                default -> {}
            }
            g2.dispose();
        }
    }

    /** Botão moderno com cantos arredondados, ícones vetoriais nativos e efeito hover */
    private static class ModernButton extends JButton {
        private final Color baseColor;
        private final Color hoverColor;
        private boolean hover = false;

        ModernButton(String texto, IconType iconType, Color baseColor, Color hoverColor) {
            super(texto);
            this.baseColor = baseColor;
            this.hoverColor = hoverColor;
            if (iconType != null && iconType != IconType.NONE) {
                setIcon(new VectorIcon(iconType));
                setIconTextGap(7);
            }
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(8, 14, 8, 14));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        void setIconType(IconType iconType) {
            if (iconType != null && iconType != IconType.NONE) {
                setIcon(new VectorIcon(iconType));
                setIconTextGap(7);
            } else {
                setIcon(null);
            }
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(hover ? hoverColor : baseColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // =========================================================================
    // RENDERIZADORES DE TABELA (BADGES COLORIDOS)
    // =========================================================================

    private static class CenterRenderer extends DefaultTableCellRenderer {
        CenterRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(new Font("Consolas", Font.PLAIN, 12));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setForeground(TEXT_MUTED);
            c.setBackground(row % 2 == 0 ? BG_CARD : new Color(15, 23, 42));
            return c;
        }
    }

    private static class EventTypeBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String str = value != null ? value.toString() : "";
            lbl.setFont(FONT_BADGE);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);

            Color badgeBg = new Color(30, 41, 59);
            Color badgeFg = TEXT_PRIMARY;

            if (str.contains("MEDICAO")) {
                badgeBg = new Color(8, 51, 68);
                badgeFg = new Color(56, 189, 248);
            } else if (str.contains("ALARME_EMITIDO")) {
                badgeBg = new Color(127, 29, 29);
                badgeFg = new Color(254, 202, 202);
            } else if (str.contains("OBSERVACAO")) {
                badgeBg = new Color(120, 53, 15);
                badgeFg = new Color(253, 230, 138);
            } else if (str.contains("FALHA")) {
                badgeBg = new Color(88, 28, 135);
                badgeFg = new Color(233, 213, 255);
            } else if (str.contains("RECONHECIDO")) {
                badgeBg = new Color(154, 52, 18);
                badgeFg = new Color(254, 215, 170);
            } else if (str.contains("RESOLVIDO")) {
                badgeBg = new Color(6, 78, 59);
                badgeFg = new Color(167, 243, 208);
            }

            lbl.setOpaque(true);
            lbl.setBackground(badgeBg);
            lbl.setForeground(badgeFg);
            return lbl;
        }
    }

    private static class OriginBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            lbl.setFont(new Font("Consolas", Font.BOLD, 11));
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setOpaque(true);
            lbl.setBackground(row % 2 == 0 ? BG_CARD : new Color(15, 23, 42));
            lbl.setForeground(TEXT_PRIMARY);
            return lbl;
        }
    }

    private static class IntegrityBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String str = value != null ? value.toString() : "";
            lbl.setFont(FONT_BADGE);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setOpaque(true);

            if ("OK".equalsIgnoreCase(str)) {
                lbl.setBackground(new Color(6, 78, 59));
                lbl.setForeground(new Color(167, 243, 208));
            } else {
                lbl.setBackground(new Color(127, 29, 29));
                lbl.setForeground(new Color(254, 202, 202));
            }
            return lbl;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SistemaMvpUI().setVisible(true);
        });
    }
}
