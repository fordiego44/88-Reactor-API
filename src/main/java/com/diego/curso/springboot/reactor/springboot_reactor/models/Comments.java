package com.diego.curso.springboot.reactor.springboot_reactor.models;

import java.util.ArrayList;
import java.util.List;

public class Comments {

    private List<String> comments;

    public Comments() {
        this.comments = new ArrayList<>();
    }

    public void addComment(String comment){
        this.comments.add(comment);
    }

    @Override
    public String toString() {
        return "Comments{" +
                "comments=" + comments +
                '}';
    }
}
