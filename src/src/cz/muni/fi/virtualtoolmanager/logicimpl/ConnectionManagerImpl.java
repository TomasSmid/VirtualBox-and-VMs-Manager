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

import cz.muni.fi.virtualtoolmanager.pubapi.entities.PhysicalMachine;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.IncompatibleVirtToolAPIVersionException;
import cz.muni.fi.virtualtoolmanager.pubapi.managers.ConnectionManager;
import cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualizationToolManager;
import cz.muni.fi.virtualtoolmanager.pubapi.types.ClosingActionType;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

/**
 *
 * @author Tomáš Šmíd
 */
public class ConnectionManagerImpl implements ConnectionManager{

    @Override
    public VirtualizationToolManager connectTo(PhysicalMachine physicalMachine){
        return connectTo(physicalMachine,2000l);
    }
    
    @Override
    public VirtualizationToolManager connectTo(PhysicalMachine physicalMachine, long millis){
        return connectTo(physicalMachine,millis,true);
    }
    
    @Override
    public void disconnectFrom(PhysicalMachine physicalMachine){
        disconnectFrom(physicalMachine, ClosingActionType.NONE);
    }

    @Override
    public void disconnectFrom(PhysicalMachine physicalMachine, ClosingActionType closingAction) {
        OutputHandler outputHandler = new OutputHandler();
        
        if(physicalMachine == null){
            outputHandler.printErrorMessage("Disconnection operation failure: "
                    + "There was made an attempt to disconnect from a null "
                    + "physical machine.");
            return;
        }
        
        //check if the physical machine is connected
        if(!isConnected(physicalMachine)){
            outputHandler.printErrorMessage("Disconnection operation failure: "
                    + "Physical machine " + physicalMachine + " cannot be "
                    + "disconnected, because it is not connected.");
            return;
        }
        
        outputHandler.printMessage("Disconnecting from the physical machine " + physicalMachine);
        
        switch(closingAction){
            case NONE:{
                removePMFromListOfConnectedPMs(physicalMachine);                
                break;
            }
            case SHUT_DOWN_RUNNING_VM:{
                //preserve the original output streams and then set up new streams
                PrintStream origOutStream = OutputHandler.getOutputStream();
                PrintStream origErrStream = OutputHandler.getErrorOutputStream();
                //output stream set up to null - no info messages are required on watched output stream
                //error output stream set up to auxilliary stream - no error messages are required on
                //watched error output stream, but the error message will be needed if there appears 
                //an error while the method this::connectTo() is being processed
                final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
                setOutputStreams(null, new PrintStream(errContent));

                //check the connection with the physical machine is ok and get the virt. tool manager
                VirtualizationToolManager virtualizationToolManager = connectTo(physicalMachine,-1,false);
                if(virtualizationToolManager == null){
                    //set up output streams back to the standard output stream and standard error output stream
                    setOutputStreams(origOutStream, origErrStream);
                    outputHandler.printErrorMessage("Disconnection operation failure: "
                            + "There could not be stopped the work with virtual machines "
                            + "properly, " + getReasonOfDisconnectionFailure(errContent.toString()));
                    removePMFromListOfConnectedPMs(physicalMachine);
                    outputHandler.printMessage("Physical machine " + physicalMachine + " was disconnected");
                    return;
                }

                //set up output streams back to the standard output stream and standard error output stream for
                //information provision while the work with virtual machines is being stopped
                setOutputStreams(origOutStream, origErrStream);
                //stop working with all virtual machines from the physical machine and shut the running
                //virtual machines down
                virtualizationToolManager.close();
                removePMFromListOfConnectedPMs(physicalMachine);
                break;
            }
            default: throw new IllegalArgumentException("Illegal enumeration literal of type "
                    + "ClosingActionType occured while trying to disconnect from " + physicalMachine);
        }
        
        outputHandler.printMessage("Physical machine " + physicalMachine + " was disconnected");
    }

