/**
 * Copyright 2011 Vecna Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
*/

package com.vecna.maven.hibernate;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.PropertyUtils;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.NamingStrategy;

import com.vecna.maven.commons.BuildClassPathMojo;

/**
 * Base Mojo for manipulating Hibernate schema (generation, updates, validation).
 * Features:
 *
 * <ul>
 * <li>Multiple configuration files</li>
 * <li>Multiple property files</li>
 * <li>Properties supplied as Mojo parameters</li>
 * <li>Classes supplied as Mojo parameters</li>
 * <li>Mappings supplied as Mojo parameters</li>
 * </ul>
 *
 * @author ogolberg@vecna.com
 */
public abstract class HibernateSchemaMojo extends BuildClassPathMojo {
  /**
   * Reference to the maven project. Internal.
   */
  @Component
  private MavenProject project;

  /**
   * Hibernate config (hibernate.cfg.xml) files. Can be filesystem or classpath resources.
   */
  @Parameter
  private String[] configFiles;

  /**
   * Hibernate property (hibernate.properties) files. Can be filesystem or classpath resources.
   */
  @Parameter
  private String[] propertyFiles;

  /**
   * Additional mapping resources (hbm.xml).
   */
  @Parameter
  private String[] additionalMappings;

  /**
   * Additional classes to be mapped (in addition to the ones specified in Hibernate configuration files).
   */
  @Parameter
  private String[] additionalClasses;

  /**
   * Hibernate properties (override properties retrieved from property files).
   */
  @Parameter
  private Properties properties;

  /**
   * The naming strategy to use (class name).
   */
  @Parameter
  private String namingStrategy;

  /**
   * Disable auto-detection of Envers.
   */
  @Parameter
  private boolean disableEnvers;

  /**
   * Skip execution.
   */
  @Parameter
  private boolean skip;

  /**
   * {@inheritDoc}
   */
  @Override
  protected MavenProject getProject() {
    return project;
  }

  /**
   * Resolves a filesystem or classpath URL from a string. Classpath resources are resolved using
   * the current thread's context classloader (to play nice with Maven).
   * @param string the string to resolve.
   * @return a valid filesystem or classpath resource <code>string</code> points to.
   * @throws MojoExecutionException if the argument does not point to a valid filesystem of classpath resource
   */
  private URL getURL(String string) throws MojoExecutionException {
    File file = new File(string);
    if (file.exists()) {
      try {
        return file.toURL();
      } catch (MalformedURLException e) {
        throw new MojoExecutionException(file + " cannot be converted to a URL", e);
      }
    } else {
      URL url = Thread.currentThread().getContextClassLoader().getResource(string);
      if (url == null) {
        throw new MojoExecutionException(string + " is not a valid file or classpath resource");
      }
      return url;
    }
  }

  /**
   * Create mapping metadata from provided Hibernate configuration
   * @return mapping metadata
   * @throws MojoExecutionException if a mapping class cannot be resolved or if the naming strategy cannot be instantiated
   */
  protected Configuration createMappings() throws MojoExecutionException {
    Configuration configuration = new AnnotationConfiguration();

    if (configFiles != null) {
      for (String configFile : configFiles) {
        if (configFile != null && !configFile.equals("")) {
          configuration.configure(getURL(configFile));
        }
      }
    }

    if (additionalClasses != null) {
      for (String additionalClass : additionalClasses) {
        try {
          configuration.addClass(Class.forName(additionalClass));
        } catch (ClassNotFoundException e) {
          throw new MojoExecutionException("coudn't add additional classes", e);
        }
      }
    }

    if (additionalMappings != null) {
      for (String mapping : additionalMappings) {
        configuration.addURL(getURL(mapping));
      }
    }

    if (propertyFiles != null) {
      for (String propertyFile : propertyFiles) {
        URL url = getURL(propertyFile);
        Properties properties = PropertyUtils.loadProperties(url);
        configuration.addProperties(properties);
      }
    }

    if (properties != null) {
      configuration.addProperties(properties);
    }

    if (namingStrategy != null) {
      try {
        @SuppressWarnings("rawtypes")
        Class nsClass = Thread.currentThread().getContextClassLoader().loadClass(namingStrategy);
        configuration.setNamingStrategy((NamingStrategy) nsClass.newInstance());
      } catch (Exception e) {
        throw new MojoExecutionException(namingStrategy + " is not a valid naming strategy", e);
      }
    }

    configuration.buildMappings();

    if (!disableEnvers) {
      if (tryEnableEnvers(configuration)) {
        getLog().info("Detected Envers");
      }
    }

    return configuration;
  }

  /**
   * Add Envers mappings if Envers is present on the classpath
   * @param configuration hibernate configuration.
   * @return whether Envers has been detected on the classpath and Envers schema support has been successfully activated.
   * @throws MojoExecutionException if encountered an incompatible version of the Envers API.
   */
  protected boolean tryEnableEnvers(Configuration configuration) throws MojoExecutionException {
    Class<?> enversConfigClass;

    try {
      final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      enversConfigClass = classLoader.loadClass("org.hibernate.envers.configuration.AuditConfiguration");
    } catch (ClassNotFoundException e) {
      return false;
    }

    try {
      Method getFor = enversConfigClass.getMethod("getFor", Configuration.class);
      getFor.invoke(null, configuration);
    } catch (Exception e) {
      throw new MojoExecutionException("Unexpected error while creating the Envers configuration", e);
    }

    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected final void executeWithClassLoader() throws MojoExecutionException, MojoFailureException {
    if (!skip) {
      Configuration configuration = createMappings();
      executeWithMappings(configuration);
    } else {
      getLog().info("skipping execution");
    }
  }

  /**
   * This method will be executed in the build classpath classloader context after the mapping metadata is built.
   * @param configuration mapping metadata.
   * @throws MojoExecutionException on execution exception
   * @throws MojoFailureException on execution failure
   */
  protected abstract void executeWithMappings(Configuration configuration) throws MojoExecutionException, MojoFailureException;
}
