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
import cz.muni.fi.virtualtoolmanager.pubapi.entities.VirtualMachine;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnexpectedVMStateException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownVirtualMachineException;
import cz.muni.fi.virtualtoolmanager.pubapi.managers.ConnectionManager;
import cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualMachineManager;
import cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualizationToolManager;
import cz.muni.fi.virtualtoolmanager.pubapi.types.CloneType;
import java.io.PrintStream;
import java.util.List;
import java.util.UUID;

/**
 * Class that provide the implementation of methods declared in
 * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualizationToolManager
 * VirtualizationToolManager}.
 * 
 * @author Tomáš Šmíd
 */
public class VirtualizationToolManagerImpl implements VirtualizationToolManager{
    /** Represents the physical machine with which the particular instance of
     * this class is associated, respectively this manager can manage all
     * virtual machines of this physical machine, but no others */ 
    private final PhysicalMachine hostMachine;
    
    /**
     * The first and the only class constructor, which is package private so
     * the instance of this class cannot be created over the borders of its package.
     * In this constructor is set up the physical machine to which the virtualization
     * tool manager is associated.
     * @param hostMachine represents the connected physical machine
     */
    VirtualizationToolManagerImpl(PhysicalMachine hostMachine){
        this.hostMachine = hostMachine;
    }
    
    /**
     * <div>
     * Method that implements the method
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualizationToolManager#registerVirtualMachine(String)
     * VirtualizationToolManager::registerVirtualMachine(String)}.
     * </div>
     * <div>
     * If there occurs any error while the registration operation is being
     * processed, then there can be thrown one of the following exceptions:
     * <ul>
     * <li><strong>IllegalArgumentException - </strong>thrown when the given
     * virtual machine name is <code>null</code> or empty
     * <li><strong>
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownVirtualMachineException
     * UnknownVirtualMachineException} - </strong>thrown when the virtual machine
     * of the given name is not present in the default VirtualBox machine folder
     * or there cannot be opened the virtual hard disk which is specified in the
     * configuration file
     * <li><strong>
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException
     * ConnectionFailureException} - </strong>thrown when the physical machine,
     * on which the virtual machine should be registered, is not connected or
     * if there occurs any connection problem during comunication with the remote
     * VirtualBox web server like:
     * <ol>
     * <li>incorrect key values of physical machine (IP address, port number of
     * VirtualBox web server, username or user password)
     * <li>remote VirtualBox web server is not running
     * <li>network connection is not working properly or at all
     * </ol>
     * If any of the mentioned error occurs then the physical machine will be
     * automatically disconnected.
     * </ul>
     * </div>
     * @param name represents the name of virtual machine which should be registered
     */
    @Override
    public void registerVirtualMachine(String name) {
        OutputHandler outputHandler = new OutputHandler();
        
        if(name == null){
            throw new IllegalArgumentException("A null name of a virtual machine "
                    + "used for virtual machine registration operation.");
        }
        
        if(name.trim().isEmpty()){
            throw new IllegalArgumentException("An empty name of a virtual machine "
                    + "used for virtual machine registration operation.");
        }
        
        ConnectionManager connectionManager = new ConnectionManagerImpl();
        if(!connectionManager.isConnected(hostMachine)){
            throw new ConnectionFailureException("Virtual machine registration "
                    + "operation failure: Virtual machine with name \"" + name 
                    + "\" cannot be registered, because the physical machine "
                    + hostMachine + " on which should be virtual machine"
                    + "registered is not connected.");
        }
        
        outputHandler.printMessage("Registering virtual machine \"" + name + "\"");
        
        NativeVBoxAPIManager nativeVBoxAPIManager = new NativeVBoxAPIManager();
        try{
            if(!nativeVBoxAPIManager.registerVirtualMachine(hostMachine, name)){
                outputHandler.printMessage("Virtual machine \"" + name + "\" is "
                        + "already registered");
                return;
            }
        }catch (ConnectionFailureException ex){
            connectionManager.disconnectFrom(hostMachine);
            throw ex;
        }
        
        outputHandler.printMessage("Virtual machine \"" + name + "\" has been "
                + "registered successfully");
    }

