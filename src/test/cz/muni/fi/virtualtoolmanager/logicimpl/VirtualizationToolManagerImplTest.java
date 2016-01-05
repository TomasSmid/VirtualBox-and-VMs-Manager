/*
 * Copyright 2015 Tom8š Šmíd.
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

import cz.muni.fi.virtualtoolmanager.pubapi.io.OutputHandler;
import cz.muni.fi.virtualtoolmanager.pubapi.entities.PhysicalMachine;
import cz.muni.fi.virtualtoolmanager.pubapi.entities.VirtualMachine;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnexpectedVMStateException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownVirtualMachineException;
import cz.muni.fi.virtualtoolmanager.pubapi.types.CloneType;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.virtualbox_4_3.MachineState;

/**
 * This test class ensure unit testing of class VirtualizationToolManagerImpl
 * and is intended to be a pointer that class VirtualizationToolManagerImpl
 * works as expected.
 *
 * @author Tomáš Šmíd
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({VirtualizationToolManagerImpl.class, NativeVBoxAPIManager.class, ConnectionManagerImpl.class,
                 NativeVBoxAPIMachine.class, VirtualMachineManagerImpl.class})
public class VirtualizationToolManagerImplTest {
    
    @Rule
    ExpectedException exception = ExpectedException.none();

    private VirtualizationToolManagerImpl sut;
    private NativeVBoxAPIManager natAPIManMock;
    private NativeVBoxAPIMachine natAPIMachMock;
    private PhysicalMachine hostMachine = new PMBuilder().build();
    private ConnectionManagerImpl conManMock;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    @Before
    public void setUp() throws Exception {
        natAPIManMock = mock(NativeVBoxAPIManager.class);
        conManMock = mock(ConnectionManagerImpl.class);
        natAPIMachMock = mock(NativeVBoxAPIMachine.class);
        whenNew(NativeVBoxAPIManager.class).withNoArguments().thenReturn(natAPIManMock);
        whenNew(ConnectionManagerImpl.class).withNoArguments().thenReturn(conManMock);
        whenNew(NativeVBoxAPIMachine.class).withNoArguments().thenReturn(natAPIMachMock);
        sut = new VirtualizationToolManagerImpl(hostMachine);
        OutputHandler.setOutputStream(new PrintStream(outContent));
        OutputHandler.setErrorOutputStream(new PrintStream(errContent));
    }

    @After
    public void cleanUp() {
        OutputHandler.setErrorOutputStream(null);
        OutputHandler.setOutputStream(null);
    }

    /**
     * This test tests that if the registration of a virtual machine is
     * successful then there should be written an information about that on
     * standard output, but standard error output should stay empty, because no
     * exception nor error should appear.
     */
    @Test
    public void registerVirtualMachineIdealCase(){
        //represents name of a virtual machine which should be newly registered
        String vmName = "VirtualMachine_01";

        //there should be returned a positive answer when the method NativeVBoxAPIManager::registerVirtualMachine()
        //is called which means the operation was performed successfully
        when(natAPIManMock.registerVirtualMachine(hostMachine, vmName)).thenReturn(true);
        //there should be returned a positive answer when the method ConnectionManagerImpl::isConnected() is called
        //with required physical machine which means that the physical machine is connected and can be worked with it
        when(conManMock.isConnected(hostMachine)).thenReturn(true);

        sut.registerVirtualMachine(vmName);

        assertFalse("There should be written a message on a standard output informing about"
                + "a successful operation", outContent.toString().isEmpty());
        assertTrue("There should not be written any message on a standard error output",
                errContent.toString().isEmpty());

    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to register a virtual machine by a null name.
     */
    @Test
    public void registerVirtualMachineWithNullName(){
        //there should be returned a positive answer when the method ConnectionManagerImpl::isConnected() is called
        //with required physical machine which means that the physical machine is connected and can be worked with it
        when(conManMock.isConnected(hostMachine)).thenReturn(true);

        exception.expect(IllegalArgumentException.class);
        sut.registerVirtualMachine(null);
        
        //checks the method NativeVBoxAPIManager::registerVirtualMachine() was not called as expected
        verify(natAPIManMock, never()).registerVirtualMachine(
                any(PhysicalMachine.class), anyString());
    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to register a virtual machine by an empty name.
     */
    @Test
    public void registerVirtualMachineWithEmptyName(){
        //there should be returned a positive answer when the method ConnectionManagerImpl::isConnected() is called
        //with required physical machine which means that the physical machine is connected and can be worked with it
        when(conManMock.isConnected(hostMachine)).thenReturn(true);

        exception.expect(IllegalArgumentException.class);
        sut.registerVirtualMachine("");
        
        //checks the method NativeVBoxAPIManager::registerVirtualMachine() was not called as expected
        verify(natAPIManMock, never()).registerVirtualMachine(
                any(PhysicalMachine.class), anyString());
    }

    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to register a virtual machine on a physical
     * machine which is not connected.
     */
    @Test
    public void registerVirtualMachineOnDisconnectedPhysicalMachine(){
        //represents name of a virtual machine which should be newly registered
        String vmName = "VirtualMachine_01";

        //there is returned a negative answer when the method ConnectionManagerImpl::isConnected() is called
        //with a required host machine which means the host machine is not connected and that's why the virtual
        //machine cannot be registered (in fact, the negative answer would firsly return
        //ConnectedPhysicalMachine::isConnected())
        when(conManMock.isConnected(hostMachine)).thenReturn(false);

        exception.expect(ConnectionFailureException.class);
        sut.registerVirtualMachine(vmName);
        
        //checks the method NativeVBoxAPIManager::registerVirtualMachine() was not called as expected
        verify(natAPIManMock, never()).registerVirtualMachine(
                any(PhysicalMachine.class), anyString());
    }

    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to register a virtual machine on a physical
     * machine with which is the connection lost while there is being processed
     * the registration operation.
     */
    @Test
    public void registerVirtualMachineWithSuddenNetworkConnectionLoss(){
        //represents name of a virtual machine which should be newly registered
        String vmName = "VirtualMachine_01";
        //mock object of type ConnectionFailureException for easier and better test control
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);

        //there is returned a positive answer when the method ConnectionManagerImpl::isConnected() is called
        //with a required host machine which means the host machine is connected and therefore there can be done
        //any work with it
        when(conManMock.isConnected(hostMachine)).thenReturn(true);
        //there should be thrown ConnectionFailureException exception when the method
        //NativeVBoxAPIManager::registerVirtualMachine() is called with a required arguments
        doThrow(conFailExMock).when(natAPIManMock).registerVirtualMachine(hostMachine, vmName);

        exception.expect(ConnectionFailureException.class);
        sut.registerVirtualMachine(vmName);
        
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMock).disconnectFrom(hostMachine);
    }

    /**
     * This test tests that there is not possible to register one virtual
     * machine more than once (if virtual machine is still in the list of
     * registered virtual machines) on a particular physical machine.
     */
    @Test
    public void registerAlreadyRegisteredVirtualMachine(){
        //represents name of a virtual machine which should be newly registered
        String vmName = "VirtualMachine_01";

        //there is returned a positive answer when the method ConnectionManagerImpl::isConnected() is called
        //with a required host machine which means the host machine is connected and therefore there can be done
        //any work with it
        when(conManMock.isConnected(hostMachine)).thenReturn(true);
        //this step simulates the second call of the method VirtualizationToolManager::registerVirtualMachine()
        //with the same virtual machine which is still registered after first call
        when(natAPIManMock.registerVirtualMachine(hostMachine, vmName)).thenReturn(false);
        
        sut.registerVirtualMachine(vmName);

        assertFalse("There should be written a message on a standard output that the virtual machine"
                + "cannot be registered, because it is already registered", outContent.toString().isEmpty());
        assertTrue("There should not be written a message on a standard error output", errContent.toString().isEmpty());
    }

    /**
     * This test tests that there is invoked UnknownVirtualMachineException when
     * there is made an attempt to register a virtual machine which is not
     * present in the VirtualBox virtual machine folder (at least virtual machine
     * with the given name).
     */
    @Test
    public void registerNonexistentVirtualMachine(){
        //represents name of a virtual machine which should be newly registered
        String vmName = "VirtualMachine_01";
        //mock object of type UnknownVirtualMachineException for easier and better test control
        UnknownVirtualMachineException unVirtMachExMock = mock(UnknownVirtualMachineException.class);

        //there is returned a positive answer when the method ConnectionManagerImpl::isConnected() is called
        //with a required host machine which means the host machine is connected and therefore there can be done
        //any work with it
        when(conManMock.isConnected(hostMachine)).thenReturn(true);
        //there should be thrown ConnectionFailureException exception when the method
        //NativeVBoxAPIManager::registerVirtualMachine() is called with a required arguments
        doThrow(unVirtMachExMock).when(natAPIManMock).registerVirtualMachine(hostMachine, vmName);

        exception.expect(UnknownVirtualMachineException.class);
        sut.registerVirtualMachine(vmName);
    }

    /**
     * This test tests that if there are all neccessary conditions met and
     * required virtual machine exists on the particular physical machine, then
     * there is returned the required VirtualMachine object and no further steps
     * are performed.
     */
    @Test
    public void findVirtualMachineByIdWithSomeMatch(){
        //represents a virtual machine which is required and should be returned as a result of get operation
        VirtualMachine expVM = new VMBuilder().build();

        //means that the hostMachine is connected and so there can be done a work with it
        when(conManMock.isConnected(hostMachine)).thenReturn(true);
        //there should be returned expVM object when the method NativeVBoxAPIManager::getVirtualMachine() is called
        //with an ID of a reguired virtual machine (all conditions (checks) are met at this point and virtual machine
        //can be returned)
        when(natAPIManMock.getVirtualMachine(hostMachine, expVM.getId().toString())).thenReturn(expVM);

        VirtualMachine actVM = sut.findVirtualMachineById(expVM.getId());

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertTrue("There should not be a message on a standard error output", errContent.toString().isEmpty());
        //checks the returned virtual machine is realy the same as the original
        assertDeepVMsEquals(expVM, actVM);
    }

    /**
     * This test tests that there is returned a null object of type
     * VirtualMachine when there does not exist any virtual machine with a
     * required ID on a particular physical machine.
     */
    @Test
    public void findVirtualMachineByIdWithNoMatch(){
        //represents a virtual machine which is required and should be returned as a result of get operation
        VirtualMachine expVM = new VMBuilder().build();
        UnknownVirtualMachineException unknownVMExMock =
                mock(UnknownVirtualMachineException.class);

        //means that the hostMachine is connected and so there can be done a work with it
        when(conManMock.isConnected(hostMachine)).thenReturn(true);
       //there should be returned a null object when the method NativeVBoxAPIManager::getVirtualMachine()
        //is called with an ID of a reguired virtual machine
        doThrow(unknownVMExMock).when(natAPIManMock).getVirtualMachine(
                hostMachine, expVM.getId().toString());

        VirtualMachine actVM = sut.findVirtualMachineById(expVM.getId());
                
        assertNull("The returned virtual machine object should be null", actVM);
    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to retrieve a virtual machine by a null id.
     */
    @Test
    public void findVirtualMachineByIdWithNullUUID(){
        exception.expect(IllegalArgumentException.class);
        sut.findVirtualMachineById(null);
        
        //checks the method NativeVBoxAPIManager::getVirtualMachine() has never been called as expected
        verify(natAPIManMock, never()).getVirtualMachine(
                any(PhysicalMachine.class), anyString());
    }

    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to retrieve a virtual machine by id from a
     * physical machine which is not connected.
     */
    @Test
    public void findVirtualMachineByIdOnDisconnectedPhysicalMachine(){
        //represents a virtual machine which is required and should be returned as a result of get operation
        VirtualMachine expVM = new VMBuilder().build();

        //means that the host machine is not connected and so there cannot be done any work with it
        when(conManMock.isConnected(hostMachine)).thenReturn(false);

        exception.expect(ConnectionFailureException.class);
        sut.findVirtualMachineById(expVM.getId());
        
        //checks the method NativeVBoxAPIManager::getVirtualMachine() has never been called as expected
        verify(natAPIManMock, never()).getVirtualMachine(
                any(PhysicalMachine.class), anyString());
    }

    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to retrieve a virtual machine by id from a
     * physical machine with which is the connection lost while there is being
     * processed the virtual machine retrieve operation.
     */
    @Test
    public void findVirtualMachineByIdWithSuddenNetworkConnectionLoss(){
        //represents a virtual machine which is required and should be returned as a result of get operation
        VirtualMachine expVM = new VMBuilder().build();
        //mock object of type ConnectionFailureException for easier and better test control
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);

        //means that the hostMachine is connected and so there can be done a work with it
        when(conManMock.isConnected(hostMachine)).thenReturn(true);
       //there should be thrown a ConnectionFailureException exception when the method
        //NativeVBoxAPIManager::getVirtualMachine() is called and means there occured any connection problem
        //when there was being worked with VirtualBox on the host machine
        doThrow(conFailExMock).when(natAPIManMock).getVirtualMachine(hostMachine, expVM.getId().toString());

        exception.expect(ConnectionFailureException.class);
        sut.findVirtualMachineById(expVM.getId());
        
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMock).disconnectFrom(hostMachine);
    }

    /**
     * This test tests that if there are all neccessary conditions met and
     * required virtual machine exists on the particular physical machine then
     * there is returned the required VirtualMachine object and no further steps
     * are performed.
     */
    @Test
    public void findVirtualMachineByNameWithSomeMatch(){
        //represents a virtual machine which is required and should be returned as a result of get operation
        VirtualMachine expVM = new VMBuilder().build();

        //means that the hostMachine is connected and so there can be done a work with it
        when(conManMock.isConnected(hostMachine)).thenReturn(true);
        //there should be returned expVM object when the method NativeVBoxAPIManager::getVirtualMachine() is called
        //with a name of a reguired virtual machine (all conditions (checks) are met at this point and virtual machine
        //can be returned)
        when(natAPIManMock.getVirtualMachine(hostMachine, expVM.getName())).thenReturn(expVM);

        VirtualMachine actVM = sut.findVirtualMachineByName(expVM.getName());

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertTrue("There should not be a message on a standard error output", errContent.toString().isEmpty());
        //checks the returned virtual machine is realy the same as the original
        assertDeepVMsEquals(expVM, actVM);
    }

    /**
     * This test tests that there is returned a null object of type
     * VirtualMachine when there does not exist any virtual machine with a
     * required name on a particular physical machine.
     */
    @Test
    public void findVirtualMachineByNameWithNoMatch(){
        //represents a virtual machine which is required and should be returned as a result of get operation
        VirtualMachine expVM = new VMBuilder().build();
        UnknownVirtualMachineException unknownVMExMock =
                mock(UnknownVirtualMachineException.class);

        //means that the hostMachine is connected and so there can be done a work with it
        when(conManMock.isConnected(hostMachine)).thenReturn(true);
       //there should be returned a null object when the method NativeVBoxAPIManager::getVirtualMachine()
        //is called with a name of a reguired virtual machine
        doThrow(unknownVMExMock).when(natAPIManMock).getVirtualMachine(
                hostMachine, expVM.getName());

        VirtualMachine actVM = sut.findVirtualMachineByName(expVM.getName());
                
        assertNull("The returned virtual machine object should be null", actVM);
    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to retrieve a virtual machine by a null name.
     */
    @Test
    public void findVirtualMachineByNameWithNullName(){
        exception.expect(IllegalArgumentException.class);
        sut.findVirtualMachineByName(null);
        
        //checks the method NativeVBoxAPIManager::getVirtualMachine() has never been called as expected
        verify(natAPIManMock, never()).getVirtualMachine(
                any(PhysicalMachine.class), anyString());
    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to retrieve a virtual machine by an empty name.
     */
    @Test
    public void findVirtualMachineByNameWithEmptyName(){
        exception.expect(IllegalArgumentException.class);
        sut.findVirtualMachineByName("");
        
        //checks the method NativeVBoxAPIManager::getVirtualMachine() has never been called as expected
        verify(natAPIManMock, never()).getVirtualMachine(
                any(PhysicalMachine.class), anyString());
    }

    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to retrieve a virtual machine by name from a
     * physical machine which is not connected.
     */
    @Test
    public void findVirtualMachineByNameOnDisconnectedPhysicalMachine(){
        //represents a virtual machine which is required and should be returned as a result of get operation
        VirtualMachine expVM = new VMBuilder().build();

        //means that the host machine is not connected and so there cannot be done any work with it
        when(conManMock.isConnected(hostMachine)).thenReturn(false);

        exception.expect(ConnectionFailureException.class);
        sut.findVirtualMachineByName(expVM.getName());
        
        //checks the method NativeVBoxAPIManager::getVirtualMachine() has never been called as expected
        verify(natAPIManMock, never()).getVirtualMachine(
                any(PhysicalMachine.class), anyString());
    }

    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to retrieve a virtual machine by name from a
     * physical machine with which is the connection lost while there is being
     * processed the virtual machine retrieve operation.
     */
    @Test
    public void findVirtualMachineByNameWithSuddenNetworkConnectionLoss(){
        //represents a virtual machine which is required and should be returned as a result of get operation
        VirtualMachine expVM = new VMBuilder().build();
        //mock object of type ConnectionFailureException for easier and better test control
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);

        //means that the hostMachine is connected and so there can be done a work with it
        when(conManMock.isConnected(hostMachine)).thenReturn(true);
       //there should be thrown a ConnectionFailureException exception when the method
        //NativeVBoxAPIManager::getVirtualMachine() is called and means there occured any connection problem
        //when there was being worked with VirtualBox on the host machine
        doThrow(conFailExMock).when(natAPIManMock).getVirtualMachine(hostMachine, expVM.getName());
        
        exception.expect(ConnectionFailureException.class);
        sut.findVirtualMachineByName(expVM.getName());
        
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMock).disconnectFrom(hostMachine);
    }

    /**
     * This test tests that if there exist some virtual machines on a particular
     * physical machine, then the list of this virtual machines is returned as a
     * result of get operation and no message should be written to any output.
     */
    @Test
    public void getVirtualMachinesWithReturnedNonemptyVMsList(){
        //represents first of two registered virtual machines from a particular physical machine
        VirtualMachine vm1 = new VMBuilder().build();
        //represents a second of two registered virtual machines from a particular physical machine
        VirtualMachine vm2 = new VMBuilder().name("VirtualMachine_02")
                .id(UUID.fromString("793d084a-cb01-4a55-a9b7-531c4555aaaa"))
                .build();
        List<VirtualMachine> expVMs = Arrays.asList(vm1, vm2);

        //means that the hostMachine is connected and so there can be done a work with it
        when(conManMock.isConnected(hostMachine)).thenReturn(true);
       //there should be returned the list of registered virtual machines when the method
        //NativeVBoxAPIManager::getAllVirtualMachines() is called
        when(natAPIManMock.getAllVirtualMachines(hostMachine)).thenReturn(expVMs);

        List<VirtualMachine> actVMs = sut.getVirtualMachines();

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertTrue("There should not be a message on a standard error output", errContent.toString().isEmpty());
        assertDeepVMsEquals(expVMs, actVMs);
    }

    /**
     * This test tests that there is returned an empty list of virtual machines
     * without any informing message on any output when the method
     * VirtualizationToolManagerImpl::getVirtualMachines() is called.
     */
    @Test
    public void getVirtualMachinesWithReturnedEmptyVMsList(){
        //means that the hostMachine is connected and so there can be done a work with it
        when(conManMock.isConnected(hostMachine)).thenReturn(true);
       //there should be returned an empty list of registered virtual machines when the method
        //NativeVBoxAPIManager::getAllVirtualMachines() is called
        when(natAPIManMock.getAllVirtualMachines(hostMachine)).thenReturn(new ArrayList<>());

        List<VirtualMachine> actVMs = sut.getVirtualMachines();

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertTrue("There should not be a message on a standard error output", errContent.toString().isEmpty());
        assertTrue("The list of virtual machines should be empty", actVMs.isEmpty());
    }

    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to retrieve all virtual machines from a physical
     * machine which is not connected.
     */
    @Test
    public void getVirtualMachinesFromDisconnectedPhysicalMachine(){
        //means that the host machine is not connected and so there cannot be done any work with it
        when(conManMock.isConnected(hostMachine)).thenReturn(false);

        exception.expect(ConnectionFailureException.class);
        sut.getVirtualMachines();
        
        //checks the method NativeVBoxAPIManager::getAllVirtualMachines() has never been called as expected
        verify(natAPIManMock, never()).getAllVirtualMachines(any(PhysicalMachine.class));
    }

    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to retrieve all virtual machines from a physical
     * machine with which the connection is lost while there is being processed
     * all virtual machines retrieve operation.
     */
    @Test
    public void getVirtualMachinesWithSuddenNetworkConnectionLoss(){
        //mock object of type ConnectionFailureException for easier and better test control
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);
        
        //means that the hostMachine is connected and so there can be done a work with it
        when(conManMock.isConnected(hostMachine)).thenReturn(true);
       //there should be thrown a ConnectionFailureException exception when the method
        //NativeVBoxAPIManager::getAllVirtualMachines() is called and means there occured any connection problem
        //when there was being worked with VirtualBox on the host machine
        doThrow(conFailExMock).when(natAPIManMock).getAllVirtualMachines(hostMachine);
        
        exception.expect(ConnectionFailureException.class);
        sut.getVirtualMachines();
                
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMock).disconnectFrom(hostMachine);
    }

    /**
     * This test tests that the required virtual machine is successfully removed
     * when all important values are valid and that there appears an informing
     * message on a standard output about successful operation.
     */
    @Test
    public void removeValidVirtualMachine() {
        //represents a virtual machine which intended to be removed
        VirtualMachine vm = new VMBuilder().build();

        //means that the hostMachine is connected and so there can be done a work with it
        when(conManMock.isConnected(hostMachine)).thenReturn(true);

        sut.removeVirtualMachine(vm);

        assertFalse("There should be a message on a standard output that the virtual machine was "
                + "successfully removed", outContent.toString().isEmpty());
        assertTrue("There should not be a message on a standard error output", errContent.toString().isEmpty());
    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to remove a null virtual machine.
     */
    @Test
    public void removeNullVirtualMachine(){
        exception.expect(IllegalArgumentException.class);
        sut.removeVirtualMachine(null);
        
        //checks the method NativeVBoxAPIManager::removeVirtualMachine() has never been called
        verify(natAPIManMock, never()).removeVirtualMachine(any(VirtualMachine.class));
    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to remove a virtual machine which has specified a wrong
     * physical machine attribute against the correct one.
     */
    @Test
    public void removeVirtualMachineWithIncorrectPhysicalMachine(){
        //represents a virtual machine which is intended to be removed
        VirtualMachine vm = new VMBuilder().hostMachine(
                new PMBuilder().addressIP("11.11.11.0").build()).build();

        exception.expect(IllegalArgumentException.class);
        sut.removeVirtualMachine(vm);
        
        //checks the method NativeVBoxAPIManager::removeVirtualMachine() has never been called
        verify(natAPIManMock, never()).removeVirtualMachine(any(VirtualMachine.class));
    }

    /**
     * This test tests that there is invoked UnknownVirtualMachineException when
     * there is made an attempt to remove a virtual machine which does not exist.
     */
    @Test
    public void removeNonexistentVirtualMachine(){
        //represents a virtual machine which should be removed
        VirtualMachine vm = new VMBuilder().build();
        //mock object of type UnknownVirtualMachineException for easier and better test control
        UnknownVirtualMachineException unVirtMachExMock = mock(UnknownVirtualMachineException.class);

        //means that the hostMachine is connected and so there can be done a work with it
        when(conManMock.isConnected(hostMachine)).thenReturn(true);
       //there should be thrown UnknownVirtualMachineException exception when the method
        //NativeVBoxAPIManager::removeVirtualMachine() is called with the required virtual machine
        //and means the virtual machine cannot be removed, because it does not exist on a particular
        //physical machine
        doThrow(unVirtMachExMock).when(natAPIManMock).removeVirtualMachine(vm);

        exception.expect(UnknownVirtualMachineException.class);
        sut.removeVirtualMachine(vm);
    }

    /**
     * This test tests that there is invoked UnexpectedVMStateException when
     * there is made an attempt to remove a virtual machine which is not in a
     * required state for virtual machine removal operation.
     */
    @Test
    public void removeVirtualMachineWithInvalidState(){
        //represents a virtual machine which should be removed
        VirtualMachine vm = new VMBuilder().build();
        //mock object of type UnexpectedVMStateException for easier and better test control
        UnexpectedVMStateException unexVMStateMock = mock(UnexpectedVMStateException.class);

        //means that the hostMachine is connected and so there can be done a work with it
        when(conManMock.isConnected(hostMachine)).thenReturn(true);
       //there should be thrown UnexpectedVMStateException exception when the method
        //NativeVBoxAPIManager::removeVirtualMachine() is called with the required virtual machine
        //and means the virtual machine cannot be removed, because it is not in a required state for removing
        doThrow(unexVMStateMock).when(natAPIManMock).removeVirtualMachine(vm);

        exception.expect(UnexpectedVMStateException.class);
        sut.removeVirtualMachine(vm);
    }

    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to remove a virtual machine from a physical
     * machine which is not connected.
     */
    @Test
    public void removeVirtualMachineOnDisconnectedPhysicalMachine(){
        //represents a virtual machine which is intended to be removed
        VirtualMachine vm = new VMBuilder().build();

        //means that the hostMachine is not connected and so there cannot be done a work with it
        when(conManMock.isConnected(hostMachine)).thenReturn(false);

        exception.expect(ConnectionFailureException.class);
        sut.removeVirtualMachine(vm);
        
        //checks the method NativeVBoxAPIManager::removeVirtualMachine() has never been called
        verify(natAPIManMock, never()).removeVirtualMachine(any(VirtualMachine.class));
    }

    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to remove virtual machine from a physical machine
     * with which the connection is lost while the virtual machine removal
     * operation is being processed.
     */
    @Test
    public void removeVirtualMachineWithSuddenNetworkConnectionLoss(){
        //represents a virtual machine which should be removed
        VirtualMachine vm = new VMBuilder().build();
        //mock object of type ConnectionFailureException for easier and better test control
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);

        //means that the hostMachine is connected and so there can be done a work with it
        when(conManMock.isConnected(hostMachine)).thenReturn(true);
        //there should be thrown a ConnectionFailureException exception when the method
        //NativeVBoxAPIManager::removeVirtualMachine() is called and means there occured any connection problem
        //when there was being worked with VirtualBox on the host machine
        doThrow(conFailExMock).when(natAPIManMock).removeVirtualMachine(vm);

        exception.expect(ConnectionFailureException.class);
        sut.removeVirtualMachine(vm);
        
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMock).disconnectFrom(hostMachine);
    }

    /**
     * This test tests that if there are ale conditions for cloning met then
     * there is returned a virtual machine clone as a result and on a standard
     * output appears informing message that the operation was successful.
     */
    @Test
    public void cloneValidVirtualMachine(){
        //represents a virtual machine which should be cloned
        VirtualMachine origVM = new VMBuilder().build();
        //represents a virtual machine clone which should be returned as a result of cloning operation
        VirtualMachine expVMClone = new VMBuilder().name(origVM + "_FullClone1")
                .id(UUID.fromString("793d084a-0189-4a55-a9c0-531c455aaab"))
                .build();

        //means that the host machine of virtual machine is connected and the cloning operation can start
        when(conManMock.isConnected(hostMachine)).thenReturn(true);
        //there should be returned expVMClone object when the method NativeVBoxAPIManager::createVirtualMachineClone()
        //is called with a required virtual machine
        when(natAPIManMock.createVMClone(origVM, CloneType.FULL_FROM_ALL_STATES)).thenReturn(expVMClone);

        VirtualMachine actVMClone = sut.cloneVirtualMachine(origVM, CloneType.FULL_FROM_ALL_STATES);

        assertFalse("There should be a message on a standard output that the virtual machine clone"
                + "was created successfully", outContent.toString().isEmpty());
        assertTrue("There should not be a message on a standard error output", errContent.toString().isEmpty());
        assertDeepVMsEquals(expVMClone, actVMClone);
    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to clone a null virtual machine.
     */
    @Test
    public void cloneNullVirtualMachine(){
        exception.expect(IllegalArgumentException.class);
        sut.cloneVirtualMachine(null, CloneType.FULL_FROM_ALL_STATES);
                
        //checks the method NativeVBoxAPIManager::createVirtualMachineClone() has never been called
        verify(natAPIManMock, never()).createVMClone(any(VirtualMachine.class),
                any(CloneType.class));
    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to clone a virtual machine which has wrong physical
     * machine attribute against the correct one.
     */
    @Test
    public void cloneVirtualMachineWithIncorrectPhysicalMachine(){
        //represents a virtual machine which should be cloned
        VirtualMachine origVM = new VMBuilder().hostMachine(
                new PMBuilder().addressIP("11.11.11.0").build()).build();

        exception.expect(IllegalArgumentException.class);
        sut.cloneVirtualMachine(origVM, CloneType.FULL_FROM_ALL_STATES);
        
        //checks the method NativeVBoxAPIManager::createVirtualMachineClone() has never been called
        verify(natAPIManMock, never()).createVMClone(any(VirtualMachine.class),
                any(CloneType.class));
    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to clone a virtual machine according a null clone mode.
     */
    @Test
    public void cloneVirtualMachineWithNullCloneMode(){
        //represents a virtual machine which should be cloned
        VirtualMachine origVM = new VMBuilder().build();

        exception.expect(IllegalArgumentException.class);
        sut.cloneVirtualMachine(origVM, null);
        
        //checks the method NativeVBoxAPIManager::createVirtualMachineClone() has never been called
        verify(natAPIManMock, never()).createVMClone(any(VirtualMachine.class),
                any(CloneType.class));

    }
    
    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to clone a virtual machine on a physical machine
     * which is not connected.
     */
    @Test
    public void cloneVirtualMachineOnDisconnectedPhysicalMachine(){
        //represents a virtual machine which should be cloned
        VirtualMachine origVM = new VMBuilder().build();

        //means that the hostMachine is not connected and therefore the virtual machine cannot be cloned
        when(conManMock.isConnected(hostMachine)).thenReturn(false);

        exception.expect(ConnectionFailureException.class);
        sut.cloneVirtualMachine(origVM, CloneType.FULL_FROM_ALL_STATES);
        
        verify(natAPIManMock, never()).createVMClone(any(VirtualMachine.class),
                any(CloneType.class));
    }

    /**
     * This test tests that there is invoked UnknownVirtualMachineException when
     * there is made an attempt to clone a virtual machine which does not exist.
     */
    @Test
    public void cloneNonexistentVirtualMachine(){
        //represents a virtual machine which should be cloned
        VirtualMachine origVM = new VMBuilder().build();
        //mock object of type UnknownVirtualMachineException for easier and better test control
        UnknownVirtualMachineException unVirtMachExMock =
                mock(UnknownVirtualMachineException.class);

        //means that the host machine is connected and therefore the virtual machine can be cloned
        when(conManMock.isConnected(hostMachine)).thenReturn(true);
        //there should be thrown UnknownVirtualMachineException exception when the method
        //NativeVBoxAPIManager::createVirtualMachineClone() is called with a required virtual machine
        //and means the virtual machine does not exist and cannot be cloned
        doThrow(unVirtMachExMock).when(natAPIManMock).createVMClone(origVM,
                CloneType.FULL_FROM_ALL_STATES);

        exception.expect(UnknownVirtualMachineException.class);
        sut.cloneVirtualMachine(origVM, CloneType.FULL_FROM_ALL_STATES);
    }

    /**
     * This test tests that there is invoked UnexpectedVMStateException when
     * there is made an attempt to clone an inaccessible virtual machine.
     */
    @Test
    public void cloneInaccessibleVirtualMachine(){
        //represents a virtual machine which should be cloned
        VirtualMachine origVM = new VMBuilder().build();
        //mock object of type UnexpectedVMStateException for easier and better test control
        UnexpectedVMStateException unexVMStateMock = mock(UnexpectedVMStateException.class);

        //means that the host machine is connected and therefore the virtual machine can be cloned
        when(conManMock.isConnected(hostMachine)).thenReturn(true);
        //there should be thrown UnexpectedVMStateException exception when the method
        //NativeVBoxAPIManager::createVirtualMachineClone() is called with a required virtual machine
        //and means the virtual machine is not accessible and cannot be cloned
        doThrow(unexVMStateMock).when(natAPIManMock).createVMClone(origVM,
                CloneType.FULL_FROM_ALL_STATES);
        
        exception.expect(UnexpectedVMStateException.class);
        sut.cloneVirtualMachine(origVM, CloneType.FULL_FROM_ALL_STATES);
    }

    /**
     * This test tests that there is invoked UnexpectedVMStateException when
     * there is made an attempt to clone a virtual machine which is not in a
     * state required for a virtual machine cloning operaiton.
     */
    @Test
    public void cloneVirtualMachineWithIncorrectState(){
        //represents a virtual machine which should be cloned
        VirtualMachine origVM = new VMBuilder().build();
        //mock object of type UnexpectedVMStateException for easier and better test control
        UnexpectedVMStateException unexVMStateMock = mock(UnexpectedVMStateException.class);

        //means that the host machine is connected and therefore the virtual machine can be cloned
        when(conManMock.isConnected(hostMachine)).thenReturn(true);
        //there should be thrown UnexpectedVMStateException exception when the method
        //NativeVBoxAPIManager::createVirtualMachineClone() is called with a required virtual machine
        //and means the virtual machine is not in a required state for cloning and cannot be cloned
        doThrow(unexVMStateMock).when(natAPIManMock).createVMClone(origVM,
                CloneType.FULL_FROM_ALL_STATES);
        
        exception.expect(UnexpectedVMStateException.class);
        sut.cloneVirtualMachine(origVM, CloneType.FULL_FROM_ALL_STATES);
    }

    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to clone a virtual machine a physical machine
     * with which the connection is lost while the virtual machine cloning
     * operation is being processed.
     */
    @Test
    public void cloneVirtualMachineWithSuddenNetworkConnectionLoss(){
        //represents a virtual machine which should be cloned
        VirtualMachine origVM = new VMBuilder().build();
        //mock object of type ConnectionFailureException for easier and better test control
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);

        //means that the hostMachine is connected and therefore the virtual machine can be cloned
        when(conManMock.isConnected(hostMachine)).thenReturn(true);
        //there should be thrown ConnectionFailureException exception when the method
        //NativeVBoxAPIManager::createVirtualMachineClone() is called and means there
        //occured any connection problem when there was being worked with VirtualBox on the host machine
        doThrow(conFailExMock).when(natAPIManMock).createVMClone(origVM,
                CloneType.FULL_FROM_ALL_STATES);

        exception.expect(ConnectionFailureException.class);
        sut.cloneVirtualMachine(origVM, CloneType.FULL_FROM_ALL_STATES);
        
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMock).disconnectFrom(hostMachine);
    }
    
    /**
     * This test tests that if the method VirtualizationToolManagerImpl::close()
     * is called and no error occurs during its execution, then all running
     * virtual machines will be shut down.
     * 
     * @throws Exception 
     */
    @Test
    public void closeWithNonemptyVMsListAndSomeRunningVM() throws Exception {
        VirtualMachine vm1 = new VMBuilder().build();
        VirtualMachine vm2 = new VMBuilder().name("VirtualMachine_02")
                                            .id(UUID.fromString("002d084a-0189-4a55-9ab7-531c455570a1"))
                                            .build();
        List<VirtualMachine> vms = Arrays.asList(vm1,vm2);
        
        when(conManMock.isConnected(hostMachine)).thenReturn(true);
        when(natAPIManMock.getAllVirtualMachines(hostMachine)).thenReturn(vms);
        when(natAPIMachMock.getVMState(vm1)).thenReturn("PoweredOff");
        when(natAPIMachMock.getVMState(vm2)).thenReturn("Running");
        
        sut.close();
        
        assertFalse("There should be a message on an output stream that the closing operation "
                + "is being performed and that it finished successfully", outContent.toString().isEmpty());
        assertTrue("There should not be any error message on an error output stream", errContent.toString().isEmpty());
        verify(natAPIMachMock, never()).shutDownVM(vm1);
        verify(natAPIMachMock).shutDownVM(vm2);
    }
    
    /**
     * This test tests that if the method VirtualizationToolManagerImpl::close()
     * is called and no error occurs during its execution, then all virtual
     * machines are checked if they are running, but because there is no running
     * VM, there should not be performed any further action.
     */
    @Test
    public void closeWithNonemptyVMsListAndNoRunningVM(){
        VirtualMachine vm1 = new VMBuilder().build();
        VirtualMachine vm2 = new VMBuilder().name("VirtualMachine_02")
                                            .id(UUID.fromString("002d084a-0189-4a55-9ab7-531c455570a1"))
                                            .build();
        List<VirtualMachine> vms = Arrays.asList(vm1,vm2);
        
        when(conManMock.isConnected(hostMachine)).thenReturn(true);
        when(natAPIManMock.getAllVirtualMachines(hostMachine)).thenReturn(vms);
        when(natAPIMachMock.getVMState(vm1)).thenReturn("PoweredOff");
        when(natAPIMachMock.getVMState(vm2)).thenReturn("Saved");        
        
        sut.close();
        
        assertFalse("There should be a message on an output stream that the closing operation "
                + "is being performed and that it finished successfully", outContent.toString().isEmpty());
        assertTrue("There should not be any error message on an error output stream", errContent.toString().isEmpty());
        verify(natAPIMachMock, never()).shutDownVM(any(VirtualMachine.class));
    }
    
    /**
     * This test tests that if the method VirtualizationToolManagerImpl::close()
     * is called for physical machine on which there are not virtual machines
     * then the closing operation is successfully finished.
     */
    @Test
    public void closeWithEmptyVMsList() {
        when(conManMock.isConnected(hostMachine)).thenReturn(true);
        when(natAPIManMock.getAllVirtualMachines(hostMachine)).thenReturn(new ArrayList<>());
        
        sut.close();
        
        assertFalse("There should be a message on an output stream that the closing operation "
                + "is being performed and that it finished successfully", outContent.toString().isEmpty());
        assertTrue("There should not be any error message on an error output stream", errContent.toString().isEmpty());
        verify(natAPIMachMock, never()).getVMState(any(VirtualMachine.class));
        verify(natAPIMachMock, never()).shutDownVM(any(VirtualMachine.class));
    }
    
    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to perform virtualization tool closing operation
     * for a physical machine which is not connected.
     */
    @Test
    public void closeOnDisconnectedPhysicalMachine(){
        when(conManMock.isConnected(hostMachine)).thenReturn(false);
        
        exception.expect(ConnectionFailureException.class);
        sut.close();
        
        verify(natAPIManMock, never()).getAllVirtualMachines(any(PhysicalMachine.class));
        verify(natAPIMachMock, never()).getVMState(any(VirtualMachine.class));
        verify(natAPIMachMock, never()).shutDownVM(any(VirtualMachine.class));
    }
    
    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to perform virtualization tool closing operation
     * for a physical machine with which the connection is lost while the
     * closing operation is being processed.
     */
    @Test
    public void closeWithSuddenConnectionLossDuringAllVMsRetrieveOperation(){
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);
        
        when(conManMock.isConnected(hostMachine)).thenReturn(true, true, false);
        doThrow(conFailExMock).when(natAPIManMock).getAllVirtualMachines(hostMachine);
        
        exception.expect(ConnectionFailureException.class);
        sut.close();
        
        verify(conManMock).disconnectFrom(hostMachine);
        verify(natAPIMachMock, never()).getVMState(any(VirtualMachine.class));
        verify(natAPIMachMock, never()).shutDownVM(any(VirtualMachine.class));
    }
    
    /**
     * This test tests that there is invoked UnknownVirtualMachineException when
     * there is made an attempt to perform virtualization tool closing operation,
     * but during all virtual machine retrieve operation occurs any error of
     * virtualization tool VirtualBox associated with virtual machines.
     */
    @Test
    public void closeWithVirtualBoxErrorDuringAllVMsRetrieveOperation(){
        UnknownVirtualMachineException unVirtMachExMock = 
                mock(UnknownVirtualMachineException.class);
        
        when(conManMock.isConnected(hostMachine)).thenReturn(true);
        doThrow(unVirtMachExMock).when(natAPIManMock).getAllVirtualMachines(hostMachine);
        
        exception.expect(UnknownVirtualMachineException.class);
        sut.close();
        
        verify(natAPIMachMock, never()).getVMState(any(VirtualMachine.class));
        verify(natAPIMachMock, never()).shutDownVM(any(VirtualMachine.class));
    }
    
    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to perform virtualization tool closing operation,
     * but the connection with the physical machine, on which is located the
     * virtualization tool, for which is being performed closing operation,
     * is lost while virtual machine state query operation is being processed.
     */
    @Test
    public void closeWithSuddenConnectionLossDuringVMStateRetrieveOperation(){
        VirtualMachine vm1 = new VMBuilder().build();
        VirtualMachine vm2 = new VMBuilder().name("VirtualMachine_02")
                                            .id(UUID.fromString("002d084a-0189-4a55-9ab7-531c455570a1"))
                                            .build();
        List<VirtualMachine> vms = Arrays.asList(vm1,vm2);
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);
        
        when(conManMock.isConnected(hostMachine)).thenReturn(true, true, true, false);
        when(natAPIManMock.getAllVirtualMachines(hostMachine)).thenReturn(vms);
        doThrow(conFailExMock).when(natAPIMachMock).getVMState(vm1);
        
        exception.expect(ConnectionFailureException.class);
        sut.close();
        
        verify(conManMock).disconnectFrom(hostMachine);
        verify(natAPIMachMock, never()).getVMState(vm2);
        verify(natAPIMachMock, never()).shutDownVM(any(VirtualMachine.class));
    }
    
    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to perform virtualization tool closing operation,
     * but the connection with the physical machine, on which is located the
     * virtualization tool, for which is being performed closing operation,
     * is lost while virtual machine shut down operation is being processed.
     */
    @Test
    public void closeWithSuddenConnectionLossDuringVMShutdownOperation(){
        VirtualMachine vm1 = new VMBuilder().build();
        VirtualMachine vm2 = new VMBuilder().name("VirtualMachine_02")
                                            .id(UUID.fromString("002d084a-0189-4a55-9ab7-531c455570a1"))
                                            .build();
        List<VirtualMachine> vms = Arrays.asList(vm1,vm2);
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);
        
        when(conManMock.isConnected(hostMachine)).thenReturn(true, true, true, true, true, true, false);
        when(natAPIManMock.getAllVirtualMachines(hostMachine)).thenReturn(vms);
        when(natAPIMachMock.getVMState(vm1)).thenReturn("Running");
        when(natAPIMachMock.getVMState(vm2)).thenReturn("Running");
        doThrow(conFailExMock).when(natAPIMachMock).getVMState(vm2);
        
        exception.expect(ConnectionFailureException.class);
        sut.close();
        
        verify(natAPIMachMock).shutDownVM(vm1);
        verify(conManMock).disconnectFrom(hostMachine);
        verify(natAPIMachMock, never()).shutDownVM(vm2);
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
