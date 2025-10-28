package com.example.app.ui;

import com.example.app.core.CoreService;

public class Main {

    public static void main(String[] args) {
        CoreService service = new CoreService();
        System.out.println(service.getToolkitName());
    }
}
