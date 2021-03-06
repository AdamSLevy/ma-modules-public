/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.serotonin.m2m2.db.dao.JsonDataDao;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.json.JsonDataVO;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.web.mvc.rest.v1.exception.RestValidationFailedException;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestMessageLevel;
import com.serotonin.m2m2.web.mvc.rest.v1.message.RestProcessResult;
import com.serotonin.m2m2.web.mvc.rest.v1.model.jsondata.JsonDataModel;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * 
 * NoSQL Endpoint for storage of String data and JSON Objects
 * 
 * nosql/{data-id}/{data}
 * 
 * @author Terry Packer
 *
 */
@Api(value="JSON Store", description="Store custom data")
@RestController
@RequestMapping("/v1/json-data")
public class JsonDataRestController extends MangoVoRestController<JsonDataVO, JsonDataModel, JsonDataDao>{

	private static Log LOG = LogFactory.getLog(JsonDataRestController.class);
	enum MapOperation {
		APPEND,REPLACE,DELETE
	}

    private ObjectMapper mapper;
	
	/**
	 * @param dao
	 */
	@Autowired
	public JsonDataRestController(ObjectMapper mapper) {
		super(JsonDataDao.instance);
	    this.mapper = mapper;
	}

	@ApiOperation(
			value = "List all available xids",
			notes = "Shows any xids that you have read permissions for",
			response = List.class
			)
    @RequestMapping(method = RequestMethod.GET, produces={"application/json"})
    public ResponseEntity<List<String>> list(
    		HttpServletRequest request
   		){
        
    	RestProcessResult<List<String>> result = new RestProcessResult<List<String>>(HttpStatus.OK);
    	User user = this.checkUser(request, result);
    	if(result.isOk()){
    		List<JsonDataVO> all = this.dao.getAll();
    		List<String> available = new ArrayList<String>();
    		for(JsonDataVO vo : all){
    			//Check existing permissions
				if(Permissions.hasPermission(user, vo.getReadPermission())){
					available.add(vo.getXid());
				}
    		}
    		return result.createResponseEntity(available);
    	}
    	
    	return result.createResponseEntity();
	}

	@ApiOperation(
			value = "Get Public JSON Data",
			notes = "Returns only the data"
			)
    @RequestMapping(method = RequestMethod.GET, value="/public/{xid}", produces={"application/json"})
    public ResponseEntity<JsonDataModel> getPublicData(
    		HttpServletRequest request, 
    		@ApiParam(value = "XID", required = true, allowMultiple = false)
    		@PathVariable String xid
   		){
		RestProcessResult<JsonDataModel> result = new RestProcessResult<JsonDataModel>(HttpStatus.OK);
		JsonDataVO vo = JsonDataDao.instance.getByXid(xid);

		if(vo == null){
			result.addRestMessage(getDoesNotExistMessage());
			return result.createResponseEntity();
		}else{
			//Check existing permissions
			if(!vo.isPublicData()){
				result.addRestMessage(getUnauthorizedMessage());
				return result.createResponseEntity();
			}
			else{ 
				return result.createResponseEntity(new JsonDataModel(vo));
			}
		}
	}
	
	@ApiOperation(
			value = "Get JSON Data",
			notes = "Returns only the data"
			)
    @RequestMapping(method = RequestMethod.GET, value="/{xid}", produces={"application/json"})
    public ResponseEntity<JsonDataModel> getData(
    		HttpServletRequest request, 

    		@ApiParam(value = "XID", required = true, allowMultiple = false)
    		@PathVariable String xid
   		){
		return getDataWithPath(request, xid, null);
	}
	
	@ApiOperation(
			value = "Get JSON Data using a path",
			notes = "To get a sub component of the data use a path of member.submember"
			)
    @RequestMapping(method = RequestMethod.GET, value="/{xid}/{path:.*}", produces={"application/json"})
    public ResponseEntity<JsonDataModel> getDataWithPath(
    		HttpServletRequest request, 

    		@ApiParam(value = "XID", required = true, allowMultiple = false)
    		@PathVariable String xid,
    		
    		@ApiParam(value = "Data path using dots as separator", required = true, allowMultiple = false)
    		@PathVariable String path
   		){
        
    	RestProcessResult<JsonDataModel> result = new RestProcessResult<JsonDataModel>(HttpStatus.OK);
    	User user = this.checkUser(request, result);
    	if(result.isOk()){
    		
    		JsonDataVO vo = JsonDataDao.instance.getByXid(xid);
    		if(vo == null){
    			result.addRestMessage(getDoesNotExistMessage());
    		}else{
    			//Check existing permissions
				if(!Permissions.hasPermission(user, vo.getReadPermission())){
					result.addRestMessage(getUnauthorizedMessage());
					return result.createResponseEntity();
				}
				
				if(path == null)
					return result.createResponseEntity(new JsonDataModel(vo));
				else{
		    		String[] pathParts;
		    		if(path.contains("."))
		    			pathParts = path.split("\\.");
		    		else
		    			pathParts = new String[]{ path };
		    		
		    		Object data = vo.getJsonData();
		    		if (data instanceof JsonNode) {
		    		    data = getSubset(pathParts, (JsonNode) data);
		    		}
		    		if(data == null){
		    			result.addRestMessage(getDoesNotExistMessage());
		    			return result.createResponseEntity();
		    		}else{
		    			vo.setJsonData(data);
		    			return result.createResponseEntity(new JsonDataModel(vo));
		    		}
				}
    		}
    	}
    	
    	return result.createResponseEntity();
	}

