/*
 *     Copyright 2023 Philerd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.model.objects;

import com.google.gson.Gson;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class Alert implements Serializable {

    private final transient DateFormat utcDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private String id;
    private String filterProfile;
    private String strategyId;
    private String context;
    private String documentId;
    private String filterType;
    private String date;

    public Alert() {

    }

    public Alert(String filterProfile, String strategyId, String context, String documentId, String filterType) {

        this.id = UUID.randomUUID().toString();
        this.filterProfile = filterProfile;
        this.strategyId = strategyId;
        this.context = context;
        this.documentId = documentId;
        this.filterType = filterType;
        this.date = utcDateFormat.format(new Date());

    }

    @Override
    public String toString() {

        final Gson gson = new Gson();
        return gson.toJson(this);

    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).
                append(id).
                append(filterProfile).
                append(strategyId).
                append(context).
                append(documentId).
                append(filterType).
                append(date).
                toHashCode();
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStrategyId() {
        return strategyId;
    }

    public void setStrategyId(String strategyId) {
        this.strategyId = strategyId;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getFilterType() {
        return filterType;
    }

    public void setFilterType(String filterType) {
        this.filterType = filterType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFilterProfile() {
        return filterProfile;
    }

    public void setFilterProfile(String filterProfile) {
        this.filterProfile = filterProfile;
    }

}
