package com.wjq;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.wjq.entity.PprtDgMetaCore;
import com.wjq.entity.TableEntity;
import com.wjq.mapper.PprtDgMetaCoreDao;
import com.wjq.service.PprtDgMetaCoreService;
import com.wjq.service.impl.PprtDgMetaCoreServiceImpl;
import com.xiaoleilu.hutool.collection.CollUtil;
import com.xiaoleilu.hutool.lang.Console;
import com.xiaoleilu.hutool.poi.excel.ExcelUtil;
import com.xiaoleilu.hutool.poi.excel.sax.handler.RowHandler;
import com.xiaoleilu.hutool.thread.ThreadUtil;
import com.xiaoleilu.hutool.util.ArrayUtil;
import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.RowBounds;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;

/**
 * Hello world!
 *
 */
@Log4j
public class App 
{
    public static void main( String[] args )
    {
		readExcel();
		start();
		ExecutorServiceUtil.executorService.shutdown();
    }

    public static void readExcel(){
    	String path = "modelv1.0.xlsx";
		ExcelUtil.read07BySax(path, -1, createRowHandler());
		addLastOne();
		log.info("共统计表名：" + tableMap.size());
		log.info("共进入队列数：" + globalQueue.size());
	}

	private static int tableNameRow = 2;
	private static int tableNameColumn = 3;
	private static int proDesRow = 20;
	private static int procDesColumn = 2;
	private static int remarkRow = 20;
	private static int remarkColumn = 9;
	private static int proNameColumn = 1;

	private static int procLine = 21;

	public static void insertTable(){
		List<PprtDgMetaCore> list = new ArrayList<>();

	}

	private static Map<String, TableEntity> tableMap = new ConcurrentHashMap<>();
	public static LinkedBlockingQueue<TableEntity> globalQueue = new LinkedBlockingQueue<>();

	public static LinkedBlockingQueue<List<Map<String, List<Map<String, Map<String, String>>>>>> queue = new LinkedBlockingQueue();
	private static RowHandler createRowHandler() {
		return new RowHandler() {
			public void handle(int sheetIndex, int rowIndex, List<Object> rowlist) {
				if(0 != sheetIndex){
					Map<String, List<Map<String, Map<String, String>>>> tableMap = new HashMap<>();
					//获取表名
					if(tableNameRow == rowIndex){
						if(!"英文名称".equals(rowlist.get(tableNameColumn-2))){
							log.error(sheetIndex + "," + rowIndex + "," + sheetIndex + ",表名格式不正确！！！！  " + rowlist.toString());
						}else{
							tableName = (String)rowlist.get(tableNameColumn);
							putTableToTableMap();
						}
					}else if("属性描述".equals(rowlist.get(procDesColumn))){
						procLine = rowIndex + 2;
					}else{
						if(rowIndex >= procLine){
							if(rowlist.get(proNameColumn) != null && StringUtils.isNotEmpty((String)rowlist.get(proNameColumn))){
								proDes = rowlist.get(procDesColumn)==null?"":(String)rowlist.get(procDesColumn);
								remark = rowlist.get(remarkColumn)==null?"":(String)rowlist.get(remarkColumn);
								proName = (String)rowlist.get(proNameColumn);
								putColumnToTableMap();
							}
						}
					}
				}
			}
		};
	}

	private static String tableName = "";
	private static String proDes = "";
	private static String remark = "";
	private static String proName = "";

	private static void putTableToTableMap() {
		Map<String, Map<String, String>> columnMap = new HashMap<>();
		TableEntity tableEntity = TableEntity.builder().build();
		List<TableEntity.ColumnEntity> columnEntities = new ArrayList<>();
		tableEntity.setColumnEntities(columnEntities);
		if(StringUtils.isNotEmpty(tableName) && !tableMap.containsKey(tableName)){
			tableEntity.setTableName(tableName);
			tableMap.put(tableName, tableEntity);
			log.info("读取表名: " + tableName);
			addToQueue();
		}else{
			log.error("---------> 该表名被跳过！  " + tableName);
		}
	}

	private static void putColumnToTableMap(){
		if(tableMap.containsKey(tableName)){
			TableEntity tableEntity = tableMap.get(tableName);
			List<TableEntity.ColumnEntity> listColumn = tableEntity.getColumnEntities();
			boolean flag = true;
			if(listColumn.size()>=0){
				for(TableEntity.ColumnEntity columnEntity : listColumn){
					if(proName.equals(columnEntity.getProName())){
						flag = false;
						break;
					}
				}
				if(flag){
					TableEntity.ColumnEntity columnEntity =  tableMap.get(tableName).new ColumnEntity();
					columnEntity.setCmt(remark.trim());
					columnEntity.setProName(proName.trim());
					columnEntity.setRemark(proDes.trim());
					listColumn.add(columnEntity);
					log.info("读取属性名: " + proName + ", 读取属性描述: " + proDes + ", 读取备注: " + remark);
				}else{
					log.error("---------> 该属性被跳过！  " + proName);
				}
			}
		}
	}

	private static void addToQueue(){
		for(Map.Entry<String,TableEntity> entry : tableMap.entrySet()){
			if(!tableName.equals(entry.getKey())){
				try{
					globalQueue.put(tableMap.remove(entry.getKey()));
					log.info("进入队列: " + entry.getKey());
				}catch (Exception e){
					e.printStackTrace();
					log.error(e.getStackTrace());
				}
			}
		}
	}

	private static void addLastOne(){
		try{
			globalQueue.put(tableMap.remove(tableName));
			log.info("进入队列: " + tableName);
		}catch (Exception e){
			e.printStackTrace();
			log.error(e.getStackTrace());
		}
	}

	public static void start(){
		try{
			ExecutorServiceUtil.completionService.submit(new Callable<HashMap<String, String>>() {
				@Override
				public HashMap<String, String> call() throws Exception {
					while(!globalQueue.isEmpty()){
						// 当前排队线程数
						int queueSize = ((ThreadPoolExecutor)ExecutorServiceUtil.executorService).getQueue().size();
						if(queueSize<ExecutorServiceUtil.QUEUE_MAX_SIZE){
							TableEntity tableEntity = globalQueue.take();
							DbAction.addToTask(tableEntity);
							log.info("----> globalQueue.size: " + globalQueue.size());
						}
					}
					HashMap<String,String> map = new HashMap<>();
					map.put("globalQueue thread", "is over!");
					return map;
				}
			});
			int i = 0;
			while(i<900){
				Future<HashMap<String,String>> future = ExecutorServiceUtil.completionService.take();
				HashMap<String,String> resule = future.get();
				if(null != resule){
					log.info("----------------> " +i+ " 线程结果: " + resule.toString());
				}else{
					log.info("----------------> 线程跳过: " + i);
					break;
				}
				i++;
			}
		}catch(Exception e){
			log.error(e.getMessage());
			e.printStackTrace();
			ExecutorServiceUtil.executorService.shutdownNow();
		}
	}
}
