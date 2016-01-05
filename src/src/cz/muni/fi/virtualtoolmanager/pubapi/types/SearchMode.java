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
 * Enumeration class that is used for determining how precisely the virtual
 * machine search operation will be performed.
 * 
 * @see cz.muni.fi.virtualtoolmanager.pubapi.managers.SearchManager
 * @see cz.muni.fi.virtualtoolmanager.logicimpl.SearchManagerImpl
 * 
 * @author Tomáš Šmíd
 */
public enum SearchMode {
    /** Represents the search mode in which must the searched virtual machine 
     * be completely the same as that specified and required by search criteria,
     * with this search mode does not matter on the search order (priority)*/
    ABSOLUTE_EQUALITY,
    /** Represents the search mode in which the searched virtual machine has not
     * to be precisely the same as that specified and required by search criteria,
     * as the result of this search mode are searched those virtual machines,
     * which utmost match the search criteria */
    TOLERANT
}
