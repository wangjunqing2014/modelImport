package com.wjq.mapper;

import com.baomidou.mybatisplus.mapper.Wrapper;

public class MyMapper<T> extends Wrapper<T> {

	private String mySql = "";

	@Override
	public String getSqlSegment() {
		return null;
	}

}
