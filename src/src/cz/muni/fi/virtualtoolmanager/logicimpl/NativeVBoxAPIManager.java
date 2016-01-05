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
import cz.muni.fi.virtualtoolmanager.pubapi.types.CloneType;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.virtualbox_4_3.CleanupMode;
import org.virtualbox_4_3.CloneMode;
import org.virtualbox_4_3.CloneOptions;
import org.virtualbox_4_3.IConsole;
import org.virtualbox_4_3.IGuestOSType;
import org.virtualbox_4_3.IMachine;
import org.virtualbox_4_3.IMedium;
import org.virtualbox_4_3.IMediumAttachment;
import org.virtualbox_4_3.IProgress;
import org.virtualbox_4_3.ISession;
import org.virtualbox_4_3.ISnapshot;
import org.virtualbox_4_3.ISystemProperties;
import org.virtualbox_4_3.IVirtualBox;
import org.virtualbox_4_3.LockType;
import org.virtualbox_4_3.MachineState;
import org.virtualbox_4_3.MediumState;
import org.virtualbox_4_3.SessionState;
import org.virtualbox_4_3.VBoxException;
import org.virtualbox_4_3.VirtualBoxManager;



/**
 * Class that is used to call the native methods from VirtualBox API to ensure
 * performing of operations with one or more virtual machines.
 * 
 * @see cz.muni.fi.virtualtoolmanager.logicimpl.VirtualizationToolManagerImpl
 * 
 * @author Tomáš Šmíd
 */
class NativeVBoxAPIManager {
    
    /**
     * This method registers the required virtual machine at VirtualBox hypervisor.
     * To ensure the correct performing of this operation, the virtual machine
     * and its configuration file must be present in VirtualBox machines folder
     * which is defaultly set up to "VirtualBox VMs".
     * If the machine is registered successfully, then it can be immediately used
     * for any virtual machine operation.
     * If there occurs any error, the following exceptions can be thrown:
     * 1)ConnectionFailureException - thrown when there occured any connection
     * problem (invalid physical machine attribute values, not running web server,
     * network connection not working properly or at all) when the native VirtualBox
     * manager object is being retrieved (is connecting to VirtualBox web server)
     * 2)UnknownVirtualMachineException - thrown when there cannot be open
     * the given virtual machine -> there does not exist the path to the virtual
     * machine in form "VBoxDefaultMachineFolder\\vmname\\vmname.vbox" or there
     * is missing the virtual hard disk that is specified in configuration file
     * of virtual machine or any other problem
     * 
     * @param physicalMachine represents the physical machine on which the virtual
     * machine should be registered
     * @param name name of the virtual machine which should be registered
     * @return true if the registration operation finished successfully (VM has
     * been registered), false if the virtual machine has already been registered
     */
    public boolean registerVirtualMachine(PhysicalMachine physicalMachine, String name){
        //get the url for the PM connection
        String url = getURL(physicalMachine);
        boolean vmIsRegistered = true;
        VirtualBoxManager virtualBoxManager = VirtualBoxManager.createInstance(null);
        
        try{
            //connect to VirtualBox web server
            virtualBoxManager.connect(url, physicalMachine.getUsername(), physicalMachine.getUserPassword());
        }catch(VBoxException ex){
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw new ConnectionFailureException(getErrorMessage(0, physicalMachine, name));
        }
        
        IVirtualBox vbox = virtualBoxManager.getVBox();        
        IMachine vboxMachine = null;
        try{
            //try to get the VirtualBox virtual machine instance
            vboxMachine = vbox.findMachine(name);
        }catch(VBoxException ex){
            //VM is not registered
            vmIsRegistered = false;
        }
        
        if(!vmIsRegistered){
            ISystemProperties systemProperties = vbox.getSystemProperties();
            try{
                //try to open the machine - check the virtual machine really exists
                //in a required place
                vboxMachine = vbox.openMachine(systemProperties.getDefaultMachineFolder() 
                        + "\\" + name + "\\" + name + ".vbox");
            }catch(VBoxException ex){
                String defaultMachineFolder = systemProperties.getDefaultMachineFolder();                
                virtualBoxManager.disconnect();
                virtualBoxManager.cleanup();
                if(ex.getMessage().contains("(Path not found.)")){
                    throw new UnknownVirtualMachineException(getErrorMessage(
                            1, physicalMachine, name, defaultMachineFolder));
                }else{
                    if(ex.getMessage().contains("Could not find an open hard disk")){
                        throw new UnknownVirtualMachineException(getErrorMessage(
                                2, physicalMachine, name, defaultMachineFolder));
                    }else{
                        throw new UnknownVirtualMachineException("Virtual machine "
                                + "registration operation failure: " + ex.getMessage());
                    }
                }
            }
            
            //register the virtual machine
            vbox.registerMachine(vboxMachine);
            
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            return true;
        }
        
        virtualBoxManager.disconnect();
        virtualBoxManager.cleanup();
        return false;
    }
    
