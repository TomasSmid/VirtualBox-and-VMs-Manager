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
import cz.muni.fi.virtualtoolmanager.pubapi.types.CloneType;
import java.util.List;
import java.util.UUID;
import org.virtualbox_4_3.VirtualBoxManager;



/**
 *
 * @author Tomáš Šmíd
 */
final class NativeVBoxAPIManager {
    
    private VirtualBoxManager virtualBoxManager;
    
    public NativeVBoxAPIManager(){
        this(VirtualBoxManager.createInstance(null));
    }
    
    NativeVBoxAPIManager(VirtualBoxManager virtualBoxManager){
        this.virtualBoxManager = virtualBoxManager;
    }
    
    
    public boolean registerVirtualMachine(PhysicalMachine physicalMachine, String name) {
        throw new UnsupportedOperationException("Unsupported operation");
    }
    
    public VirtualMachine getVirtualMachineById(PhysicalMachine physicalMachine, UUID id) {
        throw new UnsupportedOperationException("Unsupported operation");
    }
    
    public VirtualMachine getVirtualMachineByName(PhysicalMachine physicalMachine, String name) {
        throw new UnsupportedOperationException("Unsupported operation");
    }
    
    public List<VirtualMachine> getVirtualMachines(PhysicalMachine physicalMachine) {
        throw new UnsupportedOperationException("Unsupported operation");
    }
    
    public void removeVirtualMachine(VirtualMachine virtualMachine) {
        throw new UnsupportedOperationException("Unsupported operation");
    }
    
    public VirtualMachine createVMClone(VirtualMachine virtualMachine, CloneType cloneType) {
        throw new UnsupportedOperationException("Unsupported operation");
    }
    
    /*private void checkPMIsNotNull(PhysicalMachine pm, String errMsg){
        if(pm == null){
            throw new IllegalArgumentException(errMsg);
        }
    }
    
    private void checkVMIsNotNull(VirtualMachine vm, String errMsg){
        if(vm == null){
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
    
    private void checkCloneTypeIsNotNull(CloneType cloneType, String errMsg){
        if(cloneType == null){
            throw new IllegalArgumentException(errMsg);
        }
    }
    
    private void checkVMStateForCloning(MachineState state, String errMsg) throws UnexpectedVMStateException{
        switch(state){
            case PoweredOff:
            case Saved     :
            case Running   :
            case Paused    : break;
            default        : throw new UnexpectedVMStateException(errMsg);
        }
    }
    
    private void checkVMStateForRemoving(MachineState state, String errMsg) throws UnexpectedVMStateException{
        if(state != MachineState.PoweredOff){
            throw new UnexpectedVMStateException(errMsg);
        }
    }
    
    private VirtualMachine createVirtualMachine(IMachine vboxMachine, IGuestOSType gost, PhysicalMachine pm){
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
        String typeOfOS = gost.getFamilyId();
        String identifierOfOS = gost.getId();
        
        VirtualMachine vm = new VirtualMachine.Builder(id, name, pm)
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
        return vm;
    }
    
    private VirtualMachine getVM(PhysicalMachine pm, String key, String[] errMsgs) throws InterruptedException,
             ConnectionFailureException, IncompatibleVirtToolAPIVersionException, UnknownVirtualMachineException{
        
        NativeVBoxAPIConnection natapiCon = NativeVBoxAPIConnection.getInstance();
        
        VirtualBoxManager vbm = natapiCon.getVirtualBoxManager(pm, errMsgs[0]);
        IVirtualBox vbox = vbm.getVBox();
        IMachine vboxMachine = null;
        IGuestOSType gost = null;
        
        try{
            vboxMachine = vbox.findMachine(key);
            gost = vbox.getGuestOSType(vboxMachine.getOSTypeId());
        }catch(VBoxException ex){
            vbm.disconnect();
            vbm.cleanup();
            throw new UnknownVirtualMachineException(errMsgs[1]);
        }
        
        VirtualMachine vm = createVirtualMachine(vboxMachine,gost,pm);
        vbm.disconnect();
        vbm.cleanup();
        
        return vm;
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
        
        while(medium.getParent().getMachineIds().get(0).equals(vboxMachine.getId())){
            IMedium m = medium.getParent();
            medium = m;
        }
        
        removeLinkedCloneChildren(medium,vbox);
        removeVBoxMachine(vboxMachine);
    }
    
    private void deleteSnapshot(IMedium parentMedium, IVirtualBox vbox, String machineName, ISession session){
        IMachine parentMachine = vbox.findMachine(parentMedium.getMachineIds().get(0));
        ISnapshot snapshot = parentMachine.findSnapshot(null);
        
        for(long i = 0; i < parentMachine.getSnapshotCount(); ++i){            
            ISnapshot tmp;
            if(!snapshot.getChildren().isEmpty()){
                tmp = snapshot.getChildren().get(0);
            }else{
                tmp = null;
            }
            
            if(snapshot.getName().contains(machineName)){
                parentMachine.lockMachine(session, LockType.Write);
                IConsole console = session.getConsole();
                IProgress p = console.deleteSnapshot(snapshot.getId());
                while(!p.getCompleted()){
                    //do nothing, just loop while condition is true
                }
                session.unlockMachine();
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
        
        for(;;){
            try{
                vbox.findMachine(cloneName);
                break;
            }catch(VBoxException ex){
                ++count;
                cloneName = origName + sufix + count;
            }
        }
        
        return cloneName;
        
    }
    
    private void takeSnapshot(IMachine vboxMachine, ISession session, String cloneName){
        vboxMachine.lockMachine(session, LockType.Shared);
        IConsole c = session.getConsole();
        IProgress p = c.takeSnapshot("Linked Base For " + vboxMachine.getName() + " and " + cloneName, null);
        while(!p.getCompleted()){
            //do nothing, just loop until snapshot is created
        }
        session.unlockMachine();
    }
    
    private List<CloneOptions> getCloneOptions(CloneType cloneType){
        List<CloneOptions> clops = new ArrayList<>();
        
        if(cloneType == CloneType.LINKED){
            clops.add(CloneOptions.Link);
        }
        
        return clops;
    }
    
    private CloneMode getCloneMode(CloneType cloneType){
        switch(cloneType){
            case FULL_FROM_MACHINE_STATE            : return CloneMode.MachineState;
            case FULL_FROM_MACHINE_AND_CHILD_STATES : return CloneMode.MachineAndChildStates;
            case FULL_FROM_ALL_STATES               : return CloneMode.AllStates;
            case LINKED                             : return CloneMode.MachineState;
            default                                 : return CloneMode.MachineState;
        }
    }*/
}