    /**
     * <div>
     * Method that implements the method
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualizationToolManager#findVirtualMachineById(UUID)
     * VirtualizationToolManager::findVirtualMachineById()}.
     * </div>
     * <div>
     * This method is implemented the same as the method {@link #findVirtualMachineByName(String)}.
     * One difference is that if occurs problem, then the following exception
     * can be thrown:
     * <ul>
     * <li><strong>IllegalArgumentException - </strong>thrown when the given
     * virtual machine uuid is <code>null</code>
     * </ul>
     * </div>
     * @param id virtual machine uuid
     * @return matched virtual machine if success, <code>null</code> otherwise
     */
    @Override
    public VirtualMachine findVirtualMachineById(UUID id) {        
        //just test if id is not null
        if(id == null){
            throw new IllegalArgumentException("A null id of virtual machine "
                    + "used for virtual machine retrieve operation by id.");
        }
        //it is possible to call findVirtualMachineByName(), because to native 
        //method for VM retrieve is always given a string parameter
        return findVirtualMachineByName(id.toString());
    }

    /**
     * <div>
     * Method implements the method
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualizationToolManager#findVirtualMachineByName(String)
     * VirtualizationToolManager::findVirtualMachineByName(String)}.
     * </div>
     * <div>
     * If there occurs error, then one of the following exceptions can be thrown:
     * <ul>
     * <li><strong>IllegalArgumentException - </strong>thrown when the given
     * virtual machine name is <code>null</code> or empty
     * <li><strong>
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException
     * ConnectionFailureException} - </strong>thrown when the host machine is not
     * connected or one of the further problems occur:
     * <ol>
     * <li>incorrect key values of physical machine (IP address, port number of
     * VirtualBox web server, username or user password)
     * <li>remote VirtualBox web server is not running
     * <li>network connection is not working properly or at all
     * </ol>
     * If any of the mentioned error occurs then the physical machine will be
     * automatically disconnected.
     * </ul>
     * </div>
     * @param name represents the virtual machine name
     * @return matched virtual machine if success, <code>null</code> otherwise
     */
    @Override
    public VirtualMachine findVirtualMachineByName(String name) {
        if(name == null){
            throw new IllegalArgumentException("A null name of virtual machine "
                    + "used for virtual machine retrieve operation by name.");
        }
        
        if(name.trim().isEmpty()){
            throw new IllegalArgumentException("An empty name or id of virtual "
                    + "machine used for virtual machine retrieve operation.");
        }
        
        ConnectionManager connectionManager = new ConnectionManagerImpl();
        if(!connectionManager.isConnected(hostMachine)){
            throw new ConnectionFailureException("Virtual machine retrieve "
                    + "operation failure: Virtual machine with name or id \"" 
                    + name + "\" cannot be retrieved, because the physical "
                    + "machine " + hostMachine + " is not connected.");
        }
        
        NativeVBoxAPIManager nativeVBoxAPIManager = new NativeVBoxAPIManager();
        VirtualMachine virtualMachine;
        try{
            virtualMachine = nativeVBoxAPIManager.getVirtualMachine(hostMachine, name);
        }catch(UnknownVirtualMachineException ex){
            return null;
        }catch (ConnectionFailureException ex) {
            connectionManager.disconnectFrom(hostMachine);
            throw ex;
        }
        
        return virtualMachine;
    }

