package com.parking.webapp;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.parking.webapp.model.TicketModel;
import com.parking.webapp.service.TicketBroadcaster;
import com.parking.webapp.util.QrCodeUtil;

class TicketPrintTest {

    @Test
    void testQrCodeGeneration() {
        String testUuid = "550e8400-e29b-41d4-a716-446655440000";
        String base64Qr = QrCodeUtil.generateQrBase64(testUuid, 180, 180);

        assertNotNull(base64Qr);
        assertTrue(base64Qr.startsWith("data:image/png;base64,"));
        assertTrue(base64Qr.length() > 100);
    }

    @Test
    void testTicketBroadcaster() {
        AtomicReference<TicketModel> receivedTicket = new AtomicReference<>();
        var reg = TicketBroadcaster.register(receivedTicket::set);

        TicketModel ticket = TicketModel.builder()
                .uuid("test-ticket-1234")
                .isPayed(false)
                .build();

        TicketBroadcaster.broadcast(ticket);

        try {
            Thread.sleep(150);
        } catch (InterruptedException ignored) {
        }

        assertNotNull(receivedTicket.get());
        assertTrue("test-ticket-1234".equals(receivedTicket.get().getUuid()));

        reg.remove();
    }

    @Test
    void testTicketUiBroadcasterWithEvent() {
        com.parking.webapp.events.TicketUiBroadcaster uiBroadcaster = new com.parking.webapp.events.TicketUiBroadcaster();
        AtomicReference<TicketModel> received = new AtomicReference<>();
        var reg = uiBroadcaster.register(received::set);

        TicketModel ticket = TicketModel.builder()
                .uuid("event-ticket-9876")
                .isPaid(false)
                .build();

        uiBroadcaster.onTicketCreated(new com.parking.webapp.events.TicketCreatedEvent(this, ticket));

        assertNotNull(received.get());
        assertTrue("event-ticket-9876".equals(received.get().getUuid()));

        reg.remove();
    }
}
