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
import cz.muni.fi.virtualtoolmanager.pubapi.entities.SearchCriteria;
import cz.muni.fi.virtualtoolmanager.pubapi.entities.VirtualMachine;
import cz.muni.fi.virtualtoolmanager.pubapi.managers.ConnectionManager;
import cz.muni.fi.virtualtoolmanager.pubapi.managers.SearchManager;
import cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualizationToolManager;
import cz.muni.fi.virtualtoolmanager.pubapi.types.SearchCriterionType;
import cz.muni.fi.virtualtoolmanager.pubapi.types.SearchMode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Class that provide the implementation of methods declared in
 * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.SearchManager
 * SearchManager}.
 * 
 * @author Tomáš Šmíd
 */
public class SearchManagerImpl implements SearchManager{

    /** Represents the maximum deviation which is used for both search modes and
     * for hard disks values and memory and video memory, thanks to this attribute
     * there it is possible to match some virtual machine which does not have
     * the precise required value of a particular attribute, but it was relatively
     * close */
    private int maxDeviation;
    
    /**
     * The first constructor of this class. In this constructor is deviation
     * attribute set to zero.
     */
    public SearchManagerImpl(){
        this(0);
    }
    
    /**
     * The second constructor of the class. In this constructor is deviation
     * set to passed in value.
     * @param deviation represents the deviation which will be used for searching
     */
    public SearchManagerImpl(int deviation){
        checkAndSetMaxDeviation(deviation);
    }
    
    /**
     * <div>
     * Method that implements the method
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.SearchManager#search(SearchCriteria, SearchMode, List)
     * SearchManager::search(SearchCriteria, SearchMode, List)}.
     * </div>
     * <div>     
     * Search order (priority) of the search criteria take the effect only
     * in mode tolerant in which the order of searched properties can be very
     * important. It is not necessary to define search order for all properties,
     * only for the chosen. The remaining search criterion which were not included
     * in client search order will be ordered according to implicit setting and
     * appended to the explicitly set up search order. If the search order is not
     * specified, then there will be used the whole default search order.
     * If there occurs any error, then there can be thrown the following exceptions:
     * <ul>
     * <li><strong>IllegalArgumentException - </strong>thrown when the given
     * search criteria or search mode are <code>null</code> or search criteria
     * has no specified attribute or there was used illegal search mode
     * </ul>
     * </div>
     * @param searchCriteria defines what properties must have the searched
     * virtual machine
     * @param mode defines how precise the searching will be used
     * @param searchOrder defines the priority of each search criterion
     * @return list of matched virtual machines
     */
    @Override
    public List<VirtualMachine> search(SearchCriteria searchCriteria, SearchMode mode,
                                       List<SearchCriterionType> searchOrder) {
        ConnectionManager connectionManager = new ConnectionManagerImpl();        
        
        validateSearchCriteria(searchCriteria);
        validateSearchMode(mode);
        
        if(connectionManager.getConnectedPhysicalMachines().isEmpty()){
            return new ArrayList<>();
        }
        
        List<VirtualMachine> virtualMachines = getAllAvailableVMs();
        if(virtualMachines.isEmpty()){
            return new ArrayList<>();
        }
        
        if(isSearchOrderValid(searchOrder)){
            List<SearchCriterionType> tempSearchOrder = removeNullAndDuplicitValues(searchOrder);
            searchOrder = completeSearchOrder(tempSearchOrder);
        }else{
            searchOrder = getDefaultSearchOrder();
        }        
        List<SearchCriterionType> finalSearchOrder = filterSearchOrder(searchOrder,searchCriteria);
        
        return findAllSuitableVMs(virtualMachines,mode,finalSearchOrder,searchCriteria);
    }
    
    public void setMaxDeviation(int deviation){
        checkAndSetMaxDeviation(deviation);
    }
    
    public int getMaxDeviation(){
        return this.maxDeviation;
    }
    
    private void checkAndSetMaxDeviation(int deviation){
        if(deviation >= 0 && deviation <= 100){
            this.maxDeviation = deviation;
        }else{
            this.maxDeviation = 0;
        }
    }
    
    private void validateSearchCriteria(SearchCriteria searchCriteria){
        if(searchCriteria == null){
            throw new IllegalArgumentException("A null search criteria used for "
                    + "virtual machine search operation.");
        }
                
        if(!hasAnySpecifiedParameter(searchCriteria)){
            throw new IllegalArgumentException("No search criteria specified for "
                    + "virtual machine search operation.");
        }
    }
    
