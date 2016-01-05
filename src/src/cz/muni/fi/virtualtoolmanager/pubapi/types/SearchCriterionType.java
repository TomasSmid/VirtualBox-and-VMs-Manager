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
 * Enumeration class that is used for creation of order in which the specified
 * search criteria should be searched. That search order or search criteria
 * priority is made by sequent addition of literals to the array. First added
 * literal is meant to be the prioritiest search criterion or virtual machine
 * required property. So the order of literal addition to the array is important.
 * 
 * @see cz.muni.fi.virtualtoolmanager.pubapi.managers.SearchManager
 * @see cz.muni.fi.virtualtoolmanager.logicimpl.SearchManagerImpl
 * 
 * @author Tomáš Šmíd
 */
public enum SearchCriterionType {
    /** uuid of virtual machine */
    ID,
    /** name of virtual machine */
    NAME,
    /** type of OS used in virtual machine */
    OS_TYPE,
    /** identifier of OS used in virtual machine */
    OS_IDENTIFIER,
    /** number of virtual CPUs used by virtual machine */
    CPU_COUNT,
    /** CPU execution limitation used by virtual machine */
    CPU_EXEC_CAP,
    /** free space size of virtual hard disk used by virtual machine */
    HDD_FREE_SPACE,
    /** total space size of virtual hard disk used by virtual machine */
    HDD_TOTAL_SIZE,
    /** size of memory used by virtual machine */
    RAM,
    /** size of video memory used by virtual machine */
    VRAM,
    /** number of monitors used by virtual machine */
    MONITOR_COUNT
}
