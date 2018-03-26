package com.wjq.application;

import cn.hutool.setting.dialect.Props;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.wjq.entity.PprtDgMetaCore;
import com.wjq.service.PprtDgMetaCoreService;
import com.wjq.service.impl.PprtDgMetaCoreServiceImpl;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;

public class BaseApp {

	protected static PprtDgMetaCoreService pprtDgMetaCoreService;
	public static Props props = new Props("config.properties");

	static{
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/spring.xml");
		pprtDgMetaCoreService = (PprtDgMetaCoreServiceImpl)context.getBean("pprtDgMetaCoreServiceImpl");
		props.autoLoad(true);
	}

}