	@ApiOperation(
			value = "Append JSON Data to existing",
			notes = "{path} is the path to data starting with xid/member/sub-member",
			response=JsonDataModel.class
			)
	@ApiResponses({
			@ApiResponse(code = 201, message = "Data Created", response=JsonDataModel.class),
			@ApiResponse(code = 401, message = "Unauthorized Access", response=ResponseEntity.class),
			@ApiResponse(code = 409, message = "Data Already Exists")
			})
	@RequestMapping(method = RequestMethod.PUT, value="/{xid}", consumes={"application/json"}, produces={"application/json"})
    public ResponseEntity<JsonDataModel> updateJsonData(
    		@ApiParam(value = "XID", required = true, allowMultiple = false)
    		@PathVariable String xid,
    		
    		@ApiParam(value = "Read Permissions", required = false, defaultValue="", allowMultiple = true)
    		@RequestParam(required=false, defaultValue="") Set<String> readPermission,

    		@ApiParam(value = "Edit Permissions", required = false, defaultValue="", allowMultiple = true)
    		@RequestParam(required=false, defaultValue="") Set<String> editPermission,

    		@ApiParam(value = "Name", required = true, allowMultiple = false, defaultValue="")
    		@RequestParam(required=false, defaultValue="") String name,
    		
    		@ApiParam(value = "Is public?", required = true, allowMultiple = false, defaultValue="false")
    		@RequestParam(required=false, defaultValue="false") boolean publicData,
    		
    		@ApiParam( value = "Data to save", required = true )
    		@RequestBody(required=false)
    		JsonNode data,
    		UriComponentsBuilder builder,
    		HttpServletRequest request) throws RestValidationFailedException {
		return updateJsonData(xid, null, readPermission, editPermission, name, publicData, data, builder, request);
	}
	
