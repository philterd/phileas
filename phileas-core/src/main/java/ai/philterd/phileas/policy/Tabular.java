/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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

import ai.philterd.phileas.model.filtering.RedactionWarning;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.ColumnFilterStrategy;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The policy's {@code tabular} section: column rules for CSV and Excel (.xlsx) documents. A column with a rule
 * is redacted whole, left unchanged, or scanned with only some filters; a column without one follows its sheet's
 * {@code unlistedColumns}. Header rows are never redacted.
 */
public class Tabular {

    /** What happens to the columns of a sheet that have no rule. */
    public enum UnlistedColumns {

        /** Scanned with every filter in {@code identifiers}. */
        IDENTIFIERS("identifiers"),

        /** Left unchanged. */
        IGNORE("ignore"),

        /** Replaced whole with the sheet's {@code unlistedStrategies}, with no detection. */
        REDACT("redact");

        private final String value;

        UnlistedColumns(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

    }

    /** What a column rule does to its column's data cells. */
    public enum Action {

        /** Each data cell is replaced whole by the rule's strategies, with no detection. */
        REDACT("redact"),

        /** The data cells are left unchanged. */
        IGNORE("ignore"),

        /** Each data cell is scanned with only the rule's filters. */
        FILTER("filter");

        private final String value;

        Action(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

    }

    /** A rule for the columns picked by a header or an index. */
    public static class Column {

        @SerializedName("header")
        @Expose
        private String header;

        @SerializedName("index")
        @Expose
        private Integer index;

        @SerializedName("action")
        @Expose
        private Action action;

        @SerializedName("strategies")
        @Expose
        private List<ColumnFilterStrategy> strategies;

        @SerializedName("filters")
        @Expose
        private List<String> filters;

        /** Matched against the displayed text of the last header row, trimmed. {@code null} for an index rule. */
        public String getHeader() {
            return header;
        }

        public void setHeader(final String header) {
            this.header = header;
        }

        /** The column's 1-based position. {@code null} for a header rule. */
        public Integer getIndex() {
            return index;
        }

        public void setIndex(final Integer index) {
            this.index = index;
        }

        public Action getAction() {
            return action;
        }

        public void setAction(final Action action) {
            this.action = action;
        }

        /** The strategies of a {@code redact} rule. */
        public List<ColumnFilterStrategy> getStrategies() {
            return strategies;
        }

        public void setStrategies(final List<ColumnFilterStrategy> strategies) {
            this.strategies = strategies;
        }

        /** The {@code identifiers} keys a {@code filter} rule scans with. */
        public List<String> getFilters() {
            return filters;
        }

        public void setFilters(final List<String> filters) {
            this.filters = filters;
        }

        private String selector() {
            return header != null ? "header '" + header.strip() + "'" : "index " + index;
        }

    }

    /** The column rules for one worksheet, or for every worksheet without an entry and for a CSV document. */
    public static class Sheet {

        @SerializedName("worksheet")
        @Expose
        private String worksheet;

        @SerializedName("headerRows")
        @Expose
        private int headerRows = 1;

        @SerializedName("unlistedColumns")
        @Expose
        private UnlistedColumns unlistedColumns = UnlistedColumns.IDENTIFIERS;

        @SerializedName("unlistedStrategies")
        @Expose
        private List<ColumnFilterStrategy> unlistedStrategies;

        @SerializedName("columns")
        @Expose
        private List<Column> columns = new ArrayList<>();

        // The default for unlistedStrategies, one instance per sheet so it can be wired once like a policy's own.
        private transient List<ColumnFilterStrategy> defaultUnlistedStrategies;

        /** The worksheet name, matched case-insensitively, or {@code null} for the entry without one. */
        public String getWorksheet() {
            return worksheet;
        }

        public void setWorksheet(final String worksheet) {
            this.worksheet = worksheet;
        }

        /** The number of leading header rows, which are never redacted. Default 1. */
        public int getHeaderRows() {
            return headerRows;
        }