    /**
     * This method retrieves the required virtual machine from a remote physical
     * machine.
     * If there occurs any error, the following exceptions can be thrown:
     * 1)ConnectionFailureException - thrown when there occured any connection
     * problem (invalid physical machine attribute values, not running web server,
     * network connection not working properly or at all) when the native VirtualBox
     * manager object is being retrieved (is connecting to VirtualBox web server)
     * 2)UnknownVirtualMachineException - thrown when the required virtual machine
     * does not exist on the physical machine
     * @param physicalMachine represents the physical machine from should be
     * retrieved the required virtual machine
     * @param nameOrId name or uuid of the required virtual machine
     * @return retrieved virtual machine 
     */
    public VirtualMachine getVirtualMachine(PhysicalMachine physicalMachine, String nameOrId){
        //get the url for PM connection
        String url = getURL(physicalMachine);
        VirtualBoxManager virtualBoxManager = VirtualBoxManager.createInstance(null);
        
        try{
            //connect to VirtualBox web server
            virtualBoxManager.connect(url, physicalMachine.getUsername(), physicalMachine.getUserPassword());
        }catch(VBoxException ex){
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw new ConnectionFailureException(getErrorMessage(3, physicalMachine, nameOrId));
        }
        
        IVirtualBox vbox = virtualBoxManager.getVBox();        
        IMachine vboxMachine;
        try{
            vboxMachine = vbox.findMachine(nameOrId);
        }catch(VBoxException ex){
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            //without error message - in this case has not to be VM found
            throw new UnknownVirtualMachineException();
        }
        
        //get the retrieved VirtualBox virtual machine as new API virtual machine object
        IGuestOSType guestOSType = vbox.getGuestOSType(vboxMachine.getOSTypeId());
        VirtualMachine virtualMachine = getConvertedVM(vboxMachine, guestOSType, physicalMachine);
        
        virtualBoxManager.disconnect();
        virtualBoxManager.cleanup();
        return virtualMachine;
    }
    
    /**
     * This method retrieves all virtual machines from a remote physical
     * machine.
     * If there occurs any error, the following exceptions can be thrown:
     * 1)ConnectionFailureException - thrown when there occured any connection
     * problem (invalid physical machine attribute values, not running web server,
     * network connection not working properly or at all) when the native VirtualBox
     * manager object is being retrieved (is connecting to VirtualBox web server)
     * 2)UnknownVirtualMachineException - thrown when there occurs any error of
     * VirtualBox
     *  
     * @param physicalMachine physical machine from which all virtual machines
     * should be retrieved
     * @return list of all registered virtual machines on the physical machine
     */
    public List<VirtualMachine> getAllVirtualMachines(PhysicalMachine physicalMachine){
        //get the url for PM connection
        String url = getURL(physicalMachine);
        VirtualBoxManager virtualBoxManager = VirtualBoxManager.createInstance(null);
        
        try{
            //connect to VirtualBox web server
            virtualBoxManager.connect(url, physicalMachine.getUsername(), physicalMachine.getUserPassword());
        }catch(VBoxException ex){
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw new ConnectionFailureException(getErrorMessage(4, physicalMachine, ""));
        }
        
        IVirtualBox vbox = virtualBoxManager.getVBox();        
        List<IMachine> vboxMachines = null;
        try{
            vboxMachines = vbox.getMachines();
        }catch(VBoxException ex){
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw new UnknownVirtualMachineException(getErrorMessage(5, physicalMachine, "") + ex.getMessage());
        }
        
        if(vboxMachines.isEmpty()){
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            return new ArrayList<>();
        }        
        
        //get all VirtualBox virtual machines as new API virtual machine objects
        List<VirtualMachine> virtualMachines = new ArrayList<>();
        for(IMachine vboxMachine : vboxMachines){
            IGuestOSType guestOSType = vbox.getGuestOSType(vboxMachine.getOSTypeId());
            virtualMachines.add(getConvertedVM(vboxMachine, guestOSType, physicalMachine));
        }
        
        virtualBoxManager.disconnect();
        virtualBoxManager.cleanup();
        return virtualMachines;
    }
    
