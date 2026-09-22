package com.parking.webapp.views;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.parking.webapp.dto.ExitResult;
import com.parking.webapp.model.TicketModel;
import com.parking.webapp.service.ParkingWebService;
import com.parking.webapp.views.components.QrScannerComponent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Control de Salida | SAP Business One")
@Route(value = "salida", layout = MainLayout.class)
public class ExitView extends VerticalLayout {

    private final ParkingWebService parkingService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final QrScannerComponent qrScanner = new QrScannerComponent();
    private final Div statusContainer = new Div();
    private final TextField manualExitCodeField = new TextField();
    private boolean isProcessing = false;

    public ExitView(ParkingWebService parkingService) {
        this.parkingService = parkingService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setMaxWidth("1250px");
        getStyle().set("margin", "0 auto");
        getStyle().set("padding-top", "1.5rem");

        createHeaderSection();
        createMainLayout();
    }

    private void createHeaderSection() {
        Div headerPanel = new Div();
        headerPanel.addClassName("sap-panel");
        headerPanel.setWidthFull();

        H2 title = new H2("Control de Salida y Apertura de Barrera");
        title.getStyle().set("color", "#102a43");
        title.getStyle().set("font-size", "1.45rem");
        title.getStyle().set("font-weight", "600");
        title.getStyle().set("margin", "0 0 0.25rem 0");

        Paragraph subtitle = new Paragraph("Aproxime el código QR de su ticket pagado frente al lector óptico para autorizar la apertura de la talanquera.");
        subtitle.getStyle().set("color", "var(--sap-text-muted)");
        subtitle.getStyle().set("margin", "0");
        subtitle.getStyle().set("font-size", "0.9rem");

        headerPanel.add(new VerticalLayout(title, subtitle));
        add(headerPanel);
    }

    private void createMainLayout() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setSpacing(true);

        // Columna Izquierda: Escáner óptico
        VerticalLayout leftSide = new VerticalLayout();
        leftSide.setWidth("50%");
        leftSide.setSpacing(true);
        leftSide.setPadding(false);

        Div scannerPanel = new Div();
        scannerPanel.addClassName("sap-panel");
        scannerPanel.setWidthFull();

        Div scannerHeader = new Div();
        scannerHeader.addClassName("sap-panel-header");

        H3 scannerTitle = new H3("Lector Óptico de Salida (Cámara)");
        scannerTitle.addClassName("sap-panel-title");

        Button restartBtn = new Button("Reconectar Lector", VaadinIcon.CAMERA.create(), e -> {
            isProcessing = false;
            qrScanner.startScanning();
            showIdleStatus();
        });
        restartBtn.addClassName("sap-btn-secondary");

        scannerHeader.add(scannerTitle, restartBtn);

        qrScanner.setOnScanListener(this::handleExitScan);
        qrScanner.setOnErrorListener(err -> System.err.println("Exit Scanner notice: " + err));

        scannerPanel.add(scannerHeader, qrScanner);

        // Panel de Entrada Manual
        Div manualPanel = new Div();
        manualPanel.addClassName("sap-panel");
        manualPanel.setWidthFull();

        Div manualHeader = new Div();
        manualHeader.addClassName("sap-panel-header");
        H3 manualTitle = new H3("Validación Manual / Terminal Auxiliar");
        manualTitle.addClassName("sap-panel-title");
        manualHeader.add(manualTitle);

        manualExitCodeField.setPlaceholder("Ingrese código UUID de ticket...");
        manualExitCodeField.setWidthFull();
        manualExitCodeField.setClearButtonVisible(true);

        Button validateBtn = new Button("Validar Salida", VaadinIcon.CHECK.create(), e -> {
            String code = manualExitCodeField.getValue();
            if (code != null && !code.isBlank()) {
                isProcessing = false;
                handleExitScan(code.trim());
            }
        });
        validateBtn.addClassName("sap-btn-primary");

        HorizontalLayout manualLayout = new HorizontalLayout(manualExitCodeField, validateBtn);
        manualLayout.setWidthFull();
        manualLayout.setFlexGrow(1, manualExitCodeField);
        manualLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

        // Botones de prueba rápida
        HorizontalLayout quickButtons = new HorizontalLayout();
        quickButtons.setSpacing(true);
        quickButtons.setAlignItems(FlexComponent.Alignment.CENTER);
        quickButtons.getStyle().set("margin-top", "0.75rem");

        Span quickLabel = new Span("Pruebas Rápidas:");
        quickLabel.getStyle().set("font-size", "0.85rem");
        quickLabel.getStyle().set("color", "var(--sap-text-muted)");
        quickLabel.getStyle().set("font-weight", "600");

        Button testPaid = new Button("DEMO-PAID-002 (Pagado)", e -> {
            manualExitCodeField.setValue("DEMO-PAID-002");
            isProcessing = false;
            handleExitScan("DEMO-PAID-002");
        });
        testPaid.addClassName("sap-btn-secondary");

        Button testPending = new Button("DEMO-PENDING-001 (No Pagado)", e -> {
            manualExitCodeField.setValue("DEMO-PENDING-001");
            isProcessing = false;
            handleExitScan("DEMO-PENDING-001");
        });
        testPending.addClassName("sap-btn-secondary");

        quickButtons.add(quickLabel, testPaid, testPending);

        manualPanel.add(manualHeader, manualLayout, quickButtons);

