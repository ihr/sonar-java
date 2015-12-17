/*
 * SonarQube Java
 * Copyright (C) 2010 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.jacoco;


import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import org.bson.types.ObjectId;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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


    public ExecutionDataReaderPack readExecutionDataFromMongoDB(MongoClientURI mongoDbUrl, String documentId) {
        if (mongoDbUrl == null) {
            JaCoCoExtensions.LOG.info("Project coverage is set to 0% as no JaCoCo execution data has been found in MongoDB URL: {}", mongoDbUrl);
        }
        ExecutionDataVisitor executionDataVisitor = new ExecutionDataVisitor();


        MongoClient mongoClient = new MongoClient(mongoDbUrl);
        GridFSBucket gridFSBucket = GridFSBuckets.create(mongoClient.getDatabase(mongoDbUrl.getDatabase()), "jacoco");
        boolean currentReportFormat = CurrentReportFormatDetector.isCurrentReportFormat(gridFSBucket.openDownloadStream(new ObjectId(documentId)), documentId);

        JacocoReportReader jacocoReportReader;
        JaCoCoExtensions.LOG.info("Analysing MongoDB JaCoCo analze document {}", documentId);
        try(GridFSDownloadStream stream = gridFSBucket.openDownloadStream(new ObjectId(documentId))) {
            jacocoReportReader = new JacocoReportReader(stream, currentReportFormat)
                    .readJacocoReport(executionDataVisitor, executionDataVisitor);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Unable to read %s", documentId), e);
        }

        return new ExecutionDataReaderPack(executionDataVisitor, jacocoReportReader, currentReportFormat);
    }


}
