package com.hx.vr.ch.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest.Builder;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.hx.vr.ch.gateway.obj.App;
import com.hx.vr.ch.gateway.service.AppService;

import reactor.core.publisher.Mono;

@Component
@Scope("prototype")
public class TokenFilterGatewayFilter implements GatewayFilter {
	private final static Logger log = LoggerFactory.getLogger(TokenFilterGatewayFilter.class);
	private final static String JWT_HEADER = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.";
	@Autowired
	private AppService service;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		if(log.isDebugEnabled()) {
			log.debug("========请求地址："+exchange.getRequest().getURI().getPath());
		}
		if("/api/channel/box/register".equals(exchange.getRequest().getURI().getPath())){
			return chain.filter(exchange);
		}
		
		String token = exchange.getRequest().getHeaders().getFirst("token");
		if(log.isDebugEnabled()) {
			log.debug("=========请求token："+token);
		}
		if (StringUtils.isEmpty(token)) {
			token = exchange.getRequest().getQueryParams().getFirst("token");
		}
		if (StringUtils.isEmpty(token)) {
            String content = "{\"code\":401,\"msg\":\"Token为空\"}";
            log.error("=========token为空！");
            return error(exchange,content);
		}
		String appkey = exchange.getRequest().getHeaders().getFirst("appkey");
		if (StringUtils.isEmpty(appkey)) {
			appkey = exchange.getRequest().getQueryParams().getFirst("appkey");
		}
		
		if(StringUtils.isEmpty(appkey)) {
			String content ="{\"code\":401,\"msg\":\"appkey为空\"}";
			log.error("=========appkey为空！");
			return error(exchange,content);
		}
		if(appkey.length()<32) {
			String content ="{\"code\":401,\"msg\":\"appkey格式不正确\"}";
			log.error("=========appkey格式不正确："+appkey);
			return error(exchange,content);
		}
		App app = service.findByKey(appkey);
		
		if (app != null) {
			if (log.isDebugEnabled()) {
				log.debug("==appkey:" + app.getAppKey() + " secret:" + app.getAppSecret());
			}
			String secret = app.getAppSecret();

			Algorithm algorithm = Algorithm.HMAC256(new StringBuilder(appkey).append( ":").append( secret).toString());
			try {
				JWTVerifier verifier = JWT.require(algorithm).build(); // Reusable verifier instance
				DecodedJWT jwt = verifier.verify(JWT_HEADER  + token);
				String[] ids = jwt.getClaim("id").asArray(String.class);
				String ch = ids[0];
				String id = ids[1];
				if (log.isDebugEnabled()) {
					log.debug("=======渠道编号:" + ch+" 盒子ID:"+id);
				}
				
				if (!StringUtils.isEmpty(id)) {
					Builder build=exchange.getRequest().mutate().header("box_id", id).header("channel", ch);
					if(!exchange.getRequest().getHeaders().containsKey("appkey")) {
						build.header("appkey", appkey);
					}
				    exchange = exchange.mutate().request(build.build())
							.build();
					
					return chain.filter(exchange);
				}else {
		            String content = "{\"code\":402,\"msg\":\"Token无法解析\"}";
		            log.error("=========TOKEN中无法获取盒子ID");
		            return error(exchange ,content);
				}
			} catch (JWTVerificationException ex) {
	            String content = "{\"code\":402,\"msg\":\"Token无法解析\"}";
	            log.error("=========无法从TOKEN中解析JWT数据！");
	            return error(exchange ,content);
			}

		}
		log.error("=========App不存在："+appkey);
		String content = "{\"code\":403,\"msg\":\"App不存在\"}";
        return error(exchange,content);

	}
	
	private static Mono<Void> error(ServerWebExchange exchange,String content){
		ServerHttpResponse response = exchange.getResponse();
        //设置headers
        HttpHeaders httpHeaders = response.getHeaders();
        httpHeaders.add("Content-Type", "application/json; charset=UTF-8");
        httpHeaders.add("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        //设置body
        DataBuffer bodyDataBuffer = response.bufferFactory().wrap(content.getBytes());
        return response.writeWith(Mono.just(bodyDataBuffer));
	}
}
