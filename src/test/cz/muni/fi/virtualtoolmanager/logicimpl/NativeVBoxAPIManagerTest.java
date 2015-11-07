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
import cz.muni.fi.virtualtoolmanager.pubapi.entities.VirtualMachine;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnexpectedVMStateException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownVirtualMachineException;
import cz.muni.fi.virtualtoolmanager.pubapi.types.CloneType;
import java.io.File;
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
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.doThrow;
import org.powermock.api.mockito.PowerMockito;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import org.virtualbox_4_3.CleanupMode;
import org.virtualbox_4_3.CloneMode;
import org.virtualbox_4_3.CloneOptions;
import org.virtualbox_4_3.IConsole;
import org.virtualbox_4_3.IGuestOSType;
import org.virtualbox_4_3.IMachine;
import org.virtualbox_4_3.IMedium;
import org.virtualbox_4_3.IMediumAttachment;
import org.virtualbox_4_3.IProgress;
import org.virtualbox_4_3.ISession;
import org.virtualbox_4_3.ISnapshot;
import org.virtualbox_4_3.ISystemProperties;
import org.virtualbox_4_3.IVirtualBox;
import org.virtualbox_4_3.IVirtualBoxErrorInfo;
import org.virtualbox_4_3.MachineState;
import org.virtualbox_4_3.MediumState;
import org.virtualbox_4_3.SessionState;
import org.virtualbox_4_3.VBoxException;
import org.virtualbox_4_3.VirtualBoxManager;

