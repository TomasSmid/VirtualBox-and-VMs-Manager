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

import cz.muni.fi.virtualtoolmanager.pubapi.types.ProtocolType;
import java.util.Objects;

/**
 * <div>
 * Class that represents a single port-forwarding rule on a single virtual
 * machine.
 * </div>
 * <div>
 * For initializing an object of type this class there is used Builder pattern
 * for easier usability and readibility the client code if this class is used.
 * </div>
 * 
 * @see cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualMachineManager
 * @see cz.muni.fi.virtualtoolmanager.logicimpl.VirtualMachineManagerImpl
 * 
 * @author Tomáš Šmíd
 */
public final class PortRule implements Comparable<PortRule>{
    /** Represents the name of a port rule, must be unique on a destination 
     * virtual machine */
    private final String name;
    /** Represents the protocol used for traffic */
    private final ProtocolType protocol;
    /** Represents the IP address of host machine, if not specified, then VirtualBox
     * listens to all traffic on the specified host port coming from any host
     * interface, if specified, then VirtualBox listens to all traffic on the
     * specified host port coming from the specified host interface e.g. 127.0.0.1 */
    private final String hostIP;
    /** Represents the port number used on the host machine */
    private final int hostPort;
    /** Represents the IP address of virtual machine, not neccessary to be specified
     * if virtual machine uses dynamic IP address assigned from VirtualBox DHCP
     * server, but if virtual machine uses a static IP address, then it must be
     * specified to ensure the port rule can be used and working properly
     */
    private final String guestIP;
    /** Represents the port number used on the virtual machine */
    private final int guestPort;
    
    /**
     * Class that represents a key part of so called Builder pattern. Static class
     * <code>Builder</code> is used only for more transparent, easier and faster set
     * up of {@link cz.muni.fi.virtualtoolmanager.pubapi.entities.PortRule PortRule}
     * attributes.
     */
    public static class Builder {
        /** port rule name, mandatory - must be specified in constructor */
        private final String name;
        /** port number of host machine, mandatory - must be specified in constructor */
        private final int hostPort;
        /** port number of virtual machine, mandatory - must be specified in constructor */
        private final int guestPort;
        /** protocol used for traffic, optional - default value set up to TCP */
        private ProtocolType protocol = ProtocolType.TCP;
        /** IP address of host machine, optional - default value set up to "" */
        private String hostIP = "";
        /** IP address of virtual machine, optional - default value set up to "" */
        private String guestIP = "";        
        
        /**
         * The first and the only constructor in which are set up the key port
         * rule values.
         * 
         * @param ruleName name of the rule, must be unique on the destination
         * virtual machine
         * @param hostPort port number used on host machine
         * @param guestPort port number used on virtual machine
         */
        public Builder (String ruleName, int hostPort, int guestPort){
            this.name = ruleName;
            this.hostPort = hostPort;
            this.guestPort = guestPort;
        }
        
        /**
         * Sets the type of protocol used for traffic.
         * @param value type of protocol used for port rule
         * @return instance of actual Builder class for more compact and dynamic
         * building class
         */
        public Builder protocol(ProtocolType value){
            protocol = value;
            return this;
        }
        
        /**
         * Sets the IP address of the host machine.
         * @param value IP address of the host machine
         * @return instance of actual Builder class for more compact and dynamic
         * building class
         */
        public Builder hostIP(String value){
            hostIP = value;
            return this;
        }
        
        /**
         * Sets the static IP address of the virtual machine.
         * @param value static IP address of the virtual machine
         * @return instance of actual Builder class for more compact and dynamic
         * building class
         */
        public Builder guestIP(String value){
            guestIP = value;
            return this;
        }
        
        /**
         * Creates new port rule with values of attributes set up by
         * Builder class.
         * @return new port rule
         */
        public PortRule build(){
            return new PortRule(this);
        }
    }
    
    private PortRule(Builder builder){
        this.name = builder.name;
        this.protocol = builder.protocol;
        this.hostIP = builder.hostIP;
        this.hostPort = builder.hostPort;
        this.guestIP = builder.guestIP;
        this.guestPort = builder.guestPort;
    }

    public String getName() {
        return name;
    }

    public ProtocolType getProtocol() {
        return protocol;
    }

    public String getHostIP() {
        return hostIP;
    }

    public int getHostPort() {
        return hostPort;
    }

    public String getGuestIP() {
        return guestIP;
    }

    public int getGuestPort() {
        return guestPort;
    }
    
    /**
     * This method is used for check if the actual port rule is same as
     * another port rule. Comparison is performed according to name of port rule
     * and used host port number.
     * 
     * @param obj represents port rule instance which is going to be compared 
     * with this port rule
     * @return <code>true</code> if the objects are same,
     * <code>false</code> otherwise
     */
    @Override
    public boolean equals(Object obj){
        if(obj == this) return true;
        if(!(obj instanceof PortRule)) return false;
        PortRule rule = (PortRule)obj;
        return ((this.name == rule.name) || 
                (this.name != null && this.name.equals(rule.name))) &&
               (this.hostPort == rule.hostPort);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + Objects.hashCode(this.name);
        hash = 41 * hash + this.hostPort;
        return hash;
    }
    
    /**
     * Method is used to print information about this port rule to string.
     * @return string in form <code>"[Port rule: name=&lt;prname&gt;,
     * protocol=&lt;protocol&gt;, hostIP=&lt;hostIP&gt;, hostPort=&lt;hostPort&gt;,
     * guestIP=&lt;guestIP&gt;, guestPort=&lt;guestPort&gt;]"</code>
     */
    @Override
    public String toString(){
        String hip = hostIP.equals("") ? "\"\"" : hostIP;
        String gip = guestIP.equals("") ? "\"\"" : guestIP;
        return "[" + "Port rule: name=" + name + ", protocol=" + protocol.toString() +
               ", hostIP=" + hip + ", hostPort=" + hostPort + ", guestIP=" +
               gip + ", guestPort=" + guestPort + "]";
    }
    
    /**
     * Method used to compare two port rules - this with another - in
     * order to sort (order) port rules. For comparison are used name of port rule
     * and host port number.
     * @param rule represents port rule with which is this one compared
     * @return <code>-1</code> if this port rule is less than another one,
     * <code>0</code>if this port rule is equal to another one,
     * <code>1</code>if this port rule is greater than another one
     */
    @Override
    public int compareTo(PortRule rule) {
        int result = this.name.compareTo(rule.name);
        if(result == 0){
            if(this.hostPort < rule.hostPort) return -1;
            if(this.hostPort > rule.hostPort) return 1;
            return 0;
        }else
            return result;
    }
}
