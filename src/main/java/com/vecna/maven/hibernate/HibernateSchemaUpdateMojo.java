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
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;

/**
 * Compares Hibernate schema against a live DB and outputs changes to a file or the DB.
 *
 * @author ogolberg@vecna.com
 */
@Mojo(name = "update",
      defaultPhase = LifecyclePhase.COMPILE,
      requiresDependencyResolution = ResolutionScope.RUNTIME,
      threadSafe = true)
public class HibernateSchemaUpdateMojo extends HibernateSchemaOutputMojo {
  /**
   * Generates upgrade script for the schema.
   * {@inheritDoc}
   */
  @Override
  protected void executeWithMappings(Configuration configuration) throws MojoExecutionException, MojoFailureException {
    SchemaUpdate schemaUpdate = new SchemaUpdate(configuration);
    schemaUpdate.setFormat(format);

    if (outputFile != null) {
      initializePath();
      schemaUpdate.setOutputFile(outputFile);
      schemaUpdate.setDelimiter(delimiter);
    }

    schemaUpdate.execute(print, export);
  }
}
