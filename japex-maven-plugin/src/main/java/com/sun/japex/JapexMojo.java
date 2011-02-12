package com.sun.japex;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.configuration.PlexusConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Goal to run Japex micro-benchmarks.
 *
 * @goal japex
 * 
 * @phase test
 */
public class JapexMojo
    extends AbstractMojo
{
    /**
     * Location of the output reports.
     * @parameter default-value="${project.build.directory}/reports/japex"
     */
    private File reportDirectory;
    
    /**
     * The XML of the Japex config to run.
     * 
     * @parameter
     */
    private PlexusConfiguration japexConfig;
    
    /**
     * Pathnames of one or more japex configuration files.
     * @parameter
     */
    private File[] japexConfigFiles;
    
    /**
     * Produce HTML report?
     * @parameter default-value="true"
     */
    private boolean html;
    /**
     * More information?
     * @parameter default-value="false"
     */
    private boolean verbose;
    /**
     * Just about no output.
     * @parameter default-value="false"
     */
    private boolean silent;
    
    public void execute() throws MojoExecutionException {
    }
}