    private boolean hasAnySpecifiedParameter(SearchCriteria searchCriteria){
        UUID id = searchCriteria.getVmId();
        String name = searchCriteria.getVmName();
        String versionOfOS = searchCriteria.getIdentifierOfOS();
        String typeOfOS = searchCriteria.getTypeOfOS();
        Long cpuCount = searchCriteria.getCountOfCPU();
        Long monitorCount = searchCriteria.getCountOfMonitors();
        Long cpuExecCap = searchCriteria.getCpuExecutionCap();
        Long hddFreeSpace = searchCriteria.getHardDiskFreeSpaceSize();
        Long hddTotalSize = searchCriteria.getHardDiskTotalSize();
        Long ram = searchCriteria.getSizeOfRAM();
        Long vram = searchCriteria.getSizeOfVRAM();
        
        if(id != null && !id.toString().trim().isEmpty()){
            return true;
        }
        
        if(name != null && !name.trim().isEmpty()){
            return true;
        }
        
        if(versionOfOS != null && !versionOfOS.trim().isEmpty()){
            return true;
        }
        
        if(typeOfOS != null && !typeOfOS.trim().isEmpty()){
            return true;
        }
        
        if(cpuCount != null && cpuCount >= 0){
            return true;
        }
        
        if(monitorCount != null && monitorCount >= 0){
            return true;
        }
        
        if(cpuExecCap != null && cpuExecCap >= 0){
            return true;
        }
        
        if(hddFreeSpace != null && hddFreeSpace >= 0){
            return true;
        }
        
        if(hddTotalSize != null && hddTotalSize >= 0){
            return true;
        }
        
        if(ram != null && ram >= 0){
            return true;
        }
        
        if(vram != null && vram >= 0){
            return true;
        }
        
        return false;
    }
    
    private void validateSearchMode(SearchMode searchMode){
        if(searchMode == null){
            throw new IllegalArgumentException("A null search mode used for "
                    + "virtual machine search operation.");
        }
        switch(searchMode){
            case ABSOLUTE_EQUALITY :
            case TOLERANT          : break;
            default                : throw new IllegalArgumentException("Illegal "
                    + "enumeration literal used for virtual machine search "
                    + "operation. There was used enumeration literal of type "
                    + "SearchMode \"" + searchMode.toString() + "\", but the "
                    + "allowed enumeration literals are only \"ABSOLUTE_EQUALITY\" "
                    + "and \"TOLERANT\".");
        }
    }
    
    private List<VirtualMachine> getAllAvailableVMs(){        
        List<VirtualMachine> virtualMachines = new ArrayList<>();
        ConnectionManager connectionManager = new ConnectionManagerImpl();
        int index = 0;
        int numOfPMs = connectionManager.getConnectedPhysicalMachines().size();
        
        do{
            PhysicalMachine physicalMachine = connectionManager.getConnectedPhysicalMachines().get(index);
            VirtualizationToolManager virtualizationToolManager = new VirtualizationToolManagerImpl(physicalMachine);
            List<VirtualMachine> tempVMs = virtualizationToolManager.getVirtualMachines();
            if(!tempVMs.isEmpty()){
                virtualMachines.addAll(tempVMs);
            }
            if(numOfPMs == connectionManager.getConnectedPhysicalMachines().size()){
                ++index;                
            }else{
                numOfPMs = connectionManager.getConnectedPhysicalMachines().size();
            }
        }while(index < numOfPMs);
        
        
        return virtualMachines;
    }
    
    private boolean isSearchOrderValid(List<SearchCriterionType> searchOrder){
        if(searchOrder == null || searchOrder.isEmpty()){
            return false;
        }
        
        boolean retValue = false;
        for(SearchCriterionType scType : searchOrder){
            if(scType != null){
                switch(scType){
                    case ID            :
                    case NAME          :
                    case OS_TYPE       :
                    case OS_IDENTIFIER :
                    case CPU_COUNT     :
                    case CPU_EXEC_CAP  :
                    case HDD_FREE_SPACE:
                    case HDD_TOTAL_SIZE:
                    case RAM           :
                    case VRAM          :
                    case MONITOR_COUNT : retValue = true;
                                         break;
                    default            : throw new IllegalArgumentException("Illegal "
                            + "enumeration literal of type SearchCriterionType "
                            + "used for virtual machine search operation. The "
                            + "allowed values are: \"ID\", \"NAME\", \"OS_TYPE\""
                            + ", \"OS_IDENTIFIER\", \"CPU_COUNT\", \"CPU_EXEC_CAP\""
                            + ", \"HDD_FREE_SPACE\", \"HDD_TOTAL_SIZE\", \"RAM\""
                            + ", \"VRAM\", \"MONITOR_COUNT\".");
                }
            }
        }
        
        return retValue;
    }
    
