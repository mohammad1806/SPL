package bgu.spl.net.srv;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;


public class ConnectionsImpl <T> implements Connections<T> {
    ConcurrentHashMap<Integer, ConnectionHandler<T>> idconnect;
    ConcurrentHashMap<Integer, String> users;
    public ConnectionsImpl(){
        this.idconnect = new ConcurrentHashMap<>();
        users = new ConcurrentHashMap<>();
    }

    @Override
    public void connect(int connectionId, BlockingConnectionHandler<T> handler) {
        idconnect.putIfAbsent(connectionId,handler);
    }
    public void addUser(int id, String name){
        users.putIfAbsent(id,name);
    }

    public boolean isExistUserName(String name){
        return users.contains(name);
    }
    @Override
    public boolean send(int connectionId, T msg) {

        if(idconnect.containsKey(connectionId)){
            idconnect.get(connectionId).send(msg);
            return true;
        }
        return false;
    }
    @Override
    public void disconnect(int connectionId) {
        if(idconnect.contains(connectionId)){
            try {
                idconnect.get(connectionId).close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        idconnect.remove(connectionId);
        users.remove(connectionId);
    }

    public ConcurrentHashMap<Integer, String> getUsers(){
        return users;
    }
    public ConcurrentHashMap<Integer, ConnectionHandler<T>> getIdconnect(){
        return idconnect;
    }

}