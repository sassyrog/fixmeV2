package com.fixme;

import com.fixme.Colour;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

public class BrokerHandler {
	private static SocketChannel client;
	private String ConnID;
	private static ByteBuffer buffer;
	private static BrokerHandler instance;

	public static BrokerHandler start() {
		if (instance == null)
			instance = new BrokerHandler();
		return instance;
	}

	public static void stop() throws IOException {
		client.close();
		buffer = null;
	}

	private BrokerHandler() {
		try {
			buffer = ByteBuffer.allocate(256);

			client = SocketChannel.open();
			InetSocketAddress addr = new InetSocketAddress("localhost", 5000);
			client.configureBlocking(false);
			client.connect(addr);

			while (!client.finishConnect())
				System.out.println("connecting to server...");
			this.ConnID = this.getServerResponse().trim();
			Colour.out.green("\n\tConnected to FIX router: Connection ID = " + this.ConnID + ".\n");

		} catch (ConnectException ce) {
			if (ce.getMessage().contains("Connection refused:"))
				Colour.out.red("\n\tConnection refused by FIX router.\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getServerResponse() throws IOException {
		Selector selector = Selector.open();
		client.register(selector, SelectionKey.OP_READ);
		while (true) {
			if (selector.select() > 0) {
				Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
				int count;

				while (iter.hasNext()) {
					try {
						SelectionKey sKey = iter.next();
						if (sKey.isReadable()) {
							SocketChannel sc = (SocketChannel) sKey.channel();
							buffer.flip();
							buffer.clear();
							count = sc.read(buffer);
							if (count > 0) {
								buffer.flip();
								String response = Charset.forName("UTF-8").decode(buffer).toString().trim();
								client.register(selector, SelectionKey.OP_WRITE);
								return response;
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					iter.remove();
				}

			}
		}
	}

	public String sendMessage(String msg) throws IOException {
		buffer.flip();
		buffer.clear();
		buffer.put(msg.getBytes());
		buffer.flip();
		client.write(buffer);
		return this.getServerResponse();
	}

	/**
	 * @return the connID
	 */
	public String getConnID() {
		return this.ConnID;
	}

}
