package com.mtnfog.phileas.configuration;

import org.aeonbits.owner.Config;

/**
 * Phileas configuration using Aeonbits OWNER.
 * Not all of the properties exposed through this file may be documented.
 */
@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({
        "file:application.properties",
        "system:properties",
        "system:env"
})
public interface PhileasConfiguration extends Config {

    // General
    @DefaultValue("./indexes/")
    @Key("indexes.directory")
    String indexesDirectory();

    @DefaultValue("http://localhost:18080/")
    @Key("PHILTER_NER_ENDPOINT")
    String philterNerEndpoint();

    // Span Disambiguation
    @DefaultValue("false")
    @Key("span.disambiguation.enabled")
    boolean spanDisambiguationEnabled();

    @DefaultValue("murmur3")
    @Key("span.disambiguation.hash.algorithm")
    String spanDisambiguationHashAlgorithm();

    @DefaultValue("5")
    @Key("span.window.size")
    int spanWindowSize();

    @DefaultValue("32")
    @Key("span.disambiguation.vector.size")
    int spanDisambiguationVectorSize();

    @DefaultValue("true")
    @Key("span.disambiguation.ignore.stopwords")
    boolean spanDisambiguationIgnoreStopWords();

    @DefaultValue("a, as, able, about, above, according, accordingly, across, actually, after, afterwards, again, against, aint, all, allow, allows, almost, alone, along, already, also, although, always, am, among, amongst, an, and, another, any, anybody, anyhow, anyone, anything, anyway, anyways, anywhere, apart, appear, appreciate, appropriate, are, arent, around, as, aside, ask, asking, associated, at, available, away, awfully, be, became, because, become, becomes, becoming, been, before, beforehand, behind, being, believe, below, beside, besides, best, better, between, beyond, both, brief, but, by, cmon, cs, came, can, cant, cannot, cant, cause, causes, certain, certainly, changes, clearly, co, com, come, comes, concerning, consequently, consider, considering, contain, containing, contains, corresponding, could, couldnt, course, currently, definitely, described, despite, did, didnt, different, do, does, doesnt, doing, dont, done, down, downwards, during, each, edu, eg, eight, either, else, elsewhere, enough, entirely, especially, et, etc, even, ever, every, everybody, everyone, everything, everywhere, ex, exactly, example, except, far, few, ff, fifth, first, five, followed, following, follows, for, former, formerly, forth, four, from, further, furthermore, get, gets, getting, given, gives, go, goes, going, gone, got, gotten, greetings, had, hadnt, happens, hardly, has, hasnt, have, havent, having, he, hes, hello, help, hence, her, here, heres, hereafter, hereby, herein, hereupon, hers, herself, hi, him, himself, his, hither, hopefully, how, howbeit, however, i, id, ill, im, ive, ie, if, ignored, immediate, in, inasmuch, inc, indeed, indicate, indicated, indicates, inner, insofar, instead, into, inward, is, isnt, it, itd, itll, its, its, itself, just, keep, keeps, kept, know, knows, known, last, lately, later, latter, latterly, least, less, lest, let, lets, like, liked, likely, little, look, looking, looks, ltd, mainly, many, may, maybe, me, mean, meanwhile, merely, might, more, moreover, most, mostly, much, must, my, myself, name, namely, nd, near, nearly, necessary, need, needs, neither, never, nevertheless, new, next, nine, no, nobody, non, none, noone, nor, normally, not, nothing, novel, now, nowhere, obviously, of, off, often, oh, ok, okay, old, on, once, one, ones, only, onto, or, other, others, otherwise, ought, our, ours, ourselves, out, outside, over, overall, own, particular, particularly, per, perhaps, placed, please, plus, possible, presumably, probably, provides, que, quite, qv, rather, rd, re, really, reasonably, regarding, regardless, regards, relatively, respectively, right, said, same, saw, say, saying, says, second, secondly, see, seeing, seem, seemed, seeming, seems, seen, self, selves, sensible, sent, serious, seriously, seven, several, shall, she, should, shouldnt, since, six, so, some, somebody, somehow, someone, something, sometime, sometimes, somewhat, somewhere, soon, sorry, specified, specify, specifying, still, sub, such, sup, sure, ts, take, taken, tell, tends, th, than, thank, thanks, thanx, that, thats, thats, the, their, theirs, them, themselves, then, thence, there, theres, thereafter, thereby, therefore, therein, theres, thereupon, these, they, theyd, theyll, theyre, theyve, think, third, this, thorough, thoroughly, those, though, through, throughout, thru, thus, to, together, too, took, toward, towards, tried, tries, truly, try, trying, twice, two, un, under, unfortunately, unless, unlikely, until, unto, up, upon, us, use, used, useful, uses, using, usually, value, various, very, via, viz, vs, want, wants, was, wasnt, way, we, wed, well, were, weve, welcome, well, went, were, werent, what, whats, whatever, when, whence, whenever, where, wheres, whereafter, whereas, whereby, wherein, whereupon, wherever, whether, which, while, whither, who, whos, whoever, whole, whom, whose, why, will, willing, wish, with, within, without, wont, wonder, would, would, wouldnt, yes, yet, you, youd, youll, youre, youve, your, yours, yourself, yourselves")
    @Key("span.disambiguation.stopwords")
    String spanDisambiguationStopWords();

