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

import cz.muni.fi.virtualtoolmanager.pubapi.entities.PortRule;
import cz.muni.fi.virtualtoolmanager.pubapi.entities.VirtualMachine;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnexpectedVMStateException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownPortRuleException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownVirtualMachineException;
import cz.muni.fi.virtualtoolmanager.pubapi.managers.ConnectionManager;
import cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualMachineManager;
import cz.muni.fi.virtualtoolmanager.pubapi.types.ProtocolType;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Tomáš Šmíd
 */
public class VirtualMachineManagerImpl implements VirtualMachineManager{
    
    @Override
    public void startVM(VirtualMachine virtualMachine) {
        OutputHandler outputHandler = new OutputHandler();
        
        if(virtualMachine == null){
            outputHandler.printErrorMessage("Virtual machine starting operation "
                    + "failure: There was made an attempt to start a null virtual "
                    + "machine.");
            return;
        }
        
        ConnectionManager connectionManager = new ConnectionManagerImpl();
        if(!connectionManager.isConnected(virtualMachine.getHostMachine())){
            outputHandler.printErrorMessage("Virtual machine starting operation "
                    + "failure: Virtual machine " + virtualMachine + " cannot be "
                    + "started, because its host machine " + virtualMachine.getHostMachine()
                    + " is not connected.");
            return;
        }
        
        outputHandler.printMessage("Starting virtual machine " + virtualMachine);
        
        NativeVBoxAPIMachine nativeVBoxAPIMachine = new NativeVBoxAPIMachine();
        try{
            nativeVBoxAPIMachine.startVM(virtualMachine);
        } catch (UnknownVirtualMachineException | UnexpectedVMStateException ex) {
            outputHandler.printErrorMessage(ex.getMessage());
            return;
        }catch(ConnectionFailureException ex){
            connectionManager.disconnectFrom(virtualMachine.getHostMachine());
            outputHandler.printErrorMessage(ex.getMessage());
            return;
        }
        
        outputHandler.printMessage("Virtual machine " + virtualMachine + " is running");
    }

    @Override
    public void shutDownVM(VirtualMachine virtualMachine) {
        OutputHandler outputHandler = new OutputHandler();
        
        if(virtualMachine == null){
            outputHandler.printErrorMessage("Virtual machine shutdown operation "
                    + "failure: There was made an attempt to shut down a null virtual "
                    + "machine.");
            return;
        }
        
        ConnectionManager connectionManager = new ConnectionManagerImpl();
        if(!connectionManager.isConnected(virtualMachine.getHostMachine())){
            outputHandler.printErrorMessage("Virtual machine shutdown operation "
                    + "failure: Virtual machine " + virtualMachine + " cannot be "
                    + "shut down, because its host machine " + virtualMachine.getHostMachine()
                    + " is not connected.");
            return;
        }
        
        outputHandler.printMessage("Shutting down virtual machine " + virtualMachine);
        
        NativeVBoxAPIMachine nativeVBoxAPIMachine = new NativeVBoxAPIMachine();
        try{
            nativeVBoxAPIMachine.shutDownVM(virtualMachine);
        } catch (UnknownVirtualMachineException | UnexpectedVMStateException ex) {
            outputHandler.printErrorMessage(ex.getMessage());
            return;
        }catch(ConnectionFailureException ex){
            connectionManager.disconnectFrom(virtualMachine.getHostMachine());
            outputHandler.printErrorMessage(ex.getMessage());
            return;
        }
        
        outputHandler.printMessage("Virtual machine " + virtualMachine + " is powered off");
    }

    @Override
    public void addPortRule(VirtualMachine virtualMachine, PortRule portRule) {
        OutputHandler outputHandler = new OutputHandler();
        ConnectionManager connectionManager = new ConnectionManagerImpl();
        
        if(virtualMachine == null){
            outputHandler.printErrorMessage("New port-forwarding rule addition operation "
                    + "failure: There was made an attempt to add a new port-forwarding "
                    + "rule to a null virtual machine.");
            return;
        }
        
        if(!isPortRuleValid(portRule, virtualMachine)){
            //an error message was already printed, so just exit
            return;
        }        
        
        if(!connectionManager.isConnected(virtualMachine.getHostMachine())){
            outputHandler.printErrorMessage("New port-forwarding rule addition operation "
                    + "failure: New port rule cannot be added to the virtual machine "
                    + virtualMachine + ", because its host machine " + virtualMachine.getHostMachine()
                    + " is not connected.");
            return;
        }
        
        outputHandler.printMessage("Adding new port-forwarding rule " + portRule + " to the "
                + "virtual machine " + virtualMachine);
        
        NativeVBoxAPIMachine nativeVBoxAPIMachine = new NativeVBoxAPIMachine();
        try{
            nativeVBoxAPIMachine.addPortRule(virtualMachine, portRule);
        } catch (UnknownVirtualMachineException | UnexpectedVMStateException ex) {
            outputHandler.printErrorMessage(ex.getMessage());
            return;
        } catch (ConnectionFailureException ex){
            connectionManager.disconnectFrom(virtualMachine.getHostMachine());
            outputHandler.printErrorMessage(ex.getMessage());
            return;
        }
        
        outputHandler.printMessage("Port-forwarding rule " + portRule + " has been "
                + "added successfully");
    }

