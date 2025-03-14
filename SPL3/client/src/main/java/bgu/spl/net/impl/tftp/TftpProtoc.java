package bgu.spl.net.impl.tftp;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TftpProtoc {

    private volatile boolean shouldTerminate = false;

    private String name = null;
    private File fileToWriteToSrv = null;
    private File fileToReadFromSrv = null;
    private byte[] byteToSend = null;
    private short lastOpc = 0;
    private short blockNumber = 1;
    private ArrayList<Byte> filesNames = new ArrayList<Byte>();
    private final String currentDirectory = System.getProperty("user.dr");
    public byte[] process(byte[] msg) {

        short opCode = (short) (((msg[0] & 0x00ff) << 8) | (msg[1] & 0x00ff));
        if( opCode == 1 ) {
            //RRQ
            String fileNameToRead = byteArrayToString(Arrays.copyOfRange(msg, 2, msg.length - 1), "UTF-8");
            fileToReadFromSrv = new File(currentDirectory, fileNameToRead);
            if (fileToReadFromSrv.exists()) {
                System.out.println("File already exists - File name exists on WRQ.");
                return null;
            }
            try {
                System.out.println("start reading...");
                fileToReadFromSrv.createNewFile();
                lastOpc = opCode;
                return msg;
            } catch (IOException e) {
            }
        } else if ( opCode == 2 ) {
            //WRQ
            String fileNameToSend = byteArrayToString(Arrays.copyOfRange(msg, 2, msg.length - 1), "UTF-8");
            fileToWriteToSrv = new File(currentDirectory, fileNameToSend);
            if (!fileToWriteToSrv.exists()) {
                System.out.println("File not found!");
                return null;
            }
            try {
                Path p = Paths.get(fileToWriteToSrv.getPath());
                byteToSend = Files.readAllBytes(p);
            } catch (IOException e) {
            }
            lastOpc = opCode;
            return msg;
        } else if ( opCode == 3 ) {
            //DATA
            if (fileToReadFromSrv != null) {
                byte[] toRead = Arrays.copyOfRange(msg, 6, msg.length);
                try (FileOutputStream fis = new FileOutputStream(fileToReadFromSrv,true)) {
                    fis.write(toRead);
                    fis.close();
                    byte[] res = sendACK(msg);
                    if (toRead.length < 512) {
                        System.out.println("Downloading Done Successfully!");
                        notifyWriter();
                        lastOpc = 3;
                        fileToReadFromSrv = null;
                        blockNumber = 1;
                    }
                    return res;
                } catch (IOException ex) {
                }

            } else if (lastOpc == 6) {
                int i = 0;
                ArrayList<Byte> myls = new ArrayList<>();
                transferMsgToList(filesNames,msg);
                if ( msg.length < (6 + 512)) {
                    while (i <= filesNames.size()) {
                        if (i == filesNames.size() || filesNames.get(i) == 0) {
                            System.out.println(new String(arrayListToByteArray(myls), StandardCharsets.UTF_8));
                            myls.clear();
                        } else {
                            myls.add(filesNames.get(i));
                        }
                        i++;
                    }
                    filesNames.clear();
                }
                byte[] res = sendACK(msg);
                if (msg.length < 512) {
                    notifyWriter();
                    blockNumber = 1;
                }
                return res;
            }
        } else if ( opCode == 4 ) {
            //ACK
            if( lastOpc == 0){
                short num =  ( short ) (short)(((msg[2] & 0xff) << 8) | (msg[3] & 0xff));
                System.out.println("ACK received for block number: " + num );
                return responeToACK();
            }else if( lastOpc == 1 ){
                System.out.println("ACK received for RRQ!");
            }else if( lastOpc == 2 ){
                lastOpc = 0;
                System.out.println("ACK received for WRQ!\nStart sending data...");
                return responeToACK();
            }else if ( lastOpc == 7 ){
                System.out.println("ACK received for LOGRQ!");
            } else if ( lastOpc == 8 ) {
                System.out.println("ACK received for DELRQ!");
            } else if ( lastOpc == 10) {
                System.out.println("ACK received for DISC!");
                return responeToACK();
            }

        } else if ( opCode == 5 ) {
            //ERROR
            if ( fileToReadFromSrv != null ){
                fileToReadFromSrv.delete();
                fileToReadFromSrv = null;
            } else if ( fileToWriteToSrv != null ){
                fileToWriteToSrv = null;
            } else if ( lastOpc == 2 && byteToSend != null ){
                byteToSend = null;
            }
            notifyWriter();
            System.out.println(new String(Arrays.copyOfRange(msg, 4, msg.length - 1), StandardCharsets.UTF_8));
            return null;
        } else if ( opCode == 6 ) {
            //DIRQ
            lastOpc = 6;
            return msg;
        } else if ( opCode == 7 ) {
            //LOGRQ
            name = byteArrayToString(Arrays.copyOfRange(msg, 2, msg.length - 1), "UTF-8");
            lastOpc = opCode;
            return msg;
        } else if ( opCode == 8 ) {
            //DELRQ
            lastOpc = opCode;
            return msg;
        } else if ( opCode == 9 ) {
            //BCAST
            String fileName = new String(Arrays.copyOfRange(msg,3,msg.length-1));
            if ( msg[2] == 1 ){
                System.out.println("Notification: file - " + fileName + " added to the server!");
            }else {
                System.out.println("Notification: file - " + fileName + " deleted from the server!");
            }
            return null;
        } else if ( opCode == 10 ){
                //DISC
                lastOpc = opCode;
                return msg;
        }
        notifyWriter();
        return null;
    }

    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    public byte[] responeToACK(){

        if ( lastOpc == 0 ) {
            return sendDataPocket();
        } else if ( lastOpc == 10 ){
            shouldTerminate = true;
        }
        notifyWriter();
        return null;
    }

    public void notifyWriter(){
        synchronized (this) {
            this.notifyAll();
        }
    }
    private static String byteArrayToString(byte[] byteArray, String charsetName) {
        try {
            // Create a String from the byte array using the specified encoding
            return new String(byteArray, charsetName);
        } catch (Exception e) {
            return null; // Handle the exception as needed
        }
    }

    public byte[] sendDataPocket(){

        byte[] res ;
        if ( byteToSend == null ){
            return null;
        }
            if ( byteToSend.length >=  512) {
                res = new byte[6 + 512];
                System.arraycopy(byteToSend, 0, res, 6, 512);
                byteToSend = reduce(byteToSend, true);
                res[0] = 0;
                res[1] = 3;
                res[2] = (byte) (((short) 512) >> 8);
                res[3] = (byte) (((short) 512) & 0xff);
                res[4] = (byte) (blockNumber >> 8);
                res[5] = (byte) (blockNumber & 0xff);
                blockNumber++;
            } else {
                res = reduce(byteToSend,false);
                System.out.println("File sent!");
                res[0] = 0;
                res[1] = 3;
                res[2] = (byte) (((short) byteToSend.length) >> 8);
                res[3] = (byte) (((short) byteToSend.length) & 0xff);
                res[4] = (byte) ((blockNumber >> 8));
                res[5] = (byte) (blockNumber & 0x00ff);
                blockNumber = 1;
                byteToSend = null;
                fileToWriteToSrv = null;
                lastOpc = 0;
                notifyWriter();
            }
        return res;
    }

    public byte[] sendACK(byte[] msg){
        byte[] toRet = new byte[4];
        toRet[0] = 0;
        toRet[1] = 4;
        toRet[2] = msg[4];
        toRet[3] = msg[5];
        return toRet;
    }

    private static byte[] arrayListToByteArray(ArrayList<Byte> byteList) {
        byte[] byteArray = new byte[byteList.size()];
        for (int i = 0; i < byteList.size(); i++) {
            byteArray[i] = byteList.get(i);
        }
        return byteArray;
    }

    public byte[] reduce(byte[] bytes, boolean big){
        byte[] ret;
        if(big) {
            ret = new byte[bytes.length - 512];
            int j = 0;
            for (int i = 512; i < bytes.length; i++) {
                ret[j] = bytes[i];
                j++;
            }
        }else {
            ret = new byte[ 6 + bytes.length];
            for (int i = 0; i < bytes.length; i++){
                ret[i + 6] = bytes[i];
            }
        }
        return ret;
    }

    void transferMsgToList(List<Byte> ls, byte[] msg){

        for(int j = 6; j < msg.length; j++){
            ls.add(msg[j]);
        }
    }
}
