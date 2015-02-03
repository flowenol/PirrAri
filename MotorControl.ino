#include <PololuQik.h>
#include <SoftwareSerial.h>
#include <SPI.h>

#define DIRECTION_MOTOR_SPEED 60

PololuQik2s9v1 pololuQik(3, 2, 4);

int leftPin = 5;
int rightPin = 6;

int forwardPin = 8;
int backwardPin = 9;

int left = LOW;
int right = LOW;

int forward = LOW;
int backward = LOW;

byte command = 0x00;

byte currentDirectionMotorSpeed = 0;
byte currentMotorSpeed = 0;
byte motorSpeed = 60;

void setup() {
  pinMode(leftPin, INPUT);
  pinMode(rightPin, INPUT);
  pinMode(forwardPin, INPUT);
  pinMode(backwardPin, INPUT);
  pololuQik.init();

  // setup SPI
  // have to send on master in, *slave out*
  pinMode(MISO, OUTPUT);

  // turn on SPI in slave mode
  SPCR |= _BV(SPE);

  // turn on interrupts
  SPCR |= _BV(SPIE);
  //SPI.attachInterrupt();

}

// SPI interrupt routine
ISR (SPI_STC_vect)
{
  Serial.println("SPI Interrupt");
  byte c = SPDR;
  
  Serial.print("Current command: ");
  Serial.println(command);
  
  switch (command) {
    case 0x00:
      if (c == 0x01 || c == 0x02) {
        command = c;  
        Serial.print("SPI set command: ");
        Serial.println(command);
      } else {
        Serial.print("SPI Unknown command: ");
        Serial.println(c);
      }
      break;
    case 0x01:
      Serial.print("SPI setting motorSpeed ");
      Serial.println(c); 
      motorSpeed = c;
      command = 0x00;
      break;
    case 0x02:
      Serial.print("SPI sending motorSpeed ");
      Serial.println(motorSpeed);
      SPDR = motorSpeed;
      command = 0x00;
      break;
  }
  Serial.println("------------------"); 
}  

void loop() {
  delay(10);
  left = digitalRead(leftPin);
  right = digitalRead(rightPin);
  
  if (left == HIGH && right == HIGH) {
    pololuQik.setM0Coast();
    currentDirectionMotorSpeed = 0;
  } else if (left == HIGH && currentDirectionMotorSpeed != DIRECTION_MOTOR_SPEED) {
    pololuQik.setM0Speed(DIRECTION_MOTOR_SPEED);
    currentDirectionMotorSpeed = DIRECTION_MOTOR_SPEED;
  } else if (right == HIGH && currentDirectionMotorSpeed != DIRECTION_MOTOR_SPEED) {
    pololuQik.setM0Speed(-60);
    currentDirectionMotorSpeed = DIRECTION_MOTOR_SPEED;
  } else if (left == LOW && right == LOW) {
    pololuQik.setM0Coast();
    currentDirectionMotorSpeed = 0;
  }
  
  forward = digitalRead(forwardPin);
  backward = digitalRead(backwardPin);
    
  if (forward == HIGH && backward == HIGH) {
    pololuQik.setM1Coast();
    currentMotorSpeed = 0;
  } else if (forward == HIGH && currentMotorSpeed != motorSpeed) {
    pololuQik.setM1Speed(-motorSpeed);
    currentMotorSpeed = motorSpeed;
  } else if (backward == HIGH && currentMotorSpeed != motorSpeed) {
    pololuQik.setM1Speed(motorSpeed);
    currentMotorSpeed = motorSpeed;
  } else if (forward == LOW && backward == LOW) {
    pololuQik.setM1Coast();
    currentMotorSpeed = 0;
  }  
}


