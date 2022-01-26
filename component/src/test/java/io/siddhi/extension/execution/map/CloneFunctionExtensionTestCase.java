/*
 * Copyright (c)  2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.siddhi.extension.execution.map;

import io.siddhi.core.SiddhiAppRuntime;
import io.siddhi.core.SiddhiManager;
import io.siddhi.core.event.Event;
import io.siddhi.core.exception.SiddhiAppCreationException;
import io.siddhi.core.stream.input.InputHandler;
import io.siddhi.core.stream.output.StreamCallback;
import io.siddhi.core.util.EventPrinter;
import io.siddhi.core.util.SiddhiTestHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CloneFunctionExtensionTestCase {
    private static final Logger log = LogManager.getLogger(CloneFunctionExtensionTestCase.class);
    private AtomicInteger count = new AtomicInteger(0);
    private volatile boolean eventArrived;

    @BeforeMethod
    public void init() {
        count.set(0);
        eventArrived = false;
    }

    @Test
    public void testCloneFunctionExtension() throws InterruptedException {
        log.info("CloneFunctionExtension TestCase");

        List<Map> clonedMap = new ArrayList<>();
        List<Map> dataMap = new ArrayList<>();

        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "\ndefine stream inputStream (symbol string, price long, volume long);";
        String query = ("@info(name = 'query1') from inputStream " +
                "select symbol,price,map:create() as tmpMap" +
                " insert into tmpStream;" +
                "@info(name = 'query2') from tmpStream  select symbol,price,tmpMap, " +
                "map:put(tmpMap,symbol,price) as map1" +
                " insert into outputStream;" +
                "@info(name = 'query3') " +
                "from outputStream " +
                "select map:clone(map1) as cloneMap, map1 " +
                " insert into outputStream2;"
        );

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(
                inStreamDefinition + query);

        siddhiAppRuntime.addCallback("outputStream2", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
                EventPrinter.print(events);
                for (Event event : events) {
                    count.incrementAndGet();
                    eventArrived = true;
                    clonedMap.add((Map) event.getData(0));
                    dataMap.add((Map) event.getData(1));
                }
            }
        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("inputStream");
        siddhiAppRuntime.start();
        inputHandler.send(new Object[]{"IBM", 100, 100L});
        SiddhiTestHelper.waitForEvents(100, 1, count, 60000);

        AssertJUnit.assertTrue(eventArrived);
        AssertJUnit.assertEquals(1, count.get());

        AssertJUnit.assertNotSame(dataMap.get(0), clonedMap.get(0));
        AssertJUnit.assertEquals(dataMap.get(0).get("IBM"), clonedMap.get(0).get("IBM"));

        siddhiAppRuntime.shutdown();
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void testCloneFunctionExtension1() {
        log.info("CloneFunctionExtension TestCase with test attributeExpressionExecutors length");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "\ndefine stream inputStream (symbol string, price long, volume long);";
        String query = ("@info(name = 'query1') from inputStream " +
                "select symbol,price,map:create() as tmpMap" +
                " insert into tmpStream;" +
                "@info(name = 'query2') from tmpStream  select symbol,price,tmpMap, map:put(tmpMap,symbol,price) " +
                "as map1 insert into outputStream;" +
                "@info(name = 'query3') from outputStream " +
                "select map:isEmpty(map1) as isMap1,map:clone(symbol,price) as isMap2" +
                " insert into outputStream2;"
        );

        siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
    }
}
