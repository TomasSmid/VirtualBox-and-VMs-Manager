/*
 * Copyright 2015 Tomáš Šmíd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cz.muni.fi.virtualtoolmanager.logicimpl;

import cz.muni.fi.virtualtoolmanager.pubapi.io.OutputHandler;
import cz.muni.fi.virtualtoolmanager.pubapi.entities.PhysicalMachine;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.IncompatibleVirtToolAPIVersionException;
import cz.muni.fi.virtualtoolmanager.pubapi.managers.ConnectionManager;
import cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualizationToolManager;
import cz.muni.fi.virtualtoolmanager.pubapi.types.ClosingActionType;
import java.io.PrintStream;
import java.util.List;

/**
 * <div>
 * Class that provides the implementation of all methods from
 * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.ConnectionManager}.
 * </div>
 * 
 * @author Tomáš Šmíd
 */
public class ConnectionManagerImpl implements ConnectionManager{

    /**
     * <div>
     * Method that implements the method 
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.ConnectionManager#connectTo(PhysicalMachine)
     * ConnectionManager::connectTo(PhysicalMachine)}.
     * </div>
     * <div>
     * <p>
     * For each physical machine connection operation are used 3 attempts to
     * establish the connection. Between each unsuccessful attempt is set up
     * the waiting time to 2 seconds.
     * <p>
     * If there occurs any failure during the processing the connection operation,
     * there can be thrown one of the following exceptions:
     * <ul>
     * <li><strong>IllegalArgumentException - </strong>thrown when the given
     * physical machine is <code>null</code>
     * <li><strong>
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException
     * ConnectionFailureException} - </strong>thrown when there cannot be
     * the connection with the physical machine established and one of the following
     * problems occur:
     * <ol>
     * <li>Physical machine given as the input parameter of this method has some
     * key value (IP address, port number of VirtualBox web server, username and
     * user password) incorrect.
     * <li>VirtualBox web server is not running at the moment of the connection
     * establishment.
     * <li>Network connection is not working properly or at all.
     * </ol>
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.IncompatibleVirtToolAPIVersionException
     * IncompatibleVirtToolAPIVersionException} - </strong>thrown when the API
     * version of VirtualBox installation on the physical machine, which
     * there is being established the connection with, is incorrect (different
     * from the correct API version 4.3).
     * </ul> 
     * </div>
     * @param physicalMachine represents the physical machine which is going
     * to be connected
     * @return manager of type {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualizationToolManager
     * VirtualizationToolManager} for managing virtual machines on the newly
     * connected physical machine
     */
    @Override
    public VirtualizationToolManager connectTo(PhysicalMachine physicalMachine){
        return connectTo(physicalMachine,2000l);
    }
    
    /**
     * <div>
     * Method that implements the method
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.ConnectionManager#connectTo(PhysicalMachine, long)
     * ConnectionManager::connectTo(PhysicalMachine, long)}.
     * </div>
     * <div>
     * <p>
     * This method use the same implementation as the method {@link #connectTo(PhysicalMachine)}.
     * <p>
     * With this method is possible specified the wating time between the
     * unsuccessful connection establishment attempts. If the given time in
     * milliseconds is negative, then it indicates that there will be performed
     * just one attempt to establish the connection with a particular physical
     * machine.
     * </div>
     * @param physicalMachine represents the physical machine which is going to
     * be connected
     * @param millis time in milliseconds determining the waiting time between
     * each unsuccessful connection establishment attempt
     * @return manager of type {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualizationToolManager
     * VirtualizationToolManager} for managing virtual machines on the newly
     * connected physical machine
     */
    @Override
    public VirtualizationToolManager connectTo(PhysicalMachine physicalMachine, long millis){
        return connectTo(physicalMachine,millis,true);
    }
    