    /**
     * This method removes the specified virtual machine on a remote physical
     * machine. To remove virtual machine it must be powered off. This implementation
     * ensures that all the virtual machine files will be deleted and virtual
     * machine will be also unregistered from VirtualBox.
     * 1)ConnectionFailureException - thrown when there occured any connection
     * problem (invalid physical machine attribute values, not running web server,
     * network connection not working properly or at all) when the native VirtualBox
     * manager object is being retrieved (is connecting to VirtualBox web server)
     * 2)UnknownVirtualMachineException - thrown when the specified virtual machine
     * is not in the list of registered virtual machines on the remote physical
     * machine
     * 3)IllegalStateException - thrown when the specified virtual machine was
     * found as registered virtual machine, but a few moments later if it should
     * be unregistered, because it is not accessible, it throw the vbox exception
     * 4)UnexpectedVMStateException - thrown when the virtual machine is not
     * powered off
     * @param virtualMachine virtual machine which should be removed
     */
    public void removeVirtualMachine(VirtualMachine virtualMachine){
        //get the url for PM connection
        String url = getURL(virtualMachine.getHostMachine());
        String username = virtualMachine.getHostMachine().getUsername();
        String userPassword = virtualMachine.getHostMachine().getUserPassword();
        VirtualBoxManager virtualBoxManager = VirtualBoxManager.createInstance(null);
        
        try{
            //connect to VirtualBox web server
            virtualBoxManager.connect(url, username, userPassword);
        }catch(VBoxException ex){
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw new ConnectionFailureException(getErrorMessage(6, virtualMachine.getHostMachine(),
                                                 virtualMachine.getName()));
        }
        
        IVirtualBox vbox = virtualBoxManager.getVBox();        
        IMachine vboxMachine = null;
        try{
            //get the VirtualBox virtual machine instance
            vboxMachine = vbox.findMachine(virtualMachine.getId().toString());
        }catch(VBoxException ex){
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw new UnknownVirtualMachineException(getErrorMessage(7, virtualMachine.getHostMachine(),
                                                     virtualMachine.getName()));
        }
        
        //check the virtual machine is accessible and can be removed
        if(!vboxMachine.getAccessible()){
            try{
                vboxMachine.unregister(CleanupMode.DetachAllReturnHardDisksOnly);
            }catch(VBoxException ex){
                virtualBoxManager.disconnect();
                virtualBoxManager.cleanup();
                /*machine was not registered -> this should not normally happen, because if the machine
                was not registered, then the VBoxException would be invoked earlier at vbox.findMachine()*/
                throw new IllegalStateException(ex);
            }
        }else{            
            //check the virtual machine is powered off
            if(vboxMachine.getState() != MachineState.PoweredOff){
                virtualBoxManager.disconnect();
                virtualBoxManager.cleanup();
                throw new UnexpectedVMStateException(getErrorMessage(8, virtualMachine.getHostMachine(),
                                                     virtualMachine.getName()));
            }

            if(isLinkedClone(vboxMachine, vbox)){                
                try{
                    //remove virtual machine as a child of some another VM
                    removeVMAsSnapshot(vboxMachine, virtualBoxManager);
                }catch(UnexpectedVMStateException ex){
                    virtualBoxManager.disconnect();
                    virtualBoxManager.cleanup();
                    throw ex;
                }
            }else{
                try{
                    //remove VM as a top unit (it is not child, but can have children)
                    removeVMAsStandaloneUnit(vboxMachine, virtualBoxManager);
                }catch(UnexpectedVMStateException ex){
                    virtualBoxManager.disconnect();
                    virtualBoxManager.cleanup();
                    throw ex;
                }
            }
        }
        
        virtualBoxManager.disconnect();
        virtualBoxManager.cleanup();
    }
    
