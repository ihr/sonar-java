/*
 * SonarQube Java
 * Copyright (C) 2010-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.jacoco;

import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.IExecutionDataVisitor;
import org.jacoco.core.data.ISessionInfoVisitor;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class JacocoReportReader {

  private final InputStream inputStream;
  private final boolean currentReportFormat;

  public JacocoReportReader(InputStream inputStream, boolean currentReportFormat) {
    this.inputStream = new BufferedInputStream(inputStream);
    this.currentReportFormat = currentReportFormat;
  }

  /**
   * Read JaCoCo report determining the format to be used.
   *
   * @param executionDataVisitor visitor to store execution data.
   * @param sessionInfoStore     visitor to store info session.
   * @return true if binary format is the latest one.
   * @throws IOException in case of error or binary format not supported.
   */
  public JacocoReportReader readJacocoReport(IExecutionDataVisitor executionDataVisitor, ISessionInfoVisitor sessionInfoStore) throws IOException {

    if (currentReportFormat) {
      ExecutionDataReader reader = new ExecutionDataReader(inputStream);
      reader.setSessionInfoVisitor(sessionInfoStore);
      reader.setExecutionDataVisitor(executionDataVisitor);
      reader.read();
    } else {
      org.jacoco.previous.core.data.ExecutionDataReader reader = new org.jacoco.previous.core.data.ExecutionDataReader(inputStream);
      reader.setSessionInfoVisitor(sessionInfoStore);
      reader.setExecutionDataVisitor(executionDataVisitor);
      reader.read();
    }

    return this;
  }


}
