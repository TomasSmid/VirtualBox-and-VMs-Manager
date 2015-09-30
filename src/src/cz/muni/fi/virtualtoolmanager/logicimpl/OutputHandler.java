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

import java.io.PrintStream;

/**
 *
 * @author Tomáš Šmíd
 */
class OutputHandler {
    private static PrintStream outputStream = System.out;
    private static PrintStream errOutputStream = System.err;
    
    public static void setOutputStream(PrintStream printStream){
        outputStream = printStream;
    }
    
    public static void setErrorOutputStream(PrintStream printStream){
        errOutputStream = printStream;
    }
    
    public static PrintStream getOutputStream(){
        return outputStream;
    }
    
    public static PrintStream getErrorOutputStream(){
        return errOutputStream;
    }
    
    public void printMessage(String message){
        print(outputStream,message);
    }
    
    public void printErrorMessage(String message){
        print(errOutputStream,message);
    }
    
    private void print(PrintStream printStream, String message){
        if(printStream != null && message != null && !message.trim().isEmpty()){
            printStream.println(message);
        }
    }
}
