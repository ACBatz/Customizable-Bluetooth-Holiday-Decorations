import subprocess
import sys

class ServerConnection:
        client_sock=None
        client_info=None
        status=False
        def __init__(self,server_sock):
                self.server_sock = server_sock
        def stop(self):
                self._stop.set()
        def accept(self):
                #print "Waiting for Bluetooth connection..."
                self.client_sock,self.client_info=self.server_sock.accept()
                #print "Accepted request from: ",self.client_info
                if not self.client_sock==None:
                        self.send("1")
                        self.status=True
                        return True
                else:
                        return False
        def send(self,data):
                self.client_sock.send(data)
        def listen(self):
                #print "Listening..."
                try:
                        data=self.client_sock.recv(1024)
                        if len(data)==0:
                                closeSocket()
                                #print "Socket closed"
                        else:
                                #print "Received [%s]" % data
                                return data
                except IOError:
                        #print "IOError"
                        self.closeSocket()
                except KeyboardInterrupt:
                        #print "KeyboardInterrupt"
                        self.closeSocket()
        def closeSocket(self):
                self.client_sock.close()
                self.status=False
        def hasConnection(self):
                return self.status

