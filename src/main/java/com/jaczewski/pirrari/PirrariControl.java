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

    private static final byte DUMMY_BYTE = 0x0F;
    private static final byte SET_MOTOR_SPEED = 0x01;
    private static final byte GET_MOTOR_SPEED = 0x02;

    private static final byte GET_DISTANCE = 0x01;
    private static final byte GET_CURRENT_LOWER_HALF = 0x02;
    private static final byte GET_CURRENT_UPPER_HALF = 0x03;
    private static final byte GET_MOTOR_CURRENT_LOWER_HALF = 0x04;
    private static final byte GET_MOTOR_CURRENT_UPPER_HALF = 0x05;

    public static final PirrariControl CONTROL = new PirrariControl();

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
        peripheralsPower = gpioController.provisionDigitalOutputPin(RaspiPin.GPIO_07, "peripheralsPower", PinState.HIGH);
        motorsReady = gpioController.provisionDigitalInputPin(RaspiPin.GPIO_04, "motorsReady");
    }

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

    public synchronized int getDistance() throws IOException, InterruptedException {
        synchronized (peripheralsPower) {

            if (!peripheralsPowerValue) {
                return 0;
            }

            metricsSensor.write(GET_DISTANCE);
            Thread.sleep(10);
            metricsSensor.write(DUMMY_BYTE);
            Thread.sleep(10);

            return metricsSensor.write(DUMMY_BYTE)[0];
        }
    }

    public synchronized int getOverallCurrent() throws IOException, InterruptedException {
        synchronized (peripheralsPower) {

            if (!peripheralsPowerValue) {
                return 0;
            }

            metricsSensor.write(GET_CURRENT_LOWER_HALF);
            Thread.sleep(10);
            metricsSensor.write(DUMMY_BYTE);
            Thread.sleep(10);

            int current = Byte.toUnsignedInt(metricsSensor.write(DUMMY_BYTE)[0]);
            Thread.sleep(10);

            metricsSensor.write(GET_CURRENT_UPPER_HALF);
            Thread.sleep(10);
            metricsSensor.write(DUMMY_BYTE);
            Thread.sleep(10);

            return (Byte.toUnsignedInt(metricsSensor.write(DUMMY_BYTE)[0]) << 8) | current;
        }
    }

    public synchronized int getMotorCurrent() throws IOException, InterruptedException {
        synchronized (peripheralsPower) {

            if (!peripheralsPowerValue) {
                return 0;
            }

            metricsSensor.write(GET_MOTOR_CURRENT_LOWER_HALF);
            Thread.sleep(10);
            metricsSensor.write(DUMMY_BYTE);
            Thread.sleep(10);

            int motorCurrent = Byte.toUnsignedInt(metricsSensor.write(DUMMY_BYTE)[0]);
            Thread.sleep(10);

            metricsSensor.write(GET_MOTOR_CURRENT_UPPER_HALF);
            Thread.sleep(10);
            metricsSensor.write(DUMMY_BYTE);
            Thread.sleep(10);

            return (Byte.toUnsignedInt(metricsSensor.write(DUMMY_BYTE)[0]) << 8) | motorCurrent;

        }
    }

    public void setMotorSpeed(int speed) throws IOException, InterruptedException {
        synchronized (peripheralsPower) {

            if (!peripheralsPowerValue) {
                return;
            }

            motorSpeed.write(SET_MOTOR_SPEED);
            Thread.sleep(10);
            motorSpeed.write((byte) speed);
        }
    }

    public synchronized int getMotorSpeed() throws IOException, InterruptedException {
        synchronized (peripheralsPower) {

            if (!peripheralsPowerValue) {
                return 0;
            }

            motorSpeed.write(GET_MOTOR_SPEED);
            Thread.sleep(10);
            motorSpeed.write(DUMMY_BYTE);
            Thread.sleep(10);

            return motorSpeed.write(DUMMY_BYTE)[0];
        }
    }
}
