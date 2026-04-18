package com.hejulian.blog.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Category extends BaseEntity {

    private String name;

    private String slug;

    private String description;
}

