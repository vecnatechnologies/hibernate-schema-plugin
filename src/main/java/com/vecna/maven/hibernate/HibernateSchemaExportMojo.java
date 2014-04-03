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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;

/**
 * Exports Hibernate schema to a file or live DB.
 *
 * @author ogolberg@vecna.com
 */
@Mojo(name = "export",
      defaultPhase = LifecyclePhase.COMPILE,
      requiresDependencyResolution = ResolutionScope.RUNTIME,
      threadSafe = true)
public class HibernateSchemaExportMojo extends HibernateSchemaOutputMojo {
  /**
   * Whether to execute drop SQL statements before creating the schema.
   */
  @Parameter
  private boolean drop = true;

  /**
   * Exports the schema.
   * {@inheritDoc}
   */
  @Override
  protected void executeWithMappings(Configuration configuration) throws MojoExecutionException, MojoFailureException {
    SchemaExport schemaExport = new SchemaExport(configuration);
    schemaExport.setFormat(format);

    if (outputFile != null) {
      initializePath();
      schemaExport.setOutputFile(outputFile);
      schemaExport.setDelimiter(delimiter);
    }

    schemaExport.execute(print, export, false, !drop);
  }
}
