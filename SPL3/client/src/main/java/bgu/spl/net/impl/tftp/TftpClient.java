package bgu.spl.net.impl.tftp;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

public class TftpClient {
    //TODO: implement the main logic of the client, when using a thread per client the main logic goes here
    public static void main(String[] args) throws IOException {
        try (Socket sock = new Socket(args[0], Integer.parseInt(args[1]))) {
            TftpProtoc proc=new TftpProtoc();
            TftpEncDec enc=new TftpEncDec();
            Thread reader = new Thread(new ReadThread(proc,enc, sock));
            reader.start();
            WriteThread write =new WriteThread(proc,enc,sock);
            write.run();
        }
    }
}