        public void setHeaderRows(final int headerRows) {
            this.headerRows = headerRows;
        }

        public UnlistedColumns getUnlistedColumns() {
            return unlistedColumns == null ? UnlistedColumns.IDENTIFIERS : unlistedColumns;
        }

        public void setUnlistedColumns(final UnlistedColumns unlistedColumns) {
            this.unlistedColumns = unlistedColumns;
        }

        /** The policy's {@code unlistedStrategies}, or {@code null} when it sets none. */
        public List<ColumnFilterStrategy> getUnlistedStrategies() {
            return unlistedStrategies;
        }

        public void setUnlistedStrategies(final List<ColumnFilterStrategy> unlistedStrategies) {
            this.unlistedStrategies = unlistedStrategies;
        }

        /** The strategies that replace unlisted columns when they are redacted: a single {@code REDACT} by default. */
        public synchronized List<ColumnFilterStrategy> getEffectiveUnlistedStrategies() {
            if (unlistedStrategies != null) {
                return unlistedStrategies;
            }
            if (defaultUnlistedStrategies == null) {
                defaultUnlistedStrategies = List.of(ColumnFilterStrategy.redact());
            }
            return defaultUnlistedStrategies;
        }

        public List<Column> getColumns() {
            return columns == null ? Collections.emptyList() : columns;
        }

        public void setColumns(final List<Column> columns) {
            this.columns = columns;
        }

        /**
         * Resolves the column rules against a sheet's headers. A header rule applies to every column whose header
         * matches it. When a header rule and an index rule pick the same column, the more protective one applies
         * (redact, then filter, then ignore); two filter rules scan it with both rules' filters, and of two redact
         * rules the one listed first applies. A header rule that matches no column is reported as a warning.
         * @param headers The last header row's displayed text by 1-based column, trimmed.
         * @return The rule for each column that has one, and the warnings.
         */
        public ResolvedColumns resolve(final Map<Integer, String> headers) {

            final Map<Integer, Column> rules = new LinkedHashMap<>();
            final List<RedactionWarning> warnings = new ArrayList<>();

            for (final Column column : getColumns()) {

                if (column.getIndex() != null) {
                    pick(rules, column.getIndex(), column);
                    continue;
                }

                final String wanted = column.getHeader().strip();
                boolean matched = false;
                for (final Map.Entry<Integer, String> header : headers.entrySet()) {
                    if (header.getValue() != null && header.getValue().strip().equals(wanted)) {
                        pick(rules, header.getKey(), column);
                        matched = true;
                    }
                }
                if (!matched) {
                    warnings.add(RedactionWarning.unmatchedHeader(wanted, worksheet));
                }

            }

            return new ResolvedColumns(this, rules, warnings);

        }

        private static void pick(final Map<Integer, Column> rules, final int index, final Column column) {
            final Column existing = rules.get(index);
            if (existing == null || protection(column.getAction()) > protection(existing.getAction())) {
                rules.put(index, column);
            } else if (existing.getAction() == Action.FILTER && column.getAction() == Action.FILTER) {
                final Set<String> filters = new LinkedHashSet<>(existing.getFilters());
                filters.addAll(column.getFilters());
                final Column combined = new Column();
                combined.setAction(Action.FILTER);
                combined.setFilters(new ArrayList<>(filters));
                rules.put(index, combined);
            }
        }

        private static int protection(final Action action) {
            return switch (action) {
                case REDACT -> 2;
                case FILTER -> 1;
                case IGNORE -> 0;
            };
        }

    }

    /** A sheet's column rules resolved against its headers. */
    public record ResolvedColumns(Sheet sheet, Map<Integer, Column> rules, List<RedactionWarning> warnings) {

        /** The rule for a 1-based column, or {@code null} when it follows {@code unlistedColumns}. */
        public Column ruleFor(final int column) {
            return rules.get(column);
        }

    }

