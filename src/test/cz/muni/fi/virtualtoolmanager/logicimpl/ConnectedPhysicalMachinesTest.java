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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

/**
 * This test class ensure unit testing of class ConnectedPhysicalMachines and
 * is intended to be a pointer that class ConnectedPhysicalMachines works as expected.
 * 
 * @author Tomáš Šmíd
 */
public class ConnectedPhysicalMachinesTest {
    
    @Rule
    public ExpectedException exception = ExpectedException.none();
    
    private ConnectedPhysicalMachines sut;
    private List<PhysicalMachine> physicalMachines;
    
    public ConnectedPhysicalMachinesTest(){
        physicalMachines = new ArrayList<>();
    }
    
    @Before
    public void setUp(){
        sut = ConnectedPhysicalMachines.getInstance();
    }
    
    @After
    public void cleanup(){
        if(physicalMachines != null){
            for(PhysicalMachine pm : physicalMachines){
                sut.remove(pm);
            }
            physicalMachines.clear();
        }
    }
    
    /**
     * This test tests that if the method ConnectedPhysicalMachines::add() is
     * called with a non-null physical machine then this physical machine is
     * correctly added to the list of connected physical machines.
     */
    @Test
    public void addPhysicalMachineWithValidArgument(){
        //represents a physical machine which should be added to the list of connected physical machines
        PhysicalMachine pm = new PMBuilder().build();
        //physical machine pm is added to the list "physicalMachines" for after test cleanup
        //and keeping the consistent environment for testing
        physicalMachines.add(pm);
        
        assertNotNull("Physical machine " + pm.toString() + " should be correctly instantiated, not null",pm);
        assertFalse("Physical machine " + pm.toString() + " should not be connected", sut.isConnected(pm));
        
        sut.add(pm);
        assertTrue("Physical machine " + pm.toString() + " should be connected",sut.isConnected(pm));
    }
    
    /**
     * This test tests that if the method ConnectedPhysicalMachines::add() is
     * called with a null physical machine then this physical machine is not added
     * to the list of connected physical machines, but there is thrown an
     * IllegalArgumentException exception.
     */
    @Test
    public void addNullPhysicalMachine(){
        exception.expect(IllegalArgumentException.class);
        sut.add(null);
    }
    
    /**
     * This test tests that if the method ConnectedPhysicalMachines::remove() is
     * called with a non-null physical machine which is in the list of connected
     * physical machines then this physical machine is removed from that list and
     * if there are another machines then these are not affected by this operation.
     */
    @Test
    public void removeValidConnectedPhysicalMachine(){
        //represents a physical machine which should both added to and later removed from the list of connected physical machines
        PhysicalMachine pm1 = new PMBuilder().build();
        //represents a physical machine which should be just added to the list of connected physical machines
        PhysicalMachine pm2 = new PMBuilder().addressIP("140.150.10.10").username("").userPassword("").build();
        //physical machine pm is added to the list "physicalMachines" for after test cleanup
        //and keeping the consistent environment for testing
        physicalMachines.add(pm1);
        //physical machine pm is added to the list "physicalMachines" for after test cleanup
        //and keeping the consistent environment for testing
        physicalMachines.add(pm2);
        
        assertFalse("Physical machine " + pm1.toString() + " should not be connected "
                + "(it has not been added to the list of connected physical machines yet)", sut.isConnected(pm1));
        assertFalse("Physical machine " + pm2.toString() + " should not be connected "
                + "(it has not been added to the list of connected physical machines yet)", sut.isConnected(pm2));
        
        sut.add(pm1);
        sut.add(pm2);
        
        assertTrue("Physical machine " + pm1.toString() + " should be connected "
                + "(it has already been added to the list of connected physical machines)",sut.isConnected(pm1));
        assertTrue("Physical machine " + pm2.toString() + " should be connected "
                + "(it has already been added to the list of connected physical machines)",sut.isConnected(pm2));
        
        assertTrue("Physical machine " + pm1.toString() + " should be successfully removed, "
                + "but it is not",sut.remove(pm1));
        
        assertFalse("Physical machine " + pm1.toString() + " should not be connected, "
                + "because it has been removed from the list of connected physical machines", sut.isConnected(pm1));
        assertTrue("Physical machine " + pm2.toString() + " should be connected",sut.isConnected(pm2));
    }
    