    @Override
    public void deletePortRule(VirtualMachine virtualMachine, PortRule portRule) {
        OutputHandler outputHandler = new OutputHandler();
        
        if(virtualMachine == null){
            outputHandler.printErrorMessage("Port-forwarding rule deletion operation "
                    + "failure: There was made an attempt to delete a port-forwarding "
                    + "rule from a null virtual machine.");
            return;
        }
        
        if(portRule == null){
            outputHandler.printErrorMessage("Port-forwarding rule deletion operation "
                    + "failure: There was made an attempt to delete a null port-forwarding rule.");
            return;
        }        
        if(portRule.getName() == null){
            outputHandler.printErrorMessage("Port-forwarding rule deletion operation "
                    + "failure: There was made an attempt to delete a port-forwarding "
                    + "rule with a null name.");
            return;
        }        
        if(portRule.getName().trim().isEmpty()){
            outputHandler.printErrorMessage("Port-forwarding rule deletion operation "
                    + "failure: There was made an attempt to delete a port-forwarding "
                    + "rule with an empty name.");
            return;
        }
        
        ConnectionManager connectionManager = new ConnectionManagerImpl();
        if(!connectionManager.isConnected(virtualMachine.getHostMachine())){
            outputHandler.printErrorMessage("Port-forwarding rule deletion operation "
                    + "failure: Port rule cannot be deleted from the virtual machine "
                    + virtualMachine + ", because its host machine " + virtualMachine.getHostMachine()
                    + " is not connected.");
            return;
        }
        
        outputHandler.printMessage("Deleting port-forwarding rule \"" + portRule.getName() + "\" "
                + "from virtual machine " + virtualMachine);
        
        NativeVBoxAPIMachine nativeVBoxAPIMachine = new NativeVBoxAPIMachine();
        try{
            nativeVBoxAPIMachine.deletePortRule(virtualMachine, portRule.getName());
        }catch(UnknownVirtualMachineException | UnexpectedVMStateException | UnknownPortRuleException ex){
            outputHandler.printErrorMessage(ex.getMessage());
            return;
        }catch(ConnectionFailureException ex){
            connectionManager.disconnectFrom(virtualMachine.getHostMachine());
            outputHandler.printErrorMessage(ex.getMessage());
            return;
        }
        
        outputHandler.printMessage("Port-forwarding rule \"" + portRule.getName() + "\" "
                + "deleted successfully");
    }

    @Override
    public void deleteAllPortRules(VirtualMachine virtualMachine) {
        OutputHandler outputHandler = new OutputHandler();
        
        if(virtualMachine == null){
            outputHandler.printErrorMessage("All port-forwarding rules deletion operation "
                    + "failure: There was made an attempt to delete all port rules from "
                    + "a null virtual machine.");
            return;
        }
        
        ConnectionManager connectionManager = new ConnectionManagerImpl();
        if(!connectionManager.isConnected(virtualMachine.getHostMachine())){
            outputHandler.printErrorMessage("All port-forwarding rules deletion operation "
                    + "failure: Port rules cannot be deleted from the virtual machine "
                    + virtualMachine + ", because its host machine " + virtualMachine.getHostMachine()
                    + " is not connected.");
            return;
        }
        
        PrintStream origOutStream = OutputHandler.getOutputStream();
        PrintStream origErrStream = OutputHandler.getErrorOutputStream();
        final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        setOutputStreams(null, new PrintStream(errContent));
        
        List<PortRule> portRules = getPortRules(virtualMachine);
        
        setOutputStreams(origOutStream, origErrStream);        
        if(!portRules.isEmpty()){
            outputHandler.printMessage("Deleting all port-forwarding rules from virtual "
                    + "machine " + virtualMachine);
            for(PortRule portRule : portRules){
                deletePortRule(virtualMachine, portRule);
            }
        }else{
            String[] messages = {"All port-forwarding rules deletion operation failure: Unable to connect to the physical machine "
                                    + virtualMachine.getHostMachine() + ". Most probably there occured one of these problems: 1. Network connection is not working "
                                    + "properly or at all / 2. The VirtualBox web server is not running / 3. One of the key value (IP address, number of web server port, "
                                    + "username or user password) of the physical machine has been changed and it is incorrect now (used value is not the actual correct one).",
                                 "All port-forwarding rules deletion operation failure: There is no virtual machine " + virtualMachine + " on "
                                    + "physical machine " + virtualMachine.getHostMachine() + " known to VirtualBox. Port-forwarding rules cannot be deleted from a nonexistent virtual machine.",
                                 "All port-forwarding rules deletion operation failure: There cannot be deleted port-forwarding rules from virtual "
                                    + "machine " + virtualMachine + ", because its network adapter is not attached to the required network adapter of type NAT."};
            if(isErrorOutputStreamEmpty(errContent.toString(), messages)){
                outputHandler.printMessage("There is no port-forwarding rule to be deleted "
                        + "from virtual machine " + virtualMachine);
            }
        }
    }

