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
import java.util.List;
import org.virtualbox_4_3.VirtualBoxManager;



/**
 *
 * @author Tomáš Šmíd
 */
class NativeVBoxAPIConnection {
    
    private final VirtualBoxManager virtualBoxManager;
    
    public NativeVBoxAPIConnection(){
        this(VirtualBoxManager.createInstance(null));
    }
    
    NativeVBoxAPIConnection(VirtualBoxManager virtualBoxManager){
        this.virtualBoxManager = virtualBoxManager;
    }
    
    public void connectTo(PhysicalMachine physicalMachine){
            throw new UnsupportedOperationException("Unsupported operation");
    }
    
    /*public void disconnectFrom(PhysicalMachine physicalMachine){
        throw new UnsupportedOperationException("Unsupported operation");
    }
    
    public boolean isConnected(PhysicalMachine physicalMachine){
        throw new UnsupportedOperationException("Unsupported operation");
    }
    
    public List<PhysicalMachine> getConnectedPhysicalMachines(){
        throw new UnsupportedOperationException("Unsupported operation");
    }
    
    VirtualBoxManager getVirtualBoxManager(PhysicalMachine physicalMachine, String errMsg){
        throw new UnsupportedOperationException("Unsupported operation");
    }*/
    
    /*private void checkPMIsNotNull(PhysicalMachine pm, String errMsg){
        if(pm == null){
            throw new IllegalArgumentException(errMsg);
        }
    }
    
    private VirtualBoxManager validateConnectionToPM(PhysicalMachine pm, String partOfErrMsg) 
            throws ConnectionFailureException, IncompatibleVirtToolAPIVersionException{
        
        String url = "http://" + pm.getAddressIP() + ":" + pm.getPortOfVTWebServer();
        VirtualBoxManager vbm = VirtualBoxManager.createInstance(null);
        IVirtualBox vbox = null;        
        
        try{
            vbm.connect(url, pm.getUsername(), pm.getUserPassword());
            vbox = vbm.getVBox();
        }catch(VBoxException ex){
            throw new ConnectionFailureException(partOfErrMsg + "Most probably there "
                    + "could be one of two possible problems - "
                    + "network connection is not working or remote VirtualBox "
                    + "web server is not running.");
        }
        
        if(!vbox.getAPIVersion().equals("4_3")){
            throw new IncompatibleVirtToolAPIVersionException("Incompatible version of "
                    + "VirtualBox API: Required VBox API version is 4_3, but actual "
                    + "VirtualBox API version is " + vbox.getAPIVersion() + ". "
                    + "There is no guarantee this API would work with incompatible "
                    + "VirtualBox API version correctly, that's why this physical machine "
                    + " has not been connected and thus cannot be operated with.");
        }
        
        return vbm;
    }
    
    private VirtualBoxManager tryToConnectTo(PhysicalMachine pm, String partOfErrMsg) 
            throws ConnectionFailureException, InterruptedException, IncompatibleVirtToolAPIVersionException{
        
        VirtualBoxManager vbm = null;        
        AccessedPhysicalMachines apm = AccessedPhysicalMachines.getInstance();
        int attempt = 0;
        
        while(attempt < 3){
            try{
                vbm = validateConnectionToPM(pm,partOfErrMsg);
                break;
            }catch(ConnectionFailureException ex){
                ++attempt;
                if(attempt == 3){
                    apm.remove(pm);//if connected, then will be removed, otherwise nothing will happen
                    throw ex;
                }else{
                    Thread.sleep(5000l);
                }
            }catch(IncompatibleVirtToolAPIVersionException ex){
                apm.remove(pm);//if connected, then will be removed, otherwise nothing will happen 
                throw ex;
            }
        }
        
        return vbm;
    }
    
    private void stopRunningVMs(PhysicalMachine pm){
        VirtualizationToolManager vtm = new VirtualizationToolManagerImpl(pm);
        List<VirtualMachine> vms = vtm.getVirtualMachines();
        VirtualMachineManager vmm = vtm.getVirtualMachineManager();
        
        if(!vms.isEmpty()){
            for(VirtualMachine vm : vms){
                String ms = vmm.getVMState(vm);
                if (ms.equals("Running") || ms.equals("Paused") || ms.equals("Stuck")) {
                    vmm.shutDownVM(vm);
                }
            }
        }
    }*/
}