	@ApiOperation(
			value = "Update JSON Data",
			notes = "{path} is the path to data with dots data.member.submember",
			response=JsonDataModel.class
			)
	@ApiResponses({
			@ApiResponse(code = 201, message = "Data Created", response=JsonDataModel.class),
			@ApiResponse(code = 401, message = "Unauthorized Access", response=ResponseEntity.class),
			@ApiResponse(code = 403, message = "Data Doesn't Exists")
			})
	@RequestMapping(method = RequestMethod.PUT, value="/{xid}/{path:.*}", consumes={"application/json"}, produces={"application/json"})
    public ResponseEntity<JsonDataModel> updateJsonData(
    		@ApiParam(value = "XID", required = true, allowMultiple = false)
    		@PathVariable String xid,
    		
    		@ApiParam(value = "Data path using dots as separator", required = true, allowMultiple = false)
    		@PathVariable String path,
    		
    		@ApiParam(value = "Read Permissions", required = false, defaultValue="", allowMultiple = true)
    		@RequestParam(required=false, defaultValue="") Set<String> readPermission,

    		@ApiParam(value = "Edit Permissions", required = false, defaultValue="", allowMultiple = true)
    		@RequestParam(required=false, defaultValue="") Set<String> editPermission,

    		@ApiParam(value = "Name", required = true, allowMultiple = false, defaultValue="")
    		@RequestParam(required=false, defaultValue="") String name,
    		
    		@ApiParam(value = "Is public?", required = true, allowMultiple = false, defaultValue="false")
    		@RequestParam(required=false, defaultValue="false") boolean publicData,
    		
    		@ApiParam( value = "Data to save", required = true )
    		@RequestBody(required=true)
    		JsonNode data,
    		UriComponentsBuilder builder,
    		HttpServletRequest request) throws RestValidationFailedException {
		RestProcessResult<JsonDataModel> result = new RestProcessResult<JsonDataModel>(HttpStatus.CREATED);
		return modifyJsonData(MapOperation.APPEND, result, xid, path, readPermission, editPermission, name, publicData, data, builder, request);
	}
	
	
	@ApiOperation(
			value = "Create JSON Data",
			notes = "{path} is the path to data starting with xid/member/sub-member",
			response=JsonDataModel.class
			)
	@ApiResponses({
			@ApiResponse(code = 201, message = "Data Created", response=JsonDataModel.class),
			@ApiResponse(code = 401, message = "Unauthorized Access", response=ResponseEntity.class),
			@ApiResponse(code = 409, message = "Data Already Exists")
			})
	@RequestMapping(method = RequestMethod.POST, value="/{xid}", consumes={"application/json"}, produces={"application/json"})
    public ResponseEntity<JsonDataModel> createJsonData(
    		@ApiParam(value = "XID", required = true, allowMultiple = false)
    		@PathVariable String xid,
    		
    		@ApiParam(value = "Read Permissions", required = false, defaultValue="", allowMultiple = true)
    		@RequestParam(required=false, defaultValue="") Set<String> readPermission,

    		@ApiParam(value = "Edit Permissions", required = false, defaultValue="", allowMultiple = true)
    		@RequestParam(required=false, defaultValue="") Set<String> editPermission,

    		@ApiParam(value = "Name", required = true, allowMultiple = false, defaultValue="")
    		@RequestParam(required=false, defaultValue="") String name,
    		
    		@ApiParam(value = "Is public?", required = true, allowMultiple = false, defaultValue="false")
    		@RequestParam(required=false, defaultValue="false") boolean publicData,
    		
    		@ApiParam( value = "Data to save", required = true )
    		@RequestBody(required=true)
    		JsonNode data,
    		UriComponentsBuilder builder,
    		HttpServletRequest request) throws RestValidationFailedException {
		return replaceJsonData(xid, null, readPermission, editPermission, name, publicData, data, builder, request);
	}
	
	@ApiOperation(
			value = "Replace JSON Data",
			notes = "{path} is the path to data with dots data.member.submember",
			response=JsonDataModel.class
			)
	@ApiResponses({
			@ApiResponse(code = 201, message = "Data Created", response=JsonDataModel.class),
			@ApiResponse(code = 401, message = "Unauthorized Access", response=ResponseEntity.class),
			@ApiResponse(code = 409, message = "Data Already Exists")
			})
	@RequestMapping(method = RequestMethod.POST, value="/{xid}/{path:.*}", consumes={"application/json"}, produces={"application/json"})
    public ResponseEntity<JsonDataModel> replaceJsonData(
    		@ApiParam(value = "XID", required = true, allowMultiple = false)
    		@PathVariable String xid,
    		
    		@ApiParam(value = "Data path using dots as separator", required = true, allowMultiple = false)
    		@PathVariable String path,
    		
    		@ApiParam(value = "Read Permissions", required = false, defaultValue="", allowMultiple = true)
    		@RequestParam(required=false, defaultValue="") Set<String> readPermission,

    		@ApiParam(value = "Edit Permissions", required = false, defaultValue="", allowMultiple = true)
    		@RequestParam(required=false, defaultValue="") Set<String> editPermission,

    		@ApiParam(value = "Name", required = true, allowMultiple = false, defaultValue="")
    		@RequestParam(required=false, defaultValue="") String name,
    		
    		@ApiParam(value = "Is public?", required = true, allowMultiple = false, defaultValue="false")
    		@RequestParam(required=false, defaultValue="false") boolean publicData,
    		
    		@ApiParam( value = "Data to save", required = true )
    		@RequestBody(required=true)
    		JsonNode data,
    		UriComponentsBuilder builder,
    		HttpServletRequest request) throws RestValidationFailedException {
		RestProcessResult<JsonDataModel> result = new RestProcessResult<JsonDataModel>(HttpStatus.CREATED);
		return modifyJsonData(MapOperation.REPLACE, result, xid, path, readPermission, editPermission, name, publicData, data, builder, request);
	}
	
	@ApiOperation(
			value = "Fully Delete JSON Data",
			notes = "",
			response=JsonDataModel.class
			)
	@ApiResponses({
			@ApiResponse(code = 201, message = "Data Deleted", response=JsonDataModel.class),
			@ApiResponse(code = 401, message = "Unauthorized Access", response=ResponseEntity.class),
			@ApiResponse(code = 403, message = "Data Doesn't Exists")
			})
	@RequestMapping(method = RequestMethod.DELETE, value="/{xid}", produces={"application/json"})
    public ResponseEntity<JsonDataModel> deleteJsonData(
    		@ApiParam(value = "XID", required = true, allowMultiple = false)
    		@PathVariable String xid,
    		
    		UriComponentsBuilder builder,
    		HttpServletRequest request) throws RestValidationFailedException {
		return deletePartialJsonData(xid, null, builder, request);
	}
	
