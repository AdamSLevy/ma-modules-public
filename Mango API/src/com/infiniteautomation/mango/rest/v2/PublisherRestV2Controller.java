/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.rest.v2;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.infiniteautomation.mango.rest.v2.exception.AlreadyExistsRestException;
import com.infiniteautomation.mango.rest.v2.exception.NotFoundRestException;
import com.infiniteautomation.mango.rest.v2.exception.ValidationFailedRestException;
import com.infiniteautomation.mango.rest.v2.model.RestValidationResult;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.QueryDataPageStream;
import com.serotonin.m2m2.web.mvc.rest.v1.model.publisher.AbstractPublisherModel;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import net.jazdw.rql.parser.ASTNode;

/**
 * @author Terry Packer
 * 
 */
@Api(value="Publishers", description="Publishers endpoints")
@RestController()
@RequestMapping("/v2/publishers")
public class PublisherRestV2Controller extends AbstractMangoVoRestV2Controller<PublisherVO<?>, AbstractPublisherModel<?, ?>, PublisherDao>{

	public PublisherRestV2Controller(){
		super(PublisherDao.instance);
	}

	@ApiOperation(
			value = "Query Publishers",
			notes = "Use RQL formatted query, admin only",
			response=AbstractPublisherModel.class,
			responseContainer="List"
			)
	@RequestMapping(method = RequestMethod.GET, produces={"application/json", "application/sero-json"})
    public ResponseEntity<QueryDataPageStream<PublisherVO<?>>> queryRQL(@AuthenticationPrincipal User user, HttpServletRequest request) {
		assertAdmin(user);
		ASTNode node = this.parseRQLtoAST(request);
		return new ResponseEntity<>(getPageStream(node), HttpStatus.OK);		
	}
	
	@ApiOperation(
			value = "Get all data publishers",
			notes = "Only returns publishers to admin users"
			)
    @RequestMapping(method = RequestMethod.GET, produces={"application/json", "application/sero-json"}, value = "/list")
    public ResponseEntity<List<AbstractPublisherModel<?,?>>> getAll(@AuthenticationPrincipal User user, HttpServletRequest request) {
		assertAdmin(user);
        List<PublisherVO<?>> publishers = this.dao.getAll();
        List<AbstractPublisherModel<?,?>> models = new ArrayList<AbstractPublisherModel<?,?>>();
        for(PublisherVO<?> pub : publishers)
   			models.add(pub.asModel());
        return new ResponseEntity<>(models, HttpStatus.OK);
    }
	
	@ApiOperation(
			value = "Get publisher by xid",
			notes = "Only returns publishers for admin users"
			)
	@RequestMapping(method = RequestMethod.GET, value = "/{xid}", produces={"application/json"})
    public ResponseEntity<AbstractPublisherModel<?,?>> getPublisher(
    		@AuthenticationPrincipal User user,
    		HttpServletRequest request, 
    		@ApiParam(value = "Valid Publisher XID", required = true, allowMultiple = false)
    		@PathVariable String xid) {
		assertAdmin(user);
        PublisherVO<?> vo = this.dao.getByXid(xid);
        if (vo == null)
            throw new NotFoundRestException();
        else
        	return new ResponseEntity<>(vo.asModel(), HttpStatus.OK);
    }
	
	
	/**
	 * Update a publisher
	 * @param xid
	 * @param model
     * @param builder
	 * @param request
	 * @return
	 */
	@ApiOperation(value = "Create publisher", notes="Admin only")
	@RequestMapping(method = RequestMethod.POST, 		
		consumes={"application/json", "application/sero-json"}, 
		produces={"application/json", "application/sero-json"})
    public ResponseEntity<AbstractPublisherModel<?,?>> save(
    		@AuthenticationPrincipal User user,
    		@RequestBody(required=true) AbstractPublisherModel<?,?> model, 
    		UriComponentsBuilder builder, 
    		HttpServletRequest request) {
		assertAdmin(user);
		PublisherVO<?> vo = model.getData();
        PublisherVO<?> existing = this.dao.getByXid(vo.getXid());
        if (existing != null) 
        	throw new AlreadyExistsRestException(vo.getXid());
        
        ProcessResult validation = new ProcessResult();
        vo.validate(validation);
		if(validation.getHasMessages())
			throw new ValidationFailedRestException(new RestValidationResult(validation));
		else
			Common.runtimeManager.savePublisher(vo);
        
    	URI location = builder.path("/v2/publishers/{xid}").buildAndExpand(vo.getXid()).toUri();
    	return getResourceCreated(vo.asModel(), location.toString());
    }
	
	/**
	 * Update a publisher
	 * @param xid
	 * @param model
     * @param builder
	 * @param request
	 * @return
	 */
	@ApiOperation(value = "Update publisher", notes="admin only")
	@RequestMapping(method = RequestMethod.PUT, value = "/{xid}", 
			consumes={"application/json", "application/sero-json"}, 
			produces={"application/json", "application/sero-json"})
    public ResponseEntity<AbstractPublisherModel<?,?>> updatePublisher(
    		@AuthenticationPrincipal User user,
       		@ApiParam(value = "Valid Publisher XID", required = true, allowMultiple = false)
    		@PathVariable String xid,
    		@RequestBody(required=true) AbstractPublisherModel<?,?> model, 
    		UriComponentsBuilder builder, 
    		HttpServletRequest request) {
		assertAdmin(user);
    	PublisherVO<?> vo = model.getData();
		
        PublisherVO<?> existing = this.dao.getByXid(xid);
        if (existing == null)
        	throw new NotFoundRestException();

        vo.setId(existing.getId());
        
        ProcessResult validation = new ProcessResult();
        vo.validate(validation);
        
        if(!model.validate()){
			throw new ValidationFailedRestException(new RestValidationResult(validation));
        }else{
            Common.runtimeManager.savePublisher(vo);
        }
     
    	URI location = builder.path("/v2/publishers/{xid}").buildAndExpand(xid).toUri();
    	return getResourceUpdated(vo.asModel(), location.toString());
    }

	@ApiOperation(
			value = "Delete publisher",
			notes = "admin only"
			)
	@RequestMapping(method = RequestMethod.DELETE, 
		consumes={"application/json", "application/sero-json"}, 
		produces={"application/json", "application/sero-json"},
		value={"/{xid}"})
    public ResponseEntity<AbstractPublisherModel<?,?>> delete(
       		@ApiParam(value = "Valid Publisher XID", required = true, allowMultiple = false)
       	 	@PathVariable String xid,
    		@AuthenticationPrincipal User user,
    		UriComponentsBuilder builder, HttpServletRequest request) {
		assertAdmin(user);
		//Check to see if it already exists
		PublisherVO<?> existing = this.dao.getByXid(xid);
		if(existing == null){
			throw new NotFoundRestException();
		}

		//Delete the Publisher
		Common.runtimeManager.deletePublisher(existing.getId());
		
        //Put a link to the updated data in the header?
    	URI location = builder.path("/v2/publishers/{xid}").buildAndExpand(existing.getXid()).toUri();
    	return getResourceDeleted(existing.asModel(), location.toString());
    }

	/**
	 * Helper method until issue #1020 is fixed
	 * @param user
	 */
	private void assertAdmin(User user){
		if(!user.isAdmin())
			throw new AccessDeniedException(user.getUsername() + " not admin, permission denied.");
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.MangoVoRestController#createModel(java.lang.Object)
	 */
	@Override
	public AbstractPublisherModel<?,?> createModel(PublisherVO<?> vo) {
		return vo.asModel();
	}

}