    /**
     * This test tests that if the method ConnectedPhysicalMachines::remove() is
     * called (typically as a part of physical machine disconnection operation)
     * with a physical machine which is not connected, then this physical machine
     * cannot be removed from the list of connected physical machines (disconnected),
     * because it is not in that list.
     */
    @Test
    public void removeValidNotConnectedPhysicalMachine(){
        //represents a physical machine which is not connected and should be removed from
        //the list of connected physical machines (disconnected)
        PhysicalMachine pm1 = new PMBuilder().build();
        //represents a physical machine which is connected and is used to show
        //that unsuccessful remove operation cannot affect another connected physical machines
        PhysicalMachine pm2 = new PMBuilder().addressIP("140.150.10.10").username("").userPassword("").build();
        //physical machine pm is added to the list "physicalMachines" for after test cleanup
        //and keeping the consistent environment for testing
        physicalMachines.add(pm2);
        
        sut.add(pm2);
        
        assertFalse("Physical machine " + pm1.toString() + " should not be possible to remove, "
                  + "because it should not be connected",sut.remove(pm1));
        
        assertFalse("Physical machine " + pm1.toString() + " should not be connected", sut.isConnected(pm1));
        assertTrue("Physical machine " + pm2.toString() + " should be connected",sut.isConnected(pm2));
    }
    
    /**
     * This test tests that if the method ConnectedPhysicalMachines::remove() is
     * called with a null physical machine, then this physical machine cannot be
     * removed and there is thrown an IllegalArgumentException exception.
     */
    @Test
    public void removeNullPhysicalMachine(){
        //represents a physical machine which is connected and is used to show
        //that unsuccessful remove operation cannot affect another connected physical machines
        PhysicalMachine pm = new PhysicalMachine("140.150.12.0","10000","Hornd","140nb48");
        //physical machine pm is added to the list "physicalMachines" for after test cleanup
        //and keeping the consistent environment for testing
        physicalMachines.add(pm);
        
        sut.add(pm);
        
        exception.expect(IllegalArgumentException.class);
        sut.remove(null);
        
        exception = ExpectedException.none();
        assertTrue("Physical machine " + pm.toString() + " should be connected",sut.isConnected(pm));
    }
    
    /**
     * This test tests that if the method ConnectedPhysicalMachines::isConnected()
     * is called with a non-null not connected physical machine, then there is
     * returned a negative answer.
     */
    @Test
    public void isMachineConnectedWithValidNotConnectedPhysicalMachine(){
        //represents a physical machine which should be queried if it is connected
        PhysicalMachine pm = new PMBuilder().build();
        
        assertNotNull("Physical machine " + pm.toString() + " should be correctly instantiated, not null",pm);
        assertFalse("Physical machine " + pm.toString() + " should not be connected", sut.isConnected(pm));        
    }
    
    /**
     * This test tests that if the method ConnectedPhysicalMachines::isConnected()
     * is called with a non-null connected physical machine, then there is
     * returned a positive answer.
     */
    @Test
    public void isMachineConnectedWithValidConnectedPhysicalMachine(){
        //represents a physical machine which should be queried if it is connected
        PhysicalMachine pm = new PMBuilder().build();
        //physical machine pm is added to the list "physicalMachines" for after test cleanup
        //and keeping the consistent environment for testing
        physicalMachines.add(pm);     
        
        sut.add(pm);
        assertTrue("Physical machine " + pm.toString() + " should be connected",sut.isConnected(pm));
    }
    
