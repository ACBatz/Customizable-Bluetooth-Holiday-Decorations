###############################
##	Author: Andrew Batzel
##	Date:	2016-02-29
###

import time
import threading
import subprocess
from bluetooth import *
import bt_discovery
from bt_discovery import *
import discovery_btn
from discovery_btn import *
from ServerConnection import ServerConnection
from led import Led

###############################
##	switch serves as a means of creating a typical switch function found in various other programming languages
###
class switch(object):
        def __init__(self,value):
                self.value=value
                self.fall=False
        def __iter__(self):
                yield self.match
                raise StopIteration
        def match(self, *args):
                if self.fall or not args:
                        return True
                elif self.value in args:
                        self.fall=True
                        return True
                else:
                        return False					

###############################
##	DecorationsMain manages the entirety of the Decorations device, it sets up the Bluetooth capability,
##	creates objects from the other supporting classes, and calls their methods in accordance with what state
##	the device should be in
###
class DecorationsMain:
        def __init__(self,server_sock=None,port=None,uuid=None):
                self.server_sock=server_sock
                self.port=port
                self.uuid=uuid
        def setup(self):
                try:
                        btResult=subprocess.call(["sudo","service","bluetooth","start"])
                        if btResult < 0:
                                print >>sys.stderr, "Child terminated by signal", -btResult
                        else:
                                print >>sys.stderr, "Child returned", btResult
                        piscan=subprocess.call(["sudo","hciconfig","hci0","piscan"])
                        if piscan < 0:
                                print >>sys.stderr, "Child terminated by signal", -piscan
                        else:
                                print >>sys.stderr, "Child returned", piscan
                except OSError as e:
                        print >>sys.stderr, "Execution failed", e

                self.server_sock=BluetoothSocket(RFCOMM)
                self.server_sock.bind(("",PORT_ANY))
                self.server_sock.listen(1)
                self.port=self.server_sock.getsockname()[1]
                self.uuid="00001101-0000-1000-8000-00805f9b34fb"
                advertise_service(self.server_sock,"decorations",service_id=self.uuid,service_classes=[self.uuid,SERIAL_PORT_CLASS],profiles=[SERIAL_PORT_PROFILE])
                led=Led(0,"leds",0)
                server=ServerConnection(self.server_sock)
                led.start()
                time.sleep(8)
				 if server.accept() == True:
                        led.stop()
                        for i in range(5):
                                led.connect()
                led.starFade()
                state=1
                while True:
                        for case in switch(state):
                                if case(0):
                                        if server.accept()==True:
                                                state=1
                                                break
                                        else:
                                                state=0
                                                break
                                if case(1):
					if server.hasConnection()==False:
                                                state=0
                                                break
                                        else:
                                                data=server.listen()
                                                led.stop()
                                                led=Led(0,"leds",1,data)
                                                led.start()
                                                state=2
                                                break
                                if case(2):
                                        if server.hasConnection()==False:
                                                state=0
                                                break
                                        else:
						state=1
                                                break

