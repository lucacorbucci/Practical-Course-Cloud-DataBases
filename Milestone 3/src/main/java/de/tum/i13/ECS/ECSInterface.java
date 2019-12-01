package de.tum.i13.ECS;

import de.tum.i13.shared.Metadata;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.Remote;
import java.rmi.RemoteException;
/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public interface ECSInterface extends Remote {

    String shutdownServer(String[] request, String ip, ObjectInputStream ois, ObjectOutputStream oos) throws RemoteException;
    Metadata getMetadata();
    boolean isReady();
    boolean sendMetadata(ObjectOutputStream out, Metadata metadata);
    String computeHash(String ip, int port);
    boolean sendMetadataToAll(Metadata metadata, String hash);
}
