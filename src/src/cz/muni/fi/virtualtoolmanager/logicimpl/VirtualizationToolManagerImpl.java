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

import cz.muni.fi.virtualtoolmanager.io.OutputHandler;
import cz.muni.fi.virtualtoolmanager.pubapi.entities.PhysicalMachine;
import cz.muni.fi.virtualtoolmanager.pubapi.entities.VirtualMachine;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnexpectedVMStateException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownVirtualMachineException;
import cz.muni.fi.virtualtoolmanager.pubapi.managers.ConnectionManager;
import cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualMachineManager;
import cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualizationToolManager;
import cz.muni.fi.virtualtoolmanager.pubapi.types.CloneType;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author Tomáš Šmíd
 */
public class VirtualizationToolManagerImpl implements VirtualizationToolManager{
    
    private final PhysicalMachine hostMachine;
    
    VirtualizationToolManagerImpl(PhysicalMachine hostMachine){
        this.hostMachine = hostMachine;
    }
    
    @Override
    public void registerVirtualMachine(String name) {
        OutputHandler outputHandler = new OutputHandler();
        
        if(name == null){
            outputHandler.printErrorMessage("Virtual machine registration operation "
                    + "failure: There was made an attempt to register a virtual machine "
                    + "with a null name.");
            return;
        }
        
        if(name.trim().isEmpty()){
            outputHandler.printErrorMessage("Virtual machine registration operation "
                    + "failure: There was made an attempt to register a virtual machine "
                    + "with an empty name.");
            return;
        }
        
        ConnectionManager connectionManager = new ConnectionManagerImpl();
        if(!connectionManager.isConnected(hostMachine)){
            outputHandler.printErrorMessage("Virtual machine registration operation "
                    + "failure: Virtual machine with name \"" + name + "\" cannot be "
                    + "registered, because the physical machine " + hostMachine 
                    + " on which should be virtual machine registered is not connected.");
            return;
        }
        
        outputHandler.printMessage("Registering virtual machine \"" + name + "\"");
        
        NativeVBoxAPIManager nativeVBoxAPIManager = new NativeVBoxAPIManager();
        try{
            if(!nativeVBoxAPIManager.registerVirtualMachine(hostMachine, name)){
                outputHandler.printMessage("Virtual machine \"" + name + "\" is already registered");
                return;
            }
        } catch (UnknownVirtualMachineException ex) {
            outputHandler.printErrorMessage(ex.getMessage());
            return;
        } catch (ConnectionFailureException ex){
            outputHandler.printErrorMessage("Connection error occured: There will be stopped "
                    + "the work with physical machine " + hostMachine
                    + " and its virtual machines and physical machine will be disconnected -> "
                    + ex.getMessage());
            connectionManager.disconnectFrom(hostMachine);
            return;
        }
        
        outputHandler.printMessage("Virtual machine \"" + name + "\" has been registered successfully");
    }

    @Override
    public VirtualMachine findVirtualMachineById(UUID id) {
        OutputHandler outputHandler = new OutputHandler();
        //just test if id is not null
        if(id == null){
            outputHandler.printErrorMessage("Virtual machine retrieve operation "
                    + "failure: There was made an attempt to retrieve virtual machine "
                    + "by a null id.");
            return null;
        }
        //it is possible to call findVirtualMachineByName(), because to native method for VM retrieve is
        //always given a string parameter
        return findVirtualMachineByName(id.toString());
    }

    @Override
    public VirtualMachine findVirtualMachineByName(String name) {
        OutputHandler outputHandler = new OutputHandler();
        
        if(name == null){
            outputHandler.printErrorMessage("Virtual machine retrieve operation "
                    + "failure: There was made an attempt to retrieve virtual machine "
                    + "by a null name.");
            return null;
        }
        
        if(name.trim().isEmpty()){
            outputHandler.printErrorMessage("Virtual machine retrieve operation "
                    + "failure: There was made an attempt to retrieve virtual machine "
                    + "by an empty id or name.");
            return null;
        }
        
        ConnectionManager connectionManager = new ConnectionManagerImpl();
        if(!connectionManager.isConnected(hostMachine)){
            outputHandler.printErrorMessage("Virtual machine retrieve operation "
                    + "failure: Virtual machine with name or id \"" + name + "\" "
                    + "cannot be retrieved, because the physical machine " + hostMachine
                    + "is not connected.");
            return null;
        }
        
        NativeVBoxAPIManager nativeVBoxAPIManager = new NativeVBoxAPIManager();
        VirtualMachine virtualMachine;
        try{
            virtualMachine = nativeVBoxAPIManager.getVirtualMachine(hostMachine, name);
        } catch (UnknownVirtualMachineException ex){
            outputHandler.printErrorMessage(ex.getMessage());
            return null;
        } catch (ConnectionFailureException ex) {
            outputHandler.printErrorMessage("Connection error occured: There will be stopped "
                    + "the work with physical machine " + hostMachine
                    + " and its virtual machines and physical machine will be disconnected -> "
                    + ex.getMessage());
            connectionManager.disconnectFrom(hostMachine);
            return null;
        }
        
        return virtualMachine;
    }

