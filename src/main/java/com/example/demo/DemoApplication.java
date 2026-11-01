package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class DemoApplication {

	private static final Logger log = LoggerFactory.getLogger(DemoApplication.class);

	/** Đọc giá trị từ application.properties */
	@Value("${spring.datasource.url}")
	private String datasourceUrl;

	@Value("${spring.mongodb.uri}")
	private String mongoUri;

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	/** Log cổng thực tế mà server đang chạy (kể cả khi đổi cổng qua server.port) */
	@EventListener
	public void onWebServerReady(WebServerInitializedEvent event) {
		int port = event.getWebServer().getPort();
		log.info("Ứng dụng đang chạy tại: http://localhost:{}", port);
		log.info("Đã kết nối database: {}", datasourceUrl);
		log.info("MongoDB URI: {}", mongoUri.replaceAll("//[^@/]*@", "//***@"));
	}

}
