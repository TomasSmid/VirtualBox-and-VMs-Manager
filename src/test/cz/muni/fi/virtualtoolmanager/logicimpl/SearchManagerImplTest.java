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

import cz.muni.fi.virtualtoolmanager.io.OutputHandler;
import cz.muni.fi.virtualtoolmanager.pubapi.entities.PhysicalMachine;
import cz.muni.fi.virtualtoolmanager.pubapi.entities.SearchCriteria;
import cz.muni.fi.virtualtoolmanager.pubapi.entities.VirtualMachine;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException;
import cz.muni.fi.virtualtoolmanager.pubapi.types.SearchCriterionType;
import cz.muni.fi.virtualtoolmanager.pubapi.types.SearchMode;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 *
 * @author Tomáš Šmíd
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SearchManagerImpl.class, VirtualizationToolManagerImpl.class, ConnectionManagerImpl.class})
public class SearchManagerImplTest {
    
    private SearchManagerImpl sut;
    private VirtualizationToolManagerImpl vtmMock;
    private ConnectionManagerImpl conManMock;
    
    @Before
    public void setUp() throws Exception {
        vtmMock = mock(VirtualizationToolManagerImpl.class);
        conManMock = mock(ConnectionManagerImpl.class);
        whenNew(VirtualizationToolManagerImpl.class).withAnyArguments().thenReturn(vtmMock);
        whenNew(ConnectionManagerImpl.class).withNoArguments().thenReturn(conManMock);
        sut = new SearchManagerImpl();
    }
    
    @Test
    public void settingUpMaxDeviationViaDefaultConstructor(){
        SearchManagerImpl searchManager = new SearchManagerImpl();
        
        assertEquals("Values should be same",0,searchManager.getMaxDeviation());
    }
    
    @Test
    public void settingUpMaxDeviationViaConstructorWithOneParameterCorrectValue(){
        SearchManagerImpl searchManager = new SearchManagerImpl(5);
        
        assertEquals("Values should be same",5,searchManager.getMaxDeviation());
    }
    
    @Test
    public void settingUpMaxDeviationViaConstructorWithOneParameterNegativeValue(){
        SearchManagerImpl searchManager = new SearchManagerImpl(-1);
        
        assertEquals("Values should be same",0,searchManager.getMaxDeviation());
    }
    
    @Test
    public void settingUpMaxDeviationViaConstructorWithOneParameterTooBigValue(){
        SearchManagerImpl searchManager = new SearchManagerImpl(101);
        
        assertEquals("Values should be same",0,searchManager.getMaxDeviation());
    }
    
    @Test
    public void settingUpMaxDeviationViaSetterCorrectValue(){
        SearchManagerImpl searchManager = new SearchManagerImpl();
        
        searchManager.setMaxDeviation(100);
        assertEquals("Values should be same",100,searchManager.getMaxDeviation());
    }
    
    @Test
    public void settingUpMaxDeviationViaSetterNegativeValue(){
        SearchManagerImpl searchManager = new SearchManagerImpl();
        
        searchManager.setMaxDeviation(-1);
        assertEquals("Values should be same",0,searchManager.getMaxDeviation());
    }
    
    @Test
    public void settingUpMaxDeviationViaSetterTooBigValue(){
        SearchManagerImpl searchManager = new SearchManagerImpl();
        
        searchManager.setMaxDeviation(101);
        assertEquals("Values should be same",0,searchManager.getMaxDeviation());
    }
    
    /**
     * This test tests that if the method SearchManagerImpl::search() is called
     * and all conditions are met, then if there exist a precisely required virtual
     * machines, then those virtual machines are returned as a result of the method call.
     */
    @Test
    public void searchPreciselyWithSomeExistentVMsAndSomeMatch(){
        //represent connected physical machines from which are retrieved all virtual machines
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("102.10.91.12").build();
        PhysicalMachine pm3 = new PMBuilder().addressIP("102.10.91.11").webserverPort("1005")
                                             .username("John").userPassword("Doe123").build();
        //represent all available virtual machines from connected physical machines
        VirtualMachine vm1 = new VMBuilder().build();
        VirtualMachine vm2 = new VMBuilder().id(UUID.fromString("000d084a-0189-4a55-a9b7-531c455570a1"))
                                            .hostMachine(pm2).sizeOfRAM(8192L)
                                            .hardDiskFreeSpaceSize(8888880001L).hardDiskTotalSize(1200041548L)
                                            .build();
        VirtualMachine vm3 = new VMBuilder().id(UUID.fromString("000d0815-aa89-bbcd-a9b7-531c455570a1"))
                                            .name("VirtualMachine_02").countOfCPU(2L).countOfMonitors(2L)
                                            .cpuExecutionCap(50L).hardDiskFreeSpaceSize(14548744897L)
                                            .hardDiskTotalSize(2300000098745L).sizeOfRAM(2096L)
                                            .sizeOfVRAM(16L).typeOfOS("MS-Windows").identifierOfOS("Win10_64")
                                            .hostMachine(pm2).build();
        VirtualMachine vm4 = new VMBuilder().id(UUID.fromString("1a2d0815-aa89-bbcd-a9b7-531c455ab015"))
                                            .name("VirtualMachine_03").typeOfOS("iOS").identifierOfOS("iOS9_32")
                                            .hostMachine(pm2).build();
        //represents a list of connected physical machines
        List<PhysicalMachine> connectedPMs = Arrays.asList(pm1,pm2,pm3);
        //represents a list of all available virtual machines from connected physical machine pm1
        List<VirtualMachine> vmsFromPM1 = Arrays.asList(vm1);
        //represents a list of all available virtual machines from connected physical machine pm2
        List<VirtualMachine> vmsFromPM2 = Arrays.asList(vm2,vm3,vm4);
        //represents a list (empty) of all available virtual machines (none) from connected physical machine pm3
        List<VirtualMachine> vmsFromPM3 = new ArrayList<>();
        //represents search criteria according to which are virtual machines searched
        SearchCriteria searchCriteria = new SearchCriteria.Builder().typeOfOS("Linux").sizeOfRAM(4096L).build();
        //represents an order by which the search operation will be controled
        List<SearchCriterionType> searchOrder = Arrays.asList(SearchCriterionType.RAM,
                                                              SearchCriterionType.NAME,
                                                              SearchCriterionType.OS_TYPE);
        //represents a list of expected virtual machines (a result of search method call)
        List<VirtualMachine> expVMs = Arrays.asList(vm1);
        
        //there should be returned a list of connected PMs when the method
        //ConnectionManagerImpl::getConnectedPhysicalMachines() is called
        when(conManMock.getConnectedPhysicalMachines()).thenReturn(connectedPMs);
        //after the first call of the method VirtualizationToolManagerImpl::getVirtualMachines() there should
        //returned a list of all available VMs from connected PM pm1, after the second call a list of all
        //available VMs from connected PM pm2 and after the third call an empty list of VMs from connected PM pm3
        when(vtmMock.getVirtualMachines()).thenReturn(vmsFromPM1).thenReturn(vmsFromPM2).thenReturn(vmsFromPM3);
        
        List<VirtualMachine> actVMs = sut.search(searchCriteria, SearchMode.ABSOLUTE_EQUALITY, searchOrder);
        
        Collections.sort(expVMs, vmComparator);
        Collections.sort(actVMs, vmComparator);
        
        //checks the expected and the actual found VMs are equal
        assertDeepVMsEquals(expVMs, actVMs);
    }
    
    /**
     * This test tests that if the method SearchManagerImpl::search() is called
     * and all conditions are met, then, because there does not exist any virtual
     * machine with name "MyVirtualPC", there is search operation stopped after first
     * round, because there is required a absolute accuracy, so the second required
     * attribute is not even tested (VMs are not searched according to the second
     * search criterion), and returned an empty list of found VMs.
     */
    @Test
    public void searchPreciselyWithSomeExistentVMsAndNoMatchAfterFirstSearchCriteriaComparison(){
        //represent connected physical machines from which are retrieved all virtual machines
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("102.10.91.12").build();
        PhysicalMachine pm3 = new PMBuilder().addressIP("102.10.91.11").webserverPort("1005")
                                             .username("John").userPassword("Doe123").build();
        //represent all available virtual machines from connected physical machines
        VirtualMachine vm1 = new VMBuilder().build();
        VirtualMachine vm2 = new VMBuilder().id(UUID.fromString("000d084a-0189-4a55-a9b7-531c455570a1"))
                                            .hostMachine(pm2).sizeOfRAM(8192L)
                                            .hardDiskFreeSpaceSize(8888880001L).hardDiskTotalSize(1200041548L)
                                            .build();
        VirtualMachine vm3 = new VMBuilder().id(UUID.fromString("000d0815-aa89-bbcd-a9b7-531c455570a1"))
                                            .name("VirtualMachine_02").countOfCPU(2L).countOfMonitors(2L)
                                            .cpuExecutionCap(50L).hardDiskFreeSpaceSize(14548744897L)
                                            .hardDiskTotalSize(2300000098745L).sizeOfRAM(2096L)
                                            .sizeOfVRAM(16L).typeOfOS("MS-Windows").identifierOfOS("Win10_64")
                                            .hostMachine(pm2).build();
        VirtualMachine vm4 = new VMBuilder().id(UUID.fromString("1a2d0815-aa89-bbcd-a9b7-531c455ab015"))
                                            .name("VirtualMachine_03").typeOfOS("iOS").identifierOfOS("iOS9_32")
                                            .hostMachine(pm2).build();
        //represents a list of connected physical machines
        List<PhysicalMachine> connectedPMs = Arrays.asList(pm1,pm2,pm3);
        //represents a list of all available virtual machines from connected physical machine pm1
        List<VirtualMachine> vmsFromPM1 = Arrays.asList(vm1);
        //represents a list of all available virtual machines from connected physical machine pm2
        List<VirtualMachine> vmsFromPM2 = Arrays.asList(vm2,vm3,vm4);
        //represents a list (empty) of all available virtual machines (none) from connected physical machine pm3
        List<VirtualMachine> vmsFromPM3 = new ArrayList<>();
        //represents search criteria according to which are virtual machines searched
        SearchCriteria searchCriteria = new SearchCriteria.Builder().name("MyVirtualPC").typeOfOS("Windows").build();
        //represents an order by which the search operation will be controled
        List<SearchCriterionType> searchOrder = Arrays.asList(SearchCriterionType.RAM,
                                                              SearchCriterionType.NAME,
                                                              SearchCriterionType.OS_TYPE);
                
        //there should be returned a list of connected PMs when the method
        //ConnectionManagerImpl::getConnectedPhysicalMachines() is called
        when(conManMock.getConnectedPhysicalMachines()).thenReturn(connectedPMs);
        //after the first call of the method VirtualizationToolManagerImpl::getVirtualMachines() there should
        //returned a list of all available VMs from connected PM pm1, after the second call a list of all
        //available VMs from connected PM pm2 and after the third call an empty list of VMs from connected PM pm3
        when(vtmMock.getVirtualMachines()).thenReturn(vmsFromPM1).thenReturn(vmsFromPM2).thenReturn(vmsFromPM3);
        
        List<VirtualMachine> actVMs = sut.search(searchCriteria, SearchMode.ABSOLUTE_EQUALITY, searchOrder);
        
        assertTrue("The returned list of VMs should be empty", actVMs.isEmpty());
    }
    
