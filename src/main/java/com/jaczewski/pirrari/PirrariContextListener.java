package com.jaczewski.pirrari;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class PirrariContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            PirrariControl.CONTROL.init();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        PirrariControl.CONTROL.close();
    }
}