    /**
     * This method creates a particular virtual machine clone. There can be created
     * four types of clones - 3 types of full clone and one linked clone. Creation of a
     * full clone take a longer time than creation a linked clone. Full clone
     * is independent on the original virtual machine. Name of clones are created
     * as follows: for full and linked clone is used the name of original virtual
     * machine as a base. To this name is suffixed the string "_FullCloneX" for
     * full clones and "_LinkedCloneY" for linked clones, where X and Y represent
     * the lowest available number.
     * 1)ConnectionFailureException - thrown when there occured any connection
     * problem (invalid physical machine attribute values, not running web server,
     * network connection not working properly or at all) when the native VirtualBox
     * manager object is being retrieved (is connecting to VirtualBox web server)
     * 2)UnknownVirtualMachineException - thrown when the specified virtual machine
     * is not in the list of registered virtual machines on the remote physical
     * machine
     * 3)UnexpectedVMStateException - thrown when the virtual machine is not
     * accessible or it is not in a required state ("poweredOff","running","paused",
     * "saved") or the cloning operation was not finished successfully
     * 
     * @param virtualMachine original virtual machine which should be cloned
     * @param cloneType type of clone which should be created
     * @return cloned virtual machine
     */
    public VirtualMachine createVMClone(VirtualMachine virtualMachine, CloneType cloneType){
        String url = getURL(virtualMachine.getHostMachine());
        String username = virtualMachine.getHostMachine().getUsername();
        String userPassword = virtualMachine.getHostMachine().getUserPassword();
        VirtualBoxManager virtualBoxManager = VirtualBoxManager.createInstance(null);
        
        try{
            virtualBoxManager.connect(url, username, userPassword);
        }catch(VBoxException ex){
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw new ConnectionFailureException(getErrorMessage(9, virtualMachine.getHostMachine(),
                                                 virtualMachine.getName()));
        }
        
        IVirtualBox vbox = virtualBoxManager.getVBox();        
        IMachine vboxMachine = null;
        try{
            vboxMachine = vbox.findMachine(virtualMachine.getId().toString());
        }catch(VBoxException ex){
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw new UnknownVirtualMachineException(getErrorMessage(10, virtualMachine.getHostMachine(),
                                                     virtualMachine.getName()));
        }
        
        if(!vboxMachine.getAccessible()){
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw new UnexpectedVMStateException(getErrorMessage(11, virtualMachine.getHostMachine(),
                                                 virtualMachine.getName()) + vboxMachine.getAccessError().getText());
        }
        
        switch(vboxMachine.getState()){
            case PoweredOff:
            case Saved     :
            case Running   :
            case Paused    : break;
            default        : throw new UnexpectedVMStateException(getErrorMessage(12, virtualMachine.getHostMachine(),
                                                                  virtualMachine.getName()));
        }        
        checkMediumStateForCloning(vboxMachine);
        
        String cloneName = getNewCloneName(vboxMachine.getName(), vbox, cloneType, vboxMachine.getOSTypeId());
        
        IMachine clonableVBoxMachine;
        if(cloneType == CloneType.LINKED){
            ISession session = virtualBoxManager.getSessionObject();
            try{
                takeSnapshot(vboxMachine,session,cloneName);
            }catch(VBoxException ex){
                if(session.getState() == SessionState.Locked){
                    session.unlockMachine();
                    while(session.getState() != SessionState.Unlocked){
                        //do nothing, just loop until the condition is true
                    }
                }
                virtualBoxManager.disconnect();
                virtualBoxManager.cleanup();
                throw new UnexpectedVMStateException("Cloning virtual machine " 
                        + virtualMachine + " on physical machine " 
                        + virtualMachine.getHostMachine() + " failure: " 
                        + ex.getMessage());
            }catch(UnexpectedVMStateException ex){
                virtualBoxManager.disconnect();
                virtualBoxManager.cleanup();
                throw ex;
            }
            ISnapshot snapshot = vboxMachine.getCurrentSnapshot();
            clonableVBoxMachine = snapshot.getMachine();
            
        }else{
            clonableVBoxMachine = vbox.findMachine(vboxMachine.getId());
        }
        
        IMachine vboxMachineClone = vbox.createMachine(null, cloneName, null, clonableVBoxMachine.getOSTypeId(), null);
        List<CloneOptions> cloneOptions = getCloneOptions(cloneType);
        CloneMode cloneMode = getCloneMode(cloneType);
        
        IProgress progress = clonableVBoxMachine.cloneTo(vboxMachineClone, cloneMode, cloneOptions);        
        Long progressPercent = 0L;
        OutputHandler outputHandler = new OutputHandler();
        try{
            while(!progress.getCompleted()){
                if(progress.getPercent() > progressPercent){
                    progressPercent = progress.getPercent();
                    outputHandler.printMessage("Cloning progress > " + progressPercent + "%");
                }
            }
        }catch(VBoxException ex){
            if(ex.getMessage().contains("connect")){
                String nativeAPIErrorInfo = "No more error info";
                if(progress.getResultCode() != 0){
                     nativeAPIErrorInfo = progress.getErrorInfo().getText();
                }
                virtualBoxManager.disconnect();
                virtualBoxManager.cleanup();
                throw new ConnectionFailureException(getErrorMessage(13, virtualMachine.getHostMachine(),
                                                        virtualMachine.getName()) + nativeAPIErrorInfo);
            }
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw new UnexpectedVMStateException(ex);
        }
        
        if(progress.getResultCode() != 0){
            String nativeAPIErrorInfo = progress.getErrorInfo().getText();
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw new UnexpectedVMStateException(getErrorMessage(14, virtualMachine.getHostMachine(),
                                                     virtualMachine.getName()) + nativeAPIErrorInfo);
        }
        
        vboxMachineClone.saveSettings();        
        vbox.registerMachine(vboxMachineClone);
        
        IGuestOSType guestOSType = vbox.getGuestOSType(vboxMachineClone.getOSTypeId());
        VirtualMachine vmClone = getConvertedVM(vboxMachineClone, guestOSType, virtualMachine.getHostMachine());
        
        virtualBoxManager.disconnect();
        virtualBoxManager.cleanup();
        
        return vmClone;
    }
    
    private String getErrorMessage(int index, PhysicalMachine physicalMachine, String vmNameOrId){
        return getErrorMessage(index, physicalMachine, vmNameOrId, "");
    }
    
