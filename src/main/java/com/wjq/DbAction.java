package com.wjq;

import cn.hutool.setting.dialect.Props;
import com.baomidou.mybatisplus.enums.SqlLike;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.wjq.entity.PprtDgMetaCore;
import com.wjq.entity.TableEntity;
import com.wjq.service.PprtDgMetaCoreService;
import com.wjq.service.impl.PprtDgMetaCoreServiceImpl;
import com.wjq.util.ExecutorServiceUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public class DbAction {
	private static PprtDgMetaCoreService pprtDgMetaCoreService;
	private static String baseProjectId = "8a52f77b5fe28e4f015fe2926d850000";
	public static Props props = new Props("config.properties");

	static{
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/spring.xml");
		pprtDgMetaCoreService = (PprtDgMetaCoreServiceImpl)context.getBean("pprtDgMetaCoreServiceImpl");
		props.autoLoad(true);
	}

	public static List<PprtDgMetaCore> getTablesByName(String tableName){
		//先查询表
		List<PprtDgMetaCore> tableList = pprtDgMetaCoreService.selectList(new EntityWrapper<PprtDgMetaCore>()
				.eq("PRT_ID", props.getStr("baseProjectId"))
				.like("NAME", tableName.trim()));
		return tableList;
	}

	public static List<PprtDgMetaCore> getColumnsByPrtId(String prtId, String name){
		//查询字段
		List<PprtDgMetaCore> tableList = pprtDgMetaCoreService.selectList(new EntityWrapper<PprtDgMetaCore>()
				.eq("PRT_ID", prtId).like("CH_NAME", name , SqlLike.CUSTOM));
		return tableList;
	}

	private static Map<String, String> doUpdate(TableEntity tableEntity){
		Map<String, String> map = new HashMap<>();
		List<PprtDgMetaCore> updateList = new ArrayList<>();
		for(TableEntity.ColumnEntity columnEntity : tableEntity.getColumnEntities()){
			if(StringUtils.isNotEmpty(columnEntity.getCmt()) || StringUtils.isNotEmpty(columnEntity.getRemark())) {
				//先查询表
				List<PprtDgMetaCore> list = getTablesByName(tableEntity.getTableName());
				for (PprtDgMetaCore pprtDgMetaCore : list) {
					//再根据查询字段：
					List<PprtDgMetaCore> columnList = getColumnsByPrtId(pprtDgMetaCore.getId(), columnEntity.getProName());
					if(null == columnList || columnList.size() == 0){
						log.info( "未查询到对应的字段! " + tableEntity.getTableName() + "," + columnEntity.getProName());
						continue;
					}
					PprtDgMetaCore tmpColumn = null;
					for(PprtDgMetaCore column : columnList){
						tmpColumn = PprtDgMetaCore.builder().build();
						tmpColumn.setId(column.getId());
						tmpColumn.setRemarks(columnEntity.getRemark());
						tmpColumn.setCmt(columnEntity.getCmt());
						updateList.add(tmpColumn);
						log.info(tableEntity.getTableName() + ", 更新字段: " + column.getName() + "|" + column.getChName() +
								", 添加属性描述: " + tmpColumn.getRemarks() + ", 添加备注: " + tmpColumn.getCmt());
						map.put(new StringBuilder(tableEntity.getTableName()).append("|").append(columnEntity.getProName()).toString(),
								", 添加属性描述: " + tmpColumn.getRemarks() + ", 添加备注: " + tmpColumn.getCmt());
					}
				}
			}
		}
		if(updateList.size()>0){
			pprtDgMetaCoreService.updateBatchById(updateList);
		}
		return map;
	}

	public static void addToTask(TableEntity tableEntity){
		ExecutorServiceUtil.completionService.submit(new Callable<Map<String, String>>() {
			private TableEntity tableEntity;
			public Callable setTableEntity(TableEntity tableEntity){
				this.tableEntity = tableEntity;
				return this;
			}
			@Override
			public Map<String, String> call() throws Exception {
				Map<String, String> result = new HashMap<>(doUpdate(tableEntity));
				// 当前排队线程数
				int queueSize = ((ThreadPoolExecutor)ExecutorServiceUtil.executorService).getQueue().size();
				// 当前活动线程数
				int activeCount = ((ThreadPoolExecutor)ExecutorServiceUtil.executorService).getActiveCount();
				// 执行完成线程数
				long completedTaskCount = ((ThreadPoolExecutor)ExecutorServiceUtil.executorService).getCompletedTaskCount();
				// 总线程数（排队线程数 + 活动线程数 +  执行完成线程数）
				long taskCount = ((ThreadPoolExecutor)ExecutorServiceUtil.executorService).getTaskCount();
				log.info("当前排队线程数: " + queueSize);
				log.info("当前活动线程数: " + activeCount);
				log.info("执行完成线程数: " + completedTaskCount);
				log.info("总线程数: " + taskCount);
				return result;
			}
		}.setTableEntity(tableEntity));
	}

}
