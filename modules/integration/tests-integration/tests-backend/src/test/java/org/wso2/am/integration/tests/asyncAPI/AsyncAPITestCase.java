package org.wso2.am.integration.tests.asyncAPI;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONObject;
import org.junit.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.AsyncAPISpecificationValidationResponseDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
public class AsyncAPITestCase extends APIMIntegrationBaseTest {

    private String apiId;
    private String apiImportId;
    private String resourcePath;
    private String oasVersion;

    @Factory(dataProvider = "userModeDataProvider")
    public AsyncAPITestCase(TestUserMode userMode, String oasVersion) {
        this.userMode = userMode;
        this.oasVersion = oasVersion;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        resourcePath = "asyncAPI" + File.separator;
    }

    @Test(groups = { "wso2.am" }, description = "API creation")
    public void testNewAPI() throws Exception {
        String apiData = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(resourcePath + "apiData.json"), "UTF-8");
        ObjectMapper objectMapper = new ObjectMapper();
        APIDTO apidto = objectMapper.readValue(apiData, APIDTO.class);
        apidto.setProvider(user.getUserName());
        APIDTO responseApiDto = restAPIPublisher.addAPI(apidto, oasVersion);
        apiId = responseApiDto.getId();
        restAPIPublisher.changeAPILifeCycleStatus(apiId, Constants.PUBLISHED);

