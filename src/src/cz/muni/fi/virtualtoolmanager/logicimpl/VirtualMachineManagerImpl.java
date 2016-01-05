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
import cz.muni.fi.virtualtoolmanager.pubapi.entities.PortRule;
import cz.muni.fi.virtualtoolmanager.pubapi.entities.VirtualMachine;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnexpectedVMStateException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownVirtualMachineException;
import cz.muni.fi.virtualtoolmanager.pubapi.managers.ConnectionManager;
import cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualMachineManager;
import cz.muni.fi.virtualtoolmanager.pubapi.types.FrontEndType;
import cz.muni.fi.virtualtoolmanager.pubapi.types.ProtocolType;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that provide the implementation of methods declared in
 * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualMachineManager
 * VirtualMachineManager}.
 * 
 * @author Tomáš Šmíd
 */
public class VirtualMachineManagerImpl implements VirtualMachineManager{
    
    /**
     * <div>
     * Method that implements the method
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualMachineManager#startVM(VirtualMachine, FrontEndType)
     * VirtualMachineManager::startVM(VirtualMachine, FrontEndType}.
     * </div>
     * <div>
     * This method is implemented to start the required virtual machine with the
     * specified front-end type {@link cz.muni.fi.virtualtoolmanager.pubapi.types.FrontEndType
     * FrontEndType}.
     * If any error occurs, then there can be thrown the following exceptions:
     * <ul>
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException
     * ConnectionFailureException} - </strong>thrown when there occured any connection
     * problem (invalid physical machine attribute values, not running web server,
     * network connection not working properly or at all) when the native VirtualBox
     * manager object is being retrieved or when the physical machine is not
     * connected
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownVirtualMachineException
     * UnknownVirtualMachineException} - </strong>thrown when the given virtual machine
     * is being retrieved from remote physical machine, but that virtual machine
     * does not exist (is not registered at the VirtualBox hypervisor) on the 
     * remote physical machine
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnexpectedVMStateException
     * UnexpectedVMStateException} - </strong>thrown when the virtual machine cannot be
     * accessed (e.g. corrupted configuration files) or when the virtual machine
     * is in a not required state ("running","paused") for starting operation or
     * the starting operation is aborted because of the another process which
     * is using the virtual machine at the same moment.
     * <li><strong>IllegalArgumentException - </strong>thrown when the given
     * virtual machine is <code>null</code> or the given front-end type is
     * <code>null</code>
     * </ul>
     * </div>
     * @param virtualMachine virtual machine which should be started
     * @param frontEndType type of front-end used for virtual machine
     */
    @Override
    public void startVM(VirtualMachine virtualMachine, FrontEndType frontEndType) {
        OutputHandler outputHandler = new OutputHandler();
        
        if(virtualMachine == null){
            throw new IllegalArgumentException("A null virtual machine used for "
                    + "virtual machine starting operation.");
        }
        
        if(frontEndType == null){
            throw new IllegalArgumentException("A null front-end type used for "
                    + "virtual machine starting operation.");
        }
        
        ConnectionManager connectionManager = new ConnectionManagerImpl();
        if(!connectionManager.isConnected(virtualMachine.getHostMachine())){
            throw new ConnectionFailureException("Virtual machine starting "
                    + "operation failure: Virtual machine " + virtualMachine
                    + " cannot be started, because its host machine "
                    + virtualMachine.getHostMachine() + " is not connected.");
        }
        
        outputHandler.printMessage("Starting virtual machine " + virtualMachine);
        
        NativeVBoxAPIMachine nativeVBoxAPIMachine = new NativeVBoxAPIMachine();
        try{
            nativeVBoxAPIMachine.startVM(virtualMachine, frontEndType);
        }catch(ConnectionFailureException ex){
            connectionManager.disconnectFrom(virtualMachine.getHostMachine());
            throw ex;
        }
        
        outputHandler.printMessage("Virtual machine " + virtualMachine + " is running");
    }