    /**
     * <div>
     * Method that implements the method
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualizationToolManager#getVirtualMachines()
     * VirtualizationToolManager::getVirtualMachines()}.
     * </div>
     * <div>
     * If there occurs error, then one of the following exceptions can be thrown:
     * <ul>
     * <li><strong>
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException
     * ConnectionFailureException} - </strong>thrown when the host machine is not
     * connected or one of the further problems occur:
     * <ol>
     * <li>incorrect key values of physical machine (IP address, port number of
     * VirtualBox web server, username or user password)
     * <li>remote VirtualBox web server is not running
     * <li>network connection is not working properly or at all
     * </ol>
     * If any of the mentioned error occurs then the physical machine will be
     * automatically disconnected.
     * <li><strong>
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownVirtualMachineException
     * UnknownVirtualMachineException} - </strong>thrown when there occurs any
     * VirtualBox error
     * </ul>
     * </div>
     * @return list of all registered virtual machines from the host machine
     */
    @Override
    public List<VirtualMachine> getVirtualMachines() {
        ConnectionManager connectionManager = new ConnectionManagerImpl();
        
        if(!connectionManager.isConnected(hostMachine)){
            throw new ConnectionFailureException("All virtual machines retrieve "
                    + "operation failure: There cannot be retrieved all virtual "
                    + "machines from physical machine " + hostMachine + ", "
                    + "because it is not connected.");
        }
        
        NativeVBoxAPIManager nativeVBoxAPIManager = new NativeVBoxAPIManager();
        List<VirtualMachine> virtualMachines;
        try{
            virtualMachines = nativeVBoxAPIManager.getAllVirtualMachines(hostMachine);
        }catch (ConnectionFailureException ex) {
            connectionManager.disconnectFrom(hostMachine);
            throw ex;
        }
        
        return virtualMachines;
    }

    /**
     * <div>
     * Method that implements the method
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualizationToolManager#removeVirtualMachine(VirtualMachine)
     * VirtualizationToolManager::removeVirtualMachine(VirtualMachine)}.
     * </div>
     * <div>
     * <p>
     * This method completely removes the specified virtual machine from the 
     * host machine. So virtual machine is not only unregistered, but all its
     * files are destroyed.
     * <p>
     * This implementation works differently than the standard user APIs for
     * managing VirtualBox like <strong>VBoxManage</strong> or <strong>VirtualBox
     * GUI</strong> which do not remove those virtual machines that have any
     * child (linked clone). This implementation remove all the nested tree from
     * the the specified virtual machine recursively. So with this method can
     * be virtual machines removed faster and easier, but it is also less safe,
     * because there can happen the virtual machine which was not intended to be
     * removed is removed as a child of any other virtual machine.
     * If there occurs any error, then the following exceptions can be thrown:
     * <ul>
     * <li><strong>
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException
     * ConnectionFailureException} - </strong>thrown when the host machine is not
     * connected or one of the further problems occur:
     * <ol>
     * <li>incorrect key values of physical machine (IP address, port number of
     * VirtualBox web server, username or user password)
     * <li>remote VirtualBox web server is not running
     * <li>network connection is not working properly or at all
     * </ol>
     * If any of the mentioned error occurs then the physical machine will be
     * automatically disconnected.
     * <li><strong>
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownVirtualMachineException
     * UnknownVirtualMachineException} - </strong>thrown when the specified virtual
     * machine is not in the list of registered virtual machines on the remote
     * physical machine
     * <li><strong>IllegalStateException - </strong>thrown when the specified 
     * virtual machine was found as registered virtual machine, but a few moments 
     * later if it should be unregistered, because it is not accessible, it 
     * throw the vbox exception
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnexpectedVMStateException
     * UnexpectedVMStateException} - </strong>thrown when the virtual machine
     * which should be removed is not powered off
     * <li><strong>IllegalArgumentException - </strong>thrown when the given
     * virtual machine is <code>null</code> or its host machine is not correct
     * </ul>
     * </div>
     * @param virtualMachine represents the virtual machine which should be
     * removed
     */
    @Override
    public void removeVirtualMachine(VirtualMachine virtualMachine) {
        OutputHandler outputHandler = new OutputHandler();
        
        if(virtualMachine == null){
            throw new IllegalArgumentException("A null virtual machine used for "
                    + "virtual machine removal operation.");
        }
        
        if(!virtualMachine.getHostMachine().equals(hostMachine)){
            throw new IllegalArgumentException("Virtual machine with incorrect "
                    + "physical machine used for virtual machine removal operation.");
        }
        
        ConnectionManager connectionManager = new ConnectionManagerImpl();
        if(!connectionManager.isConnected(hostMachine)){
            throw new ConnectionFailureException("Virtual machine removal "
                    + "operation failure: Virtual machine " + virtualMachine
                    + " cannot be removed, because the physical machine "
                    + hostMachine + " on which the virtual machine is found is "
                    + "not connected.");
        }
        
        outputHandler.printMessage("Removing virtual machine " + virtualMachine);
        
        NativeVBoxAPIManager nativeVBoxAPIManager = new NativeVBoxAPIManager();
        try {
            nativeVBoxAPIManager.removeVirtualMachine(virtualMachine);
        }catch (ConnectionFailureException ex){
            connectionManager.disconnectFrom(hostMachine);
            throw ex;
        }
        
        outputHandler.printMessage("Virtual machine " + virtualMachine 
                + " removed successfully");
    }

