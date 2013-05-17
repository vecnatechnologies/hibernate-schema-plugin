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
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.property.Getter;
import org.hibernate.tool.hbm2x.DocExporter;

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.Type;

/**
 * Generates schema documentation. This plugin enhances the Hibernate model with documentation extracted from the javadocs
 * (sources must be provided). Then it feeds the Hibernate model into the Hibernate Tools DocExporter.
 * @requiresDependencyResolution
 * @phase compile
 * @goal doc
 * @threadSafe
 * @author ogolberg@vecna.com
 */
public class HibernateDocMojo extends HibernateSchemaMojo {
  /**
   * @parameter output directory
   */
  private File outputDir;

  /**
   * @parameter source directories
   */
  private File[] sourceDirs = new File[0];

  /**
   * @parameter regex for figuring out which columns are encrypted (full name of the Hibernate type must match)
   */
  private String encryptedTypeRegex;

  /**
   * @return build javadocs from the source locations
   */
  private JavaDocBuilder findJavadocs() {
    JavaDocBuilder builder = new JavaDocBuilder();
    for (File sourceDir : sourceDirs) {
      if (!sourceDir.exists() || !sourceDir.isDirectory()) {
        getLog().error("Invalid source directory: " + sourceDir);
      } else {
        builder.addSourceTree(sourceDir);
      }
    }
    return builder;
  }

  /**
   * @return javadoc types from java types (for looking up method javadocs by signature)
   */
  private Type[] getJavaDocTypes(Class<?>[] classes) {
    com.thoughtworks.qdox.model.Type[] types = new com.thoughtworks.qdox.model.Type[classes.length];
    for (int i = 0; i < types.length; i++) {
      types[i] = new com.thoughtworks.qdox.model.Type(classes[i].getName());
    }
    return types;
  }

  /**
   * @return javadocs for a simple Hibernate property
   */
  private String getSimpleValueJavadoc(Property property, Class<?> cls, JavaClass javaClass) {
    Getter getter = property.getGetter(cls);

    Member member = getter.getMember();

    if (member instanceof Field) {
      JavaField field = javaClass.getFieldByName(member.getName());
      if (field != null) {
        return field.getComment();
      }
    } else if (member instanceof Method) {
      Method method = (Method) member;
      Type[] types = getJavaDocTypes(method.getParameterTypes());
      JavaMethod javaMethod = javaClass.getMethodBySignature(method.getName(), types, true);
      if (javaMethod == null) {
        getLog().warn("can't find java method docs for " + javaClass.getName()
                      + " . " + method.getName() + " " + Arrays.asList(types));
      } else {
        DocletTag tag = javaMethod.getTagByName("return", true);

        if (tag != null && tag.getValue() != null) {
          return tag.getValue();
        }
      }
    }

    return null;
  }

  /**
   * set a comment on Hibernate columns
   */
  private void setComment(String comment, Iterator<Column> columnIterator) {
    while (columnIterator.hasNext()) {
      Column column = columnIterator.next();
      if (encryptedTypeRegex != null && column.getValue() instanceof SimpleValue) {
        String typeName = ((SimpleValue) column.getValue()).getTypeName();
        if (typeName != null && typeName.matches(encryptedTypeRegex)) {
          comment += " [encrypted]";
        }
      }
      column.setComment(comment);
    }
  }

  /**
   * set a comment on Hibernate columns mapped to a property
   */
  private void setComment(String comment, Property prop) {
    @SuppressWarnings("unchecked") Iterator<Column> columnIterator = prop.getColumnIterator();
    setComment(comment, columnIterator);
  }

  /**
   * concatenate javadoc comments for nested properties
   */
  private String accumulateJavadoc(String comment, String accumulatedJavadoc) {
    if (comment == null) {
      comment = "???";
    }

    if (accumulatedJavadoc != null) {
      comment = accumulatedJavadoc + " - " + comment;
    }

    return comment;
  }

