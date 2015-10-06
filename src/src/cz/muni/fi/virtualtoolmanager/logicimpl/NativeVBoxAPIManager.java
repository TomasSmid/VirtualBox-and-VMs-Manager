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
import cz.muni.fi.virtualtoolmanager.pubapi.entities.VirtualMachine;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnexpectedVMStateException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownVirtualMachineException;
import cz.muni.fi.virtualtoolmanager.pubapi.types.CloneType;
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
import org.virtualbox_4_3.IProgress;
import org.virtualbox_4_3.ISession;
import org.virtualbox_4_3.ISnapshot;
import org.virtualbox_4_3.ISystemProperties;
import org.virtualbox_4_3.IVirtualBox;
import org.virtualbox_4_3.LockType;
import org.virtualbox_4_3.MachineState;
import org.virtualbox_4_3.SessionState;
import org.virtualbox_4_3.VBoxException;
import org.virtualbox_4_3.VirtualBoxManager;



/**
 *
 * @author Tomáš Šmíd
 */
class NativeVBoxAPIManager {
    
    /**
     * 
     * 
     * @param physicalMachine
     * @param name
     * @return
     * @throws ConnectionFailureException
     * @throws UnknownVirtualMachineException 
     */
    public boolean registerVirtualMachine(PhysicalMachine physicalMachine, String name) throws ConnectionFailureException,
                                                                                               UnknownVirtualMachineException {
        String url = "http://" + physicalMachine.getAddressIP() + ":" + physicalMachine.getPortOfVTWebServer();
        boolean vmIsRegistered = true;
        VirtualBoxManager virtualBoxManager = VirtualBoxManager.createInstance(null);
        
        try{
            virtualBoxManager.connect(url, physicalMachine.getUsername(), physicalMachine.getUserPassword());
        }catch(VBoxException ex){
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw new ConnectionFailureException(getErrorMessage(0, physicalMachine, name));
        }
        
        IVirtualBox vbox = virtualBoxManager.getVBox();        
        IMachine vboxMachine = null;
        try{
            vboxMachine = vbox.findMachine(name);
        }catch(VBoxException ex){
            vmIsRegistered = false;
        }
        
        if(!vmIsRegistered){
            ISystemProperties systemProperties = vbox.getSystemProperties();
            try{
                vboxMachine = vbox.openMachine(systemProperties.getDefaultMachineFolder() + "\\" + name + "\\" + name + ".vbox");
            }catch(VBoxException ex){
                virtualBoxManager.disconnect();
                virtualBoxManager.cleanup();
                throw new UnknownVirtualMachineException(getErrorMessage(1, physicalMachine, name));
            }
            
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
     * 
     * 
     * @param physicalMachine
     * @param nameOrId
     * @return
     * @throws ConnectionFailureException
     * @throws UnknownVirtualMachineException 
     */
    public VirtualMachine getVirtualMachine(PhysicalMachine physicalMachine, String nameOrId) throws ConnectionFailureException,
                                                                                                     UnknownVirtualMachineException {
        String url = "http://" + physicalMachine.getAddressIP() + ":" + physicalMachine.getPortOfVTWebServer();
        VirtualBoxManager virtualBoxManager = VirtualBoxManager.createInstance(null);
        
        try{
            virtualBoxManager.connect(url, physicalMachine.getUsername(), physicalMachine.getUserPassword());
        }catch(VBoxException ex){
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw new ConnectionFailureException(getErrorMessage(2, physicalMachine, nameOrId));
        }
        
        IVirtualBox vbox = virtualBoxManager.getVBox();        
        IMachine vboxMachine;
        try{
            vboxMachine = vbox.findMachine(nameOrId);
        }catch(VBoxException ex){
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            //without error message - in this case has not to be VM found out
            throw new UnknownVirtualMachineException();
        }
        
        IGuestOSType guestOSType = vbox.getGuestOSType(vboxMachine.getOSTypeId());
        VirtualMachine virtualMachine = getConvertedVM(vboxMachine, guestOSType, physicalMachine);
        
        virtualBoxManager.disconnect();
        virtualBoxManager.cleanup();
        return virtualMachine;
    }
    
    public List<VirtualMachine> getAllVirtualMachines(PhysicalMachine physicalMachine) throws ConnectionFailureException,
                                                                                              UnknownVirtualMachineException {
        String url = "http://" + physicalMachine.getAddressIP() + ":" + physicalMachine.getPortOfVTWebServer();
        VirtualBoxManager virtualBoxManager = VirtualBoxManager.createInstance(null);
        
        try{
            virtualBoxManager.connect(url, physicalMachine.getUsername(), physicalMachine.getUserPassword());
        }catch(VBoxException ex){
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw new ConnectionFailureException(getErrorMessage(3, physicalMachine, ""));
        }
        
        IVirtualBox vbox = virtualBoxManager.getVBox();        
        List<IMachine> vboxMachines = null;
        try{
            vboxMachines = vbox.getMachines();
        }catch(VBoxException ex){
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw new UnknownVirtualMachineException(getErrorMessage(4, physicalMachine, "") + ex.getMessage());
        }
        
        if(vboxMachines.isEmpty()){
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            return new ArrayList<>();
        }        
        
        List<VirtualMachine> virtualMachines = new ArrayList<>();
        for(IMachine vboxMachine : vboxMachines){
            IGuestOSType guestOSType = vbox.getGuestOSType(vboxMachine.getOSTypeId());
            virtualMachines.add(getConvertedVM(vboxMachine, guestOSType, physicalMachine));
        }
        
        virtualBoxManager.disconnect();
        virtualBoxManager.cleanup();
        return virtualMachines;
    }
    
    public void removeVirtualMachine(VirtualMachine virtualMachine) throws ConnectionFailureException,
                                                                           UnknownVirtualMachineException,
                                                                           UnexpectedVMStateException {
        String url = "http://" + virtualMachine.getHostMachine().getAddressIP() + ":" 
                + virtualMachine.getHostMachine().getPortOfVTWebServer();
        String username = virtualMachine.getHostMachine().getUsername();
        String userPassword = virtualMachine.getHostMachine().getUserPassword();
        VirtualBoxManager virtualBoxManager = VirtualBoxManager.createInstance(null);
        
        try{
            virtualBoxManager.connect(url, username, userPassword);
        }catch(VBoxException ex){
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw new ConnectionFailureException(getErrorMessage(5, virtualMachine.getHostMachine(),
                                                 virtualMachine.getName()));
        }
        
        IVirtualBox vbox = virtualBoxManager.getVBox();        
        IMachine vboxMachine = null;
        try{
            vboxMachine = vbox.findMachine(virtualMachine.getId().toString());
        }catch(VBoxException ex){
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw new UnknownVirtualMachineException(getErrorMessage(6, virtualMachine.getHostMachine(),
                                                     virtualMachine.getName()));
        }
        
        if(!vboxMachine.getAccessible()){
            try{
                vboxMachine.unregister(CleanupMode.DetachAllReturnHardDisksOnly);
            }catch(VBoxException ex){
                /*machine was not registered -> this should not normally happen, because if the machine
                was not registered, then the VBoxException would be invoked earlier at vbox.findMachine()*/
                throw new IllegalStateException(ex);
            }
        }else{
            if(vboxMachine.getState() != MachineState.PoweredOff){
                throw new UnexpectedVMStateException(getErrorMessage(7, virtualMachine.getHostMachine(),
                                                     virtualMachine.getName()));
            }

            if(isLinkedClone(vboxMachine, vbox)){
                ISession session = virtualBoxManager.getSessionObject();
                removeVMAsSnapshot(vboxMachine, vbox, session);
            }else{
                removeVMAsStandaloneUnit(vboxMachine, vbox);
            }
        }
        
        virtualBoxManager.disconnect();
        virtualBoxManager.cleanup();
    }
    
    public VirtualMachine createVMClone(VirtualMachine virtualMachine, CloneType cloneType) throws ConnectionFailureException,
                                                                                                   UnknownVirtualMachineException,
                                                                                                   UnexpectedVMStateException {
        String url = "http://" + virtualMachine.getHostMachine().getAddressIP() + ":" 
                + virtualMachine.getHostMachine().getPortOfVTWebServer();
        String username = virtualMachine.getHostMachine().getUsername();
        String userPassword = virtualMachine.getHostMachine().getUserPassword();
        VirtualBoxManager virtualBoxManager = VirtualBoxManager.createInstance(null);
        
        try{
            virtualBoxManager.connect(url, username, userPassword);
        }catch(VBoxException ex){
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw new ConnectionFailureException(getErrorMessage(8, virtualMachine.getHostMachine(),
                                                 virtualMachine.getName()));
        }
        
        IVirtualBox vbox = virtualBoxManager.getVBox();        
        IMachine vboxMachine = null;
        try{
            vboxMachine = vbox.findMachine(virtualMachine.getId().toString());
        }catch(VBoxException ex){
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw new UnknownVirtualMachineException(getErrorMessage(9, virtualMachine.getHostMachine(),
                                                     virtualMachine.getName()));
        }
        
        if(!vboxMachine.getAccessible()){
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
            throw new UnexpectedVMStateException(getErrorMessage(10, virtualMachine.getHostMachine(),
                                                 virtualMachine.getName()) + vboxMachine.getAccessError().getText());
        }
        
        switch(vboxMachine.getState()){
            case PoweredOff:
            case Saved     :
            case Running   :
            case Paused    : break;
            default        : throw new UnexpectedVMStateException(getErrorMessage(11, virtualMachine.getHostMachine(),
                                                                  virtualMachine.getName()));
        }
        
        String cloneName = getNewCloneName(vboxMachine.getName(), vbox, cloneType);
        
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
                throw new UnexpectedVMStateException("Cloning virtual machine " + virtualMachine + " on physical machine " + virtualMachine.getHostMachine() + " failure: " + ex.getMessage());
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
        while(!progress.getCompleted()){
            if(progress.getPercent() > progressPercent){
                progressPercent = progress.getPercent();
                outputHandler.printMessage("Cloning progress > " + progressPercent + "%");
            }
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
        String[] errMessages = /* 00 */{"Connection operation failure while trying to register virtual machine " + vmNameOrId + ": Unable to connect to the physical machine " + physicalMachine + ". Most probably there occured one of these problems: 1. Network connection is not working properly or at all / 2. The VirtualBox web "
                                        + " server is not running / 3. One of the key value (IP address, number of web server port, username or user password) of the physical machine has been changed and it is incorrect now (used value is not the actual correct one).",
                               /* 01 */ "Virtual machine registration operation failure: There is no virtual machine folder named \"" + vmNameOrId + "\" with configuration file \"" + vmNameOrId + ".vbox\" in VirtualBox default machine folder \"VirtualBox VMs\".",
                               /* 02 */ "Connection operation failure while trying to retrieve virtual machine " + vmNameOrId + ": Unable to connect to the physical machine " + physicalMachine + ". Most probably there occured one of these problems: 1. Network connection is not working properly or at all / 2. The VirtualBox web "
                                        + " server is not running / 3. One of the key value (IP address, number of web server port, username or user password) of the physical machine has been changed and it is incorrect now (used value is not the actual correct one).",                               
                               /* 03 */ "Connection operation failure while trying to retrieve all virtual machines: Unable to connect to the physical machine " + physicalMachine + ". Most probably there occured one of these problems: 1. Network connection is not working properly or at all / 2. The VirtualBox web "
                                        + " server is not running / 3. One of the key value (IP address, number of web server port, username or user password) of the physical machine has been changed and it is incorrect now (used value is not the actual correct one).",
                               /* 04 */ "All virtual machines retrieve operation failure: There did not manage to retrieve virtual machines from physical machine " + physicalMachine + " -> ",
                               /* 05 */ "Connection operation failure while trying to remove virtual machine " + vmNameOrId + ": Unable to connect to the physical machine " + physicalMachine + ". Most probably there occured one of these problems: 1. Network connection is not working properly or at all / 2. The VirtualBox web "
                                        + " server is not running / 3. One of the key value (IP address, number of web server port, username or user password) of the physical machine has been changed and it is incorrect now (used value is not the actual correct one).",
                               /* 06 */ "Virtual machine removal operation failure: There is no virtual machine " + vmNameOrId + " on physical machine " + physicalMachine + ". Nonexistent virtual machine cannot be removed.",
                               /* 07 */ "Virtual machine removal operation failure: Virtual machine " + vmNameOrId + " from physical machine " + physicalMachine + " cannot be removed, because it is not powered off.",
                               /* 08 */ "Connection operation failure while trying to clone virtual machine " + vmNameOrId + ": Unable to connect to the physical machine " + physicalMachine + ". Most probably there occured one of these problems: 1. Network connection is not working properly or at all / 2. The VirtualBox web "
                                        + " server is not running / 3. One of the key value (IP address, number of web server port, username or user password) of the physical machine has been changed and it is incorrect now (used value is not the actual correct one).",
                               /* 09 */ "Virtual machine cloning operation failure: There is no virtual machine " + vmNameOrId + " on physical machine " + physicalMachine + ". Nonexistent virtual machine cannot be cloned.",
                               /* 10 */ "Virtual machine cloning operation failure: Virtual machine " + vmNameOrId + " on physical machine " + physicalMachine + " cannot be cloned, because it is not accessible -> ",
                               /* 11 */ "Virtual machine cloning operation failure: Virtual machine " + vmNameOrId + " on physical machine " + physicalMachine + " cannot be cloned, because it is not in a valid state for cloning. The required states are: \"PoweredOff\", \"Saved\", \"Running\", \"Paused\"."};
        
        return errMessages[index];
    }
    
    private VirtualMachine getConvertedVM(IMachine vboxMachine, IGuestOSType guestOSType,
                                          PhysicalMachine physicalMachine){
        IMedium medium = vboxMachine.getMedium("SATA", 0, 0);
        UUID id = UUID.fromString(vboxMachine.getId());
        String name = vboxMachine.getName();
        Long cpuCount = vboxMachine.getCPUCount();
        Long monitorCount = vboxMachine.getMonitorCount();
        Long cpuExecCap = vboxMachine.getCPUExecutionCap();
        Long hardDiskFreeSpaceSize = medium.getLogicalSize()-medium.getSize();
        Long hardDiskTotalSize = medium.getLogicalSize();
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
        IMedium baseMedium = vboxMachine.getMedium("SATA", 0, 0).getBase();
        IMachine baseVBoxMachine = vbox.findMachine(baseMedium.getMachineIds().get(0));
        
        return (vboxMachine.getName().contains("_LinkClone") || !baseVBoxMachine.getId().equals(vboxMachine.getId()));
    }
    
    private void removeVMAsSnapshot(IMachine vboxMachine, IVirtualBox vbox, ISession session){
        String machineName = vboxMachine.getName();
        IMedium medium = vboxMachine.getMedium("SATA", 0, 0);
        IMedium parentMedium;
        
        while(medium.getParent().getMachineIds().get(0).equals(vboxMachine.getId())){
            IMedium m = medium.getParent();
            medium = m;
        }        
        parentMedium = medium.getParent();
        
        removeLinkedCloneChildren(medium,vbox);
        removeVBoxMachine(vboxMachine);
        deleteSnapshot(parentMedium, vbox, machineName, session);
    }
    
    private void removeLinkedCloneChildren(IMedium medium, IVirtualBox vbox){
        List<IMedium> meds = medium.getChildren();
        IMachine machine = vbox.findMachine(medium.getMachineIds().get(0));
        
        if(!meds.isEmpty()){
            for(IMedium med : meds){
                IMachine am = vbox.findMachine(med.getMachineIds().get(0));
                if(am.getId().equals(machine.getId())){
                    am = null;
                }
                removeLinkedCloneChildren(med,vbox);
                if(am != null){
                    removeVBoxMachine(am);
                }
            }
        }
    }
    
    private void removeVBoxMachine(IMachine vboxMachine){
        List<IMedium> mediums = vboxMachine.unregister(CleanupMode.DetachAllReturnHardDisksOnly);
        vboxMachine.deleteConfig(mediums);
    }
    
    private void removeVMAsStandaloneUnit(IMachine vboxMachine, IVirtualBox vbox){
        IMedium medium = vboxMachine.getMedium("SATA", 0, 0);
        
        if(medium.getParent() != null){
            while(medium.getParent().getMachineIds().get(0).equals(vboxMachine.getId())){
                IMedium m = medium.getParent();
                medium = m;
                if(medium.getParent() == null){
                    break;
                }
                if(medium.getParent().getMachineIds() == null){
                    break;
                }
                if(medium.getParent().getMachineIds().get(0) == null){
                    break;
                }
            }
        }
        
        removeLinkedCloneChildren(medium,vbox);
        removeVBoxMachine(vboxMachine);
    }
    
    private void deleteSnapshot(IMedium parentMedium, IVirtualBox vbox, String machineName, ISession session){
        IMachine parentMachine = vbox.findMachine(parentMedium.getMachineIds().get(0));
        ISnapshot snapshot = parentMachine.findSnapshot(null);
        long snapshotCount = parentMachine.getSnapshotCount(); 
        
        for(long i = 0; i < snapshotCount; ++i){            
            ISnapshot tmp;
            if(snapshot == null){
                break;
            }
            if(!snapshot.getChildren().isEmpty()){//pridat prvne kontrolu na snapshot == null
                tmp = snapshot.getChildren().get(0);
            }else{
                tmp = null;
            }
            
            if(snapshot.getName().contains(machineName)){
                parentMachine.lockMachine(session, LockType.Write);
                IConsole console = session.getConsole();
                IProgress p = console.deleteSnapshot(snapshot.getId());
                while(!p.getCompleted()){
                    //do nothing, just loop until condition is true
                }
                session.unlockMachine();
                while(session.getState() != SessionState.Unlocked){
                    //do nothing, just loop until the condition is true
                }
                //mozna sem pridat break
            }
            
            snapshot = tmp;
        }
    }
    
    private String getNewCloneName(String origName, IVirtualBox vbox, CloneType cloneType){
        String sufix = null;
        
        switch(cloneType){
            case FULL_FROM_MACHINE_AND_CHILD_STATES :
            case FULL_FROM_MACHINE_STATE            :
            case FULL_FROM_ALL_STATES               : sufix = "_FullClone"; break;
            case LINKED                             : sufix = "_LinkClone"; break;
            default: throw new IllegalArgumentException("Cloning virtual machine " + origName + " failure: There was used illegal type of clone.");
        }
        
        int count = 1;
        String cloneName = origName + sufix + count;
        
        /*for(;;){
            try{
                vbox.findMachine(cloneName);
                break;
            }catch(VBoxException ex){
                ++count;
                cloneName = origName + sufix + count;
            }
        }*/
        for(;;){
            try{
                vbox.findMachine(cloneName);
                ++count;
                cloneName = origName + sufix + count;
            }catch(VBoxException ex){
                break;
            }
        }
        
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
