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
package cz.muni.fi.virtualtoolmanager.pubapi.io;

import java.io.PrintStream;

/**
 * Class that is used to print informing or error messages on the set up output
 * streams.
 * 
 * @author Tomáš Šmíd
 */
public class OutputHandler {
    /** Represents output stream for informing messages, default is System.out */
    private static PrintStream outputStream = System.out;
    /** Represents output stream for error messages, default is System.err */
    private static PrintStream errOutputStream = System.err;
    
    /**
     * Sets the output stream for the informing messages.
     * @param printStream represents new output stream that will be used for
     * informing messages
     */
    public static void setOutputStream(PrintStream printStream){
        outputStream = printStream;
    }
    
    /**
     * Sets the output stream for the error messages.
     * @param printStream represents new output stream that will be used for
     * error messages
     */
    public static void setErrorOutputStream(PrintStream printStream){
        errOutputStream = printStream;
    }
    
    /**
     * Gets the actual output stream used for informing messages.
     * @return actual output stream for informing messages
     */
    public static PrintStream getOutputStream(){
        return outputStream;
    }
    
    /**
     * Gets the actual output stream for error messages. 
     * @return actual error output stream
     */
    public static PrintStream getErrorOutputStream(){
        return errOutputStream;
    }
    
    /**
     * Prints the informing message passed to the method on the set up output
     * stream.
     * @param message represents informing message which is going to be printed
     */
    public void printMessage(String message){
        print(outputStream,message);
    }
    
    /**
     * Prints the error message passed to the method on the set up error output
     * stream.
     * @param message represents error message which is going to be printed
     */
    public void printErrorMessage(String message){
        print(errOutputStream,message);
    }
    
    /**
     * Prints the all passed messages to the given output stream. Before printing
     * are given output stream and message checked if are not null. If one of the
     * given parameter is null, message will not be printed.
     * @param printStream destination output stream of message
     * @param message message which should be printed
     */
    private void print(PrintStream printStream, String message){
        if(printStream != null && message != null && !message.trim().isEmpty()){
            printStream.println(message);
        }
    }
}
