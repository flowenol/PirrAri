 
#include <SoftwareSerial.h>
#define SSID        "dude"
#define PASS        "voodoo123"
#define DST_IP      "220.181.111.85"    //baidu.com

SoftwareSerial dbgSerial(10, 11); // RX, TX

String command = "";
boolean commandFinished = false;

void setup()
{
  // Open serial communications and wait for port to open:
  Serial.begin(9600);
  Serial.setTimeout(6000);
  dbgSerial.begin(9600);  //can't be faster than 19200 for softserial
  
  // test if the module is ready
  boolean initialized = false;
  for (int i = 0; i < 5 ; i++) {
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
  
  Serial.setTimeout(100);
}
void loop()
{
   while (Serial.available()) {
    char character = Serial.read();
    
    if (character == '\n' || character == '\r') {
      commandFinished = true; 
      break; 
    }
    
    command += String(character);
   }
   
   if (commandFinished) {
     command.trim();
     
     if (command != "" && command == "Link") {
       dbgSerial.println("Someone has connected.");
       Serial.println("AT+CIPSEND=0,23");
       Serial.println("What is your command?");
     }
     if (command != "" && command.substring(0, 4) == "+IPD") {
       dbgSerial.println("Received command: " + command);
       
       if (command == "+IPD,0,8:status") {
         dbgSerial.println("Status command received");
         Serial.println("AT+CIPSEND=0,5");
         Serial.println("off");
       }
       
       delay(100);
       
       Serial.println("AT+CIPSEND=0,23");
       Serial.println("What is your command?");
     }
     command = "";
     commandFinished = false;
   }
}

boolean initialize() {
  Serial.println("AT+RST");
  if (Serial.find("ready")) {
    dbgSerial.println("OK, module is ready.");
    return true;
  }
  dbgSerial.println("ERROR, no response from module.");
  return false;
}

boolean connect()
{
  Serial.println("AT+CWMODE=1");
  String cmd = "AT+CWJAP=\"";
  cmd += SSID;
  cmd += "\",\"";
  cmd += PASS;
  cmd += "\"";
  Serial.println(cmd);

  if (Serial.find("OK")) { 
      dbgSerial.println("OK, connected to WiFi.");
      return true;
  }
  dbgSerial.println("Error, not connected to WiFi.");
  return false;
}

void printIp() {
  Serial.println("AT+CIFSR");
  dbgSerial.print("Ip Address: ");
  char ip[50];
  Serial.readBytesUntil('O', ip, 50);
  String ipString = String(ip);
  ipString.remove(0, 10);
  ipString.trim();
  dbgSerial.println(ipString.substring(0, 15)); 
}

boolean openPort() {
 // set multiple connection mode
  Serial.println("AT+CIPMUX=1");
  if (!Serial.find("OK")) {
    dbgSerial.println("ERROR, couldn't set multiple connection mode.");
    return false;
  }
  // set server socket
  Serial.println("AT+CIPSERVER=1,9999");
  if (!Serial.find("OK")) {
    dbgSerial.println("ERROR, couldn't open server socket."); 
    
    return false;
  }
  dbgSerial.println("OK, opened port 9999."); 
 
  return true;
}

void die() {
  dbgSerial.println("Dying...");
  while(1) delay(1000);
}
