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
package org.sonar.api.batch.fs.internal;

import org.sonar.api.batch.fs.FileSystem.Index;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.PathUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

/**
 * @since 4.2
 */
class AbsolutePathPredicate extends AbstractFilePredicate {

  private final String path;
  private final File baseDir;

  AbsolutePathPredicate(String path, File baseDir) {
    this.baseDir = baseDir;
    this.path = PathUtils.sanitize(path);
  }

  @Override
  public boolean apply(InputFile f) {
    return path.equals(f.absolutePath());
  }

  @Override
  public Iterable<InputFile> get(Index index) {
    String relative = PathUtils.sanitize(new PathResolver().relativePath(baseDir, new File(path)));
    if (relative == null) {
      return Collections.<InputFile>emptyList();
    }
    InputFile f = index.inputFile(relative);
    return f != null ? Arrays.asList(f) : Collections.<InputFile>emptyList();
  }

  @Override
  public int priority() {
    return USE_INDEX;
  }
}
