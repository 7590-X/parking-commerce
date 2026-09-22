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
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
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

@PageTitle("Salida de Parqueo | SmartParking")
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
        setMaxWidth("1200px");
        getStyle().set("margin", "0 auto");

        createHeaderSection();
        createKioskScannerSection();
        createManualFallbackSection();
    }

    private void createHeaderSection() {
        H1 title = new H1("Terminal de Salida y Apertura de Barrera");
        title.getStyle().set("margin-bottom", "0.25rem");
        title.getStyle().set("font-size", "2.2rem");

        Paragraph subtitle = new Paragraph("Presente el código QR de su ticket pagado frente a la cámara para abrir la talanquera.");
        subtitle.getStyle().set("color", "var(--text-muted)");
        subtitle.getStyle().set("margin-top", "0");

        add(new VerticalLayout(title, subtitle));
    }

    private void createKioskScannerSection() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setSpacing(true);

        // Columna Izquierda: Escáner de cámara continuo
        VerticalLayout leftSide = new VerticalLayout();
        leftSide.setWidth("50%");
        leftSide.setSpacing(true);
        leftSide.setPadding(false);

        Div scannerWrapper = new Div();
        scannerWrapper.addClassName("qr-scanner-wrapper");
        scannerWrapper.setWidthFull();

        H2 scannerTitle = new H2("Cámara de Salida Vehicular");
        scannerTitle.getStyle().set("font-size", "1.2rem");
        scannerTitle.getStyle().set("margin-top", "0");

        qrScanner.setOnScanListener(this::handleExitScan);
        qrScanner.setOnErrorListener(err -> System.err.println("Exit Scanner info: " + err));

        scannerWrapper.add(scannerTitle, qrScanner);

        Button restartBtn = new Button("Reiniciar Cámara", VaadinIcon.CAMERA.create(), e -> {
            isProcessing = false;
            qrScanner.startScanning();
            showIdleStatus();
        });
        restartBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);

        leftSide.add(scannerWrapper, restartBtn);

        // Columna Derecha: Estado de la barrera / Autorización
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
        idleBox.addClassName("ticket-receipt-card");
        idleBox.getStyle().set("text-align", "center");
        idleBox.getStyle().set("padding", "3.5rem 1.5rem");

        Span icon = new Span("🚧");
        icon.getStyle().set("font-size", "3.5rem");

        H2 title = new H2("Barrera en Espera");
        title.getStyle().set("margin-top", "1rem");
        title.getStyle().set("color", "var(--text-muted)");

        Paragraph p = new Paragraph("Enfoque su ticket frente a la cámara. El sistema verificará el pago y abrirá la talanquera automáticamente.");
        p.getStyle().set("color", "var(--text-subtle)");

        Span barrierState = new Span("ESTADO DE BARRERA: CERRADA");
        barrierState.addClassName("status-pill");
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
        banner.addClassName("exit-banner-success");

        Span icon = new Span("✅");
        icon.getStyle().set("font-size", "4rem");

        H1 title = new H1("¡SALIDA AUTORIZADA!");
        title.getStyle().set("color", "#34d399");
        title.getStyle().set("margin", "0.5rem 0");

        H3 barrierMsg = new H3("🚧 Talanquera Abierta - Buen Viaje");
        barrierMsg.getStyle().set("color", "#f8fafc");
        barrierMsg.getStyle().set("margin", "0.25rem 0 1.5rem 0");

        Paragraph details = new Paragraph(
                "Ticket: " + ticket.getUuid() + " | Salida registrada: " + formatter.format(Instant.now())
        );
        details.getStyle().set("color", "var(--text-muted)");
        details.getStyle().set("font-size", "0.9rem");

        Span timerMsg = new Span("La barrera se cerrará y la terminal se reanudará en unos momentos...");
        timerMsg.getStyle().set("color", "#a7f3d0");
        timerMsg.getStyle().set("font-size", "0.85rem");

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
        banner.addClassName("exit-banner-danger");

        Span icon = new Span("🚫");
        icon.getStyle().set("font-size", "4rem");

        H1 title = new H1("SALIDA DENEGADA");
        title.getStyle().set("color", "#f87171");
        title.getStyle().set("margin", "0.5rem 0");

        H3 reasonMsg = new H3(reason);
        reasonMsg.getStyle().set("color", "#ffffff");
        reasonMsg.getStyle().set("margin", "0.5rem 0 1.5rem 0");

        Button payRedirectBtn = new Button("Ir al Módulo de Pago", VaadinIcon.CREDIT_CARD.create(), e -> {
            getUI().ifPresent(ui -> ui.navigate(PaymentView.class));
        });
        payRedirectBtn.addClassName("btn-success-glow");

        Button retryBtn = new Button("Intentar de Nuevo", VaadinIcon.REFRESH.create(), e -> {
            isProcessing = false;
            showIdleStatus();
            qrScanner.startScanning();
        });
        retryBtn.getStyle().set("margin-left", "1rem");

        HorizontalLayout actionRow = new HorizontalLayout(payRedirectBtn, retryBtn);
        actionRow.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        banner.add(icon, title, reasonMsg, actionRow);
        statusContainer.add(banner);
    }

    private void createManualFallbackSection() {
        Div manualSection = new Div();
        manualSection.addClassName("metric-card");
        manualSection.setWidthFull();

        Span manualTitle = new Span("Validación Manual o Pruebas:");
        manualTitle.getStyle().set("font-weight", "600");
        manualTitle.getStyle().set("font-size", "0.9rem");
        manualTitle.getStyle().set("color", "var(--text-muted)");

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
        validateBtn.addClassName("btn-primary-glow");

        HorizontalLayout manualLayout = new HorizontalLayout(manualExitCodeField, validateBtn);
        manualLayout.setWidthFull();
        manualLayout.setFlexGrow(1, manualExitCodeField);

        HorizontalLayout quickButtons = new HorizontalLayout();
        quickButtons.setSpacing(true);
        quickButtons.getStyle().set("margin-top", "0.5rem");

        Button testPaid = new Button("Probar DEMO-PAID-002 (Pagado)", e -> {
            manualExitCodeField.setValue("DEMO-PAID-002");
            isProcessing = false;
            handleExitScan("DEMO-PAID-002");
        });
        testPaid.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        Button testPending = new Button("Probar DEMO-PENDING-001 (No Pagado)", e -> {
            manualExitCodeField.setValue("DEMO-PENDING-001");
            isProcessing = false;
            handleExitScan("DEMO-PENDING-001");
        });
        testPending.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        quickButtons.add(new Span("Pruebas Rápidas:"), testPaid, testPending);

        manualSection.add(manualTitle, manualLayout, quickButtons);
        add(manualSection);
    }
}
