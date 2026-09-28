package com.backend.javaleap.entity;

import com.backend.javaleap.entity.enums.AccountSubType;
import com.backend.javaleap.entity.enums.AccountType;
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

/** Chart of Accounts entry. Group accounts organise the tree and cannot be posted to. */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "account")
public class Account extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "sub_type", nullable = false, length = 30)
    private AccountSubType subType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Account parent;

    @Builder.Default
    @Column(name = "is_group", nullable = false)
    private Boolean groupAccount = Boolean.FALSE;

    @Builder.Default
    @Column(nullable = false)
    private Boolean reconcilable = Boolean.FALSE;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(length = 255)
    private String description;

    /** Only active, non-group accounts accept journal lines. */
    public boolean isPostable() {
        return !Boolean.TRUE.equals(groupAccount) && Boolean.TRUE.equals(active);
    }
}