    @Override
    public boolean isConnected(PhysicalMachine physicalMachine) {
        //check the physical machine is not null - null physical machine cannot be connected
        if(physicalMachine == null){
            return false;
        }
        
        ConnectedPhysicalMachines connectedPhysicalMachines = ConnectedPhysicalMachines.getInstance();
        
        //finds out if the physical machine is connected (is placed in the list of connected
        //physical machines) and returns answer
        return connectedPhysicalMachines.isConnected(physicalMachine);
    }

    @Override
    public List<PhysicalMachine> getConnectedPhysicalMachines() {
        ConnectedPhysicalMachines connectedPhysicalMachines = ConnectedPhysicalMachines.getInstance();
        return connectedPhysicalMachines.getConnectedPhysicalMachines();
    }
    
    @Override
    public void close() {
        while(!getConnectedPhysicalMachines().isEmpty()){
            
            disconnectFrom(getConnectedPhysicalMachines().get(0), ClosingActionType.SHUT_DOWN_RUNNING_VM);
        }
    }
    
    private void setOutputStreams(PrintStream printStream){
        setOutputStreams(printStream, printStream);
    }
    
    private void setOutputStreams(PrintStream stdOutput, PrintStream stdErrOutput){
        OutputHandler.setOutputStream(stdOutput);
        OutputHandler.setErrorOutputStream(stdErrOutput);        
    }
    
    private VirtualizationToolManager connectTo(PhysicalMachine physicalMachine, long millis,
                                                boolean doStandardConnection){
        OutputHandler outputHandler = new OutputHandler();
        
        if(physicalMachine == null){
            outputHandler.printErrorMessage("Connection operation failure: There "
                    + "was made an attempt to connect to a null physical machine.");
            return null;
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
        }
        
        outputHandler.printMessage("Connecting to the physical machine " + physicalMachine);
        
        try{
            establishConnection(physicalMachine, millis);
        }catch(ConnectionFailureException | IncompatibleVirtToolAPIVersionException ex){
            outputHandler.printErrorMessage(ex.getMessage());
            return null;
        }
        
        if(doStandardConnection){
            ConnectedPhysicalMachines connectedPhysicalMachines = ConnectedPhysicalMachines.getInstance();
            connectedPhysicalMachines.add(physicalMachine);
        }
        outputHandler.printMessage("Physical machine " + physicalMachine + " has "
                + "been connected successfully");
        
        return new VirtualizationToolManagerImpl(physicalMachine);        
    }
    
    private void establishConnection(PhysicalMachine physicalMachine, long millis) throws ConnectionFailureException,
                                                                                          IncompatibleVirtToolAPIVersionException{
        //number of attempts for connection establishment, max. number of attempts is 3
        int attempt = (millis == -1 ? 3 : 1);        
        NativeVBoxAPIConnection nativeVBoxAPIConnection = new NativeVBoxAPIConnection();
        
        //max. 3 attempts to try to connect to the physical machine
        do{
            try {
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
            }catch(IncompatibleVirtToolAPIVersionException ex){
                throw ex;
            }
        }while(attempt < 4);
    }
    
    private String getReasonOfDisconnectionFailure(String errorMessage){
        if(errorMessage.startsWith("Incompatible version")){
            return "because there was changed an API version of VirtualBox and it "
                    + "is incorrect now.";
        }
        
        return "because there occured one of these problems: 1. Network connection "
                + "is not working properly or at all / 2. The VirtualBox web server "
                + "is not running / 3. One of the key value (IP address, number of web "
                + "server port, username or user password) of the physical machine "
                + "has been changed and it is incorrect now.";
    }
    
    private void removePMFromListOfConnectedPMs(PhysicalMachine physicalMachine){
        ConnectedPhysicalMachines connectedPhysicalMachines = ConnectedPhysicalMachines.getInstance();
        
        if(!connectedPhysicalMachines.remove(physicalMachine)){
            throw new IllegalStateException("Physical machine " + physicalMachine
                    + "was not removed successfully from the list of connected "
                    + "physical machines despite its presence in this list.");
        }
    }
}
