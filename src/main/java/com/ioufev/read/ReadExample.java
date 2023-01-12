/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.ioufev.read;

import com.google.common.collect.ImmutableList;
import com.ioufev.conn.ClientExample;
import com.ioufev.conn.ClientExampleRunner;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.enumerated.ServerState;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ReadExample implements ClientExample {

    public static void main(String[] args) throws Exception {
        ReadExample example = new ReadExample();

        new ClientExampleRunner(example, true).run();
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void run(OpcUaClient client, CompletableFuture<OpcUaClient> future) throws Exception {
        // synchronous connect
        client.connect().get();

        NodeId nodeId = new NodeId(2, "通道 1.设备 1.标记 1"); // 字符串类型的地址
//        NodeId nodeId = new NodeId(4, 7); // 数字类型的地址

        DataValue dataValue = client.readValue(0.0, TimestampsToReturn.Both, nodeId).get();

        System.out.println("-----读取-----");
        System.out.println("-----通道 1.设备 1.标记 1：" + dataValue.getValue().getValue());
        //---------------------------------------
        //Identifiers.DataTypesFolder

        NodeId nodeId1 = new NodeId(2,"通道 1.设备 1");
        List<? extends UaNode> nodes = client.getAddressSpace().browseNodes(nodeId1);
        for(UaNode node:nodes){
            NodeId nodeId2 = node.getNodeId();
            if(node.getNodeClass() == NodeClass.Variable){
                DataValue value2 = client.readValue(0.0, TimestampsToReturn.Both, nodeId2).get();
                System.out.println(nodeId2.getIdentifier().toString() + ": "+ value2.getValue().getValue());
            }
        }

        // synchronous read request via VariableNode
        UaVariableNode node = client.getAddressSpace().getVariableNode(Identifiers.Server_ServerStatus_StartTime);
        DataValue value = node.readValue();

        logger.info("StartTime={}", value.getValue().getValue());

        // asynchronous read request
        readServerStateAndTime(client).thenAccept(values -> {
            DataValue v0 = values.get(0);
            DataValue v1 = values.get(1);

            logger.info("State={}", ServerState.from((Integer) v0.getValue().getValue()));
            logger.info("CurrentTime={}", v1.getValue().getValue());

            future.complete(client);
        });
    }

    private CompletableFuture<List<DataValue>> readServerStateAndTime(OpcUaClient client) {
        List<NodeId> nodeIds = ImmutableList.of(
            Identifiers.Server_ServerStatus_State,
            Identifiers.Server_ServerStatus_CurrentTime);

        return client.readValues(0.0, TimestampsToReturn.Both, nodeIds);
    }

}
