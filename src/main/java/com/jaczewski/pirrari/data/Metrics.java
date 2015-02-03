package com.jaczewski.pirrari.data;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "metrics")
public class Metrics {

    private String signalStrength;
    private String motorSpeed;
    private String distance;

    public String getSignalStrength() {
        return signalStrength;
    }

    public void setSignalStrength(String signalStrength) {
        this.signalStrength = signalStrength;
    }

    public String getMotorSpeed() {
        return motorSpeed;
    }

    public void setMotorSpeed(String motorSpeed) {
        this.motorSpeed = motorSpeed;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }
}
