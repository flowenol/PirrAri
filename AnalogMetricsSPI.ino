#include <SPI.h>

#define TIMEOUT -1 
#define CURRENT_COUNT_MAX 5
#define CURRENT_ZERO_READ_VALUE 514

int triggerPin = 9;
int echoPin = 8;
int overallCurrentPin = A1;
int motorCurrentPin = A2;

int distance = 0;

int overallCurrent = 0;
int overallCurrentSum = 0;
int overallCurrentCount = 0;

int motorCurrent = 0;
int motorCurrentSum = 0;
int motorCurrentCount = 0;

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
  SPDR = byte(distance); 
}  // end of interrupt service routine (ISR) SPI_STC_vect


void loop() {
  delay(100);
  
  distanceMeasure();
 
  overallCurrentMeasure();
  motorCurrentMeasure();
}

void overallCurrentMeasure() {
  if (overallCurrentCount == CURRENT_COUNT_MAX) {
    overallCurrent = CURRENT_ZERO_READ_VALUE - (overallCurrentSum / CURRENT_COUNT_MAX);
    overallCurrentSum = 0;
    overallCurrentCount = 0; 
  }
  overallCurrentSum += analogRead(overallCurrentPin);
  overallCurrentCount++;
}

void motorCurrentMeasure() {
  if (motorCurrentCount == CURRENT_COUNT_MAX) {
    motorCurrent = CURRENT_ZERO_READ_VALUE - (motorCurrentSum / CURRENT_COUNT_MAX);
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
    Serial.println("Timeout!!!");
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
