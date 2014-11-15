/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.util.UnitUtil;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.PointLocatorVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.IntervalLoggingProperties;
import com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.IntervalLoggingType;
import com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.LoggingProperties;
import com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.LoggingType;
import com.serotonin.m2m2.web.mvc.rest.v1.model.pointValue.DataTypeEnum;


/**
 * Rest Data Model
 * 
 * 
 * @author Terry Packer
 *
 */
public class DataPointModel extends AbstractActionVoModel<DataPointVO>{
	
	private DataPointVO vo;
	
	public DataPointModel(){
		super(new DataPointVO());
		this.vo = (DataPointVO) this.data;
	}
	/**
	 * @param vo
	 */
	public DataPointModel(DataPointVO vo) {
		super(vo);
		this.vo = vo;
	}
	
	//Data Source XID here?
	
	@JsonGetter("deviceName")
	public String getDeviceName(){
		return this.vo.getDeviceName();
	}
	
	@JsonSetter("deviceName")
	public void setDeviceName(String deviceName){
		this.vo.setDeviceName(deviceName);
	}
	
	@JsonGetter("pointFolderId")
	public int getPointFolderId(int id){
		return this.vo.getPointFolderId();
	}
	@JsonSetter("pointFolderId")
	public void setPointFolderId(int id){
		this.vo.setPointFolderId(id);
	}
	
	@JsonGetter("loggingProperties")
	public LoggingProperties getLoggingProperties(){
		
		//Are we interval logging
		if(this.vo.getLoggingType() == DataPointVO.LoggingTypes.INTERVAL){
			//What kind of interval?
			switch(this.vo.getIntervalLoggingType()){
				case DataPointVO.IntervalLoggingTypes.INSTANT:
				case DataPointVO.IntervalLoggingTypes.MAXIMUM:
				case DataPointVO.IntervalLoggingTypes.MINIMUM:
					return new IntervalLoggingProperties(
							LoggingType.convertTo(this.vo.getLoggingType()),
							IntervalLoggingType.convertTo(this.vo.getIntervalLoggingType()));
				case DataPointVO.IntervalLoggingTypes.AVERAGE:
					//TODO this needs the sample window size and period
					return new IntervalLoggingProperties(
						LoggingType.convertTo(this.vo.getLoggingType()),
						IntervalLoggingType.convertTo(this.vo.getIntervalLoggingType()));
				default:
					throw new ShouldNeverHappenException("Unknown Interval Logigng Type: " + this.vo.getIntervalLoggingType());
			}
		}else{
			return new LoggingProperties(LoggingType.convertTo(this.vo.getLoggingType()));
		}
		
	}
	@JsonSetter("loggingProperties")
	public void setLoggingProperties(LoggingProperties props){
		//TODO Finish this
		System.out.println(props.getType());
	}
	
	//TODO Implement this with subclass JSON Mappings probably
	@JsonGetter("pointLocator")
	public PointLocatorVO getPointLocator(){
		return this.vo.getPointLocator();
	}
	@JsonSetter("pointLocator")
	public void setPointLocator(PointLocatorVO plVo){
		//TODO This is broken, but the getter is working fine
		this.vo.setPointLocator(plVo);
	}
	
	
	//TODO Missing Many Properties HERE
	@JsonGetter("unit")
	public String getUnit(){
		return UnitUtil.formatLocal(this.vo.getUnit());
	}
	@JsonSetter("unit")
	public void setUnit(String unit){
		this.vo.setUnit(UnitUtil.parseLocal(unit));
	}

	@JsonGetter("dataType")
	public DataTypeEnum getDataType(){
		return DataTypeEnum.convertTo(this.vo.getPointLocator().getDataTypeId());
	}
	
	@JsonSetter("dataType")
	public void setDataType(DataTypeEnum type){
		throw new ShouldNeverHappenException("Can't set a data type yet!");
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.model.AbstractRestModel#validate(com.serotonin.m2m2.i18n.ProcessResult)
	 */
	//@Override
	public void validate(ProcessResult response) {
		this.vo.validate(response);
	}
	
	@JsonIgnore
	public DataPointVO getData(){
		return this.vo;
	}
	
}
