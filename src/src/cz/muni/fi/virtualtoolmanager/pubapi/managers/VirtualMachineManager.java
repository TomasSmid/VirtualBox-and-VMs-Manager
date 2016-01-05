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

import cz.muni.fi.virtualtoolmanager.pubapi.entities.PortRule;
import cz.muni.fi.virtualtoolmanager.pubapi.entities.VirtualMachine;
import cz.muni.fi.virtualtoolmanager.pubapi.types.FrontEndType;
import java.util.List;

/**
 * <div>
 * Interface that declare what method the implementation of the virtual machine
 * manager should provide.
 * </div>
 * <div>
 * By implementing the declared methods, it should be possible to remotely
 * control one virtual machine, e.g. virtual machine starting operation.
 * </div>
 * 
 * @see cz.muni.fi.virtualtoolmanager.pubapi.entities.VirtualMachine
 * @see cz.muni.fi.virtualtoolmanager.pubapi.types.FrontEndType
 * @see cz.muni.fi.virtualtoolmanager.pubapi.entities.PortRule
 * 
 * @author Tomáš Šmíd
 */
public interface VirtualMachineManager {
    
    /**
     * <div>
     * Method that the given virtual machine starts in the mode specified by
     * the given front-end type.
     * </div>
     * @param virtualMachine represents the virtual machine which is going to
     * be started
     * @param frontEndType represents the front-end type used for the virtual
     * machine
     */
    public void startVM(VirtualMachine virtualMachine, FrontEndType frontEndType);
    
    /**
     * <div>
     * Method that the given virtual machine shuts down.
     * </div>
     * @param virtualMachine represents the virtual machine which is going to be
     * shut down
     */
    public void shutDownVM(VirtualMachine virtualMachine);
    
    /**
     * <div>
     * Method that ensures the addition of a new port-forwarding rule to a single
     * virtual machine.
     * </div>
     * <div>
     * <p>
     * To use the port-forwarding rules the virtual machine must be attached to the
     * NAT network adapter.
     * <p>
     * The name of a port rule which should be added to a particular virtual
     * machine must be unique on that virtual machine accross the all its existing
     * port-forwarding rules. Also host port number can be used only with a one
     * port-forwarding rule on a single virtual machine.
     * </div>
     * @param virtualMachine represents the virtual machine to which the new
     * port-forwarding rule will be added
     * @param portRule represents the port rule, which should be added to the
     * virtual machine
     */
    public void addPortRule(VirtualMachine virtualMachine, PortRule portRule);
    
    /**
     * <div>
     * Method that deletes a particular port-forwarding rule from a particular
     * virtual machine.
     * </div>
     * <div>
     * To ensure the correct deletion of the specified port rule it is necessary
     * the port rule really exists for the virtual machine.
     * </div>
     * @param virtualMachine represents a particular virtual machine from which
     * the specified port-forwarding rule should be deleted
     * @param portRule represents the port rule which will be deleted from a 
     * particular virtual machine
     */
    public void deletePortRule(VirtualMachine virtualMachine, PortRule portRule);
    
    /**
     * <div>
     * Method that deletes all existing port-forwarding rules of a particular
     * virtual machine.
     * </div>
     * @param virtualMachine represents the virtual machine whose all port rules
     * will be deleted
     */
    public void deleteAllPortRules(VirtualMachine virtualMachine);
    
    /**
     * <div>
     * Method that retrieves all existing port rules from a particular virtual
     * machine.
     * </div>
     * @param virtualMachine represents the virtual machine from which should
     * be retrieved all its port rules
     * @return list of existing port rules of a particular virtual machine
     */
    public List<PortRule> getPortRules(VirtualMachine virtualMachine);
    
    /**
     * <div>
     * Method that gets the actual state of a particular virtual machine.
     * </div>
     * @param virtualMachine represents the virtual machine whose state should be
     * got
     * @return virtual machine state as string
     */
    public String getVMState(VirtualMachine virtualMachine);
    
}