	@ApiOperation(
			value = "Partially Delete JSON Data",
			notes = "{path} is the path to data with dots data.member.submember",
			response=JsonDataModel.class
			)
	@ApiResponses({
			@ApiResponse(code = 201, message = "Data Deleted", response=JsonDataModel.class),
			@ApiResponse(code = 401, message = "Unauthorized Access", response=ResponseEntity.class),
			@ApiResponse(code = 403, message = "Data Doesn't Exists")
			})
	@RequestMapping(method = RequestMethod.DELETE, value="/{xid}/{path:.*}", produces={"application/json"})
    public ResponseEntity<JsonDataModel> deletePartialJsonData(
    		@ApiParam(value = "XID", required = true, allowMultiple = false)
    		@PathVariable String xid,
    		
    		@ApiParam(value = "Data path using dots as separator", required = true, allowMultiple = false)
    		@PathVariable String path,
    		
    		UriComponentsBuilder builder,
    		HttpServletRequest request) throws RestValidationFailedException {

		RestProcessResult<JsonDataModel> result = new RestProcessResult<JsonDataModel>(HttpStatus.CREATED);
    	User user = this.checkUser(request, result);
    	if(result.isOk()){
    		
    		JsonDataVO vo = this.dao.getByXid(xid);
			if(vo != null){
				//Going to delete data
				
				//Check existing permissions
				if(!Permissions.hasPermission(user, vo.getEditPermission())){
					result.addRestMessage(getUnauthorizedMessage());
					return result.createResponseEntity();
				}
				JsonDataModel model = new JsonDataModel(vo);
				
				if(path == null){
					//Delete the whole thing
					this.dao.delete(vo.getId());
				}else{
					//Delete something from the map
					Object existingData = convertToMap(vo.getJsonData());

					//Find the Object to replace with this map
					String[] pathParts;
		    		if(path.contains("."))
		    			pathParts = path.split("\\.");
		    		else
		    			pathParts = new String[]{ path };
					if(!modify(MapOperation.DELETE, pathParts, existingData, null)){
						result.addRestMessage(getDoesNotExistMessage());
						return result.createResponseEntity();
					}else{		    		
			    		if(!model.validate()){
			    		    result.addRestMessage(this.getValidationFailedError());
			    		    return result.createResponseEntity(model);
			    		}
			    		try {
			                String initiatorId = request.getHeader("initiatorId");
			                this.dao.save(vo, initiatorId);
			            } catch (Exception e) {
			                LOG.error(e.getMessage(),e);
			                result.addRestMessage(getInternalServerErrorMessage(e.getMessage()));
			            }
					}
				}
				return result.createResponseEntity(model);	
			}else{
				result.addRestMessage(getDoesNotExistMessage());
			}
    	}
    	
    	return result.createResponseEntity();
	}
	

