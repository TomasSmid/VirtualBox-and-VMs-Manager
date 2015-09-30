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
import cz.muni.fi.virtualtoolmanager.pubapi.types.ProtocolType;
import java.util.List;
import org.virtualbox_4_3.IConsole;
import org.virtualbox_4_3.IMachine;
import org.virtualbox_4_3.INATEngine;
import org.virtualbox_4_3.INetworkAdapter;
import org.virtualbox_4_3.IProgress;
import org.virtualbox_4_3.ISession;
import org.virtualbox_4_3.IVirtualBox;
import org.virtualbox_4_3.LockType;
import org.virtualbox_4_3.MachineState;
import org.virtualbox_4_3.NATProtocol;
import org.virtualbox_4_3.NetworkAttachmentType;
import org.virtualbox_4_3.SessionState;
import org.virtualbox_4_3.VBoxException;
import org.virtualbox_4_3.VirtualBoxManager;



/**
 *
 * @author Tomáš Šmíd
 */
class NativeVBoxAPIMachine {
    
    /**
     * 
     * 
     * @param virtualMachine
     * @throws ConnectionFailureException
     * @throws UnknownVirtualMachineException
     * @throws UnexpectedVMStateException 
     */
    public void startVM(VirtualMachine virtualMachine) throws ConnectionFailureException,
                                                              UnknownVirtualMachineException,
                                                              UnexpectedVMStateException { 
        VirtualBoxManager virtualBoxManager = VirtualBoxManager.createInstance(null);
        IMachine vboxMachine = null;
        int[] errMsgNum = {0, 1};
        
        try{
            //get the required VM from VirtualBox
            vboxMachine = getVBoxMachine(virtualBoxManager, virtualMachine, errMsgNum);
        }catch (ConnectionFailureException | UnknownVirtualMachineException ex){
            //just do the clean up after performed operation(s)
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw ex;
        }
        
        //check the VM is accessible - its source files are not missing nor corrupted
        //- and thus there can be performed any operation with it (if the VM is not accessible, then
        //the VM can be just unregistered (removed from the list of all known VMs to VirtualBox))
        if(!vboxMachine.getAccessible()){
            //just do the clean up after performed operation(s)
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw new UnexpectedVMStateException(getErrorMessage(2, virtualMachine)
                                    + vboxMachine.getAccessError().getText());
        }
        
        //check the VM has not been started yet
        switch(vboxMachine.getState()){
            case Running:
            case Paused : throw new UnexpectedVMStateException(getErrorMessage(3, virtualMachine));
            default     : break;
        }
        
        //all conditions for starting VM are met - start the VM
        ISession session = virtualBoxManager.getSessionObject();
        try{
            IProgress progress = vboxMachine.launchVMProcess(session, "gui", "");
            while(!progress.getCompleted()){
                virtualBoxManager.waitForEvents(0l);
                progress.waitForCompletion(200);
            }
            while(vboxMachine.getState() != MachineState.Running){
                //just loop until that is true
            }
        }catch(VBoxException ex){
            //just do the clean up after performed operation(s)
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            //VM cannot be started now, because there is another process which is using the VM now
            //(that process locked the VM for itself)
            throw new UnexpectedVMStateException(getErrorMessage(4, virtualMachine));
        }
        
        //operation finished successfully (VM is running now), now release the VM for another processes
        //and do the after operation(s) clean up
        session.unlockMachine();
        virtualBoxManager.disconnect();
        virtualBoxManager.cleanup();
    }
    
