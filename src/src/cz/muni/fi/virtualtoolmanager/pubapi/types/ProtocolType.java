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
 * Enumeration class that is used for determining what protocol should be used
 * for traffic during port-forwarding.
 * 
 * @see cz.muni.fi.virtualtoolmanager.pubapi.entities.PortRule
 * @see cz.muni.fi.virtualtoolmanager.logicimpl.VirtualMachineManagerImpl
 * 
 * @author Tomáš Šmíd
 */
public enum ProtocolType {
    /** Represents TCP protocol type */
    TCP,
    /** Represents UDP protocol type */
    UDP
}
