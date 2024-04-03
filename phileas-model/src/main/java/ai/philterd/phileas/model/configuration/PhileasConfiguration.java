package ai.philterd.phileas.model.configuration;

import org.apache.commons.lang3.StringUtils;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class PhileasConfiguration {

    private final String DEFAULT_STOP_WORDS = "a, as, able, about, above, according, accordingly, across, actually, after, afterwards, again, against, aint, all, allow, allows, almost, alone, along, already, also, although, always, am, among, amongst, an, and, another, any, anybody, anyhow, anyone, anything, anyway, anyways, anywhere, apart, appear, appreciate, appropriate, are, arent, around, as, aside, ask, asking, associated, at, available, away, awfully, be, became, because, become, becomes, becoming, been, before, beforehand, behind, being, believe, below, beside, besides, best, better, between, beyond, both, brief, but, by, cmon, cs, came, can, cant, cannot, cant, cause, causes, certain, certainly, changes, clearly, co, com, come, comes, concerning, consequently, consider, considering, contain, containing, contains, corresponding, could, couldnt, course, currently, definitely, described, despite, did, didnt, different, do, does, doesnt, doing, dont, done, down, downwards, during, each, edu, eg, eight, either, else, elsewhere, enough, entirely, especially, et, etc, even, ever, every, everybody, everyone, everything, everywhere, ex, exactly, example, except, far, few, ff, fifth, first, five, followed, following, follows, for, former, formerly, forth, four, from, further, furthermore, get, gets, getting, given, gives, go, goes, going, gone, got, gotten, greetings, had, hadnt, happens, hardly, has, hasnt, have, havent, having, he, hes, hello, help, hence, her, here, heres, hereafter, hereby, herein, hereupon, hers, herself, hi, him, himself, his, hither, hopefully, how, howbeit, however, i, id, ill, im, ive, ie, if, ignored, immediate, in, inasmuch, inc, indeed, indicate, indicated, indicates, inner, insofar, instead, into, inward, is, isnt, it, itd, itll, its, its, itself, just, keep, keeps, kept, know, knows, known, last, lately, later, latter, latterly, least, less, lest, let, lets, like, liked, likely, little, look, looking, looks, ltd, mainly, many, may, maybe, me, mean, meanwhile, merely, might, more, moreover, most, mostly, much, must, my, myself, name, namely, nd, near, nearly, necessary, need, needs, neither, never, nevertheless, new, next, nine, no, nobody, non, none, noone, nor, normally, not, nothing, novel, now, nowhere, obviously, of, off, often, oh, ok, okay, old, on, once, one, ones, only, onto, or, other, others, otherwise, ought, our, ours, ourselves, out, outside, over, overall, own, particular, particularly, per, perhaps, placed, please, plus, possible, presumably, probably, provides, que, quite, qv, rather, rd, re, really, reasonably, regarding, regardless, regards, relatively, respectively, right, said, same, saw, say, saying, says, second, secondly, see, seeing, seem, seemed, seeming, seems, seen, self, selves, sensible, sent, serious, seriously, seven, several, shall, she, should, shouldnt, since, six, so, some, somebody, somehow, someone, something, sometime, sometimes, somewhat, somewhere, soon, sorry, specified, specify, specifying, still, sub, such, sup, sure, ts, take, taken, tell, tends, th, than, thank, thanks, thanx, that, thats, thats, the, their, theirs, them, themselves, then, thence, there, theres, thereafter, thereby, therefore, therein, theres, thereupon, these, they, theyd, theyll, theyre, theyve, think, third, this, thorough, thoroughly, those, though, through, throughout, thru, thus, to, together, too, took, toward, towards, tried, tries, truly, try, trying, twice, two, un, under, unfortunately, unless, unlikely, until, unto, up, upon, us, use, used, useful, uses, using, usually, value, various, very, via, viz, vs, want, wants, was, wasnt, way, we, wed, well, were, weve, welcome, well, went, were, werent, what, whats, whatever, when, whence, whenever, where, wheres, whereafter, whereas, whereby, wherein, whereupon, wherever, whether, which, while, whither, who, whos, whoever, whole, whom, whose, why, will, willing, wish, with, within, without, wont, wonder, would, would, wouldnt, yes, yet, you, youd, youll, youre, youve, your, yours, yourself, yourselves";

    private final Properties properties;
    private final String applicationName;

    public PhileasConfiguration(final String propertyFileName, final String applicationName) throws IOException {

        final FileReader fileReader = new FileReader(propertyFileName);
        this.properties = new Properties();
        properties.load(fileReader);

        this.applicationName = applicationName;

    }

    public PhileasConfiguration(final Properties properties, final String applicationName) throws IOException {

        this.properties = properties;
        this.applicationName = applicationName;

    }

    public String indexesDirectory() {
        return getProperty("indexes.directory", "./indexes/");
    }

    public String nerEndpoint() {
        return getProperty("ner.endpoint", "http://localhost:8080/");
    }

    public int nerTimeout() {
        return Integer.parseInt(getProperty("ner.timeout.sec", "600"));
    }

    public int nerMaxIdleConnections() {
        return Integer.parseInt(getProperty("ner.max.idle.connections", "30"));
    }

    public int nerKeepAliveDuration() {
        return Integer.parseInt(getProperty("ner.keep.alive.duration.ms", "30"));
    }

    public boolean spanDisambiguationEnabled() {
        return Boolean.parseBoolean(getProperty("span.disambiguation.enabled", "false"));
    }

    public String spanDisambiguationHashAlgorithm() {
        return getProperty("span.disambiguation.hash.algorithm", "murmur3");
    }

    public int spanDisambiguationVectorSize() {
        return Integer.parseInt(getProperty("span.disambiguation.vector.size", "512"));
    }

    public boolean spanDisambiguationIgnoreStopWords() {
        return Boolean.parseBoolean(getProperty("span.disambiguation.ignore.stopwords", "true"));
    }

    public String spanDisambiguationStopWords() {
        return getProperty("span.disambiguation.stopwords", DEFAULT_STOP_WORDS);
    }

    public int spanWindowSize() {
        return Integer.parseInt(getProperty("span.window.size", "5"));
    }

    public double bloomFilterFpp() {
        return Double.parseDouble(getProperty("filter.fpp", "0.05"));
    }

    // Caching

    public boolean cacheRedisEnabled() {
        return Boolean.parseBoolean(getProperty("cache.redis.enabled", "false"));
    }

    public boolean cacheRedisCluster() {
        return Boolean.parseBoolean(getProperty("cache.redis.cluster", "false"));
    }

    public String cacheRedisHost() {
        return getProperty("cache.redis.host", "localhost");
    }

    public int cacheRedisPort() {
        return Integer.parseInt(getProperty("cache.redis.port", "6379"));
    }

    public String cacheRedisAuthToken() {
        return getProperty("cache.redis.auth.token", "");
    }

    public boolean cacheRedisSsl() {
        return Boolean.parseBoolean(getProperty("cache.redis.ssl", "false"));
    }

    public String cacheRedisTrustStore() {
        return getProperty("cache.redis.truststore", "");
    }

    public String cacheRedisTrustStorePassword() {
        return getProperty("cache.redis.truststore.password", "");
    }

    public String cacheRedisKeyStore() {
        return getProperty("cache.redis.keystore", "");
    }

    public String cacheRedisKeyStorePassword() {
        return getProperty("cache.redis.keystore.password", "");
    }

    // Policies

    public String policiesDirectory() {
        return getProperty("filter.policies.directory", "./policies/");
    }

    public String policiesS3Bucket() {
        return getProperty("filter.policies.s3.bucket", "");
    }

    public String policiesS3Prefix() {
        return getProperty("filter.policies.s3.prefix", "");
    }

    public String policiesS3Region() {
        return getProperty("filter.policies.s3.region", "us-east-1");
    }

    // Metrics

    public String metricsPrefix() {
        return getProperty("metrics.prefix", applicationName);
    }

    // See: https://github.com/micrometer-metrics/micrometer/blob/master/implementations/micrometer-registry-prometheus/src/main/java/io/micrometer/prometheus/PrometheusConfig.java
    // The step size to use in computing windowed statistics like max. The default is 1 minute.
    // To get the most out of these statistics, align the step interval to be close to your scrape interval.
    public int metricsStep() {
        return Integer.parseInt(getProperty("metrics.step", "60"));
    }

    public boolean metricsJmxEnabled() {
        return Boolean.parseBoolean(getProperty("metrics.jmx.enabled", "false"));
    }

    public boolean metricsPrometheusEnabled() {
        return Boolean.parseBoolean(getProperty("metrics.prometheus.enabled", "false"));
    }

    public int metricsPrometheusPort() {
        return Integer.parseInt(getProperty("metrics.prometheus.port", "9100"));
    }

    public String metricsPrometheusContext() {
        return getProperty("metrics.prometheus.context", "metrics");
    }

    public boolean metricsDataDogEnabled() {
        return Boolean.parseBoolean(getProperty("metrics.datadog.enabled", "false"));
    }

    public String metricsDataDogApiKey() {
        return getProperty("metrics.datadog.apikey", "metrics");
    }

    public boolean metricsCloudWatchEnabled() {
        return Boolean.parseBoolean(getProperty("metrics.cloudwatch.enabled", "false"));
    }

    public String metricsCloudWatchRegion() {
        return String.valueOf(getProperty("metrics.cloudwatch.region", "us-east-1"));
    }

    public String metricsCloudWatchNamespace() {
        return String.valueOf(getProperty("metrics.cloudwatch.namespace", applicationName));
    }

    public String metricsHostname() {
        return String.valueOf(getProperty("metrics.hostname", ""));
    }

    private String getProperty(final String property, final String defaultValue) {

        final String environmentVariableValue = getEnvironmentVariable(property);

        if(!StringUtils.isEmpty(environmentVariableValue)) {
            return environmentVariableValue;
        }

        final String systemPropertyValue = getSystemProperty(property);

        if(!StringUtils.isEmpty(systemPropertyValue)) {
            return systemPropertyValue;
        }

        final String propertyFileValue = getFileProperty(property);

        if(!StringUtils.isEmpty(propertyFileValue)) {
            return propertyFileValue;
        }

        return defaultValue;

    }

    private String getEnvironmentVariable(final String environmentVariable) {
        return System.getenv(environmentVariable);
    }

    private String getSystemProperty(final String property) {
        return System.getProperty(property);
    }

    private String getFileProperty(final String property) {
        return properties.getProperty(property);
    }

}
