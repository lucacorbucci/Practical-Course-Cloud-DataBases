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

    /**
     * This function is used when a server wants to leave the network
     *
     * @param request the String[] with the full request received by the ECS
     * @param ip      ip of the server that wants to leave the network
     * @param ois     ObjectInputStream of the server that wants to leave the network
     * @param oos     ObjectOutputStream of the server that wants to leave the network
     * @return
     * @throws RemoteException
     */
    String shutdownServer(String[] request, String ip, ObjectInputStream ois, ObjectOutputStream oos) throws RemoteException;

    /**
     * Test function used in the tests to get the metadata from the ECS
     *
     * @return metadata from the ECS
     */
    Metadata getMetadata();

    /**
     * This function is used in the tests to check if the ECS is ready to receive requests
     *
     * @return true or false
     */
    boolean isReady();

    /**
     * This function is used to send metadata to a SINGLE server
     *
     * @param out      ObjectOutputStream o the server
     * @param metadata metadata that we want to send
     * @return true if we send metadata false otherwise
     */
    boolean sendMetadata(ObjectOutputStream out, Metadata metadata);

    /**
     * This function is used to send the data to all the servers that are in the network
     *
     * @param metadata metadata that we want to send
     * @param hash     hash of the server that don't need the new metadata
     * @return true if we send metadata false otherwise
     */
    boolean sendMetadataToAll(Metadata metadata, String hash);

    /**
     * This function is called when a new server call the ECS to join the network
     *
     * @param request  the String[] with the full request received by the ECS
     * @param remoteIP IP of the server that wants to join the network
     * @param remoteIn ObjectInputStream of the server that wants to join the network
     * @param out      ObjectOutputStream of the server that wants to join the network
     */
    void addNewServer(String[] request, String remoteIP, ObjectInputStream remoteIn, ObjectOutputStream out);
}
