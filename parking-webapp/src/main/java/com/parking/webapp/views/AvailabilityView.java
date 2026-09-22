package com.parking.webapp.views;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.parking.webapp.dto.TelemetryDto;
import com.parking.webapp.model.ParkingModel;
import com.parking.webapp.service.ParkingWebService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
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

@PageTitle("Disponibilidad de Parqueo | SmartParking")
@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "disponibilidad", layout = MainLayout.class)
public class AvailabilityView extends VerticalLayout {

    private final ParkingWebService parkingService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    // Componentes interactivos que se actualizan
    private final Span maxCapacitySpan = new Span("0");
    private final Span currentCapacitySpan = new Span("0");
    private final Span availableCapacitySpan = new Span("0");
    private final Span percentageSpan = new Span("0%");
    private final Span lastUpdatedSpan = new Span("Sin registrar");
    private final Span statusPill = new Span();
    private final Div progressBarFill = new Div();
    private final H1 parkingNameTitle = new H1("Estacionamiento");
    private final Paragraph parkingAddressText = new Paragraph("Cargando ubicación...");

    private Registration pollRegistration;

    public AvailabilityView(ParkingWebService parkingService) {
        this.parkingService = parkingService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setMaxWidth("1200px");
        getStyle().set("margin", "0 auto");

        createHeaderSection();
        createMetricsGrid();
        createProgressBarSection();
        createFooterActions();

        refreshData();
    }

    private void createHeaderSection() {
        parkingNameTitle.getStyle().set("margin-bottom", "0.25rem");
        parkingNameTitle.getStyle().set("font-size", "2.2rem");

        parkingAddressText.getStyle().set("color", "var(--text-muted)");
        parkingAddressText.getStyle().set("margin-top", "0");

        VerticalLayout titles = new VerticalLayout(parkingNameTitle, parkingAddressText);
        titles.setPadding(false);
        titles.setSpacing(false);

        HorizontalLayout headerBar = new HorizontalLayout(titles, statusPill);
        headerBar.setWidthFull();
        headerBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerBar.setAlignItems(FlexComponent.Alignment.CENTER);

        add(headerBar);
    }

    private void createMetricsGrid() {
        HorizontalLayout metricsGrid = new HorizontalLayout();
        metricsGrid.setWidthFull();
        metricsGrid.setSpacing(true);

        // 1. Capacidad Máxima
        Div maxCard = createMetricCard("Capacidad Total", maxCapacitySpan, "Espacios totales del recinto", "rgba(99, 102, 241, 0.4)");

        // 2. Espacios Ocupados
        Div currentCard = createMetricCard("Ocupación Actual", currentCapacitySpan, "Vehículos estacionados", "rgba(245, 158, 11, 0.4)");

        // 3. Espacios Disponibles
        Div availableCard = createMetricCard("Espacios Libres", availableCapacitySpan, "Listos para recibir vehículos", "rgba(16, 185, 129, 0.4)");

        // 4. Porcentaje
        Div percentCard = createMetricCard("Nivel de Ocupación", percentageSpan, "Capacidad utilizada", "rgba(139, 92, 246, 0.4)");

        metricsGrid.add(maxCard, currentCard, availableCard, percentCard);
        add(metricsGrid);
    }

    private Div createMetricCard(String title, Span valueSpan, String subtitle, String borderColor) {
        Div card = new Div();
        card.addClassName("metric-card");
        card.getStyle().set("flex", "1");
        card.getStyle().set("border-left", "4px solid " + borderColor);

        Span titleSpan = new Span(title);
        titleSpan.addClassName("metric-title");

        valueSpan.addClassName("metric-value");

        Span subSpan = new Span(subtitle);
        subSpan.addClassName("metric-subtitle");

        card.add(titleSpan, valueSpan, subSpan);
        return card;
    }