    /**
     * <div>
     * Method that implements the method
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.ConnectionManager#disconnectFrom(PhysicalMachine)
     * ConnectionManager::disconnectFrom(PhysicalMachine)}.
     * </div>
     * <div>
     * <p>
     * The given physical machine is just removed from the list of connected
     * physical machines.
     * <p>
     * If there occurs any failure during the processing the disconnection operation,
     * there can be thrown one of the following exceptions:
     * <ul>
     * <li><strong>IllegalArgumentException - </strong>thrown when the given
     * physical machine is <code>null</code>
     * <li><strong>
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException
     * ConnectionFailureException} - </strong>thrown when the given physical 
     * machine is not connected
     * </ul>
     * </div>
     * @param physicalMachine represents the physical machine which is going to
     * be disconnected
     */
    @Override
    public void disconnectFrom(PhysicalMachine physicalMachine){
        disconnectFrom(physicalMachine, ClosingActionType.NONE);
    }

    /**
     * <div>
     * Method that implements the method
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.ConnectionManager#disconnectFrom(PhysicalMachine, ClosingActionType)
     * ConnectionManager::disconnectFrom(PhysicalMachine, ClosingActionType)}.
     * </div>
     * <div>
     * <p>
     * If there occurs any failure during the processing the disconnection operation,
     * there can be thrown one of the following exceptions:
     * <ul>
     * <li><strong>IllegalArgumentException - </strong>thrown when the either given
     * physical machine or closing action type is <code>null</code>
     * <li><strong>
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException
     * ConnectionFailureException} - </strong>thrown when the disconnection
     * operation cannot be performed correctly because of the following problems:
     * <ol>
     * <li>the given physical machine is not connected,
     * <li>the given physical machine has incorrect key value (IP address,
     * poort number of VirtualBox web server, username or user password),
     * <li>remote VirtualBox web server is not running,
     * <li>network connection is not working properly or at all.
     * </ol>
     * <strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.IncompatibleVirtToolAPIVersionException
     * IncompatibleVirtToolAPIVersionException} - </strong>thrown when the API
     * version of VirtualBox installation on the physical machine, has been 
     * changed to incorrect version (different from the correct API version 4.3).
     * </ul>
     * </div>
     * @param physicalMachine represents the physical machine which is going to
     * be disconnected
     * @param closingAction specifies what action should be performed before
     * the physical machine is disconnected
     */
    @Override
    public void disconnectFrom(PhysicalMachine physicalMachine, 
            ClosingActionType closingAction) {
        OutputHandler outputHandler = new OutputHandler();
                
        if(physicalMachine == null){
            throw new IllegalArgumentException("A null physical machine used for "
                    + "the disconnection operation.");
        }
        
        if(closingAction == null){
            throw new IllegalArgumentException("A null closing action type "
                    + "used for the disconnection operation.");
        }
        
        //check if the physical machine is connected
        if(!isConnected(physicalMachine)){
            throw new ConnectionFailureException("Disconnection operation failure: "
                    + "Physical machine " + physicalMachine + " cannot be "
                    + "disconnected, because it is not connected.");
        }
        
        outputHandler.printMessage("Disconnecting from the physical machine " + physicalMachine);
        
        switch(closingAction){
            case NONE:{
                //just remove the physical machine from the list of connected PMs
                removePMFromListOfConnectedPMs(physicalMachine);                
                break;
            }
            case SHUT_DOWN_RUNNING_VM:{
                String errMsgBase = "Disconnection operation failure: Virtual "
                        + "machines could not be shut down";
                VirtualizationToolManager virtualizationToolManager = null;
                
                try{
                    //try the connection with the PM - if correct,then its running
                    //VMs can be shut down
                    virtualizationToolManager = connectTo(physicalMachine,-1,false);
                }catch(ConnectionFailureException ex){
                    //remove the PM from list of connected PMs
                    removePMFromListOfConnectedPMs(physicalMachine);
                    outputHandler.printMessage("Physical machine " + physicalMachine
                            + " was disconnected");
                    throw new ConnectionFailureException(errMsgBase + ", because "
                            + "there occured one of these problems: 1. Network "
                            + "connection is not working properly or at all / "
                            + "2. The VirtualBox web server is not running / "
                            + "3. One of the key value (IP address, number of "
                            + "web server port, username or user password) of "
                            + "the physical machine has been changed and it is "
                            + "incorrect now.");
                }catch(IncompatibleVirtToolAPIVersionException ex){
                    //remove the PM from list of connected PMs
                    removePMFromListOfConnectedPMs(physicalMachine);
                    outputHandler.printMessage("Physical machine " + physicalMachine
                            + " was disconnected");
                    throw new IncompatibleVirtToolAPIVersionException(errMsgBase
                            + ", because there has been changed an API version "
                            + "of VirtualBox and it is incorrect now.");
                }
                                
                try{
                    //shut down all running VMs on the physical machine
                    virtualizationToolManager.close();
                }catch(RuntimeException ex){
                    throw ex;
                }finally{
                    //always remove the PM from list of connected PMs
                    removePMFromListOfConnectedPMs(physicalMachine);
                }
                break;
            }
            default: throw new IllegalArgumentException("Illegal enumeration "
                    + "literal of type ClosingActionType occured while trying "
                    + "to disconnect from " + physicalMachine);
        }
        
        outputHandler.printMessage("Physical machine " + physicalMachine
                + " was disconnected");
    }

