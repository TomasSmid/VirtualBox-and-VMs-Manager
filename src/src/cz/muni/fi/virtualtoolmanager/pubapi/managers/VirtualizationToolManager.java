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
package cz.muni.fi.virtualtoolmanager.pubapi.managers;

import cz.muni.fi.virtualtoolmanager.pubapi.entities.VirtualMachine;
import cz.muni.fi.virtualtoolmanager.pubapi.types.CloneType;
import java.util.List;
import java.util.UUID;

/**
 * <div>
 * Interface that declare what method the implementation of the virtualization
 * tool manager should provide.
 * </div>
 * <div>
 * By implementing the declared methods, it should be possible to remotely
 * control one or more virtual machine according to the used operation.
 * </div>
 * 
 * @see cz.muni.fi.virtualtoolmanager.pubapi.entities.VirtualMachine
 * @see cz.muni.fi.virtualtoolmanager.pubapi.types.CloneType
 * 
 * @author Tomáš Šmíd
 */
public interface VirtualizationToolManager {
    
    /**
     * <div>
     * Method that ensures the registration of a particular virtual machine at
     * virtualization tool VirtualBox.
     * </div>
     * <div>
     * <p>
     * To ensure the correct performing of this operation the virtual machine
     * must be located on the remote physical machine on the required path:
     * "defaultVirtualBoxMachineFolder\\vmname\vmname.vbox" and in the folder
     * "vmname" must be present all neccessary virtual hard disks which are
     * attached to the virtual machine.
     * <p>
     * If the method is finished successfully, the registered virtual machine
     * should be available for all possible operations.
     * </div>
     * @param name represents the name of virtual machine which is going to be
     * registered
     */
    public void registerVirtualMachine(String name);
    
    /**
     * <div>
     * Method that retrieves a particular virtual machine by its uuid.
     * </div>
     * @param id represents the uuid of the required virtual machine
     * @return matched virtual machine
     */
    public VirtualMachine findVirtualMachineById(UUID id);
    
    /**
     * <div>
     * Method that retrieves a particular virtual machine by its name.
     * </div>
     * @param name represents the name of the required virtual machine
     * @return matched virtual machine
     */
    public VirtualMachine findVirtualMachineByName(String name);
    
    /**
     * <div>
     * Method that gets all registered virtual machines from a particular
     * physical machine.
     * </div>
     * @return list of all registered virtual machines from a particular physical
     * machine
     */
    public List<VirtualMachine> getVirtualMachines();
    
    /**
     * <div>
     * Method that ensures the complete virtual machine removal from the physical
     * machine.
     * </div>
     * @param virtualMachine represents the virtual machine which is going to be
     * removed
     */
    public void removeVirtualMachine(VirtualMachine virtualMachine);
    
    /**
     * <div>
     * Method that creates the virtual machine clone from the given virtual
     * machine and according to specified clone type.
     * </div>
     * @param virtualMachine represents the virtual machine which will be cloned
     * @param type represents the type of clone
     * @return newly created virtual machine clone
     */
    public VirtualMachine cloneVirtualMachine(VirtualMachine virtualMachine, CloneType type);
    
    /**
     * <div>
     * Method that ensures all running virtual machines on a particular physical
     * machine are shut down.
     * </div>
     * <div>
     * This method is generaly meant to be used together with
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.ConnectionManager#close() 
     * ConnectionManager::close()} method.
     * </div>
     */
    public void close();
}