    @SerializedName("sheets")
    @Expose
    private List<Sheet> sheets = new ArrayList<>();

    public List<Sheet> getSheets() {
        return sheets == null ? Collections.emptyList() : sheets;
    }

    public void setSheets(final List<Sheet> sheets) {
        this.sheets = sheets;
    }

    /**
     * The sheet entry that applies to a worksheet: the one that names it, case-insensitively, or else the one
     * without a worksheet.
     * @param worksheet The worksheet name, or {@code null} for a CSV document, which only the entry without a
     *                  worksheet applies to.
     * @return The entry, or {@code null} when none applies.
     */
    public Sheet sheetFor(final String worksheet) {
        Sheet unnamed = null;
        for (final Sheet sheet : getSheets()) {
            if (sheet.getWorksheet() == null) {
                unnamed = sheet;
            } else if (worksheet != null && sheet.getWorksheet().equalsIgnoreCase(worksheet)) {
                return sheet;
            }
        }
        return unnamed;
    }

    /**
     * Refuses a {@code filter} rule that names a key not defined in the policy's identifiers.
     * @param identifiers The policy's identifiers.
     */
    public void validate(final Identifiers identifiers) {
        final Set<String> defined = definedKeys(identifiers);
        for (final Sheet sheet : getSheets()) {
            for (final Column column : sheet.getColumns()) {
                if (column.getFilters() == null) {
                    continue;
                }
                for (final String key : column.getFilters()) {
                    if (!defined.contains(key)) {
                        throw new IllegalArgumentException("The tabular column rule for " + column.selector()
                                + " names filter '" + key + "', which is not defined in identifiers.");
                    }
                }
            }
        }
    }

    /**
     * A copy of a policy whose identifiers keep only the given keys and which has no column rules: the policy a
     * {@code filter} rule's cells are scanned with.
     * @param policy The policy.
     * @param keys The {@code identifiers} keys to keep.
     * @return The policy.
     */
    public static Policy restrictIdentifiers(final Policy policy, final List<String> keys) {
        final Gson gson = new Gson();
        final JsonObject json = gson.toJsonTree(policy).getAsJsonObject();
        json.remove("tabular");
        final JsonObject identifiers = json.has("identifiers") && json.get("identifiers").isJsonObject()
                ? json.getAsJsonObject("identifiers") : new JsonObject();
        final JsonObject kept = new JsonObject();
        for (final String key : keys) {
            if (!identifiers.has(key) || identifiers.get(key).isJsonNull()) {
                throw new IllegalArgumentException("The tabular column filter '" + key
                        + "' is not defined in identifiers.");
            }
            kept.add(key, identifiers.get(key));
        }
        json.add("identifiers", kept);
        return gson.fromJson(json, Policy.class);
    }

    private static Set<String> definedKeys(final Identifiers identifiers) {
        final Set<String> keys = new HashSet<>();
        if (identifiers == null) {
            return keys;
        }
        for (final Map.Entry<String, JsonElement> entry : new Gson().toJsonTree(identifiers).getAsJsonObject().entrySet()) {
            if (!entry.getValue().isJsonNull()) {
                keys.add(entry.getKey());
            }
        }
        return keys;
    }

    /**
     * Reads {@code tabular} strictly, as the PhiSQL compilers check it, so a mistake in a column rule fails when
     * the policy loads rather than leaving a column unredacted. Whether a {@code filter} rule's keys are defined in
     * identifiers is checked when the policy's filters are built, by {@link #validate(Identifiers)}.
     */
    public static class Adapter implements JsonDeserializer<Tabular>, JsonSerializer<Tabular> {

        private static final Set<String> COLUMN_STRATEGIES = Set.of(AbstractFilterStrategy.REDACT,
                AbstractFilterStrategy.RANDOM_REPLACE, AbstractFilterStrategy.STATIC_REPLACE,
                AbstractFilterStrategy.CRYPTO_REPLACE, AbstractFilterStrategy.FPE_ENCRYPT_REPLACE,
                AbstractFilterStrategy.HASH_SHA256_REPLACE, AbstractFilterStrategy.LAST_4, AbstractFilterStrategy.MASK,
                AbstractFilterStrategy.TRUNCATE, AbstractFilterStrategy.ABBREVIATE, AbstractFilterStrategy.MAP_REPLACE);

