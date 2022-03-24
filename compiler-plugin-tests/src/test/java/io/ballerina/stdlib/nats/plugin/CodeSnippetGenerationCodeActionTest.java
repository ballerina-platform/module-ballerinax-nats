/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.nats.plugin;

import io.ballerina.projects.plugins.codeaction.CodeActionArgument;
import io.ballerina.projects.plugins.codeaction.CodeActionInfo;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static io.ballerina.stdlib.nats.plugin.CompilerPluginTestUtils.BALLERINA_SOURCES;
import static io.ballerina.stdlib.nats.plugin.CompilerPluginTestUtils.EXPECTED_SOURCES;
import static io.ballerina.stdlib.nats.plugin.CompilerPluginTestUtils.RESOURCE_DIRECTORY;
import static io.ballerina.stdlib.nats.plugin.NatsCodeTemplate.NODE_LOCATION;

/**
 * A class for testing code actions.
 */
public class CodeSnippetGenerationCodeActionTest extends AbstractCodeActionTest {

    @Test
    public void testEmptyServiceCodeAction()
            throws IOException {
        Path filePath = RESOURCE_DIRECTORY.resolve(BALLERINA_SOURCES)
                .resolve("code_action_service_1")
                .resolve("service.bal");
        Path resultPath = RESOURCE_DIRECTORY.resolve(EXPECTED_SOURCES)
                .resolve("service_1")
                .resolve("result.bal");
        performTest(filePath, LinePosition.from(4, 0),
                getExpectedCodeAction("service.bal", 6, 1), resultPath);
    }

    @Test
    public void testServiceWithVariablesCodeAction()
            throws IOException {
        Path filePath = RESOURCE_DIRECTORY.resolve(BALLERINA_SOURCES)
                .resolve("code_action_service_2")
                .resolve("service.bal");
        Path resultPath = RESOURCE_DIRECTORY.resolve(EXPECTED_SOURCES)
                .resolve("service_2")
                .resolve("result.bal");
        performTest(filePath, LinePosition.from(4, 0),
                getExpectedCodeAction("service.bal", 7, 1), resultPath);
    }

    private CodeActionInfo getExpectedCodeAction(String filePath, int line, int offset) {
        LineRange lineRange = LineRange.from(filePath, LinePosition.from(4, 0),
                LinePosition.from(line, offset));
        CodeActionArgument locationArg = CodeActionArgument.from(NODE_LOCATION, lineRange);
        CodeActionInfo codeAction = CodeActionInfo.from("Insert service template", List.of(locationArg));
        codeAction.setProviderName("NATS_119/ballerinax/nats/ADD_REMOTE_FUNCTION_CODE_SNIPPET");
        return codeAction;
    }
}
