package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {

    private List<Byte> byteList = new ArrayList<>();

    @Override
    public byte[] decodeNextByte(byte nextByte) {

        byteList.add(nextByte);
        short opCode;
        if (byteList.size() >= 2) {
            opCode = extractShortFromList(byteList);
            if (opCode == 6 || opCode == 10) {
                return arrayListToByteArray(byteList);
            }
            if (byteList.size() >= 4) {
                if (opCode == 4) {
                    return arrayListToByteArray(byteList);
                } else if (opCode == 3) {
                    short dataPackSize = extractShortFromList(byteList, 2);
                    if (byteList.size() == dataPackSize + 6) {
                        return arrayListToByteArray(byteList);
                    }
                }else if ( nextByte == '\0'){
                    return arrayListToByteArray(byteList);
                }
            }


        }
        return null; // Continue reading the message
    }

    @Override
    public byte[] encode(byte[] message) {
        return message;
    }

    // Helper methods for conversion
    private short extractShortFromList(List<Byte> list) {
        return (short) (((short) list.get(0)) << 8 | (short) (list.get(1)) & 0x00ff);
    }

    private short extractShortFromList(List<Byte> list, int startIndex) {
        return (short) (((short) list.get(startIndex)) << 8 | (short) (list.get(startIndex + 1)) & 0x00ff);
    }

    private byte[] arrayListToByteArray(List<Byte> list) {
        byte[] byteArray = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            byteArray[i] = list.get(i);
        }
        list.clear();
        return byteArray;
    }

}