        @Override
        public Tabular deserialize(final JsonElement json, final Type typeOfT,
                                   final JsonDeserializationContext context) throws JsonParseException {

            if (json == null || json.isJsonNull()) {
                return null;
            }

            final JsonObject object = object(json, "The tabular section");
            for (final String key : object.keySet()) {
                if (!"sheets".equals(key)) {
                    throw new JsonParseException("The tabular section has an unknown key '" + key
                            + "'. The valid key is sheets.");
                }
            }

            final JsonElement sheetsJson = object.get("sheets");
            if (sheetsJson == null || !sheetsJson.isJsonArray() || sheetsJson.getAsJsonArray().isEmpty()) {
                throw new JsonParseException("The tabular sheets must be a non-empty list of sheet entries.");
            }

            final Tabular tabular = new Tabular();
            final List<Sheet> sheets = new ArrayList<>();
            final Set<String> worksheets = new HashSet<>();
            boolean unnamed = false;

            for (final JsonElement sheetJson : sheetsJson.getAsJsonArray()) {
                final Sheet sheet = sheet(sheetJson, context);
                if (sheet.getWorksheet() != null) {
                    // Worksheet names match case-insensitively, as Excel compares them.
                    if (!worksheets.add(sheet.getWorksheet().toLowerCase(Locale.ROOT))) {
                        throw new JsonParseException("The tabular section has more than one sheet for worksheet '"
                                + sheet.getWorksheet() + "'.");
                    }
                } else if (unnamed) {
                    throw new JsonParseException("The tabular section has more than one sheet without a worksheet.");
                } else {
                    unnamed = true;
                }
                sheets.add(sheet);
            }

            tabular.setSheets(sheets);
            return tabular;

        }

        private static Sheet sheet(final JsonElement json, final JsonDeserializationContext context) {

            final JsonObject object = object(json, "A tabular sheet");
            final Sheet sheet = new Sheet();

            for (final Map.Entry<String, JsonElement> entry : object.entrySet()) {
                final JsonElement value = entry.getValue();
                switch (entry.getKey()) {
                    case "worksheet" -> {
                        if (!isString(value) || value.getAsString().isBlank()) {
                            throw new JsonParseException("The tabular sheet worksheet must be a non-empty string.");
                        }
                        sheet.setWorksheet(value.getAsString());
                    }
                    case "headerRows" -> sheet.setHeaderRows(wholeNumber(value, 0, "The tabular sheet headerRows"));
                    case "unlistedColumns" -> sheet.setUnlistedColumns(unlistedColumns(value));
                    case "unlistedStrategies" -> sheet.setUnlistedStrategies(strategies(value, context, "unlistedStrategies"));
                    case "columns" -> {
                        if (!value.isJsonArray()) {
                            throw new JsonParseException("The tabular sheet columns must be a list of column rules.");
                        }
                        final List<Column> columns = new ArrayList<>();
                        for (final JsonElement column : value.getAsJsonArray()) {
                            columns.add(column(column, context));
                        }
                        sheet.setColumns(columns);
                    }
                    default -> throw new JsonParseException("A tabular sheet has an unknown key '" + entry.getKey()
                            + "'. Valid keys are worksheet, headerRows, unlistedColumns, unlistedStrategies, and columns.");
                }
            }

            if (sheet.getUnlistedStrategies() != null && sheet.getUnlistedColumns() != UnlistedColumns.REDACT) {
                throw new JsonParseException("The tabular sheet unlistedStrategies applies only when unlistedColumns is redact.");
            }

            final Set<String> selected = new HashSet<>();
            for (final Column column : sheet.getColumns()) {
                if (column.getHeader() != null && sheet.getHeaderRows() == 0) {
                    throw new JsonParseException("The column rule for header '" + column.getHeader()
                            + "' needs headerRows of 1 or more.");
                }
                if (!selected.add(column.selector())) {
                    throw new JsonParseException("A tabular sheet has more than one column rule for "
                            + column.selector() + ".");
                }
            }

            return sheet;

        }

