#include <SPI.h>

#define TIMEOUT -1 

int triggerPin = 9;
int echoPin = 8;
int distance = 0;

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
  Serial.println("SPI");
  SPDR = byte(distance); 
}  // end of interrupt service routine (ISR) SPI_STC_vect


void loop() {
  delay(500);
  trigger();
  
  long echoDuration = echo();
  if (echoDuration == TIMEOUT) {
    Serial.println("Timeout!!!");
    return;
  }
  
  float distanceCm = (float((echoDuration / 2)) / 1000000) * 340.29 * 100;
  distance = int(distanceCm);
  Serial.print("Distance: ");
  Serial.println(distance);
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
