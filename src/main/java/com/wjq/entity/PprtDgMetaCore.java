package com.wjq.entity;

import java.io.Serializable;

import java.util.Date;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableName;
import java.io.Serializable;

import com.baomidou.mybatisplus.annotations.Version;

import lombok.*;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author wangjunqing
 * @since 2018-03-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("pprt_dg_meta_core")
public class PprtDgMetaCore extends Model<PprtDgMetaCore> {

    private static final long serialVersionUID = 1L;

    @TableId("ID")
	private String id;
    /**
     * 元数据名称
     */
	@TableField("NAME")
	private String name;
    /**
     * 属性名称
     */
	@TableField("CH_NAME")
	private String chName;
    /**
     * 元数据类型，如表、视图、键值等
     */
	@TableField("TYPE")
	private String type;
	@TableField("META_MDL")
	private String metaMdl;
    /**
     * 元数据实体，JSON格式
     */
	@TableField("ENTITY")
	private String entity;
    /**
     * 父节点ID
     */
	@TableField("PRT_ID")
	private String prtId;
    /**
     * 备注
     */
	@TableField("CMT")
	private String cmt;
    /**
     * 元数据创建时间
     */
	@TableField("CRT_DATE")
	private Date crtDate;
	@TableField("LAST_UPDATE")
	private Date lastUpdate;
	@TableField("BIZ_DOMAIN")
	private String bizDomain;
	@TableField("BIZ_NO")
	private String bizNo;
	@TableField("NAMESPACE")
	private String namespace;
	@TableField("REF_OBJECT")
	private String refObject;
	@TableField("CYCLE")
	private String cycle;
	@TableField("USE_TABLE")
	private String useTable;
	@TableField("FILE_NAME")
	private String fileName;
	@TableField("FILE_HEADER")
	private String fileHeader;
	@TableField("LVEL")
	private String lvel;
	@TableField("CRT_USER")
	private String crtUser;
	@TableField("TENANTID")
	private String tenantid;
	@TableField("MGR_TYPE")
	private String mgrType;
	@TableField("PREC")
	private Integer prec;
	@TableField("DATA_TYPE")
	private String dataType;
	@TableField("LENGTH")
	private Integer length;
    /**
     * 格式化规则
     */
	@TableField("FMT_RULE")
	private String fmtRule;
    /**
     * 缺省展示组件类型
     */
	@TableField("DEFAULT_VIEW")
	private String defaultView;
    /**
     * 属性描述
     */
	@TableField("REMARKS")
	private String remarks;
	@TableField("KBUSER")
	private String kbuser;
	@TableField("KEYTAB")
	private String keytab;
	@TableField("LAYER")
	private String layer;


	@Override
	protected Serializable pkVal() {
		return this.id;
	}

}