    @Override
    public List<VirtualMachine> getVirtualMachines() {
        OutputHandler outputHandler = new OutputHandler();
        ConnectionManager connectionManager = new ConnectionManagerImpl();
        
        if(!connectionManager.isConnected(hostMachine)){
            outputHandler.printErrorMessage("All virtual machines retrieve operation "
                    + "failure: There cannot be retrieved all virtual machines from "
                    + "physical machine " + hostMachine + ", because it is not connected.");
            return new ArrayList<>();
        }
        
        NativeVBoxAPIManager nativeVBoxAPIManager = new NativeVBoxAPIManager();
        List<VirtualMachine> virtualMachines;
        try{
            virtualMachines = nativeVBoxAPIManager.getAllVirtualMachines(hostMachine);
        } catch (UnknownVirtualMachineException ex){
            outputHandler.printErrorMessage(ex.getMessage());
            return new ArrayList<>();
        } catch (ConnectionFailureException ex) {
            outputHandler.printErrorMessage("Connection error occured: There will be stopped "
                    + "the work with physical machine " + hostMachine
                    + " and its virtual machines and physical machine will be disconnected -> "
                    + ex.getMessage());
            connectionManager.disconnectFrom(hostMachine);
            return new ArrayList<>();
        }
        
        return virtualMachines;
    }

    @Override
    public void removeVirtualMachine(VirtualMachine virtualMachine) {
        OutputHandler outputHandler = new OutputHandler();
        
        if(virtualMachine == null){
            outputHandler.printErrorMessage("Virtual machine removal operation "
                    + "failure: There was made an attempt to remove a null virtual "
                    + "machine from physical machine " + hostMachine + ".");
            return;
        }
        
        if(!virtualMachine.getHostMachine().equals(hostMachine)){
            outputHandler.printErrorMessage("Virtual machine removal operation "
                    + "failure: Virtual machine " + virtualMachine + " cannot be "
                    + "removed, because its physical machine is incorrect.");
            return;
        }
        
        ConnectionManager connectionManager = new ConnectionManagerImpl();
        if(!connectionManager.isConnected(hostMachine)){
            outputHandler.printErrorMessage("Virtual machine removal operation "
                    + "failure: Virtual machine " + virtualMachine + " cannot be "
                    + "removed, because the physical machine " + hostMachine 
                    + " on which the virtual machine is found is not connected.");
            return;
        }
        
        outputHandler.printMessage("Removing virtual machine " + virtualMachine 
                + " from physical machine " + hostMachine);
        
        NativeVBoxAPIManager nativeVBoxAPIManager = new NativeVBoxAPIManager();
        try {
            nativeVBoxAPIManager.removeVirtualMachine(virtualMachine);
        } catch (UnknownVirtualMachineException | UnexpectedVMStateException ex) {
            outputHandler.printErrorMessage(ex.getMessage());
            return;
        } catch (ConnectionFailureException ex){
            outputHandler.printErrorMessage("Connection error occured: There will be stopped "
                    + "the work with physical machine " + hostMachine
                    + " and its virtual machines and physical machine will be disconnected -> "
                    + ex.getMessage());
            connectionManager.disconnectFrom(hostMachine);
            return;
        }
        
        outputHandler.printMessage("Virtual machine " + virtualMachine + " removed successfully");
    }

