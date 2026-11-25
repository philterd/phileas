/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.policy;

import ai.philterd.phileas.policy.config.Analysis;
import ai.philterd.phileas.policy.config.Csv;
import ai.philterd.phileas.policy.config.Email;
import ai.philterd.phileas.policy.config.Pdf;
import ai.philterd.phileas.policy.config.Review;
import ai.philterd.phileas.policy.config.Splitting;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

public class Config {

    @SerializedName("splitting")
    @Expose
    private Splitting splitting = new Splitting();

    @SerializedName("pdf")
    @Expose
    private Pdf pdf = new Pdf();

    @SerializedName("postFilters")
    @Expose
    private PostFilters postFilters = new PostFilters();

    @SerializedName("analysis")
    @Expose
    private Analysis analysis = new Analysis();

    @SerializedName("review")
    @Expose
    private Review review = new Review();

    @SerializedName("email")
    @Expose
    @JsonAdapter(Email.Adapter.class)
    private Email email = new Email();

    @SerializedName("csv")
    @Expose
    @JsonAdapter(Csv.Adapter.class)
    private Csv csv = new Csv();

    public Splitting getSplitting() {
        return splitting;
    }

    public void setSplitting(Splitting splitting) {
        this.splitting = splitting;
    }

    public Pdf getPdf() {
        return pdf;
    }

    public void setPdf(Pdf pdf) {
        this.pdf = pdf;
    }

    public PostFilters getPostFilters() {
        return postFilters;
    }

    public void setPostFilters(PostFilters postFilters) {
        this.postFilters = postFilters;
    }

    public Analysis getAnalysis() {
        return analysis;
    }

    public void setAnalysis(Analysis analysis) {
        this.analysis = analysis;
    }

    public Review getReview() {
        return review;
    }

    public void setReview(Review review) {
        this.review = review;
    }

    /**
     * How an email message is redacted beyond its text.
     * @return The {@link Email} settings, with the defaults when the policy has none.
     */
    public Email getEmail() {
        return email == null ? new Email() : email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    /**
     * How a CSV document is read.
     * @return The {@link Csv} settings, with the defaults when the policy has none.
     */
    public Csv getCsv() {
        return csv == null ? new Csv() : csv;
    }

    public void setCsv(Csv csv) {
        this.csv = csv;
    }

}
