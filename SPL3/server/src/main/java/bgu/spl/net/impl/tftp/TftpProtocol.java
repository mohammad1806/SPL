package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.ConnectionsImpl;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.FileOutputStream;
import java.util.Map;

public class TftpProtocol implements BidiMessagingProtocol<byte[]> {
    private static final String serverFilesFolder = "Files"; // Set your server files folder
    private boolean shouldTerminate = false;
    private ConnectionsImpl<byte[]> connections;
    private String userName = null;
    private int clientId;
    private short blockNumber = 0;
    private byte[] bytesToSend = null;
    private File fileToWriteTo = null;
    private short lastOpc = 0;
    private boolean sent = false;
    @Override
    public void start(int connectionId, ConnectionsImpl<byte[]> connections) {
        this.connections = connections;
        this.clientId = connectionId;

    }

    @Override
    public void process(byte[] message) {

        short opcode = getOpcode(message);
        if (opcode == 1) {
            lastOpc = 1;
            handleReadRequest(message);
        } else if (opcode == 2) {
            lastOpc = 2;
            handleWriteRequest(message);
        } else if (opcode == 3) {
            lastOpc = 3;
            handleDataPacket(message);
        } else if (opcode == 4) {
            lastOpc = 4;
            handleAcknowledgment(message);
        }  else if (opcode == 6) {
            lastOpc = 6;
            handleDirectoryListingRequest();
        } else if (opcode == 7) {
            lastOpc = 7;
            handleLoginRequest(message);
        } else if (opcode == 8) {
            lastOpc = 8;
            handleDeleteRequest(message);
        } else if (opcode == 10) {
            lastOpc = 9;
            handleDisconnect(message);
        }
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    private short getOpcode(byte[] message) {
        return (short) ((message[0] & 0x00ff) << 8 | (message[1] & 0x00ff));
    }

    private void handleReadRequest(byte[] message) {

        String fileName = readNameToString(message);
        File file = new File(serverFilesFolder, fileName);
        if (!file.exists()) {
            sendError((short) 1, "File not found - File doesnt exist in the server!");
        } else if (userName == null){
            sendError((short) 6,"User not logged in - Any opcode received before Login completes.");
        }else{
            try {
                Path p = Paths.get(file.getPath());
                bytesToSend = Files.readAllBytes(p);
                System.out.println("sending file...");
                sent = false;
                sendDataPocket();

            } catch (IOException e) {
            }
        }
    }
    private void handleWriteRequest(byte[] message) {

        String fileName = readNameToString(message);
        fileToWriteTo = new File(serverFilesFolder,fileName);
        if ( fileToWriteTo.exists() ) {
            sendError((short) 5,"File already exists - File exist on WRQ.");
        } else if (userName == null){
            sendError((short) 6,"User not logged in - Any opcode received before Login completes.");
        }else{
            try {
                System.out.println("Reading file...");
                fileToWriteTo.createNewFile();
                sendACK(message);
                sendBCAST((byte) 1,fileToWriteTo.getName());

            } catch (IOException e) {
            }
        }
    }

    private void handleDataPacket(byte[] message) {

        try( FileOutputStream fis = new FileOutputStream(fileToWriteTo,true)){
            short size = (short) ((message[2] & 0x00ff) << 8 | (message[3] & 0x00ff));
            fis.write(Arrays.copyOfRange(message,6,size + 6));
            fis.close();
            sendACK(message);
            if( message.length < (6 + 512)){
                System.out.println("File uploaded successfully!");
            }
        }catch (IOException ex) {}
    }

    private void handleAcknowledgment(byte[] message) {
        if ( !sent ) {
            sendDataPocket();
        } else if ( lastOpc == 1 ) {
            System.out.println("File sent!");
            lastOpc = 0;
            bytesToSend = null;
        } else {
            System.out.println("Directory sent!");
            lastOpc = 0;
            bytesToSend = null;
        }
    }

    private void handleDirectoryListingRequest() {

        if (userName == null){
            sendError((short) 6, "User not logged in - Any opcode received before Login completes.");
            return;
        }
        ArrayList<Byte> filesNames = new ArrayList<>();
        File directory = new File(serverFilesFolder);
        // Check if the directory exists
        if (directory.exists() && directory.isDirectory()) {
            // Get all files in the directory
            File[] files = directory.listFiles();
            int i = 0;
            // Iterate over the files and print their names
            if (files != null) {
                for (File file : files) {
                    byte[] dis = new byte[file.getName().length()];
                    byte[] src = file.getName().getBytes(StandardCharsets.UTF_8);
                    System.arraycopy(src, 0, dis, 0, file.getName().length());
                    for( int j = 0; j < dis.length; j++){
                        filesNames.add(i+j,dis[j]);
                    }
                    filesNames.add(i + file.getName().length(),(byte)0);
                    i += file.getName().length() + 1;
                }

                bytesToSend = new byte[i - 1];
                byte[] src2 = converToByteArray(filesNames);
                System.arraycopy(src2, 0, bytesToSend, 0, i - 1);
                sendDataPocket();
            }
        } else {
            System.out.println("directory not exist!");
        }
    }

    private void handleLoginRequest(byte[] message) {

        String name = readNameToString(message);
        if (userName != null){
            sendError((short) 7,"User already logged in - Login username already connected.");
            return;
        } else if ( connections.isExistUserName(name) ){
            sendError((short) 0,"login fail - UserName already exist!");
            return;
        }
        sendACK(message);
        System.out.println("ACK sent");
        userName = name;
        connections.addUser(clientId,userName);
        System.out.println(userName +" logged in successfully!");
    }

    private void handleDeleteRequest(byte[] message) {

        String fileToDelete = readNameToString(message);
        File file = new File(serverFilesFolder,fileToDelete);
        if( !file.exists() ){
            sendError((short) 1,"File not found - File doesnt exist in the server!");
            return;
        }else if (userName == null){
            sendError((short) 6,"User not logged in - Any opcode received before Login completes.");
            return;
        }
        if( file.delete() ) {
            sendACK(message);
            System.out.println("File : " + fileToDelete + " deleted successfully.");
            sendBCAST((byte) 0,fileToDelete);
        }else {
            System.out.println("Error deleting file: " + fileToDelete);
        }
    }

    private void handleDisconnect(byte[] msg) {

        if ( userName == null ){
            sendError((short) 6,"User not logged in - Any opcode received before Login completes.");
            return;
        }
        sendACK(msg);
        System.out.println(userName + " logged out!");
        connections.disconnect(clientId);
        userName = null;
    }

    public void sendBCAST(byte delORad, String fileName){
        byte[] res = new byte[fileName.length() + 4];
        res[0] = (byte) 0;
        res[1] = (byte) 9;
        res[2] = delORad;
        System.arraycopy(fileName.getBytes(StandardCharsets.UTF_8),0,res,3,fileName.length());
        res[res.length - 1] = (byte) 0;
        for(Integer i : connections.getUsers().keySet()){
                connections.send(i,res);
        }

    }
    public String readNameToString(byte[] msg){

        byte[] toString = new byte[msg.length - 3];
        System.arraycopy(msg,2,toString,0,toString.length);
        return new String(toString, StandardCharsets.UTF_8);

    }

    public void sendError(short i, String errMsg){
        byte[] toSend = new byte[errMsg.length() + 5];
        toSend[0] = 0;
        toSend[1] = 5;
        toSend[2] = (byte) (i >> 8);
        toSend[3] = (byte) ((i) & 0x00ff);
        System.arraycopy(errMsg.getBytes(StandardCharsets.UTF_8),0,toSend,4,errMsg.length());
        toSend[toSend.length - 1] = 0;
        connections.send(clientId,toSend);
    }

    public void sendDataPocket(){

        byte[] res ;
        if ( bytesToSend.length >= 512) {
            res = new byte[6 + 512];
            System.arraycopy(bytesToSend, 0, res, 6, 512);
            bytesToSend = reduce(bytesToSend, true);
            blockNumber++;
            res[0] = 0;
            res[1] = 3;
            res[2] = (byte) (((short) 512) >> 8);
            res[3] = (byte) (((short) 512) & 0x00ff);
            res[4] = (byte) (blockNumber >> 8);
            res[5] = (byte) (blockNumber & 0x00ff);

        } else {
            res = reduce(bytesToSend,false);
            blockNumber++;
            res[0] = 0;
            res[1] = 3;
            res[2] = (byte) (((short) bytesToSend.length) >> 8);
            res[3] = (byte) (((short) bytesToSend.length) & 0x00ff);
            res[4] = (byte) (blockNumber >> 8);
            res[5] = (byte) (blockNumber & 0x00ff);
            blockNumber = 0;
            bytesToSend = null;
            sent = true;
        }
        connections.send(clientId,res);
    }
    public void sendACK(byte[] msg){

        byte[] res = new byte[4];
        res[0] = (byte) (0);
        res[1] = (byte) (4);
        if(lastOpc == 3){
            res[2] = msg[4];
            res[3] = msg[5];
        }else {
            res[2] = (byte) (0);
            res[3] = (byte) (0);
        }
        connections.send((Integer)clientId,res);

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

    public byte[] converToByteArray(ArrayList<Byte> ls){

        byte[] ret = new byte[ls.size()];
        for( int i = 0; i < ret.length; i++){
            ret[i] = ls.get(i);
        }
        return ret;
    }
}