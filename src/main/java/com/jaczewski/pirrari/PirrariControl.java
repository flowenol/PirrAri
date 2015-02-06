package com.jaczewski.pirrari;

import java.io.IOException;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.io.spi.SpiFactory;

public class PirrariControl {

    private static final byte DUMMY_BYTE = 0x03;
    private static final byte SET_MOTOR_SPEED = 0x01;
    private static final byte GET_MOTOR_SPEED = 0x02;

    public static final PirrariControl CONTROL = new PirrariControl();


    private GpioController gpioController;

    private GpioPinDigitalOutput left;
    private GpioPinDigitalOutput right;
    private GpioPinDigitalOutput forward;
    private GpioPinDigitalOutput backward;

    private GpioPinDigitalOutput peripheralsPower;
    private boolean peripheralsPowerValue = true;

    private GpioPinDigitalInput motorsReady;

    private SpiDevice metricsSensor;
    private SpiDevice motorSpeed;

    public void init() throws IOException {
        gpioController = GpioFactory.getInstance();
        metricsSensor = SpiFactory.getInstance(SpiChannel.CS0, 100000);
        motorSpeed = SpiFactory.getInstance(SpiChannel.CS1, 100000);

        left = gpioController.provisionDigitalOutputPin(RaspiPin.GPIO_21, "left", PinState.LOW);
        right = gpioController.provisionDigitalOutputPin(RaspiPin.GPIO_22, "right", PinState.LOW);
        forward = gpioController.provisionDigitalOutputPin(RaspiPin.GPIO_23, "forward", PinState.LOW);
        backward = gpioController.provisionDigitalOutputPin(RaspiPin.GPIO_24, "backward", PinState.LOW);
        peripheralsPower = gpioController.provisionDigitalOutputPin(RaspiPin.GPIO_07, "peripheralsPower", PinState.HIGH);
        motorsReady = gpioController.provisionDigitalInputPin(RaspiPin.GPIO_04, "motorsReady");
    }

    public void close() {
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

    public void peripheralsPower(boolean on) {
        this.peripheralsPower.setState(on);
        this.peripheralsPowerValue = on;
    }

    public boolean getPeripheralsPower() {
        return this.peripheralsPowerValue;
    }

    public boolean getMotorsReady() {
        return this.motorsReady.isLow();
    }

    public synchronized int getDistance() throws IOException, InterruptedException {
        if (!peripheralsPowerValue) {
            return 0;
        }
        synchronized (metricsSensor) {
            return metricsSensor.write(DUMMY_BYTE)[0];
        }
    }

    public void setMotorSpeed(int speed) throws IOException, InterruptedException {
        if (!peripheralsPowerValue) {
            return;
        }
        synchronized (motorSpeed) {
            motorSpeed.write(SET_MOTOR_SPEED);
            Thread.sleep(10);
            motorSpeed.write((byte) speed);
        }
    }

    public synchronized int getMotorSpeed() throws IOException, InterruptedException {
        if (!peripheralsPowerValue) {
            return 0;
        }
        synchronized (motorSpeed) {
            motorSpeed.write(GET_MOTOR_SPEED);
            Thread.sleep(10);
            motorSpeed.write(DUMMY_BYTE);
            Thread.sleep(10);

            return motorSpeed.write(DUMMY_BYTE)[0];
        }
    }
}
