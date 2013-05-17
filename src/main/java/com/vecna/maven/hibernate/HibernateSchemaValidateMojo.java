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

import java.sql.SQLException;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.hibernate.cfg.Configuration;

import com.vecna.dbDiff.business.dbCompare.impl.RdbCompareError;
import com.vecna.dbDiff.business.dbCompare.impl.RdbCompareErrorType;
import com.vecna.dbDiff.hibernate.HibernateSchemaValidator;
import com.vecna.dbDiff.model.relationalDb.RelationalValidationException;

/**
 * Validates a live DB schema against Hibernate mappings.
 *
 * @requiresDependencyResolution
 * @phase compile
 * @goal validate
 * @threadSafe
 * @author ogolberg@vecna.com
 */
public class HibernateSchemaValidateMojo extends HibernateSchemaMojo {
  /**
   * Validates the schema.
   * {@inheritDoc}
   */
  @Override
  protected void executeWithMappings(Configuration configuration) throws MojoExecutionException, MojoFailureException {
    List<RdbCompareError> errors;

    try {
      errors = new HibernateSchemaValidator(configuration).validate();
    } catch (RelationalValidationException e) {
      throw new MojoExecutionException("failed to compare hibernate schema to live schema", e);
    } catch (SQLException e) {
      throw new MojoExecutionException("failed to compare hibernate schema to live schema", e);
    }

    for (RdbCompareError error : errors) {
      if (error.getErrorType() != RdbCompareErrorType.COL_TYPE_WARNING) {
        getLog().error(error.getErrorType() + ": " + error.getMessage());
      }
    }
  }
}