    private void createProgressBarSection() {
        Div section = new Div();
        section.addClassName("metric-card");
        section.setWidthFull();

        H3 title = new H3("Monitoreo de Aforo en Tiempo Real");
        title.getStyle().set("margin-top", "0");
        title.getStyle().set("margin-bottom", "0.75rem");

        Div track = new Div();
        track.addClassName("progress-track");

        progressBarFill.addClassName("progress-fill");
        progressBarFill.setWidth("0%");
        progressBarFill.getStyle().set("background", "linear-gradient(90deg, #10b981 0%, #6366f1 100%)");

        track.add(progressBarFill);

        HorizontalLayout timeInfo = new HorizontalLayout();
        timeInfo.setWidthFull();
        timeInfo.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        timeInfo.getStyle().set("margin-top", "0.75rem");

        Span labelUpdated = new Span("Última actualización de telemetría:");
        labelUpdated.getStyle().set("color", "var(--text-muted)");
        labelUpdated.getStyle().set("font-size", "0.85rem");

        lastUpdatedSpan.getStyle().set("font-weight", "600");
        lastUpdatedSpan.getStyle().set("color", "var(--text-main)");
        lastUpdatedSpan.getStyle().set("font-size", "0.85rem");

        HorizontalLayout lastUpdatedLayout = new HorizontalLayout(labelUpdated, lastUpdatedSpan);
        lastUpdatedLayout.setSpacing(true);

        Span autoRefreshBadge = new Span("⚡ Auto-refresco activo cada 3s");
        autoRefreshBadge.getStyle().set("color", "#34d399");
        autoRefreshBadge.getStyle().set("font-size", "0.8rem");
        autoRefreshBadge.getStyle().set("font-weight", "600");

        timeInfo.add(lastUpdatedLayout, autoRefreshBadge);

        section.add(title, track, timeInfo);
        add(section);
    }

    private void createFooterActions() {
        Button refreshBtn = new Button("Refrescar Datos", VaadinIcon.REFRESH.create(), e -> {
            refreshData();
            Notification.show("Datos actualizados", 1500, Notification.Position.BOTTOM_END);
        });
        refreshBtn.addClassName("btn-primary-glow");

        Button simulateEntryBtn = new Button("Simular Entrada (Ticket +1)", VaadinIcon.CAR.create(), e -> {
            try {
                var ticket = parkingService.createManualTicket();
                refreshData();
                Notification n = Notification.show("✅ Entrada registrada! Ticket: " + ticket.getUuid(), 4000, Notification.Position.TOP_CENTER);
                n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Error al registrar entrada: " + ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });
        simulateEntryBtn.getStyle().set("color", "#a5b4fc");
        simulateEntryBtn.getStyle().set("border", "1px solid rgba(99, 102, 241, 0.4)");
        simulateEntryBtn.getStyle().set("background", "rgba(99, 102, 241, 0.1)");
        simulateEntryBtn.getStyle().set("border-radius", "12px");

        HorizontalLayout actions = new HorizontalLayout(refreshBtn, simulateEntryBtn);
        actions.setSpacing(true);
        add(actions);
    }

    private void refreshData() {
        try {
            ParkingModel parking = parkingService.getParkingInfo();
            if (parking != null) {
                parkingNameTitle.setText(parking.getName() != null ? parking.getName() : "Estacionamiento Central");
                parkingAddressText.setText(parking.getAddress() != null ? parking.getAddress() : "Ubicación del parqueo");
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

            // Configurar color de barra y badge según aforo
            statusPill.removeAll();
            statusPill.setClassName("status-pill");

            if (percentage >= 100) {
                statusPill.addClassName("danger");
                statusPill.setText("🚫 PARQUEO LLENO");
                progressBarFill.getStyle().set("background", "linear-gradient(90deg, #f59e0b 0%, #ef4444 100%)");
            } else if (percentage >= 75) {
                statusPill.addClassName("warning");
                statusPill.setText("⚠️ POCOS ESPACIOS");
                progressBarFill.getStyle().set("background", "linear-gradient(90deg, #10b981 0%, #f59e0b 100%)");
            } else {
                statusPill.addClassName("success");
                statusPill.setText("✅ PLAZAS DISPONIBLES");
                progressBarFill.getStyle().set("background", "linear-gradient(90deg, #10b981 0%, #6366f1 100%)");
            }

            Instant lastUpdated = telemetry.lastUpdated();
            if (lastUpdated != null) {
                lastUpdatedSpan.setText(formatter.format(lastUpdated));
            } else {
                lastUpdatedSpan.setText("Recién iniciado");
            }
        } catch (Exception e) {
            lastUpdatedSpan.setText("Error al conectar: " + e.getMessage());
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        ui.setPollInterval(3000); // Polling cada 3 segundos
        pollRegistration = ui.addPollListener(event -> refreshData());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (pollRegistration != null) {
            pollRegistration.remove();
        }
        super.onDetach(detachEvent);
    }
}
