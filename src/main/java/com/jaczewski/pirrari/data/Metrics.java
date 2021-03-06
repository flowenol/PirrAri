package com.jaczewski.pirrari.data;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "metrics")
public class Metrics {

    private String signalStrength;
    private String motorSpeed;
    private String distance;
    private String peripheralsPower;
    private String motorsReady;
    private String overallCurrent;
    private String motorCurrent;

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

    public String getPeripheralsPower() {
        return peripheralsPower;
    }

    public void setPeripheralsPower(String peripheralsPower) {
        this.peripheralsPower = peripheralsPower;
    }

    public String getMotorsReady() {
        return motorsReady;
    }

    public void setMotorsReady(String motorsReady) {
        this.motorsReady = motorsReady;
    }

    public String getOverallCurrent() {
        return overallCurrent;
    }

    public void setOverallCurrent(String overallCurrent) {
        this.overallCurrent = overallCurrent;
    }

    public String getMotorCurrent() {
        return motorCurrent;
    }

    public void setMotorCurrent(String motorCurrent) {
        this.motorCurrent = motorCurrent;
    }
}