    /**
     * <div>
     * Method that implements the method
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualMachineManager#shutDownVM(VirtualMachine) 
     * VirtualMachineManager::shutDownVM(VirtualMachine}.
     * </div>
     * <div>
     * This method is implemented to shut down the required virtual machine like
     * the machine was unplugged. So it is fast, but no state are saved and any
     * data can be lost.
     * If any error occurs, then there can be thrown the following exceptions:
     * <ul>
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException
     * ConnectionFailureException} - </strong>thrown when there occured any connection
     * problem (invalid physical machine attribute values, not running web server,
     * network connection not working properly or at all) when the native VirtualBox
     * manager object is being retrieved or when the physical machine is not
     * connected
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownVirtualMachineException
     * UnknownVirtualMachineException} - </strong>thrown when the given virtual machine
     * is being retrieved from remote physical machine, but that virtual machine
     * does not exist (is not registered at the VirtualBox hypervisor) on the 
     * remote physical machine
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnexpectedVMStateException
     * UnexpectedVMStateException} - </strong>thrown when the virtual machine cannot be
     * accessed (e.g. corrupted configuration files) or when the virtual machine
     * is not in a required state ("running", "paused", "stuck") for shut down 
     * operation
     * <li><strong>IllegalArgumentException - </strong>thrown when the given
     * virtual machine is <code>null</code>
     * </ul>
     * </div>
     * @param virtualMachine virtual machine which should be shut down     
     */
    @Override
    public void shutDownVM(VirtualMachine virtualMachine) {
        OutputHandler outputHandler = new OutputHandler();
        
        if(virtualMachine == null){
            throw new IllegalArgumentException("A null virtual machine used for "
                    + "virtual machine shutdown operation.");
        }
        
        ConnectionManager connectionManager = new ConnectionManagerImpl();
        if(!connectionManager.isConnected(virtualMachine.getHostMachine())){
            throw new ConnectionFailureException("Virtual machine shutdown "
                    + "operation failure: Virtual machine " + virtualMachine
                    + " cannot be shut down, because its host machine "
                    + virtualMachine.getHostMachine() + " is not connected.");
        }
        
        outputHandler.printMessage("Shutting down virtual machine " + virtualMachine);
        
        NativeVBoxAPIMachine nativeVBoxAPIMachine = new NativeVBoxAPIMachine();
        try{
            nativeVBoxAPIMachine.shutDownVM(virtualMachine);
        }catch(ConnectionFailureException ex){            
            connectionManager.disconnectFrom(virtualMachine.getHostMachine());
            throw ex;
        }
        
        outputHandler.printMessage("Virtual machine " + virtualMachine + " is powered off");
    }

    /**
     * <div>
     * Method that imlements method
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualMachineManager#addPortRule(VirtualMachine, PortRule) 
     * VirtualMachineManager::addPortRule(VirtualMachine, PortRule)}.
     * </div>
     * <div>
     * If there occurs any error, then the following exceptions can be thrown:
     * <ul>
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException
     * ConnectionFailureException} - </strong>thrown when there occured any connection
     * problem (invalid physical machine attribute values, not running web server,
     * network connection not working properly or at all) when the native VirtualBox
     * manager object is being retrieved or when the physical machine is not
     * connected
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownVirtualMachineException
     * UnknownVirtualMachineException} - </strong>thrown when the given virtual machine
     * is being retrieved from remote physical machine, but that virtual machine
     * does not exist (is not registered at the VirtualBox hypervisor) on the 
     * remote physical machine
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnexpectedVMStateException
     * UnexpectedVMStateException} - </strong>thrown when the virtual machine is not
     * attached to the NAT network adapter -&gt; no port-forwarding can be done
     * <li><strong>IllegalArgumentException - </strong>thrown when the given
     * virtual machine is <code>null</code> or the given port rule is not valid
     * </ul>
     * </div>
     * @param virtualMachine virtual machine to which a new port-forwarding rule
     * should be added
     * @param portRule a new port-forwarding rule 
     */
    @Override
    public void addPortRule(VirtualMachine virtualMachine, PortRule portRule) {
        OutputHandler outputHandler = new OutputHandler();
        String operation = "new port-forwarding rule addition operation.";
        
        if(virtualMachine == null){
            throw new IllegalArgumentException("A null virtual machine used for "
                    + "new port-forwarding rule addition operation.");
        }
        
        validatePortRule(portRule,operation,virtualMachine);
        
        ConnectionManager connectionManager = new ConnectionManagerImpl();
        if(!connectionManager.isConnected(virtualMachine.getHostMachine())){
            throw new ConnectionFailureException("New port-forwarding rule addition "
                    + "operation failure: New port rule cannot be added to the "
                    + "virtual machine " + virtualMachine + ", because its host "
                    + "machine is not connected.");
        }
        
        outputHandler.printMessage("Adding new port-forwarding rule " + portRule
                + " to the virtual machine " + virtualMachine);
        
        NativeVBoxAPIMachine nativeVBoxAPIMachine = new NativeVBoxAPIMachine();
        try{
            nativeVBoxAPIMachine.addPortRule(virtualMachine, portRule);
        }catch (ConnectionFailureException ex){
            connectionManager.disconnectFrom(virtualMachine.getHostMachine());
            throw ex;
        }
        
        outputHandler.printMessage("Port-forwarding rule " + portRule + " has "
                + "been added successfully");
    }