    /**
     * 
     * 
     * @param virtualMachine
     * @throws ConnectionFailureException
     * @throws UnknownVirtualMachineException
     * @throws UnexpectedVMStateException 
     */
    public void shutDownVM(VirtualMachine virtualMachine) throws ConnectionFailureException,
                                                                 UnknownVirtualMachineException,
                                                                 UnexpectedVMStateException {
        VirtualBoxManager virtualBoxManager = VirtualBoxManager.createInstance(null);
        IMachine vboxMachine = null;
        int[] errMsgNum = {5, 6};
        
        try{
            //get the required VM from VirtualBox
            vboxMachine = getVBoxMachine(virtualBoxManager, virtualMachine, errMsgNum);
        }catch (ConnectionFailureException | UnknownVirtualMachineException ex){
            //just do the clean up after performed operation(s)
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw ex;
        }
        
        //check the VM is accessible - its source files are not missing nor corrupted
        //- and thus there can be performed any operation with it (if the VM is not accessible, then
        //the VM can be just unregistered (removed from the list of all known VMs to VirtualBox))
        if(!vboxMachine.getAccessible()){
            //just do the clean up after performed operation(s)
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw new UnexpectedVMStateException(getErrorMessage(7, virtualMachine)
                                    + vboxMachine.getAccessError().getText());
        }
        
        //check the VM is in a required state for VM shutdown operation
        switch(vboxMachine.getState()){
            case Running:
            case Paused : 
            case Stuck  : break;
            default     : throw new UnexpectedVMStateException(getErrorMessage(8, virtualMachine));
        }
        
        //all conditions for VM shutdown are met - VM can be shut down
        ISession session = virtualBoxManager.getSessionObject();
        //process which ensures VM shutdown must firstly lock the VM for itself
        //(not using the exclusive lock, just shared - if there exists another process which locked VM for itself)
        vboxMachine.lockMachine(session, LockType.Shared);
        IConsole console = session.getConsole();
        //shut the VM down
        IProgress progress = console.powerDown();
        while(!progress.getCompleted()){
            virtualBoxManager.waitForEvents(0l);
            progress.waitForCompletion(200);
        }
        //release the VM for another processes and wait while the VM is definitively powered down and unlocked
        session.unlockMachine();
        while(vboxMachine.getState() != MachineState.PoweredOff){
            
        }
        while(session.getState() != SessionState.Unlocked){
            
        }
        
        //operation finished successfully - do the clean up after performed operation(s)
        virtualBoxManager.disconnect();
        virtualBoxManager.cleanup();
    }
    
    /**
     * 
     * 
     * @param virtualMachine
     * @param portRule
     * @throws ConnectionFailureException
     * @throws UnknownVirtualMachineException
     * @throws UnexpectedVMStateException 
     */
    public void addPortRule(VirtualMachine virtualMachine, PortRule portRule) throws ConnectionFailureException,
                                                                                     UnknownVirtualMachineException,
                                                                                     UnexpectedVMStateException {
        VirtualBoxManager virtualBoxManager = VirtualBoxManager.createInstance(null);
        IMachine vboxMachine = null;
        int[] errMsgNum = {9, 10};
        
        try{
            //get the required VM from VirtualBox
            vboxMachine = getVBoxMachine(virtualBoxManager, virtualMachine, errMsgNum);
        }catch (ConnectionFailureException | UnknownVirtualMachineException ex){
            //just do the clean up after performed operation(s)
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw ex;
        }
        
        //get network adapter of VM and check that it is of a required type
        INetworkAdapter netAdapter = vboxMachine.getNetworkAdapter(0L);
        if(netAdapter.getAttachmentType() != NetworkAttachmentType.NAT){
            //just do the clean up after performed operation(s)
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw new UnexpectedVMStateException(getErrorMessage(11, virtualMachine));
        }
        
        //get the NAT engine thanks to it there will be possible to add a new port-forwarding rule to VM
        INATEngine natEngine = netAdapter.getNATEngine();
        NATProtocol natp = (portRule.getProtocol() == ProtocolType.TCP ? NATProtocol.TCP : NATProtocol.UDP);
        String hostIP = (portRule.getHostIP() == null ? "" : portRule.getHostIP());
        String guestIP = (portRule.getGuestIP() == null ? "" : portRule.getGuestIP());
        
        //add new port-forwarding rule to the VM
        natEngine.addRedirect(portRule.getName(), natp, hostIP, portRule.getHostPort(),
                              guestIP, portRule.getGuestPort());
        
        //operation finished successfully - do the clean up after performed operation(s)
        virtualBoxManager.disconnect();
        virtualBoxManager.cleanup();
    }
    
    
    public void deletePortRule(VirtualMachine virtualMachine, String ruleName) throws ConnectionFailureException,
                                                                                      UnknownVirtualMachineException,
                                                                                      UnknownPortRuleException,
                                                                                      UnexpectedVMStateException {
        VirtualBoxManager virtualBoxManager = VirtualBoxManager.createInstance(null);
        IMachine vboxMachine = null;
        int[] errMsgNum = {12, 13};
        
        try{
            //get the required VM from VirtualBox
            vboxMachine = getVBoxMachine(virtualBoxManager, virtualMachine, errMsgNum);
        }catch (ConnectionFailureException | UnknownVirtualMachineException ex){
            //just do the clean up after performed operation(s)
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw ex;
        }
        
        //get network adapter of VM and check if it is of a required type
        INetworkAdapter netAdapter = vboxMachine.getNetworkAdapter(0L);
        if(netAdapter.getAttachmentType() != NetworkAttachmentType.NAT){
            //just do the clean up after performed operation(s)
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw new UnexpectedVMStateException(getErrorMessage(14, virtualMachine));
        }
        //get NAT engine thanks to it there can be a port-forwarding rule deleted
        INATEngine natEngine = netAdapter.getNATEngine();
        
        try{
            //delete the port-forwarding rule, if there is not port rule with the given name, VBoxException is invoked
            natEngine.removeRedirect(ruleName);
        }catch(VBoxException ex){
            //just do the clean up after performed operation(s)
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw new UnknownPortRuleException(getErrorMessage(15, virtualMachine) + ruleName
                                            + "Nonexistent port-forwarding rule cannot be deleted.");
        }
        
        //operation finished successfully - do the clean up after performed operation(s)
        virtualBoxManager.disconnect();
        virtualBoxManager.cleanup();
    }
    