  /**
   * Populate Hibernate properties with comments from javadocs (including nested properties).
   * @param propertyIterator iterator over top-level properties
   * @param accumulatedJavadoc comments accumulated so far (for nested properties)
   */
  private void processProperties(Iterator<Property> propertyIterator, Class<?> cls,
                                 JavaDocBuilder javaDocs, String accumulatedJavadoc) {
    JavaClass javaClass = javaDocs.getClassByName(cls.getName());

    if (javaClass != null) {
    while (propertyIterator.hasNext()) {
      Property prop = propertyIterator.next();

      Value value = prop.getValue();

      if (value instanceof Collection) {
        Collection collection = (Collection) value;

        Value elementValue = collection.getElement();

        if (elementValue instanceof Component) {
          processComponent((Component) elementValue, javaDocs, accumulatedJavadoc);
        }

        Table collectionTable = collection.getCollectionTable();

        if (collectionTable.getComment() == null) {
          collectionTable.setComment(getSimpleValueJavadoc(prop, cls, javaClass));
        }
      } else if (value instanceof Component) {
        String comment = getSimpleValueJavadoc(prop, cls, javaClass);
        comment = accumulateJavadoc(comment, accumulatedJavadoc);
        processComponent((Component) value, javaDocs, comment);
      } else if (value instanceof SimpleValue) {
        String comment = getSimpleValueJavadoc(prop, cls, javaClass);
        comment = accumulateJavadoc(comment, accumulatedJavadoc);
        setComment(comment, prop);
      }
    }

    }
  }

  /**
   * Process a component (embedded property) and populate its properties (including nested ones) with javadoc comments.
   * @param component component model
   * @param javaDocs javadocs
   * @param accumulatedJavadoc comments accumulated so far (for nested components)
   */
  private void processComponent(Component component, JavaDocBuilder javaDocs, String accumulatedJavadoc) {
    @SuppressWarnings("unchecked") Iterator<Property> propertyIterator = component.getPropertyIterator();
    processProperties(propertyIterator, component.getComponentClass(), javaDocs, accumulatedJavadoc);
  }

  /**
   * Populate table/column comments in a Hibernate model from javadocs
   */
  private void populateCommentsFromJavadocs(Configuration configuration, JavaDocBuilder javaDocs) {
    Iterator<PersistentClass> mappedClasses = configuration.getClassMappings();
    while (mappedClasses.hasNext()) {
      PersistentClass mappedClass = mappedClasses.next();

      Table table = mappedClass.getTable();
      JavaClass javaClass = javaDocs.getClassByName(mappedClass.getClassName());

      if (javaClass != null) {
        if (table != null) {
          String comment = javaClass.getComment();

          if (mappedClass.getDiscriminator() != null) {
            String newComment = "Discriminator '" + mappedClass.getDiscriminatorValue() + "': " + comment;
            if (table.getComment() != null) {
              newComment = table.getComment() + "<br><br>" + newComment;
            }
            table.setComment(newComment);
            @SuppressWarnings("unchecked")
            Iterator<Column> discriminatorColumns = mappedClass.getDiscriminator().getColumnIterator();
            setComment("discriminator - see table comment", discriminatorColumns);
          } else {
            table.setComment(comment);
          }
        }

        @SuppressWarnings("unchecked") Iterator<Property> propertyIterator = mappedClass.getPropertyIterator();
        processProperties(propertyIterator, mappedClass.getMappedClass(), javaDocs, null);
      }

      if (mappedClass.getIdentifierProperty() != null) {
        setComment("Primary key", mappedClass.getIdentifierProperty());
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeWithMappings(Configuration configuration) throws MojoExecutionException, MojoFailureException {
    populateCommentsFromJavadocs(configuration, findJavadocs());
    try {
      FileUtils.forceMkdir(outputDir);
    } catch (IOException e) {
      throw new MojoExecutionException("cannot create output directory " + outputDir, e);
    }
    new DocExporter(configuration, outputDir).start();
  }
}
