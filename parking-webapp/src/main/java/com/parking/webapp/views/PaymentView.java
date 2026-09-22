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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
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

@PageTitle("Cobro de Ticket | SAP Business One")
@Route(value = "pago", layout = MainLayout.class)
public class PaymentView extends VerticalLayout {

    private final ParkingWebService parkingService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final QrScannerComponent qrScanner = new QrScannerComponent();
    private final TextField manualCodeField = new TextField();
    private final Div invoiceContainer = new Div();
    private TicketModel currentTicket = null;

    public PaymentView(ParkingWebService parkingService) {
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

        H2 title = new H2("Caja y Liquidación de Tickets");
        title.getStyle().set("color", "#102a43");
        title.getStyle().set("font-size", "1.45rem");
        title.getStyle().set("font-weight", "600");
        title.getStyle().set("margin", "0 0 0.25rem 0");

        Paragraph subtitle = new Paragraph("Presente el código QR impreso en el ticket frente al escáner óptico o digite el identificador.");
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

        // Columna Izquierda: Escáner y Consulta Manual
        VerticalLayout leftCol = new VerticalLayout();
        leftCol.setWidth("50%");
        leftCol.setSpacing(true);
        leftCol.setPadding(false);

        // Panel de Cámara
        Div scannerPanel = new Div();
        scannerPanel.addClassName("sap-panel");
        scannerPanel.setWidthFull();

        Div scannerHeader = new Div();
        scannerHeader.addClassName("sap-panel-header");
        H3 scannerTitle = new H3("Lector Óptico QR (Cámara)");
        scannerTitle.addClassName("sap-panel-title");

        Button restartCameraBtn = new Button("Reconectar Cámara", VaadinIcon.CAMERA.create(), e -> qrScanner.startScanning());
        restartCameraBtn.addClassName("sap-btn-secondary");

        scannerHeader.add(scannerTitle, restartCameraBtn);

        qrScanner.setOnScanListener(this::handleQrScanned);
        qrScanner.setOnErrorListener(err -> System.err.println("QR Scanner notice: " + err));

        scannerPanel.add(scannerHeader, qrScanner);

        // Panel de Entrada Manual
        Div manualPanel = new Div();
        manualPanel.addClassName("sap-panel");
        manualPanel.setWidthFull();

        Div manualHeader = new Div();
        manualHeader.addClassName("sap-panel-header");
        H3 manualTitle = new H3("Búsqueda Manual de Ticket");
        manualTitle.addClassName("sap-panel-title");
        manualHeader.add(manualTitle);

        manualCodeField.setPlaceholder("Ingrese código UUID del ticket...");
        manualCodeField.setWidthFull();
        manualCodeField.setClearButtonVisible(true);

        Button searchBtn = new Button("Consultar Ticket", VaadinIcon.SEARCH.create(), e -> {
            String code = manualCodeField.getValue();
            if (code != null && !code.isBlank()) {
                handleQrScanned(code.trim());
            } else {
                Notification.show("Ingrese un código de ticket", 2000, Notification.Position.MIDDLE);
            }
        });
        searchBtn.addClassName("sap-btn-primary");

        HorizontalLayout inputRow = new HorizontalLayout(manualCodeField, searchBtn);
        inputRow.setWidthFull();
        inputRow.setFlexGrow(1, manualCodeField);
        inputRow.setAlignItems(FlexComponent.Alignment.BASELINE);

        // Accesos de prueba rápida
        HorizontalLayout demoRow = new HorizontalLayout();
        demoRow.setSpacing(true);
        demoRow.setAlignItems(FlexComponent.Alignment.CENTER);
        demoRow.getStyle().set("margin-top", "0.75rem");

        Span demoLabel = new Span("Tickets Demo:");
        demoLabel.getStyle().set("font-size", "0.85rem");
        demoLabel.getStyle().set("color", "var(--sap-text-muted)");
        demoLabel.getStyle().set("font-weight", "600");

        Button btnDemo1 = new Button("DEMO-PENDING-001 (Pendiente)", e -> {
            manualCodeField.setValue("DEMO-PENDING-001");
            handleQrScanned("DEMO-PENDING-001");
        });
        btnDemo1.addClassName("sap-btn-secondary");

        Button btnDemo2 = new Button("DEMO-PAID-002 (Pagado)", e -> {
            manualCodeField.setValue("DEMO-PAID-002");
            handleQrScanned("DEMO-PAID-002");
        });
        btnDemo2.addClassName("sap-btn-secondary");

        demoRow.add(demoLabel, btnDemo1, btnDemo2);

        manualPanel.add(manualHeader, inputRow, demoRow);

        leftCol.add(scannerPanel, manualPanel);

        // Columna Derecha: Recibo de Liquidación
        VerticalLayout rightCol = new VerticalLayout();
        rightCol.setWidth("50%");
        rightCol.setSpacing(true);
        rightCol.setPadding(false);

        invoiceContainer.setWidthFull();
        showEmptyInvoiceState();

        rightCol.add(invoiceContainer);

        layout.add(leftCol, rightCol);
        add(layout);
    }

    private void showEmptyInvoiceState() {
        invoiceContainer.removeAll();

        Div panel = new Div();
        panel.addClassName("sap-panel");
        panel.getStyle().set("text-align", "center");
        panel.getStyle().set("padding", "4rem 2rem");

        Span icon = new Span("📄");
        icon.getStyle().set("font-size", "3.5rem");

        H3 title = new H3("Sin Ticket Seleccionado");
        title.getStyle().set("color", "var(--sap-text-muted)");
        title.getStyle().set("margin", "1rem 0 0.5rem 0");

        Paragraph desc = new Paragraph("Enfoque un código QR en el lector óptico o digite el código manual para visualizar los detalles de cobro.");
        desc.getStyle().set("color", "var(--sap-text-secondary)");
        desc.getStyle().set("font-size", "0.9rem");

        panel.add(icon, title, desc);
        invoiceContainer.add(panel);
    }

