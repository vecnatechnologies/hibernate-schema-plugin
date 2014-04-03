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
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;

/**
 * Base Mojo for generating SQL code from Hibernate schema (creating or updating the schema).
 * @author ogolberg@vecna.com
 */
public abstract class HibernateSchemaOutputMojo extends HibernateSchemaMojo {
  /**
   * Output file for SQL code.
   */
  @Parameter
  protected String outputFile;

  /**
   * Whether to format the output.
   */
  @Parameter
  protected boolean format = false;

  /**
   * The delimiter to use for SQL code. Default is semicolon.
   */
  @Parameter
  protected String delimiter = ";";

  /**
   * Whether to modify the live DB.
   */
  @Parameter
  protected boolean export = true;

  /**
   * Whether to enable output to stdout.
   */
  @Parameter
  protected boolean print = true;

  /**
   * Initialize parent directories for the output files.
   * @throws MojoExecutionException if directories couldn't be created.
   */
  protected void initializePath() throws MojoExecutionException {
    File file = new File(outputFile);
    File dir = file.getParentFile();
    if (!dir.exists()) {
      try {
        FileUtils.forceMkdir(dir);
      } catch (IOException e) {
        throw new MojoExecutionException("couldn't create directory " + dir, e);
      }
    }
  }
}
