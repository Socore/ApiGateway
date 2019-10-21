package com.hx.vr.ch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;


@SpringCloudApplication
@EnableDiscoveryClient
@EnableHystrix 
@EnableAutoConfiguration
public class GatewayServer {

	private static final Logger log=LoggerFactory.getLogger(GatewayServer.class);
	public static void main(String[] args) {
		SpringApplication.run(GatewayServer.class, args);
		log.info("=======网关启动成功！");
		
	}
}
