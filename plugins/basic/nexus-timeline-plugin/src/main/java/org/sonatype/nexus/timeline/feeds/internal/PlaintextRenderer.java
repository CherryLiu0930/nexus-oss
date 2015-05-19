/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-2015 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.timeline.feeds.internal;

import java.net.URL;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.timeline.feeds.FeedEvent;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.goodies.template.TemplateEngine;
import org.sonatype.sisu.goodies.template.TemplateParameters;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Component renders feed content as {@link "text/plain"}.
 *
 * @since 3.0
 */
@Singleton
@Named
public class PlaintextRenderer
    extends ComponentSupport
{
  private final Provider<TemplateEngine> templateEngineProvider;

  @Inject
  public PlaintextRenderer(@Named("shared-velocity") final Provider<TemplateEngine> templateEngineProvider) {
    this.templateEngineProvider = checkNotNull(templateEngineProvider);
  }

  public String getContent(final FeedEvent evt) {
    final URL templateURL = getTemplateFor(evt);
    if (templateURL != null) {
      final TemplateParameters templateParameters = new TemplateParameters();
      for (Map.Entry<String, String> d : evt.getData().entrySet()) {
        templateParameters.set(d.getKey(), d.getValue());
      }
      return templateEngineProvider.get().render(this, templateURL, templateParameters);
    }
    // TODO: Some human-readable fallback?
    return evt.getData().toString();
  }

  // ==

  private URL getTemplateFor(final FeedEvent evt) {
    return getClass().getResource("plaintext-" + evt.getTemplateId() + ".vm");
  }
}