    /**
     * <div>
     * Method that implements the method
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualMachineManager#deletePortRule(VirtualMachine, PortRule)
     * VirtualMachineManager::deletePortRule(VirtualMachine,PortRule)}.
     * </div>
     * <div>
     * <ul>
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException
     * ConnectionFailureException} - </strong>thrown when there occured any connection
     * problem (invalid physical machine attribute values, not running web server,
     * network connection not working properly or at all) when the native VirtualBox
     * manager object is being retrieved or when the physical machine is not
     * connected
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownVirtualMachineException
     * UnknownVirtualMachineException} - </strong>thrown when the given virtual machine
     * is being retrieved from remote physical machine, but that virtual machine
     * does not exist (is not registered at the VirtualBox hypervisor) on the 
     * remote physical machine
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnexpectedVMStateException
     * UnexpectedVMStateException} - </strong>thrown when the virtual machine is not
     * attached to the NAT network adapter -&gt; no port-forwarding can be done
     * <li><strong>IllegalArgumentException - </strong>thrown when the given
     * virtual machine is <code>null</code> or the given port rule is not valid
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownPortRuleException
     * UnknownPortRuleException} - </strong>thrown when there is made an attempt to
     * delete a non-existent port-forwarding rule
     * </ul>
     * </div>
     * @param virtualMachine virtual machine from which the port-forwarding rule
     * should be deleted
     * @param portRule a port-forwarding rule which should be deleted
     */
    @Override
    public void deletePortRule(VirtualMachine virtualMachine, PortRule portRule) {
        OutputHandler outputHandler = new OutputHandler();
        String operation = "port-forwarding rule deletion operation.";
        
        if(virtualMachine == null){
            throw new IllegalArgumentException("A null virtual machine used for "
                    + "port-forwarding rule deletion operation.");
        }
        
        checkPortRuleIsNull(portRule, operation);
        checkPortRuleName(portRule.getName(), operation);
        
        ConnectionManager connectionManager = new ConnectionManagerImpl();
        if(!connectionManager.isConnected(virtualMachine.getHostMachine())){
            throw new ConnectionFailureException("Port-forwarding rule deletion "
                    + "operation failure: Port rule cannot be deleted from the "
                    + "virtual machine " + virtualMachine + ", because its host "
                    + "machine is not connected.");
        }
        
        outputHandler.printMessage("Deleting port-forwarding rule \"" 
                + portRule.getName() + "\" from virtual machine " + virtualMachine);
        
        NativeVBoxAPIMachine nativeVBoxAPIMachine = new NativeVBoxAPIMachine();
        try{
            nativeVBoxAPIMachine.deletePortRule(virtualMachine, portRule.getName());
        }catch(ConnectionFailureException ex){            
            connectionManager.disconnectFrom(virtualMachine.getHostMachine());
            throw ex;
        }
        
        outputHandler.printMessage("Port-forwarding rule \"" + portRule.getName()
                + "\" deleted successfully");
    }

