package com.parking.webapp.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;

public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
    }

    private void createHeader() {
        // Logotipo / Marca
        Span logoIcon = new Span("🅿️");
        logoIcon.getStyle().set("font-size", "1.5rem");

        H2 brandTitle = new H2("SmartParking");
        brandTitle.getStyle().set("margin", "0");
        brandTitle.getStyle().set("font-size", "1.25rem");
        brandTitle.getStyle().set("font-weight", "800");
        brandTitle.getStyle().set("color", "#ffffff");

        Span iotBadge = new Span("IoT Cloud");
        iotBadge.addClassName("brand-badge");
        iotBadge.getStyle().set("font-size", "0.75rem");
        iotBadge.getStyle().set("padding", "0.2rem 0.6rem");

        HorizontalLayout brand = new HorizontalLayout(logoIcon, brandTitle, iotBadge);
        brand.setAlignItems(FlexComponent.Alignment.CENTER);
        brand.setSpacing(true);

        // Enlaces de navegación
        RouterLink availabilityLink = new RouterLink("📊 Disponibilidad", AvailabilityView.class);
        availabilityLink.addClassName("nav-tab-link");

        RouterLink paymentLink = new RouterLink("💳 Cobro / Pago", PaymentView.class);
        paymentLink.addClassName("nav-tab-link");

        RouterLink exitLink = new RouterLink("🚗 Terminal Salida", ExitView.class);
        exitLink.addClassName("nav-tab-link");

        HorizontalLayout navTabs = new HorizontalLayout(availabilityLink, paymentLink, exitLink);
        navTabs.setAlignItems(FlexComponent.Alignment.CENTER);
        navTabs.setSpacing(true);

        // Barra superior
        HorizontalLayout header = new HorizontalLayout(brand, navTabs);
        header.addClassName("app-navbar");
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        addToNavbar(header);
    }
}