    public List<String> getPortRules(VirtualMachine virtualMachine) throws ConnectionFailureException,
                                                                           UnknownVirtualMachineException,
                                                                           UnexpectedVMStateException {
        VirtualBoxManager virtualBoxManager = VirtualBoxManager.createInstance(null);
        IMachine vboxMachine = null;
        int[] errMsgNum = {16, 17};
        
        try{
            //get the required VM from VirtualBox
            vboxMachine = getVBoxMachine(virtualBoxManager, virtualMachine, errMsgNum);
        }catch (ConnectionFailureException | UnknownVirtualMachineException ex){
            //just do the clean up after performed operation(s)
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw ex;
        }
        
        //get network adapter of VM and check if it is of a required type
        INetworkAdapter netAdapter = vboxMachine.getNetworkAdapter(0L);
        if(netAdapter.getAttachmentType() != NetworkAttachmentType.NAT){
            throw new UnexpectedVMStateException(getErrorMessage(18, virtualMachine));
        }
        
        //get NAT engine thanks to it there can be retrieved all port-forwarding rules from VM
        INATEngine natEngine = netAdapter.getNATEngine();
        //get all port-forwarding rules
        List<String> redirects = natEngine.getRedirects();
        
        //operation finished successfully - do the clean up after performed operation(s)
        virtualBoxManager.disconnect();
        virtualBoxManager.cleanup();
        
        return redirects;
    }
    
    public String getVMState(VirtualMachine virtualMachine) throws ConnectionFailureException,
                                                                   UnknownVirtualMachineException,
                                                                   UnexpectedVMStateException{
        VirtualBoxManager virtualBoxManager = VirtualBoxManager.createInstance(null);
        IMachine vboxMachine = null;
        int[] errMsgNum = {19, 20};
        
        try{
            //get the required VM from VirtualBox
            vboxMachine = getVBoxMachine(virtualBoxManager, virtualMachine, errMsgNum);
        }catch (ConnectionFailureException | UnknownVirtualMachineException ex){
            //just do the clean up after performed operation(s)
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw ex;
        }
        
        //check the VM is accessible - its source files are not missing nor corrupted
        //- and thus there can be performed any operation with it (if the VM is not accessible, then
        //the VM can be just unregistered (removed from the list of all known VMs to VirtualBox))
        if(!vboxMachine.getAccessible()){
            //just do the clean up after performed operation(s)
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw new UnexpectedVMStateException(getErrorMessage(21, virtualMachine)
                                    + vboxMachine.getAccessError().getText());
        }
        
        //get state of the VM
        String vmState = vboxMachine.getState().name();
        
        //operation finished successfully - do the clean up after performed operation(s)
        virtualBoxManager.disconnect();
        virtualBoxManager.cleanup();
        
        return vmState;
    }
    
