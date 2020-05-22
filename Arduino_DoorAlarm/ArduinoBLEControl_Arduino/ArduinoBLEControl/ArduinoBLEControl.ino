/*********************************************************************
  * Laura Arjona. UW EE P 523. SPRING 2020
  * Example of simple interaction beteween Adafruit Circuit Playground
  * and Android App. Communication with BLE - uart
*********************************************************************/
#include <Arduino.h>
#include <SPI.h>
#include "Adafruit_BLE.h"
#include "Adafruit_BluefruitLE_SPI.h"
#include "Adafruit_BluefruitLE_UART.h"
#include <Adafruit_CircuitPlayground.h>

#include "BluefruitConfig.h"

#if SOFTWARE_SERIAL_AVAILABLE
  #include <SoftwareSerial.h>
#endif

#define ARRAY_SIZE 5

const int speaker = 5;       // The CP microcontroller pin for the speaker
const int leftButton = 4;    // The CP microcontroller pin for the left button
const int rightButton = 19;  // The CP microcontroller pin for the right button

// Strings to compare incoming BLE messages
String start = "start";
String red = "red";
String readtemp = "readtemp";
String stp = "stop";

int  sensorTemp = 0;
float sensorX = 0;
float sensorY = 0;
float sensorZ = 0;
float valuesZ[] = {0,0,0,0,0};

long previousTime;

bool alarmMode = false;
bool alarmTriggered = false;
bool blinkToggle = true;
bool btnRightPressed = false;
bool btnLeftPressed = false;

/*=========================================================================
    APPLICATION SETTINGS
    -----------------------------------------------------------------------*/
    #define FACTORYRESET_ENABLE         0
    #define MINIMUM_FIRMWARE_VERSION    "0.6.6"
    #define MODE_LED_BEHAVIOUR          "MODE"
/*=========================================================================*/

// Create the bluefruit object, either software serial...uncomment these lines

Adafruit_BluefruitLE_UART ble(BLUEFRUIT_HWSERIAL_NAME, BLUEFRUIT_UART_MODE_PIN);

/* ...hardware SPI, using SCK/MOSI/MISO hardware SPI pins and then user selected CS/IRQ/RST */
// Adafruit_BluefruitLE_SPI ble(BLUEFRUIT_SPI_CS, BLUEFRUIT_SPI_IRQ, BLUEFRUIT_SPI_RST);

/* ...software SPI, using SCK/MOSI/MISO user-defined SPI pins and then user selected CS/IRQ/RST */
//Adafruit_BluefruitLE_SPI ble(BLUEFRUIT_SPI_SCK, BLUEFRUIT_SPI_MISO,
//                             BLUEFRUIT_SPI_MOSI, BLUEFRUIT_SPI_CS,
//                             BLUEFRUIT_SPI_IRQ, BLUEFRUIT_SPI_RST);


// A small helper to show errors on the serial monitor
void error(const __FlashStringHelper*err) {
  Serial.println(err);
  while (1);
}


