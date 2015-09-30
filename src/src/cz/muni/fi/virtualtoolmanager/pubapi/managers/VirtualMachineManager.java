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
import java.util.List;

/**
 *
 * @author Tomáš Šmíd
 */
public interface VirtualMachineManager {
    
    public void startVM(VirtualMachine virtualMachine);
    
    public void shutDownVM(VirtualMachine virtualMachine);
    
    public void addPortRule(VirtualMachine virtualMachine, PortRule portRule);
    
    public void deletePortRule(VirtualMachine virtualMachine, PortRule portRule);
    
    public void deleteAllPortRules(VirtualMachine virtualMachine);
    
    public List<PortRule> getPortRules(VirtualMachine virtualMachine);
    
    public String getVMState(VirtualMachine virtualMachine);
    
}
