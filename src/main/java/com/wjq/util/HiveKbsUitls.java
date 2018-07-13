package com.wjq.util;

import com.wjq.entity.Column;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.sql.*;
import java.util.*;
import java.util.Date;

@Component
/**
* @author: wangjq
* @date: 2018-04-11 14:30
*/
public class HiveKbsUitls {
	private Configuration conf;
	private static String driverName = "org.apache.hive.jdbc.HiveDriver";

	private static String hiveUrl;
	private static String user;
	private static String keytabPath;
	private static String krbConfPath;
	private static String hadoopHome;

	private static Logger log = LoggerFactory.getLogger(HiveKbsUitls.class);

	static{
		try{
			Class.forName(driverName);

			Properties properties = new Properties();
			InputStream is = HiveKbsUitls.class.getClassLoader().getResourceAsStream("config.properties");
			properties.load(is);
			user = properties.getProperty("ibdcUser");
			krbConfPath = properties.getProperty("krb5Conf");
			hiveUrl = properties.getProperty("hiveUrl");
			keytabPath = properties.getProperty("keytabFile");
			hadoopHome = properties.getProperty("hadoopHome");
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	private static Connection getConnection(){
		Connection conn = null;
		try{
			Configuration conf = new Configuration();
			conf.set("hadoop.security.authentication", "Kerberos");
			System.setProperty("java.security.krb5.conf", krbConfPath);
			if(null != hadoopHome){
				System.setProperty("hadoop.home.dir", hadoopHome);
			}
			UserGroupInformation.setConfiguration(conf);
			UserGroupInformation.loginUserFromKeytab(user, keytabPath);
			conn = DriverManager.getConnection(hiveUrl);
		}catch (Exception e){
			e.printStackTrace();
			log.error("-------------------------" + e.getMessage());
		}
		return conn;
	}

	public static List<Column> getColumns(String database, String table){
		List<Column> columnList = new ArrayList<>();
		try{
			Column column;
			String col_name = "", data_type = "", comment = "";
			String sql = "describe "+ database +"." + table;
			List<Map<String, Object>> list = execQuerySql(sql);
			if(null!=list && list.size()>0){
				for(Map<String, Object> map : list){
					if(null == map.get("col_name") || map.get("col_name").toString().startsWith("#") || "".equals(map.get("col_name"))){
						break;
					}
					column = new Column();
					col_name = (String)map.get("col_name");
					column.setName(col_name);
					column.setChName(comment);
					data_type = map.get("data_type")==null?"":(String)map.get("data_type");
					if(data_type.indexOf("(")>0){
						column.setType(data_type.substring(0, data_type.indexOf("(")));
						int end = data_type.indexOf(",")>-1?data_type.indexOf(","):data_type.indexOf(")");
						String length = data_type.substring(data_type.indexOf("(")+1, end);
						column.setLength(Integer.parseInt(length));
					}else{
						column.setType(data_type);
					}
					comment = map.get("comment")==null?"":(String)map.get("comment");
					column.setCmt(comment);
					column.setNullable(true);
					columnList.add(column);
				}
			}
		}catch (Exception e){
			e.printStackTrace();
			log.error(e.toString());
		}
		return columnList;
	}

	/**
	 * 执行hive sql
	 * 需要调用者自己关闭connection
	 */
	public static boolean execNonQuerySql(String sql, Connection conn){
		return getExecuteBoolean(sql, conn);
	}

	/**
	 * 执行hive sql
	 */
	public static boolean execNonQuerySql(String sql){
		Connection conn = getConnection();
		return getExecuteBoolean(sql, conn);
	}

	private static boolean getExecuteBoolean(String sql, Connection conn) {
		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
		boolean flag = true;
		Statement statement = null;
		try{
			statement = conn.createStatement();
			log.info("-------------- 执行sql 为: " + sql);
			statement.execute(sql);
		}catch(Exception e){
			flag = false;
			e.printStackTrace();
		}finally {
			close(statement);
		}
		return flag;
	}

	/**
	 * 执行hive sql
	 */
	public static List<Map<String, Object>> execQuerySql(String sql){
		return getExecuteList(sql, getConnection());
	}

	/**
	 * 执行hive sql
	 * 需要调用者自己关闭connection
	 */
	public static List<Map<String, Object>> execQuerySql(String sql, Connection conn){
		return getExecuteList(sql, conn);
	}

	private static List<Map<String, Object>> getExecuteList(String sql, Connection conn) {
		Statement statement = null;
		ResultSet res = null;
		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
		try{
			statement = conn.createStatement();
			res = statement.executeQuery(sql);
			log.info("--------------  执行sql 为: " + sql);
			while (res.next()){
				ResultSetMetaData md = res.getMetaData();
				int columnCount = md.getColumnCount();   //获得列数
				Map<String,Object> rowData = new HashMap<String,Object>();
				for (int i = 1; i <= columnCount; i++) {
					rowData.put(md.getColumnName(i), res.getObject(i));
				}
				list.add(rowData);
			}
			log.info("-------------- 获得结果数 : " + list.size());
		}catch(Exception e){
			e.printStackTrace();
			log.error(e.getMessage());
		}finally {
			close(res);
			close(statement);
		}
		return list;
	}

	/**
	 * 关闭连接
	 */
	public static void close(Connection conn) {
		try {
			if (conn != null && !conn.isClosed()) {
				conn.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			conn = null;
		}
	}

	/**
	 * 关闭Statement
	 * @param stmt
	 */
	public static void close(Statement stmt) {
		try {
			if (stmt != null) {
				stmt.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			stmt = null;
		}
	}

	/**
	 * 关闭PreparedStatement
	 * @param pst
	 */
	public static void close(PreparedStatement pst) {
		try {
			if (pst != null) {
				pst.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			pst = null;
		}
	}

	/**
	 * 关闭ResultSet
	 * @param rs
	 */
	public static void close(ResultSet rs) {
		try {
			if (rs != null) {
				rs.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			rs = null;
		}
	}

	public static void main(String[] args){
//		List list = getColumns("ptemp3","abc");
//		String sql1 = "drop table oozieweb2.logging_event ";
//		String sql = "create table oozieweb2.logging_event(" +
//				"timestmp BIGINT," +
//				"formatted_message string," +
//				"logger_name string," +
//				"level_string string," +
//				"thread_name string," +
//				"reference_flag SMALLINT," +
//				"arg0 string," +
//				"arg1 string," +
//				"arg2 string," +
//				"arg3 string," +
//				"caller_filename string," +
//				"caller_class string," +
//				"caller_method string," +
//				"caller_line string," +
//				"event_id BIGINT" +
//				") ";
//		boolean flag = execSql(sql1, null);
//		boolean flag1 = execSql(sql, null);

	}
}