    /**
     * This test tests that if the method SearchManagerImpl::search() is called
     * and all conditions are met, then there is returned an empty list of found
     * VMs even if there exists any VM, which has all required attributes except
     * one which is different than required, because there must be absolute equality
     * and the different attribute value was found out in the last search round.
     */
    @Test
    public void searchPreciselyWithSomeExistentVMsAndNoMatchAfterMoreSearchCriteriaComparisons(){
        //represent connected physical machines from which are retrieved all virtual machines
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("102.10.91.12").build();
        PhysicalMachine pm3 = new PMBuilder().addressIP("102.10.91.11").webserverPort("1005")
                                             .username("John").userPassword("Doe123").build();
        //represent all available virtual machines from connected physical machines
        VirtualMachine vm1 = new VMBuilder().build();
        VirtualMachine vm2 = new VMBuilder().id(UUID.fromString("000d084a-0189-4a55-a9b7-531c455570a1"))
                                            .hostMachine(pm2).sizeOfRAM(8192L)
                                            .hardDiskFreeSpaceSize(8888880001L).hardDiskTotalSize(1200041548L)
                                            .build();
        VirtualMachine vm3 = new VMBuilder().id(UUID.fromString("000d0815-aa89-bbcd-a9b7-531c455570a1"))
                                            .name("VirtualMachine_02").countOfCPU(2L).countOfMonitors(2L)
                                            .cpuExecutionCap(50L).hardDiskFreeSpaceSize(14548744897L)
                                            .hardDiskTotalSize(2300000098745L).sizeOfRAM(2096L)
                                            .sizeOfVRAM(16L).typeOfOS("MS-Windows").identifierOfOS("Win10_64")
                                            .hostMachine(pm2).build();
        VirtualMachine vm4 = new VMBuilder().id(UUID.fromString("1a2d0815-aa89-bbcd-a9b7-531c455ab015"))
                                            .name("VirtualMachine_03").typeOfOS("iOS").identifierOfOS("iOS9_32")
                                            .hostMachine(pm2).build();
        //represents a list of connected physical machines
        List<PhysicalMachine> connectedPMs = Arrays.asList(pm1,pm2,pm3);
        //represents a list of all available virtual machines from connected physical machine pm1
        List<VirtualMachine> vmsFromPM1 = Arrays.asList(vm1);
        //represents a list of all available virtual machines from connected physical machine pm2
        List<VirtualMachine> vmsFromPM2 = Arrays.asList(vm2,vm3,vm4);
        //represents a list (empty) of all available virtual machines (none) from connected physical machine pm3
        List<VirtualMachine> vmsFromPM3 = new ArrayList<>();
        //represents search criteria according to which are virtual machines searched
        SearchCriteria searchCriteria = new SearchCriteria.Builder().name("MyVirtualPC").typeOfOS("MS-Windows")
                                                                    .countOfCPU(2L).sizeOfVRAM(16L).build();
        //represents an order by which the search operation will be controled
        List<SearchCriterionType> searchOrder = Arrays.asList(SearchCriterionType.RAM,
                                                              SearchCriterionType.OS_TYPE,
                                                              SearchCriterionType.VRAM,
                                                              SearchCriterionType.CPU_COUNT,
                                                              SearchCriterionType.MONITOR_COUNT,
                                                              SearchCriterionType.NAME);
                
        //there should be returned a list of connected PMs when the method
        //ConnectionManagerImpl::getConnectedPhysicalMachines() is called
        when(conManMock.getConnectedPhysicalMachines()).thenReturn(connectedPMs);
        //after the first call of the method VirtualizationToolManagerImpl::getVirtualMachines() there should
        //returned a list of all available VMs from connected PM pm1, after the second call a list of all
        //available VMs from connected PM pm2 and after the third call an empty list of VMs from connected PM pm3
        when(vtmMock.getVirtualMachines()).thenReturn(vmsFromPM1).thenReturn(vmsFromPM2).thenReturn(vmsFromPM3);
        
        List<VirtualMachine> actVMs = sut.search(searchCriteria, SearchMode.ABSOLUTE_EQUALITY, searchOrder);
        
        assertTrue("The returned list of VMs should be empty", actVMs.isEmpty());
    }
    
    @Test
    public void searchPreciselyWithNoExistentVMs(){
        //represent connected physical machines from which are retrieved all virtual machines
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("102.10.91.12").build();
        PhysicalMachine pm3 = new PMBuilder().addressIP("102.10.91.11").webserverPort("1005")
                                             .username("John").userPassword("Doe123").build();
        //represents a list of connected physical machines
        List<PhysicalMachine> connectedPMs = Arrays.asList(pm1,pm2,pm3);
        //represents a list (empty) of all available virtual machines (none) from connected physical machine pm1
        List<VirtualMachine> vmsFromPM1 = new ArrayList<>();
        //represents a list (empty) of all available virtual machines (none) from connected physical machine pm2
        List<VirtualMachine> vmsFromPM2 = new ArrayList<>();
        //represents a list (empty) of all available virtual machines (none) from connected physical machine pm3
        List<VirtualMachine> vmsFromPM3 = new ArrayList<>();
        //represents search criteria according to which are virtual machines searched
        SearchCriteria searchCriteria = new SearchCriteria.Builder().name("VirtualMachine_02").typeOfOS("MS-Windows")
                                                                    .countOfCPU(2L).sizeOfVRAM(16L).build();
        //represents an order by which the search operation will be controled
        List<SearchCriterionType> searchOrder = Arrays.asList(SearchCriterionType.RAM,
                                                              SearchCriterionType.OS_TYPE,
                                                              SearchCriterionType.VRAM,
                                                              SearchCriterionType.CPU_COUNT,
                                                              SearchCriterionType.MONITOR_COUNT,
                                                              SearchCriterionType.NAME);
                
        //there should be returned a list of connected PMs when the method
        //ConnectionManagerImpl::getConnectedPhysicalMachines() is called
        when(conManMock.getConnectedPhysicalMachines()).thenReturn(connectedPMs);
        //after all method calls there should be returned an empty list of available VMs from a particular PM
        when(vtmMock.getVirtualMachines()).thenReturn(vmsFromPM1).thenReturn(vmsFromPM2).thenReturn(vmsFromPM3);
        
        List<VirtualMachine> actVMs = sut.search(searchCriteria, SearchMode.ABSOLUTE_EQUALITY, searchOrder);
        
        assertTrue("The returned list of VMs should be empty", actVMs.isEmpty());
    }
    
    /**
     * This test tests that if the method SearchManagerImpl::search() is called
     * with search criteria to which precisely match any virtual machine, then
     * this virtual machine is returned as a result of the search operation.
     */
    @Test
    public void searchTolerantlyWithSomeExistentVMsAndSomePreciseMatch(){
        //represent connected physical machines from which are retrieved all virtual machines
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("102.10.91.12").build();
        PhysicalMachine pm3 = new PMBuilder().addressIP("102.10.91.11").webserverPort("1005")
                                             .username("John").userPassword("Doe123").build();
        //represent all available virtual machines from connected physical machines
        VirtualMachine vm1 = new VMBuilder().build();
        VirtualMachine vm2 = new VMBuilder().id(UUID.fromString("000d084a-0189-4a55-a9b7-531c455570a1"))
                                            .hostMachine(pm2).sizeOfRAM(8192L)
                                            .hardDiskFreeSpaceSize(8888880001L).hardDiskTotalSize(1200041548L)
                                            .build();
        VirtualMachine vm3 = new VMBuilder().id(UUID.fromString("000d0815-aa89-bbcd-a9b7-531c455570a1"))
                                            .name("VirtualMachine_02").countOfCPU(2L).countOfMonitors(2L)
                                            .cpuExecutionCap(50L).hardDiskFreeSpaceSize(14548744897L)
                                            .hardDiskTotalSize(2300000098745L).sizeOfRAM(2096L)
                                            .sizeOfVRAM(16L).typeOfOS("MS-Windows").identifierOfOS("Win10_64")
                                            .hostMachine(pm2).build();
        VirtualMachine vm4 = new VMBuilder().id(UUID.fromString("1a2d0815-aa89-bbcd-a9b7-531c455ab015"))
                                            .name("VirtualMachine_03").typeOfOS("iOS").identifierOfOS("iOS9_32")
                                            .hostMachine(pm3).build();
        //represents a list of connected physical machines
        List<PhysicalMachine> connectedPMs = Arrays.asList(pm1,pm2,pm3);
        //represents a list of all available virtual machines from connected physical machine pm1
        List<VirtualMachine> vmsFromPM1 = Arrays.asList(vm1);
        //represents a list of all available virtual machines from connected physical machine pm2
        List<VirtualMachine> vmsFromPM2 = Arrays.asList(vm2,vm3);
        //represents a list (empty) of all available virtual machines (none) from connected physical machine pm3
        List<VirtualMachine> vmsFromPM3 = Arrays.asList(vm4);
        //represents search criteria according to which are virtual machines searched
        SearchCriteria searchCriteria = new SearchCriteria.Builder().typeOfOS("iOS").sizeOfRAM(4096L).build();
        //represents an order by which the search operation will be controled
        List<SearchCriterionType> searchOrder = Arrays.asList(SearchCriterionType.RAM,
                                                              SearchCriterionType.NAME,
                                                              SearchCriterionType.OS_TYPE);
        //represents a list of expected virtual machines (a result of search method call)
        List<VirtualMachine> expVMs = Arrays.asList(vm4);
        
        //there should be returned a list of connected PMs when the method
        //ConnectionManagerImpl::getConnectedPhysicalMachines() is called
        when(conManMock.getConnectedPhysicalMachines()).thenReturn(connectedPMs);
        //after the first call of the method VirtualizationToolManagerImpl::getVirtualMachines() there should
        //returned a list of all available VMs from connected PM pm1, after the second call a list of all
        //available VMs from connected PM pm2 and after the third call an empty list of VMs from connected PM pm3
        when(vtmMock.getVirtualMachines()).thenReturn(vmsFromPM1).thenReturn(vmsFromPM2).thenReturn(vmsFromPM3);
        
        List<VirtualMachine> actVMs = sut.search(searchCriteria, SearchMode.TOLERANT, searchOrder);
        
        Collections.sort(expVMs, vmComparator);
        Collections.sort(actVMs, vmComparator);
        
        //checks the expected and the actual found VMs are equal
        assertDeepVMsEquals(expVMs, actVMs);
    }
    
    /**
     * This test tests that even if the several first attempts are not successful
     * and thus there is not found any virtual machine, the search operation
     * continues and search according to all specified search criteria in a required
     * order while this search order can significantly affect the result of search
     * operation - in this test are in 3. attempt found two VMs with required HDD
     * free space size, but in the last 4. attempt are found another VMs with
     * different HDD free space size than the required one and thus these found
     * VMs from 4. attempt are not included to the result of search operation.
     */
    @Test
    public void searchTolerantlyWithSomeExistentVMsAndSomeImpreciseMatchAndCustomizedSearchOrder1(){
        //represent connected physical machines from which are retrieved all virtual machines
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("102.10.91.12").build();
        PhysicalMachine pm3 = new PMBuilder().addressIP("102.10.91.11").webserverPort("1005")
                                             .username("John").userPassword("Doe123").build();
        //represent all available virtual machines from connected physical machines
        VirtualMachine vm1 = new VMBuilder().build();
        VirtualMachine vm2 = new VMBuilder().id(UUID.fromString("000d084a-0189-4a55-a9b7-531c455570a1"))
                                            .hostMachine(pm2).sizeOfRAM(8192L)
                                            .hardDiskFreeSpaceSize(8888880001L).hardDiskTotalSize(1200041548L)
                                            .build();
        VirtualMachine vm3 = new VMBuilder().id(UUID.fromString("000d0815-aa89-bbcd-a9b7-531c455570a1"))
                                            .name("VirtualMachine_02").countOfCPU(2L).countOfMonitors(2L)
                                            .cpuExecutionCap(50L).hardDiskFreeSpaceSize(17548744897L)
                                            .hardDiskTotalSize(2300000098745L).sizeOfRAM(2096L)
                                            .sizeOfVRAM(16L).typeOfOS("MS-Windows").identifierOfOS("Win10_64")
                                            .hostMachine(pm2).build();
        VirtualMachine vm4 = new VMBuilder().id(UUID.fromString("1a2d0815-aa89-bbcd-a9b7-531c455ab015"))
                                            .name("VirtualMachine_03").typeOfOS("iOS").identifierOfOS("iOS9_32")
                                            .hostMachine(pm3).hardDiskFreeSpaceSize(17200458999L).build();                                            
        //represents a list of connected physical machines
        List<PhysicalMachine> connectedPMs = Arrays.asList(pm1,pm2,pm3);
        //represents a list of all available virtual machines from connected physical machine pm1
        List<VirtualMachine> vmsFromPM1 = Arrays.asList(vm1);
        //represents a list of all available virtual machines from connected physical machine pm2
        List<VirtualMachine> vmsFromPM2 = Arrays.asList(vm2,vm3);
        //represents a list (empty) of all available virtual machines (none) from connected physical machine pm3
        List<VirtualMachine> vmsFromPM3 = Arrays.asList(vm4);
        //set up a possible 5% deviation up to required value - this will be used when looking for a VM with a required HDD free space
        sut.setMaxDeviation(5);
        //represents search criteria according to which are virtual machines searched
        SearchCriteria searchCriteria = new SearchCriteria.Builder().countOfCPU(3L)
                                                                    .hardDiskTotalSize(1200L)
                                                                    .hardDiskFreeSpaceSize(17184012099L)
                                                                    .name("VirtualMachine_01")
                                                                    .build();
        //represents an order by which the search operation will be controled
        List<SearchCriterionType> searchOrder = Arrays.asList(SearchCriterionType.HDD_TOTAL_SIZE,
                                                              SearchCriterionType.CPU_COUNT,
                                                              SearchCriterionType.HDD_FREE_SPACE,
                                                              SearchCriterionType.NAME);                                                              
        //represents a list of expected virtual machines (a result of search method call)
        List<VirtualMachine> expVMs = Arrays.asList(vm3,vm4);
        
        //there should be returned a list of connected PMs when the method
        //ConnectionManagerImpl::getConnectedPhysicalMachines() is called
        when(conManMock.getConnectedPhysicalMachines()).thenReturn(connectedPMs);
        //after the first call of the method VirtualizationToolManagerImpl::getVirtualMachines() there should
        //returned a list of all available VMs from connected PM pm1, after the second call a list of all
        //available VMs from connected PM pm2 and after the third call an empty list of VMs from connected PM pm3
        when(vtmMock.getVirtualMachines()).thenReturn(vmsFromPM1).thenReturn(vmsFromPM2).thenReturn(vmsFromPM3);
        
        List<VirtualMachine> actVMs = sut.search(searchCriteria, SearchMode.TOLERANT, searchOrder);
        
        //checks the expected and the actual found VMs are equal
        assertDeepVMsEquals(expVMs, actVMs);
    }
    