    /**
     * <div>
     * Method that implements the method
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualMachineManager#deleteAllPortRules(VirtualMachine)
     * VirtualMachineManager::deleteAllPortRules(VirtualMachine)}.
     * </div>
     * <div>
     * The implementation of this method uses the implementation of method
     * {@link #deletePortRule(VirtualMachine, PortRule)}.
     * If there occurs any error, then the following exceptions can be thrown:
     * <ul>
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException
     * ConnectionFailureException} - </strong>thrown when there occured any connection
     * problem (invalid physical machine attribute values, not running web server,
     * network connection not working properly or at all) when the native VirtualBox
     * manager object is being retrieved or when the physical machine is not
     * connected
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownVirtualMachineException
     * UnknownVirtualMachineException} - </strong>thrown when the given virtual machine
     * is being retrieved from remote physical machine, but that virtual machine
     * does not exist (is not registered at the VirtualBox hypervisor) on the 
     * remote physical machine
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnexpectedVMStateException
     * UnexpectedVMStateException} - </strong>thrown when the virtual machine is not
     * attached to the NAT network adapter -&gt; no port-forwarding can be done
     * <li><strong>IllegalArgumentException - </strong>thrown when the given
     * virtual machine is <code>null</code>
     * </ul>
     * </div>
     * @param virtualMachine virtual machine from which all its port-forwarding
     * rules will be removed
     */
    @Override
    public void deleteAllPortRules(VirtualMachine virtualMachine) {
        OutputHandler outputHandler = new OutputHandler();
        
        if(virtualMachine == null){
            throw new IllegalArgumentException("A null virtual machine used for "
                    + "all port-forwarding rules deletion operation.");
        }
        
        ConnectionManager connectionManager = new ConnectionManagerImpl();
        if(!connectionManager.isConnected(virtualMachine.getHostMachine())){
            throw new ConnectionFailureException("All port-forwarding rules "
                    + "deletion operation failure: Port rules cannot be deleted "
                    + "from the virtual machine " + virtualMachine + ", because "
                    + "its host machine is not connected.");
        }
        
        PrintStream origOutStream = OutputHandler.getOutputStream();
        PrintStream origErrStream = OutputHandler.getErrorOutputStream();
        List<PortRule> registeredPortRules = null;
        
        setOutputStreams(null);
        try{
            registeredPortRules = getPortRules(virtualMachine);
        }catch(UnknownVirtualMachineException ex){
            throw new UnknownVirtualMachineException("All port-forwarding rules "
                    + "deletion operation failure: There is no virtual machine "
                    + virtualMachine + " known to VirtualBox.");
        }catch(UnexpectedVMStateException ex){
            throw new UnexpectedVMStateException("All port-forwarding rules deletion "
                    + "operation failure: There cannot be deleted port-forwarding "
                    + "rules from virtual machine " + virtualMachine + ", because "
                    + "its network adapter is not attached to the required network "
                    + "adapter of type NAT.");
        }catch(ConnectionFailureException ex){
            throw new ConnectionFailureException("All port-forwarding rules deletion "
                    + "operation failure: Unable to connect to the physical machine "
                    + virtualMachine.getHostMachine() + ". Most probably there "
                    + "occured one of these problems: 1. Network connection is "
                    + "not working properly or at all / 2. The VirtualBox web "
                    + "server is not running / 3. One of the key value (IP address, "
                    + "number of web server port, username or user password) of "
                    + "the physical machine has been changed and it is incorrect "
                    + "now (used value is not the actual correct one).");
        }
        
        setOutputStreams(origOutStream, origErrStream);        
        if(!registeredPortRules.isEmpty()){
            outputHandler.printMessage("Deleting all port-forwarding rules from "
                    + "virtual machine " + virtualMachine);
            for(PortRule portRule : registeredPortRules){
                deletePortRule(virtualMachine, portRule);
            }
        }else{
            outputHandler.printMessage("There is no port-forwarding rule to be "
                    + "deleted from virtual machine " + virtualMachine);
        }
    }

    /**
     * <div>
     * Method that implements the method
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualMachineManager#getPortRules(VirtualMachine)
     * VirtualMachineManager::getPortRules(VirtualMachine)}.
     * </div>
     * <div>
     * This method returns all existing port-forwarding rules of a particular
     * virtual machine.
     * If there occurs any error, then the following exceptions can be thrown:
     * <ul>
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException
     * ConnectionFailureException} - </strong>thrown when there occured any connection
     * problem (invalid physical machine attribute values, not running web server,
     * network connection not working properly or at all) when the native VirtualBox
     * manager object is being retrieved or when the physical machine is not
     * connected
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownVirtualMachineException
     * UnknownVirtualMachineException} - </strong>thrown when the given virtual machine
     * is being retrieved from remote physical machine, but that virtual machine
     * does not exist (is not registered at the VirtualBox hypervisor) on the 
     * remote physical machine
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnexpectedVMStateException
     * UnexpectedVMStateException} - </strong>thrown when the virtual machine is not
     * attached to the NAT network adapter -&gt; no port-forwarding can be done
     * <li><strong>IllegalArgumentException - </strong>thrown when the given
     * virtual machine is <code>null</code>
     * </ul>
     * </div>
     * @param virtualMachine virtual machine from which are retrieved all its
     * registered port-forwarding rules
     * @return list of all registered port-forwarding rules of a particular virtual
     * machine
     */
    @Override
    public List<PortRule> getPortRules(VirtualMachine virtualMachine) {
        OutputHandler outputHandler = new OutputHandler();        
        
        if(virtualMachine == null){
            throw new IllegalArgumentException("A null virtual machine used for "
                    + "all port-forwarding rules retrieve operation.");
        }
        
        ConnectionManager connectionManager = new ConnectionManagerImpl();
        if(!connectionManager.isConnected(virtualMachine.getHostMachine())){
            throw new ConnectionFailureException("All port-forwarding rules retrieve "
                    + "operation failure: Port rules cannot be retrieved from "
                    + "the virtual machine " + virtualMachine + ", because its "
                    + "host machine is not connected.");
        }
        
        NativeVBoxAPIMachine nativeVBoxAPIMachine = new NativeVBoxAPIMachine();
        List<String> strPortRules;
        try{
            strPortRules = nativeVBoxAPIMachine.getPortRules(virtualMachine);
        }catch (ConnectionFailureException ex){
            connectionManager.disconnectFrom(virtualMachine.getHostMachine());
            throw ex;
        }
        
        if(!strPortRules.isEmpty()){
            return getConvertedPortRules(strPortRules);
        }
        
        return new ArrayList<>();
    }

