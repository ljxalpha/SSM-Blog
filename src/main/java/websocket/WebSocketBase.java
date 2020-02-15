package websocket;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

import utils.DateConvert;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @ServerEndpoint 注解是一个类层次的注解，它的功能主要是将目前的类定义成一个websocket服务器端,
 *                 注解的值将被用于监听用户连接的终端访问URL地址,客户端可以通过这个URL来连接到WebSocket服务器端
 * @author uptop
 */
@ServerEndpoint("/hello")
public class WebSocketBase {
	// 静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
	//TODO 经测试，不同的用户对该ServerEndpoint的访问时多线程的；现阶段用synchronized实现线程安全，不够高效，有优化空间
	private static int onlineCount = 0;

	// concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。若要实现服务端与单一客户端通信的话，可以使用Map来存放，其中Key可以为用户标识
	public static ConcurrentHashMap<String, WebSocketBase> webAdminSocketSMap = new ConcurrentHashMap<>();
	public static ConcurrentHashMap<String, WebSocketBase> webCommenSocketMap = new ConcurrentHashMap<>();

	/**
	 * 管理员的认证消息
	 */
	private static String adminSlogan = "#Iamsuperadmin#";

	// 与某个客户端的连接会话，需要通过它来给客户端发送数据
	private Session session;

	// 管理员
	private String admin;
	
	/**
	 * 连接建立成功调用的方法
	 *
	 * @param session
	 *            可选的参数。session为与某个客户端的连接会话，需要通过它来给客户端发送数据
	 */
	@OnOpen
	public void onOpen(Session session) throws IOException {
		this.session = session;
		webCommenSocketMap.put(session.getId(), this);
		addOnlineCount(); // 在线数加1
		String message = "*ADMIN* " + DateConvert.convert2s(new Date()) + "有新连接加入！当前在线人数（包括我）为" + getOnlineCount();
		System.out.println(message);
		sendMessageToAdminUsers(message);
	}
	
	/**
	 * 连接关闭调用的方法
	 */
	@OnClose
	public void onClose(Session session) throws IOException {
		if(webCommenSocketMap.containsKey(session.getId())){
			webCommenSocketMap.remove(session.getId());
		}
		else{
			webAdminSocketSMap.remove(session.getId());
		}
		subOnlineCount(); // 在线数减1
		String message = "*ADMIN* " + DateConvert.convert2s(new Date()) + "有一连接关闭！当前在线人数为" + getOnlineCount();
		System.out.println(message);
		sendMessageToAdminUsers(message);
	}

	/**
	 * 收到客户端消息后调用的方法
	 *
	 * @param message
	 *            客户端发送过来的消息
	 * @param session
	 *            可选的参数
	 */
	@OnMessage
	public void onMessage(String message, Session session) throws IOException {
		System.out.println("来自客户端的消息:" + message);
		if(adminSlogan.equals(message)){
			webAdminSocketSMap.put(session.getId(), webCommenSocketMap.get(session.getId()));
			webCommenSocketMap.remove(session.getId());
			String singleMessage = "*ADMIN* " + DateConvert.convert2s(new Date()) + "当前在线人数为" + getOnlineCount();
			session.getBasicRemote().sendText(singleMessage);
		}
		else if(webAdminSocketSMap.containsKey(session.getId())){
			sendMessageToCommonUsers(message);
		}
	}

	/**
	 * 发生错误时调用
	 *
	 * @param session
	 * @param error
	 */
	@OnError
	public void onError(Session session, Throwable error) {
		System.out.println("发生错误");
		error.printStackTrace();
	}

	/**
	 * 这个方法与上面几个方法不一样。没有用注解，是根据自己需要添加的方法。
	 *
	 * @param message
	 * @throws IOException
	 */
	public void sendMessageToCommonUsers(String message) throws IOException {
		for(String sid: webCommenSocketMap.keySet()){
			Session session = webCommenSocketMap.get(sid).session;
			session.getBasicRemote().sendText(message);
		}
	}

	/**
	 * 这个方法与上面几个方法不一样。没有用注解，是根据自己需要添加的方法。
	 *
	 * @param message
	 * @throws IOException
	 */
	public void sendMessageToAdminUsers(String message) throws IOException {
		for(String sid: webAdminSocketSMap.keySet()){
			Session session = webAdminSocketSMap.get(sid).session;
			session.getBasicRemote().sendText(message);
		}
	}

	public static synchronized int getOnlineCount() {
		return onlineCount;
	}

	public static synchronized void addOnlineCount() {
		WebSocketBase.onlineCount++;
	}

	public static synchronized void subOnlineCount() {
		WebSocketBase.onlineCount--;
	}

}