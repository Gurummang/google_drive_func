package com.GASB.google_drive_func.model.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Getter
@NoArgsConstructor
@Table(name="admin")
public class AdminUsers {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "org_id", nullable = false)
    private Org org;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "last_login")
    private Timestamp lastLogin;

    @Builder
    public AdminUsers(Org org, String email, String password, String firstName, String lastName, Timestamp lastLogin) {
        this.org = org;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.lastLogin = lastLogin;
    }

    public Timestamp getLastLogin() {
        // Return a copy of the Timestamp object to avoid exposing the internal representation
        return new Timestamp(lastLogin.getTime());
    }
}