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

import com.sun.corba.se.spi.ior.ObjectId;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ExecutionDataReaderStrategy {

    public ExecutionDataReaderPack readExecutionDataFromFile(File jacocoExecutionData) {
        if (jacocoExecutionData == null || !jacocoExecutionData.isFile()) {
            JaCoCoExtensions.LOG.info("Project coverage is set to 0% as no JaCoCo execution data has been dumped: {}", jacocoExecutionData);
            jacocoExecutionData = null;
        }
        ExecutionDataVisitor executionDataVisitor = new ExecutionDataVisitor();
        boolean currentReportFormat = CurrentReportFormatDetector.isCurrentReportFormat(jacocoExecutionData);

        try {
            JaCoCoExtensions.LOG.info("Analysing {}", jacocoExecutionData);
            new JacocoReportReader(new FileInputStream(jacocoExecutionData), currentReportFormat)
                    .readJacocoReport(executionDataVisitor, executionDataVisitor);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Unable to read %s", jacocoExecutionData.getAbsolutePath()), e);
        }

        return new ExecutionDataReaderPack(executionDataVisitor, currentReportFormat);
    }


    public ExecutionDataReaderPack readExecutionDataFromHttp(String url, String documentId) {
        if (url == null) {
            JaCoCoExtensions.LOG.info("Project coverage is set to 0% as no JaCoCo execution data has been found via URL: {}", url);
        }
        ExecutionDataVisitor executionDataVisitor = new ExecutionDataVisitor();

        File tempFile = null;
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setDoOutput(false);
            connection.setDoInput(true);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.setUseCaches(false);

            tempFile = File.createTempFile("sonar-" + System.currentTimeMillis(), "-jacoco.exec");
            tempFile.deleteOnExit();
            long bytesReceived = Files.copy(connection.getInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            JaCoCoExtensions.LOG.info("Received {} bytes of JaCoCo report", bytesReceived);

        } catch (IOException e) {
            JaCoCoExtensions.LOG.error("Exception while trying to connect to URL: {}", url, e);
        }

        boolean currentReportFormat;
        try (InputStream stream = new FileInputStream(tempFile)) {
            currentReportFormat = CurrentReportFormatDetector.isCurrentReportFormat(stream, documentId);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Unable to read %s", documentId), e);
        }

        JaCoCoExtensions.LOG.info("Using JaCoCo report over HTTP ");
        try (InputStream stream = new FileInputStream(tempFile)) {
            new JacocoReportReader(stream, currentReportFormat)
                    .readJacocoReport(executionDataVisitor, executionDataVisitor);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Unable to read %s", documentId), e);
        }

        return new ExecutionDataReaderPack(executionDataVisitor, currentReportFormat);
    }


}
