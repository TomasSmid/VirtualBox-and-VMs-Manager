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
package cz.muni.fi.virtualtoolmanager.pubapi.types;

/**
 * Enumeration class that is used for determining what type of virtual machine
 * clone should be created.
 * 
 * @see cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualizationToolManager
 * @see cz.muni.fi.virtualtoolmanager.logicimpl.VirtualizationToolManagerImpl
 * 
 * @author Tomáš Šmíd
 */
public enum CloneType {
    /** Virtual machine clone is created as full clone just from the actual 
     * state of original virtual machine*/
    FULL_FROM_MACHINE_STATE,
    /** Virtual machine clone is created as full clone from the actual state 
     * of the original virtual machine and its child states */
    FULL_FROM_MACHINE_AND_CHILD_STATES,
    /** Virtual machine clone is created as full clone from all states of the
     * original virtual machine */
    FULL_FROM_ALL_STATES,
    /** Virtual machine clone is created as a linked clone of the original
     * virtual machine */
    LINKED
}