    /**
     * This test tests that even if there occur several attempts which are not
     * successful (and not first nor last in order) and thus there is not found
     * any virtual machine, the search operation continues and search according
     * to all specified search criteria in a required order while this search
     * order can significantly affect the result of search operation - in this
     * test are in 1. attempt found two VMs with required HDD free space size,
     * in 2. and 4. attempt there is not found any VM, in 3. attempt there is
     * found just one VM with a required RAM size, which was already found in 1.
     * attempt and in the last 5. attempt are found another VMs, but they were
     * not found in previous attempts thus these found VMs from 4. attempt are
     * not included to the result of search operation.
     */
    @Test
    public void searchTolerantlyWithSomeExistentVMsAndSomeImpreciseMatchCustomizedSearchOrder2(){
        //represent connected physical machines from which are retrieved all virtual machines
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("102.10.91.12").build();
        PhysicalMachine pm3 = new PMBuilder().addressIP("102.10.91.11").webserverPort("1005")
                                             .username("John").userPassword("Doe123").build();
        //represent all available virtual machines from connected physical machines
        VirtualMachine vm1 = new VMBuilder().build();
        VirtualMachine vm2 = new VMBuilder().id(UUID.fromString("000d084a-0189-4a55-a9b7-531c455570a1"))
                                            .hostMachine(pm2).sizeOfRAM(8192L)
                                            .hardDiskFreeSpaceSize(8888880001L).hardDiskTotalSize(1200041548L)
                                            .build();
        VirtualMachine vm3 = new VMBuilder().id(UUID.fromString("000d0815-aa89-bbcd-a9b7-531c455570a1"))
                                            .name("VirtualMachine_02").countOfCPU(2L).countOfMonitors(2L)
                                            .cpuExecutionCap(50L).hardDiskFreeSpaceSize(17548744897L)
                                            .hardDiskTotalSize(2300000098745L).sizeOfRAM(2096L)
                                            .sizeOfVRAM(16L).typeOfOS("MS-Windows").identifierOfOS("Win10_64")
                                            .hostMachine(pm2).build();
        VirtualMachine vm4 = new VMBuilder().id(UUID.fromString("1a2d0815-aa89-bbcd-a9b7-531c455ab015"))
                                            .name("VirtualMachine_03").typeOfOS("iOS").identifierOfOS("iOS9_32")
                                            .hostMachine(pm3).hardDiskFreeSpaceSize(17200458999L).build();                                            
        //represents a list of connected physical machines
        List<PhysicalMachine> connectedPMs = Arrays.asList(pm1,pm2,pm3);
        //represents a list of all available virtual machines from connected physical machine pm1
        List<VirtualMachine> vmsFromPM1 = Arrays.asList(vm1);
        //represents a list of all available virtual machines from connected physical machine pm2
        List<VirtualMachine> vmsFromPM2 = Arrays.asList(vm2,vm3);
        //represents a list (empty) of all available virtual machines (none) from connected physical machine pm3
        List<VirtualMachine> vmsFromPM3 = Arrays.asList(vm4);
        //set up a possible 5% deviation up to required value - this will be used when looking for a VM with a required HDD free space
        sut.setMaxDeviation(5);
        //represents search criteria according to which are virtual machines searched
        SearchCriteria searchCriteria = new SearchCriteria.Builder().countOfCPU(3L)
                                                                    .hardDiskTotalSize(1200L)
                                                                    .hardDiskFreeSpaceSize(17184012099L)
                                                                    .name("VirtualMachine_01")
                                                                    .sizeOfRAM(4000L)
                                                                    .build();
        //represents an order by which the search operation will be controled
        List<SearchCriterionType> searchOrder = Arrays.asList(SearchCriterionType.HDD_FREE_SPACE,
                                                              SearchCriterionType.CPU_COUNT,
                                                              SearchCriterionType.RAM,
                                                              SearchCriterionType.HDD_TOTAL_SIZE,
                                                              SearchCriterionType.NAME);                                                              
        //represents a list of expected virtual machines (a result of search method call)
        List<VirtualMachine> expVMs = Arrays.asList(vm4);
        
        //there should be returned a list of connected PMs when the method
        //ConnectionManagerImpl::getConnectedPhysicalMachines() is called
        when(conManMock.getConnectedPhysicalMachines()).thenReturn(connectedPMs);
        //after the first call of the method VirtualizationToolManagerImpl::getVirtualMachines() there should
        //returned a list of all available VMs from connected PM pm1, after the second call a list of all
        //available VMs from connected PM pm2 and after the third call an empty list of VMs from connected PM pm3
        when(vtmMock.getVirtualMachines()).thenReturn(vmsFromPM1).thenReturn(vmsFromPM2).thenReturn(vmsFromPM3);
        
        List<VirtualMachine> actVMs = sut.search(searchCriteria, SearchMode.TOLERANT, searchOrder);
        
        //checks the expected and the actual found VMs are equal
        assertDeepVMsEquals(expVMs, actVMs);
    }
    
    /**
     * This test tests that even if there occur several attempts which are not
     * successful (and are last in order) and thus there is not found any virtual
     * machine, the search operation continues and search according to all specified
     * search criteria in a required order while this search order can significantly
     * affect the result of search operation - in this test are in 1. attempt found
     * two VMs, in 2. attempt just one VM which was also found in 1. attempt and
     * in 3. attempt there are found another VMs, but they were not found in the
     * previous attempts thus these machines are not included in the result, in
     * 4. and 5. attempt is not found any VM so the final result is that after
     * last successful attempt thus after 3. attempt.
     */
    @Test
    public void searchTolerantlyWithSomeExistentVMsAndSomeImpreciseMatchCustomizedSearchOrder3(){
        //represent connected physical machines from which are retrieved all virtual machines
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("102.10.91.12").build();
        PhysicalMachine pm3 = new PMBuilder().addressIP("102.10.91.11").webserverPort("1005")
                                             .username("John").userPassword("Doe123").build();
        //represent all available virtual machines from connected physical machines
        VirtualMachine vm1 = new VMBuilder().build();
        VirtualMachine vm2 = new VMBuilder().id(UUID.fromString("000d084a-0189-4a55-a9b7-531c455570a1"))
                                            .hostMachine(pm2).sizeOfRAM(8192L)
                                            .hardDiskFreeSpaceSize(8888880001L).hardDiskTotalSize(1200041548L)
                                            .build();
        VirtualMachine vm3 = new VMBuilder().id(UUID.fromString("000d0815-aa89-bbcd-a9b7-531c455570a1"))
                                            .name("VirtualMachine_02").countOfCPU(2L).countOfMonitors(2L)
                                            .cpuExecutionCap(50L).hardDiskFreeSpaceSize(17548744897L)
                                            .hardDiskTotalSize(2300000098745L).sizeOfRAM(2096L)
                                            .sizeOfVRAM(16L).typeOfOS("MS-Windows").identifierOfOS("Win10_64")
                                            .hostMachine(pm2).build();
        VirtualMachine vm4 = new VMBuilder().id(UUID.fromString("1a2d0815-aa89-bbcd-a9b7-531c455ab015"))
                                            .name("VirtualMachine_03").typeOfOS("iOS").identifierOfOS("iOS9_32")
                                            .hostMachine(pm3).hardDiskFreeSpaceSize(17200458999L).build();                                            
        //represents a list of connected physical machines
        List<PhysicalMachine> connectedPMs = Arrays.asList(pm1,pm2,pm3);
        //represents a list of all available virtual machines from connected physical machine pm1
        List<VirtualMachine> vmsFromPM1 = Arrays.asList(vm1);
        //represents a list of all available virtual machines from connected physical machine pm2
        List<VirtualMachine> vmsFromPM2 = Arrays.asList(vm2,vm3);
        //represents a list (empty) of all available virtual machines (none) from connected physical machine pm3
        List<VirtualMachine> vmsFromPM3 = Arrays.asList(vm4);
        //set up a possible 5% deviation up to required value - this will be used when looking for a VM with a required HDD free space
        sut.setMaxDeviation(5);
        //represents search criteria according to which are virtual machines searched
        SearchCriteria searchCriteria = new SearchCriteria.Builder().countOfCPU(3L)
                                                                    .hardDiskTotalSize(1200L)
                                                                    .hardDiskFreeSpaceSize(17184012099L)
                                                                    .name("VirtualMachine_01")
                                                                    .sizeOfRAM(4000L)
                                                                    .build();
        //represents an order by which the search operation will be controled
        List<SearchCriterionType> searchOrder = Arrays.asList(SearchCriterionType.HDD_FREE_SPACE,                                                              
                                                              SearchCriterionType.RAM,
                                                              SearchCriterionType.NAME,
                                                              SearchCriterionType.HDD_TOTAL_SIZE,
                                                              SearchCriterionType.CPU_COUNT);                                                              
        //represents a list of expected virtual machines (a result of search method call)
        List<VirtualMachine> expVMs = Arrays.asList(vm4);
        
        //there should be returned a list of connected PMs when the method
        //ConnectionManagerImpl::getConnectedPhysicalMachines() is called
        when(conManMock.getConnectedPhysicalMachines()).thenReturn(connectedPMs);
        //after the first call of the method VirtualizationToolManagerImpl::getVirtualMachines() there should
        //returned a list of all available VMs from connected PM pm1, after the second call a list of all
        //available VMs from connected PM pm2 and after the third call an empty list of VMs from connected PM pm3
        when(vtmMock.getVirtualMachines()).thenReturn(vmsFromPM1).thenReturn(vmsFromPM2).thenReturn(vmsFromPM3);
        
        List<VirtualMachine> actVMs = sut.search(searchCriteria, SearchMode.TOLERANT, searchOrder);
        
        //checks the expected and the actual found VMs are equal
        assertDeepVMsEquals(expVMs, actVMs);
    }
    
    /**
     * This test tests that even if the several first attempts are not successful
     * and thus there is not found any virtual machine, the search operation
     * continues and search according to all specified search criteria in a default
     * order while this search order can significantly affect the result of search
     * operation - in this test are in 2. attempt found two VMs with required name,
     * but in the last 3. attempt are found another VMs with different name than
     * the required one and thus these found VMs from 4. attempt are not included
     * to the result of search operation.
     */
    @Test
    public void searchTolerantlyWithSomeExistentVMsAndSomeImpreciseMatchAndDefaultSearchOrder1(){
        //represent connected physical machines from which are retrieved all virtual machines
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("102.10.91.12").build();
        PhysicalMachine pm3 = new PMBuilder().addressIP("102.10.91.11").webserverPort("1005")
                                             .username("John").userPassword("Doe123").build();
        //represent all available virtual machines from connected physical machines
        VirtualMachine vm1 = new VMBuilder().build();
        VirtualMachine vm2 = new VMBuilder().id(UUID.fromString("000d084a-0189-4a55-a9b7-531c455570a1"))
                                            .hostMachine(pm2).sizeOfRAM(8192L)
                                            .hardDiskFreeSpaceSize(8888880001L).hardDiskTotalSize(1200041548L)
                                            .build();
        VirtualMachine vm3 = new VMBuilder().id(UUID.fromString("000d0815-aa89-bbcd-a9b7-531c455570a1"))
                                            .name("VirtualMachine_02").countOfCPU(2L).countOfMonitors(2L)
                                            .cpuExecutionCap(50L).hardDiskFreeSpaceSize(17548744897L)
                                            .hardDiskTotalSize(2300000098745L).sizeOfRAM(2096L)
                                            .sizeOfVRAM(16L).typeOfOS("MS-Windows").identifierOfOS("Win10_64")
                                            .hostMachine(pm2).build();
        VirtualMachine vm4 = new VMBuilder().id(UUID.fromString("1a2d0815-aa89-bbcd-a9b7-531c455ab015"))
                                            .name("VirtualMachine_03").typeOfOS("iOS").identifierOfOS("iOS9_32")
                                            .hostMachine(pm3).hardDiskFreeSpaceSize(17200458999L).build();                                            
        //represents a list of connected physical machines
        List<PhysicalMachine> connectedPMs = Arrays.asList(pm1,pm2,pm3);
        //represents a list of all available virtual machines from connected physical machine pm1
        List<VirtualMachine> vmsFromPM1 = Arrays.asList(vm1);
        //represents a list of all available virtual machines from connected physical machine pm2
        List<VirtualMachine> vmsFromPM2 = Arrays.asList(vm2,vm3);
        //represents a list (empty) of all available virtual machines (none) from connected physical machine pm3
        List<VirtualMachine> vmsFromPM3 = Arrays.asList(vm4);
        //set up a possible 5% deviation up to required value - this will be used when looking for a VM with a required HDD free space
        sut.setMaxDeviation(5);
        //represents search criteria according to which are virtual machines searched
        SearchCriteria searchCriteria = new SearchCriteria.Builder().id(UUID.fromString("11111111-aa89-bbcd-a9b7-531c455ab015"))
                                                                    .hardDiskFreeSpaceSize(17184012099L)
                                                                    .name("VirtualMachine_01")
                                                                    .build();        
        //represents a list of expected virtual machines (a result of search method call)
        List<VirtualMachine> expVMs = Arrays.asList(vm1,vm2);
        
        //there should be returned a list of connected PMs when the method
        //ConnectionManagerImpl::getConnectedPhysicalMachines() is called
        when(conManMock.getConnectedPhysicalMachines()).thenReturn(connectedPMs);
        //after the first call of the method VirtualizationToolManagerImpl::getVirtualMachines() there should
        //returned a list of all available VMs from connected PM pm1, after the second call a list of all
        //available VMs from connected PM pm2 and after the third call an empty list of VMs from connected PM pm3
        when(vtmMock.getVirtualMachines()).thenReturn(vmsFromPM1).thenReturn(vmsFromPM2).thenReturn(vmsFromPM3);
        
        List<VirtualMachine> actVMs = sut.search(searchCriteria, SearchMode.TOLERANT, null);
        
        //checks the expected and the actual found VMs are equal
        assertDeepVMsEquals(expVMs, actVMs);
    }
    