    private String getErrorMessage(int index, VirtualMachine virtualMachine){
        String[] errMessages = /* 00 */{"Connection operation failure while trying to start the virtual machine " + virtualMachine + ": Unable to connect to the physical machine " + virtualMachine.getHostMachine() + ". Most probably there occured one of these problems: 1. Network connection is not working properly or at all / 2. The VirtualBox web "
                                        + " server is not running / 3. One of the key value (IP address, number of web server port, username or user password) of the physical machine has been changed and it is incorrect now (used value is not the actual correct one).",
                               /* 01 */ "Virtual machine starting operation failure: There is no virtual machine " + virtualMachine + " on physical machine " + virtualMachine.getHostMachine() + " known to VirtualBox. Nonexistent virtual machine cannot be started.",
                               /* 02 */ "Virtual machine starting operation failure: Virtual machine " + virtualMachine + " cannot be started, because it is not accessible -> ",
                               /* 03 */ "Virtual machine starting operation failure: Virtual machine " + virtualMachine + " cannot be started, because it is already running.",
                               /* 04 */ "Virtual machine starting operation failure: Virtual machine " + virtualMachine + " cannot be started now, because there exists another process which is working with this virtual machine now (that process has locked the virtual machine just for itself).",
                               /* 05 */ "Connection operation failure while trying to shut down the virtual machine " + virtualMachine + ": Unable to connect to the physical machine " + virtualMachine.getHostMachine() + ". Most probably there occured one of these problems: 1. Network connection is not working properly or at all / 2. The VirtualBox web "
                                        + "server is not running / 3. One of the key value (IP address, number of web server port, username or user password) of the physical machine has been changed and it is incorrect now (used value is not the actual correct one).",
                               /* 06 */ "Virtual machine shutdown operation failure: There is no virtual machine " + virtualMachine + " on physical machine " + virtualMachine.getHostMachine() + " known to VirtualBox. Nonexistent virtual machine cannot be shut down.",
                               /* 07 */ "Virtual machine shutdown operation failure: Virtual machine " + virtualMachine + " cannot be shut down, because it is not accessible -> ",
                               /* 08 */ "Virtual machine shutdown operation failure: Virtual machine " + virtualMachine + " cannot be shut down, because it is not in any of the required states (\"Running\", \"Paused\", \"Stuck\") for virtual machine shutdown operation.",
                               /* 09 */ "Connection operation failure while trying to add new port-forwarding rule to the virtual machine " + virtualMachine + ": Unable to connect to the physical machine " + virtualMachine.getHostMachine() + ". Most probably there occured one of these problems: 1. Network connection is not working properly or at all / 2. The VirtualBox web "
                                        + "server is not running / 3. One of the key value (IP address, number of web server port, username or user password) of the physical machine has been changed and it is incorrect now (used value is not the actual correct one).",
                               /* 10 */ "New port-forwarding rule addition operation failure: There is no virtual machine " + virtualMachine + " on physical machine " + virtualMachine.getHostMachine() + " known to VirtualBox. New port-forwarding rule cannot be added to a nonexistent virtual machine.",
                               /* 11 */ "New port-forwarding rule addition operation failure: There cannot be added any port-forwarding rule to virtual machine " + virtualMachine + ", because its network adapter is not attached to the required network adapter of type NAT.",
                               /* 12 */ "Connection operation failure while trying to delete port-forwarding rule from the virtual machine " + virtualMachine + ": Unable to connect to the physical machine " + virtualMachine.getHostMachine() + ". Most probably there occured one of these problems: 1. Network connection is not working properly or at all / 2. The VirtualBox web "
                                        + "server is not running / 3. One of the key value (IP address, number of web server port, username or user password) of the physical machine has been changed and it is incorrect now (used value is not the actual correct one).",
                               /* 13 */ "Port-forwarding rule deletion operation failure: There is no virtual machine " + virtualMachine + " on physical machine " + virtualMachine.getHostMachine() + " known to VirtualBox. Port-forwarding rule cannot be deleted from a nonexistent virtual machine.",
                               /* 14 */ "Port-forwarding rule deletion operation failure: There cannot be deleted any port-forwarding rule from virtual machine " + virtualMachine + ", because its network adapter is not attached to the required network adapter of type NAT (there cannot exist any port-forwarding rule on this virtual machine).",
                               /* 15 */ "Port-forwarding rule deletion operation failure: On virtual machine " + virtualMachine + " there is no port-forwarding rule with name = ",
                               /* 16 */ "Connection operation failure while trying to get all port-forwarding rules from the virtual machine " + virtualMachine + ": Unable to connect to the physical machine " + virtualMachine.getHostMachine() + ". Most probably there occured one of these problems: 1. Network connection is not working properly or at all / 2. The VirtualBox web "
                                        + "server is not running / 3. One of the key value (IP address, number of web server port, username or user password) of the physical machine has been changed and it is incorrect now (used value is not the actual correct one).",
                               /* 17 */ "All port-forwarding rules retrieve operation failure: There is no virtual machine " + virtualMachine + " on physical machine " + virtualMachine.getHostMachine() + " known to VirtualBox. Port-forwarding rules cannot be retrieved from a nonexistent virtual machine.",
                               /* 18 */ "All port-forwarding rules retrieve operation failure: There cannot be retrieved port-forwarding rules from virtual machine " + virtualMachine + ", because its network adapter is not attached to the required network adapter of type NAT (there cannot exist any port-forwarding rule on this virtual machine).",
                               /* 19 */ "Connection operation failure while trying to find out the state of the virtual machine " + virtualMachine + ": Unable to connect to the physical machine " + virtualMachine.getHostMachine() + ". Most probably there occured one of these problems: 1. Network connection is not working properly or at all / 2. The VirtualBox web "
                                        + "server is not running / 3. One of the key value (IP address, number of web server port, username or user password) of the physical machine has been changed and it is incorrect now (used value is not the actual correct one).",
                               /* 20 */ "Virtual machine state finding out operation failures: There is no virtual machine " + virtualMachine + " on physical machine " + virtualMachine.getHostMachine() + " known to VirtualBox. There cannot be found out a state of nonexistent virtual machine.",
                               /* 21 */ "Virtual machine state finding out operation failure: There cannot be found out the state of the virtual machine " + virtualMachine + ", because it is not accessible -> "};
        
        return errMessages[index];
    }
    
