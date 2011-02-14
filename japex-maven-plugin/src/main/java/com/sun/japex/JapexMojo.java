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

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.configuration.PlexusConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

/**
 * Goal to run Japex micro-benchmarks.
 *
 * @goal japex
 * 
 * @phase test
 */
public class JapexMojo extends AbstractMojo {
    /**
     * Location of the output reports.
     * @parameter default-value="${project.build.directory}/reports/japex"
     */
    private File reportDirectory;
    
    /**
     * The XML of the Japex config to run. This should contain a single testSuite element. (Multiple test suites is a potential 
     * future enhancement). If you specify the configuration here, don't use <strong>japexConfigFiles</strong>.
     * 
     * @parameter
     */
    private PlexusConfiguration japexConfig;
    
    /**
     * Pathnames of one or more japex configuration files. If you specify configuration files here, don't 
     * use <strong>japexConfig</strong>.
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
    
    /**
     * The Maven project object
     * 
     * @parameter expression="${project}"
     * @readonly
     */
    private MavenProject project;
    
    public void execute() throws MojoExecutionException {
    	StringWriter japexOutput = new StringWriter(); // for now, capture all at once.
    	PrintWriter printWriter = new PrintWriter(japexOutput);
    	Japex japex = new Japex();
    	japex.setOutputWriter(printWriter);
    	japex.setHtml(html);
    	Japex.silent = silent;
    	Japex.verbose = verbose;
    	japex.setOutputDirectory(reportDirectory);
    	
    	// we need a class loader for the test class path. Let's try the simple solution.
    	URL[] testClasspathUrls;
    	try {
			List<?> pathlist = project.getTestClasspathElements();
			testClasspathUrls = new URL[pathlist.size()];
			for (int x = 0; x < pathlist.size(); x++) {
				String path = (String)pathlist.get(x);
				getLog().debug("test classpath entry: " + path);
				testClasspathUrls[x] = new File(path).toURI().toURL();
			}
		} catch (DependencyResolutionRequiredException e) {
			throw new MojoExecutionException("Error resolving test classpath", e);
		} catch (MalformedURLException e) {
			throw new MojoExecutionException("Error in URL from test classpath", e);
		}
		/**
		 * Use the Japex class loader which knows how to find the japex classes relative to itself 
		 * and otherwise isolate.
		 */
		URLClassLoader classLoader = new JapexClassLoader(testClasspathUrls);
		japex.getNamedClasspaths().put("maven.test.classpath", classLoader);
		if (japexConfig != null) {
			StringWriter writer = new StringWriter();
			XmlConfigurationWriter xmlWriter = new XmlConfigurationWriter();
			try {
				xmlWriter.write(japexConfig, writer);
			} catch (IOException e) {
				throw new MojoExecutionException("Failed to write config to temp string", e);
			} catch (XMLStreamException e) {
				throw new MojoExecutionException("Failed to write config to temp string", e);
			}
			String config = writer.toString();
			File configFile;
			try {
				configFile = File.createTempFile("japex", ".xml");
				FileUtils.writeStringToFile(configFile, config, "utf-8");
			} catch (IOException e) {
				throw new MojoExecutionException("Failed to write config to temp file", e);
			}
			japexConfigFiles = new File[] { configFile };
		}
		if (japexConfigFiles == null || japexConfigFiles.length == 0) {
			getLog().info("No japex config files and no inline japexConfig; skipping");
			return;
		}
		List<String> configFileStrings = new ArrayList<String>();
		for (int x = 0; x < japexConfigFiles.length; x++) {
			configFileStrings.add(japexConfigFiles[x].getAbsolutePath());
		}
		try {
			japex.run(configFileStrings);
		} catch (JapexException je) {
			printWriter.close();
			getLog().error(japexOutput.toString());
			// CONVENTION: there's enough information in the string, we don't need the full backtrace.
			return;
		} 
		printWriter.close();
		getLog().info(japexOutput.toString());
    }
}
