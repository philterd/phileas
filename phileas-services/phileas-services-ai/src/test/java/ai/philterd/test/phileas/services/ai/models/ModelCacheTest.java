package ai.philterd.test.phileas.services.ai.models;

import ai.philterd.phileas.service.ai.models.ModelCache;
import opennlp.tools.doccat.DocumentCategorizerME;
import org.junit.Assert;
import org.junit.Test;

public class ModelCacheTest {

    @Test
    public void getDoesntExist() {

        final ModelCache modelCache = ModelCache.getInstance();
        final DocumentCategorizerME documentCategorizerME = modelCache.get("doesnt-exist");

        Assert.assertNull(documentCategorizerME);

    }

}
