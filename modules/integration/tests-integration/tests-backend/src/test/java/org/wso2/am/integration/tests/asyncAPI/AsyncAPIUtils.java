package org.wso2.am.integration.tests.asyncAPI;

import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Document;
import org.junit.Assert;

public class AsyncAPIUtils {

    public static void validateUpdatedDefinitionForPublisher(String original, String updated) {
        Aai20Document originalObj = (Aai20Document) Library.readDocumentFromJSONString(original);
        Aai20Document updatedObj = (Aai20Document) Library.readDocumentFromJSONString(updated);
        Assert.assertEquals(originalObj.info.title, updatedObj.info.title);
        Assert.assertEquals(originalObj.info.version, updatedObj.info.version);
        if (originalObj.servers != null && updatedObj.servers != null) {
            if (originalObj.getServers().get(0).url != null && updatedObj.getServers().get(0).url != null) {
                Assert.assertEquals(originalObj.getServers().get(0).url, updatedObj.getServers().get(0).url);
            }
        }
        if (originalObj.channels != null && updatedObj.channels != null) {
            for (String x : originalObj.channels.keySet()){
                if (x != null) {
                    for (String y : updatedObj.channels.keySet()){
                        if (y != null) {
                            Assert.assertEquals(x, y);
                        }
                    }
                }
            }
        }
    }

    public static void validateUpdatedDefinitionForStore(String original, String updated) {
        Aai20Document originalObj = (Aai20Document) Library.readDocumentFromJSONString(original);
        Aai20Document updatedObj = (Aai20Document) Library.readDocumentFromJSONString(updated);
        Assert.assertEquals(originalObj.info.title, updatedObj.info.title);
        Assert.assertEquals(originalObj.info.version, updatedObj.info.version);
    }
}
