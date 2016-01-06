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

import com.google.common.base.Preconditions;
import org.jacoco.core.data.ExecutionDataWriter;

import javax.annotation.Nullable;
import java.io.*;

public class CurrentReportFormatDetector {

    public static boolean isCurrentReportFormat(@Nullable File jacocoExecutionData) {
        if (jacocoExecutionData == null) {
            return true;
        }
        String absolutePath = jacocoExecutionData.getAbsolutePath();
        try {
            return isCurrentReportFormat(new FileInputStream(jacocoExecutionData), absolutePath);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File" + absolutePath + " not found");
        }
    }

    public static boolean isCurrentReportFormat(InputStream inputStream, String sourceId) {

        try (DataInputStream dis = new DataInputStream(inputStream)) {
            byte firstByte = dis.readByte();
            Preconditions.checkState(firstByte == ExecutionDataWriter.BLOCK_HEADER);
            Preconditions.checkState(dis.readChar() == ExecutionDataWriter.MAGIC_NUMBER);
            char version = dis.readChar();
            boolean isCurrentFormat = version == ExecutionDataWriter.FORMAT_VERSION;
            if (!isCurrentFormat) {
                JaCoCoExtensions.LOG.warn("You are not using the latest JaCoCo binary format version, please consider upgrading to latest JaCoCo version.");
            }
            return isCurrentFormat;
        } catch (IOException | IllegalStateException e) {
            throw new IllegalStateException(String.format("Unable to read %s to determine JaCoCo binary format.", sourceId), e);
        }
    }
}
