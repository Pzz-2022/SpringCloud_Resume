package com.resume.position.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author 彭政
 * @since 2023-06-18
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class Position implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 岗位ID
     */
    @TableId(value = "pk_position_id", type = IdType.AUTO)
    private Long pkPositionId;

    /**
     * 公司ID
     */
    private Long companyId;

    /**
     * 岗位名称
     */
    private String positionName;

    /**
     * 创建人ID
     */
    private Long createUserId;

    /**
     * 岗位描述
     */
    private String description;

    /**
     * 招聘人数
     */
    private Integer hc;

    /**
     * 工作城市
     */
    private String workingCity;

    /**
     * 工作年限：不限、0-3年、3-5年、5-10年、10年以上
     */
    private String workingYears;

    /**
     * 学历要求：大专、本科、硕士、博士
     */
    private String educationBackground;

    /**
     * 工作性质：不限、兼职、全职、外包、实习
     */
    private String type;

    /**
     * 薪资-最低(k)
     */
    private Integer salaryMin;

    /**
     * 薪资-最高(k)
     */
    private Integer salaryMax;

    /**
     * 薪资-多少月
     */
    private Integer salaryMonth;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 删除状态：1-已删除、0-未删除
     */
    @TableLogic
    private Integer isDeleted;


}