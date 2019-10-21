package com.hx.vr.ch.gateway.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class SignFilterGatewayFilterFactory  extends AbstractGatewayFilterFactory<Object> implements Ordered{

	@Autowired
	private SignFilterGatewayFilter filter;
	@Override
	public int getOrder() {
		
		return -1;
	}

	@Override
	public GatewayFilter apply(Object config) {
		return filter;
	}

	public void setFilter(SignFilterGatewayFilter filter) {
		this.filter = filter;
	}

	

}
