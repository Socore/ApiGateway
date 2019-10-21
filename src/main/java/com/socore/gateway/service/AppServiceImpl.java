package com.hx.vr.ch.gateway.service;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.hx.vr.ch.gateway.dao.AppDao;
import com.hx.vr.ch.gateway.obj.App;

@Component
public class AppServiceImpl implements AppService {

	private final static String PRRFIX="_app:";
	
	private AppDao dao;
	private StringRedisTemplate redis;
	
	@Autowired
	public AppServiceImpl(AppDao dao,StringRedisTemplate redis) {
		this.dao=dao;
		this.redis=redis;
	}
	@Override
	public App findByKey(String key) {
		if(StringUtils.isBlank(key)) {
			return null;
		}
		App app=null;
		String secret=redis.opsForValue().get(PRRFIX+key);
		if(StringUtils.isBlank(secret)) {
			app=dao.findByKey(key);
			if(app==null) {
				return null;
			}
			secret=app.getAppSecret();
			redis.opsForValue().set(PRRFIX+key, secret);
		}else {
			app=new App();
			app.setAppKey(key);
			app.setAppSecret(secret);
		}
		
		return app;
	}

}