    /**
     * This test tests that if the method ConnectedPhysicalMachines::isConnected()
     * is called with a null physical machine, then this physical machine represents
     * an invalid physical machine which cannot be queried for its accessibility.
     */
    @Test
    public void isMachineConnectedWithNullPhysicalMachine(){
        //represents a physical machine which is connected and is used to show
        //that unsuccessful query operation for physical machine accessibility
        //cannot affect another connected physical machines
        PhysicalMachine pm = new PMBuilder().build();
        //physical machine pm is added to the list "physicalMachines" for after test cleanup
        //and keeping the consistent environment for testing
        physicalMachines.add(pm);
        
        sut.add(pm);
        
        exception.expect(IllegalArgumentException.class);
        sut.isConnected(null);
        
        exception = ExpectedException.none();
        assertTrue("Physical machine " + pm.toString() + " should be connected",sut.isConnected(pm));
    }
    
    /**
     * This test tests that if the method ConnectedPhysicalMachines::getConnectedPhysicalMachines()
     * is called when there are some connected physical machines, then these are
     * returned as result of the method call.
     */
    @Test
    public void getConnectedPhysicalMachinesWithNonemptyPMsListBasic(){
        //represents the first of two connected physical machines
        PhysicalMachine pm1 = new PMBuilder().build();
        //represents the second of two connected physical machines 
        PhysicalMachine pm2 = new PMBuilder().addressIP("10.0.0.10").webserverPort("1154")
                                             .username("").userPassword("").build();        
        
        //physical machine pm is added to the list "physicalMachines" for after test cleanup
        //and keeping the consistent environment for testing
        physicalMachines.add(pm1);
        //physical machine pm is added to the list "physicalMachines" for after test cleanup
        //and keeping the consistent environment for testing
        physicalMachines.add(pm2);
        
        sut.add(pm1);
        sut.add(pm2);
        
        List<PhysicalMachine> expectedList = Arrays.asList(pm1,pm2);        
        List<PhysicalMachine> actualList = sut.getConnectedPhysicalMachines();
        
        assertListsEquals(expectedList,actualList);
    }
    
    /**
     * This test tests that if the method ConnectedPhysicalMachines::getConnectedPhysicalMachines()
     * is called closely after deletion operation, but there stay some connected
     * physical machines, then there is returned a non-empty list of connected
     * physical machines with the correct number of connected machines.
     */
    @Test
    public void getConnectedPhysicalMachinesWithNonemptyPMsListAfterPMDeletion(){
        //represents a physical machine which is added to the list of connected physical machines,
        //but later is removed
        PhysicalMachine pm1 = new PMBuilder().build();
        //represents a connected physical machine
        PhysicalMachine pm2 = new PMBuilder().addressIP("10.0.0.10").webserverPort("1154")
                                             .username("").userPassword("").build();
        
        //physical machine pm is added to the list "physicalMachines" for after test cleanup
        //and keeping the consistent environment for testing
        physicalMachines.add(pm2);
        
        sut.add(pm1);
        sut.add(pm2);
        
        sut.remove(pm1);
        
        List<PhysicalMachine> expectedList = Arrays.asList(pm2);
        List<PhysicalMachine> actualList = sut.getConnectedPhysicalMachines();
        
        assertListsEquals(expectedList,actualList);
    }
    
    /**
     * This test tests that if the method ConnectedPhysicalMachines::getConnectedPhysicalMachines()
     * is called when there is no connected physical machine, then there is returned
     * an empty list of connected physical machines.
     */
    @Test
    public void getConnectedPhysicalMahcinesWithEmptyPMsList(){
        assertTrue("List of connected physical machines should be empty",
                   sut.getConnectedPhysicalMachines().isEmpty());
    }
    