    /**
     * Method that implements the method
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.ConnectionManager#isConnected(PhysicalMachine)
     * ConnectionManager::isConnected(PhysicalMachine)}.
     * If there occurs any error during the method is being processed,
     * the following exceptions can be thrown:
     * <ul>
     * <li><strong>IllegalArgumentException - </strong>thrown when the given
     * physical machine is <code>null</code>.
     * </ul>
     * @param physicalMachine represents the queried physical machine
     * @return <code>true</code> if the physical machine is connected,
     * <code>false</code> otherwise
     */
    @Override
    public boolean isConnected(PhysicalMachine physicalMachine) {
        //check the physical machine is not null - null physical machine cannot 
        //be connected
        if(physicalMachine == null){
            throw new IllegalArgumentException("A null physical machine used "
                    + "for query operation of physical machine availability.");
        }
        
        ConnectedPhysicalMachines connectedPhysicalMachines =
                ConnectedPhysicalMachines.getInstance();        
        //finds out if the physical machine is connected (is placed in the list
        //of connected physical machines) and returns answer
        return connectedPhysicalMachines.isConnected(physicalMachine);
    }

    /**
     * <div>
     * Method implements the method
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.ConnectionManager#getConnectedPhysicalMachines()
     * ConnectionManager::getConnectedPhysicalMachines()}.
     * </div>
     * @return list of all connected physical machines
     */
    @Override
    public List<PhysicalMachine> getConnectedPhysicalMachines() {
        ConnectedPhysicalMachines connectedPhysicalMachines =
                ConnectedPhysicalMachines.getInstance();
        return connectedPhysicalMachines.getConnectedPhysicalMachines();
    }
    
    /**
     * <div>
     * Method that implements the method
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.ConnectionManager#close()
     * ConnectionManager::close()}.
     * </div>
     * <div>
     * In this method is called for each connected physical machine the
     * disconnection method {@link #disconnectFrom(PhysicalMachine, ClosingActionType)}
     * with closing action set up to shut down all running virtual machines on 
     * a particular physical machine.
     * </div>
     */
    @Override
    public void close() {
        while(!getConnectedPhysicalMachines().isEmpty()){
            
            disconnectFrom(getConnectedPhysicalMachines().get(0),
                    ClosingActionType.SHUT_DOWN_RUNNING_VM);
        }
    }
    
    private void setOutputStreams(PrintStream printStream){
        setOutputStreams(printStream, printStream);
    }
    
    private void setOutputStreams(PrintStream stdOutput, PrintStream stdErrOutput){
        OutputHandler.setOutputStream(stdOutput);
        OutputHandler.setErrorOutputStream(stdErrOutput);        
    }
    