        leftSide.add(scannerPanel, manualPanel);

        // Columna Derecha: Estado de la Barrera
        VerticalLayout rightSide = new VerticalLayout();
        rightSide.setWidth("50%");
        rightSide.setSpacing(true);
        rightSide.setPadding(false);

        statusContainer.setWidthFull();
        showIdleStatus();

        rightSide.add(statusContainer);

        layout.add(leftSide, rightSide);
        add(layout);
    }

    private void showIdleStatus() {
        statusContainer.removeAll();

        Div idleBox = new Div();
        idleBox.addClassName("sap-panel");
        idleBox.getStyle().set("text-align", "center");
        idleBox.getStyle().set("padding", "3.5rem 2rem");

        Span icon = new Span("🚧");
        icon.getStyle().set("font-size", "3.5rem");

        H3 title = new H3("Talanquera en Espera");
        title.getStyle().set("margin-top", "1rem");
        title.getStyle().set("color", "var(--sap-text)");

        Paragraph p = new Paragraph("Enfoque su ticket frente a la cámara. El sistema verificará la liquidación del pago y accionará la barrera de salida automáticamente.");
        p.getStyle().set("color", "var(--sap-text-secondary)");
        p.getStyle().set("font-size", "0.9rem");

        Span barrierState = new Span("ESTADO DE BARRERA: CERRADA");
        barrierState.addClassName("sap-badge");
        barrierState.addClassName("warning");
        barrierState.getStyle().set("margin-top", "1rem");

        idleBox.add(icon, title, p, barrierState);
        statusContainer.add(idleBox);
    }

    private void handleExitScan(String scannedCode) {
        if (isProcessing) {
            return;
        }
        isProcessing = true;

        getUI().ifPresent(ui -> ui.access(() -> {
            manualExitCodeField.setValue(scannedCode);
            ExitResult result = parkingService.processExit(scannedCode);
            if (result.success()) {
                showSuccessStatus(result.ticket());
            } else {
                showDeniedStatus(scannedCode, result.message());
            }
        }));
    }

    private void showSuccessStatus(TicketModel ticket) {
        statusContainer.removeAll();

        Div banner = new Div();
        banner.addClassName("sap-alert-banner");
        banner.addClassName("success");

        Span icon = new Span("✅");
        icon.getStyle().set("font-size", "3.5rem");

        H2 title = new H2("SALIDA AUTORIZADA");
        title.getStyle().set("color", "var(--sap-success)");
        title.getStyle().set("margin", "0.5rem 0");
        title.getStyle().set("font-weight", "700");

        H3 barrierMsg = new H3("Talanquera Abierta - Buen Viaje");
        barrierMsg.getStyle().set("color", "var(--sap-text)");
        barrierMsg.getStyle().set("margin", "0.25rem 0 1rem 0");

        Paragraph details = new Paragraph(
                "Ticket: " + ticket.getUuid() + " | Salida registrada: " + formatter.format(Instant.now())
        );
        details.getStyle().set("color", "var(--sap-text-muted)");
        details.getStyle().set("font-size", "0.9rem");

        Span timerMsg = new Span("Reanudando escáner en 5 segundos...");
        timerMsg.getStyle().set("color", "var(--sap-success)");
        timerMsg.getStyle().set("font-size", "0.85rem");
        timerMsg.getStyle().set("font-weight", "600");

        banner.add(icon, title, barrierMsg, details, timerMsg);
        statusContainer.add(banner);

        // Auto-reinicio tras 5 segundos
        UI currentUI = UI.getCurrent();
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                if (currentUI != null && currentUI.isAttached()) {
                    currentUI.access(() -> {
                        isProcessing = false;
                        showIdleStatus();
                        qrScanner.startScanning();
                    });
                }
            } catch (InterruptedException ignored) {
            }
        }).start();
    }

    private void showDeniedStatus(String scannedCode, String reason) {
        statusContainer.removeAll();

        Div banner = new Div();
        banner.addClassName("sap-alert-banner");
        banner.addClassName("danger");

        Span icon = new Span("🚫");
        icon.getStyle().set("font-size", "3.5rem");

        H2 title = new H2("SALIDA DENEGADA");
        title.getStyle().set("color", "var(--sap-danger)");
        title.getStyle().set("margin", "0.5rem 0");
        title.getStyle().set("font-weight", "700");

        H3 reasonMsg = new H3(reason);
        reasonMsg.getStyle().set("color", "var(--sap-text)");
        reasonMsg.getStyle().set("margin", "0.25rem 0 1.5rem 0");

        Button payRedirectBtn = new Button("Ir al Módulo de Cobro", VaadinIcon.CREDIT_CARD.create(), e -> {
            getUI().ifPresent(ui -> ui.navigate(PaymentView.class));
        });
        payRedirectBtn.addClassName("sap-btn-primary");

        Button retryBtn = new Button("Intentar de Nuevo", VaadinIcon.REFRESH.create(), e -> {
            isProcessing = false;
            showIdleStatus();
            qrScanner.startScanning();
        });
        retryBtn.addClassName("sap-btn-secondary");

        HorizontalLayout actionRow = new HorizontalLayout(payRedirectBtn, retryBtn);
        actionRow.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        actionRow.setSpacing(true);

        banner.add(icon, title, reasonMsg, actionRow);
        statusContainer.add(banner);
    }
}
