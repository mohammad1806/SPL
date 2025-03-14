package bgu.spl.net.impl.tftp;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

public class WriteThread implements Runnable{

    private TftpProtoc protoc;
    private TftpEncDec encDec;
    private final Socket sock;
    private BufferedOutputStream out;

    public WriteThread(TftpProtoc protoc, TftpEncDec encDec, Socket sock) {
        this.protoc = protoc;
        this.encDec = encDec;
        this.sock = sock;
    }
    @Override
    public void run() {

        try {
            out = new BufferedOutputStream(sock.getOutputStream());
            Scanner scan = new Scanner(System.in);
            while ( !protoc.shouldTerminate() ) {
                String input = scan.nextLine();
                String myS = removeFirstSpaceSequence(input);
                byte[] to = encDec.encode(myS);
                if ( to != null) {
                    byte[] ret = protoc.process(to);
                    if (ret != null) {
                        out.write(to);
                        out.flush();
                        try {
                            synchronized (protoc) {
                                protoc.wait();
                            }
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
            System.out.println("writer thread closed");
            out.close();
            sock.close();
            } catch (IOException e) {}
    }
    public static String removeFirstSpaceSequence(String input) {
        StringBuilder output = new StringBuilder();
        int beg = 0;
        int end = 0;
        boolean bool = true;
        int i = 0;
        if( input.charAt(0) != ' '){
            beg++; end++;
        }
        while ( i < input.length() ){
            while ( i < input.length() && input.charAt(i) == ' ' && bool){
                if( beg == end ) {
                    beg++;
                }
                i++;
            }
            if(bool & (end + 1) == beg){end++;}
            if(beg == 2){ bool = false;}
            if( i != input.length()) {
                output.append(input.charAt(i));
            }
            i++;
        }
        return output.toString();
    }
}