    @Override
    public List<PortRule> getPortRules(VirtualMachine virtualMachine) {
        OutputHandler outputHandler = new OutputHandler();        
        
        if(virtualMachine == null){
            outputHandler.printErrorMessage("All port-forwarding rules retrieve operation "
                    + "failure: There was made an attempt to get all port-forwarding rules "
                    + "from a null virtual machine.");
            return new ArrayList<>();
        }
        
        ConnectionManager connectionManager = new ConnectionManagerImpl();
        if(!connectionManager.isConnected(virtualMachine.getHostMachine())){
            outputHandler.printErrorMessage("All port-forwarding rules retrieve operation "
                    + "failure: Port rules cannot be retrieved from the virtual machine "
                    + virtualMachine + ", because its host machine " + virtualMachine.getHostMachine()
                    + " is not connected.");
            return new ArrayList<>();
        }
        
        NativeVBoxAPIMachine nativeVBoxAPIMachine = new NativeVBoxAPIMachine();
        List<String> strPortRules;
        try{
            strPortRules = nativeVBoxAPIMachine.getPortRules(virtualMachine);
        } catch (UnknownVirtualMachineException | UnexpectedVMStateException ex) {
            outputHandler.printErrorMessage(ex.getMessage());
            return new ArrayList<>();
        } catch (ConnectionFailureException ex){
            connectionManager.disconnectFrom(virtualMachine.getHostMachine());
            outputHandler.printErrorMessage(ex.getMessage());
            return new ArrayList<>();
        }
        
        if(!strPortRules.isEmpty()){
            return getConvertedPortRules(strPortRules);
        }
        
        return new ArrayList<>();
    }

