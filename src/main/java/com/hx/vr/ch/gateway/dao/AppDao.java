package com.hx.vr.ch.gateway.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import com.hx.vr.ch.gateway.obj.App;

@Component
public class AppDao extends JdbcTemplate{

	public AppDao() {
	}
	@Autowired
	public AppDao(DataSource ds) {
		super(ds);
	}
	
	public App findByKey(String key) {
		String sql="select id,app_secret,status,channel,type from b_app where status=1 and  app_key=?";
		List<App> list=this.query(sql, new RowMapper<App>() {
			@Override
			public App mapRow(ResultSet rs, int rowNum) throws SQLException {
				App a=new App();
				a.setId(rs.getInt(1));
				a.setAppKey(key);
				a.setAppSecret(rs.getString(2));
				a.setStatus(rs.getInt(3));
				a.setChannel(rs.getLong(4));
				a.setType(rs.getInt(5));
				return a;
			}
		},key);
		
		return list.size()>0?list.get(0):null;
	}
	
}
