/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2014 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.formfields;

/**
 * Implemented by {@link FormField}s whose value should be selected from a data store (combobox).
 * The data store should return collections of records that have "id" and "name" fields.
 *
 * @since 2.7
 */
public interface Selectable
{

  /**
   * Returns resource path to an id/name rest store resource.
   * E.g.
   * "/service/local/repositories"
   * "/service/siesta/capabilities"
   */
  String getStorePath();

  /**
   * Returns the name of the property which contains the collation of objects.
   * E.g. For restlet1x resources this should be in most of teh cases equal to "data".
   */
  String getStoreRoot();

  /**
   * Returns the name of the property that should be considered as an record id. Defaults to "id";
   */
  String getIdMapping();

  /**
   * Returns the name of the property that should be considered as an record description. Defaults to "name";
   */
  String getNameMapping();

}
