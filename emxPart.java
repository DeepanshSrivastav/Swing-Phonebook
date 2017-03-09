/*
 ** ${CLASS:MarketingFeature}
 **
 ** Copyright (c) 1993-2015 Dassault Systemes. All Rights Reserved.
 ** This program contains proprietary and trade secret information of
 ** Dassault Systemes.
 ** Copyright notice is precautionary only and does not evidence any actual
 ** or intended publication of such program
 */

import java.util.Map;

import com.mando.util.cdmConstantsUtil;
import com.mando.util.cdmStringUtil;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;

import matrix.db.Context;
import matrix.util.StringList;


import com.matrixone.apps.domain.util.MapList;
import java.util.HashMap;
import matrix.db.JPO;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.engineering.Part;
import java.util.Iterator;
import com.matrixone.apps.engineering.EngineeringConstants;
import com.matrixone.apps.engineering.EngineeringUtil;
import matrix.db.BusinessObject;

/**
 * The <code>emxPart</code> class contains code for the "Part" business type.
 *
 * @version EC 9.5.JCI.0 - Copyright (c) 2002, MatrixOne, Inc.
 */
  public class ${CLASSNAME} extends ${CLASS:emxPartBase}
  {
      /**
       * Constructor.
       *
       * @param context the eMatrix <code>Context</code> object.
       * @param args holds no arguments.
       * @throws Exception if the operation fails.
       * @since EC 9.5.JCI.0.
       */

	public ${CLASSNAME}(Context context, String[] args) throws Exception {
		super(context, args);
	}

	
	/**
	 * It does not allow that Proto Part have Production Part as parent
	 * @param context
	 * @param args
	 * @return
	 */
	public int checkIfChildIsProto(Context context, String[] args) throws Exception {
		
		/**
		 *   'eService Program Argument 1' '${FROMOBJECTID}'
        'eService Program Argument 2' '${TOOBJECTID}'
		 */
		try {
			
			String strFromObjectId = args[0]; // parent Part
			String strToObjectId = args[1];   // child Part
			
			DomainObject doParentPart = new DomainObject(strFromObjectId);
			String strParentPhase 		= doParentPart.getInfo(context, "attribute[cdmPartPhase]");
			String strParentType 		= doParentPart.getInfo(context, DomainObject.SELECT_TYPE);
			String strParentCurrent 	= doParentPart.getInfo(context, DomainObject.SELECT_CURRENT);
			String strParentEoObjectId 	= doParentPart.getInfo(context, "to[Affected Item].from.id");
			
			
			DomainObject doChildPart = new DomainObject(strToObjectId);
			String strChildPhase 		= doChildPart.getInfo(context, "attribute[cdmPartPhase]");
			String strChildType 		= doChildPart.getInfo(context, DomainObject.SELECT_TYPE);
			String strChildCurrent 		= doChildPart.getInfo(context, DomainObject.SELECT_CURRENT);
			String strChildEoObjectId 	= doChildPart.getInfo(context, "to[Affected Item].from.id");
			

			if ("Production".equals(strParentPhase)) {
				
				if("Proto".equals(strChildPhase)) {
					
					String strErrorMessage = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),"emxEngineeringCentral.Alert.ProtoChildPart");
					throw new FrameworkException(strErrorMessage);
					
				}
				
			}

			
			if ("Proto".equals(strParentPhase)) {
				
				if("Production".equals(strChildPhase) && "Preliminary".equals(strChildCurrent)) {
					
					String strErrorMessage = "New Production Part cannot be add as child below Proto Part.";
					throw new FrameworkException(strErrorMessage);
					
				}
				
			}
			
			
			if(cdmConstantsUtil.TYPE_CDMPHANTOMPART.equals(strParentType)) {
				
				DomainObject parentEoObj = new DomainObject(strParentEoObjectId);
				String strEOType = (String) parentEoObj.getInfo(context, "type");
				
				if("cdmPEO".equals(strEOType)) {
					
					if( "Production".equals(strChildPhase) ) {
						String strErrorMessage = "This part does not correspond to the eotype of the PhantomPart";
						throw new FrameworkException(strErrorMessage);
					}
					
				} else {
					
					if( "Proto".equals(strChildPhase) ) {
						String strErrorMessage = "This part does not correspond to the eotype of the PhantomPart";
						throw new FrameworkException(strErrorMessage);
					}
					
				}
				
			}
			
			
			// Phantom Part cannot be child part.
			if(cdmConstantsUtil.TYPE_CDMPHANTOMPART.equals(strChildType)) {
				String strErrorMessage = EnoviaResourceBundle.getProperty(context,"emxEngineeringCentralStringResource",context.getLocale(),"emxEngineeringCentral.Alert.PhantomNoChild");
				throw new FrameworkException(strErrorMessage);
			}
			
			
			// Parent part must not have same part as child.
			StringList slChildPart = doParentPart.getInfoList(context, "from[EBOM].to.id");
			
			if(slChildPart.contains(strToObjectId)) {
				String strErrorMessage = EnoviaResourceBundle.getProperty(context,"emxEngineeringCentralStringResource",context.getLocale(),"emxEngineeringCentral.Alert.notallowsamechild");
				throw new FrameworkException(strErrorMessage);
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

		return 0;

	}
	
	/**
	 * reset drawing no in child part when deleting EBOM connection.
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public int removeDrawingNo(Context context, String[] args) throws Exception {
		
		/**
		 *   'eService Program Argument 1' '${FROMOBJECTID}'
         *	 'eService Program Argument 2' '${TOOBJECTID}'
		 */
		try {
			
			String strFromObjectId = args[0]; // parent Part
			String strToObjectId = args[1];   // child Part
			
			DomainObject doParentPart = new DomainObject(strFromObjectId);
			String strParentType = doParentPart.getInfo(context, DomainObject.SELECT_TYPE);
			
			// Phantom Part cannot be child part.
			if(cdmConstantsUtil.TYPE_CDMPHANTOMPART.equals(strParentType)) {
				
				DomainObject doChildPart = new DomainObject(strToObjectId);
				doChildPart.setAttributeValue(context, "cdmPartDrawingNo", "");
			}

			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		
		return 0;
		
	}
	
	
	
	 /**
	   * Checks that the Specification which is going to be connected to Part is having states
	   * "Review", "Approved" and "Released". If any spec object not having these states, it cannot
	   * be added as "Part Specification". Also, "Review" should not be the first state.
	   *
	   * @param context the eMatrix <code>Context</code> object.
	   *
	   * @return int 0-success 1-failure.
	   * @throws Exception if the operation fails.
	   * @since EC 10-6
	   */
	    public int ensureSpecificationStates(Context context, String[] args) throws Exception
	    {
	    	
	    	//do not delete.
	    	//overrides original check method.
	    	return 0;
	    }

	    

	/**
	 * add by jtkim 2017-01-26
	 * @param context
	 * @param args
	 * @throws Exception
	 */
	public void setUpdatePhantomChildDrawingNo(Context context, String[] args) throws Exception {
		try {
			String fromObjectId = args[0];
			String toObjectId = args[1];

			if (cdmStringUtil.isNotEmpty(fromObjectId) && cdmStringUtil.isNotEmpty(toObjectId)) {
				DomainObject fromObj = DomainObject.newInstance(context, fromObjectId);
				DomainObject toObj = DomainObject.newInstance(context, toObjectId);

				StringList fromBusSelect = new StringList();
				fromBusSelect.add(cdmConstantsUtil.SELECT_TYPE);
				fromBusSelect.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO);

				Map fromObjInfoMap = fromObj.getInfo(context, fromBusSelect);

				String fromType = (String) fromObjInfoMap.get(cdmConstantsUtil.SELECT_TYPE);
				if (cdmConstantsUtil.TYPE_CDMPHANTOMPART.equals(fromType)) {
					String drawingNo = (String) fromObjInfoMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO);
					toObj.setAttributeValue(context, cdmConstantsUtil.ATTRIBUTE_CDM_PART_DRAWING_NO, drawingNo);
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	 /*
	  * Retrieves the EBOM data. Modified for expand issue for latest and latest complete.
	  *
	  * @param context the ENOVIA <code>Context</code> object
	  * @param args[] programMap
	  * @throws Exception if error encountered while carrying out the request
	  */
	 @com.matrixone.apps.framework.ui.ProgramCallable
	 public MapList getEBOMsWithRelSelectablesSB(Context context, String[] args) throws Exception{
long time0	= System.currentTimeMillis();

		 HashMap paramMap = (HashMap) JPO.unpackArgs(args);

		 String sExpandLevels = getStringValue(paramMap, "emxExpandFilter");
		 String selectedFilterValue = getStringValue(paramMap, "ENCBOMRevisionCustomFilter");
System.out.println("sExpandLevels="+sExpandLevels);
		 
		 MapList retList = expandEBOM(context, paramMap);
/*************************************************
*	Start for performance 20170306
*************************************************/
/*
		 if("Latest".equals(selectedFilterValue) || "Latest Complete".equals(selectedFilterValue) || "Latest Release".equals(selectedFilterValue)){
			// handles manual expansion by each level for latest and latest complete
			 int expandLevel = "All".equals(sExpandLevels)? 0: Integer.parseInt(sExpandLevels);
			 MapList childList = null;
			 Map obj = null;
			 int level;
			 for(int index=0; index < retList.size(); index++){
				 obj = (Map)retList.get(index);
				 if(expandLevel == 0 || Integer.parseInt((String)obj.get("level")) < expandLevel){
					 paramMap.put("partId", (String)obj.get(SELECT_ID));
					 childList = expandEBOM(context, paramMap);
					 if(childList!=null && !childList.isEmpty()){
						 for(int cnt=0; cnt<childList.size(); cnt++){
							 level = Integer.parseInt((String)obj.get("level"))+1;
							((Map)childList.get(cnt)).put("level", String.valueOf(level));
						 }
						 retList.addAll(index+1,childList);
					 }
				}
			 }
		 }
*/
/*************************************************
*	End for performance 20170306
*************************************************/
long time1	= System.currentTimeMillis();
System.out.println(">>>getEBOMsWithRelSelectablesSB time1="+(time1-time0)+"("+(time1-time0)+")");

		 return retList;
	 }

	 /*
	  * Retrieves the EBOM data. Method is added to expand the ebom and fetch the
	  * Latest or Latest Complete nodes in child.
	  *
	  * @param context the ENOVIA <code>Context</code> object
	  * @param HashMap paramMap
	  * @throws Exception if error encountered while carrying out the request
	  */

 	public MapList expandEBOM(Context context, HashMap paramMap) throws Exception {  //name modified from getEBOMsWithRelSelectablesSB
		//HashMap paramMap = (HashMap) JPO.unpackArgs(args);

		int nExpandLevel = 0;

		String partId = getStringValue(paramMap, "objectId");
		String sExpandLevels = getStringValue(paramMap, "emxExpandFilter");
		String selectedFilterValue = getStringValue(paramMap, "ENCBOMRevisionCustomFilter");
		String strAttrEnableCompliance = PropertyUtil.getSchemaProperty(context, "attribute_EnableCompliance");
		String complete = PropertyUtil.getSchemaProperty(context, "policy", DomainConstants.POLICY_DEVELOPMENT_PART, "state_Complete");
		String release = PropertyUtil.getSchemaProperty(context, "policy", DomainConstants.POLICY_EC_PART, "state_Release");
		String SELECT_ATTR_ENABLE_COMPLIANCE = "attribute[" + strAttrEnableCompliance + "]";
		String curRevision;
		String latestObjectId;
		String latestRevision;

		if (!isValidData(selectedFilterValue)) {
			selectedFilterValue = "As Stored";
		}

		if (!isValidData(sExpandLevels)) {
			sExpandLevels = getStringValue(paramMap, "ExpandFilter");
		}
		
		String SELECT_LAST_EBOM_EXISTS = "last.from[EBOM]";

		StringList objectSelect = createStringList(new String[] {SELECT_ID, SELECT_REVISION, SELECT_LAST_ID, SELECT_LAST_REVISION,SELECT_LAST_CURRENT,
																		SELECT_REL_FROM_EBOM_EXISTS, SELECT_ATTR_ENABLE_COMPLIANCE, SELECT_LAST_EBOM_EXISTS});
		
		//BOM UI Performance: Attributes required for Related Physical title column
		String attrVPLMVName = PropertyUtil.getSchemaProperty(context,"attribute_PLMEntity.V_Name");
        attrVPLMVName = "attribute["+attrVPLMVName+"]";
        String typeVPLMProd = PropertyUtil.getSchemaProperty(context,"type_PLMEntity");
        
        String selectProdctId = "from["+DomainConstants.RELATIONSHIP_PART_SPECIFICATION+"].to."+DomainConstants.SELECT_ID;
        String selectPartVName = "attribute["+EngineeringConstants.ATTRIBUTE_V_NAME+"]";
        String selectProdctIdSel = "from["+DomainConstants.RELATIONSHIP_PART_SPECIFICATION+"|to.type.kindof["+typeVPLMProd+"]].to."+DomainConstants.SELECT_ID;
       
/*************************************************
*	Start for performance 20170306
*************************************************/
//	    objectSelect.add(selectProdctId);
//		objectSelect.add(selectProdctIdSel);
//		objectSelect.add(selectPartVName);		
		objectSelect.add(SELECT_CURRENT);
/*************************************************
*	End for performance 20170306
*************************************************/
				
		//BOM UI Performance: Attributes required for Related Physical title column

		StringList relSelect = createStringList(new String[] {SELECT_RELATIONSHIP_ID, SELECT_ATTRIBUTE_FIND_NUMBER});

/*************************************************
*	Start for performance 20170306
*************************************************/
/*		if (!isValidData(sExpandLevels) || ("Latest".equals(selectedFilterValue) || "Latest Complete".equals(selectedFilterValue) ||"Latest Release".equals(selectedFilterValue))) {
			nExpandLevel = 1;
			partId = getStringValue(paramMap, "partId") == null? partId : getStringValue(paramMap, "partId");
		} else if ("All".equalsIgnoreCase(sExpandLevels)) {
*/
		if ("All".equalsIgnoreCase(sExpandLevels)) {
/*************************************************
*	End for performance 20170306
*************************************************/
			nExpandLevel = 0;
		} else {
			nExpandLevel = Integer.parseInt(sExpandLevels);
		}

		Part partObj = new Part(partId);

		MapList ebomList = partObj.getRelatedObjects(context,
				          							 RELATIONSHIP_EBOM,
				          							 TYPE_PART,
				          							 objectSelect,
				          							 relSelect,
					                                 false,
					                                 true,
					                                 (short) nExpandLevel,
					                                 null, null, 0);

		  Iterator itr = ebomList.iterator();
		  Map newMap;

/*************************************************
*	Start for performance 20170306
*************************************************/
//		  StringList ebomDerivativeList = EngineeringUtil.getDerivativeRelationships(context, RELATIONSHIP_EBOM, true);
/*************************************************
*	End for performance 20170306
*************************************************/
		  
	      if ("Latest".equals(selectedFilterValue) || ("Latest Complete".equals(selectedFilterValue)) || ("Latest Release".equals(selectedFilterValue))) {
	          //Iterate through the maplist and add those parts that are latest but not connected

	          while (itr.hasNext()) {
	              newMap = (Map) itr.next();

	              curRevision    = getStringValue(newMap, SELECT_REVISION);
	              latestObjectId = getStringValue(newMap, SELECT_LAST_ID);
	              latestRevision = getStringValue(newMap, SELECT_LAST_REVISION);

	              if (nExpandLevel != 0) {
/*************************************************
*	Start for performance 20170306
*************************************************/
		        	  //newMap.put("hasChildren", EngineeringUtil.getHasChildren(newMap, ebomDerivativeList));
		        	  newMap.put("hasChildren", getStringValue(newMap, SELECT_REL_FROM_EBOM_EXISTS));
/*************************************************
*	End for performance 20170306
*************************************************/
	              }

	              if ("Latest".equals(selectedFilterValue)) {
	            	  newMap.put(SELECT_ID, latestObjectId);
/*************************************************
*	Start for performance 20170306
*************************************************/
	            	  newMap.put(SELECT_CURRENT, SELECT_LAST_CURRENT);
	            	  //newMap.put("hasChildren", EngineeringUtil.getHasChildren(newMap, ebomDerivativeList));
	            	  newMap.put("hasChildren", getStringValue(newMap, SELECT_LAST_EBOM_EXISTS));
/*************************************************
*	End for performance 20170306
*************************************************/
	              }

	              else {
/*************************************************
*	Start for performance 20170306
*************************************************/
//	                   DomainObject domObjLatest = DomainObject.newInstance(context, latestObjectId);
//	                   String currSta = domObjLatest.getInfo(context, DomainConstants.SELECT_CURRENT);
	                   String currSta = getStringValue(newMap, SELECT_LAST_CURRENT);
/*************************************************
*	End for performance 20170306
*************************************************/

	                   if (curRevision.equals(latestRevision)) {
	                	   if (complete.equalsIgnoreCase(currSta) || release.equalsIgnoreCase(currSta)) {
	                		   newMap.put(SELECT_ID, latestObjectId);
/*************************************************
*	Start for performance 20170306
*************************************************/
	                		   newMap.put(SELECT_CURRENT, SELECT_LAST_CURRENT);
/*************************************************
*	End for performance 20170306
*************************************************/
	                	   } else {
	                		   itr.remove();
	                	   }
	                   }
	                   else {
/*************************************************
*	Start for performance 20170306
*************************************************/
						   DomainObject domObjLatest = DomainObject.newInstance(context, latestObjectId);
/*************************************************
*	End for performance 20170306
*************************************************/
	                	   while(true) {
	                   		   if(currSta.equalsIgnoreCase(complete) || currSta.equalsIgnoreCase(release)) {
	                   			   newMap.put(SELECT_ID, latestObjectId);
	                   			   	break;
	                   		   } else {
	                   			   BusinessObject boObj = domObjLatest.getPreviousRevision(context);
	                   			   if(!(boObj.toString()).equals("..") ) {
	                   				   boObj.open(context);
	                   				latestObjectId = boObj.getObjectId();
	                   				   domObjLatest = DomainObject.newInstance(context,latestObjectId);
	                        		   currSta = domObjLatest.getInfo(context,DomainConstants.SELECT_CURRENT);
	                   			   } else {
	                   				   itr.remove();
	                   				   break;
	                   			}
	                   		 }
	                  	  }//End of while
	                   }//End of Else
	               }
	            }//End of While

	      	}//End of IF, Latest or Latest complete filter is selected

	      else if (nExpandLevel != 0) {
	          while (itr.hasNext()) {
	        	  newMap = (Map) itr.next();

	        	  // To display  + or - in the bom display	 
/*************************************************
*	Start for performance 20170306
*************************************************/
//	        	  newMap.put("hasChildren", EngineeringUtil.getHasChildren(newMap, ebomDerivativeList));
	        	  newMap.put("hasChildren", getStringValue(newMap, SELECT_REL_FROM_EBOM_EXISTS));
/*************************************************
*	End for performance 20170306
*************************************************/
	          }
	      }

	 	return ebomList;
	 }

	private String getStringValue(Map map, String key) {
		return (String) map.get(key);
	}
	private boolean isValidData(String data) {
		return ((data == null || "null".equals(data)) ? 0 : data.trim().length()) > 0;
	}

	private StringList createStringList(String[] selectable) {
		int length = length(selectable);
		StringList list = new StringList(length);
		for (int i = 0; i < length; i++)
			list.add(selectable[i]);
		return list;
	}

	private int length(Object[] array) {
		return array == null ? 0 : array.length;
	}


}