    private List<SearchCriterionType> removeNullAndDuplicitValues(List<SearchCriterionType> inSearchOrder){
        List<SearchCriterionType> retSearchOrder = new ArrayList<>();
        
        for(SearchCriterionType scType : inSearchOrder){
            if(scType != null && !retSearchOrder.contains(scType)){
                retSearchOrder.add(scType);
            }
        }
        
        return retSearchOrder;
    }
    
    private List<SearchCriterionType> getDefaultSearchOrder(){
        return new ArrayList(Arrays.asList(SearchCriterionType.ID,
                                           SearchCriterionType.NAME,
                                           SearchCriterionType.OS_TYPE,
                                           SearchCriterionType.OS_IDENTIFIER,
                                           SearchCriterionType.CPU_COUNT,
                                           SearchCriterionType.CPU_EXEC_CAP,
                                           SearchCriterionType.RAM,
                                           SearchCriterionType.HDD_FREE_SPACE,
                                           SearchCriterionType.VRAM,
                                           SearchCriterionType.MONITOR_COUNT,
                                           SearchCriterionType.HDD_TOTAL_SIZE));
    }
    
    private List<SearchCriterionType> completeSearchOrder(List<SearchCriterionType> searchOrder){
        List<SearchCriterionType> defaultSearchOrder = getDefaultSearchOrder();
        
        defaultSearchOrder.removeAll(searchOrder);
        searchOrder.addAll(defaultSearchOrder);
        
        return searchOrder;
    }
    
    private List<SearchCriterionType> filterSearchOrder(List<SearchCriterionType> searchOrder,
                                                        SearchCriteria searchCriteria){
        if(searchCriteria.getVmId() == null || searchCriteria.getVmId().toString().trim().isEmpty()) {
            searchOrder.remove(SearchCriterionType.ID);
        }
        if(searchCriteria.getVmName() == null || searchCriteria.getVmName().trim().isEmpty()){
            searchOrder.remove(SearchCriterionType.NAME);
        }
        if(searchCriteria.getTypeOfOS() == null || searchCriteria.getTypeOfOS().trim().isEmpty()){
            searchOrder.remove(SearchCriterionType.OS_TYPE);
        }
        if(searchCriteria.getIdentifierOfOS() == null || searchCriteria.getIdentifierOfOS().trim().isEmpty()){
            searchOrder.remove(SearchCriterionType.OS_IDENTIFIER);
        }
        if(searchCriteria.getCountOfCPU() == null || searchCriteria.getCountOfCPU() < 0){
            searchOrder.remove(SearchCriterionType.CPU_COUNT);
        }
        if(searchCriteria.getCpuExecutionCap() == null || searchCriteria.getCpuExecutionCap() < 0){
            searchOrder.remove(SearchCriterionType.CPU_EXEC_CAP);
        }
        if(searchCriteria.getCountOfMonitors() == null || searchCriteria.getCountOfMonitors() < 0){
            searchOrder.remove(SearchCriterionType.MONITOR_COUNT);
        }
        if(searchCriteria.getHardDiskFreeSpaceSize() == null || searchCriteria.getHardDiskFreeSpaceSize() < 0){
            searchOrder.remove(SearchCriterionType.HDD_FREE_SPACE);
        }
        if(searchCriteria.getHardDiskTotalSize() == null || searchCriteria.getHardDiskTotalSize() < 0){
            searchOrder.remove(SearchCriterionType.HDD_TOTAL_SIZE);
        }
        if(searchCriteria.getSizeOfRAM() == null || searchCriteria.getSizeOfRAM() < 0){
            searchOrder.remove(SearchCriterionType.RAM);
        }
        if(searchCriteria.getSizeOfVRAM() == null || searchCriteria.getSizeOfVRAM() < 0){
            searchOrder.remove(SearchCriterionType.VRAM);
        }
        
        return searchOrder;
    }
    
