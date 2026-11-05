package ai.philterd.phileas.policy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestPhisqlValidate {

    @Test
    public void test() {
        // This returns true because {} is apparently a valid (empty) policy.
        Assertions.assertTrue(PolicySchema.validate("{}"));
    }

    @Test
    public void mapReplaceWithGeneratorsValidatesAgainstSchema() {

        final String json = """
                {
                  "generators": {
                    "vendor-namer": {
                      "type": "ollama",
                      "endpoint": "http://localhost:11434",
                      "model": "llama3.1",
                      "prompt": "Replace {{token}}.",
                      "timeoutMs": 2000
                    }
                  },
                  "identifiers": {
                    "identifiers": [
                      {
                        "identifierFilterStrategies": [
                          {
                            "strategy": "MAP_REPLACE",
                            "mappings": { "Acme Corp": "Widget Co" },
                            "mappingFiles": [ "/etc/philter/vendors.tsv" ],
                            "caseSensitive": false,
                            "generator": "vendor-namer",
                            "fallbackStrategy": "REDACT"
                          }
                        ]
                      }
                    ]
                  }
                }
                """;

        Assertions.assertTrue(PolicySchema.validate(json));

    }

}
