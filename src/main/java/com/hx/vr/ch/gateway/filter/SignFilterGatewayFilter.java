package com.hx.vr.ch.gateway.filter;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

import com.hx.vr.ch.gateway.obj.App;
import com.hx.vr.ch.gateway.service.AppService;

import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author
 *
 */

@Component
@Scope("prototype")
public class SignFilterGatewayFilter implements GatewayFilter {
	private final static Logger log = LoggerFactory.getLogger(SignFilterGatewayFilter.class);

	@Autowired
	private AppService app;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

		ServerHttpRequest request = exchange.getRequest();

		String key = request.getHeaders().getFirst("appkey");
		if (StringUtils.isEmpty(key)) {
			key = request.getQueryParams().getFirst("appkey");
		}
		if (StringUtils.isEmpty(key)) {
			String content = "{\"code\":404,\"msg\":\"appkey不能为空！\"}";
			log.error("=====appkey不能为空！");
			return error(exchange, content);
		}
		if (key.length() < 32) {
			String content = "{\"code\":404,\"msg\":\"appkey格式不正确\"}";
			log.error("=====appkey格式不正确：" + key);
			return error(exchange, content);
		}

		String sign = request.getQueryParams().getFirst("sign");

		if (StringUtils.isEmpty(sign) || sign.length() < 32) {
			String content = "{\"code\":404,\"msg\":\"sign不能为空！\"}";
			log.error("==sign为空！");
			return error(exchange, content);
		}
		if (StringUtils.isEmpty(sign) || sign.length() < 32) {
			String content = "{\"code\":404,\"msg\":\"sign格式不正确！\"}";
			log.error("==sign格式不正确：" + sign);
			return error(exchange, content);
		}
		App a = app.findByKey(key);

		if (a == null) {
			String content = "{\"code\":405,\"msg\":\"App不存在\"}";
			log.error("==AppKey[" + key + "]对应的APP不存在！");
			return error(exchange, content);
		}
		String secret = a.getAppSecret();
		ServerWebExchange exc = null;

		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>(request.getQueryParams());
		boolean rs = false;
		if ("GET".equalsIgnoreCase(request.getMethodValue())) {
			rs = validateSign(map, key, secret, sign);
			exc = exchange;

		} else if ("POST".equalsIgnoreCase(request.getMethodValue())) {
			URI uri = exchange.getRequest().getURI();
			URI newUri = UriComponentsBuilder.fromUri(uri).build(true).toUri();
			
			ServerHttpRequest oldRequest = exchange.getRequest().mutate().uri(newUri).build();
			Flux<DataBuffer> body = request.getBody();
			// 缓存读取的request body信息
			AtomicReference<String> bodyRef = new AtomicReference<>();
			// 读取request body到缓存
			body.subscribe(dataBuffer -> {
				CharBuffer charBuffer = StandardCharsets.UTF_8.decode(dataBuffer.asByteBuffer());
				DataBufferUtils.release(dataBuffer);
				bodyRef.set(charBuffer.toString());
			});
			// 获取request body
			String bodyStr = bodyRef.get();
			MultiValueMap<String, String> postMap = stringToMap(bodyStr);
			if(postMap!=null) {
				map.addAll(postMap);
				DataBuffer bodyDataBuffer = stringBuffer(bodyStr);
				Flux<DataBuffer> bodyFlux = Flux.just(bodyDataBuffer);
				// 封装我们的request
				ServerHttpRequest newRequest = new ServerHttpRequestDecorator(oldRequest) {
					@Override
					public Flux<DataBuffer> getBody() {
						return bodyFlux;
					}
				};
				exc = exchange.mutate().request(newRequest).build();
			}else {
				exc=exchange;
			}
			rs = validateSign(map, key, secret, sign);
			
			
		}

		if (rs) {
			return chain.filter(exc);
		} else {
			String content = "{\"code\":406,\"msg\":\"验签失败\"}";
			log.error("==验签失败！");
			return error(exchange, content);
		}

	}

	protected DataBuffer stringBuffer(String value) {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

		NettyDataBufferFactory nettyDataBufferFactory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
		DataBuffer buffer = nettyDataBufferFactory.allocateBuffer(bytes.length);
		buffer.write(bytes);
		return buffer;
	}

	/*
	 * 将body转化为map
	 */
	private MultiValueMap<String, String> stringToMap(String body) {
		if (body == null || body.length() == 0) {
			return null;
		}
		String[] ps = body.split("&");
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		for (String p : ps) {
			String[] ks = p.split("=");
			if (ks.length == 2) {
				map.add(ks[0], ks[1]);
			}
		}

		return map;
	}

	private boolean validateSign(MultiValueMap<String, String> params, String appKey, String appSecret, String sign) {
		if (params == null || params.size() == 0) {
			return false;
		}
		List<String> list = new ArrayList<String>(params.size() - 1);
		Set<String> keys = params.keySet();
		for (String key : keys) {
			if ("sign".equals(key)) {
				continue;
			}
			list.add(key);
		}
		Collections.sort(list, Collator.getInstance(java.util.Locale.CHINA));
		StringBuilder sb = new StringBuilder();
		for (String key : list) {
			sb.append(key).append("=").append(params.getFirst(key));
		}
		sb.append("_");
		sb.append(appSecret);
		if (log.isDebugEnabled()) {
			log.debug("==签名字符串 :" + sb);
		}
		try {
			String ssign = DigestUtils.md5DigestAsHex(sb.toString().getBytes("UTF-8")).toLowerCase();
			if (log.isDebugEnabled()) {
				log.debug("==服务器端签名:" + ssign);
			}
			if (sign.equals(ssign)) {
				return true;
			}
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage(), e.getCause());
			e.printStackTrace();
		}

		return false;

		
	}

	private static Mono<Void> error(ServerWebExchange exchange, String content) {
		ServerHttpResponse response = exchange.getResponse();
		// 设置headers
		HttpHeaders hs = response.getHeaders();
		hs.add("Content-Type", "application/json; charset=UTF-8");
		hs.add("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
		// 设置body
		DataBuffer bodyDataBuffer = response.bufferFactory().wrap(content.getBytes());
		return response.writeWith(Mono.just(bodyDataBuffer));
	}

}
