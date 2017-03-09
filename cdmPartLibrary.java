import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;

import com.mando.util.SqlSessionUtil;
import com.mando.util.cdmConstantsUtil;
import com.mando.util.cdmJsonDataUtil;
import com.mando.util.cdmOwnerRolesUtil;
import com.mando.util.cdmPropertiesUtil;
import com.mando.util.cdmStringUtil;
import com.matrixone.apps.common.Company;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.engineering.EngineeringConstants;
import com.matrixone.apps.engineering.EngineeringUtil;
import com.matrixone.apps.engineering.Part;
import com.matrixone.apps.engineering.PartFamily;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MQLCommand;
import matrix.util.SelectList;
import matrix.util.StringList;

import matrix.db.BusinessObjectWithSelectList;
import java.util.Hashtable;

/**
 * @author jh.Lee
 * @desc 
 */
public class ${CLASSNAME} {
	
	String rootName = "PROJECT";
	String rootType = "General Library";
	String propertyFile = "/config/mybatis/config.properties";
	String plmPartUrl = "PLM_PART_URL";
	String plmPartsUrl = "PLM_PARTS_URL";
	
	
	private ArrayList columnValues;
	
	public ${CLASSNAME} () throws Exception {
		
	}
	
	public ${CLASSNAME}(Context context, String[] args) throws Exception {
		
	}
	
	/**
	 * @desc "General Library" Tree View
	 */
	public MapList getFullLevelOrg(Context context, String[] args) throws Exception {
		HashMap<String,MapList> fullLevelData = new HashMap<String,MapList>();
		MapList objectList = new MapList();
		try{		
            StringList strListSelBus	= new StringList();
            strListSelBus.add(DomainObject.SELECT_ID);
            strListSelBus.add(DomainObject.SELECT_TYPE);
            strListSelBus.add(DomainObject.SELECT_NAME);
            strListSelBus.add(Company.SELECT_ORGANIZATION_NAME);
       
            // Relationship Select
            StringList strListSelRel	= new StringList();
            strListSelRel.add(DomainObject.SELECT_RELATIONSHIP_ID);
            strListSelRel.add(DomainObject.SELECT_FROM_ID);
            strListSelRel.add(DomainObject.SELECT_FIND_NUMBER);
		
            objectList = DomainObject.findObjects(context,
            									  rootType,
										          "*",
										          "*",
										          "*",
										          cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION,
										          "", // where expression
										          "",
										          false,
										          strListSelBus,
										          (short) 0);
            
            fullLevelData.put(rootType, objectList);
        }catch(Exception e){
	        e.printStackTrace();
	    }
		return fullLevelData.get(rootType);
    }
	
	/**
	 * @desc "General Library" Chooser View First Node
	 */
	public MapList getGeneralLibraryFirstNode(Context context, String[] args) throws Exception {
		
		MapList nodeList = new MapList();
		HashMap paramMap = (HashMap) JPO.unpackArgs(args);
		
		String master = (String)paramMap.get("searchMode");
		String refName = (String)paramMap.get("refName");
		String displayKey = (String)paramMap.get("displayKey");
		if(displayKey==null || "".equalsIgnoreCase(displayKey)) displayKey = "name";
		
		String type = "General Class";
		String relation = "Subclass";
		SelectList selectList = new SelectList(6);
		selectList.addId();
		selectList.addName();
		selectList.addType();
		selectList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_TITLE);
		selectList.add(displayKey);
		selectList.add("from[" + relation + "].to.name");
		selectList.add("from[" + relation + "].to.id");
		
