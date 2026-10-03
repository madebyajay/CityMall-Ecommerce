package com.citymall.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    /** Optional parent for subcategory support (null = top-level). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent")
    @Builder.Default
    private List<Category> children = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "category", cascade = CascadeType.REMOVE)
    @Builder.Default
    private List<Product> products = new java.util.ArrayList<>();
}

