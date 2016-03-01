###############################
##	Author: Andrew Batzel
##	Date:	2016-02-29
###

import threading
import time
import sys
from neopixel import *
import serial

###############################
##	Led does all of the heavy lifting associated with programming the LED colors and receiving data over the serial bus for audio responsiveness
###
class Led(threading.Thread):
        def __init__(self,threadId,name,state=0,data=None,star=Color(255,255,0),ornament=Color(0,0,255),color_1=Color(0,255,0),color_2=Color(0,255,0),color_3=Color(0,255,0),color_4=Color(0,255,0),color_5=Color(0,255,0),strip=Adafruit_NeoPixel(50,18,800000,5,False,255)):
                threading.Thread.__init__(self)
                self._stop=threading.Event()
                self.threadId=threadId
                self.name=name
                self.state=state
                self.data=data
                self.star=star
                self.ornament=ornament
                self.color_1=color_1
                self.color_2=color_2
                self.color_3=color_3
                self.color_4=color_4
                self.color_5=color_5
                self.strip=strip
                self.cR=1.0
                self.cG=0.0
                self.cB=0.0
                self.col=0
                self.phaseLength = 30
                self.period = 180
                self.iteration = 0
                self.ser=serial.Serial('/dev/ttyACM0', 9600)
        def stop(self):
                self._stop.set()
        def reset(self):
                self._stop.clear()
        def stopped(self):
                return self._stop.isSet()
        def exit(self):
                self.exit()
        def startup(self):
                for i in range(self.strip.numPixels()):
                        self.strip.setPixelColor(i, Color(255,255,255))
                        self.strip.show()
                        time.sleep(.1)
                for i in range(self.strip.numPixels()):
                        self.strip.setPixelColor(self.strip.numPixels()-i, Color(0,0,0))
                        self.strip.show()
                        time.sleep(.05)

        def standby(self):
                while not self.stopped():
                        for i in range(5):
                                self.strip.setPixelColor(i, Color(0,0,255))
                        self.strip.show()
                        time.sleep(1)
                        for i in range(5):
                                self.strip.setPixelColor(i, Color(0,0,0))
                        self.strip.show()
                        time.sleep(1)
	def connect(self):
                for i in range(5):
                        self.strip.setPixelColor(i, Color(0,0,255))
                self.strip.show()
                time.sleep(.25)
                for i in range(5):
                        self.strip.setPixelColor(i , Color(0,0,0))
                self.strip.show()
                time.sleep(.25)
        def starFade(self):
                for i in range(255):
                        for j in range(5):
                                self.strip.setPixelColor(j, Color(i, i, 0))
                                self.strip.show()
                        time.sleep(.0001)
        def turnOn(self):
                for i in range(self.strip.numPixels()):
                        if i < 5:
                                self.strip.setPixelColor(i,self.star)
                        elif i % 6 == 0:
                                self.strip.setPixelColor(i,self.ornament)
                        else:
                                self.strip.setPixelColor(i,self.color_1)
                self.strip.show()
        def turnOff(self):
                for i in range(self.strip.numPixels()):
                        self.strip.setPixelColor(i,Color(0,0,0))
                self.strip.show()

	def chase(self,color_1=None,color_2=None,color_3=None,color_4=None,color_5=None):
                if color_1==None:
                        color_1=Color(0,255,0)
                        color_2=color_1
                        color_3=color_1
                        color_4=color_1
                        color_5=color_1
                if color_2==None:
                        color_2=color_1
                        color_3=color_1
                        color_4=color_1
                        color_5=color_1
                if color_3==None:
                        color_3=color_1
                        color_4=color_2
                        color_5=color_1
                if color_4==None:
                        color_4=color_1
                        color_5=color_2
                if color_5==None:
                        color_5=color_1
                while not self.stopped():
                        for i in range(0,self.strip.numPixels()+6,1):
                                if i > 4:
                                        if not i % 6 == 0:
                                                self.strip.setPixelColor(i,color_1)
                                                tmp=color_1
                                                color_1=color_2
                                                color_2=color_3
                                                color_3=color_4
                                                color_4=color_5
                                                color_5=tmp
                                if i > 10:
                                        if not (i-6)%6==0:
                                                self.strip.setPixelColor(i-6,Color(0,0,0))
                                self.strip.show()
                                if not i%6==0:
                                        time.sleep(.1)
                                if self.stopped():
                                        break;
                        for i in range(self.strip.numPixels(),-2,-1):
                                if i > 4:
                                        if not i % 6 == 0:
                                                self.strip.setPixelColor(i,color_1)
                                                tmp=color_1
                                                color_1=color_2
                                                color_2=color_3
                                                color_3=color_4
                                                color_4=color_5
                                                color_5=tmp

                                if i < 45:
                                        if not (i+6)%6==0:
                                                if not i+6 < 5:
                                                        self.strip.setPixelColor(i+6,Color(0,0,0))
                                self.strip.show()
                                if not i%6==0:
                                        time.sleep(.1)
                                if self.stopped():
                                        break
        def configure(self,data):
                seq=None
                star_color=None
                ornament_color=None
                color_1=None
                color_2=None
                color_3=None
                color_4=None
                color_5=None
                if len(data)>0:
                        seq=int(float(data[0]))
                        if len(data)>5:
                                star_color=int(data[1:7],16)
                                print star_color
                                if len(data)>11:
                                        ornament_color=int(data[7:13],16)
                                        print ornament_color
                                        if len(data)>17:
                                                color_1=int(data[13:19],16)
                                                if len(data)>23:
                                                        color_2=int(data[19:25],16)
                                                        if len(data)>29:
                                                                color_3=int(data[25:31],16)
                                                                if len(data)>35:
                                                                        color_4=int(data[31:37],16)
                                                                        if len(data)>41:
                                                                                color_5=int(data[37:43],16)
                if not star_color==None:
                        self.setStar(star_color)
                if not ornament_color==None:
                        self.setOrnaments(ornament_color)
                if seq==0:
                        self.turnOff()
                elif seq==1:
                        self.turnOn()
                elif color_1==None:
                        pass
                elif color_2==None:
                        if seq==2 or seq==3:
                                self.setOneColor(color_1)
                        else:
                                self.chase(color_1)
                elif color_3==None:
                        if seq==2:
                                self.setTwoColors(color_1,color_2)
                        elif seq==3:
                                while not self.stopped():
                                        self.setTwoColors(color_1,color_2)
                                        time.sleep(1)
                                        self.setTwoColors(color_2,color_1)
                                        time.sleep(1)
                        else:
                                self.chase(color_1,color_2)
                elif color_4==None:
                        if seq==2:
                                self.setThreeColors(color_1,color_2,color_3)
                        elif seq==3:
                                while not self.stopped():
                                        self.setThreeColors(color_1,color_2,color_3)
                                        time.sleep(1)
                                        self.setThreeColors(color_3,color_1,color_2)
                                        time.sleep(1)
                                        self.setThreeColors(color_2,color_3,color_1)
                                        time.sleep(1)
                        else:
                                self.chase(color_1,color_2,color_3)

                elif color_5==None:
                        if seq==2:
				self.setFourColors(color_1,color_2,color_3,color_4)
                        elif seq==3:
                                while not self.stopped():
                                        self.setFourColors(color_1,color_2,color_3,color_4)
                                        time.sleep(1)
                                        self.setFourColors(color_4,color_1,color_2,color_3)
                                        time.sleep(1)
                                        self.setFourColors(color_3,color_4,color_1,color_2)
                                        time.sleep(1)
                                        self.setFourColors(color_2,color_3,color_4,color_1)
                                        time.sleep(1)
                        else:
                                self.chase(color_1,color_2,color_3,color_4)
                else:
                        if seq==2:
                                self.setFiveColors(color_1,color_2,color_3,color_4,color_5)
                        elif seq==3:
                                while not self.stopped():
                                        self.setFiveColors(color_1,color_2,color_3,color_4,color_5)
                                        time.sleep(1)
                                        self.setFiveColors(color_5,color_1,color_2,color_3,color_4)
                                        time.sleep(1)
                                        self.setFiveColors(color_4,color_5,color_1,color_2,color_3)
                                        time.sleep(1)
                                        self.setFiveColors(color_3,color_4,color_5,color_1,color_2)
                                        time.sleep(1)
                                        self.setFiveColors(color_2,color_3,color_4,color_5,color_1)
                                        time.sleep(1)
                        else:
                                self.chase(color_1,color_2,color_3,color_4,color_5)
	def setStar(self,color):
                for i in range(5):
                        self.strip.setPixelColor(i,color)
                self.strip.show()
        def setOrnaments(self,color):
                for i in range(self.strip.numPixels()):
                        if i > 4:
                                if i % 6 == 0:
                                        self.strip.setPixelColor(i,color)
                self.strip.show()
        def setOneColor(self,color):
                for i in range(self.strip.numPixels()):
                        if i > 4:
                                if not i % 6 == 0:
                                        self.strip.setPixelColor(i,color)
                self.strip.show()
        def setTwoColors(self,color_1,color_2):
                for i in range(self.strip.numPixels()):
                        if i > 4:
                                if not i % 6 == 0:
                                        self.strip.setPixelColor(i,color_1)
                                        tmp=color_1
                                        color_1=color_2
                                        color_2=tmp
                self.strip.show()
        def setThreeColors(self,color_1,color_2,color_3):
                for i in range(self.strip.numPixels()):
                        if i > 4:
                                if not i % 6 == 0:
                                        self.strip.setPixelColor(i,color_1)
                                        tmp=color_1
                                        color_1=color_2
                                        color_2=color_3
                                        color_3=tmp
                self.strip.show()
        def setFourColors(self,color_1,color_2,color_3,color_4):
                for i in range(self.strip.numPixels()):
                        if i > 4:
                                if not i % 6 == 0:
                                        self.strip.setPixelColor(i,color_1)
                                        tmp=color_1
                                        color_1=color_2
                                        color_2=color_3
                                        color_3=color_4
                                        color_4=tmp
                self.strip.show()
        def setFiveColors(self,color_1,color_2,color_3,color_4,color_5):
                for i in range(self.strip.numPixels()):
                        if i > 4:
                                if not i % 6 == 0:
                                        self.strip.setPixelColor(i,color_1)
                                        tmp=color_1
                                        color_1=color_2
                                        color_2=color_3
                                        color_3=color_4
                                        color_4=color_5
                                        color_5=tmp
                self.strip.show()
				
	def reactive(self):
                while not self.stopped():
                        for k in range(10):
                                for i in range(self.strip.numPixels()-1, 6, -1):
                                        if not i % 6 == 0:
                                                if (i % 6) - 1 == 0:
                                                        self.strip.setPixelColor(i, self.strip.getPixelColor(i-2))
                                                else:
                                                        self.strip.setPixelColor(i, self.strip.getPixelColor(i-1))
                                self.strip.show()
                                try:
                                        self.ser.flushInput()
                                        time.sleep(.0001)
                                        micLevel = int(self.ser.readline())
                                except:
                                        micLevel = 0
                                if micLevel < 40:
                                        self.strip.setPixelColor(5, Color(0,0,0))
                                else:
                                        self.strip.setPixelColor(5, self.getColor(self.getVal(micLevel)))
                                        for i in range(5):
                                                self.strip.setPixelColor(i, Color(min(2 * int(self.getVal(micLevel) * 255),255), min(2 * int(self.getVal(micLevel) * 255),255), 0))
                                        for i in range(self.strip.numPixels()):
                                                if i > 4:
                                                        if i % 6 == 0:
                                                                self.strip.setPixelColor(i, Color(0, min(2 * int(self.getVal(micLevel) * 255), 255), min(2 * int(self.getVal(micLevel) * 255), 255)))
                                self.strip.show()
                                time.sleep(.001)
                                self.fadeRgb()
                        self.col = (self.col + 1) % 6
                        self.cycleRgb(self.col)
						
        def getVal(self, level):
                try:
                        val = float(level) / 1024.0
                except:
                        val = 0.0
                val = min(1.0, val)
                return val
				
        def getColor(self, val):
                if val > 1.0:
                        val = 1.0
                return Color(int(val * self.cR * 255), int(val * self.cG * 255), int(val * self.cB * 255))

	def fadeRgb(self):
                phase = self.iteration / self.phaseLength
                step = self.iteration % self.phaseLength
                if phase == 0:
                        self.cR = 1
                        self.cG = step / self.phaseLength
                        self.cB = 0
                elif phase == 1:
                        self.cR = (self.phaseLength - step) / self.phaseLength
                        self.cG = 1
                        self.cB = 0
                elif phase == 2:
                        self.cR = 0
                        self.cG = 1
                        self.cB = step / self.phaseLength
                elif phase == 3:
                        self.cR = 0
                        self.cG = (self.phaseLength - step) / self.phaseLength
                        self.cB = 1
                elif phase == 4:
                        self.cR = step / self.phaseLength
                        self.cG = 0
                        self.cB = 1
                else:
                        self.cR = 1
                        self.cG = 0
                        self.cB = (self.phaseLength - step) / self.phaseLength
                self.iteration = (self.iteration + 1) % self.period
		
		 def cycleRgb(self, col):
                if col == 0:
                        self.cR = 1
                        self.cG = 0
                        self.cB = 0
                elif col == 1:
                        self.cR = 0.5
                        self.cG = 0.5
                        self.cB = 0
                elif col == 2:
                        self.cR = 0
                        self.cG = 1
                        self.cB = 0
                elif col == 3:
                        self.cR = 0
                        self.cG = 0.5
                        self.cB = 0.5
                elif col == 4:
                        self.cR = 0
                        self.cG = 0
                        self.cB = 1
                else:
                        self.cR = 0.5
                        self.cG = 0
                        self.cB = 0.5
				
        def run(self):
                if self.state==0:
                        self.strip.begin()
                        self.startup()
                        self.standby()
                elif self.state==1:
                        self.configure(self.data)
                else:
                        print "bad state"
