"""Listener OSC minimo para verificar oscpad sin TouchDesigner.

Uso: python tools/osc_listen.py [puerto]
Imprime address y argumentos float32 de cada mensaje UDP recibido.
"""

import socket
import struct
import sys


def parse(data: bytes):
    if data.startswith(b"#bundle\x00"):
        off, out = 16, []
        while off + 4 <= len(data):
            size = struct.unpack(">i", data[off : off + 4])[0]
            off += 4
            out.extend(parse(data[off : off + size]))
            off += size
        return out
    end = data.index(b"\x00")
    address = data[:end].decode("ascii")
    tag_start = (end + 4) & ~3
    tag_end = data.index(b"\x00", tag_start)
    tags = data[tag_start + 1 : tag_end].decode("ascii")
    args, off = [], (tag_end + 4) & ~3
    for tag in tags:
        if tag == "f":
            args.append(struct.unpack(">f", data[off : off + 4])[0])
            off += 4
        elif tag == "i":
            args.append(struct.unpack(">i", data[off : off + 4])[0])
            off += 4
        elif tag == "s":
            stop = data.index(b"\x00", off)
            args.append(data[off:stop].decode("ascii"))
            off = (stop + 4) & ~3
    return [(address, args)]


def main():
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8000
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind(("0.0.0.0", port))
    print(f"escuchando UDP {port}")
    while True:
        data, peer = sock.recvfrom(65535)
        for address, args in parse(data):
            print(peer[0], address, args, flush=True)


if __name__ == "__main__":
    main()