        private static Column column(final JsonElement json, final JsonDeserializationContext context) {

            final JsonObject object = object(json, "A tabular column rule");
            final Column column = new Column();

            for (final Map.Entry<String, JsonElement> entry : object.entrySet()) {
                final JsonElement value = entry.getValue();
                switch (entry.getKey()) {
                    case "header" -> {
                        if (!isString(value) || value.getAsString().isBlank()) {
                            throw new JsonParseException("The tabular column header must be a non-empty string.");
                        }
                        column.setHeader(value.getAsString());
                    }
                    case "index" -> column.setIndex(wholeNumber(value, 1, "The tabular column index"));
                    case "action" -> column.setAction(action(value));
                    case "strategies" -> column.setStrategies(strategies(value, context, "column strategies"));
                    case "filters" -> column.setFilters(filters(value));
                    default -> throw new JsonParseException("A tabular column rule has an unknown key '" + entry.getKey()
                            + "'. Valid keys are header, index, action, strategies, and filters.");
                }
            }

            if ((column.getHeader() == null) == (column.getIndex() == null)) {
                throw new JsonParseException("A tabular column rule needs exactly one of header or index.");
            }
            if (column.getAction() == null) {
                throw new JsonParseException("The tabular column rule for " + column.selector() + " needs an action.");
            }
            if (column.getAction() == Action.REDACT && column.getStrategies() == null) {
                throw new JsonParseException("The redact column rule for " + column.selector() + " needs strategies.");
            }
            if (column.getAction() != Action.REDACT && column.getStrategies() != null) {
                throw new JsonParseException("Strategies apply only to a redact column rule.");
            }
            if (column.getAction() == Action.FILTER && column.getFilters() == null) {
                throw new JsonParseException("The filter column rule for " + column.selector() + " needs filters.");
            }
            if (column.getAction() != Action.FILTER && column.getFilters() != null) {
                throw new JsonParseException("Filters apply only to a filter column rule.");
            }

            return column;

        }

        private static Action action(final JsonElement json) {
            if (isString(json)) {
                for (final Action action : Action.values()) {
                    if (action.getValue().equals(json.getAsString())) {
                        return action;
                    }
                }
            }
            throw new JsonParseException("The tabular column action must be one of redact, ignore, and filter.");
        }

        private static UnlistedColumns unlistedColumns(final JsonElement json) {
            if (isString(json)) {
                for (final UnlistedColumns unlisted : UnlistedColumns.values()) {
                    if (unlisted.getValue().equals(json.getAsString())) {
                        return unlisted;
                    }
                }
            }
            throw new JsonParseException("The tabular sheet unlistedColumns must be one of identifiers, ignore, and redact.");
        }

        private static List<String> filters(final JsonElement json) {
            final List<String> filters = new ArrayList<>();
            if (json.isJsonArray() && !json.getAsJsonArray().isEmpty()) {
                final Set<String> distinct = new LinkedHashSet<>();
                for (final JsonElement filter : json.getAsJsonArray()) {
                    if (!isString(filter) || filter.getAsString().isEmpty() || !distinct.add(filter.getAsString())) {
                        distinct.clear();
                        break;
                    }
                }
                filters.addAll(distinct);
            }
            if (filters.isEmpty()) {
                throw new JsonParseException("The tabular column filters must be a non-empty list of distinct "
                        + "identifiers keys.");
            }
            return filters;
        }