    /**
     * This test tests that even if there occur attempts which are not successful
     * (and are not the first nor the last attempts in order)and thus there is not
     * found any virtual machine, the search operation continues and search
     * according to all specified search criteria in a default order while this
     * search order can significantly affect the result of search operation -
     * in this test are in 1. attempt found two VMs with required name, in 2. attempt
     * there is not found any VM and in the last 3. attempt there are found another
     * VMs, but they were not found in the 1. attempt thus these VMs are not included
     * in the result of search operation.
     */
    @Test
    public void searchTolerantlyWithSomeExistentVMsAndSomeImpreciseMatchDefaultSearchOrder2(){
        //represent connected physical machines from which are retrieved all virtual machines
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("102.10.91.12").build();
        PhysicalMachine pm3 = new PMBuilder().addressIP("102.10.91.11").webserverPort("1005")
                                             .username("John").userPassword("Doe123").build();
        //represent all available virtual machines from connected physical machines
        VirtualMachine vm1 = new VMBuilder().build();
        VirtualMachine vm2 = new VMBuilder().id(UUID.fromString("000d084a-0189-4a55-a9b7-531c455570a1"))
                                            .hostMachine(pm2).sizeOfRAM(8192L)
                                            .hardDiskFreeSpaceSize(8888880001L).hardDiskTotalSize(1200041548L)
                                            .build();
        VirtualMachine vm3 = new VMBuilder().id(UUID.fromString("000d0815-aa89-bbcd-a9b7-531c455570a1"))
                                            .name("VirtualMachine_02").countOfCPU(2L).countOfMonitors(2L)
                                            .cpuExecutionCap(50L).hardDiskFreeSpaceSize(17548744897L)
                                            .hardDiskTotalSize(2300000098745L).sizeOfRAM(2096L)
                                            .sizeOfVRAM(16L).typeOfOS("MS-Windows").identifierOfOS("Win10_64")
                                            .hostMachine(pm2).build();
        VirtualMachine vm4 = new VMBuilder().id(UUID.fromString("1a2d0815-aa89-bbcd-a9b7-531c455ab015"))
                                            .name("VirtualMachine_03").typeOfOS("iOS").identifierOfOS("iOS9_32")
                                            .hostMachine(pm3).hardDiskFreeSpaceSize(17200458999L).build();                                            
        //represents a list of connected physical machines
        List<PhysicalMachine> connectedPMs = Arrays.asList(pm1,pm2,pm3);
        //represents a list of all available virtual machines from connected physical machine pm1
        List<VirtualMachine> vmsFromPM1 = Arrays.asList(vm1);
        //represents a list of all available virtual machines from connected physical machine pm2
        List<VirtualMachine> vmsFromPM2 = Arrays.asList(vm2,vm3);
        //represents a list (empty) of all available virtual machines (none) from connected physical machine pm3
        List<VirtualMachine> vmsFromPM3 = Arrays.asList(vm4);
        //set up a possible 5% deviation up to required value - this will be used when looking for a VM with a required HDD free space
        sut.setMaxDeviation(5);
        //represents search criteria according to which are virtual machines searched
        SearchCriteria searchCriteria = new SearchCriteria.Builder().countOfCPU(3L)
                                                                    .hardDiskFreeSpaceSize(17184012099L)
                                                                    .name("VirtualMachine_01")
                                                                    .build();        
        //represents a list of expected virtual machines (a result of search method call)
        List<VirtualMachine> expVMs = Arrays.asList(vm1,vm2);
        
        //there should be returned a list of connected PMs when the method
        //ConnectionManagerImpl::getConnectedPhysicalMachines() is called
        when(conManMock.getConnectedPhysicalMachines()).thenReturn(connectedPMs);
        //after the first call of the method VirtualizationToolManagerImpl::getVirtualMachines() there should
        //returned a list of all available VMs from connected PM pm1, after the second call a list of all
        //available VMs from connected PM pm2 and after the third call an empty list of VMs from connected PM pm3
        when(vtmMock.getVirtualMachines()).thenReturn(vmsFromPM1).thenReturn(vmsFromPM2).thenReturn(vmsFromPM3);
        
        List<VirtualMachine> actVMs = sut.search(searchCriteria, SearchMode.TOLERANT, null);
        
        //checks the expected and the actual found VMs are equal
        assertDeepVMsEquals(expVMs, actVMs); 
    }
    
    /**
     * This test tests that even if there occur attempts which are not successful
     * (are last attempts in order) and thus there is not
     * found any virtual machine, the search operation continues and search
     * according to all specified search criteria in a default order while this
     * search order can significantly affect the result of search operation -
     * in this test are in 1. attempt found two VMs with required name, in 2. attempt
     * there are found another VMs, but they were not found in the 1. attempt
     * thus these VMs are not included in the result of search operation and in
     * the last 3. attempt there is not found any VM.
     */
    @Test
    public void searchTolerantlyWithSomeExistentVMsAndSomeImpreciseMatchDefaultSearchOrder3(){
        //represent connected physical machines from which are retrieved all virtual machines
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("102.10.91.12").build();
        PhysicalMachine pm3 = new PMBuilder().addressIP("102.10.91.11").webserverPort("1005")
                                             .username("John").userPassword("Doe123").build();
        //represent all available virtual machines from connected physical machines
        VirtualMachine vm1 = new VMBuilder().build();
        VirtualMachine vm2 = new VMBuilder().id(UUID.fromString("000d084a-0189-4a55-a9b7-531c455570a1"))
                                            .hostMachine(pm2).sizeOfRAM(8192L)
                                            .hardDiskFreeSpaceSize(8888880001L).hardDiskTotalSize(1200041548L)
                                            .build();
        VirtualMachine vm3 = new VMBuilder().id(UUID.fromString("000d0815-aa89-bbcd-a9b7-531c455570a1"))
                                            .name("VirtualMachine_02").countOfCPU(2L).countOfMonitors(2L)
                                            .cpuExecutionCap(50L).hardDiskFreeSpaceSize(17548744897L)
                                            .hardDiskTotalSize(2300000098745L).sizeOfRAM(2096L)
                                            .sizeOfVRAM(16L).typeOfOS("MS-Windows").identifierOfOS("Win10_64")
                                            .hostMachine(pm2).build();
        VirtualMachine vm4 = new VMBuilder().id(UUID.fromString("1a2d0815-aa89-bbcd-a9b7-531c455ab015"))
                                            .name("VirtualMachine_03").typeOfOS("iOS").identifierOfOS("iOS9_32")
                                            .hostMachine(pm3).hardDiskFreeSpaceSize(17200458999L).build();                                            
        //represents a list of connected physical machines
        List<PhysicalMachine> connectedPMs = Arrays.asList(pm1,pm2,pm3);
        //represents a list of all available virtual machines from connected physical machine pm1
        List<VirtualMachine> vmsFromPM1 = Arrays.asList(vm1);
        //represents a list of all available virtual machines from connected physical machine pm2
        List<VirtualMachine> vmsFromPM2 = Arrays.asList(vm2,vm3);
        //represents a list (empty) of all available virtual machines (none) from connected physical machine pm3
        List<VirtualMachine> vmsFromPM3 = Arrays.asList(vm4);
        //set up a possible 5% deviation up to required value - this will be used when looking for a VM with a required HDD free space
        sut.setMaxDeviation(5);
        //represents search criteria according to which are virtual machines searched
        SearchCriteria searchCriteria = new SearchCriteria.Builder().hardDiskTotalSize(1200L)
                                                                    .hardDiskFreeSpaceSize(17184012099L)
                                                                    .name("VirtualMachine_01")
                                                                    .build();        
        //represents a list of expected virtual machines (a result of search method call)
        List<VirtualMachine> expVMs = Arrays.asList(vm1,vm2);
        
        //there should be returned a list of connected PMs when the method
        //ConnectionManagerImpl::getConnectedPhysicalMachines() is called
        when(conManMock.getConnectedPhysicalMachines()).thenReturn(connectedPMs);
        //after the first call of the method VirtualizationToolManagerImpl::getVirtualMachines() there should
        //returned a list of all available VMs from connected PM pm1, after the second call a list of all
        //available VMs from connected PM pm2 and after the third call an empty list of VMs from connected PM pm3
        when(vtmMock.getVirtualMachines()).thenReturn(vmsFromPM1).thenReturn(vmsFromPM2).thenReturn(vmsFromPM3);
        
        List<VirtualMachine> actVMs = sut.search(searchCriteria, SearchMode.TOLERANT, null);
        
        //checks the expected and the actual found VMs are equal
        assertDeepVMsEquals(expVMs, actVMs); 
    }
    
    /**
     * This test tests that if the method SearchManagerImpl::search() is called
     * with search criteria which are formed by a required number of hard disk
     * free space size, but also there is set up some deviation, then there will
     * be matched also VMs with hard disk free space size bigger than the precise
     * required number, but which are in the required interval between required
     * precise number and this number increased by a set up deviation.
     */
    @Test
    public void searchWithSetUpDeviationHDDFreeSpaceSize(){
        //represent connected physical machines from which are retrieved all virtual machines
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("102.10.91.12").build();
        PhysicalMachine pm3 = new PMBuilder().addressIP("102.10.91.11").webserverPort("1005")
                                             .username("John").userPassword("Doe123").build();
        //represent all available virtual machines from connected physical machines
        VirtualMachine vm1 = new VMBuilder().build();
        VirtualMachine vm2 = new VMBuilder().id(UUID.fromString("000d084a-0189-4a55-a9b7-531c455570a1"))
                                            .hostMachine(pm2).sizeOfRAM(8192L)
                                            .hardDiskFreeSpaceSize(8888880001L).hardDiskTotalSize(1200041548L)
                                            .build();
        VirtualMachine vm3 = new VMBuilder().id(UUID.fromString("000d0815-aa89-bbcd-a9b7-531c455570a1"))
                                            .name("VirtualMachine_02").countOfCPU(2L).countOfMonitors(2L)
                                            .cpuExecutionCap(50L).hardDiskFreeSpaceSize(14548744895L)
                                            .hardDiskTotalSize(2300000098745L).sizeOfRAM(2096L)
                                            .sizeOfVRAM(16L).typeOfOS("MS-Windows").identifierOfOS("Win10_64")
                                            .hostMachine(pm2).build();
        VirtualMachine vm4 = new VMBuilder().id(UUID.fromString("1a2d0815-aa89-bbcd-a9b7-531c455ab015"))
                                            .name("VirtualMachine_03").typeOfOS("iOS").identifierOfOS("iOS9_32")
                                            .hostMachine(pm3).build();
        //represents a list of connected physical machines
        List<PhysicalMachine> connectedPMs = Arrays.asList(pm1,pm2,pm3);
        //represents a list of all available virtual machines from connected physical machine pm1
        List<VirtualMachine> vmsFromPM1 = Arrays.asList(vm1);
        //represents a list of all available virtual machines from connected physical machine pm2
        List<VirtualMachine> vmsFromPM2 = Arrays.asList(vm2,vm3);
        //represents a list (empty) of all available virtual machines (none) from connected physical machine pm3
        List<VirtualMachine> vmsFromPM3 = Arrays.asList(vm4);
        //set up a possible 5% deviation up to required value - this will be used when looking for a VM with a required HDD free space
        sut.setMaxDeviation(5);
        //represents search criteria according to which are virtual machines searched
        SearchCriteria searchCriteria = new SearchCriteria.Builder().hardDiskFreeSpaceSize(13855947520L).build();
        
        //represents a list of expected virtual machines (a result of search method call)
        List<VirtualMachine> expVMs = Arrays.asList(vm1,vm3,vm4);
        
        //there should be returned a list of connected PMs when the method
        //ConnectionManagerImpl::getConnectedPhysicalMachines() is called
        when(conManMock.getConnectedPhysicalMachines()).thenReturn(connectedPMs);
        //after the first call of the method VirtualizationToolManagerImpl::getVirtualMachines() there should
        //returned a list of all available VMs from connected PM pm1, after the second call a list of all
        //available VMs from connected PM pm2 and after the third call an empty list of VMs from connected PM pm3
        when(vtmMock.getVirtualMachines()).thenReturn(vmsFromPM1).thenReturn(vmsFromPM2).thenReturn(vmsFromPM3);        
        List<VirtualMachine> actVMs = sut.search(searchCriteria, SearchMode.TOLERANT, null);
        
        Collections.sort(expVMs, vmComparator);
        Collections.sort(actVMs, vmComparator);
        
        //checks the expected and the actual found VMs are equal
        assertDeepVMsEquals(expVMs, actVMs);
    }
    
