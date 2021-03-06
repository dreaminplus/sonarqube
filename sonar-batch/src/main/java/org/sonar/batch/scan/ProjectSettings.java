/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.scan;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.bootstrap.AnalysisMode;
import org.sonar.batch.bootstrap.GlobalSettings;
import org.sonar.batch.protocol.input.ProjectReferentials;

import javax.annotation.Nullable;

public class ProjectSettings extends Settings {

  private static final Logger LOG = LoggerFactory.getLogger(ProjectSettings.class);

  private Configuration deprecatedConfiguration;

  private final GlobalSettings globalSettings;
  private final ProjectReferentials projectReferentials;
  private final AnalysisMode mode;

  public ProjectSettings(ProjectReactor reactor, GlobalSettings globalSettings, PropertyDefinitions propertyDefinitions,
    ProjectReferentials projectReferentials, Configuration deprecatedConfiguration, AnalysisMode mode) {
    super(propertyDefinitions);
    this.mode = mode;
    getEncryption().setPathToSecretKey(globalSettings.getString(CoreProperties.ENCRYPTION_SECRET_KEY_PATH));
    this.globalSettings = globalSettings;
    this.projectReferentials = projectReferentials;
    this.deprecatedConfiguration = deprecatedConfiguration;
    init(reactor);
  }

  private void init(ProjectReactor reactor) {
    LOG.info("Load project settings");

    addProperties(globalSettings.getProperties());

    addProperties(projectReferentials.settings(reactor.getRoot().getKeyWithBranch()));

    addProperties(reactor.getRoot().getProperties());
  }

  @Override
  protected void doOnSetProperty(String key, @Nullable String value) {
    deprecatedConfiguration.setProperty(key, value);
  }

  @Override
  protected void doOnRemoveProperty(String key) {
    deprecatedConfiguration.clearProperty(key);
  }

  @Override
  protected void doOnClearProperties() {
    deprecatedConfiguration.clear();
  }

  @Override
  protected void doOnGetProperties(String key) {
    if (mode.isPreview() && key.endsWith(".secured") && !key.contains(".license")) {
      throw MessageException.of("Access to the secured property '" + key
        + "' is not possible in preview mode. The SonarQube plugin which requires this property must be deactivated in preview mode.");
    }
  }
}
