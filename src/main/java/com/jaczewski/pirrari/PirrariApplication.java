package com.jaczewski.pirrari;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@Configuration
@ApplicationPath("/PirrAri/control")
public class PirrariApplication extends ResourceConfig {

    public PirrariApplication() {
        register(PirrariResource.class);
    }
}
