package com.parking.webapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_tickets")
public class TicketModel {

    @Id
    @Column(name = "uuid")
    private String uuid;

    @Column(name = "reference")
    private String reference;

    @Column(name = "entrance_date", nullable = false)
    private LocalDateTime entranceDate;

    @Column(name = "amount")
    private Long amount;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "exit_date")
    private LocalDateTime exitDate;

    @Column(name = "paid", nullable = false)
    private Boolean paid;
}