void setup(void)
{
  CircuitPlayground.begin();
  pinMode(speaker, OUTPUT);     // write out to the speaker
  pinMode(leftButton, INPUT);   // read in from the buttons
  pinMode(rightButton,INPUT);
  

  Serial.begin(115200);

  /* Initialise the module */
  Serial.print(F("Initialising the Bluefruit LE module: "));

  if ( !ble.begin(VERBOSE_MODE) )
  {
    error(F("Couldn't find Bluefruit, make sure it's in CoMmanD mode & check wiring?"));
  }
  Serial.println( F("OK!") );

  if ( FACTORYRESET_ENABLE )
  {
    /* Perform a factory reset to make sure everything is in a known state */
    Serial.println(F("Performing a factory reset: "));
    if ( ! ble.factoryReset() ){
      error(F("Couldn't factory reset"));
    }
  }

  /* Disable command echo from Bluefruit */
  ble.echo(false);

  Serial.println("Requesting Bluefruit info:");
  /* Print Bluefruit information */
  ble.info();

  Serial.println(F("Please use Adafruit Bluefruit LE app to connect in UART mode"));
  Serial.println(F("Then Enter characters to send to Bluefruit"));
  Serial.println();

  ble.verbose(false);  // debug info is a little annoying after this point!

  /* Wait for connection */
  while (! ble.isConnected()) {
      delay(500);
  }
Serial.println("CONECTED:");
  Serial.println(F("******************************"));

  // LED Activity command is only supported from 0.6.6
  if ( ble.isVersionAtLeast(MINIMUM_FIRMWARE_VERSION) )
  {
    // Change Mode LED Activity
    Serial.println(F("Change LED activity to " MODE_LED_BEHAVIOUR));
    ble.sendCommandCheckOK("AT+HWModeLED=" MODE_LED_BEHAVIOUR);
  }

  // Set module to DATA mode
  Serial.println( F("Switching to DATA mode!") );
  ble.setMode(BLUEFRUIT_MODE_DATA);

  //set lights to green
  for(int i = 0; i < 11; i++){
    CircuitPlayground.setPixelColor(i,0,204,0);
  }
    delay(50);

  Serial.println(F("******************************"));
 
  delay(1000);
}
/**************************************************************************/
/*!
   Constantly poll for new command or response data
*/
/**************************************************************************/
void loop(void)
{

  while (alarmTriggered){
    for(int i = 0; i < 11; i++){
      CircuitPlayground.setPixelColor(i,221, 44, 44);
    }
    makeTone(speaker,440,1000);
    CircuitPlayground.clearPixels();
    delay(500);
  }
  
  // Save received data to string
  String received = "";
  while ( ble.available() )
  {
    int c = ble.read();
    Serial.print((char)c);
    received += (char)c;
        delay(50);
  }

//reading accelerometer sensor
  sensorX = CircuitPlayground.motionX();
  sensorY = CircuitPlayground.motionY();
  sensorZ = CircuitPlayground.motionZ();

  Serial.println((String)"Accel: " + sensorX + ", " + sensorY + ", " + sensorZ);

//keep running window of Z values from accelerometer
  for (int idx = ARRAY_SIZE-1; idx > 0; idx--){
    valuesZ[idx-1] = valuesZ[idx];
  }
  valuesZ[ARRAY_SIZE-1] = sensorZ;
  delay(100);
  
//check if motion has occured and send message to andriod by checking the difference between first and last value
  if (valuesZ[0] != 0 && abs(valuesZ[ARRAY_SIZE-1] - valuesZ[0]) > 1){
    Serial.println("DOOR OPENED");
    alarmMode = true;
    //Send data to Android Device
    char output[8];
    String data = "alarm";
    data.toCharArray(output,8);
    ble.print(data); 
  }
//alarm has been triggered
  if(alarmMode){
    blinkOrange(blinkToggle);
    blinkToggle = !blinkToggle;
    if (CircuitPlayground.rightButton()){
      btnRightPressed = true;
    }
    if (CircuitPlayground.leftButton()){
      btnLeftPressed = true;
    }

    if (btnRightPressed){
      if (btnLeftPressed){
        resetAlarm();
        //Send data to Android Device
        char output[8];
        String data = "reset";
        data.toCharArray(output,8);
        ble.print(data);
      }
    }
  }

  if(received == red){
    Serial.println("RECEIVED RED!!!!"); 
    for(int i = 0; i < 11; i++){
      CircuitPlayground.setPixelColor(i,221, 44, 44);
    }
    delay(50);
  }

  else if (received == "reset"){
    resetAlarm();
//    alarmMode = false;
//    btnLeftPressed = false;
//    btnRightPressed = false;
//    Serial.println("RESET ALARM");
//    //set lights back to green
//    for(int i = 0; i < 11; i++){
//    CircuitPlayground.setPixelColor(i,0,204,0);
//    }
  }

  else if (received == "ALARM"){
    Serial.println("TRIGGER ALARM - INTRUDER");
    alarmTriggered = true;
  }
 
  else if(received == readtemp){
       
    sensorTemp = CircuitPlayground.temperature(); // returns a floating point number in Centigrade
    Serial.println("Read temperature sensor"); 
    delay(10);

   //Send data to Android Device
    char output[8];
    String data = "";
    data += sensorTemp;
    Serial.println(data);
    data.toCharArray(output,8);
    ble.print(data);
  }
 
  else if (received == stp){
      CircuitPlayground.clearPixels();
      Serial.println("cleared");

    }
    
  }

  void resetAlarm(){
    alarmMode = false;
    btnLeftPressed = false;
    btnRightPressed = false;
    Serial.println("RESET ALARM");
    //set lights to green
    for(int i = 0; i < 11; i++){
      CircuitPlayground.setPixelColor(i,0,204,0);
    }
    delay(50);
    
  }

  void blinkOrange(bool toggle){
      for(int i = 0; i < 11; i++){
        if (toggle){
          CircuitPlayground.setPixelColor(i, 255, 128, 0);
        } else {
          CircuitPlayground.clearPixels();
          //CircuitPlayground.setPixelColor(i, 255, 255, 255);
        }
      }
      delay(100);
  }

  void makeTone (unsigned char speakerPin, int frequencyInHertz, long timeInMilliseconds) {
  int x;   
  long delayAmount = (long)(1000000/frequencyInHertz);
  long loopTime = (long)((timeInMilliseconds*1000)/(delayAmount*2));
  for (x=0; x<loopTime; x++) {        // the wave will be symetrical (same time high & low)
     digitalWrite(speakerPin,HIGH);   // Set the pin high
     delayMicroseconds(delayAmount);  // and make the tall part of the wave
     digitalWrite(speakerPin,LOW);    // switch the pin back to low
     delayMicroseconds(delayAmount);  // and make the bottom part of the wave
  }  
}

 
