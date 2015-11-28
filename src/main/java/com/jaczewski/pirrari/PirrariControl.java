package com.jaczewski.pirrari;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.jaczewski.pirrari.data.Metrics;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.io.spi.SpiFactory;

@Component
@Scope("singleton")
public class PirrariControl {

    private static final float CURRENT_UNIT = 2.0f; // mA
    private static final float MOTOR_CURRENT_UNIT = 2.0f; // mA

    private static final byte DUMMY_BYTE = 0x0F;
    private static final byte SET_MOTOR_SPEED = 0x01;
    private static final byte GET_MOTOR_SPEED = 0x02;

    private static final byte GET_DISTANCE = 0x01;
    private static final byte GET_CURRENT_LOWER_HALF = 0x02;
    private static final byte GET_CURRENT_UPPER_HALF = 0x03;
    private static final byte GET_MOTOR_CURRENT_LOWER_HALF = 0x04;
    private static final byte GET_MOTOR_CURRENT_UPPER_HALF = 0x05;

    private GpioController gpioController;

    private GpioPinDigitalOutput operational;

    private GpioPinDigitalOutput left;
    private GpioPinDigitalOutput right;
    private GpioPinDigitalOutput forward;
    private GpioPinDigitalOutput backward;

    private GpioPinDigitalOutput peripheralsPower;
    private boolean peripheralsPowerValue = false;

    private GpioPinDigitalInput motorsReady;

    private SpiDevice metricsSensor;
    private SpiDevice motorSpeed;

    private Metrics metrics = new Metrics();

    @PostConstruct
    public void init() throws IOException {
        gpioController = GpioFactory.getInstance();
        metricsSensor = SpiFactory.getInstance(SpiChannel.CS0, 100000);
        motorSpeed = SpiFactory.getInstance(SpiChannel.CS1, 100000);

        // wake on WiFi marker
        operational = gpioController.provisionDigitalOutputPin(RaspiPin.GPIO_05, "operational", PinState.HIGH);

        left = gpioController.provisionDigitalOutputPin(RaspiPin.GPIO_21, "left", PinState.LOW);
        right = gpioController.provisionDigitalOutputPin(RaspiPin.GPIO_22, "right", PinState.LOW);
        forward = gpioController.provisionDigitalOutputPin(RaspiPin.GPIO_23, "forward", PinState.LOW);
        backward = gpioController.provisionDigitalOutputPin(RaspiPin.GPIO_24, "backward", PinState.LOW);
        peripheralsPower = gpioController
                .provisionDigitalOutputPin(RaspiPin.GPIO_07, "peripheralsPower", PinState.HIGH);
        motorsReady = gpioController.provisionDigitalInputPin(RaspiPin.GPIO_04, "motorsReady");

        fetchMetrics();
    }

    private void fetchMetrics() {
        CompletableFuture.supplyAsync(() -> {
            Metrics newMetrics = new Metrics();
            try {
                synchronized (peripheralsPower) {
                    newMetrics.setMotorsReady(String.valueOf(getMotorsReady()));
                    newMetrics.setPeripheralsPower(String.valueOf(getPeripheralsPower()));
                    newMetrics.setDistance(String.valueOf(getDistance()));
                    newMetrics.setMotorSpeed(String.valueOf(getMotorSpeed()));
                    newMetrics.setOverallCurrent(String.format("%.1f", getOverallCurrent() * CURRENT_UNIT));
                    newMetrics.setMotorCurrent(String.format("%.1f", getMotorCurrent() * MOTOR_CURRENT_UNIT));
                    newMetrics.setSignalStrength(getSignalStrength());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return newMetrics;
        }).exceptionally(ex -> metrics).thenAccept(newMetrics -> {
            metrics = newMetrics;
            fetchMetrics();
        });
    }

    @PreDestroy
    public void close() {
        operational.low();
        gpioController.shutdown();
    }

    public void left(boolean on) {
        this.left.setState(on);
    }

    public void right(boolean on) {
        this.right.setState(on);
    }

    public void forward(boolean on) {
        this.forward.setState(on);
    }

    public void backward(boolean on) {
        this.backward.setState(on);
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public void peripheralsPower(boolean on) {
        synchronized (peripheralsPower) {
            this.peripheralsPower.setState(!on);
            this.peripheralsPowerValue = on;
        }
    }

    public boolean getPeripheralsPower() {
        return this.peripheralsPowerValue;
    }

    public boolean getMotorsReady() {
        return this.motorsReady.isLow() && peripheralsPowerValue;
    }

    public int getDistance() throws IOException, InterruptedException {
        if (!peripheralsPowerValue) {
            return 0;
        }

        return receiveUnsignedValue(metricsSensor, GET_DISTANCE, 80);
    }

    public int getOverallCurrent() throws IOException, InterruptedException {
        if (!peripheralsPowerValue) {
            return 0;
        }

        int current = receiveUnsignedValue(metricsSensor, GET_CURRENT_LOWER_HALF, 10);
        Thread.sleep(10);
        return (receiveUnsignedValue(metricsSensor, GET_CURRENT_UPPER_HALF, 100) << 8) | current;
    }

    public int getMotorCurrent() throws IOException, InterruptedException {
        if (!peripheralsPowerValue) {
            return 0;
        }

        int motorCurrent = receiveUnsignedValue(metricsSensor, GET_MOTOR_CURRENT_LOWER_HALF, 100);

        return (receiveUnsignedValue(metricsSensor, GET_MOTOR_CURRENT_UPPER_HALF, 100) << 8) | motorCurrent;
    }

    public void setMotorSpeed(int speed) throws IOException, InterruptedException {
        synchronized (peripheralsPower) {
            if (!peripheralsPowerValue) {
                return;
            }

            sendValue(motorSpeed, SET_MOTOR_SPEED, (byte) speed, 10);
        }
    }

    public int getMotorSpeed() throws IOException, InterruptedException {
        synchronized (peripheralsPower) {
            if (!peripheralsPowerValue) {
                return 0;
            }

            return receiveUnsignedValue(motorSpeed, GET_MOTOR_SPEED, 10);
        }
    }

    public String getSignalStrength() throws IOException {
        Process process = new ProcessBuilder("/bin/bash", "-c",
                "iwconfig wlan0 | perl -l -ne '/Signal level=[0-9]{1,3}\\/100+/ && print $&' | cut -d= -f2").start();

        String signalStrength = null;
        try (InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            signalStrength = bufferedReader.readLine();

        }

        return signalStrength;
    }

    private void sendValue(SpiDevice device, byte command, byte value, int timeout) throws IOException,
            InterruptedException {
        Thread.sleep(timeout);
        device.write(command);
        Thread.sleep(timeout);
        device.write(value);
    }

    private int receiveUnsignedValue(SpiDevice device, byte command, int timeout) throws IOException,
            InterruptedException {
        Thread.sleep(timeout);
        device.write(command);
        Thread.sleep(timeout);
        device.write(DUMMY_BYTE);
        Thread.sleep(timeout);
        return Byte.toUnsignedInt(device.write(DUMMY_BYTE)[0]);
    }
}
