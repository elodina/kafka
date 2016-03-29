/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ly.stealth.mesos.kafka

import org.junit.{Test, After, Before}
import org.junit.Assert._
import org.I0Itec.zkclient.ZkClient
import kafka.utils.{ZKStringSerializer, ZkUtils}
import scala.collection.JavaConversions._
import java.util

class RebalancerTest extends MesosTestCase {
  var zkClient: ZkClient = null

  @Before
  override def before {
    super.before

    val port = 56789
    testCluster.zkConnect = s"localhost:$port"

    startZkServer()
    zkClient = zkServer.getZkClient
    zkClient.setZkSerializer(ZKStringSerializer)
    testCluster.rebalancer = new Rebalancer(() => testCluster.zkConnect)
  }

  @After
  override def after {
    super.after
    stopZkServer()
  }

  @Test
  def start {
    Nodes.addBroker(new Broker("0", testCluster))
    Nodes.addBroker(new Broker("1", testCluster))

    testCluster.topics.addTopic("topic", Map(0 -> util.Arrays.asList(0), 1 -> util.Arrays.asList(0)))
    assertFalse(testCluster.rebalancer.running)
    testCluster.rebalancer.start(util.Arrays.asList("topic"), util.Arrays.asList("0", "1"))

    assertTrue(testCluster.rebalancer.running)
    assertFalse(testCluster.rebalancer.state.isEmpty)
  }

  @Test
  def start_in_progress {
    testCluster.topics.addTopic("topic", Map(0 -> util.Arrays.asList(0), 1 -> util.Arrays.asList(0)))
    ZkUtils.createPersistentPath(zkClient, ZkUtils.ReassignPartitionsPath, "")

    try { testCluster.rebalancer.start(util.Arrays.asList("t1"), util.Arrays.asList("0", "1")); fail() }
    catch { case e: Rebalancer.Exception => assertTrue(e.getMessage, e.getMessage.contains("in progress")) }
  }
}