    /**
     * This test tests that if the method SearchManagerImpl::search() is called
     * with search criteria which are formed by a required number of hard disk
     * total size, but also there is set up some deviation, then there will
     * be matched also VMs with hard disk total size bigger than the precise
     * required number, but which are in the required interval between required
     * precise number and this number increased by a set up deviation.
     */
    @Test
    public void searchWithSetUpDeviationHDDTotalSize(){
        //represent connected physical machines from which are retrieved all virtual machines
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("102.10.91.12").build();
        PhysicalMachine pm3 = new PMBuilder().addressIP("102.10.91.11").webserverPort("1005")
                                             .username("John").userPassword("Doe123").build();
        //represent all available virtual machines from connected physical machines
        VirtualMachine vm1 = new VMBuilder().build();
        VirtualMachine vm2 = new VMBuilder().id(UUID.fromString("000d084a-0189-4a55-a9b7-531c455570a1"))
                                            .hostMachine(pm2).sizeOfRAM(8192L)
                                            .hardDiskFreeSpaceSize(8888880001L).hardDiskTotalSize(1200041548L)
                                            .build();
        VirtualMachine vm3 = new VMBuilder().id(UUID.fromString("000d0815-aa89-bbcd-a9b7-531c455570a1"))
                                            .name("VirtualMachine_02").countOfCPU(2L).countOfMonitors(2L)
                                            .cpuExecutionCap(50L).hardDiskFreeSpaceSize(14548744895L)
                                            .hardDiskTotalSize(2300000098745L).sizeOfRAM(2096L)
                                            .sizeOfVRAM(16L).typeOfOS("MS-Windows").identifierOfOS("Win10_64")
                                            .hostMachine(pm2).build();
        VirtualMachine vm4 = new VMBuilder().id(UUID.fromString("1a2d0815-aa89-bbcd-a9b7-531c455ab015"))
                                            .name("VirtualMachine_03").typeOfOS("iOS").identifierOfOS("iOS9_32")
                                            .hostMachine(pm3).build();
        //represents a list of connected physical machines
        List<PhysicalMachine> connectedPMs = Arrays.asList(pm1,pm2,pm3);
        //represents a list of all available virtual machines from connected physical machine pm1
        List<VirtualMachine> vmsFromPM1 = Arrays.asList(vm1);
        //represents a list of all available virtual machines from connected physical machine pm2
        List<VirtualMachine> vmsFromPM2 = Arrays.asList(vm2,vm3);
        //represents a list (empty) of all available virtual machines (none) from connected physical machine pm3
        List<VirtualMachine> vmsFromPM3 = Arrays.asList(vm4);
        //set up a possible 8% deviation up to required value - this will be used when looking for a VM with a required HDD total size
        sut.setMaxDeviation(8);
        //represents search criteria according to which are virtual machines searched
        SearchCriteria searchCriteria = new SearchCriteria.Builder().hardDiskTotalSize(2129629721065L).build();
        
        //represents a list of expected virtual machines (a result of search method call)
        List<VirtualMachine> expVMs = Arrays.asList(vm3);
        
        //there should be returned a list of connected PMs when the method
        //ConnectionManagerImpl::getConnectedPhysicalMachines() is called
        when(conManMock.getConnectedPhysicalMachines()).thenReturn(connectedPMs);
        //after the first call of the method VirtualizationToolManagerImpl::getVirtualMachines() there should
        //returned a list of all available VMs from connected PM pm1, after the second call a list of all
        //available VMs from connected PM pm2 and after the third call an empty list of VMs from connected PM pm3
        when(vtmMock.getVirtualMachines()).thenReturn(vmsFromPM1).thenReturn(vmsFromPM2).thenReturn(vmsFromPM3);        
        List<VirtualMachine> actVMs = sut.search(searchCriteria, SearchMode.TOLERANT, null);
        
        Collections.sort(expVMs, vmComparator);
        Collections.sort(actVMs, vmComparator);
        
        //checks the expected and the actual found VMs are equal
        assertDeepVMsEquals(expVMs, actVMs);
    }
    
    /**
     * This test tests that if the method SearchManagerImpl::search() is called
     * with search criteria which are formed by a required number of RAM size,
     * but also there is set up some deviation, then there will be matched also
     * VMs with RAM size bigger than the precise required number, but which are
     * in the required interval between required precise number and this number
     * increased by a set up deviation.
     */
    @Test
    public void searchWithSetUpDeviationRAM(){
        //represent connected physical machines from which are retrieved all virtual machines
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("102.10.91.12").build();
        PhysicalMachine pm3 = new PMBuilder().addressIP("102.10.91.11").webserverPort("1005")
                                             .username("John").userPassword("Doe123").build();
        //represent all available virtual machines from connected physical machines
        VirtualMachine vm1 = new VMBuilder().build();
        VirtualMachine vm2 = new VMBuilder().id(UUID.fromString("000d084a-0189-4a55-a9b7-531c455570a1"))
                                            .hostMachine(pm2).sizeOfRAM(8192L)
                                            .hardDiskFreeSpaceSize(8888880001L).hardDiskTotalSize(1200041548L)
                                            .build();
        VirtualMachine vm3 = new VMBuilder().id(UUID.fromString("000d0815-aa89-bbcd-a9b7-531c455570a1"))
                                            .name("VirtualMachine_02").countOfCPU(2L).countOfMonitors(2L)
                                            .cpuExecutionCap(50L).hardDiskFreeSpaceSize(14548744895L)
                                            .hardDiskTotalSize(2300000098745L).sizeOfRAM(2096L)
                                            .sizeOfVRAM(16L).typeOfOS("MS-Windows").identifierOfOS("Win10_64")
                                            .hostMachine(pm2).build();
        VirtualMachine vm4 = new VMBuilder().id(UUID.fromString("1a2d0815-aa89-bbcd-a9b7-531c455ab015"))
                                            .name("VirtualMachine_03").typeOfOS("iOS").identifierOfOS("iOS9_32")
                                            .hostMachine(pm3).build();
        //represents a list of connected physical machines
        List<PhysicalMachine> connectedPMs = Arrays.asList(pm1,pm2,pm3);
        //represents a list of all available virtual machines from connected physical machine pm1
        List<VirtualMachine> vmsFromPM1 = Arrays.asList(vm1);
        //represents a list of all available virtual machines from connected physical machine pm2
        List<VirtualMachine> vmsFromPM2 = Arrays.asList(vm2,vm3);
        //represents a list (empty) of all available virtual machines (none) from connected physical machine pm3
        List<VirtualMachine> vmsFromPM3 = Arrays.asList(vm4);
        //set up a possible 15% deviation up to required value - this will be used when looking for a VM with a required RAM size
        sut.setMaxDeviation(15);
        //represents search criteria according to which are virtual machines searched
        SearchCriteria searchCriteria = new SearchCriteria.Builder().sizeOfRAM(3571L).build();
        
        //represents a list of expected virtual machines (a result of search method call)
        List<VirtualMachine> expVMs = Arrays.asList(vm1, vm4);
        
        //there should be returned a list of connected PMs when the method
        //ConnectionManagerImpl::getConnectedPhysicalMachines() is called
        when(conManMock.getConnectedPhysicalMachines()).thenReturn(connectedPMs);
        //after the first call of the method VirtualizationToolManagerImpl::getVirtualMachines() there should
        //returned a list of all available VMs from connected PM pm1, after the second call a list of all
        //available VMs from connected PM pm2 and after the third call an empty list of VMs from connected PM pm3
        when(vtmMock.getVirtualMachines()).thenReturn(vmsFromPM1).thenReturn(vmsFromPM2).thenReturn(vmsFromPM3);        
        List<VirtualMachine> actVMs = sut.search(searchCriteria, SearchMode.TOLERANT, null);
        
        Collections.sort(expVMs, vmComparator);
        Collections.sort(actVMs, vmComparator);
        
        //checks the expected and the actual found VMs are equal
        assertDeepVMsEquals(expVMs, actVMs);
    }
    
    /**
     * This test tests that if the method SearchManagerImpl::search() is called
     * with search criteria which are formed by a required number of video RAM size,
     * but also there is set up some deviation, then there will be matched also
     * VMs with video RAM size bigger than the precise required number, but which are
     * in the required interval between required precise number and this number
     * increased by a set up deviation.
     */
    @Test
    public void searchWithSetUpDeviationVRAM(){
        //represent connected physical machines from which are retrieved all virtual machines
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("102.10.91.12").build();
        PhysicalMachine pm3 = new PMBuilder().addressIP("102.10.91.11").webserverPort("1005")
                                             .username("John").userPassword("Doe123").build();
        //represent all available virtual machines from connected physical machines
        VirtualMachine vm1 = new VMBuilder().build();
        VirtualMachine vm2 = new VMBuilder().id(UUID.fromString("000d084a-0189-4a55-a9b7-531c455570a1"))
                                            .hostMachine(pm2).sizeOfRAM(8192L)
                                            .hardDiskFreeSpaceSize(8888880001L).hardDiskTotalSize(1200041548L)
                                            .build();
        VirtualMachine vm3 = new VMBuilder().id(UUID.fromString("000d0815-aa89-bbcd-a9b7-531c455570a1"))
                                            .name("VirtualMachine_02").countOfCPU(2L).countOfMonitors(2L)
                                            .cpuExecutionCap(50L).hardDiskFreeSpaceSize(14548744895L)
                                            .hardDiskTotalSize(2300000098745L).sizeOfRAM(2096L)
                                            .sizeOfVRAM(16L).typeOfOS("MS-Windows").identifierOfOS("Win10_64")
                                            .hostMachine(pm2).build();
        VirtualMachine vm4 = new VMBuilder().id(UUID.fromString("1a2d0815-aa89-bbcd-a9b7-531c455ab015"))
                                            .name("VirtualMachine_03").typeOfOS("iOS").identifierOfOS("iOS9_32")
                                            .hostMachine(pm3).build();
        //represents a list of connected physical machines
        List<PhysicalMachine> connectedPMs = Arrays.asList(pm1,pm2,pm3);
        //represents a list of all available virtual machines from connected physical machine pm1
        List<VirtualMachine> vmsFromPM1 = Arrays.asList(vm1);
        //represents a list of all available virtual machines from connected physical machine pm2
        List<VirtualMachine> vmsFromPM2 = Arrays.asList(vm2,vm3);
        //represents a list (empty) of all available virtual machines (none) from connected physical machine pm3
        List<VirtualMachine> vmsFromPM3 = Arrays.asList(vm4);
        //set up a possible 20% deviation up to required value - this will be used when looking for a VM with a required VRAM size
        sut.setMaxDeviation(20);
        //represents search criteria according to which are virtual machines searched
        SearchCriteria searchCriteria = new SearchCriteria.Builder().sizeOfVRAM(10L).build();
        
        //represents a list of expected virtual machines (a result of search method call)
        List<VirtualMachine> expVMs = Arrays.asList(vm1,vm2,vm4);
        
        //there should be returned a list of connected PMs when the method
        //ConnectionManagerImpl::getConnectedPhysicalMachines() is called
        when(conManMock.getConnectedPhysicalMachines()).thenReturn(connectedPMs);
        //after the first call of the method VirtualizationToolManagerImpl::getVirtualMachines() there should
        //returned a list of all available VMs from connected PM pm1, after the second call a list of all
        //available VMs from connected PM pm2 and after the third call an empty list of VMs from connected PM pm3
        when(vtmMock.getVirtualMachines()).thenReturn(vmsFromPM1).thenReturn(vmsFromPM2).thenReturn(vmsFromPM3);        
        List<VirtualMachine> actVMs = sut.search(searchCriteria, SearchMode.TOLERANT, null);
        
        Collections.sort(expVMs, vmComparator);
        Collections.sort(actVMs, vmComparator);
        
        //checks the expected and the actual found VMs are equal
        assertDeepVMsEquals(expVMs, actVMs);
    }
    
    /**
     * This test tests that if the method SearchManagerImpl::search() is called
     * when there is no connected physical machine from which there could be
     * retrieved any virtual machine for search operation then the search operation
     * is stopped and an empty list of found virtual machines returned.
     */
    @Test
    public void searchWithNoConnectedPMs(){
        //represents search criteria according to which are virtual machines searched
        SearchCriteria searchCriteria = new SearchCriteria.Builder().name("VirtualMachine_02").typeOfOS("MS-Windows")
                                                                    .countOfCPU(2L).sizeOfVRAM(16L).build();
        //represents an order by which the search operation will be controled
        List<SearchCriterionType> searchOrder = Arrays.asList(SearchCriterionType.RAM,
                                                              SearchCriterionType.OS_TYPE,
                                                              SearchCriterionType.VRAM,
                                                              SearchCriterionType.CPU_COUNT,
                                                              SearchCriterionType.MONITOR_COUNT,
                                                              SearchCriterionType.NAME);
        
        //there should be returned an empty list of connected PMs when the method
        //ConnectionManagerImpl::getConnectedPhysicalMachines() is called
        when(conManMock.getConnectedPhysicalMachines()).thenReturn(new ArrayList<>());
                
        List<VirtualMachine> actVMs = sut.search(searchCriteria, SearchMode.ABSOLUTE_EQUALITY, searchOrder);
        
        assertTrue("The returned list of VMs should be empty", actVMs.isEmpty());
    }
    