/**
 * This test class ensure unit testing of class NativeVBoxAPIManager and is
 * intended to be a pointer that class NativeVBoxAPIConnection works as
 * expected.
 *
 * @author Tomáš Šmíd
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({NativeVBoxAPIManager.class, VirtualBoxManager.class})
public class NativeVBoxAPIManagerTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private NativeVBoxAPIManager sut;
    private VirtualBoxManager vbmMock;
    private IVirtualBox vboxMock;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(VirtualBoxManager.class);
        vbmMock = mock(VirtualBoxManager.class);
        vboxMock = mock(IVirtualBox.class);
        when(VirtualBoxManager.createInstance(null)).thenReturn(vbmMock);
        sut = new NativeVBoxAPIManager();
    }

    /**
     * This test tests that there does not appear any error nor exception when
     * the method NativeVBoxAPIManager::registerVirtualMachine() is called with
     * valid arguments and virtual machine which should be newly added to the
     * list of all available virtual machines on the particular physical machine
     * is not already registered.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void registerVirtualMachineIdealCase() throws Exception {
        //physical machine on which should be virtual machine registered
        PhysicalMachine pm = new PMBuilder().build();
        //virtual machine which is intended to be newly registered
        String vmName = "VirtualMachine_01";
        //mock object of type ISystemProperties for better test control
        ISystemProperties sysPropMock = mock(ISystemProperties.class);

        //mocked object of type IVirtualBox is returned in order to have a control
        //over the method performing
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //when the method IVirtualBox::findMachine() is called then the exception VBoxException
        //is invoked which means that the virtual machine with name "VirtualMachine_01" can be registered
        doThrow(VBoxException.class).when(vboxMock).findMachine(vmName);
        //there should be returned a mock object of type ISystemProperties when the method
        //IVirtualBox::getSystemProperties() is called in order to control behavior and returned values of its methods
        when(vboxMock.getSystemProperties()).thenReturn(sysPropMock);

        assertTrue("There should be returned positive answer, which means that the virtual machine "
                + vmName + " was successfully registered", sut.registerVirtualMachine(pm, vmName));
    }

    /**
     * This test tests that there cannot be registered virtual machine on
     * physical machine which is already registered on this physical machine.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void registerAlreadyRegisteredVirtualMachine() throws Exception {
        //physical machine on which should be virtual machine registered
        PhysicalMachine pm = new PMBuilder().build();
        //virtual machine which is intended to be newly registered
        String vmName = "VirtualMachine_01";
        //mock object of type ISystemProperties for better test control
        ISystemProperties sysPropMock = mock(ISystemProperties.class);
        //mock object of type IMachine for better test control
        IMachine vboxMachineMock = mock(IMachine.class);

        //mocked object of type IVirtualBox is returned in order to have a control
        //over the method performing
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //when the method IVirtualBox::findMachine() is called first time then the exception VBoxException
        //is invoked which means that the virtual machine with name "VirtualMachine_01" can be registered
        //when the method is called second time or several times then there does not happen anything
        //which means that the virtual machine is already registered and cannot be registered again
        doThrow(VBoxException.class).doReturn(vboxMachineMock).when(vboxMock).findMachine(vmName);
        //there should be returned a mock object of type ISystemProperties when the method
        //IVirtualBox::getSystemProperties() is called in order to control behavior and returned values of its methods
        when(vboxMock.getSystemProperties()).thenReturn(sysPropMock);

        assertTrue("There should be returned positive answer, which means that the virtual machine "
                + vmName + " was successfully registered", sut.registerVirtualMachine(pm, vmName));
        assertFalse("There should be returned negative answer, which means that the virtual machine "
                + vmName + " could not be registered, because it has already been registered",
                sut.registerVirtualMachine(pm, vmName));
    }

    /**
     * This test tests that there is invoked UnknownVirtualMachineException
     * exception when the virtual machine which is intended to be registered on
     * a physical machine is not present (its image (dvi)) in a default VM
     * folder of the virtualization tool VirtualBox on this physical machine.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void registerNonexistentVirtualMachine() throws Exception {
        //physical machine on which should be virtual machine registered
        PhysicalMachine pm = new PMBuilder().build();
        //virtual machine which is intended to be newly registered
        String vmName = "VirtualMachine_01";
        //mock object of type ISystemProperties for better test control
        ISystemProperties sysPropMock = mock(ISystemProperties.class);
        VBoxException vboxExceptionMock = mock(VBoxException.class);

        //mocked object of type IVirtualBox is returned in order to have a control
        //over the method performing
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //when the method IVirtualBox::findMachine() is called then the exception VBoxException
        //is invoked which means that the virtual machine with name "VirtualMachine_01" can be registered        
        doThrow(VBoxException.class).when(vboxMock).findMachine(vmName);
        //there should be returned a mock object of type ISystemProperties when the method
        //IVirtualBox::getSystemProperties() is called in order to control behavior and returned values of its methods
        when(vboxMock.getSystemProperties()).thenReturn(sysPropMock);
        //when the method IVirtualBox::openMachine() is called then the exception VBoxException
        //is invoked which means that the virtual machine with name "VirtualMachine_01" is not
        //present in a default VM folder of virtualization tool VirtualBox 
        doThrow(vboxExceptionMock).when(vboxMock).openMachine(contains(vmName));
        //string "VirtualBox VMs" should be returned when the method ISystemProperties::getDefaultMachineFolder() is called
        when(sysPropMock.getDefaultMachineFolder()).thenReturn("VirtualBox VMs");
        //string "(Path not found.)" should be returned when the method VBoxException::getMessage() is called
        when(vboxExceptionMock.getMessage()).thenReturn("(Path not found.)");

        exception.expect(UnknownVirtualMachineException.class);
        sut.registerVirtualMachine(pm, vmName);
    }
    
    /**
     * This test tests that there is invoked UnknownVirtualMachineException
     * exception when the virtual machine which is intended to be registered on
     * a physical machine has not its vdi file in a default VM
     * folder of the virtualization tool VirtualBox on this physical machine.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void registerVirtualMachineWithMissingVDI() throws Exception {
        //physical machine on which should be virtual machine registered
        PhysicalMachine pm = new PMBuilder().build();
        //virtual machine which is intended to be newly registered
        String vmName = "VirtualMachine_01";
        //mock object of type ISystemProperties for better test control
        ISystemProperties sysPropMock = mock(ISystemProperties.class);
        VBoxException vboxExceptionMock = mock(VBoxException.class);

        //mocked object of type IVirtualBox is returned in order to have a control
        //over the method performing
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //when the method IVirtualBox::findMachine() is called then the exception VBoxException
        //is invoked which means that the virtual machine with name "VirtualMachine_01" can be registered        
        doThrow(VBoxException.class).when(vboxMock).findMachine(vmName);
        //there should be returned a mock object of type ISystemProperties when the method
        //IVirtualBox::getSystemProperties() is called in order to control behavior and returned values of its methods
        when(vboxMock.getSystemProperties()).thenReturn(sysPropMock);
        //when the method IVirtualBox::openMachine() is called then the exception VBoxException
        //is invoked which means that the virtual machine with name "VirtualMachine_01" is not
        //present in a default VM folder of virtualization tool VirtualBox 
        doThrow(vboxExceptionMock).when(vboxMock).openMachine(contains(vmName));
        //string "VirtualBox VMs" should be returned when the method ISystemProperties::getDefaultMachineFolder() is called
        when(sysPropMock.getDefaultMachineFolder()).thenReturn("VirtualBox VMs");
        //string "Error occured: Could not find an open hard disk" should be returned when the method VBoxException::getMessage() is called
        when(vboxExceptionMock.getMessage()).thenReturn("Error occured: Could not find an open hard disk");

        exception.expect(UnknownVirtualMachineException.class);
        sut.registerVirtualMachine(pm, vmName);
    }
    
    /**
     * This test tests that there is invoked UnknownVirtualMachineException
     * exception when the virtual machine which is intended to be registered on
     * a physical machine could not be opened (missing or corrupted configuration
     * files or another problem).
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void registerVirtualMachineWithUnsuccessfulMachineOpening() throws Exception {
        //physical machine on which should be virtual machine registered
        PhysicalMachine pm = new PMBuilder().build();
        //virtual machine which is intended to be newly registered
        String vmName = "VirtualMachine_01";
        //mock object of type ISystemProperties for better test control
        ISystemProperties sysPropMock = mock(ISystemProperties.class);
        VBoxException vboxExceptionMock = mock(VBoxException.class);

        //mocked object of type IVirtualBox is returned in order to have a control
        //over the method performing
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //when the method IVirtualBox::findMachine() is called then the exception VBoxException
        //is invoked which means that the virtual machine with name "VirtualMachine_01" can be registered        
        doThrow(VBoxException.class).when(vboxMock).findMachine(vmName);
        //there should be returned a mock object of type ISystemProperties when the method
        //IVirtualBox::getSystemProperties() is called in order to control behavior and returned values of its methods
        when(vboxMock.getSystemProperties()).thenReturn(sysPropMock);
        //when the method IVirtualBox::openMachine() is called then the exception VBoxException
        //is invoked which means that the virtual machine with name "VirtualMachine_01" is not
        //present in a default VM folder of virtualization tool VirtualBox 
        doThrow(vboxExceptionMock).when(vboxMock).openMachine(contains(vmName));
        //string "VirtualBox VMs" should be returned when the method ISystemProperties::getDefaultMachineFolder() is called
        when(sysPropMock.getDefaultMachineFolder()).thenReturn("VirtualBox VMs");
        //string "Error occured" should be returned when the method VBoxException::getMessage() is called
        when(vboxExceptionMock.getMessage()).thenReturn("Error occured");

        exception.expect(UnknownVirtualMachineException.class);
        sut.registerVirtualMachine(pm, vmName);
    }

    /**
     * This test tests that if there suddenly appears network connection problem
     * then there will be invoked ConnectionFailureException exception.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void registerVirtualMachineWithSuddenNetworkConnectionLoss() throws Exception {
        //physical machine on which should be virtual machine registered
        PhysicalMachine pm = new PMBuilder().build();
        //a url of physical machine used for establishing connection with physical machine
        String url = "http://" + pm.getAddressIP() + ":" + pm.getPortOfVTWebServer();
        //virtual machine which is intended to be newly registered
        String vmName = "VirtualMachine_01";

        //at a moment when the work with physical machine pm should start there is
        //invoked VBoxException which simulates an inavailable network connection
        doThrow(VBoxException.class).when(vbmMock).connect(url, pm.getUsername(), pm.getUserPassword());

        exception.expect(ConnectionFailureException.class);
        sut.registerVirtualMachine(pm, vmName);
    }

    /**
     * This test tests that there is returned the required virtual machine
     * without any exception and error occurance when the method
     * NativeVBoxAPIManager::getVirtualMachine() is called with vmId which
     * actually identify some virtual machine from the list of registered
     * virtual machines.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void getVirtualMachineByIdWithSomeMatch() throws Exception {
        //represents host machine from which is virtual machine required
        PhysicalMachine pm = new PMBuilder().build();
        //represents virtual machine with all correct and expected values of its attributes 
        VirtualMachine expectedVM = new VMBuilder().build();
        //represents ID of required virtual machine
        String vmId = expectedVM.getId().toString();
        //represents mock object of type IMachine for better test control
        IMachine machineMocked = mock(IMachine.class);
        //represents mock object of type IGuestOSType for better test control
        IGuestOSType guestOSTypeMocked = mock(IGuestOSType.class);
        //represents mock object of type IMedium for better test control
        IMedium mediumMocked = mock(IMedium.class);
        //represents mock object of type IMediumAttachment for easier and better test control 
        IMediumAttachment medAttachMock = mock(IMediumAttachment.class);
        //represents list of SATA medium controllers
        List<IMediumAttachment> medAttachs = Arrays.asList(medAttachMock);

        //mock object of type IVirtualBox is returned when the method VirtualBoxManager::getVBox()
        //is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //mock object of type IMachine is returned when the method IVirtualBox::findMachine() is called
        //with vmId of required virtual machine in order to control returned values of its methods
        when(vboxMock.findMachine(vmId)).thenReturn(machineMocked);
        //mock object of type IGuestOSType is returned when the method IVirtualBox::getGuestOSType() is called
        //with OS type vmId of required virtual machine in order to control returned values of its methods
        when(vboxMock.getGuestOSType(machineMocked.getOSTypeId())).thenReturn(guestOSTypeMocked);
        //string with value = "Linux" should be returned when the method IGuestOSType::getFamilyId() is called
        when(guestOSTypeMocked.getFamilyId()).thenReturn("Linux");
        //string with value = "Fedora_64" should be returned when the method IGuestOSType::getId() is called
        when(guestOSTypeMocked.getId()).thenReturn("Fedora_64");
        //list medAttachs is returned when the method IMachine::getMediumAttachmentOfController() is called for SATA type
        when(machineMocked.getMediumAttachmentsOfController("SATA")).thenReturn(medAttachs);
        //port number = 0 should be returned when the method IMediumAttachment::getPort() is called
        when(medAttachMock.getPort()).thenReturn(0);
        //device number = 0 should be returned when the method IMediumAttachment::getDevice() is called
        when(medAttachMock.getDevice()).thenReturn(0);
        //mock object of type IMedium is returned when the method IMachine::getMedium() is called
        //with particular arguments in order to control returned values of its methods
        when(machineMocked.getMedium("SATA", 0, 0)).thenReturn(mediumMocked);
        //string with value = "VirtualMachine_01" should be returned when the method IMachine::getName() is called
        when(machineMocked.getName()).thenReturn(expectedVM.getName());
        //string with value = "793d084a-0189-4a55-a9b7-531c455570a1" should be returned when the methodIMachine::getId() is called
        when(machineMocked.getId()).thenReturn(vmId);
        //there should be returned Long value = 1 when the method IMachine::getCPUCount() is called
        when(machineMocked.getCPUCount()).thenReturn(expectedVM.getCountOfCPU());
        //there should be returned Long value = 1 when the method IMachine::getMonitorCount() is called
        when(machineMocked.getMonitorCount()).thenReturn(expectedVM.getCountOfMonitors());
        //there should be returned Long value = 100 when the method IMachine::getCPUExecutionCap() is called
        when(machineMocked.getCPUExecutionCap()).thenReturn(expectedVM.getCPUExecutionCap());
        //there should be returned Long value = 4096 when the method IMachine::getMemorySize() is called
        when(machineMocked.getMemorySize()).thenReturn(expectedVM.getSizeOfRAM());
        //there should be returned Long value = 12 when the method IMachine::getVRAMSize() is called
        when(machineMocked.getVRAMSize()).thenReturn(expectedVM.getSizeOfVRAM());
        //there should be returned Long value = 21474836480 when the method IMedium::getLogicalSize() is called
        when(mediumMocked.getLogicalSize()).thenReturn(expectedVM.getHardDiskTotalSize());
        //there should be returned Long value = 7187988480 when the method IMedium::getSize() is called
        when(mediumMocked.getSize()).thenReturn(expectedVM.getHardDiskTotalSize() - expectedVM.getHardDiskFreeSpaceSize());

        //there should not be invoked any exception nor any error should not appear
        VirtualMachine actualVM = sut.getVirtualMachine(pm, vmId);

        //all attributes of VirtualMachine objects will be compared and should be same
        assertDeepVMsEquals(expectedVM, actualVM);
    }

    /**
     * This test tests that there should be invoked
     * UnknownVirtualMachineException exception when there is not any registered
     * virtual machine with the used uuid.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void getVirtualMachineByIdWithNoMatch() throws Exception {
        //represents host machine from which is virtual machine required
        PhysicalMachine pm = new PMBuilder().build();
        //represents ID of required virtual machine
        String vmId = "793d084a-0189-4a55-a9b7-531c455570a1";

        //mock object of type IVirtualBox is returned when the method VirtualBoxManager::getVBox()
        //is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there should be thrown VBoxException exception when the method IVirtualBox::findMachine()
        //is called with uuid of required virtual machine which means there is not any
        //registered virtual machine with vmId equal to used uuid
        doThrow(VBoxException.class).when(vboxMock).findMachine(vmId);

        //there is expected to be thrown UnknownVirtualMachineException exception as a result
        //of unsuccessful virtual machine getting
        exception.expect(UnknownVirtualMachineException.class);
        sut.getVirtualMachine(pm, vmId);
    }

    /**
     * This test tests that there is invoked ConnectionFailureException
     * exception if there uccurs any network connection problem while the method
     * NativeVBoxAPIManager::getVirtualMachineById() is processed (particularly
     * when the method VirtualBoxManager::connect() is called).
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void getVirtualMachineByIdWithSuddenNetworkConnectionLoss() throws Exception {
        //represents host machine from which is virtual machine required
        PhysicalMachine pm = new PMBuilder().build();
        //represents ID of required virtual machine
        String vmId = "793d084a-0189-4a55-a9b7-531c455570a1";
        //represents a url used for connecting physical machine pm
        String url = "http://" + pm.getAddressIP() + ":" + pm.getPortOfVTWebServer();

       //there should be thrown VBoxException exception when there is made an attempt to get
        //VirtualBoxManager object which means that there any connection error occured
        doThrow(VBoxException.class).when(vbmMock).connect(url, pm.getUsername(), pm.getUserPassword());

        exception.expect(ConnectionFailureException.class);
        sut.getVirtualMachine(pm, vmId);
    }

    /**
     * This test tests that there is returned the required virtual machine
     * without any exception and error occurance when the method
     * NativeVBoxAPIManager::getVirtualMachine() is called with name which
     * actually identify some virtual machine from the list of registered
     * virtual machines.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void getVirtualMachineByNameWithSomeMatch() throws Exception {
        //represents host machine from which is virtual machine required
        PhysicalMachine pm = new PMBuilder().build();
        //represents virtual machine with all correct and expected values of its attributes 
        VirtualMachine expectedVM = new VMBuilder().build();
        //represents name of required virtual machine
        String vmName = expectedVM.getName();
        //represents mock object of type IMachine for better test control
        IMachine machineMocked = mock(IMachine.class);
        //represents mock object of type IGuestOSType for better test control
        IGuestOSType guestOSTypeMocked = mock(IGuestOSType.class);
        //represents mock object of type IMedium for better test control
        IMedium mediumMocked = mock(IMedium.class);
        //represents mock object of type IMediumAttachment for easier and better test control 
        IMediumAttachment medAttachMock = mock(IMediumAttachment.class);
        //represents list of SATA medium controllers
        List<IMediumAttachment> medAttachs = Arrays.asList(medAttachMock);

        //mock object of type IVirtualBox is returned when the method VirtualBoxManager::getVBox()
        //is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //mock object of type IMachine is returned when the method IVirtualBox::findMachine() is called
        //with name of required virtual machine in order to control returned values of its methods
        when(vboxMock.findMachine(vmName)).thenReturn(machineMocked);
        //mock object of type IGuestOSType is returned when the method IVirtualBox::getGuestOSType()
        //is called with OS type vmId of required virtual machine in order to control returned values of its methods
        when(vboxMock.getGuestOSType(machineMocked.getOSTypeId())).thenReturn(guestOSTypeMocked);
        //string with value = "Linux" should be returned when the method IGuestOSType::getFamilyId() is called
        when(guestOSTypeMocked.getFamilyId()).thenReturn("Linux");
        //string with value = "Fedora_64" should be returned when the method IGuestOSType::getId() is called
        when(guestOSTypeMocked.getId()).thenReturn("Fedora_64");
        //list medAttachs is returned when the method IMachine::getMediumAttachmentOfController() is called for SATA type
        when(machineMocked.getMediumAttachmentsOfController("SATA")).thenReturn(medAttachs);
        //port number = 0 should be returned when the method IMediumAttachment::getPort() is called
        when(medAttachMock.getPort()).thenReturn(0);
        //device number = 0 should be returned when the method IMediumAttachment::getDevice() is called
        when(medAttachMock.getDevice()).thenReturn(0);
        //mock object of type IMedium is returned when the method IMachine::getMedium() is called
        //with particular arguments in order to control returned values of its methods
        when(machineMocked.getMedium("SATA", 0, 0)).thenReturn(mediumMocked);
        //string with value = "VirtualMachine_01" should be returned when the method IMachine::getName() is called
        when(machineMocked.getName()).thenReturn(expectedVM.getName());
        //string with value = "793d084a-0189-4a55-a9b7-531c455570a1" should be returned when the method IMachine::getId() is called
        when(machineMocked.getId()).thenReturn(expectedVM.getId().toString());
        //there should be returned Long value = 1 when the method IMachine::getCPUCount() is called
        when(machineMocked.getCPUCount()).thenReturn(expectedVM.getCountOfCPU());
        //there should be returned Long value = 1 when the method IMachine::getMonitorCount() is called
        when(machineMocked.getMonitorCount()).thenReturn(expectedVM.getCountOfMonitors());
        //there should be returned Long value = 100 when the method IMachine::getCPUExecutionCap() is called
        when(machineMocked.getCPUExecutionCap()).thenReturn(expectedVM.getCPUExecutionCap());
        //there should be returned Long value = 4096 when the method IMachine::getMemorySize() is called
        when(machineMocked.getMemorySize()).thenReturn(expectedVM.getSizeOfRAM());
        //there should be returned Long value = 12 when the method IMachine::getVRAMSize() is called
        when(machineMocked.getVRAMSize()).thenReturn(expectedVM.getSizeOfVRAM());
        //there should be returned Long value = 21474836480 when the method IMedium::getLogicalSize() is called
        when(mediumMocked.getLogicalSize()).thenReturn(expectedVM.getHardDiskTotalSize());
        //there should be returned Long value = 7187988480 when the method IMedium::getSize() is called
        when(mediumMocked.getSize()).thenReturn(expectedVM.getHardDiskTotalSize() - expectedVM.getHardDiskFreeSpaceSize());

        //there should not be invoked any exception nor any error should not appear
        VirtualMachine actualVM = sut.getVirtualMachine(pm, vmName);

        //all attributes of VirtualMachine objects will be compared and should be same
        assertDeepVMsEquals(expectedVM, actualVM);
    }

    /**
     * This test tests that there should be invoked
     * UnknownVirtualMachineException exception when there is not any registered
     * virtual machine with the used name.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void getVirtualMachineByNameWithNoMatch() throws Exception {
        //represents host machine from which is virtual machine required
        PhysicalMachine pm = new PMBuilder().build();
        //represents name of required virtual machine
        String vmName = "VirtualMachine_01";

        //mock object of type IVirtualBox is returned when the method VirtualBoxManager::getVBox()
        //is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there should be thrown VBoxException exception when the method IVirtualBox::findMachine() is called
        //with uuid of required virtual machine which means there is not any registered virtual machine
        //with vmId equal to used uuid
        doThrow(VBoxException.class).when(vboxMock).findMachine(vmName);

        //there is expected to be thrown UnknownVirtualMachineException exception as a result
        //of unsuccessful virtual machine getting
        exception.expect(UnknownVirtualMachineException.class);
        sut.getVirtualMachine(pm, vmName);
    }

    /**
     * This test tests that there is invoked ConnectionFailureException
     * exception if there uccurs any network connection problem while the method
     * NativeVBoxAPIManager::getVirtualMachine() is processed (particularly when
     * the method VirtualBoxManager::connect() is called).
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void getVirtualMachineByNameWithSuddenNetworkConnectionLoss() throws Exception {
        //represents host machine from which is virtual machine required
        PhysicalMachine pm = new PMBuilder().build();
        //represents name of required virtual machine
        String vmName = "VirtualMachine_01";
        //represents a url used for connecting physical machine pm
        String url = "http://" + pm.getAddressIP() + ":" + pm.getPortOfVTWebServer();

       //there should be thrown VBoxException exception when there is made an attempt to get
        //VirtualBoxManager object which means that there any connection error occured
        doThrow(VBoxException.class).when(vbmMock).connect(url, pm.getUsername(), pm.getUserPassword());

        exception.expect(ConnectionFailureException.class);
        sut.getVirtualMachine(pm, vmName);
    }

    /**
     * This test tests that if there is a non-empty list of registered virtual
     * machines on VirtualBox on a particular physical machine then this list is
     * returned as a result when the method is called with physical machine
     * where this non=empty list of virtual machines is located and no
     * exceptions nor errors should appear.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void getVirtualMachinesWithReturnedNonemptyVMsList() throws Exception {
        //represents host machine from which are virtual machines required
        PhysicalMachine pm = new PMBuilder().build();
        //represents first of two registered virtual machines which are on physical machine pm
        VirtualMachine vm1 = new VMBuilder().build();
        //represents second of two registered virtual machines which are on physical machine pm
        VirtualMachine vm2 = new VMBuilder().id(UUID.fromString("897d084a-0189-847a-bb12-531c455570a1"))
                .name("VirtualMachine_02").hardDiskFreeSpaceSize(10021544L)
                .hardDiskTotalSize(180001544L).sizeOfRAM(2048L)
                .typeOfOS("MS-Windows").identifierOfOS("Win10_64").build();
        //represents list of virtual machines which is expected as a result of NativeVBoxAPIManager::getAllVirtualMachines() method call
        List<VirtualMachine> expList = Arrays.asList(vm1, vm2);
        //represents mock object of type IMachine for better test control
        IMachine machineMocked1 = mock(IMachine.class);
        //represents mock object of type IMachine for better test control
        IMachine machineMocked2 = mock(IMachine.class);
        //represents mock object of type IGuestOSType for better test control
        IGuestOSType guestOSTypeMocked = mock(IGuestOSType.class);
        //represents mock object of type IMedium for better test control
        IMedium mediumMocked1 = mock(IMedium.class);
        //represents mock object of type IMedium for better test control
        IMedium mediumMocked2 = mock(IMedium.class);
        //represents list of vbox machines which is expected as a result of IVirtualBox::getMachines() method call
        List<IMachine> vboxMachines = Arrays.asList(machineMocked1, machineMocked2);
        //represents mock object of type IMediumAttachment for easier and better test control 
        IMediumAttachment medAttachMock1 = mock(IMediumAttachment.class);
        //represents list of SATA medium controllers
        List<IMediumAttachment> medAttachs1 = Arrays.asList(medAttachMock1);
        //represents mock object of type IMediumAttachment for easier and better test control 
        IMediumAttachment medAttachMock2 = mock(IMediumAttachment.class);
        //represents list of SATA medium controllers
        List<IMediumAttachment> medAttachs2 = Arrays.asList(medAttachMock2);

        //mock object of type IVirtualBox is returned when the method VirtualBoxManager::getVBox()
        //is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there is returned the list of mock objects of type IMachine in order to
        //control returned values of their methods
        when(vboxMock.getMachines()).thenReturn(vboxMachines);
        //mock object of type IGuestOSType is returned when the method IVirtualBox::getGuestOSType() is called
        //with OS type vmId of required virtual machine in order to control returned values of its methods
        when(vboxMock.getGuestOSType(machineMocked1.getOSTypeId())).thenReturn(guestOSTypeMocked);
        //mock object of type IGuestOSType is returned when the method IVirtualBox::getGuestOSType() is called
        //with OS type vmId of required virtual machine in order to control returned values of its methods
        when(vboxMock.getGuestOSType(machineMocked2.getOSTypeId())).thenReturn(guestOSTypeMocked);
        //for first virtual machine there should be returned string with value = "Linux" and for the second
        //one string with value = "MS-Windows" when the method IGuestOSType::getFamily() is called
        when(guestOSTypeMocked.getFamilyId()).thenReturn("Linux").thenReturn("MS-Windows");
        //for first virtual machine there should be returned string with value = "Fedora_64" and for second
        //one string with value = "Win10_64" when the method IGuestOSType::getId() is called
        when(guestOSTypeMocked.getId()).thenReturn("Fedora_64").thenReturn("Win10_64");
        //mock object of type IMedium is returned when the method IMachine::getMedium() is called with
        //particular arguments in order to control returned values of its methods
        when(machineMocked1.getMedium("SATA", 0, 0)).thenReturn(mediumMocked1);
        //mock object of type IMedium is returned when the method IMachine::getMedium() is called with
        //particular arguments in order to control returned values of its methods
        when(machineMocked2.getMedium("SATA", 0, 0)).thenReturn(mediumMocked2);
        //there should be returned string with value = "VirtualMachine_01" when the method IMachine::getName() is called
        when(machineMocked1.getName()).thenReturn(vm1.getName());
        //there should be returned string with value = "VirtualMachine_02" when the method IMachine::getName() is called
        when(machineMocked2.getName()).thenReturn(vm2.getName());
        //there should be returned string with value = "793d084a-0189-4a55-a9b7-531c455570a1"when the 
        //method IMachine::getId() is called
        when(machineMocked1.getId()).thenReturn(vm1.getId().toString());
        //there should be returned string with value = "397d084a-0189-847a-bb12-531c455570a1" when the
        //method IMachine::getId() is called
        when(machineMocked2.getId()).thenReturn(vm2.getId().toString());
        //there should be returned Long value = 1 when the method IMachine::getCPUCount() is called
        when(machineMocked1.getCPUCount()).thenReturn(vm1.getCountOfCPU());
        //there should be returned Long value = 1 when the method IMachine::getCPUCount() is called
        when(machineMocked2.getCPUCount()).thenReturn(vm2.getCountOfCPU());
        //there should be returned Long value = 1 the method IMachine::getMonitorCount() is called
        when(machineMocked1.getMonitorCount()).thenReturn(vm1.getCountOfMonitors());
        //there should be returned Long value = 1 the method IMachine::getMonitorCount() is called
        when(machineMocked2.getMonitorCount()).thenReturn(vm2.getCountOfMonitors());
        //there should be returned Long value = 100 when the method IMachine::getCPUExecutionCap() is called
        when(machineMocked1.getCPUExecutionCap()).thenReturn(vm1.getCPUExecutionCap());
        //there should be returned Long value = 100 when the method IMachine::getCPUExecutionCap() is called
        when(machineMocked2.getCPUExecutionCap()).thenReturn(vm2.getCPUExecutionCap());
        //there should be returned Long value = 4096 when the method IMachine::getMemorySize() is called
        when(machineMocked1.getMemorySize()).thenReturn(vm1.getSizeOfRAM());
        //there should be returned Long value = 2048 when the method IMachine::getMemorySize() is called
        when(machineMocked2.getMemorySize()).thenReturn(vm2.getSizeOfRAM());
        //there should be returned Long value = 12 when the method IMachine::getVRAMSize() is called
        when(machineMocked1.getVRAMSize()).thenReturn(vm1.getSizeOfVRAM());
        //there should be returned Long value = 12 when the method IMachine::getVRAMSize() is called
        when(machineMocked2.getVRAMSize()).thenReturn(vm2.getSizeOfVRAM());
        //there should be returned Long value = 21474836480 when the method IMedium::getLogicalSize() is called
        when(mediumMocked1.getLogicalSize()).thenReturn(vm1.getHardDiskTotalSize());
        //there should be returned Long value = 180001544 when the method IMedium::getLogicalSize() is called
        when(mediumMocked2.getLogicalSize()).thenReturn(vm2.getHardDiskTotalSize());                
        //there should be returned Long value = 7187988480 when the method IMedium::getSize() is called
        when(mediumMocked1.getSize()).thenReturn(vm1.getHardDiskTotalSize() - vm1.getHardDiskFreeSpaceSize());
        //there should be returned Long value = 169980000 when the method IMedium::getSize() is called
        when(mediumMocked2.getSize()).thenReturn(vm2.getHardDiskTotalSize() - vm2.getHardDiskFreeSpaceSize());
        //list medAttachs is returned when the method IMachine::getMediumAttachmentOfController() is called for SATA type
        when(machineMocked1.getMediumAttachmentsOfController("SATA")).thenReturn(medAttachs1);
        //port number = 0 should be returned when the method IMediumAttachment::getPort() is called
        when(medAttachMock1.getPort()).thenReturn(0);
        //device number = 0 should be returned when the method IMediumAttachment::getDevice() is called
        when(medAttachMock1.getDevice()).thenReturn(0);
        //list medAttachs is returned when the method IMachine::getMediumAttachmentOfController() is called for SATA type
        when(machineMocked2.getMediumAttachmentsOfController("SATA")).thenReturn(medAttachs2);
        //port number = 0 should be returned when the method IMediumAttachment::getPort() is called
        when(medAttachMock2.getPort()).thenReturn(0);
        //device number = 0 should be returned when the method IMediumAttachment::getDevice() is called
        when(medAttachMock2.getDevice()).thenReturn(0);
        //when the method NativeVBoxAPIManager::getAllVirtualMachines() is called then there should
        //be returned a list with 2 virtual machines
        List<VirtualMachine> actList = sut.getAllVirtualMachines(pm);

        //both lists are sorted in order to establish the same conditions for comparation
        Collections.sort(expList, vmComparator);
        Collections.sort(actList, vmComparator);

        //each virtual machine from list expList is compared against the virtual machine from
        //list actList (all their attributes)
        assertDeepVMsEquals(expList, actList);
    }

    /**
     * This test tests that if there is not any virtual machine on VirtualBox on
     * a particular physical machine then there should be returned an empty list
     * of virtual machines without any exception or error occurance.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void getVirtualMachinesWithReturnedEmptyVMsList() throws Exception {
        //represents host machine from which are virtual machines required
        PhysicalMachine pm = new PMBuilder().build();

       //mock object of type IVirtualBox is returned when the method VirtualBoxManager::getVBox()
        //is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
       //there is returned an empty list of objects of type IMachine in order to
        //which means there is no virtual machine on VirtualBox on particular physical machine
        when(vboxMock.getMachines()).thenReturn(new ArrayList<>());

        //there should be returned an empty list of object of type VirtualMachine
        List<VirtualMachine> vmsList = sut.getAllVirtualMachines(pm);

        assertTrue("List of virtual machines should be empty", vmsList.isEmpty());
    }

    /**
     * This test tests that there is invoked ConnectionFailureException
     * exception if there uccurs any network connection problem while the method
     * NativeVBoxAPIManager::getAllVirtualMachines() is processed (particularly
     * when the method VirtualBoxManager::connect() is called).
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void getVirtualMachinesWithSuddenNetworkConnectionLoss() throws Exception {
        //represents host machine from which are virtual machines required
        PhysicalMachine pm = new PMBuilder().build();
        //represents a url used for connecting physical machine pm
        String url = "http://" + pm.getAddressIP() + ":" + pm.getPortOfVTWebServer();

       //there should be thrown VBoxException exception when there is made an attempt to get
        //VirtualBoxManager object which means that there any connection error occured
        doThrow(VBoxException.class).when(vbmMock).connect(url, pm.getUsername(), pm.getUserPassword());

        exception.expect(ConnectionFailureException.class);
        sut.getAllVirtualMachines(pm);
    }

    /**
     * This test tests that a virtual machine which is like a standalone unit
     * (it means that virtual machine is not linked clone) is removed without
     * any exception or error occurance.
     */
    /*@Test
     public void removeStandaloneExistentVirtualMachineWithoutChildren(){
     //represents virtual machine which should be removed
     VirtualMachine vm = new VMBuilder().build();
     //represents mock object of type IVirtualBox for better test control
     IVirtualBox vboxMocked = mock(IVirtualBox.class);
     //represents mock object of type IMachine for better test control
     IMachine machineMocked = mock(IMachine.class);
     //represents mock object of type IMedium for better test control
     IMedium mediumMocked = mock(IMedium.class);
     //represents mock object of type List<String> for better test control
     List<String> machIdsListMocked = mock(List.class);
        
     //mock object of type IVirtualBox is returned when the method VirtualBoxManager::getVBox()
     //is called in order to control returned values of its methods
     when(vbmMocked.getVBox()).thenReturn(vboxMocked);
     //mock object of type IMachine is returned when the method IVirtualBox::findMachine() is called
     //with ID of virtual machine which should be removed in order to control returned values of its methods
     when(vboxMocked.findMachine(vm.getId().toString())).thenReturn(machineMocked);
     //there should be returned string with value = "793d084a-0189-4a55-a9b7-531c455570a1" when the method
     //IMachine::getId() is called
     when(machineMocked.getId()).thenReturn(vm.getId().toString());
     //mock object of type IMedium is returned when the method IMachine::getMedium() is called
     //with a particular arguments in order to control returned values of its methods
     when(machineMocked.getMedium("SATA", 0, 0)).thenReturn(mediumMocked);
     //there should be returned mock object of type List<String> when the method IMedium::getMachineIds()
     //is called in order to control the content of the list (returned values)
     when(mediumMocked.getMachineIds()).thenReturn(machIdsListMocked);
     //there should be returned string with value = "793d084a-0189-4a55-a9b7-531c455570a1" when the method
     //List<String>::get() is called
     when(machIdsListMocked.get(0)).thenReturn(vm.getId().toString());
     //there is returned null object of type IMedium as a parent medium when the method
     //IMedium::getParent() is called which means that there is no children (linked clones) which
     //should be removed as first
     when(mediumMocked.getParent()).thenReturn(null);        
     //there is returned an empty list of children mediums when the method IMedium::getChildren()
     //is called for object mediumMocked
     when(mediumMocked.getChildren()).thenReturn(new ArrayList<>());
        
     //virtual machine should be removed without any exception or error occurance
     sut.removeVirtualMachine(vm);
     }*/
    /**
     *
     */
    /*@Test
     public void removeStandaloneExistentVirtualMachineWithChildren(){
     //represents virtual machine which should be removed
     VirtualMachine vm = new VMBuilder().build();
     //represents mock object of type IVirtualBox for better test control
     IVirtualBox vboxMocked = mock(IVirtualBox.class);
     //represents mock object of type IMachine for better test control
     IMachine machineMocked = mock(IMachine.class);
     IMachine iterMachineMocked = mock(IMachine.class);
     //represents mock object of type IMedium for better test control
     IMedium mediumMocked = mock(IMedium.class);
     //represents mock object of type IMedium for better test control (parent medium)
     IMedium parentMediumMocked = mock(IMedium.class);
     List<String> machIDsListMocked = mock(List.class);
     List<IMedium> childrenListMocked = mock(List.class);
        
     //mock object of type IVirtualBox is returned when the method VirtualBoxManager::getVBox()
     //is called in order to control returned values of its methods
     when(vbmMocked.getVBox()).thenReturn(vboxMocked);
     //mock object of type IMachine is returned when the method IVirtualBox::findMachine() is called
     //with ID of virtual machine which should be removed in order to control returned values of its methods
     when(vboxMocked.findMachine(vm.getId().toString())).thenReturn(machineMocked);
     when(vboxMocked.findMachine(anyString())).thenReturn(iterMachineMocked);
     when(iterMachineMocked.getId()).thenReturn("8c42220c-dbe1-45fe-90c5-e2babcce8d5b")
     .thenReturn("8c42220c-dbe1-45fe-90c5-e2babcce8d5b")
     .thenReturn("67b5a5a0-9837-4521-a8f5-7d024ca9935a");
     when(machineMocked.getId()).thenReturn(vm.getId().toString());
     //mock object of type IMedium is returned when the method IMachine::getMedium() is called
     //with a particular arguments in order to control returned values of its methods
     when(machineMocked.getMedium("SATA", 0, 0)).thenReturn(mediumMocked);
     when(mediumMocked.getBase()).thenReturn(mediumMocked);
     when(mediumMocked.getMachineIds()).thenReturn(machIDsListMocked);
     //there is returned null object of type IMedium as a parent medium when the method
     //IMedium::getParent() is called which means that there is no children (linked clones) which
     //should be removed as first        
     when(mediumMocked.getParent()).thenReturn(parentMediumMocked)
     .thenReturn(parentMediumMocked)
     .thenReturn(null);
     when(parentMediumMocked.getMachineIds()).thenReturn(machIDsListMocked);
     when(machIDsListMocked.get(0)).thenReturn(vm.getId().toString())
     .thenReturn(vm.getId().toString())
     .thenReturn(vm.getId().toString())
     .thenReturn(vm.getId().toString())
     .thenReturn(vm.getId().toString())
     .thenReturn(vm.getId().toString())
     .thenReturn(vm.getId().toString())
     .thenReturn("8c42220c-dbe1-45fe-90c5-e2babcce8d5b")//LCopy2
     .thenReturn("8c42220c-dbe1-45fe-90c5-e2babcce8d5b")//LCopy2
     .thenReturn("67b5a5a0-9837-4521-a8f5-7d024ca9935a")//LCopy1
     .thenReturn("67b5a5a0-9837-4521-a8f5-7d024ca9935a")//LCopy1
     .thenReturn(vm.getId().toString());
     //there is returned an empty list of children mediums when the method IMedium::getChildren()
     //is called for object mediumMocked
     when(mediumMocked.getChildren()).thenReturn(childrenListMocked);
     when(childrenListMocked.isEmpty()).thenReturn(false)
     .thenReturn(false)
     .thenReturn(true);
        
     //virtual machine should be removed without any exception or error occurance
     sut.removeVirtualMachine(vm);
     }*/
    /**
     *
     */
    /*@Test
     public void removeVirtualMachineAsLinkedCloneWithoutChildren(){
        
     }
    
     @Test
     public void removeVirtualMachineAsLinkedCloneWithChildren(){
        
     }*/
    /**
     * This test tests that a virtual machine which is not in state "PoweredOff"
     * cannot be removed.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void removeVirtualMachineWithInvalidState() throws Exception {
        //represents a virtual machine which is shhould be removed
        VirtualMachine vm = new VMBuilder().build();
        //represents mock object of type IMachine for easier and better test control
        IMachine machineMocked = mock(IMachine.class);

       //there should be returned mock object of type IVirtualBox when the method VirtualBoxManager::getVBox()
        //is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
       //there should be returned mock object of type IMachine when the method IVirtualBox::findMachine() is called
        //with ID of a virtual machine which should be removed in order to control returned values of its methods
        when(vboxMock.findMachine(vm.getId().toString())).thenReturn(machineMocked);
        //there should be returned a positive answer if there is a query for the virtual machine accessibility
        when(machineMocked.getAccessible()).thenReturn(true);
       //if there appears a query for the virtual machine state there should be answered that
        //the virtual machine is running (working)
        when(machineMocked.getState()).thenReturn(MachineState.Running);

       //there should be invoked UnexpectedVMStateException exception because
        //the virtual machine is not powered off, but running
        exception.expect(UnexpectedVMStateException.class);
        sut.removeVirtualMachine(vm);
    }

    /**
     * This test tests that there cannot be removed a virtual machine which is
     * not present in a list of registered virtual machines on a particular
     * physical machine.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void removeNonexistentVirtualMachine() throws Exception {
        //represents a virtual machine which should be removed
        VirtualMachine vm = new VMBuilder().build();

        //there should be returned mock object of type IVirtualBox when the method VirtualBoxManager::getVBox()
        //is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there should be thrown VBoxException exception when the method IVirtualBox::findMachine() is called
        //which means that the required virtual machine is not registered and therefore cannot be removed
        doThrow(VBoxException.class).when(vboxMock).findMachine(vm.getId().toString());

        exception.expect(UnknownVirtualMachineException.class);
        sut.removeVirtualMachine(vm);
    }

    /**
     * This test tests that a virtual machine which is in a list of registered
     * virtual machines, but is not accessible (damaged files, removed files
     * etc.) is just unregistered (deleted from the list of registered virtual
     * machines, but its source files are not manipulated).
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void removeInaccessibleVirtualMachine() throws Exception {
        //represents a virtual machine which should be removed
        VirtualMachine vm = new VMBuilder().build();
        IMachine machineMocked = mock(IMachine.class);

        //there should be returned mock object of type IVirtualBox when the method VirtualBoxManager::getVBox()
        //is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there should be returned mock object of type IMachine when the method IVirtualBox::findMachine() is called
        //with ID of a virtual machine which should be removed in order to control returned values of its methods
        when(vboxMock.findMachine(vm.getId().toString())).thenReturn(machineMocked);
        //if there appears a query for the virtual machine accessibility there is returned a negative answer
        when(machineMocked.getAccessible()).thenReturn(false);

        sut.removeVirtualMachine(vm);

        //checks whether the virtual machine was just unregistered as expected or if there was made even more
        //deletion which should not be performed
        verify(machineMocked).unregister(CleanupMode.DetachAllReturnHardDisksOnly);
        verify(machineMocked, never()).deleteConfig(any(List.class));
    }

    /**
     * This test tests that there should be invoked a ConnectionFailureException
     * exception if there appears any network connection problem while the
     * method NativeVBoxAPIManager::removeVirtualMachine() is being processed.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void removeVirtualMachineWithSuddenNetworkConnectionLoss() throws Exception {
        //represents a virtual machine which should be removed
        VirtualMachine vm = new VMBuilder().build();
        String url = "http://" + vm.getHostMachine().getAddressIP()
                + ":" + vm.getHostMachine().getPortOfVTWebServer();
        String username = vm.getHostMachine().getUsername();
        String userPassword = vm.getHostMachine().getUserPassword();

        //there should be thrown VBoxExcepton exception when the method VirtualBoxManager::connect()
        //is called with a required physical machine which signals a network connection problem
        doThrow(VBoxException.class).when(vbmMock).connect(url, username, userPassword);

        exception.expect(ConnectionFailureException.class);
        sut.removeVirtualMachine(vm);
    }

    /**
     * This test tests that there is returned correctly named virtual machine
     * clone when there should be created a full clone of a particular virtual
     * machine and no exception nor error should appear.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void createVirtualMachineFullCloneIdealCase() throws Exception {
        PrintStream origOutStream = OutputHandler.getOutputStream();
        OutputHandler.setOutputStream(System.out);
        //represents a virtual machine which should be cloned
        VirtualMachine vm = new VMBuilder().build();
        //represents newly created virtual machine clone
        VirtualMachine expClone = new VMBuilder().id(UUID.fromString("399d0aea-01aa-4a55-a9b7-5cd345570a1"))
                .name(vm.getName() + "_FullClone1").build();
        //represents mock object of type IMachine for easier and better test control (vboxMachine)
        IMachine vboxMachineMock = mock(IMachine.class);
        //represents mock object of type IMachine for easier and better test control (clonableMachine)
        IMachine clonableMachineMock = mock(IMachine.class);
        //represents mock object of type IMachine for easier and better test control (vboxMachineClone)
        IMachine machineCloneMocked = mock(IMachine.class);
        //represents mock object of type IMedium for easier and better test control
        IGuestOSType guestOSTypeMocked = mock(IGuestOSType.class);
        //represents mock object of type IMedium for easier and better test control
        IMedium cloneMediumMock = mock(IMedium.class);
        //represents mock object of type IProgress for easier and better test control
        IProgress progressMock = mock(IProgress.class);
        //represents mock object of type IMediumAttachment for easier and better test control
        IMediumAttachment medAttachMock = mock(IMediumAttachment.class);
        //list of SATA controllers
        List<IMediumAttachment> medAttachs = Arrays.asList(medAttachMock);
        //represents mock object of type IMedium for easier and better test control
        IMedium vboxMachMediumMock = mock(IMedium.class);
        //represents mock object of type ISystemProperties for easier and  better test control
        ISystemProperties sysPropMock = mock(ISystemProperties.class);
        File cloneFolderMock = mock(File.class);
        PowerMockito.whenNew(File.class).withAnyArguments().thenReturn(cloneFolderMock);

       //there should be returned mock object of type IVirtualBox when the method VirtualBoxManager::getVBox()
        //is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
       //there should be returned mock object of type IMachine when the method IVirtualBox::findMachine()
        //is called in order to control returned values of its methods
        doReturn(vboxMachineMock).doReturn(clonableMachineMock).when(vboxMock).findMachine(vm.getId().toString());
        //there should be returned a positive answer to a query for a virtual machine accessibility
        when(vboxMachineMock.getAccessible()).thenReturn(true);
        //there should be returned machine state with value = "PoweredOff" (also could be "Running", "Saved", "Paused")
        when(vboxMachineMock.getState()).thenReturn(MachineState.PoweredOff);
        when(vboxMachineMock.getMediumAttachmentsOfController("SATA")).thenReturn(medAttachs);
        when(medAttachMock.getPort()).thenReturn(0);
        when(medAttachMock.getDevice()).thenReturn(0);
        when(vboxMachineMock.getMedium("SATA", 0, 0)).thenReturn(vboxMachMediumMock);
        when(vboxMachMediumMock.getState()).thenReturn(MediumState.Created);
        //there should be returned string with value = "VirtualMachine_01"
        when(vboxMachineMock.getName()).thenReturn(vm.getName());
        when(vboxMachineMock.getOSTypeId()).thenReturn("Fedora_64");
        when(vboxMock.getSystemProperties()).thenReturn(sysPropMock);
        when(sysPropMock.getDefaultMachineFolder()).thenReturn("VirtualBox VMs");
        doThrow(VBoxException.class).when(vboxMock).findMachine(expClone.getName());
        when(cloneFolderMock.isDirectory()).thenReturn(false);        
        when(vboxMachineMock.getId()).thenReturn(vm.getId().toString());
        when(clonableMachineMock.getOSTypeId()).thenReturn("Fedora_64");
       //there should be returned mock object of type IMachine when the method IVirtualBox::createMachine()
        //is called in order to control returned values of its methods
        when(vboxMock.createMachine(null, expClone.getName(), null, "Fedora_64", null)).thenReturn(machineCloneMocked);
        when(clonableMachineMock.cloneTo(machineCloneMocked, CloneMode.AllStates, new ArrayList<>())).thenReturn(progressMock);
        when(progressMock.getCompleted()).thenReturn(false, false, false, false, true);
        when(progressMock.getPercent()).thenReturn(0L, 20L, 50L, 99L);
        when(progressMock.getResultCode()).thenReturn(0);
        //there should be returned string with value = "Fedora_64"
        when(vboxMachineMock.getOSTypeId()).thenReturn("Fedora_64");
        //there should be returned string with value = "Fedora_64"
        when(machineCloneMocked.getOSTypeId()).thenReturn("Fedora_64");
        //there should be returned mock object of type IGuestOSType when the method IVirtualBox::getGuestOSType()
        //is called in order to control returned values of its methods
        when(vboxMock.getGuestOSType("Fedora_64")).thenReturn(guestOSTypeMocked);
        when(machineCloneMocked.getMediumAttachmentsOfController("SATA")).thenReturn(medAttachs);
        when(medAttachMock.getPort()).thenReturn(0);
        when(medAttachMock.getDevice()).thenReturn(0);
        //there should be returned mock object of type IMedium when the method IMachine::getMedium() is called
        //in order to control returned values of its methods
        when(machineCloneMocked.getMedium("SATA", 0, 0)).thenReturn(cloneMediumMock);
        //there should be returned string with value = "399d0aea-01aa-4a55-a9b7-5cd345570a1"
        when(machineCloneMocked.getId()).thenReturn(expClone.getId().toString());
        //there should be returned string with value = "VirtualMachine_01_FCopy1"
        when(machineCloneMocked.getName()).thenReturn(expClone.getName());
        //there should be returned Long value = 1
        when(machineCloneMocked.getCPUCount()).thenReturn(expClone.getCountOfCPU());
        //there should be returned Long value = 1
        when(machineCloneMocked.getMonitorCount()).thenReturn(expClone.getCountOfMonitors());
        //there should be returned Long value = 100
        when(machineCloneMocked.getCPUExecutionCap()).thenReturn(expClone.getCPUExecutionCap());
        //there should be returned Long value = 4096
        when(machineCloneMocked.getMemorySize()).thenReturn(expClone.getSizeOfRAM());
        //there should be returned Long value = 12
        when(machineCloneMocked.getVRAMSize()).thenReturn(expClone.getSizeOfVRAM());
        //there should be returned Long value = 21474836480
        when(cloneMediumMock.getLogicalSize()).thenReturn(expClone.getHardDiskTotalSize());
        //there should be returned Long value = 7187988480
        when(cloneMediumMock.getSize()).thenReturn(expClone.getHardDiskTotalSize() - expClone.getHardDiskFreeSpaceSize());
        //there should be returned string with value = "Linux"
        when(guestOSTypeMocked.getFamilyId()).thenReturn(expClone.getTypeOfOS());
        //there should be returned string with value = "Fedora_64"
        when(guestOSTypeMocked.getId()).thenReturn(expClone.getIdentifierOfOS());

        //there should not appear any exception nor error
        VirtualMachine actClone = sut.createVMClone(vm, CloneType.FULL_FROM_ALL_STATES);

        assertDeepVMsEquals(expClone, actClone);
        OutputHandler.setOutputStream(origOutStream);
    }

    /**
     * This test tests that there is returned correctly named virtual machine
     * clone when there should be created a linked clone of a particular virtual
     * machine and no exception nor error should appear.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void createVirtualMachineLinkedCloneIdealCase() throws Exception {
        PrintStream origOutStream = OutputHandler.getOutputStream();
        OutputHandler.setOutputStream(System.out);
        //represents a virtual machine which should be cloned
        VirtualMachine vm = new VMBuilder().build();
        //represents newly created virtual machine clone
        VirtualMachine expClone = new VMBuilder().id(UUID.fromString("399d0aea-01aa-4a55-a9b7-5cd345570a1"))
                .name(vm.getName() + "_LinkClone1").build();
        //represents mock object of type IMachine for easier and better test control (vboxMachine)
        IMachine vboxMachineMock = mock(IMachine.class);
        //represents mock object of type IMachine for easier and better test control (clonableMachine)
        IMachine clonableMachineMock = mock(IMachine.class);
        //represents mock object of type IMachine for easier and better test control (vboxMachineClone)
        IMachine vboxMachineCloneMock = mock(IMachine.class);
        //represents mock object of type IMedium for easier and better test control
        IGuestOSType guestOSTypeMocked = mock(IGuestOSType.class);
        //represents mock object of type IMedium for easier and better test control
        IMedium cloneMediumMock = mock(IMedium.class);
        //represents mock object of type ISession for easier and better test control
        ISession sessionMock = mock(ISession.class);
        //represents mock object of type IConsole for easier and better test control
        IConsole consoleMock = mock(IConsole.class);
        //represents mock object of type IProgress for easier and better test control
        IProgress progressMock1 = mock(IProgress.class);
        //represents mock object of type IProgress for easier and better test control
        IProgress progressMock2 = mock(IProgress.class);
        //represents mock object of type ISnapshot for easier and better test control
        ISnapshot snapshotMock = mock(ISnapshot.class);
        //represents mock object of type IMediumAttachment for easier and better test control
        IMediumAttachment medAttachMock = mock(IMediumAttachment.class);
        //list of SATA controllers
        List<IMediumAttachment> medAttachs = Arrays.asList(medAttachMock);
        //represents mock object of type IMedium for easier and better test control
        IMedium vboxMachMediumMock = mock(IMedium.class);
        //represents mock object of type ISystemProperties for easier and  better test control
        ISystemProperties sysPropMock = mock(ISystemProperties.class);
        File cloneFolderMock = mock(File.class);
        PowerMockito.whenNew(File.class).withAnyArguments().thenReturn(cloneFolderMock);

       //there should be returned mock object of type IVirtualBox when the method VirtualBoxManager::getVBox()
        //is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
       //there should be returned mock object of type IMachine when the method IVirtualBox::findMachine()
        //is called in order to control returned values of its methods
        doReturn(vboxMachineMock).when(vboxMock).findMachine(vm.getId().toString());
        //there should be returned a positive answer to a query for a virtual machine accessibility
        when(vboxMachineMock.getAccessible()).thenReturn(true);
        //there should be returned machine state with value = "PoweredOff" (also could "Running", "Saved", "Paused")
        when(vboxMachineMock.getState()).thenReturn(MachineState.PoweredOff);
        when(vboxMachineMock.getMediumAttachmentsOfController("SATA")).thenReturn(medAttachs);
        when(medAttachMock.getPort()).thenReturn(0);
        when(medAttachMock.getDevice()).thenReturn(0);
        when(vboxMachineMock.getMedium("SATA", 0, 0)).thenReturn(vboxMachMediumMock);
        when(vboxMachMediumMock.getState()).thenReturn(MediumState.Created);
        //there should be returned string with value = "VirtualMachine_01"
        when(vboxMachineMock.getName()).thenReturn(vm.getName());
        when(vboxMock.getSystemProperties()).thenReturn(sysPropMock);
        when(sysPropMock.getDefaultMachineFolder()).thenReturn("VirtualBox VMs");
        doThrow(VBoxException.class).when(vboxMock).findMachine(expClone.getName());
        when(cloneFolderMock.isDirectory()).thenReturn(false);
        when(vbmMock.getSessionObject()).thenReturn(sessionMock);
        when(sessionMock.getConsole()).thenReturn(consoleMock);
        when(consoleMock.takeSnapshot("Linked Base For " + vm.getName() + " and " + expClone.getName(), null))
                .thenReturn(progressMock1);
        when(progressMock1.getCompleted()).thenReturn(false, false, true);
        when(sessionMock.getState()).thenReturn(SessionState.Unlocking, SessionState.Unlocked);
        when(vboxMachineMock.getCurrentSnapshot()).thenReturn(snapshotMock);
        when(snapshotMock.getMachine()).thenReturn(clonableMachineMock);
        when(clonableMachineMock.getOSTypeId()).thenReturn("Fedora_64");
        //there should be returned mock object of type IMachine when the method IVirtualBox::createMachine()
        //is called in order to control returned values of its methods
        when(vboxMock.createMachine(null, expClone.getName(), null, "Fedora_64", null)).thenReturn(vboxMachineCloneMock);        
        when(clonableMachineMock.cloneTo(vboxMachineCloneMock, CloneMode.MachineState, Arrays.asList(CloneOptions.Link)))
                .thenReturn(progressMock2);
        when(progressMock2.getCompleted()).thenReturn(false, false, false, false, true);
        when(progressMock2.getPercent()).thenReturn(0L, 20L, 50L, 99L);
        when(progressMock2.getResultCode()).thenReturn(0);
        //there should be returned string with value = "Fedora_64"
        when(vboxMachineCloneMock.getOSTypeId()).thenReturn("Fedora_64");
        //there should be returned mock object of type IGuestOSType when the method IVirtualBox::getGuestOSType()
        //is called in order to control returned values of its methods
        when(vboxMock.getGuestOSType("Fedora_64")).thenReturn(guestOSTypeMocked);
        when(vboxMachineCloneMock.getMediumAttachmentsOfController("SATA")).thenReturn(medAttachs);
        when(medAttachMock.getPort()).thenReturn(0);
        when(medAttachMock.getDevice()).thenReturn(0);
       //there should be returned mock object of type IMedium when the method IMachine::getMedium() is called
        //in order to control returned values of its methods
        when(vboxMachineCloneMock.getMedium("SATA", 0, 0)).thenReturn(cloneMediumMock);
        //there should be returned string with value = "399d0aea-01aa-4a55-a9b7-5cd345570a1"
        when(vboxMachineCloneMock.getId()).thenReturn(expClone.getId().toString());
        //there should be returned string with value = "VirtualMachine_01_FCopy1"
        when(vboxMachineCloneMock.getName()).thenReturn(expClone.getName());
        //there should be returned Long value = 1
        when(vboxMachineCloneMock.getCPUCount()).thenReturn(expClone.getCountOfCPU());
        //there should be returned Long value = 1
        when(vboxMachineCloneMock.getMonitorCount()).thenReturn(expClone.getCountOfMonitors());
        //there should be returned Long value = 100
        when(vboxMachineCloneMock.getCPUExecutionCap()).thenReturn(expClone.getCPUExecutionCap());
        //there should be returned Long value = 4096
        when(vboxMachineCloneMock.getMemorySize()).thenReturn(expClone.getSizeOfRAM());
        //there should be returned Long value = 12
        when(vboxMachineCloneMock.getVRAMSize()).thenReturn(expClone.getSizeOfVRAM());
        //there should be returned Long value = 21474836480
        when(cloneMediumMock.getLogicalSize()).thenReturn(expClone.getHardDiskTotalSize());
        //there should be returned Long value = 7187988480
        when(cloneMediumMock.getSize()).thenReturn(expClone.getHardDiskTotalSize() - expClone.getHardDiskFreeSpaceSize());
        //there should be returned string with value = "Linux"
        when(guestOSTypeMocked.getFamilyId()).thenReturn(expClone.getTypeOfOS());
        //there should be returned string with value = "Fedora_64"
        when(guestOSTypeMocked.getId()).thenReturn(expClone.getIdentifierOfOS());

        //there should not appear any exception nor error
        VirtualMachine actClone = sut.createVMClone(vm, CloneType.LINKED);

        assertDeepVMsEquals(expClone, actClone);
        OutputHandler.setOutputStream(origOutStream);
    }

    /**
     * This test tests that there should be possible to create any clone of a
     * virtual machine which is not present in a list of registered machines.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void createCloneOfNonexistentVirtualMachine() throws Exception {
        //represents a virtual machine which should be cloned
        VirtualMachine vm = new VMBuilder().build();

        //there should be returned mock object of type IVirtualBox when the method VirtualBoxManager::getVBox()
        //is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there should be thrown VBoxException exception when the method IVirtualBox::findMachine() is called
        //with an ID of required virtual machine and means that that virtual machine is not present on
        //a particular physical machine
        doThrow(VBoxException.class).when(vboxMock).findMachine(vm.getId().toString());

        exception.expect(UnknownVirtualMachineException.class);
        sut.createVMClone(vm, CloneType.LINKED);
    }

    /**
     * This test tests that there cannot be cloned a virtual machine which is
     * not accessible.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void createInaccessibleVirtualMachineClone() throws Exception {
        //represents a virtual machine which should be cloned
        VirtualMachine vm = new VMBuilder().build();
        //represents mock object of type IMachine for easier and better test control
        IMachine machineMocked = mock(IMachine.class);
        //represents mock object of type IVirtualBoxErrorInfo for easier and better test control
        IVirtualBoxErrorInfo vboxErrInfoMock = mock(IVirtualBoxErrorInfo.class);

        //there should be returned mock object of type IVirtualBox when the method VirtualBoxManager::getVBox()
        //is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there should be returned mock object of type IMachine when the method IVirtualBox::findMachine()
        //is called with an ID of a required virtual machine in order to control returned values of its methods
        when(vboxMock.findMachine(vm.getId().toString())).thenReturn(machineMocked);        
        //there should be returned a negative answer to query for virtual machine accessibility which means
        //that the virtual machine cannot be cloned
        when(machineMocked.getAccessible()).thenReturn(false);
        when(machineMocked.getAccessError()).thenReturn(vboxErrInfoMock);
        when(vboxErrInfoMock.getText()).thenReturn("Error text");

        exception.expect(UnexpectedVMStateException.class);
        sut.createVMClone(vm, CloneType.FULL_FROM_MACHINE_AND_CHILD_STATES);
    }

    /**
     * This test tests that there cannot be cloned a virtual machine which is
     * not in a required state for cloning ("PoweredOff", "Running", "Saved",
     * "Paused").
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void createVirtualMachineCloneWithIncorrectState() throws Exception {
        //represents a virtual machine which should be cloned
        VirtualMachine vm = new VMBuilder().build();
        //represents mock object of type IMachine for easier and better test control
        IMachine machineMocked = mock(IMachine.class);

        //there should be returned mock object of type IVirtualBox when the method VirtualBoxManager::getVBox()
        //is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there should be returned mock object of type IMachine when the method IVirtualBox::findMachine()
        //is called with an ID of a required virtual machine in order to control returned values of its methods
        when(vboxMock.findMachine(vm.getId().toString())).thenReturn(machineMocked);
        //there should be returned a positive answer to query for virtual machine accessibility
        when(machineMocked.getAccessible()).thenReturn(true);
        //there should be returned virtual machine state with value "Starting" which
        //signals an incorrect state of virtual machine for this operation (the same problem
        //represents states like "Aborted", "Restoring", "Saving", "Stopping" etc.)
        when(machineMocked.getState()).thenReturn(MachineState.Starting);

        exception.expect(UnexpectedVMStateException.class);
        sut.createVMClone(vm, CloneType.FULL_FROM_MACHINE_STATE);
    }

    /**
     * This test tests that there should be invoked ConnectionFailureException
     * exception when any connection problem appears while the method
     * NativeVBoxAPIManager::createVirtualMachineClone() is being processed.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void createVirtualMachineCloneWithSuddenNetworkConnectionLoss() throws Exception {
        //represents a virtual machine which should be cloned
        VirtualMachine vm = new VMBuilder().build();
        String username = vm.getHostMachine().getUsername();
        String userPassword = vm.getHostMachine().getUserPassword();
        String url = "http://" + vm.getHostMachine().getAddressIP()
                + ":" + vm.getHostMachine().getPortOfVTWebServer();

        //there should be returned VBoxException exception when the method VirtualBoxManager::connect()
        //is called with a required physical machine and means that there is any connection problem
        doThrow(VBoxException.class).when(vbmMock).connect(url, username, userPassword);

        exception.expect(ConnectionFailureException.class);
        sut.createVMClone(vm, CloneType.LINKED);
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
        assertEquals("vm1 = " + expVM.getName() + ", vm2 = " + actVM.getName() + " VMs should have same HDD free space size", expVM.getHardDiskFreeSpaceSize(), actVM.getHardDiskFreeSpaceSize());
        assertEquals("vm1 = " + expVM.getName() + ", vm2 = " + actVM.getName() + " VMs should have same HDD total size", expVM.getHardDiskTotalSize(), actVM.getHardDiskTotalSize());
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