    /**
     * This test tests that if the method ConnectedPhysicalMachines::getConnectedPhysicalMachines()
     * is called closely after deletion operation and there is no connected
     * physical machine after that, then there is returned an empty list of
     * connected physical machines.
     */
    @Test
    public void getConnectedPhysicalMachinesWithEmptyPMsListAfterPMDeletion(){
        //represents a physical machine which is added to the list of connected physical machines,
        //but later is removed
        PhysicalMachine pm = new PMBuilder().build();
        
        sut.add(pm);        
        assertFalse("List of connected physical machines should not be empty",
                    sut.getConnectedPhysicalMachines().isEmpty());
        
        sut.remove(pm);
        assertTrue("List of connected physical machines should be empty after deletion of last machine in list",
                    sut.getConnectedPhysicalMachines().isEmpty());
    }
    
    /**
     * This test tests that if the method ConnectedPhysicalMachines::getConnectedPhysicalMachines()
     * is called after an unsuccessful attempt to add a null physical machine to
     * the list of connected physical machines and there was no connected physical
     * machine before, then there is returned an empty list of connected physical
     * machines.
     */
    @Test
    public void getConnectedPhysicalMachinesWithEmptyPMsListAfterNullPMAdditionToEmptyList(){
        exception.expect(IllegalArgumentException.class);
        sut.add(null);
        
        assertTrue("List of connected physical machines should be still empty after null "
                + "physical machine addition", sut.getConnectedPhysicalMachines().isEmpty());
    }
    
    /**
     * This test tests that if the method ConnectedPhysicalMachines::getConnectedPhysicalMachines()
     * is called closely after an attempt to add a null physical machine to the
     * non-empty list of connected physical machines, then there is returned
     * the non-empty list of connected physical machines with a correct number
     * of connected machines.
     */
    @Test
    public void getConnectedPhysicalMachinesWithNonemptyPMsListAfterNullPMAdditionToNonemptyList(){
        //represents a physical machine which is connected and is used to show
        //that unsuccessful retrieve operation cannot affect another connected physical machines
        PhysicalMachine pm = new PMBuilder().build();
        //physical machine pm is added to the list "physicalMachines" for after test cleanup
        //and keeping the consistent environment for testing
        physicalMachines.add(pm);
        
        sut.add(pm);
        exception.expect(IllegalArgumentException.class);
        sut.add(null);
        
        List<PhysicalMachine> expectedList = Arrays.asList(pm);
        List<PhysicalMachine> actualList = sut.getConnectedPhysicalMachines();
        
        assertListsEquals(expectedList, actualList);
    }
    
    private void assertListsEquals(List<PhysicalMachine> expList, List<PhysicalMachine> actList){
        assertEquals("Size of both lists should be same",expList.size(),actList.size());
        for(int i = 0; i < expList.size(); ++i){
           PhysicalMachine expPM = expList.get(i);
           PhysicalMachine actPM = actList.get(i);
           assertEquals("IP Addresses of both physical machines should be same (PMs on index " + i + ")",
                        expPM.getAddressIP(),actPM.getAddressIP());
           assertEquals("Port of virtualization tool web server of both physical machines should be same (PMs on index " + i + ")",
                        expPM.getPortOfVTWebServer(), actPM.getPortOfVTWebServer());
           assertEquals("Username of both physical machines should be same (PMs on index " + i + ")",
                        expPM.getUsername(),actPM.getUsername());
           assertEquals("User password of both physical machines should be same (on index " + i + ")",
                        expPM.getUserPassword(), actPM.getUserPassword());
        }
    }
    
    class PMBuilder{
        private String addressIP = "180.148.14.10";
        private String portOfVBoxWebServer = "18083";
        private String username = "Jack";
        private String userPassword = "tr1h15jk7";

        public PMBuilder(){

        }

        public PMBuilder addressIP(String value){
            this.addressIP = value;
            return this;
        }

        public PMBuilder webserverPort(String value){
            this.portOfVBoxWebServer = value;
            return this;
        }

        public PMBuilder username(String value){
            this.username = value;
            return this;
        }

        public PMBuilder userPassword(String value){
            this.userPassword = value;
            return this;
        }

        public PhysicalMachine build(){
            return new PhysicalMachine(this.addressIP,this.portOfVBoxWebServer,
                                       this.username,this.userPassword);
        }
    }
}