    @Override
    public String getVMState(VirtualMachine virtualMachine) {
        OutputHandler outputHandler = new OutputHandler();
        
        if(virtualMachine == null){
            outputHandler.printErrorMessage("Virtual machine state finding out operation "
                    + "failures: There was made an attempt to find out the state of a null "
                    + "virtual machine.");
            return null;
        }
        
        ConnectionManager connectionManager = new ConnectionManagerImpl();
        if(!connectionManager.isConnected(virtualMachine.getHostMachine())){
            outputHandler.printErrorMessage("Virtual machine state finding out operation "
                    + "failures: There cannot be found out the state of virtual machine " 
                    + virtualMachine + ", because its host machine " + virtualMachine.getHostMachine()
                    + " is not connected.");
            return null;
        }
        
        NativeVBoxAPIMachine nativeVBoxAPIMachine = new NativeVBoxAPIMachine();
        String vmState;
        try{
            vmState = nativeVBoxAPIMachine.getVMState(virtualMachine);
        }catch(UnknownVirtualMachineException | UnexpectedVMStateException ex){
            outputHandler.printErrorMessage(ex.getMessage());
            return null;
        }catch(ConnectionFailureException ex){
            connectionManager.disconnectFrom(virtualMachine.getHostMachine());
            outputHandler.printErrorMessage(ex.getMessage());
            return null;
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
    
    private boolean isPortRuleValid(PortRule portRule, VirtualMachine virtualMachine){
        OutputHandler outputHandler = new OutputHandler();
        
        if(portRule == null) {
            outputHandler.printErrorMessage("New port-forwarding rule addition operation "
                    + "failure: There was made an attempt to add a null port rule to the "
                    + "virtual machine " + virtualMachine + ".");
            return false;
        }
        if(portRule.getName() == null) {
            outputHandler.printErrorMessage("New port-forwarding rule addition operation "
                    + "failure: There was made an attempt to add a new port rule with a null "
                    + "name to the virtual machine " + virtualMachine + ".");
            return false;
        }
        if(portRule.getName().trim().isEmpty()){
            outputHandler.printErrorMessage("New port-forwarding rule addition operation "
                    + "failure: There was made an attempt to add a new port rule with an empty "
                    + "name to the virtual machine " + virtualMachine + ".");
            return false;
        }
        if(portRule.getHostPort() <= 0){
            outputHandler.printErrorMessage("New port-forwarding rule addition operation "
                    + "failure: There was made an attempt to add a new port rule with a "
                    + "negative or zero host port number to the virtual machine " + virtualMachine
                    + ". Port number can be one from the interval <1;65535>.");
            return false;
        }
        if(portRule.getHostPort() > 65535){
            outputHandler.printErrorMessage("New port-forwarding rule addition operation "
                    + "failure: There was made an attempt to add a new port rule with a "
                    + "too big host port number to the virtual machine " + virtualMachine
                    + ". Port number can be one from the interval <1;65535>.");
            return false;
        }
        if(portRule.getGuestPort() <= 0){
            outputHandler.printErrorMessage("New port-forwarding rule addition operation "
                    + "failure: There was made an attempt to add a new port rule with a "
                    + "negative or zero guest port number to the virtual machine " + virtualMachine
                    + ". Port number can be one from the interval <1;65535>.");
            return false;
        }
        if(portRule.getGuestPort() > 65535){
            outputHandler.printErrorMessage("New port-forwarding rule addition operation "
                    + "failure: There was made an attempt to add a new port rule with a "
                    + "too big guest port number to the virtual machine " + virtualMachine
                    + ". Port number can be one from the interval <1;65535>.");
            return false;
        }
        
        //do the final port rule validation steps and return the result
        return checkPortRuleNameAndHostPortDuplicity(portRule, virtualMachine);
    }
    
    private boolean checkPortRuleNameAndHostPortDuplicity(PortRule portRule, VirtualMachine virtualMachine){
        OutputHandler outputHandler = new OutputHandler();
        //preserve the original output streams for later return to them and do the new set up
        PrintStream origOutStream = OutputHandler.getOutputStream();
        PrintStream origErrStream = OutputHandler.getErrorOutputStream();
        final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        setOutputStreams(null, new PrintStream(errContent));
        
        List<PortRule> registeredPortRules = getPortRules(virtualMachine);
        
        //set output streams to original streams
        setOutputStreams(origOutStream, origErrStream);
        
        if(!registeredPortRules.isEmpty()){
            for(PortRule regPortRule : registeredPortRules){
                if(portRule.getName().equals(regPortRule.getName())) {
                    outputHandler.printErrorMessage("New port-forwarding rule addition operation "
                            + "failure: Port rule cannot be added to the virtual machine, because "
                            + "there already exists port rule with name = " + portRule.getName()
                            + " for virtual machine " + virtualMachine + " on physical machine "
                            + virtualMachine.getHostMachine() + ".");
                    return false;
                }
                if(portRule.getHostPort() == regPortRule.getHostPort()) {
                    outputHandler.printErrorMessage("New port-forwarding rule addition operation "
                            + "failure: Port rule cannot be added to the virtual machine, because "
                            + "there already exists port rule using host port number = " + portRule.getHostPort()
                            + " for virtual machine " + virtualMachine + " on physical machine "
                            + virtualMachine.getHostMachine() + ".");
                    return false;
                }
            }
        }else{
            String[] messages = {"New port-forwarding rule addition operation failure: Unable to connect to the physical machine "
                                    + virtualMachine.getHostMachine() + ". Most probably there occured one of these problems: 1. Network connection is not working "
                                    + "properly or at all / 2. The VirtualBox web server is not running / 3. One of the key value (IP address, number of web server port, "
                                    + "username or user password) of the physical machine has been changed and it is incorrect now (used value is not the actual correct one).",
                                 "New port-forwarding rule addition operation failure: There is no virtual machine " + virtualMachine + " on "
                                    + "physical machine " + virtualMachine.getHostMachine() + " known to VirtualBox. New port-forwarding rule cannot be added to a nonexistent virtual machine.",
                                 "New port-forwarding rule addition operation failure: There cannot be added any port-forwarding rule to virtual "
                                    + "machine " + virtualMachine + ", because its network adapter is not attached to the required network adapter of type NAT."};
            return isErrorOutputStreamEmpty(errContent.toString(), messages);
        }
        
        return true;
    }
    
    private boolean isErrorOutputStreamEmpty(String strErrContent, String[] messages){
        OutputHandler outputHandler = new OutputHandler();
        
        if(!strErrContent.trim().isEmpty()){
            if(strErrContent.startsWith("Connection operation failure")){
                outputHandler.printErrorMessage(messages[0]);
                return false;

            }
            if(strErrContent.contains("There is no virtual machine")){
                outputHandler.printErrorMessage(messages[1]);
                return false;
            }
            if(strErrContent.contains("network adapter")){
                outputHandler.printErrorMessage(messages[2]);
                return false;
            }
        }
        
        return true;
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