    /**
     * Method represents the implementation for methods
     * {@link #connectTo(PhysicalMachine)} and {@link #connectTo(PhysicalMachine, long)}.
     * @param physicalMachine represents the physical machine which is going to
     * be connected
     * @param millis waiting time in milliseconds between each unsuccessful
     * connection establishment attempt
     * @param doStandardConnection if <code>true</code> then is performed normal
     * connection - is made an attempt to connect to a physical machine and on
     * success the physical machine is added to the list of connected physical
     * machines, if <code>false</code> then there is just performed the connection
     * test - used by disconnection method with closing action type set up to
     * shut down all running virtual machines
     * @return manager of type {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualizationToolManager
     * VirtualizationToolManager} for managing virtual machines on the newly
     * connected physical machine
     */
    private VirtualizationToolManager connectTo(PhysicalMachine physicalMachine,
            long millis, boolean doStandardConnection){
        OutputHandler outputHandler = new OutputHandler();
        
        if(physicalMachine == null){
            throw new IllegalArgumentException("A null physical machine used "
                    + "for connection operation.");
        }
        
        if(millis < -1){
            millis = -1l;
        }
        
        if(doStandardConnection){
            if(isConnected(physicalMachine)){
                outputHandler.printMessage("Physical machine " + physicalMachine
                        + " is already connected.");            

                return new VirtualizationToolManagerImpl(physicalMachine);
            }
            outputHandler.printMessage("Connecting to the physical machine "
                    + physicalMachine);
        }
        
        establishConnection(physicalMachine, millis);
        
        if(doStandardConnection){
            ConnectedPhysicalMachines connectedPhysicalMachines = ConnectedPhysicalMachines.getInstance();
            connectedPhysicalMachines.add(physicalMachine);
            outputHandler.printMessage("Physical machine " + physicalMachine
                    + " has been connected successfully");
        }
        
        return new VirtualizationToolManagerImpl(physicalMachine);        
    }
    
    /**
     * Method that ensures performing of all necessary attempts to establish the
     * connection with the physical machine.
     * @param physicalMachine represents the physical machine which is going to
     * be connected
     * @param millis waiting time in milliseconds between each unsuccessful
     * connection establishment attempt
     */
    private void establishConnection(PhysicalMachine physicalMachine, long millis){
        //number of attempts for connection establishment, max. number of attempts 
        //is 3 (set up the start value)
        int attempt = (millis == -1 ? 3 : 1);        
        NativeVBoxAPIConnection nativeVBoxAPIConnection = new NativeVBoxAPIConnection();
        
        //max. 3 attempts to try to connect to the physical machine
        do{
            try {
                //connect to the physical machine
                nativeVBoxAPIConnection.connectTo(physicalMachine);
                break;
            } catch (ConnectionFailureException ex) {
                ++attempt;
                if(attempt >= 4){
                    throw ex;
                }else{
                    //set the end time, to which the system current time must get to stop looping,
                    //with parameter millis which represents max wait time for pause between each
                    //connection establishment attempt
                    long endTime = System.currentTimeMillis() + millis;
                    while(System.currentTimeMillis() < endTime){
                        //loop about millis/1000 seconds
                    }
                }             
            }
        }while(attempt < 4);
    }
    
    /**
     * Method that ensures the physical machine which is being disconnected is
     * also removed from the list of connected physical machines.
     * @param physicalMachine represents the physical machines which is going to
     * be deleted from the list of connected physical machines
     */
    private void removePMFromListOfConnectedPMs(PhysicalMachine physicalMachine){
        ConnectedPhysicalMachines connectedPhysicalMachines = ConnectedPhysicalMachines.getInstance();
        
        if(!connectedPhysicalMachines.remove(physicalMachine)){
            throw new IllegalStateException("Physical machine " + physicalMachine
                    + "was not removed successfully from the list of connected "
                    + "physical machines despite its presence in this list.");
        }
    }
}