    private String getErrorMessage(int index, PhysicalMachine physicalMachine, String vmNameOrId, String note){
        String[] errMessages = /* 00 */{"Connection operation failure while trying to register virtual machine " + vmNameOrId + ": Unable to connect to the physical machine " + physicalMachine + ". Most probably there occured one of these problems: 1. Network connection is not working properly or at all / 2. The VirtualBox web "
                                        + " server is not running / 3. One of the key value (IP address, number of web server port, username or user password) of the physical machine has been changed and it is incorrect now (used value is not the actual correct one).",
                               /* 01 */ "Virtual machine registration operation failure: There is no virtual machine folder named \"" + vmNameOrId + "\" with configuration file \"" + vmNameOrId + ".vbox\" in VirtualBox default machine folder \"" + note + "\".",
                               /* 02 */ "Virtual machine registration operation failure: Could not find an open hard disk of VM \"" + vmNameOrId + "\" -> missing or corrupted vdi file \"" + note + "\\" + vmNameOrId + "\\" + vmNameOrId + ".vdi\".",
                               /* 03 */ "Connection operation failure while trying to retrieve virtual machine " + vmNameOrId + ": Unable to connect to the physical machine " + physicalMachine + ". Most probably there occured one of these problems: 1. Network connection is not working properly or at all / 2. The VirtualBox web "
                                        + " server is not running / 3. One of the key value (IP address, number of web server port, username or user password) of the physical machine has been changed and it is incorrect now (used value is not the actual correct one).",                               
                               /* 04 */ "Connection operation failure while trying to retrieve all virtual machines: Unable to connect to the physical machine " + physicalMachine + ". Most probably there occured one of these problems: 1. Network connection is not working properly or at all / 2. The VirtualBox web "
                                        + " server is not running / 3. One of the key value (IP address, number of web server port, username or user password) of the physical machine has been changed and it is incorrect now (used value is not the actual correct one).",
                               /* 05 */ "All virtual machines retrieve operation failure: There did not manage to retrieve virtual machines from physical machine " + physicalMachine + " -> ",
                               /* 06 */ "Connection operation failure while trying to remove virtual machine " + vmNameOrId + ": Unable to connect to the physical machine " + physicalMachine + ". Most probably there occured one of these problems: 1. Network connection is not working properly or at all / 2. The VirtualBox web "
                                        + " server is not running / 3. One of the key value (IP address, number of web server port, username or user password) of the physical machine has been changed and it is incorrect now (used value is not the actual correct one).",
                               /* 07 */ "Virtual machine removal operation failure: There is no virtual machine " + vmNameOrId + " on physical machine " + physicalMachine + ". Nonexistent virtual machine cannot be removed.",
                               /* 08 */ "Virtual machine removal operation failure: Virtual machine " + vmNameOrId + " from physical machine " + physicalMachine + " cannot be removed, because it is not powered off.",                               
                               /* 09 */ "Connection operation failure while trying to clone virtual machine " + vmNameOrId + ": Unable to connect to the physical machine " + physicalMachine + ". Most probably there occured one of these problems: 1. Network connection is not working properly or at all / 2. The VirtualBox web "
                                        + " server is not running / 3. One of the key value (IP address, number of web server port, username or user password) of the physical machine has been changed and it is incorrect now (used value is not the actual correct one).",
                               /* 10 */ "Virtual machine cloning operation failure: There is no virtual machine " + vmNameOrId + " on physical machine " + physicalMachine + ". Nonexistent virtual machine cannot be cloned.",
                               /* 11 */ "Virtual machine cloning operation failure: Virtual machine " + vmNameOrId + " on physical machine " + physicalMachine + " cannot be cloned, because it is not accessible -> ",
                               /* 12 */ "Virtual machine cloning operation failure: Virtual machine " + vmNameOrId + " on physical machine " + physicalMachine + " cannot be cloned, because it is not in a valid state for cloning. The required states are: \"PoweredOff\", \"Saved\", \"Running\", \"Paused\".",
                               /* 13 */ "Connection error occured while cloning VM \"" + vmNameOrId + "\": ",
                               /* 14 */ "Virtual machine cloning operation failure: "};//error info from VBox API should follow
        
        return errMessages[index];
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
    
    private IMedium getVMHardDisk(IMachine vboxMachine){
        List<IMediumAttachment> medAttachs = vboxMachine.getMediumAttachmentsOfController("SATA");
        if(medAttachs.isEmpty()){
            return null;
        }
        IMediumAttachment medAttach = medAttachs.get(0);
        IMedium medium;
        try{
            medium = vboxMachine.getMedium("SATA", medAttach.getPort(), medAttach.getDevice());
        }catch(VBoxException ex){
            return null;
        }
        
        return medium;
    }
    
    private VirtualMachine getConvertedVM(IMachine vboxMachine,
            IGuestOSType guestOSType, PhysicalMachine physicalMachine){
        IMedium medium = getVMHardDisk(vboxMachine);
        UUID id = UUID.fromString(vboxMachine.getId());
        String name = vboxMachine.getName();
        Long cpuCount = vboxMachine.getCPUCount();
        Long monitorCount = vboxMachine.getMonitorCount();
        Long cpuExecCap = vboxMachine.getCPUExecutionCap();
        Long hardDiskFreeSpaceSize = (medium == null ? null : medium.getLogicalSize()-medium.getSize());
        Long hardDiskTotalSize = (medium == null ? null : medium.getLogicalSize());
        Long ram = vboxMachine.getMemorySize();
        Long vram = vboxMachine.getVRAMSize();
        String typeOfOS = guestOSType.getFamilyId();
        String identifierOfOS = guestOSType.getId();
        
        return new VirtualMachine.Builder(id, name, physicalMachine)
                                              .countOfCPU(cpuCount)
                                              .countOfMonitors(monitorCount)
                                              .cpuExecutionCap(cpuExecCap)
                                              .hardDiskFreeSpaceSize(hardDiskFreeSpaceSize)
                                              .hardDiskTotalSize(hardDiskTotalSize)
                                              .sizeOfRAM(ram)
                                              .sizeOfVRAM(vram)
                                              .typeOfOS(typeOfOS)
                                              .identifierOfOS(identifierOfOS)
                                              .build();
    }
    
    private boolean isLinkedClone(IMachine vboxMachine, IVirtualBox vbox){
        IMedium medium = getVMHardDisk(vboxMachine);
        if(medium == null){
            return false;
        }
        IMedium baseMedium = medium.getBase();
        
        return (!baseMedium.getMachineIds().get(0).equals(vboxMachine.getId()));        
    }
    
    private void checkLinkedCloneChildrenState(IMedium medium,
            VirtualBoxManager vbm, String checkedMachineId){
        List<IMedium> meds = medium.getChildren();
        
        if(!meds.isEmpty()){
            for(IMedium med : meds){                
                if(!med.getMachineIds().isEmpty()){
                    IMachine am = vbm.getVBox().findMachine(med.getMachineIds().get(0));
                    if(checkedMachineId == null || !am.getId().equals(checkedMachineId)){
                        if(am.getState() != MachineState.PoweredOff){
                            throw new UnexpectedVMStateException("Virtual machine removal operation failure: "
                                    + "Linked clone \"" + am.getName() + "\" is not powered off. All linked "
                                    + "clone of VM that is required to be removed must be powered "
                                    + "off to be ensured a correct VM removal.");
                        }
                        checkMediumStateForDeletion(med, vbm);
                        ISession session = vbm.getSessionObject();
                        try{
                           //try to exclusively lock the machine, if exception is invoked, then another process works with
                           //this machine concurrently and VM removal operation cannot be performed correctly with guarantee
                           am.lockMachine(session, LockType.Write);
                           session.unlockMachine();
                           while(session.getState() != SessionState.Unlocked){

                           }
                        }catch(VBoxException ex){
                            throw new UnexpectedVMStateException("Virtual machine removal operation failure: "
                                    + "With linked clone \"" + am.getName() + "\" is working another process at the moment. "
                                    + "There cannot be done any work with linked clone of VM that is required to be removed "
                                    + "while this VM is being removed.");
                        }
                    }
                    checkLinkedCloneChildrenState(med,vbm,am.getId());                
                }else{
                    checkMediumStateForDeletion(med, vbm);
                    checkLinkedCloneChildrenState(medium, vbm, null);
                }                
            }
        }
    }
    
    private void checkMediumStateForDeletion(IMedium medium, VirtualBoxManager vbm){
        boolean loop = false;
        String vboxMachineName = "";
        
        if(!medium.getMachineIds().isEmpty()){
            vboxMachineName = vbm.getVBox().findMachine(medium.getMachineIds().get(0)).getName();            
        }
        
        do{
            switch(medium.getState()){
                case Created:
                case NotCreated: loop = false;
                                 break;
                case Creating:{
                    throw new UnexpectedVMStateException("Virtual machine removal operation failure: "
                            + "VM \"" + vboxMachineName + "\" cannot be correctly removed, because "
                            + "its storage medium is just being created.");
                }
                case Deleting:{
                    throw new UnexpectedVMStateException("Virtual machine removal operation failure: "
                            + "Storage medium of VM \"" + vboxMachineName + "\" is already being deleted.");
                }
                case Inaccessible:{
                    if(!loop && medium.getLastAccessError().trim().isEmpty()){
                        loop = true;
                        MediumState mediumState = medium.refreshState();                        
                    }else{
                        throw new UnexpectedVMStateException("Virtual machine removal operation failure: "
                                + "Inaccessible storage medium of VM \"" + vboxMachineName + "\" -> "
                                + medium.getLastAccessError());
                    }
                }
                case LockedRead:{
                    throw new UnexpectedVMStateException("Virtual machine removal operation failure: "
                            + "VM \"" + vboxMachineName + "\" cannot be removed now, because "
                            + "its storage medium is locked for reading by another process, which is working "
                            + "with this storage medium at this moment, and cannot be correctly deleted.");
                }
                case LockedWrite:{
                    throw new UnexpectedVMStateException("Virtual machine removal operation failure: "
                            + "VM \"" + vboxMachineName + "\" cannot be removed now, because "
                            + "its storage medium is locked for writing by another process, which is working "
                            + "with this storage medium at this moment, and cannot be correctly deleted.");
                }
                default: throw new IllegalArgumentException("Illegal enumeration literal of type "
                        + "MediumState used while checking the medium state for VM removal operation.");
            }
        }while(loop);
    }
    
    private IMedium getParentMedium(IMedium medium, String vboxMachineId){        
        while(medium.getParent().getMachineIds().get(0).equals(vboxMachineId)){
            IMedium m = medium.getParent();
            medium = m;
        }
        
        return medium.getParent();
    }
    
    private void removeVMAsSnapshot(IMachine vboxMachine, VirtualBoxManager vbm){
        String machineName = vboxMachine.getName();
        IMedium medium = getVMHardDisk(vboxMachine);
        IMedium parentMedium;
        
        //not necessary to check if medium is null,, because if it was this method would not be called
        while(medium.getParent().getMachineIds().get(0).equals(vboxMachine.getId())){
            IMedium m = medium.getParent();
            medium = m;
        }        
        parentMedium = medium.getParent();
        
        checkMediumStateForDeletion(medium, vbm);
        checkLinkedCloneChildrenState(medium, vbm, null);
        removeLinkedCloneChildren(medium,vbm);
        removeVBoxMachine(vboxMachine);
        deleteSnapshot(parentMedium, vbm, machineName);
    }
    
    private void removeLinkedCloneChildren(IMedium medium, VirtualBoxManager vbm){
        List<IMedium> meds = medium.getChildren();
        IVirtualBox vbox = vbm.getVBox();
        IMachine machine = null;        
        
        if(!meds.isEmpty()){
            if(!medium.getMachineIds().isEmpty()){
                machine = vbox.findMachine(medium.getMachineIds().get(0));
            }
            for(IMedium med : meds){
                IMachine am = null;
                if(!med.getMachineIds().isEmpty()){
                    am = vbox.findMachine(med.getMachineIds().get(0));
                }
                if(am != null){
                    if(machine != null){
                        if(am.getId().equals(machine.getId())){
                            am = null;
                        }
                    }
                    removeLinkedCloneChildren(med,vbm);
                    if(am != null){
                        removeVBoxMachine(am);
                        deleteSnapshot(medium,vbm,am.getName());
                    }
                }else{
                    removeLinkedCloneChildren(med, vbm);
                    med.deleteStorage();
                }
            }
        }
    }
    
    private void removeVBoxMachine(IMachine vboxMachine){
        List<IMedium> mediums = vboxMachine.unregister(CleanupMode.DetachAllReturnHardDisksOnly);
        IProgress progress = vboxMachine.deleteConfig(mediums);
        while(!progress.getCompleted()){
            progress.waitForCompletion(200);
        }
        if(progress.getResultCode() != 0){
            progress = vboxMachine.deleteConfig(null);
            while(!progress.getCompleted()){
                progress.waitForCompletion(200);
            }
            if(progress.getResultCode() != 0){
                throw new UnexpectedVMStateException("Virtual machine removal operation failure: "
                        + "Failed to correctly remove configuration files of VM \"" + vboxMachine.getName() + "\".");
            }
        }
    }
    
    private void removeVMAsStandaloneUnit(IMachine vboxMachine, VirtualBoxManager vbm){
        IMedium medium = getVMHardDisk(vboxMachine);
        
        if(medium != null){
            if(medium.getParent() != null){
                while(medium.getParent().getMachineIds().get(0).equals(vboxMachine.getId())){
                    IMedium m = medium.getParent();
                    medium = m;
                    if(medium.getParent() == null){
                        break;
                    }
                }
            }
            checkMediumStateForDeletion(medium, vbm);
            checkLinkedCloneChildrenState(medium, vbm, null);
            removeLinkedCloneChildren(medium,vbm);
        }
        removeVBoxMachine(vboxMachine);
    }
    
    private void deleteSnapshot(IMedium parentMedium, VirtualBoxManager vbm, String machineName){
        IMachine parentMachine = vbm.getVBox().findMachine(parentMedium.getMachineIds().get(0));
        ISnapshot snapshot = parentMachine.findSnapshot(null);
        long snapshotCount = parentMachine.getSnapshotCount();
        
        for(long i = 0; i < snapshotCount; ++i){            
            ISnapshot tmp;
            if(snapshot == null){
                break;
            }
            if(!snapshot.getChildren().isEmpty()){
                tmp = snapshot.getChildren().get(0);
            }else{
                tmp = null;
            }
                        
            if(snapshot.getName().endsWith(machineName)){
                try{
                    ISession session = vbm.getSessionObject();
                    parentMachine.lockMachine(session, LockType.Shared);
                    IConsole console = session.getConsole();
                    IProgress progress = console.deleteSnapshot(snapshot.getId());
                    while(!progress.getCompleted()){
                        //do nothing, just loop until condition is true
                    }
                    if(progress.getResultCode() != 0){
                        String errMsg = progress.getErrorInfo().getText();
                        try{
                            session.unlockMachine();
                            while(session.getState() != SessionState.Unlocked){
                                //do nothing, just loop until the condition is true
                            }
                        }catch(VBoxException ex){
                            //not necessary to manage this exception, just means the machine has already been unlocked
                        }
                        throw new UnexpectedVMStateException("Virtual machine removal operation failure: " + errMsg);
                    }
                    session.unlockMachine();
                    while(session.getState() != SessionState.Unlocked){
                        //do nothing, just loop until the condition is true
                    }
                }catch(VBoxException ex){
                    throw new UnexpectedVMStateException("Virtual machine removal operation failure: " + ex.getMessage());
                }
            }
            
            snapshot = tmp;
        }
    }
    
    private void checkMediumStateForCloning(IMachine vboxMachine){
        boolean loop = false;
        IMedium medium = getVMHardDisk(vboxMachine);
        if(medium == null){
            throw new UnexpectedVMStateException("Virtual machine cloning operation failure: "
                    + "VM \"" + vboxMachine.getName() + "\" cannot be cloned, because it has "
                    + "no attached storage medium, which could be cloned.");
        }
        do{
            switch(medium.getState()){
                case Created   :
                case LockedRead: loop = false;
                                 break;
                case Creating:{
                    throw new UnexpectedVMStateException("Virtual machine cloning operation failure: "
                            + "VM \"" + vboxMachine.getName() + "\" cannot be cloned now, because its "
                            + "storage medium is just being created and cannot be cloned at this moment.");
                }
                case Deleting:{
                    throw new UnexpectedVMStateException("Virtual machine cloning operation failure: "
                            + "VM \"" + vboxMachine.getName() + "\" cannot be cloned now, because its "
                            + "storage medium is just being deleted and cannot be cloned.");
                }
                case Inaccessible:{
                    if(!loop && medium.getLastAccessError().trim().isEmpty()){
                        loop = true;
                        medium.refreshState();
                    }else{
                        throw new UnexpectedVMStateException("Virtual machine cloning operation failure: "
                                + "VM \"" + vboxMachine.getName() + "\" cannot be cloned now, because its "
                                + "storage medium is inaccessible now -> " + medium.getLastAccessError());
                    }
                }
                case LockedWrite:{
                    throw new UnexpectedVMStateException("Virtual machine cloning operation failure: "
                            + "VM \"" + vboxMachine.getName() + "\" cannot be cloned now, because its "
                            + "storage medium is just being locked for writing for another process, "
                            + "which is working with this storage medium now, and no other process "
                            + "can work with data of this storage medium nor it can be cloned at this "
                            + "moment.");
                }
                case NotCreated:{
                    throw new UnexpectedVMStateException("Virtual machine cloning operation failure: "
                            + "VM \"" + vboxMachine.getName() + "\" cannot be cloned now, because its "
                            + "storage medium is just being deleted and cannot be cloned.");
                }
                default: throw new IllegalArgumentException("Illegal enumeration literal of type "
                        + "MediumState used while checking state of medium of VM \"" 
                        + vboxMachine.getName() + "\".");
            }
        }while(loop);
    }
    
    private String getNewCloneName(String origName, IVirtualBox vbox, CloneType cloneType, String osTypeId){
        String sufix = null;
        int count = 1;
        IMachine auxMachine = null;
        String defMachFolder = vbox.getSystemProperties().getDefaultMachineFolder();
        boolean loop = false;
        
        switch(cloneType){
            case FULL_FROM_MACHINE_AND_CHILD_STATES :
            case FULL_FROM_MACHINE_STATE            :
            case FULL_FROM_ALL_STATES               : sufix = "_FullClone"; break;
            case LINKED                             : sufix = "_LinkClone"; break;
            default: throw new IllegalArgumentException("Cloning virtual machine " + origName + " failure: There was used illegal type of clone.");
        }

        String cloneName = origName + sufix + count;
        
        do{
            for(;;){
                try{
                    vbox.findMachine(cloneName);
                    ++count;
                    cloneName = origName + sufix + count;
                }catch(VBoxException ex){
                    break;
                }
            }
            File cloneFolder = new File(defMachFolder + "/" + cloneName);
            if(cloneFolder.isDirectory()){
                String[] files = cloneFolder.list();
                if(files.length == 0){
                    loop = false;
                }else{
                    for(int i = 0; i < files.length; ++i){
                        if(files[i].endsWith(".vbox") || files[i].endsWith(".vdi")){
                            loop = true;
                            ++count;
                            cloneName = origName + sufix + count;
                            break;
                        }else{
                            loop = false;
                        }
                    }
                }
            }else{
                loop = false;
            }
        }while(loop);
        
        return cloneName;
    }
    
    private void takeSnapshot(IMachine vboxMachine, ISession session, String cloneName){
        vboxMachine.lockMachine(session, LockType.Shared);
        IConsole console = session.getConsole();
        IProgress progress = console.takeSnapshot("Linked Base For " + vboxMachine.getName() 
                                                + " and " + cloneName, null);
        while(!progress.getCompleted()){
            //do nothing, just loop until snapshot is created
        }
        if(progress.getResultCode() != 0){
            try{
                session.unlockMachine();
                while(session.getState() != SessionState.Unlocked){
                    //do nothing, just loop until the condition is true
                }
            }catch(VBoxException ex){
                //session was already unlocked, do nothing
            }            
            throw new UnexpectedVMStateException("Virtual machine cloning operation failure: " 
                                            + progress.getErrorInfo().getText());
        }
        
        session.unlockMachine();
        while(session.getState() != SessionState.Unlocked){
            //do nothing, just loop until the condition is true
        }
    }
    
    private List<CloneOptions> getCloneOptions(CloneType cloneType){
        List<CloneOptions> cloneOptions = new ArrayList<>();
        
        if(cloneType == CloneType.LINKED){
            cloneOptions.add(CloneOptions.Link);
        }
        
        return cloneOptions;
    }
    
    private CloneMode getCloneMode(CloneType cloneType){
        switch(cloneType){
            case FULL_FROM_MACHINE_STATE            : return CloneMode.MachineState;
            case FULL_FROM_MACHINE_AND_CHILD_STATES : return CloneMode.MachineAndChildStates;
            case FULL_FROM_ALL_STATES               : return CloneMode.AllStates;
            case LINKED                             : return CloneMode.MachineState;
            default                                 : return CloneMode.MachineState;
        }
    }
}
