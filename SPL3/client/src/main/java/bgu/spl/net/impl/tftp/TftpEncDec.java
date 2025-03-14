package bgu.spl.net.impl.tftp;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TftpEncDec {

    private List<Byte> message;
    public TftpEncDec(){
        this.message = new ArrayList<Byte>();
    }
    public byte[] decodeNextByte(byte nextByte) {

        message.add(nextByte);
        int len = message.size();
        if ( len >= 2 ){
            short result = (short) (((message.get(0) & 0x00ff) << 8) | (message.get(1) & 0x00ff));
            if ( result == 4 && len == 4){
                return convertToArray(message);
            }
            else if ( result == 9 && len > 3 && nextByte == 0 ){
                return convertToArray(message);
            }
            else if ( result == 5 && len > 4 && nextByte == 0){
                return convertToArray(message);
            }
            else if ( result == 3 && len >= 4 ){
                short blockSize = (short) (((message.get(2) & 0x00ff) << 8) | (message.get(3) & 0x00ff));
                if( len == blockSize + 6 ) {
                    return convertToArray(message);
                }
            }
        }
        return null;
    }


    public byte[] encode(String message) {

        byte[] toSend;
        short opCode;
        int i;
        if (( i = message.indexOf("RRQ")) >= 0 && islegal(message,i,1)) {
            opCode = 1;
            return makeReady(message.substring(i + 3).getBytes(StandardCharsets.UTF_8), opCode);
        } else if (( i = message.indexOf("WRQ")) >= 0 && islegal(message,i,2)) {
            opCode = 2;
            return makeReady(message.substring(i + 3).getBytes(StandardCharsets.UTF_8), opCode);
        } else if (( i = message.indexOf("DIRQ")) >= 0 && islegal(message,i,6)) {
            opCode = 6;
            toSend = new byte[2]; toSend[0] = (byte) ((opCode >> 8)); toSend[1] = (byte) (opCode & 0x00ff);
            return toSend;
        } else if (( i = message.indexOf("LOGRQ")) >= 0 && islegal(message,i,7)) {
            opCode = 7;
            return makeReady(message.substring(i + 5).getBytes(StandardCharsets.UTF_8), opCode);
        } else if (( i = message.indexOf("DELRQ")) >= 0 && islegal(message,i,8)) {
            opCode = 8;
            return makeReady(message.substring(i + 5).getBytes(StandardCharsets.UTF_8), opCode);
        } else if (( i = message.indexOf("DISC")) >= 0 && islegal(message,i,10)) {
            opCode = 10;
            toSend = new byte[2]; toSend[0] = (byte) (opCode >> 8); toSend[1] = (byte) (opCode & 0x00ff);
            return toSend;
        } else {
            System.out.println("Illegal TFTP operation - Unknown Opcode.");
            return null;
        }
    }

    private byte[] makeReady(byte[] userOrFileName, short value) {
        // Use bitwise operations to convert short to byte and store it in the array
        byte[] byteArray = new byte[userOrFileName.length + 3];
        byteArray[0] = (byte) (value >> 8);
        byteArray[1] = (byte) (value & 0x00ff);
        int size = 2;
        for (int i = 0; i < userOrFileName.length; i++){
            byteArray[i + 2] = userOrFileName[i];
            size++;
        }
        byteArray[size] = 0;
        return byteArray;
    }

    public byte[] convertToArray(List<Byte> ls){

        byte[] ret = new byte[message.size()];
        for( int i = 0; i < ls.size(); i++){
            ret[i] = ls.get(i);
        }
        message.clear();
        return ret;
    }

    public boolean islegal(String msg, int i, int opc){
        if( ((opc == 1 || opc == 2) && msg.length() == (i + 3)) || ((opc == 7 || opc == 8) && msg.length() == (i + 5))){
            return false;
        }
        for( int j = 0; j < i; j++){
            if( i != 0 && msg.charAt(j) != ' '){
                return false;
            }
        }
        return true;
    }
}