    // Cache Service
    @DefaultValue("false")
    @Key("cache.redis.enabled")
    boolean cacheRedisEnabled();

    @DefaultValue("false")
    @Key("cache.redis.cluster")
    boolean cacheRedisCluster();

    @Key("cache.redis.host")
    String cacheRedisHost();

    @Key("cache.redis.port")
    int cacheRedisPort();

    @Key("cache.redis.auth.token")
    String cacheRedisAuthToken();

    @DefaultValue("false")
    @Key("cache.redis.ssl")
    boolean cacheRedisSsl();

    @Key("cache.redis.truststore")
    String cacheRedisTrustStore();

    @Key("cache.redis.truststore.password")
    String cacheRedisTrustStorePassword();

    @Key("cache.redis.keystore")
    String cacheRedisKeyStore();

    @Key("cache.redis.keystore.password")
    String cacheRedisKeyStorePassword();

    // Filter Profiles
    @DefaultValue("./profiles/")
    @Key("filter.profiles.directory")
    String filterProfilesDirectory();

    @Key("filter.profiles.s3.bucket")
    String filterProfilesS3Bucket();

    @DefaultValue("us-east-1")
    @Key("filter.profiles.s3.region")
    String filterProfilesS3Region();

    // Metrics
    @DefaultValue("philter")
    @Key("metrics.prefix")
    String metricsPrefix();

    @DefaultValue("false")
    @Key("metrics.jmx.enabled")
    boolean metricsJmxEnabled();

    @DefaultValue("false")
    @Key("metrics.datadog.enabled")
    boolean metricsDataDogEnabled();

    @Key("metrics.datadog.apikey")
    String metricsDataDogApiKey();

    @DefaultValue("false")
    @Key("metrics.cloudwatch.enabled")
    boolean metricsCloudWatchEnabled();

    @DefaultValue("us-east-1")
    @Key("metrics.cloudwatch.region")
    String metricsCloudWatchRegion();

    @Key("metrics.cloudwatch.access.key")
    String metricsCloudWatchAccessKey();

    @Key("metrics.cloudwatch.secret.key")
    String metricsCloudWatchSecretKey();

    @Key("metrics.cloudwatch.namespace")
    String metricsCloudWatchNamespace();

    // Store
    @DefaultValue("false")
    @Key("store.enabled")
    boolean storeEnabled();

    @DefaultValue("philter")
    @Key("store.elasticsearch.index")
    String storeElasticSearchIndex();

    @DefaultValue("localhost")
    @Key("store.elasticsearch.host")
    String storeElasticSearchHost();

    @DefaultValue("https")
    @Key("store.elasticsearch.scheme")
    String storeElasticSearchScheme();

    @DefaultValue("9200")
    @Key("store.elasticsearch.port")
    int storeElasticSearchPort();

}