        private static List<ColumnFilterStrategy> strategies(final JsonElement json, final JsonDeserializationContext context,
                                                             final String where) {
            if (!json.isJsonArray() || json.getAsJsonArray().isEmpty()) {
                throw new JsonParseException("The tabular " + where + " must be a non-empty list of strategies.");
            }
            final List<ColumnFilterStrategy> strategies = new ArrayList<>();
            for (final JsonElement element : json.getAsJsonArray()) {
                final JsonObject strategy = object(element, "A tabular " + where + " entry");
                if (strategy.has("condition")) {
                    throw new JsonParseException("The tabular " + where
                            + " cannot have a condition: a whole-column strategy replaces every cell.");
                }
                if (strategy.has("strategy")) {
                    final JsonElement name = strategy.get("strategy");
                    if (!isString(name) || !COLUMN_STRATEGIES.contains(name.getAsString())) {
                        throw new JsonParseException("The tabular " + where + " has an unknown strategy " + name
                                + ". Valid strategies are " + String.join(", ", new TreeSet<>(COLUMN_STRATEGIES)) + ".");
                    }
                }
                strategies.add(context.deserialize(strategy, ColumnFilterStrategy.class));
            }
            return strategies;
        }

        private static int wholeNumber(final JsonElement json, final int minimum, final String name) {
            if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
                try {
                    final int value = new BigDecimal(json.getAsString()).intValueExact();
                    if (value >= minimum) {
                        return value;
                    }
                } catch (final ArithmeticException | NumberFormatException e) {
                    // Refused below.
                }
            }
            throw new JsonParseException(name + " must be a whole number of " + minimum + " or more.");
        }

        private static boolean isString(final JsonElement json) {
            return json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isString();
        }

        private static JsonObject object(final JsonElement json, final String name) {
            if (!json.isJsonObject()) {
                throw new JsonParseException(name + " must be an object.");
            }
            return json.getAsJsonObject();
        }

        // A strategy's condition is written even when empty; a column strategy has none, so it is left out.
        private static JsonArray strategies(final List<ColumnFilterStrategy> strategies, final JsonSerializationContext context) {
            final JsonArray array = new JsonArray();
            for (final ColumnFilterStrategy strategy : strategies) {
                final JsonElement json = context.serialize(strategy, ColumnFilterStrategy.class);
                if (json.isJsonObject()) {
                    json.getAsJsonObject().remove("condition");
                }
                array.add(json);
            }
            return array;
        }

        @Override
        public JsonElement serialize(final Tabular tabular, final Type typeOfSrc, final JsonSerializationContext context) {

            final JsonArray sheets = new JsonArray();

            for (final Sheet sheet : tabular.getSheets()) {

                final JsonObject sheetJson = new JsonObject();
                if (sheet.getWorksheet() != null) {
                    sheetJson.addProperty("worksheet", sheet.getWorksheet());
                }
                sheetJson.addProperty("headerRows", sheet.getHeaderRows());
                sheetJson.addProperty("unlistedColumns", sheet.getUnlistedColumns().getValue());
                if (sheet.getUnlistedStrategies() != null) {
                    sheetJson.add("unlistedStrategies", strategies(sheet.getUnlistedStrategies(), context));
                }

                final JsonArray columns = new JsonArray();
                for (final Column column : sheet.getColumns()) {
                    final JsonObject columnJson = new JsonObject();
                    if (column.getHeader() != null) {
                        columnJson.addProperty("header", column.getHeader());
                    } else {
                        columnJson.addProperty("index", column.getIndex());
                    }
                    columnJson.addProperty("action", column.getAction().getValue());
                    if (column.getStrategies() != null) {
                        columnJson.add("strategies", strategies(column.getStrategies(), context));
                    }
                    if (column.getFilters() != null) {
                        columnJson.add("filters", context.serialize(column.getFilters()));
                    }
                    columns.add(columnJson);
                }
                sheetJson.add("columns", columns);

                sheets.add(sheetJson);

            }

            final JsonObject object = new JsonObject();
            object.add("sheets", sheets);
            return object;

        }

    }

}
