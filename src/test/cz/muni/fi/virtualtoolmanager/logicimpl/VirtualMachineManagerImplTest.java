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

import cz.muni.fi.virtualtoolmanager.pubapi.io.OutputHandler;
import cz.muni.fi.virtualtoolmanager.pubapi.entities.PhysicalMachine;
import cz.muni.fi.virtualtoolmanager.pubapi.entities.PortRule;
import cz.muni.fi.virtualtoolmanager.pubapi.entities.VirtualMachine;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnexpectedVMStateException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownPortRuleException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownVirtualMachineException;
import cz.muni.fi.virtualtoolmanager.pubapi.types.FrontEndType;
import cz.muni.fi.virtualtoolmanager.pubapi.types.ProtocolType;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * This test class ensures unit testing of class VirtualMachineManagerImpl and
 * is intended to be a pointer that class VirtualMachineManagerImpl works as
 * expected.
 *
 * @author Tomáš Šmíd
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({VirtualMachineManagerImpl.class, NativeVBoxAPIMachine.class, ConnectionManagerImpl.class})
public class VirtualMachineManagerImplTest {

    @Rule
    ExpectedException exception = ExpectedException.none();
    
    private VirtualMachineManagerImpl sut;
    private NativeVBoxAPIMachine natAPIMachMock;
    private ConnectionManagerImpl conManMock;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    @Before
    public void setUp() throws Exception {
        natAPIMachMock = mock(NativeVBoxAPIMachine.class);
        conManMock = mock(ConnectionManagerImpl.class);
        whenNew(NativeVBoxAPIMachine.class).withNoArguments().thenReturn(natAPIMachMock);
        whenNew(ConnectionManagerImpl.class).withNoArguments().thenReturn(conManMock);
        sut = new VirtualMachineManagerImpl();
        OutputHandler.setErrorOutputStream(new PrintStream(errContent));
        OutputHandler.setOutputStream(new PrintStream(outContent));
    }

    @After
    public void cleanUp() {
        OutputHandler.setErrorOutputStream(null);
        OutputHandler.setOutputStream(null);
    }

    /**
     * This test tests that if there are all neccessary conditions for virtual
     * machine start-up met then the virtual machine is successfully started and
     * on a standard output appears an informing message.
     */
    @Test
    public void startVMIdealCase() {
        //represents a virtual machine which should be started
        VirtualMachine vm = new VMBuilder().build();

        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);

        //there should not appear any exception nor error
        sut.startVM(vm, FrontEndType.GUI);

        assertFalse("There should be written a message on a standard output that the virtual "
                + "machine was successfully started", outContent.toString().isEmpty());
        assertTrue("There should not be written a message on a standard error output", errContent.toString().isEmpty());
    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to start a null virtual machine.
     */
    @Test
    public void startNullVM(){
        exception.expect(IllegalArgumentException.class);
        sut.startVM(null, FrontEndType.GUI);
        
        //checks the method NativeVBoxAPIMachine::startVM() has never been called as expected
        verify(natAPIMachMock, never()).startVM(any(VirtualMachine.class), any(FrontEndType.class));
    }
    
    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to start a virtual machine with a null front-end type.
     */
    @Test
    public void startVMWithNullFrontEndType(){
        //represents a virtual machine which should be started
        VirtualMachine vm = new VMBuilder().build();
        
        exception.expect(IllegalArgumentException.class);
        sut.startVM(vm, null);
        
        //checks the method NativeVBoxAPIMachine::startVM() has never been called as expected
        verify(natAPIMachMock, never()).startVM(any(VirtualMachine.class), any(FrontEndType.class));
    }

    /**
     * This test tests that there is invoked UnknownVirtualMachineException when
     * there is made an attempt to start a non-existent virtual machine.
     */
    @Test
    public void startNonexistentVM(){
        //represents a virtual machine which should be started
        VirtualMachine vm = new VMBuilder().build();
        UnknownVirtualMachineException unknownVMExMock =
                mock(UnknownVirtualMachineException.class);

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
        doThrow(unknownVMExMock).when(natAPIMachMock).startVM(vm, FrontEndType.GUI);

        exception.expect(UnknownVirtualMachineException.class);
        sut.startVM(vm, FrontEndType.GUI);
    }

    /**
     * This test tests that there is invoked UnexpectedVMStateException when there
     * is made an attempt to start an inaccessible virtual machine.
     */
    @Test
    public void startInaccessibleVM(){
        //represents a virtual machine which should be started
        VirtualMachine vm = new VMBuilder().build();
        UnexpectedVMStateException unexpVMStateMock =
                mock(UnexpectedVMStateException.class);
        
       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
        doThrow(unexpVMStateMock).when(natAPIMachMock).startVM(vm, FrontEndType.GUI);
        
        exception.expect(UnexpectedVMStateException.class);
        sut.startVM(vm, FrontEndType.GUI);
    }

    /**
     * This test tests that there is invoked UnexpectedVMStateException when there
     * is made an attempt to start a virtual machine which is not in a required
     * state for virtual machine starting operation.
     */
    @Test
    public void startVMWithInvalidState(){
        //represents a virtual machine which should be started
        VirtualMachine vm = new VMBuilder().build();
        //mock object of type UnexpectedVMStateException for better test control
        UnexpectedVMStateException unexVMStateMock = mock(UnexpectedVMStateException.class);
        
       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the UnexpectedVMStateException exception when
        //the method NativeVBoxAPIMachine::startVM() is called with a required virtual machine
        //and means that the virtual machine has already been started, because the only
        //invalid states are "Running" and "Paused" and therefore cannot be started again
        doThrow(unexVMStateMock).when(natAPIMachMock).startVM(vm, FrontEndType.GUI);

        exception.expect(UnexpectedVMStateException.class);
        sut.startVM(vm, FrontEndType.GUI);
    }

