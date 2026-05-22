package com.example.creatorsettlement.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "creators")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Creator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Builder
    public Creator(String name, String email) {
        this.name = name;
        this.email = email;
    }
}
