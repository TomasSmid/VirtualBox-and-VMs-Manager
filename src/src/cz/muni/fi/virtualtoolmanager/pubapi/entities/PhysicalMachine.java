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
 * @author Tomáš Šmíd
 */
public final class PhysicalMachine implements Comparable<PhysicalMachine>{
    private final String addressIP;
    private final String portOfVTWebServer;
    private final String username;
    private final String userPassword;
    
    public PhysicalMachine(String addressIP, String websrvPort, String username, String userPassword){
        if(addressIP == null || addressIP.trim().isEmpty()){
            throw new IllegalArgumentException("Physical machine initialization failure: "
                    + "IP address of physical machine must be specified as non-empty string value,"
                    + " which complies with IPv4 form.");
        }else{
            this.addressIP = addressIP.trim();
        }
        
        if(websrvPort == null || websrvPort.trim().isEmpty()){
            throw new IllegalArgumentException("Physical machine initialization failure: "
                    + "Port of remote web server must be specified as non-empty string value"
                    + " containing only numbers.");
        }else{
            this.portOfVTWebServer = websrvPort.trim();
        }
        
        this.username = (username == null ? "" : username.trim());
        this.userPassword = (userPassword == null ? "" : userPassword.trim());
    }

    public String getAddressIP() {
        return addressIP;
    }

    public String getPortOfVTWebServer() {
        return portOfVTWebServer;
    }

    public String getUsername() {
        return username;
    }

    public String getUserPassword() {
        return userPassword;
    }
    
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
    
    @Override
    public String toString(){
        return "[" + "Physical machine: IP address=" + this.addressIP 
                + ", VT web server port=" + this.portOfVTWebServer 
                + ", username=" + (this.username.equals("") ? "\"\"" : this.username) 
                + ", user password=" + (this.userPassword.equals("") ? "\"\"" : this.userPassword)
                + "]"; 
    }
    
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