        testUpdatedAsyncAPIDefinitionInPublisher(apidto, oasVersion);
        testUpdatedAsyncAPIDefinitionInStore(apidto, oasVersion);

    }

    @Test(groups = { "wso2.am" }, description = "API update", dependsOnMethods = "testNewAPI")
    public void testAPIUpdate() throws Exception {
        String updatedAPIData = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(resourcePath + "apiUpdateData.json"),
                "UTF-8");

        ObjectMapper objectMapper = new ObjectMapper();
        APIDTO apidto = objectMapper.readValue(updatedAPIData, APIDTO.class);
        apidto.setProvider(user.getUserName());
        APIDTO updatedApiDto = restAPIPublisher.updateAPI(apidto, apiId);

        testUpdatedAsyncAPIDefinitionInPublisher(updatedApiDto, oasVersion);
        testUpdatedAsyncAPIDefinitionInStore(updatedApiDto, oasVersion);
    }

    @Test(groups = { "wso2.am" }, description = "API definition update", dependsOnMethods = "testAPIUpdate")
    public void testAPIDefinitionUpdate() throws Exception {
        String originalDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream( resourcePath + "AsyncAPI.json"),
                "UTF-8");
        String responseDefinition = restAPIPublisher.updateAsyncAPI(apiId, originalDefinition);

        APIDTO apidto = restAPIPublisher.getAPIByID(apiId, user.getUserDomain());
        Assert.assertNotNull(apidto);

        String publisherDefinition = restAPIPublisher.getAsyncAPIById(apiId);
        String storeDefinition = restAPIStore.getAsyncAPIById(apiId, user.getUserDomain());

        validateAsyncAPIDefinition(responseDefinition);
        validateAsyncAPIDefinition(publisherDefinition);
        validateAsyncAPIDefinition(storeDefinition);

        AsyncAPIUtils.validateUpdatedDefinitionForPublisher(originalDefinition, publisherDefinition);
        AsyncAPIUtils.validateUpdatedDefinitionForStore(originalDefinition, storeDefinition);
        //AsyncAPIUtils.validateUpdatedDefinition(originalDefinition, responseDefinition);
    }

    @Test(groups = { "wso2.am" }, description = "API definition import", dependsOnMethods = "testAPIDefinitionUpdate")
    public void testAPIDefinitionImport() throws Exception {
        String originalDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(resourcePath + "AsyncAPI_import.json"),
                "UTF-8");
        String additionalProperties = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(resourcePath + "additionalProperties.json"),
                "UTF-8");
        JSONObject additionalPropertiesObj = new JSONObject(additionalProperties);
        additionalPropertiesObj.put("provider", user.getUserName());

        //test case for file import
        File file = getTempFileWithContent(originalDefinition);
        APIDTO apidto = restAPIPublisher.importAsyncAPIDefinition(null, file, additionalPropertiesObj.toString());
        apiImportId = apidto.getId();

        restAPIPublisher.changeAPILifeCycleStatus(apiImportId, Constants.PUBLISHED);

        String storeDefinition = restAPIStore.getAsyncAPIById(apiImportId, user.getUserDomain());
        String publisherDefinition = restAPIPublisher.getAsyncAPIById(apiImportId);

        validateAsyncAPIDefinition(publisherDefinition);
        validateAsyncAPIDefinition(storeDefinition);

        AsyncAPIUtils.validateUpdatedDefinitionForPublisher(originalDefinition, publisherDefinition);
        AsyncAPIUtils.validateUpdatedDefinitionForStore(originalDefinition, storeDefinition);

        //validating a definition which contains errors
        String originalDefinitionWithError = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(resourcePath + "AsyncAPI_import_withErrors.json"),
                "UTF-8");
        validateAsyncAPIDefinitionWithErrors(originalDefinitionWithError);

        //test case for URL import
        String additionalProperties2 = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(resourcePath + "additionalProperties2.json"),
                "UTF-8");
        JSONObject additionalPropertiesObj2 = new JSONObject(additionalProperties2);
        additionalPropertiesObj2.put("provider", user.getUserName());
        String url = "https://raw.githubusercontent.com/ZiyamSanthosh/AsyncAPI_WebSocket_Example/main/SampleWebSocket.yml";
        APIDTO apidto2 = restAPIPublisher.importAsyncAPIDefinition(url, null, additionalPropertiesObj2.toString());
    }

    private void testUpdatedAsyncAPIDefinitionInPublisher(APIDTO apidto, String oasVersion) throws Exception {
        String asyncAPIDefinition = restAPIPublisher.getAsyncAPIById(apiId);
        validateAsyncAPIDefinition(asyncAPIDefinition);
    }

    private void testUpdatedAsyncAPIDefinitionInStore(APIDTO apidto, String oasVersion) throws Exception {
        String asyncAPIDefinition = restAPIStore.getAsyncAPIById(apiId, user.getUserDomain());
        validateAsyncAPIDefinition(asyncAPIDefinition);
    }

    private void validateAsyncAPIDefinition(String asyncAPIDefinition) throws Exception {
        File file = getTempFileWithContent(asyncAPIDefinition);
        AsyncAPISpecificationValidationResponseDTO responseDTO = restAPIPublisher.validateAsyncAPIDefinition(file);
        Assert.assertTrue(responseDTO.isIsValid());
    }

    private void validateAsyncAPIDefinitionWithErrors(String asyncAPIDefinition) throws Exception {
        File file = getTempFileWithContent(asyncAPIDefinition);
        AsyncAPISpecificationValidationResponseDTO responseDTO = restAPIPublisher.validateAsyncAPIDefinition(file);
        Assert.assertFalse(responseDTO.isIsValid());
    }

    private File getTempFileWithContent(String asyncAPIDefinition) throws Exception {
        File temp = File.createTempFile("AsyncAPI", ".json");
        temp.deleteOnExit();
        BufferedWriter out = new BufferedWriter(new FileWriter(temp));
        out.write(asyncAPIDefinition);
        out.close();
        return temp;
    }

    private void testDeleteApi(String apiId) throws Exception {
        if (apiId == null){
            return;
        }
        restAPIPublisher.deleteAPI(apiId);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        testDeleteApi(apiId);
        testDeleteApi(apiImportId);
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] {
                new Object[] { TestUserMode.SUPER_TENANT_ADMIN, "v3" },
                new Object[] { TestUserMode.TENANT_ADMIN, "v3" },
        };
    }

}
