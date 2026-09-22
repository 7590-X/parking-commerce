package com.parking.webapp.views;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import com.parking.webapp.model.TicketModel;
import com.parking.webapp.service.ParkingWebService;
import com.parking.webapp.views.components.QrScannerComponent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Pago de Ticket | SmartParking")
@Route(value = "pago", layout = MainLayout.class)
public class PaymentView extends VerticalLayout {

    private final ParkingWebService parkingService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final QrScannerComponent qrScanner = new QrScannerComponent();
    private final TextField manualCodeField = new TextField();
    private final Div receiptContainer = new Div();
    private TicketModel currentTicket = null;

    public PaymentView(ParkingWebService parkingService) {
        this.parkingService = parkingService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setMaxWidth("1200px");
        getStyle().set("margin", "0 auto");

        createHeaderSection();
        createMainContent();
    }

    private void createHeaderSection() {
        H1 title = new H1("Módulo de Cobro y Pago de Ticket");
        title.getStyle().set("margin-bottom", "0.25rem");
        title.getStyle().set("font-size", "2.2rem");

        Paragraph subtitle = new Paragraph("Aproxime el código QR del ticket a la cámara web o ingrese el identificador manualmente.");
        subtitle.getStyle().set("color", "var(--text-muted)");
        subtitle.getStyle().set("margin-top", "0");

        add(new VerticalLayout(title, subtitle));
    }

    private void createMainContent() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setSpacing(true);

        // Columna izquierda: Cámara y Escáner QR
        VerticalLayout leftColumn = new VerticalLayout();
        leftColumn.setWidth("50%");
        leftColumn.setSpacing(true);
        leftColumn.setPadding(false);

        Div scannerWrapper = new Div();
        scannerWrapper.addClassName("qr-scanner-wrapper");
        scannerWrapper.setWidthFull();

        H2 scannerHeader = new H2("Escáner de Ticket QR");
        scannerHeader.getStyle().set("font-size", "1.2rem");
        scannerHeader.getStyle().set("margin-top", "0");
        scannerHeader.getStyle().set("color", "var(--text-main)");

        qrScanner.setOnScanListener(this::handleQrScanned);
        qrScanner.setOnErrorListener(err -> {
            // Error silencioso o advertencia suave
            System.err.println("QR Scanner info: " + err);
        });

        scannerWrapper.add(scannerHeader, qrScanner);

