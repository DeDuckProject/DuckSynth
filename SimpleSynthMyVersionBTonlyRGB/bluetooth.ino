//#define BASIC_MSGS
//#define DEBUG
#define READ_BYTES
#define ONLY_BLUETOOTH_CONTROL  1
//#include <avr/io.h>

int ledPin = LED_PIN;
//int state = 0;
int flag = 0;
String stringValue;
bool bluetoothControl = true;
bool bluetoothConnection = false;
int knobValue[NUM_OF_KNOBS];


void initBT() {
#ifdef DEBUG
  //pinMode(LED_PIN, OUTPUT);
  digitalWrite(ledPin, LOW);
#endif //DEBUG
  int i=0;
  Serial.begin(9600); // Default connection rate for my BT module
  for (i=0;i<NUM_OF_KNOBS;i++){
   knobValue[i]=512; 
  }
}


bool getCommand(){
  while (Serial.available() > 0) {  
    bluetoothConnection = true;
#ifdef READ_BYTES
    char c = Serial.read();
    
    if (c=='\n'){
      parseCommand(stringValue);
      stringValue=""; 
    }else{
      stringValue+=c; 
    }
#else
    stringValue = Serial.readString();
    parseCommand(stringValue);
#endif //READ_BYTES
  }
/*  else{
    bluetoothConnection = false;
  }
  */
#ifndef ONLY_BLUETOOTH_CONTROL
  return shouldReadValuesFromBT();
#else
  return true;
#endif
}
//command format "knob 3=1023"
void parseCommand(String com){
  String part1,part2,part3;
  int knob,value;

  part1 = com.substring(0,com.indexOf(" "));  //"command word"
  part2 = com.substring(com.indexOf(" ")+1,com.indexOf("="));  //number
  part3 = com.substring(com.indexOf("=")+1);

  if (part1.equalsIgnoreCase("k")){
    knob = part2.toInt();
    value = part3.toInt();
    knobValue[knob] = value;
  }
  
  
  
#ifdef DEBUG
#if 0
  if (knob==0){
   digitalWrite(ledPin, LOW); 
  }else if (knob==1){
    digitalWrite(ledPin, HIGH); 
  }
#else
  if ((value>512) && (knob==3))
    digitalWrite(ledPin, HIGH);
  else
    digitalWrite(ledPin, LOW); 
#endif
#endif //DEBUG
}

bool shouldReadValuesFromBT(){
 return (bluetoothConnection &&  bluetoothControl);
}

int btRead(int knobNum){
  //divide by 4 because received value is 0-1023 and we need 0-255 for analogWrite...
 return map(knobValue[knobNum],0,1023,0,255);
}