    /**
     * <div>
     * Method that implements the method
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualizationToolManager#cloneVirtualMachine(VirtualMachine, CloneType)
     * VirtualizationToolManager::cloneVirtualMachine(VirtualMachine,cloneType)}.
     * </div>
     * <div>
     * With implementation of this method it is possible to create four different
     * clone types - 3 types of full clones and one type of linked clone.
     * Full clones are different in states which are used for cloning operation
     * from the original virtual machine and specified with {@link cz.muni.fi.virtualtoolmanager.pubapi.types.CloneType 
     * CloneType}. Full clones are completely independent on the original virtual
     * machines. But it takes a longer time to create a full clone. Linked clone
     * is created much quicker, but it depends on the original virtual machine.
     * Names of full clones are formed from the name of the original virtual machine
     * and the suffix "_FullCloneX", where X is the lowest available number.
     * The same works for linked clones, but instead of "_FullCloneX" it uses suffix
     * "_LinkedCloneY", where Y is determined by the same way as X.
     * If there occurs an error, then there can be thrown one of the following
     * exception:
     * <ul>
     * <li><strong>
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException
     * ConnectionFailureException} - </strong>thrown when the host machine is not
     * connected or one of the further problems occur:
     * <ol>
     * <li>incorrect key values of physical machine (IP address, port number of
     * VirtualBox web server, username or user password)
     * <li>remote VirtualBox web server is not running
     * <li>network connection is not working properly or at all
     * </ol>
     * If any of the mentioned error occurs then the physical machine will be
     * automatically disconnected.
     * <li><strong>
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownVirtualMachineException
     * UnknownVirtualMachineException} - </strong>thrown when the specified virtual
     * machine is not in the list of registered virtual machines on the remote
     * physical machine
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnexpectedVMStateException
     * UnexpectedVMStateException} - </strong>thrown when the virtual machine is
     * not accessible or it is not in a required state ("running","poweredOff",
     * "paused","saved")
     * <li><strong>IllegalArgumentException - </strong>thrown when either the given
     * virtual machine or clone type is <code>null</code> or virtual machine
     * host machine is not correct
     * </ul>
     * </div>
     * @param virtualMachine represents the virtual machine which should be cloned
     * @param cloneType specifies the type of the final clone
     * @return virtual machine clone
     */
    @Override
    public VirtualMachine cloneVirtualMachine(VirtualMachine virtualMachine, CloneType cloneType) {
        OutputHandler outputHandler = new OutputHandler();
        
        if(virtualMachine == null){
            throw new IllegalArgumentException("A null virtual machine used for "
                    + "virtual machine cloning operation.");
        }
        
        if(cloneType == null){
            throw new IllegalArgumentException("A null clone type used for "
                    + "virtual machine cloning operation.");
        }
        
        if(!virtualMachine.getHostMachine().equals(hostMachine)){
            throw new IllegalArgumentException("Virtual machine with incorrect "
                    + "physical machine used for virtual machine cloning operation.");
        }
        
        ConnectionManager connectionManager = new ConnectionManagerImpl();
        if(!connectionManager.isConnected(hostMachine)){
            throw new ConnectionFailureException("Virtual machine cloning "
                    + "operation failure: Virtual machine " + virtualMachine 
                    + " cannot be cloned, because the physical machine "
                    + hostMachine + " on which the virtual machine is found is "
                    + "not connected.");
        }
        
        outputHandler.printMessage("Cloning virtual machine " + virtualMachine);
        
        NativeVBoxAPIManager nativeVBoxAPIManager = new NativeVBoxAPIManager();
        VirtualMachine vmClone;
        try {
            vmClone = nativeVBoxAPIManager.createVMClone(virtualMachine, cloneType);
        }catch (ConnectionFailureException ex){
            connectionManager.disconnectFrom(hostMachine);
            throw ex;
        }
        
        outputHandler.printMessage("Cloning operation finished successfully");
        return vmClone;
    }

