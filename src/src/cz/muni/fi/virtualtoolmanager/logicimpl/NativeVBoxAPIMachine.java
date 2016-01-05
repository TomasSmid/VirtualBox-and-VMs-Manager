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
import cz.muni.fi.virtualtoolmanager.pubapi.entities.PortRule;
import cz.muni.fi.virtualtoolmanager.pubapi.entities.VirtualMachine;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnexpectedVMStateException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownPortRuleException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownVirtualMachineException;
import cz.muni.fi.virtualtoolmanager.pubapi.types.FrontEndType;
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
 * Class that is used to call the native methods from VirtualBox API to ensure
 * performing of operations associated with one virtual machine like starting
 * operation.
 * 
 * @see cz.muni.fi.virtualtoolmanager.logicimpl.VirtualMachineManagerImpl
 * 
 * @author Tomáš Šmíd
 */
class NativeVBoxAPIMachine {
    
    /**
     * This method starts a particular virtual machine with the specified
     * front-end type. All the parameters validation is done in manager class
     * {@link cz.muni.fi.virtualtoolmanager.logicimpl.VirtualMachineManagerImpl
     * VirtualMachineManagerImpl}.
     * If there occurs any error there can be thrown one of the following
     * exceptions:
     * 1)ConnectionFailureException - thrown when there occured any connection
     * problem (invalid physical machine attribute values, not running web server,
     * network connection not working properly or at all) when the native VirtualBox
     * manager object is being retrieved
     * 2)UnknownVirtualMachineException - thrown when the given virtual machine
     * is being retrieved (native object of VirtualBox virtual machine) from
     * remote physical machine, but that virtual machine does not exist (is not
     * registered at the VirtualBox hypervisor) on the remote physical machine
     * 3)UnexpectedVMStateException - thrown when the virtual machine cannot be
     * accessed (e.g. corrupted configuration files) or when the virtual machine
     * is in a not required state ("running","paused") for starting operation or
     * the starting operation is aborted because of the another process which
     * is using the virtual machine at the same moment.
     * @param virtualMachine represents the virtual machine which is going to be
     * started
     */
    public void startVM(VirtualMachine virtualMachine, FrontEndType frontEndType){ 
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
        String type = frontEndType.toString().toLowerCase();
        try{
            IProgress progress = vboxMachine.launchVMProcess(session, type, "");
            //wait while the starting operation is finished
            while(!progress.getCompleted()){
                virtualBoxManager.waitForEvents(0l);
                progress.waitForCompletion(200);
            }
            //check the starting operation has been finished successfully
            if(progress.getResultCode() == 0){
                while(vboxMachine.getState() != MachineState.Running){
                    //just loop until the condition is true
                } 
            }else{                
                //operation was not finished successfully, unlock machine and do 
                //the clean up after performed operation(s)
                virtualBoxManager.disconnect();
                virtualBoxManager.cleanup();
                throw new UnexpectedVMStateException(getErrorMessage(4, virtualMachine));               
            }
        }catch(VBoxException ex){
            //just do the clean up after performed operation(s)
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            //VM cannot be started now, because there is another process which is using the VM now
            //(that process locked the VM for itself)
            throw new UnexpectedVMStateException(getErrorMessage(5, virtualMachine));
        }
        
        //operation finished successfully (VM is running now), now release the VM for another processes
        //and do the after operation(s) clean up
        session.unlockMachine();
        virtualBoxManager.disconnect();
        virtualBoxManager.cleanup();
    }
    
