package com.mtnfog.phileas.model.profile.filters.strategies;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.conditions.ParsedCondition;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Replacement;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.services.AnonymizationService;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.UUID;

public abstract class AbstractFilterStrategy {

    public static final String DEFAULT_REDACTION = "{{{REDACTED-%t}}}";

    public static final String REDACT = "REDACT";
    public static final String RANDOM_REPLACE = "RANDOM_REPLACE";
    public static final String STATIC_REPLACE = "STATIC_REPLACE";
    public static final String CRYPTO_REPLACE = "CRYPTO_REPLACE";
    public static final String HASH_SHA256_REPLACE = "HASH_SHA256_REPLACE";

    public static final String HASH_SALT = "HASH_SALT";

    public static final String REPLACEMENT_SCOPE_DOCUMENT = "DOCUMENT";
    public static final String REPLACEMENT_SCOPE_CONTEXT = "CONTEXT";

    public static final String TOKEN = "token";
    public static final String STARTSWITH = "startswith";
    public static final String EQUALS = "==";
    public static final String NOT_EQUALS = "!=";
    public static final String GREATER_THAN = ">";
    public static final String LESS_THAN = "<";
    public static final String GREATER_THAN_EQUALS = ">=";
    public static final String LESS_THAN_EQUALS = "<=";

    public static final String CONTEXT = "context";
    public static final String TYPE = "type";
    public static final String CONFIDENCE = "confidence";
    public static final String CLASSIFICATION = "classification";

    @SerializedName("id")
    @Expose
    protected String id = UUID.randomUUID().toString();

    @SerializedName("strategy")
    @Expose
    protected String strategy = REDACT;

    @SerializedName("redactionFormat")
    @Expose
    protected String redactionFormat = DEFAULT_REDACTION;

    @SerializedName("replacementScope")
    @Expose
    protected String replacementScope = REPLACEMENT_SCOPE_DOCUMENT;

    @SerializedName("staticReplacement")
    @Expose
    protected String staticReplacement = "";

    @SerializedName("condition")
    @Expose
    protected String condition = "";

    @SerializedName("alert")
    @Expose
    protected boolean alert = false;

    @SerializedName("salt")
    @Expose
    protected boolean salt;

    /**
     * Gets the replacement for a token.
     * @param context The context.
     * @param documentId The document ID.
     * @param token The token.
     * @param crypto The encryption key used to encrypt values when enabled.
     * @param anonymizationService The {@link AnonymizationService} for the token.
     * @return A replacement value for a token.
     */
    public abstract Replacement getReplacement(String classification, String context, String documentId, String token, Crypto crypto, AnonymizationService anonymizationService) throws Exception;

    /**
     * Evaluates the condition on the given token.
     * @param context The context.
     * @param documentId The document ID.
     * @param token The token.
     * @param confidence The span's confidence.
     * @param classification The span's classification.
     * @return <code>true</code> if the condition matches; otherwise <code>false</code>.
     */
    public abstract boolean evaluateCondition(String context, String documentId, String token, String condition, double confidence, String classification);

    protected String getRedactedToken(String token, String label, FilterType filterType) {

        String replacement = getValueOrDefault(redactionFormat, DEFAULT_REDACTION)
                .replaceAll("%t", filterType.getType());

        if(StringUtils.isNotEmpty(label)) {
            replacement = replacement.replaceAll("%l", label);
        }

        replacement = replacement.replaceAll("%v", token);

        return replacement;

    }

    /**
     * Evaluates a token condition.
     * @param parsedCondition The {@link ParsedCondition} to evaluate.
     * @param token The token.
     * @return <code>true</code> if the condition is satisfied; otherwise <code>false</code>.
     */
    protected boolean evaluateTokenCondition(ParsedCondition parsedCondition, String token) {

        boolean conditionSatisfied = false;

        final String value = parsedCondition.getValue().replace("\"", "");

        switch (parsedCondition.getOperator().toLowerCase()) {
            case STARTSWITH:
                conditionSatisfied = (token.startsWith(value));
                break;
            case EQUALS:
                conditionSatisfied = (token.equalsIgnoreCase(value));
                break;
        }

        return conditionSatisfied;

    }

    /**
     * Gets the value of an object or the <code>defaultValue</code> if the value is <code>null</code>.
     * @param value The value.
     * @param defaultValue The default value.
     * @param <T> The type of object.
     * @return The value of the object or the <code>defaultValue</code> if the value is <code>null</code>.
     */
    protected static<T> T getValueOrDefault(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * Gets an anonymized token for a token. This function is called by <code>getReplacement</code>.
     * @param context The context.
     * @param token The token.
     * @param anonymizationService The {@link AnonymizationService} for the token.
     * @return An anonymized version of the token, or <code>null</code> if the token has already been anonymized.
     * @throws IOException Thrown if the cache service is not accessible.
     */
    protected String getAnonymizedToken(String context, String token, AnonymizationService anonymizationService) throws IOException {

        String replacement = null;

        // Have we seen this token in this context before?
        if (anonymizationService.getAnonymizationCacheService().contains(context, token)) {

            // Yes, we have previously seen this token in this context.
            replacement = anonymizationService.getAnonymizationCacheService().get(context, token);

        } else {

            // Make sure we aren't trying to anonymize a token we have already anonymized.
            if (anonymizationService.getAnonymizationCacheService().containsValue(context, token)) {

                // This token is the result of an already replaced value.
                // So the "replacement" is null. The filter won't replace the token when the replacement value is null.
                replacement = null;

            } else {

                // This is not an already anonymized token.
                replacement = anonymizationService.anonymize(token);
                anonymizationService.getAnonymizationCacheService().put(context, token, replacement);

            }

        }

        return replacement;

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getRedactionFormat() {
        return redactionFormat;
    }

    public void setRedactionFormat(String redactionFormat) {
        this.redactionFormat = redactionFormat;
    }

    public String getReplacementScope() {
        return replacementScope;
    }

    public void setReplacementScope(String replacementScope) {
        this.replacementScope = replacementScope;
    }

    public String getStaticReplacement() {
        return staticReplacement;
    }

    public void setStaticReplacement(String staticReplacement) {
        this.staticReplacement = staticReplacement;
    }

    public void setConditions(String condition) {
        this.condition = condition;
    }

    public String getCondition() {
        return condition;
    }

    public boolean isAlert() {
        return alert;
    }

    public void setAlert(boolean alert) {
        this.alert = alert;
    }

    public boolean isSalt() {
        return salt;
    }

    public void setSalt(boolean salt) {
        this.salt = salt;
    }

}