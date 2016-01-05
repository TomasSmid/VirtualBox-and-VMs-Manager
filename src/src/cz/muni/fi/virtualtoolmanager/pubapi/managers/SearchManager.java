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

import cz.muni.fi.virtualtoolmanager.pubapi.entities.SearchCriteria;
import cz.muni.fi.virtualtoolmanager.pubapi.entities.VirtualMachine;
import cz.muni.fi.virtualtoolmanager.pubapi.types.SearchCriterionType;
import cz.muni.fi.virtualtoolmanager.pubapi.types.SearchMode;
import java.util.List;

/**
 * <div>
 * Interface that declare what method the implementation of the search manager
 * should provide.
 * </div>
 * <div>
 * By implementing the declared methods, it should be possible to search virtual
 * machines according to the specified search criteria. 
 * </div>
 * 
 * @see cz.muni.fi.virtualtoolmanager.pubapi.entities.VirtualMachine
 * @see cz.muni.fi.virtualtoolmanager.pubapi.types.SearchCriterionType
 * @see cz.muni.fi.virtualtoolmanager.pubapi.entities.SearchCriteria
 * @see cz.muni.fi.virtualtoolmanager.pubapi.types.SearchMode
 * 
 * @author Tomáš Šmíd
 */
public interface SearchManager {
    
    /**
     * <div>
     * Method that searches the virtual machines according to the required
     * search criteria with a set up search criterion priority.
     * </div>
     * <div>
     * Using this method can be the work with virtual machines easier a faster.
     * </div>
     * @param searchCriteria represents the virtual machine properties which are
     * required for the searched virtual machine
     * @param mode specifies how precise the searching operation is
     * @param searchOrder represents the priority of each search criterion
     * @return list of matched virtual machines
     */
    public List<VirtualMachine> search(SearchCriteria searchCriteria, SearchMode mode,
                                       List<SearchCriterionType> searchOrder);
}