    /**
     * This test tests that if the method SearchManagerImpl::search() is called
     * with a null search criteria parameter, then the search operation is stopped
     * and an empty list of found virtual machines is returned.
     */
    @Test
    public void searchWithNullSearchCriteria(){
        //represent connected physical machines from which are retrieved all virtual machines
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("102.10.91.12").build();
        PhysicalMachine pm3 = new PMBuilder().addressIP("102.10.91.11").webserverPort("1005")
                                             .username("John").userPassword("Doe123").build();
        //represents a list of connected physical machines
        List<PhysicalMachine> connectedPMs = Arrays.asList(pm1,pm2,pm3);
        //represents an order by which the search operation will be controled
        List<SearchCriterionType> searchOrder = Arrays.asList(SearchCriterionType.RAM,
                                                              SearchCriterionType.OS_TYPE,
                                                              SearchCriterionType.VRAM,
                                                              SearchCriterionType.CPU_COUNT,
                                                              SearchCriterionType.MONITOR_COUNT,
                                                              SearchCriterionType.NAME);
                
        //there should be returned a list of connected PMs when the method
        //ConnectionManagerImpl::getConnectedPhysicalMachines() is called
        when(conManMock.getConnectedPhysicalMachines()).thenReturn(connectedPMs);
        
        List<VirtualMachine> actVMs = sut.search(null, SearchMode.ABSOLUTE_EQUALITY, searchOrder);
        
        assertTrue("The returned list of VMs should be empty", actVMs.isEmpty());
    }
    
    /**
     * This test tests that if the method SearchManagerImpl::search() is called
     * with a search criteria parameter which does not have specified any of its
     * parameter, then the search operation is stopped and an empty list of found
     * virtual machines is returned.
     */
    @Test
    public void searchWithSearchCriteriaNotHavingSpecifiedAnyParameter(){
        //represent connected physical machines from which are retrieved all virtual machines
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("102.10.91.12").build();
        PhysicalMachine pm3 = new PMBuilder().addressIP("102.10.91.11").webserverPort("1005")
                                             .username("John").userPassword("Doe123").build();
        //represents a list of connected physical machines
        List<PhysicalMachine> connectedPMs = Arrays.asList(pm1,pm2,pm3);
        //represents search criteria according to which are virtual machines searched
        SearchCriteria searchCriteria = new SearchCriteria.Builder().name("").typeOfOS("").countOfCPU(null).build();
        //represents an order by which the search operation will be controled
        List<SearchCriterionType> searchOrder = Arrays.asList(SearchCriterionType.RAM,
                                                              SearchCriterionType.OS_TYPE,
                                                              SearchCriterionType.VRAM,
                                                              SearchCriterionType.CPU_COUNT,
                                                              SearchCriterionType.MONITOR_COUNT,
                                                              SearchCriterionType.NAME);
                
        //there should be returned a list of connected PMs when the method
        //ConnectionManagerImpl::getConnectedPhysicalMachines() is called
        when(conManMock.getConnectedPhysicalMachines()).thenReturn(connectedPMs);
        
        List<VirtualMachine> actVMs = sut.search(searchCriteria, SearchMode.ABSOLUTE_EQUALITY, searchOrder);
        
        assertTrue("The returned list of VMs should be empty", actVMs.isEmpty());
    }
    
    /**
     * This test tests that if the method SearchManagerImpl::search() is called
     * with a null search mode, then the search operation is stopped and an empty
     * list of found virtual machines is returned.
     */
    @Test
    public void searchWithNullSearchMode(){
        //represent connected physical machines from which are retrieved all virtual machines
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("102.10.91.12").build();
        PhysicalMachine pm3 = new PMBuilder().addressIP("102.10.91.11").webserverPort("1005")
                                             .username("John").userPassword("Doe123").build();
        //represents a list of connected physical machines
        List<PhysicalMachine> connectedPMs = Arrays.asList(pm1,pm2,pm3);
        //represents search criteria according to which are virtual machines searched
        SearchCriteria searchCriteria = new SearchCriteria.Builder().name("VirtualMachine_02").typeOfOS("MS-Windows")
                                                                    .countOfCPU(2L).sizeOfVRAM(16L).build();
        //represents an order by which the search operation will be controled
        List<SearchCriterionType> searchOrder = Arrays.asList(SearchCriterionType.RAM,
                                                              SearchCriterionType.OS_TYPE,
                                                              SearchCriterionType.VRAM,
                                                              SearchCriterionType.CPU_COUNT,
                                                              SearchCriterionType.MONITOR_COUNT,
                                                              SearchCriterionType.NAME);
                
        //there should be returned a list of connected PMs when the method
        //ConnectionManagerImpl::getConnectedPhysicalMachines() is called
        when(conManMock.getConnectedPhysicalMachines()).thenReturn(connectedPMs);
        
        List<VirtualMachine> actVMs = sut.search(searchCriteria, null, searchOrder);
        
        assertTrue("The returned list of VMs should be empty", actVMs.isEmpty());
    }
    
    /**
     * This test tests that if the method SearchManagerImpl::search() is called
     * with a null search order, then it is not considered as an illegal argument,
     * but instead of customized search order is used preset default search order.
     */
    @Test
    public void searchWithNullSearchOrder(){
        //represent connected physical machines from which are retrieved all virtual machines
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("102.10.91.12").build();
        PhysicalMachine pm3 = new PMBuilder().addressIP("102.10.91.11").webserverPort("1005")
                                             .username("John").userPassword("Doe123").build();
        //represent all available virtual machines from connected physical machines
        VirtualMachine vm1 = new VMBuilder().build();
        VirtualMachine vm2 = new VMBuilder().id(UUID.fromString("000d084a-0189-4a55-a9b7-531c455570a1"))
                                            .hostMachine(pm2).sizeOfRAM(8192L)
                                            .hardDiskFreeSpaceSize(8888880001L).hardDiskTotalSize(1200041548L)
                                            .build();
        VirtualMachine vm3 = new VMBuilder().id(UUID.fromString("000d0815-aa89-bbcd-a9b7-531c455570a1"))
                                            .name("VirtualMachine_02").countOfCPU(2L).countOfMonitors(2L)
                                            .cpuExecutionCap(50L).hardDiskFreeSpaceSize(17548744897L)
                                            .hardDiskTotalSize(2300000098745L).sizeOfRAM(2096L)
                                            .sizeOfVRAM(16L).typeOfOS("MS-Windows").identifierOfOS("Win10_64")
                                            .hostMachine(pm2).build();
        VirtualMachine vm4 = new VMBuilder().id(UUID.fromString("1a2d0815-aa89-bbcd-a9b7-531c455ab015"))
                                            .name("VirtualMachine_03").typeOfOS("iOS").identifierOfOS("iOS9_32")
                                            .hostMachine(pm3).hardDiskFreeSpaceSize(17200458999L).build();                                            
        //represents a list of connected physical machines
        List<PhysicalMachine> connectedPMs = Arrays.asList(pm1,pm2,pm3);
        //represents a list of all available virtual machines from connected physical machine pm1
        List<VirtualMachine> vmsFromPM1 = Arrays.asList(vm1);
        //represents a list of all available virtual machines from connected physical machine pm2
        List<VirtualMachine> vmsFromPM2 = Arrays.asList(vm2,vm3);
        //represents a list (empty) of all available virtual machines (none) from connected physical machine pm3
        List<VirtualMachine> vmsFromPM3 = Arrays.asList(vm4);
        //set up a possible 5% deviation up to required value - this will be used when looking for a VM with a required HDD free space
        sut.setMaxDeviation(5);
        //represents search criteria according to which are virtual machines searched
        SearchCriteria searchCriteria = new SearchCriteria.Builder().hardDiskTotalSize(1200L)
                                                                    .hardDiskFreeSpaceSize(17184012099L)
                                                                    .name("VirtualMachine_01")
                                                                    .build();        
        //represents a list of expected virtual machines (a result of search method call)
        List<VirtualMachine> expVMs = Arrays.asList(vm1,vm2);
        
        //there should be returned a list of connected PMs when the method
        //ConnectionManagerImpl::getConnectedPhysicalMachines() is called
        when(conManMock.getConnectedPhysicalMachines()).thenReturn(connectedPMs);
        //after the first call of the method VirtualizationToolManagerImpl::getVirtualMachines() there should
        //returned a list of all available VMs from connected PM pm1, after the second call a list of all
        //available VMs from connected PM pm2 and after the third call an empty list of VMs from connected PM pm3
        when(vtmMock.getVirtualMachines()).thenReturn(vmsFromPM1).thenReturn(vmsFromPM2).thenReturn(vmsFromPM3);
        
        List<VirtualMachine> actVMs = sut.search(searchCriteria, SearchMode.TOLERANT, null);
        
        //checks the expected and the actual found VMs are equal
        assertDeepVMsEquals(expVMs, actVMs); 
    }
    
    /**
     * This test tests that if the method SearchManagerImpl::search() is called
     * with an empty search order, then there is used the preset default search
     * order instead of the customized search order.
     */
    @Test
    public void searchWithEmptySearchOrder(){
       //represent connected physical machines from which are retrieved all virtual machines
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("102.10.91.12").build();
        PhysicalMachine pm3 = new PMBuilder().addressIP("102.10.91.11").webserverPort("1005")
                                             .username("John").userPassword("Doe123").build();
        //represent all available virtual machines from connected physical machines
        VirtualMachine vm1 = new VMBuilder().build();
        VirtualMachine vm2 = new VMBuilder().id(UUID.fromString("000d084a-0189-4a55-a9b7-531c455570a1"))
                                            .hostMachine(pm2).sizeOfRAM(8192L)
                                            .hardDiskFreeSpaceSize(8888880001L).hardDiskTotalSize(1200041548L)
                                            .build();
        VirtualMachine vm3 = new VMBuilder().id(UUID.fromString("000d0815-aa89-bbcd-a9b7-531c455570a1"))
                                            .name("VirtualMachine_02").countOfCPU(2L).countOfMonitors(2L)
                                            .cpuExecutionCap(50L).hardDiskFreeSpaceSize(17548744897L)
                                            .hardDiskTotalSize(2300000098745L).sizeOfRAM(2096L)
                                            .sizeOfVRAM(16L).typeOfOS("MS-Windows").identifierOfOS("Win10_64")
                                            .hostMachine(pm2).build();
        VirtualMachine vm4 = new VMBuilder().id(UUID.fromString("1a2d0815-aa89-bbcd-a9b7-531c455ab015"))
                                            .name("VirtualMachine_03").typeOfOS("iOS").identifierOfOS("iOS9_32")
                                            .hostMachine(pm3).hardDiskFreeSpaceSize(17200458999L).build();                                            
        //represents a list of connected physical machines
        List<PhysicalMachine> connectedPMs = Arrays.asList(pm1,pm2,pm3);
        //represents a list of all available virtual machines from connected physical machine pm1
        List<VirtualMachine> vmsFromPM1 = Arrays.asList(vm1);
        //represents a list of all available virtual machines from connected physical machine pm2
        List<VirtualMachine> vmsFromPM2 = Arrays.asList(vm2,vm3);
        //represents a list (empty) of all available virtual machines (none) from connected physical machine pm3
        List<VirtualMachine> vmsFromPM3 = Arrays.asList(vm4);
        //set up a possible 5% deviation up to required value - this will be used when looking for a VM with a required HDD free space
        sut.setMaxDeviation(5);
        //represents search criteria according to which are virtual machines searched
        SearchCriteria searchCriteria = new SearchCriteria.Builder().hardDiskTotalSize(1200L)
                                                                    .hardDiskFreeSpaceSize(17184012099L)
                                                                    .name("VirtualMachine_01")
                                                                    .build();
        //represents an empty search order
        List<SearchCriterionType> searchOrder = new ArrayList<>();
        //represents a list of expected virtual machines (a result of search method call)
        List<VirtualMachine> expVMs = Arrays.asList(vm1,vm2);
        
        //there should be returned a list of connected PMs when the method
        //ConnectionManagerImpl::getConnectedPhysicalMachines() is called
        when(conManMock.getConnectedPhysicalMachines()).thenReturn(connectedPMs);
        //after the first call of the method VirtualizationToolManagerImpl::getVirtualMachines() there should
        //returned a list of all available VMs from connected PM pm1, after the second call a list of all
        //available VMs from connected PM pm2 and after the third call an empty list of VMs from connected PM pm3
        when(vtmMock.getVirtualMachines()).thenReturn(vmsFromPM1).thenReturn(vmsFromPM2).thenReturn(vmsFromPM3);
        
        List<VirtualMachine> actVMs = sut.search(searchCriteria, SearchMode.TOLERANT, searchOrder);
        
        //checks the expected and the actual found VMs are equal
        assertDeepVMsEquals(expVMs, actVMs);  
    }
    
