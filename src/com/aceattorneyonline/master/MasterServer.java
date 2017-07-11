package com.aceattorneyonline.master;

import java.io.IOException;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

public class MasterServer {
	private static final int HOST_PORT = 27016;

	public static void main(String[] args) throws IOException, InterruptedException {
		NioEventLoopGroup bossGroup = new NioEventLoopGroup();
		NioEventLoopGroup workerGroup = new NioEventLoopGroup();

		// Max 1500 threads for slow tasks
		final EventExecutorGroup group = new DefaultEventExecutorGroup(1500);

		ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.group(bossGroup, workerGroup)
			.channel(NioServerSocketChannel.class)
			.childHandler(new MasterServerInitializer(group))
			.option(ChannelOption.SO_BACKLOG, 20)
			.childOption(ChannelOption.SO_KEEPALIVE, true);

		bootstrap.bind(HOST_PORT).sync();
	}

}