		boolean isLeaf = true, hasChild=false;
        String strLeaf = null, strChild = null;
        MapList mapList = new MapList();
		try {
			MapList rootList = DomainObject.findObjects(context, 
					rootType, 
					rootName, 
					"*", 
					"*", 
					cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION, 
					"",
					"",
					true, 
					selectList, 
					(short)0);
			
			if(rootList==null || rootList.size()==0) return new MapList();
			Map rootMap = (Map)rootList.get(0);
			String rootId = (String)rootMap.get("id");
			DomainObject nodeObj = DomainObject.newInstance(context);
			nodeObj.setId(rootId);
			mapList = nodeObj.getRelatedObjects(context, 
					relation, 		// relationship
					type,     		// type
					selectList,     // objects
					null,  			// relationships
					false,          // to
					true,          	// from
					(short)1,       // recurse
					null,           // where
					null,           // relationship where
					(short)0);      // limit
			mapList.sort("name", "ascending", "string");
			
			if(mapList != null && mapList.size() > 0) {
				for (int i = 0; i < mapList.size(); i++) {
					Map map = (Map) mapList.get(i);
					String name = (String)map.get("name");
					String display = (String)map.get(displayKey);
					if(map.get("from[" + relation + "].to.id") == null) {
						hasChild = false;
						isLeaf = true;
					} else {
						hasChild = true;
						isLeaf = true;
					}
					strLeaf = (isLeaf?"TRUE":"FALSE");
					strChild = (hasChild?"TRUE":"FALSE");
					map.put("name", name);
					map.put(displayKey, display);
					map.put("IS_LEAF", strLeaf);
					map.put("hasChild", strChild);
					nodeList.add(map);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return nodeList;
    }
	
	/**
	 * @desc "General Library" Chooser View Expand Node
	 */
	@SuppressWarnings("deprecation")
	public MapList expandGeneralLibraryNode(Context context, String[]args) throws Exception {
    	MapList expandList = new MapList();
    	try {
    		HashMap paramMap = (HashMap) JPO.unpackArgs(args);
    		String type = "General Class";
    		String relation = "Subclass";
    		
    		SelectList selectList = new SelectList(6);
        	selectList.addId();
    		selectList.addName();
    		selectList.addType();
    		selectList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_TITLE);
    		selectList.add("level");
    		selectList.add("from[" + relation + "].to.name");
    		selectList.add("from[" + relation + "].to.id");
			
			String objectId = (String)paramMap.get("objectId");
			String filterType = (String)paramMap.get("filterType");
			String orgId = (String)paramMap.get("orgId");
			
			boolean isLeaf = true, hasChild=false;
	        String strLeaf = null, strChild = null;
			
			MapList mapList = new MapList();
			if(objectId == null){
				if(orgId == null){
					objectId = MqlUtil.mqlCommand(context, "print bus $1 $2 $3 select $4 dump", new String[] {rootType, rootName, "-", DomainConstants.SELECT_ID});
					DomainObject nodeObj = new DomainObject(objectId);
					mapList = nodeObj.getRelatedObjects(context, 
							relation, 		// relationship
							type,     		// type
							selectList,     // objects
							null,  			// relationships
							false,          // to
							true,          	// from
							(short)0,       // recurse
							null,           // where
							null,           // relationship where
							(short)0);      // limit
					mapList.sort("name", "ascending", "string");
					if(mapList != null && mapList.size() > 0) {
						for (int i = 0; i < mapList.size(); i++) {
							Map map = (Map) mapList.get(i);
							String name  = (String)map.get("name");
							String level = (String)map.get("level");
							if(map.get("from[" + relation + "].to.id") != null){
								hasChild = true;
								isLeaf = true;
							} else {
								hasChild = false;
								isLeaf = true;
							}
							strLeaf = (isLeaf?"TRUE":"FALSE");
							strChild = (hasChild?"TRUE":"FALSE");
							map.put("IS_LEAF", strLeaf);
							map.put("hasChild", strChild);
							expandList.add(map);	
						}
					}
				}else{
					DomainObject nodeObj = new DomainObject(orgId);
					mapList = nodeObj.getRelatedObjects(context, 
							relation, 		// relationship
							type,     		// type
							selectList,     // objects
							null,  			// relationships
							false,          // to
							true,          	// from
							(short)1,       // recurse
							null,           // where
							null,           // relationship where
							(short)0);      // limit
					mapList.sort("name", "ascending", "string");
					if(mapList != null && mapList.size() > 0) {
						for (int i = 0; i < mapList.size(); i++) {
							Map map = (Map) mapList.get(i);
							String name  = (String)map.get("name");
							String level = (String)map.get("level");
							if(map.get("from[" + relation + "].to.id") == null) {
								hasChild = false;
								isLeaf = true;
							} else {
								hasChild = true;
								isLeaf = true;
							}
							strLeaf = (isLeaf?"TRUE":"FALSE");
							strChild = (hasChild?"TRUE":"FALSE");
							map.put("IS_LEAF", strLeaf);
							map.put("hasChild", strChild);
							expandList.add(map);	
						}
					}
				}
			}else{
				DomainObject nodeObj = new DomainObject(objectId);
				mapList = nodeObj.getRelatedObjects(context, 
						relation, 		// relationship
						type,     		// type
						selectList,     // objects
						null,  			// relationships
						false,          // to
						true,          	// from
						(short)1,       // recurse
						null,           // where
						null,           // relationship where
						(short)0);      // limit
				mapList.sort("name", "ascending", "string");
				if(mapList != null && mapList.size() > 0) {
					for (int i = 0; i < mapList.size(); i++) {
						Map map = (Map) mapList.get(i);
						String name  = (String)map.get("name");
						String level = (String)map.get("level");
						if(map.get("from[" + relation + "].to.id") == null) {
							hasChild = false;
							isLeaf = true;
						} else {
							hasChild = true;
							isLeaf = true;
						}
						strLeaf = (isLeaf?"TRUE":"FALSE");
						strChild = (hasChild?"TRUE":"FALSE");
						map.put("IS_LEAF", strLeaf);
						map.put("hasChild", strChild);
						expandList.add(map);	
					}
				}
			}
	        
    	} catch(Exception e) {
    		e.printStackTrace();
    		throw e;
    	}
    	return expandList;
    }
	
	/**
	 * @desc "Part Library" Chooser View First Node
	 */
	public MapList getPartLibraryFirstNode(Context context, String[] args) throws Exception {
		
		MapList nodeList = new MapList();
		HashMap paramMap = (HashMap) JPO.unpackArgs(args);
		
		String master = (String)paramMap.get("searchMode");
		String refName = (String)paramMap.get("refName");
		String displayKey = (String)paramMap.get("displayKey");
		if(displayKey==null || "".equalsIgnoreCase(displayKey)) displayKey = "name";
		
		String type = "Part Library";
		String relation = "Subclass";
		SelectList selectList = new SelectList(6);
		selectList.addId();
		selectList.addName();
		selectList.addType();
		selectList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_TITLE);
		selectList.add(displayKey);
		selectList.add("from[" + relation + "].to.name");
		selectList.add("from[" + relation + "].to.id");
		
		boolean isLeaf = true, hasChild=false;
        String strLeaf = null, strChild = null;
        MapList mapList = new MapList();
		try {
			MapList rootList = DomainObject.findObjects(context, type, "*", "*", "*", cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION, "", "", true, selectList, (short)0);
			
			if(rootList==null || rootList.size()==0) return new MapList();
			rootList.sort("name", "ascending", "string");
			
			if(rootList != null && rootList.size() > 0) {
				for (int i = 0; i < rootList.size(); i++) {
					Map map = (Map) rootList.get(i);
					String name = (String)map.get("name");
					String display = (String)map.get(displayKey);
					if(map.get("from[" + relation + "].to.id") == null) {
						hasChild = false;
						isLeaf = true;
					} else {
						hasChild = true;
						isLeaf = false;
					}
					strLeaf = (isLeaf?"TRUE":"FALSE");
					strChild = (hasChild?"TRUE":"FALSE");
					map.put("name", name);
					map.put(displayKey, display);
					map.put("IS_LEAF", strLeaf);
					map.put("hasChild", strChild);
					nodeList.add(map);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return nodeList;
    }
	
	/**
	 * @desc "Part Library" Chooser View Expand Node
	 */
	@SuppressWarnings("deprecation")
	public MapList expandPartLibraryLowLankNode(Context context, String[]args) throws Exception {
    	MapList expandList = new MapList();
    	try {
    		HashMap paramMap = (HashMap) JPO.unpackArgs(args);
    		String type = "General Class";
    		String relation = "Subclass";
    		
    		String text = (String)paramMap.get("text");
    		String displayTitle = (String)paramMap.get("displayTitle");
    		
    		SelectList selectList = new SelectList(9);
        	selectList.addId();
    		selectList.addName();
    		selectList.addType();
    		selectList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_TITLE);
    		selectList.add("level");
    		selectList.add("from[" + relation + "].to.name");
    		selectList.add("from[" + relation + "].to.id");
    		selectList.add(displayTitle);
    		selectList.add("from["+DomainConstants.RELATIONSHIP_CLASSIFIED_ITEM+"].to.name");
    		selectList.add("from["+DomainConstants.RELATIONSHIP_CLASSIFIED_ITEM+"].to.id");
    		
    		StringBuffer strBufferExpandWhere = new StringBuffer();
    		strBufferExpandWhere.append(displayTitle);
    		strBufferExpandWhere.append(" ~~ '*");
    		strBufferExpandWhere.append(text);
    		strBufferExpandWhere.append("*'");
			
			String objectId = (String)paramMap.get("objectId");
			String filterType = (String)paramMap.get("filterType");
			String orgId = (String)paramMap.get("orgId");
			
			boolean isLeaf = true, hasChild=false;
	        String strLeaf = null, strChild = null;
			
			MapList mapList = new MapList();
			
			if(objectId == null){
				if(orgId == null){
					
					MapList rootList = DomainObject.findObjects(context, DomainConstants.TYPE_PART_FAMILY, "*", "*", "*", cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION, strBufferExpandWhere.toString(), "", true, selectList, (short)0);
					if(rootList==null || rootList.size()==0) return new MapList();
					rootList.sort("name", "ascending", "string");
					
					if(rootList != null && rootList.size() > 0) {
						for (int i = 0; i < rootList.size(); i++) {
							Map map = (Map) rootList.get(i);
							String name  = (String)map.get("name");
							String level = (String)map.get("level");
							String objType = (String)map.get("type");
							
							if (map.get("from[" + relation + "].to.id") == null ){
								hasChild = false;
								isLeaf = true;
								strLeaf = (isLeaf ? "TRUE":"FALSE");
								strChild = (hasChild ? "TRUE":"FALSE");
								map.put("IS_LEAF", strLeaf);
								map.put("hasChild", strChild);
								expandList.add(map);
							} 
									
						}
					}
				}else{
					DomainObject nodeObj = new DomainObject(orgId);
					mapList = nodeObj.getRelatedObjects(context, 
							"Subclass",                 // relationship
							"Part Family",     	        // type
							selectList,     			// objects
							null,  						// relationships
							false,          			// to
							true,          				// from
							(short)1,       			// recurse
							null,           			// where
							null,           			// relationship where
							(short)0);      			// limit
					mapList.sort("name", "ascending", "string");
					for (int i = 0; i < mapList.size(); i++) {
						Map map = (Map) mapList.get(i);
						String name  = (String)map.get("name");
						String level = (String)map.get("level");
						String objType = (String)map.get("type");
						
						if(map.get("from[" + relation + "].to.id") != null ){
							hasChild = true;
							isLeaf = false;
						} else if (map.get("from[" + relation + "].to.id") == null ){
							hasChild = false;
							isLeaf = true;
						} 
						
						strLeaf = (isLeaf?"TRUE":"FALSE");
						strChild = (hasChild?"TRUE":"FALSE");
						map.put("IS_LEAF", strLeaf);
						map.put("hasChild", strChild);
						expandList.add(map);	
					}
				}
			}else{
				DomainObject nodeObj = new DomainObject(objectId);
				mapList = nodeObj.getRelatedObjects(context, 
						"Subclass",					// relationship
						"Part Family",     			// type
						selectList,     			// objects
						null,  						// relationships
						false,          			// to
						true,          				// from
						(short)1,       			// recurse
						null,           			// where
						null,           			// relationship where
						(short)0);      			// limit
				mapList.sort("name", "ascending", "string");
				for (int i = 0; i < mapList.size(); i++) {
					Map map = (Map) mapList.get(i);
					String name  = (String)map.get("name");
					String level = (String)map.get("level");
					String objType = (String)map.get("type");
					
					if(map.get("from[" + relation + "].to.id") != null ){
						hasChild = true;
						isLeaf = false;
					} else if (map.get("from[" + relation + "].to.id") == null ){
						hasChild = false;
						isLeaf = true;
					} 
					
					strLeaf = (isLeaf?"TRUE":"FALSE");
					strChild = (hasChild?"TRUE":"FALSE");
					map.put("IS_LEAF", strLeaf);
					map.put("hasChild", strChild);
					expandList.add(map);	
				}
			}
	        
    	} catch(Exception e) {
    		e.printStackTrace();
    		throw e;
    	}
    	return expandList;
    }
	
	/**
	 * @desc "Document Library" Chooser View First Node
	 */
	public MapList getDocumentLibraryFirstNode(Context context, String[] args) throws Exception {
		
		MapList nodeList = new MapList();
		HashMap paramMap = (HashMap) JPO.unpackArgs(args);
		
		String master = (String)paramMap.get("searchMode");
		String refName = (String)paramMap.get("refName");
		String displayKey = (String)paramMap.get("displayKey");
		if(displayKey==null || "".equalsIgnoreCase(displayKey)) displayKey = "name";
		
		String type = "Document Library";
		String relation = "Subclass";
		SelectList selectList = new SelectList(6);
		selectList.addId();
		selectList.addName();
		selectList.addType();
		selectList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_TITLE);
		selectList.add(displayKey);
		selectList.add("from[" + relation + "].to.name");
		selectList.add("from[" + relation + "].to.id");
		
		boolean isLeaf = true, hasChild=false;
        String strLeaf = null, strChild = null;
        MapList mapList = new MapList();
		try {
			MapList rootList = DomainObject.findObjects(context, type, "*", "*", "*", cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION, "", "", true, selectList, (short)0);
			
			if(rootList==null || rootList.size()==0) return new MapList();
			rootList.sort("name", "ascending", "string");
			
			if(rootList != null && rootList.size() > 0) {
				for (int i = 0; i < rootList.size(); i++) {
					Map map = (Map) rootList.get(i);
					String name = (String)map.get("name");
					String display = (String)map.get(displayKey);
					if(map.get("from[" + relation + "].to.id") == null) {
						hasChild = false;
						isLeaf = true;
					} else {
						hasChild = true;
						isLeaf = true;
					}
					strLeaf = (isLeaf?"TRUE":"FALSE");
					strChild = (hasChild?"TRUE":"FALSE");
					map.put("name", name);
					map.put(displayKey, display);
					map.put("IS_LEAF", strLeaf);
					map.put("hasChild", strChild);
					nodeList.add(map);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return nodeList;
    }
	
	/**
	 * @desc "Document Library" Chooser View Expand Node
	 */
	@SuppressWarnings("deprecation")
	public MapList expandDocumentLibraryLowLankNode(Context context, String[]args) throws Exception {
    	MapList expandList = new MapList();
    	try {
    		HashMap paramMap = (HashMap) JPO.unpackArgs(args);
    		String type = "General Class";
    		String relation = "Subclass";
    		
    		SelectList selectList = new SelectList(9);
        	selectList.addId();
    		selectList.addName();
    		selectList.addType();
    		selectList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_TITLE);
    		selectList.add("level");
    		selectList.add("from[" + relation + "].to.name");
    		selectList.add("from[" + relation + "].to.id");
    		selectList.add("from[" + DomainConstants.RELATIONSHIP_CLASSIFIED_ITEM + "].to.name");
    		selectList.add("from[" + DomainConstants.RELATIONSHIP_CLASSIFIED_ITEM + "].to.id");
			
			String objectId = (String)paramMap.get("objectId");
			String filterType = (String)paramMap.get("filterType");
			String orgId = (String)paramMap.get("orgId");
			
			boolean isLeaf = true, hasChild=false;
	        String strLeaf = null, strChild = null;
			
			MapList mapList = new MapList();
			
			if(objectId == null){
				if(orgId == null){
					
					MapList rootList = DomainObject.findObjects(context, "Document Family", "*", "*", "*", cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION, "", "", true, selectList, (short)0);
					if(rootList==null || rootList.size()==0) return new MapList();
					rootList.sort("name", "ascending", "string");
					
					if(rootList != null && rootList.size() > 0) {
						for (int i = 0; i < rootList.size(); i++) {
							Map map = (Map) rootList.get(i);
							String name  = (String)map.get("name");
							String level = (String)map.get("level");
							String objType = (String)map.get("type");
							
							if(map.get("from[" + relation + "].to.id") != null && map.get("from[" + DomainConstants.RELATIONSHIP_CLASSIFIED_ITEM + "].to.id") == null){
								hasChild = true;
								isLeaf = true;
							} else if (map.get("from[" + relation + "].to.id") == null && map.get("from[" + DomainConstants.RELATIONSHIP_CLASSIFIED_ITEM + "].to.id") == null){
								hasChild = false;
								isLeaf = true;
							} else if (map.get("from[" + relation + "].to.id") == null && map.get("from[" + DomainConstants.RELATIONSHIP_CLASSIFIED_ITEM + "].to.id") != null){
								hasChild = true;
								isLeaf = true;
							} 
							
							if(DomainConstants.TYPE_DOCUMENT.equals(objType)){
								hasChild = false;
								isLeaf = false;
							}
							
							strLeaf = (isLeaf?"TRUE":"FALSE");
							strChild = (hasChild?"TRUE":"FALSE");
							map.put("IS_LEAF", strLeaf);
							map.put("hasChild", strChild);
							expandList.add(map);		
						}
					}
				}else{
					DomainObject nodeObj = new DomainObject(orgId);
					mapList = nodeObj.getRelatedObjects(context, 
							"Subclass,Classified Item", // relationship
							"Document Family,Document",     	// type
							selectList,     			// objects
							null,  						// relationships
							false,          			// to
							true,          				// from
							(short)1,       			// recurse
							null,           			// where
							null,           			// relationship where
							(short)0);      			// limit
					mapList.sort("name", "ascending", "string");
					for (int i = 0; i < mapList.size(); i++) {
						Map map = (Map) mapList.get(i);
						String name  = (String)map.get("name");
						String level = (String)map.get("level");
						String objType = (String)map.get("type");
						
						if(map.get("from[" + relation + "].to.id") != null && map.get("from[" + DomainConstants.RELATIONSHIP_CLASSIFIED_ITEM + "].to.id") == null){
							hasChild = true;
							isLeaf = true;
						} else if (map.get("from[" + relation + "].to.id") == null && map.get("from[" + DomainConstants.RELATIONSHIP_CLASSIFIED_ITEM + "].to.id") == null){
							hasChild = false;
							isLeaf = true;
						} else if (map.get("from[" + relation + "].to.id") == null && map.get("from[" + DomainConstants.RELATIONSHIP_CLASSIFIED_ITEM + "].to.id") != null){
							hasChild = true;
							isLeaf = true;
						} 
						
						if(DomainConstants.TYPE_DOCUMENT.equals(objType)){
							hasChild = false;
							isLeaf = false;
						}
						
						strLeaf = (isLeaf?"TRUE":"FALSE");
						strChild = (hasChild?"TRUE":"FALSE");
						map.put("IS_LEAF", strLeaf);
						map.put("hasChild", strChild);
						expandList.add(map);	
					}
				}
			}else{
				DomainObject nodeObj = new DomainObject(objectId);
				mapList = nodeObj.getRelatedObjects(context, 
						"Subclass,Classified Item", // relationship
						"Document Family,Document",     	// type
						selectList,     			// objects
						null,  						// relationships
						false,          			// to
						true,          				// from
						(short)1,       			// recurse
						null,           			// where
						null,           			// relationship where
						(short)0);      			// limit
				mapList.sort("name", "ascending", "string");
				for (int i = 0; i < mapList.size(); i++) {
					Map map = (Map) mapList.get(i);
					String name  = (String)map.get("name");
					String level = (String)map.get("level");
					String objType = (String)map.get("type");
					
					if(map.get("from[" + relation + "].to.id") != null && map.get("from[" + DomainConstants.RELATIONSHIP_CLASSIFIED_ITEM + "].to.id") == null){
						hasChild = true;
						isLeaf = true;
					} else if (map.get("from[" + relation + "].to.id") == null && map.get("from[" + DomainConstants.RELATIONSHIP_CLASSIFIED_ITEM + "].to.id") == null){
						hasChild = false;
						isLeaf = true;
					} else if (map.get("from[" + relation + "].to.id") == null && map.get("from[" + DomainConstants.RELATIONSHIP_CLASSIFIED_ITEM + "].to.id") != null){
						hasChild = true;
						isLeaf = true;
					} 
					
					if(DomainConstants.TYPE_DOCUMENT.equals(objType)){
						hasChild = false;
						isLeaf = false;
					}
					
					strLeaf = (isLeaf?"TRUE":"FALSE");
					strChild = (hasChild?"TRUE":"FALSE");
					map.put("IS_LEAF", strLeaf);
					map.put("hasChild", strChild);
					expandList.add(map);	
				}
			}
	        
    	} catch(Exception e) {
    		e.printStackTrace();
    		throw e;
    	}
    	return expandList;
    }
	
	/**
	 * @desc "Common Code" Chooser View First Node
	 */
	public MapList getCommonCodeFirstNode(Context context, String[] args) throws Exception {
		
		MapList nodeList = new MapList();
		HashMap paramMap = (HashMap) JPO.unpackArgs(args);
		
		String refName = (String)paramMap.get("refName");
		String displayTitle = (String)paramMap.get("displayTitle");
		String displayKey = (String)paramMap.get("displayKey");
		String fieldNameDisplay = (String)paramMap.get("fieldNameDisplay");
		if(fieldNameDisplay.contains("_") && !"".equals(fieldNameDisplay)){
			fieldNameDisplay = fieldNameDisplay.replace("_", " ");
		}
		
		if(displayKey==null || "".equalsIgnoreCase(displayKey)) displayKey = "name";
		
		String type = cdmConstantsUtil.TYPE_CDMCOMMONCODE;//cdmCommonCode
		String relation = cdmConstantsUtil.RELATIONSHIP_CDMCOMMONCODERELATIONSHIP;//cdmCommonCodeRelationship
		SelectList selectList = new SelectList(6);
		selectList.addId();
		selectList.addName();
		selectList.addType();
		selectList.add(displayTitle);
		selectList.add(displayKey);
		selectList.add("from[" + relation + "].to.name");
		selectList.add("from[" + relation + "].to.id");
//		if("Product Group Name".equals(fieldNameDisplay)){
//			selectList.add("attribute[cdmCommonTemp2]");
//		}
		boolean isLeaf = true, hasChild=false;
        String strLeaf = null, strChild = null;
        MapList mapList = new MapList();
		try {
			MapList rootList = DomainObject.findObjects(context, 
					type, 
					"*", 
					"-", 
					"*", 
					cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION, 
					cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_COMMON_CODE+"=='"+fieldNameDisplay+"'",
					"",
					true, 
					selectList, 
					(short)0);
			
			if(rootList==null || rootList.size()==0) return new MapList();
			Map rootMap = (Map)rootList.get(0);
			String rootId = (String)rootMap.get("id");
			DomainObject nodeObj = DomainObject.newInstance(context);
			nodeObj.setId(rootId);
			mapList = nodeObj.getRelatedObjects(context, 
					relation, 		// relationship
					type,     		// type
					selectList,     // objects
					null,  			// relationships
					false,          // to
					true,          	// from
					(short)1,       // recurse
					null,           // where
					null,           // relationship where
					(short)0);      // limit
			mapList.sort(displayTitle, "ascending", "string");
			
			if(mapList != null && mapList.size() > 0) {
				for (int i = 0; i < mapList.size(); i++) {
					Map map = (Map) mapList.get(i);
					String name = (String)map.get("name");
					String title = (String)map.get(displayTitle);
					//String temp = (String)map.get("attribute[cdmCommonTemp2]");
					if(map.get("from[" + relation + "].to.id") == null) {
						hasChild = false;
						isLeaf = true;
						
					} else {
						hasChild = true;
						isLeaf = false;
					}
					strLeaf = (isLeaf?"TRUE":"FALSE");
					strChild = (hasChild?"TRUE":"FALSE");
					map.put("name", name);
					//map.put(displayKey, display);
					map.put("IS_LEAF", strLeaf);
					map.put("hasChild", strChild);
					nodeList.add(map);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return nodeList;
    }
	
	/**
	 * @desc "Common Code" Chooser View Expand Node
	 */
	public MapList expandCommonCodeLowLankNode(Context context, String[]args) throws Exception {
    	MapList expandList = new MapList();
    	try {
    		HashMap paramMap = (HashMap) JPO.unpackArgs(args);
    		String type = cdmConstantsUtil.TYPE_CDMCOMMONCODE;//cdmCommonCode
    		String relation = cdmConstantsUtil.RELATIONSHIP_CDMCOMMONCODERELATIONSHIP;//cdmCommonCodeRelationship
    		String text = (String)paramMap.get("text");
    		String displayTitle = (String)paramMap.get("displayTitle");
    		String fieldNameDisplay = (String)paramMap.get("fieldNameDisplay");
    		if("_".contains(fieldNameDisplay) && !"".equals(fieldNameDisplay)){
    			fieldNameDisplay = fieldNameDisplay.replace("_", " ");
    		}
    		
    		SelectList selectList = new SelectList(6);
        	selectList.addId();
    		selectList.addName();
    		selectList.addType();
    		selectList.add(displayTitle);
    		selectList.add("level");
    		selectList.add("from[" + relation + "].to.name");
    		selectList.add("from[" + relation + "].to.id");
    		
    		StringBuffer strBufferExpandWhere = new StringBuffer();
    		strBufferExpandWhere.append(displayTitle);
    		strBufferExpandWhere.append(" ~~ '*");
    		strBufferExpandWhere.append(text);
    		strBufferExpandWhere.append("*'");
    		
    		StringBuffer strBufferFirstWhere = new StringBuffer();
    		strBufferFirstWhere.append(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_COMMON_CODE);
    		strBufferFirstWhere.append("=='");
    		strBufferFirstWhere.append(fieldNameDisplay);
    		strBufferFirstWhere.append("'");
			
			String objectId = (String)paramMap.get("objectId");
			String filterType = (String)paramMap.get("filterType");
			String orgId = (String)paramMap.get("orgId");
			
			boolean isLeaf = true, hasChild=false;
	        String strLeaf = null, strChild = null;
			
			MapList mapList = new MapList();
			if(objectId == null){
				if(orgId == null){
					MapList rootList = DomainObject.findObjects(context, type, "*", "-", "*", cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION, strBufferFirstWhere.toString(), "", true, selectList, (short)0);
					if(rootList==null || rootList.size()==0) return new MapList();
					Map rootMap = (Map)rootList.get(0);
					String rootId = (String)rootMap.get(DomainConstants.SELECT_ID);
					DomainObject nodeObj = DomainObject.newInstance(context);
					nodeObj.setId(rootId);
					mapList = nodeObj.getRelatedObjects(context, 
							relation, 		// relationship
							type,     		// type
							selectList,     // objects
							null,  			// relationships
							false,          // to
							true,          	// from
							(short)0,       // recurse
							null,           // where
							null,           // relationship where
							(short)0);      // limit
					mapList.sort(DomainConstants.SELECT_NAME, "ascending", "string");
					if(mapList != null && mapList.size() > 0) {
						for (int i = 0; i < mapList.size(); i++) {
							Map map = (Map) mapList.get(i);
							String name  = (String)map.get(DomainConstants.SELECT_NAME);
							String level = (String)map.get(DomainConstants.SELECT_LEVEL);
							String title = (String)map.get(displayTitle);
							if(title.contains(text) || title.equals(text)){
								if(map.get("from[" + relation + "].to.id") != null){
									//hasChild = true;
									//isLeaf = false;
								} else {
									hasChild = false;
									isLeaf = true;
									strLeaf = (isLeaf?"TRUE":"FALSE");
									strChild = (hasChild?"TRUE":"FALSE");
									map.put("IS_LEAF", strLeaf);
									map.put("hasChild", strChild);
									expandList.add(map);
								}
							}
						}
					}
				}else{
					DomainObject nodeObj = new DomainObject(orgId);
					mapList = nodeObj.getRelatedObjects(context, 
							relation, 		// relationship
							type,     		// type
							selectList,     // objects
							null,  			// relationships
							false,          // to
							true,          	// from
							(short)1,       // recurse
							null,           // where
							null,           // relationship where
							(short)0);      // limit
					mapList.sort("name", "ascending", "string");
					
					if(mapList != null && mapList.size() > 0) {
						for (int i = 0; i < mapList.size(); i++) {
							Map map = (Map) mapList.get(i);
							String name  = (String)map.get("name");
							String level = (String)map.get("level");
							if(map.get("from[" + relation + "].to.id") == null) {
								hasChild = false;
								isLeaf = true;
							} else {
								hasChild = true;
								isLeaf = true;
							}
							strLeaf = (isLeaf?"TRUE":"FALSE");
							strChild = (hasChild?"TRUE":"FALSE");
							map.put("IS_LEAF", strLeaf);
							map.put("hasChild", strChild);
							expandList.add(map);	
						}
					}
				}
			}else{
				DomainObject nodeObj = new DomainObject(objectId);
				mapList = nodeObj.getRelatedObjects(context, 
						relation, 		// relationship
						type,     		// type
						selectList,     // objects
						null,  			// relationships
						false,          // to
						true,          	// from
						(short)1,       // recurse
						null,           // where
						null,           // relationship where
						(short)0);      // limit
				mapList.sort("name", "ascending", "string");
				
				if(mapList != null && mapList.size() > 0) {
					for (int i = 0; i < mapList.size(); i++) {
						Map map = (Map) mapList.get(i);
						String name  = (String)map.get("name");
						String level = (String)map.get("level");
						if(map.get("from[" + relation + "].to.id") == null) {
							hasChild = false;
							isLeaf = true;
						} else {
							hasChild = true;
							isLeaf = true;
						}
						strLeaf = (isLeaf?"TRUE":"FALSE");
						strChild = (hasChild?"TRUE":"FALSE");
						map.put("IS_LEAF", strLeaf);
						map.put("hasChild", strChild);
						expandList.add(map);	
					}
				}
			}
			expandList.sort(displayTitle, "ascending", "string");
    	} catch(Exception e) {
    		e.printStackTrace();
    		throw e;
    	}
    	return expandList;
    }
	
	/**
	 * @desc "Project" Chooser View First Node
	 */
	public MapList getProjectGroupFirstNode(Context context, String[] args) throws Exception {
		
		MapList nodeList = new MapList();
		HashMap paramMap = (HashMap) JPO.unpackArgs(args);
		
		String refName = (String)paramMap.get("refName");
		String displayKey = (String)paramMap.get("displayKey");
		if(displayKey==null || "".equalsIgnoreCase(displayKey)) displayKey = "name";
		
		String rootName = "Project Tree";
		String type = cdmConstantsUtil.TYPE_CDM_SUB_PROJECT_GROUP;//cdmSubProjectGroup
		String relation = cdmConstantsUtil.RELATIONSHIP_CDM_PROJECT_GROUP_OBJECT_TYPE_RELATIONSHIP;//cdmProjectGroupObjectTypeRelationShip
		SelectList selectList = new SelectList(6);
		selectList.addId();
		selectList.addName();
		selectList.addType();
		selectList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PROJECT_CODE);
		selectList.add(displayKey);
		selectList.add("from[" + relation + "].to.name");
		selectList.add("from[" + relation + "].to.id");
		
		boolean isLeaf = true, hasChild=false;
        String strLeaf = null, strChild = null;
        MapList mapList = new MapList();
		try {
			String strProjectGroupRootId = MqlUtil.mqlCommand(context, "print bus $1 $2 $3 select $4 dump", new String[] {type, rootName, "-", DomainConstants.SELECT_ID});
			DomainObject nodeObj = new DomainObject(strProjectGroupRootId);
			MapList rootList = nodeObj.getRelatedObjects(context, 
					relation, 		// relationship
					type,     		// type
					selectList,     // objects
					null,  			// relationships
					false,          // to
					true,          	// from
					(short)1,       // recurse
					null,           // where
					null,           // relationship where
					(short)0);      // limit
			
			if(rootList==null || rootList.size()==0) return new MapList();
			rootList.sort(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PROJECT_CODE, "ascending", "string");
			
			if(rootList != null && rootList.size() > 0) {
				for (int i = 0; i < rootList.size(); i++) {
					Map map = (Map) rootList.get(i);
					String name = (String)map.get(DomainConstants.SELECT_NAME);
					String display = (String)map.get(displayKey);
					if(map.get("from[" + relation + "].to.id") == null) {
						hasChild = false;
						isLeaf = true;
					} else {
						hasChild = true;
						isLeaf = false;
					}
					strLeaf = (isLeaf?"TRUE":"FALSE");
					strChild = (hasChild?"TRUE":"FALSE");
					map.put(DomainConstants.SELECT_NAME, name);
					map.put(displayKey, display);
					map.put("IS_LEAF", strLeaf);
					map.put("hasChild", strChild);
					nodeList.add(map);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return nodeList;
    }
	
	/**
	 * @desc "Project" Chooser View Expand Node
	 */
	public MapList expandProjectGroupLowLankNode(Context context, String[]args) throws Exception {
    	MapList expandList = new MapList();
    	try {
    		HashMap paramMap = (HashMap) JPO.unpackArgs(args);
    		String type = cdmConstantsUtil.TYPE_CDM_SUB_PROJECT_GROUP;//cdmSubProjectGroup
    		String relation = cdmConstantsUtil.RELATIONSHIP_CDM_PROJECT_GROUP_OBJECT_TYPE_RELATIONSHIP;//cdmProjectGroupObjectTypeRelationShip
    		
    		SelectList selectList = new SelectList(6);
        	selectList.addId();
    		selectList.addName();
    		selectList.addType();
    		selectList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PROJECT_CODE);
    		selectList.add("level");
    		selectList.add("from[" + relation + "].to.name");
    		selectList.add("from[" + relation + "].to.id");
			
			String objectId = (String)paramMap.get("objectId");
			String filterType = (String)paramMap.get("filterType");
			String orgId = (String)paramMap.get("orgId");
			String text = (String)paramMap.get("text");
    		String displayTitle = (String)paramMap.get("displayTitle");
    		
    		StringBuffer strBufferExpandWhere = new StringBuffer();
    		strBufferExpandWhere.append(displayTitle);
    		strBufferExpandWhere.append(" ~~ '*");
    		strBufferExpandWhere.append(text);
    		strBufferExpandWhere.append("*'");
			
			boolean isLeaf = true, hasChild=false;
	        String strLeaf = null, strChild = null;
			MapList mapList = new MapList();
			if(objectId == null){
				if(orgId == null){
					mapList = DomainObject.findObjects(context, "cdmSubProjectGroup,cdmProjectGroupObject", "*", "*", "*", "*", strBufferExpandWhere.toString(), true, selectList); 
					mapList.sort("name", "ascending", "string");
					if(mapList != null && mapList.size() > 0) {
						for (int i = 0; i < mapList.size(); i++) {
							Map map = (Map) mapList.get(i);
							String name  = (String)map.get("name");
							String level = (String)map.get("level");
							if(map.get("from[" + relation + "].to.id") != null){
								//hasChild = true;
								//isLeaf = false;
							} else {
								hasChild = false;
								isLeaf = true;
								strLeaf = (isLeaf?"TRUE":"FALSE");
								strChild = (hasChild?"TRUE":"FALSE");
								map.put("IS_LEAF", strLeaf);
								map.put("hasChild", strChild);
								expandList.add(map);
							}
						}
					}
				}else{
					DomainObject nodeObj = new DomainObject(orgId);
					mapList = nodeObj.getRelatedObjects(context, 
							relation, 		// relationship
							"cdmSubProjectGroup,cdmProjectGroupObject", // type
							selectList,     // objects
							null,  			// relationships
							false,          // to
							true,          	// from
							(short)1,       // recurse
							null,           // where
							null,           // relationship where
							(short)0);      // limit
					mapList.sort("name", "ascending", "string");
					
					if(mapList != null && mapList.size() > 0) {
						for (int i = 0; i < mapList.size(); i++) {
							Map map = (Map) mapList.get(i);
							String name  = (String)map.get("name");
							String level = (String)map.get("level");
							if(map.get("from[" + relation + "].to.id") == null) {
								hasChild = false;
								isLeaf = true;
							} else {
								hasChild = true;
								isLeaf = false;
							}
							strLeaf = (isLeaf?"TRUE":"FALSE");
							strChild = (hasChild?"TRUE":"FALSE");
							map.put("IS_LEAF", strLeaf);
							map.put("hasChild", strChild);
							expandList.add(map);	
						}
					}
				}
			}else{
				DomainObject nodeObj = new DomainObject(objectId);
				mapList = nodeObj.getRelatedObjects(context, 
						relation, 		// relationship
						"cdmSubProjectGroup,cdmProjectGroupObject", // type
						selectList,     // objects
						null,  			// relationships
						false,          // to
						true,          	// from
						(short)1,       // recurse
						null,           // where
						null,           // relationship where
						(short)0);      // limit
				mapList.sort("name", "ascending", "string");
				
				if(mapList != null && mapList.size() > 0) {
					for (int i = 0; i < mapList.size(); i++) {
						Map map = (Map) mapList.get(i);
						String name  = (String)map.get("name");
						String level = (String)map.get("level");
						if(map.get("from[" + relation + "].to.id") == null) {
							hasChild = false;
							isLeaf = true;
						} else {
							hasChild = true;
							isLeaf = false;
						}
						strLeaf = (isLeaf?"TRUE":"FALSE");
						strChild = (hasChild?"TRUE":"FALSE");
						map.put("IS_LEAF", strLeaf);
						map.put("hasChild", strChild);
						expandList.add(map);	
					}
				}
			}
			expandList.sort(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PROJECT_CODE, "ascending", "string");
    	} catch(Exception e) {
    		e.printStackTrace();
    		throw e;
    	}
    	return expandList;
    }
	
	/**
	 * @desc "Work Space" Chooser View First Node
	 */
	public MapList getWorkspaceFirstNode(Context context, String[] args) throws Exception {
		
		MapList nodeList = new MapList();
		HashMap paramMap = (HashMap) JPO.unpackArgs(args);
		
		String refName = (String)paramMap.get("refName");
		String displayKey = (String)paramMap.get("displayKey");
		if(displayKey==null || "".equalsIgnoreCase(displayKey)) displayKey = "name";
		
		String type = "Workspace";
		String relation = "Data Vaults";
		SelectList selectList = new SelectList(6);
		selectList.addId();
		selectList.addName();
		selectList.addType();
		selectList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_TITLE);
		selectList.add(displayKey);
		selectList.add("from[" + relation + "].to.name");
		selectList.add("from[" + relation + "].to.id");
		
		boolean isLeaf = true, hasChild=false;
        String strLeaf = null, strChild = null;
        MapList mapList = new MapList();
		try {
			MapList rootList = DomainObject.findObjects(context, type, "*", "*", "*", cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION, "", "", true, selectList, (short)0);
			
			if(rootList==null || rootList.size()==0) return new MapList();
			rootList.sort("name", "ascending", "string");
			
			if(rootList != null && rootList.size() > 0) {
				for (int i = 0; i < rootList.size(); i++) {
					Map map = (Map) rootList.get(i);
					String name = (String)map.get("name");
					String display = (String)map.get(displayKey);
					if(map.get("from[" + relation + "].to.id") == null) {
						hasChild = false;
						isLeaf = true;
					} else {
						hasChild = true;
						isLeaf = true;
					}
					strLeaf = (isLeaf?"TRUE":"FALSE");
					strChild = (hasChild?"TRUE":"FALSE");
					map.put("name", name);
					map.put(displayKey, display);
					map.put("IS_LEAF", strLeaf);
					map.put("hasChild", strChild);
					nodeList.add(map);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return nodeList;
    }
	
	/**
	 * @desc "Work Space" Chooser View Expand Node
	 */
	public MapList expandWorkspaceLowLankNode(Context context, String[]args) throws Exception {
	    MapList expandList = new MapList();
	    try {
	    	HashMap paramMap = (HashMap) JPO.unpackArgs(args);
	    	String type = "Workspace Vault";
	    	String relation = "Data Vaults";
	    		
	    	SelectList selectList = new SelectList(9);
	        selectList.addId();
	    	selectList.addName();
	    	selectList.addType();
	    	selectList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_TITLE);
	    	selectList.add("level");
	    	selectList.add("from[" + relation + "].to.name");
	    	selectList.add("from[" + relation + "].to.id");
	    	selectList.add("from[" + DomainConstants.RELATIONSHIP_SUB_VAULTS + "].to.name");
	    	selectList.add("from[" + DomainConstants.RELATIONSHIP_SUB_VAULTS + "].to.id");
				
			String objectId = (String)paramMap.get("objectId");
			String filterType = (String)paramMap.get("filterType");
			String orgId = (String)paramMap.get("orgId");
				
			boolean isLeaf = true, hasChild=false;
		    String strLeaf = null, strChild = null;
				
			MapList mapList = new MapList();
				
			if(objectId == null){
				if(orgId == null){
					MapList rootList = DomainObject.findObjects(context, "Workspace,Workspace Vault", "*", "*", "*", cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION, "", "", true, selectList, (short)0);
					if(rootList==null || rootList.size()==0) return new MapList();
					rootList.sort("name", "ascending", "string");
					if(rootList != null && rootList.size() > 0) {
						for (int i = 0; i < rootList.size(); i++) {
							Map map = (Map) rootList.get(i);
							String name  = (String)map.get("name");
							String level = (String)map.get("level");
							String objType = (String)map.get("type");
								
							if(map.get("from[" + relation + "].to.id") != null && map.get("from[" + DomainConstants.RELATIONSHIP_SUB_VAULTS + "].to.id") == null){
								hasChild = true;
								isLeaf = true;
							} else if (map.get("from[" + relation + "].to.id") == null && map.get("from[" + DomainConstants.RELATIONSHIP_SUB_VAULTS + "].to.id") == null){
								hasChild = false;
								isLeaf = true;
							} else if (map.get("from[" + relation + "].to.id") == null && map.get("from[" + DomainConstants.RELATIONSHIP_SUB_VAULTS + "].to.id") != null){
								hasChild = true;
								isLeaf = true;
							} 
								
							if("Document".equals(objType)){
								hasChild = true;
								isLeaf = true;
							}
								
							strLeaf = (isLeaf?"TRUE":"FALSE");
							strChild = (hasChild?"TRUE":"FALSE");
							map.put("IS_LEAF", strLeaf);
							map.put("hasChild", strChild);
							expandList.add(map);		
						}
					}
				}else{
					DomainObject nodeObj = new DomainObject(orgId);
					mapList = nodeObj.getRelatedObjects(context, 
							"Data Vaults,Sub Vaults",   // relationship
							"Workspace Vault",     	    // type
							selectList,     			// objects
							null,  						// relationships
							false,          			// to
							true,          				// from
							(short)1,       			// recurse
							null,           			// where
							null,           			// relationship where
							(short)0);      			// limit
					mapList.sort("name", "ascending", "string");
					for (int i = 0; i < mapList.size(); i++) {
						Map map = (Map) mapList.get(i);
						String name  = (String)map.get("name");
						String level = (String)map.get("level");
						String objType = (String)map.get("type");
							
						if(map.get("from[" + relation + "].to.id") != null && map.get("from[" + DomainConstants.RELATIONSHIP_SUB_VAULTS + "].to.id") == null){
							hasChild = true;
							isLeaf = true;
						} else if (map.get("from[" + relation + "].to.id") == null && map.get("from[" + DomainConstants.RELATIONSHIP_SUB_VAULTS + "].to.id") == null){
							hasChild = false;
							isLeaf = true;
						} else if (map.get("from[" + relation + "].to.id") == null && map.get("from[" + DomainConstants.RELATIONSHIP_SUB_VAULTS + "].to.id") != null){
							hasChild = true;
							isLeaf = true;
						} 
							
						if("Document".equals(objType)){
							hasChild = true;
							isLeaf = true;
						}
							
						strLeaf = (isLeaf?"TRUE":"FALSE");
						strChild = (hasChild?"TRUE":"FALSE");
						map.put("IS_LEAF", strLeaf);
						map.put("hasChild", strChild);
						expandList.add(map);	
					}
				}
			}else{
				DomainObject nodeObj = new DomainObject(objectId);
				mapList = nodeObj.getRelatedObjects(context, 
						"Data Vaults,Sub Vaults",   // relationship
						"Workspace Vault",     	    // type
						selectList,     			// objects
						null,  						// relationships
						false,          			// to
						true,          				// from
						(short)1,       			// recurse
						null,           			// where
						null,           			// relationship where
						(short)0);      			// limit
				mapList.sort("name", "ascending", "string");
				for (int i = 0; i < mapList.size(); i++) {
					Map map = (Map) mapList.get(i);
					String name  = (String)map.get("name");
					String level = (String)map.get("level");
					String objType = (String)map.get("type");
						
					if(map.get("from[" + relation + "].to.id") != null && map.get("from[" + DomainConstants.RELATIONSHIP_SUB_VAULTS + "].to.id") == null){
						hasChild = true;
						isLeaf = true;
					} else if (map.get("from[" + relation + "].to.id") == null && map.get("from[" + DomainConstants.RELATIONSHIP_SUB_VAULTS + "].to.id") == null){
						hasChild = false;
						isLeaf = true;
					} else if (map.get("from[" + relation + "].to.id") == null && map.get("from[" + DomainConstants.RELATIONSHIP_SUB_VAULTS + "].to.id") != null){
						hasChild = true;
						isLeaf = true;
					} 
						
					if("Document".equals(objType)){
						hasChild = false;
						isLeaf = false;
					}
						
					strLeaf = (isLeaf?"TRUE":"FALSE");
					strChild = (hasChild?"TRUE":"FALSE");
					map.put("IS_LEAF", strLeaf);
					map.put("hasChild", strChild);
					expandList.add(map);	
				}
			}
		        
	    } catch(Exception e) {
	    	e.printStackTrace();
	    	throw e;
	    }
	    return expandList;
	}

	/**
	 * @desc "CDM Create Part" Process
	 */
    @SuppressWarnings("unchecked")
    public String createPart(Context context, String[] args) throws Exception{
    	HashMap paramMap = (HashMap) JPO.unpackArgs(args);
    	String strType               = (String)paramMap.get("TypeActual");
    	String strPhase              = (String)paramMap.get("Phase");
    	String strPartNo             = (String)paramMap.get("PartNo");
    	String strPartCode           = (String)paramMap.get("PartCode");
    	String strVehicle            = (String)paramMap.get("Vehicle");
    	String strPartName           = (String)paramMap.get("PartName");
    	String strProject            = (String)paramMap.get("Project");
    	String strApprovalType       = (String)paramMap.get("ApprovalType");
    	String strPartType           = (String)paramMap.get("PartType");
    	String strGlobal             = (String)paramMap.get("Global");
    	String strDrawingNo          = (String)paramMap.get("DrawingNo");
    	String strUnitOfMeasure      = (String)paramMap.get("UnitOfMeasure");
    	String strECONumber          = (String)paramMap.get("ECONumber");
    	String strItemType           = (String)paramMap.get("ItemType");
    	String strOEMItemNumber      = (String)paramMap.get("OEMItemNumber");
    	String strComments 			 = (String)paramMap.get("Comments");
    	String strChangeReason 		 = (String)paramMap.get("ChangeReason");
    	String strOrg1 				 = (String)paramMap.get("Org1");
    	String strOrg2 				 = (String)paramMap.get("Org2");
    	String strOrg3 				 = (String)paramMap.get("Org3");
    	String strProductType 		 = (String)paramMap.get("ProductType");
    	String strERPInterface 		 = (String)paramMap.get("ERPInterface");
    	String strSurface 			 = (String)paramMap.get("Surface");
    	String strEstimatedWeight 	 = (String)paramMap.get("EstimatedWeight");
    	String strEstimatedWeightUOM = (String)paramMap.get("EstimatedWeightUOM");
    	String strMaterial 			 = (String)paramMap.get("Material");
    	String strRealWeight 	     = (String)paramMap.get("RealWeight");
    	String strSize 				 = (String)paramMap.get("Size");
    	String strCADWeight 		 = (String)paramMap.get("CADWeight");
    	String strSurfaceTreatment   = (String)paramMap.get("SurfaceTreatment");
    	String strIsCasting 		 = (String)paramMap.get("IsCasting");
    	String strOption1 			 = (String)paramMap.get("Option1");
    	String strOption2 			 = (String)paramMap.get("Option2");
    	String strOption3 			 = (String)paramMap.get("Option3");
    	String strOption4 			 = (String)paramMap.get("Option4");
    	String strOption5 			 = (String)paramMap.get("Option5");
    	String strOption6 			 = (String)paramMap.get("Option6");
    	String strOptionETC 		 = (String)paramMap.get("OptionETC");
    	String strOptionDescription  = (String)paramMap.get("OptionDescription");
    	String strPublishingTarget 	 = (String)paramMap.get("PublishingTarget");
    	String strInvestor 			 = (String)paramMap.get("Investor");
    	String strProjectType 		 = (String)paramMap.get("ProjectType");
    	String strMode      		 = (String)paramMap.get("mode");
    	String strStandardBOM		 = (String)paramMap.get("StandardBOM");
    	String strNoOfParts     	 = (String)paramMap.get("noOfParts");
    	String strApplyPartList   	 = (String)paramMap.get("ApplyPartList");
    	String strSpecificationYn    = (String)paramMap.get("VariantPart");
    	String KeyInTextPartNo   	 = (String)paramMap.get("KeyInTextPartNo");
    	String strMaterialCoSign   	 = (String)paramMap.get("MaterialCoSign");
    	String strSurfaceTreatmentCoSign  = (String)paramMap.get("SurfaceTreatmentCoSign");
    	
    	String VehicleObjectId 		 = (String)paramMap.get("VehicleObjectId");
    	String ProjectObjectId 		 = (String)paramMap.get("ProjectObjectId");
    	String ProjectTypeObjectId 	 = (String)paramMap.get("ProjectTypeObjectId");
    	String ProductTypeObjectId 	 = (String)paramMap.get("ProductTypeObjectId");
    	String Org1ObjectId 		 = (String)paramMap.get("Org1ObjectId");
    	String Org2ObjectId 		 = (String)paramMap.get("Org2ObjectId");
    	String Org3ObjectId 		 = (String)paramMap.get("Org3ObjectId");
    	String Option1ObjectId 		 = (String)paramMap.get("Option1ObjectId");
    	String Option2ObjectId 		 = (String)paramMap.get("Option2ObjectId");
    	String Option3ObjectId 		 = (String)paramMap.get("Option3ObjectId");
    	String Option4ObjectId 		 = (String)paramMap.get("Option4ObjectId");
    	String Option5ObjectId 		 = (String)paramMap.get("Option5ObjectId");
    	String Option6ObjectId 		 = (String)paramMap.get("Option6ObjectId");
    	String ECONumberOid 		 = (String)paramMap.get("ECONumberOid");
    	String materialOID 		     = (String)paramMap.get("materialOID");
    	String surfaceTreatmentOID   = (String)paramMap.get("surfaceTreatmentOID");
    	String partFamilyObjectId    = (String)paramMap.get("PartOid");
    	String strRevision		     = (String)paramMap.get("Revision");
    	String DrawingNo_Oid 		 = StringUtils.trimToEmpty((String)paramMap.get("DrawingNo_Oid"));
    	
    	
    	String partObjectId = DomainConstants.EMPTY_STRING;
    	HashMap attributes = new HashMap();
    	attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_PHASE             , 	strPhase);                       
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_NAME              ,  strPartName);                    
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_APPROVAL_TYPE     , 	strApprovalType);                
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_TYPE              , 	strPartType);                    
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_GLOBAL            , 	strGlobal);                      
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_UOM               ,  strUnitOfMeasure);               
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_ITEM_TYPE         , 	strItemType);                    
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_OEM_ITEM_NUMBER   , 	strOEMItemNumber);               
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_DESCRIPTION       , 	strComments);                    
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_CHANGE_REASON     , 	strChangeReason);                
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_ERP_INTERFACE     , 	strERPInterface);                
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_SURFACE           , 	strSurface);                     
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_ESTIMATED_WEIGHT  , 	strEstimatedWeight);             
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_MATERIAL          , 	strMaterial);                    
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_REAL_WEIGHT       , 	strRealWeight);                  
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_SIZE              , 	strSize);                        
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_CAD_WEIGHT        , 	strCADWeight);                   
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_SURFACE_TREATMENT , 	strSurfaceTreatment);            
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_IS_CASTING        , 	strIsCasting);                   
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_ETC        , 	strOptionETC);                   
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_DESCRIPTION, 	strOptionDescription);           
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_PUBLISHING_TARGET ,  strPublishingTarget);            
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_INVESTOR          , 	strInvestor);   
        
        if(! DomainConstants.EMPTY_STRING.equals(DrawingNo_Oid)){
        	
        	String strCADObjectDrawingNo = StringUtils.trimToEmpty(new DomainObject(DrawingNo_Oid).getInfo(context, cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_DRAWING_NO));
        	
        	if(! DomainConstants.EMPTY_STRING.equals(strCADObjectDrawingNo)){
        		attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_DRAWING_NO,    strCADObjectDrawingNo);
        	}
        	
        	String strDrawingRelPartType = StringUtils.trimToEmpty(new DomainObject(DrawingNo_Oid).getInfo(context, "to[Part Specification].from.type"));
        	    
        	if( "cdmMechanicalPart".equals(strDrawingRelPartType) ){
        		
        		String strDrawingRelPartRevision = StringUtils.trimToEmpty(new DomainObject(DrawingNo_Oid).getInfo(context, "to[Part Specification].from.revision"));
        		if( ! strRevision.equals(strDrawingRelPartRevision) ){
        			
        			System.out.println("strRevision     " + strRevision);
        			System.out.println("strDrawingRelPartRevision    " + strDrawingRelPartRevision);
        			String strSeriesPartNotRevisionMessage = "Series Part throw Revision.";
            		throw new FrameworkException(strSeriesPartNotRevisionMessage);
        			
        		}
        		
        		String strDrawingRelPartCurrent = StringUtils.trimToEmpty(new DomainObject(DrawingNo_Oid).getInfo(context, "to[Part Specification].from.current"));
        		if( "Release".equals(strDrawingRelPartCurrent) ){
        			
        			System.out.println("strDrawingRelPartCurrent    " + strDrawingRelPartCurrent);
        			String strSeriesPartNotCurrentMessage = "The Seriespart state should not be released.";
            		throw new FrameworkException(strSeriesPartNotCurrentMessage);
            		
        		}
        		
        	}
        	
        }
        
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_STANDARD_BOM , 	 strStandardBOM);
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_ESTIMATED_WEIGHT_UOM ,    strEstimatedWeightUOM);
        
        if( ! DomainConstants.EMPTY_STRING.equals(materialOID) ){
        	
        	if( DomainConstants.EMPTY_STRING.equals(surfaceTreatmentOID) ){
        		attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_PLM_OBJECTID    , 	materialOID + "|none");
        	}else{
        		attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_PLM_OBJECTID    , 	materialOID + "|");
        	}
        	
        }
        
        if( ! DomainConstants.EMPTY_STRING.equals(surfaceTreatmentOID) ){
        	
        	if( DomainConstants.EMPTY_STRING.equals(materialOID) ){
        		attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_PLM_OBJECTID    ,    "none|" + surfaceTreatmentOID);
        	}else{
        		attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_PLM_OBJECTID    , 	materialOID + "|" + surfaceTreatmentOID);
        	}
        	
        }
        
        
        if(DomainConstants.EMPTY_STRING.equals(StringUtils.trimToEmpty(strSpecificationYn))){
        	strSpecificationYn = "N";
        }
        if(DomainConstants.EMPTY_STRING.equals(StringUtils.trimToEmpty(strMaterialCoSign))){
        	strMaterialCoSign = "N";
        }
        if(DomainConstants.EMPTY_STRING.equals(StringUtils.trimToEmpty(strSurfaceTreatmentCoSign))){
        	strSurfaceTreatmentCoSign = "N";
        }
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_PLM_VARIANT_YN    , 	strSpecificationYn);
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_PLM_MATERIAL_CO_SIGN_YN    , 	strMaterialCoSign);
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_PLM_SURFACE_TREATMENT_CO_SIGN_YN    , 	strSurfaceTreatmentCoSign);
        
        if(DomainConstants.EMPTY_STRING.equals(StringUtils.trimToEmpty(strNoOfParts))){
        	strNoOfParts = "1";
        }
        
        String strPartCheck = "false";
        String strSizeCheck = "false";
        String strSurfaceTreatmentCheck = "false";
        if(! DomainConstants.EMPTY_STRING.equals(strApplyPartList)){
        	String[] strApplyPartListArray = strApplyPartList.split(",");
            for(int k=0; k<strApplyPartListArray.length; k++){
            	String strApplyPartListValue = strApplyPartListArray[k];
            	if("Part".equals(strApplyPartListValue)){
            		strPartCheck = "true";
            	}else if("Surface Treatment".equals(strApplyPartListValue)){
            		strSurfaceTreatmentCheck = "true";
            	}else if("Size".equals(strApplyPartListValue)){
            		strSizeCheck = "true";
            	}
            }
	    }
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_APPLY_PART_LIST, strPartCheck);
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_APPLY_SIZE_LIST, strSizeCheck);
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_APPLY_SURFACE_TREATMENT_LIST, strSurfaceTreatmentCheck);
        
        try{
        	ContextUtil.startTransaction(context, true);
        	
        	String strNewPartName = KeyInTextPartNo;
        	
        	if(strNewPartName.length() == 10){
        		
        		String strBlockCode = strNewPartName.substring(0, 5);
        		String strPartFamilyBlockCodeName = strBlockCode + ":" + strPartName;
        		String strMqlValue = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "temp query bus $1 $2 $3 where $4 select $5 dump $6", new String[] {"Part Family", "*", "*", "attribute["+cdmConstantsUtil.ATTRIBUTE_CDM_PART_FAMILY_BLOCK_CODE_NAME+"] == \""+strPartFamilyBlockCodeName+"\" ", DomainConstants.SELECT_ID, "|"}));
        		
        		if(! DomainConstants.EMPTY_STRING.equals(strMqlValue)){
        			
        			String[] strMqlValueArray = strMqlValue.split("\\|");
        			partFamilyObjectId = strMqlValueArray[3];
        			
        		}
        		
    		}
        	
        	StringList slPartNewList = new StringList();
        	int iNoOfParts = Integer.parseInt(strNoOfParts);
        	
        	if(iNoOfParts > 1){
        		
        		partObjectId = createPartObject(context, attributes, strPhase, strType, strNewPartName, iNoOfParts, strRevision);
        		
        		if(! DomainConstants.EMPTY_STRING.equals(partObjectId)){
        			
        			String[] newPartIdArray = partObjectId.split(",");
        			
            		for(int k=0; k<newPartIdArray.length; k++){
            			
            			slPartNewList.add(newPartIdArray[k]);
            			
            		}
            		
        	    }
        		
        	}else{
        		
        		partObjectId = createPartObject(context, attributes, strPhase, strType, strNewPartName, strRevision);	
        		slPartNewList.add(partObjectId);
        		
        	}
        	
        	for(int i=0; i<slPartNewList.size(); i++){
        		
        		String strNewPartId = (String)slPartNewList.get(i);
        		
        		if("AttributeGroup".equals(strMode)){
        			
    	        	String strPartFamilyObjectId = (String)paramMap.get("PartFamilyObjectId");
    	        	StringList slList = getPartFamilyAttributeGroup(context, strPartFamilyObjectId);
    	        	int iListSize = slList.size();
    	        	HashMap hmAttributeGroupValues = new HashMap();
    	        	
    	        	for(int h=0; h<iListSize; h++){
    	        		String strAttr = (String)slList.get(h);
    	        		String strParamValue = (String)paramMap.get(strAttr);
    	        		
    	        		hmAttributeGroupValues.put(strAttr, strParamValue);
    	        	}
    	        	
    	        	setAttributeGroup(context, strNewPartId, hmAttributeGroupValues, strPartFamilyObjectId);
    	        	
        		}else{
        			
        			partObjectRelationShip(context, strNewPartId, VehicleObjectId, ProjectObjectId, ProjectTypeObjectId, ProductTypeObjectId, Org1ObjectId, Org2ObjectId, Org3ObjectId, Option1ObjectId, Option2ObjectId, Option3ObjectId, Option4ObjectId, Option5ObjectId, Option6ObjectId, ECONumberOid, DrawingNo_Oid);
        	        
        	        PartFamily partFamily = (PartFamily)DomainObject.newInstance(context, DomainConstants.TYPE_PART_FAMILY, DomainConstants.ENGINEERING);
            		partFamily.setId(partFamilyObjectId);
    			    partFamily.addPart(context, strNewPartId);	
    			    
        		}
        		
        		///////////////////////////////  PhantomPart Connect MechanicalPart  Start !!!
        		if(! DomainConstants.EMPTY_STRING.equals(DrawingNo_Oid)){
	        		
        			String strDrawingRelPartType = StringUtils.trimToEmpty(new DomainObject(DrawingNo_Oid).getInfo(context, "to[Part Specification].from.type"));
	        	    
	            	if( "cdmPhantomPart".equals(strDrawingRelPartType) ){
	            		
	            		try{
	            			
	            			ContextUtil.pushContext(context, null, null, null);
	            			MqlUtil.mqlCommand(context, "trigger off", new String[]{});
	            			String strDrawingRelPartId = StringUtils.trimToEmpty(new DomainObject(DrawingNo_Oid).getInfo(context, "to[Part Specification].from.id"));
		            		if( ! strDrawingRelPartId.equals(strNewPartId) ){
		            			MqlUtil.mqlCommand(context, "connect bus $1 relationship $2 to $3", new String[]{strDrawingRelPartId, "EBOM", strNewPartId});
		            		}
	            			
	            		}catch(Exception e){
	            			throw new FrameworkException(e.getMessage().toString());
	        			}finally{
	        				MqlUtil.mqlCommand(context, "trigger on", new String[]{});
	        				ContextUtil.popContext(context);
	        			}
	            		
	            	}
	            	
        		}
        		///////////////////////////////  PhantomPart Connect MechanicalPart  End !!!  
    	        
        	}
	        
	        ContextUtil.commitTransaction(context);
        } catch(Exception e) {
        	e.printStackTrace();
        	System.out.println("Message().toString()        "+e.getMessage().toString());
        	//ContextUtil.abortTransaction(context);
        	throw new FrameworkException(e.getMessage().toString());
        }
    	return partObjectId;
    }
    
    @SuppressWarnings("unchecked")
    private String createPartObject(Context context, HashMap attributes, String strPhase, String strType, String strPartNo, int iNoOfParts, String strRevision) throws Exception{
    	DomainObject partObj = new DomainObject();
    	StringBuffer strBuffer = new StringBuffer();
    	SqlSession sqlSession = null;
    	try{
    		
    		SqlSessionUtil.reNew("plm");
    	    sqlSession = SqlSessionUtil.getSqlSession();
    	    
    	    Map paramMap = new HashMap();
    	    if(strPartNo.length() == 10){
    	    	paramMap.put("BLOCKCODE", strPartNo.substring(0, 5));
    		}else{
    			paramMap.put("BLOCKCODE", strPartNo);
    		}
    	    
    	    StringBuffer strOptionBuffer = new StringBuffer();
    	    
    	    List<Map<String, String>> optionsList = sqlSession.selectList("getPartBlockCodeOptionsMap", paramMap);
    	    
    	    for(int k=0; k<optionsList.size(); k++){
    	    	Map map = (Map)optionsList.get(k);
    	    	String strLabelName = (String)map.get("LABELNAME");
    	    	strOptionBuffer.append(strLabelName);
    	    	strOptionBuffer.append("|");
    	    }
    	    attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_LABEL_NAME,    strOptionBuffer.toString());
    		
    			
    		StringBuffer strUrl = new StringBuffer();
        	String strSpecificationYn = (String)attributes.get(cdmConstantsUtil.ATTRIBUTE_CDM_PART_PLM_VARIANT_YN);
        	StringBuffer strParamBuffer = new StringBuffer();
        	boolean isArrayData = false;
        		
        	if(strPartNo.length() == 5){
        		strUrl.append(cdmPropertiesUtil.getPropValue(propertyFile, plmPartsUrl));
        		strParamBuffer.append("uid");
        		strParamBuffer.append("=");
        		strParamBuffer.append(context.getUser());
        		strParamBuffer.append("&");
        		strParamBuffer.append("blockCode");
        		strParamBuffer.append("=");
        		strParamBuffer.append(strPartNo);//Block Code
        		strParamBuffer.append("&");
        		strParamBuffer.append("count");
        		strParamBuffer.append("=");
        		strParamBuffer.append(String.valueOf(iNoOfParts));//Number of Part
        		strParamBuffer.append("&");
        		strParamBuffer.append("serialYn");
        		strParamBuffer.append("=");
        		strParamBuffer.append("N");
        		strParamBuffer.append("&");
        		strParamBuffer.append("specificationYn");
        		strParamBuffer.append("=");
        		strParamBuffer.append(strSpecificationYn);
        			
        		isArrayData = true;
        			
        	}else if(strPartNo.length() == 10){
        		strUrl.append(cdmPropertiesUtil.getPropValue(propertyFile, plmPartUrl));
        		strParamBuffer.append("uid");
        		strParamBuffer.append("=");
        		strParamBuffer.append(context.getUser());
        		strParamBuffer.append("&");
        		strParamBuffer.append("partNumber");
        		strParamBuffer.append("=");
        		strParamBuffer.append(strPartNo);//Part Number
        			
        	}
        		
        	String strPLMPartURL = strUrl.append(strParamBuffer.toString()).toString();
        	String strJsonObject = null;
        		
        	// to alert PLM Error. added by ci.lee 
        	try {
        		strJsonObject = cdmJsonDataUtil.getJSON(strPLMPartURL);
					
        	} catch (Exception e) {
    			String strErrorMessage = EnoviaResourceBundle.getProperty(context,"emxEngineeringCentralStringResource",context.getLocale(),"emxEngineeringCentral.Alert.CannotGetPartNoFromPLM");
    			throw new FrameworkException(strErrorMessage);
    		} finally {
    			sqlSession.close();
    		}
        		
        	String strResultInfo = cdmJsonDataUtil.getJSONResultData(strJsonObject, "RESULT" );
        	String strPartFullName = DomainConstants.EMPTY_STRING;
        		
        	if("SUCCESS".equals(strResultInfo)){
        		if(isArrayData){
        			for(int i=0; i<iNoOfParts; i++){
        					
        				strPartFullName = cdmJsonDataUtil.getJSONResultArrayData(strJsonObject, "DATA", i);
        					
        					
        				String strMqlValue = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "temp query bus $1 $2 $3 where $4 select $5 dump $6", new String[] {cdmConstantsUtil.TYPE_CATDrawing, "*", "*", "attribute["+cdmConstantsUtil.ATTRIBUTE_CDM_DRAWING_NO+"] == \""+strPartNo+"\" ", DomainConstants.SELECT_ID, "|"}));
        	    			
        	    		if(cdmConstantsUtil.TEXT_PROTO.equals(strPhase)){
        	    				
        	    			if(DomainConstants.EMPTY_STRING.equals(strRevision)){
        	    	    		strRevision = "01";
        	    	    	}
        	    				
        	    	        partObj.createObject(context, strType, strPartFullName, strRevision, cdmConstantsUtil.POLICY_CDM_PART_POLICY, cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION);
        	    	            
        	    	        if(! DomainConstants.EMPTY_STRING.equals(strMqlValue)){
        	    	    		attributes.remove(cdmConstantsUtil.ATTRIBUTE_CDM_PART_DRAWING_NO);
        	    	    	}
        	    	        	
        	    	    	partObj.setAttributeValues(context, attributes);
        	    	        	
        	    	        strBuffer.append(partObj.getObjectId(context));
        	    	        strBuffer.append(",");
        	    	            
        	    	    }else if(cdmConstantsUtil.TEXT_PRODUCTION.equals(strPhase)){
        	    	        	
        	    	        if(DomainConstants.EMPTY_STRING.equals(strRevision)){
        	    	    		strRevision = "A";
        	    	    	}
        	    	        	
        	    	        partObj.createObject(context, strType, strPartFullName, strRevision, cdmConstantsUtil.POLICY_CDM_PART_POLICY, cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION);

        	    	        if(! DomainConstants.EMPTY_STRING.equals(strMqlValue)){
        	    	    		attributes.remove(cdmConstantsUtil.ATTRIBUTE_CDM_PART_DRAWING_NO);
        	    	    	}
        	    	        	
        	    	    	partObj.setAttributeValues(context, attributes);
        	    	        	
        	    	        strBuffer.append(partObj.getObjectId(context));
        	    	        strBuffer.append(",");
        	    	            
        	    	    }
        					
        			}
        		}else{
        			strPartFullName = cdmJsonDataUtil.getJSONResultData(strJsonObject, "PARTNUMBER" );
        		}
    		    System.out.println("strPartFullName     :     "+strPartFullName);
    		}
        		
        		
        	if("FAIL".equals(strResultInfo)){
        		throw new FrameworkException (cdmJsonDataUtil.getJSONResultData(strJsonObject, "ERRMSG"));
    			//return cdmJsonDataUtil.getJSONResultData(strJsonObject, "ERRMSG");
        	}
    		
        	
    	}catch (Exception e){
    		e.printStackTrace();
    		throw new FrameworkException(e.toString());
    	}finally {
			sqlSession.close();
		}
    	return strBuffer.toString();
	}

    /**
	 * @desc "CDM Create Part" AttributeGroup Create 
	 */
	private void setAttributeGroup(Context context, String partObjectId, HashMap attributeGroupValues, String partFamilyObjectId) throws Exception {
		try{
			MQLCommand mqlCommand = new MQLCommand();
			mqlCommand.open(context);
			try{
				PartFamily partFamily = (PartFamily)DomainObject.newInstance(context, DomainConstants.TYPE_PART_FAMILY, DomainConstants.ENGINEERING);
				partFamily.setId(partFamilyObjectId);
			    mqlCommand.executeCommand(context, "set transaction savepoint $1", "PartFamily");
			    partFamily.addPart(context, partObjectId);
			    
			    DomainObject partObj = new DomainObject(partObjectId);
				partObj.setAttributeValues(context, attributeGroupValues);
			}catch(Exception e){
			    mqlCommand.executeCommand(context, "abort transaction $1","PartFamily");
			    throw e;
			}
			
		}catch(Exception e){
			throw e;
		}
	}

	/**
	 * @desc "CDM Create Part"  Part Family Attribute Group  
	 */
	private StringList getPartFamilyAttributeGroup(Context context, String strPartFamilyObjectId) throws Exception {
		StringList slList = new StringList();
		try{
			if(UIUtil.isNotNullAndNotEmpty(strPartFamilyObjectId)){
	        	
	        	DomainObject parentObj = new DomainObject(strPartFamilyObjectId);
	        	String strPartFamilyName = parentObj.getInfo(context, DomainConstants.SELECT_NAME);
	            String strSysInterface = parentObj.getAttributeValue(context, DomainConstants.ATTRIBUTE_MXSYSINTERFACE);
	
	        	String strMQL             = "print interface $1 select derived dump";
	            String resStr             = MqlUtil.mqlCommand(context, strMQL, true, strSysInterface);
	        	StringList slResultList   = FrameworkUtil.split(resStr, ",");
	            String attributeGroupName = (String)slResultList.get(0);   
	            String mqlValue = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 $2 $3 select $4 dump", new String[] {"cdmAttributeGroupObject", attributeGroupName, "-", "attribute[cdmAttributeGroupSequence]"}));
	            
	            if(! DomainConstants.EMPTY_STRING.equals(mqlValue)){
	            	String[] mqlValueArray = mqlValue.split("\\|");
		        	for(int k=0; k<mqlValueArray.length; k++){
		        		Map map = new HashMap();
		        		String[] resultArray = mqlValueArray[k].split(",");
		        		String attrName     = resultArray[0];
		        		slList.add(attrName);
		        	}
	            }
	            
	        }
		}catch(Exception e){
			throw e;
		}
		return slList;
	}

	/**
     * 
     * Description : JPO getPartObjectRealationship 
     * @param context
     * @param args
     * @throws Exception
     */
    public boolean checkPartObjectRelationShip(Context context,String args[])throws Exception{

    	boolean check = true;
    	try{
    		HashMap paramHm = new HashMap();
    		paramHm = JPO.unpackArgs(args);
    		String  partObjectId     	= (String)paramHm.get("partObjectId");
    		String  VehicleObjectId  	= (String)paramHm.get("VehicleObjectId");
    		String  ProjectObjectId  	= (String)paramHm.get("ProjectObjectId");
    		String  ProjectTypeObjectId = (String)paramHm.get("ProjectTypeObjectId");
    		String  ProductTypeObjectId = (String)paramHm.get("ProductTypeObjectId");
    		String  Org1ObjectId        = (String)paramHm.get("Org1ObjectId");
    		String  Org2ObjectId        = (String)paramHm.get("Org2ObjectId");
    		String  Org3ObjectId        = (String)paramHm.get("Org3ObjectId");
    		String  Option1ObjectId     = (String)paramHm.get("Option1ObjectId");
    		String  Option2ObjectId     = (String)paramHm.get("Option2ObjectId");
    		String  Option3ObjectId     = (String)paramHm.get("Option3ObjectId");
    		String  Option4ObjectId     = (String)paramHm.get("Option4ObjectId");
    		String  Option5ObjectId     = (String)paramHm.get("Option5ObjectId");
    		String  Option6ObjectId     = (String)paramHm.get("Option6ObjectId");
    		String  ECONumberOid        = (String)paramHm.get("ECONumberOid");
    		String  DrawingNo_Oid       = (String)paramHm.get("DrawingNo_Oid");
    		
    		partObjectRelationShip(context, partObjectId, VehicleObjectId, ProjectObjectId, ProjectTypeObjectId, ProductTypeObjectId, Org1ObjectId, Org2ObjectId, Org3ObjectId, Option1ObjectId, Option2ObjectId, Option3ObjectId, Option4ObjectId, Option5ObjectId, Option6ObjectId, ECONumberOid, DrawingNo_Oid);
    	}catch(Exception e){
    		check = false;
    		e.printStackTrace();
    	}finally{
    		return check;
    		
    	}
    
    }
    
    /**
	 * @desc "CDM Create Part" or "Edit Part" RelationShip
	 */
    private void partObjectRelationShip(Context context, String partObjectId, String vehicleObjectId,
    		String projectObjectId, String projectTypeObjectId, String productTypeObjectId, String org1ObjectId, 
			String org2ObjectId, String org3ObjectId, String option1ObjectId, String option2ObjectId,
			String option3ObjectId, String option4ObjectId, String option5ObjectId, String option6ObjectId, String ecId, String drawingId) throws Exception{
    	try{
    		DomainObject partObj = new DomainObject(partObjectId);
    		if(UIUtil.isNotNullAndNotEmpty(vehicleObjectId)){
    			StringList relVehicleIdList = partObj.getInfoList(context, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_VEHICLE+"].id");
    			if(relVehicleIdList.size() > 0 && ! "".equals(relVehicleIdList)){
    				for(int k=0; k<relVehicleIdList.size(); k++){
    					DomainRelationship.disconnect(context, relVehicleIdList.get(k).toString());
    				}
    			}
    		    if(vehicleObjectId.contains(",")){
    				String[] objectIdArray = vehicleObjectId.split(",");
    				StringBuffer strBuffer = new StringBuffer();
    				for(int i=0; i<objectIdArray.length; i++){
    					String strVehicleId = objectIdArray[i];
    					DomainRelationship.connect(context, new DomainObject(strVehicleId), cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_VEHICLE, partObj);
    				}
    		    }else{
    			    if(!"".equals(vehicleObjectId)){
    					DomainRelationship.connect(context, new DomainObject(vehicleObjectId), cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_VEHICLE, partObj);
    				}
    		    }
    		}
    		
    		if(UIUtil.isNotNullAndNotEmpty(projectObjectId)){
    			createPartConnectDomainRelationship(context, projectObjectId, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PROJECT, partObj);
    		}
    		
        	if(UIUtil.isNotNullAndNotEmpty(projectTypeObjectId)){
    			createPartConnectDomainRelationship(context, projectTypeObjectId, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PROJECT_TYPE, partObj);
        	}
        	
        	if(UIUtil.isNotNullAndNotEmpty(productTypeObjectId)){
    			createPartConnectDomainRelationship(context, productTypeObjectId, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PRODUCT_TYPE, partObj);
        	}
        	
        	if(UIUtil.isNotNullAndNotEmpty(ecId)){
        		try{
        			ContextUtil.pushContext(context, null, null, null);
        			MqlUtil.mqlCommand(context, "trigger off", new String[]{});
        			createPartConnectDomainRelationship(context, ecId, DomainConstants.RELATIONSHIP_AFFECTED_ITEM, partObj);
        		}catch(Exception e){
    				throw e;
    			}finally{
    				MqlUtil.mqlCommand(context, "trigger on", new String[]{});
    				ContextUtil.popContext(context);
    			}
        	}
        	
        	System.out.println("drawingId     "+drawingId);
        	if(UIUtil.isNotNullAndNotEmpty(drawingId)){
        		
    			try{
        				
        			DomainObject drawingObj = new DomainObject(drawingId);
        			String strDrawingType = drawingObj.getInfo(context, DomainConstants.SELECT_TYPE);
        			String strDrawingName = drawingObj.getAttributeValue(context, cdmConstantsUtil.ATTRIBUTE_CDM_DRAWING_NO);
        				
        			String strPartName = partObj.getInfo(context, DomainConstants.SELECT_NAME);
        			String relDrawingId = partObj.getInfo(context, "from["+DomainConstants.RELATIONSHIP_PART_SPECIFICATION+"].id");
        			
        			ContextUtil.pushContext(context, null, null, null);
        			MqlUtil.mqlCommand(context, "trigger off", new String[]{});
        			
        			
//        			if(strPartName.equals(strDrawingName)){
//        				
//        				if(cdmConstantsUtil.TYPE_CATDrawing.equals(strDrawingType) || cdmConstantsUtil.TYPE_CDMAUTOCAD.equals(strDrawingType) || cdmConstantsUtil.TYPE_CDMNXDRAWING.equals(strDrawingType)){
//            				
//                			if(relDrawingId != null && !"".equals(relDrawingId)){
//                				DomainRelationship.disconnect(context, relDrawingId);
//                			}	
//                			
//                			DomainRelationship.connect(context, partObj, DomainConstants.RELATIONSHIP_PART_SPECIFICATION, drawingObj );
//            			}
//        				
//        			}else{
//        					
//        				if(relDrawingId != null && !"".equals(relDrawingId)){
//            				DomainRelationship.disconnect(context, relDrawingId);
//            			}			
//        														
//        			}
        			
        			System.out.println("!!!!!!!!!!strDrawingName     " + strDrawingName);
        			partObj.setAttributeValue(context, cdmConstantsUtil.ATTRIBUTE_CDM_PART_DRAWING_NO, strDrawingName );	
        			
    				
    			}catch(Exception e){
    				throw e;
    			}finally{
    				MqlUtil.mqlCommand(context, "trigger on", new String[]{});
    				ContextUtil.popContext(context);
    			}
        	}
    		
    		if(UIUtil.isNotNullAndNotEmpty(org1ObjectId)){
    			createPartDisConnectDomainRelationship(context, partObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_ORG, cdmConstantsUtil.ATTRIBUTE_CDM_PART_ORG_REL_ATTRIBUTE, "Org1");
    			createPartConnectDomainRelationship(context, org1ObjectId, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_ORG, partObj, cdmConstantsUtil.ATTRIBUTE_CDM_PART_ORG_REL_ATTRIBUTE, "Org1");
    		}
    		
    		if(UIUtil.isNotNullAndNotEmpty(org2ObjectId)){
    			createPartDisConnectDomainRelationship(context, partObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_ORG, cdmConstantsUtil.ATTRIBUTE_CDM_PART_ORG_REL_ATTRIBUTE, "Org2");
    			createPartConnectDomainRelationship(context, org2ObjectId, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_ORG, partObj, cdmConstantsUtil.ATTRIBUTE_CDM_PART_ORG_REL_ATTRIBUTE, "Org2");
    		}
        	if(UIUtil.isNotNullAndNotEmpty(org3ObjectId)){
        		createPartDisConnectDomainRelationship(context, partObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_ORG, cdmConstantsUtil.ATTRIBUTE_CDM_PART_ORG_REL_ATTRIBUTE, "Org3");
    			createPartConnectDomainRelationship(context, org3ObjectId, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_ORG, partObj, cdmConstantsUtil.ATTRIBUTE_CDM_PART_ORG_REL_ATTRIBUTE, "Org3");
        	}
        	
        	if(UIUtil.isNotNullAndNotEmpty(option1ObjectId)){
        		createPartDisConnectDomainRelationship(context, partObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option1");
    			createPartConnectDomainRelationship(context, option1ObjectId, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, partObj, cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option1");
        	}
        	if(UIUtil.isNotNullAndNotEmpty(option2ObjectId)){
        		createPartDisConnectDomainRelationship(context, partObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option2");
    			createPartConnectDomainRelationship(context, option2ObjectId, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, partObj, cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option2");
        	}
        	if(UIUtil.isNotNullAndNotEmpty(option3ObjectId)){
        		createPartDisConnectDomainRelationship(context, partObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option3");
    			createPartConnectDomainRelationship(context, option3ObjectId, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, partObj, cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option3");
        	}
        	if(UIUtil.isNotNullAndNotEmpty(option4ObjectId)){
        		createPartDisConnectDomainRelationship(context, partObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option4");
    			createPartConnectDomainRelationship(context, option4ObjectId, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, partObj, cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option4");
        	}
        	if(UIUtil.isNotNullAndNotEmpty(option5ObjectId)){
        		createPartDisConnectDomainRelationship(context, partObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option5");
    			createPartConnectDomainRelationship(context, option5ObjectId, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, partObj, cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option5");
        	}
        	if(UIUtil.isNotNullAndNotEmpty(option6ObjectId)){
        		createPartDisConnectDomainRelationship(context, partObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option6");
    			createPartConnectDomainRelationship(context, option6ObjectId, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, partObj, cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option6");
        	}
        	
    	}catch(Exception e){
    		e.printStackTrace();
    	}
	}

    /**
	 * @desc  RelationShip DisConnect,Connect Process
	 */
	private void createPartConnectDomainRelationship(Context context, String selectId, String strRel, DomainObject partObj) throws Exception{
		try{
			String relId = partObj.getInfo(context, "to["+strRel+"].id");
			if(relId != null && !"".equals(relId)){
				DomainRelationship.disconnect(context, relId);
			}
			DomainRelationship.connect(context, new DomainObject(selectId), strRel, partObj);
		}catch(Exception e){
			throw e;
		}
	}

	/**
	 * @desc  RelationShip SetAttribute Process
	 */
	private void createPartConnectDomainRelationship(Context context, String commonId, String strRel, DomainObject partObj,  String strRelAttr, String strRelAttrValue) throws Exception {
		try{
			DomainRelationship domObjRelWithBusUnit = DomainRelationship.connect(context, new DomainObject(commonId), strRel, partObj);
			domObjRelWithBusUnit.setAttributeValue(context, strRelAttr, strRelAttrValue);
		}catch(Exception e){
			throw e;
		}
	}

	/**
	 * @desc  RelationShip DisConnect Process
	 */
	private void createPartDisConnectDomainRelationship(Context context, DomainObject partObj, String strRel, String strRelAttr, String strRelAttrValue) throws Exception {
		try{
			String relId = partObj.getInfo(context, "to["+strRel+"].id");
		    String relValue = partObj.getInfo(context, "to["+strRel+"].attribute["+strRelAttr+"].value");
		    
			if(relId != null && !"".equals(relId) && strRelAttrValue.equals(relValue)){
				DomainRelationship.disconnect(context, relId);
			}
		}catch(Exception e){
			throw e;
		}
	}

	/**
	 * @desc  "Part Revise"
	 */
	@SuppressWarnings("unchecked")
    public String revisePart(Context context, String[] args) throws Exception{
    	HashMap paramMap = (HashMap) JPO.unpackArgs(args);
    	String strRevisePartId = "";
    	try{
    		ContextUtil.startTransaction(context, true);
    		String strPartId         = (String) paramMap.get("objectId");
    		String strCustomRevision = (String) paramMap.get("Revision");
    		String strPartNo         = (String) paramMap.get("PartNo");
    		String strPartName       = (String) paramMap.get("PartName");
    		String strPhase          = (String) paramMap.get("Phase");
    		String strECobjectId     = (String) paramMap.get("EC_Oid");
    		String strHighLankType   = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump $3", new String[] {strPartId, "to["+DomainConstants.RELATIONSHIP_EBOM+"].from.type", "|"}));
    		
    		//String strEOType = new DomainObject(strECobjectId).getInfo(context, "type");
    		
            strRevisePartId = revise(context, strPartId, strCustomRevision, strPhase, DomainConstants.EMPTY_STRING);
            revisePartSetAttributeValueAndConnection(context, strPartId, strECobjectId, strRevisePartId);
            
            DomainObject domObj = new DomainObject(strPartId);
            String strType = domObj.getInfo(context, "type");
            
            
            if( cdmConstantsUtil.TYPE_CDMMECHANICALPART.equals(strType) && ! strHighLankType.contains(cdmConstantsUtil.TYPE_CDMPHANTOMPART) ){
            	
            	String strDrawingNo = StringUtils.trimToEmpty(domObj.getInfo(context, cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO));
				String strRevision  = domObj.getInfo(context, DomainConstants.SELECT_REVISION);
            	System.out.println("Revise Series Part Drawing No     :     "+strDrawingNo);
        		
        		if(! DomainConstants.EMPTY_STRING.equals(strDrawingNo)){
        			
            		SelectList selectList = new SelectList(3);
            		selectList.addId();
            		selectList.addType();
            		selectList.addName();
            		
            		StringBuffer strBufferWhere = new StringBuffer();
            		strBufferWhere.append(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO);
            		strBufferWhere.append(" == ");
            		strBufferWhere.append(strDrawingNo);
            		strBufferWhere.append(" && ");
    				strBufferWhere.append(DomainConstants.SELECT_REVISION);
    				strBufferWhere.append(" == ");
    				strBufferWhere.append(strRevision);
    				strBufferWhere.append(" && ");
    				strBufferWhere.append(DomainConstants.SELECT_ID);
    				strBufferWhere.append(" != ");
    				strBufferWhere.append(strPartId);
    				strBufferWhere.append(" && ");
    				strBufferWhere.append("revision");
    				strBufferWhere.append(" == ");
    				strBufferWhere.append("last.revision");
            		
            		MapList mlSeriesPartList = DomainObject.findObjects(context, 
            					cdmConstantsUtil.TYPE_CDMMECHANICALPART,        // type
            					DomainConstants.QUERY_WILDCARD,   				// name
            					DomainConstants.QUERY_WILDCARD,   				// revision
            					DomainConstants.QUERY_WILDCARD,   				// policy
            					cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION,     // vault
            					strBufferWhere.toString(),             			// where
            					DomainConstants.EMPTY_STRING,     				// query
            					true,							  				// expand
            					selectList,                      				// objects
            					(short)0);                        				// limits
            		
            		System.out.println("strBufferWhere       "+strBufferWhere.toString());
            		System.out.println("mlSeriesPartList     "+mlSeriesPartList);
            		
	        		
		        	for(int i=0; i<mlSeriesPartList.size(); i++){
		        		
		        		Map map = (Map)mlSeriesPartList.get(i);
		        		String id = (String)map.get(DomainConstants.SELECT_ID);
		        		
		        		try{
		        			
		        			String strSeriesRevisePartId = revise(context, id, strCustomRevision, strPhase, DomainConstants.EMPTY_STRING);
		        			
		        			if(!"".equals(strECobjectId)){
		        				revisePartSetAttributeValueAndConnection(context, id, strECobjectId, strSeriesRevisePartId);
		        	        }
		        			
		        		}catch(Exception e){
		        			e.printStackTrace();
		        		}
		        		
		        	}
		        	
        		}
        		
            }
            
            ContextUtil.commitTransaction(context);
    	}catch(Exception e){
    		ContextUtil.abortTransaction(context);
    		throw e;
    	}
    	return strRevisePartId;
    }
	
	public static void revisePartSetAttributeValueAndConnection(Context context, String strPartId, String strECobjectId, String strRevisePartId) throws Exception{
		try{
			if(!"".equals(strECobjectId)){
				ContextUtil.pushContext(context, null, null, null);
				MqlUtil.mqlCommand(context, "trigger off", new String[]{});
				//new DomainObject(strPartId).setAttributeValue(context, "Is Version Object", "True");
	        	DomainRelationship.connect(context, new DomainObject(strECobjectId), DomainConstants.RELATIONSHIP_AFFECTED_ITEM, new DomainObject(strRevisePartId));
	        }
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			MqlUtil.mqlCommand(context, "trigger on", new String[]{});
			ContextUtil.popContext(context);
		}
	}

	public static String revise(Context context, String strPartObjectId, String strCustomRevision, String strPhase, String strparentOID) throws Exception{
		DomainObject revisePartObj = new DomainObject();
		try{
			String strUser = context.getUser();
//			ContextUtil.pushContext(context, null, null, null);
//			MqlUtil.mqlCommand(context, "trigger off", new String[]{});
			Part part = new Part(strPartObjectId);
			System.out.println("Part ObjectId     =     " + strPartObjectId);
			String strFindNumber = StringUtils.trimToEmpty(part.getInfo(context, "to["+DomainConstants.RELATIONSHIP_EBOM+"]."+DomainConstants.SELECT_ATTRIBUTE_FIND_NUMBER));
			
			BusinessObject lastRev = part.getLastRevision(context);
			strCustomRevision = strCustomRevision.toUpperCase();
			BusinessObject newbo = lastRev.revise(context, strCustomRevision, lastRev.getVault());
        	revisePartObj = new DomainObject(newbo.getObjectId());
        	revisePartObj.setAttributeValue(context, cdmConstantsUtil.ATTRIBUTE_CDM_PART_PHASE, strPhase);
        	System.out.println("revisePartObj     =     " + revisePartObj);
        	
        	// added by ci.lee 
        	//get related drawing objectIds from previous part
    		DomainObject doOriginPart = new DomainObject(strPartObjectId);
    		StringList slDrawingIds = doOriginPart.getInfoList(context, "from["+DomainConstants.RELATIONSHIP_PART_SPECIFICATION+"].to.id");
    		
    		
            // connect drawing to new revision.
    		StringList busSelect = new StringList();
    		busSelect.add(DomainObject.SELECT_TYPE);
    		busSelect.add("last.id");
    		busSelect.add("last.current");
    		
            for (Iterator iterator = slDrawingIds.iterator(); iterator.hasNext();) {
				String strDrawingId = (String) iterator.next();

				DomainObject doDrawing = new DomainObject(strDrawingId);
				Map mapDrw = doDrawing.getInfo(context, busSelect);
				
				String strDrawingType = (String) mapDrw.get(DomainObject.SELECT_TYPE);
				
				// connect only Auto CAD or NX Drawing. 
				if ("cdmAutoCAD".equals(strDrawingType) || "cdmNXDrawing".equals(strDrawingType)) {

					String strLastId = (String) mapDrw.get("last.id");
					String strLastCurrent = (String) mapDrw.get("last.current");
	
					
					DomainRelationship.connect(context, revisePartObj, DomainConstants.RELATIONSHIP_PART_SPECIFICATION, new DomainObject(strLastId));
					
					//"PRIVATE"
//					if("PRIVATE".equals(strLastCurrent)) {
//					} else {
//						DomainRelationship.connect(context, revisePartObj, DomainConstants.RELATIONSHIP_PART_SPECIFICATION, doDrawing);
//						
//					}
					
				}
			}

            
        	
        	String strPartMasterObjectId = StringUtils.trimToEmpty(part.getInfo(context, "to[Part Revision].from.id"));
        	if(! DomainConstants.EMPTY_STRING.equals(strPartMasterObjectId)){
        		MqlUtil.mqlCommand(context, "connect bus $1 relationship $2 to $3", new String[]{strPartMasterObjectId, "Part Revision", revisePartObj.getObjectId(context)});
        	}
			
			if(! DomainConstants.EMPTY_STRING.equals((strparentOID))){
				
				DomainObject parentObj = new DomainObject(strparentOID);	
				String strHighLankPartCurrent = parentObj.getInfo(context, DomainConstants.SELECT_CURRENT);
				//String strIsVersionObject = parentObj.getInfo(context, "attribute[Is Version Object]");
				String strIsVersionObject = StringUtils.trimToEmpty( parentObj.getInfo(context, "next.revision") );
				
				if(DomainConstants.STATE_PART_RELEASE.equals(strHighLankPartCurrent) && "".equals(strIsVersionObject)){
					revisePartStructure(context, strparentOID, revisePartObj, strFindNumber);
				}else if(DomainConstants.STATE_PART_PRELIMINARY.equals(strHighLankPartCurrent) && "".equals(strIsVersionObject)){
					revisePartStructure(context, strparentOID, revisePartObj, strFindNumber, strPartObjectId);
				}
				
			}else{
				
				String strHighLankPartIds = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump $3", new String[] {strPartObjectId, "to["+DomainConstants.RELATIONSHIP_EBOM+"].from.id", "|"}));
				System.out.println("HighLank PartIds     =     "+strHighLankPartIds);
				
				if(! DomainConstants.EMPTY_STRING.equals(strHighLankPartIds)){
					if(strHighLankPartIds.contains("|")){
	        			String[] strHighLankPartIdArray = strHighLankPartIds.split("\\|");
						for(int k=0; k<strHighLankPartIdArray.length; k++){
							String strHighLankPartId = strHighLankPartIdArray[k];
							DomainObject domObj = new DomainObject(strHighLankPartId);	
							String strHighLankPartCurrent = domObj.getInfo(context, DomainConstants.SELECT_CURRENT);
							//String strIsVersionObject = domObj.getInfo(context, "attribute[Is Version Object]");
							String strIsVersionObject = StringUtils.trimToEmpty( domObj.getInfo(context, "next.revision") );
							
							if(DomainConstants.STATE_PART_RELEASE.equals(strHighLankPartCurrent) && "".equals(strIsVersionObject)){
								revisePartStructure(context, strHighLankPartIdArray[k], revisePartObj, strFindNumber);
							}else if(DomainConstants.STATE_PART_PRELIMINARY.equals(strHighLankPartCurrent) && "".equals(strIsVersionObject)){
								revisePartStructure(context, strHighLankPartIdArray[k], revisePartObj, strFindNumber, strPartObjectId);
							}
							
						}
	        		}else{
	        			
	        			DomainObject domObj = new DomainObject(strHighLankPartIds);	
						String strHighLankPartCurrent = domObj.getInfo(context, DomainConstants.SELECT_CURRENT);
						//String strIsVersionObject = domObj.getInfo(context, "attribute[Is Version Object]");
						String strIsVersionObject = StringUtils.trimToEmpty( domObj.getInfo(context, "next.revision"));
						
						if(DomainConstants.STATE_PART_RELEASE.equals(strHighLankPartCurrent) && "".equals(strIsVersionObject)){
							revisePartStructure(context, strHighLankPartIds, revisePartObj, strFindNumber);
						}else if(DomainConstants.STATE_PART_PRELIMINARY.equals(strHighLankPartCurrent) && "".equals(strIsVersionObject)){
							revisePartStructure(context, strHighLankPartIds, revisePartObj, strFindNumber, strPartObjectId);
						}
	        		}
				}else{
					revisePartStructure(context, strHighLankPartIds, revisePartObj, strFindNumber);
				}
				
			}
        	
        	StringList slRowLankPartList = revisePartObj.getInfoList(context, "from["+DomainConstants.RELATIONSHIP_EBOM+"].to.id");
        	if(slRowLankPartList.size() > 0){
        		for(int i=0; i<slRowLankPartList.size(); i++){
            		String strRowLankPartId = (String)slRowLankPartList.get(i);
            		DomainObject dObj = new DomainObject(strRowLankPartId);
            		String strRowLankPartCurrent = dObj.getInfo(context, DomainConstants.SELECT_CURRENT);
            		//String strIsVersionObject = dObj.getInfo(context, "attribute[Is Version Object]");
            		String strIsVersionObject = StringUtils.trimToEmpty( dObj.getInfo(context, "next.revision") );
            		
            		if(DomainConstants.STATE_PART_PRELIMINARY.equals(strRowLankPartCurrent)){
            			BusinessObject strPreviousRev = dObj.getPreviousRevision(context);
            			if(! "..".equals(strPreviousRev.toString())){
            				if(slRowLankPartList.contains(strPreviousRev.getObjectId(context))){
            					MqlUtil.mqlCommand(context, "disconnect bus $1 rel $2 to $3", new String[] {revisePartObj.getObjectId(context), DomainConstants.RELATIONSHIP_EBOM, strPreviousRev.getObjectId(context)});	
            				}
            			}
            		}
            	}	
        	}
        	
        	StringList slPrevPartRowLankList = new DomainObject(strPartObjectId).getInfoList(context, "from["+DomainConstants.RELATIONSHIP_EBOM+"].to.id");
        	System.out.println("###slPrevPartRowLankList         "+slPrevPartRowLankList);
        	if(slPrevPartRowLankList.size() > 0){
        		for(int i=0; i<slPrevPartRowLankList.size(); i++){
            		String strPrevRowLankPartId = (String)slPrevPartRowLankList.get(i);
            		DomainObject prevPartObj = new DomainObject(strPrevRowLankPartId);
            		String strPrevRowLankPartCurrent = prevPartObj.getInfo(context, DomainConstants.SELECT_CURRENT);
            		//String strPrevIsVersionObject = prevPartObj.getInfo(context, "attribute[Is Version Object]");
            		String strPrevIsVersionObject = StringUtils.trimToEmpty( prevPartObj.getInfo(context, "next.revision") );
            		
            		if(DomainConstants.STATE_PART_PRELIMINARY.equals(strPrevRowLankPartCurrent) && "".equals(strPrevIsVersionObject)){
            			MqlUtil.mqlCommand(context, "disconnect bus $1 rel $2 to $3", new String[] {strPartObjectId, DomainConstants.RELATIONSHIP_EBOM, strPrevRowLankPartId});	
            		}
            	}	
        	}
        	
        	MqlUtil.mqlCommand(context, "mod bus $1 $2 $3", new String[] {revisePartObj.getObjectId(context), DomainConstants.SELECT_OWNER, strUser});
		}catch(Exception e){
			throw e;
		}finally{
//			MqlUtil.mqlCommand(context, "trigger on", new String[]{});
//			ContextUtil.popContext(context);
		}
		System.out.println("revisePartObjId     :::     "+revisePartObj.getObjectId(context));
		return revisePartObj.getObjectId(context);
	}
	
	
	public static String revisePartParentStructure (Context context, String strRevisePartId, String strPartObjectId, String strparentOID) throws Exception{
		DomainObject revisePartObj = new DomainObject(strRevisePartId);
		try{
			String strUser = context.getUser();
			ContextUtil.pushContext(context, null, null, null);
			MqlUtil.mqlCommand(context, "trigger off", new String[]{});
			Part part = new Part(strPartObjectId);
			System.out.println("Part ObjectId      =    "+strPartObjectId);
			String strFindNumber = StringUtils.trimToEmpty(part.getInfo(context, "to["+DomainConstants.RELATIONSHIP_EBOM+"]."+DomainConstants.SELECT_ATTRIBUTE_FIND_NUMBER));
			
			if(! DomainConstants.EMPTY_STRING.equals((strparentOID))){
				
				DomainObject parentObj = new DomainObject(strparentOID);	
				String strHighLankPartCurrent = parentObj.getInfo(context, DomainConstants.SELECT_CURRENT);
				//String strIsVersionObject = parentObj.getInfo(context, "attribute[Is Version Object]");
				String strIsVersionObject = StringUtils.trimToEmpty( parentObj.getInfo(context, "next.revision") );
				
				if(DomainConstants.STATE_PART_RELEASE.equals(strHighLankPartCurrent) && "".equals(strIsVersionObject)){
					revisePartStructure(context, strparentOID, revisePartObj, strFindNumber);
				}else if(DomainConstants.STATE_PART_PRELIMINARY.equals(strHighLankPartCurrent) && "".equals(strIsVersionObject)){
					revisePartStructure(context, strparentOID, revisePartObj, strFindNumber, strPartObjectId);
				}
				
			}else{
				
				String strHighLankPartIds = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump $3", new String[] {strPartObjectId, "to["+DomainConstants.RELATIONSHIP_EBOM+"].from.id", "|"}));
				System.out.println("HighLank PartIds     =     "+strHighLankPartIds);
				
				if(! DomainConstants.EMPTY_STRING.equals(strHighLankPartIds)){
					if(strHighLankPartIds.contains("|")){
	        			String[] strHighLankPartIdArray = strHighLankPartIds.split("\\|");
						for(int k=0; k<strHighLankPartIdArray.length; k++){
							String strHighLankPartId = strHighLankPartIdArray[k];
							DomainObject domObj = new DomainObject(strHighLankPartId);	
							String strHighLankPartCurrent = domObj.getInfo(context, DomainConstants.SELECT_CURRENT);
							//String strIsVersionObject = domObj.getInfo(context, "attribute[Is Version Object]");
							String strIsVersionObject = StringUtils.trimToEmpty( domObj.getInfo(context, "next.revision") );
							
							if(DomainConstants.STATE_PART_RELEASE.equals(strHighLankPartCurrent) && "".equals(strIsVersionObject)){
								revisePartStructure(context, strHighLankPartIdArray[k], revisePartObj, strFindNumber);
							}else if(DomainConstants.STATE_PART_PRELIMINARY.equals(strHighLankPartCurrent) && "".equals(strIsVersionObject)){
								revisePartStructure(context, strHighLankPartIdArray[k], revisePartObj, strFindNumber, strPartObjectId);
							}
							
						}
	        		}else{
	        			
	        			DomainObject domObj = new DomainObject(strHighLankPartIds);	
						String strHighLankPartCurrent = domObj.getInfo(context, DomainConstants.SELECT_CURRENT);
						//String strIsVersionObject = domObj.getInfo(context, "attribute[Is Version Object]");
						String strIsVersionObject = StringUtils.trimToEmpty( domObj.getInfo(context, "next.revision") );
						
						if(DomainConstants.STATE_PART_RELEASE.equals(strHighLankPartCurrent) && "".equals(strIsVersionObject)){
							revisePartStructure(context, strHighLankPartIds, revisePartObj, strFindNumber);
						}else if(DomainConstants.STATE_PART_PRELIMINARY.equals(strHighLankPartCurrent) && "".equals(strIsVersionObject)){
							revisePartStructure(context, strHighLankPartIds, revisePartObj, strFindNumber, strPartObjectId);
						}
	        		}
					
				}else{
					revisePartStructure(context, strHighLankPartIds, revisePartObj, strFindNumber);
				}
				
			}
        	
        	StringList slRowLankPartList = revisePartObj.getInfoList(context, "from["+DomainConstants.RELATIONSHIP_EBOM+"].to.id");
        	
        	if(slRowLankPartList.size() > 0){
        		
        		for(int i=0; i<slRowLankPartList.size(); i++){
        			
            		String strRowLankPartId = (String)slRowLankPartList.get(i);
            		DomainObject dObj = new DomainObject(strRowLankPartId);
            		String strRowLankPartCurrent = dObj.getInfo(context, DomainConstants.SELECT_CURRENT);
            		//String strIsVersionObject = dObj.getInfo(context, "attribute[Is Version Object]");
            		String strIsVersionObject = StringUtils.trimToEmpty( dObj.getInfo(context, "next.revision") );
            		
            		if(DomainConstants.STATE_PART_PRELIMINARY.equals(strRowLankPartCurrent)){
            			
            			BusinessObject strPreviousRev = dObj.getPreviousRevision(context);
            			
            			if(! "..".equals(strPreviousRev.toString())){
            				
            				if(slRowLankPartList.contains(strPreviousRev.getObjectId(context))){
            					MqlUtil.mqlCommand(context, "disconnect bus $1 rel $2 to $3", new String[] {revisePartObj.getObjectId(context), DomainConstants.RELATIONSHIP_EBOM, strPreviousRev.getObjectId(context)});	
            				}
            				
            			}
            		}
            	}	
        	}
        	
        	StringList slPrevPartRowLankList = new DomainObject(strPartObjectId).getInfoList(context, "from["+DomainConstants.RELATIONSHIP_EBOM+"].to.id");
        	
        	if(slPrevPartRowLankList.size() > 0){
        		
        		for(int i=0; i<slPrevPartRowLankList.size(); i++){
        			
            		String strPrevRowLankPartId = (String)slPrevPartRowLankList.get(i);
            		DomainObject prevPartObj = new DomainObject(strPrevRowLankPartId);
            		String strPrevRowLankPartCurrent = prevPartObj.getInfo(context, DomainConstants.SELECT_CURRENT);
            		//String strPrevIsVersionObject = prevPartObj.getInfo(context, "attribute[Is Version Object]");
            		String strPrevIsVersionObject = StringUtils.trimToEmpty( prevPartObj.getInfo(context, "next.revision") );
            		
            		if(DomainConstants.STATE_PART_PRELIMINARY.equals(strPrevRowLankPartCurrent) && "".equals(strPrevIsVersionObject)){
            			MqlUtil.mqlCommand(context, "disconnect bus $1 rel $2 to $3", new String[] {strPartObjectId, DomainConstants.RELATIONSHIP_EBOM, strPrevRowLankPartId});	
            		}
            		
            	}	
        	}
        	
        	MqlUtil.mqlCommand(context, "mod bus $1 $2 $3", new String[] {revisePartObj.getObjectId(context), DomainConstants.SELECT_OWNER, strUser});
		}catch(Exception e){
			throw e;
		}finally{
			MqlUtil.mqlCommand(context, "trigger on", new String[]{});
			ContextUtil.popContext(context);
		}
		System.out.println("revisePartObjId     :::     "+revisePartObj.getObjectId(context));
		return revisePartObj.getObjectId(context);
	}
	
	
	public static String revise(Context context, String strPartObjectId, String strParentOID) throws Exception{
		
		String strPartPhase = DomainConstants.EMPTY_STRING;
		String strNextRevision = DomainConstants.EMPTY_STRING;
		
		try{
			
			Part part = new Part(strPartObjectId);
			strPartPhase = part.getAttributeValue(context, cdmConstantsUtil.ATTRIBUTE_CDM_PART_PHASE);
			String strPartDescription 	= part.getDescription(context);
			String strPartRevision 		= part.getInfo(context, DomainConstants.SELECT_REVISION);
			String strPartType 			= part.getInfo(context, DomainConstants.SELECT_TYPE);
			
			
			if(strPartDescription.contains("CHANGE_EAR")){
				
				strNextRevision = "A";
				
			}else{
			
				//////// MechanicalPart Revision Sequence Z -> ZA
				if("Z".equals(strPartRevision) && "cdmMechanicalPart".equals(strPartType)){
					strNextRevision = "ZA";
				}else{
				//////// MechanicalPart Revision Sequence Z -> AA	
					BusinessObject partlastRevObj = part.getLastRevision(context);
					strNextRevision = partlastRevObj.getNextSequence(context);
				}
				
			}
			
			
		}catch(Exception e){
			e.printStackTrace();
		}
		return revise(context, strPartObjectId, strNextRevision, strPartPhase, strParentOID);
	}
	
	
	public static String revise(Context context, String strPartObjectId, String strParentOID, String strModeType) throws Exception{
		
		String strPartPhase = DomainConstants.EMPTY_STRING;
		String strNextRevision = DomainConstants.EMPTY_STRING;
		
		try{
			
			Part part = new Part(strPartObjectId);
			strPartPhase = part.getAttributeValue(context, cdmConstantsUtil.ATTRIBUTE_CDM_PART_PHASE);
			String strPartDescription 	= part.getDescription(context);
			String strPartRevision 		= part.getInfo(context, DomainConstants.SELECT_REVISION);
			String strPartType 			= part.getInfo(context, DomainConstants.SELECT_TYPE);
			
			
			if(strPartDescription.contains("CHANGE_EAR")){
				
				if("EAR".equals(strModeType)){
					strNextRevision = "A";
				}else{
					//////// MechanicalPart Revision Sequence Z -> ZA
					if("Z".equals(strPartRevision) && "cdmMechanicalPart".equals(strPartType)){
						strNextRevision = "ZA";
					}else{
					//////// MechanicalPart Revision Sequence Z -> AA	
						BusinessObject partlastRevObj = part.getLastRevision(context);
						strNextRevision = partlastRevObj.getNextSequence(context);
					}
				}
				
			}else{
			
				//////// MechanicalPart Revision Sequence Z -> ZA
				if("Z".equals(strPartRevision) && "cdmMechanicalPart".equals(strPartType)){
					strNextRevision = "ZA";
				}else{
				//////// MechanicalPart Revision Sequence Z -> AA	
					BusinessObject partlastRevObj = part.getLastRevision(context);
					strNextRevision = partlastRevObj.getNextSequence(context);
				}
				
			}
			
			
		}catch(Exception e){
			e.printStackTrace();
		}
		return revise(context, strPartObjectId, strNextRevision, strPartPhase, strParentOID);
	}
	

	private static void revisePartStructure(Context context, String strHighLankPartId, DomainObject revisePartObj, String strFindNumber) throws Exception {
		try{
			if(! DomainConstants.EMPTY_STRING.equals(strHighLankPartId)){
				
				DomainRelationship EBOMNewRel = DomainRelationship.connect(context, new DomainObject(strHighLankPartId), DomainConstants.RELATIONSHIP_EBOM, revisePartObj);
				if("".equals(strFindNumber)){
					strFindNumber = "1";
				}
				EBOMNewRel.setAttributeValue(context, DomainConstants.ATTRIBUTE_FIND_NUMBER, strFindNumber);
	        }
		}catch(Exception e){
			throw e;
		}
	}
	
	private static void revisePartStructure(Context context, String strHighLankPartId, DomainObject revisePartObj, String strFindNumber, String strPartObjectId) throws Exception {
		try{
			if(! DomainConstants.EMPTY_STRING.equals(strHighLankPartId)){
				
				DomainRelationship EBOMNewRel = DomainRelationship.connect(context, new DomainObject(strHighLankPartId), DomainConstants.RELATIONSHIP_EBOM, revisePartObj);
				if("".equals(strFindNumber)){
					strFindNumber = "1";
				}
				EBOMNewRel.setAttributeValue(context, DomainConstants.ATTRIBUTE_FIND_NUMBER, strFindNumber);
					
				DomainObject domObj = new DomainObject(strPartObjectId);
				StringList slRelEBOMList = domObj.getInfoList(context, "to["+DomainConstants.RELATIONSHIP_EBOM+"].from.id");
				
				if(slRelEBOMList.size() > 0){
					for(int k=0; k<slRelEBOMList.size(); k++){
						String strHighPartObjectId = (String)slRelEBOMList.get(k);
						//String strIsVersionObject = new DomainObject(strHighPartObjectId).getInfo(context, "attribute[Is Version Object]");
						String strIsVersionObject = StringUtils.trimToEmpty( new DomainObject(strHighPartObjectId).getInfo(context, "next.revision") );
						
						if("".equals(strIsVersionObject)){
							MqlUtil.mqlCommand(context, "disconnect bus $1 rel $2 to $3", new String[] {strHighPartObjectId, DomainConstants.RELATIONSHIP_EBOM, strPartObjectId});
						}
					}
				}
				
	        }
		}catch(Exception e){
			throw e;
		}
	}
	
	
	public StringList accessForEditPartQuantityColumn (Context context, String[] args) throws Exception {
    	StringList accessList = new StringList();
long time0	= System.currentTimeMillis();
    	
    	try {
    		Map programMap      = (Map)JPO.unpackArgs(args);
			MapList objectList  = (MapList)programMap.get("objectList");
			
			Iterator it = objectList.iterator();
/*************************************************
*	Start for performance 20170306
*************************************************/
			int iPoint = -1;
/*************************************************
*	End for performance 20170306
*************************************************/
			while(it.hasNext()){
				
				Map map = (Map)it.next();
				String strLevel  = (String)map.get("level");
				
				if(! "0".equals(strLevel)){
					String strPartId = (String)map.get("id");
					String strParentPartId = StringUtils.trimToEmpty((String)map.get("id[parent]"));
					
					//String strHighLankPartId = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump $3", new String[] {strPartId, "to["+DomainConstants.RELATIONSHIP_EBOM+"].from.id", "|"}));
					
					if(! DomainConstants.EMPTY_STRING.equals(strParentPartId)){
/*************************************************
*	Start for performance 20170306
*************************************************/
//						DomainObject domParentObj = new DomainObject(strParentPartId);
//						String strParentCurrent = domParentObj.getInfo(context, DomainConstants.SELECT_CURRENT);
						String strParentCurrent = getParentCurrent(objectList, strParentPartId, iPoint);
/*************************************************
*	End for performance 20170306
*************************************************/
						if(DomainConstants.STATE_PART_PRELIMINARY.equals(strParentCurrent)){
							accessList.add("true");
						}else{
							accessList.add("false");
						}
					}else{
						accessList.add("false");	
					}
					
				}else{
					accessList.add("false");
				}
				
/*************************************************
*	Start for performance 20170306
*************************************************/
				iPoint++;
/*************************************************
*	End for performance 20170306
*************************************************/
			}
			
    	}catch(Exception e){
    		e.printStackTrace();
    	}
long time1	= System.currentTimeMillis();
System.out.println(">>>accessForEditPartQuantityColumn time1="+(time1-time0)+"("+(time1-time0)+")");
    	return accessList;
	}

/*************************************************
*	Start for performance 20170306
*************************************************/
	public String getParentCurrent(MapList objectList, String strParentId, int iPoint)throws Exception{
		String strCurrent = "";
		for ( int i = iPoint; i >=0 ; i--){
			Map map	= (Map)objectList.get(i);
			String strObjectId 	= (String)map.get(DomainConstants.SELECT_ID);
			if ( strObjectId.equals(strParentId) ){
				strCurrent 	= (String)map.get(DomainConstants.SELECT_CURRENT);
				break;
			}
		}
		return strCurrent;
	}
/*************************************************
*	End for performance 20170306
*************************************************/
	
	public StringList accessForEditPart(Context context, String[] args)throws Exception{
    	StringList accessList = new StringList();
long time0	= System.currentTimeMillis();
    	
    	try {
    		Map programMap      = (Map)JPO.unpackArgs(args);
			MapList objectList  = (MapList)programMap.get("objectList");
			
			Iterator it = objectList.iterator();
			while(it.hasNext()){
				
				Map map = (Map)it.next();
				String strLevel  = (String)map.get("level");
				
				if(! "0".equals(strLevel)){
					String strPartId = (String)map.get("id");
					//String strHighLankPartId = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump $3", new String[] {strPartId, "to["+DomainConstants.RELATIONSHIP_EBOM+"].from.id", "|"}));
					
					if(! DomainConstants.EMPTY_STRING.equals(strPartId)){
/*************************************************
*	Start for performance 20170306
*************************************************/
//						DomainObject domObj = new DomainObject(strPartId);
//						String strCurrent = domObj.getInfo(context, DomainConstants.SELECT_CURRENT);
						String strCurrent = (String)map.get(DomainConstants.SELECT_CURRENT);
/*************************************************
*	End for performance 20170306
*************************************************/
						
						if(DomainConstants.STATE_PART_PRELIMINARY.equals(strCurrent)){
							accessList.add("true");
						}else{
							accessList.add("false");
						}
					}else{
						accessList.add("false");	
					}
					
				}else{
					accessList.add("false");
				}
				
			}
			
			
    	}catch(Exception e){
    		e.printStackTrace();
    	}
long time1	= System.currentTimeMillis();
System.out.println(">>>accessForEditPart time1="+(time1-time0)+"("+(time1-time0)+")");
    	return accessList;
	}
	
	public Boolean hasOptionPartAccess(Context context, String[] args) throws Exception {
	
		HashMap programMap = (HashMap)JPO.unpackArgs(args);
		boolean isShowColumn = true;
		SqlSession sqlSession = null;
		try{
			
			String strObjectId  = (String)programMap.get("objectId");
	        DomainObject domObj = new DomainObject(strObjectId);
	        String strPartName  = domObj.getInfo(context, DomainConstants.SELECT_NAME);
	        String strCurrent   = domObj.getInfo(context, DomainConstants.SELECT_CURRENT);
	        
	        if(! DomainConstants.STATE_PART_PRELIMINARY.equals(strCurrent)){
	        	isShowColumn = false;
	        }
	        
	        
	        SqlSessionUtil.reNew("plm");
	        sqlSession = SqlSessionUtil.getSqlSession();
	        Map paramMap = new HashMap();
	        paramMap.put("BLOCKCODE", strPartName.substring(0, 5));
	        List<Map<String, String>> optionsList = sqlSession.selectList("getPartBlockCodeOptionsMap", paramMap);
	        
	        if(optionsList.size() == 0){
	        	isShowColumn = false;
	        }
	        
	        return isShowColumn;
	        
		}catch(Exception e){
			e.printStackTrace();
			throw e;
		}finally {
			sqlSession.close();
		}
				
		
	}

	
	
//	public boolean hasReviseAccess(Context context, String[] args) throws Exception{
//		boolean isAccess = true;
//		
//		HashMap paramMap = (HashMap)JPO.unpackArgs(args);
//        String objectId  = (String) paramMap.get("objectId");
//        if(UIUtil.isNotNullAndNotEmpty(objectId)) {
//   		    
//   		    DomainObject domObj = DomainObject.newInstance(context, objectId);
//   		    String policyClass = domObj.getInfo(context, "policy.property[PolicyClassification].value");
//   		    String isVersionObject = domObj.getInfo(context, "attribute[Is Version Object]");
//   		    String phase = domObj.getInfo(context, cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PHASE);
//   		    String eoType = StringUtils.trimToEmpty(domObj.getInfo(context, "to["+DomainConstants.RELATIONSHIP_AFFECTED_ITEM+"].from.type"));
//   		    String current = domObj.getInfo(context, DomainConstants.SELECT_CURRENT);
//   		    
//   		    if(! "Production".equalsIgnoreCase(policyClass) || "True".equals(isVersionObject) || DomainConstants.EMPTY_STRING.equals(eoType) || ! DomainConstants.STATE_PART_RELEASE.equals(current)) { 
//   		    	isAccess = false;
//   		    }
//   		    
//   		    if(! DomainConstants.EMPTY_STRING.equals(eoType)){
//   		    	if("Production".equals(phase)){
//   	   		    	if(cdmConstantsUtil.TYPE_CDMPEO.equals(eoType)){
//   	   		    		isAccess = false;
//   	   		    	}
//   	   		    }else if("Proto".equals(phase)){
//   	   		    	if(!cdmConstantsUtil.TYPE_CDMPEO.equals(eoType)){
//   	   		    		isAccess = false;
//   	   		    	}
//   	   		    }
//   		    }
//   		    
//   		    String strType = domObj.getInfo(context, DomainConstants.SELECT_TYPE);	
//   		    if(cdmConstantsUtil.TYPE_CDM_ELECTRONIC_PART.equals(strType)){
//   		    	isAccess = false;	
//   		    }
//   		    
//        }
//		return Boolean.valueOf(isAccess);
//	}
	
	
	/**
	 * added by ci.lee on 12/13/2016
	 * checks if command for revise part can be displayed on part properties .
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public boolean hasReviseAccess(Context context, String[] args) throws Exception{
		boolean isAccess = false;
		
		try {
			
			HashMap paramMap = (HashMap)JPO.unpackArgs(args);
	        String objectId  = (String) paramMap.get("objectId");
	        if(UIUtil.isNotNullAndNotEmpty(objectId)) {
	   		    
	        	StringList busSelect = new StringList();
	        	busSelect.add(DomainObject.SELECT_CURRENT);
	        	busSelect.add(DomainObject.SELECT_LAST_ID);
	        	
	   		    DomainObject domObj = DomainObject.newInstance(context, objectId);
	   		    Map objMap = domObj.getInfo(context, busSelect);
	   		    
	   		    String strCurrent = (String) objMap.get(DomainObject.SELECT_CURRENT);
	   		    String strLastId = (String) objMap.get(DomainObject.SELECT_LAST_ID);
	   		    
	   		    boolean isLast = objectId.equals(strLastId);
	   		    
	   		    if( DomainConstants.STATE_PART_RELEASE.equals(strCurrent) && isLast ){
	   		    	isAccess = true;
	   		    }
	   		    
//	   		    String strOwner  = new DomainObject(objectId).getInfo(context, DomainConstants.SELECT_OWNER);
//	   		    String strUser = context.getUser();
//	   		    if(! strOwner.equals(strUser)){
//	   		    	isAccess = false;
//	   		    }

	        }
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
   		    
		return isAccess;
	}
    
	/**
	 * @desc  "Clone Part"
	 */
    @SuppressWarnings("unchecked")
    public String clonePart(Context context, String[] args) throws Exception{
    	HashMap paramMap = (HashMap) JPO.unpackArgs(args);
    	String clonePartObjectId = "";
    	try{
    		clonePartObjectId = clonePart(context, paramMap);
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    	return clonePartObjectId;
    }
    
    private String clonePart(Context context, HashMap paramMap) throws Exception{
    	String clonePartObjectId = "";
    	StringBuffer strBuffer = new StringBuffer();
    	SqlSession sqlSession = null;
    	try{
    		ContextUtil.startTransaction(context, true);
    		String strPartId = (String) paramMap.get("objectId");
    		
    		StringList localStringList = new StringList();
    		localStringList.add("policy");
    		localStringList.add("vault");

    		DomainObject localDomainObject = DomainObject.newInstance(context, strPartId);
    		Map localMap = localDomainObject.getInfo(context, localStringList);
    		
    		Part part = (Part) DomainObject.newInstance(context, DomainConstants.TYPE_PART, DomainConstants.ENGINEERING);
    		String policy = (String) localMap.get("policy");
    		String vault = (String) localMap.get("vault");
    		part.checkPartCreateLicense(context, (String) localMap.get("policy"));

    		String revision                = StringUtils.trimToEmpty((String) paramMap.get("Revision"));
    		String strPartNo               = StringUtils.trimToEmpty((String) paramMap.get("KeyInTextPartNo"));
    		String strPartName             = StringUtils.trimToEmpty((String) paramMap.get("PartName"));
    		String strPhase                = StringUtils.trimToEmpty((String) paramMap.get("Phase"));
    		String strNoOfParts            = StringUtils.trimToEmpty((String) paramMap.get("PartCodeOfPart"));
    		String strECobjectId           = StringUtils.trimToEmpty((String) paramMap.get("EC_Oid"));
    		String strDrawingId            = StringUtils.trimToEmpty((String) paramMap.get("DrawingNo_Oid"));
    		String strDrawingNo            = StringUtils.trimToEmpty((String) paramMap.get("DrawingNo"));
    		String strIncludeRelatedDataEBOM       
    									   = StringUtils.trimToEmpty((String) paramMap.get("IncludeRelatedDataEBOM"));
    		String strIncludeRelatedDataDocument   
    									   = StringUtils.trimToEmpty((String) paramMap.get("IncludeRelatedDataDocument"));
    		String strSpecificationYn      = StringUtils.trimToEmpty((String) paramMap.get("VariantPart"));
    		String strPartPhase      	   = StringUtils.trimToEmpty((String) paramMap.get("partPhase"));
    		String manyPartsOneDrawing     = StringUtils.trimToEmpty((String) paramMap.get("manyPartsOneDrawing"));
    		
    		StringBuffer strUrl = new StringBuffer();
        	StringBuffer strParamBuffer = new StringBuffer();
        	boolean isArrayData = false;
    		
    		
    		if(strPartNo.length() == 5){
        		strUrl.append(cdmPropertiesUtil.getPropValue(propertyFile, plmPartsUrl));
        		strParamBuffer.append("uid");
        		strParamBuffer.append("=");
        		strParamBuffer.append(context.getUser());
        		strParamBuffer.append("&");
        		strParamBuffer.append("blockCode");
        		strParamBuffer.append("=");
        		strParamBuffer.append(strPartNo);//Block Code
        		strParamBuffer.append("&");
        		strParamBuffer.append("count");
        		strParamBuffer.append("=");
        		strParamBuffer.append(strNoOfParts);//Number of Part
        		strParamBuffer.append("&");
        		strParamBuffer.append("serialYn");
        		strParamBuffer.append("=");
        		strParamBuffer.append("N");
        		strParamBuffer.append("&");
        		strParamBuffer.append("specificationYn");
        		strParamBuffer.append("=");
        		strParamBuffer.append(strSpecificationYn);
        			
        		isArrayData = true;
        			
        	}else if(strPartNo.length() == 10){
        		strUrl.append(cdmPropertiesUtil.getPropValue(propertyFile, plmPartUrl));
        		strParamBuffer.append("uid");
        		strParamBuffer.append("=");
        		strParamBuffer.append(context.getUser());
        		strParamBuffer.append("&");
        		strParamBuffer.append("partNumber");
        		strParamBuffer.append("=");
        		strParamBuffer.append(strPartNo);//Part Number
        			
        	}
        		
        	String strPLMPartURL = strUrl.append(strParamBuffer.toString()).toString();
        	String strJsonObject = null;
        		
        	// to alert PLM Error. added by ci.lee 
        	try {
        		strJsonObject = cdmJsonDataUtil.getJSON(strPLMPartURL);
					
        	} catch (Exception e) {
    			String strErrorMessage = EnoviaResourceBundle.getProperty(context,"emxEngineeringCentralStringResource",context.getLocale(),"emxEngineeringCentral.Alert.CannotGetPartNoFromPLM");
    			throw new FrameworkException(strErrorMessage);
    		} 
        		
        	String strResultInfo = cdmJsonDataUtil.getJSONResultData(strJsonObject, "RESULT" );
        	String strPartFullName = DomainConstants.EMPTY_STRING;
        	
        		
        	if("SUCCESS".equals(strResultInfo)){
        		
        		if(isArrayData){
        			
        			for(int i=0; i<Integer.parseInt(strNoOfParts); i++){
        					
        				strPartFullName = cdmJsonDataUtil.getJSONResultArrayData(strJsonObject, "DATA", i);
        					
        				HashMap attributes = new HashMap();
                	    attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_NAME   		, strPartName);    
                    	attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_PHASE  		, strPhase);
                    	attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_DRAWING_NO  	, strDrawingNo);
                    	
                		
                		Part partIns = new Part(strPartId);
                		BusinessObject buCloneObject = partIns.cloneObject(context, strPartFullName, revision, cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION);
                		String strClonePartObjectId = buCloneObject.getObjectId();
                		DomainObject cloneObj = DomainObject.newInstance(context, strClonePartObjectId);
                		
                		SqlSessionUtil.reNew("plm");
                	    sqlSession = SqlSessionUtil.getSqlSession();
                	    Map blockCodeParamMap = new HashMap();
                	    blockCodeParamMap.put("BLOCKCODE", strPartNo.substring(0, 5));
                	    List<Map<String, String>> optionsList = sqlSession.selectList("getPartBlockCodeOptionsMap", blockCodeParamMap);
                	    StringBuffer strOptionLabelBuffer = new StringBuffer();
                	    
                	    for(int k=0; k<optionsList.size(); k++){
                	    	Map map = (Map)optionsList.get(k);
                	    	String strLabelName = (String)map.get("LABELNAME");
                	    	strOptionLabelBuffer.append(strLabelName);
                	    	strOptionLabelBuffer.append("|");
                	    }
                	    
                	    attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_LABEL_NAME, strOptionLabelBuffer.toString());
                	    sqlSession.close();
                		
                	    cloneObj.setAttributeValues(context, attributes);
                		clonePartObjectId = cloneObj.getObjectId(context);
                		
                		DomainObject partObj = new DomainObject(strPartId);
                		SelectList selectList = new SelectList();
                		selectList.addId();
                		selectList.addName();
                		
                		MapList partRelEBOMList = new DomainObject(clonePartObjectId).getRelatedObjects(context, 
        						DomainConstants.RELATIONSHIP_EBOM, // relationship
        						cdmConstantsUtil.TYPE_CDMPART,     // type
        						selectList,     				   // objects
        						null,  							   // relationships
        						false,                             // to
        						true,          					   // from
        						(short)1,                          // recurse
        						null,                              // where
        						null,                              // relationship where
        						(short)0);                         // limit
                		
                    			
                    	if(DomainConstants.EMPTY_STRING.equals(strIncludeRelatedDataEBOM)){
                        	
                        	for(int h=0; h<partRelEBOMList.size(); h++){
                        		Map map = (Map)partRelEBOMList.get(h);
                        		String id = (String)map.get(DomainConstants.SELECT_ID);
                        		
                        		try {
                        			
                    				ContextUtil.pushContext(context, null, null, null);
                    				MqlUtil.mqlCommand(context, "trigger off", new String[]{});
                    				MqlUtil.mqlCommand(context, "disconnect bus $1 relationship $2 to $3", new String[]{clonePartObjectId, DomainConstants.RELATIONSHIP_EBOM, id});
                				
                        		} catch (Exception e) {
                					throw e;
                				}finally {
                					MqlUtil.mqlCommand(context, "trigger on", new String[]{});
                					ContextUtil.popContext(context);
                				}
                        	}
                        	
                    	}else{
                    		
                    		for(int h=0; h<partRelEBOMList.size(); h++){
                        		Map map = (Map)partRelEBOMList.get(h);
                        		String id = (String)map.get(DomainConstants.SELECT_ID);
                        		DomainObject domObj = new DomainObject(id);
                        		//String strIsVersionObject = domObj.getAttributeValue(context, "Is Version Object");
                        		String strIsVersionObject = StringUtils.trimToEmpty( domObj.getInfo(context, "next.revision"));
                        		
                        		try {
                    				ContextUtil.pushContext(context, null, null, null);
                    				MqlUtil.mqlCommand(context, "trigger off", new String[]{});
                    				if(! "".equals(strIsVersionObject)){
                    					MqlUtil.mqlCommand(context, "disconnect bus $1 relationship $2 to $3", new String[]{clonePartObjectId, DomainConstants.RELATIONSHIP_EBOM, id});
                    				}
                				} catch (Exception e) {
                					throw e;
                				}finally {
                					MqlUtil.mqlCommand(context, "trigger on", new String[]{});
                					ContextUtil.popContext(context);
                				}
                        	}
                    		
                    	}
                    			
                    	if("Document".equals(strIncludeRelatedDataDocument)){
                    		MapList partRelDocumentList = new DomainObject(clonePartObjectId).getRelatedObjects(context, 
            																		DomainConstants.RELATIONSHIP_REFERENCE_DOCUMENT, //relationship
            																		"DOCUMENTS",	// type
            																		selectList,     // objects
            																		null,  			// relationships
            																		false,          // to
            																		true,          	// from
            																		(short)1,       // recurse
            																		null,           // where
            																		null,           // relationship where
            																		(short)0);      // limit
                    				
                    		for(int h=0; h<partRelDocumentList.size(); h++){
                    			Map map = (Map)partRelDocumentList.get(h);
                    			String id = (String)map.get(DomainConstants.SELECT_ID);
                    			
                    			try {
                    				ContextUtil.pushContext(context, null, null, null);
                    				MqlUtil.mqlCommand(context, "trigger off", new String[]{});
                    				MqlUtil.mqlCommand(context, "disconnect bus $1 relationship $2 to $3", new String[]{clonePartObjectId, DomainConstants.RELATIONSHIP_REFERENCE_DOCUMENT, id});
                				} catch (Exception e) {
                					throw e;
                				}finally {
                					MqlUtil.mqlCommand(context, "trigger on", new String[]{});
                					ContextUtil.popContext(context);
                				}
                    		}
                    		
                    	}
                		
                		if(! DomainConstants.EMPTY_STRING.equals(strECobjectId)){
                			try {
                				MqlUtil.mqlCommand(context, "connect bus $1 relationship $2 to $3", new String[]{strECobjectId, DomainConstants.RELATIONSHIP_AFFECTED_ITEM, clonePartObjectId});
            				} catch (Exception e) {
            					throw e;
            				}finally {
            					
            				}
                		}
                		
                		if(! DomainConstants.EMPTY_STRING.equals(strDrawingId)){
                			String strCADObjectDrawingNo = new DomainObject(strDrawingId).getInfo(context, cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_DRAWING_NO);
                			cloneObj.setAttributeValue(context, cdmConstantsUtil.ATTRIBUTE_CDM_PART_DRAWING_NO, strCADObjectDrawingNo);
                		}
                		MqlUtil.mqlCommand(context, "connect bus $1 relationship $2 to $3", new String[]{partObj.getObjectId(context), "Derived", clonePartObjectId});
                		
                		strBuffer.append(clonePartObjectId);
                		
                		if(Integer.parseInt(strNoOfParts)-1 != i){
                			strBuffer.append(",");
    					}
                		
                		
                		if("Y".equals(manyPartsOneDrawing)) {
                    		
                			String strPhantomObjectId = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", strPartId, "to["+DomainConstants.RELATIONSHIP_EBOM+"|from.type=='cdmPhantomPart'].from.id"));    
                			
                		    if( ! DomainConstants.EMPTY_STRING.equals(strPhantomObjectId) ) {
                		    	
                		    	DomainObject phantomObj = new DomainObject(strPhantomObjectId);
                		    	String strPartDrawingNo = phantomObj.getAttributeValue(context, "cdmPartDrawingNo");
                		    	
                		    	DomainObject clonePartObj = new DomainObject(clonePartObjectId);
                		    	clonePartObj.setAttributeValue(context, "cdmPartDrawingNo", strPartDrawingNo);
                		    	
                		    	try {
                        			
                    				ContextUtil.pushContext(context, null, null, null);
                    				MqlUtil.mqlCommand(context, "trigger off", new String[]{});
                    				MqlUtil.mqlCommand(context, "connect bus $1 relationship $2 to $3", new String[]{strPhantomObjectId, "EBOM", clonePartObjectId});
                				
                        		} catch (Exception e) {
                					throw e;
                				}finally {
                					MqlUtil.mqlCommand(context, "trigger on", new String[]{});
                					ContextUtil.popContext(context);
                				}
                		    	
                		    }
                			
                    	}
        					
        			}
        			
        		}else{
        			
        			strPartFullName = cdmJsonDataUtil.getJSONResultData(strJsonObject, "PARTNUMBER" );
        			
        			HashMap attributes = new HashMap();
            	    attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_NAME   		, strPartName);    
                	attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_PHASE  		, strPhase);
                	attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_DRAWING_NO  	, strDrawingNo);
                	
                	
                	SqlSessionUtil.reNew("plm");
            	    sqlSession = SqlSessionUtil.getSqlSession();
            	    Map blockCodeParamMap = new HashMap();
            	    blockCodeParamMap.put("BLOCKCODE", strPartNo.substring(0, 5));
            	    List<Map<String, String>> optionsList = sqlSession.selectList("getPartBlockCodeOptionsMap", blockCodeParamMap);
            	    StringBuffer strOptionLabelBuffer = new StringBuffer();
            	    
            	    for(int k=0; k<optionsList.size(); k++){
            	    	Map map = (Map)optionsList.get(k);
            	    	String strLabelName = (String)map.get("LABELNAME");
            	    	strOptionLabelBuffer.append(strLabelName);
            	    	strOptionLabelBuffer.append("|");
            	    }
            	    
            	    attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_LABEL_NAME, strOptionLabelBuffer.toString());
                	
            		
            		Part partIns = new Part(strPartId);
            		BusinessObject buCloneObject = partIns.cloneObject(context, strPartFullName, revision, cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION);
            		String strClonePartObjectId = buCloneObject.getObjectId();
            		DomainObject cloneObj = DomainObject.newInstance(context, strClonePartObjectId);
            		
            	    
            		cloneObj.setAttributeValues(context, attributes);
            		clonePartObjectId = cloneObj.getObjectId(context);
            		
            		DomainObject partObj = new DomainObject(strPartId);
            		SelectList selectList = new SelectList();
            		selectList.addId();
            		selectList.addName();
            		
            		MapList partRelEBOMList = new DomainObject(clonePartObjectId).getRelatedObjects(context, 
    						DomainConstants.RELATIONSHIP_EBOM, // relationship
    						cdmConstantsUtil.TYPE_CDMPART,     // type
    						selectList,     				   // objects
    						null,  							   // relationships
    						false,                             // to
    						true,          					   // from
    						(short)1,                          // recurse
    						null,                              // where
    						null,                              // relationship where
    						(short)0);                         // limit
            		
                			
                	if(DomainConstants.EMPTY_STRING.equals(strIncludeRelatedDataEBOM)){
                    	
                    	for(int h=0; h<partRelEBOMList.size(); h++){
                    		Map map = (Map)partRelEBOMList.get(h);
                    		String id = (String)map.get(DomainConstants.SELECT_ID);
                    		
                    		try {
                    			
                				ContextUtil.pushContext(context, null, null, null);
                				MqlUtil.mqlCommand(context, "trigger off", new String[]{});
                				MqlUtil.mqlCommand(context, "disconnect bus $1 relationship $2 to $3", new String[]{clonePartObjectId, DomainConstants.RELATIONSHIP_EBOM, id});
            				
                    		} catch (Exception e) {
            					throw e;
            				}finally {
            					MqlUtil.mqlCommand(context, "trigger on", new String[]{});
            					ContextUtil.popContext(context);
            				}
                    	}
                    	
                	}else{
                		
                		for(int h=0; h<partRelEBOMList.size(); h++){
                    		Map map = (Map)partRelEBOMList.get(h);
                    		String id = (String)map.get(DomainConstants.SELECT_ID);
                    		DomainObject domObj = new DomainObject(id);
                    		String strIsVersionObject = StringUtils.trimToEmpty( domObj.getInfo(context, "next.revision"));
                    		
                    		try {
                				ContextUtil.pushContext(context, null, null, null);
                				MqlUtil.mqlCommand(context, "trigger off", new String[]{});
                				if(! "".equals(strIsVersionObject)){
                					MqlUtil.mqlCommand(context, "disconnect bus $1 relationship $2 to $3", new String[]{clonePartObjectId, DomainConstants.RELATIONSHIP_EBOM, id});
                				}
            				} catch (Exception e) {
            					throw e;
            				}finally {
            					MqlUtil.mqlCommand(context, "trigger on", new String[]{});
            					ContextUtil.popContext(context);
            				}
                    	}
                		
                	}
                			
                	if("Document".equals(strIncludeRelatedDataDocument)){
                		MapList partRelDocumentList = new DomainObject(clonePartObjectId).getRelatedObjects(context, 
        																		DomainConstants.RELATIONSHIP_REFERENCE_DOCUMENT, //relationship
        																		"DOCUMENTS",	// type
        																		selectList,     // objects
        																		null,  			// relationships
        																		false,          // to
        																		true,          	// from
        																		(short)1,       // recurse
        																		null,           // where
        																		null,           // relationship where
        																		(short)0);      // limit
                				
                		for(int h=0; h<partRelDocumentList.size(); h++){
                			Map map = (Map)partRelDocumentList.get(h);
                			String id = (String)map.get(DomainConstants.SELECT_ID);
                			
                			try {
                				ContextUtil.pushContext(context, null, null, null);
                				MqlUtil.mqlCommand(context, "trigger off", new String[]{});
                				MqlUtil.mqlCommand(context, "disconnect bus $1 relationship $2 to $3", new String[]{clonePartObjectId, DomainConstants.RELATIONSHIP_REFERENCE_DOCUMENT, id});
            				} catch (Exception e) {
            					throw e;
            				}finally {
            					MqlUtil.mqlCommand(context, "trigger on", new String[]{});
            					ContextUtil.popContext(context);
            				}
                		}
                		
                	}
            		
            		if(! DomainConstants.EMPTY_STRING.equals(strECobjectId)){
            			
            			try {
            				MqlUtil.mqlCommand(context, "connect bus $1 relationship $2 to $3", new String[]{strECobjectId, DomainConstants.RELATIONSHIP_AFFECTED_ITEM, clonePartObjectId});
        				} catch (Exception e) {
        					throw e;
        				}
            			
            		}
            		
            		if(! DomainConstants.EMPTY_STRING.equals(strDrawingId)){
            			String strCADObjectDrawingNo = new DomainObject(strDrawingId).getInfo(context, cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_DRAWING_NO);
            			cloneObj.setAttributeValue(context, cdmConstantsUtil.ATTRIBUTE_CDM_PART_DRAWING_NO, strCADObjectDrawingNo);
            		}
            		
            		MqlUtil.mqlCommand(context, "connect bus $1 relationship $2 to $3", new String[]{partObj.getObjectId(context), "Derived", clonePartObjectId});
            		
            		strBuffer.append(clonePartObjectId);
            		
            		if("Y".equals(manyPartsOneDrawing)) {
                		
            			String strPhantomObjectId = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", strPartId, "to["+DomainConstants.RELATIONSHIP_EBOM+"|from.type=='cdmPhantomPart'].from.id"));    
            			
            		    if( ! DomainConstants.EMPTY_STRING.equals(strPhantomObjectId) ) {
            		    	
            		    	DomainObject phantomObj = new DomainObject(strPhantomObjectId);
            		    	String strPartDrawingNo = phantomObj.getAttributeValue(context, "cdmPartDrawingNo");
            		    	
            		    	DomainObject clonePartObj = new DomainObject(clonePartObjectId);
            		    	clonePartObj.setAttributeValue(context, "cdmPartDrawingNo", strPartDrawingNo);
            		    	
            		    	try {
                    			
                				ContextUtil.pushContext(context, null, null, null);
                				MqlUtil.mqlCommand(context, "trigger off", new String[]{});
                				MqlUtil.mqlCommand(context, "connect bus $1 relationship $2 to $3", new String[]{strPhantomObjectId, "EBOM", clonePartObjectId});
                				
                    		} catch (Exception e) {
            					throw e;
            				}finally {
            					MqlUtil.mqlCommand(context, "trigger on", new String[]{});
            					ContextUtil.popContext(context);
            				}
            		    	
            		    }
            			
                	}
            		
        		}
        		
    		}
        	
        	if("FAIL".equals(strResultInfo)){
        		throw new FrameworkException (cdmJsonDataUtil.getJSONResultData(strJsonObject, "ERRMSG"));
        	}
        	
    		
    		ContextUtil.commitTransaction(context);
    		
    	}catch(Exception e){
    		
    		ContextUtil.abortTransaction(context);
    		e.printStackTrace();
    		throw new FrameworkException(e.getMessage().toString());
    		
    	}
    	
    	System.out.println("strBuffer.toString()     "+strBuffer.toString());
    	return strBuffer.toString();
    	
	}
    
    /**
	 * @desc  "Part clone" Detail Edit
	 */
    public MapList getPartCloneEditDetails (Context context, String[] args) throws Exception {
    	MapList mlPartCloneEditList = new MapList();
    	try{
    		HashMap paramMap = (HashMap) JPO.unpackArgs(args);
    		SelectList selectList = new SelectList();
    		selectList.addId();
    		selectList.addName();
    		String objectIds = (String)paramMap.get("objectId");
    		
    		if(! DomainConstants.EMPTY_STRING.equals(objectIds)){
    			String[] objectIdArray = objectIds.split(",");
        		for(int i=0; i<objectIdArray.length; i++){
        			Map map = new HashMap();
        			map.put(DomainConstants.SELECT_ID, objectIdArray[i]);
        			mlPartCloneEditList.add(map);
        		}
    		}
    		System.out.println("mlPartCloneEditList        "+mlPartCloneEditList);
//    		String rootPartId = MqlUtil.mqlCommand(context, "print bus "+objectId+" select to[Derived].from.id dump");
//    		DomainObject domObj = new DomainObject(rootPartId);
//    		mlPartCloneEditList = domObj.getRelatedObjects(context, "Derived", "*", selectList, null, true, false, (short)1, "", null);
//    		mlPartCloneEditList.sort();
    	}catch(Exception e){
    		e.printStackTrace();	
    	}
    	return mlPartCloneEditList;
    }

	@SuppressWarnings("unchecked")
    private String createPartObject(Context context, HashMap attributes, String strPhase, String strType, String strPartNo, String strRevision) throws Exception{
    	DomainObject partObj = new DomainObject();
    	SqlSession sqlSession = null;
    	try{
    		StringBuffer strUrl = new StringBuffer();
    		String strSpecificationYn = (String)attributes.get(cdmConstantsUtil.ATTRIBUTE_CDM_PART_PLM_VARIANT_YN);
    		StringBuffer strParamBuffer = new StringBuffer();
    		boolean isArrayData = false;
    		
    		if(strPartNo.length() == 5){
    			strUrl.append(cdmPropertiesUtil.getPropValue(propertyFile, plmPartsUrl));
    			strParamBuffer.append("uid");
    			strParamBuffer.append("=");
    			strParamBuffer.append(context.getUser());
    			strParamBuffer.append("&");
    			strParamBuffer.append("blockCode");
    			strParamBuffer.append("=");
    			strParamBuffer.append(strPartNo);//Block Code
    			strParamBuffer.append("&");
    			strParamBuffer.append("count");
    			strParamBuffer.append("=");
    			strParamBuffer.append("1");//Number of Part
    			strParamBuffer.append("&");
    			strParamBuffer.append("serialYn");
    			strParamBuffer.append("=");
    			strParamBuffer.append("N");
    			strParamBuffer.append("&");
    			strParamBuffer.append("specificationYn");
    			strParamBuffer.append("=");
    			strParamBuffer.append(strSpecificationYn);
    			
    			isArrayData = true;
    			
    		}else if(strPartNo.length() == 10){
    			strUrl.append(cdmPropertiesUtil.getPropValue(propertyFile, plmPartUrl));
    			strParamBuffer.append("uid");
    			strParamBuffer.append("=");
    			strParamBuffer.append(context.getUser());
    			strParamBuffer.append("&");
    			strParamBuffer.append("partNumber");
    			strParamBuffer.append("=");
    			strParamBuffer.append(strPartNo);//Part Number
    			
    		}
    		
    		String strPLMPartURL = strUrl.append(strParamBuffer.toString()).toString();
    		String strJsonObject = null;
    		
    		// to alert PLM Error. added by ci.lee 
    		try {
    			strJsonObject = cdmJsonDataUtil.getJSON(strPLMPartURL);
			} catch (Exception e) {

				String strErrorMessage = EnoviaResourceBundle.getProperty(context,"emxEngineeringCentralStringResource",context.getLocale(),"emxEngineeringCentral.Alert.CannotGetPartNoFromPLM");
				throw new FrameworkException(strErrorMessage);
			}
    		
    		String strResultInfo = cdmJsonDataUtil.getJSONResultData(strJsonObject, "RESULT" );
    		if("SUCCESS".equals(strResultInfo)){
    			if(isArrayData){
    				strPartNo = cdmJsonDataUtil.getJSONResultArrayData(strJsonObject, "DATA", 0);
    			}else{
    				strPartNo = cdmJsonDataUtil.getJSONResultData(strJsonObject, "PARTNUMBER" );
    			}
		    	System.out.println("Part  No     :     "+strPartNo);
		    }
    		
    		if("FAIL".equals(strResultInfo)){
    			String strFailMessage = cdmJsonDataUtil.getJSONResultData(strJsonObject, "ERRMSG");
    			System.out.println(" Fail Message      "+strFailMessage);
    			throw new FrameworkException (strFailMessage);
				//return cdmJsonDataUtil.getJSONResultData(strJsonObject, "ERRMSG");
		    }
    		
    		
    		
    		SqlSessionUtil.reNew("plm");
    	    sqlSession = SqlSessionUtil.getSqlSession();
    	    Map paramMap = new HashMap();
    	    paramMap.put("BLOCKCODE", strPartNo.substring(0, 5));
    	    List<Map<String, String>> optionsList = sqlSession.selectList("getPartBlockCodeOptionsMap", paramMap);
    	    StringBuffer strBuffer = new StringBuffer();
    	    for(int k=0; k<optionsList.size(); k++){
    	    	Map map = (Map)optionsList.get(k);
    	    	String strLabelName = (String)map.get("LABELNAME");
    	    	strBuffer.append(strLabelName);
    	    	strBuffer.append("|");
    	    }
    	    attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_LABEL_NAME,    strBuffer.toString());
    	    sqlSession.close();
    	    
    	    
    	    
    	    String strMqlValue = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "temp query bus $1 $2 $3 where $4 select $5 dump $6", new String[] {cdmConstantsUtil.TYPE_CATDrawing, "*", "*", "attribute["+cdmConstantsUtil.ATTRIBUTE_CDM_DRAWING_NO+"] == \""+strPartNo+"\" ", DomainConstants.SELECT_ID, "|"}));
    	    
	    	if(cdmConstantsUtil.TEXT_PROTO.equals(strPhase)){
	    		
	    		if(DomainConstants.EMPTY_STRING.equals(strRevision)){
	    			strRevision = "01";
	    		}
	    		
//	    		if("cdmPhantomPart".equals(strType)){
//	    			ContextUtil.pushContext(context, null, null, null);
//		        	MqlUtil.mqlCommand(context, "trigger off", new String[]{});	
//	    		}

	    		partObj.createObject(context, strType, strPartNo, strRevision, cdmConstantsUtil.POLICY_CDM_PART_POLICY, cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION);
	        	
//	        	if("cdmPhantomPart".equals(strType)){
//	        		MqlUtil.mqlCommand(context, "trigger on", new String[]{});
//		        	ContextUtil.popContext(context);
//		        	MqlUtil.mqlCommand(context, "mod bus $1 $2 $3", new String[] {partObj.getObjectId(context), DomainConstants.SELECT_OWNER, context.getUser()});	
//	    		}
	        	
	        	
	        	//MqlUtil.mqlCommand(context, "add bus $1 $2 $3 policy $4 vault $5", new String[] {strType, strPartNo, "01", cdmConstantsUtil.POLICY_CDM_PART_POLICY, cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION});  
	        	
	    		if(! DomainConstants.EMPTY_STRING.equals(strMqlValue)){
	    			attributes.remove(cdmConstantsUtil.ATTRIBUTE_CDM_PART_DRAWING_NO);
	    		}
	    		partObj.setAttributeValues(context, attributes);
	    		
	        	
	        }else if(cdmConstantsUtil.TEXT_PRODUCTION.equals(strPhase)){
	        	
	        	if(DomainConstants.EMPTY_STRING.equals(strRevision)){
	    			strRevision = "A";
	    		}
	        	
	        	partObj.createObject(context, strType, strPartNo, strRevision, cdmConstantsUtil.POLICY_CDM_PART_POLICY, cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION);
	        	
	    		if(! DomainConstants.EMPTY_STRING.equals(strMqlValue)){
	    			attributes.remove(cdmConstantsUtil.ATTRIBUTE_CDM_PART_DRAWING_NO);
	    		}
	    		partObj.setAttributeValues(context, attributes);
	        	
	        }
	    	
	    	
	    	return partObj.getObjectId(context);
	    	
    	}catch (Exception e){
    		e.printStackTrace();
    		System.out.println("Error Message     :     " + e.getMessage().toString());
    		throw new FrameworkException (e.getMessage().toString());
    	}
    	
	}
	
	public String editPart(Context context, String[] args) throws Exception{
    	HashMap paramMap = (HashMap) JPO.unpackArgs(args);
    	String strType               = (String)paramMap.get("TypeActual");
    	String strPhase              = (String)paramMap.get("Phase");
    	String strPartNo             = (String)paramMap.get("PartNo");
    	String strVehicle            = (String)paramMap.get("Vehicle");
    	String strPartName           = (String)paramMap.get("PartName");
    	String strProject            = (String)paramMap.get("Project");
    	String strApprovalType       = (String)paramMap.get("ApprovalType");
    	String strPartType           = (String)paramMap.get("PartType");
    	String strGlobal             = (String)paramMap.get("Global");
    	String strDrawingNo          = (String)paramMap.get("DrawingNo");
    	String strUnitOfMeasure      = (String)paramMap.get("UnitOfMeasure");
    	String strECONumber          = (String)paramMap.get("ECONumber");
    	String strItemType           = (String)paramMap.get("ItemType");
    	String strOEMItemNumber      = (String)paramMap.get("OEMItemNumber");
    	String strComments 			 = (String)paramMap.get("Comments");
    	String strChangeReason 		 = (String)paramMap.get("ChangeReason");
    	String strOrg1 				 = (String)paramMap.get("Org1");
    	String strOrg2 				 = (String)paramMap.get("Org2");
    	String strOrg3 				 = (String)paramMap.get("Org3");
    	String strProductType 		 = (String)paramMap.get("ProductType");
    	String strERPInterface 		 = (String)paramMap.get("ERPInterface");
    	String strSurface 			 = (String)paramMap.get("Surface");
    	String strEstimatedWeight 	 = (String)paramMap.get("EstimatedWeight");
    	String strEstimatedWeightUOM = (String)paramMap.get("EstimatedWeightUOM");
    	String strMaterial 			 = (String)paramMap.get("Material");
    	String strRealWeight 	     = (String)paramMap.get("RealWeight");
    	String strSize 				 = (String)paramMap.get("Size");
    	String strCADWeight 		 = (String)paramMap.get("CADWeight");
    	String strSurfaceTreatment   = (String)paramMap.get("SurfaceTreatment");
    	String strIsCasting 		 = (String)paramMap.get("IsCasting");
    	String strOption1 			 = (String)paramMap.get("Option1");
    	String strOption2 			 = (String)paramMap.get("Option2");
    	String strOption3 			 = (String)paramMap.get("Option3");
    	String strOption4 			 = (String)paramMap.get("Option4");
    	String strOption5 			 = (String)paramMap.get("Option5");
    	String strOption6 			 = (String)paramMap.get("Option6");
    	String strOptionETC 		 = (String)paramMap.get("OptionETC");
    	String strOptionDescription  = (String)paramMap.get("OptionDescription");
    	String strPublishingTarget 	 = (String)paramMap.get("PublishingTarget");
    	String strInvestor 			 = (String)paramMap.get("Investor");
    	String strProjectType 		 = (String)paramMap.get("ProjectType");
    	String objectId     		 = (String)paramMap.get("objectId");
    	String objectType   		 = (String)paramMap.get("objectType");
    	String strStandardBOM		 = (String)paramMap.get("StandardBOM");
    	String strApplyPartList   	 = (String)paramMap.get("ApplyPartList");
    	String strMaterialCoSign   	 = (String)paramMap.get("MaterialCoSign");
    	String strSurfaceTreatmentCoSign  = (String)paramMap.get("SurfaceTreatmentCoSign");
    	String strRevision   	     = (String)paramMap.get("Revision");
    	
    	
    	if(DomainConstants.EMPTY_STRING.equals(StringUtils.trimToEmpty(strMaterialCoSign))){
        	strMaterialCoSign = "N";
        }
        if(DomainConstants.EMPTY_STRING.equals(StringUtils.trimToEmpty(strSurfaceTreatmentCoSign))){
        	strSurfaceTreatmentCoSign = "N";
        }
        
    	
    	String VehicleObjectId 		 = (String)paramMap.get("VehicleObjectId");
    	String ProjectObjectId 		 = (String)paramMap.get("ProjectObjectId");
    	String ProjectTypeObjectId 	 = (String)paramMap.get("ProjectTypeObjectId");
    	String ProductTypeObjectId 	 = (String)paramMap.get("ProductTypeObjectId");
    	String Org1ObjectId 		 = (String)paramMap.get("Org1ObjectId");
    	String Org2ObjectId 		 = (String)paramMap.get("Org2ObjectId");
    	String Org3ObjectId 		 = (String)paramMap.get("Org3ObjectId");
    	String Option1ObjectId 		 = (String)paramMap.get("Option1ObjectId");
    	String Option2ObjectId 		 = (String)paramMap.get("Option2ObjectId");
    	String Option3ObjectId 		 = (String)paramMap.get("Option3ObjectId");
    	String Option4ObjectId 		 = (String)paramMap.get("Option4ObjectId");
    	String Option5ObjectId 		 = (String)paramMap.get("Option5ObjectId");
    	String Option6ObjectId 		 = (String)paramMap.get("Option6ObjectId");
    	String ECONumberOid 		 = (String)paramMap.get("ECONumberOid");
    	String DrawingNo_Oid 		 = (String)paramMap.get("DrawingNo_Oid");
    	String materialOID 		     = (String)paramMap.get("materialOID");
    	String surfaceTreatmentOID   = (String)paramMap.get("surfaceTreatmentOID");
    	
    	HashMap attributes = new HashMap();
    	
    	
    	if( ! DomainConstants.EMPTY_STRING.equals(materialOID) ){
        	
        	if( DomainConstants.EMPTY_STRING.equals(surfaceTreatmentOID) ){
        		attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_PLM_OBJECTID    , 	materialOID + "|none");
        	}else{
        		attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_PLM_OBJECTID    , 	materialOID + "|");
        	}
        	
        }
        
        if( ! DomainConstants.EMPTY_STRING.equals(surfaceTreatmentOID) ){
        	
        	if( DomainConstants.EMPTY_STRING.equals(materialOID) ){
        		attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_PLM_OBJECTID    ,    "none|" + surfaceTreatmentOID);
        	}else{
        		attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_PLM_OBJECTID    , 	materialOID + "|" + surfaceTreatmentOID);
        	}
        	
        }
    	
    	String editCreatePartObjectId = "";
    	DomainObject partObj = new DomainObject();
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_PHASE             , 	strPhase);                       
        //attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_APPROVAL_TYPE     , 	strApprovalType);                
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_TYPE              , 	strPartType);                    
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_GLOBAL            , 	strGlobal);                      
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_UOM               ,  strUnitOfMeasure);               
        //attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_ITEM_TYPE         , 	strItemType);                    
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_OEM_ITEM_NUMBER   , 	strOEMItemNumber);               
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_DESCRIPTION       , 	strComments);                    
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_CHANGE_REASON     , 	strChangeReason);                
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_ERP_INTERFACE     , 	strERPInterface);                
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_SURFACE           , 	strSurface);                     
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_ESTIMATED_WEIGHT  , 	strEstimatedWeight);             
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_MATERIAL          , 	strMaterial);                    
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_REAL_WEIGHT       , 	strRealWeight);                  
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_SIZE              , 	strSize);                        
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_CAD_WEIGHT        , 	strCADWeight);                   
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_SURFACE_TREATMENT , 	strSurfaceTreatment);            
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_IS_CASTING        , 	strIsCasting);                   
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_ETC        , 	strOptionETC);                   
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_DESCRIPTION, 	strOptionDescription);           
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_PUBLISHING_TARGET ,  strPublishingTarget);            
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_INVESTOR          , 	strInvestor); 
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_DRAWING_NO        , 	strDrawingNo);
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_STANDARD_BOM      , 	strStandardBOM);
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_ESTIMATED_WEIGHT_UOM       , 	strEstimatedWeightUOM);
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_PLM_MATERIAL_CO_SIGN_YN    , 	strMaterialCoSign);
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_PLM_SURFACE_TREATMENT_CO_SIGN_YN    , 	strSurfaceTreatmentCoSign);
        
        
        String strPartCheck = "false";
        String strSizeCheck = "false";
        String strSurfaceTreatmentCheck = "false";
        if(! DomainConstants.EMPTY_STRING.equals(strApplyPartList)){
        	String[] strApplyPartListArray = strApplyPartList.split(",");
            for(int k=0; k<strApplyPartListArray.length; k++){
            	String strApplyPartListValue = strApplyPartListArray[k];
            	if("Part".equals(strApplyPartListValue)){
            		strPartCheck = "true";
            	}else if("Surface Treatment".equals(strApplyPartListValue)){
            		strSurfaceTreatmentCheck = "true";
            	}else if("Size".equals(strApplyPartListValue)){
            		strSizeCheck= "true";
            	}
            }
        }
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_APPLY_PART_LIST, strPartCheck);
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_APPLY_SIZE_LIST, strSizeCheck);
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_APPLY_SURFACE_TREATMENT_LIST, strSurfaceTreatmentCheck);
        
        HashMap objectAttrMap = new HashMap();
        try{
        	
        	ContextUtil.startTransaction(context, true);
        	
        	ContextUtil.pushContext(context, null, null, null);
        	MqlUtil.mqlCommand(context, "trigger off", new String[]{});
        	
        	if(DomainConstants.TYPE_PART.equals(objectType)){ 
        		attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_NAME, strPartName);     
        		editCreatePartObjectId = createPartObject(context, attributes, strPhase, cdmConstantsUtil.TYPE_CDMMECHANICALPART, strPartNo, DomainConstants.EMPTY_STRING);	
        		partObj.setId(editCreatePartObjectId);
        	}else{
        		//partObj.setId(objectId);
        		partObj = new DomainObject(objectId);
        		partObj.setAttributeValues(context, attributes);
        	}
        	
        	
        	///////////////////////////////  PhantomPart Connect MechanicalPart  && SeriesPart Validation Start !!!
        	if(! DomainConstants.EMPTY_STRING.equals(DrawingNo_Oid)){
	
        		String strDrawingRelPartType = StringUtils.trimToEmpty(new DomainObject(DrawingNo_Oid).getInfo(context, "to[Part Specification].from.type"));
        		
        	    if("cdmMechanicalPart".equals(strDrawingRelPartType) ){
        		
        			String strDrawingRelPartRevision = StringUtils.trimToEmpty(new DomainObject(DrawingNo_Oid).getInfo(context, "to[Part Specification].from.revision"));
        				
        			if(! strRevision.equals(strDrawingRelPartRevision) ){
        			
        				String strSeriesPartNotRevisionMessage = "Series Part throw Revision";
        				throw new FrameworkException(strSeriesPartNotRevisionMessage);
        			
        			}
        		
        		}else{
        				
//        			try{
//        	        		
//        	        	String strDrawingRelPartRevision = StringUtils.trimToEmpty(new DomainObject(DrawingNo_Oid).getInfo(context, "to[Part Specification].from.revision"));
//        	        	if( strRevision.equals(strDrawingRelPartRevision) ){
//        	        			
//        	        		String strSeriesPartNotRevisionMessage = "Series Part throw Revision";
//        	            	throw new FrameworkException(strSeriesPartNotRevisionMessage);
//        	        			
//        	        	}else{
//        	        			
//        	        		String strDrawingRelPartId = StringUtils.trimToEmpty(new DomainObject(DrawingNo_Oid).getInfo(context, "to[Part Specification].from.id"));
//                			MqlUtil.mqlCommand(context, "connect bus $1 relationship $2 to $3", new String[]{strDrawingRelPartId, "EBOM", objectId});
//          		
//        	        	}
//            				
//            		}catch(Exception e){
//            			throw new FrameworkException(e.getMessage().toString());
//            		}
        				
        		}
	
        	}
        	///////////////////////////////  PhantomPart Connect MechanicalPart  && SeriesPart Validation  End !!!  
        	
        	
        	partObjectRelationShip(context, partObj.getObjectId(context), VehicleObjectId, ProjectObjectId, ProjectTypeObjectId, ProductTypeObjectId, Org1ObjectId, Org2ObjectId, Org3ObjectId, Option1ObjectId, Option2ObjectId, Option3ObjectId, Option4ObjectId, Option5ObjectId, Option6ObjectId, ECONumberOid, DrawingNo_Oid);
        	
        	MqlUtil.mqlCommand(context, "trigger on", new String[]{});
        	ContextUtil.popContext(context);
        	
        	ContextUtil.commitTransaction(context);
        }catch(Exception e){
        	e.printStackTrace();
        	ContextUtil.abortTransaction(context);
        	throw new FrameworkException(e.getMessage().toString());
        }
    	return partObj.getObjectId(context);
    }
    
    public Boolean isModificationAllowed(Context context, String[] args) throws Exception {
        boolean isShowCommand = true;
        try{
            HashMap paramMap = (HashMap)JPO.unpackArgs(args);
            if(true == isShowCommand){
            	String strObjectId = (String)paramMap.get("objectId");
                String strType     = new DomainObject(strObjectId).getInfo(context, DomainConstants.SELECT_TYPE);
                String strCurrent  = new DomainObject(strObjectId).getInfo(context, DomainConstants.SELECT_CURRENT);
                
                if(cdmConstantsUtil.TYPE_CDMPHANTOMPART.equals(strType) || cdmConstantsUtil.TYPE_CDMMECHANICALPART.equals(strType)){
                	return isShowCommand;		
                }else{
                	isShowCommand = false;
                }
            }
            
        } catch(Exception e) {
        	e.printStackTrace();
        	throw e;
        }
        return isShowCommand;
    }
    
    public Boolean isModificationOptionAllowed(Context context, String[] args) throws Exception {
        boolean isShowCommand = true;
        try{
            HashMap paramMap = (HashMap)JPO.unpackArgs(args);
            String strObjectId = (String)paramMap.get("objectId");
            DomainObject domObj = new DomainObject(strObjectId);
            
            String strCurrent  = domObj.getInfo(context, DomainConstants.SELECT_CURRENT);
            
            if(! DomainConstants.STATE_PART_PRELIMINARY.equals(strCurrent)){
            	isShowCommand = false;
            }
            
            String strType  = domObj.getInfo(context, DomainConstants.SELECT_TYPE);
            
            if(! cdmConstantsUtil.TYPE_CDMMECHANICALPART.equals(strType)){
            	isShowCommand = false;
            }
            
            String strPrevPartId = StringUtils.trimToEmpty(domObj.getInfo(context, "previous.id"));
            if(! DomainConstants.EMPTY_STRING.equals(strPrevPartId)){
            	String strExportYN = new DomainObject(strPrevPartId).getInfo(context, "to["+DomainConstants.RELATIONSHIP_AFFECTED_ITEM+"].from.attribute["+cdmConstantsUtil.ATTRIBUTE_CDM_EC_CATEGORY+"]");
                if( "No".equals(strExportYN)){
                	isShowCommand = false;
                }	
            }
            
//            String strOwner  = new DomainObject(strObjectId).getInfo(context, DomainConstants.SELECT_OWNER);
//            String strUser = context.getUser();
//            if(! strOwner.equals(strUser)){
//            	isShowCommand = false;
//            }
                
            
        } catch(Exception e) {
        	e.printStackTrace();
        	throw e;
        }
        return isShowCommand;
    }
    
    public Boolean isModificationEditPart(Context context, String[] args) throws Exception {
        boolean isShowCommand = true;
        try{
            HashMap paramMap = (HashMap)JPO.unpackArgs(args);
            if(true == isShowCommand){
            	String strObjectId = (String)paramMap.get("objectId");
                String strCurrent  = new DomainObject(strObjectId).getInfo(context, DomainConstants.SELECT_CURRENT);
                if(! DomainConstants.STATE_PART_PRELIMINARY.equals(strCurrent)){
                	isShowCommand = false;		
                }
//                String strOwner  = new DomainObject(strObjectId).getInfo(context, DomainConstants.SELECT_OWNER);
//                String strUser = context.getUser();
//                if(! strOwner.equals(strUser)){
//                	isShowCommand = false;
//                }
                
            }
            
        } catch(Exception e) {
        	e.printStackTrace();
        	throw e;
        }
        return isShowCommand;
    }
    
    public Boolean isPartToolbarModificationAllowed(Context context, String[] args) throws Exception {
        boolean isShowCommand = true;
        try{
            HashMap paramMap = (HashMap)JPO.unpackArgs(args);
            isShowCommand = (boolean)JPO.invoke(context, "emxENCActionLinkAccess", null, "isModificationAllowed", JPO.packArgs(paramMap), Boolean.class);
            if(true == isShowCommand){
            	String strObjectId = (String)paramMap.get("objectId");
                String strType     = new DomainObject(strObjectId).getInfo(context, DomainConstants.SELECT_TYPE);	
                
                if(cdmConstantsUtil.TYPE_CDMPHANTOMPART.equals(strType) || cdmConstantsUtil.TYPE_CDMMECHANICALPART.equals(strType)){
                	return isShowCommand;	
                }else{
                	isShowCommand = false;
                }
            }
            
        } catch(Exception e) {
        	e.printStackTrace();
        	throw e;
        }
        return isShowCommand;
    }
    
    public Boolean isElectronicPartModificationAllowed(Context context, String[] args) throws Exception {
        boolean isShowCommand = true;
        try{
            HashMap paramMap = (HashMap)JPO.unpackArgs(args);
            if(true == isShowCommand){
            	String strObjectId = (String)paramMap.get("objectId");
                String strType     = new DomainObject(strObjectId).getInfo(context, DomainConstants.SELECT_TYPE);	
                
                if(cdmConstantsUtil.TYPE_CDM_ELECTRONIC_PART.equals(strType) || cdmConstantsUtil.TYPE_CDM_ELECTRONIC_ASSEMBLY_PART.equals(strType)){
                	return isShowCommand;	
                }else{
                	isShowCommand = false;
                }
            }
            
        } catch(Exception e) {
        	e.printStackTrace();
        	throw e;
        }
        return isShowCommand;
    }
    
    public Boolean isElectronicPartToolbarModificationAllowed(Context context, String[] args) throws Exception {
        boolean isShowCommand = true;
        try{
            HashMap paramMap = (HashMap)JPO.unpackArgs(args);
            System.out.println("paramMap          "+paramMap);
            isShowCommand = (boolean)JPO.invoke(context, "emxENCActionLinkAccess", null, "isModificationAllowed", JPO.packArgs(paramMap), Boolean.class);
            if(true == isShowCommand){
            	String strObjectId = (String)paramMap.get("objectId");
                String strType     = new DomainObject(strObjectId).getInfo(context, DomainConstants.SELECT_TYPE);	
                
                if(!cdmConstantsUtil.TYPE_CDM_ELECTRONIC_ASSEMBLY_PART.equals(strType)){
                	isShowCommand = false;
                }
            }
            
        } catch(Exception e) {
        	e.printStackTrace();
        	throw e;
        }
        return isShowCommand;
    }
    
    public Boolean hasCloneAccess(Context context, String[] args) throws Exception {
        boolean isShowCommand = true;
        try{
            HashMap paramMap = (HashMap)JPO.unpackArgs(args);
            isShowCommand = (boolean)JPO.invoke(context, "emxENCActionLinkAccessBase", null, "hasCloneAccess", JPO.packArgs(paramMap), Boolean.class);
            if(true == isShowCommand){
            	String strObjectId = (String)paramMap.get("objectId");
                String strType     = new DomainObject(strObjectId).getInfo(context, DomainConstants.SELECT_TYPE);	
                
                if(cdmConstantsUtil.TYPE_CDMPHANTOMPART.equals(strType) || cdmConstantsUtil.TYPE_CDMMECHANICALPART.equals(strType)){
                	return isShowCommand;	
                }else{
                	isShowCommand = false;
                }
            }
            
        } catch(Exception e) {
        	e.printStackTrace();
        	throw e;
        }
        return isShowCommand;
    }
    
    
    /**
	 * @desc  "Engineering Spec Edit"
	 */
    @SuppressWarnings("unchecked")
    public String editPartEngineeringSpec(Context context, String[] args) throws Exception{
    	
    	HashMap paramMap = (HashMap) JPO.unpackArgs(args);
    	String objectId = (String)paramMap.get("objectId");
    	
    	try{
    		
    		String strPartFamilyId = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {objectId, "to["+DomainConstants.RELATIONSHIP_CLASSIFIED_ITEM+"].from.id"});
    		
    		if(UIUtil.isNotNullAndNotEmpty(strPartFamilyId)){
    			
    			DomainObject partFamilyObj = new DomainObject(strPartFamilyId);
    	        String strSysInterface = partFamilyObj.getAttributeValue(context, DomainConstants.ATTRIBUTE_MXSYSINTERFACE);

    	        String strMQL             = "print interface $1 select derived dump";
    	        String resStr             = MqlUtil.mqlCommand(context, strMQL, true, strSysInterface);
    	        StringList slResultList   = FrameworkUtil.split(resStr, ",");
    	        
    	        int iResultSize = slResultList.size();
    	        HashMap hmAttributeGroupValues = new HashMap();
    	        
    			if(iResultSize > 0){
    				
    				int iCnt = 0;
    				
    				for(int i=0; i<iResultSize; i++){
    					
    					String strAttributeGroupObjectName = (String)slResultList.get(i);
    					String strExists = MqlUtil.mqlCommand(context, "print bus $1 $2 $3 select $4 dump", new String[] {"cdmAttributeGroupObject", strAttributeGroupObjectName, "-", "exists"});
    					
    					if("TRUE".equals(strExists)){
    						
    						String mqlValue = MqlUtil.mqlCommand(context, "print bus $1 $2 $3 select $4 dump", new String[] {"cdmAttributeGroupObject", strAttributeGroupObjectName, "-", "attribute[cdmAttributeGroupSequence]"});
    				        
    						if(! DomainConstants.EMPTY_STRING.equals(mqlValue)){
    							
    				        	String[] mqlValueArray = mqlValue.split("\\|");
    				        	
        				    	for(int k=0; k<mqlValueArray.length; k++){
        				    		
        				    		String[] resultArray = mqlValueArray[k].split(",");
        				    		String attrName      = resultArray[0];
        				    		String strParamValue = (String)paramMap.get(attrName);
        				    		System.out.println("attrName      : "+attrName);
        				    		System.out.println("strParamValue : "+strParamValue);
        				    		hmAttributeGroupValues.put(attrName, strParamValue);
        				    		
        				    	}	
        				    	
    				        }
    					}
    				}
    			}
    			
    			setEngineeringSpecEditAttributes(context, objectId, hmAttributeGroupValues);
    			
	        }
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    	return objectId;
    }
    
    private void setEngineeringSpecEditAttributes(Context context, String objectId, HashMap hmAttributeGroupValues) throws Exception{
		try{
			DomainObject domObj = new DomainObject(objectId);
			domObj.setAttributeValues(context, hmAttributeGroupValues);
		}catch(Exception e){
			throw e;
		}
	}

    /**
	 * @desc  Part Relation get EC Data
	 */
	public MapList getPartRelatedECsData (Context context, String[] args) throws Exception {
		
		MapList mlPartRelatedECList = new MapList();
		HashMap paramMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String)paramMap.get("objectId");
		
		SelectList selectList = new SelectList(3);
		selectList.addId();
		selectList.addName();
		selectList.addType();
		
		SelectList selectRelList = new SelectList();

		String relation = DomainConstants.RELATIONSHIP_AFFECTED_ITEM;
		String type = cdmConstantsUtil.TYPE_CDMECO + "," + cdmConstantsUtil.TYPE_CDMEAR + "," + cdmConstantsUtil.TYPE_CDMPEO;
		try {
			if(objectId == null || "".equals(objectId)) return new MapList();
			DomainObject domObj = DomainObject.newInstance(context);
			domObj.setId(objectId);
			mlPartRelatedECList = domObj.getRelatedObjects(context, 
					relation,       // relationship
					type,           // type
					selectList,     // objects
					selectRelList,  // relationships
					true,           // to
					false,          // from
					(short)1,       // recurse
					null,           // where
					null,           // relationship where
					(short)0);      // limit
			
			
			mlPartRelatedECList.sort("name", "ascending", "string");
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return mlPartRelatedECList;
    }
    
	/**
	 * @param strTypeBuffer 
	 * @desc   SeriesPart
	 */
    public MapList getSeriesPartData(Context context, String[] args) throws Exception {
		MapList mlSeriesPartList = new MapList();
		HashMap paramMap = (HashMap) JPO.unpackArgs(args);
		String objectId = (String)paramMap.get("objectId");
		
		SelectList selectList = new SelectList(3);
		selectList.addId();
		selectList.addName();
		selectList.addType();
		
		boolean isPhantom = false;
		try {
			String strHighLankPartIds = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump $3", new String[] {objectId, "to["+DomainConstants.RELATIONSHIP_EBOM+"].from.id", "|"}));
			if(strHighLankPartIds.contains("|")){
				String[] strHighLankPartIdArray = strHighLankPartIds.split("\\|");
				for(int k=0; k<strHighLankPartIdArray.length; k++){
					String strHighLankPartId = strHighLankPartIdArray[k];
					String strType = new DomainObject(strHighLankPartId).getTypeName();
					if(cdmConstantsUtil.TYPE_CDMPHANTOMPART.equals(strType)){
						isPhantom = true;
					}
				}
			}else{
				String strType = new DomainObject(strHighLankPartIds).getTypeName();
				if(cdmConstantsUtil.TYPE_CDMPHANTOMPART.equals(strType)){
					isPhantom = true;
				}
			}
			
			if(isPhantom){
				
				if(strHighLankPartIds.contains("|")){
					
					String[] strHighLankPartIdArray = strHighLankPartIds.split("\\|");
					for(int k=0; k<strHighLankPartIdArray.length; k++){
						String strHighLankPartId = strHighLankPartIdArray[k];
						
						String strDrawingNo = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {objectId, cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO});
						StringBuffer strBufferWhere = new StringBuffer();
						strBufferWhere.append(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO);
						strBufferWhere.append(" == ");
						strBufferWhere.append(strDrawingNo);
						MapList mlSeriesList = new DomainObject(strHighLankPartId).getRelatedObjects(context, 
								DomainConstants.RELATIONSHIP_EBOM,   // relationship
								cdmConstantsUtil.TYPE_CDMPART,       // type
								selectList,     					 // objects
								null,  								 // relationships
								false,          					 // to
								true,           					 // from
								(short)1,       					 // recurse
								strBufferWhere.toString(),           // where
								null,           					 // relationship where
								(short)0);      					 // limit					

						mlSeriesPartList.add(mlSeriesList);
						
					}
					mlSeriesPartList.sort("name", "ascending", "string");
					
				}else{
					
					String strDrawingNo = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {objectId, cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO});
					StringBuffer strBufferWhere = new StringBuffer();
					strBufferWhere.append(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO);
					strBufferWhere.append(" == ");
					strBufferWhere.append(strDrawingNo);
					mlSeriesPartList = new DomainObject(strHighLankPartIds).getRelatedObjects(context, 
							DomainConstants.RELATIONSHIP_EBOM,   // relationship
							cdmConstantsUtil.TYPE_CDMPART,       // type
							selectList,     					 // objects
							null,  								 // relationships
							false,          					 // to
							true,           					 // from
							(short)1,       					 // recurse
							strBufferWhere.toString(),           // where
							null,           					 // relationship where
							(short)0);      					 // limit					

					mlSeriesPartList.sort("name", "ascending", "string");
				}
				
			}else{
				
//				String strDrawingNo = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {objectId, cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO});
				DomainObject domObj = new DomainObject(objectId);
				String strDrawingNo = domObj.getInfo(context, cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO);
				String strRevision  = domObj.getInfo(context, DomainConstants.SELECT_REVISION);
				
				String relation = EngineeringConstants.RELATIONSHIP_PART_SPECIFICATION;
				String type = cdmConstantsUtil.TYPE_CDMPART;
				StringBuffer strBufferWhere = new StringBuffer();
				strBufferWhere.append(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO);
				strBufferWhere.append(" == ");
				strBufferWhere.append(strDrawingNo);
				strBufferWhere.append(" && ");
				strBufferWhere.append(DomainConstants.SELECT_REVISION);
				strBufferWhere.append(" == ");
				strBufferWhere.append(strRevision);
			
				if(strDrawingNo == null || "".equals(strDrawingNo)) return new MapList();
				
				mlSeriesPartList = DomainObject.findObjects(context, 
						type,         				                    // type
						DomainConstants.QUERY_WILDCARD,   				// name
						DomainConstants.QUERY_WILDCARD,   				// revision
						DomainConstants.QUERY_WILDCARD,   				// policy
						cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION,     // vault
						strBufferWhere.toString(),             			// where
						DomainConstants.EMPTY_STRING,     				// query
						true,							  				// expand
						selectList,                      				// objects
						(short)0);                        				// limits
				
				mlSeriesPartList.sort("name", "ascending", "string");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return mlSeriesPartList;
    }
    
    /**
	 * @desc  Part Investor (Table setting programHTML)
	 */
    public Vector getInvestorRangeHTML(Context context, String[] args) throws Exception {
		try {
			HashMap programMap = (HashMap) JPO.unpackArgs(args);
			Vector vecResult = new Vector();
			MapList objectList = (MapList) programMap.get("objectList");
			StringBuffer strBuffer = new StringBuffer();
			String strFieldRangeValues = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getSession().getLocale(), "emxEngineeringCentral.Attribute.cdmPartInvestor");
			if(! DomainConstants.EMPTY_STRING.equals(strFieldRangeValues)){
				String[] arrFieldRangeValues = strFieldRangeValues.split(",");
				int objSize = objectList.size();
				for (int k=0; k<objSize; k++) {
					for (int i = 0; i < arrFieldRangeValues.length; i++) {
						strBuffer.append("<td><input type=\"checkbox\" name=\"Investor\" value =\""+arrFieldRangeValues[i]+"\" style=\"height:14px;padding:0px;margin:0px\">\""+arrFieldRangeValues[i]+"\"&nbsp;&nbsp;&nbsp;&nbsp;</input></td>");
					}
					vecResult.add(strBuffer.toString());
				}	
			}
			return vecResult;
		} catch (Exception exp) {
			exp.printStackTrace();
			throw exp;
		}
	}
	
    /**
   	 * @desc  Part Investor (Table setting program)
   	 */
	public HashMap getInvestorRange(Context context, String[] args) throws Exception {
		HashMap rangeMap = new HashMap();
		try {
			String strSelectStringResource = DomainObject.EMPTY_STRING;
			StringList fieldRangeValues = new StringList();
			StringList fieldDisplayRangeValues = new StringList();
			String strFieldRangeValues = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getSession().getLocale(), "emxEngineeringCentral.Attribute.cdmPartInvestor");
			if(! DomainConstants.EMPTY_STRING.equals(strFieldRangeValues)){
				String[] arrFieldRangeValues = strFieldRangeValues.split(",");
				for (int i = 0; i < arrFieldRangeValues.length; i++) {
					fieldRangeValues.addElement(arrFieldRangeValues[i]);
					fieldDisplayRangeValues.addElement(arrFieldRangeValues[i]);
				}	
			}
			rangeMap.put("field_choices", fieldRangeValues);
			rangeMap.put("field_display_choices", fieldDisplayRangeValues);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return rangeMap;
	}
	
	/**
   	 * @desc  Part ApprovalType (Table "setting" "program")
   	 */
	public HashMap getApprovalTypeRange(Context context, String[] args) throws Exception {
		HashMap rangeMap = new HashMap();
		try {
			String strSelectStringResource = DomainObject.EMPTY_STRING;
			StringList fieldRangeValues = new StringList();
			StringList fieldDisplayRangeValues = new StringList();
			HashMap paramMap = (HashMap) JPO.unpackArgs(args);
			Map requestMap = (Map)paramMap.get("requestMap");
			String strLanguage = (String)requestMap.get("languageStr");
			
			AttributeType attrPartApprovalType = new AttributeType(cdmConstantsUtil.ATTRIBUTE_CDM_PART_APPROVAL_TYPE);
		    attrPartApprovalType.open(context);
		    StringList attrPartApprovalTypeList = attrPartApprovalType.getChoices(context);
		    StringList attrPartApprovalTypeKOList = i18nNow.getAttrRangeI18NStringList(cdmConstantsUtil.ATTRIBUTE_CDM_PART_APPROVAL_TYPE, attrPartApprovalTypeList, strLanguage);
		    attrPartApprovalType.close(context);
			
		    int iSize = attrPartApprovalTypeList.size();
			for (int i = 0; i < iSize; i++) {
				fieldRangeValues.addElement(attrPartApprovalTypeList.get(i).toString());
				fieldDisplayRangeValues.addElement(attrPartApprovalTypeKOList.get(i).toString());
			}
			rangeMap.put("field_choices", fieldRangeValues);
			rangeMap.put("field_display_choices", fieldDisplayRangeValues);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return rangeMap;
	}
	
	/**
   	 * @desc  Part PartType (Table "setting" "program")
   	 */
	public HashMap getPartTypeRange(Context context, String[] args) throws Exception {
		HashMap rangeMap = new HashMap();
		try {
			String strSelectStringResource = DomainObject.EMPTY_STRING;
			StringList fieldRangeValues = new StringList();
			StringList fieldDisplayRangeValues = new StringList();
			HashMap paramMap = (HashMap) JPO.unpackArgs(args);
			Map requestMap = (Map)paramMap.get("requestMap");
			String strLanguage = (String)requestMap.get("languageStr");
			
			AttributeType attrPartType = new AttributeType(cdmConstantsUtil.ATTRIBUTE_CDM_PART_TYPE);
		    attrPartType.open(context);
		    //StringList attrPartTypeList = attrPartType.getChoices(context);
		    StringList attrPartTypeList = new StringList();
		    attrPartTypeList.add(0, "singleSupply");
		    attrPartTypeList.add(1, "halfFinishedProduct");
		    attrPartTypeList.add(2, "completeProduct");
		    StringList attrPartTypeLanguageList = i18nNow.getAttrRangeI18NStringList(cdmConstantsUtil.ATTRIBUTE_CDM_PART_TYPE, attrPartTypeList, strLanguage);
		    attrPartType.close(context);
			
		    int iSize = attrPartTypeList.size();
			for (int i = 0; i < iSize; i++) {
				if(! "rawMaterial".equals(attrPartTypeList.get(i).toString())){
					fieldRangeValues.addElement(attrPartTypeList.get(i).toString());
					fieldDisplayRangeValues.addElement(attrPartTypeLanguageList.get(i).toString());
				}
			}
			
			rangeMap.put("field_choices", fieldRangeValues);
			rangeMap.put("field_display_choices", fieldDisplayRangeValues);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return rangeMap;
	}
	
	/**
   	 * @desc  Part Global (Table "setting" "program")
   	 */
	public HashMap getGlobalRange(Context context, String[] args) throws Exception {
		HashMap rangeMap = new HashMap();
		try {
			String strSelectStringResource = DomainObject.EMPTY_STRING;
			StringList fieldRangeValues = new StringList();
			StringList fieldDisplayRangeValues = new StringList();
			HashMap paramMap = (HashMap) JPO.unpackArgs(args);
			Map requestMap = (Map)paramMap.get("requestMap");
			String strLanguage = (String)requestMap.get("languageStr");
			
			AttributeType attrPartGlobal = new AttributeType(cdmConstantsUtil.ATTRIBUTE_CDM_PART_GLOBAL);
		    attrPartGlobal.open(context);
		    StringList attrPartGlobalList = attrPartGlobal.getChoices(context);
		    StringList attrPartGlobalKOList = i18nNow.getAttrRangeI18NStringList(cdmConstantsUtil.ATTRIBUTE_CDM_PART_GLOBAL, attrPartGlobalList, strLanguage);
		    attrPartGlobal.close(context);
			
		    int iSize = attrPartGlobalList.size();
			for (int i = 0; i < iSize; i++) {
				fieldRangeValues.addElement(attrPartGlobalList.get(i).toString());
				fieldDisplayRangeValues.addElement(attrPartGlobalKOList.get(i).toString());
			}
			rangeMap.put("field_choices", fieldRangeValues);
			rangeMap.put("field_display_choices", fieldDisplayRangeValues);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return rangeMap;
	}
	
	/**
   	 * @desc  Part UnitOfMeasure (Table "setting" "program")
   	 */
	public HashMap getUnitOfMeasureRange(Context context, String[] args) throws Exception {
		HashMap rangeMap = new HashMap();
		try {
			String strSelectStringResource = DomainObject.EMPTY_STRING;
			StringList fieldRangeValues = new StringList();
			StringList fieldDisplayRangeValues = new StringList();
			HashMap paramMap = (HashMap) JPO.unpackArgs(args);
			Map requestMap = (Map)paramMap.get("requestMap");
			String strLanguage = (String)requestMap.get("languageStr");
			
			AttributeType attrUnitofMeasure = new AttributeType(cdmConstantsUtil.ATTRIBUTE_CDM_PART_UOM);
		    attrUnitofMeasure.open(context);
		    StringList attrUnitofMeasureList = attrUnitofMeasure.getChoices(context);
		    StringList attrUnitofMeasureKOList = i18nNow.getAttrRangeI18NStringList(cdmConstantsUtil.ATTRIBUTE_CDM_PART_UOM, attrUnitofMeasureList, strLanguage);
		    attrUnitofMeasure.close(context);
			
		    int iSize = attrUnitofMeasureList.size();
			for (int i = 0; i < iSize; i++) {
				fieldRangeValues.addElement(attrUnitofMeasureList.get(i).toString());
				fieldDisplayRangeValues.addElement(attrUnitofMeasureList.get(i).toString());
			}
			rangeMap.put("field_choices", fieldRangeValues);
			rangeMap.put("field_display_choices", fieldDisplayRangeValues);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return rangeMap;
	}
	
	/**
   	 * @desc  Part ItemType (Table "setting" "program")
   	 */
	public HashMap getItemTypeRange(Context context, String[] args) throws Exception {
		HashMap rangeMap = new HashMap();
		try {
			String strSelectStringResource = DomainObject.EMPTY_STRING;
			StringList fieldRangeValues = new StringList();
			StringList fieldDisplayRangeValues = new StringList();
			HashMap paramMap = (HashMap) JPO.unpackArgs(args);
			Map requestMap = (Map)paramMap.get("requestMap");
			String strLanguage = (String)requestMap.get("languageStr");
			
			AttributeType attrPartItemType = new AttributeType(cdmConstantsUtil.ATTRIBUTE_CDM_PART_ITEM_TYPE);
		    attrPartItemType.open(context);
		    StringList attrPartItemTypeList = attrPartItemType.getChoices(context);
		    StringList attrPartItemTypeKOList = i18nNow.getAttrRangeI18NStringList(cdmConstantsUtil.ATTRIBUTE_CDM_PART_ITEM_TYPE, attrPartItemTypeList, strLanguage);
		    attrPartItemType.close(context);
			
		    int iSize = attrPartItemTypeList.size();
			for (int i = 0; i < iSize; i++) {
				fieldRangeValues.addElement(attrPartItemTypeList.get(i).toString());
				fieldDisplayRangeValues.addElement(attrPartItemTypeKOList.get(i).toString());
			}
			rangeMap.put("field_choices", fieldRangeValues);
			rangeMap.put("field_display_choices", fieldDisplayRangeValues);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return rangeMap;
	}
	
	/**
   	 * @desc  Part ERPInterface (Table "setting" "program")
   	 */
	public HashMap getERPInterfaceRange(Context context, String[] args) throws Exception {
		HashMap rangeMap = new HashMap();
		try {
			String strSelectStringResource = DomainObject.EMPTY_STRING;
			StringList fieldRangeValues = new StringList();
			StringList fieldDisplayRangeValues = new StringList();
			HashMap paramMap = (HashMap) JPO.unpackArgs(args);
			Map requestMap = (Map)paramMap.get("requestMap");
			String strLanguage = (String)requestMap.get("languageStr");
			
			AttributeType attrPartERPInterface = new AttributeType(cdmConstantsUtil.ATTRIBUTE_CDM_PART_ERP_INTERFACE);
		    attrPartERPInterface.open(context);
		    StringList attrPartERPInterfaceList = attrPartERPInterface.getChoices(context);
		    StringList attrPartERPInterfaceKOList = i18nNow.getAttrRangeI18NStringList(cdmConstantsUtil.ATTRIBUTE_CDM_PART_ERP_INTERFACE, attrPartERPInterfaceList, strLanguage);
		    attrPartERPInterface.close(context);
			
		    int iSize = attrPartERPInterfaceList.size();
			for (int i = 0; i < iSize; i++) {
				fieldRangeValues.addElement(attrPartERPInterfaceList.get(i).toString());
				fieldDisplayRangeValues.addElement(attrPartERPInterfaceKOList.get(i).toString());
			}
			rangeMap.put("field_choices", fieldRangeValues);
			rangeMap.put("field_display_choices", fieldDisplayRangeValues);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return rangeMap;
	}
	
	/**
   	 * @desc  Part IsCasting (Table "setting" "program")
   	 */
	public HashMap getIsCastingRange(Context context, String[] args) throws Exception {
		HashMap rangeMap = new HashMap();
		try {
			String strSelectStringResource = DomainObject.EMPTY_STRING;
			StringList fieldRangeValues = new StringList();
			StringList fieldDisplayRangeValues = new StringList();
			HashMap paramMap = (HashMap) JPO.unpackArgs(args);
			Map requestMap = (Map)paramMap.get("requestMap");
			String strLanguage = (String)requestMap.get("languageStr");
			
			AttributeType attrPartIsCasting = new AttributeType(cdmConstantsUtil.ATTRIBUTE_CDM_PART_IS_CASTING);
		    attrPartIsCasting.open(context);
		    StringList attrPartIsCastingList = attrPartIsCasting.getChoices(context);
		    StringList attrPartIsCastingKOList = i18nNow.getAttrRangeI18NStringList(cdmConstantsUtil.ATTRIBUTE_CDM_PART_IS_CASTING, attrPartIsCastingList, strLanguage);
		    attrPartIsCasting.close(context);
			
		    int iSize = attrPartIsCastingList.size();
			for (int i = 0; i < iSize; i++) {
				fieldRangeValues.addElement(attrPartIsCastingList.get(i).toString());
				fieldDisplayRangeValues.addElement(attrPartIsCastingKOList.get(i).toString());
			}
			rangeMap.put("field_choices", fieldRangeValues);
			rangeMap.put("field_display_choices", fieldDisplayRangeValues);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return rangeMap;
	}
	
	/**
   	 * @desc  Part Type Range(Table "setting" "program")
   	 */
	public HashMap getTypeRange(Context context, String[] args) throws Exception {
		HashMap rangeMap = new HashMap();
		try {
			String strSelectStringResource = DomainObject.EMPTY_STRING;
			StringList fieldRangeValues = new StringList();
			StringList fieldDisplayRangeValues = new StringList();
			fieldRangeValues.addElement(PropertyUtil.getSchemaProperty(context, "type_cdmMechanicalPart"));
			fieldDisplayRangeValues.addElement(EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getSession().getLocale(), "emxEngineeringCentral.Label.MechanicalPart"));
			fieldRangeValues.addElement(PropertyUtil.getSchemaProperty(context, "type_cdmPhantomPart"));
			fieldDisplayRangeValues.addElement(EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getSession().getLocale(), "emxEngineeringCentral.Label.PhantomPart"));
			
			rangeMap.put("field_choices", fieldRangeValues);
			rangeMap.put("field_display_choices", fieldDisplayRangeValues);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return rangeMap;
	}
	
	/**
   	 * @desc  Part 'Standard BOM' Range(Table "setting" "program")
   	 */
	public HashMap getStandardBOMRange(Context context, String[] args) throws Exception {
		HashMap rangeMap = new HashMap();
		try {
			String strSelectStringResource = DomainObject.EMPTY_STRING;
			StringList fieldRangeValues = new StringList();
			StringList fieldDisplayRangeValues = new StringList();
			fieldRangeValues.addElement("NO");
			fieldDisplayRangeValues.addElement("NO");
			
			rangeMap.put("field_choices", fieldRangeValues);
			rangeMap.put("field_display_choices", fieldDisplayRangeValues);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return rangeMap;
	}
	
	/**
	 * @param context
	 * @throws Exception
	 * add by js.hyun 16.09.12
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Object getChoices(Context context, String[] args) throws Exception {
	    HashMap tempMap = new HashMap();

	    StringList fieldRangeValues = new StringList();
	    StringList fieldDisplayRangeValues = new StringList();

	    fieldRangeValues.addElement("MCA");
	    fieldRangeValues.addElement("MIL");
	    fieldRangeValues.addElement("MMT");
	    
	    fieldDisplayRangeValues.addElement("MCA");
	    fieldDisplayRangeValues.addElement("MIL");
	    fieldDisplayRangeValues.addElement("MMT");
	    
	    tempMap.put("field_choices", fieldRangeValues);
	    tempMap.put("field_display_choices", fieldDisplayRangeValues);
	    return tempMap;
	}
	
	/**
	 * @param context
	 * @param args
	 * @throws Exception
	 * add by js.hyun 16.09.12
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Map createPart2(Context context, String[] args) throws Exception{
    	HashMap paramMap = (HashMap) JPO.unpackArgs(args);
    	Map returnMap = new HashMap();
    	String strType               = (String)paramMap.get("TypeActual");
    	String strPhase              = (String)paramMap.get("Phase");
    	String strPartNo             = (String)paramMap.get("PartNo");
    	String strVehicle            = (String)paramMap.get("Vehicle");
    	String strPartName           = (String)paramMap.get("PartName");
    	String strProject            = (String)paramMap.get("Project");
    	String strApprovalType       = (String)paramMap.get("ApprovalType");
    	String strPartType           = (String)paramMap.get("PartType");
    	String strGlobal             = (String)paramMap.get("Global");
    	String strDrawingNo          = (String)paramMap.get("DrawingNo");
    	String strUnitOfMeasure      = (String)paramMap.get("UnitOfMeasure");
    	String strECONumber          = (String)paramMap.get("ECONumber");
    	String strItemType           = (String)paramMap.get("ItemType");
    	String strOEMItemNumber      = (String)paramMap.get("OEMItemNumber");
    	String strComments 			 = (String)paramMap.get("Comments");
    	String strChangeReason 		 = (String)paramMap.get("ChangeReason");
    	String strOrg1 				 = (String)paramMap.get("Org1");
    	String strOrg2 				 = (String)paramMap.get("Org2");
    	String strOrg3 				 = (String)paramMap.get("Org3");
    	String strProductType 		 = (String)paramMap.get("ProductType");
    	String strERPInterface 		 = (String)paramMap.get("ERPInterface");
    	String strSurface 			 = (String)paramMap.get("Surface");
    	String strEstimatedWeight 	 = (String)paramMap.get("EstimatedWeight");
    	String strMaterial 			 = (String)paramMap.get("Material");
    	String strRealWeight 	     = (String)paramMap.get("RealWeight");
    	String strSize 				 = (String)paramMap.get("Size");
    	String strCADWeight 		 = (String)paramMap.get("CADWeight");
    	String strSurfaceTreatment   = (String)paramMap.get("SurfaceTreatment");
    	String strIsCasting 		 = (String)paramMap.get("IsCasting");
    	String strOption1 			 = (String)paramMap.get("Option1");
    	String strOption2 			 = (String)paramMap.get("Option2");
    	String strOption3 			 = (String)paramMap.get("Option3");
    	String strOption4 			 = (String)paramMap.get("Option4");
    	String strOption5 			 = (String)paramMap.get("Option5");
    	String strOption6 			 = (String)paramMap.get("Option6");
    	String strOptionETC 		 = (String)paramMap.get("OptionETC");
    	String strOptionDescription  = (String)paramMap.get("OptionDescription");
    	String strPublishingTarget 	 = (String)paramMap.get("PublishingTarget");
    	String strInvestor 			 = (String)paramMap.get("Investor");
    	String strProjectType 		 = (String)paramMap.get("ProjectType");
    	String strStandardBOM		 = (String)paramMap.get("StandardBOM");
    	
    	String VehicleObjectId 		 = (String)paramMap.get("VehicleOID");
    	String ProjectObjectId 		 = (String)paramMap.get("ProjectOID");
    	String ProjectTypeObjectId 	 = (String)paramMap.get("ProjectTypeOID");
    	String ProductTypeObjectId 	 = (String)paramMap.get("ProductTypeOID");
    	String Org1ObjectId 		 = (String)paramMap.get("Org1OID");
    	String Org2ObjectId 		 = (String)paramMap.get("Org2OID");
    	String Org3ObjectId 		 = (String)paramMap.get("Org3OID");
    	String Option1ObjectId 		 = (String)paramMap.get("Option1OID");
    	String Option2ObjectId 		 = (String)paramMap.get("Option2OID");
    	String Option3ObjectId 		 = (String)paramMap.get("Option3OID");
    	String Option4ObjectId 		 = (String)paramMap.get("Option4OID");
    	String Option5ObjectId 		 = (String)paramMap.get("Option5OID");
    	String Option6ObjectId 		 = (String)paramMap.get("Option6OID");
    	String bomObjectId   		 = (String)paramMap.get("bomObjectId");
    	String DrawingNoObjectId 	 = (String)paramMap.get("DrawingNoOID");
    	String ECONumberObjectId 	 = (String)paramMap.get("ECONumberOID");
    	
    	String partObjectId = "";
    	HashMap attributes = new HashMap();
    	attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_PHASE             , 	strPhase);                       
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_NAME              ,  strPartName);                    
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_APPROVAL_TYPE     , 	strApprovalType);                
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_TYPE              , 	strPartType);                    
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_GLOBAL            , 	strGlobal);                      
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_UOM               ,  strUnitOfMeasure);               
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_ITEM_TYPE         , 	strItemType);                    
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_OEM_ITEM_NUMBER   , 	strOEMItemNumber);               
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_DESCRIPTION       , 	strComments);                    
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_CHANGE_REASON     , 	strChangeReason);                
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_ERP_INTERFACE     , 	strERPInterface);                
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_SURFACE           , 	strSurface);                     
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_ESTIMATED_WEIGHT  , 	strEstimatedWeight);             
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_MATERIAL          , 	strMaterial);                    
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_REAL_WEIGHT       , 	strRealWeight);                  
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_SIZE              , 	strSize);                        
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_CAD_WEIGHT        , 	strCADWeight);                   
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_SURFACE_TREATMENT , 	strSurfaceTreatment);            
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_IS_CASTING        , 	strIsCasting);                   
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_ETC        , 	strOptionETC);                   
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_DESCRIPTION, 	strOptionDescription);           
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_PUBLISHING_TARGET ,  strPublishingTarget);            
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_INVESTOR          , 	strInvestor); 
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_DRAWING_NO        , 	strDrawingNo); 
        attributes.put(cdmConstantsUtil.ATTRIBUTE_CDM_PART_STANDARD_BOM      , 	strStandardBOM);
        
        try{
        	ContextUtil.startTransaction(context, true);
	        partObjectId = createPartObject(context, attributes, strPhase, strType, strPartNo, DomainConstants.EMPTY_STRING);
	        returnMap.put("id", partObjectId);
	        //partObjectRelationShip(context, partObjectId, VehicleObjectId, ProjectObjectId, ProjectTypeObjectId, ProductTypeObjectId, Org1ObjectId, Org2ObjectId, Org3ObjectId, Option1ObjectId, Option2ObjectId, Option3ObjectId, Option4ObjectId, Option5ObjectId, Option6ObjectId, ECONumberObjectId, DrawingNoObjectId);
	        ContextUtil.commitTransaction(context);
        } catch(Exception e) {
        	e.printStackTrace();
        	ContextUtil.abortTransaction(context);
        }
        System.out.println("returnMap     :     "+returnMap);
    	return returnMap;
    }
	
	/**
   	 * @desc  Part(WebForm) 'Block Code'(Table "setting" "programHTML")
   	 */
	public String getPartNoTextboxForCreateHtmlOutput(Context context, String[] args) throws Exception {
		HashMap hmParamMap = (HashMap) JPO.unpackArgs(args);
        HashMap hmRequestMap = (HashMap) hmParamMap.get("requestMap");
		StringBuffer strBuffer = new StringBuffer();

		strBuffer.append("<input type=\"text\"  name=\"PartNo\" id=\"PartNo\" width=\"30\" class=\"inputSelect\"  value=\"\" ></input>");
		strBuffer.append("<input type=\"hidden\"  name=\"PartNoOID\" id=\"PartNoOID\" value=\"\" ></input>");
		strBuffer.append("<input type=\"button\" value=\"...\" onclick=\"javascript:window.open('../common/cdmPartLibraryChooser.jsp?&amp;header=emxEngineeringCentral.header.PartLibrary&amp;multiSelect=false&amp;ShowIcons=true&amp;searchMode=GeneralLibrary&amp;program=cdmPartLibrary:getPartLibraryFirstNode&amp;expandProgram=cdmPartLibrary:expandPartLibraryLowLankNode&amp;");
		strBuffer.append("StringResourceFileId=emxEngineeringCentralStringResource&amp;displayKey=name&amp;rootNode=PartLibrary&amp;fieldNameActual=PartLibrary&amp;fieldNameDisplay=PartLibrary&amp;fromPage=PartLibraryForm&amp;firstLevelSelect=false&amp;secondLevel=false&amp;processURL=../engineeringcentral/cdmPartChooserFormProcess.jsp', '', 'width=500', 'height=600')\"></input>");
		return strBuffer.toString();
	}
	
	/**
   	 * @desc  Part(WebForm) 'Part Name'(Table "setting" "programHTML")
   	 */
	public String getPartNameTextboxForCreateHtmlOutput(Context context, String[] args) throws Exception {
		HashMap hmParamMap = (HashMap) JPO.unpackArgs(args);
        HashMap hmRequestMap = (HashMap) hmParamMap.get("requestMap");
		StringBuffer strBuffer = new StringBuffer();

		strBuffer.append("<input type=\"text\"  name=\"PartName\" id=\"PartName\" width=\"30\" value =\"\" ></input>");
		return strBuffer.toString();
	}
	
	/**
   	 * @desc  Part(WebForm) ChangeReason(Table "setting" "programHTML")
   	 */
	public String ChangeReason(Context context, String[] args) throws Exception{
    	HashMap hmParamMap = (HashMap) JPO.unpackArgs(args);
        HashMap hmRequestMap = (HashMap) hmParamMap.get("requestMap");
        String strMode = (String) hmRequestMap.get("mode");
        
        StringBuffer sb = new StringBuffer();
        if("view".equals(strMode)){
        	String strObjectId = (String) hmRequestMap.get("objectId");
        	String strChangeReasonAttrValue = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {strObjectId, cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_CHANGE_REASON});
        	sb.append(strChangeReasonAttrValue);
        }else{
        	sb.append("<input value=\"\" id=\"ChangeReason\" name=\"ChangeReason\" fieldlabel=\"Change Reason\"");
        	sb.append(" title=\"Change Reason\" size=\"20\" onkeypress=\"javascript:submitFunction(event)\" type=\"text\"></input>");
        	sb.append("<input type=\"hidden\" name=\"highestFN\" value=\"\"></input>");
        }
    	return sb.toString();
	}
	
	/**
   	 * @desc  Part MarkUp Policy Default Display
   	 */
	public HashMap getDefaultPartPolicyValue(Context context, String[] args) throws Exception {
		HashMap paramMap = (HashMap)JPO.unpackArgs(args);
		HashMap requestMap = (HashMap)paramMap.get("requestMap");
		String languageStr = (String) requestMap.get("languageStr");
		String objectId = (String)requestMap.get("objectId");
	    
	    DomainObject domObj = DomainObject.newInstance(context, objectId);
	    String policy = domObj.getInfo(context, DomainConstants.SELECT_POLICY);
	    
	    String defaultVal = cdmConstantsUtil.TEXT_PRODUCTION;
		HashMap defaultMap = new HashMap();
		
		defaultMap.put("Default_AddNewRow", defaultVal);
		defaultMap.put("Default_AddNewRow_Display", defaultVal);
		
		defaultMap.put("Default_ExistingRow", defaultVal);
	    defaultMap.put("Default_ExistingRow_Display", defaultVal);
		return defaultMap;
	}
	
	/**
   	 * @desc  Part MarkUp Policy Range Data
   	 */
	public HashMap getPartPolicyRange(Context context, String[] args) throws Exception {
		HashMap rangeMap = new HashMap();
		try {
			String strSelectStringResource = DomainObject.EMPTY_STRING;
			StringList fieldRangeValues = new StringList();
			StringList fieldDisplayRangeValues = new StringList();
			HashMap paramMap = (HashMap) JPO.unpackArgs(args);
			Map requestMap = (Map)paramMap.get("requestMap");
			String strLanguage = (String)requestMap.get("languageStr");
			
			String development = cdmConstantsUtil.TEXT_PROTO;
			String production  = cdmConstantsUtil.TEXT_PRODUCTION;
			fieldRangeValues.add(development);
			fieldRangeValues.add(production);
			
			String strDevelopmentPolicy = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", context.getSession().getLocale(), "emxFramework.Policy.cdmPartDevelopmentPolicy");
			String strProductionPolicy  = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", context.getSession().getLocale(), "emxFramework.Policy.cdmPartProductionPolicy");
			fieldDisplayRangeValues.add(strDevelopmentPolicy);
			fieldDisplayRangeValues.add(strProductionPolicy);
			
			rangeMap.put("field_choices", fieldRangeValues);
			rangeMap.put("field_display_choices", fieldDisplayRangeValues);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return rangeMap;
	}
	
	/**
   	 * @desc  Part Edit(MarkUp) Policy - Revision Default Display 
   	 */
	public HashMap getDefaultRevisionOnPolicy (Context context,String[] args)throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
	    HashMap paramMap  = (HashMap)programMap.get("requestMap");
	    String objectId = (String)paramMap.get("objectId");
	    
	    DomainObject domObj = DomainObject.newInstance(context, objectId);
	    String revision = domObj.getInfo(context, DomainConstants.SELECT_REVISION);
	       
	    HashMap defaultMap = new HashMap();
	    defaultMap.put("Default_AddNewRow",revision);
	    defaultMap.put("Default_ExistingRow",revision);
	    return defaultMap;
	}
	
	/**
   	 * @desc  Part Edit(MarkUp) Part Default Display
   	 */
	public HashMap getDefaultPartValue(Context context, String[] args) throws Exception {
		HashMap paramMap = (HashMap)JPO.unpackArgs(args);
		HashMap requestMap = (HashMap)paramMap.get("requestMap");
		String languageStr = (String) requestMap.get("languageStr");

		HashMap defaultMap = new HashMap();
		String defaultVal= PropertyUtil.getSchemaProperty(context,"type_cdmMechanicalPart");
		defaultMap.put("Default_AddNewRow", defaultVal);
	    defaultMap.put("Default_ExistingRow", defaultVal);
	    
	    String strType = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", context.getSession().getLocale(), "emxFramework.Type.cdmMechanicalPart");
	    defaultMap.put("Default_AddNewRow_Display", strType);
	    defaultMap.put("Default_ExistingRow_Display", strType);
	    return defaultMap;
	}
	
	/**
   	 * @desc  Part Edit(MarkUp) Vehicle Data Update
   	 */
	public Boolean updateVehicle(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap)JPO.unpackArgs(args);
		Map requestMap = (Map)programMap.get("requestMap");
		String strLanguage = (String)requestMap.get("languageStr");
	    HashMap paramMap = (HashMap)programMap.get("paramMap");
	    String newRDValue = (String)paramMap.get("New Value");
	    String objectId = (String)paramMap.get("objectId");
	    DomainObject domObj = new DomainObject(objectId);
	    String title = cdmStringUtil.browserCommonCodeLanguage(strLanguage);
	    
	    try{
	    	StringList relVehicleIdList = domObj.getInfoList(context, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_VEHICLE+"].id");
			if(relVehicleIdList.size() > 0 && ! "".equals(relVehicleIdList)){
				for(int k=0; k<relVehicleIdList.size(); k++){
					DomainRelationship.disconnect(context, relVehicleIdList.get(k).toString());
				}
			}
		    if(newRDValue.contains("|")){
				String[] objectIdArray = newRDValue.split("\\|");
				StringBuffer strBuffer = new StringBuffer();
				for(int i=0; i<objectIdArray.length; i++){
					String strVehicleId = objectIdArray[i];
					if(!"".equals(newRDValue)){
						DomainRelationship.connect(context, new DomainObject(strVehicleId), cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_VEHICLE, domObj);
					}
				}
		    }else{
			    if(!"".equals(newRDValue)){
					DomainRelationship.connect(context, new DomainObject(newRDValue), cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_VEHICLE, domObj);
				}
		    }
	    }catch(Exception e){
	    	throw e;
	    }
	    return Boolean.TRUE;
	}
	
	/**
   	 * @desc  Part Edit(MarkUp) Project Data Update
   	 */
	public Boolean updateProject(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap)JPO.unpackArgs(args);
		Map requestMap = (Map)programMap.get("requestMap");
		String strLanguage = (String)requestMap.get("languageStr");
	    HashMap paramMap = (HashMap)programMap.get("paramMap");
	    String newRDValue = (String)paramMap.get("New Value");
	    String objectId = (String)paramMap.get("objectId");
	    DomainObject domObj = new DomainObject(objectId);
	    String title = cdmStringUtil.browserCommonCodeLanguage(strLanguage);
	    
	    DomainObject newDomObj = new DomainObject(newRDValue);
	    String relProjectId = domObj.getInfo(context, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PROJECT+"].id");
		if(relProjectId != null && !"".equals(relProjectId)){
			DomainRelationship.disconnect(context, relProjectId);
		}
		DomainRelationship.connect(context, newDomObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PROJECT, domObj);

		return Boolean.TRUE;
	}
	
	/**
   	 * @desc  Part Edit(MarkUp) ProjectType Data Update
   	 */
	public Boolean updateProjectType(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap)JPO.unpackArgs(args);
		Map requestMap = (Map)programMap.get("requestMap");
		String strLanguage = (String)requestMap.get("languageStr");
	    HashMap paramMap = (HashMap)programMap.get("paramMap");
	    String newRDValue = (String)paramMap.get("New Value");
	    String objectId = (String)paramMap.get("objectId");
	    DomainObject domObj = new DomainObject(objectId);
	    String title = cdmStringUtil.browserCommonCodeLanguage(strLanguage);
	    
	    DomainObject newDomObj = new DomainObject(newRDValue);
	    String relProjectTypeId = domObj.getInfo(context, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PROJECT_TYPE+"].id");
		if(relProjectTypeId != null && !"".equals(relProjectTypeId)){
			DomainRelationship.disconnect(context, relProjectTypeId);
		}
		DomainRelationship.connect(context, newDomObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PROJECT_TYPE, domObj);
		
	    return Boolean.TRUE;
	}
	
	/**
   	 * @desc  Part Edit(MarkUp) ProductType Data Update
   	 */
	public Boolean updateProductType(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap)JPO.unpackArgs(args);
		Map requestMap = (Map)programMap.get("requestMap");
		String strLanguage = (String)requestMap.get("languageStr");
	    HashMap paramMap = (HashMap)programMap.get("paramMap");
	    String newRDValue = (String)paramMap.get("New Value");
	    String objectId = (String)paramMap.get("objectId");
	    DomainObject domObj = new DomainObject(objectId);
	    String title = cdmStringUtil.browserCommonCodeLanguage(strLanguage);
	    
	    DomainObject newDomObj = new DomainObject(newRDValue);
	    String relProductTypeId = domObj.getInfo(context, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PRODUCT_TYPE+"].id");
		if(relProductTypeId != null && !"".equals(relProductTypeId)){
			DomainRelationship.disconnect(context, relProductTypeId);
		}
		DomainRelationship.connect(context, newDomObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PRODUCT_TYPE, domObj);
	    
	    return Boolean.TRUE;
	}
	
	/**
   	 * @desc  Part Edit(MarkUp) Org1 Data Update
   	 */
	public Boolean updateOrg1(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap)JPO.unpackArgs(args);
		Map requestMap = (Map)programMap.get("requestMap");
		String strLanguage = (String)requestMap.get("languageStr");
	    HashMap paramMap = (HashMap)programMap.get("paramMap");
	    String newRDValue = (String)paramMap.get("New Value");
	    String objectId = (String)paramMap.get("objectId");
	    DomainObject domObj = new DomainObject(objectId);
	    String title = cdmStringUtil.browserCommonCodeLanguage(strLanguage);
	    
	    DomainObject newDomObj = new DomainObject(newRDValue);
	    updateColumnDisConnectDomainRelationship(context, domObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_ORG, cdmConstantsUtil.ATTRIBUTE_CDM_PART_ORG_REL_ATTRIBUTE, "Org1");
		updateColumnDomainRelationship(context, newDomObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_ORG, domObj, cdmConstantsUtil.ATTRIBUTE_CDM_PART_ORG_REL_ATTRIBUTE, "Org1");
	    
		return Boolean.TRUE;
	}
	
	/**
   	 * @desc  Part Edit(MarkUp) Org2 Data Update
   	 */
	public Boolean updateOrg2(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap)JPO.unpackArgs(args);
		Map requestMap = (Map)programMap.get("requestMap");
		String strLanguage = (String)requestMap.get("languageStr");
	    HashMap paramMap = (HashMap)programMap.get("paramMap");
	    String newRDValue = (String)paramMap.get("New Value");
	    String objectId = (String)paramMap.get("objectId");
	    DomainObject domObj = new DomainObject(objectId);
	    String title = cdmStringUtil.browserCommonCodeLanguage(strLanguage);
	    
	    DomainObject newDomObj = new DomainObject(newRDValue);
	    updateColumnDisConnectDomainRelationship(context, domObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_ORG, cdmConstantsUtil.ATTRIBUTE_CDM_PART_ORG_REL_ATTRIBUTE, "Org2");
		updateColumnDomainRelationship(context, newDomObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_ORG, domObj, cdmConstantsUtil.ATTRIBUTE_CDM_PART_ORG_REL_ATTRIBUTE, "Org2");
		
	    return Boolean.TRUE;
	}

	/**
   	 * @desc  Part Edit(MarkUp) Org3 Data Update
   	 */
	public Boolean updateOrg3(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap)JPO.unpackArgs(args);
		Map requestMap = (Map)programMap.get("requestMap");
		String strLanguage = (String)requestMap.get("languageStr");
	    HashMap paramMap = (HashMap)programMap.get("paramMap");
	    String newRDValue = (String)paramMap.get("New Value");
	    String objectId = (String)paramMap.get("objectId");
	    DomainObject domObj = new DomainObject(objectId);
	    String title = cdmStringUtil.browserCommonCodeLanguage(strLanguage);
	    
	    DomainObject newDomObj = new DomainObject(newRDValue);
	    updateColumnDisConnectDomainRelationship(context, domObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_ORG, cdmConstantsUtil.ATTRIBUTE_CDM_PART_ORG_REL_ATTRIBUTE, "Org3");
		updateColumnDomainRelationship(context, newDomObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_ORG, domObj, cdmConstantsUtil.ATTRIBUTE_CDM_PART_ORG_REL_ATTRIBUTE, "Org3");
		
	    return Boolean.TRUE;
	}
	
	/**
   	 * @desc  Part Edit(MarkUp) Relationship Update
   	 */
	private void updateColumnDomainRelationship(Context context, DomainObject newDomObj, String strRel, DomainObject domObj, String relAttr, String relAttrValue) throws Exception{
		try{
			DomainRelationship.connect(context, newDomObj, strRel, domObj);
			String relId = domObj.getInfo(context, "to["+strRel+"].id");
			DomainRelationship.setAttributeValue(context, relId, relAttr, relAttrValue);
		}catch(Exception e){
			throw e;
		}
	}
	
	/**
   	 * @desc  Part Edit(MarkUp) Option1 Update
   	 */
	public Boolean updateOption1(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap)JPO.unpackArgs(args);
		Map requestMap = (Map)programMap.get("requestMap");
		String strLanguage = (String)requestMap.get("languageStr");
	    HashMap paramMap = (HashMap)programMap.get("paramMap");
	    String newRDValue = (String)paramMap.get("New Value");
	    String objectId = (String)paramMap.get("objectId");
	    DomainObject domObj = new DomainObject(objectId);
	    String title = cdmStringUtil.browserCommonCodeLanguage(strLanguage);
	    
	    DomainObject newDomObj = new DomainObject(newRDValue);
		updateColumnDisConnectDomainRelationship(context, domObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option1");
		updateColumnDomainRelationship(context, newDomObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, domObj, cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option1");
	    
	    return Boolean.TRUE;
	}
	
	/**
   	 * @desc  Part Edit(MarkUp) Option2 Update
   	 */
	public Boolean updateOption2(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap)JPO.unpackArgs(args);
		Map requestMap = (Map)programMap.get("requestMap");
		String strLanguage = (String)requestMap.get("languageStr");
	    HashMap paramMap = (HashMap)programMap.get("paramMap");
	    String newRDValue = (String)paramMap.get("New Value");
	    String objectId = (String)paramMap.get("objectId");
	    DomainObject domObj = new DomainObject(objectId);
	    String title = cdmStringUtil.browserCommonCodeLanguage(strLanguage);
	    
	    DomainObject newDomObj = new DomainObject(newRDValue);
		updateColumnDisConnectDomainRelationship(context, domObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option2");
		updateColumnDomainRelationship(context, newDomObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, domObj, cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option2");
	    
	    return Boolean.TRUE;
	}
	
	/**
   	 * @desc  Part Edit(MarkUp) Option3 Update
   	 */
	public Boolean updateOption3(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap)JPO.unpackArgs(args);
		Map requestMap = (Map)programMap.get("requestMap");
		String strLanguage = (String)requestMap.get("languageStr");
	    HashMap paramMap = (HashMap)programMap.get("paramMap");
	    String newRDValue = (String)paramMap.get("New Value");
	    String objectId = (String)paramMap.get("objectId");
	    DomainObject domObj = new DomainObject(objectId);
	    String title = cdmStringUtil.browserCommonCodeLanguage(strLanguage);
	    
	    DomainObject newDomObj = new DomainObject(newRDValue);
	    updateColumnDisConnectDomainRelationship(context, domObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option3");
		updateColumnDomainRelationship(context, newDomObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, domObj, cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option3");
	    
	    return Boolean.TRUE;
	}
	
	/**
   	 * @desc  Part Edit(MarkUp) Option4 Update
   	 */
	public Boolean updateOption4(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap)JPO.unpackArgs(args);
		Map requestMap = (Map)programMap.get("requestMap");
		String strLanguage = (String)requestMap.get("languageStr");
	    HashMap paramMap = (HashMap)programMap.get("paramMap");
	    String newRDValue = (String)paramMap.get("New Value");
	    String objectId = (String)paramMap.get("objectId");
	    DomainObject domObj = new DomainObject(objectId);
	    String title = cdmStringUtil.browserCommonCodeLanguage(strLanguage);
	    
	    DomainObject newDomObj = new DomainObject(newRDValue);
	    updateColumnDisConnectDomainRelationship(context, domObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option4");
		updateColumnDomainRelationship(context, newDomObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, domObj, cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option4");
	    
	    return Boolean.TRUE;
	}
	
	/**
   	 * @desc  Part Edit(MarkUp) Option5 Update
   	 */
	public Boolean updateOption5(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap)JPO.unpackArgs(args);
		Map requestMap = (Map)programMap.get("requestMap");
		String strLanguage = (String)requestMap.get("languageStr");
	    HashMap paramMap = (HashMap)programMap.get("paramMap");
	    String newRDValue = (String)paramMap.get("New Value");
	    String objectId = (String)paramMap.get("objectId");
	    DomainObject domObj = new DomainObject(objectId);
	    String title = cdmStringUtil.browserCommonCodeLanguage(strLanguage);
	    
	    DomainObject newDomObj = new DomainObject(newRDValue);
	    updateColumnDisConnectDomainRelationship(context, domObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option5");
		updateColumnDomainRelationship(context, newDomObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, domObj, cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option5");
	    
	    return Boolean.TRUE;
	}
	
	/**
   	 * @desc  Part Edit(MarkUp) Option6 Update
   	 */
	public Boolean updateOption6(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap)JPO.unpackArgs(args);
		Map requestMap = (Map)programMap.get("requestMap");
		String strLanguage = (String)requestMap.get("languageStr");
	    HashMap paramMap = (HashMap)programMap.get("paramMap");
	    String newRDValue = (String)paramMap.get("New Value");
	    String objectId = (String)paramMap.get("objectId");
	    DomainObject domObj = new DomainObject(objectId);
	    String title = cdmStringUtil.browserCommonCodeLanguage(strLanguage);
	    DomainObject newDomObj = new DomainObject(newRDValue);
	    
		updateColumnDisConnectDomainRelationship(context, domObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option6");
		updateColumnDomainRelationship(context, newDomObj, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, domObj, cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option6");
	    
	    return Boolean.TRUE;
	}
	
	/**
   	 * @desc  Part Edit(MarkUp) DrawingNo Update
   	 */
	public Boolean updateDrawingNo(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap)JPO.unpackArgs(args);
		Map requestMap = (Map)programMap.get("requestMap");
		String strLanguage = (String)requestMap.get("languageStr");
	    HashMap paramMap = (HashMap)programMap.get("paramMap");
	    String newRDValue = (String)paramMap.get("New Value");
	    String objectId = (String)paramMap.get("objectId");
	    DomainObject domObj = new DomainObject(objectId);
	    String title = cdmStringUtil.browserCommonCodeLanguage(strLanguage);
	    
	    try{
	    	DomainObject newDomObj = new DomainObject(newRDValue);
	    	String relDrawingId = domObj.getInfo(context, "from["+DomainConstants.RELATIONSHIP_PART_SPECIFICATION+"].id");
			if(relDrawingId != null && !"".equals(relDrawingId)){
				DomainRelationship.disconnect(context, relDrawingId);
			}
			DomainRelationship.connect(context, domObj, DomainConstants.RELATIONSHIP_PART_SPECIFICATION, new DomainObject(relDrawingId));
	    }catch(Exception e){
	    	e.printStackTrace();
	    	throw e;
	    }
		
		return Boolean.TRUE;
	}
	
	/**
   	 * @desc  Part Edit(MarkUp) EC Update
   	 */
	public Boolean updateECONumber(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap)JPO.unpackArgs(args);
		Map requestMap = (Map)programMap.get("requestMap");
		String strLanguage = (String)requestMap.get("languageStr");
	    HashMap paramMap = (HashMap)programMap.get("paramMap");
	    String newRDValue = (String)paramMap.get("New Value");
	    String objectId = (String)paramMap.get("objectId");
	    DomainObject domObj = new DomainObject(objectId);
	    String title = cdmStringUtil.browserCommonCodeLanguage(strLanguage);
	    
	    try{
	    	
	    	DomainObject newDomObj = new DomainObject(newRDValue);
		    String relECId = domObj.getInfo(context, "to["+DomainConstants.RELATIONSHIP_AFFECTED_ITEM+"].id");
			if(relECId != null && !"".equals(relECId)){
				DomainRelationship.disconnect(context, relECId);
			}
			DomainRelationship.connect(context, newDomObj, DomainConstants.RELATIONSHIP_AFFECTED_ITEM, domObj);
			
	    }catch(Exception e){
	    	e.printStackTrace();
	    	throw e;
	    }
		
		return Boolean.TRUE;
	}
	
	/**
   	 * @desc  Part Edit(MarkUp) Relationship DisConnect
   	 */
	private void updateColumnDisConnectDomainRelationship(Context context, DomainObject domObj, String strRel, String strRelAttr, String strRelAttrValue) throws Exception{
		try{
			String relId = domObj.getInfo(context, "to["+strRel+"].id");
		    String relAttrValue = domObj.getInfo(context, "to["+strRel+"].attribute["+strRelAttr+"].value");
		    
			if(relId != null && !"".equals(relId) && strRelAttrValue.equals(relAttrValue)){
				DomainRelationship.disconnect(context, relId);
			}
		}catch (Exception e){
			throw e;
		}
	}
	
	/**
   	 * @desc  Part Edit(MarkUp) PartType Update
   	 */
	public Boolean updatePartType(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap)JPO.unpackArgs(args);
		Map requestMap = (Map)programMap.get("requestMap");
		String strLanguage = (String)requestMap.get("languageStr");
	    HashMap paramMap = (HashMap)programMap.get("paramMap");
	    String strNewValue = (String)paramMap.get("New Value");
	    String strObjectId = (String)paramMap.get("objectId");
	    try{
	    	DomainObject domObj = new DomainObject(strObjectId);
	    	domObj.setAttributeValue(context, cdmConstantsUtil.ATTRIBUTE_CDM_PART_TYPE, strNewValue);
	    	return Boolean.TRUE;
	    }catch(Exception e){
	    	e.printStackTrace();
	    	return Boolean.FALSE;
	    }
	    
	}

	/**
   	 * @desc  Part Edit(MarkUp) Owner Default Display
   	 */
	public HashMap getDefaultOwner(Context context, String[] args) throws Exception {
		HashMap paramMap = (HashMap)JPO.unpackArgs(args);
		HashMap requestMap = (HashMap)paramMap.get("requestMap");
		String languageStr = (String) requestMap.get("languageStr");
		String user = context.getUser();

		HashMap defaultMap = new HashMap();
		defaultMap.put("Default_AddNewRow", user);
	    //defaultMap.put("Default_ExistingRow", user);
	    defaultMap.put("Default_AddNewRow_Display", user);
	    //defaultMap.put("Default_ExistingRow_Display", user);
	    return defaultMap;
	}
	
	/**
   	 * @desc  Part Edit(MarkUp) UOM Default Display
   	 */
	public HashMap getDefaultUOM (Context context,String[] args) throws Exception {
	       
		HashMap defaultMap = new HashMap();
	    defaultMap.put("Default_AddNewRow", "Each");
	    defaultMap.put("Default_AddNewRow_Display", "Each");
	     
	    return defaultMap;
	}
	
	/**
   	 * @desc  Part EngineeringSpec Tab Display
   	 */
	public Boolean isEngineeringSpec(Context context, String []args) throws Exception {
		HashMap paramMap = (HashMap)JPO.unpackArgs(args);
		String objectId = (String)paramMap.get("objectId");
		DomainObject domObj = new DomainObject(objectId);
		String strPartFamilyId = domObj.getInfo(context, "to["+EngineeringConstants.RELATIONSHIP_CLASSIFIED_ITEM+"].from.id");
		
		if(UIUtil.isNullOrEmpty(strPartFamilyId)){
			return Boolean.FALSE;
		}else{
			return Boolean.TRUE;
		}
    }
	
	/**
   	 * @desc  BOM Lookup Default Vehicle
   	 */
	public Vector lookupDefaultVehicle(Context context, String[] args) throws Exception {
long time0	= System.currentTimeMillis();
        Vector columnValues = new Vector();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objList = (MapList) programMap.get("objectList");
            String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
            
/*************************************************
*	Start for performance 20170306
*************************************************/
			String bArr [] = new String[objList.size()];
			for (int i = 0; i < objList.size(); i++)
			{
				// Get Business object Id
            	Map map = (Map)objList.get(i);
				bArr [i] = (String)map.get(DomainConstants.SELECT_ID);
			}
			StringList bSel = new StringList();
			bSel.add("to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_VEHICLE+"].from."+displayTitle);
			BusinessObjectWithSelectList bwsl = BusinessObject.getSelectBusinessObjectData(context, bArr, bSel);
			
            for(int k=0; k<bwsl.size(); k++){				
               StringList slPartRelVehicleList = bwsl.getElement(k).getSelectDataList("to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_VEHICLE+"].from."+displayTitle);
    		    if(slPartRelVehicleList != null && slPartRelVehicleList.size() > 0){
    		    	StringBuffer strBuffer = new StringBuffer();
    		    	for(int i=0; i<slPartRelVehicleList.size(); i++){
    		    		String strVehicle = (String)slPartRelVehicleList.get(i);
    		    		strBuffer.append(strVehicle);
    		    	    if(i != slPartRelVehicleList.size()-1){
    		    	    	strBuffer.append(",");
    		    	    }
    		    	}
    		    	columnValues.add(strBuffer.toString());
    		    }else{
    		    	columnValues.add(DomainConstants.EMPTY_STRING);
    		    }
			}
/*			
            for(int k=0; k<objList.size(); k++){
            	Map map = (Map)objList.get(k);
            	String strId = (String)map.get(DomainConstants.SELECT_ID);
            	String strName = (String)map.get("Name");
            	
            	DomainObject partObj = new DomainObject(strId);
                StringList slPartRelVehicleList = partObj.getInfoList(context, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_VEHICLE+"].from."+displayTitle);
    		    if(slPartRelVehicleList.size() > 0){
    		    	StringBuffer strBuffer = new StringBuffer();
    		    	for(int i=0; i<slPartRelVehicleList.size(); i++){
    		    		String strVehicle = (String)slPartRelVehicleList.get(i);
    		    		strBuffer.append(strVehicle);
    		    	    if(i != slPartRelVehicleList.size()-1){
    		    	    	strBuffer.append(",");
    		    	    }
    		    	}
    		    	columnValues.add(strBuffer.toString());
    		    }else{
    		    	columnValues.add(DomainConstants.EMPTY_STRING);
    		    }
            }
*/
/*************************************************
*	End for performance 20170306
*************************************************/
        }catch(Exception e){
        	throw e;
        }
long time1	= System.currentTimeMillis();
System.out.println(">>>lookupDefaultVehicle time1="+(time1-time0)+"("+(time1-time0)+")");
        return columnValues;
	}
	
	/**
   	 * @desc  BOM Lookup Default Project
   	 */
	public Vector lookupDefaultProject(Context context, String[] args) throws Exception {
long time0	= System.currentTimeMillis();
        Vector columnValues = new Vector();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objList = (MapList) programMap.get("objectList");
            String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
            
            for(int k=0; k<objList.size(); k++){
            	Map map = (Map) objList.get(k);
            	String strId = (String) map.get(DomainConstants.SELECT_ID);
            	String strProject = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {strId, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PROJECT+"].from."+cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PROJECT_CODE});
            	columnValues.add(strProject);
            }
            
        }catch(Exception e){
        	throw e;
        }
long time1	= System.currentTimeMillis();
System.out.println(">>>lookupDefaultProject time1="+(time1-time0)+"("+(time1-time0)+")");
        return columnValues;
	}
	
	/**
   	 * @desc  EO "Create Date"
   	 */
	public Vector getEOCreateDate(Context context, String[] args) throws Exception {
        Vector columnValues = new Vector();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objList = (MapList) programMap.get("objectList");
            
            for(int k=0; k<objList.size(); k++){
            	Map map = (Map) objList.get(k);
            	String strEOId = (String) map.get(DomainConstants.SELECT_ID);
            	
            	String strEOCreateDate = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {strEOId, "attribute["+cdmConstantsUtil.ATTRIBUTE_CDM_EC_CREATE_DATE+"]"});
            	String sDate = DomainConstants.EMPTY_STRING;
            	if(! "".equals(strEOCreateDate)){
            		DateFormat sdFormat = new SimpleDateFormat("yyyy-MM-dd");
                	Date to = sdFormat.parse(strEOCreateDate);
                    sDate = sdFormat.format(to);
            	}
            	columnValues.add(sDate);
            }
            
        }catch(Exception e){
        	throw e;
        }
        return columnValues;
	}
	
	/**
   	 * @desc  EO "Create Owner"
   	 */
	public Vector getEOCreateOwner(Context context, String[] args) throws Exception {
        Vector columnValues = new Vector();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objList = (MapList) programMap.get("objectList");
            
            for(int k=0; k<objList.size(); k++){
            	Map map = (Map) objList.get(k);
            	String strEOId = (String) map.get(DomainConstants.SELECT_ID);
            	String strEOCreateOwner = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {strEOId, "owner"});
            	String strEOCreator = cdmOwnerRolesUtil.getUserFullName(context, strEOCreateOwner);
            	
            	columnValues.add(strEOCreator);
            }
            
        }catch(Exception e){
        	throw e;
        }
        return columnValues;
	}
	
	/**
   	 * @desc  BOM Lookup Default Product Division
   	 */
	public Vector lookupDefaultProjectType(Context context, String[] args) throws Exception {
        Vector columnValues = new Vector();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objList = (MapList) programMap.get("objectList");
            String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
            
            for(int k=0; k<objList.size(); k++){
            	Map map = (Map) objList.get(k);
            	String strId = (String) map.get(DomainConstants.SELECT_ID);
            	String strProductDivision = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {strId, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PROJECT_TYPE+"].from."+displayTitle});
            	columnValues.add(strProductDivision);
            }
            
        }catch(Exception e){
        	throw e;
        }
        return columnValues;
	}
	
	/**
   	 * @desc  BOM Lookup Default ProductType
   	 */
	public Vector lookupDefaultProductType(Context context, String[] args) throws Exception {
        Vector columnValues = new Vector();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objList = (MapList) programMap.get("objectList");
            String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
            
            for(int k=0; k<objList.size(); k++){
            	Map map = (Map) objList.get(k);
            	String strId = (String) map.get(DomainConstants.SELECT_ID);
            	String strProductType = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {strId, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PRODUCT_TYPE+"].from."+displayTitle});
            	columnValues.add(strProductType);
            }
            
        }catch(Exception e){
        	throw e;
        }
        return columnValues;
	}
	
	/**
   	 * @desc  BOM Lookup Default Org1
   	 */
	public Vector lookupDefaultOrg1(Context context, String[] args) throws Exception {
        Vector columnValues = new Vector();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objList = (MapList) programMap.get("objectList");
            String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
            
            for(int k=0; k<objList.size(); k++){
            	Map map = (Map) objList.get(k);
            	String strId = (String) map.get(DomainConstants.SELECT_ID);
            	String strOrg1 = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {strId, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_ORG+"||"+cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_ORG_REL_ATTRIBUTE+" == 'Org1'].from."+displayTitle});
            	columnValues.add(strOrg1);
            }
            
        }catch(Exception e){
        	throw e;
        }
        return columnValues;
	}
	
	/**
   	 * @desc  BOM Lookup Default Org2
   	 */
	public Vector lookupDefaultOrg2(Context context, String[] args) throws Exception {
        Vector columnValues = new Vector();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objList = (MapList) programMap.get("objectList");
            String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
            
            for(int k=0; k<objList.size(); k++){
            	Map map = (Map) objList.get(k);
            	String strId = (String) map.get(DomainConstants.SELECT_ID);
            	String strOrg2 = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {strId, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_ORG+"||"+cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_ORG_REL_ATTRIBUTE+" == 'Org2'].from."+displayTitle});
            	columnValues.add(strOrg2);
            }
            
        }catch(Exception e){
        	throw e;
        }
        return columnValues;
	}
	
	/**
   	 * @desc  BOM Lookup Default Org3
   	 */
	public Vector lookupDefaultOrg3(Context context, String[] args) throws Exception {
        Vector columnValues = new Vector();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objList = (MapList) programMap.get("objectList");
            String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
            
            for(int k=0; k<objList.size(); k++){
            	Map map = (Map) objList.get(k);
            	String strId = (String) map.get(DomainConstants.SELECT_ID);
            	String strOrg3 = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {strId, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_ORG+"||"+cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_ORG_REL_ATTRIBUTE+" == 'Org3'].from."+displayTitle});
            	columnValues.add(strOrg3);
            }
            
        }catch(Exception e){
        	throw e;
        }
        return columnValues;
	}
	
	/**
   	 * @desc  BOM Lookup Default Option1
   	 */
	public Vector lookupDefaultOption1(Context context, String[] args) throws Exception {
        Vector columnValues = new Vector();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objList = (MapList) programMap.get("objectList");
            String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
            
            for(int k=0; k<objList.size(); k++){
            	Map map = (Map) objList.get(k);
            	String strId = (String) map.get(DomainConstants.SELECT_ID);
            	String strOption1 = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {strId, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION+"||"+cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE+" == 'Option1'].from."+displayTitle});
            	columnValues.add(strOption1);
            }
            
        }catch(Exception e){
        	throw e;
        }
        return columnValues;
	}
	
	/**
   	 * @desc  BOM Lookup Default Option2
   	 */
	public Vector lookupDefaultOption2(Context context, String[] args) throws Exception {
        Vector columnValues = new Vector();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objList = (MapList) programMap.get("objectList");
            String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
            
            for(int k=0; k<objList.size(); k++){
            	Map map = (Map) objList.get(k);
            	String strId = (String) map.get(DomainConstants.SELECT_ID);
            	String strOption2 = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {strId, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION+"||"+cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE+" == 'Option2'].from."+displayTitle});
            	columnValues.add(strOption2);
            }
            
        }catch(Exception e){
        	throw e;
        }
        return columnValues;
	}
	
	/**
   	 * @desc  BOM Lookup Default Option3
   	 */
	public Vector lookupDefaultOption3(Context context, String[] args) throws Exception {
        Vector columnValues = new Vector();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objList = (MapList) programMap.get("objectList");
            String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
            
            for(int k=0; k<objList.size(); k++){
            	Map map = (Map) objList.get(k);
            	String strId = (String) map.get(DomainConstants.SELECT_ID);
            	String strOption3 = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {strId, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION+"||"+cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE+" == 'Option3'].from."+displayTitle});
            	columnValues.add(strOption3);
            }
            
        }catch(Exception e){
        	throw e;
        }
        return columnValues;
	}
	
	/**
   	 * @desc  BOM Lookup Default Option4
   	 */
	public Vector lookupDefaultOption4(Context context, String[] args) throws Exception {
        Vector columnValues = new Vector();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objList = (MapList) programMap.get("objectList");
            String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
            
            for(int k=0; k<objList.size(); k++){
            	Map map = (Map) objList.get(k);
            	String strId = (String) map.get(DomainConstants.SELECT_ID);
            	String strOption4 = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {strId, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION+"||"+cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE+" == 'Option4'].from."+displayTitle});
            	columnValues.add(strOption4);
            }
            
        }catch(Exception e){
        	throw e;
        }
        return columnValues;
	}
	
	/**
   	 * @desc  BOM Lookup Default Option5
   	 */
	public Vector lookupDefaultOption5(Context context, String[] args) throws Exception {
        Vector columnValues = new Vector();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objList = (MapList) programMap.get("objectList");
            String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
            
            for(int k=0; k<objList.size(); k++){
            	Map map = (Map) objList.get(k);
            	String strId = (String) map.get(DomainConstants.SELECT_ID);
            	String strOption5 = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {strId, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION+"||"+cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE+" == 'Option5'].from."+displayTitle});
            	columnValues.add(strOption5);
            }
            
        }catch(Exception e){
        	throw e;
        }
        return columnValues;
	}
	
	/**
   	 * @desc  BOM Lookup Default Option6
   	 */
	public Vector lookupDefaultOption6(Context context, String[] args) throws Exception {
        Vector columnValues = new Vector();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objList = (MapList) programMap.get("objectList");
            String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
            
            for(int k=0; k<objList.size(); k++){
            	Map map = (Map) objList.get(k);
            	String strId = (String) map.get(DomainConstants.SELECT_ID);
            	String strOption6 = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {strId, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION+"||"+cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE+" == 'Option6'].from."+displayTitle});
            	columnValues.add(strOption6);
            }
            
        }catch(Exception e){
        	throw e;
        }
        return columnValues;
	}
	
	/**
   	 * @desc  BOM ApplyPartList Table Value
   	 */
	public Vector getApplyPartListValue(Context context, String[] args) throws Exception {
        Vector columnValues = new Vector();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objList = (MapList) programMap.get("objectList");
            
            for(int k=0; k<objList.size(); k++){
            	Map map = (Map) objList.get(k);
            	StringBuffer strBuffer = new StringBuffer();
            	String strId = (String) map.get(DomainConstants.SELECT_ID);
            	DomainObject domObj = new DomainObject(strId);
            	String strApplyPartListValue = domObj.getInfo(context, cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_APPLY_PART_LIST);
            	String strApplySizeListValue = domObj.getInfo(context, cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_APPLY_SIZE_LIST);
            	String strApplySurfaceTreatmentListValue = domObj.getInfo(context, cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_APPLY_SURFACE_TREATMENT_LIST);

            	if("true".equals(strApplyPartListValue)){
            		strBuffer.append("Part");
            		strBuffer.append(",");
            	}
            	if("true".equals(strApplySizeListValue)){
            		strBuffer.append("Size");
            		strBuffer.append(",");
            	}
            	if("true".equals(strApplySurfaceTreatmentListValue)){
            		strBuffer.append("Surface Treatment");
            		strBuffer.append(",");
            	}
            	
            	if(strBuffer.toString().length() == 0){
            		columnValues.add(strBuffer.toString());
            	}else{
            		columnValues.add(strBuffer.toString().substring(0, strBuffer.toString().lastIndexOf(",") ));
            	}
            }
            
        }catch(Exception e){
        	throw e;
        }
        return columnValues;
	}
	
	/**
   	 * @desc  Part Edit(MarkUp) Project Data Update
   	 */
	public Boolean updateApplyPartList(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap)JPO.unpackArgs(args);
		Map requestMap = (Map)programMap.get("requestMap");
		String strLanguage = (String)requestMap.get("languageStr");
	    HashMap paramMap = (HashMap)programMap.get("paramMap");
	    String newValue = (String)paramMap.get("New Value");
	    String objectId = (String)paramMap.get("objectId");
	    
	    DomainObject domObj = new DomainObject(objectId);
	    if(! DomainConstants.EMPTY_STRING.equals(newValue)){
	    	String[] newValueArray = newValue.split(",");
		    for(int i=0; i<newValueArray.length; i++){
		    	String strApplyPartListNewValue = newValueArray[i];
		    	
		    	if("Part".equals(strApplyPartListNewValue)){
		    		domObj.setAttributeValue(context, cdmConstantsUtil.ATTRIBUTE_CDM_PART_APPLY_PART_LIST, "true");
		    	}else if("Size".equals(strApplyPartListNewValue)){
		    		domObj.setAttributeValue(context, cdmConstantsUtil.ATTRIBUTE_CDM_PART_APPLY_SIZE_LIST, "true");
		    	}else if("Surface Treatment".equals(strApplyPartListNewValue)){
		    		domObj.setAttributeValue(context, cdmConstantsUtil.ATTRIBUTE_CDM_PART_APPLY_SURFACE_TREATMENT_LIST, "true");
		    	}
		    }
	    }
	    
		return Boolean.TRUE;
	}
	
	/**
   	 * @desc  Part ApplyPartList (Table setting Range Program)
   	 */
	public HashMap getApplyPartListRange(Context context, String[] args) throws Exception {
		HashMap rangeMap = new HashMap();
		try {
			String strSelectStringResource = DomainObject.EMPTY_STRING;
			StringList fieldRangeValues = new StringList();
			StringList fieldDisplayRangeValues = new StringList();
			String strFieldRangeValues = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getSession().getLocale(), "emxEngineeringCentral.Attribute.cdmApplyPartList");
			if(! DomainConstants.EMPTY_STRING.equals(strFieldRangeValues)){
				String[] arrFieldRangeValues = strFieldRangeValues.split(",");
				for (int i = 0; i < arrFieldRangeValues.length; i++) {
					fieldRangeValues.addElement(arrFieldRangeValues[i]);
					fieldDisplayRangeValues.addElement(arrFieldRangeValues[i]);
				}
		    }
			rangeMap.put("field_choices", fieldRangeValues);
			rangeMap.put("field_display_choices", fieldDisplayRangeValues);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return rangeMap;
	}
	
	
	/**
   	 * @desc  cdmPart Form Vehicle Field Value
   	 */
	public String getPartVehicleForm(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap paramMap = (HashMap) programMap.get("paramMap");
		String objectId = cdmStringUtil.setEmpty((String)paramMap.get("objectId"));
		String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
		String strVehicle = "";
		if(! "".equals(objectId)){
			DomainObject partObj = new DomainObject(objectId);
            StringList slPartRelVehicleList = partObj.getInfoList(context, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_VEHICLE+"].from."+displayTitle);
		    if(slPartRelVehicleList.size() > 0){
		    	StringBuffer strBuffer = new StringBuffer();
		    	for(int i=0; i<slPartRelVehicleList.size(); i++){
		    		String vehicle = (String)slPartRelVehicleList.get(i);
		    		strBuffer.append(vehicle);
		    	    if(i != slPartRelVehicleList.size()-1){
		    	    	strBuffer.append(",");
		    	    }
		    	}
		    	strVehicle = strBuffer.toString();
		    }
		}
		return strVehicle;
	}
	
	/**
   	 * @desc  cdmPart Form Project Field Value
   	 */
	public String getPartProjectForm(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap paramMap = (HashMap) programMap.get("paramMap");
		String objectId = cdmStringUtil.setEmpty((String)paramMap.get("objectId"));
		String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
		String strProject = DomainConstants.EMPTY_STRING;
		
		try{
			if(! DomainConstants.EMPTY_STRING.equals(objectId)){
				strProject = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {objectId, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PROJECT+"].from."+cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PROJECT_CODE});
			}
		}catch(Exception e){
			throw e;
		}
		return strProject;
	}
	
	/**
   	 * @desc  cdmPart Form Product Division Field Value
   	 */
	public String getPartProjectTypeForm(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap paramMap = (HashMap) programMap.get("paramMap");
		String objectId = cdmStringUtil.setEmpty((String)paramMap.get("objectId"));
		String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
		String strProjectType = DomainConstants.EMPTY_STRING;
		
		try{
			if(! DomainConstants.EMPTY_STRING.equals(objectId)){
				strProjectType = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {objectId, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PROJECT_TYPE+"].from."+displayTitle});
			}
		}catch(Exception e){
			throw e;
		}
		return strProjectType;
	}
	
	/**
   	 * @desc  cdmPart Form ProductType Field Value
   	 */
	public String getPartProductTypeForm (Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap paramMap = (HashMap) programMap.get("paramMap");
		String objectId = cdmStringUtil.setEmpty((String)paramMap.get("objectId"));
		String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
		String strProductType = DomainConstants.EMPTY_STRING;
		
		try{
			if(! DomainConstants.EMPTY_STRING.equals(objectId)){
				strProductType = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {objectId, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PRODUCT_TYPE+"].from."+displayTitle});
			}
		}catch(Exception e){
			throw e;
		}
		return strProductType;
	}
	
	/**
   	 * @desc  cdmPart Form Org1 Field Value
   	 */
	public String getPartOrg1Form(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap paramMap = (HashMap) programMap.get("paramMap");
		String objectId = cdmStringUtil.setEmpty((String)paramMap.get("objectId"));
		String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
		String strPartOrg1 = DomainConstants.EMPTY_STRING;
		
		try{
			if(! DomainConstants.EMPTY_STRING.equals(objectId)){
				strPartOrg1 = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {objectId, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_ORG+"||"+cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_ORG_REL_ATTRIBUTE+" == 'Org1'].from."+displayTitle});	
			}
		}catch(Exception e){
			throw e;
		}
		return strPartOrg1;
	}
	
	/**
   	 * @desc  cdmPart Form Org2 Field Value
   	 */
	public String getPartOrg2Form(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap paramMap = (HashMap) programMap.get("paramMap");
		String objectId = cdmStringUtil.setEmpty((String)paramMap.get("objectId"));
		String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
		String strPartOrg2 = DomainConstants.EMPTY_STRING;
		
		try{
			if(! DomainConstants.EMPTY_STRING.equals(objectId)){
				strPartOrg2 = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {objectId, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_ORG+"||"+cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_ORG_REL_ATTRIBUTE+" == 'Org2'].from."+displayTitle});
			}
		}catch(Exception e){
			throw e;
		}
		return strPartOrg2;
	}
	
	/**
   	 * @desc  cdmPart Form Org3 Field Value
   	 */
	public String getPartOrg3Form(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap paramMap = (HashMap) programMap.get("paramMap");
		String objectId = cdmStringUtil.setEmpty((String)paramMap.get("objectId"));
		String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
		String strPartOrg3 = DomainConstants.EMPTY_STRING;
		
		try{
			if(! DomainConstants.EMPTY_STRING.equals(objectId)){
				strPartOrg3 = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {objectId, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_ORG+"||"+cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_ORG_REL_ATTRIBUTE+" == 'Org3'].from."+displayTitle});
			}
		}catch(Exception e){
			throw e;
		}
		return strPartOrg3;
	}
	
	/**
   	 * @desc  cdmPart Form Option1 Field Value
   	 */
	public String getPartOption1Form(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap paramMap = (HashMap) programMap.get("paramMap");
		String objectId = cdmStringUtil.setEmpty((String)paramMap.get("objectId"));
		String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
		String strPartOption1 = DomainConstants.EMPTY_STRING;
		
		try{
			if(! DomainConstants.EMPTY_STRING.equals(objectId)){
				strPartOption1 = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {objectId, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION+"||"+cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE+" == 'Option1'].from."+displayTitle});
			}
		}catch(Exception e){
			throw e;
		}
		return strPartOption1;
	}
	
	/**
   	 * @desc  cdmPart Form Option2 Field Value
   	 */
	public String getPartOption2Form(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap paramMap = (HashMap) programMap.get("paramMap");
		String objectId = cdmStringUtil.setEmpty((String)paramMap.get("objectId"));
		String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
		String strPartOption2 = DomainConstants.EMPTY_STRING;
		
		try{
			if(! DomainConstants.EMPTY_STRING.equals(objectId)){
				strPartOption2 = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {objectId, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION+"||"+cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE+" == 'Option2'].from."+displayTitle});
			}
		}catch(Exception e){
			throw e;
		}
		return strPartOption2;
	}
	
	/**
   	 * @desc  cdmPart Form Option3 Field Value
   	 */
	public String getPartOption3Form(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap paramMap = (HashMap) programMap.get("paramMap");
		String objectId = cdmStringUtil.setEmpty((String)paramMap.get("objectId"));
		String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
		String strPartOption3 = DomainConstants.EMPTY_STRING;
		
		try{
			if(! DomainConstants.EMPTY_STRING.equals(objectId)){
				strPartOption3 = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {objectId, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION+"||"+cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE+" == 'Option3'].from."+displayTitle});
			}
		}catch(Exception e){
			throw e;
		}
		return strPartOption3;
	}
	
	/**
   	 * @desc  cdmPart Form Option4 Field Value
   	 */
	public String getPartOption4Form(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap paramMap = (HashMap) programMap.get("paramMap");
		String objectId = cdmStringUtil.setEmpty((String)paramMap.get("objectId"));
		String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
		String strPartOption4 = DomainConstants.EMPTY_STRING;
		
		try{
			if(! DomainConstants.EMPTY_STRING.equals(objectId)){
				strPartOption4 = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {objectId, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION+"||"+cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE+" == 'Option4'].from."+displayTitle});
			}
		}catch(Exception e){
			throw e;
		}
		return strPartOption4;
	}
	
	/**
   	 * @desc  cdmPart Form Option5 Field Value
   	 */
	public String getPartOption5Form(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap paramMap = (HashMap) programMap.get("paramMap");
		String objectId = cdmStringUtil.setEmpty((String)paramMap.get("objectId"));
		String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
		String strPartOption5 = DomainConstants.EMPTY_STRING;
		
		try{
			if(! DomainConstants.EMPTY_STRING.equals(objectId)){
				strPartOption5 = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {objectId, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION+"||"+cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE+" == 'Option5'].from."+displayTitle});
			}
		}catch(Exception e){
			throw e;
		}
		return strPartOption5;
	}
	
	/**
   	 * @desc  cdmPart Form Option6 Field Value
   	 */
	public String getPartOption6Form(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap paramMap = (HashMap) programMap.get("paramMap");
		String objectId = cdmStringUtil.setEmpty((String)paramMap.get("objectId"));
		String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
		String strPartOption6 = DomainConstants.EMPTY_STRING;
		
		try{
			if(! DomainConstants.EMPTY_STRING.equals(objectId)){
				strPartOption6 = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {objectId, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION+"||"+cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE+" == 'Option6'].from."+displayTitle});
			}
		}catch(Exception e){
			throw e;
		}
		return strPartOption6;
	}
	
	/**
   	 * @desc  cdmPart Form Field Update
   	 */
	public Object updateAddPartForm(Context context, String[] args)  throws Exception {
		HashMap hmProgramMap = (HashMap) JPO.unpackArgs(args);
  	    HashMap hmRequestMap = (HashMap) hmProgramMap.get("requestMap");
  	    HashMap hmFieldMap   = (HashMap) hmProgramMap.get("fieldMap");
  	    String strFieldName  = (String) hmFieldMap.get(DomainConstants.SELECT_NAME);
  	    
  	    HashMap hmParamMap = (HashMap) hmProgramMap.get("paramMap");
  	    String strLanguage = (String) hmParamMap.get("languageStr");
  	    String strObjectId = (String) hmParamMap.get("objectId");
  	    String strNewValue = (String) hmParamMap.get("New Value");
  	    String strNewOid   = cdmStringUtil.setEmpty((String) hmParamMap.get("New OID")); 
  	    
  	    try{
  	    	if("Vehicle".equals(strFieldName)){
  	    		if(strNewOid.contains(",")){
  					String[] objectIdArray = strNewOid.split(",");
  					StringBuffer strBuffer = new StringBuffer();
  					for(int i=0; i<objectIdArray.length; i++){
  						String strVehicleId = objectIdArray[i];
  						DomainRelationship.connect(context, new DomainObject(strVehicleId), cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_VEHICLE, new DomainObject(strObjectId));
  					}
  			    }else{
  				    if(!"".equals(strNewOid)){
  						DomainRelationship.connect(context, new DomainObject(strNewOid), cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_VEHICLE, new DomainObject(strObjectId));
  					}
  			    }
  	    	}else if("Project".equals(strFieldName)){
  	    		createPartConnectDomainRelationship(context, strNewOid, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PROJECT, new DomainObject(strObjectId));
  	    	}else if("ProjectType".equals(strFieldName)){
  	    		createPartConnectDomainRelationship(context, strNewOid, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PROJECT_TYPE, new DomainObject(strObjectId));
  	    	}else if("ProductType".equals(strFieldName)){
  	    		createPartConnectDomainRelationship(context, strNewOid, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PRODUCT_TYPE, new DomainObject(strObjectId));
  	    	}else if("Org1".equals(strFieldName)){
  	    		createPartConnectDomainRelationship(context, strNewOid, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_ORG, new DomainObject(strObjectId), cdmConstantsUtil.ATTRIBUTE_CDM_PART_ORG_REL_ATTRIBUTE, "Org1");
  	    	}else if("Org2".equals(strFieldName)){
  	    		if(! DomainConstants.EMPTY_STRING.equals(strNewOid)){
  	    			createPartConnectDomainRelationship(context, strNewOid, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_ORG, new DomainObject(strObjectId), cdmConstantsUtil.ATTRIBUTE_CDM_PART_ORG_REL_ATTRIBUTE, "Org2");
  	    		}
  	    	}else if("Org3".equals(strFieldName)){
  	    		if(! DomainConstants.EMPTY_STRING.equals(strNewOid)){
  	    		    createPartConnectDomainRelationship(context, strNewOid, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_ORG, new DomainObject(strObjectId), cdmConstantsUtil.ATTRIBUTE_CDM_PART_ORG_REL_ATTRIBUTE, "Org3");
  	    		}
  	    	}else if("Option1".equals(strFieldName)){
  	    		if(! DomainConstants.EMPTY_STRING.equals(strNewOid)){
  	    			createPartConnectDomainRelationship(context, strNewOid, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, new DomainObject(strObjectId), cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option1");
  	    		}
  	    	}else if("Option2".equals(strFieldName)){
  	    		if(! DomainConstants.EMPTY_STRING.equals(strNewOid)){
  	    			createPartConnectDomainRelationship(context, strNewOid, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, new DomainObject(strObjectId), cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option2");
  	    		}
  	    	}else if("Option3".equals(strFieldName)){
  	    		if(! DomainConstants.EMPTY_STRING.equals(strNewOid)){
  	    			createPartConnectDomainRelationship(context, strNewOid, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, new DomainObject(strObjectId), cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option3");
  	    		}
  	    	}else if("Option4".equals(strFieldName)){
  	    		if(! DomainConstants.EMPTY_STRING.equals(strNewOid)){
  	    			createPartConnectDomainRelationship(context, strNewOid, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, new DomainObject(strObjectId), cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option4");
  	    		}
  	    	}else if("Option5".equals(strFieldName)){
  	    		if(! DomainConstants.EMPTY_STRING.equals(strNewOid)){
  	    			createPartConnectDomainRelationship(context, strNewOid, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, new DomainObject(strObjectId), cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option5");
  	    		}
  	    	}else if("Option6".equals(strFieldName)){
  	    		if(! DomainConstants.EMPTY_STRING.equals(strNewOid)){
  	    			createPartConnectDomainRelationship(context, strNewOid, cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_OPTION, new DomainObject(strObjectId), cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_REL_ATTRIBUTE, "Option6");
  	    		}
  	    	}else if("DrawingNo".equals(strFieldName)){
  	    		if(! DomainConstants.EMPTY_STRING.equals(strNewOid)){
  	          		String relDrawingId = new DomainObject(strObjectId).getInfo(context, "from["+DomainConstants.RELATIONSHIP_PART_SPECIFICATION+"].id");
  	      			if(relDrawingId != null && !"".equals(relDrawingId)){
  	      				DomainRelationship.disconnect(context, relDrawingId);
  	      			}
  	      			try{
  	      				ContextUtil.pushContext(context, null, null, null);
  	      				MqlUtil.mqlCommand(context, "trigger off", new String[]{});
  	      				DomainRelationship.connect(context, new DomainObject(strObjectId), DomainConstants.RELATIONSHIP_PART_SPECIFICATION, new DomainObject(strNewOid));
  	      			}catch(Exception e){
  	      				throw e;
  	      			}finally{
  	      				MqlUtil.mqlCommand(context, "trigger on", new String[]{});
  	      				ContextUtil.popContext(context);
  	      			}
  	    		}
  	    	}else if("ECONumber".equals(strFieldName)){
  	    		if(! DomainConstants.EMPTY_STRING.equals(strNewOid)){
  	    			try{
  	        			createPartConnectDomainRelationship(context, strNewOid, DomainConstants.RELATIONSHIP_AFFECTED_ITEM, new DomainObject(strObjectId));
  	        		}catch(Exception e){
  	    				throw e;
  	    			}finally{
  	    				
  	    			}
  	    		}
  	    	}
  	    	
		}catch(Exception e){
			throw e;
		}
  	    return Boolean.TRUE;
    }
	
    public StringList excludeConnectedObjectsEC(Context context, String[] args) throws Exception {
        Map programMap = (Map) JPO.unpackArgs(args);
        StringList slexcludeConnectedObjectsECList = new StringList();
        
        SelectList selectList = new SelectList();
        selectList.addId();
        selectList.addOwner();
        
        StringBuffer strTypeBuffer = new StringBuffer();
        strTypeBuffer.append(cdmConstantsUtil.TYPE_CDMECO).append(",");
        strTypeBuffer.append(cdmConstantsUtil.TYPE_CDMEAR).append(",");
        strTypeBuffer.append(cdmConstantsUtil.TYPE_CDMDCR).append(",");
        strTypeBuffer.append(cdmConstantsUtil.TYPE_CDMPEO);
        
        String strUser = context.getUser();
        StringBuffer strWhereBuffer = new StringBuffer();
        strWhereBuffer.append(strUser);
        strWhereBuffer.append(" != 'owner'");
        
        MapList mlList = DomainObject.findObjects(context,
        											strTypeBuffer.toString(),
        											DomainConstants.QUERY_WILDCARD,
        											strWhereBuffer.toString(),
        											selectList);
        
        for(Object objMap:mlList){
        	Map objectMap = (Map)objMap;
        	String strObjectId = (String)objectMap.get(DomainConstants.SELECT_ID);
        	slexcludeConnectedObjectsECList.add(strObjectId);
        }
        return slexcludeConnectedObjectsECList;
    }
    
	public StringList excludeRecursiveOIDAddExisting(Context context, String args[]) throws Exception {
		
		Map programMap = (Map) JPO.unpackArgs(args);
		String strObjectId = (String)programMap.get("objectId");
        StringList slExcludeObjectsPartList = new StringList();
        slExcludeObjectsPartList.add(strObjectId);
//        SelectList selectList = new SelectList();
//        selectList.addId();
//        selectList.addOwner();
        
//        StringBuffer strTypeBuffer = new StringBuffer();
//        strTypeBuffer.append(cdmConstantsUtil.TYPE_CDMPART);
        
//        String strUser = context.getUser();
//        StringBuffer strWhereBuffer = new StringBuffer();
//        strWhereBuffer.append(strUser);
//        strWhereBuffer.append(" == 'owner'");
        
//        MapList mlList = DomainObject.findObjects(context,
//        											strTypeBuffer.toString(),
//        											DomainConstants.QUERY_WILDCARD,
//        											strWhereBuffer.toString(),
//        											selectList);
//        
//        for(Object objMap:mlList){
//        	Map objectMap = (Map)objMap;
//        	String strObjectId = (String)objectMap.get(DomainConstants.SELECT_ID);
//        	slExcludeObjectsPartList.add(strObjectId);
//        }
        return slExcludeObjectsPartList;
		
	}
	
	
	public StringList excludeSearchObjectsEC(Context context, String[] args) throws Exception {
        Map programMap = (Map) JPO.unpackArgs(args);
        StringList slexcludeConnectedObjectsECList = new StringList();
        
        SelectList selectList = new SelectList();
        selectList.addId();
        selectList.addOwner();
        
        StringBuffer strTypeBuffer = new StringBuffer();
        strTypeBuffer.append(cdmConstantsUtil.TYPE_CDMECO).append(",");
        strTypeBuffer.append(cdmConstantsUtil.TYPE_CDMEAR).append(",");
        strTypeBuffer.append(cdmConstantsUtil.TYPE_CDMPEO);
        
        String strUser = context.getUser();
        StringBuffer strWhereBuffer = new StringBuffer();
        strWhereBuffer.append(strUser);
        strWhereBuffer.append(" != 'owner'");
        
        MapList mlList = DomainObject.findObjects(context,
        											strTypeBuffer.toString(),
        											DomainConstants.QUERY_WILDCARD,
        											strWhereBuffer.toString(),
        											selectList);
        
        for(Object objMap:mlList){
        	Map objectMap = (Map)objMap;
        	String strObjectId = (String)objectMap.get(DomainConstants.SELECT_ID);
        	slexcludeConnectedObjectsECList.add(strObjectId);
        }
        return slexcludeConnectedObjectsECList;
    }
	
	
	public StringList getIncludeProjectGroup(Context context, String[] args) throws Exception {
		StringList objectList = new StringList();
		try {
			HashMap paramMap = (HashMap) JPO.unpackArgs(args);
			
			String strObjectId 	  = (String) paramMap.get("objectId");
			String strTextSearch  = (String) paramMap.get("txtTextSearch");
			
			StringList objectSelects = new StringList();
			objectSelects.add(DomainConstants.SELECT_ID);
			objectSelects.add(DomainConstants.SELECT_NAME);
			objectSelects.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PROJECT_CODE);

			StringBuffer strTypeBuffer = new StringBuffer();
			strTypeBuffer.append(cdmConstantsUtil.TYPE_CDM_PROJECT_GROUP_OBJECT).append(",");
			strTypeBuffer.append(cdmConstantsUtil.TYPE_CDM_SUB_PROJECT_GROUP);
			
			StringBuffer strWhereBuffer = new StringBuffer();
			strWhereBuffer.append("attribute[cdmProjectCode] ~~ '");
			strWhereBuffer.append(strTextSearch);
			strWhereBuffer.append("'");
			
			
			MapList mlProjectGroupList = DomainObject.findObjects(context, strTypeBuffer.toString(), "*", strWhereBuffer.toString(), objectSelects);
			
			for (int i = 0; i < mlProjectGroupList.size(); i++) {
				Map tempMap = (Map) mlProjectGroupList.get(i);
				strObjectId = (String) tempMap.get("id");
				objectList.add(strObjectId);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return objectList;
	}
	
	
	
	public StringList excludePreviousDrawing (Context context, String args[]) throws Exception {
		StringList slExcludeObjectsPartList = new StringList();
		try{
			Map programMap = (Map) JPO.unpackArgs(args);
			String strObjectId = (String)programMap.get("objectId");
			String strPreviousId = StringUtils.trimToEmpty(new DomainObject(strObjectId).getInfo(context, "previous.id"));
			if(! DomainConstants.EMPTY_STRING.equals(strPreviousId)){
				String strPreviousDrawingId = StringUtils.trimToEmpty(new DomainObject(strPreviousId).getInfo(context, "from["+DomainConstants.RELATIONSHIP_PART_SPECIFICATION+"].to.id"));
				if(! DomainConstants.EMPTY_STRING.equals(strPreviousDrawingId)){
					slExcludeObjectsPartList.add(strPreviousDrawingId);
				}
			}	
		}catch(Exception e){
			e.printStackTrace();
			throw e;
		}
        return slExcludeObjectsPartList;
	}
	
	
	/* This method is used to diaplay range values(latest,latest released) for revision column*/
    public HashMap getRevisions(Context context, String[] args) throws Exception {
        //IR-040860 - Starts
        HashMap map = (HashMap)JPO.unpackArgs(args);
        HashMap paramMap = (HashMap)map.get("paramMap");
        String languageStr  = (String) paramMap.get("languageStr");
        //IR-040860 - Ends
    	HashMap revMap = new HashMap();
        //IR-040860 - Starts
        String latestReleased = EngineeringUtil.i18nStringNow(context,"emxEngineeringCentral.Part.WhereUsedRevisionLatestReleased",languageStr);
        String latest = EngineeringUtil.i18nStringNow(context,"emxEngineeringCentral.EBOMManualAddExisting.RevisionOption.Latest",languageStr);
        StringList fieldRangeValues = new StringList(EngineeringUtil.i18nStringNow(context,"emxEngineeringCentral.Part.WhereUsedRevisionLatestReleased","en"));
        StringList fieldDisplayRangeValues = new StringList(latestReleased);//Hardcode
        fieldRangeValues.add(EngineeringUtil.i18nStringNow(context,"emxEngineeringCentral.EBOMManualAddExisting.RevisionOption.Latest","en"));
        //IR-040860 - Ends
		fieldDisplayRangeValues.add(latest);
		revMap.put("field_choices", fieldRangeValues);
		revMap.put("field_display_choices", fieldDisplayRangeValues);

		return revMap;
	}
    
    public Boolean editPartOptionDetails(Context context, String[] args) throws Exception {
        boolean isOptionDetailSuccess = true;
        try{
            HashMap paramMap = (HashMap)JPO.unpackArgs(args);
            String strObjectId = (String)paramMap.get("objectId");
            DomainObject domObj = new DomainObject(strObjectId);
            String strOptionSize = StringUtils.trimToEmpty((String)paramMap.get("optionSize"));

            if(! DomainConstants.EMPTY_STRING.equals(strOptionSize)){
            	int iOptionSize = Integer.parseInt(strOptionSize);
                for(int i=0; i<iOptionSize; i++){
                	String strOptionDetailValue = (String)paramMap.get("OptionDetailValue"+String.valueOf(i));
                	domObj.setAttributeValue(context, "cdmPartOption"+String.valueOf(i+1), strOptionDetailValue);
                }
            }
            
        } catch(Exception e) {
        	e.printStackTrace();
        	isOptionDetailSuccess = false;
        	throw e;
        } finally {
        	return isOptionDetailSuccess;
        }
        
    }
    
	
//    public String exportBOMExcelData (Context context, String[] args) throws Exception {
//    	
//    	String returnValue = "";
//    	try {
//    		HashMap programMap = (HashMap) JPO.unpackArgs(args);
//    		MapList objectList = (MapList) programMap.get("objectList");
//    		int iObjectList = objectList.size();
//    		
//			String[] objectIds = new String[iObjectList];
//			int iCount = 1;
//			List partLevelList = new ArrayList();
//			String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
//			for(int i = 0; i < objectList.size(); i++) {
//    			Map objInfo = new HashMap();
//    			Map map = (Map)objectList.get(i);
//    			String strLevel            = cdmStringUtil.setEmpty((String)map.get("level"));
//    			String strPartId           = cdmStringUtil.setEmpty((String)map.get(DomainConstants.SELECT_ID));
//    			String strPartNo           = cdmStringUtil.setEmpty((String)map.get(DomainConstants.SELECT_NAME));
//    			String strPartRevision     = cdmStringUtil.setEmpty((String)map.get(DomainConstants.SELECT_REVISION));
//    			String strPartName         = cdmStringUtil.setEmpty((String)map.get(cdmConstantsUtil.ATTRIBUTE_CDM_PART_NAME));
//    			String strPartProductType  = cdmStringUtil.setEmpty((String)map.get("to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PRODUCT_TYPE+"].from."+displayTitle));
//    			String strPartProjectType  = cdmStringUtil.setEmpty((String)map.get("to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PROJECT_TYPE+"].from."+displayTitle));
//    			String strPartProject      = cdmStringUtil.setEmpty((String)map.get("to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PROJECT+"].from."+cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PROJECT_CODE));
//    			StringList slVehicleRelIdList = new DomainObject(strPartId).getInfoList(context, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_VEHICLE+"].from."+displayTitle);
//            	StringBuffer strVehicleBuffer = new StringBuffer();
//            	if(slVehicleRelIdList.size()>0){
//            		for (int h=0; h<slVehicleRelIdList.size(); h++) {
//            			strVehicleBuffer.append(slVehicleRelIdList.get(h));
//            			if(slVehicleRelIdList.size()-1 != h){
//            				strVehicleBuffer.append(",");	
//            			}
//        			}	
//            	}
//            	   			
//    			objInfo.put("No", iCount);
//    			objInfo.put("PartNo", strPartNo);
//    			objInfo.put("PartRev", strPartRevision);
//    			objInfo.put("PartName", strPartName);
//    			objInfo.put("PartProductType", strPartProductType);
//    			objInfo.put("PartProjectType", strPartProjectType);
//    			objInfo.put("PartProject", strPartProject);
//    			objInfo.put("PartVehicle", strVehicleBuffer.toString());
//    			objInfo.put("Level_"+strLevel, strLevel);
//
//				partLevelList.add(objInfo);
//				iCount++;
//    		}
//			Date today = new Date();
//			SimpleDateFormat formatter = new SimpleDateFormat("yyMMdd_HHmmss");
//
//			String tempPath = (String) programMap.get("tempPath");
//			String fileName = (String) programMap.get("fileName");
//			
//			String tempFile = "BOM_LEVEL_EXPORT.xlsx";
//			returnValue = fileName + "_" + formatter.format(today) + ".xlsx";
//			
//			Map<String, Object> mapBeans = new HashMap<String, Object>();
//			mapBeans.put("part", partLevelList);
//			
//			// Excel Export
//			net.sf.jxls.transformer.XLSTransformer transformer = new net.sf.jxls.transformer.XLSTransformer();
//			transformer.transformXLS(tempPath + File.separator + tempFile, mapBeans, returnValue);
//    	} catch (Exception e) {
//    		e.printStackTrace();
//    		throw e;
//    	}
//    	return returnValue;
//    }
	
    
    
    public int partRelationCADObjectTrigger(Context context, String[] args) throws Exception {
		
    	SqlSession sqlSession = null;
		try {
			
			String strPartObjectId = StringUtils.trimToEmpty(args[0]); 
			
			DomainObject domObj = new DomainObject(strPartObjectId);
			String strName     = domObj.getInfo(context, DomainConstants.SELECT_NAME);
			String strType     = domObj.getInfo(context, DomainConstants.SELECT_TYPE);
			
			//String strName = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump ", new String[] {strPartObjectId, DomainConstants.SELECT_NAME});
			
			if("cdmMechanicalPart".equals(strType) || "cdmPhantomPart".equals(strType)){
				
				if( "cdmPhantomPart".equals(strType) ) {
					domObj.setAttributeValue(context, "cdmPartType", "singleSupply");
				}
				
				StringList strListSelBus	= new StringList();
				strListSelBus.add(DomainObject.SELECT_ID);
				strListSelBus.add(DomainObject.SELECT_TYPE);
				strListSelBus.add(DomainObject.SELECT_NAME);
				strListSelBus.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_DRAWING_NO);
				strListSelBus.add(Company.SELECT_ORGANIZATION_NAME);
				
				StringBuffer strWhereBuffer = new StringBuffer();
				strWhereBuffer.append("current");
				strWhereBuffer.append(" == ");
				strWhereBuffer.append("IN_WORK");
				strWhereBuffer.append(" && ");
				strWhereBuffer.append("attribute[cdmDrawingNo]");
				strWhereBuffer.append(" == \"");
				strWhereBuffer.append(strName);
				strWhereBuffer.append("\" && ");
				strWhereBuffer.append("to[Part Specification]");
				strWhereBuffer.append(" == ");
				strWhereBuffer.append(" 'False' ");
				strWhereBuffer.append(" && ");
				strWhereBuffer.append("revision");
				strWhereBuffer.append(" == ");
				strWhereBuffer.append("last.revision");
				
				
				String strSearchType = "CATDrawing,CATPart,CATProduct";
				MapList objectList = DomainObject.findObjects(context,
																strSearchType,
																"*",
																"*",
																"*",
																cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION,
																strWhereBuffer.toString(), // where expression
																"",
																false,
																strListSelBus,
																(short) 0);
				
				for(int i=0; i<objectList.size(); i++){
					
					Map map = (Map)objectList.get(i);
					String id = (String)map.get(DomainConstants.SELECT_ID);
					String type = (String)map.get(DomainConstants.SELECT_TYPE);
					
					try{
						ContextUtil.pushContext(context, null, null, null);
						MqlUtil.mqlCommand(context, "trigger off", new String[]{});
						DomainRelationship.connect(context, new DomainObject(strPartObjectId), DomainConstants.RELATIONSHIP_PART_SPECIFICATION, new DomainObject(id));
					
						if("CATDrawing".equals(type)){
							String drawingNo = String.valueOf((String)map.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_DRAWING_NO));
							new DomainObject(strPartObjectId).setAttributeValue(context, cdmConstantsUtil.ATTRIBUTE_CDM_PART_DRAWING_NO, drawingNo);
						}
						
						if("cdmPhantomPart".equals(strType)){
						
							String drawingNo = String.valueOf((String)map.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_DRAWING_NO));
							DomainObject partObj = new DomainObject(strPartObjectId);
							partObj.setAttributeValue(context, cdmConstantsUtil.ATTRIBUTE_CDM_PART_DRAWING_NO, drawingNo);
							
							if("CATProduct".equals(type) || "CATPart".equals(type)){
								DomainObject obj = new DomainObject(id);
								obj.setAttributeValue(context, "IEF-EBOMSync-PartTypeAttribute", cdmConstantsUtil.TYPE_CDMPHANTOMPART);
							}
							
						}
					 
					}catch(Exception e){
						throw e;
					}finally{
						MqlUtil.mqlCommand(context, "trigger on", new String[]{});
						ContextUtil.popContext(context);
					}
					
					
				}
				
				// [B] modify by jtkim 2016-01-10
				// Part Option Setting.
				DomainObject ebomPartObj = DomainObject.newInstance(context, strPartObjectId);
				SqlSessionUtil.reNew("plm");
				sqlSession = SqlSessionUtil.getSqlSession();
				Map paramMap = new HashMap();
				paramMap.put("BLOCKCODE", strName.substring(0, 5));
				List<Map<String, String>> optionsList = sqlSession.selectList("getPartBlockCodeOptionsMap", paramMap);
				StringBuffer strBuffer = new StringBuffer();
				for(int k=0; k<optionsList.size(); k++){
					Map map = (Map)optionsList.get(k);
					String strLabelName = (String)map.get("LABELNAME");
					strBuffer.append(strLabelName);
					strBuffer.append("|");
				}
				ebomPartObj.setAttributeValue(context, cdmConstantsUtil.ATTRIBUTE_CDM_PART_OPTION_LABEL_NAME, strBuffer.toString());
				// [E] modify by jtkim 2016-01-10
				
				
				
			}
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			sqlSession.close();
		}

		return 0;

	}
    
    
    
    public int partRevisionRelationCADObjectTrigger(Context context, String[] args) throws Exception {
		
		
		try {
			
			String strPartObjectId = StringUtils.trimToEmpty(args[0]); 
			String strPartReviseObjectId = StringUtils.trimToEmpty(args[1]);
			
			String strName = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump ", new String[] {strPartObjectId, DomainConstants.SELECT_NAME});
			
			StringList strListSelBus	= new StringList();
			strListSelBus.add(DomainObject.SELECT_ID);
			strListSelBus.add(DomainObject.SELECT_TYPE);
			strListSelBus.add(DomainObject.SELECT_NAME);
			strListSelBus.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_DRAWING_NO);
			strListSelBus.add(Company.SELECT_ORGANIZATION_NAME);
			
			StringBuffer strWhereBuffer = new StringBuffer();
			strWhereBuffer.append("current");
			strWhereBuffer.append(" == ");
			strWhereBuffer.append("IN_WORK");
			strWhereBuffer.append(" && ");
			strWhereBuffer.append("attribute[cdmDrawingNo]");
			strWhereBuffer.append(" == \"");
			strWhereBuffer.append(strName);
			strWhereBuffer.append("\" && ");
			strWhereBuffer.append("to[Part Specification]");
			strWhereBuffer.append(" == ");
			strWhereBuffer.append(" 'False' ");
			strWhereBuffer.append(" && ");
			strWhereBuffer.append("revision");
			strWhereBuffer.append(" == ");
			strWhereBuffer.append("last.revision");
			
			
			String strSearchType = "CATDrawing,CATPart,CATProduct";
			MapList objectList = DomainObject.findObjects(context,
															strSearchType,
															"*",
															"*",
															"*",
															cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION,
															strWhereBuffer.toString(), // where expression
															"",
															false,
															strListSelBus,
															(short) 0);
			
			for(int i=0; i<objectList.size(); i++){
				
				Map map = (Map)objectList.get(i);
				String id = (String)map.get(DomainConstants.SELECT_ID);
				String type = (String)map.get(DomainConstants.SELECT_TYPE);
				
				try{
					ContextUtil.pushContext(context, null, null, null);
					MqlUtil.mqlCommand(context, "trigger off", new String[]{});
					DomainRelationship.connect(context, new DomainObject(strPartReviseObjectId), DomainConstants.RELATIONSHIP_PART_SPECIFICATION, new DomainObject(id));
				
					if("CATDrawing".equals(type)){
						String drawingNo = String.valueOf((String)map.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_DRAWING_NO));
						new DomainObject(strPartReviseObjectId).setAttributeValue(context, cdmConstantsUtil.ATTRIBUTE_CDM_PART_DRAWING_NO, drawingNo);
					}
					
					if("CATPart".equals(type) || "CATProduct".equals(type)){
						new DomainObject(id).setAttributeValue(context, "IEF-EBOMSync-PartTypeAttribute", cdmConstantsUtil.TYPE_CDMPHANTOMPART);
					}
					
				 
				}catch(Exception e){
					throw e;
				}finally{
					MqlUtil.mqlCommand(context, "trigger on", new String[]{});
					ContextUtil.popContext(context);
				}
				
				
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

		return 0;

	}
    
    /**
     * 
     * @param context
     * @param args
     * @throws Exception
     */
	public void adjustEBOM(Context context, String[] args) throws Exception {
		try {

			
			ContextUtil.startTransaction(context, true);
			String strPartId = args[0];

			Part part = new Part(strPartId);

			BusinessObject lastRev = part.getLastRevision(context);
			String strNextSeq = lastRev.getNextSequence(context);
			BusinessObject newbo = lastRev.revise(context, strNextSeq, lastRev.getVault());
			
			this.adjustEBOM(context, newbo.getObjectId(context));
			
			ContextUtil.commitTransaction(context);

		} catch (Exception e) {
			ContextUtil.abortTransaction(context);
			
			e.printStackTrace();
		}
	}
    
    
	
	/**
	 * 
	 * @param context
	 * @param strNewRevPartId
	 * @throws Exception
	 */
    public void adjustEBOM(Context context, String strNewRevPartId) throws Exception {
    	
    	try {

    		ContextUtil.pushContext(context);
    		MqlUtil.mqlCommand(context, "trigger off");
    		
    		//strNewRevPartId
			DomainObject doNewRevPart = new DomainObject(strNewRevPartId);
			String strPrevPartId = doNewRevPart.getInfo(context, "previous.id");

			StringList busSelect = new StringList();
			busSelect.add(DomainObject.SELECT_ID);
			busSelect.add(DomainObject.SELECT_CURRENT);
			busSelect.add("previous.id");
			busSelect.add("islast");
			
			StringList relSelect = new StringList();
			relSelect.add(DomainRelationship.SELECT_ID);
			relSelect.add(EngineeringConstants.SELECT_ATTRIBUTE_QUANTITY);
			relSelect.add(EngineeringConstants.SELECT_ATTRIBUTE_FIND_NUMBER);
			relSelect.add(EngineeringConstants.SELECT_ATTRIBUTE_REFERENCE_DESIGNATOR);

			if (StringUtils.isNotEmpty(strPrevPartId)) {

				
				DomainObject doPrevPart = new DomainObject(strPrevPartId);
				MapList mlParentBom = doPrevPart.getRelatedObjects(context, 
													 DomainConstants.RELATIONSHIP_EBOM, // Relationship
													 DomainConstants.TYPE_PART, //  Type name
													 busSelect, // objects Select
													 relSelect, // Rel selects
													 true,      // to Direction
													 false,     // from Direction
													 (short) 1, // recusion level
													 "", //"current == Release", 
													 "",
												     0);
				
				
				
				for (int i = 0; i < mlParentBom.size(); i++) {

					Map objMap = (Map) mlParentBom.get(i);
					String strParentId = (String) objMap.get(DomainObject.SELECT_ID);
					String strParentCurrent = (String) objMap.get(DomainObject.SELECT_CURRENT);
					
					String strQty = (String) objMap.get(EngineeringConstants.SELECT_ATTRIBUTE_QUANTITY);
					String strFindNumber = (String) objMap.get(EngineeringConstants.SELECT_ATTRIBUTE_FIND_NUMBER);
					String strRefDes = (String) objMap.get(EngineeringConstants.SELECT_ATTRIBUTE_REFERENCE_DESIGNATOR);
					
					String strEbomId = (String) objMap.get(DomainRelationship.SELECT_ID);
					


					DomainRelationship doEbom = DomainRelationship.connect(context, new DomainObject(strParentId), DomainConstants.RELATIONSHIP_EBOM, new DomainObject(strNewRevPartId));
					doEbom.setAttributeValue(context, EngineeringConstants.ATTRIBUTE_QUANTITY, strQty);
					doEbom.setAttributeValue(context, EngineeringConstants.ATTRIBUTE_FIND_NUMBER, strFindNumber);
					doEbom.setAttributeValue(context, EngineeringConstants.ATTRIBUTE_REFERENCE_DESIGNATOR, strRefDes);
					
				}
			}
			
			
			MapList mlWhereUsed = doNewRevPart.getRelatedObjects(context, 
																 DomainConstants.RELATIONSHIP_EBOM, // Relationship
																 DomainConstants.TYPE_PART, // From Type name
																 busSelect, // objects Select
																 relSelect, // Rel selects
																 true,      // to Direction
																 false,     // from Direction
																 (short) 1, // recursion level
																 "revision == last",        // object where 
																 "",        // rel where 
															     0);
			
			
			
			for (int i = 0; i < mlWhereUsed.size(); i++) {

				Map whereUsedMap = (Map) mlWhereUsed.get(i);
				String strParentPartId = (String) whereUsedMap.get(DomainObject.SELECT_ID);
				String strParentPartCurrent = (String) whereUsedMap.get(DomainObject.SELECT_CURRENT);
				
				if("Preliminary".equals(strParentPartCurrent)) {
					
					DomainObject doParentPart = new DomainObject(strParentPartId);
					
					busSelect.add(doParentPart);
					MapList mlChildBom = doParentPart.getRelatedObjects(context, 
																		 DomainConstants.RELATIONSHIP_EBOM, // Relationship
																		 DomainConstants.TYPE_PART, // From Type name
																		 busSelect, // objects Select
																		 relSelect, // Rel selects
																		 false,      // to Direction
																		 true,     // from Direction
																		 (short) 1, // recursion level
																		 "",        // object where 
																		 "",        // rel where 
																	     0);
		
					StringList slPreviousChildOids = new StringList();
					for (int k = 0; k < mlChildBom.size(); k++) {
		
						Map objMap = (Map) mlChildBom.get(k);
						String strPrevId = (String) objMap.get("previous.id");
						slPreviousChildOids.add(strPrevId);
					}
		
					
					String strPrevParentId = doParentPart.getInfo(context, "previous.id");
					
					for (int k = 0; k < mlChildBom.size(); k++) {
		
						Map objMap = (Map) mlChildBom.get(k);
						String strChildPartId = (String) objMap.get(DomainObject.SELECT_ID);
						String strEbomId = (String) objMap.get(DomainRelationship.SELECT_ID);
		
						if (slPreviousChildOids.contains(strChildPartId)) {
							
							MqlUtil.mqlCommand(context, "del connection $1", strEbomId);
							
							String strNextId = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", strChildPartId, "next.id");
							String strBeforeBOMID = MqlUtil.mqlCommand(context, "print bus "+strPrevParentId+" select from[EBOM|to.id=="+strNextId+"].id dump");
							
							
							if(StringUtils.isNotEmpty(strBeforeBOMID)) {
								MqlUtil.mqlCommand(context, "del connection $1", strBeforeBOMID);
							}
						}
					}
				}
			}
			
			
			MapList mlChildBom = doNewRevPart.getRelatedObjects(context, 
																 DomainConstants.RELATIONSHIP_EBOM, // Relationship
																 DomainConstants.TYPE_PART, // From Type name
																 busSelect, // objects Select
																 relSelect, // Rel selects
																 false,      // to Direction
																 true,     // from Direction
																 (short) 1, // recursion level
																 "",        // object where 
																 "",        // rel where 
															     0);

			StringList slPreviousOids = new StringList();
			for (int i = 0; i < mlChildBom.size(); i++) {

				Map objMap = (Map) mlChildBom.get(i);
				String strPrevId = (String) objMap.get("previous.id");
				slPreviousOids.add(strPrevId);
			}

			for (int i = 0; i < mlChildBom.size(); i++) {

				Map objMap = (Map) mlChildBom.get(i);
				String strChildPartId = (String) objMap.get(DomainObject.SELECT_ID);
				String strEbomId = (String) objMap.get(DomainRelationship.SELECT_ID);

				if (slPreviousOids.contains(strChildPartId)) {
					
					MqlUtil.mqlCommand(context, "del connection $1", strEbomId);
					String strNextId = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", strChildPartId, "next.id");
					
					String strBeforeBOMID = MqlUtil.mqlCommand(context, "print bus "+strPrevPartId+" select from[EBOM|to.id=="+strNextId+"].id dump");
					
					if(StringUtils.isNotEmpty(strBeforeBOMID)) {
						MqlUtil.mqlCommand(context, "del connection $1", strBeforeBOMID);
						
					}
				}
			}
			
			
    		
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			MqlUtil.mqlCommand(context, "trigger on");
			ContextUtil.popContext(context);
		}
    	
    	
    }
    
    
    public Boolean editPartSurfaceTreatment (Context context, String[] args) throws Exception {
        boolean isOptionDetailSuccess = true;
        try{
            HashMap paramMap = (HashMap)JPO.unpackArgs(args);
            System.out.println("!!!!!!paramMap     " + paramMap);
            String strObjectId = (String)paramMap.get("objectId");
            DomainObject domObj = new DomainObject(strObjectId);
            	
            
            //domObj.setAttributeValue(context, "", "");
            
        } catch(Exception e) {
        	e.printStackTrace();
        	isOptionDetailSuccess = false;
        	throw e;
        } finally {
        	return isOptionDetailSuccess;
        }
        
    }
    
    
    
    public Boolean editPartMaterial (Context context, String[] args) throws Exception {
        boolean isOptionDetailSuccess = true;
        try{
            HashMap paramMap = (HashMap)JPO.unpackArgs(args);
            String strObjectId = (String)paramMap.get("objectId");
            DomainObject domObj = new DomainObject(strObjectId);

            //domObj.setAttributeValue(context, "", "");
            
        } catch(Exception e) {
        	e.printStackTrace();
        	isOptionDetailSuccess = false;
        	throw e;
        } finally {
        	return isOptionDetailSuccess;
        }
        
    }
    
    
	
}
