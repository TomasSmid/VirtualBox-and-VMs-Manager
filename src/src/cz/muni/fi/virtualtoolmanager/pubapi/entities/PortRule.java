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
 *
 * @author Tomáš Šmíd
 */
public final class PortRule implements Comparable<PortRule>{
    private final String name;
    private final ProtocolType protocol;
    private final String hostIP;
    private final int hostPort;
    private final String guestIP;
    private final int guestPort;
    
    public static class Builder {
        private final String name;
        private final int hostPort;
        private final int guestPort;
        private ProtocolType protocol = ProtocolType.TCP;
        private String hostIP = "";        
        private String guestIP = "";        
        
        public Builder (String ruleName, int hostPort, int guestPort){
            this.name = ruleName;
            this.hostPort = hostPort;
            this.guestPort = guestPort;
        }
        
        public Builder protocol(ProtocolType value){
            protocol = value;
            return this;
        }
        
        public Builder hostIP(String value){
            hostIP = value;
            return this;
        }
        
        public Builder guestIP(String value){
            guestIP = value;
            return this;
        }
        
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
    
    @Override
    public String toString(){
        String hip = hostIP.equals("") ? "\"\"" : hostIP;
        String gip = guestIP.equals("") ? "\"\"" : guestIP;
        return "[" + "Port rule: name=" + name + ", protocol=" + protocol.toString() +
               ", hostIP=" + hip + ", hostPort=" + hostPort + ", guestIP=" +
               gip + ", guestPort=" + guestPort + "]";
    }
    
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
