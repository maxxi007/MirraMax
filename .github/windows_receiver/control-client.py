import socket, json, sys

HOST='127.0.0.1'; PORT=5005

def send(msg):
    s=socket.socket(socket.AF_INET,socket.SOCK_STREAM)
    s.connect((HOST,PORT)); s.sendall((json.dumps(msg)+"\n").encode('utf-8')); s.close()

if __name__ == "__main__":
    if len(sys.argv)<2:
        print("Usage: python control-client.py tap x y | swipe x1 y1 x2 y2 duration | key keycode")
        sys.exit(1)
    cmd=sys.argv[1]
    if cmd=='tap':
        x=float(sys.argv[2]); y=float(sys.argv[3]); send({"type":"tap","x":x,"y":y})
    elif cmd=='swipe':
        x1=float(sys.argv[2]); y1=float(sys.argv[3]); x2=float(sys.argv[4]); y2=float(sys.argv[5]); dur=int(sys.argv[6]) if len(sys.argv)>6 else 200
        send({"type":"swipe","x1":x1,"y1":y1,"x2":x2,"y2":y2,"duration":dur})
    elif cmd=='key':
        kc=int(sys.argv[2]); send({"type":"key","keyCode":kc})
    else:
        print("Unknown")

