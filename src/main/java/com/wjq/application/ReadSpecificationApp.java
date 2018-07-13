package com.wjq.application;

import cn.hutool.core.io.FileUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.sax.handler.RowHandler;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.wjq.DbAction;
import com.wjq.entity.PprtDgMetaCore;
import com.wjq.entity.TableEntity;
import com.wjq.util.ExecutorServiceUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
* @author: wangjq
* @date: 2018-03-26 14:26
 * 根据规范接口excel更新模型数据
*/
@Slf4j
public class ReadSpecificationApp extends BaseApp {

	private static Map<String, TableEntity> tableMap = new ConcurrentHashMap<>();
	public static LinkedBlockingQueue<TableEntity> globalQueue = new LinkedBlockingQueue<>();
	private static AtomicInteger hasNum = new AtomicInteger(0);

	public static void main( String[] args )
	{
		readExcel();
		updateDb();
	}

	public static void readExcel(){
		File[] files = FileUtil.ls("files");
		for(File file : files){
			log.info("读取Excel: " + file.getName());
			ExcelUtil.read07BySax(file, 0, createRowHandler());

		}
		log.info("共统计表名：" + tableMap.size());
	}

	private static int tableCodeRow = 2;
	private static int tableCodeColumn = 0;
	private static int tableCnNameColumn = 1;
	private static int tableCmtColumn = 2;

	private static String tableName = "";
	private static String tableCnName = "";
	private static String tableCmt = "";

	private static RowHandler createRowHandler(){
		return new RowHandler() {
			public void handle(int sheetIndex, int rowIndex, List<Object> rowlist) {
				Map<String, List<Map<String, Map<String, String>>>> tableMap = new HashMap<>();
				//校验格式
				if(tableCodeRow-2 == rowIndex){
					if(!"接口单元编码".equals(rowlist.get(tableCodeColumn))
							|| !"接口单元名称".equals(rowlist.get(tableCnNameColumn))
							|| !"接口单元说明".equals(rowlist.get(tableCmtColumn))){
						log.error(sheetIndex + "," + rowIndex + "," + sheetIndex + ", 表名格式不正确！！！！  " + rowlist.toString());
					}
				}else if(rowIndex >= tableCodeRow){
					if(rowlist.get(tableCodeColumn) != null && StringUtils.isNotEmpty((String)rowlist.get(tableCodeColumn))){
						tableName = rowlist.get(tableCodeColumn)==null?"":(String)rowlist.get(tableCodeColumn);
						tableCnName = rowlist.get(tableCnNameColumn)==null?"":(String)rowlist.get(tableCnNameColumn);
						tableCmt = (String)rowlist.get(tableCmtColumn)==null?"":(String)rowlist.get(tableCmtColumn);
						putTableToTableMap();
					}
				}
			}
		};
	}

	private static void putTableToTableMap() {
//		Map<String, Map<String, String>> columnMap = new HashMap<>();
		TableEntity tableEntity = TableEntity.builder().build();
//		List<TableEntity.ColumnEntity> columnEntities = new ArrayList<>();
//		tableEntity.setColumnEntities(columnEntities);
		if(StringUtils.isNotEmpty(tableName) && !tableMap.containsKey(tableName)){
			tableEntity.setTableName(tableName.trim());
			tableEntity.setTableChName(tableCnName.trim());
			tableEntity.setTableCmt(tableCmt.trim());
			tableMap.put(tableName, tableEntity);
			log.info("读取表名: " + tableEntity.toString());
		}else{
			log.error("---------> 该表名已存在，被跳过!!!  " + tableName);
		}
	}

	private static void updateDb(){
		List<PprtDgMetaCore> list = null;
		List<PprtDgMetaCore> updateList = new ArrayList<>();
		for(Map.Entry<String,TableEntity> entry : tableMap.entrySet()){
			list = getTablesByName(entry.getKey());
			log.info("查询表 " + entry.getKey() + ", 结果: " + list.size());
			for(PprtDgMetaCore pprtDgMetaCore : list){
				PprtDgMetaCore newPprt = PprtDgMetaCore.builder().build();
				newPprt.setId(pprtDgMetaCore.getId());
				newPprt.setCmt(entry.getValue().getTableCmt());
				newPprt.setChName(entry.getValue().getTableChName());
				updateList.add(newPprt);
				hasNum.incrementAndGet();
			}
			if(updateList.size() >= 100){
				doUpdate(updateList);
				updateList.clear();
			}
		}
		if(updateList.size() > 0){
			doUpdate(updateList);
		}
		log.info("共有表名：" + tableMap.size() + ", 共更新表名：" + hasNum.intValue());
	}


	private static void doUpdate(List<PprtDgMetaCore> list){
		log.info(list.toString());
		boolean flag = pprtDgMetaCoreService.updateBatchById(list);
		String result = flag==true?"成功":"失败";
		log.info("更新数据" + list.size() + ", 更新结果: " + result);
	}
}