	/**
	 * Helper to modify data
	 * @param result
	 * @param xid
	 * @param path
	 * @param readPermissions
	 * @param editPermissions
	 * @param name
	 * @param data
	 * @param builder
	 * @param request
	 * @return
	 */
	private ResponseEntity<JsonDataModel> modifyJsonData(MapOperation operation, RestProcessResult<JsonDataModel> result,
			String xid, String path, Set<String> readPermissions, Set<String> editPermissions, String name, boolean publicData, 
			Object data, UriComponentsBuilder builder, HttpServletRequest request){

		User user = this.checkUser(request, result);
    	if(result.isOk()){
    		
    		JsonDataVO vo = this.dao.getByXid(xid);
			if(vo != null){
				//Going to merge data
				
				//Check existing permissions
				if(!Permissions.hasPermission(user, vo.getEditPermission())){
					result.addRestMessage(getUnauthorizedMessage());
					return result.createResponseEntity();
				}
				
				//Replace the data
				vo.setName(name);
				vo.setPublicData(publicData);
				vo.setReadPermission(Permissions.implodePermissionGroups(readPermissions));
				vo.setEditPermission(Permissions.implodePermissionGroups(editPermissions));

				//Merge the maps
				Object existingData = convertToMap(vo.getJsonData());

				if(path == null){
					vo.setJsonData(data);
				}else{
					//Find the Object to replace with this map
					String[] pathParts;
		    		if(path.contains("."))
		    			pathParts = path.split("\\.");
		    		else
		    			pathParts = new String[]{ path };
					if(!modify(operation, pathParts, existingData, data)){
						result.addRestMessage(getDoesNotExistMessage());
						return result.createResponseEntity();
					}
						
				}				
			}else{
				if(operation == MapOperation.APPEND){
					result.addRestMessage(getDoesNotExistMessage());
					return result.createResponseEntity();
				}
				
				//Going to create a new one
				vo = new JsonDataVO();
				vo.setXid(xid);
				vo.setName(name);
				vo.setPublicData(publicData);
				vo.setReadPermission(Permissions.implodePermissionGroups(readPermissions));
				vo.setEditPermission(Permissions.implodePermissionGroups(editPermissions));
				vo.setJsonData(data);
			}
    		
    		JsonDataModel model = new JsonDataModel(vo);
    		if(!model.validate()){
    		    result.addRestMessage(this.getValidationFailedError());
    		    // return only the data that was saved, i.e. the data that we supplied a path to
                vo.setJsonData(data);
    		    return result.createResponseEntity(model);
    		}
    		
    		//Ensure we have the correct permissions
    		//First we must check to ensure that the User actually has editPermission before they can save it otherwise
    		// they won't be able to modify it.
    		Set<String> userPermissions = Permissions.explodePermissionGroups(user.getPermissions());
    		
    		if(!user.isAdmin() && Collections.disjoint(userPermissions, editPermissions)){
    			//Return validation error
    			result.addRestMessage(this.getValidationFailedError());
    			model.addValidationMessage("jsonData.editPermissionRequired", RestMessageLevel.ERROR, "editPermission");
    			vo.setJsonData(data);
    		    return result.createResponseEntity(model);
    		}
    		
    		try {
                String initiatorId = request.getHeader("initiatorId");
                this.dao.save(vo, initiatorId);
                // return only the data that was saved, i.e. the data that we supplied a path to
                vo.setJsonData(data);
                URI location = builder.path("/v1/json-data/{xid}").buildAndExpand(new Object[]{vo.getXid()}).toUri();
                result.addRestMessage(this.getResourceCreatedMessage(location));
                return result.createResponseEntity(model);
            } catch (Exception e) {
                LOG.error(e.getMessage(),e);
                result.addRestMessage(getInternalServerErrorMessage(e.getMessage()));
            }
    	}
    	
    	return result.createResponseEntity();
	}
	
	/**
	 * Modify the data at the path
	 * 
	 * @param pathParts
	 * @param existingData
	 * @return true if found
	 */
	@SuppressWarnings("unchecked")
	private boolean modify(MapOperation operation, String[] dataPath, Object existingData, Object newData) {
		
		Object sub = existingData;
		int count = 0;
		for(String path : dataPath){
			if((sub != null)&&(sub instanceof Map)){
				count ++;
				if(count == dataPath.length){
					//Found it
					switch(operation){
					case APPEND:
						Object existingValue = ((Map<String, Object>)sub).get(path);
						if(existingValue != null){
							if(existingValue instanceof Map){
								if(newData instanceof Map)
									((Map<String, Object>)existingValue).putAll((Map<String, Object>)newData);
								else
									return false; //What do we do here?
							}
						}else{
							//New entry
							((Map<String, Object>)sub).put(path, newData);
						}
					case REPLACE:
						//Update it
						((Map<String, Object>)sub).put(path, newData);
						return true;
					case DELETE:
						Object removed = ((Map<String, Object>)sub).remove(path);
						return removed != null;
					}
				}
				sub = ((Map<String,Object>)sub).get(path);
			}
		}
		return false;
	}

	private JsonNode getSubset(String[] dataPath, JsonNode node) {
	    if (node == null) return null;
	    
		for (String path : dataPath) {
		    node = node.get(path);
		    if (node == null) {
		        return null;
		    }
		}
		return node;
	}
	
	private Object convertToMap(Object existingData) {
        // TODO this conversion to Map type used to happen in the DAO, we should re-work the REST controller code to work
	    // directly with JsonNodes (as per the get by path method)
        if (existingData instanceof JsonNode) {
            TypeFactory typeFactory = mapper.getTypeFactory();
            MapType mapType = typeFactory.constructMapType(HashMap.class, String.class, Object.class);
            try {
                existingData = mapper.convertValue(existingData, mapType);
            } catch(Exception e) {
                LOG.error(e.getMessage(), e);
                existingData = null;
            }
        }
        return existingData;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.MangoVoRestController#createModel(java.lang.Object)
	 */
	@Override
	public JsonDataModel createModel(JsonDataVO vo) {
		return new JsonDataModel(vo);
	}
	
}
