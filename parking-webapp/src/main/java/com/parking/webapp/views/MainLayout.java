package com.parking.webapp.views;

import com.parking.webapp.events.TicketUiBroadcaster;
import com.parking.webapp.model.ParkingModel;
import com.parking.webapp.service.ParkingWebService;
import com.parking.webapp.views.components.TicketPrintDialog;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.shared.Registration;

public class MainLayout extends AppLayout {

    private final ParkingWebService parkingService;
    private final TicketUiBroadcaster ticketUiBroadcaster;
    private Registration ticketBroadcastRegistration;

    public MainLayout(ParkingWebService parkingService, TicketUiBroadcaster ticketUiBroadcaster) {
        this.parkingService = parkingService;
        this.ticketUiBroadcaster = ticketUiBroadcaster;
        createHeader();
    }

    private void createHeader() {
        // Marca estilo SAP Business One
        Span sapBadge = new Span("PARQUEO");
        sapBadge.addClassName("sap-logo-badge");

        H2 brandTitle = new H2("Gestión de Parqueo Empresarial");
        brandTitle.addClassName("sap-app-title");

        HorizontalLayout brand = new HorizontalLayout(sapBadge, brandTitle);
        brand.addClassName("sap-brand-container");
        brand.setAlignItems(FlexComponent.Alignment.CENTER);

        // Enlaces de navegación con diseño cuadrado y pestañas de menú
        RouterLink availabilityLink = new RouterLink("📊 Disponibilidad", AvailabilityView.class);
        availabilityLink.addClassName("sap-nav-tab");

        RouterLink paymentLink = new RouterLink("💳 Cobro de Ticket", PaymentView.class);
        paymentLink.addClassName("sap-nav-tab");

        RouterLink exitLink = new RouterLink("🚗 Control de Salida", ExitView.class);
        exitLink.addClassName("sap-nav-tab");

        HorizontalLayout navTabs = new HorizontalLayout(availabilityLink, paymentLink, exitLink);
        navTabs.addClassName("sap-nav-tabs");
        navTabs.setAlignItems(FlexComponent.Alignment.CENTER);
        navTabs.setSpacing(false);

        // Indicador de conexión / sistema
        Span systemStatus = new Span("● En Línea");
        systemStatus.getStyle().set("color", "#74d99f");
        systemStatus.getStyle().set("font-size", "0.8rem");
        systemStatus.getStyle().set("font-weight", "600");

        HorizontalLayout rightSide = new HorizontalLayout(systemStatus);
        rightSide.setAlignItems(FlexComponent.Alignment.CENTER);

        // Barra superior completa
        HorizontalLayout header = new HorizontalLayout(brand, navTabs, rightSide);
        header.addClassName("sap-shell-bar");
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        addToNavbar(header);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        ui.getPage().addJavaScript("/ticket-printer.js");

        ticketBroadcastRegistration = ticketUiBroadcaster.register(ticket -> {
            ui.access(() -> {
                ParkingModel parking = (parkingService != null) ? parkingService.getParkingInfo() : null;
                TicketPrintDialog.show(ticket, parking);
            });
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (ticketBroadcastRegistration != null) {
            ticketBroadcastRegistration.remove();
        }
        super.onDetach(detachEvent);
    }
}
