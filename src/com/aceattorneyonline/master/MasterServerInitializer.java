package com.aceattorneyonline.master;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutorGroup;

public class MasterServerInitializer extends ChannelInitializer<SocketChannel> {
	private final EventExecutorGroup group;

	public MasterServerInitializer(EventExecutorGroup group) {
		this.group = group;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
		
		// xxx: this might not support future clients with a more elegant protocol
		// we'll probably end up modifying the pipeline as we receive messages
		pipeline.addLast(new DelimiterBasedFrameDecoder(1024, Unpooled.copiedBuffer(new byte[] {'%'})));

		// Trigger the handler after 5 seconds of no I/O with a client
		pipeline.addLast(new IdleStateHandler(0, 0, 5));

		// The "business logic"
		pipeline.addLast(group, new MasterServerHandler());
	}
}