    private IMachine getVBoxMachine(VirtualBoxManager virtualBoxManager, VirtualMachine virtualMachine,
                                    int[] errMsgNum) throws ConnectionFailureException,
                                                            UnknownVirtualMachineException{
        String url = "http://" + virtualMachine.getHostMachine().getAddressIP()
                + ":" + virtualMachine.getHostMachine().getPortOfVTWebServer();
        String username = virtualMachine.getHostMachine().getUsername();
        String userPassword = virtualMachine.getHostMachine().getUserPassword();
        
        //connect to the VirtualBox web server which should be running on physical 
        //machine virtualMachine.getHostMachine()
        try{
            virtualBoxManager.connect(url, username, userPassword);
        }catch(VBoxException ex){
            //there occured any connection problem, the required operation cannot be finished successfully
            throw new ConnectionFailureException(getErrorMessage(errMsgNum[0], virtualMachine));
        }
        
        //get the instance of VirtualBox in order to retrieve the required VM
        IVirtualBox vbox = virtualBoxManager.getVBox();
        IMachine vboxMachine = null;
        try{
            //get the required virtual machine from VirtualBox
            vboxMachine = vbox.findMachine(virtualMachine.getId().toString());
        }catch(VBoxException ex){
            //the required VM is not present on the PM virtualMachine.getHostMachine()
            throw new UnknownVirtualMachineException(getErrorMessage(errMsgNum[1], virtualMachine));
        }
        
        return vboxMachine;
    }
    
    /*public String getVMState(VirtualMachine virtualMachine) {
        throw new UnsupportedOperationException("Unsupported operation");
    }*/
    