        // Controles de cámara y entrada manual
        HorizontalLayout cameraControls = new HorizontalLayout();
        cameraControls.setWidthFull();
        cameraControls.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        Button restartCameraBtn = new Button("Reiniciar Cámara", VaadinIcon.CAMERA.create(), e -> qrScanner.startScanning());
        restartCameraBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);

        cameraControls.add(restartCameraBtn);

        // Entrada manual como respaldo
        Div manualSection = new Div();
        manualSection.addClassName("metric-card");
        manualSection.setWidthFull();

        Span manualTitle = new Span("Entrada Manual o Pruebas:");
        manualTitle.getStyle().set("font-weight", "600");
        manualTitle.getStyle().set("font-size", "0.9rem");
        manualTitle.getStyle().set("color", "var(--text-muted)");

        manualCodeField.setPlaceholder("Pegue o escriba UUID...");
        manualCodeField.setWidthFull();
        manualCodeField.setClearButtonVisible(true);

        Button searchBtn = new Button("Consultar", VaadinIcon.SEARCH.create(), e -> {
            String code = manualCodeField.getValue();
            if (code != null && !code.isBlank()) {
                handleQrScanned(code.trim());
            } else {
                Notification.show("Ingrese un código de ticket", 2000, Notification.Position.MIDDLE);
            }
        });
        searchBtn.addClassName("btn-primary-glow");

        HorizontalLayout manualInputLayout = new HorizontalLayout(manualCodeField, searchBtn);
        manualInputLayout.setWidthFull();
        manualInputLayout.setFlexGrow(1, manualCodeField);

        // Atajos rápidos para probar
        HorizontalLayout testBadges = new HorizontalLayout();
        testBadges.setSpacing(true);
        testBadges.getStyle().set("margin-top", "0.5rem");

        Button demoPendingBtn = new Button("DEMO-PENDING-001", e -> {
            manualCodeField.setValue("DEMO-PENDING-001");
            handleQrScanned("DEMO-PENDING-001");
        });
        demoPendingBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        Button demoPaidBtn = new Button("DEMO-PAID-002", e -> {
            manualCodeField.setValue("DEMO-PAID-002");
            handleQrScanned("DEMO-PAID-002");
        });
        demoPaidBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        testBadges.add(new Span("Pruebas:"), demoPendingBtn, demoPaidBtn);

        manualSection.add(manualTitle, manualInputLayout, testBadges);

        leftColumn.add(scannerWrapper, cameraControls, manualSection);

        // Columna derecha: Recibo de cobro
        VerticalLayout rightColumn = new VerticalLayout();
        rightColumn.setWidth("50%");
        rightColumn.setSpacing(true);
        rightColumn.setPadding(false);

        receiptContainer.setWidthFull();
        showWaitingReceipt();

        rightColumn.add(receiptContainer);

        layout.add(leftColumn, rightColumn);
        add(layout);
    }

    private void showWaitingReceipt() {
        receiptContainer.removeAll();
        Div placeholder = new Div();
        placeholder.addClassName("ticket-receipt-card");
        placeholder.getStyle().set("text-align", "center");
        placeholder.getStyle().set("padding", "3rem 1.5rem");

        Span icon = new Span("🎫");
        icon.getStyle().set("font-size", "3.5rem");

        H2 prompt = new H2("Esperando Lectura de Ticket");
        prompt.getStyle().set("color", "var(--text-muted)");
        prompt.getStyle().set("font-size", "1.25rem");
        prompt.getStyle().set("margin-top", "1rem");

        Paragraph desc = new Paragraph("Alinee el código QR en el lector o ingrese el código para ver el desglose y proceder al cobro.");
        desc.getStyle().set("color", "var(--text-subtle)");
        desc.getStyle().set("font-size", "0.9rem");

        placeholder.add(icon, prompt, desc);
        receiptContainer.add(placeholder);
    }

    private void handleQrScanned(String scannedCode) {
        getUI().ifPresent(ui -> ui.access(() -> {
            Optional<TicketModel> optTicket = parkingService.findTicket(scannedCode);
            if (optTicket.isEmpty()) {
                Notification n = Notification.show("❌ Ticket no encontrado: " + scannedCode, 3000, Notification.Position.TOP_CENTER);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                showWaitingReceipt();
                return;
            }

            this.currentTicket = optTicket.get();
            manualCodeField.setValue(currentTicket.getUuid());
            renderTicketReceipt(this.currentTicket);
            Notification.show("Ticket detectado: " + currentTicket.getUuid(), 2000, Notification.Position.BOTTOM_END);
        }));
    }

    private void renderTicketReceipt(TicketModel ticket) {
        receiptContainer.removeAll();

        Div card = new Div();
        card.addClassName("ticket-receipt-card");

        // Encabezado
        Div header = new Div();
        header.addClassName("receipt-header");

        H2 receiptTitle = new H2("Recibo de Estacionamiento");
        receiptTitle.getStyle().set("margin", "0");
        receiptTitle.getStyle().set("font-size", "1.4rem");

        Span statusBadge = new Span();
        statusBadge.addClassName("status-pill");
        if (ticket.isPayed()) {
            statusBadge.addClassName("success");
            statusBadge.setText("✅ PAGADO");
        } else {
            statusBadge.addClassName("warning");
            statusBadge.setText("⏳ PENDIENTE DE PAGO");
        }

        HorizontalLayout headerRow = new HorizontalLayout(receiptTitle, statusBadge);
        headerRow.setWidthFull();
        headerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        header.add(headerRow);

        // Desglose
        Div body = new Div();

        body.add(createReceiptRow("Código Ticket:", ticket.getUuid()));

        Instant entry = ticket.getEntryTime() != null ? ticket.getEntryTime() : Instant.now();
        body.add(createReceiptRow("Hora de Entrada:", formatter.format(entry)));

        Instant now = Instant.now();
        long minutes = Math.max(1, Duration.between(entry, now).toMinutes());
        long hours = minutes / 60;
        long remMinutes = minutes % 60;
        String elapsedStr = (hours > 0 ? hours + " h " : "") + remMinutes + " min";
        body.add(createReceiptRow("Tiempo Transcurrido:", elapsedStr));

        // Tarifa
        long billableHours = (long) Math.ceil(minutes / 60.0);
        float calculatedAmount = ticket.getAmount() > 0 ? ticket.getAmount() : (10.0f + Math.max(0, billableHours - 1) * 5.0f);

        body.add(createReceiptRow("Tarifa Aplicada:", "Q10.00 base + Q5.00/h"));

        if (ticket.isPayed() && ticket.getPaymentTime() != null) {
            body.add(createReceiptRow("Fecha de Pago:", formatter.format(ticket.getPaymentTime())));
        }

        // Total
        Div totalRow = new Div();
        totalRow.addClassName("receipt-row");
        totalRow.addClassName("receipt-total");

        Span totalLabel = new Span("TOTAL A PAGAR:");
        totalLabel.getStyle().set("font-size", "1.1rem");
        totalLabel.getStyle().set("font-weight", "700");

        Span totalAmount = new Span(String.format("Q%.2f", calculatedAmount));
        totalAmount.addClassName("amount-highlight");

        totalRow.add(totalLabel, totalAmount);
        body.add(totalRow);

        // Acciones
        HorizontalLayout actions = new HorizontalLayout();
        actions.setWidthFull();
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        actions.getStyle().set("margin-top", "1.5rem");

        if (!ticket.isPayed()) {
            Button payBtn = new Button("Pagar Ticket (" + String.format("Q%.2f", calculatedAmount) + ")", VaadinIcon.CREDIT_CARD.create(), e -> {
                try {
                    TicketModel paid = parkingService.payTicket(ticket.getUuid());
                    this.currentTicket = paid;
                    renderTicketReceipt(paid);

                    Notification n = Notification.show("✅ ¡Pago procesado con éxito! Ticket marcado como pagado.", 4000, Notification.Position.TOP_CENTER);
                    n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } catch (Exception ex) {
                    Notification n = Notification.show("Error al procesar pago: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER);
                    n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            payBtn.addClassName("btn-success-glow");
            actions.add(payBtn);
        } else {
            Button alreadyPaidBtn = new Button("Ticket Ya Pagado", VaadinIcon.CHECK_CIRCLE.create());
            alreadyPaidBtn.setEnabled(false);
            alreadyPaidBtn.addClassName("status-pill");
            alreadyPaidBtn.addClassName("success");

            Button clearBtn = new Button("Escanear Otro Ticket", VaadinIcon.ARROW_RIGHT.create(), e -> showWaitingReceipt());
            clearBtn.addClassName("btn-primary-glow");

            actions.add(alreadyPaidBtn, clearBtn);
        }

        card.add(header, body, actions);
        receiptContainer.add(card);
    }

    private Div createReceiptRow(String label, String value) {
        Div row = new Div();
        row.addClassName("receipt-row");

        Span labelSpan = new Span(label);
        labelSpan.addClassName("receipt-label");

        Span valueSpan = new Span(value);
        valueSpan.addClassName("receipt-value");

        row.add(labelSpan, valueSpan);
        return row;
    }
}
