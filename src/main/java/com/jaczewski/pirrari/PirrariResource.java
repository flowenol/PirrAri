package com.jaczewski.pirrari;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.jaczewski.pirrari.data.Metrics;

@RestController
@RequestMapping("/PirrAri/control")
public class PirrariResource {

    @Autowired
    private PirrariControl control;

    @RequestMapping(value = "/left/{on}", method = RequestMethod.GET)
    public String left(@PathVariable("on") Boolean on) {
        control.left(on);
        return on.toString();
    }

    @RequestMapping(value = "/right/{on}", method = RequestMethod.GET)
    public String right(@PathVariable("on") Boolean on) {
        control.right(on);
        return on.toString();
    }

    @RequestMapping(value = "/forward/{on}", method = RequestMethod.GET)
    public String forward(@PathVariable("on") Boolean on) {
        control.forward(on);
        return on.toString();
    }

    @RequestMapping(value = "/backward/{on}", method = RequestMethod.GET)
    public String backward(@PathVariable("on") Boolean on) {
        control.backward(on);
        return on.toString();
    }

    @RequestMapping(value = "/peripheralsPower/{on}", method = RequestMethod.GET)
    public String peripheralsPower(@PathVariable("on") Boolean on) {
        control.peripheralsPower(on);
        return on.toString();
    }

    @RequestMapping("/metrics")
    public Metrics getMetrics() {
        return control.getMetrics();
    }

    @RequestMapping(value = "/speed/{speed}", method = RequestMethod.POST)
    public void setSpeed(@PathVariable("speed") Integer speed) {
        try {
            control.setMotorSpeed(speed);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @RequestMapping(value = "/shutdown", method = RequestMethod.POST)
    public void shutdown() {
        try {
            new ProcessBuilder("/bin/bash", "-c", "halt").start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
