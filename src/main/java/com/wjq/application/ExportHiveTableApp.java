package com.wjq.application;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.UnicodeUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import cn.hutool.poi.excel.style.StyleUtil;
import cn.hutool.setting.dialect.Props;
import com.wjq.entity.Column;
import com.wjq.entity.PprtDgMetaCore;
import com.wjq.util.HiveKbsUitls;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;

import java.util.*;

/**
* @author: wangjq
* @date: 2018-07-09 15:23
 * 以Excel的形式导出B域数据pdata下新接入的表，表名由 xurx 提供
*/
@Slf4j
public class ExportHiveTableApp extends BaseApp {

	private static Map<String, List<PprtDgMetaCore>> pMap = new HashMap<>();
	private static List<PprtDgMetaCore> listMeta = new ArrayList<>();

	public static void main(String[] args){
		doExport();
	}

	/**
	 * 表字段信息
	 */
	public static void doExport(){
		Props props = new Props("config.properties", "UTF-8");
		String excelName = props.getStr("excelName");
		String dbName = props.getStr("dbName");
		List<String> list = FileUtil.readLines("fileName.txt", "UTF-8");

		ExcelWriter excelWriter = ExcelUtil.getWriter(excelName, "汇总");
		Font font = excelWriter.createFont();
		font.setBold(true);

		//输出表名汇总
		exportTotalInterfaces(dbName, list, excelWriter, font);

		List pList = null;
		for(String a : list){
			List<Column> columns = HiveKbsUitls.getColumns(dbName, a);
			List<Map<String, String>> mapList = new ArrayList<>();

			PprtDgMetaCore updateMc = null;
			pList = new ArrayList<>();
			for(Column c : columns){
				Map map = new HashMap();
				map.put("column", c.getName());
				map.put("commont", c.getChName());
				mapList.add(map);

				updateMc = PprtDgMetaCore.builder().build();
				updateMc.setName(c.getName());
				updateMc.setChName(c.getChName());
				updateMc.setRemarks(c.getRemarks());
				updateMc.setCmt(c.getCmt());
				updateMc.setType(c.getType());
				updateMc.setLength(c.getLength());
				updateMc.setMetaMdl("model.PhysicalModelClm");
//				updateMc.setPrtId(copyMetaCore.getId());
				updateMc.setEntity("{\"nullable\":"+c.isNullable()+", \"pk\":"+c.isPk()+"}");
				updateMc.setCrtDate(new Date());
				pList.add(updateMc);

				log.info("----- 封装字段: " + a + "." + c.getName() + ", " + c.getChName());
			}
			pMap.put(a, pList);

			Map<String, String> alias = MapUtil.newHashMap();
			alias.put("column", "字段名称");
			alias.put("commont", "描述   ");
			excelWriter.setHeaderAlias(alias);

			String sheetName = a.split("_")[2];
			excelWriter.setOrCreateSheet(sheetName);
			excelWriter.resetRow();
			excelWriter.passCurrentRow();

			StyleUtil.setFontStyle(font, IndexedColors.BLACK.getIndex(), (short)11, null);

			CellStyle headStyle = excelWriter.getStyleSet().getHeadCellStyle();
			headStyle.setFont(font);
			StyleUtil.setColor(headStyle, IndexedColors.AUTOMATIC, FillPatternType.SOLID_FOREGROUND);

			excelWriter.write(mapList);
			excelWriter.autoSizeColumn(0);
			excelWriter.autoSizeColumn(1);
			excelWriter.flush();

			log.info("----- 单个表信息输出完毕! ");
		}
		// 关闭writer，释放内存
		excelWriter.close();

		doDbAction();

		log.info("----------- end! ");
	}

	/**
	 * 汇总表信息
	 * @param dbName
	 * @param list
	 * @param excelWriter
	 * @param font
	 */
	private static void exportTotalInterfaces(String dbName, List<String> list, ExcelWriter excelWriter, Font font) {
		List<Map<String,String>> totalList = new ArrayList<>();
		Map<String, String> tMap = null;
		List<Map<String, String>> tlist = new ArrayList<>();
		PprtDgMetaCore pprtDgMetaCore = null;
		for(String name : list){
			tMap = new LinkedHashMap<>();
			tMap.put("表名/接口", name);
			tMap.put("安全级别", "");
			tMap.put("涉及的最高级别的字段名", "");
			String comment = getTableComment(dbName, name);
			tMap.put("备注", comment);
			totalList.add(tMap);

			pprtDgMetaCore = PprtDgMetaCore.builder()
					.chName(comment)
					.name(name)
					.metaMdl("model.PhysicalModel")
					.bizDomain("base_default")
					.prtId("8a52f77b5fe28e4f015fe2926d850000")
					.crtDate(new Date()).build();
			listMeta.add(pprtDgMetaCore);

			log.info("----- 封装表: " + name);
		}
		excelWriter.getStyleSet().setAlign(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
		StyleUtil.setFontStyle(font, IndexedColors.BLACK.getIndex(), (short)13, null);
		CellStyle headStyle1 = excelWriter.getStyleSet().getHeadCellStyle();
		headStyle1.setFont(font);

		StyleUtil.setColor(headStyle1, IndexedColors.YELLOW.index, FillPatternType.SOLID_FOREGROUND);

		excelWriter.write(totalList);
		excelWriter.autoSizeColumn(0);
		excelWriter.autoSizeColumn(1);
		excelWriter.flush();
		log.info("----- 汇总信息输出完毕! ");
	}

	public static String getTableComment(String dbName, String tableName){
		String sql = "desc formatted " + dbName + "." + tableName;
		List<Map<String,Object>> list = HiveKbsUitls.execQuerySql(sql);
		String comment = "";
		if(list.size()>0){
			for(Map<String, Object> map : list){
				if(null != map.get("data_type") && ((String)map.get("data_type")).trim().equals("comment")){
					comment = (String)map.get("comment");
					comment = UnicodeUtil.toString(comment);
					break;
				}
			}
		}
		return comment.trim();
	}

	/**
	 * @param listMeta
	 * @return
	 */
	public static void doDbAction(){
		List<String> idList = new ArrayList<>();
		PprtDgMetaCore pprtDgMetaCore = null;
		PprtDgMetaCore column = null;
		for(int i=0; i<listMeta.size(); i++){
			pprtDgMetaCore = listMeta.get(i);
			if(getTablesByName(pprtDgMetaCore.getName()).size() == 0){
				pprtDgMetaCoreService.insert(pprtDgMetaCore);
				log.info("------- 保存表: " + pprtDgMetaCore.getName());
				List<PprtDgMetaCore> list = pMap.get(pprtDgMetaCore.getName());
				if(list.size()>0){
					for(PprtDgMetaCore pprtDgMetaCore1 : list){
						pprtDgMetaCore1.setPrtId(pprtDgMetaCore.getId());
					}
					pprtDgMetaCoreService.insertBatch(list);
					log.info("------- 保存表字段信息: " + list.size());
				}
			}
		}
	}
}