    /**
     * This test tests that there is invoked UnexpectedVMStateException when
     * there is made an attempt to start a virtual machine which is already
     * locked (there exists another process which is working with the virtual
     * machine at the same moment).
     */
    @Test
    public void startAlreadyLockedVM(){
        //represents a virtual machine which should be started
        VirtualMachine vm = new VMBuilder().build();
        //mock object of type UnexpectedVMStateException for better test control
        UnexpectedVMStateException unexVMStateMock = mock(UnexpectedVMStateException.class);

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the UnexpectedVMStateException exception when
        //the method NativeVBoxAPIMachine::startVM() is called with a required virtual machine
        //and means that the virtual machine is being used by another process now
        //which has the lock on the virtual machine
        doThrow(unexVMStateMock).when(natAPIMachMock).startVM(vm, FrontEndType.GUI);

        exception.expect(UnexpectedVMStateException.class);
        sut.startVM(vm, FrontEndType.GUI);
    }

    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to start a virtual machine which is located on
     * a not connected physical machine.
     */
    @Test
    public void startVMOnDisconnectedPhysicalMachine(){
        //represents a virtual machine which should be started
        VirtualMachine vm = new VMBuilder().build();

       //there should be returned a negative answer which means the host machine
        //is not connected and therefore there is not possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(false);
        
        exception.expect(ConnectionFailureException.class);
        sut.startVM(vm, FrontEndType.GUI);
        
        //checks the method NativeVBoxAPIMachine::startVM() has never been called as expected
        verify(natAPIMachMock, never()).startVM(any(VirtualMachine.class), any(FrontEndType.class));
    }

    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there occurs some connection problem during virtual machine starting
     * operation. Also check the disconnection operation is called as a reaction
     * to the connection failure.
     */
    @Test
    public void startVMWithSuddenNetworkConnectionLoss() throws Exception {
        //represents a virtual machine which should be started
        VirtualMachine vm = new VMBuilder().build();
        //mock object of type ConnectionfailureException for better test control
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the ConnectionFailureException exception when
        //the method NativeVBoxAPIMachine::startVM() is called with a required virtual machine
        //and means that there occured any connection problem at the beginning
        //of the start-up operation
        doThrow(conFailExMock).when(natAPIMachMock).startVM(vm, FrontEndType.GUI);

        exception.expect(ConnectionFailureException.class);
        sut.startVM(vm, FrontEndType.GUI);
        
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMock).disconnectFrom(vm.getHostMachine());
    }

    /**
     * This test tests that if there are all neccessary conditions for virtual
     * machine shutdown operation met then the operation is performed successfully
     * and on a standard output appears an informing message about that.
     */
    @Test
    public void shutDownVMIdealCase() {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();

        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);

        //there should not appear any exception nor error
        sut.shutDownVM(vm);

        assertFalse("There should be written a message on a standard output that the virtual "
                + "machine was successfully shut down", outContent.toString().isEmpty());
        assertTrue("There should not be written a message on a standard error output", errContent.toString().isEmpty());
        
    }
    
    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to shut down a null virtual machine.
     */
    @Test
    public void shutDownNullVM(){
        exception.expect(IllegalArgumentException.class);
        sut.shutDownVM(null);
        
        //checks the method NativeVBoxAPIMachine::shutDownVM() has never been called as expected
        verify(natAPIMachMock, never()).shutDownVM(any(VirtualMachine.class));
    }

    /**
     * This test tests that there is invoked UneknownVirtualMachineException
     * when there is made an attempt to shut down an unknown virtual machine.
     */
    @Test
    public void shutDownNonexistentVM(){
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();
        //mock object of type UnknownVirtualMachineException for better test control
        UnknownVirtualMachineException unVirtMachExMock = mock(UnknownVirtualMachineException.class);

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the UnknownVirtualMachineException exception when
        //the method NativeVBoxAPIMachine::shutDownVM() is called with a required virtual machine
        //and means that the virtual machine does not exist and cannot be shut down
        doThrow(unVirtMachExMock).when(natAPIMachMock).shutDownVM(vm);

        exception.expect(UnknownVirtualMachineException.class);
        sut.shutDownVM(vm);
    }

    /**
     * This test tests that there is invoked UnexpectedVMStateException when
     * there is made an attempt to shut down an inaccessible virtual machine.
     */
    @Test
    public void shutDownInaccessibleVM(){
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();
        //mock object of type UnexpectedVMStateException for better test control
        UnexpectedVMStateException unexVMStateExMock = mock(UnexpectedVMStateException.class);

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the UnexpectedVMStateException exception when
        //the method NativeVBoxAPIMachine::shutDownVM() is called with a required virtual machine
        //and means that the virtual machine's source files are missing or corrupted
        //and cannot be shut down
        doThrow(unexVMStateExMock).when(natAPIMachMock).shutDownVM(vm);

        exception.expect(UnexpectedVMStateException.class);
        sut.shutDownVM(vm);
    }

    /**
     * This test tests that there is invoked UnexpectedVMStateException when there
     * is made an attempt to shut down a virtual machine which is not in a required
     * state for virtual machine shutdown operation.
     */
    @Test
    public void shutDownVMWithInvalidState(){
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();
        //mock object of type UnexpectedVMStateException for better test control
        UnexpectedVMStateException unexVMStateExMock = mock(UnexpectedVMStateException.class);

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the UnexpectedVMStateException exception when
        //the method NativeVBoxAPIMachine::shutDownVM() is called with a required virtual machine
        //and means that the virtual machine is not ready to be shut down or it is already powered off,
        //because the only valid states are "Running", "Paused" or "Stuck" and all another states are
        //invalid
        doThrow(unexVMStateExMock).when(natAPIMachMock).shutDownVM(vm);
        
        exception.expect(UnexpectedVMStateException.class);
        sut.shutDownVM(vm);
    }
    
    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to shut down a virtual machine which is located
     * on a not connected physical machine.
     */
    @Test
    public void shutDownVMOnDisconnectedPhysicalMachine(){
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();

       //there should be returned a negative answer which means the host machine
        //is not connected and therefore there is not possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(false);
        
        exception.expect(ConnectionFailureException.class);
        sut.shutDownVM(vm);
        
        //checks the method NativeVBoxAPIMachine::shutDownVM() has never been called as expected
        verify(natAPIMachMock, never()).shutDownVM(any(VirtualMachine.class));
    }

    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there occurs some connection problem during virtual machine shutdown
     * operation. Also check the disconnection operation is called as a reaction
     * to the connection problem.
     */
    @Test
    public void shutDownVMWithSuddenNetworkConnectionLoss(){
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();
        //mock object of type ConnectionFailureException for better test control
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the ConnectionFailureException exception when
        //the method NativeVBoxAPIMachine::shutDownVM() is called with a required virtual machine
        //and means that there occured any connection problem at the beginning
        //of the shutdown operation
        doThrow(conFailExMock).when(natAPIMachMock).shutDownVM(vm);

        exception.expect(ConnectionFailureException.class);
        sut.shutDownVM(vm);
        
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMock).disconnectFrom(vm.getHostMachine());
    }

    /**
     * This test tests that if there are all neccessary conditions for new
     * port rule addition operation met then the operation is performed successfully
     * and on a standard output appears an informing message about that.
     */
    @Test
    public void addPortRuleIdealCase(){
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();
       
       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be returned an empty list of used port rules in order to
        //fasten the port rule name and host port duplicity check
        when(natAPIMachMock.getPortRules(vm)).thenReturn(new ArrayList<>());
        
        //there should not appear any exception nor error
        sut.addPortRule(vm,portRule);

        assertFalse("There should be written a message on a standard output that the particular "
                + "port rule was successfully added to the particular virtual machine", outContent.toString().isEmpty());
        assertTrue("There should not be written a message on a standard error output", errContent.toString().isEmpty());
    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to add a new port-forwarding rule to a null virtual
     * machine.
     */
    @Test
    public void addPortRuleWithNullVirtualMachine() throws Exception {
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();
        
        exception.expect(IllegalArgumentException.class);
        sut.addPortRule(null, portRule);
        
        //checks the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMock, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
    }
    
    /**
     * This test tests that there is invoked UnknownVirtualMachineException when
     * there is made an attempt to get all port rules from a non-existent VM for
     * check duplicity reasons.
     */
    @Test
    public void addPortRuleWithNonexistentVMGetter(){
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();
        //mock object of type UnknownVirtualMachineException for better test control
        UnknownVirtualMachineException unVirtMachExMock = mock(UnknownVirtualMachineException.class);
        
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be thrown the UnknownVirtualMachineException exception when
        //the method NativeVBoxAPIMachine::getPortRules() is called with a required
        //virtual machine and means that the virtual machine does not exist and
        //port rules cannot got and used for new port rule name and host port duplicity
        doThrow(unVirtMachExMock).when(natAPIMachMock).getPortRules(vm);
        
        exception.expect(UnknownVirtualMachineException.class);
        sut.addPortRule(vm, portRule);
        
        //check the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMock, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
    }

    /**
     * This test tests that there is invoked UnknownVirtualMachineException when
     * there is made an attempt to add a new port-forwarding rule to an unknown
     * virtual machine.
     */
    @Test
    public void addPortRuleWithNonexistentVMAddition(){
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();
        //mock object of type UnknownVirtualMachineException for better test control
        UnknownVirtualMachineException unVirtMachExMock = mock(UnknownVirtualMachineException.class);
        
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be thrown the UnknownVirtualMachineException exception when
        //the method NativeVBoxAPIMachine::addPortRule() is called with a required
        //virtual machine and port rule and means that the virtual machine does not
        //exist and the port rule cannot be added to that virtual machine
        doThrow(unVirtMachExMock).when(natAPIMachMock).addPortRule(vm, portRule);
        
        exception.expect(UnknownVirtualMachineException.class);
        sut.addPortRule(vm, portRule);
    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to add a null port rule as a new port-forwarding rule
     * to a virtual machine.
     */
    @Test
    public void addNullPortRule(){
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        
        exception.expect(IllegalArgumentException.class);
        sut.addPortRule(vm, null);
        
        //checks the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMock, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to add a port rule with no specified (null) name as a
     * new port-forwarding rule to a virtual machine.
     */
    @Test
    public void addPortRuleWithNullName(){
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder(null, 22, 1540).build();
        
        exception.expect(IllegalArgumentException.class);
        sut.addPortRule(vm, portRule);
        
        //checks the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMock, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to add a port rule with an empty name as a new
     * port-forwarding rule to a virtual machine.
     */
    @Test
    public void addPortRuleWithEmptyName(){
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("", 22, 1540).build();
    
        exception.expect(IllegalArgumentException.class);
        sut.addPortRule(vm, portRule);
        
        //checks the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMock, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to add a port rule with a negative host port number as
     * a new port-forwarding rule to a virtual machine.
     */
    @Test
    public void addPortRuleWithInvalidHostPortNumberNegative(){
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01", -1, 1540).build();
    
        exception.expect(IllegalArgumentException.class);
        sut.addPortRule(vm, portRule);
        
        //checks the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMock, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
    }
    
    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to add a port rule with a host port number equal to 0
     * as a new port-forwarding rule to a virtual machine.
     */
    @Test
    public void addPortRuleWithInvalidHostPortNumberZero(){
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01", 0, 1540).build();
        
        exception.expect(IllegalArgumentException.class);
        sut.addPortRule(vm, portRule);
        
        //checks the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMock, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to add a port rule with a too big host port number
     * (bigger than 65535) as a new port-forwarding rule to a virtual machine.
     */
    @Test
    public void addPortRuleWithInvalidHostPortNumberTooBig(){
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01", 65536, 1540).build();
        
        exception.expect(IllegalArgumentException.class);
        sut.addPortRule(vm, portRule);
        
        //checks the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMock, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to add a port rule with a negative guest port number as
     * a new port-forwarding rule to a virtual machine.
     */
    @Test
    public void addPortRuleWithInvalidGuestPortNumberNegative(){
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01", 22, -1).build();
        
        exception.expect(IllegalArgumentException.class);
        sut.addPortRule(vm, portRule);
        
        //checks the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMock, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
    }
    
    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to add a port rule with a guest port number equal to 0
     * as a new port-forwarding rule to a virtual machine.
     */
    @Test
    public void addPortRuleWithInvalidGuestPortNumberZero(){
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01", 22, 0).build();
        
        exception.expect(IllegalArgumentException.class);
        sut.addPortRule(vm, portRule);
        
        //checks the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMock, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to add a port rule with a too big host port number
     * (bigger than 65535) as a new port-forwarding rule to a virtual machine.
     */
    @Test
    public void addPortRuleWithInvalidGuestPortNumberTooBig(){
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01", 22, 65536).build();
        
        exception.expect(IllegalArgumentException.class);
        sut.addPortRule(vm, portRule);
        
        //checks the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMock, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to add a port rule with a duplicit name (name, which
     * is used for another already existing port-forwarding rule) as a new
     * port-forwarding rule to a virtual machine.
     */
    @Test
    public void addPortRuleWithDuplicitName(){
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();
        //represents a non=empty list of used port rules on virtual machine vm
        List<String> portRules = Arrays.asList("PortRule_01,TCP,,44,,19540");
        
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be returned a non-empty list of used port rules with one
        //port rule which has the same name as a port rule which should be newly added
        when(natAPIMachMock.getPortRules(vm)).thenReturn(portRules);
        
        exception.expect(IllegalArgumentException.class);
        sut.addPortRule(vm, portRule);
        
        //checks the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMock, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to add a port rule with a duplicit host port number
     * (port number, which is used for another already existing port-forwarding
     * rule) as a new port-forwarding rule to a virtual machine.
     */
    @Test
    public void addPortRuleWithDuplicitHostPortNumber(){
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();
        //represents a non=empty list of used port rules on virtual machine vm
        List<String> portRules = Arrays.asList("PortRule_02,UDP,,22,,19540");
        
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be returned a non-empty list of used port rules with one
        //port rule which has the same host port number as a port rule which should be newly added
        when(natAPIMachMock.getPortRules(vm)).thenReturn(portRules);
        
        exception.expect(IllegalArgumentException.class);
        sut.addPortRule(vm, portRule);
        
        //checks the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMock, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
    }

    /**
     * This test tests that there is invoked UnexpectedVMStateException when
     * there is made an attempt to add a new port-forwarding rule to a virtual
     * machine which is not using NAT network adapter.
     */
    @Test
    public void addPortRuleWithInvalidAttachmentType(){
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();
        //mock object of type UnexpectedVMStateException for better test control
        UnexpectedVMStateException unexVMStateExMock = mock(UnexpectedVMStateException.class);
        
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be returned an empty list of used port rules in order to
        //fasten the port rule name and host port duplicity check
        when(natAPIMachMock.getPortRules(vm)).thenReturn(new ArrayList<>());
        //there should returned the UnexpectedVMStateException exception when the method
        //NativeVBoxAPIMachine::addPortRule() is called with a required virtual machine
        //which has not set up a valid network attachment type
        doThrow(unexVMStateExMock).when(natAPIMachMock).addPortRule(vm, portRule);
        
        exception.expect(UnexpectedVMStateException.class);
        sut.addPortRule(vm, portRule);
    }
    
    /**
     * This test tests that there is invoked UnexpectedVMStateException when
     * there is made an attempt to add a new port-forwarding rule to a virtual
     * machine which is not using NAT network adapter and this state is found
     * during retrieving all existing port rules for name and host port number
     * duplicity check.
     */
    @Test
    public void addPortRuleWithInvalidAttachmentTypeGetter(){
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();
        //mock object of type UnexpectedVMStateException for better test control
        UnexpectedVMStateException unexVMStateExMock = mock(UnexpectedVMStateException.class);
        
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be returned an empty list of used port rules in order to
        //fasten the port rule name and host port duplicity check
        when(natAPIMachMock.getPortRules(vm)).thenReturn(new ArrayList<>());
        //there should returned the UnexpectedVMStateException exception when the method
        //NativeVBoxAPIMachine::addPortRule() is called with a required virtual machine
        //which has not set up a valid network attachment type
        doThrow(unexVMStateExMock).when(natAPIMachMock).addPortRule(vm, portRule);
        
        exception.expect(UnexpectedVMStateException.class);
        sut.addPortRule(vm, portRule);
        
        verify(natAPIMachMock, never()).addPortRule(vm, portRule);
    }
    
    /**
     * This test tests that there is invoked ConnectionFailureException when there
     * is made an attempt to add a new port-forwarding rule to a virtual machine
     * whose host machine is not connected.
     */
    @Test
    public void addPortRuleOnDisconnectedPhysicalMachine(){
        //represents a virtual machine to which should be added a new port rule        
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();

       //there should be returned a negative answer which means the host machine
        //is not connected and therefore there is not possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(false);
        
        exception.expect(ConnectionFailureException.class);
        sut.addPortRule(vm, portRule);
        
        //checks the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMock, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
    }
    
    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to add a new port-forwarding rule to a virtual
     * machine with whose host machine is lost the connection during the retrieving
     * all port rules for name and host port number duplicity check. Also check
     * there is performed disconnection operation as a reaction to the connection
     * failure.
     */
    @Test
    public void addPortRuleWithSuddenNetworkConnectionLossInnerCheckGetter(){        
        //represents a virtual machine to which should be added a new port rule
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();
        //mock object of type ConnectionFailureException for better test control
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the ConnectionFailureException exception when
        //the method NativeVBoxAPIMachine::getPortRules() is called with a required virtual machine
        //and means that there occured any connection problem and port rules cannot be retrived
        doThrow(conFailExMock).when(natAPIMachMock).getPortRules(vm);

        exception.expect(ConnectionFailureException.class);
        sut.addPortRule(vm, portRule);
        
        //checks the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMock, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMock).disconnectFrom(vm.getHostMachine());
    }

    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to add a new port-forwarding rule to a virtual
     * machine with whose host machine is lost a connection while there is being
     * processed the new port-forwarding rule addition operation itself. Also
     * check there is performed disconnection operation as a reaction to the
     * connection failure.
     */
    @Test
    public void addPortRuleWithSuddenNetworkConnectionLossInnerCheckAddition(){
        //represents a virtual machine to which should be added a new port rule
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();
        //mock object of type ConnectionFailureException for better test control
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the ConnectionFailureException exception when
        //the method NativeVBoxAPIMachine::addPortRule() is called with a required virtual machine
        //and means that there occured any connection problem and a new port rule cannot be added
        //to the virtual machine
        doThrow(conFailExMock).when(natAPIMachMock).addPortRule(vm, portRule);

        exception.expect(ConnectionFailureException.class);
        sut.addPortRule(vm, portRule);
        
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMock).disconnectFrom(vm.getHostMachine());
    }
    
    /**
     * This test tests that if there are all neccessary conditions for port rule
     * deletion operation met then the port rule deletion operation is performed
     * successfully and on a standard output appears an informing message about that.
     */
    @Test
    public void deletePortRuleIdealCase() {
        //represents a virtual machine from which a port rule should be deleted
        VirtualMachine vm = new VMBuilder().build();
        //represents a port rule which should be deleted from the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();
       
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
        
        //there should not appear any exception nor error
        sut.deletePortRule(vm, portRule);

        assertFalse("There should be written a message on a standard output that"
                + "the particular port rule was successfully deleted  from the "
                + "particular virtual machine", outContent.toString().isEmpty());
        assertTrue("There should not be written a message on a standard error "
                + "output", errContent.toString().isEmpty());
    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to delete a port rule from a null virtual machine.
     */
    @Test
    public void deletePortRuleWithNullVirtualMachine(){
        //represents a new port rule which should be deleted from the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();
        
        exception.expect(IllegalArgumentException.class);
        sut.deletePortRule(null, portRule);
        
        //checks the method NativeVBoxAPIMachine::deletePortRule() has never been called as expected
        verify(natAPIMachMock, never()).deletePortRule(any(VirtualMachine.class), anyString());
    }

    /**
     * This test tests that there is invoked UnknownVirtualMachineException when
     * there is made an attempt to delete a port rule from a non-existent
     * virtual machine.
     */
    @Test
    public void deletePortRuleWithNonexistentVM(){
        //represents a virtual machine from which a port rule should be deleted
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be deleted from the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();
        UnknownVirtualMachineException unknownVMExMock =
                mock(UnknownVirtualMachineException.class);
        
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
        doThrow(unknownVMExMock).when(natAPIMachMock).deletePortRule(vm, portRule.getName());
        
        exception.expect(UnknownVirtualMachineException.class);
        sut.deletePortRule(vm, portRule);
    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to delete a null port rule from a virtual machine.
     */
    @Test
    public void deleteNullPortRule(){
        //represents a virtual machine from which a port rule should be deleted
        VirtualMachine vm = new VMBuilder().build();
        
        exception.expect(IllegalArgumentException.class);
        sut.deletePortRule(vm, null);
        
        //checks the method NativeVBoxAPIMachine::deletePortRule() has never been called as expected
        verify(natAPIMachMock, never()).deletePortRule(any(VirtualMachine.class), anyString());
    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to delete a port rule with a null name from a virtual
     * machine.
     */
    @Test
    public void deletePortRuleWithNullName(){
        //represents a virtual machine from which a port rule should be deleted
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be deleted from the virtual machine vm
        PortRule portRule = new PortRule.Builder(null,22,1540).build();
        
        exception.expect(IllegalArgumentException.class);
        sut.deletePortRule(vm, portRule);
        
        //checks the method NativeVBoxAPIMachine::deletePortRule() has never been called as expected
        verify(natAPIMachMock, never()).deletePortRule(any(VirtualMachine.class), anyString());
    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to delete a port rule with an empty name from a virtual
     * machine.
     */
    @Test
    public void deletePortRuleWithEmptyName(){
        //represents a virtual machine from which a port rule should be deleted
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be deleted from the virtual machine vm
        PortRule portRule = new PortRule.Builder("",22,1540).build();
    
        exception.expect(IllegalArgumentException.class);
        sut.deletePortRule(vm, portRule);
        
        //checks the method NativeVBoxAPIMachine::deletePortRule() has never been called as expected
        verify(natAPIMachMock, never()).deletePortRule(any(VirtualMachine.class), anyString());
    }
    
    /**
     * This test tests that there is invoked UnexpectedVMStateException when there
     * is made an attempt to delete a port rule from a virtual machine which is
     * not using NAT network adapter.
     */
    @Test
    public void deletePortRuleWithInvalidAttachmentType(){
        //represents a virtual machine from which a port rule should be deleted
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be deleted from the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();
        //mock object of type UnexpectedVMStateException for better test control
        UnexpectedVMStateException unexVMStateExMock = mock(UnexpectedVMStateException.class);
        
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);        
        //there should returned the UnexpectedVMStateException exception when the method
        //NativeVBoxAPIMachine::deletePortRule() is called with a required virtual machine
        //which has not set up a valid network attachment type
        doThrow(unexVMStateExMock).when(natAPIMachMock).deletePortRule(vm, portRule.getName());
        
        exception.expect(UnexpectedVMStateException.class);
        sut.deletePortRule(vm, portRule);
    }
    
    /**
     * This test tests that there is invoked UnknownPortRuleException when there
     * is made an attempt to delete a port rule which is not registered on a
     * specified virtual machine.
     */
    @Test
    public void deleteUnknownPortRule(){
        //represents a virtual machine from which a port rule should be deleted
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be deleted from the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();
        //mock object of type UnexpectedVMStateException for better test control
        UnknownPortRuleException unPortRuleExMock = mock(UnknownPortRuleException.class);
        
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should returned the UnknownPortRuleException exception when the method
        //NativeVBoxAPIMachine::deletePortRule() is called with a required virtual machine
        //and port rule which does not exist
        doThrow(unPortRuleExMock).when(natAPIMachMock).deletePortRule(vm, portRule.getName());
        
        exception.expect(UnknownPortRuleException.class);
        sut.deletePortRule(vm, portRule);
    }
    
    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to delete a port rule from a virtual machine whose
     * host machine is not connected.
     */
    @Test
    public void deletePortRuleFromDisconnectedPhysicalMachine(){
        //represents a virtual machine from which should be deleted a port rule
        VirtualMachine vm = new VMBuilder().build();
        //represents a port rule which should be deleted from the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();

       //there should be returned a negative answer which means the host machine
        //is not connected and therefore there is not possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(false);
        
        exception.expect(ConnectionFailureException.class);
        sut.deletePortRule(vm, portRule);
        
        //checks the method NativeVBoxAPIMachine::deletePortRule() has never been called as expected
        verify(natAPIMachMock, never()).deletePortRule(any(VirtualMachine.class), anyString());
    }
    
    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to delete a port rule from a virtual machine
     * with whose host machine is lost connection while there is being processed
     * port-forwarding rule deletion operation itself.
     */
    @Test
    public void deletePortRuleWithSuddenNetworkConnectionLoss(){
        //represents a virtual machine from which should be deleted a port rule
        VirtualMachine vm = new VMBuilder().build();
        //represents a port rule which should be deleted from the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();
        //mock object of type ConnectionFailureException for better test cotrol
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the ConnectionFailureException exception when
        //the method NativeVBoxAPIMachine::deletePortRule() is called with a required virtual machine
        //and means that there occured any connection problem and a port rule cannot be deleted
        //from the virtual machine
        doThrow(conFailExMock).when(natAPIMachMock).deletePortRule(vm, portRule.getName());

        exception.expect(ConnectionFailureException.class);
        sut.deletePortRule(vm, portRule);
        
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMock).disconnectFrom(vm.getHostMachine());
    }

    /**
     * This test tests that if there are all neccessary conditions for all port
     * rules deletion met and some port rules exist on a particular virtual machine
     * then all these port rules are successfully deleted and on a standard output
     * appears an informing message about that.
     */
    @Test
    public void deleteAllPortRulesWithSomeRules(){
        //represents a virtual machine from which all port rules should be deleted
        VirtualMachine vm = new VMBuilder().build();
        //represents the first of two port rules which exist on the virtual machine vm
        String portRule1 = "PortRule_01,TCP,,22,,1540";
        //represents the second of two port rules which exist on the virtual machine vm
        String portRule2 = "PortRule_02,UDP,10.10.10.10,5875,15.15.15.15,3650";
        //represents a list of existent port rules on virtual machine vm
        List<String> portRules = Arrays.asList(portRule1,portRule2);
        
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be returned a non-empty list of existent port rules when the method
        //NativeVBoxAPIMachine::getPortRules() is called with a required virtual machine
        when(natAPIMachMock.getPortRules(vm)).thenReturn(portRules);
        
        //there should not appear any exception nor error
        sut.deleteAllPortRules(vm);
        
        assertFalse("There should be written a message on a standard output that all port rules were "
                + "successfully deleted", outContent.toString().isEmpty());
        assertTrue("There should not be written a message on a standard error output", errContent.toString().isEmpty());        
    }

    /**
     * This test tests that if there are all neccessary conditions for all port
     * rules deletion met and no port rule exists on a particular virtual machine
     * then the port rule deletion operation is not needed to be performed and
     * on a standard output appears an informing message about that.
     */
    @Test
    public void deleteAllPortRulesWithNoRules(){
        //represents a virtual machine from which all port rules should be deleted
        VirtualMachine vm = new VMBuilder().build();
        
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be returned an empty list of existent port rules when the method
        //NativeVBoxAPIMachine::getPortRules() is called with a required virtual machine
        when(natAPIMachMock.getPortRules(vm)).thenReturn(new ArrayList<>());
        
        //there should not appear any exception nor error
        sut.deleteAllPortRules(vm);
        
        assertFalse("There should be written a message on a standard output that there are no port rules "
                + "to be deleted", outContent.toString().isEmpty());
        assertTrue("There should not be written a message on a standard error output", errContent.toString().isEmpty());
        //checks that the method NativeVBoxAPIMachine::deletePortRule() has never been called as expeted
        verify(natAPIMachMock, never()).deletePortRule(any(VirtualMachine.class), anyString());
    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to delete all port-forwarding rules from a null virtual
     * machine.
     */
    @Test
    public void deleteAllPortRulesWithNullVirtualMachine(){
        exception.expect(IllegalArgumentException.class);
        sut.deleteAllPortRules(null);
        
        //checks that the method NativeVBoxAPIMachine::getPortRules() has never been called as expected
        verify(natAPIMachMock, never()).getPortRules(any(VirtualMachine.class));
        //checks that the method NativeVBoxAPIMachine::deletePortRule() has never been called as expeted
        verify(natAPIMachMock, never()).deletePortRule(any(VirtualMachine.class), anyString());
    }

    /**
     * This test tests that there is invoked UnknownVirtualMachineException when
     * there is made an attempt to delete all port rules from a virtual machine
     * which does not exist and this state is found during retrieving all port
     * rules which should be deleted.
     */
    @Test
    public void deleteAllPortRulesWithNonexistentVMGetter(){
        //represents a virtual machine from which all port rules should be deleted
        VirtualMachine vm = new VMBuilder().build();
        //mock object of type UnknownVirtualMachineException for better test control
        UnknownVirtualMachineException unVirtMachExMock = mock(UnknownVirtualMachineException.class);
                
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be thrown the UnknownVirtualMachineException exception when the method
        //NativeVBoxAPIMachine::getPortRules() is called with a required virtual machine and means
        //that there is no virtual machine like required on a particular physical machine
        doThrow(unVirtMachExMock).when(natAPIMachMock).getPortRules(vm);
        
        exception.expect(UnknownVirtualMachineException.class);
        sut.deleteAllPortRules(vm);
        
        //checks that the method NativeVBoxAPIMachine::deletePortRule() has never been called as expeted
        verify(natAPIMachMock, never()).deletePortRule(any(VirtualMachine.class), anyString());
    }
    
    /**
     * This test tests that there is invoked UnknownVirtualMachineException when
     * there is made an attempt to delete all port rules from a virtual machine
     * which does not exist and this state is found while port-forwarding rule
     * deletion operation is being processed for the first port rule.
     */
    @Test
    public void deleteAllPortRulesWithNonexistentVMDeletion(){
        //represents a virtual machine from which all port rules should be deleted
        VirtualMachine vm = new VMBuilder().build();
        //represents the first of two port rules which exist on the virtual machine vm
        String portRule1 = "PortRule_01,TCP,,22,,1540";
        //represents the second of two port rules which exist on the virtual machine vm
        String portRule2 = "PortRule_02,UDP,10.10.10.10,5875,15.15.15.15,3650";
        //represents a list of existent port rules on virtual machine vm
        List<String> portRules = Arrays.asList(portRule1,portRule2);
        //mock object of type UnknownVirtualMachineException for better test control
        UnknownVirtualMachineException unVirtMachExMock = mock(UnknownVirtualMachineException.class);
        
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be returned a non-empty list of existent port rules when the method
        //NativeVBoxAPIMachine::getPortRules() is called with a required virtual machine
        when(natAPIMachMock.getPortRules(vm)).thenReturn(portRules);
        //there should be thrown the UnknownVirtualMachineException exception when
        //the method NativeVBoxAPIMachine::deletePortRule() is called with the first
        //of the rules and simulates a case when a virtual machine from which should be
        //deleted all port rules is suddenly lost (removed from a list of registered virtual machines)
        doThrow(unVirtMachExMock).when(natAPIMachMock).deletePortRule(vm, "PortRule_01");
        
        exception.expect(UnknownVirtualMachineException.class);
        sut.deleteAllPortRules(vm);
    }
    
    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to delete all port rules from a virtual machine
     * whose host machine is not connected.
     */
    @Test
    public void deleteAllPortRulesFromDisconnectedPhysicalMachine(){
        //represents a virtual machine from which should be all port rules deleted        
        VirtualMachine vm = new VMBuilder().build();

       //there should be returned a negative answer which means the host machine
        //is not connected and therefore there is not possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(false);
        
        exception.expect(ConnectionFailureException.class);
        sut.deleteAllPortRules(vm);
        
        //checks that the method NativeVBoxAPIMachine::getPortRules() has never been called as expected
        verify(natAPIMachMock, never()).getPortRules(any(VirtualMachine.class));
        //checks that the method NativeVBoxAPIMachine::deletePortRule() has never been called as expeted
        verify(natAPIMachMock, never()).deletePortRule(any(VirtualMachine.class), anyString());
    }
    
    /**
     * This test tests that there is invoked ConnectionFailureException when there
     * is made an attempt to delete all port rules from a virtual machine with
     * whose host machine is lost connection while there are being retrieved all
     * port rules which should be deleted.
     */
    @Test
    public void deleteAllPortRulesWithSuddenNetworkConnectionLossInnerCheckGetter(){
        //represents a virtual machine from which should be all port rules deleted
        VirtualMachine vm = new VMBuilder().build();
        //mock object of type ConnectionFailureException for better test control
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the ConnectionFailureException exception when
        //the method NativeVBoxAPIMachine::getPortRules() is called with a required virtual machine
        //and means that there occured any connection problem and port rules cannot be retrieved
        doThrow(conFailExMock).when(natAPIMachMock).getPortRules(vm);

        exception.expect(ConnectionFailureException.class);
        sut.deleteAllPortRules(vm);
        
        //checks the method NativeVBoxAPIMachine::deletePortRule() has never been called as expected
        verify(natAPIMachMock, never()).deletePortRule(any(VirtualMachine.class), anyString());
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMock).disconnectFrom(vm.getHostMachine());
    }

    /**
     * This test tests that there is invoked ConnectionFailureException when there
     * is made an attempt to delete all port rules from a virtual machine with
     * whose host machine is lost connection while there is being processed
     * port-forwarding deletion operation itself.
     */
    @Test
    public void deleteAllPortRulesWithSuddenNetworkConnectionLossInnerCheckDeletion(){
        //represents a virtual machine from which should be all port rules deleted
        VirtualMachine vm = new VMBuilder().build();        
        //represents the first of two port rules which exist on the virtual machine vm
        String portRule1 = "PortRule_01,TCP,,22,,1540";
        //represents the second of two port rules which exist on the virtual machine vm
        String portRule2 = "PortRule_02,UDP,10.10.10.10,5875,15.15.15.15,3650";
        //represents a list of existent port rules on virtual machine vm
        List<String> portRules = Arrays.asList(portRule1,portRule2);
        //mock object of type ConnectionFailureException for better test control
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be returned a non-empty list of existent port rules when the method
        //NativeVBoxAPIMachine::getPortRules() is called with a required virtual machine
        when(natAPIMachMock.getPortRules(vm)).thenReturn(portRules);
       //there should be thrown the ConnectionFailureException exception when
        //the method NativeVBoxAPIMachine::deletePortRule() is called with a required virtual machine
        //and first of port rules and means that there occured any connection problem and a port rule
        //cannot be deleted from the virtual machine 
        doThrow(conFailExMock).when(natAPIMachMock).deletePortRule(vm, "PortRule_01");
        
        exception.expect(ConnectionFailureException.class);
        sut.deleteAllPortRules(vm);
        
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMock).disconnectFrom(vm.getHostMachine());
    }

    /**
     * This test tests that if there are all neccessary conditions for all port
     * rules retrieve operation met and on a particular virtual machine are present
     * any port rules then these port rules are returned as a result of
     * VirtualMachineManagerImpl::getPortRules() method call and no exception nor
     * error should appear.
     */
    @Test
    public void getPortRulesWithReturnedNonemptyList(){
        //represents a virtual machine from which should be all port rules retrieved
        VirtualMachine vm = new VMBuilder().build();        
        //represents the first of two port rules which exist on the virtual machine vm in a string form
        String portRuleStr1 = "PortRule_01,TCP,,22,,1540";
        //represents the second of two port rules which exist on the virtual machine vm in a string form
        String portRuleStr2 = "PortRule_02,UDP,10.10.10.10,5875,15.15.15.15,3650";
        //represents a list of existent port rules in a string form on virtual machine vm
        List<String> portRulesStr = Arrays.asList(portRuleStr1,portRuleStr2);
        PortRule portRule1 = new PortRule.Builder("PortRule_01", 22, 1540).build();
        PortRule portRule2 = new PortRule.Builder("PortRule_02", 5875, 3650).hostIP("10.10.10.10")
                                         .guestIP("15.15.15.15").protocol(ProtocolType.UDP).build();
        List<PortRule> expPortRules = Arrays.asList(portRule1,portRule2);
        
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be returned a non-empty list of existent port rules when the method
        //NativeVBoxAPIMachine::getPortRules() is called with a required virtual machine
        when(natAPIMachMock.getPortRules(vm)).thenReturn(portRulesStr);
        
        List<PortRule> actPortRules = sut.getPortRules(vm);
        
        //lists are sorted in order to have surer comparison results
        Collections.sort(expPortRules, prComparator);
        Collections.sort(actPortRules, prComparator);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertTrue("There should not be written a message on a standard error output", errContent.toString().isEmpty());
        assertDeepPRsEquals(expPortRules, actPortRules);
    }

    /**
     * This test tests that if there are all neccessary conditions for all port
     * rules retrieve operation met and on a particular virtual machine is not present
     * any port rule then there is returned an empty list of port rules as a result of
     * VirtualMachineManagerImpl::getPortRules() method call and no exception nor
     * error should appear.
     */
    @Test
    public void getPortRulesWithReturnedEmptyList(){
        //represents a virtual machine from which should be all port rules retrieved
        VirtualMachine vm = new VMBuilder().build();
        
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be returned an empty list of existent port rules when the method
        //NativeVBoxAPIMachine::getPortRules() is called with a required virtual machine
        when(natAPIMachMock.getPortRules(vm)).thenReturn(new ArrayList<>());
        
        List<PortRule> actPortRules = sut.getPortRules(vm);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertTrue("There should not be written a message on a standard error output", errContent.toString().isEmpty());
        assertTrue("The list of port rules should be empty", actPortRules.isEmpty());
    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to retrieve all port rules from a null virtual machine.
     */
    @Test
    public void getPortRulesWithNullVirtualMachine() {
        exception.expect(IllegalArgumentException.class);
        sut.getPortRules(null);
    }

    /**
     * This test tests that there is invoked UnkownVirtualMachineException when
     * there is made an attempt to retrieve all port rules from a virtual machine
     * which does not exist.
     */
    @Test
    public void getPortRulesWithNonexistentVM(){
        //represents a virtual machine from which should be all port rules retrieved
        VirtualMachine vm = new VMBuilder().build();
        //mock object of type UnknownVirtualMachineException for better test control
        UnknownVirtualMachineException unVirtMachExMock = mock(UnknownVirtualMachineException.class);
        
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be thrown the UnknownVirtualMachineException exception when the method
        //NativeVBoxAPIMachine::getPortRules() is called with a required virtual machine and
        //means that the virtual machine does not exist and therefore port rules cannot be retrieved
        doThrow(unVirtMachExMock).when(natAPIMachMock).getPortRules(vm);
        
        exception.expect(UnknownVirtualMachineException.class);
        sut.getPortRules(vm);
    }
    
    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to retrieve all port rules from a virtual machine
     * whose host machine is not connected.
     */
    @Test
    public void getPortRulesFromDisconnectedPhysicalMachine(){
        //represents a virtual machine from which should be all port rules retrieved
        VirtualMachine vm = new VMBuilder().build();        

       //there should be returned a negative answer which means the host machine
        //is not connected and therefore there is not possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(false);
        
        exception.expect(ConnectionFailureException.class);
        sut.getPortRules(vm);
        
        //checks the method NativeVBoxAPIMachine::getPortRules() has never been called as expected
        verify(natAPIMachMock, never()).getPortRules(any(VirtualMachine.class));
    }

    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to retrieve all port rules from a virtual machine
     * with whose host machine is lost connection while there is being processed
     * all port-forwarding rules retrieve operation itself.
     */
    @Test
    public void getPortRulesWithSuddenNetworkConnectionLoss(){
        //represents a virtual machine from which should be deleted a port rule
        VirtualMachine vm = new VMBuilder().build();
        //mock object of type UnknownVirtualMachineException for better test control
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the ConnectionFailureException exception when
        //the method NativeVBoxAPIMachine::getPortRules() is called with a required virtual machine
        //and means that there occured any connection problem and port rules cannot be retrieved
        //from the virtual machine
        doThrow(conFailExMock).when(natAPIMachMock).getPortRules(vm);

        exception.expect(ConnectionFailureException.class);
        sut.getPortRules(vm);
        
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMock).disconnectFrom(vm.getHostMachine());
    }
    
    /**
     * This test tests that if there are all neccessary conditions for finding out
     * the virtual machine state met then the virtual machine is successfully
     * started and on a standard output appears an informing message.
     */
    @Test
    public void getVMStateIdealCase() {
        //represents a virtual machine whose state should be found out
        VirtualMachine vm = new VMBuilder().build();

        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);

        //there should not appear any exception nor error
        sut.getVMState(vm);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertTrue("There should not be written a message on a standard error output", errContent.toString().isEmpty());
    }

    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to query the state of a null virtual machine.
     */
    @Test
    public void getNullVMState(){
        exception.expect(IllegalArgumentException.class);
        sut.getVMState(null);
        
        //checks the method NativeVBoxAPIMachine::getVMState() has never been called as expected
        verify(natAPIMachMock, never()).getVMState(any(VirtualMachine.class));
    }

    /**
     * This test tests that there is invoked UnknownVirtualMachineException when
     * there is made an attempt to query the state of a virtual machine which
     * does not exist.
     */
    @Test
    public void getNonexistentVMState(){
        //represents a virtual machine whose state should be found out
        VirtualMachine vm = new VMBuilder().build();
        //mock object of type UnknownVirtualMachineException for better test control
        UnknownVirtualMachineException unVirtMachExMock = mock(UnknownVirtualMachineException.class);

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the UnknownVirtualMachineException exception when
        //the method NativeVBoxAPIMachine::getVMState() is called with a required virtual machine
        //and means that the virtual machine does not exist and cannot be started
        doThrow(unVirtMachExMock).when(natAPIMachMock).getVMState(vm);

        exception.expect(UnknownVirtualMachineException.class);
        sut.getVMState(vm);
    }

    /**
     * This test tests that there is invoked UnexpectedVMStateException when
     * there is made an attempt to query the state of an inaccessible virtual
     * machine.
     */
    @Test
    public void getInaccessibleVMState(){
        //represents a virtual machine whose state should be found out
        VirtualMachine vm = new VMBuilder().build();
        //mock object of type UnexpectedVMStateException for better test control
        UnexpectedVMStateException unexVMStateMock = mock(UnexpectedVMStateException.class);

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the UnexpectedVMStateException exception when
        //the method NativeVBoxAPIMachine::getVMState() is called with a required virtual machine
        //and means that the virtual machine's source files are missing or corrupted
        //and cannot be started
        doThrow(unexVMStateMock).when(natAPIMachMock).getVMState(vm);
        
        exception.expect(UnexpectedVMStateException.class);
        sut.getVMState(vm);
    }

    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to query the state of a virtual machine whose
     * host machine is not connected.
     */
    @Test
    public void getVMStateOnDisconnectedPhysicalMachine(){
        //represents a virtual machine whose state should be found out
        VirtualMachine vm = new VMBuilder().build();

       //there should be returned a negative answer which means the host machine
        //is not connected and therefore there is not possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(false);
        
        exception.expect(ConnectionFailureException.class);
        sut.getVMState(vm);
        
        //checks the method NativeVBoxAPIMachine::getVMState() has never been called as expected
        verify(natAPIMachMock, never()).getVMState(any(VirtualMachine.class));
    }

    /**
     * This test tests that there is invoked ConnectionFailureException when there
     * is made an attempt to query the state of a virtual machine with whose host
     * machine is lost connection while there is being processed VM state query
     * operation itself.
     */
    @Test
    public void getVMStateWithSuddenNetworkConnectionLoss(){
        //represents a virtual machine whose state should be found out
        VirtualMachine vm = new VMBuilder().build();
        //mock object of type ConnectionfailureException for better test control
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMock.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the ConnectionFailureException exception when
        //the method NativeVBoxAPIMachine::getVMState() is called with a required virtual machine
        //and means that there occured any connection problem at the beginning
        //of the start-up operation
        doThrow(conFailExMock).when(natAPIMachMock).getVMState(vm);

        exception.expect(ConnectionFailureException.class);
        sut.getVMState(vm);
        
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMock).disconnectFrom(vm.getHostMachine());
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
    
    private void assertDeepPRsEquals(List<PortRule> expPRs, List<PortRule> actPRs) {
        for (int i = 0; i < expPRs.size(); ++i) {
            PortRule expPR = expPRs.get(i);
            PortRule actPR = actPRs.get(i);
            assertDeepPRsEquals(expPR, actPR);
        }
    }

    private void assertDeepPRsEquals(PortRule expPR, PortRule actPR) {        
        assertEquals("Port rules should have same name", expPR.getName(), actPR.getName());
        assertEquals("Port rules should have same protocol", expPR.getProtocol(), actPR.getProtocol());
        assertEquals("Port rules should have same host IP address", expPR.getHostIP(), actPR.getHostIP());
        assertEquals("Port rules should have same host port number", expPR.getHostPort(), actPR.getHostPort());
        assertEquals("Port rules should have same guest IP address", expPR.getGuestIP(), actPR.getGuestIP());
        assertEquals("Port rules should have same guest port number", expPR.getGuestPort(), actPR.getGuestPort());        
    }
    
    private static Comparator<PortRule> prComparator = new Comparator<PortRule>() {

        @Override
        public int compare(PortRule o1, PortRule o2) {
            
            return o1.getName().compareTo(o2.getName());
            
        }
    };
}