    /**
     * <div>
     * Method that implements the method
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualMachineManager#getVMState(VirtualMachine)
     * VirtualMachineManager#getVMState(VirtualMachine)}
     * </div>
     * <div>
     * This method gets the actual state of a particular virtual machine.
     * If there occurs any error, then the following exceptions can be thrown:
     * <ul>
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException
     * ConnectionFailureException} - </strong>thrown when there occured any connection
     * problem (invalid physical machine attribute values, not running web server,
     * network connection not working properly or at all) when the native VirtualBox
     * manager object is being retrieved or when the physical machine is not
     * connected
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownVirtualMachineException
     * UnknownVirtualMachineException} - </strong>thrown when the given virtual machine
     * is being retrieved from remote physical machine, but that virtual machine
     * does not exist (is not registered at the VirtualBox hypervisor) on the 
     * remote physical machine
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnexpectedVMStateException
     * UnexpectedVMStateException} - </strong>thrown when the virtual machine cannot be
     * accessed (e.g. corrupted configuration files)
     * operation
     * <li><strong>IllegalArgumentException - </strong>thrown when the given
     * virtual machine is <code>null</code>
     * </ul>
     * </div>
     * @param virtualMachine represents the queried virtual machine
     * @return the actual state of the virtual machine as string
     */
    @Override
    public String getVMState(VirtualMachine virtualMachine) {
        OutputHandler outputHandler = new OutputHandler();
        
        if(virtualMachine == null){
            throw new IllegalArgumentException("A null virtual machine used for "
                    + "virtual machine state query operation.");
        }
        
        ConnectionManager connectionManager = new ConnectionManagerImpl();
        if(!connectionManager.isConnected(virtualMachine.getHostMachine())){
            throw new ConnectionFailureException("Virtual machine state query "
                    + "operation failures: There cannot be found out the state "
                    + "of virtual machine " + virtualMachine + ", because its "
                    + "host machine is not connected.");
        }
        
        NativeVBoxAPIMachine nativeVBoxAPIMachine = new NativeVBoxAPIMachine();
        String vmState;
        try{
            vmState = nativeVBoxAPIMachine.getVMState(virtualMachine);
        }catch(ConnectionFailureException ex){
            connectionManager.disconnectFrom(virtualMachine.getHostMachine());
            throw ex;
        }
        
        return vmState;
    }
    
    private void setOutputStreams(PrintStream printStream){
        setOutputStreams(printStream, printStream);
    }
    
    private void setOutputStreams(PrintStream stdOutput, PrintStream stdErrOutput){
        OutputHandler.setOutputStream(stdOutput);
        OutputHandler.setErrorOutputStream(stdErrOutput);        
    }
    
    private void checkPortRuleIsNull(PortRule portRule, String operation){
        if(portRule == null) {
            throw new IllegalArgumentException("A null port rule used for " + operation);
        }
    }
    
    private void checkPortRuleName(String name, String operation){
        if(name == null) {
            throw new IllegalArgumentException("Port rule with a null name used "
                    + "for " + operation);
        }
        if(name.trim().isEmpty()){
            throw new IllegalArgumentException("Port rule with an empty name used "
                    + "for " + operation);
        }
    }
    