    /**
     * <div>
     * Method that implements the method
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualizationToolManager#close()
     * VirtualizationToolManager::close()}.
     * </div>
     * <div>
     * This method is implemented using {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualMachineManager#getVMState(VirtualMachine)
     * VirtualMachineManager::getVMState(VirtualMachine)} and
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualMachineManager#shutDownVM(VirtualMachine)
     * VirtualMachineManager::shutDownVM(VirtualMachine)}.
     * So this method shuts down all running virtual machines of a particular
     * physical machine.
     * If error occurs, then the following exceptions can be thrown:
     * <ul>
     * <li><strong>
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException
     * ConnectionFailureException} - </strong>thrown when the host machine is not
     * connected or one of the further problems occur:
     * <ol>
     * <li>incorrect key values of physical machine (IP address, port number of
     * VirtualBox web server, username or user password)
     * <li>remote VirtualBox web server is not running
     * <li>network connection is not working properly or at all
     * </ol>
     * <li><strong>
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownVirtualMachineException
     * UnknownVirtualMachineException} - </strong>thrown when the specified virtual
     * machine is not in the list of registered virtual machines on the remote
     * physical machine
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnexpectedVMStateException
     * UnexpectedVMStateException} - </strong>thrown when the virtual machine is
     * not accessible or it is not in a required state
     * </ul>
     * </div>
     */
    @Override
    public void close() {
        OutputHandler outputHandler = new OutputHandler();        
        ConnectionManager connectionManager = new ConnectionManagerImpl();
        
        if(!connectionManager.isConnected(hostMachine)){
            throw new ConnectionFailureException("Virtualization tool closing "
                    + "operation failure: Virtual machines cannot be shut down, "
                    + "because their host machine " + hostMachine + " is not "
                    + "connected.");
        }
        
        outputHandler.printMessage("Shutting down all virtual machines on "
                + "physical machine " + hostMachine);
                
        List<VirtualMachine> virtualMachines;
        String errMsg = "Virtualization tool closing operation failure: Virtual "
                + "machines could not be properly shut down -> ";
        try{
            virtualMachines = getVirtualMachines();
            if(!virtualMachines.isEmpty()){
                VirtualMachineManager virtualMachineManager =
                        new VirtualMachineManagerImpl();
                for(VirtualMachine virtualMachine : virtualMachines){
                    String vmState = virtualMachineManager.getVMState(virtualMachine);                
                    switch(vmState){
                        case "Running":
                        case "Paused" :
                        case "Stuck"  : virtualMachineManager.shutDownVM(virtualMachine);
                                        break;
                        default       : break;
                    }
                }
            }
        }catch(UnknownVirtualMachineException ex){
            throw new UnknownVirtualMachineException(errMsg + ex.getMessage());
        }catch(UnexpectedVMStateException ex){
            throw new UnexpectedVMStateException(errMsg + ex.getMessage());
        }catch(ConnectionFailureException ex){
            throw new ConnectionFailureException(errMsg + ex.getMessage());
        }
        
        outputHandler.printMessage("Virtualization tool closing operation "
                + "finished successfully.");
    }
    
    private void setOutputStreams(PrintStream printStream){
        setOutputStreams(printStream, printStream);
    }
    
    private void setOutputStreams(PrintStream stdOutput, PrintStream stdErrOutput){
        OutputHandler.setOutputStream(stdOutput);
        OutputHandler.setErrorOutputStream(stdErrOutput);        
    }
    
}