    /*private void checkVMIsNotNull(VirtualMachine vm, String errMsg){
        if(vm == null){
            throw new IllegalArgumentException(errMsg);
        }
    }
    
    private void checkPMIsNotNull(PhysicalMachine pm, String errMsg){
        if(pm == null){
            throw new IllegalArgumentException(errMsg);
        }
    }
    
    private void checkVMIdIsNotNullNorEmpty(UUID id, String errMsg){
        if(id == null || id.toString().isEmpty()){
            throw new IllegalArgumentException(errMsg);
        }
    }
    
    private void checkVMNameIsNotNullNorEmpty(String name, String errMsg){
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException(errMsg);
        }
    }
    
    private void checkPMIsConnected(PhysicalMachine pm, String errMsg) throws UnexpectedVMStateException{
        NativeVBoxAPIConnection natapiCon = NativeVBoxAPIConnection.getInstance();
        
        if(!natapiCon.isConnected(pm)){
            throw new UnexpectedVMStateException(errMsg);
        }
    }
    
    private void checkVMStateIsValidForStart(MachineState state, String errMsg) throws UnexpectedVMStateException{
        switch(state){
            case Running:
            case Paused : throw new UnexpectedVMStateException(errMsg);
            default     : break;
        }
    }
    
    private void checkVMStateIsValidForShutdown(MachineState state, String errMsg) throws UnexpectedVMStateException{
        switch(state){
            case Running:
            case Paused :
            case Stuck  : break;
            default     : throw new UnexpectedVMStateException(errMsg);
        }
    }
    
    private void checkPortRuleValidity(VirtualMachine virtualMachine, PortRule portRule, IMachine vboxMachine) throws PortRuleDuplicityException{
        String errMsgForPRNullCheck = "Creating new port forwarding rule failure: There was made an attempt to create a null port forwarding rule for virtual machine " + virtualMachine + ".";
        String errMsgForPRNameCheck = "Creating new port forwarding rule failure: Name of port rule " + portRule + " is null or empty.";
        String errMsgForPRHostPortCheck = "Creating new port forwarding rule failure: Host port number of new port forwarding rule " + portRule + " is negative or too big. Host port number can be from the range 0-65535.";
        String errMsgForPRGuestPortCheck = "Creating new port forwarding rule failure: Guest port number of new port forwarding rule " + portRule + " is negative or too big. Guest port number can be from the range 0-65535.";
        String errMsgForPRNameDuplicityCheck = "Crating new port forwarding rule failure: There already exists port forwarding rule with name = " + portRule.getName() + " on virtual machine " + virtualMachine + ".";
        String errMsgForPRHPDuplicityCheck = "Creating new port forwarding rule failure: There already exists port forwarding rule using host port number = " + portRule.getHostPort() + " on virtual machine " + virtualMachine + ".";
        
        checkPortRuleIsNotNull(portRule,errMsgForPRNullCheck);
        checkPortRuleNameIsNotNullNorEmpty(portRule.getName(),errMsgForPRNameCheck);
        checkPortRuleHostOrGuestPortIsValid(portRule.getHostPort(),errMsgForPRHostPortCheck);
        checkPortRuleHostOrGuestPortIsValid(portRule.getGuestPort(),errMsgForPRGuestPortCheck);
        checkPortRuleNameDuplicity(vboxMachine, portRule.getName(),errMsgForPRNameDuplicityCheck);
        checkPortRuleHostPortDuplicity(vboxMachine, portRule.getHostPort(),errMsgForPRHPDuplicityCheck);
    }
    
    private void checkPortRuleIsNotNull(PortRule portRule, String errMsg){
        if(portRule == null){
            throw new IllegalArgumentException(errMsg);
        }
    }
    
    private void checkPortRuleNameIsNotNullNorEmpty(String name, String errMsg){
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException(errMsg);
        }
    }
    
    private void checkPortRuleHostOrGuestPortIsValid(int port, String errMsg){
        if(port < 0 || port > 65535){
            throw new IllegalArgumentException(errMsg);
        }
    }
    
    private void checkPortRuleNameDuplicity(IMachine vboxMachine, String name, String errMsg) throws PortRuleDuplicityException{
        INetworkAdapter adapter = vboxMachine.getNetworkAdapter(0L);
        INATEngine natEngine = adapter.getNATEngine();
        List<String> redirects = natEngine.getRedirects();
        
        for(String redirect : redirects){
            String[] parts = redirect.split(",");
            if(parts[0].equals(name)){
                throw new PortRuleDuplicityException(errMsg);
            }
        }
    }
    
    private void checkPortRuleHostPortDuplicity(IMachine vboxMachine, int port, String errMsg) throws PortRuleDuplicityException{
        INetworkAdapter adapter = vboxMachine.getNetworkAdapter(0L);
        INATEngine natEngine = adapter.getNATEngine();
        List<String> redirects = natEngine.getRedirects();
        
        for(String redirect : redirects){
            String[] parts = redirect.split(",");
            int redHostPort = Integer.parseInt(parts[3]);
            if(redHostPort == port){
                throw new PortRuleDuplicityException(errMsg);
            }
        }
    }
    
    private PortRule redirectToPortRule(String redirect){
        String parts[] = redirect.split(",");
        String name = parts[0];
        ProtocolType protocol = ProtocolType.valueOf(parts[1]);
        String hostIP = parts[2];
        int hostPort = Integer.parseInt(parts[3]);
        String guestIP = parts[4];
        int guestPort = Integer.parseInt(parts[5]);
        
        return new PortRule.Builder(name, hostPort, guestPort).protocol(protocol)
                           .hostIP(hostIP).guestIP(guestIP).build();
    }*/
}
