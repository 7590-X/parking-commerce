package com.parking.webapp.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_tickets")
public class TicketModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "uuid", unique = true)
    private String uuid;

    @Column(name = "entry_type")
    private Instant entryTime;

    @Column(name = "out_time", nullable = true)
    private Instant outTime;

    @Column(name = "payment_type", nullable = true)
    private Instant paymentTime;

    @Column(name = "is_payed")
    private boolean isPaid;

    @Column(name = "amount", precision = 10, scale = 2, nullable = true)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_id")
    private ParkingModel parking;

    @PrePersist
    public void prePersist() {
        if (this.uuid == null || this.uuid.isBlank()) {
            this.uuid = UUID.randomUUID().toString();
        }
        if (this.entryTime == null) {
            this.entryTime = Instant.now();
        }
    }

    /**
     * Alias de compatibilidad hacia atrás para isPaid.
     */
    public boolean isPayed() {
        return this.isPaid;
    }

    /**
     * Alias de compatibilidad hacia atrás para isPaid.
     */
    public void setPayed(boolean payed) {
        this.isPaid = payed;
    }

    public static class TicketModelBuilder {
        public TicketModelBuilder isPayed(boolean isPayed) {
            this.isPaid = isPayed;
            return this;
        }
    }
}

