/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.wso2.siddhi.sdk.launcher.debug;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4JLoggerFactory;
import org.wso2.siddhi.sdk.launcher.debug.dto.MessageDTO;
import org.wso2.siddhi.sdk.launcher.util.Constants;

import java.io.PrintStream;

/**
 * {@code VMDebugServer} will open a websocket server for external clients to connect.
 * The websocket server is implemented with netty websocket library.
 */
public class VMDebugServer {

    /**
     * Start the web socket server.
     */
    public void startServer() {
        InternalLoggerFactory.setDefaultFactory(new Log4JLoggerFactory());
        //lets start the server in a new thread.
        Runnable run = VMDebugServer.this::startListning;
        Thread thread = new Thread((run));
        thread.setName("Message Listener");
        thread.start();
    }

    private void startListning() {
        int port = getDebugPort();
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new DebugServerInitializer());
            Channel ch = b.bind(port).sync().channel();

            PrintStream out = System.out;
            out.println(DebugConstants.DEBUG_MESSAGE + port);

            ch.closeFuture().sync();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    /**
     * Push message to client.
     *
     * @param debugSession current debugging session
     * @param status       debug point information
     */
    public void pushMessageToClient(VMDebugSession debugSession, MessageDTO status) {
        ObjectMapper mapper = new ObjectMapper();
        String json;
        try {
            json = mapper.writeValueAsString(status);
        } catch (JsonProcessingException e) {
            json = DebugConstants.ERROR_JSON;
        }
        debugSession.getChannel().write(new TextWebSocketFrame(json));
        debugSession.getChannel().flush();
    }

    private int getDebugPort() {
        String debugPort = System.getProperty(Constants.SYSTEM_PROP_SIDDHI_DEBUG);
        if (debugPort == null || debugPort.equals("")) {
            debugPort = DebugConstants.DEFAULT_DEBUG_PORT;
        }
        return Integer.parseInt(debugPort);
    }

    /**
     * Debug server initializer class.
     */
    static class DebugServerInitializer extends ChannelInitializer<SocketChannel> {

        @Override
        public void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(new HttpServerCodec());
            pipeline.addLast(new HttpObjectAggregator(65536));
            pipeline.addLast(new VMDebugServerHandler());
        }
    }
}
