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
package cz.muni.fi.virtualtoolmanager.pubapi.entities;

import java.util.Objects;

/**
 * 
 * <div>
 * Class that is generaly used for connection purposes, because contains
 * information which are needed to perform connection operation and so access
 * virtual machines which are present on a particular physical machine.
 * </div>
 * <div>
 * Also for the physical machine holds it represents a host machine on which 
 * web server of VirtualBox should be running in order to be able to control
 * its virtual machines remotely.
 * </div>
 * 
 * @see cz.muni.fi.virtualtoolmanager.pubapi.managers.ConnectionManager
 * @see cz.muni.fi.virtualtoolmanager.logicimpl.ConnectionManagerImpl
 * @see cz.muni.fi.virtualtoolmanager.logicimpl.VirtualizationToolManagerImpl
 * @see cz.muni.fi.virtualtoolmanager.logicimpl.SearchManagerImpl
 * @see cz.muni.fi.virtualtoolmanager.logicimpl.VirtualMachineManagerImpl
 * @see cz.muni.fi.virtualtoolmanager.pubapi.entities.VirtualMachine
 * 
 * 
 * @author Tomáš Šmíd
 */
public final class PhysicalMachine implements Comparable<PhysicalMachine>{
    /** Represents an IP address of physical machine with VirtualBox installation 
     * and its running web server */
    private final String addressIP;
    /** Represents a port number on which is running web server of VirtualBox */
    private final String portOfVTWebServer;
    /** Represents a username of the user account under which the VirtualBox web
     * server has been launched on the remote physical machine */
    private final String username;
    /** Represents a user password of the user account under which the VirtualBox
     * web server has been launched on the remote physical machine */
    private final String userPassword;
    
    /**
     * The first and the only constructor of class <code>PhysicalMachine</code>.
     * To ensure successful creation of an object of class <code>PhysicalMachine</code>
     * it is needed to pass to the constructor strings with physical machine
     * IP address and VirtualBox web server port number that are not null nor
     * empty. Username and user password can be passed as a null or empty strings.
     * There can be thrown following exceptions:
     * <ul>
     * <li><strong>IllegalArgumentException - </strong>thrown when a null or
     * empty string value of physical machine IP address or VirtualBox web server
     * port number are passed to the constructor.
     * </ul>
     * 
     * @param addressIP represents IP address of physical machine
     * @param websrvPort represents port number of VirtualBox web server
     * @param username represents username of user account under which the remote
     * VirtualBox web server is running
     * @param userPassword represents user password of user account under which
     * the remote VirtualBox web server is running
     */
    public PhysicalMachine(String addressIP, String websrvPort, String username,
            String userPassword){
        if(addressIP == null || addressIP.trim().isEmpty()){
            throw new IllegalArgumentException("Physical machine initialization "
                    + "failure: IP address of physical machine must be specified "
                    + "as non-empty string value, which complies with IPv4 or "
                    + "IPv6 form.");
        }else{
            this.addressIP = addressIP.trim();
        }
        
        if(websrvPort == null || websrvPort.trim().isEmpty()){
            throw new IllegalArgumentException("Physical machine initialization "
                    + "failure: Port number of remote web server must be "
                    + "specified as non-empty string value containing only numbers.");
        }else{
            this.portOfVTWebServer = websrvPort.trim();
        }
        
        this.username = (username == null ? "" : username.trim());
        this.userPassword = (userPassword == null ? "" : userPassword.trim());
    }

    /**
     * Gets the physical machine IP address.
     * @return IP address of the physical machine as string
     */
    public String getAddressIP() {
        return addressIP;
    }

    /**
     * Gets the port number on which the VirtualBox web server is running.     * 
     * @return VirtualBox web server port as string
     */
    public String getPortOfVTWebServer() {
        return portOfVTWebServer;
    }

    /**
     * Gets the username of the user account under which the VirtualBox web server
     * is running on the remote physical machine and which is used for
     * authentication while the connection establishment is being processed.
     * @return username as string
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the user password of the user account under which the VirtualBox web
     * server is running on the remote physical machine and which is used for
     * authentication while the connection establishment is being processed.
     * @return user password as string
     */
    public String getUserPassword() {
        return userPassword;
    }
    
    /**
     * This method is used for check if this physical machine is same as
     * another physical machine. Comparison is performed according to all
     * physical machine properties - IP address, port number of VirtualBox web
     * server, username and user password.
     * 
     * @param obj represents physical machine instance which is going to be
     * compared with this physical machine
     * @return <code>true</code> if the objects are same,
     * <code>false</code> otherwise
     */
    @Override
    public boolean equals(Object obj){
        if(obj == this) return true;
        if(!(obj instanceof PhysicalMachine)) return false;
        PhysicalMachine pm = (PhysicalMachine)obj;
        return ((this.addressIP == pm.addressIP) || 
                (this.addressIP != null && this.addressIP.equals(pm.addressIP))) &&
               ((this.portOfVTWebServer == pm.portOfVTWebServer) ||
                (this.portOfVTWebServer != null && this.portOfVTWebServer.equals(pm.portOfVTWebServer))) &&
               ((this.username == pm.username) ||
                (this.username != null && this.username.equals(pm.username))) &&
               ((this.userPassword == pm.userPassword) || 
                (this.userPassword != null && this.userPassword.equals(pm.userPassword)));
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.addressIP);
        hash = 89 * hash + Objects.hashCode(this.portOfVTWebServer);
        hash = 89 * hash + Objects.hashCode(this.username);
        hash = 89 * hash + Objects.hashCode(this.userPassword);
        return hash;
    }
    
    /**
     * Method is used to print information about this physical machine to string.
     * @return string in form <code>"[Physical machine: IP address=&lt;ipaddress&gt;,
     * VT web server port=&lt;websrvport&gt;, username=&lt;username&gt;, user
     * password=&lt;userpassword&gt;]"</code>
     */
    @Override
    public String toString(){
        return "[" + "Physical machine: IP address=" + this.addressIP 
                + ", VT web server port=" + this.portOfVTWebServer 
                + ", username=" + (this.username.equals("") ? "\"\"" : this.username) 
                + ", user password=" + (this.userPassword.equals("") ? "\"\"" : this.userPassword)
                + "]"; 
    }
    
    /**
     * Method used to compare two physical machines - this with another - in
     * order to sort (order) physical machines. For comparison are used 
     * all physical machine properties. 
     * @param pm represents physical machine with which is this one compared
     * @return <code>-1</code> if this physical machine is less than another one,
     * <code>0</code>if this physical machine is equal to another one,
     * <code>1</code>if this physical machine is greater than another one
     */
    @Override
    public int compareTo(PhysicalMachine pm){
        int result = this.addressIP.compareTo(pm.addressIP);
        
        if(result == 0){
            result = this.portOfVTWebServer.compareTo(pm.portOfVTWebServer);
        }else{
            return result;
        }
        
        if(result == 0){
            result = this.username.compareTo(pm.username);
        }else{
            return result;
        }
        
        return (result == 0 ? this.userPassword.compareTo(pm.userPassword) : result);
    }
}
