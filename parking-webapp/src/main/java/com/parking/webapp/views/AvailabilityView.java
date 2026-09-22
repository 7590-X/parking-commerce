package com.parking.webapp.views;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.parking.webapp.dto.TelemetryDto;
import com.parking.webapp.model.ParkingModel;
import com.parking.webapp.service.ParkingWebService;
import com.parking.webapp.service.TicketBroadcaster;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.shared.Registration;

@PageTitle("Disponibilidad de Parqueo")
@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "disponibilidad", layout = MainLayout.class)
public class AvailabilityView extends VerticalLayout {

    private final ParkingWebService parkingService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final Span maxCapacitySpan = new Span("0");
    private final Span currentCapacitySpan = new Span("0");
    private final Span availableCapacitySpan = new Span("0");
    private final Span percentageSpan = new Span("0%");
    private final Span lastUpdatedSpan = new Span("Sin registrar");
    private final Span statusBadge = new Span();
    private final Div progressBarFill = new Div();
    private final H2 parkingNameTitle = new H2("Estacionamiento Central");
    private final Paragraph parkingAddressText = new Paragraph("Cargando ubicación...");

    private Registration pollRegistration;
    private Registration ticketPollRegistration;

    public AvailabilityView(ParkingWebService parkingService) {
        this.parkingService = parkingService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setMaxWidth("1250px");
        getStyle().set("margin", "0 auto");
        getStyle().set("padding-top", "1.5rem");

        createHeaderSection();
        createKpiTilesGrid();
        createTelemetryPanel();
        createActionToolbar();

        refreshData();
    }

    private void createHeaderSection() {
        Div headerPanel = new Div();
        headerPanel.addClassName("sap-panel");
        headerPanel.setWidthFull();

        parkingNameTitle.getStyle().set("color", "#102a43");
        parkingNameTitle.getStyle().set("font-size", "1.45rem");
        parkingNameTitle.getStyle().set("font-weight", "600");
        parkingNameTitle.getStyle().set("margin", "0 0 0.25rem 0");

        parkingAddressText.getStyle().set("color", "var(--sap-text-muted)");
        parkingAddressText.getStyle().set("margin", "0");
        parkingAddressText.getStyle().set("font-size", "0.9rem");

        VerticalLayout titles = new VerticalLayout(parkingNameTitle, parkingAddressText);
        titles.setPadding(false);
        titles.setSpacing(false);

        HorizontalLayout bar = new HorizontalLayout(titles, statusBadge);
        bar.setWidthFull();
        bar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        bar.setAlignItems(FlexComponent.Alignment.CENTER);

        headerPanel.add(bar);
        add(headerPanel);
    }

    private void createKpiTilesGrid() {
        HorizontalLayout tilesGrid = new HorizontalLayout();
        tilesGrid.setWidthFull();
        tilesGrid.setSpacing(true);

        // 1. Capacidad Máxima
        Div maxTile = createKpiTile("Capacidad Máxima", maxCapacitySpan, "Espacios totales definidos",
                "indicator-navy");

        // 2. Cantidad Actual
        Div currentTile = createKpiTile("Cantidad Actual", currentCapacitySpan, "Vehículos ocupando plaza",
                "indicator-blue");

        // 3. Disponibles
        Div availableTile = createKpiTile("Plazas Disponibles", availableCapacitySpan, "Capacidad libre de ingreso",
                "indicator-green");

        // 4. Nivel de Ocupación
        Div percentTile = createKpiTile("Porcentaje de Ocupación", percentageSpan, "Utilización del estacionamiento",
                "indicator-amber");

        tilesGrid.add(maxTile, currentTile, availableTile, percentTile);
        add(tilesGrid);
    }

    private Div createKpiTile(String title, Span valueSpan, String subtext, String indicatorClass) {
        Div tile = new Div();
        tile.addClassName("sap-kpi-tile");
        tile.addClassName(indicatorClass);

        Span titleSpan = new Span(title);
        titleSpan.addClassName("sap-kpi-label");

        valueSpan.addClassName("sap-kpi-value");

        Span subSpan = new Span(subtext);
        subSpan.addClassName("sap-kpi-subtext");

        tile.add(titleSpan, valueSpan, subSpan);
        return tile;
    }