    /**
     * This method shuts down a particular virtual machine. The implemented
     * shutdown operation is equivalent to powering off the computer from the
     * the plug socket, so the operations processed in the moment of this method
     * call are not finished and there can be lost data, but it is much more
     * faster then using the power button access.
     * All the parameters validation is done in manager class
     * {@link cz.muni.fi.virtualtoolmanager.logicimpl.VirtualMachineManagerImpl
     * VirtualMachineManagerImpl}.
     * If there occurs any error there can be thrown one of the following
     * exceptions:
     * 1)ConnectionFailureException - thrown when there occured any connection
     * problem (invalid physical machine attribute values, not running web server,
     * network connection not working properly or at all) when the native VirtualBox
     * manager object is being retrieved
     * 2)UnknownVirtualMachineException - thrown when the given virtual machine
     * is being retrieved (native object of VirtualBox virtual machine) from
     * remote physical machine, but that virtual machine does not exist (is not
     * registered at the VirtualBox hypervisor) on the remote physical machine
     * 3)UnexpectedVMStateException - thrown when the virtual machine cannot be
     * accessed (e.g. corrupted configuration files) or when the virtual machine
     * is not in a required state ("running", "paused", "stuck") for shutdown
     * operation or the shutdown operation was not finished successfully
     * @param virtualMachine represents the virtual machine which is going to be
     * shut down
     */
    public void shutDownVM(VirtualMachine virtualMachine){
        VirtualBoxManager virtualBoxManager = VirtualBoxManager.createInstance(null);
        IMachine vboxMachine = null;
        int[] errMsgNum = {6, 7};
        
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
            throw new UnexpectedVMStateException(getErrorMessage(8, virtualMachine)
                                    + vboxMachine.getAccessError().getText());
        }
        
        //check the VM is in a required state for VM shutdown operation
        switch(vboxMachine.getState()){
            case Running:
            case Paused : 
            case Stuck  : break;
            default     : throw new UnexpectedVMStateException(getErrorMessage(9, virtualMachine));
        }
        
        //all conditions for VM shutdown are met - VM can be shut down
        ISession session = virtualBoxManager.getSessionObject();
        //process which ensures VM shutdown must firstly lock the VM for itself
        //(not using the exclusive lock, just shared - if there exists another 
        //process which locked VM for itself)
        vboxMachine.lockMachine(session, LockType.Shared);
        IConsole console = session.getConsole();
        //shut the VM down
        IProgress progress = console.powerDown();
        while(!progress.getCompleted()){
            virtualBoxManager.waitForEvents(0l);
            progress.waitForCompletion(200);
        }
        
        try{
            //release the VM for another processes and wait while the VM is 
            //definitively powered down and unlocked
            session.unlockMachine();
        }catch(VBoxException ex){
            //machine has already been unlocked -> second attempt to unlock 
            //unlocked machine invoked exception - after powerDown() operation 
            //had finished the session unlocked machine, the call session.unlockMachine()
            //is performed for sure in order to ensure that the machine surely would not stay locked
        }
        while(session.getState() != SessionState.Unlocked){
            //just loop until the condition is true
        }
        
        if(progress != null){
            if(progress.getResultCode() != 0){
                //get error info
                String nativeAPIErrorInfo = progress.getErrorInfo().getText();
                //operation finished successfully - do the clean up after performed operation(s)
                virtualBoxManager.disconnect();
                virtualBoxManager.cleanup();
                throw new UnexpectedVMStateException(getErrorMessage(10, virtualMachine) + nativeAPIErrorInfo);
            }
        }
        
