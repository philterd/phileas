/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.philterd.phileas.benchmarks;

import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public class Result {

    @SerializedName("phileas_version")
    private String phileasVersion;

    private String document;
    private String redactor;

    @SerializedName("workload_millis")
    private long workloadMillis;

    @SerializedName("calls_per_sec")
    private Map<Integer, Long> callsPerSecond;

    @SerializedName("run_id")
    private String runId;

    @SerializedName("branch")
    private String branch;

    private long timestamp;

    public Result() throws IOException, InterruptedException {

        this.callsPerSecond = new HashMap<>();
        this.phileasVersion = System.getProperty("phileasVersion");
        this.timestamp = System.currentTimeMillis();
        this.runId = UUID.randomUUID().toString();

        Process process = Runtime.getRuntime().exec("git rev-parse --abbrev-ref HEAD");
        process.waitFor();

        byte[] output = process.getInputStream().readAllBytes();
        this.branch = new String(output).trim();

    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    public String getRedactor() {
        return redactor;
    }

    public void setRedactor(String redactor) {
        this.redactor = redactor;
    }

    public long getWorkloadMillis() {
        return workloadMillis;
    }

    public void setWorkloadMillis(long workloadMillis) {
        this.workloadMillis = workloadMillis;
    }

    public Map<Integer, Long> getCallsPerSecond() {
        return callsPerSecond;
    }

    public void setCallsPerSecond(Map<Integer, Long> callsPerSecond) {
        this.callsPerSecond = callsPerSecond;
    }

    public String getPhileasVersion() {
        return phileasVersion;
    }

    public void setPhileasVersion(String phileasVersion) {
        this.phileasVersion = phileasVersion;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }
}
