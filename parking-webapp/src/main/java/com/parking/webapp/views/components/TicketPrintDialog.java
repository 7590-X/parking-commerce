package com.parking.webapp.views.components;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.parking.webapp.model.ParkingModel;
import com.parking.webapp.model.TicketModel;
import com.parking.webapp.util.QrCodeUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * Diálogo modal para la visualización e impresión de tickets de parqueo
 * adaptado específicamente para impresoras térmicas de 80mm / 88mm.
 */
public class TicketPrintDialog extends Dialog {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public TicketPrintDialog(TicketModel ticket, ParkingModel parking) {
        setHeaderTitle("Comprobante de Ingreso de Parqueo");
        setModal(true);
        setCloseOnOutsideClick(true);
        setCloseOnEsc(true);
        setWidth("420px");
        setMaxWidth("95vw");

        // Cargar script de impresión si no se ha cargado aún
        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.getPage().addJavaScript("/ticket-printer.js");
        }

        // Datos del parqueo
        String parkingName = (parking != null && parking.getName() != null && !parking.getName().isBlank())
                ? parking.getName()
                : "ESTACIONAMIENTO CENTRAL";
        String parkingAddress = (parking != null && parking.getAddress() != null && !parking.getAddress().isBlank())
                ? parking.getAddress()
                : "Campus Universitario";

        // Contenedor principal del ticket (formato papel térmico 88mm)
        Div ticketContainer = new Div();
        ticketContainer.setId("printable-ticket-content");
        ticketContainer.addClassName("sap-thermal-ticket");

        // Cabecera
        H3 title = new H3(parkingName);
        title.addClassName("ticket-title");

        Div subtitle = new Div();
        subtitle.setText(parkingAddress);
        subtitle.addClassName("ticket-sub");

        Div subheader = new Div();
        subheader.setText("*** TICKET DE INGRESO ***");
        subheader.addClassName("ticket-sub");
        subheader.getStyle().set("font-weight", "bold");
        subheader.getStyle().set("letter-spacing", "1px");

        Div divider1 = new Div();
        divider1.addClassName("ticket-divider");

        // Filas de datos
        String uuid = ticket.getUuid() != null ? ticket.getUuid() : "N/A";
        Instant entryTime = ticket.getEntryTime() != null ? ticket.getEntryTime() : Instant.now();
        String formattedDate = DATE_FORMATTER.format(entryTime);

        Div rowTicket = createTicketRow("No. Ticket:", uuid.length() > 8 ? uuid.substring(0, 8).toUpperCase() : uuid);
        Div rowFecha = createTicketRow("Fecha/Hora:", formattedDate);
        Div rowEstado = createTicketRow("Estado:", "PENDIENTE DE PAGO");

        Div divider2 = new Div();
        divider2.addClassName("ticket-divider");

        // Código QR en Base64
        Div qrBox = new Div();
        qrBox.addClassName("ticket-qr-box");

        String qrBase64 = QrCodeUtil.generateQrBase64(uuid, 220, 220);
        if (!qrBase64.isBlank()) {
            Image qrImage = new Image(qrBase64, "QR Ticket: " + uuid);
            qrImage.addClassName("ticket-qr-img");
            qrBox.add(qrImage);
        }

        Div uuidText = new Div();
        uuidText.setText(uuid);
        uuidText.addClassName("ticket-uuid-text");
        qrBox.add(uuidText);

        // Cuadro de información de tarifa
        Div pricingBox = new Div();
        pricingBox.addClassName("ticket-pricing");
        pricingBox.setText("TARIFA: Base Q10.00 (1ra hora) + Q5.00/hora adicional");

        Div divider3 = new Div();
        divider3.addClassName("ticket-divider");

        // Pie de ticket
        Div footer = new Div();
        footer.addClassName("ticket-footer");
        footer.setText(
                "Conserve este ticket para realizar su pago en caja y habilitar la talanquera de salida. ¡Gracias por su visita!");

        ticketContainer.add(title, subtitle, subheader, divider1, rowTicket, rowFecha, rowEstado, divider2, qrBox,
                pricingBox, divider3, footer);

        // Envoltura visual para centrar el ticket en el diálogo
        Div wrapper = new Div(ticketContainer);
        wrapper.addClassName("sap-ticket-preview-wrapper");
        add(wrapper);

        // Botones de acción
        createFooterButtons();
    }

    private Div createTicketRow(String label, String value) {
        Div row = new Div();
        row.addClassName("ticket-row");

        Span lblSpan = new Span(label);
        lblSpan.getStyle().set("font-weight", "bold");

        Span valSpan = new Span(value);
        valSpan.getStyle().set("text-align", "right");

        row.add(lblSpan, valSpan);
        return row;
    }

    private void createFooterButtons() {
        Button printBtn = new Button("Imprimir Ticket", VaadinIcon.PRINT.create(), e -> {
            UI currentUI = UI.getCurrent();
            if (currentUI != null) {
                currentUI.getPage().executeJs(
                        "if (window.printThermalTicket) { window.printThermalTicket(); } else { window.print(); }");
            }
        });
        printBtn.addClassName("sap-btn-primary");
        printBtn.getStyle().set("padding", "0.6rem 1.25rem");

        Button closeBtn = new Button("Cerrar", VaadinIcon.CLOSE.create(), e -> close());
        closeBtn.addClassName("sap-btn-secondary");

        HorizontalLayout footerLayout = new HorizontalLayout(closeBtn, printBtn);
        footerLayout.setWidthFull();
        footerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footerLayout.setSpacing(true);
        footerLayout.getStyle().set("padding-top", "0.5rem");

        getFooter().add(footerLayout);
    }

    /**
     * Método de conveniencia para abrir el diálogo con un ticket y modelo de
     * parqueo.
     */
    public static void show(TicketModel ticket, ParkingModel parking) {
        TicketPrintDialog dialog = new TicketPrintDialog(ticket, parking);
        dialog.open();
    }
}