    /**
     * This test tests that if the method SearchManagerImpl::search() is called
     * with a search order which is formed by only null values, then there is
     * used the preset default search order instead of the customized search order.
     */
    @Test
    public void searchWithNonemptySearchOrderFormedByNulls(){
        //represent connected physical machines from which are retrieved all virtual machines
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("102.10.91.12").build();
        PhysicalMachine pm3 = new PMBuilder().addressIP("102.10.91.11").webserverPort("1005")
                                             .username("John").userPassword("Doe123").build();
        //represent all available virtual machines from connected physical machines
        VirtualMachine vm1 = new VMBuilder().build();
        VirtualMachine vm2 = new VMBuilder().id(UUID.fromString("000d084a-0189-4a55-a9b7-531c455570a1"))
                                            .hostMachine(pm2).sizeOfRAM(8192L)
                                            .hardDiskFreeSpaceSize(8888880001L).hardDiskTotalSize(1200041548L)
                                            .build();
        VirtualMachine vm3 = new VMBuilder().id(UUID.fromString("000d0815-aa89-bbcd-a9b7-531c455570a1"))
                                            .name("VirtualMachine_02").countOfCPU(2L).countOfMonitors(2L)
                                            .cpuExecutionCap(50L).hardDiskFreeSpaceSize(17548744897L)
                                            .hardDiskTotalSize(2300000098745L).sizeOfRAM(2096L)
                                            .sizeOfVRAM(16L).typeOfOS("MS-Windows").identifierOfOS("Win10_64")
                                            .hostMachine(pm2).build();
        VirtualMachine vm4 = new VMBuilder().id(UUID.fromString("1a2d0815-aa89-bbcd-a9b7-531c455ab015"))
                                            .name("VirtualMachine_03").typeOfOS("iOS").identifierOfOS("iOS9_32")
                                            .hostMachine(pm3).hardDiskFreeSpaceSize(17200458999L).build();                                            
        //represents a list of connected physical machines
        List<PhysicalMachine> connectedPMs = Arrays.asList(pm1,pm2,pm3);
        //represents a list of all available virtual machines from connected physical machine pm1
        List<VirtualMachine> vmsFromPM1 = Arrays.asList(vm1);
        //represents a list of all available virtual machines from connected physical machine pm2
        List<VirtualMachine> vmsFromPM2 = Arrays.asList(vm2,vm3);
        //represents a list (empty) of all available virtual machines (none) from connected physical machine pm3
        List<VirtualMachine> vmsFromPM3 = Arrays.asList(vm4);
        //set up a possible 5% deviation up to required value - this will be used when looking for a VM with a required HDD free space
        sut.setMaxDeviation(5);
        //represents search criteria according to which are virtual machines searched
        SearchCriteria searchCriteria = new SearchCriteria.Builder().hardDiskTotalSize(1200L)
                                                                    .hardDiskFreeSpaceSize(17184012099L)
                                                                    .name("VirtualMachine_01")
                                                                    .build();
        //represents an empty search order
        List<SearchCriterionType> searchOrder = Arrays.asList(null,null,null);
        //represents a list of expected virtual machines (a result of search method call)
        List<VirtualMachine> expVMs = Arrays.asList(vm1,vm2);
        
        //there should be returned a list of connected PMs when the method
        //ConnectionManagerImpl::getConnectedPhysicalMachines() is called
        when(conManMock.getConnectedPhysicalMachines()).thenReturn(connectedPMs);
        //after the first call of the method VirtualizationToolManagerImpl::getVirtualMachines() there should
        //returned a list of all available VMs from connected PM pm1, after the second call a list of all
        //available VMs from connected PM pm2 and after the third call an empty list of VMs from connected PM pm3
        when(vtmMock.getVirtualMachines()).thenReturn(vmsFromPM1).thenReturn(vmsFromPM2).thenReturn(vmsFromPM3);
        
        List<VirtualMachine> actVMs = sut.search(searchCriteria, SearchMode.TOLERANT, searchOrder);
        
        //checks the expected and the actual found VMs are equal
        assertDeepVMsEquals(expVMs, actVMs);  
    }
    
    /**
     * This test tests that if the method SearchManagerImpl::search() is called
     * with a search order which is formed by both valid and null values, then
     * there are removed the null values from the used search order.
     */
    @Test
    public void searchWithNonemptySearchOrderFormedByValidAndNullValues(){
        //represent connected physical machines from which are retrieved all virtual machines
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("102.10.91.12").build();
        PhysicalMachine pm3 = new PMBuilder().addressIP("102.10.91.11").webserverPort("1005")
                                             .username("John").userPassword("Doe123").build();
        //represent all available virtual machines from connected physical machines
        VirtualMachine vm1 = new VMBuilder().build();
        VirtualMachine vm2 = new VMBuilder().id(UUID.fromString("000d084a-0189-4a55-a9b7-531c455570a1"))
                                            .hostMachine(pm2).sizeOfRAM(8192L)
                                            .hardDiskFreeSpaceSize(8888880001L).hardDiskTotalSize(1200041548L)
                                            .build();
        VirtualMachine vm3 = new VMBuilder().id(UUID.fromString("000d0815-aa89-bbcd-a9b7-531c455570a1"))
                                            .name("VirtualMachine_02").countOfCPU(2L).countOfMonitors(2L)
                                            .cpuExecutionCap(50L).hardDiskFreeSpaceSize(17548744897L)
                                            .hardDiskTotalSize(2300000098745L).sizeOfRAM(2096L)
                                            .sizeOfVRAM(16L).typeOfOS("MS-Windows").identifierOfOS("Win10_64")
                                            .hostMachine(pm2).build();
        VirtualMachine vm4 = new VMBuilder().id(UUID.fromString("1a2d0815-aa89-bbcd-a9b7-531c455ab015"))
                                            .name("VirtualMachine_03").typeOfOS("iOS").identifierOfOS("iOS9_32")
                                            .hostMachine(pm3).hardDiskFreeSpaceSize(17200458999L).build();                                            
        //represents a list of connected physical machines
        List<PhysicalMachine> connectedPMs = Arrays.asList(pm1,pm2,pm3);
        //represents a list of all available virtual machines from connected physical machine pm1
        List<VirtualMachine> vmsFromPM1 = Arrays.asList(vm1);
        //represents a list of all available virtual machines from connected physical machine pm2
        List<VirtualMachine> vmsFromPM2 = Arrays.asList(vm2,vm3);
        //represents a list (empty) of all available virtual machines (none) from connected physical machine pm3
        List<VirtualMachine> vmsFromPM3 = Arrays.asList(vm4);
        //set up a possible 5% deviation up to required value - this will be used when looking for a VM with a required HDD free space
        sut.setMaxDeviation(5);
        //represents search criteria according to which are virtual machines searched
        SearchCriteria searchCriteria = new SearchCriteria.Builder().hardDiskTotalSize(1200L)
                                                                    .hardDiskFreeSpaceSize(17184012099L)
                                                                    .name("VirtualMachine_01")
                                                                    .build();
        //represents an empty search order
        List<SearchCriterionType> searchOrder = Arrays.asList(SearchCriterionType.HDD_FREE_SPACE,
                                                              null,null,null,null,
                                                              SearchCriterionType.HDD_TOTAL_SIZE,
                                                              null,null,null,null,
                                                              SearchCriterionType.NAME);
        //represents a list of expected virtual machines (a result of search method call)
        List<VirtualMachine> expVMs = Arrays.asList(vm3,vm4);
        
        //there should be returned a list of connected PMs when the method
        //ConnectionManagerImpl::getConnectedPhysicalMachines() is called
        when(conManMock.getConnectedPhysicalMachines()).thenReturn(connectedPMs);
        //after the first call of the method VirtualizationToolManagerImpl::getVirtualMachines() there should
        //returned a list of all available VMs from connected PM pm1, after the second call a list of all
        //available VMs from connected PM pm2 and after the third call an empty list of VMs from connected PM pm3
        when(vtmMock.getVirtualMachines()).thenReturn(vmsFromPM1).thenReturn(vmsFromPM2).thenReturn(vmsFromPM3);
        
        List<VirtualMachine> actVMs = sut.search(searchCriteria, SearchMode.TOLERANT, searchOrder);
        
        //checks the expected and the actual found VMs are equal
        assertDeepVMsEquals(expVMs, actVMs);  
    }
    
    /**
     * This test tests that if the method SearchManagerImpl::search() is called
     * with a search order which is formed by valid values while some of them are
     * duplicit, then those duplicit values are removed and VMs are searched
     * according to filtered (without duplicity) customized search order.
     */
    @Test
    public void searchWithNonemptySearchOrderFormedByValidDuplicitValues(){
        //represent connected physical machines from which are retrieved all virtual machines
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("102.10.91.12").build();
        PhysicalMachine pm3 = new PMBuilder().addressIP("102.10.91.11").webserverPort("1005")
                                             .username("John").userPassword("Doe123").build();
        //represent all available virtual machines from connected physical machines
        VirtualMachine vm1 = new VMBuilder().build();
        VirtualMachine vm2 = new VMBuilder().id(UUID.fromString("000d084a-0189-4a55-a9b7-531c455570a1"))
                                            .hostMachine(pm2).sizeOfRAM(8192L)
                                            .hardDiskFreeSpaceSize(8888880001L).hardDiskTotalSize(1200041548L)
                                            .build();
        VirtualMachine vm3 = new VMBuilder().id(UUID.fromString("000d0815-aa89-bbcd-a9b7-531c455570a1"))
                                            .name("VirtualMachine_02").countOfCPU(2L).countOfMonitors(2L)
                                            .cpuExecutionCap(50L).hardDiskFreeSpaceSize(17548744897L)
                                            .hardDiskTotalSize(2300000098745L).sizeOfRAM(2096L)
                                            .sizeOfVRAM(16L).typeOfOS("MS-Windows").identifierOfOS("Win10_64")
                                            .hostMachine(pm2).build();
        VirtualMachine vm4 = new VMBuilder().id(UUID.fromString("1a2d0815-aa89-bbcd-a9b7-531c455ab015"))
                                            .name("VirtualMachine_03").typeOfOS("iOS").identifierOfOS("iOS9_32")
                                            .hostMachine(pm3).hardDiskFreeSpaceSize(17200458999L).build();                                            
        //represents a list of connected physical machines
        List<PhysicalMachine> connectedPMs = Arrays.asList(pm1,pm2,pm3);
        //represents a list of all available virtual machines from connected physical machine pm1
        List<VirtualMachine> vmsFromPM1 = Arrays.asList(vm1);
        //represents a list of all available virtual machines from connected physical machine pm2
        List<VirtualMachine> vmsFromPM2 = Arrays.asList(vm2,vm3);
        //represents a list (empty) of all available virtual machines (none) from connected physical machine pm3
        List<VirtualMachine> vmsFromPM3 = Arrays.asList(vm4);
        //set up a possible 5% deviation up to required value - this will be used when looking for a VM with a required HDD free space
        sut.setMaxDeviation(5);
        //represents search criteria according to which are virtual machines searched
        SearchCriteria searchCriteria = new SearchCriteria.Builder().hardDiskTotalSize(1200L)
                                                                    .hardDiskFreeSpaceSize(17184012099L)
                                                                    .name("VirtualMachine_01")
                                                                    .build();
        //represents an empty search order
        List<SearchCriterionType> searchOrder = Arrays.asList(SearchCriterionType.HDD_FREE_SPACE,
                                                              SearchCriterionType.HDD_TOTAL_SIZE,
                                                              SearchCriterionType.HDD_FREE_SPACE,
                                                              SearchCriterionType.NAME,
                                                              SearchCriterionType.NAME);
        //represents a list of expected virtual machines (a result of search method call)
        List<VirtualMachine> expVMs = Arrays.asList(vm3,vm4);
        
        //there should be returned a list of connected PMs when the method
        //ConnectionManagerImpl::getConnectedPhysicalMachines() is called
        when(conManMock.getConnectedPhysicalMachines()).thenReturn(connectedPMs);
        //after the first call of the method VirtualizationToolManagerImpl::getVirtualMachines() there should
        //returned a list of all available VMs from connected PM pm1, after the second call a list of all
        //available VMs from connected PM pm2 and after the third call an empty list of VMs from connected PM pm3
        when(vtmMock.getVirtualMachines()).thenReturn(vmsFromPM1).thenReturn(vmsFromPM2).thenReturn(vmsFromPM3);
        
        List<VirtualMachine> actVMs = sut.search(searchCriteria, SearchMode.TOLERANT, searchOrder);
        
        //checks the expected and the actual found VMs are equal
        assertDeepVMsEquals(expVMs, actVMs);  
    }
    
