package com.backend.javaleap.entity;

import com.backend.javaleap.entity.enums.ContactType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

/** Contact master: customers (regional power grids) and vendors (tile manufacturers). */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "contact")
public class Contact extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "contact_type", nullable = false, length = 10)
    private ContactType contactType;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(unique = true, length = 15)
    private String gstin;

    @Column(length = 10)
    private String pan;

    @Column(length = 120)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(name = "address_line", length = 255)
    private String addressLine;

    @Column(length = 60)
    private String city;

    @Column(length = 60)
    private String state;

    @Column(length = 10)
    private String pincode;

    @Builder.Default
    @Column(nullable = false, length = 60)
    private String country = "India";

    @Builder.Default
    @Column(name = "payment_terms_days", nullable = false)
    private Integer paymentTermsDays = 30;

    @Column(name = "credit_limit", precision = 16, scale = 2)
    private BigDecimal creditLimit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receivable_account_id")
    private Account receivableAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payable_account_id")
    private Account payableAccount;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(length = 500)
    private String notes;

    public boolean isCustomer() {
        return contactType == ContactType.CUSTOMER || contactType == ContactType.BOTH;
    }

    public boolean isVendor() {
        return contactType == ContactType.VENDOR || contactType == ContactType.BOTH;
    }
}