    private void checkPortNumber(int port, String hostGuest, String operation){
        if(port <= 0){
            throw new IllegalArgumentException("Port rule with a negative or zero "
                    + hostGuest + " port number used for " + operation 
                    + " Port number can be one from the interval <1;65535>.");
        }
        if(port > 65535){
            throw new IllegalArgumentException("Port rule with a too big "
                    + hostGuest + " port number used for " + operation
                    + "Port number can be one from the interval <1;65535>.");
        }
    }
    
    private void validatePortRule(PortRule portRule, String operation,
            VirtualMachine virtualMachine){
        
        checkPortRuleIsNull(portRule, operation);
        checkPortRuleName(portRule.getName(), operation);
        checkPortNumber(portRule.getHostPort(), "host", operation);
        checkPortNumber(portRule.getGuestPort(), "guest", operation);
        
        //do the final port rule validation steps and return the result
        checkPortRuleNameAndHostPortDuplicity(portRule, virtualMachine);
    }
    
    private void checkPortRuleNameAndHostPortDuplicity(PortRule portRule, VirtualMachine virtualMachine){        
        List<PortRule> registeredPortRules = null;
        //preserve the original output streams for later return to them and do the new set up
        PrintStream origOutStream = OutputHandler.getOutputStream();
        PrintStream origErrStream = OutputHandler.getErrorOutputStream();        
        setOutputStreams(null);
        
        try{
            registeredPortRules = getPortRules(virtualMachine);
        }catch(UnknownVirtualMachineException ex){
            throw new UnknownVirtualMachineException("New port-forwarding rule "
                    + "addition operation failure: There is no virtual machine "
                    + virtualMachine + " known to VirtualBox. New port-forwarding "
                    + "rule cannot be added to a nonexistent virtual machine.");
        }catch(UnexpectedVMStateException ex){
            throw new UnexpectedVMStateException("New port-forwarding rule addition "
                    + "operation failure: There cannot be added any port-forwarding "
                    + "rule to virtual machine " + virtualMachine + ", because "
                    + "its network adapter is not attached to the required "
                    + "network adapter of type NAT.");
        }catch(ConnectionFailureException ex){
            throw new ConnectionFailureException("New port-forwarding rule addition "
                    + "operation failure: Unable to connect to the physical machine "
                    + virtualMachine.getHostMachine() + ". Most probably there "
                    + "occured one of these problems: 1. Network connection is "
                    + "not working properly or at all / 2. The VirtualBox web "
                    + "server is not running / 3. One of the key value (IP address, "
                    + "number of web server port, username or user password) of "
                    + "the physical machine has been changed and it is incorrect "
                    + "now (used value is not the actual correct one).");
        }        
        
        //set output streams to original streams
        setOutputStreams(origOutStream, origErrStream);
        
        if(!registeredPortRules.isEmpty()){
            for(PortRule regPortRule : registeredPortRules){
                if(portRule.getName().equals(regPortRule.getName())){
                    throw new IllegalArgumentException("Port rule cannot be added "
                            + "to the virtual machine " + virtualMachine + ", "
                            + "because there already exists port rule with name "
                            + "\"" + portRule.getName() + "\".");
                }
                if(portRule.getHostPort() == regPortRule.getHostPort()) {
                    throw new IllegalArgumentException("Port rule cannot be added "
                            + "to the virtual machine " + virtualMachine + ", "
                            + "because there already exists port rule using host "
                            + "port number \"" + portRule.getHostPort() + "\".");
                }
            }
        }
    }
    
    private List<PortRule> getConvertedPortRules(List<String> strPortRules){
        List<PortRule> portRules = new ArrayList<>();
        
        for(String strPortRule : strPortRules){
            String parts[] = strPortRule.split(",");
            //parts[0] = name of rule, parts[1] = protocol, parts[2] = host IP address,
            //parts[3] = host port number, parts[4] = guest IP address, parts[5] = guest port number                        
            String name = parts[0];
            ProtocolType protocol = (parts[1].equals("TCP") ? ProtocolType.TCP : ProtocolType.UDP);
            String hostIP = parts[2];
            int hostPort = Integer.parseInt(parts[3]);
            String guestIP = parts[4];
            int guestPort = Integer.parseInt(parts[5]);
            
            portRules.add(new PortRule.Builder(name, hostPort, guestPort).protocol(protocol)
                           .hostIP(hostIP).guestIP(guestIP).build());
        }
        
        return portRules;
    }
}
