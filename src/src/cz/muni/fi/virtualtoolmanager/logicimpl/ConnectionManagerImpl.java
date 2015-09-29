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
        OutputHandler outputHandler = new OutputHandler();
        
        if(physicalMachine == null){
            outputHandler.printErrorMessage("Connection operation failure: There "
                    + "was made an attempt to connect to a null physical machine.");
            return null;
        }
        
        if(isConnected(physicalMachine)){
            outputHandler.printMessage("Physical machine " + physicalMachine
                    + " is already connected.");            
            
            return new VirtualizationToolManagerImpl(physicalMachine);
        }
        
        outputHandler.printMessage("Connecting to the physical machine " + physicalMachine);
        
        try{
            establishConnection(physicalMachine);
        }catch(ConnectionFailureException | IncompatibleVirtToolAPIVersionException ex){
            outputHandler.printErrorMessage(ex.getMessage());
            return null;
        }
        
        outputHandler.printMessage("Physical machine " + physicalMachine + " has "
                + "been connected successfully");
        
        return new VirtualizationToolManagerImpl(physicalMachine);        
    }

    @Override
    public void disconnectFrom(PhysicalMachine physicalMachine) {
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
        
        //output stream set up to null - no info messages are required on standard output stream
        //error output stream set up to auxilliary stream - no error messages are erquired on
        //standard error output, but the error message will be needed if there appears an error while the method this::connectTo() is being processed
        final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        setOutputStreams(null, new PrintStream(errContent));
        
        //check the connection with the physical machine is ok and get the virt. tool manager
        VirtualizationToolManager virtualizationToolManager = connectTo(physicalMachine);
        if(virtualizationToolManager == null){
            //set up output streams back to the standard output stream and standard error output stream
            setOutputStreams(System.out, System.err);
            outputHandler.printErrorMessage("Disconnection operation failure: "
                    + "There could not be stopped the work with virtual machines "
                    + "properly, " + getReasonOfDisconnectionFailure(errContent.toString()));
            removePMFromListOfConnectedPMs(physicalMachine);
            outputHandler.printMessage("Physical machine " + physicalMachine + " was disconnected");
            return;
        }
        
        //set up output streams back to the standard output stream and standard error output stream for
        //information provision while the work with virtual machines is being stopped
        setOutputStreams(System.out, System.err);
        //stop working with all virtual machines from the physical machine and shut the running
        //virtual machines down
        virtualizationToolManager.close();
        removePMFromListOfConnectedPMs(physicalMachine);
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
        List<PhysicalMachine> allConnectedPhysicalMachines = getConnectedPhysicalMachines();
        
        if(!allConnectedPhysicalMachines.isEmpty()){
            for(PhysicalMachine connectedPM : allConnectedPhysicalMachines){
                disconnectFrom(connectedPM);
            }
        }
    }
    
    private void setOutputStreams(PrintStream printStream){
        setOutputStreams(printStream, printStream);
    }
    
    private void setOutputStreams(PrintStream stdOutput, PrintStream stdErrOutput){
        OutputHandler.setStandardOutput(stdOutput);
        OutputHandler.setStandardErrorOutput(stdErrOutput);        
    }
    
    private void establishConnection(PhysicalMachine physicalMachine) throws ConnectionFailureException,
                                                                             IncompatibleVirtToolAPIVersionException{
        //number of attempts for connection establishment, max. number of attempts is 3
        int attempt = 1;
        long MAX_WAIT_TIME = 2000l;
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
                    long endTime = System.currentTimeMillis() + MAX_WAIT_TIME;
                    while(System.currentTimeMillis() < endTime){
                        //loop about 2 seconds
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
