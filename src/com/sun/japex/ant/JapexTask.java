/*
 * Japex software ("Software")
 *
 * Copyright, 2004-2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Software is licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at:
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations.
 *
 *    Sun supports and benefits from the global community of open source
 * developers, and thanks the community for its important contributions and
 * open standards-based technology, which Sun has adopted into many of its
 * products.
 *
 *    Please note that portions of Software may be provided with notices and
 * open source licenses from such communities and third parties that govern the
 * use of those portions, and any licenses granted hereunder do not alter any
 * rights and obligations you may have under such open source licenses,
 * however, the disclaimer of warranty and limitation of liability provisions
 * in this License will apply to all Software in this distribution.
 *
 *    You acknowledge that the Software is not designed, licensed or intended
 * for use in the design, construction, operation or maintenance of any nuclear
 * facility.
 *
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 */

package com.sun.japex.ant;

import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

import com.sun.japex.Japex;

public class JapexTask extends MatchingTask {

    /*************************  -classpath option *************************/
    protected Path compileClasspath = null;

    /**
     * Gets the classpath.
     */
    public Path getClasspath() {
        return compileClasspath;
    }

    /**
     * Set the classpath to be used for this compilation.
     */
    public void setClasspath(Path classpath) {
        if (compileClasspath == null) {
            compileClasspath = classpath;
        } else {
            compileClasspath.append(classpath);
        }
    }

    /**
     * Creates a nested classpath element.
     */
    public Path createClasspath() {
        if (compileClasspath == null) {
            compileClasspath = new Path(project);
        }
        return compileClasspath.createPath();
    }

    /**
     * Adds a reference to a CLASSPATH defined elsewhere.
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    // -- html -----------------------------------------------------------
    
    private boolean html = true;

    public boolean getHtml() {
        return this.html;
    }

    public void setHtml(boolean html) {
        this.html = html;
    }

    // -- config file ----------------------------------------------------
    
    private String config = "config.xml";

    public String getConfig() {
        return this.config;
    }

    public void setConfig(String config) {
        this.config = config;
    }
    
    // -- fork -----------------------------------------------------------
    
    private boolean fork = false;

    public boolean getFork() {
        return fork;
    }

    public void setFork(boolean fork) {
        this.fork = fork;
    }
    
    // -- execute() ------------------------------------------------------
    
    public void execute() throws BuildException {
        
        if (fork) {
            run(setupForkCommand().getCommandline());
        }
        else {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            
            Japex japex = new Japex();
            japex.setHtml(html);
            japex.run(config);
        }
    }
    
    /**
     * Executes the given classname with the given arguments in a separate VM.
     */
    private int run(String[] command) throws BuildException {

        for (int i = 0; i < command.length; i++) {
            System.out.println("#### command[" + i + "] =" + command[i]);
        }
        
        Execute exe = null;
        exe = new Execute();
        exe.setAntRun(project);
        exe.setCommandline(command);
        
        try {
            int rc = exe.execute();
            if (exe.killedProcess()) {
                log("Timeout: killed the sub-process", Project.MSG_WARN);
            }
            return rc;
        } 
        catch (IOException e) {
            throw new BuildException(e, location);
        }
    }
    
    private Commandline setupForkCommand() {
        CommandlineJava forkCmd = new CommandlineJava();

        Path classpath = getClasspath();
        forkCmd.createClasspath(getProject()).append(classpath);
        forkCmd.setClassname("com.sun.japex.Japex");

        if (!html) {
            forkCmd.createArgument().setValue("-nohtml");
        }        
        forkCmd.createArgument().setValue(config);
        
        Commandline cmd = new Commandline();
        cmd.createArgument(true).setLine(forkCmd.toString());
        return cmd;
    }

}