        while(vboxMachine.getState() != MachineState.PoweredOff){
            //just loop until the condition is true
        }
        
        
        //operation finished successfully - do the clean up after performed operation(s)
        virtualBoxManager.disconnect();
        virtualBoxManager.cleanup();
    }
    
    /**
     * This method adds new port-forwarding rule to a particular virtual machine.
     * The given port rule must have a unique name on the virtual machine and
     * the host port number must not be used by another existing port rule.
     * If there occurs any error there can be thrown one of the following
     * exceptions:
     * 1)ConnectionFailureException - thrown when there occured any connection
     * problem (invalid physical machine attribute values, not running web server,
     * network connection not working properly or at all) when the native VirtualBox
     * manager object is being retrieved
     * 2)UnknownVirtualMachineException - thrown when the given virtual machine
     * is being retrieved (native object of VirtualBox virtual machine) from
     * remote physical machine, but that virtual machine does not exist (is not
     * registered at the VirtualBox hypervisor) on the remote physical machine
     * 3)UnexpectedVMStateException - thrown when the virtual machine is not
     * attached to the NAT network adapter -> no port-forwarding can be done
     * @param virtualMachine represents the virtual machine to which will be added
     * a new port-forwarding rule
     * @param portRule represents a new port-forwarding rule
     */
    public void addPortRule(VirtualMachine virtualMachine, PortRule portRule){
        VirtualBoxManager virtualBoxManager = VirtualBoxManager.createInstance(null);
        IMachine vboxMachine = null;
        int[] errMsgNum = {11, 12};
        
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
            throw new UnexpectedVMStateException(getErrorMessage(13, virtualMachine));
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
    
    /**
     * This method deletes existing port-forwarding rule from a particular virtual
     * machine.
     * If there occurs any error there can be thrown one of the following
     * exceptions:
     * 1)ConnectionFailureException - thrown when there occured any connection
     * problem (invalid physical machine attribute values, not running web server,
     * network connection not working properly or at all) when the native VirtualBox
     * manager object is being retrieved
     * 2)UnknownVirtualMachineException - thrown when the given virtual machine
     * is being retrieved (native object of VirtualBox virtual machine) from
     * remote physical machine, but that virtual machine does not exist (is not
     * registered at the VirtualBox hypervisor) on the remote physical machine
     * 3)UnexpectedVMStateException - thrown when the virtual machine is not
     * attached to the NAT network adapter -> no port-forwarding can be done
     * 4)UnknownPortRuleException - thrown when there is made an attempt to
     * delete a non-existent port-forwarding rule
     * @param virtualMachine represents the virtual machine from which will be
     * deleted a port-forwarding rule
     * @param ruleName  represents a port-forwarding rule which will be deleted
     */
    public void deletePortRule(VirtualMachine virtualMachine, String ruleName){
        VirtualBoxManager virtualBoxManager = VirtualBoxManager.createInstance(null);
        IMachine vboxMachine = null;
        int[] errMsgNum = {14, 15};
        
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
            throw new UnexpectedVMStateException(getErrorMessage(16, virtualMachine));
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
            throw new UnknownPortRuleException(getErrorMessage(17, virtualMachine) + ruleName
                                            + ". Nonexistent port-forwarding rule cannot be deleted.");
        }
        
        //operation finished successfully - do the clean up after performed operation(s)
        virtualBoxManager.disconnect();
        virtualBoxManager.cleanup();
    }
    
    /**
     * This method gets all existing port-forwarding rules of a particular
     * virtual machine.
     * If there occurs any error there can be thrown one of the following
     * exceptions:
     * 1)ConnectionFailureException - thrown when there occured any connection
     * problem (invalid physical machine attribute values, not running web server,
     * network connection not working properly or at all) when the native VirtualBox
     * manager object is being retrieved
     * 2)UnknownVirtualMachineException - thrown when the given virtual machine
     * is being retrieved (native object of VirtualBox virtual machine) from
     * remote physical machine, but that virtual machine does not exist (is not
     * registered at the VirtualBox hypervisor) on the remote physical machine
     * 3)UnexpectedVMStateException - thrown when the virtual machine is not
     * attached to the NAT network adapter -> no port-forwarding can be done
     * @param virtualMachine represents the virtual machine from which will be
     * retrieved all port-forwarding rules
     * @return list of all existing port-forwarding rules associated with the
     * given virtual machine
     */
    public List<String> getPortRules(VirtualMachine virtualMachine){
        VirtualBoxManager virtualBoxManager = VirtualBoxManager.createInstance(null);
        IMachine vboxMachine = null;
        int[] errMsgNum = {18, 19};
        
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
            throw new UnexpectedVMStateException(getErrorMessage(20, virtualMachine));
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
    
    /**
     * This method gets the actual state of a particular virtual machine.
     * The given port rule must have a unique name on the virtual machine and
     * the host port number must not be used by another existing port rule.
     * If there occurs any error there can be thrown one of the following
     * exceptions:
     * 1)ConnectionFailureException - thrown when there occured any connection
     * problem (invalid physical machine attribute values, not running web server,
     * network connection not working properly or at all) when the native VirtualBox
     * manager object is being retrieved
     * 2)UnknownVirtualMachineException - thrown when the given virtual machine
     * is being retrieved (native object of VirtualBox virtual machine) from
     * remote physical machine, but that virtual machine does not exist (is not
     * registered at the VirtualBox hypervisor) on the remote physical machine
     * 3)UnexpectedVMStateException - thrown when the virtual machine cannot be
     * accessed (e.g. corrupted configuration files)
     * @param virtualMachine represents the virtual machine to which will be added
     * a new port-forwarding rule
     * @return actual virtual machine state as string
     */
    public String getVMState(VirtualMachine virtualMachine){
        VirtualBoxManager virtualBoxManager = VirtualBoxManager.createInstance(null);
        IMachine vboxMachine = null;
        int[] errMsgNum = {21, 22};
        
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
            throw new UnexpectedVMStateException(getErrorMessage(23, virtualMachine)
                                    + vboxMachine.getAccessError().getText());
        }
        
        //get state of the VM
        String vmState = vboxMachine.getState().name();
        
        //operation finished successfully - do the clean up after performed operation(s)
        virtualBoxManager.disconnect();
        virtualBoxManager.cleanup();
        
        return vmState;
    }
    
    /**
     * Gets the error message placed in array of messages on a given index.
     * @param index index of a required error message in array
     * @param virtualMachine actually used virtual machine
     * @return the required error message
     */
    private String getErrorMessage(int index, VirtualMachine virtualMachine){
        String[] errMessages = /* 00 */{"Connection operation failure while trying to start the virtual machine " + virtualMachine + ": Unable to connect to the physical machine " + virtualMachine.getHostMachine() + ". Most probably there occured one of these problems: 1. Network connection is not working properly or at all / 2. The VirtualBox web "
                                        + " server is not running / 3. One of the key value (IP address, number of web server port, username or user password) of the physical machine has been changed and it is incorrect now (used value is not the actual correct one).",
                               /* 01 */ "Virtual machine starting operation failure: There is no virtual machine " + virtualMachine + " on physical machine " + virtualMachine.getHostMachine() + " known to VirtualBox. Nonexistent virtual machine cannot be started.",
                               /* 02 */ "Virtual machine starting operation failure: Virtual machine " + virtualMachine + " cannot be started, because it is not accessible -> ",
                               /* 03 */ "Virtual machine starting operation failure: Virtual machine " + virtualMachine + " cannot be started, because it is already running.",
                               /* 04 */ "Virtual machine starting operation failure: Failed to open the session with virtual machine " + virtualMachine + ". It can be caused by a missing or corrupted \"" + virtualMachine.getName() + ".vdi\" file or there is being performed another operation (e.g. cloning) with this virtual machine so it cannot be started now.",
                               /* 05 */ "Virtual machine starting operation failure: Virtual machine " + virtualMachine + " cannot be started now, because there exists another process which is working with this virtual machine now (that process has locked the virtual machine just for itself).",
                               /* 06 */ "Connection operation failure while trying to shut down the virtual machine " + virtualMachine + ": Unable to connect to the physical machine " + virtualMachine.getHostMachine() + ". Most probably there occured one of these problems: 1. Network connection is not working properly or at all / 2. The VirtualBox web "
                                        + "server is not running / 3. One of the key value (IP address, number of web server port, username or user password) of the physical machine has been changed and it is incorrect now (used value is not the actual correct one).",
                               /* 07 */ "Virtual machine shutdown operation failure: There is no virtual machine " + virtualMachine + " on physical machine " + virtualMachine.getHostMachine() + " known to VirtualBox. Nonexistent virtual machine cannot be shut down.",
                               /* 08 */ "Virtual machine shutdown operation failure: Virtual machine " + virtualMachine + " cannot be shut down, because it is not accessible -> ",
                               /* 09 */ "Virtual machine shutdown operation failure: Virtual machine " + virtualMachine + " cannot be shut down, because it is not in any of the required states (\"Running\", \"Paused\", \"Stuck\") for virtual machine shutdown operation.",
                               /* 10 */ "Virtual machine shutdown operation failure: ", //error info from VirtualBox API should follow
                               /* 11 */ "Connection operation failure while trying to add new port-forwarding rule to the virtual machine " + virtualMachine + ": Unable to connect to the physical machine " + virtualMachine.getHostMachine() + ". Most probably there occured one of these problems: 1. Network connection is not working properly or at all / 2. The VirtualBox web "
                                        + "server is not running / 3. One of the key value (IP address, number of web server port, username or user password) of the physical machine has been changed and it is incorrect now (used value is not the actual correct one).",
                               /* 12 */ "New port-forwarding rule addition operation failure: There is no virtual machine " + virtualMachine + " on physical machine " + virtualMachine.getHostMachine() + " known to VirtualBox. New port-forwarding rule cannot be added to a nonexistent virtual machine.",
                               /* 13 */ "New port-forwarding rule addition operation failure: There cannot be added any port-forwarding rule to virtual machine " + virtualMachine + ", because its network adapter is not attached to the required network adapter of type NAT.",
                               /* 14 */ "Connection operation failure while trying to delete port-forwarding rule from the virtual machine " + virtualMachine + ": Unable to connect to the physical machine " + virtualMachine.getHostMachine() + ". Most probably there occured one of these problems: 1. Network connection is not working properly or at all / 2. The VirtualBox web "
                                        + "server is not running / 3. One of the key value (IP address, number of web server port, username or user password) of the physical machine has been changed and it is incorrect now (used value is not the actual correct one).",
                               /* 15 */ "Port-forwarding rule deletion operation failure: There is no virtual machine " + virtualMachine + " on physical machine " + virtualMachine.getHostMachine() + " known to VirtualBox. Port-forwarding rule cannot be deleted from a nonexistent virtual machine.",
                               /* 16 */ "Port-forwarding rule deletion operation failure: There cannot be deleted any port-forwarding rule from virtual machine " + virtualMachine + ", because its network adapter is not attached to the required network adapter of type NAT (there cannot exist any port-forwarding rule on this virtual machine).",
                               /* 17 */ "Port-forwarding rule deletion operation failure: On virtual machine " + virtualMachine + " there is no port-forwarding rule with name = ",
                               /* 18 */ "Connection operation failure while trying to get all port-forwarding rules from the virtual machine " + virtualMachine + ": Unable to connect to the physical machine " + virtualMachine.getHostMachine() + ". Most probably there occured one of these problems: 1. Network connection is not working properly or at all / 2. The VirtualBox web "
                                        + "server is not running / 3. One of the key value (IP address, number of web server port, username or user password) of the physical machine has been changed and it is incorrect now (used value is not the actual correct one).",
                               /* 19 */ "All port-forwarding rules retrieve operation failure: There is no virtual machine " + virtualMachine + " on physical machine " + virtualMachine.getHostMachine() + " known to VirtualBox. Port-forwarding rules cannot be retrieved from a nonexistent virtual machine.",
                               /* 20 */ "All port-forwarding rules retrieve operation failure: There cannot be retrieved port-forwarding rules from virtual machine " + virtualMachine + ", because its network adapter is not attached to the required network adapter of type NAT (there cannot exist any port-forwarding rule on this virtual machine).",
                               /* 21 */ "Connection operation failure while trying to find out the state of the virtual machine " + virtualMachine + ": Unable to connect to the physical machine " + virtualMachine.getHostMachine() + ". Most probably there occured one of these problems: 1. Network connection is not working properly or at all / 2. The VirtualBox web "
                                        + "server is not running / 3. One of the key value (IP address, number of web server port, username or user password) of the physical machine has been changed and it is incorrect now (used value is not the actual correct one).",
                               /* 22 */ "Virtual machine state finding out operation failures: There is no virtual machine " + virtualMachine + " on physical machine " + virtualMachine.getHostMachine() + " known to VirtualBox. There cannot be found out a state of nonexistent virtual machine.",
                               /* 23 */ "Virtual machine state finding out operation failure: There cannot be found out the state of the virtual machine " + virtualMachine + ", because it is not accessible -> "};
        
        return errMessages[index];
    }
    
    /**
     * Retrieves the native VirtualBox virtual machine instance which can be used
     * for further processing. 
     * @param virtualBoxManager native VirtualBox manager
     * @param virtualMachine virtual machine whose native instance should be retrieved
     * @param errMsgNum array of integers determining the error messages
     * @return native VirtualBox virtual machine instance
     */
    private IMachine getVBoxMachine(VirtualBoxManager virtualBoxManager,
            VirtualMachine virtualMachine, int[] errMsgNum){
        String url = getURL(virtualMachine.getHostMachine());
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
    
    /**
     * This method creates the correct form of url from the given IP address
     * and the port number of the VirtualBox web server. Thanks this method
     * it is possible to connect to the physical machine and its VirtualBox
     * web server with IPv4 or IPv6.
     * 
     * @param physicalMachine represents the physical machine whose IP address
     * and port number of VirtualBox web server will be used for new url creation
     * @return newly created url defining the physical machine
     */
    private String getURL(PhysicalMachine physicalMachine){
        if(physicalMachine.getAddressIP().contains(".")){
            return "http://" + physicalMachine.getAddressIP() + ":"
                    + physicalMachine.getPortOfVTWebServer();
        }
        
        return "http://[" + physicalMachine.getAddressIP() + "]:"
                + physicalMachine.getPortOfVTWebServer();
    }
}
