package com.pvp.travelmatch.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String password;

    private Integer age;
    private String gender;
    private String city;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<TravelPlan> travelPlans;


    @Column(nullable = false)
    private Boolean verified = false;

    private String verificationCode;

    private LocalDateTime codeExpiry;

    @Column(name = "reset_code")
    private String resetCode;

    @Column(name = "reset_code_expiry")
    private LocalDateTime resetCodeExpiry;
}