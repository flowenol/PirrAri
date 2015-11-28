 
#include <SoftwareSerial.h>
#define SSID        "dude"
#define PASS        "voodoo123"
#define SHUTDOWN_TIMEOUT 20000

SoftwareSerial wifiSerial(10, 16);
SoftwareSerial debug(14, 15); // RX, TX

const int relayPin = 9;
const int devicePin = 7;
const int softwareUpdatePin = 6;
const int chpdPin = 8;

String command = "";
boolean commandFinished = false;

boolean relayStatus = false;

int operational = LOW;
boolean operationalState = false;
int operationalCheckCounter = 0;

void setup()
{
  // Open wifiSerial communications and wait for port to open:
  pinMode(relayPin, OUTPUT);
  pinMode(devicePin, INPUT);
  pinMode(softwareUpdatePin, INPUT);
  pinMode(chpdPin, OUTPUT);
  
  digitalWrite(relayPin, HIGH);
  digitalWrite(chpdPin, HIGH);
  
  debug.begin(9600);  //can't be faster than 19200
  wifiSerial.begin(9600);
  wifiSerial.setTimeout(6000);
  wifiSerial.listen();
  
  // test if the module is ready
  boolean initialized = false;
  for (int i = 0; i < 100 ; i++) {
    if (initialize()) {
      initialized = true;
      break;
    } 
  }
  
  if (!initialized) {
    die();
  }
  
  delay(1000);
  
  // connect to the wifi
  boolean connected = false;
  for (int i = 0; i < 100; i++)
  {
    if (connect())
    {
      connected = true;
      break;
    }
  }
  if (!connected) {
    die();
  }
  
  //print the ip addr
  printIp(); 
  
  // connect to the wifi
  boolean opened = false;
  for (int i = 0; i < 5; i++)
  {
    if (openPort())
    {
      opened = true;
      break;
    }
  }
  if (!opened) {
    die();
  }
  
  wifiSerial.setTimeout(100);
}

void loop()
{
  
  checkIfIsOperationalAndShutdown();
  
  while (wifiSerial.available()) {
    char character = wifiSerial.read();
    
    if (character == '\n' || character == '\r') {
      commandFinished = true; 
      break; 
    }
    
    command += String(character);
   }
   
   if (commandFinished) {
     command.trim();
     
     if (command == "Link") {
       debug.println("Someone has connected.");
       wifiSerial.println("AT+CIPSEND=0,2");
       wifiSerial.print("> ");
     }
     
     if (command.substring(0, 4) == "+IPD") {
       debug.println("Received command: " + command);
        
       if (command == "+IPD,0,4:on") {
         debug.println("On command received");
         relayStatus = true;
         digitalWrite(relayPin, LOW);
       }
       
       if (command == "+IPD,0,5:off") {
         debug.println("Off command received");
         relayStatus = false;
         operationalState = false;
         digitalWrite(relayPin, HIGH);
       }
       
        if (command == "+IPD,0,8:status") {
         debug.println("Status command received");
         
         String statusMsg;
         if (relayStatus) {
           statusMsg = "Relay is On.";
         } else {
           statusMsg = "Relay is Off.";
         }
         
         wifiSerial.print("AT+CIPSEND=0,");
         wifiSerial.println(statusMsg.length() + 2);
         wifiSerial.println(statusMsg);
       }
       
       if (command == "+IPD,0,8:device") {
         debug.println("Device command received");
         operational = digitalRead(devicePin);
         
         String deviceMsg;
         if (operational == HIGH) {
           deviceMsg = "Device is On.";
         } else {
           deviceMsg = "Device is Off.";
         }
         
         wifiSerial.print("AT+CIPSEND=0,");
         wifiSerial.println(deviceMsg.length() + 2);
         wifiSerial.println(deviceMsg);
       } 
     
       delay(200);
       
       wifiSerial.println("AT+CIPSEND=0,2");
       wifiSerial.print("> ");
       
     } 
     
     command = "";
     commandFinished = false;
   }
}

void checkIfIsOperationalAndShutdown() {
  operationalCheckCounter++;
  if (operationalCheckCounter == 1000) {
     operational = digitalRead(devicePin);
    
     if (operational == HIGH) {
       if (operationalState == false) {
         delay(1000);
         operational = digitalRead(devicePin);
         if (operational == HIGH) {
           debug.println("Device is on now");
           operationalState = true;
         }          
       }
     } else if (operational == LOW) {
        if (operationalState == true) {
           delay(1000);
           
           operational = digitalRead(devicePin);
           if (operational == LOW) {
             
             debug.println("Device is off now");
             operationalState = false;

             if (digitalRead(softwareUpdatePin) == LOW) {             
               debug.println("Shutting down device...");
               delay(SHUTDOWN_TIMEOUT);
               debug.println("Device shutdown");
               
               digitalWrite(relayPin, HIGH);
               relayStatus = false;
             } else {
               debug.println("Device is off due to software update");  
             }
           }
        } 
     }
     
     operationalCheckCounter = 0;
  } 
}

boolean initialize() {
  wifiSerial.println("AT+RST");
  if (wifiSerial.find("ready")) {
    debug.println("OK, module is ready.");
    return true;
  }
  debug.println("ERROR, no response from module.");
  return false;
}

boolean connect()
{
  wifiSerial.println("AT+CWMODE=1");
  String cmd = "AT+CWJAP=\"";
  cmd += SSID;
  cmd += "\",\"";
  cmd += PASS;
  cmd += "\"";
  wifiSerial.println(cmd);

  if (wifiSerial.find("OK")) { 
      debug.println("OK, connected to WiFi.");
      return true;
  }
  debug.println("Error, not connected to WiFi.");
  return false;
}

void printIp() {
  wifiSerial.println("AT+CIFSR");
  debug.print("Ip Address: ");
  char ip[50];
  wifiSerial.readBytesUntil('O', ip, 50);
  String ipString = String(ip);
  ipString.remove(0, 10);
  ipString.trim();
  debug.println(ipString.substring(0, 15)); 
}

boolean openPort() {
 // set multiple connection mode
  wifiSerial.println("AT+CIPMUX=1");
  if (!wifiSerial.find("OK")) {
    debug.println("ERROR, couldn't set multiple connection mode.");
    return false;
  }
  // set server socket
  wifiSerial.println("AT+CIPSERVER=1,9999");
  if (!wifiSerial.find("OK")) {
    debug.println("ERROR, couldn't open server socket."); 
    
    return false;
  }
  debug.println("OK, opened port 9999."); 
 
  return true;
}

void die() {
  debug.println("Dying...");
  while(1) delay(1000);
}