    @Override
    public VirtualMachine cloneVirtualMachine(VirtualMachine virtualMachine, CloneType cloneType) {
        OutputHandler outputHandler = new OutputHandler();
        
        if(virtualMachine == null){
            outputHandler.printErrorMessage("Virtual machine cloning operation "
                    + "failure: There was made an attempt to clone a null virtual "
                    + "machine.");
            return null;
        }
        
        if(cloneType == null){
            outputHandler.printErrorMessage("Virtual machine cloning operation "
                    + "failure: There must be specified how the virtual machine "
                    + virtualMachine + " should be cloned (clone type).");
            return null;
        }
        
        if(!virtualMachine.getHostMachine().equals(hostMachine)){
            outputHandler.printErrorMessage("Virtual machine cloning operation "
                    + "failure: Virtual machine " + virtualMachine + " cannot be "
                    + "cloned, because its physical machine is incorrect.");
            return null;
        }
        
        ConnectionManager connectionManager = new ConnectionManagerImpl();
        if(!connectionManager.isConnected(hostMachine)){
            outputHandler.printErrorMessage("Virtual machine cloning operation "
                    + "failure: Virtual machine " + virtualMachine + " cannot be "
                    + "cloned, because the physical machine " + hostMachine
                    + " on which the virtual machine is found is not connected.");
            return null;
        }
        
        outputHandler.printMessage("Cloning virtual machine " + virtualMachine
                    + "on physical machine " + hostMachine);
        
        NativeVBoxAPIManager nativeVBoxAPIManager = new NativeVBoxAPIManager();
        VirtualMachine vmClone;
        try {
            vmClone = nativeVBoxAPIManager.createVMClone(virtualMachine, cloneType);
        } catch (UnknownVirtualMachineException | UnexpectedVMStateException ex) {
            outputHandler.printErrorMessage(ex.getMessage());
            return null;
        } catch (ConnectionFailureException ex){
            outputHandler.printErrorMessage("Connection error occured: There will be stopped "
                    + "the work with physical machine " + hostMachine
                    + " and its virtual machines and physical machine will be disconnected -> "
                    + ex.getMessage());
            connectionManager.disconnectFrom(hostMachine);
            return null;
        }
        
        outputHandler.printMessage("Cloning operation finished successfully");
        return vmClone;
    }

    @Override
    public void close() {
        OutputHandler outputHandler = new OutputHandler();        
        ConnectionManager connectionManager = new ConnectionManagerImpl();
        
        if(!connectionManager.isConnected(hostMachine)){
            outputHandler.printErrorMessage("Virtualization tool closing operation "
                    + "failure: There is none work to be stopped, because physical "
                    + "machine " + hostMachine + " is not connected.");
            return;
        }
        
        outputHandler.printMessage("Stopping work with virtual machines on physical machine " + hostMachine);
        
        final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        PrintStream origOutStream = OutputHandler.getOutputStream();
        PrintStream origErrStream = OutputHandler.getErrorOutputStream();
        setOutputStreams(null, new PrintStream(errContent));
                
        List<VirtualMachine> virtualMachines = getVirtualMachines();
        if(virtualMachines.isEmpty()){            
            if(!errContent.toString().isEmpty()){
                setOutputStreams(origOutStream, origErrStream);
                if(!connectionManager.isConnected(hostMachine)){                    
                    outputHandler.printErrorMessage("Virtualization tool closing operation "
                            + "failure: Work with virtual machines was not stopped properly, "
                            + "because there occured any connection problem.");
                    return;
                }else{
                    outputHandler.printErrorMessage("Virtualization tool closing operation "
                            + "failure: Work with virtual machines was not stopped properly -> "
                            + errContent.toString());
                    return;
                }
            }
        }else{        
            VirtualMachineManager virtualMachineManager = new VirtualMachineManagerImpl();
            for(VirtualMachine virtualMachine : virtualMachines){
                String vmState = virtualMachineManager.getVMState(virtualMachine);
                if(vmState == null && !errContent.toString().isEmpty()){
                    if(!connectionManager.isConnected(hostMachine)){
                        setOutputStreams(origOutStream, origErrStream);
                        outputHandler.printErrorMessage("Virtualization tool closing operation "
                                + "failure: Work with virtual machines was not stopped properly, "
                                + "because there occured any connection problem.");
                        return;
                    }
                }
                switch(vmState){
                    case "Running":
                    case "Paused" :
                    case "Stuck"  : {
                        virtualMachineManager.shutDownVM(virtualMachine);
                        if(!connectionManager.isConnected(hostMachine)){
                            setOutputStreams(origOutStream, origErrStream);
                            outputHandler.printErrorMessage("Virtualization tool closing operation "
                                    + "failure: Work with virtual machines was not stopped properly, "
                                    + "because there occured any connection problem.");
                            return;
                        }
                        break;
                    }
                    default       : break;
                }
            }
        }
        
        setOutputStreams(origOutStream, origErrStream);
        outputHandler.printMessage("Work with virtual machines on physical machine " + hostMachine
                    + " was successfully stopped");
    }
    
    private void setOutputStreams(PrintStream printStream){
        setOutputStreams(printStream, printStream);
    }
    
    private void setOutputStreams(PrintStream stdOutput, PrintStream stdErrOutput){
        OutputHandler.setOutputStream(stdOutput);
        OutputHandler.setErrorOutputStream(stdErrOutput);        
    }
    
}
