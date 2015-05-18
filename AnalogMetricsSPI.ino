#include <SPI.h>

#define TIMEOUT -1 
#define CURRENT_COUNT_MAX 200
#define MOTOR_CURRENT_COUNT_MAX 200
#define CURRENT_ZERO_READ_VALUE 700
#define MOTOR_CURRENT_ZERO_READ_VALUE 675

int triggerPin = 9;
int echoPin = 8;
int overallCurrentPin = A1;
int motorCurrentPin = A2;

int distance = 0;

int overallCurrent = 0;
long overallCurrentSum = 0;
int overallCurrentCount = 0;

int motorCurrent = 0;
long motorCurrentSum = 0;
int motorCurrentCount = 0;

byte command = 0x00;

void setup() {
  Serial.begin(9600);
  pinMode(triggerPin, OUTPUT);
  pinMode(echoPin, INPUT);
  
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
  ////Serial.println("SPI Interrupt");
  byte c = SPDR;
  
  //Serial.print("Current command: ");
  ////Serial.println(command);
  
  switch (command) {
    case 0x00:
      if (c == 0x01 || c == 0x02 || c == 0x03 || c == 0x04 || c == 0x05) {
        command = c;  
        //Serial.print("SPI set command: ");
        //Serial.println(command);
      } else {
        //Serial.print("SPI Unknown command: ");
        //Serial.println(c);
      }
      break;
    case 0x01:
      //Serial.print("SPI sending distance ");
      //Serial.println(distance); 
      SPDR = byte(distance);
      command = 0x00;
      break;
    case 0x02:
      //Serial.println("SPI sending overall current lower half");
      SPDR = byte(overallCurrent);
      command = 0x00;
      break;
    case 0x03:
      //Serial.println("SPI sending overall current upper half");
      SPDR = byte(overallCurrent >> 8);
      command = 0x00;
      break;
    case 0x04:
      //Serial.println("SPI sending motor current lower half");
      SPDR = byte(motorCurrent);
      command = 0x00;
      break;
    case 0x05:
      //Serial.println("SPI sending motor current upper half");
      SPDR = byte(motorCurrent >> 8);
      command = 0x00;
      break;
  }
  //Serial.println("------------------"); 
}  // end of interrupt service routine (ISR) SPI_STC_vect


void loop() {
  delay(10);
  
  distanceMeasure();
 
  overallCurrentMeasure();
  motorCurrentMeasure();
}

void overallCurrentMeasure() {
  if (overallCurrentCount == CURRENT_COUNT_MAX) {
    overallCurrent = (overallCurrentSum / CURRENT_COUNT_MAX) - CURRENT_ZERO_READ_VALUE;
    if (overallCurrent < 0) {
      overallCurrent = 0;
    }
    //overallCurrent = abs(overallCurrentSum / CURRENT_COUNT_MAX);
    overallCurrentSum = 0;
    overallCurrentCount = 0;
  }
  overallCurrentSum += analogRead(overallCurrentPin);
  overallCurrentCount++;
}

void motorCurrentMeasure() {
  if (motorCurrentCount == CURRENT_COUNT_MAX) {
    motorCurrent = (motorCurrentSum / MOTOR_CURRENT_COUNT_MAX) - MOTOR_CURRENT_ZERO_READ_VALUE;
    if (motorCurrent < 0) {
      motorCurrent = 0;
    }
    //motorCurrent = (motorCurrentSum / MOTOR_CURRENT_COUNT_MAX);  
    motorCurrentSum = 0;
    motorCurrentCount = 0; 
  }
  motorCurrentSum += analogRead(motorCurrentPin);
  motorCurrentCount++;
}

void distanceMeasure() {
  trigger();
  
  long echoDuration = echo();
  if (echoDuration == TIMEOUT) {
    return;
  }
  
  float distanceCm = (float((echoDuration / 2)) / 1000000) * 340.29 * 100;
  distance = int(distanceCm);
}

void trigger() {
  digitalWrite(triggerPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(triggerPin, LOW); 
}

long echo() {
  int timeout = 3000;
  while (digitalRead(echoPin) == LOW && timeout > 0) {
    timeout--;
  }

  if (timeout == 0) {
    return TIMEOUT;
  }
 
  timeout = 3000;
  unsigned long startTime = micros();
  unsigned long endTime = startTime;
 
  while (digitalRead(echoPin) == HIGH && timeout > 0) {
    endTime = micros();
    timeout--;
  }
 
  if (timeout == 0) {
     return TIMEOUT;
  }
 
  return endTime - startTime;   
}