    /**
     * This test tests that if the method SearchManagerImpl::search() is called
     * with a search order which is formed by both valid values while some of them are
     * duplicit and null values, then those duplicit and null values are removed
     * and VMs are searched according to filtered (without duplicity) customized search order.
     */
    @Test
    public void searchWithNonemptySearchOrderFormedByValidDuplicitAndNullValues(){
        //represent connected physical machines from which are retrieved all virtual machines
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("102.10.91.12").build();
        PhysicalMachine pm3 = new PMBuilder().addressIP("102.10.91.11").webserverPort("1005")
                                             .username("John").userPassword("Doe123").build();
        //represent all available virtual machines from connected physical machines
        VirtualMachine vm1 = new VMBuilder().build();
        VirtualMachine vm2 = new VMBuilder().id(UUID.fromString("000d084a-0189-4a55-a9b7-531c455570a1"))
                                            .hostMachine(pm2).sizeOfRAM(8192L)
                                            .hardDiskFreeSpaceSize(8888880001L).hardDiskTotalSize(1200041548L)
                                            .build();
        VirtualMachine vm3 = new VMBuilder().id(UUID.fromString("000d0815-aa89-bbcd-a9b7-531c455570a1"))
                                            .name("VirtualMachine_02").countOfCPU(2L).countOfMonitors(2L)
                                            .cpuExecutionCap(50L).hardDiskFreeSpaceSize(17548744897L)
                                            .hardDiskTotalSize(2300000098745L).sizeOfRAM(2096L)
                                            .sizeOfVRAM(16L).typeOfOS("MS-Windows").identifierOfOS("Win10_64")
                                            .hostMachine(pm2).build();
        VirtualMachine vm4 = new VMBuilder().id(UUID.fromString("1a2d0815-aa89-bbcd-a9b7-531c455ab015"))
                                            .name("VirtualMachine_03").typeOfOS("iOS").identifierOfOS("iOS9_32")
                                            .hostMachine(pm3).hardDiskFreeSpaceSize(17200458999L).build();                                            
        //represents a list of connected physical machines
        List<PhysicalMachine> connectedPMs = Arrays.asList(pm1,pm2,pm3);
        //represents a list of all available virtual machines from connected physical machine pm1
        List<VirtualMachine> vmsFromPM1 = Arrays.asList(vm1);
        //represents a list of all available virtual machines from connected physical machine pm2
        List<VirtualMachine> vmsFromPM2 = Arrays.asList(vm2,vm3);
        //represents a list (empty) of all available virtual machines (none) from connected physical machine pm3
        List<VirtualMachine> vmsFromPM3 = Arrays.asList(vm4);
        //set up a possible 5% deviation up to required value - this will be used when looking for a VM with a required HDD free space
        sut.setMaxDeviation(5);
        //represents search criteria according to which are virtual machines searched
        SearchCriteria searchCriteria = new SearchCriteria.Builder().hardDiskTotalSize(1200L)
                                                                    .hardDiskFreeSpaceSize(17184012099L)
                                                                    .name("VirtualMachine_01")
                                                                    .build();
        //represents an empty search order
        List<SearchCriterionType> searchOrder = Arrays.asList(SearchCriterionType.HDD_FREE_SPACE,
                                                              null,null,
                                                              SearchCriterionType.HDD_FREE_SPACE,
                                                              null,null,
                                                              SearchCriterionType.HDD_TOTAL_SIZE,
                                                              null,null,
                                                              SearchCriterionType.NAME,
                                                              null,null,
                                                              SearchCriterionType.NAME);
        //represents a list of expected virtual machines (a result of search method call)
        List<VirtualMachine> expVMs = Arrays.asList(vm3,vm4);
        
        //there should be returned a list of connected PMs when the method
        //ConnectionManagerImpl::getConnectedPhysicalMachines() is called
        when(conManMock.getConnectedPhysicalMachines()).thenReturn(connectedPMs);
        //after the first call of the method VirtualizationToolManagerImpl::getVirtualMachines() there should
        //returned a list of all available VMs from connected PM pm1, after the second call a list of all
        //available VMs from connected PM pm2 and after the third call an empty list of VMs from connected PM pm3
        when(vtmMock.getVirtualMachines()).thenReturn(vmsFromPM1).thenReturn(vmsFromPM2).thenReturn(vmsFromPM3);
        
        List<VirtualMachine> actVMs = sut.search(searchCriteria, SearchMode.TOLERANT, searchOrder);
        
        //checks the expected and the actual found VMs are equal
        assertDeepVMsEquals(expVMs, actVMs);  
    }
    
    @Test
    public void searchWithAnyConnectionProblemWhileRetrievingVMs() throws Exception{
        //represent connected physical machines from which are retrieved all virtual machines
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("102.10.91.12").build();
        PhysicalMachine pm3 = new PMBuilder().addressIP("102.10.91.11").webserverPort("1005")
                                             .username("John").userPassword("Doe123").build();
        //represent all available virtual machines from connected physical machines
        VirtualMachine vm1 = new VMBuilder().build();
        VirtualMachine vm2 = new VMBuilder().id(UUID.fromString("000d084a-0189-4a55-a9b7-531c455570a1"))
                                            .hostMachine(pm2).sizeOfRAM(8192L)
                                            .hardDiskFreeSpaceSize(8888880001L).hardDiskTotalSize(1200041548L)
                                            .build();
        VirtualMachine vm3 = new VMBuilder().id(UUID.fromString("000d0815-aa89-bbcd-a9b7-531c455570a1"))
                                            .name("VirtualMachine_02").countOfCPU(2L).countOfMonitors(2L)
                                            .cpuExecutionCap(50L).hardDiskFreeSpaceSize(17548744897L)
                                            .hardDiskTotalSize(2300000098745L).sizeOfRAM(2096L)
                                            .sizeOfVRAM(16L).typeOfOS("MS-Windows").identifierOfOS("Win10_64")
                                            .hostMachine(pm2).build();
        VirtualMachine vm4 = new VMBuilder().id(UUID.fromString("1a2d0815-aa89-bbcd-a9b7-531c455ab015"))
                                            .name("VirtualMachine_03").typeOfOS("iOS").identifierOfOS("iOS9_32")
                                            .hostMachine(pm3).hardDiskFreeSpaceSize(17200458999L).build();                                            
        //represents a list of connected physical machines
        List<PhysicalMachine> connectedPMs = Arrays.asList(pm1,pm2,pm3);
        //represents a list of all available virtual machines from connected physical machine pm1
        List<VirtualMachine> vmsFromPM1 = Arrays.asList(vm1);
        //represents a list of all available virtual machines from connected physical machine pm2
        List<VirtualMachine> vmsFromPM2 = Arrays.asList(vm2,vm3);
        //represents a list (empty) of all available virtual machines (none) from connected physical machine pm3
        List<VirtualMachine> vmsFromPM3 = Arrays.asList(vm4);
        //set up a possible 5% deviation up to required value - this will be used when looking for a VM with a required HDD free space
        sut.setMaxDeviation(5);
        //represents search criteria according to which are virtual machines searched
        SearchCriteria searchCriteria = new SearchCriteria.Builder().hardDiskTotalSize(1200L)
                                                                    .hardDiskFreeSpaceSize(17184012099L)
                                                                    .name("VirtualMachine_01")
                                                                    .build();
        //represents a customized search order
        List<SearchCriterionType> searchOrder = Arrays.asList(SearchCriterionType.HDD_FREE_SPACE,
                                                              null,null,
                                                              SearchCriterionType.HDD_FREE_SPACE,
                                                              null,null,
                                                              SearchCriterionType.HDD_TOTAL_SIZE,
                                                              null,null,
                                                              SearchCriterionType.NAME,
                                                              null,null,
                                                              SearchCriterionType.NAME);
        //represents a list of expected virtual machines (a result of search method call)
        List<VirtualMachine> expVMs = Arrays.asList(vm3);
        NativeVBoxAPIManager natAPIManMock = mock(NativeVBoxAPIManager.class);
        whenNew(NativeVBoxAPIManager.class).withNoArguments().thenReturn(natAPIManMock);
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        OutputHandler.setErrorOutputStream(new PrintStream(errContent));
        
        //there should be returned a list of connected PMs when the method
        //ConnectionManagerImpl::getConnectedPhysicalMachines() is called
        when(conManMock.getConnectedPhysicalMachines()).thenReturn(connectedPMs);
        //after the first call of the method VirtualizationToolManagerImpl::getVirtualMachines() there should
        //returned a list of all available VMs from connected PM pm1, after the second call a list of all
        //available VMs from connected PM pm2 and after the third call an empty list of VMs from connected PM pm3
        doReturn(vmsFromPM1).doReturn(vmsFromPM2).doCallRealMethod().when(vtmMock).getVirtualMachines();
        when(conManMock.isConnected(pm3)).thenReturn(true);
        doThrow(conFailExMock).when(natAPIManMock).getAllVirtualMachines(pm3);
        when(conFailExMock.getMessage()).thenReturn("Some error message");
        
        List<VirtualMachine> actVMs = sut.search(searchCriteria, SearchMode.TOLERANT, searchOrder);
        
        //checks the expected and the actual found VMs are equal
        assertDeepVMsEquals(expVMs, actVMs);
        assertFalse("There should be written any error message on an error output stream",
                    errContent.toString().isEmpty());
        OutputHandler.setErrorOutputStream(System.err);
    }
    

    /**
     * Class Builder for easier and faster creating and setting up new object of
     * type PhysicalMachine.
     */
    class PMBuilder {

        private String addressIP = "180.148.14.10";
        private String portOfVBoxWebServer = "18083";
        private String username = "Jack";
        private String userPassword = "tr1h15jk7";

        public PMBuilder() {

        }

        public PMBuilder addressIP(String value) {
            this.addressIP = value;
            return this;
        }

        public PMBuilder webserverPort(String value) {
            this.portOfVBoxWebServer = value;
            return this;
        }

        public PMBuilder username(String value) {
            this.username = value;
            return this;
        }

        public PMBuilder userPassword(String value) {
            this.userPassword = value;
            return this;
        }

        public PhysicalMachine build() {
            return new PhysicalMachine(this.addressIP, this.portOfVBoxWebServer,
                    this.username, this.userPassword);
        }
    }

    /**
     * Class Builder for easier and faster creating and setting up new object of
     * type VirtualMachine.
     */
    class VMBuilder {

        private UUID id = UUID.fromString("793d084a-0189-4a55-a9b7-531c455570a1");
        private String name = "VirtualMachine_01";
        private PhysicalMachine hostMachine = new PMBuilder().build();
        private Long countOfCPU = 1L;
        private Long countOfMonitors = 1L;
        private Long cpuExecutionCap = 100L;
        private Long hardDiskFreeSpaceSize = 14286848000L;
        private Long hardDiskTotalSize = 21474836480L;
        private Long sizeOfRAM = 4096L;
        private Long sizeOfVRAM = 12L;
        private String typeOfOS = "Linux";
        private String identifierOfOS = "Fedora_64";

        public VMBuilder() {

        }

        public VMBuilder id(UUID uuid) {
            this.id = uuid;
            return this;
        }

        public VMBuilder name(String name) {
            this.name = name;
            return this;
        }

        public VMBuilder hostMachine(PhysicalMachine hostMachine) {
            this.hostMachine = hostMachine;
            return this;
        }

        public VMBuilder countOfCPU(Long count) {
            this.countOfCPU = count;
            return this;
        }

        public VMBuilder countOfMonitors(Long count) {
            this.countOfMonitors = count;
            return this;
        }

        public VMBuilder cpuExecutionCap(Long cpuExecutionCap) {
            this.cpuExecutionCap = cpuExecutionCap;
            return this;
        }

        public VMBuilder hardDiskFreeSpaceSize(Long size) {
            this.hardDiskFreeSpaceSize = size;
            return this;
        }

        public VMBuilder hardDiskTotalSize(Long size) {
            this.hardDiskTotalSize = size;
            return this;
        }

        public VMBuilder sizeOfRAM(Long size) {
            this.sizeOfRAM = size;
            return this;
        }

        public VMBuilder sizeOfVRAM(Long size) {
            this.sizeOfVRAM = size;
            return this;
        }

        public VMBuilder typeOfOS(String typeOfOS) {
            this.typeOfOS = typeOfOS;
            return this;
        }

        public VMBuilder identifierOfOS(String identifierOfOS) {
            this.identifierOfOS = identifierOfOS;
            return this;
        }

        public VirtualMachine build() {
            return new VirtualMachine.Builder(id, name, hostMachine)
                    .countOfCPU(countOfCPU).countOfMonitors(countOfMonitors)
                    .cpuExecutionCap(cpuExecutionCap)
                    .hardDiskFreeSpaceSize(hardDiskFreeSpaceSize)
                    .hardDiskTotalSize(hardDiskTotalSize)
                    .sizeOfRAM(sizeOfRAM)
                    .sizeOfVRAM(sizeOfVRAM)
                    .typeOfOS(typeOfOS)
                    .identifierOfOS(identifierOfOS)
                    .build();
        }
    }
    
    private void assertDeepVMsEquals(List<VirtualMachine> expVMs, List<VirtualMachine> actVMs) {
        assertEquals("List should have same size", expVMs, actVMs);
        for (int i = 0; i < expVMs.size(); ++i) {
            VirtualMachine expVM = expVMs.get(i);
            VirtualMachine actVM = actVMs.get(i);
            assertDeepVMsEquals(expVM, actVM);
        }
    }

    private void assertDeepVMsEquals(VirtualMachine expVM, VirtualMachine actVM) {
        assertEquals("VMs should have same id", expVM.getId(), actVM.getId());
        assertEquals("VMs should have same name", expVM.getName(), actVM.getName());
        assertEquals("VMs should have same host machine", expVM.getHostMachine(), actVM.getHostMachine());
        assertEquals("VMs should have same count of CPUs", expVM.getCountOfCPU(), actVM.getCountOfCPU());
        assertEquals("VMs should have same count of monitors", expVM.getCountOfMonitors(), actVM.getCountOfMonitors());
        assertEquals("VMs should have same CPUExecutionCap", expVM.getCPUExecutionCap(), actVM.getCPUExecutionCap());
        assertEquals("VMs should have same HDD free space size", expVM.getHardDiskFreeSpaceSize(), actVM.getHardDiskFreeSpaceSize());
        assertEquals("VMs should have same HDD total size", expVM.getHardDiskTotalSize(), actVM.getHardDiskTotalSize());
        assertEquals("VMs should have same RAM size", expVM.getSizeOfRAM(), actVM.getSizeOfRAM());
        assertEquals("VMs should have same video RAM size", expVM.getSizeOfVRAM(), actVM.getSizeOfVRAM());
        assertEquals("VMs should have same type of OS", expVM.getTypeOfOS(), actVM.getTypeOfOS());
        assertEquals("VMs should have same version of OS", expVM.getIdentifierOfOS(), actVM.getIdentifierOfOS());
    }

    private static Comparator<VirtualMachine> vmComparator = new Comparator<VirtualMachine>() {

        @Override
        public int compare(VirtualMachine o1, VirtualMachine o2) {
            int res = o1.getId().compareTo(o2.getId());

            if (res == 0) {
                return (o1.getName().compareTo(o2.getName()));
            }

            return res;
        }
    };
}
