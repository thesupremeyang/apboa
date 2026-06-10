import socket
import sys

def test_connection(host, port):
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(5)
        result = sock.connect_ex((host, port))
        sock.close()
        return result == 0
    except Exception as e:
        return False

if __name__ == "__main__":
    host = "117.72.185.237"
    port = 3000
    if test_connection(host, port):
        print(f"成功连接到 {host}:{port}")
    else:
        print(f"无法连接到 {host}:{port}")