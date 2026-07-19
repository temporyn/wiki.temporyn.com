package com.temporyn.wiki.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "directory")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Directory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public static Directory create(String name, Long parentId, int sortOrder) {
        Directory directory = new Directory();
        directory.name = name;
        directory.parentId = parentId;
        directory.sortOrder = sortOrder;
        return directory;
    }

    public void update(String name, Long parentId) {
        this.name = name;
        this.parentId = parentId;
    }
}