    private void createTelemetryPanel() {
        Div panel = new Div();
        panel.addClassName("sap-panel");
        panel.setWidthFull();

        Div panelHeader = new Div();
        panelHeader.addClassName("sap-panel-header");

        H3 panelTitle = new H3("Monitoreo de Ocupación y Registro de Telemetría");
        panelTitle.addClassName("sap-panel-title");

        Span autoRefreshTag = new Span("Muestreo en tiempo real (3s)");
        autoRefreshTag.addClassName("sap-badge");
        autoRefreshTag.addClassName("neutral");

        panelHeader.add(panelTitle, autoRefreshTag);

        // Barra de progreso cuadrada corporativa
        Div track = new Div();
        track.addClassName("sap-progress-track");

        progressBarFill.addClassName("sap-progress-fill");
        progressBarFill.setWidth("0%");
        progressBarFill.getStyle().set("background-color", "var(--sap-primary)");

        track.add(progressBarFill);

        // Detalles de última modificación
        HorizontalLayout infoRow = new HorizontalLayout();
        infoRow.setWidthFull();
        infoRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        infoRow.setAlignItems(FlexComponent.Alignment.CENTER);
        infoRow.getStyle().set("margin-top", "1rem");

        Span labelUpdated = new Span("Última fecha de modificación:");
        labelUpdated.getStyle().set("color", "var(--sap-text-muted)");
        labelUpdated.getStyle().set("font-size", "0.9rem");
        labelUpdated.getStyle().set("font-weight", "500");

        lastUpdatedSpan.getStyle().set("color", "var(--sap-text)");
        lastUpdatedSpan.getStyle().set("font-size", "0.9rem");
        lastUpdatedSpan.getStyle().set("font-weight", "600");

        HorizontalLayout lastUpdatedLayout = new HorizontalLayout(labelUpdated, lastUpdatedSpan);
        lastUpdatedLayout.setSpacing(true);

        Span systemNotice = new Span("Protocolo de Comunicación: AMQP / RabbitMQ Activo");
        systemNotice.getStyle().set("color", "var(--sap-text-secondary)");
        systemNotice.getStyle().set("font-size", "0.85rem");

        infoRow.add(lastUpdatedLayout, systemNotice);

        panel.add(panelHeader, track, infoRow);
        add(panel);
    }

    private void createActionToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setSpacing(true);

        Button refreshBtn = new Button("Actualizar Datos", VaadinIcon.REFRESH.create(), e -> {
            refreshData();
            Notification.show("Información actualizada correctamente", 1500, Notification.Position.BOTTOM_END);
        });
        refreshBtn.addClassName("sap-btn-primary");

        Button simulateEntryBtn = new Button("Simular Entrada de Vehículo (+1)", VaadinIcon.CAR.create(), e -> {
            try {
                var ticket = parkingService.createManualTicket();
                refreshData();
                Notification n = Notification.show("Ingreso registrado. Ticket: " + ticket.getUuid(), 3500,
                        Notification.Position.TOP_CENTER);
                n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Error al registrar entrada: " + ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });
        simulateEntryBtn.addClassName("sap-btn-secondary");

        toolbar.add(refreshBtn, simulateEntryBtn);
        add(toolbar);
    }

    private void refreshData() {
        try {
            ParkingModel parking = parkingService.getParkingInfo();
            if (parking != null) {
                parkingNameTitle.setText(parking.getName() != null ? parking.getName() : "Estacionamiento Central");
                parkingAddressText
                        .setText(parking.getAddress() != null ? parking.getAddress() : "Campus Universitario");
            }

            TelemetryDto telemetry = parkingService.obtainTelemetry();
            short max = telemetry.max();
            short current = telemetry.current();
            short available = (short) Math.max(0, max - current);

            maxCapacitySpan.setText(String.valueOf(max));
            currentCapacitySpan.setText(String.valueOf(current));
            availableCapacitySpan.setText(String.valueOf(available));

            int percentage = max > 0 ? (current * 100) / max : 0;
            percentageSpan.setText(percentage + "%");
            progressBarFill.setWidth(Math.min(100, percentage) + "%");

            // Configurar color y badge de aforo
            statusBadge.removeAll();
            statusBadge.setClassName("sap-badge");

            if (percentage >= 100) {
                statusBadge.addClassName("danger");
                statusBadge.setText("PARQUEO LLENO");
                progressBarFill.getStyle().set("background-color", "var(--sap-danger)");
            } else if (percentage >= 75) {
                statusBadge.addClassName("warning");
                statusBadge.setText("DISPONIBILIDAD LIMITADA");
                progressBarFill.getStyle().set("background-color", "var(--sap-warning)");
            } else {
                statusBadge.addClassName("success");
                statusBadge.setText("PLAZAS DISPONIBLES");
                progressBarFill.getStyle().set("background-color", "var(--sap-success)");
            }

            Instant lastUpdated = telemetry.lastUpdated();
            if (lastUpdated != null) {
                lastUpdatedSpan.setText(formatter.format(lastUpdated));
            } else {
                lastUpdatedSpan.setText("Sin registro previo");
            }
        } catch (Exception e) {
            lastUpdatedSpan.setText("Error de comunicación: " + e.getMessage());
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        ui.setPollInterval(3000);
        pollRegistration = ui.addPollListener(event -> refreshData());
        ticketPollRegistration = TicketBroadcaster.register(ticket -> {
            ui.access(this::refreshData);
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (pollRegistration != null) {
            pollRegistration.remove();
        }
        if (ticketPollRegistration != null) {
            ticketPollRegistration.remove();
        }
        super.onDetach(detachEvent);
    }
}
