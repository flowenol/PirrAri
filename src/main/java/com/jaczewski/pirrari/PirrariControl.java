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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Scope("singleton")
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

    @PostConstruct
    public void init() throws IOException {
        /*gpioController = GpioFactory.getInstance();
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
        */
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

            return receiveUnsignedValue(metricsSensor, GET_DISTANCE, 80);
        }
    }

    public synchronized int getOverallCurrent() throws IOException, InterruptedException {
        synchronized (peripheralsPower) {

            if (!peripheralsPowerValue) {
                return 0;
            }

            int current = receiveUnsignedValue(metricsSensor, GET_CURRENT_LOWER_HALF, 10);
            Thread.sleep(10);
            return (receiveUnsignedValue(metricsSensor, GET_CURRENT_UPPER_HALF, 100) << 8) | current;
        }
    }

    public synchronized int getMotorCurrent() throws IOException, InterruptedException {
        synchronized (peripheralsPower) {

            if (!peripheralsPowerValue) {
                return 0;
            }

            int motorCurrent = receiveUnsignedValue(metricsSensor, GET_MOTOR_CURRENT_LOWER_HALF, 100);

            return (receiveUnsignedValue(metricsSensor, GET_MOTOR_CURRENT_UPPER_HALF, 100) << 8) | motorCurrent;

        }
    }

    public void setMotorSpeed(int speed) throws IOException, InterruptedException {
        synchronized (peripheralsPower) {

            if (!peripheralsPowerValue) {
                return;
            }

           sendValue(motorSpeed, SET_MOTOR_SPEED, (byte) speed, 10);
        }
    }

    public synchronized int getMotorSpeed() throws IOException, InterruptedException {
        synchronized (peripheralsPower) {

            if (!peripheralsPowerValue) {
                return 0;
            }

            return receiveUnsignedValue(motorSpeed, GET_MOTOR_SPEED, 10);
        }
    }

    private void sendValue(SpiDevice device, byte command, byte value, int timeout) throws IOException, InterruptedException {
        Thread.sleep(timeout);
        device.write(command);
        Thread.sleep(timeout);
        device.write(value);
    }

    private int receiveUnsignedValue(SpiDevice device, byte command, int timeout) throws IOException, InterruptedException {
        Thread.sleep(timeout);
        device.write(command);
        Thread.sleep(timeout);
        device.write(DUMMY_BYTE);
        Thread.sleep(timeout);
        return Byte.toUnsignedInt(device.write(DUMMY_BYTE)[0]);
    }
}