    private void handleQrScanned(String code) {
        getUI().ifPresent(ui -> ui.access(() -> {
            Optional<TicketModel> opt = parkingService.findTicket(code);
            if (opt.isEmpty()) {
                Notification n = Notification.show("Ticket no encontrado en el sistema: " + code, 3000, Notification.Position.TOP_CENTER);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                showEmptyInvoiceState();
                return;
            }

            this.currentTicket = opt.get();
            manualCodeField.setValue(currentTicket.getUuid());
            renderInvoice(currentTicket);
        }));
    }

    private void renderInvoice(TicketModel ticket) {
        invoiceContainer.removeAll();

        Div invoicePanel = new Div();
        invoicePanel.addClassName("sap-invoice-panel");

        // Cabecera del comprobante
        Div header = new Div();
        header.addClassName("sap-panel-header");

        H3 invoiceTitle = new H3("Comprobante de Estacionamiento");
        invoiceTitle.addClassName("sap-panel-title");

        Span statusBadge = new Span();
        statusBadge.addClassName("sap-badge");
        if (ticket.isPayed()) {
            statusBadge.addClassName("success");
            statusBadge.setText("PAGADO");
        } else {
            statusBadge.addClassName("warning");
            statusBadge.setText("PENDIENTE DE PAGO");
        }

        header.add(invoiceTitle, statusBadge);

        // Tabla de datos estructurada estilo ERP
        Div tableContainer = new Div();
        tableContainer.getElement().setProperty("innerHTML", buildInvoiceTableHtml(ticket));

        // Total a pagar obtenido de la estrategia de servicio
        java.math.BigDecimal calculatedAmount = parkingService.calculateCurrentFee(ticket);

        Div totalBox = new Div();
        totalBox.addClassName("sap-invoice-total-box");

        VerticalLayout totalLabels = new VerticalLayout();
        totalLabels.setPadding(false);
        totalLabels.setSpacing(false);

        Span totalTitle = new Span("TOTAL A CANCELAR:");
        totalTitle.getStyle().set("font-weight", "700");
        totalTitle.getStyle().set("color", "var(--sap-text)");
        totalTitle.getStyle().set("font-size", "0.95rem");

        Span totalSubtext = new Span("Impuestos y estadía incluidos");
        totalSubtext.getStyle().set("font-size", "0.8rem");
        totalSubtext.getStyle().set("color", "var(--sap-text-secondary)");

        totalLabels.add(totalTitle, totalSubtext);

        Span totalAmount = new Span(String.format("Q%.2f", calculatedAmount));
        totalAmount.addClassName("sap-invoice-total-amount");

        totalBox.add(totalLabels, totalAmount);

        // Botones de acción inferiores
        HorizontalLayout actions = new HorizontalLayout();
        actions.setWidthFull();
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        actions.setSpacing(true);
        actions.getStyle().set("margin-top", "1.5rem");

        if (!ticket.isPayed()) {
            Button payBtn = new Button("Procesar Pago (" + String.format("Q%.2f", calculatedAmount) + ")", VaadinIcon.CHECK.create(), e -> {
                try {
                    TicketModel paid = parkingService.payTicket(ticket.getUuid());
                    this.currentTicket = paid;
                    renderInvoice(paid);

                    Notification n = Notification.show("Cobro registrado satisfactoriamente. Ticket marcado como PAGADO.", 4000, Notification.Position.TOP_CENTER);
                    n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } catch (Exception ex) {
                    Notification n = Notification.show("Error al registrar pago: " + ex.getMessage(), 3500, Notification.Position.TOP_CENTER);
                    n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            payBtn.addClassName("sap-btn-success");
            actions.add(payBtn);
        } else {
            Button clearBtn = new Button("Atender Siguiente Ticket", VaadinIcon.ARROW_RIGHT.create(), e -> showEmptyInvoiceState());
            clearBtn.addClassName("sap-btn-primary");
            actions.add(clearBtn);
        }

        invoicePanel.add(header, tableContainer, totalBox, actions);
        invoiceContainer.add(invoicePanel);
    }

    private String buildInvoiceTableHtml(TicketModel ticket) {
        Instant entry = ticket.getEntryTime() != null ? ticket.getEntryTime() : Instant.now();
        long minutes = Math.max(1, Duration.between(entry, Instant.now()).toMinutes());
        long hours = minutes / 60;
        long remMinutes = minutes % 60;
        String elapsedStr = (hours > 0 ? hours + " h " : "") + remMinutes + " min";

        String paymentTimeStr = (ticket.isPayed() && ticket.getPaymentTime() != null)
                ? formatter.format(ticket.getPaymentTime())
                : "Pendiente";

        return "<table class='sap-invoice-table'>" +
                "<tr><td class='label-cell'>Código de Identificación:</td><td class='value-cell'>" + ticket.getUuid() + "</td></tr>" +
                "<tr><td class='label-cell'>Fecha y Hora de Ingreso:</td><td class='value-cell'>" + formatter.format(entry) + "</td></tr>" +
                "<tr><td class='label-cell'>Tiempo Transcurrido:</td><td class='value-cell'>" + elapsedStr + "</td></tr>" +
                "<tr><td class='label-cell'>Régimen Tarifario:</td><td class='value-cell'>Tarifa Base Q10.00 + Q5.00/h</td></tr>" +
                "<tr><td class='label-cell'>Fecha / Hora de Liquidación:</td><td class='value-cell'>" + paymentTimeStr + "</td></tr>" +
                "</table>";
    }
}