    private List<VirtualMachine> findAllSuitableVMs(List<VirtualMachine> virtualMachines, SearchMode mode,
                                                    List<SearchCriterionType> searchOrder,
                                                    SearchCriteria searchCriteria){
        List<VirtualMachine> matchedVMs = virtualMachines;
        boolean someMatchedVM = false;
        
        for(SearchCriterionType scType : searchOrder){
            List<VirtualMachine> tempMatchedVMs = new ArrayList<>();
            for(VirtualMachine virtualMachine : virtualMachines){                
                switch(scType){
                    case ID:{
                        if(virtualMachine.getId().equals(searchCriteria.getVmId())){
                            tempMatchedVMs.add(virtualMachine);
                        }
                        break;
                    }

                    case NAME:{
                        if(virtualMachine.getName().equals(searchCriteria.getVmName())){
                            tempMatchedVMs.add(virtualMachine);
                        }
                        break;
                    }

                    case OS_TYPE:{
                        if(virtualMachine.getTypeOfOS().equals(searchCriteria.getTypeOfOS())){
                            tempMatchedVMs.add(virtualMachine);
                        }
                        break;
                    }

                    case OS_IDENTIFIER:{
                        if(virtualMachine.getIdentifierOfOS().equals(searchCriteria.getIdentifierOfOS())){
                            tempMatchedVMs.add(virtualMachine);
                        }
                        break;
                    }

                    case CPU_COUNT:{
                        if(virtualMachine.getCountOfCPU() == searchCriteria.getCountOfCPU()){
                            tempMatchedVMs.add(virtualMachine);
                        }
                        break;
                    }

                    case CPU_EXEC_CAP:{
                        if(virtualMachine.getCPUExecutionCap() == searchCriteria.getCpuExecutionCap()){
                            tempMatchedVMs.add(virtualMachine);
                        }
                        break;
                    }

                    case HDD_FREE_SPACE:{
                        BigDecimal tempNum = new BigDecimal(String.valueOf((searchCriteria.getHardDiskFreeSpaceSize()/(double)100)*maxDeviation));
                        long dev = tempNum.setScale(0, RoundingMode.HALF_UP).longValue();
                        if(virtualMachine.getHardDiskFreeSpaceSize() >= searchCriteria.getHardDiskFreeSpaceSize() &&
                                virtualMachine.getHardDiskFreeSpaceSize() <= (searchCriteria.getHardDiskFreeSpaceSize() + dev)){

                            tempMatchedVMs.add(virtualMachine);
                        }
                        break;
                    }

                    case HDD_TOTAL_SIZE:{
                        BigDecimal tempNum = new BigDecimal(String.valueOf((searchCriteria.getHardDiskTotalSize()/(double)100)*maxDeviation));
                        long dev = tempNum.setScale(0, RoundingMode.HALF_UP).longValue();
                        if(virtualMachine.getHardDiskTotalSize() >= searchCriteria.getHardDiskTotalSize() &&
                                virtualMachine.getHardDiskTotalSize() <= (searchCriteria.getHardDiskTotalSize() + dev)){

                            tempMatchedVMs.add(virtualMachine);
                        }
                        break;
                    }

                    case RAM:{
                        BigDecimal tempNum = new BigDecimal(String.valueOf((searchCriteria.getSizeOfRAM()/(double)100)*maxDeviation));
                        long dev = tempNum.setScale(0, RoundingMode.HALF_UP).longValue();
                        if(virtualMachine.getSizeOfRAM() >= searchCriteria.getSizeOfRAM() && 
                                virtualMachine.getSizeOfRAM() <= (searchCriteria.getSizeOfRAM() + dev)){

                            tempMatchedVMs.add(virtualMachine);
                        }
                        break;
                    }

                    case VRAM:{
                        BigDecimal tempNum = new BigDecimal(String.valueOf((searchCriteria.getSizeOfVRAM()/(double)100)*maxDeviation));
                        long dev = tempNum.setScale(0, RoundingMode.HALF_UP).longValue();
                        if(virtualMachine.getSizeOfVRAM() >= searchCriteria.getSizeOfVRAM() &&
                                virtualMachine.getSizeOfVRAM() <= (searchCriteria.getSizeOfVRAM() + dev)){

                            tempMatchedVMs.add(virtualMachine);
                        }
                        break;
                    }

                    case MONITOR_COUNT:{
                        if(virtualMachine.getCountOfMonitors() == searchCriteria.getCountOfMonitors()){
                            tempMatchedVMs.add(virtualMachine);
                        }
                        break;
                    }

                    default: return new ArrayList<>();
                }
            }
            if(tempMatchedVMs.isEmpty()){
                if(mode == SearchMode.ABSOLUTE_EQUALITY){
                    return new ArrayList<>();
                }
            }else{                
                List<VirtualMachine> backupMatchedVMs = matchedVMs;
                matchedVMs.retainAll(tempMatchedVMs);
                if(matchedVMs.isEmpty()){
                    if(mode == SearchMode.ABSOLUTE_EQUALITY){
                        return matchedVMs;//empty array of VMs
                    }else{
                        matchedVMs = backupMatchedVMs;
                    }
                }
                if(!someMatchedVM){
                    someMatchedVM = true;
                }
            }
        }
        
        return (someMatchedVM ? matchedVMs : new ArrayList<>());
    }
}
