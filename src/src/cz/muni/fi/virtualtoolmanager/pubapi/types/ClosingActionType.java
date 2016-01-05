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
 * Enumeration class that is used for determining what final actions should be
 * performed before the physical machine is disconnected.
 * 
 * @see cz.muni.fi.virtualtoolmanager.pubapi.managers.ConnectionManager
 * @see cz.muni.fi.virtualtoolmanager.logicimpl.ConnectionManagerImpl
 * 
 * @author Tomáš Šmíd
 */
public enum ClosingActionType {
    /** No special actions specified */
    NONE,
    /** Before the physical machine is disconnected, all running virtual machines
     * should be shut down */
    SHUT_DOWN_RUNNING_VM
}
