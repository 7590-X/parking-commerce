package com.parking.webapp.views.components;

import java.util.UUID;
import java.util.function.Consumer;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;

@Tag("div")
public class QrScannerComponent extends Div {

    private final String scannerId;
    private Consumer<String> scanConsumer;
    private Consumer<String> errorConsumer;
    private boolean active = true;

    public QrScannerComponent() {
        this.scannerId = "qr-reader-" + UUID.randomUUID().toString().substring(0, 8);
        setId(scannerId);
        addClassName("qr-scanner-container");
        setWidth("100%");
        getStyle().set("max-width", "420px");
        getStyle().set("margin", "0 auto");
        getStyle().set("border-radius", "16px");
        getStyle().set("overflow", "hidden");
    }

    public void setOnScanListener(Consumer<String> consumer) {
        this.scanConsumer = consumer;
    }

    public void setOnErrorListener(Consumer<String> errorConsumer) {
        this.errorConsumer = errorConsumer;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        ui.getPage().addJavaScript("/html5-qrcode.min.js");
        ui.getPage().addJavaScript("/qr-scanner-controller.js");
        if (active) {
            startScanning();
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        stopScanning();
        super.onDetach(detachEvent);
    }

    public void startScanning() {
        this.active = true;
        getElement().executeJs("window.QrScannerController.start($0, $1)", scannerId, getElement());
    }

    public void stopScanning() {
        this.active = false;
        getElement().executeJs("window.QrScannerController.stop($0)", scannerId);
    }

    @ClientCallable
    public void onQrDecoded(String decodedText) {
        if (scanConsumer != null && active) {
            scanConsumer.accept(decodedText);
        }
    }

    @ClientCallable
    public void onScannerError(String errorMessage) {
        if (errorConsumer != null) {
            errorConsumer.accept(errorMessage);
        }
    }
}
