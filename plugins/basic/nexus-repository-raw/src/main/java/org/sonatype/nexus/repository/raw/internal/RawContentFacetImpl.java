/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.raw.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.io.TempStreamSupplier;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.InvalidContentException;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;

import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_ATTRIBUTES;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_PATH;

/**
 * A {@link RawContentFacet} that persists to a {@link StorageFacet}.
 *
 * @since 3.0
 */
@Named
public class RawContentFacetImpl
    extends FacetSupport
    implements RawContentFacet
{
  private final static List<HashAlgorithm> hashAlgorithms = Lists.newArrayList(MD5, SHA1);

  // TODO: raw does not have config, this method is here only to have this bundle do Import-Package org.sonatype.nexus.repository.config
  // TODO: as FacetSupport subclass depends on it. Actually, this facet does not need any kind of configuration
  // TODO: it's here only to circumvent this OSGi/maven-bundle-plugin issue.
  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    // empty
  }

  @Nullable
  @Override
  @Transactional(retryOn = IllegalStateException.class)
  public Content get(final String path) {
    StorageTx tx = UnitOfWork.currentTransaction();

    final Component component = getComponent(tx, path, tx.getBucket());
    if (component == null) {
      return null;
    }

    final Asset asset = tx.firstAsset(component);
    final Blob blob = tx.requireBlob(asset.requireBlobRef());

    return toContent(asset, blob);
  }

  @Override
  public Content put(final String path, final Payload content) throws IOException, InvalidContentException {
    try (final TempStreamSupplier streamSupplier = new TempStreamSupplier(content.openInputStream())) {
      return doPutContent(path, streamSupplier, content);
    }
  }

  @Transactional(retryOn = {ONeedRetryException.class, ORecordDuplicatedException.class})
  protected Content doPutContent(final String path, final Supplier<InputStream> streamSupplier, final Payload payload)
      throws IOException
  {
    StorageTx tx = UnitOfWork.currentTransaction();

    final Bucket bucket = tx.getBucket();
    Component component = getComponent(tx, path, bucket);
    Asset asset;
    if (component == null) {
      // CREATE
      component = tx.createComponent(bucket, getRepository().getFormat())
          .group(getGroup(path))
          .name(getName(path));

      // Set attributes map to contain "raw" format-specific metadata (in this case, path)
      component.formatAttributes().set(P_PATH, path);
      tx.saveComponent(component);

      asset = tx.createAsset(bucket, component);
      asset.name(component.name());
    }
    else {
      // UPDATE
      asset = tx.firstAsset(component);
    }

    AttributesMap contentAttributes = null;
    if (payload instanceof Content) {
      contentAttributes = ((Content) payload).getAttributes();
    }
    Content.applyToAsset(asset, Content.maintainLastModified(asset, contentAttributes));
    final AssetBlob assetBlob = tx
        .setBlob(asset, path, streamSupplier.get(), hashAlgorithms, null, payload.getContentType());
    tx.saveAsset(asset);

    return toContent(asset, assetBlob.getBlob());
  }

  private String getGroup(String path) {
    StringBuilder group = new StringBuilder();
    if (!path.startsWith("/")) {
      group.append("/");
    }
    int i = path.lastIndexOf("/");
    if (i != -1) {
      group.append(path.substring(0, i));
    }
    return group.toString();
  }

  private String getName(String path) {
    int i = path.lastIndexOf("/");
    if (i != -1) {
      return path.substring(i + 1);
    }
    else {
      return path;
    }
  }

  @Override
  @Transactional
  public boolean delete(final String path) throws IOException {
    StorageTx tx = UnitOfWork.currentTransaction();

    final Component component = getComponent(tx, path, tx.getBucket());
    if (component == null) {
      return false;
    }

    tx.deleteComponent(component);
    return true;
  }

  @Override
  @Transactional(retryOn = ONeedRetryException.class)
  public void setCacheInfo(final String path, final Content content, final CacheInfo cacheInfo) throws IOException {
    StorageTx tx = UnitOfWork.currentTransaction();

    // by EntityId
    Asset asset = Content.findAsset(tx, content);
    if (asset == null) {
      // by format coordinates
      Component component = tx.findComponentWithProperty(P_PATH, path, tx.getBucket());
      if (component != null) {
        asset = tx.firstAsset(component);
      }
    }
    if (asset == null) {
      log.debug("Attempting to set cache info for non-existent raw component {}", path);
      return;
    }

    log.debug("Updating cacheInfo of {} to {}", path, cacheInfo);
    CacheInfo.applyToAsset(asset, cacheInfo);
    tx.saveAsset(asset);
  }

  // TODO: Consider a top-level indexed property (e.g. "locator") to make these common lookups fast
  private Component getComponent(StorageTx tx, String path, Bucket bucket) {
    String property = String.format("%s.%s.%s", P_ATTRIBUTES, RawFormat.NAME, P_PATH);
    return tx.findComponentWithProperty(property, path, bucket);
  }

  private Content toContent(final Asset asset, final Blob blob) {
    final Content content = new Content(new BlobPayload(blob, asset.requireContentType()));
    Content.extractFromAsset(asset, hashAlgorithms, content.getAttributes());
    return content;
  }
}
