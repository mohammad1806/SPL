package bgu.spl.net.impl.tftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class ReadThread implements Runnable{

    private TftpProtoc protoc;
    private TftpEncDec encDec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;

    public ReadThread(TftpProtoc protoc, TftpEncDec encDec, Socket sock){
        this.protoc = protoc;
        this.encDec = encDec;
        this.sock = sock;

    }
    @Override
    public void run() {

        try {
            while (!protoc.shouldTerminate()) {
                int read;
                in = new BufferedInputStream(sock.getInputStream());
                out = new BufferedOutputStream(sock.getOutputStream());
                while (!protoc.shouldTerminate() && (read = in.read()) >= 0) {
                    byte[] nextMessage = encDec.decodeNextByte((byte) read);
                    if (nextMessage != null) {
                        byte[] response = protoc.process(nextMessage);
                        if (response != null) {
                            out.write(response);
                            out.flush();
                        }
                    }
                }
        }
            in.close();
            out.close();
            sock.close();
        }catch (IOException e) {
        }
    }
}
