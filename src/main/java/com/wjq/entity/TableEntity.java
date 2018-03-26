package com.wjq.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;

import java.util.List;

@Data
@Builder
public class TableEntity {
	private String tableName;
	private String tableChName;
	private String tableCmt;
	private List<ColumnEntity> columnEntities;

	@Data
	public class ColumnEntity{
		private String proName;
		private String remark;
		private String cmt;
	}
}
