const int sampleWindow = 5; // Sample window width in mS (5 mS = 200Hz)
unsigned int sample;        // Value read from sound sensor

void setup() {
  // Setup serial port
Serial.begin(9600);
}

void loop() {
  // Continually send sound amplitude sample
  Serial.println(getSample());
}

int getSample() {
  unsigned long startMillis= millis();  // Start of sample window
  unsigned int peakToPeak = 0;   // peak-to-peak level
  
  unsigned int signalMax = 0;
  unsigned int signalMin = 1024;
  
  while (millis() - startMillis < sampleWindow) {
    sample = analogRead(0);
    if (sample < 1024)  // toss out spurious readings {
         if (sample > signalMax) {
            signalMax = sample;  // save just the max levels
         }
         else if (sample < signalMin) {
            signalMin = sample;  // save just the min levels
         }
      }
   }
   peakToPeak = signalMax - signalMin;  // max - min = peak-peak amplitude
 
   return peakToPeak;
}
