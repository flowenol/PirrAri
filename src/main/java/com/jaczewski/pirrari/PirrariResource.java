package com.jaczewski.pirrari;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.jaczewski.pirrari.data.Metrics;

@Path("/")
@Consumes(MediaType.TEXT_PLAIN)
public class PirrariResource {

    private static final float CURRENT_UNIT = 2.0f; //mA
    private static final float MOTOR_CURRENT_UNIT = 2.0f; //mA

    @GET
    @Path("/left/{on}")
    @Produces(MediaType.TEXT_PLAIN)
    public String left(@PathParam("on") Boolean on) {
        PirrariControl.CONTROL.left(on);
        return on.toString();
    }

    @GET
    @Path("/right/{on}")
    @Produces(MediaType.TEXT_PLAIN)
    public String right(@PathParam("on") Boolean on) {
        PirrariControl.CONTROL.right(on);
        return on.toString();
    }

    @GET
    @Path("/forward/{on}")
    @Produces(MediaType.TEXT_PLAIN)
    public String forward(@PathParam("on") Boolean on) {
        PirrariControl.CONTROL.forward(on);
        return on.toString();
    }

    @GET
    @Path("/backward/{on}")
    @Produces(MediaType.TEXT_PLAIN)
    public String backward(@PathParam("on") Boolean on) {
        PirrariControl.CONTROL.backward(on);
        return on.toString();
    }

    @GET
    @Path("/peripheralsPower/{on}")
    @Produces(MediaType.TEXT_PLAIN)
    public String peripheralsPower(@PathParam("on") Boolean on) {
        PirrariControl.CONTROL.peripheralsPower(on);
        return on.toString();
    }

    @GET
    @Path("/metrics")
    @Produces(MediaType.APPLICATION_JSON)
    public Metrics getMetrics() {
        Metrics metrics = new Metrics();

        try {

            metrics.setMotorsReady(String.valueOf(PirrariControl.CONTROL.getMotorsReady()));
            metrics.setPeripheralsPower(String.valueOf(PirrariControl.CONTROL.getPeripheralsPower()));
            metrics.setDistance(String.valueOf(PirrariControl.CONTROL.getDistance()));
            metrics.setMotorSpeed(String.valueOf(PirrariControl.CONTROL.getMotorSpeed()));
            metrics.setOverallCurrent(String.format("%.1f", PirrariControl.CONTROL.getOverallCurrent() * CURRENT_UNIT));
            metrics.setMotorCurrent(String.format("%.1f", PirrariControl.CONTROL.getMotorCurrent() * MOTOR_CURRENT_UNIT));

            Process process = new ProcessBuilder("/bin/bash", "-c",
                    "iwconfig wlan0 | perl -l -ne '/Quality=[0-9]{2}\\/100+/ && print $&' | cut -d= -f2").start();

            try (InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream());
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

                metrics.setSignalStrength(bufferedReader.readLine());

            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return metrics;
    }

    @POST
    @Path("/speed/{speed}")
    public void setSpeed(@PathParam("speed") Integer speed) {
        try {
            PirrariControl.CONTROL.setMotorSpeed(speed);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @POST
    @Path("/shutdown")
    public void shutdown() {
        try {
            new ProcessBuilder("/bin/bash", "-c", "halt").start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
