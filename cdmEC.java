import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;

import com.mando.util.SqlSessionUtil;
import com.mando.util.cdmConstantsUtil;
import com.mando.util.cdmFTPUtil;
import com.mando.util.cdmJsonDataUtil;
import com.mando.util.cdmOwnerRolesUtil;
import com.mando.util.cdmPropertiesUtil;
import com.mando.util.cdmStringUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.Job;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.engineering.EngineeringConstants;
import com.matrixone.apps.engineering.Part;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.FileList;
import matrix.db.JPO;
import matrix.util.Pattern;
import matrix.util.SelectList;
import matrix.util.StringList;

import matrix.db.BusinessObjectWithSelectList;
import java.util.Hashtable;

/**
 * @author jh.Park
 * @desc
 */
public class ${CLASSNAME} {

	/**
	 * @author jaehyun CreateEC.
	 */
	@SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
	@com.matrixone.apps.framework.ui.CreateProcessCallable
	public Map createEC(Context context, String[] args) throws Exception {
		Map returnMap = new HashMap();
		try {
			ContextUtil.startTransaction(context, true);

			SimpleDateFormat sdfFormat = new SimpleDateFormat("yyyy-", Locale.KOREA);
			Date date = new Date();
			String strYear = sdfFormat.format(date);

			HashMap paramMap = (HashMap) JPO.unpackArgs(args);
			String strType = (String) paramMap.get(cdmConstantsUtil.TEXT_TYPEACTUAL);
			String strName = (String) paramMap.get("Code");
			String strCDMOnly = (String) paramMap.get(cdmConstantsUtil.TEXT_CDMONLY);
			String strECTitle = (String) paramMap.get(cdmConstantsUtil.TEXT_TITLE);
			String strProjectOID = StringUtils.trimToEmpty((String) paramMap.get(cdmConstantsUtil.TEXT_PROJECTOID));
			String strObjectId = StringUtils.trimToEmpty((String) paramMap.get(cdmConstantsUtil.TEXT_OBJECTID));
			String strParentOID = StringUtils.trimToEmpty((String) paramMap.get("parentOID"));

			// Number Generator, Auto Name
			DomainObject domECObj = new DomainObject();

			String ngType = "eService Number Generator";
			String ngName = "cdmECNumberGenerator";
			String ngRev = "cdmEC";
			String ngVault = "eService Administration";

			BusinessObject numGenerator = new BusinessObject(ngType, ngName, ngRev, ngVault);
			int number = Integer.parseInt(numGenerator.getAttributeValues(context, "eService Next Number").getValue());
			domECObj.createObject(context, strType, strName, "-", cdmConstantsUtil.POLICY_CDM_EC_POLICY, "eService Production");

			numGenerator.setAttributeValue(context, "eService Next Number", String.valueOf(number + 1));

			String strECObjectId = MqlUtil.mqlCommand(context, "print bus '" + strType + "' '" + String.format("X-" + strYear + "%05d", number) + "' - select id dump");

			if (!DomainConstants.EMPTY_STRING.equals(strProjectOID)) {
				MqlUtil.mqlCommand(context, "connect bus '" + strProjectOID + "' relationship '" + cdmConstantsUtil.RELATIONSHIP_CDM_PROJECT_RELATIONSHIP_EC + "' to " + strECObjectId);
			}

			returnMap.put(cdmConstantsUtil.ATTRIBUTE_CDM_EC_CATEGORY, strCDMOnly);
			returnMap.put("Title", strECTitle);

			domECObj.setAttributeValues(context, returnMap);

			if (!DomainConstants.EMPTY_STRING.equals(strObjectId)) {
				
				String[] strObjectIdArray = strObjectId.split(",");
				
				StringList slList = new StringList();
				Map map = new HashMap();
				
				for (int i = 0; i < strObjectIdArray.length; i++) {
					
					String strPartObjectId = strObjectIdArray[i];
					
					if (!DomainConstants.EMPTY_STRING.equals(strPartObjectId)) {
						
						String strParentId = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump $3", new String[] { strPartObjectId, "to[" + DomainConstants.RELATIONSHIP_EBOM + "].from.id", "|" }));
						
						String strFindNumber = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump $3", new String[] { strPartObjectId, "to["+DomainConstants.RELATIONSHIP_EBOM+"]."+DomainConstants.SELECT_ATTRIBUTE_FIND_NUMBER, "|" }));
						if(DomainConstants.EMPTY_STRING.equals(strFindNumber)){
							strFindNumber = "1";
						}
						
						if(! slList.contains(strPartObjectId)){
							map.put(strPartObjectId, strParentId);
						}
						
						if (!strParentOID.equals(strParentId)) {
							
							if (strParentId.contains("|")) {
								
								String[] strParentObjectIdArray = strParentId.split("\\|");
								
								String strRevisePartId = DomainConstants.EMPTY_STRING;
								if(slList.contains(strPartObjectId)){
									
									for (int k = 0; k < strParentObjectIdArray.length; k++) {
										
										if(! DomainConstants.EMPTY_STRING.equals(strRevisePartId)){
											String strReviseId = ${CLASS:cdmPartLibrary}.revisePartParentStructure(context, strRevisePartId, strPartObjectId, strParentId);
											System.out.println("CREATE Revise ObjectId     #=     " + strReviseId);	
										}
										
									}
									
								}else{
									
									for (int k = 0; k < strParentObjectIdArray.length; k++) {
										
										String strIsVersionObject = new DomainObject(strParentObjectIdArray[k]).getAttributeValue(context, DomainConstants.ATTRIBUTE_IS_VERSION_OBJECT);
											
										if ("False".equals(strIsVersionObject)) {
												
											strParentId = strParentObjectIdArray[k];
												
											if(DomainConstants.EMPTY_STRING.equals(strRevisePartId)){
													
												strRevisePartId = ${CLASS:cdmPartLibrary}.revise(context, strPartObjectId, strParentId);
												System.out.println("CREATE EO Revise Part ObjectId     =     " + strRevisePartId);
												${CLASS:cdmPartLibrary}.revisePartSetAttributeValueAndConnection(context, strPartObjectId, strECObjectId, strRevisePartId);
												
											}else{
													
												strRevisePartId = ${CLASS:cdmPartLibrary}.revisePartParentStructure(context, strRevisePartId, strPartObjectId, strParentId);
												System.out.println("CREATE EO Revise Part ObjectId     ==     " + strRevisePartId);
													
											}
										
										}
										
									}
									
								}
								
								
							} else {
								
								if(slList.contains(strPartObjectId)){
									
									String strRevisePartId = StringUtils.trimToEmpty(new DomainObject(strPartObjectId).getInfo(context, "next.id"));
									
									if(! "".equals(strRevisePartId)){
										
										//String strParentId = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump $3", new String[] { strPartObjectId, "to[" + DomainConstants.RELATIONSHIP_EBOM + "].from.id", "|" }));
										String strParentIds = (String)map.get(strPartObjectId);
										
										if (!strParentId.equals(strParentIds)) {
											
											if (strParentIds.contains("|")) {
												
												String[] strParentObjectIdsArray = strParentIds.split("\\|");
												
												for (int l = 0; l < strParentObjectIdsArray.length; l++) {
													
													String strParentObjectId = strParentObjectIdsArray[l];
													
													if(! DomainConstants.EMPTY_STRING.equals(strParentObjectId)){
														
														String strIsVersionObject = new DomainObject(strParentObjectId).getAttributeValue(context, DomainConstants.ATTRIBUTE_IS_VERSION_OBJECT);
														
														if ("True".equals(strIsVersionObject)) {
															
															try{
																ContextUtil.pushContext(context, null, null, null);
																MqlUtil.mqlCommand(context, "trigger off", new String[]{});
																
																String strParentPartId = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump $3", new String[] { strPartObjectId, "to[" + DomainConstants.RELATIONSHIP_EBOM + "].from.id", "|" }));
																
																if(! DomainConstants.EMPTY_STRING.equals(strParentPartId) && ! strParentPartId.contains(strParentObjectId) ){
																	
																	DomainRelationship EBOMNewRel = DomainRelationship.connect(context, new DomainObject(strParentObjectId), DomainConstants.RELATIONSHIP_EBOM, new DomainObject(strPartObjectId));
																	EBOMNewRel.setAttributeValue(context, DomainConstants.ATTRIBUTE_FIND_NUMBER, strFindNumber);
																}
																
															}catch(Exception e){
																throw e;
															}finally{
																MqlUtil.mqlCommand(context, "trigger on", new String[]{});
																ContextUtil.popContext(context);
															}
															
														}
														
													}
													
												}
												
											}
											
										}else{
											
											strRevisePartId = ${CLASS:cdmPartLibrary}.revisePartParentStructure(context, strRevisePartId, strPartObjectId, strParentId);
											System.out.println("CREATE EO Revise Part ObjectId     :     " + strRevisePartId);
											
										}
										
									}
									
								}else{
									
									if(! DomainConstants.EMPTY_STRING.equals(strParentId)){
										
										//String strIsVersionObject = new DomainObject(strParentId).getAttributeValue(context, "Is Version Object");
										String strIsVersionObject = StringUtils.trimToEmpty( new DomainObject(strParentId).getInfo(context, "next.revision") );
										
										if ("".equals(strIsVersionObject)) {
											String strRevisePartId = ${CLASS:cdmPartLibrary}.revise(context, strPartObjectId, strParentId);
											System.out.println("CREATE EO Revise Part ObjectId     ::     " + strRevisePartId);
											${CLASS:cdmPartLibrary}.revisePartSetAttributeValueAndConnection(context, strPartObjectId, strECObjectId, strRevisePartId);
										}
										
									}else{
										
										String strRevisePartId = ${CLASS:cdmPartLibrary}.revise(context, strPartObjectId, strParentId);
										System.out.println("CREATE EO Revise Part ObjectId     .....     " + strRevisePartId);
										${CLASS:cdmPartLibrary}.revisePartSetAttributeValueAndConnection(context, strPartObjectId, strECObjectId, strRevisePartId);
										
									}
									
									
								}
								
							}
						
						} else {
							
							if(slList.contains(strPartObjectId)){
								
								String strRevisePartId = StringUtils.trimToEmpty(new DomainObject(strPartObjectId).getInfo(context, "next.id"));
								
								if(! "".equals(strRevisePartId)){
									strRevisePartId = ${CLASS:cdmPartLibrary}.revisePartParentStructure(context, strRevisePartId, strPartObjectId, strParentId);
									System.out.println("CREATE EO Revise Part ObjectId     ..     " + strRevisePartId);
								}
								
							}else{
								
								String strRevisePartId = ${CLASS:cdmPartLibrary}.revise(context, strPartObjectId, strParentId);
								System.out.println("CREATE EO Revise Part ObjectId     ...     " + strRevisePartId);
								${CLASS:cdmPartLibrary}.revisePartSetAttributeValueAndConnection(context, strPartObjectId, strECObjectId, strRevisePartId);
						
							}
							
						}

					}
					
					slList.add(strPartObjectId);
				}
			}

			returnMap.put("id", strECObjectId);
			ContextUtil.commitTransaction(context);

			return returnMap;

		} catch (Exception e) {
			ContextUtil.abortTransaction(context);
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * GetTable return Affected item Table
	 */
	@SuppressWarnings("rawtypes")
	@com.matrixone.apps.framework.ui.ProgramCallable
	public MapList getECTable(Context context, String[] args) throws Exception {
		Map paramMap = JPO.unpackArgs(args);
		String strECObjectId = (String) paramMap.get("objectId");
		
		MapList mlAffectedItems = new MapList();
		StringList objectSelects = new StringList();
		objectSelects.add(DomainConstants.SELECT_ID);
		
		DomainObject domECObj = new DomainObject(strECObjectId);
		mlAffectedItems = domECObj.getRelatedObjects(context, 
														DomainConstants.RELATIONSHIP_AFFECTED_ITEM, 		// Relationship
														cdmConstantsUtil.TYPE_CDMPART, // From Type name
														objectSelects, 					// objects Select
														null, 							// Rel selects
														false, 							// to Direction
														true, 							// from Direction
														(short) 1, 						// recusion level
														"", 
														"", 
														0);


		return mlAffectedItems;
	}
	
	
	/**
	 *  display first option on Affected Items Table.
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Vector getFirstOptionValue(Context context, String[] args) throws Exception {
        Vector columnValues = new Vector();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objList = (MapList) programMap.get("objectList");
            
            
            StringList busSelect = new StringList();
            busSelect.add("attribute[cdmPartOption1]");
            busSelect.add("attribute[cdmPartOptionLabelName]");
            
            for(int i=0; i<objList.size(); i++){
            	Map map = (Map) objList.get(i);
            	String strPartObjectId = (String) map.get(DomainConstants.SELECT_ID);
            	
            	
            	DomainObject doPart = new DomainObject(strPartObjectId);
            	Map mapPart = doPart.getInfo(context, busSelect);
            	
            	
            	String strOptionTitleAll = (String) mapPart.get("attribute[cdmPartOptionLabelName]");
            	String strOptionValue = (String) mapPart.get("attribute[cdmPartOption1]");
            	
            	
            	String strDisplayOptionString = "";
				if (StringUtils.isNotEmpty(strOptionTitleAll)) {

					String[] strTitleArray = strOptionTitleAll.split("\\|");
					String strTitle = strTitleArray[0];
					
					if(StringUtils.isNotEmpty(strTitle))
						strDisplayOptionString = strTitle + ":" + strOptionValue;

				} 
            	
            	columnValues.add(strDisplayOptionString);
            }
            
        }catch(Exception e){
        	throw e;
        }
        return columnValues;
	}
	
	
	/**
	 * display Drawing on Affected Items Table.
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Vector getDrawingNumber(Context context, String[] args) throws Exception {
long time0	= System.currentTimeMillis();
        Vector columnValues = new Vector();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objList = (MapList) programMap.get("objectList");
            
            
            Map findObjetIdMap = new HashMap();
            MapList mlDrawingObject = new MapList();
            
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
			String selectDrawingGet = "from["+DomainConstants.RELATIONSHIP_PART_SPECIFICATION+"].to."+DomainConstants.SELECT_ID;
			String selectDrawingSel = "from["+DomainConstants.RELATIONSHIP_PART_SPECIFICATION+"|to.type.kindof[cdmAutoCAD,cdmNXDrawing,CATDrawing]].to."+DomainConstants.SELECT_ID;
			StringList bSel = new StringList();
			bSel.add("attribute[cdmPartDrawingNo]");
			bSel.add(selectDrawingSel);
			
			BusinessObjectWithSelectList bwsl = BusinessObject.getSelectBusinessObjectData(context, bArr, bSel);
			
            for(int k=0; k<bwsl.size(); k++){	
				Map drwMap = new HashMap();
				
				String strDrawingNo = bwsl.getElement(k).getSelectData("attribute[cdmPartDrawingNo]");
				StringList slPartRelVehicleList = bwsl.getElement(k).getSelectDataList(selectDrawingGet);
				
				
				Map map = (Map) objList.get(k);
								
				String strPartObjectId = (String) map.get(DomainConstants.SELECT_ID);
				
				strDrawingNo = StringUtils.trimToEmpty(strDrawingNo);
				String strDrawingObjectId = null;
				
				if (slPartRelVehicleList != null && slPartRelVehicleList.size() > 0) {
					strDrawingObjectId = (String) slPartRelVehicleList.get(0);
				}

				drwMap.put("DRW_OID", strDrawingObjectId);
				drwMap.put("DRW_NO", strDrawingNo);
				
				if(StringUtils.isNotEmpty(strDrawingObjectId))
					findObjetIdMap.put(strDrawingNo, strDrawingObjectId);
				
				mlDrawingObject.add(drwMap);
				
			}
            
//20160306
/*
			for (int i = 0; i < objList.size(); i++) {
				Map map = (Map) objList.get(i);
				
				Map drwMap = new HashMap();
				
				
				String strPartObjectId = (String) map.get(DomainConstants.SELECT_ID);
				DomainObject doPart = new DomainObject(strPartObjectId);

				String strDrawingNo = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] { strPartObjectId, "attribute[cdmPartDrawingNo]" });
				strDrawingNo = StringUtils.trimToEmpty(strDrawingNo);
				
				
				String strDrawingObjectId = null;
				MapList mlPartSpec = doPart.getRelatedObjects(context, 
															DomainConstants.RELATIONSHIP_PART_SPECIFICATION, 	// relationship
															"cdmAutoCAD,cdmNXDrawing,CATDrawing", 	// type
															new StringList(DomainObject.SELECT_ID), 							// objects
															null,			 						// relationships
															false, 		// to
															true, 		// from
															(short) 1, 	// recurse
															null, 		// where
															null, 		// relationship where
															(short) 0); // limit

					
				if (mlPartSpec.size() > 0) {
					Map objMap = (Map) mlPartSpec.get(0);
					strDrawingObjectId = (String) objMap.get(DomainObject.SELECT_ID);
				}

				drwMap.put("DRW_OID", strDrawingObjectId);
				drwMap.put("DRW_NO", strDrawingNo);
				
				if(StringUtils.isNotEmpty(strDrawingObjectId))
					findObjetIdMap.put(strDrawingNo, strDrawingObjectId);
				
				mlDrawingObject.add(drwMap);
				
			}
*/
/*************************************************
*	End for performance 20170306
*************************************************/
			Map paramList = (HashMap) programMap.get("paramList");
			boolean isexport = false;
			String export = (String)paramList.get("exportFormat");
			if ( export != null ) {
				isexport = true;
			}

			//make url string
			for (Iterator iterator = mlDrawingObject.iterator(); iterator.hasNext();) {
				Map drwMap = (Map) iterator.next();

				String strDrawingObjectId = (String) drwMap.get("DRW_OID");
				String strDrawingNo = (String) drwMap.get("DRW_NO");

				if (strDrawingObjectId == null || strDrawingObjectId.equals("")) {
					strDrawingObjectId = (String) findObjetIdMap.get(strDrawingNo);
				}

				StringBuffer sb = new StringBuffer();

				if (StringUtils.isNotEmpty(strDrawingObjectId)) {

					String viewerURL = "../common/emxTree.jsp?objectId=" + strDrawingObjectId;

					sb.append("<a href=\"javascript:showNonModalDialog('" + viewerURL + "')\">");
					sb.append(strDrawingNo);
					sb.append("</a>");
				} else {
					sb.append(strDrawingNo);
				}

				
				
				if (isexport) {
					columnValues.add(strDrawingNo);
				} else {
					columnValues.add(sb.toString());
				}
			}
		}catch(Exception e){
			throw e;
		}
long time1	= System.currentTimeMillis();
System.out.println(">>>getDrawingNumber time1="+(time1-time0)+"("+(time1-time0)+")");
		return columnValues;
	}
	

	/**
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	public String exportEC(Context context, String[] args) throws Exception {
		String strSuccess = cdmConstantsUtil.TEXT_SUCCESS;

		Map paramMap = JPO.unpackArgs(args);
		String strECObjectId = (String) paramMap.get("objectId");

		try {

			int messageLimit = 1;
			
			MapList mlResult = this.exportValidate(context, strECObjectId, messageLimit);
			
			if(mlResult.size() != 0) {
				Map map = (Map)mlResult.get(0);
				String strValidation =  StringUtils.trimToEmpty((String) map.get("part"));
				
				if(! DomainConstants.EMPTY_STRING.equals(strValidation)){
					
					String strErrorMessage = EnoviaResourceBundle.getProperty(context,"emxEngineeringCentralStringResource",context.getLocale(),"emxEngineeringCentral.Alert.exportError");
					throw new Exception(strErrorMessage);
				}
			}
			
			ContextUtil.startTransaction(context, true);
			
			
			DomainObject domECObj = new DomainObject(strECObjectId);

			String strCDMOnly = domECObj.getInfo(context, "attribute[cdmECCategoryOnlyCDM]");

			if ("Yes".equals(strCDMOnly)) {

				this.promotePartDocument(context, strECObjectId, "Release");

				domECObj.setState(context, "Release");

			} else if ("No".equals(strCDMOnly)) {

				this.promotePartDocument(context, strECObjectId, "Review");
				
				domECObj.setState(context, "Review");

				// run a Job for sending data and file to PLM
				JPO.invoke(context, "cdmEC", null, "PLMExportBackgroundJob", JPO.packArgs(paramMap), String.class);
			}

			
			String strOwner = context.getUser();
			String strSiteName = MqlUtil.mqlCommand(context, "print person $1 select $2 dump $3", strOwner, "site.name", "|");

			if (StringUtils.isEmpty(strSiteName)) {
				strSiteName = "MDK";
			}
			
			ContextUtil.pushContext(context, null, null, null);
			MqlUtil.mqlCommand(context, "trigger off", new String[]{});
			
			domECObj.setAttributeValue(context, "cdmSiteName", strSiteName);
			
			MqlUtil.mqlCommand(context, "trigger on", new String[]{});
			ContextUtil.popContext(context);
			
			ContextUtil.commitTransaction(context);
		} catch (Exception e) {
			ContextUtil.abortTransaction(context);
			e.printStackTrace();
			return e.getMessage();
		}
		return strSuccess;
	}

	
	

	/**
	 * promote EC, Part, CAD Model and Drawings.
	 * 
	 * @param context
	 * @param strECId
	 * @param actionType 'Review' or 'Release'
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	public void promotePartDocument(Context context, String strECId, String actionType) throws Exception {

		try {

			StringList selectStmts = new StringList();
			selectStmts.add(DomainConstants.SELECT_ID);
			selectStmts.add("to[" + DomainConstants.RELATIONSHIP_EBOM + "].from.id");
			StringList selectRel = new StringList();
			selectRel.add(DomainRelationship.SELECT_LEVEL);

			Pattern typePattern = new Pattern("");
			typePattern.addPattern(cdmConstantsUtil.TYPE_CDMPART);
			typePattern.addPattern("DOCUMENTS");

			Pattern relPattern = new Pattern("");
			relPattern.addPattern(DomainConstants.RELATIONSHIP_AFFECTED_ITEM);
			relPattern.addPattern(DomainConstants.RELATIONSHIP_PART_SPECIFICATION);
			relPattern.addPattern(cdmConstantsUtil.RELATIONSHIP_ASSOCIATED_DRAWING);

			DomainObject eoObj = new DomainObject(strECId);
			MapList mlEbomPartList = eoObj.getRelatedObjects(context, relPattern.getPattern(), // relationship
																		typePattern.getPattern(), // type
																		selectStmts, 			// objects
																		selectRel, 			// relationships
																		false, 			// to
																		true, 			// from
																		(short) 3, 			// recurse
																		null, 			// where
																		null, 			// relationship where
																		(short) 0); 		// limit

			// Document has to be place prior to Part
			mlEbomPartList.sort(DomainRelationship.SELECT_LEVEL, "decending", "integer");

			for (int i = 0; i < mlEbomPartList.size(); i++) {
				Map mPartMap = (Map) mlEbomPartList.get(i);

				String strObjectId = (String) mPartMap.get(DomainConstants.SELECT_ID);
				DomainObject doObject = new DomainObject(strObjectId);
				
				// if object is a kind of Drawing, CAD
				if (doObject.isKindOf(context, "DOCUMENTS")) {

					

					if ("Review".equals(actionType)) {
						
						ContextUtil.pushContext(context, null, null, null);
	        			MqlUtil.mqlCommand(context, "trigger off", new String[]{});
						
						doObject.setState(context, "FROZEN");
						
						MqlUtil.mqlCommand(context, "trigger on", new String[]{});
	    				ContextUtil.popContext(context);
						
					} else if (cdmConstantsUtil.TEXT_RELEASE.equals(actionType)) {
						
						ContextUtil.pushContext(context, null, null, null);
	        			MqlUtil.mqlCommand(context, "trigger off", new String[]{});
	    				
						doObject.setState(context, "RELEASED");
						
						MqlUtil.mqlCommand(context, "trigger on", new String[]{});
	    				ContextUtil.popContext(context);
						
					}

					// if object is a kind of Part
				} else {

					String strPartId = strObjectId;
					String strHighLankPartId = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] { strPartId, "to[" + DomainConstants.RELATIONSHIP_EBOM + "].from.id" });

					DomainObject partObj = new DomainObject(strPartId);

					ContextUtil.pushContext(context);
					MqlUtil.mqlCommand(context, "trigger off", new String[] {});

					if ("Review".equals(actionType))
						partObj.setState(context, "Review");

					else if (cdmConstantsUtil.TEXT_RELEASE.equals(actionType))
						partObj.setState(context, "Release");

					MqlUtil.mqlCommand(context, "trigger on", new String[] {});
					ContextUtil.popContext(context);

					if ("Release".equals(actionType)) {

						BusinessObject strPreviousRev = partObj.getPreviousRevision(context);
						if (!"..".equals(strPreviousRev.toString())) {

							String strPreviousObjectId = strPreviousRev.getObjectId(context);
							String strPreviousObjectHighLankIds = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump ", new String[] { strPreviousObjectId, "to[" + DomainConstants.RELATIONSHIP_EBOM + "].from.id" }));

							if (!DomainConstants.EMPTY_STRING.equals(strPreviousObjectHighLankIds)) {
								String[] strPreviousObjectHighLankIdArray = strPreviousObjectHighLankIds.split(",");
								String strPreviousObjectHighLankId = DomainConstants.EMPTY_STRING;
								String strPreviousObjectHighLankRelId = DomainConstants.EMPTY_STRING;
								for (int k = 0; k < strPreviousObjectHighLankIdArray.length; k++) {
									strPreviousObjectHighLankId = strPreviousObjectHighLankIdArray[k]; // strPreviousObjectHighLankId
									strPreviousObjectHighLankRelId = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump ", new String[] { strPreviousObjectId, "to[" + DomainConstants.RELATIONSHIP_EBOM + "].id" }));
									if (strPreviousObjectHighLankId.equals(strHighLankPartId) && !DomainConstants.EMPTY_STRING.equals(strPreviousObjectHighLankRelId)) {
										MqlUtil.mqlCommand(context, "mod connection $1 type $2 ", new String[] { strPreviousObjectHighLankRelId, DomainConstants.RELATIONSHIP_EBOM_HISTORY });
									}
								}
							}
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * demote EO state to In-Work(Preliminary) when EO is rejected from PLM.
	 * @param context
	 * @param strECId
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	public void demotePartDocumentToInWork(Context context, String strECId) throws Exception {

		try {

			StringList selectStmts = new StringList();
			selectStmts.add(DomainConstants.SELECT_ID);
			
			
			StringList selectRel = new StringList();
			//selectRel.add(DomainRelationship.SELECT_LEVEL);

			Pattern typePattern = new Pattern("");
			typePattern.addPattern(cdmConstantsUtil.TYPE_CDMPART);
			typePattern.addPattern("DOCUMENTS");

			Pattern relPattern = new Pattern("");
			relPattern.addPattern(DomainConstants.RELATIONSHIP_AFFECTED_ITEM);
			relPattern.addPattern(DomainConstants.RELATIONSHIP_PART_SPECIFICATION);
			relPattern.addPattern(cdmConstantsUtil.RELATIONSHIP_ASSOCIATED_DRAWING);

			
			// EC -> Part -> 3D -> 2D
			// or
			// EC -> Part -> 2D
			
			DomainObject eoObj = new DomainObject(strECId);
			MapList mlEbomPartList = eoObj.getRelatedObjects(context, relPattern.getPattern(), // relationship
																		typePattern.getPattern(), // type
																		selectStmts, // objects
																		selectRel, // relationships
																		false, // to
																		true, // from
																		(short) 3, // recurse
																		null, // where
																		null, // relationship where
																		(short) 0); // limit

			// Document has to be place prior to Part
			mlEbomPartList.sort(DomainRelationship.SELECT_LEVEL, "decending", "integer");

			
			
			try {
				ContextUtil.pushContext(context);
				MqlUtil.mqlCommand(context, "trigger off", new String[] {});
				
				
				
				for (int i = 0; i < mlEbomPartList.size(); i++) {
					Map mPartMap = (Map) mlEbomPartList.get(i);

					String strObjectId = (String) mPartMap.get(DomainConstants.SELECT_ID);
					DomainObject doObject = new DomainObject(strObjectId);


					// if object is a kind of Part
					if (doObject.isKindOf(context, DomainConstants.TYPE_PART)) {
						
						doObject.setState(context, "Preliminary");

						// if object is a kind of Drawing, CAD
					} else {
						doObject.setState(context, "IN_WORK");
					}
				}
				
				
			} catch (Exception e) {
				throw e;
			} finally {
				MqlUtil.mqlCommand(context, "trigger on", new String[] {});
				ContextUtil.popContext(context);
			}
			

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * exports parts to PLM System EC state Trigger
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	/*
	 * @SuppressWarnings({ "deprecation", "rawtypes" }) public int
	 * setExportPromote(Context context, String[] args) throws Exception {
	 * String strECObjectId = args[0];
	 * 
	 * try { ContextUtil.startTransaction(context, true); DomainObject domECObj
	 * = new DomainObject(strECObjectId);
	 * 
	 * StringList objectSelects = new StringList();
	 * objectSelects.add(DomainConstants.SELECT_ID);
	 * objectSelects.add(DomainConstants.SELECT_CURRENT);
	 * 
	 * MapList mlECObjectList = domECObj.getRelatedObjects(context,
	 * DomainConstants.RELATIONSHIP_AFFECTED_ITEM, // Relationship
	 * cdmConstantsUtil.TYPE_CDMPART, // From Type name objectSelects, //
	 * objects Select null, // Rel selects false, // to Direction true, // from
	 * Direction (short) 1, // recusion level "", "", 0);
	 * 
	 * DomainObject domPartSpecObjectId = new DomainObject(); DomainObject
	 * domAssociatedObjectId = new DomainObject();
	 * 
	 * String strPartSpecificationObjectCheck = ""; String
	 * strPartSpecificationObject = ""; String strAssociatedObjectCheck = "";
	 * String strAssociatedObject = "";
	 * 
	 * String strCurrent = "";
	 * 
	 * // Check. CAT Drawing & CAT Part. if (strECObjectId != null) { for (int l
	 * = 0; l < mlECObjectList.size(); l++) { String id = ""; Map tempMap =
	 * (Map) mlECObjectList.get(l); id = (String) tempMap.get("id");
	 * 
	 * DomainObject domItemsId = new DomainObject(id);
	 * 
	 * strCurrent = domItemsId.getCurrentState(context).getName();
	 * 
	 * if (!strCurrent.equals("Release") && !strCurrent.equals("Obsolete")) {
	 * 
	 * // Check. CAT Drawing. String strPartSpecificationObjectId =
	 * MqlUtil.mqlCommand(context, "print bus '" + id + "' select from[" +
	 * DomainConstants.RELATIONSHIP_PART_SPECIFICATION + "].to.id dump | "); //
	 * Check. CAT Part. String strAssociatedObjectId =
	 * MqlUtil.mqlCommand(context, "print bus '" + id + "' select from.to.to[" +
	 * cdmConstantsUtil.RELATIONSHIP_ASSOCIATED_DRAWING + "].from.id dump |");
	 * 
	 * StringList strPartSpecificationObjectIds =
	 * FrameworkUtil.split(strPartSpecificationObjectId, "|"); StringList
	 * strAssociatedObjectIds = FrameworkUtil.split(strAssociatedObjectId, "|");
	 * 
	 * // Promote. CAT Drawing. if (!"".equals(strPartSpecificationObjectId)) {
	 * for (int j = 0; j < strPartSpecificationObjectIds.size(); j++) {
	 * strPartSpecificationObject = (String)
	 * strPartSpecificationObjectIds.get(j); domPartSpecObjectId = new
	 * DomainObject(strPartSpecificationObject);
	 * 
	 * strPartSpecificationObjectCheck =
	 * domPartSpecObjectId.getCurrentState(context).getName();
	 * 
	 * if (!strPartSpecificationObjectCheck.equals("Release") &&
	 * !strPartSpecificationObjectCheck.equals("Obsolete") &&
	 * !strPartSpecificationObjectCheck.equals("Exists")) {
	 * ContextUtil.pushContext(context, null, null, null);
	 * MqlUtil.mqlCommand(context, "trigger off", new String[]{});
	 * domPartSpecObjectId.promote(context);
	 * 
	 * MqlUtil.mqlCommand(context, "trigger on", new String[]{});
	 * ContextUtil.popContext(context); }
	 * 
	 * } } // Promote. CAT Part. if (!"".equals(strAssociatedObjectId)) { for
	 * (int k = 0; k < strAssociatedObjectIds.size(); k++) { strAssociatedObject
	 * = (String) strAssociatedObjectIds.get(k); domAssociatedObjectId = new
	 * DomainObject(strAssociatedObject); strAssociatedObjectCheck =
	 * domAssociatedObjectId.getCurrentState(context).getName();
	 * 
	 * if (!strAssociatedObjectCheck.equals("Release") &&
	 * !strAssociatedObjectCheck.equals("Obsolete") &&
	 * !strAssociatedObjectCheck.equals("Exists")) {
	 * ContextUtil.pushContext(context, null, null, null);
	 * MqlUtil.mqlCommand(context, "trigger off", new String[]{});
	 * domAssociatedObjectId.promote(context); MqlUtil.mqlCommand(context,
	 * "trigger on", new String[]{}); ContextUtil.popContext(context); } } }
	 * domItemsId.promote(context); } } }
	 * ContextUtil.commitTransaction(context); } catch (Exception e) {
	 * ContextUtil.abortTransaction(context); e.printStackTrace(); throw e; }
	 * return 0; }
	 */

	/**
	 * BackgroundJob.
	 * 
	 * @param context
	 * @param args
	 * @return : "Succeedes"
	 * @throws Exception
	 */

	// 
	@SuppressWarnings("rawtypes")
	public String PLMExportBackgroundJob(Context context, String[] args) throws Exception {
		Job job = null;
		try {
			Map paramMap = JPO.unpackArgs(args);
			String strECObjectId = (String) paramMap.get("objectId");

			String jponame = "cdmEC";
			String methodName = "executeBackgroundJob";
			String[] params = { strECObjectId };

			job = new Job(jponame, methodName, params);


			job.setNotifyOwner("No");

			job.setTitle(MqlUtil.mqlCommand(context, "print bus $1 select name dump ", new String[] { strECObjectId }));
			job.createAndSubmit(context);
			
			
			ContextUtil.pushContext(context);
			DomainRelationship.connect(context, job, "cdmJobECO", new DomainObject(strECObjectId));
			ContextUtil.popContext(context);
			
			

		} catch (Exception ex) {
			job.setErrorMessage(ex.getMessage());
			ex.printStackTrace();
		} finally {

		}
		return "Succeeded";
	}

	/**
	 * SendMail method
	 * 
	 * @throws MessagingException
	 */
	// SendMail. (RecipientType.TO, new InternerAddress("To Person");
	public void sendMail(Context context, String[] args) throws MessagingException {
		// Select Server or Domain Address.
		Properties props = System.getProperties();
		props.put("mail.transport.protocol", "smtp");
		props.put("mail.smtp.host", "mail.mando.com");
		props.put("mail.smtp.user", "plm");
		props.setProperty("mail.smtp.host", "mail.mando.com");

		String strEcoNo = args[0];
		
		try {
		
			String strUserEmail = MqlUtil.mqlCommand(context, "print person $1 select $2 dump", context.getUser(), "email");
			
			
			// Create Mail Session.
			Session session = Session.getDefaultInstance(props);
			MimeMessage msg = new MimeMessage(session);

			
			String strSubject = "[CDM] Notifying Error Occurred when sending Drawing.";
			String strContent = "Error Occurred when sending Drawing in EO[" + strEcoNo + "]";
			strContent += "<BR> Please contact your administrator.";
			
			
			// setting to, from, message.
			msg.setFrom(new InternetAddress("plm@mando.com"));
			
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress("seokju.park@halla.com"));
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress(strUserEmail));
			msg.setSubject(strSubject);
			msg.setContent(strContent, "text/html; charset=utf-8");

			// Send to mail.
			Transport.send(msg);
		} catch (Exception e) {
			
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param context
	 * @param args
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void executeBackgroundJob(Context context, String[] args) throws Exception {

		SqlSession sqlSession = null;
		String strECNumber = null;
		String strECObjectId = args[0];

		try {
			System.out.println(" EC  ObjectId     =     "+strECObjectId);
			DomainObject ecDomObj = new DomainObject(strECObjectId);
			strECNumber = ecDomObj.getInfo(context, DomainObject.SELECT_NAME);
			
			SqlSessionUtil.reNew("plm");
		    sqlSession = SqlSessionUtil.getSqlSession();
			
			this.sendPartBOMDataToPLM(context, sqlSession, args);
			this.sendDrawingFilesToPLM(context, args);

			
			String strSiteName = ecDomObj.getInfo(context, "attribute[cdmSiteName]");
			if(StringUtils.isEmpty(strSiteName)) {
				strSiteName = "MDK";
			}
			
			String KEY_PLM_FTP_SERVER 		= strSiteName + "_" + "FTP_SERVER";
			
			String PLM_FTP_SERVER 			= cdmPropertiesUtil.getPropValue("FTP.properties", KEY_PLM_FTP_SERVER);
			
			
			Map updateEOMasterFTPMap = new HashMap();
			updateEOMasterFTPMap.put("FTPIP", PLM_FTP_SERVER);
			updateEOMasterFTPMap.put("EO_NUMBER", strECNumber);
			sqlSession.update("updateFTP_IP_EOMaster", updateEOMasterFTPMap);
			
			System.out.println("================================== FTP IP ==================================");
			
			
			// update flag ec state
			Map updateEOMasterCDMFlagMap = new HashMap();
			updateEOMasterCDMFlagMap.put("CDM_FLAG", "R");
			updateEOMasterCDMFlagMap.put("EO_NUMBER", strECNumber);
			sqlSession.update("updateEOMaster", updateEOMasterCDMFlagMap);

			System.out.println("================================== CDM FLAG ================================");
			
			
			sqlSession.commit();

		} catch (Exception e) {
			sqlSession.rollback();
			
			DomainObject ecDomObj = new DomainObject(strECObjectId);
			//String strJobId = ecDomObj.getInfo(context, "to[cdmJobECO].from.id");
			
			StringList busSelect = new StringList();
			busSelect.add(DomainObject.SELECT_ID);
			busSelect.add(DomainObject.SELECT_ORIGINATED);
			
			
			MapList mlJobList = ecDomObj.getRelatedObjects(context, 
															"cdmJobECO", // relationship
															"Job", // type
															busSelect, 				// objects
															null, 					// relationships
															true, 						// to
															false, 						// from
															(short) 1, 					// recurse
															null, 						// where
															null, 						// relationship where
															(short) 0); 				// limit

			mlJobList.sort(DomainObject.SELECT_ORIGINATED, "descending", "date");

			if (mlJobList.size() > 0) {

				Map jobMap = (Map) mlJobList.get(0);

				String strJobId = (String) jobMap.get(DomainObject.SELECT_ID);

				DomainObject doJob = new DomainObject(strJobId);

				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				String exceptionAsString = sw.toString();

				doJob.setDescription(context, exceptionAsString);
			}
			
			
			try {
				this.sendMail(context, new String[]{strECNumber});
			} catch (Exception ex) {
				System.out.println("Fail to send mail.");
			}
		}finally {
			
			if(sqlSession != null)
				sqlSession.close();
			
		}
	}
	
	
	public static void main(String[] args) throws Exception{
		
		try {
			//double a = Double.parseDouble("1.0");
		    //System.out.println(String.valueOf((int) a));
			
			Context context = new Context("");
			context.setUser("admin_platform");//p13941
			context.setPassword("Qwer1234");
			context.connect();
			
			
//			SelectList selectStmts = new SelectList();
//			selectStmts.add(DomainObject.SELECT_ID);
//			selectStmts.add(DomainObject.SELECT_TYPE);
//			selectStmts.add(DomainObject.SELECT_NAME);
//			selectStmts.add(DomainObject.SELECT_REVISION);
//			selectStmts.add("attribute[cdmPartDrawingNo]");
//			selectStmts.add("to["+DomainConstants.RELATIONSHIP_PART_SPECIFICATION+"].from.name");
//			selectStmts.add("to["+DomainConstants.RELATIONSHIP_PART_SPECIFICATION+"].from.revision");
//			
//
//			SelectList selectRel = new SelectList();
			
			
//			MapList ml2DList = new DomainObject("47604.45839.2832.5572").getRelatedObjects(context, 
//					DomainConstants.RELATIONSHIP_PART_SPECIFICATION, // relationship
//					"cdmOrCADProduct", 			// type
//					selectStmts, 				// objects
//					selectRel, 					// relationships
//					false, 						// to
//					true, 						// from
//					(short) 1, 					// recurse
//					null, 						// where
//					null, 						// relationship where
//					(short) 0); 				// limit
			
			
			
//			MapList ml2DList = new DomainObject("47604.45839.32289.7659").getRelatedObjects(context, 
//					DomainConstants.RELATIONSHIP_PART_SPECIFICATION, // relationship
//					"CATDrawing", 			// type
//					selectStmts, 				// objects
//					selectRel, 					// relationships
//					false, 						// to
//					true, 						// from
//					(short) 1, 					// recurse
//					null, 						// where
//					null, 						// relationship where
//					(short) 0); 				// limit
//			
//			System.out.println("ml2DList        "+ml2DList);
//			
//			
//			
//			
//			
//			StringBuffer strDrawingBuffer = new StringBuffer();
//	    	int i2DListSize = ml2DList.size();
//	    	for(int i=0; i<i2DListSize; i++){
//	    		Map drawingMap = (Map)ml2DList.get(i);
//	    		String strId       = (String)drawingMap.get(DomainObject.SELECT_ID);
//	    		String strType     = (String)drawingMap.get(DomainObject.SELECT_TYPE);
//	    		String strName     = (String)drawingMap.get(DomainObject.SELECT_NAME);
//	    		String strRevision = (String)drawingMap.get(DomainObject.SELECT_REVISION);
//	    		
//	    		String strDrawingRelPartName       = (String)drawingMap.get("to["+DomainConstants.RELATIONSHIP_PART_SPECIFICATION+"].from.name");
//	    		
//	    		String strDrawingRelPartRevision   = (String)drawingMap.get("to["+DomainConstants.RELATIONSHIP_PART_SPECIFICATION+"].from.revision");
//	    		
//	    		
//	    		if ("CATDrawing".equals(strType)) {
//	    			
//	    			
//	    			DomainObject catDrawingObj = new DomainObject(strId);
//	    			MapList mlDerivedOutputList = catDrawingObj.getRelatedObjects(context, 
//																		"Derived Output", 			// relationship
//																		"Derived Output", 			// type
//																		selectStmts, 				// objects
//																		selectRel, 					// relationships
//																		false, 						// to
//																		true, 						// from
//																		(short) 1, 					// recurse
//																		null, 						// where
//																		null, 						// relationship where
//																		(short) 0); 				// limit
//	    			
//	    			
//	    			System.out.println("mlDerivedOutputList          "+mlDerivedOutputList);
//	    			
//	    			for(int k=0; k<mlDerivedOutputList.size(); k++){
////	    			for(int k=0; k<1; k++){
//	    				Map derivedOutputMap = (Map)mlDerivedOutputList.get(k);
//	    				String strDerivedOutputId = (String)derivedOutputMap.get(DomainObject.SELECT_ID);
//	    				
//	    				DomainObject outputDomObj = new DomainObject(strDerivedOutputId);
//	    				FileList fileList = outputDomObj.getFiles(context);
//	    				
//	    				for (Iterator iterator = fileList.iterator(); iterator.hasNext();) {
//	    					matrix.db.File objectFile = (matrix.db.File) iterator.next();
//	    					
//	    					String strFileName = objectFile.getName();
//	    					String extensionsName = strFileName.substring(strFileName.lastIndexOf(".") + 1);
//	    					extensionsName = extensionsName.toLowerCase();
//	    					
//	    					if ( "dwg".equals(extensionsName) ) {
//	    						extensionsName = "zip";
//	    					} 
//	    					
//	    					strDrawingBuffer.append(strDrawingRelPartName);
//	    					strDrawingBuffer.append("_");
//	    					strDrawingBuffer.append(strDrawingRelPartRevision);
//	    					strDrawingBuffer.append(".");
//	    					strDrawingBuffer.append(extensionsName);
//	    					
//	    					
//	    					if(iterator.hasNext()){
//	    						strDrawingBuffer.append(";");	
//	    	    			}
//	    					
//	    					//break;
//	    				}
//	    				
//	    			}
//	    			
//	    			System.out.println("!!!!!!!!!!!!strDrawingBuffer      "+strDrawingBuffer.toString());
//	    			
//	    		}
//				
//	    	}	
			
			
			
			
			
			
			
//			String strOwner = context.getUser();
//			System.out.println("strOwner        "+strOwner);
//			String strSiteName = MqlUtil.mqlCommand(context, "print person $1 select $2 dump $3", strOwner, "site.name", "|");
//			System.out.println("strSiteName     "+strSiteName);	
//			
//			if (StringUtils.isEmpty(strSiteName)) {
//				strSiteName = "MDK";
//			}
//			System.out.println("strSiteName     "+strSiteName);
//			
//			new DomainObject("47604.45839.54672.39637").setAttributeValue(context, "cdmSiteName", strSiteName);
//			System.out.println("##########################################");
			
			
//			SelectList selectStmts = new SelectList();
//			selectStmts.add("id");
//			selectStmts.add("name");
//			selectStmts.add("attribute[cdmPartDrawingNo]");
//			
//			SelectList selectRel = new SelectList();
//			
//			StringBuffer strBufferWhere = new StringBuffer();
//    		strBufferWhere.append(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO);
//    		strBufferWhere.append(" == '");
//    		strBufferWhere.append("SK303C5200");
//    		strBufferWhere.append("' && ");
//			strBufferWhere.append(DomainConstants.SELECT_ID);
//			strBufferWhere.append(" != ");
//			strBufferWhere.append("47604.45839.62748.22746");
//    		
//			DomainObject partDomObj = new DomainObject("47604.45839.62748.22746");
//    		MapList mlManyPartOneDrawingList = partDomObj.getRelatedObjects(context, 
//															DomainConstants.RELATIONSHIP_EBOM, 			// relationship
//															cdmConstantsUtil.TYPE_CDMMECHANICALPART, 	// type
//															selectStmts, 							// objects
//															selectRel, 								// relationships
//															false, 									// to
//															true, 									// from
//															(short) 1, 								// recurse
//															strBufferWhere.toString(), 									// where
//															null, 									// relationship where
//															(short) 0); 							// limit
//    		
//    		System.out.println("mlManyPartOneDrawingList     "+mlManyPartOneDrawingList);
			
			
//			String strMqlValue = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "temp query bus $1 $2 $3 select $4 $5 $6 dump $7", new String[] {"cdmPart", "SH412A1300", "01", "id", "name", "revision", "|"}));  
//            
//			System.out.println("strMqlValue      "+strMqlValue);
//			
//			if(! DomainConstants.EMPTY_STRING.equals(strMqlValue)){
//        		
//        		StringTokenizer valueTokens = new StringTokenizer(strMqlValue, "\n", false);
//	            
//	            while(valueTokens.hasMoreTokens()) {
//	            	
//	                String resultToken = valueTokens.nextToken();
//	               	StringList resultList = FrameworkUtil.split(resultToken, "|");
//	               	String strPartObjectId = StringUtils.trimToEmpty((String)resultList.get(3));
//	               	String strPartObjectName = StringUtils.trimToEmpty((String)resultList.get(4));
//	               	String strPartObjectRevision = StringUtils.trimToEmpty((String)resultList.get(5));
//	               	
//	               	System.out.println("strPartObjectId      		"+strPartObjectId);
//	               	System.out.println("strPartObjectName      		"+strPartObjectName);
//	               	System.out.println("strPartObjectRevision      	"+strPartObjectRevision);
//	               	
//	               	
//	            }
//	            
//			}
			
			
//			String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
//			DomainObject partDomObj = new DomainObject("47604.45839.6364.63057");
			
//			String strPartFamilyBlockCodeName = "EW121:1ST GEAR ASS'Y";
//			String aaa = "attribute["+cdmConstantsUtil.ATTRIBUTE_CDM_PART_FAMILY_BLOCK_CODE_NAME+"] == \""+strPartFamilyBlockCodeName+"\" ";
//			System.out.println("aaa     =     "+aaa);
//			
//			String strMqlValue = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "temp query bus $1 $2 $3 where $4 select $5 dump $6", new String[] {"Part Family", "*", "*", "attribute["+cdmConstantsUtil.ATTRIBUTE_CDM_PART_FAMILY_BLOCK_CODE_NAME+"] == \""+strPartFamilyBlockCodeName+"\" ", DomainConstants.SELECT_ID, "|"}));
//			System.out.println("strMqlValue     =     "+strMqlValue);
			
	
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	public static String extractVal(Object valObj) throws Exception {

		String[] strArr = {};
	    if (valObj != null && valObj.getClass() == strArr.getClass()) {
	        return ((String[]) valObj)[0];
	    } else if (valObj != null && valObj.getClass() == String.class) {
	        return (String) valObj;
	    } else {
	        return "";
	    }
	    
	}

	
	private Map returnNewPartParamMap (Context context, String strPartObjectId, SqlSession sqlSession) throws Exception {
		
		Map dbPartDataMap = new HashMap();
		String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
		DomainObject partDomObj = new DomainObject(strPartObjectId);
		
		SelectList slBusList = new SelectList();
		slBusList.add(cdmConstantsUtil.SELECT_TYPE											);
		slBusList.add(cdmConstantsUtil.SELECT_NAME                                    		);
		slBusList.add(cdmConstantsUtil.SELECT_REVISION                                		);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_NAME                		);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_TYPE                 		);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DESCRIPTION          		);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_UOM                  		);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION_ETC           		);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PHASE                		);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_APPROVAL_TYPE        		);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_CAD_WEIGHT           		);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_CHANGE_REASON        		);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_ERP_INTERFACE        		);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_ESTIMATED_WEIGHT    		);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_GLOBAL            			);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_INVESTOR            		);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_IS_CASTING           		);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_ITEM_TYPE            		);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_SIZE                		);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_REAL_WEIGHT         		);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OEM_ITEM_NUMBER      		);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PUBLISHING_TARGET    		);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_SURFACE_TREATMENT    		);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION_DESCRIPTION   		);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_STANDARD_BOM        		);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PLM_MATERIAL_CO_SIGN_YN	);
		slBusList.add("attribute[cdmPartEstimateWeightUnit]");
		
		
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PLM_SURFACE_TREATMENT_CO_SIGN_YN 	);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION1	);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION2	);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION3	);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION4	);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION5	);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION6	);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION7	);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION8	);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION9	);
		slBusList.add("attribute[cdmPartOption10]");
		slBusList.add("attribute[cdmPartOption11]");
		slBusList.add("attribute[cdmPartOption12]");
		slBusList.add("attribute[cdmPartOption13]");
		slBusList.add("attribute[cdmPartOption14]");
		slBusList.add("attribute[cdmPartOption15]");
		slBusList.add("attribute[cdmPartOption16]");
		slBusList.add("attribute[cdmPartOption17]");
		slBusList.add("attribute[cdmPartOption18]");
		slBusList.add("attribute[cdmPartOption19]");
		slBusList.add("attribute[cdmPartOption20]");
		
		
		slBusList.add("previous.revision"                                      	);
		slBusList.add(DomainConstants.SELECT_OWNER                              );
		slBusList.add(DomainConstants.SELECT_ORIGINATED                         );
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PLM_OBJECTID   );
		slBusList.add("to["+DomainConstants.RELATIONSHIP_EBOM+"].from.id"       );
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO		);
		
		slBusList.add("to["+DomainConstants.RELATIONSHIP_AFFECTED_ITEM+"].from.name");
		slBusList.add("to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PRODUCT_TYPE+"].from."+displayTitle);                                       
		slBusList.add("to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PROJECT+"].from.attribute["+cdmConstantsUtil.ATTRIBUTE_CDM_PROJECT_CODE+"]");  
		slBusList.add("to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PROJECT+"].from.id");
		slBusList.add("to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PROJECT_TYPE+"].from."+displayTitle);                                       
		slBusList.add("to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_VEHICLE+"].from."+displayTitle);                                          
		                                                                                                                                  
		
		
		Map partObjectDataMap = partDomObj.getInfo(context, slBusList);
		
		partObjectDataMap.get(cdmConstantsUtil.SELECT_TYPE);
		
		String strPartObjectType        = (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_TYPE);
		String strPartObjectName        = (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_NAME);    
        String strPartObjectRevision    = (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_REVISION);
        String strPartName              = (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_NAME);    
        String strPartType              = (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_TYPE);    
        String strPartComments          = (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DESCRIPTION);
        String strPartUnitOfMeasure     = (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_UOM);
        String strPartOptionETC         = (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION_ETC);
        String strPartPhase             = (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PHASE); 
        String strPartApprovalType      = (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_APPROVAL_TYPE);
        String strPartCADWeight         = (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_CAD_WEIGHT);
        String strPartChangeReason      = (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_CHANGE_REASON);    
        String strPartERPInterface      = (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_ERP_INTERFACE);
        String strPartGlobal            = (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_GLOBAL);
        String strPartInvestor          = (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_INVESTOR);    
        String strPartIsCasting         = (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_IS_CASTING);
        String strPartItemType          = (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_ITEM_TYPE);    
        String strPartSize              = (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_SIZE);
        String strPartRealWeight        = (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_REAL_WEIGHT);
        String strPartOEMItemNumber     = (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OEM_ITEM_NUMBER);
        String strPartPublishingTarget  = (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PUBLISHING_TARGET);
        String strPartSurfaceTreatment  = (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_SURFACE_TREATMENT);
        String strPartOptionDescription = (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION_DESCRIPTION);
        String strPartStandardBOM       = (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_STANDARD_BOM);
        String strPartMaterialCoSignYN  = (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PLM_MATERIAL_CO_SIGN_YN);
        String strPartOwner             = (String) partObjectDataMap.get(DomainConstants.SELECT_OWNER);                         
        String strPartOriginated        = (String) partObjectDataMap.get(DomainConstants.SELECT_ORIGINATED);
        String strDrawingNo        		= (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO);
        String strPartSurfaceTreatmentCoSignYN 
										= (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PLM_SURFACE_TREATMENT_CO_SIGN_YN);
        String strPartEstimatedWeight   = StringUtils.trimToEmpty((String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_ESTIMATED_WEIGHT));    
        String strPartEstimateWeightUnit  
        								= StringUtils.trimToEmpty((String) partObjectDataMap.get("attribute[cdmPartEstimateWeightUnit]"));
        								
        								
        String strPartOption1           = StringUtils.trimToEmpty((String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION1));  
        String strPartOption2           = StringUtils.trimToEmpty((String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION2));  
        String strPartOption3           = StringUtils.trimToEmpty((String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION3));  
        String strPartOption4           = StringUtils.trimToEmpty((String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION4));  
        String strPartOption5           = StringUtils.trimToEmpty((String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION5));  
        String strPartOption6           = StringUtils.trimToEmpty((String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION6));  
        String strPartOption7           = StringUtils.trimToEmpty((String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION7));  
        String strPartOption8           = StringUtils.trimToEmpty((String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION8));  
        String strPartOption9           = StringUtils.trimToEmpty((String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION9));
        String strPartOption10          = StringUtils.trimToEmpty((String) partObjectDataMap.get("attribute[cdmPartOption10]"));
        String strPartOption11          = StringUtils.trimToEmpty((String) partObjectDataMap.get("attribute[cdmPartOption11]"));
        String strPartOption12          = StringUtils.trimToEmpty((String) partObjectDataMap.get("attribute[cdmPartOption12]"));
        String strPartOption13          = StringUtils.trimToEmpty((String) partObjectDataMap.get("attribute[cdmPartOption13]"));
        String strPartOption14          = StringUtils.trimToEmpty((String) partObjectDataMap.get("attribute[cdmPartOption14]"));
        String strPartOption15          = StringUtils.trimToEmpty((String) partObjectDataMap.get("attribute[cdmPartOption15]"));
        String strPartOption16          = StringUtils.trimToEmpty((String) partObjectDataMap.get("attribute[cdmPartOption16]"));
        String strPartOption17          = StringUtils.trimToEmpty((String) partObjectDataMap.get("attribute[cdmPartOption17]"));
        String strPartOption18          = StringUtils.trimToEmpty((String) partObjectDataMap.get("attribute[cdmPartOption18]"));
        String strPartOption19          = StringUtils.trimToEmpty((String) partObjectDataMap.get("attribute[cdmPartOption19]"));
        String strPartOption20          = StringUtils.trimToEmpty((String) partObjectDataMap.get("attribute[cdmPartOption20]"));
        
        
        String strPreviousRevision      = StringUtils.trimToEmpty((String) partObjectDataMap.get("previous.revision"));                                 
        String strPartPLMObjectId       = StringUtils.trimToEmpty((String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PLM_OBJECTID));
        String strParentPartYN          = StringUtils.trimToEmpty((String) partObjectDataMap.get("to["+DomainConstants.RELATIONSHIP_EBOM+"].from.id")); 
        
        String strPartEOName            = StringUtils.trimToEmpty((String) partObjectDataMap.get("to["+DomainConstants.RELATIONSHIP_AFFECTED_ITEM+"].from.name"));
        String strPartProductType       = StringUtils.trimToEmpty((String) partObjectDataMap.get("to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PRODUCT_TYPE+"].from."+displayTitle));
        String strPartProject           = StringUtils.trimToEmpty((String) partObjectDataMap.get("to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PROJECT+"].from.attribute["+cdmConstantsUtil.ATTRIBUTE_CDM_PROJECT_CODE+"]"));
        String strPartProjectType       = StringUtils.trimToEmpty((String) partObjectDataMap.get("to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PROJECT_TYPE+"].from."+displayTitle));
        StringList slVehicleRelIdList   = (StringList) partDomObj.getInfoList(context, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_VEHICLE+"].from."+displayTitle);
        
        String strPartProjectId         = StringUtils.trimToEmpty((String) partObjectDataMap.get("to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_PROJECT+"].from.id"));
    	String strSystem = "";
        if(! DomainConstants.EMPTY_STRING.equals(strPartProjectId)){
        	strSystem = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", strPartProjectId, "to[cdmProjectGroupObjectTypeRelationShip].from.to[cdmProjectGroupObjectTypeRelationShip].from.to[cdmProjectGroupObjectTypeRelationShip].from.attribute[cdmProjectCode]"));
        }
        
        
        strPartApprovalType  = i18nNow.getRangeI18NString(cdmConstantsUtil.ATTRIBUTE_CDM_PART_APPROVAL_TYPE, strPartApprovalType, context.getSession().getLanguage());
        //strPartType 		 = i18nNow.getRangeI18NString(cdmConstantsUtil.ATTRIBUTE_CDM_PART_TYPE, strPartType, context.getSession().getLanguage());
        strPartIsCasting     = i18nNow.getRangeI18NString(cdmConstantsUtil.ATTRIBUTE_CDM_PART_IS_CASTING, strPartIsCasting, context.getSession().getLanguage());
        strPartUnitOfMeasure = i18nNow.getRangeI18NString(cdmConstantsUtil.ATTRIBUTE_CDM_PART_UOM, strPartUnitOfMeasure, context.getSession().getLanguage());
        
        if("YES".equals(strPartStandardBOM)){
        	strPartStandardBOM = "Y";
        }else if("NO".equals(strPartStandardBOM)){
        	strPartStandardBOM = "";
        }
        
        if(DomainConstants.EMPTY_STRING.equals(strParentPartYN)){
        	strParentPartYN = "Y";
        }else{
        	strParentPartYN = "";
        }
        
        
        String strPartMaterial = DomainConstants.EMPTY_STRING;
        String strPartSurface  = DomainConstants.EMPTY_STRING;
        
        if(! DomainConstants.EMPTY_STRING.equals(strPartPLMObjectId)){
        	String[] strPartPLMObjectIdArray = strPartPLMObjectId.split("\\|");
        	strPartMaterial = strPartPLMObjectIdArray[0];
        	strPartSurface  = strPartPLMObjectIdArray[1];
        	
        	strPartMaterial = strPartMaterial.replace("none", "");
            strPartSurface = strPartSurface.replace("none", "");
        }
        
        StringBuffer strVehicleBuffer = new StringBuffer();
    	if(slVehicleRelIdList.size()>0){
    		for (int k=0; k<slVehicleRelIdList.size(); k++) {
    			strVehicleBuffer.append(slVehicleRelIdList.get(k));
    			if(slVehicleRelIdList.size()-1 != k){
    				strVehicleBuffer.append(";");	
    			}
    		}	
    	}
    	
    	
    	if(strPartObjectType.equals(cdmConstantsUtil.TYPE_CDMMECHANICALPART) ){
    		
    		String strKstockYN = strPartObjectName.substring(0, 1);
    		if("K".equals(strKstockYN)){
    			strPartObjectType = "2";
    		}else{
    			strPartObjectType = "1";	
    		}
    		
    	}else if(strPartObjectType.equals(cdmConstantsUtil.TYPE_CDMPHANTOMPART)){
    		strPartObjectType = "3";
    	}else if(strPartObjectType.equals(cdmConstantsUtil.TYPE_CDM_ELECTRONIC_ASSEMBLY_PART) || strPartObjectType.equals(cdmConstantsUtil.TYPE_CDM_ELECTRONIC_PART)){
    		strPartObjectType = "4";
    	}
    	
    	String strDate = convertDateFormat(strPartOriginated, "MM/dd/yyyy hh:mm:ss a", "yyyy-MM-dd");
    	SimpleDateFormat transFormat = new SimpleDateFormat("yyyy-MM-dd");
    	Date dCreateOriginated = transFormat.parse(strDate);
    	
    	
    	
    	SelectList selectStmts = new SelectList();
		selectStmts.add(DomainObject.SELECT_ID);
		selectStmts.add(DomainObject.SELECT_TYPE);
		selectStmts.add(DomainObject.SELECT_NAME);
		selectStmts.add(DomainObject.SELECT_REVISION);
		selectStmts.add("attribute[cdmPartDrawingNo]");
		selectStmts.add("to["+DomainConstants.RELATIONSHIP_PART_SPECIFICATION+"].from.name");
		selectStmts.add("to["+DomainConstants.RELATIONSHIP_PART_SPECIFICATION+"].from.revision");
		

		SelectList selectRel = new SelectList();
		Pattern type2DPattern = new Pattern("CATDrawing");
		type2DPattern.addPattern("cdmNXDrawing");
		type2DPattern.addPattern("cdmAutoCAD");
		
		Pattern typeDerivedOutputPattern = new Pattern("Derived Output");
		
		MapList ml2DList = new MapList();
		
		if("4".equals(strPartObjectType)){
			
			ml2DList = partDomObj.getRelatedObjects(context, 
					DomainConstants.RELATIONSHIP_PART_SPECIFICATION, // relationship
					"cdmOrCADProduct", 			// type
					selectStmts, 				// objects
					selectRel, 					// relationships
					false, 						// to
					true, 						// from
					(short) 1, 					// recurse
					null, 						// where
					null, 						// relationship where
					(short) 0); 				// limit
			
		} else {
			
			ml2DList = partDomObj.getRelatedObjects(context, 
					DomainConstants.RELATIONSHIP_PART_SPECIFICATION, // relationship
					type2DPattern.getPattern(), // type
					selectStmts, 				// objects
					selectRel, 					// relationships
					false, 						// to
					true, 						// from
					(short) 1, 					// recurse
					null, 						// where
					null, 						// relationship where
					(short) 0); 				// limit

		} 
    	System.out.println("ml2DList   :   "+ml2DList);
		
    	
    	StringBuffer strDrawingBuffer = new StringBuffer();
    	int i2DListSize = ml2DList.size();
    	for(int i=0; i<i2DListSize; i++){
    		Map drawingMap = (Map)ml2DList.get(i);
    		
    		String strDrawingId       = (String)drawingMap.get(DomainObject.SELECT_ID);
    		String strDrawingType     = (String)drawingMap.get(DomainObject.SELECT_TYPE);
    		String strDrawingName     = (String)drawingMap.get(DomainObject.SELECT_NAME);
    		String strDrawingRevision = (String)drawingMap.get(DomainObject.SELECT_REVISION);
    		
    		String strDrawingRelPartName       = (String)drawingMap.get("to["+DomainConstants.RELATIONSHIP_PART_SPECIFICATION+"].from.name");
    		//String strDrawingRelPartName     = extractVal(oDrawingRelPartName);
    		
    		String strDrawingRelPartRevision   = (String)drawingMap.get("to["+DomainConstants.RELATIONSHIP_PART_SPECIFICATION+"].from.revision");
    		//String strDrawingRelPartRevision = extractVal(oDrawingRelPartRevision);
    		
    		//strCADRevision = strDrawingRevision;
    		if ("CATDrawing".equals(strDrawingType)) {
    			
    			DomainObject catDrawingObj = new DomainObject(strDrawingId);
    			MapList mlDerivedOutputList = catDrawingObj.getRelatedObjects(context, 
																	"Derived Output", 			// relationship
																	typeDerivedOutputPattern.getPattern(), 			// type
																	selectStmts, 				// objects
																	selectRel, 					// relationships
																	false, 						// to
																	true, 						// from
																	(short) 1, 					// recurse
																	null, 						// where
																	null, 						// relationship where
																	(short) 0); 				// limit
    			
    			
    			System.out.println("mlDerivedOutputList          "+mlDerivedOutputList);
    			
    			for(int k=0; k<mlDerivedOutputList.size(); k++){
    				Map derivedOutputMap = (Map)mlDerivedOutputList.get(k);
    				String strDerivedOutputId = (String)derivedOutputMap.get(DomainObject.SELECT_ID);
    				
    				DomainObject outputDomObj = new DomainObject(strDerivedOutputId);
    				FileList fileList = outputDomObj.getFiles(context);
    				
    				int cntZipCount = 0;
    				for (Iterator iterator = fileList.iterator(); iterator.hasNext();) {
    					matrix.db.File objectFile = (matrix.db.File) iterator.next();
    					
    					String strFileName = objectFile.getName();
    					String extensionsName = strFileName.substring(strFileName.lastIndexOf(".") + 1);
    					extensionsName = extensionsName.toLowerCase();
    					
    					if ( "dwg".equals(extensionsName) ) {
    						extensionsName = "zip";
    					} 
    					
    					if( "cgm".equals(extensionsName) || ("zip".equals(extensionsName) && cntZipCount == 0) ) {
    						
    						strDrawingBuffer.append(strDrawingRelPartName);
        					strDrawingBuffer.append("_");
        					strDrawingBuffer.append(strDrawingRelPartRevision);
        					strDrawingBuffer.append(".");
        					strDrawingBuffer.append(extensionsName);
        					
        					if(iterator.hasNext()){
        						strDrawingBuffer.append(";");	
        	    			}
        					
    					} 
    					
    					if ( "zip".equals(extensionsName) ) {
    						cntZipCount = 1;
    					} 
    					
    				}
    				
    			}
    			
    		} else if ( "cdmNXDrawing".equals(strDrawingType) || "cdmAutoCAD".equals(strDrawingType) ) {

    			DomainObject autocadAndNXDrawingObj = new DomainObject(strDrawingId);
    			FileList fileList = autocadAndNXDrawingObj.getFiles(context);
				
				for (Iterator iterator = fileList.iterator(); iterator.hasNext();) {
					matrix.db.File objectFile = (matrix.db.File) iterator.next();
					
					String strFileName = objectFile.getName();
					String extensionsName = strFileName.substring(strFileName.lastIndexOf(".") + 1);
					extensionsName = extensionsName.toLowerCase();
					
					strDrawingBuffer.append(strDrawingRelPartName);
					strDrawingBuffer.append("_");
					strDrawingBuffer.append(strDrawingRelPartRevision);
					strDrawingBuffer.append(".");
					strDrawingBuffer.append(extensionsName);
					
					if(iterator.hasNext()){
						strDrawingBuffer.append(";");	
	    			}
					
				}
				
    		}else if("cdmOrCADProduct".equals(strDrawingType)){
    			
    			DomainObject autocadAndNXDrawingObj = new DomainObject(strDrawingId);
    			FileList fileList = autocadAndNXDrawingObj.getFiles(context);
				
				for (Iterator iterator = fileList.iterator(); iterator.hasNext();) {
					matrix.db.File objectFile = (matrix.db.File) iterator.next();
					
					String strFileName = objectFile.getName();
					String extensionsName = strFileName.substring(strFileName.lastIndexOf(".") + 1);
					//extensionsName = extensionsName.toLowerCase();
					
					strDrawingBuffer.append(strDrawingRelPartName);
					strDrawingBuffer.append("_");
					strDrawingBuffer.append(strDrawingRelPartRevision);
					strDrawingBuffer.append(".");
					strDrawingBuffer.append(extensionsName);
					
					if(iterator.hasNext()){
						strDrawingBuffer.append(";");	
	    			}
					
				}
				
    		}
    		
    	}
    	System.out.println("############### Drawing Buffer String     :     "+strDrawingBuffer.toString());
    	
    	
    	String str2DFileName = strDrawingBuffer.toString();
    	String str3DFileName = DomainConstants.EMPTY_STRING;
    	String strCADNumber   = DomainConstants.EMPTY_STRING;
    	String strCADRevision = DomainConstants.EMPTY_STRING;
    	String strLinkType    = DomainConstants.EMPTY_STRING;
    	
    	
    	if(! DomainConstants.EMPTY_STRING.equals(str2DFileName)){
    		
    		strCADNumber   = str2DFileName.substring(0, str2DFileName.indexOf("_"));
        	strCADRevision = str2DFileName.substring(str2DFileName.indexOf("_")+1, str2DFileName.indexOf("."));
        	
        	String str3DObjectId = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", strPartObjectId, "from["+DomainConstants.RELATIONSHIP_PART_SPECIFICATION+"|to.type=='CATProduct'||to.type=='CATPart'].to.id"));
        	if(! DomainConstants.EMPTY_STRING.equals(str3DObjectId)){
        		str3DFileName = strDrawingBuffer.toString();
        		str3DFileName  = str3DFileName.substring(0, str3DFileName.indexOf("."));
            	str3DFileName += ".smg";
        	}
        	
        	strLinkType    = "D";
        	if("3".equals(strPartObjectType)){
        		strLinkType    = "B";
        	}
        	
    	}
    	
    	
    	if(! DomainConstants.EMPTY_STRING.equals(strDrawingNo)){
    		
    		System.out.println("Phantom Revise Series Part Drawing No     :::::     " + strDrawingNo);
    		System.out.println("Phantom Revise Series Part No     		  :::::     " + strPartObjectName);
    		
    		
    		StringList slParentPartTypeList = (StringList)partDomObj.getInfoList(context, "to[EBOM].from.type");
    		//String strPhantomPartMqlValue = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "temp query bus $1 $2 $3 where $4 select $5 dump $6", new String[] {cdmConstantsUtil.TYPE_CDMPHANTOMPART, strDrawingNo, "*", "revision==last.revision", DomainConstants.SELECT_ID, ","}));
			
    		
    		SelectList selectList = new SelectList(3);
    		selectList.addId();
    		selectList.addName();
    		selectList.addType();
    		selectList.addRevision();
    		selectList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO);
    		selectList.add("to["+DomainConstants.RELATIONSHIP_AFFECTED_ITEM+"].from.name");
    		selectList.add("from["+DomainConstants.RELATIONSHIP_PART_SPECIFICATION+"|to.type=='CATDrawing'].to.revision");
    		
    		//////////////////// ITEM DRAW TABLE INSERT [PhantomPart ] Start !!!	
	    	if( "3".equals(strPartObjectType) ){
	    			
	    		SelectList busSelect = new SelectList();
		    	busSelect.add(DomainConstants.SELECT_ID);
		    	busSelect.add(DomainConstants.SELECT_NAME);
		    	busSelect.add(DomainConstants.SELECT_REVISION);
		    	busSelect.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO);
		    	busSelect.add("to["+DomainConstants.RELATIONSHIP_AFFECTED_ITEM+"].from.name");
		    		
		    	SelectList relSelect = new SelectList();
		    		
		    		
		    	StringBuffer strBufferWhere = new StringBuffer();
		    	strBufferWhere.append(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO);
		    	strBufferWhere.append(" == '");
		    	strBufferWhere.append(strDrawingNo);
		    	strBufferWhere.append("'");
		    	strBufferWhere.append(" && ");
				strBufferWhere.append("revision");
				strBufferWhere.append(" == ");
				strBufferWhere.append("last.revision");
					
		    	MapList mlPhantomPartDrawingList = DomainObject.findObjects(context, 
		    				cdmConstantsUtil.TYPE_CDMPHANTOMPART,         	// type
		    				DomainConstants.QUERY_WILDCARD,   				// name
		    				DomainConstants.QUERY_WILDCARD,   				// revision
		    				DomainConstants.QUERY_WILDCARD,   				// policy
		    				cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION,     // vault
		    				strBufferWhere.toString(),             			// where
		    				DomainConstants.EMPTY_STRING,     				// query
		    				true,							  				// expand
		    				selectList,                      				// objects
		    				(short)0);                        				// limits
		    		
		    		
		    	String strCATDrawingRevision = "";
		    		
		    	if(mlPhantomPartDrawingList.size() > 0){
		    		Map map = (Map)mlPhantomPartDrawingList.get(0);
		    		strCATDrawingRevision = (String) map.get("from[Part Specification].to.revision");
		    	}
		    		
		    		
		    	Map objMap = partDomObj.getInfo(context, busSelect);
		    		
		    	MapList mlPhantomChildPartList = partDomObj.getRelatedObjects(context, 
							 													DomainConstants.RELATIONSHIP_EBOM, 	// Relationship
							 													cdmConstantsUtil.TYPE_CDMMECHANICALPART, // Type name
							 													busSelect, 							// objects Select
							 													relSelect, 							// Rel selects
							 													false,      						// to Direction
							 													true,     							// from Direction
							 													(short) 1, 							// recusion level
							 													"", 								// current == Release 
							 													"",
							 													0);
		    		
		    	mlPhantomChildPartList.add(0, objMap);
		    		
		    		
		    	String strSaveLinkType = "";
		    		
		    	if(mlPhantomChildPartList.size() > 1){
		    		
			    	for(int i=0; i<mlPhantomChildPartList.size(); i++){
			    			
			    		Map map = (Map)mlPhantomChildPartList.get(i);
			    			
			    		String strManyPartOneDrawingPartId 		 = StringUtils.trimToEmpty((String)map.get(DomainConstants.SELECT_ID));
			    		String strManyPartOneDrawingPartName 	 = StringUtils.trimToEmpty((String)map.get(DomainConstants.SELECT_NAME));
			    		String strManyPartOneDrawingPartRevision = StringUtils.trimToEmpty((String)map.get(DomainConstants.SELECT_REVISION));
			    		String strManyPartOneDrawingName 		 = StringUtils.trimToEmpty((String)map.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO));
			            String strManyPartOneDrawingEOName 		 = StringUtils.trimToEmpty((String)map.get("to["+DomainConstants.RELATIONSHIP_AFFECTED_ITEM+"].from.name"));
			            	
			            	
			            String strPhantomPartLinkType = "D";
				    	if(strDrawingNo.equals(strManyPartOneDrawingPartName)){
				    		strPhantomPartLinkType = "B";
				    		strSaveLinkType = "B";
				    	}
				    		
			    			
			    		Map dbItemDrawDataMap = new HashMap();
			    		dbItemDrawDataMap.put("EONO"		, strPartEOName);
			    			
			    			
			    		if (DomainConstants.EMPTY_STRING.equals(strCADNumber)) {
			    			dbItemDrawDataMap.put("CADNUMBER"	, strManyPartOneDrawingName);
			    		}else{
			    			dbItemDrawDataMap.put("CADNUMBER"	, strCADNumber);
			    		}
			    					    			
			    		if (DomainConstants.EMPTY_STRING.equals(strCADRevision)) {
			    			dbItemDrawDataMap.put("CADREV"		, strCATDrawingRevision);
			    		}else{
			    			dbItemDrawDataMap.put("CADREV"		, strCADRevision);
			    		}
			    			
			    		dbItemDrawDataMap.put("ITEMNUMBER"	, strManyPartOneDrawingPartName);
			    		dbItemDrawDataMap.put("VERSION"		, strManyPartOneDrawingPartRevision);
			    		dbItemDrawDataMap.put("LINKTYPE"	, strPhantomPartLinkType);
			    			
			    		System.out.println("3Exists PhantomPart Item Draw Data Map     ::     " + dbItemDrawDataMap);
			    			
			    		sqlSession.insert("UPDATE_DRAW_ITEM", dbItemDrawDataMap);
			    			
			    	}
			    		
		    	}
		    		
		    		
		    	if(! DomainConstants.EMPTY_STRING.equals(str2DFileName)){
		    			
		    		if("B".equals(strSaveLinkType)){
		    			strLinkType = "B";
			    	}else{
			    		strLinkType = "D";
			    	}
		    			
		    	}
		    		
		    ///////////////////////////////////	ITEM DRAW TABLE INSERT [PhantomPart] End !!!
	    	} else {
	    			
	    		////////////////////////////////////  Series Part DrawItem  Start  !!!	
	    		if(! slParentPartTypeList.contains(cdmConstantsUtil.TYPE_CDMPHANTOMPART) ){
	    			
		    		StringBuffer strBufferWhere = new StringBuffer();
			    	strBufferWhere.append(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO);
			    	strBufferWhere.append(" == '");
			    	strBufferWhere.append(strDrawingNo);
			    	strBufferWhere.append("' && ");
					strBufferWhere.append(DomainConstants.SELECT_REVISION);
					strBufferWhere.append(" == ");
					strBufferWhere.append(strPartObjectRevision);
					strBufferWhere.append(" && ");
					strBufferWhere.append("revision");
					strBufferWhere.append(" == ");
					strBufferWhere.append("last.revision");
			    		
						
			    	MapList mlManyPartOneDrawingList = DomainObject.findObjects(context, 
			    					cdmConstantsUtil.TYPE_CDMPART,         			// type
			    					DomainConstants.QUERY_WILDCARD,   				// name
			    					DomainConstants.QUERY_WILDCARD,   				// revision
			    					DomainConstants.QUERY_WILDCARD,   				// policy
			    					cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION,     // vault
			    					strBufferWhere.toString(),             			// where
			    					DomainConstants.EMPTY_STRING,     				// query
			    					true,							  				// expand
			    					selectList,                      				// objects
			    					(short)0);                        				// limits
			    		
			    		
			    	String strSaveLinkType = "";
			    		
			    		
			    	if(mlManyPartOneDrawingList.size() > 1){
			    		
				    	for(int i=0; i<mlManyPartOneDrawingList.size(); i++){
				            		
				            Map map = (Map)mlManyPartOneDrawingList.get(i);
				            String strManyPartOneDrawingPartId 		 = StringUtils.trimToEmpty((String)map.get(DomainConstants.SELECT_ID));
				            String strManyPartOneDrawingPartName 	 = StringUtils.trimToEmpty((String)map.get(DomainConstants.SELECT_NAME));
				            String strManyPartOneDrawingPartRevision = StringUtils.trimToEmpty((String)map.get(DomainConstants.SELECT_REVISION));
				            String strManyPartOneDrawingName 		 = StringUtils.trimToEmpty((String)map.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO));
				            String strManyPartOneDrawingEOName 		 = StringUtils.trimToEmpty((String)map.get("to["+DomainConstants.RELATIONSHIP_AFFECTED_ITEM+"].from.name"));
				        			
				            String strNotPhantomPartLinkType = "D";
					    	if(strDrawingNo.equals(strManyPartOneDrawingPartName)){
					    		strNotPhantomPartLinkType = "B";
					    		strSaveLinkType = "B";
					    	}
					    		
				            		
				            Map dbItemManyPartOneDrawingMap = new HashMap();
				            dbItemManyPartOneDrawingMap.put("EONO" ,    strManyPartOneDrawingEOName);
				            	
				            dbItemManyPartOneDrawingMap.put("CADNUMBER"	 ,    strDrawingNo);
				            dbItemManyPartOneDrawingMap.put("CADREV"     ,    strManyPartOneDrawingPartRevision);
				            		
				            dbItemManyPartOneDrawingMap.put("ITEMNUMBER" ,    strManyPartOneDrawingPartName);
				            dbItemManyPartOneDrawingMap.put("VERSION"	 ,    strManyPartOneDrawingPartRevision);
				            dbItemManyPartOneDrawingMap.put("LINKTYPE"	 ,    strNotPhantomPartLinkType);
				        			
				        			
				        	System.out.println("Not Exists PhantomPart Item Draw Data Map     ::     " + dbItemManyPartOneDrawingMap);
				        	sqlSession.insert("UPDATE_DRAW_ITEM",    dbItemManyPartOneDrawingMap);
				            		
				        }
				    		
			    	}
			    		
			    		
			    	if(! DomainConstants.EMPTY_STRING.equals(str2DFileName)){
			    			
			    		if("B".equals(strSaveLinkType)){
				    			
			    			strLinkType = "B";
			    				
				    	}else{
				    			
				    		strLinkType = "D";
				    			
				    	}
			    			
			    	}
	    		}
	    		////////////////////////////////////Series Part DrawItem  End  !!!	
	    	}
	    	
    	}

    	
    	/////////////////////////////////////////  MandoPartType  PLM Table Replace Start !!!
    	if( "singleSupply".equals(strPartType) ){
    		strPartType = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(), "emxEngineeringCentral.Label.PartType.singleSupply");
    	}else if( "completeProduct".equals(strPartType) ){
    		strPartType = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(), "emxEngineeringCentral.Label.PartType.completeProduct");
    	}else if( "halfFinishedProduct".equals(strPartType) ){
    		strPartType = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(), "emxEngineeringCentral.Label.PartType.halfFinishedProduct");
    	}else if( "rawMaterial".equals(strPartType) ){
    		strPartType = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(), "emxEngineeringCentral.Label.PartType.rawMaterial");
    	}
    	/////////////////////////////////////////  MandoPartType  PLM Table Replace End !!!
    	
    	
    	////////////////////////////////////////  Option PLM Table Replace Start !!!  
    	if(strPartOption1.indexOf("None") > -1){
        	strPartOption1 = strPartOption1.replace("None", "-");
    	}
        
        if(strPartOption2.indexOf("None") > -1){
        	strPartOption2 = strPartOption2.replace("None", "-");
    	}
        
        if(strPartOption3.indexOf("None") > -1){
        	strPartOption3 = strPartOption3.replace("None", "-");
    	}
        
        if(strPartOption4.indexOf("None") > -1){
        	strPartOption4 = strPartOption4.replace("None", "-");
    	}
        
        if(strPartOption5.indexOf("None") > -1){
        	strPartOption5 = strPartOption5.replace("None", "-");
    	}
        
        if(strPartOption6.indexOf("None") > -1){
        	strPartOption6 = strPartOption6.replace("None", "-");
    	}
        
        if(strPartOption7.indexOf("None") > -1){
        	strPartOption7 = strPartOption7.replace("None", "-");
    	}
        
        if(strPartOption8.indexOf("None") > -1){
        	strPartOption8 = strPartOption8.replace("None", "-");
    	}
        
        if(strPartOption9.indexOf("None") > -1){
        	strPartOption9 = strPartOption9.replace("None", "-");
    	}
        
        if(strPartOption10.indexOf("None") > -1){
        	strPartOption10 = strPartOption10.replace("None", "-");
    	}
        
        if(strPartOption11.indexOf("None") > -1){
        	strPartOption11 = strPartOption11.replace("None", "-");
    	}
        
        if(strPartOption12.indexOf("None") > -1){
        	strPartOption12 = strPartOption12.replace("None", "-");
    	}
        
        if(strPartOption13.indexOf("None") > -1){
        	strPartOption13 = strPartOption13.replace("None", "-");
    	}
        
        if(strPartOption14.indexOf("None") > -1){
        	strPartOption14 = strPartOption14.replace("None", "-");
    	}
        
        if(strPartOption15.indexOf("None") > -1){
        	strPartOption15 = strPartOption15.replace("None", "-");
    	}
        ////////////////////////////////////////Option PLM Table Replace End !!!
        
    	
    	///////////////////////////////////  ElectronicPart LinkType  Only B  Start !!!
        if( "4".equals(strPartObjectType) ){
    		strLinkType = "B";
    	}
        ///////////////////////////////////  ElectronicPart LinkType  Only B  End !!!
        
        
    	
        ///////////////////////////////////  User  Only UpperCase  Start  !!!
    	strPartOwner = strPartOwner.toUpperCase();
    	///////////////////////////////////  User  Only UpperCase  End  !!!
    	
    	if( DomainConstants.EMPTY_STRING.equals(strPartEstimatedWeight) ){
    		strPartEstimateWeightUnit = "";
    	}
    	
    	
        //dbPartDataMap.put("EONO",  strEOName);
        dbPartDataMap.put("ITEMNUMBER", 		strPartObjectName);
        dbPartDataMap.put("ITEMNAME",   		strPartName);
        dbPartDataMap.put("VERSION",  			strPartObjectRevision);    
        dbPartDataMap.put("PREVERSION", 		strPreviousRevision);
        //dbPartDataMap.put("ITEMTYPE",  			strPartItemType);
        dbPartDataMap.put("MANDOPARTTYPE",  	strPartType);
        dbPartDataMap.put("GLOBALTYPE",  		strPartGlobal);
        dbPartDataMap.put("PARTUNIT",  			strPartUnitOfMeasure);
        dbPartDataMap.put("PROJECTCODE",  		"");
        //dbPartDataMap.put("APPROVETYPE",  		strPartApprovalType);
        dbPartDataMap.put("CFGCREATOR",  		strPartOwner);
        dbPartDataMap.put("CFGCREATEDATE",  	dCreateOriginated);
        dbPartDataMap.put("CARTYPE",  			strVehicleBuffer.toString());
        dbPartDataMap.put("WEIGHT",  			strPartEstimatedWeight);
        dbPartDataMap.put("WEIGHTUNIT",  		strPartEstimateWeightUnit);
        dbPartDataMap.put("ISCASTINGPART",  	strPartIsCasting);
        dbPartDataMap.put("STANDARD",  			strPartSize);
        dbPartDataMap.put("QUALITY",  			"");
        dbPartDataMap.put("SYSTEM",  			strSystem);
        dbPartDataMap.put("PRODUCT",  			strPartProductType);
        dbPartDataMap.put("PARENTPARTYN", 		strParentPartYN);
        dbPartDataMap.put("PARTTYPE",  			strPartObjectType);
        dbPartDataMap.put("THICKNESS",  		"");
        dbPartDataMap.put("MATERIAL",  			strPartMaterial);
        dbPartDataMap.put("MATERIALCOSINE",  	strPartMaterialCoSignYN);
        dbPartDataMap.put("FINISH",  			strPartSurface);
        dbPartDataMap.put("FINISHCOSINE",  		strPartSurfaceTreatmentCoSignYN);
        dbPartDataMap.put("STDBOMYN",  			strPartStandardBOM);
        dbPartDataMap.put("CUSTITEMNUMBER",  	strPartOEMItemNumber);
        dbPartDataMap.put("OPTION1",  			strPartOption1);
        dbPartDataMap.put("OPTION2",  			strPartOption2);
        dbPartDataMap.put("OPTION3",  			strPartOption3);
        dbPartDataMap.put("OPTION4",  			strPartOption4);
        dbPartDataMap.put("OPTION5",  			strPartOption5);
        dbPartDataMap.put("OPTION6",  			strPartOption6);
        dbPartDataMap.put("OPTION7",  			strPartOption7);
        dbPartDataMap.put("OPTION8",  			strPartOption8);
        dbPartDataMap.put("OPTION9",  			strPartOption9);
        dbPartDataMap.put("OPTION10",  			strPartOption10);
        dbPartDataMap.put("OPTION11",  			strPartOption11);
        dbPartDataMap.put("OPTION12",  			strPartOption12);
        dbPartDataMap.put("OPTION13",  			strPartOption13);
        dbPartDataMap.put("OPTION14",  			strPartOption14);
        dbPartDataMap.put("OPTION15",  			strPartOption15);
        dbPartDataMap.put("CN_FILENAME_2D",  	str2DFileName);
        dbPartDataMap.put("CADNUMBER",  		strCADNumber);
        dbPartDataMap.put("CADREV",  			strCADRevision);
        dbPartDataMap.put("LINKTYPE",  			strLinkType);
        dbPartDataMap.put("CN_FILENAME_3D",  	str3DFileName);
        
        return dbPartDataMap;
	}
	
	
	
//	public static void main(String[] args) {
//		
//		try {
//			Context context = new Context("");
//			context.setUser("admin_platform");//p13941
//			context.setPassword("");
//			context.connect();
//			
//
//	    	SelectList selectStmts = new SelectList();
//			selectStmts.add(DomainObject.SELECT_ID);
//			selectStmts.add(DomainObject.SELECT_TYPE);
//			selectStmts.add(DomainObject.SELECT_NAME);
//			selectStmts.add(DomainObject.SELECT_REVISION);
//			selectStmts.add("attribute[cdmPartDrawingNo]");
//
//			SelectList selectRel = new SelectList();
//			Pattern type2DPattern = new Pattern("CATDrawing");
//			type2DPattern.addPattern("cdmNXDrawing");
//			type2DPattern.addPattern("cdmAutoCAD");
//			
//			Pattern typeDerivedOutputPattern = new Pattern("Derived Output");
//			
//			DomainObject partDomObj = new DomainObject("47604.45839.41744.12433");
//			
//			MapList ml2DList = partDomObj.getRelatedObjects(context, 
//															DomainConstants.RELATIONSHIP_PART_SPECIFICATION, // relationship
//															type2DPattern.getPattern(), // type
//															selectStmts, 				// objects
//															selectRel, 					// relationships
//															false, 						// to
//															true, 						// from
//															(short) 1, 					// recurse
//															null, 						// where
//															null, 						// relationship where
//															(short) 0); 				// limit
//	    	
//	    	System.out.println("ml2DList   :   "+ml2DList);
//			
//	    	StringBuffer strDrawingBuffer = new StringBuffer();
//	    	int i2DListSize = ml2DList.size();
//	    	for(int i=0; i<i2DListSize; i++){
//	    		Map drawingMap = (Map)ml2DList.get(i);
//	    		String strId       = (String)drawingMap.get(DomainObject.SELECT_ID);
//	    		String strType     = (String)drawingMap.get(DomainObject.SELECT_TYPE);
//	    		String strName     = (String)drawingMap.get(DomainObject.SELECT_NAME);
//	    		String strRevision = (String)drawingMap.get(DomainObject.SELECT_REVISION);
//	    		
//	    		if ("CATDrawing".equals(strType)) {
//	    			
//	    			
//	    			DomainObject catDrawingObj = new DomainObject(strId);
//	    			MapList mlDerivedOutputList = catDrawingObj.getRelatedObjects(context, 
//																		DomainConstants.RELATIONSHIP_PART_SPECIFICATION, // relationship
//																		typeDerivedOutputPattern.getPattern(), 			// type
//																		selectStmts, 				// objects
//																		selectRel, 					// relationships
//																		false, 						// to
//																		true, 						// from
//																		(short) 1, 					// recurse
//																		null, 						// where
//																		null, 						// relationship where
//																		(short) 0); 				// limit
//	    			
//	    			
//	    			for(int k=0; k<mlDerivedOutputList.size(); k++){
//	    				Map derivedOutputMap = (Map)mlDerivedOutputList.get(k);
//	    				String strDerivedOutputId = (String)derivedOutputMap.get(DomainObject.SELECT_ID);
//	    				
//	    				DomainObject outputDomObj = new DomainObject(strDerivedOutputId);
//	    				FileList fileList = outputDomObj.getFiles(context);
//	    				
//	    				for (Iterator iterator = fileList.iterator(); iterator.hasNext();) {
//	    					matrix.db.File objectFile = (matrix.db.File) iterator.next();
//	    					
//	    					String strFileName = objectFile.getName();
//	    					strFileName.lastIndexOf(".");
//	    					String extensionsName = strFileName.substring(strFileName.lastIndexOf(".") + 1);
//	    					extensionsName = extensionsName.toLowerCase();
//	    					
//	    					if ( "dwg".equals(extensionsName) ) {
//	    						extensionsName = "zip";
//	    					} 
//	    					
//	    					strDrawingBuffer.append(strName);
//	    					strDrawingBuffer.append("_");
//	    					strDrawingBuffer.append(strRevision);
//	    					strDrawingBuffer.append(".");
//	    					strDrawingBuffer.append(extensionsName);
//	    					
//	    					
//	    					if(iterator.hasNext()){
//	    						strDrawingBuffer.append(";");	
//	    	    			}
//	    					
//	    				}
//	    				
//	    			}
//	    			
//	    		} else if ( "cdmNXDrawing".equals(strType) || "cdmAutoCAD".equals(strType) ) {
//
//	    			DomainObject autocadAndNXDrawingObj = new DomainObject(strId);
//	    			FileList fileList = autocadAndNXDrawingObj.getFiles(context);
//    				
//    				for (Iterator iterator = fileList.iterator(); iterator.hasNext();) {
//    					matrix.db.File objectFile = (matrix.db.File) iterator.next();
//    					
//    					String strFileName = objectFile.getName();
//    					strFileName.lastIndexOf(".");
//    					String extensionsName = strFileName.substring(strFileName.lastIndexOf(".") + 1);
//    					extensionsName = extensionsName.toLowerCase();
//    					
//    					strDrawingBuffer.append(strName);
//    					strDrawingBuffer.append("_");
//    					strDrawingBuffer.append(strRevision);
//    					strDrawingBuffer.append(".");
//    					strDrawingBuffer.append(extensionsName);
//    					
//    					if(iterator.hasNext()){
//    						strDrawingBuffer.append(";");	
//    	    			}
//    					
//    				}
//    				
//	    		}
//	    		
//	    		System.out.println("strDrawingBuffer        "+strDrawingBuffer.toString());
//	    	}
//	    	
//			
//		}catch(Exception e){
//			e.printStackTrace();
//		}
//	}
		
		
		
		
	
	/**
	 * 
	 * @param context
	 * @param args
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void sendPartBOMDataToPLM(Context context, SqlSession sqlSession, String[] args) throws Exception {
		
		
	
	
		try {
				
			////////////////////////////////////////////// PLM Interface  Start !!!!!!!!!!!///////////////////////////
			
			String strECObjectId = args[0];
			
			DomainObject domObj = new DomainObject(strECObjectId);
			String strEOName = domObj.getInfo(context, DomainConstants.SELECT_NAME);
			
			
			////////////////////////////////////////////////////// Request Repeat  YN Start !!!
			Map viewEOMap = new HashMap();
			viewEOMap.put("ECONO", strEOName);
			String strViewCDM_YN = StringUtils.trimToEmpty((String)sqlSession.selectOne("VW_EOMASTER_CDMYN", viewEOMap));
	        System.out.println("VIEW   CDM   YN   :   "+strViewCDM_YN);
	        ////////////////////////////////////////////////////// Request Repeat  YN End !!!
	        
	        
	        ////////////////////////////////////////////////////// EO Related Affected Item Start !!!
	        SelectList selectStmts = new SelectList();
			selectStmts.add(DomainObject.SELECT_ID);
			selectStmts.add(DomainObject.SELECT_TYPE);
			selectStmts.add(DomainObject.SELECT_NAME);
			selectStmts.add(DomainObject.SELECT_REVISION);

			DomainObject ecDomObj = new DomainObject(strECObjectId);
			MapList mlPartList = ecDomObj.getRelatedObjects(context, DomainConstants.RELATIONSHIP_AFFECTED_ITEM, // relationship
																						cdmConstantsUtil.TYPE_CDMPART, // type
																						selectStmts, // objects
																						null, // relationships
																						false, // to
																						true, // from
																						(short) 1, // recurse
																						null, // where
																						null, // relationship where
																						(short) 0); // limit
			////////////////////////////////////////////////////// EO Related Affected Item End !!!
			
			
			
			///////////////////////////////////////////////////// View Table Info SnapShot Start !!!
			try {
				StringBuffer sbPartInfo = new StringBuffer();
				StringBuffer sbBomInfo = new StringBuffer();
				for (Iterator iterator = mlPartList.iterator(); iterator.hasNext();) {
					Map objMap = (Map) iterator.next();
					
					String strPartNo = (String) objMap.get(DomainObject.SELECT_NAME);
					String strPartRev = (String) objMap.get(DomainObject.SELECT_REVISION);
							
					
					Map paramMap = new HashMap();
					paramMap.put("PARTNUMBER", strPartNo);
					paramMap.put("PARENTREV", strPartRev);
					List<Map<String, String>> partList = sqlSession.selectList("get_part_info_with_partno", paramMap);
					
					
					sbPartInfo.append("PARTNUMBER");
					sbPartInfo.append("|");
					sbPartInfo.append("REV");
					sbPartInfo.append("|");
					sbPartInfo.append("ECONO");
					sbPartInfo.append("\r\n");
					
					for (int i = 0; i < partList.size(); i++) {

						Map pMap = (Map) partList.get(i);
						String strPartNumber = (String) pMap.get("PARTNUMBER");
						String strPartRevision = (String) pMap.get("REV");
						String strPartECO = (String) pMap.get("ECONO");
						
						
						sbPartInfo.append(strPartNumber);
						sbPartInfo.append("|");
						sbPartInfo.append(strPartRevision);
						sbPartInfo.append("|");
						sbPartInfo.append(strPartECO);
						sbPartInfo.append("\r\n");
					}
					
					
					
					Map paramMap1 = new HashMap();
					paramMap1.put("PARENTNUMBER", strPartNo);
					paramMap1.put("PARENTREV", strPartRev);
					List<Map<String, String>> bomList = sqlSession.selectList("get_bom_info_with_parent", paramMap1);
					
					sbBomInfo.append("PARENTNUMBER");
					sbBomInfo.append("|");
					sbBomInfo.append("PARENTREV");
					sbBomInfo.append("|");
					sbBomInfo.append("CHILDNUMBER");
					sbBomInfo.append("|");
					sbBomInfo.append("CHILDREV");
					sbBomInfo.append("|");
					sbBomInfo.append("QUANTITY");
					sbBomInfo.append("\r\n");
					
					for (int i = 0; i < bomList.size(); i++) {

						Map pMap = (Map) bomList.get(i);
						
						String strParentNo = (String) pMap.get("PARENTNUMBER");
						String strParentRev = (String) pMap.get("PARENTREV");
						String strChildNo = (String) pMap.get("CHILDNUMBER");
						String strChildRev = (String) pMap.get("CHILDREV");
						String strQty = String.valueOf(pMap.get("QUANTITY"));

						sbBomInfo.append(strParentNo);
						sbBomInfo.append("|");
						sbBomInfo.append(strParentRev);
						sbBomInfo.append("|");
						sbBomInfo.append(strChildNo);
						sbBomInfo.append("|");
						sbBomInfo.append(strChildRev);
						sbBomInfo.append("|");
						sbBomInfo.append(strQty);
						sbBomInfo.append("\r\n");
					}
				}// end for loop - affected Item00
				
				
				
				StringList busSelect = new StringList();
				busSelect.add(DomainObject.SELECT_ID);
				busSelect.add(DomainObject.SELECT_ORIGINATED);
				
				
				MapList mlJobList = ecDomObj.getRelatedObjects(context, 
																"cdmJobECO", // relationship
																"Job", // type
																busSelect, 				// objects
																null, 					// relationships
																true, 						// to
																false, 						// from
																(short) 1, 					// recurse
																null, 						// where
																null, 						// relationship where
																(short) 0); 				// limit

				mlJobList.sort(DomainObject.SELECT_ORIGINATED, "descending", "date");

				if (mlJobList.size() > 0) {

					Map jobMap = (Map) mlJobList.get(0);

					String strJobId = (String) jobMap.get(DomainObject.SELECT_ID);
					DomainObject doJob = new DomainObject(strJobId);
					
					
					String strPartInfo = sbPartInfo.toString();
					String strBomInfo = sbBomInfo.toString();
					
					
					sbPartInfo.append("=========================================================\n").append(sbBomInfo);
					
					
					doJob.setAttributeValue(context, "cdmSnapshotOfPLMData", sbPartInfo.toString());
				}
				
				
			} catch (Exception e) {
			}
			
			///////////////////////////////////////////////////// View Table Info SnapShot End !!!
	        
			
			
			
			///////////////////////////////////////////////////// Request Repeat "Y"   TB_PART & TB_NEWBOM & TB_CHANGE_PART  Start !!!
			
	        StringList slRequestRepeatPartList 		 = new StringList();
	        StringList slRequestRepeatCdmPartList 	 = new StringList();
	        Map requestRepeatNewBomMap     = new HashMap();
	        Map requestRepeatChangePartMap = new HashMap();
	        StringBuffer strPartDeleteBuffer = new StringBuffer();
	        
	        StringList slRequestRepeatCdmPartChildList 	 = new StringList();
	        
			if( "Y".equals(strViewCDM_YN) ){
			
				Map viewEOPartMap = new HashMap();
				viewEOPartMap.put("EONO", strEOName);
				viewEOPartMap.put("GUBUN", "D");
				List<Map<String, String>> requestRepeatPartList = sqlSession.selectList("VW_REQUEST_REPEAT_PART", viewEOPartMap);
		        System.out.println("REQUEST   REPEAT   PART   LIST     :     " + requestRepeatPartList);
		        
		        
		        Map viewEONewBomMap = new HashMap();
		        viewEONewBomMap.put("ECONO", strEOName);
		        viewEONewBomMap.put("CHANGE_TYPE", "D");
				List<Map<String, String>> requestRepeatNewBomList = sqlSession.selectList("VW_REQUEST_REPEAT_NEW_BOM", viewEONewBomMap);
		        System.out.println("REQUEST   REPEAT   NEW BOM   LIST     :     " + requestRepeatNewBomList);
		        
		        
		        Map viewEOChangePartMap = new HashMap();
		        viewEOChangePartMap.put("EO_NUMBER", strEOName);
				List<Map<String, String>> requestRepeatChangePartList = sqlSession.selectList("VW_REQUEST_REPEAT_CHANGE_PART", viewEOChangePartMap);
		        System.out.println("REQUEST   REPEAT   CHANGE PART   LIST     :     " + requestRepeatChangePartList);
		        	
		        
		        /////////////////////////////////////////////////// RequestRepeatCdmPartList...  Affected_Item Start !!!
		        for(int i=0; i<mlPartList.size(); i++) {
					
					Map partMap = (Map)mlPartList.get(i);
			        String strPartObjectName     = (String)partMap.get(DomainObject.SELECT_NAME);
			        String strPartObjectRevision = (String)partMap.get(DomainObject.SELECT_REVISION);
			        
			        StringBuffer strBuffer = new StringBuffer();
			        strBuffer.append(strPartObjectName);
			        strBuffer.append("|");
			        strBuffer.append(strPartObjectRevision);
			        
			        slRequestRepeatCdmPartList.add(strBuffer.toString());
			        
			        
			        String strPartObjectId     = (String)partMap.get(DomainObject.SELECT_ID);
			        DomainObject dObj = new DomainObject(strPartObjectId);
			        StringList slChildList = dObj.getInfoList(context, "from[EBOM].to.id");
			        
			        for(int n=0; n<slChildList.size(); n++){
			        	
			        	StringBuffer strCdmPartChildBuffer = new StringBuffer();
			        	String strChildId = (String)slChildList.get(n);
			        	DomainObject dChildObj 	= new DomainObject(strChildId);
			        	String strChildName 	= dChildObj.getInfo(context, "name");
			        	String strChildRevision = dChildObj.getInfo(context, "revision");
			        	strCdmPartChildBuffer.append(strPartObjectName);
			        	strCdmPartChildBuffer.append("|");
			        	strCdmPartChildBuffer.append(strChildRevision);
			        	strCdmPartChildBuffer.append("|");
			        	strCdmPartChildBuffer.append(strChildName);
			        	strCdmPartChildBuffer.append("|");
			        	strCdmPartChildBuffer.append(strChildRevision);
			        	
			        	slRequestRepeatCdmPartChildList.add(strCdmPartChildBuffer.toString());
			        	
			        }
			        
				}
		        /////////////////////////////////////////////////// RequestRepeatCdmPartList...  Affected_Item End !!!

		        
		        /////////////////////////////////////////////////// RequestRepeatCdmPartList...  Affected_Item Start !!!
		        if(requestRepeatPartList.size() > 0){
		        	
		        	for (int k=0; k<requestRepeatPartList.size(); k++) {
				    	
				    	Map requestRepeatPartMap = (Map) requestRepeatPartList.get(k);
						String strPartNumber   = (String) requestRepeatPartMap.get("ITEMNUMBER");
						String strPartRevision = (String) requestRepeatPartMap.get("VERSION");
						
						StringBuffer strBuffer = new StringBuffer();
						strBuffer.append(strPartNumber);
						strBuffer.append("|");
						strBuffer.append(strPartRevision);
						
						    	
						slRequestRepeatPartList.add(strBuffer.toString());
						
						
						System.out.println("###   RequestRepeatCdmPartList     "+slRequestRepeatCdmPartList);
						System.out.println("###   Buffer.toString()            "+strBuffer.toString());
						
						/////////////////////////////////////////////////// TB_PART_TABLE DELETE Start !!!
						if(! slRequestRepeatCdmPartList.contains(strBuffer.toString())){
							
							Map vwRequestRepeatPartMap = new HashMap();
						    vwRequestRepeatPartMap.put("EONO", 			strEOName);
						    vwRequestRepeatPartMap.put("ITEMNUMBER",  	strPartNumber);
						    vwRequestRepeatPartMap.put("VERSION", 		strPartRevision);
						    vwRequestRepeatPartMap.put("GUBUN", 		"D");
						    
						    sqlSession.update("UPDATE_REQUEST_REPEAT_PART", vwRequestRepeatPartMap);
						    
						    
						    String strDrawPartObjectId = StringUtils.trimToEmpty((String)sqlSession.selectOne("SELECT_DRAW_TABLE_PART", vwRequestRepeatPartMap));
					        System.out.println("!!!!!Draw Part ObjectId   :   " + strDrawPartObjectId);
					        
					        if(! "".equals(strDrawPartObjectId) ){
					        	sqlSession.delete("DELETE_DRAW_TABLE_PART", vwRequestRepeatPartMap);
					        }
							
						}
						/////////////////////////////////////////////////// TB_PART_TABLE DELETE End !!!
						
				    }
		        	
		        }
		        /////////////////////////////////////////////////// RequestRepeatCdmPartList...  Affected_Item End !!!
		        
		        
		        
		        //////////////////////////////////////// RequestRepeat  NewBom  List    Start !!!  
		        if(requestRepeatNewBomList.size() > 0){
		        	
		        	for (int k=0; k<requestRepeatNewBomList.size(); k++) {
				    	
		        		Map requestRepeatNewBom = (Map) requestRepeatNewBomList.get(k);
		        		String strParentPartNumber   	= (String) requestRepeatNewBom.get("PARENT_PART_NO");
						String strParentPartRevision	= (String) requestRepeatNewBom.get("PARENT_PART_REV");
						String strChildPartNumber   	= (String) requestRepeatNewBom.get("CHILD_PART_NO");
						String strChildPartRevision 	= (String) requestRepeatNewBom.get("CHILD_PART_REV");
						String strQuantity 				= (String) requestRepeatNewBom.get("QUANTITY");
						
						StringBuffer strNewBomBuffer = new StringBuffer();
						strNewBomBuffer.append(strParentPartNumber);
						strNewBomBuffer.append("|");
						strNewBomBuffer.append(strParentPartRevision);
						strNewBomBuffer.append("|");
						strNewBomBuffer.append(strChildPartNumber);
						strNewBomBuffer.append("|");
						strNewBomBuffer.append(strChildPartRevision);
						
						requestRepeatNewBomMap.put(strNewBomBuffer.toString(), strQuantity);
						
				    }
		        }
		        //////////////////////////////////////// RequestRepeat  NewBom  List    End !!!
		        
		        
		        
		        //////////////////////////////////////// RequestRepeat  ChangePart  List    Start !!!
		        if(requestRepeatChangePartList.size() > 0){
		        	
		        	for (int k=0; k<requestRepeatChangePartList.size(); k++) {
				    	
		        		Map requestRepeatChangePart = (Map) requestRepeatChangePartList.get(k);
		        		String strParentPartNumber   	= (String) requestRepeatChangePart.get("AFTER_PARENT_PART_ID");
						String strParentPartRevision	= (String) requestRepeatChangePart.get("AFTER_PARENT_PART_REV");
						String strChildPartNumber   	= (String) requestRepeatChangePart.get("AFTER_CHILD_PART_ID");
						String strChildPartRevision 	= (String) requestRepeatChangePart.get("AFTER_CHILD_PART_REV");
						String strQuantity 				= (String) requestRepeatChangePart.get("QUANTITY");
						
						StringBuffer strChangePartBuffer = new StringBuffer();
						strChangePartBuffer.append(strParentPartNumber);
						strChangePartBuffer.append("|");
						strChangePartBuffer.append(strParentPartRevision);
						strChangePartBuffer.append("|");
						strChangePartBuffer.append(strChildPartNumber);
						strChangePartBuffer.append("|");
						strChangePartBuffer.append(strChildPartRevision);
						
						//requestRepeatChangePartMap.put(strChangePartBuffer.toString(), strQuantity);
						
						System.out.println("!!!!!RequestRepeatCdmPartChildList      =     " + slRequestRepeatCdmPartChildList);
						System.out.println("!!!!!strChangePartBuffer.toString()     =     " + strChangePartBuffer.toString());
						if(! slRequestRepeatCdmPartChildList.contains(strChangePartBuffer.toString()) ){
							
							
							String strClassify = "D";
							
							Map requestRepeatBomMap = new HashMap();
							
							requestRepeatBomMap.put("PARENTNUMBER", strParentPartNumber);
							requestRepeatBomMap.put("PARENTREV", strParentPartRevision);
							requestRepeatBomMap.put("CHILDNUMBER", strChildPartNumber);
							requestRepeatBomMap.put("CHILDREV", strChildPartRevision);
							
							List<Map<String, String>> requestRepeatBomExists = (List)sqlSession.selectList("SELECT_REQUEST_REPEAT_BOM", requestRepeatBomMap);
							System.out.println("requestRepeatBomExists     " + requestRepeatBomExists);
							
							if(requestRepeatBomExists.size() > 0){
								strClassify = "N";
							}
							
							
							Map changePartDeleteUpdateMap = new HashMap();
							
							//changePartDeleteUpdateMap.put("AFTER_CHANGE_TYPE", "D");
							changePartDeleteUpdateMap.put("CLASSIFY", strClassify);
							changePartDeleteUpdateMap.put("AFTER_PARENT_PART_ID", strParentPartNumber);
							changePartDeleteUpdateMap.put("AFTER_PARENT_PART_REV", strParentPartRevision);
							changePartDeleteUpdateMap.put("AFTER_CHILD_PART_ID", strChildPartNumber);
							changePartDeleteUpdateMap.put("AFTER_CHILD_PART_REV", strChildPartRevision);
							changePartDeleteUpdateMap.put("EO_NUMBER", strEOName);
							
							System.out.println("!!!!!changePartDeleteUpdateMap     =     " + changePartDeleteUpdateMap);
							sqlSession.update("UPDATE_CHANGE_PART_1", changePartDeleteUpdateMap);
							
							//select * from VW_BOM_CDM_EXP_IF where PARENTNUMBER = 'BC111D1900' AND PARENTREV = '02' AND CHILDNUMBER = 'BC111D2000' AND CHILDREV = '01'  
							
						}
						
				    }
		        	
		        }
		        //////////////////////////////////////// RequestRepeat  ChangePart  List    End !!!
		        
		        
		        
		        //////////////////////////////////////// RequestRepeat  Draw_Table  Delete  Start !!!
		        Map requestRepeatDrawDeleteMap = new HashMap();
		        requestRepeatDrawDeleteMap.put("EONO", strEOName);
		        sqlSession.delete("DELETE_DRAW_TABLE_EO", requestRepeatDrawDeleteMap);
		        //////////////////////////////////////// RequestRepeat  Draw_Table  Delete  End !!!  
		        
			}
			///////////////////////////////////////////////////// Request Repeat "Y"   TB_PART & TB_NEWBOM & TB_CHANGE_PART  End !!!
	        
			
			
		    DateFormat sdFormat = new SimpleDateFormat("yyyy-MM-dd");
		    Date date = new Date(); 
		    String sDate = sdFormat.format(date);
		    String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
		    
		    
		    StringList busSelect = new StringList();
		    busSelect.add(DomainObject.SELECT_ID);
		    busSelect.add(DomainObject.SELECT_NAME);
		    busSelect.add(DomainObject.SELECT_REVISION);
		    
		    StringList relSelect = new StringList();
		    relSelect.add(DomainConstants.SELECT_ATTRIBUTE_QUANTITY);
		    
		    
		    /////////////////////  [Start] Affected_Item  Interface  [Start] ////////////////////   
		    for(int i=0; i<mlPartList.size(); i++) {
		    	
		        Map partMap = (Map)mlPartList.get(i);
		        String strPartObjectId       = (String)partMap.get(DomainObject.SELECT_ID);
		        String strPartObjectType     = (String)partMap.get(DomainObject.SELECT_TYPE);
		        String strPartObjectName     = (String)partMap.get(DomainObject.SELECT_NAME);
		        String strPartObjectRevision = (String)partMap.get(DomainObject.SELECT_REVISION);
		        System.out.println("PART   OBJECTID   :   "+strPartObjectId);
		        
		        
		        ////////////////////////////////////////////  PartName & PartRevision  EO Exists YN  Start !!!
		        Map viewPartMap = new HashMap();
		        viewPartMap.put("PARTNUMBER", strPartObjectName);
		        viewPartMap.put("REV", strPartObjectRevision);
		        String strViewECONumber = StringUtils.trimToEmpty((String)sqlSession.selectOne("VW_PART_ECONO", viewPartMap));
		        System.out.println("VIEW   ECO   NUMBER   :   "+strViewECONumber);
		        ////////////////////////////////////////////  PartName & PartRevision  EO Exists YN  End !!!
		        

		        ////////////////////////////////// [ (EO) Nothing & (RequestRepeat) NO ]
		        if(DomainConstants.EMPTY_STRING.equals(strViewECONumber) && ! "Y".equals(strViewCDM_YN) ){
		        	
		        	
		        	////////////////////////////////// VW_TABLE  PartNumber  Exists YN  Start !!!
		        	Map viewRevisePartMap = new HashMap();
		        	viewRevisePartMap.put("PARTNUMBER", strPartObjectName);
		        	String strViewPartNumber = StringUtils.trimToEmpty((String)sqlSession.selectOne("VW_PART_PARTNUMBER", viewRevisePartMap));
			        System.out.println("VIEW   PART   NUMBER   :   "+strViewPartNumber);
			        //////////////////////////////////VW_TABLE  PartNumber  Exists YN  Start !!!
			        
			        
			        //////////////////////////////////[ (Part Number) Nothing  ]
			        if(DomainConstants.EMPTY_STRING.equals(strViewPartNumber)){
			        	
			        	
			        	///////////////////////// TB_TABLE  PART New Insert  Start  !!!
				        Map dbPartDataMap = returnNewPartParamMap(context, strPartObjectId, sqlSession);
				        dbPartDataMap.put("EONO",  strEOName);
				        dbPartDataMap.put("GUBUN",  "N");
				    	sqlSession.insert("insertPLM_Part", dbPartDataMap);
				    	///////////////////////// TB_PART_TABLE  PART New Insert  End  !!!
				    	
				    	
				    	///////////////////////// TB_NEWBOM_TABLE  New BOM Insert  Start  !!! 
				    	if(! "cdmPhantomPart".equals(strPartObjectType) ){
				    		
				    		
				    		DomainObject partDomObj = new DomainObject(strPartObjectId);
					        
					        MapList mlChildPartList = partDomObj.getRelatedObjects(context, 
					        														DomainConstants.RELATIONSHIP_EBOM, 	// relationship
																					cdmConstantsUtil.TYPE_CDMPART, 		// type
																					busSelect, 							// objects
																					relSelect, 							// relationships
																					false, 								// to
																					true, 								// from
																					(short) 1, 							// recurse
																					"revision == last", 				// where
																					null, 								// relationship where
																					(short) 0); 						// limit
																
					        
					        String strPartOwner = (String)dbPartDataMap.get("CFGCREATOR");
				    		strPartOwner = strPartOwner.toUpperCase();
				    		
				    		SimpleDateFormat transFormat1 = new SimpleDateFormat("yyyy-MM-dd");
					    	Date dDate = transFormat1.parse(sDate);
					    	
					        
					        StringList slChildPartList = new StringList();
					        
					        for(int j=0; j<mlChildPartList.size(); j++){
					        	
					        	Map childMap = (Map)mlChildPartList.get(j);
					        	
						        String strChildPartName      = (String) childMap.get(DomainObject.SELECT_NAME);
						        String strChildPartRevision  = (String) childMap.get(DomainObject.SELECT_REVISION);
						        String strPartQuantity       = (String) childMap.get(DomainConstants.SELECT_ATTRIBUTE_QUANTITY);
						        
						        
						        StringBuffer strChildPartBuffer = new StringBuffer();
						        strChildPartBuffer.append(strChildPartName);
						        strChildPartBuffer.append("|");
						        strChildPartBuffer.append(strChildPartRevision);
						        
						        slChildPartList.add(strChildPartBuffer.toString());
						        
						        if(! DomainConstants.EMPTY_STRING.equals(strPartQuantity)){
						    		
									//if(strPartQuantity.indexOf(".") > -1){
						        		//strPartQuantity = strPartQuantity.substring(0, strPartQuantity.indexOf("."));
						        	//}
							    	
							    		
							    	//New BOM
									Map dbNewBOMDataMap = new HashMap();
								    dbNewBOMDataMap.put("PARENT_PART_NO"	,  	strPartObjectName);
								    dbNewBOMDataMap.put("PARENT_PART_REV"	, 	strPartObjectRevision);
								    dbNewBOMDataMap.put("CHILD_PART_NO"		,   strChildPartName);
								    dbNewBOMDataMap.put("CHILD_PART_REV"	,  	strChildPartRevision);    
								    dbNewBOMDataMap.put("QUANTITY"			,  	strPartQuantity);
								    dbNewBOMDataMap.put("EXPORT_DATETIME"	,  	dDate);
								    dbNewBOMDataMap.put("ECONO"				,  	strEOName);
								    dbNewBOMDataMap.put("CHANGE_TYPE"		,  	"A");
								    dbNewBOMDataMap.put("IMPORTYN"			,	"");
								    dbNewBOMDataMap.put("CREATOR"			, 	strPartOwner);
								        
								    if(! DomainConstants.EMPTY_STRING.equals(strChildPartName) && ! DomainConstants.EMPTY_STRING.equals(strChildPartRevision)){
								        sqlSession.insert("insertPLM_NEWBOM", dbNewBOMDataMap);
								    }
							        
						    	}
					        	
					        }

				    	}
				    	///////////////////////// TB_NEWBOM_TABLE  New BOM Insert  End  !!!
				    	
				        	
			        }else{
			        	
			        	sendPartBOMDataToPLMReSend(context, strPartObjectId, strEOName, sqlSession, sDate, strPartObjectName, strPartObjectRevision, displayTitle);
			        	
			        }
		        	
		        } else {
		        	
			        if( "Y".equals(strViewCDM_YN) ){
				        
			        	sendPartBOMDataToPLMRequestRepeat(context, strPartObjectId, strEOName, sqlSession, sDate, strPartObjectName, strPartObjectRevision, displayTitle, slRequestRepeatPartList, requestRepeatNewBomMap);
			        	
			        }
			        
		        }
		        
		    }
		    
			System.out.println("#######################################################################################################");
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	
	
	private void sendPartBOMDataToPLMRequestRepeat(Context context, String strPartObjectId, String strEOName, SqlSession sqlSession, String sDate, String strPartObjectName, String strPartObjectRevision, String displayTitle, StringList requestRepeatPartList, Map requestRepeatNewBomMap) throws Exception{   
		
		try{
			
			StringBuffer strPartRevisionBuffer = new StringBuffer();
			strPartRevisionBuffer.append(strPartObjectName);
			strPartRevisionBuffer.append("|");
			strPartRevisionBuffer.append(strPartObjectRevision);
			
			String strGubunFlag = "";
			
			System.out.println("Request Repeat Part List   =       " + requestRepeatPartList);
			System.out.println("strPartRevisionBuffer.toString()   " + strPartRevisionBuffer.toString());
			
			
			DomainObject partDomObj = new DomainObject(strPartObjectId);
			
		    String strPartObjectType      = partDomObj.getInfo(context, "type");
		    String strPartObjectDrawingNo = partDomObj.getInfo(context, cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO);
		    
		    String strPreviousRevision = StringUtils.trimToEmpty( partDomObj.getInfo(context, "previous.revision") );
		    System.out.println("REQUEST  REPEAT  PREVIOUS   REVISION   "+strPreviousRevision);
			
			
		    /////////////////////////////// TB_PART PartList  Now Part Name_Revision  [Exists]  TB_PART Update  Start !!!
			if ( requestRepeatPartList.contains( strPartRevisionBuffer.toString()) ) {
				
				
				SelectList selectStmts = new SelectList();
				selectStmts.add(DomainObject.SELECT_ID);
				selectStmts.add(DomainObject.SELECT_TYPE);
				selectStmts.add(DomainObject.SELECT_NAME);
				selectStmts.add(DomainObject.SELECT_REVISION);
				selectStmts.add("attribute[cdmPartDrawingNo]");
				selectStmts.add("to["+DomainConstants.RELATIONSHIP_PART_SPECIFICATION+"].from.name");
				selectStmts.add("to["+DomainConstants.RELATIONSHIP_PART_SPECIFICATION+"].from.revision");
				

				SelectList selectRel = new SelectList();
				Pattern type2DPattern = new Pattern("CATDrawing");
				type2DPattern.addPattern("cdmNXDrawing");
				type2DPattern.addPattern("cdmAutoCAD");
				
				Pattern typeDerivedOutputPattern = new Pattern("Derived Output");
				
				MapList ml2DList = partDomObj.getRelatedObjects(context, 
																DomainConstants.RELATIONSHIP_PART_SPECIFICATION, // relationship
																type2DPattern.getPattern(), // type
																selectStmts, 				// objects
																selectRel, 					// relationships
																false, 						// to
																true, 						// from
																(short) 1, 					// recurse
																null, 						// where
																null, 						// relationship where
																(short) 0); 				// limit
		    	
		    	System.out.println("Request Repeat 2D List   :   "+ml2DList);
				
		    	
		    	StringBuffer strDrawingBuffer = new StringBuffer();
		    	int i2DListSize = ml2DList.size();
		    	for (int i=0; i<i2DListSize; i++) {
		    		
		    		Map drawingMap = (Map)ml2DList.get(i);
		    		String strDrawingId       = (String)drawingMap.get(DomainObject.SELECT_ID);
		    		String strDrawingType     = (String)drawingMap.get(DomainObject.SELECT_TYPE);
		    		String strDrawingName     = (String)drawingMap.get(DomainObject.SELECT_NAME);
		    		String strDrawingRevision = (String)drawingMap.get(DomainObject.SELECT_REVISION);
		    		
		    		String strDrawingRelPartName       = (String)drawingMap.get("to["+DomainConstants.RELATIONSHIP_PART_SPECIFICATION+"].from.name");
		    		//String strDrawingRelPartName     = extractVal(oDrawingRelPartName);
		    		
		    		String strDrawingRelPartRevision   = (String)drawingMap.get("to["+DomainConstants.RELATIONSHIP_PART_SPECIFICATION+"].from.revision");
		    		//String strDrawingRelPartRevision = extractVal(oDrawingRelPartRevision);
		    		
		    		//strCADRevision = strDrawingRevision;
		    		if ("CATDrawing".equals(strDrawingType)) {
		    			
		    			
		    			DomainObject catDrawingObj = new DomainObject(strDrawingId);
		    			MapList mlDerivedOutputList = catDrawingObj.getRelatedObjects(context, 
																			"Derived Output", 			// relationship
																			typeDerivedOutputPattern.getPattern(), 			// type
																			selectStmts, 				// objects
																			selectRel, 					// relationships
																			false, 						// to
																			true, 						// from
																			(short) 1, 					// recurse
																			null, 						// where
																			null, 						// relationship where
																			(short) 0); 				// limit
		    			
		    			
		    			System.out.println("Request Repeat Derived Output List     :     " + mlDerivedOutputList);
		    			
		    			for(int k=0; k<mlDerivedOutputList.size(); k++){
		    				Map derivedOutputMap = (Map)mlDerivedOutputList.get(k);
		    				String strDerivedOutputId = (String)derivedOutputMap.get(DomainObject.SELECT_ID);
		    				
		    				DomainObject outputDomObj = new DomainObject(strDerivedOutputId);
		    				FileList fileList = outputDomObj.getFiles(context);
		    				
		    				
		    				int cntZipCount = 0;
		    				for (Iterator iterator = fileList.iterator(); iterator.hasNext();) {
		    					matrix.db.File objectFile = (matrix.db.File) iterator.next();
		    					
		    					String strFileName = objectFile.getName();
		    					String extensionsName = strFileName.substring(strFileName.lastIndexOf(".") + 1);
		    					extensionsName = extensionsName.toLowerCase();
		    					
		    					if ( "dwg".equals(extensionsName) ) {
		    						extensionsName = "zip";
		    					} 
		    					
		    					if( "cgm".equals(extensionsName) || ("zip".equals(extensionsName) && cntZipCount == 0) ) {
		    						
		    						strDrawingBuffer.append(strDrawingRelPartName);
		        					strDrawingBuffer.append("_");
		        					strDrawingBuffer.append(strDrawingRelPartRevision);
		        					strDrawingBuffer.append(".");
		        					strDrawingBuffer.append(extensionsName);
		        					
		        					if(iterator.hasNext()){
		        						strDrawingBuffer.append(";");	
		        	    			}
		        					
		    					} 
		    					
		    					if ( "zip".equals(extensionsName) ) {
		    						cntZipCount = 1;
		    					} 
		    					
		    				}
		    				
		    			}
		    			
		    		} else if ( "cdmNXDrawing".equals(strDrawingType) || "cdmAutoCAD".equals(strDrawingType) ) {

		    			DomainObject autocadAndNXDrawingObj = new DomainObject(strDrawingId);
		    			FileList fileList = autocadAndNXDrawingObj.getFiles(context);
						
						for (Iterator iterator = fileList.iterator(); iterator.hasNext();) {
							matrix.db.File objectFile = (matrix.db.File) iterator.next();
							
							String strFileName = objectFile.getName();
							String extensionsName = strFileName.substring(strFileName.lastIndexOf(".") + 1);
							extensionsName = extensionsName.toLowerCase();
							
							strDrawingBuffer.append(strDrawingRelPartName);
							strDrawingBuffer.append("_");
							strDrawingBuffer.append(strDrawingRelPartRevision);
							strDrawingBuffer.append(".");
							strDrawingBuffer.append(extensionsName);
							
							if(iterator.hasNext()){
								strDrawingBuffer.append(";");	
			    			}
							
						}
						
		    		}else if("cdmOrCADProduct".equals(strDrawingType)){
		    			
		    			DomainObject autocadAndNXDrawingObj = new DomainObject(strDrawingId);
		    			FileList fileList = autocadAndNXDrawingObj.getFiles(context);
						
						for (Iterator iterator = fileList.iterator(); iterator.hasNext();) {
							matrix.db.File objectFile = (matrix.db.File) iterator.next();
							
							String strFileName = objectFile.getName();
							String extensionsName = strFileName.substring(strFileName.lastIndexOf(".") + 1);
							//extensionsName = extensionsName.toLowerCase();
							
							strDrawingBuffer.append(strDrawingRelPartName);
							strDrawingBuffer.append("_");
							strDrawingBuffer.append(strDrawingRelPartRevision);
							strDrawingBuffer.append(".");
							strDrawingBuffer.append(extensionsName);
							
							if(iterator.hasNext()){
								strDrawingBuffer.append(";");	
			    			}
							
						}
						
		    		}
		    		
		    	}
		    	
		    	System.out.println("##### Request Repeat Drawing Buffer String     :     " + strDrawingBuffer.toString());
		    	
		    	
		    	String str2DFileName  = strDrawingBuffer.toString();
		    	String str3DFileName  = DomainConstants.EMPTY_STRING;
		    	String strCADNumber   = DomainConstants.EMPTY_STRING;
		    	String strCADRevision = DomainConstants.EMPTY_STRING;
		    	String strLinkType    = DomainConstants.EMPTY_STRING;
		    	
		    	
		    	///////////////////////////////////////////////////// Request Repeat Draw_Item Update  Start !!!
		    	if(! DomainConstants.EMPTY_STRING.equals(str2DFileName)){
		    		
		    		
		    		strCADNumber   = str2DFileName.substring(0, str2DFileName.indexOf("_"));
		        	strCADRevision = str2DFileName.substring(str2DFileName.indexOf("_")+1, str2DFileName.indexOf("."));
		        	
		        	String str3DObjectId = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", strPartObjectId, "from["+DomainConstants.RELATIONSHIP_PART_SPECIFICATION+"|to.type=='CATProduct'||to.type=='CATPart'].to.id"));
		        	if(! DomainConstants.EMPTY_STRING.equals(str3DObjectId)){
		        		str3DFileName = strDrawingBuffer.toString();
		        		str3DFileName  = str3DFileName.substring(0, str3DFileName.indexOf("."));
		            	str3DFileName += ".smg";
		        	}
		        	
		        	strLinkType = "D";
		        	if(strPartObjectName.equals(strPartObjectDrawingNo)){
		        		strLinkType = "B";
		        	}
		        	
		        		
		        	StringList slParentPartTypeList = (StringList)partDomObj.getInfoList(context, "to[EBOM].from.type");
		        		
		        	SelectList selectList = new SelectList(3);
		        	selectList.addId();
		        	selectList.addName();
		        	selectList.addType();
		        	selectList.addRevision();
		        	selectList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO);
		        	selectList.add("to["+DomainConstants.RELATIONSHIP_AFFECTED_ITEM+"].from.name");
		        	selectList.add("from["+DomainConstants.RELATIONSHIP_PART_SPECIFICATION+"|to.type=='CATDrawing'].to.revision");
		        		
		        	
		    	    ////////////////////////  ITEM DRAW TABLE INSERT [PhantomPart ] Start !!!
		    	    if("cdmPhantomPart".equals(strPartObjectType)){
		    	    		
		    	    	///////////////////////////////////// PhantomPart Series  Start !!!
		    	    	StringBuffer strBufferWhere = new StringBuffer();
		    	    	strBufferWhere.append(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO);
		    	    	strBufferWhere.append(" == '");
		    	    	strBufferWhere.append(strPartObjectDrawingNo);
		    	    	strBufferWhere.append("'");
		    	    	strBufferWhere.append(" && ");
		    	    	strBufferWhere.append("revision");
		    	    	strBufferWhere.append(" == ");
		    	    	strBufferWhere.append("last.revision");
				
				
						MapList mlPhantomPartDrawingList = DomainObject.findObjects(context, 
									cdmConstantsUtil.TYPE_CDMPHANTOMPART,         	// type
									DomainConstants.QUERY_WILDCARD,   				// name
									DomainConstants.QUERY_WILDCARD,   				// revision
									DomainConstants.QUERY_WILDCARD,   				// policy
									cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION,     // vault
									strBufferWhere.toString(),             			// where
									DomainConstants.EMPTY_STRING,     				// query
									true,							  				// expand
									selectList,                      				// objects
									(short)0);                        				// limits
				
						System.out.println("Phantom Part Drawing List     :     " + mlPhantomPartDrawingList);
						String strCATDrawingRevision = "";
						String strPhantomObjectId = "";
				
						if(mlPhantomPartDrawingList.size() > 0){
							Map map = (Map)mlPhantomPartDrawingList.get(0);
							strCATDrawingRevision = (String) map.get("from[Part Specification].to.revision");
							strPhantomObjectId = (String) map.get("id");
						}
				
						DomainObject phantomObj = new DomainObject(strPhantomObjectId);
				
						Map objMap = phantomObj.getInfo(context, selectList);
				
						SelectList relSelect = new SelectList();
						MapList mlPhantomChildPartList = phantomObj.getRelatedObjects(context, 
						 													DomainConstants.RELATIONSHIP_EBOM, 	// Relationship
						 													cdmConstantsUtil.TYPE_CDMMECHANICALPART, // Type name
						 													selectList, 							// objects Select
						 													relSelect, 							// Rel selects
						 													false,      						// to Direction
						 													true,     							// from Direction
						 													(short) 1, 							// recusion level
						 													"", 								// current == Release 
						 													"",
						 													0);
				
						mlPhantomChildPartList.add(0, objMap);
						///////////////////////////////////// PhantomPart CATDrawing Revision  End !!!
    	    	
    	    		
						if(mlPhantomChildPartList.size() > 1){
    	    		
							for(int i=0; i<mlPhantomChildPartList.size(); i++){
    		    			
								Map map = (Map)mlPhantomChildPartList.get(i);
    		    			
								String strManyPartOneDrawingPartId 		 = StringUtils.trimToEmpty((String)map.get(DomainConstants.SELECT_ID));
								String strManyPartOneDrawingPartName 	 = StringUtils.trimToEmpty((String)map.get(DomainConstants.SELECT_NAME));
								String strManyPartOneDrawingPartRevision = StringUtils.trimToEmpty((String)map.get(DomainConstants.SELECT_REVISION));
								String strManyPartOneDrawingName 		 = StringUtils.trimToEmpty((String)map.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO));
								String strManyPartOneDrawingEOName 		 = StringUtils.trimToEmpty((String)map.get("to["+DomainConstants.RELATIONSHIP_AFFECTED_ITEM+"].from.name"));
    		            	
    		            	
								String strPhantomPartLinkType = "D";
								if(strPartObjectDrawingNo.equals(strManyPartOneDrawingPartName)){
									strPhantomPartLinkType = "B";
								}
    			    		
    		    			
								Map dbItemDrawDataMap = new HashMap();
								dbItemDrawDataMap.put("EONO"		, strManyPartOneDrawingEOName);
    		    		
								if (DomainConstants.EMPTY_STRING.equals(strCADNumber)) {
									dbItemDrawDataMap.put("CADNUMBER" ,    strPartObjectDrawingNo);
								}else{
									dbItemDrawDataMap.put("CADNUMBER" ,    strCADNumber);
								}
    		    		
								if (DomainConstants.EMPTY_STRING.equals(strCADRevision)) {
									dbItemDrawDataMap.put("CADREV"    ,    strCATDrawingRevision);
								}else{
									dbItemDrawDataMap.put("CADREV"	  ,    strCADRevision);
								}
    		    		
								dbItemDrawDataMap.put("ITEMNUMBER"	, strManyPartOneDrawingPartName);
								dbItemDrawDataMap.put("VERSION"		, strManyPartOneDrawingPartRevision);
								dbItemDrawDataMap.put("LINKTYPE"	, strPhantomPartLinkType);
    		    			
    		    			
								System.out.println("!!Exists PhantomPart Item Draw Data Map     ::     " + dbItemDrawDataMap);
								sqlSession.insert("UPDATE_DRAW_ITEM", dbItemDrawDataMap);
    		    			
							}
    		    		
						}
    	    	
					////////////////////////  ITEM DRAW TABLE INSERT [PhantomPart ] End !!!
		    	    }else{
		    	    ////////////////////////  ITEM DRAW TABLE INSERT [Not  PhantomPart ] Start !!!	
		    	    		
		    	    	////////////////////////////////////  Series Part DrawItem  Start  !!!	
		    	    	if(! slParentPartTypeList.contains(cdmConstantsUtil.TYPE_CDMPHANTOMPART) ){
		
			    	    	StringBuffer strBufferWhere = new StringBuffer();
				    	    strBufferWhere.append(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO);
				    	    strBufferWhere.append(" == '");
				    	    strBufferWhere.append(strPartObjectDrawingNo);
				    	    strBufferWhere.append("' && ");
				    		strBufferWhere.append(DomainConstants.SELECT_REVISION);
				    		strBufferWhere.append(" == ");
				    		strBufferWhere.append(strPartObjectRevision);
				    		strBufferWhere.append(" && ");
				    		strBufferWhere.append("revision");
				    		strBufferWhere.append(" == ");
				    		strBufferWhere.append("last.revision");
				    		System.out.println("Many   Part   One   Drawing   Where     "+strBufferWhere.toString());
				    	    		
				    				
				    	    MapList mlManyPartOneDrawingList = DomainObject.findObjects(context, 
				    	    					cdmConstantsUtil.TYPE_CDMPART,         			// type
				    	    					DomainConstants.QUERY_WILDCARD,   				// name
				    	    					DomainConstants.QUERY_WILDCARD,   				// revision
				    	    					DomainConstants.QUERY_WILDCARD,   				// policy
				    	    					cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION,     // vault
				    	    					strBufferWhere.toString(),             			// where
				    	    					DomainConstants.EMPTY_STRING,     				// query
				    	    					true,							  				// expand
				    	    					selectList,                      				// objects
				    	    					(short)0);                        				// limits
				    	    	
				    	   	System.out.println("Many   Part   One   Drawing   List      " + mlManyPartOneDrawingList);
				    	    	
				    	    		
				    	    if(mlManyPartOneDrawingList.size() > 1){
				    	    		
				    		    for(int i=0; i<mlManyPartOneDrawingList.size(); i++){
				    		            		
				    		        Map map = (Map)mlManyPartOneDrawingList.get(i);
				    		        String strManyPartOneDrawingPartId 		 = StringUtils.trimToEmpty((String)map.get(DomainConstants.SELECT_ID));
				    		        String strManyPartOneDrawingPartName 	 = StringUtils.trimToEmpty((String)map.get(DomainConstants.SELECT_NAME));
				    		        String strManyPartOneDrawingPartRevision = StringUtils.trimToEmpty((String)map.get(DomainConstants.SELECT_REVISION));
				    		        String strManyPartOneDrawingName 		 = StringUtils.trimToEmpty((String)map.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO));
				    		        String strManyPartOneDrawingEOName 		 = StringUtils.trimToEmpty((String)map.get("to["+DomainConstants.RELATIONSHIP_AFFECTED_ITEM+"].from.name"));
				    		        			
				    		        String strNotPhantomPartLinkType = "D";
				    			    if(strPartObjectDrawingNo.equals(strManyPartOneDrawingPartName)){
				    			    	strNotPhantomPartLinkType = "B";
				    			    }
				    		            		
				    		            		
				    		        Map dbItemManyPartOneDrawingMap = new HashMap();
				    		        dbItemManyPartOneDrawingMap.put("EONO"			, strManyPartOneDrawingEOName);
				    		            
				    		        dbItemManyPartOneDrawingMap.put("CADNUMBER"		, strPartObjectDrawingNo);
				    		        dbItemManyPartOneDrawingMap.put("CADREV"		, strManyPartOneDrawingPartRevision);
				    		            
				    		        dbItemManyPartOneDrawingMap.put("ITEMNUMBER"	, strManyPartOneDrawingPartName);
				    		        dbItemManyPartOneDrawingMap.put("VERSION"		, strManyPartOneDrawingPartRevision);
				    		        dbItemManyPartOneDrawingMap.put("LINKTYPE"		, strNotPhantomPartLinkType);
				    		        			
				    		        System.out.println("!!Not Exists PhantomPart Item Draw Data Map     ::     " + dbItemManyPartOneDrawingMap);
				    		        sqlSession.insert("UPDATE_DRAW_ITEM", dbItemManyPartOneDrawingMap);
				    		            		
				    		    }
				    		    		
				    	    }
				    	    
		    	    	}
		    	    	////////////////////////////////////Series Part DrawItem  End  !!!	
		    	    		
		    	    }
		    	    ////////////////////////ITEM DRAW TABLE INSERT [Not  PhantomPart ] End !!!	
		        	
		    	}
		    	///////////////////////////////////////////////////// Request Repeat Draw_Item Update  End !!! 
		    	
		    	
				
				if( "".equals(strPreviousRevision) ){
					strGubunFlag = "NU";	
				}else{
					strGubunFlag = "RU";
				}
				
				if(cdmConstantsUtil.TYPE_CDM_ELECTRONIC_ASSEMBLY_PART.equals(strPartObjectType)){
					strLinkType = "B";
				}
				
				
				Map vwRequestRepeatPartMap = new HashMap();
			    vwRequestRepeatPartMap.put("EONO", 				strEOName);
			    vwRequestRepeatPartMap.put("ITEMNUMBER",  		strPartObjectName);
			    vwRequestRepeatPartMap.put("VERSION", 			strPartObjectRevision);
			    vwRequestRepeatPartMap.put("GUBUN", 			strGubunFlag);
			    
			    vwRequestRepeatPartMap.put("CN_FILENAME_2D",  	str2DFileName);
		    	vwRequestRepeatPartMap.put("CADNUMBER",  		strCADNumber);
		    	vwRequestRepeatPartMap.put("CADREV",  			strCADRevision);
		    	vwRequestRepeatPartMap.put("LINKTYPE",  		strLinkType);
		    	vwRequestRepeatPartMap.put("CN_FILENAME_3D",  	str3DFileName);
		    	
			    sqlSession.update("UPDATE_REQUEST_REPEAT_DRAWING_CHANGE_PART", vwRequestRepeatPartMap);
			    
			/////////////////////////////// TB_PART  PartList  Now Part Name_Revision  [Exists]  TB_PART Update  End !!!  
			} else {
				
			/////////////////////////////// TB_PART  PartList  Now Part Name_Revision  [Not Exists]	TB_PART Insert  Start !!!
				strGubunFlag = "N";
				
				Map dbPartDataMap = returnNewPartParamMap(context, strPartObjectId, sqlSession);
		        dbPartDataMap.put("EONO",  strEOName);
		        dbPartDataMap.put("GUBUN",  strGubunFlag);
		    	sqlSession.insert("insertPLM_Part", dbPartDataMap);
		    /////////////////////////////// TB_PART  PartList  Now Part Name_Revision  [Not Exists]	TB_PART Insert  End !!!	
		    	
			}
		    
		    
			
			
			/////////////////////////////// No PhantomPart  New BOM  Setting  Start !!!
		    
		    StringList busSelect = new StringList();
		    busSelect.add(DomainObject.SELECT_ID);
		    busSelect.add(DomainObject.SELECT_NAME);
		    busSelect.add(DomainObject.SELECT_REVISION);
		    
		    StringList relSelect = new StringList();
		    relSelect.add(DomainConstants.SELECT_ATTRIBUTE_QUANTITY);
		    
		    
		    if(! "cdmPhantomPart".equals(strPartObjectType) ){
	    		
		        
		        MapList mlChildPartList = partDomObj.getRelatedObjects(context, 
		        														DomainConstants.RELATIONSHIP_EBOM, 	// relationship
																		cdmConstantsUtil.TYPE_CDMPART, 		// type
																		busSelect, 							// objects
																		relSelect, 							// relationships
																		false, 								// to
																		true, 								// from
																		(short) 1, 							// recurse
																		"revision == last", 				// where
																		null, 								// relationship where
																		(short) 0); 						// limit
													
		        
		        Map viewNewBomChildMap = new HashMap();
		        viewNewBomChildMap.put("ECONO", 			strEOName);
		        viewNewBomChildMap.put("PARENT_PART_NO", 	strPartObjectName);
		        viewNewBomChildMap.put("PARENT_PART_REV", 	strPartObjectRevision);
				List<Map<String, String>> requestRepeatNewBomChildList = sqlSession.selectList("VW_REQUEST_REPEAT_NEW_BOM_CHILD", viewNewBomChildMap);
		        System.out.println("REQUEST   REPEAT   NEW   BOM   CHILD   LIST     :     " + requestRepeatNewBomChildList);
		        
		        
		        String strPartOwner = context.getUser();
	    		strPartOwner = strPartOwner.toUpperCase();
	    		
	    		SimpleDateFormat transFormat1 = new SimpleDateFormat("yyyy-MM-dd");
		    	Date dDate = transFormat1.parse(sDate);
		    	
		        
		        StringList slChildPartList = new StringList();
		        
		        for(int j=0; j<mlChildPartList.size(); j++){
		        	
		        	Map childMap = (Map)mlChildPartList.get(j);
		        	
			        String strChildPartName      = (String) childMap.get(DomainObject.SELECT_NAME);
			        String strChildPartRevision  = (String) childMap.get(DomainObject.SELECT_REVISION);
			        String strPartQuantity       = (String) childMap.get(DomainConstants.SELECT_ATTRIBUTE_QUANTITY);
			        
			        StringBuffer strChildPartBuffer = new StringBuffer();
			        strChildPartBuffer.append(strChildPartName);
			        strChildPartBuffer.append("|");
			        strChildPartBuffer.append(strChildPartRevision);
			        
			        slChildPartList.add(strChildPartBuffer.toString());
			        
			        if(! DomainConstants.EMPTY_STRING.equals(strPartQuantity)){
			    		
						//if(strPartQuantity.indexOf(".") > -1){
			        		//strPartQuantity = strPartQuantity.substring(0, strPartQuantity.indexOf("."));
			        	//}
				    	
				    	
				    	StringBuffer strNewBomBuffer = new StringBuffer(); 
				    	strNewBomBuffer.append(strPartObjectName);
				    	strNewBomBuffer.append("|");
				    	strNewBomBuffer.append(strPartObjectRevision);
				    	strNewBomBuffer.append("|");
				    	strNewBomBuffer.append(strChildPartName);
				    	strNewBomBuffer.append("|");
				    	strNewBomBuffer.append(strChildPartRevision);
				    	
				    	System.out.println("REPEAT   RESPONSE   NEW  BOM  MAP    :   " + requestRepeatNewBomMap);
				    	System.out.println("REPEAT   RESPONSE   NEW  BOM  PART   :   " + strNewBomBuffer.toString());
				    	
				    	
				    	////////////////////////////////////  RequestRepeat New BOM Add  Start !!!
				    	if (! requestRepeatNewBomMap.containsKey( strNewBomBuffer.toString()) ){
				    		
				    		String strPreviousPartId = StringUtils.trimToEmpty(partDomObj.getInfo(context, "previous.id"));
					        
						    if( "".equals(strPreviousPartId)){
						        	
						        	
						    	//New BOM
								Map dbNewBOMDataMap = new HashMap();
							    dbNewBOMDataMap.put("PARENT_PART_NO"	,  	strPartObjectName);
							    dbNewBOMDataMap.put("PARENT_PART_REV"	, 	strPartObjectRevision);
							    dbNewBOMDataMap.put("CHILD_PART_NO"		,   strChildPartName);
							    dbNewBOMDataMap.put("CHILD_PART_REV"	,  	strChildPartRevision);    
							    dbNewBOMDataMap.put("QUANTITY"			,  	strPartQuantity);
							    dbNewBOMDataMap.put("EXPORT_DATETIME"	,  	dDate);
							    dbNewBOMDataMap.put("ECONO"				,  	strEOName);
							    dbNewBOMDataMap.put("CHANGE_TYPE"		,  	"A");
							    dbNewBOMDataMap.put("IMPORTYN"			,	"");
							    dbNewBOMDataMap.put("CREATOR"			, 	strPartOwner);
						        
							    if(! DomainConstants.EMPTY_STRING.equals(strChildPartName) && ! DomainConstants.EMPTY_STRING.equals(strChildPartRevision)){
							    	sqlSession.insert("MERGE_NEW_BOM_REQUEST_REPEAT", dbNewBOMDataMap);
							    }
					        
					        }
						////////////////////////////////////  RequestRepeat New BOM Add  End !!!
						    
				    	} else {
				    		
				    		//New BOM Update
					    	Map dbNewBOMUpdateDataMap = new HashMap();
					        dbNewBOMUpdateDataMap.put("PARENT_PART_NO"	,  	strPartObjectName);
					        dbNewBOMUpdateDataMap.put("PARENT_PART_REV"	, 	strPartObjectRevision);
					        dbNewBOMUpdateDataMap.put("CHILD_PART_NO"	,   strChildPartName);
					        dbNewBOMUpdateDataMap.put("CHILD_PART_REV"	,  	strChildPartRevision);    
					        dbNewBOMUpdateDataMap.put("QUANTITY"		,  	strPartQuantity);
					        dbNewBOMUpdateDataMap.put("EXPORT_DATETIME"	,  	dDate);
					        dbNewBOMUpdateDataMap.put("ECONO"			,  	strEOName);
					        dbNewBOMUpdateDataMap.put("IMPORTYN"		,	"");
					        dbNewBOMUpdateDataMap.put("CREATOR"			, 	strPartOwner);
					        
					        if(! DomainConstants.EMPTY_STRING.equals(strChildPartName) && ! DomainConstants.EMPTY_STRING.equals(strChildPartRevision)){
					        	
					        	String requestRepeatExistsNewBom1ChildQuantity = StringUtils.trimToEmpty((String) sqlSession.selectOne("SELECT_REPEAT_PART_NEW_BOM_1", dbNewBOMUpdateDataMap) );
					        					
					        	/////////////////////////  Quantity Not Equals  New BOM  Quantity  Update  Start !!!
					        	if(! "".equals(requestRepeatExistsNewBom1ChildQuantity)){
					        		
					        		if(! requestRepeatExistsNewBom1ChildQuantity.equals(strPartQuantity)){
					        			
					        			dbNewBOMUpdateDataMap.put("CHANGE_TYPE"		,  	"U");
					        			sqlSession.update("MERGE_REQUEST_REPEAT_PLM_NEWBOM_1", dbNewBOMUpdateDataMap);	
					        		}
					        	/////////////////////////  Quantity Not Equals  New BOM  Quantity  Update  End !!!	
					        	}else{
					        		
					        	/////////////////////////  New BOM  Revise Insert  Start !!!	
						        	String requestRepeatExistsNewBom2ChildRevision = StringUtils.trimToEmpty((String) sqlSession.selectOne("SELECT_REPEAT_PART_NEW_BOM_2", dbNewBOMUpdateDataMap));
						        	
						        	String requestRepeatExistsNewBom3ChildRevision = StringUtils.trimToEmpty((String) sqlSession.selectOne("SELECT_REPEAT_PART_NEW_BOM_3", dbNewBOMUpdateDataMap));
						        	
						        	
						        	if ( "".equals(requestRepeatExistsNewBom2ChildRevision) && ! "".equals(requestRepeatExistsNewBom3ChildRevision)) {
						        								        
						        		System.out.println("REQUEST REPEAT NEW BOM UPDATE REVISION CHANGE  :  "+dbNewBOMUpdateDataMap);
						        		dbNewBOMUpdateDataMap.put("CHANGE_TYPE"		,  	"R");
						        		sqlSession.insert("insertPLM_NEWBOM", dbNewBOMUpdateDataMap);
						        								        	
						        	}	
						        /////////////////////////  New BOM  Revise Insert  End !!!	
					        	}
					        	
					        }
				    		
				    	}
				        
			    	}
		        	
		        }
		        
		        //////////////////////////////////// Request Repeat  New BOM  Delete Update  Start !!! 
		        for(int n=0; n<requestRepeatNewBomChildList.size(); n++){
		        	
		        	Map requestRepeatNewBomChildMap = (Map)requestRepeatNewBomChildList.get(n);
		        	String strTableChildPartNo 			= (String) requestRepeatNewBomChildMap.get("CHILD_PART_NO");
		        	String strTableChildPartRevision	= (String) requestRepeatNewBomChildMap.get("CHILD_PART_REV");
		        	
		        	StringBuffer strTableChildPartBuffer = new StringBuffer();
		        	strTableChildPartBuffer.append(strTableChildPartNo);
		        	strTableChildPartBuffer.append("|");
		        	strTableChildPartBuffer.append(strTableChildPartRevision);
		        	
		        	if(! slChildPartList.contains(strTableChildPartBuffer.toString()) ){
		        		
		        		Map updateNewBomMap = new HashMap();
		        		updateNewBomMap.put("PARENT_PART_NO"	,  	strPartObjectName);
		        		updateNewBomMap.put("PARENT_PART_REV"	, 	strPartObjectRevision);
		        		updateNewBomMap.put("CHILD_PART_NO"		,  	strTableChildPartNo);
		        		updateNewBomMap.put("CHILD_PART_REV"	, 	strTableChildPartRevision);
		        		updateNewBomMap.put("ECONO"				, 	strEOName);
		        		updateNewBomMap.put("CHANGE_TYPE"		,   "D");
		        		sqlSession.update("UPDATE_NEWBOM", updateNewBomMap);
		        		
		        	}
		        	
		        }
		        //////////////////////////////////// Request Repeat  New BOM  Delete Update  End !!!

	    	}
		    
		    
		    //////////////////////////////////////////////////////////////////////////////////////////////////////////////
		    ////////////////////////////  REQUEST  REPEAT  == Change Part Process ==   /////////////////////////////////// 
		    //////////////////////////////////////////////////////////////////////////////////////////////////////////////
		    
		    
		    String strEO_PLMType = strEOName.substring(0, 3);
		    System.out.println("EO PLM TYPE   =   "+strEO_PLMType);
		    System.out.println("Previous Revision   =   "+strPreviousRevision);
		   
		    if(! "PKA".equals(strEO_PLMType) && ! "".equals(strPreviousRevision)){
		    	
		    	////////////////// Material & Surface Information   Start!!!
		        String strPartPLMObjectId = StringUtils.trimToEmpty( (String) partDomObj.getInfo(context, cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PLM_OBJECTID) );
	    		String strPartMaterial = DomainConstants.EMPTY_STRING;
	    		String strPartSurface  = DomainConstants.EMPTY_STRING;
	    		
	    		if(! DomainConstants.EMPTY_STRING.equals(strPartPLMObjectId)){
	    			String[] strPartPLMObjectIdArray = strPartPLMObjectId.split("\\|");
	    			strPartMaterial = strPartPLMObjectIdArray[0];
	    			strPartSurface  = strPartPLMObjectIdArray[1];
	    		}
	    		////////////////// Material & Surface Information   End!!!
		        
			    //////////////////EO Information   Start!!!
			    Map eoMap = new HashMap();
			    eoMap.put("EO_NUMBER", strEOName);
			    List<Map<String, String>> eoDataList = (List)sqlSession.selectList("EO_DATA", eoMap);
			        
			    String strEOType = DomainConstants.EMPTY_STRING;
			    String strEAR    = DomainConstants.EMPTY_STRING;
			    if(eoDataList.size() > 0){
			        strEOType = eoDataList.get(0).get("EO_TYPE");
				    strEAR    = eoDataList.get(0).get("EO_EAR");	
			    }else{
			        strEOType = "PRODUCT";
			        strEAR    = "NP";
			    }
			    //////////////////EO Information   End!!!
			    
			    
			    
			    
			    Map partSendMap = new HashMap();
		        partSendMap.put("PARENTNUMBER", strPartObjectName);
		        partSendMap.put("PARENTREV", strPartObjectRevision);
		        	
		        System.out.println("REPEAT   RESPONSE   PartObjectName     =   "+strPartObjectName);
		        System.out.println("REPEAT   RESPONSE   PreviousRevision   =   "+strPreviousRevision);
		        System.out.println("REPEAT   RESPONSE   PreviousRevision   =   "+strPartObjectRevision);
		        	
			    //    Previous Part BOM 
			    List<Map<String, String>> previousPartViewList = sqlSession.selectList("VW_BOM_PARTNUMBER", partSendMap);
			    System.out.println("!!!!!previousPartViewList     :     " + previousPartViewList);
			    
			    //    Now Part Child Part
			    StringList slChildPartList = partDomObj.getInfoList(context, "from["+DomainConstants.RELATIONSHIP_EBOM+"].to."+DomainConstants.SELECT_ID);
			    System.out.println("!!!!!slChildPartList   :   "+slChildPartList);
			        
			    
			    
			    if(previousPartViewList.size() > 0){
			        	
			    	
			    	//////////////// Previous Part Child Part Quantity Map  Start!!! 
			    	StringList slPrevChildList = new StringList();
			    	Map quantityMap = new HashMap();
			    	
			    	for(int k=0; k<previousPartViewList.size(); k++){
				        	
				    	Map sendBOMMap = (Map)previousPartViewList.get(k);
				    	String strViewChildNumber   = (String)sendBOMMap.get("CHILDNUMBER");
				    	String strViewChildRevision = (String)sendBOMMap.get("CHILDREV");
				    	String strViewChildQuantity = String.valueOf(sendBOMMap.get("QUANTITY"));
				        	
				    	slPrevChildList.add(strViewChildNumber); 
				    	quantityMap.put(strViewChildNumber, strViewChildQuantity);
				        	
				    }
			    	//////////////// Previous Part Child Part Quantity Map End !!! 
				        
			    	
			    	
				    //////////////////////////////  Now Date Start!!!    
				    SimpleDateFormat transFormat1 = new SimpleDateFormat("yyyy-MM-dd");
					Date dDate = transFormat1.parse(sDate);
					//////////////////////////////  Now Date End!!!   
				    	
					
					///////////////////// Delete Compare StringList 
				    StringList slPartNumberChildList = new StringList();
				        
				    
				    ///////////////////// Now Part  Child Parts { [Add], [Mod] }  Start !!! 
				    for(int j=0; j<slChildPartList.size(); j++) {
			        		
				    	
			    		String strChildPartId = (String)slChildPartList.get(j);
				    	DomainObject partChildObj = new DomainObject(strChildPartId); 
				    	SelectList slPartChildBusList = new SelectList();
				    	slPartChildBusList.add(DomainConstants.SELECT_NAME);
				    	slPartChildBusList.add(DomainConstants.SELECT_REVISION);
				    	slPartChildBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_NAME);
				        	
				    	
				    	//////////////////////////////  [After Part Value Setting...] [Start]
				        Map childPartDataMap = (Map)partChildObj.getInfo(context, slPartChildBusList);
				        String strPartQuantity       = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {strChildPartId, "to["+DomainConstants.RELATIONSHIP_EBOM+"|from.id=='"+strPartObjectId+"']."+DomainConstants.SELECT_ATTRIBUTE_QUANTITY}));
				        String strChildPartNumber    = (String) childPartDataMap.get(DomainConstants.SELECT_NAME);
					    String strChildPartRevision  = (String) childPartDataMap.get(DomainConstants.SELECT_REVISION);
					    String strChildPartName   	 = (String) childPartDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_NAME);
					        
					    //if(strPartQuantity.indexOf(".") > -1){
				        	//strPartQuantity = strPartQuantity.substring(0, strPartQuantity.indexOf("."));
				        //}
					    
					    
					    StringList slPartRelVehicleIdList = partChildObj.getInfoList(context, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_VEHICLE+"].from."+displayTitle);
					    StringBuffer strPartVehicleBuffer = new StringBuffer();
					   	if(slPartRelVehicleIdList.size()>0){
					    	for (int k=0; k<slPartRelVehicleIdList.size(); k++) {
					    		strPartVehicleBuffer.append(slPartRelVehicleIdList.get(k));
					    		if(slPartRelVehicleIdList.size()-1 != k){
					    			strPartVehicleBuffer.append(",");	
					    		}
					    	}	
					    }
					   	//////////////////////////////[After Part Value Setting...] [End]
					   	
					        
					   	///////////////////// Delete Compare StringList  Start !!!
					    slPartNumberChildList.add(strChildPartNumber);
					    ///////////////////// Delete Compare StringList  End !!!
					    
					    	
					    ////////////////////////////////// [Previous Part Value Setting...] [Start]
					    String strChildPreviousPartId = StringUtils.trimToEmpty(partChildObj.getInfo(context, "previous.id"));
					    	
		        		String strChildPreviousPartNumber             = DomainConstants.EMPTY_STRING;
		        		String strChildPreviousPartRevision           = DomainConstants.EMPTY_STRING;
		        		String strChildPreviousPartName               = DomainConstants.EMPTY_STRING;
		        		String strChildPreviousPartQuantity           = DomainConstants.EMPTY_STRING;
		        		String strChildPreviousPartMaterial           = DomainConstants.EMPTY_STRING;
		        		String strChildPreviousPartTreatment          = DomainConstants.EMPTY_STRING;
		        			
		        		String strChildPreviousPartParentPartId       = DomainConstants.EMPTY_STRING;
		        		String strChildPreviousPartParentPartNumber   = DomainConstants.EMPTY_STRING;
		        		String strChildPreviousPartParentPartRevision = DomainConstants.EMPTY_STRING;
		        			
		        		StringBuffer strChildPreviousPartVehicleBuffer = new StringBuffer();
		        		
		        		strChildPreviousPartParentPartId = StringUtils.trimToEmpty(partDomObj.getInfo(context, "previous.id"));
		        		
		        		if(! DomainConstants.EMPTY_STRING.equals(strChildPreviousPartParentPartId)){
		        		
		        			DomainObject previousParentPartObj = new DomainObject(strChildPreviousPartParentPartId);
		        			strChildPreviousPartParentPartNumber    = StringUtils.trimToEmpty(previousParentPartObj.getInfo(context, "name"));
		        			strChildPreviousPartParentPartRevision  = StringUtils.trimToEmpty(previousParentPartObj.getInfo(context, "revision"));
		        			
		        		}
		        			
		        		if(! DomainConstants.EMPTY_STRING.equals(strChildPreviousPartId)){
		        				
		        			DomainObject childPrevDomObj = new DomainObject(strChildPreviousPartId);
		        			SelectList slChildPrevBusList = new SelectList();
		        			slChildPrevBusList.add(DomainConstants.SELECT_NAME);
		        			slChildPrevBusList.add(DomainConstants.SELECT_REVISION);
		        			slChildPrevBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_NAME);
		        			slChildPrevBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PLM_OBJECTID);
		        				
		        			Map childPrevDataMap = (Map) childPrevDomObj.getInfo(context, slChildPrevBusList);
		        				
		        			strChildPreviousPartNumber    = StringUtils.trimToEmpty((String) childPrevDataMap.get(DomainConstants.SELECT_NAME));
		        			strChildPreviousPartRevision  = StringUtils.trimToEmpty((String) childPrevDataMap.get(DomainConstants.SELECT_REVISION));
		        			strChildPreviousPartName      = StringUtils.trimToEmpty((String) childPrevDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_NAME)); 
		        			strChildPreviousPartQuantity  = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {strChildPreviousPartId, "to["+DomainConstants.RELATIONSHIP_EBOM+"|from.id=='"+strChildPreviousPartParentPartId+"']."+DomainConstants.SELECT_ATTRIBUTE_QUANTITY}));
		        				
		        			String strChildPreviousPartPLMObjectId = (String) childPrevDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PLM_OBJECTID);
		        				
		        			if(! DomainConstants.EMPTY_STRING.equals(strChildPreviousPartPLMObjectId)){
		        				String[] strChildPreviousPartPLMObjectIdArray = strChildPreviousPartPLMObjectId.split("\\|");
		        				strChildPreviousPartMaterial   = strChildPreviousPartPLMObjectIdArray[0];
		        				strChildPreviousPartTreatment  = strChildPreviousPartPLMObjectIdArray[1];
		        			}
		        				
		        				
//		        			if(strChildPreviousPartQuantity.indexOf(".") > -1){
//		        				strChildPreviousPartQuantity = strChildPreviousPartQuantity.substring(0, strChildPreviousPartQuantity.indexOf("."));
//					        }
		        						        		
		        				
		        			StringList slChildPreviousPartRelVehicleIdList = childPrevDomObj.getInfoList(context, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_VEHICLE+"].from."+displayTitle);
		        			if(slChildPreviousPartRelVehicleIdList.size()>0){
		        				for (int kk=0; kk<slChildPreviousPartRelVehicleIdList.size(); kk++) {
		        					strChildPreviousPartVehicleBuffer.append(slChildPreviousPartRelVehicleIdList.get(kk));
		        					if(slChildPreviousPartRelVehicleIdList.size()-1 != kk){
		        						strChildPreviousPartVehicleBuffer.append(",");	
		        					}
		        				}	
		        			}
		        				
		        		}
		        		//////////////////////////////////////////////[Previous Part Value Setting...] [End]
					    	
					    	
		        		System.out.println("REPEAT   RESPONSE   slPrevChildList      :     "+slPrevChildList);
		        		System.out.println("REPEAT   RESPONSE   strChildPartNumber   :     "+strChildPartNumber);
		        		
		        		
		        		////////////////////////////////////// Request Repeat ChangePart [Add] Update Start !!!  
			        	if(! slPrevChildList.contains(strChildPartNumber)){
			        		
			        		
			        		Map changePartDataMap = new HashMap();
			        		changePartDataMap.put("AFTER_PARENT_PART_ID", 	strPartObjectName);
			        		changePartDataMap.put("AFTER_PARENT_PART_REV", 	strPartObjectRevision);
			        		changePartDataMap.put("AFTER_CHILD_PART_ID", 	strChildPartNumber);
			        		changePartDataMap.put("AFTER_CHILD_PART_REV", 	strChildPartRevision);
			        		
			        		///////////////////////////// ChangeType Classify  Setting Start !!!
			        		String strChangeType = StringUtils.trimToEmpty((String)sqlSession.selectOne("SELECT_CHANGE_PART_CHANGE_TYPE", changePartDataMap));
					        System.out.println("Change Type One   :   " + strChangeType);
			        			
					        String strClassify = "N";
					        if(!"".equals(strChangeType)){
					        	if(! "A".equals(strChangeType)){
					        		strClassify = "D";
					        	}
					        }
					        System.out.println("#Classify   :   " + strClassify);
					        ///////////////////////////// ChangeType Classify  Setting End !!!
					        
					        
					        
			        		////////////////////////////  Request Repeat ChangePart [Add] Start !!!
			        		Map changePartMap = new HashMap();
			        		changePartMap.put("EO_NUMBER", 				strEOName);
			        		changePartMap.put("EO_TYPE", 				strEOType);
			        		changePartMap.put("EAR", 					strEAR);
			        		changePartMap.put("BEFORE_PARENT_PART_ID", 	strChildPreviousPartParentPartNumber);
			        		changePartMap.put("BEFORE_PARENT_PART_REV", strChildPreviousPartParentPartRevision);
			        		changePartMap.put("BEFORE_CHILD_PART_ID", 	strChildPreviousPartNumber);
			        		changePartMap.put("BEFORE_CHILD_PART_REV", 	strChildPreviousPartRevision);
			        		changePartMap.put("BEFORE_CHILD_PART_NAME", strChildPreviousPartName);
			        		changePartMap.put("BEFORE_QUANTITY", 		strChildPreviousPartQuantity);
			        		changePartMap.put("BEFORE_MATERIAL", 		strChildPreviousPartMaterial);
			        		changePartMap.put("BEFORE_TREATMENT", 		strChildPreviousPartTreatment);
			        		changePartMap.put("BEFORE_VEHICLE", 		strChildPreviousPartVehicleBuffer.toString());
			        		changePartMap.put("AFTER_CHANGE_TYPE", 		"A");
			        		changePartMap.put("AFTER_PARENT_PART_ID", 	strPartObjectName);
			        		changePartMap.put("AFTER_PARENT_PART_REV", 	strPartObjectRevision);
			        		changePartMap.put("AFTER_CHILD_PART_ID", 	strChildPartNumber);
			        		changePartMap.put("AFTER_CHILD_PART_REV", 	strChildPartRevision);
			        		changePartMap.put("AFTER_CHILD_PART_NAME", 	strChildPartName);
			        		changePartMap.put("AFTER_QUANTITY", 		strPartQuantity);
			        		changePartMap.put("AFTER_MATERIAL", 		strPartMaterial);
			        		changePartMap.put("AFTER_TREATMENT", 		strPartSurface);
			        		changePartMap.put("AFTER_VEHICLE", 			strPartVehicleBuffer.toString());
			        		changePartMap.put("EXPORT_DATETIME", 		dDate);
			        		changePartMap.put("INTERFACE_DATETIME", 	"");
			        		changePartMap.put("CLASSIFY", 				strClassify);
			        			
			        			
			        		System.out.println("RESPONSE   REPEAT   [ADD] ChangePart PLM Insert Interface");
			        		System.out.println("RESPONSE   REPEAT   [ADD] ChangePartMap       "+changePartMap);
						    sqlSession.update("MERGE_CHANGE_PART_REQUEST_REPEAT", changePartMap);
						    //////////////////////////// Request Repeat ChangePart [Add] End !!!    
						    
							    
			        	}else{
			        		
			        		//////////////////////////// Request Repeat ChangePart [Quantity Update] Start !!!
			        		String strPrevPartQuantity  = (String)quantityMap.get(strChildPartNumber);
			        		if(! strPartQuantity.equals(strPrevPartQuantity)){
			        			
			        			Map viewPartMap = new HashMap();
						        viewPartMap.put("PARTNUMBER", strChildPartNumber);
						        viewPartMap.put("REV", strChildPartRevision);
					        	String strViewECONumber = StringUtils.trimToEmpty((String)sqlSession.selectOne("VW_PART_ECONO", viewPartMap));
						        System.out.println("REQUEST   REPEAT   VIEW   ECO   NUMBER   :   " + strViewECONumber);
					        	
						        if(! "".equals(strViewECONumber)){
						        	
				        			Map changePartMap = new HashMap();
					        		changePartMap.put("EO_NUMBER", 				strEOName);
					        		changePartMap.put("EO_TYPE", 				strEOType);
					        		changePartMap.put("EAR", 					strEAR);
					        		changePartMap.put("BEFORE_PARENT_PART_ID", 	strChildPreviousPartParentPartNumber);              
					        		changePartMap.put("BEFORE_PARENT_PART_REV", strChildPreviousPartParentPartRevision);            
					        		changePartMap.put("BEFORE_CHILD_PART_ID", 	strChildPreviousPartNumber);                
					        		changePartMap.put("BEFORE_CHILD_PART_REV", 	strChildPreviousPartRevision);            
					        		changePartMap.put("BEFORE_CHILD_PART_NAME", strChildPreviousPartName);            
					        		changePartMap.put("BEFORE_QUANTITY", 		strChildPreviousPartQuantity);           
					        		changePartMap.put("BEFORE_MATERIAL", 		strChildPreviousPartMaterial);    
					        		changePartMap.put("BEFORE_TREATMENT", 		strChildPreviousPartTreatment);  
					        		changePartMap.put("BEFORE_VEHICLE", 		strChildPreviousPartVehicleBuffer.toString());  
					        		changePartMap.put("AFTER_CHANGE_TYPE", 		"C");
					        		changePartMap.put("AFTER_PARENT_PART_ID", 	strPartObjectName);
					        		changePartMap.put("AFTER_PARENT_PART_REV", 	strPartObjectRevision);
					        		changePartMap.put("AFTER_CHILD_PART_ID", 	strChildPartNumber);
					        		changePartMap.put("AFTER_CHILD_PART_REV", 	strChildPartRevision);
					        		changePartMap.put("AFTER_CHILD_PART_NAME", 	strChildPartName);
					        		changePartMap.put("AFTER_QUANTITY", 		strPartQuantity);
					        		changePartMap.put("AFTER_MATERIAL", 		strPartMaterial);
					        		changePartMap.put("AFTER_TREATMENT", 		strPartSurface);
					        		changePartMap.put("AFTER_VEHICLE", 			strPartVehicleBuffer.toString());
					        		changePartMap.put("EXPORT_DATETIME", 		dDate);
					        		changePartMap.put("INTERFACE_DATETIME", 	"");
					        		changePartMap.put("CLASSIFY", 				"U");
				        				
					        		System.out.println("RESPONSE   REPEAT   [MOD] ChangePart PLM Insert Interface");
					        		System.out.println("RESPONSE   REPEAT   [MOD] ChangePartMap       "+changePartMap);
								    sqlSession.update("MERGE_CHANGE_PART_REQUEST_REPEAT", changePartMap);
								    
						        	
						        }
						    //////////////////////////// Request Repeat ChangePart [Quantity Update] End !!!        
			        		} else {
			        		////////////////////////////  Request Repeat ChangePart [Classify & ChangeType Update] Start !!!
				        		
			        			String strClassify = "N";
								
								Map requestRepeatBomMap = new HashMap();
								
								requestRepeatBomMap.put("PARENTNUMBER", strPartObjectName);
								requestRepeatBomMap.put("PARENTREV", strPartObjectRevision);
								requestRepeatBomMap.put("CHILDNUMBER", strChildPartNumber);
								requestRepeatBomMap.put("CHILDREV", strChildPartRevision);
								
								List<Map<String, String>> requestRepeatBomExists = (List)sqlSession.selectList("SELECT_REQUEST_REPEAT_BOM", requestRepeatBomMap);
								System.out.println("requestRepeatBomExists     " + requestRepeatBomExists);
								
								if(requestRepeatBomExists.size() > 0){
									strClassify = "D";
								}
								
								Map changePartAddUpdateMap = new HashMap();
								
								//changePartAddUpdateMap.put("AFTER_CHANGE_TYPE", "A");
								changePartAddUpdateMap.put("CLASSIFY", strClassify);
								changePartAddUpdateMap.put("AFTER_PARENT_PART_ID", strPartObjectName);
								changePartAddUpdateMap.put("AFTER_PARENT_PART_REV", strPartObjectRevision);
								changePartAddUpdateMap.put("AFTER_CHILD_PART_ID", strChildPartNumber);
								changePartAddUpdateMap.put("AFTER_CHILD_PART_REV", strChildPartRevision);
								changePartAddUpdateMap.put("EO_NUMBER", strEOName);
								
								
								System.out.println("!!!!!changePartAddUpdateMap     =     " + changePartAddUpdateMap);
								sqlSession.update("UPDATE_CHANGE_PART_1", changePartAddUpdateMap);
							////////////////////////////  Request Repeat ChangePart [Classify & ChangeType Update] End !!!	
							    
			        		}
			        		
			        	}
			        		
			        }
				    ///////////////////////////// Now Part  Child Parts { [Add], [Mod] }  End !!! 
				        
				    
				    
				    
				    System.out.println("!!!!!iPrevChildBOMSize        "+previousPartViewList.size());
				    System.out.println("!!!!!previousPartViewList     "+previousPartViewList);
				    //////////////////////////////////////Now Part  Child Parts { [Del] }  Start !!! 
				    for(int h=0; h<previousPartViewList.size(); h++){
				        	
				        Map sendBOMMap = (Map)previousPartViewList.get(h);
				        String strAfterPartNumber   		= (String)sendBOMMap.get("PARENTNUMBER");
				        String strAfterPartRevision 		= (String)sendBOMMap.get("PARENTREV");
				        String strAfterChildPartNumber   	= (String)sendBOMMap.get("CHILDNUMBER");
				        String strAfterChildPartRevision 	= (String)sendBOMMap.get("CHILDREV");
				        String strAfterPartQuantity 		= StringUtils.trimToEmpty(String.valueOf(sendBOMMap.get("QUANTITY")));
				        String strAfterChildPart = strAfterChildPartNumber; 
					    	
				        	
				        System.out.println("!!!!!slPartNumberChildList        	"+slPartNumberChildList);
					    System.out.println("!!!!!strAfterChildPart     			"+strAfterChildPart);
					    
					    //////////////////////////////////////Now Part  Child Parts { [Del] } Process Start !!! 
				        if(! slPartNumberChildList.contains(strAfterChildPart)){
				        		
				        	Map prevMap = new HashMap();
				        	prevMap.put("ITEMNUMBER", strAfterPartNumber);
				        	prevMap.put("VERSION",    strAfterPartRevision);
		
				        		
				        	String strAfterParentPartObjectId = DomainConstants.EMPTY_STRING;
				        	String strTmpMqlValue = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "temp query bus $1 $2 $3 select $4 dump $5", new String[] {cdmConstantsUtil.TYPE_CDMPART, strAfterPartNumber, strAfterPartRevision, "id", "|"}));    
				        	if(! DomainConstants.EMPTY_STRING.equals(strTmpMqlValue)){
				        		String[] strTmpMqlValueArray = strTmpMqlValue.split("\\|");
				        		strAfterParentPartObjectId = strTmpMqlValueArray[3];
				        	}
				        	
				        	
				        	DomainObject afterParentPartObj = new DomainObject(strAfterParentPartObjectId);
				        	String strBeforeParentPartObjectId = StringUtils.trimToEmpty(afterParentPartObj.getInfo(context, "previous.id"));
				        	String strAfterParentPartEOName   = StringUtils.trimToEmpty(afterParentPartObj.getInfo(context, "to["+DomainConstants.RELATIONSHIP_AFFECTED_ITEM+"].from.name"));
				        	
				        		
				        	String strAfterChildPartObjectId = DomainConstants.EMPTY_STRING;
				        	String strMqlValue = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "temp query bus $1 $2 $3 select $4 dump $5", new String[] {cdmConstantsUtil.TYPE_CDMPART, strAfterChildPartNumber, strAfterChildPartRevision, "id", "|"}));    
				        	if(! DomainConstants.EMPTY_STRING.equals(strMqlValue)){
				        		String[] strMqlValueArray = strMqlValue.split("\\|");
				        		strAfterChildPartObjectId = strMqlValueArray[3];
				        	}
				        		
				        	
				        	if(! "".equals(strAfterChildPartObjectId)){
				        		
				        		DomainObject afterChildPartObj = new DomainObject(strAfterChildPartObjectId); 
					        	SelectList slAfterChildPartBusList = new SelectList();
					        	slAfterChildPartBusList.add("previous.id");
					        	slAfterChildPartBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_NAME);
					        	slAfterChildPartBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PLM_OBJECTID);
					        	
					        	Map afterChildDataMap = afterChildPartObj.getInfo(context, slAfterChildPartBusList);
					        		
					        	String strBeforeChildPartObjectId 	= StringUtils.trimToEmpty( (String)afterChildDataMap.get("previous.id") );
					        	String strAfterChildPartItemName    = (String)afterChildDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_NAME);
					        	String strAfterChildPartPLMObjectId = (String)afterChildDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PLM_OBJECTID);
					        	    
					        		
					        	String strAfterChildPartMaterial    = DomainConstants.EMPTY_STRING;
					        	String strAfterChildPartSurface     = DomainConstants.EMPTY_STRING;
					        	if(! DomainConstants.EMPTY_STRING.equals(strAfterChildPartPLMObjectId)){
					        	    	
					        	    String[] strPrevPartPLMObjectIdArray = strAfterChildPartPLMObjectId.split("\\|");
					        	    strAfterChildPartMaterial = strPrevPartPLMObjectIdArray[0];
					        	    strAfterChildPartSurface  = strPrevPartPLMObjectIdArray[1];
					        	        
					        	}
					        	        
					        	StringList slAfterVehicleRelIdList = afterChildPartObj.getInfoList(context, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_VEHICLE+"].from."+displayTitle);
					        	StringBuffer strAfterVehicleBuffer = new StringBuffer();
					        	if(slAfterVehicleRelIdList.size()>0){
					        	    for (int k=0; k<slAfterVehicleRelIdList.size(); k++) {
					        	    	strAfterVehicleBuffer.append(slAfterVehicleRelIdList.get(k));
					        	    	if(slAfterVehicleRelIdList.size()-1 != k){
					        	    		strAfterVehicleBuffer.append(";");	
					        	    	}
					        	    }	
					        	}
					        	
					        	
					        	
					        	String strBeforeChildPartNumber 	= "";
					        	String strBeforeChildPartRevision 	= "";
					        	String strBeforeChildPartName 		= "";
					        	String strBeforeChildPartMaterial 	= "";
					        	String strBeforeChildPartSurface 	= "";
					        	String strBeforePartQuantity 		= "";
					        	StringBuffer strBeforeVehicleBuffer = new StringBuffer();
		
					        	String strBeforeParentPartNumber 	= "";
					        	String strBeforeParentPartRevision 	= "";
					        	
					        	
					        	
					        	if(! "".equals(strBeforeParentPartObjectId)){
					        		
					        		DomainObject beforeParentPartObj = new DomainObject(strBeforeParentPartObjectId);
					        		strBeforeParentPartNumber 	= beforeParentPartObj.getInfo(context, "name");
					        		strBeforeParentPartRevision = beforeParentPartObj.getInfo(context, "revision");
					        		
					        		if(! "".equals(strBeforeChildPartObjectId) ){
					        			strBeforePartQuantity = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {strBeforeParentPartObjectId, "from["+DomainConstants.RELATIONSHIP_EBOM+"|to.id=='"+strBeforeChildPartObjectId+"']."+DomainConstants.SELECT_ATTRIBUTE_QUANTITY}));
					        			if(strBeforePartQuantity.indexOf(".") > -1){
					        				strBeforePartQuantity = strBeforePartQuantity.substring(0, strBeforePartQuantity.indexOf("."));
								        }
					        		}
					        		
					        	}
					        	
					        	
					        	if(! "".equals(strBeforeChildPartObjectId) ){
					        		
					        		DomainObject beforChildObj = new DomainObject(strBeforeChildPartObjectId);	
					        	
					        		SelectList slBeforeChildPartBusList = new SelectList();
					        		slBeforeChildPartBusList.add(DomainConstants.SELECT_NAME);
					        		slBeforeChildPartBusList.add(DomainConstants.SELECT_REVISION);
						        	slBeforeChildPartBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_NAME);
						        	slBeforeChildPartBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PLM_OBJECTID);
						        	
						        	Map beforeChildDataMap = beforChildObj.getInfo(context, slBeforeChildPartBusList);
						        	
						        	
						        	strBeforeChildPartNumber    = (String)beforeChildDataMap.get(DomainConstants.SELECT_NAME);
						        	strBeforeChildPartRevision  = (String)beforeChildDataMap.get(DomainConstants.SELECT_REVISION);
						        	strBeforeChildPartName      = (String)beforeChildDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_NAME);
						        	String strBeforeChildPartPLMObjectId = (String)beforeChildDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PLM_OBJECTID);
						        	    
						        	
						        	if(! DomainConstants.EMPTY_STRING.equals(strBeforeChildPartPLMObjectId)){
					        	    	
						        	    String[] strAfterChildPartPLMObjectIdArray = strAfterChildPartPLMObjectId.split("\\|");
						        	    strBeforeChildPartMaterial = strAfterChildPartPLMObjectIdArray[0];
						        	    strBeforeChildPartSurface  = strAfterChildPartPLMObjectIdArray[1];
						        	        
						        	}
						        	
						        	StringList slBeforeVehicleRelIdList = beforChildObj.getInfoList(context, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_VEHICLE+"].from."+displayTitle);
						        	if(slBeforeVehicleRelIdList.size()>0){
						        	    for (int k=0; k<slBeforeVehicleRelIdList.size(); k++) {
						        	    	strBeforeVehicleBuffer.append(slBeforeVehicleRelIdList.get(k));
						        	    	if(slBeforeVehicleRelIdList.size()-1 != k){
						        	    		strBeforeVehicleBuffer.append(";");	
						        	    	}
						        	    }	
						        	}
					        	}
				        		
					        	
					        	System.out.println("####################AfterParentPartEOName        "+strAfterParentPartEOName);
					        	
					        	Map viewPartMap = new HashMap();
						        viewPartMap.put("PARTNUMBER", strAfterChildPartNumber);
						        viewPartMap.put("REV", strAfterChildPartRevision);
					        	String strViewECONumber = StringUtils.trimToEmpty((String)sqlSession.selectOne("VW_PART_ECONO", viewPartMap));
						        System.out.println("REQUEST   REPEAT   VIEW   ECO   NUMBER   :   " + strViewECONumber);
					        	
						        if(! "".equals(strViewECONumber)){
						        	
						        	
						        	Map changePartDataMap = new HashMap();
					        		changePartDataMap.put("AFTER_PARENT_PART_ID", 	strAfterPartNumber);
					        		changePartDataMap.put("AFTER_PARENT_PART_REV", 	strAfterPartRevision);
					        		changePartDataMap.put("AFTER_CHILD_PART_ID", 	strAfterChildPartNumber);
					        		changePartDataMap.put("AFTER_CHILD_PART_REV", 	strAfterChildPartRevision);
					        		
					        		String strChangeType = StringUtils.trimToEmpty((String)sqlSession.selectOne("SELECT_CHANGE_PART_CHANGE_TYPE", changePartDataMap));
							        System.out.println("Change Type One   :   " + strChangeType);
					        			
							        String strClassify = "N";
							        if(!"".equals(strChangeType)){
							        	if(! "D".equals(strChangeType)){
							        		strClassify = "D";
							        	}
							        }
							        System.out.println("!Classify   :   " + strClassify);
						        	
						        	//del
					        		Map changePartMap = new HashMap();
					        		changePartMap.put("EO_NUMBER", 					strAfterParentPartEOName);
					        		changePartMap.put("EO_TYPE", 					strEOType);
					        		changePartMap.put("EAR", 						strEAR);
					        		changePartMap.put("BEFORE_PARENT_PART_ID", 		strBeforeParentPartNumber);
					        		changePartMap.put("BEFORE_PARENT_PART_REV", 	strBeforeParentPartRevision);
					        		changePartMap.put("BEFORE_CHILD_PART_ID", 		strBeforeChildPartNumber);
					        		changePartMap.put("BEFORE_CHILD_PART_REV", 		strBeforeChildPartRevision);
					        		changePartMap.put("BEFORE_CHILD_PART_NAME", 	strBeforeChildPartName);
					        		changePartMap.put("BEFORE_QUANTITY", 			strBeforePartQuantity);
					        		changePartMap.put("BEFORE_MATERIAL", 			strBeforeChildPartMaterial);
					        		changePartMap.put("BEFORE_TREATMENT", 			strBeforeChildPartSurface);
					        		changePartMap.put("BEFORE_VEHICLE", 			strBeforeVehicleBuffer.toString());
					        		changePartMap.put("AFTER_CHANGE_TYPE", 			"D");
					        		changePartMap.put("AFTER_PARENT_PART_ID", 		strPartObjectName);//strAfterPartNumber       
					        		changePartMap.put("AFTER_PARENT_PART_REV", 		strPartObjectRevision);//strAfterPartRevision 
					        		changePartMap.put("AFTER_CHILD_PART_ID", 		strAfterChildPartNumber);
					        		changePartMap.put("AFTER_CHILD_PART_REV", 		strAfterChildPartRevision);
					        		changePartMap.put("AFTER_CHILD_PART_NAME", 		strAfterChildPartItemName);
					        		changePartMap.put("AFTER_QUANTITY", 			strAfterPartQuantity);
					        		changePartMap.put("AFTER_MATERIAL", 			strAfterChildPartMaterial);
					        		changePartMap.put("AFTER_TREATMENT", 			strAfterChildPartSurface);
					        		changePartMap.put("AFTER_VEHICLE", 				strAfterVehicleBuffer.toString());
					        		changePartMap.put("EXPORT_DATETIME", 			dDate);
					        		changePartMap.put("INTERFACE_DATETIME", 		"");
					        		changePartMap.put("CLASSIFY", 					strClassify);
					        			
					        		System.out.println("RESPONSE   REPEAT   [DEL] ChangePart PLM Insert Interface");
					        		System.out.println("RESPONSE   REPEAT   [DEL] ChangePartMap       "+changePartMap);
							        sqlSession.update("MERGE_CHANGE_PART_REQUEST_REPEAT", changePartMap);
							        
						        }
					        	
				        	}
					        
			        	}
				        //////////////////////////////////////Now Part  Child Parts { [Del] } Process End !!! 
				        	
				    }
				    ////////////////////////////////////// Now Part  Child Parts { [Del] }  End !!! 
				    
			    }
		    
		    }
		    
		    
        }catch(Exception e){
        	e.printStackTrace();
        	throw e;
        }
        
	}
	
	
	
	private void sendPartBOMDataToPLMReSend(Context context, String strPartObjectId, String strEOName, SqlSession sqlSession, String sDate, String strPartObjectName, String strPartObjectRevision, String displayTitle) throws Exception{
		
		try{
			
			DomainObject partDomObj = new DomainObject(strPartObjectId);
			
			
			////////////////////////////////////////  Now Part (type, previousRevision, PLMObjectId)  Start !!!
			SelectList slDataList = new SelectList();
			slDataList.add("type");
			slDataList.add("previous.revision");
			slDataList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PLM_OBJECTID);
			
	        Map partDataMap = (Map)partDomObj.getInfo(context, slDataList);
	        String strPartObjectType = StringUtils.trimToEmpty((String) partDataMap.get("type"));
	        String strPrevRevision = StringUtils.trimToEmpty((String) partDataMap.get("previous.revision"));
	        String strPartPLMObjectId = StringUtils.trimToEmpty((String) partDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PLM_OBJECTID));
	        System.out.println("PREVIOUS   REVISION   "+strPrevRevision);
	        ////////////////////////////////////////  Now Part (type, previousRevision, PLMObjectId)  End !!!
	        

	        ///////////////////////////////////////  TB_PART_TABLE  [Revise]  Insert  Start !!!
	        Map partReviseInsertMap = returnNewPartParamMap(context, strPartObjectId, sqlSession);
	        partReviseInsertMap.put("EONO", strEOName);
	        partReviseInsertMap.put("GUBUN", "R");
	        partReviseInsertMap.put("PREVERSION", strPrevRevision);
	        
	        sqlSession.insert("insertPLM_Part", partReviseInsertMap);
	        ///////////////////////////////////////  TB_PART_TABLE  [Revise]  Insert  End !!!
	        
	        
	        
	        
	        ///////////////////////// TB_NEWBOM_TABLE  [Revise]  New BOM Insert  Start  !!! 
	        StringList busSelect = new StringList();
		    busSelect.add(DomainObject.SELECT_ID);
		    busSelect.add(DomainObject.SELECT_NAME);
		    busSelect.add(DomainObject.SELECT_REVISION);
		    
		    StringList relSelect = new StringList();
		    relSelect.add(DomainConstants.SELECT_ATTRIBUTE_QUANTITY);
	        
	        
	        if(! "cdmPhantomPart".equals(strPartObjectType) ){
	    		
		        
		        MapList mlChildPartList = partDomObj.getRelatedObjects(context, 
		        														DomainConstants.RELATIONSHIP_EBOM, 	// relationship
																		cdmConstantsUtil.TYPE_CDMPART, 		// type
																		busSelect, 							// objects
																		relSelect, 							// relationships
																		false, 								// to
																		true, 								// from
																		(short) 1, 							// recurse
																		"revision == last", 				// where
																		null, 								// relationship where
																		(short) 0); 						// limit
													
		        
		        String strPartOwner = context.getUser();
	    		strPartOwner = strPartOwner.toUpperCase();
	    		
	    		SimpleDateFormat transFormat1 = new SimpleDateFormat("yyyy-MM-dd");
		    	Date dDate = transFormat1.parse(sDate);
		    	
		        
		        for(int j=0; j<mlChildPartList.size(); j++){
		        	
		        	Map childMap = (Map)mlChildPartList.get(j);
		        	
		        	String strChildPartId        = (String) childMap.get(DomainObject.SELECT_ID);
			        String strChildPartName      = (String) childMap.get(DomainObject.SELECT_NAME);
			        String strChildPartRevision  = (String) childMap.get(DomainObject.SELECT_REVISION);
			        String strPartQuantity       = (String) childMap.get(DomainConstants.SELECT_ATTRIBUTE_QUANTITY);
			        
			        
			        String strPreviousPartId = StringUtils.trimToEmpty(partDomObj.getInfo(context, "previous.id"));
			        
			        if( "".equals(strPreviousPartId)){
			        	
			        	
			        	if(! DomainConstants.EMPTY_STRING.equals(strPartQuantity)){
				    		
							//if(strPartQuantity.indexOf(".") > -1){
				        		//strPartQuantity = strPartQuantity.substring(0, strPartQuantity.indexOf("."));
				        	//}
					    	
					    		
					    	//New BOM
							Map dbNewBOMDataMap = new HashMap();
						    dbNewBOMDataMap.put("PARENT_PART_NO"	,  	strPartObjectName);
						    dbNewBOMDataMap.put("PARENT_PART_REV"	, 	strPartObjectRevision);
						    dbNewBOMDataMap.put("CHILD_PART_NO"		,   strChildPartName);
						    dbNewBOMDataMap.put("CHILD_PART_REV"	,  	strChildPartRevision);    
						    dbNewBOMDataMap.put("QUANTITY"			,  	strPartQuantity);
						    dbNewBOMDataMap.put("EXPORT_DATETIME"	,  	dDate);
						    dbNewBOMDataMap.put("ECONO"				,  	strEOName);
						    dbNewBOMDataMap.put("CHANGE_TYPE"		,  	"A");
						    dbNewBOMDataMap.put("IMPORTYN"			,	"");
						    dbNewBOMDataMap.put("CREATOR"			, 	strPartOwner);
						        
						    if(! DomainConstants.EMPTY_STRING.equals(strChildPartName) && ! DomainConstants.EMPTY_STRING.equals(strChildPartRevision)){
						        sqlSession.update("MERGE_NEW_BOM_REQUEST_REPEAT", dbNewBOMDataMap);
						    }
					        
				    	}
			        	
			        }
		        	
		        }

	    	}
	        ///////////////////////// TB_NEWBOM_TABLE  [Revise]  New BOM Insert  Start  !!! 
	        
	        
    		
    		String strPartMaterial = DomainConstants.EMPTY_STRING;
    		String strPartSurface  = DomainConstants.EMPTY_STRING;
    		
    		if(! DomainConstants.EMPTY_STRING.equals(strPartPLMObjectId)){
    			String[] strPartPLMObjectIdArray = strPartPLMObjectId.split("\\|");
    			strPartMaterial = strPartPLMObjectIdArray[0];
    			strPartSurface  = strPartPLMObjectIdArray[1];
    		}
	        
    		
    		///////////////////////////////////////////////////////  Previous Revision No Nothing  Start !!!
	        if(! DomainConstants.EMPTY_STRING.equals(strPrevRevision)){
	        	
		        
		        //////////////////////////////////////////  EO Date Value Setting  Start !!!
		        Map eoMap = new HashMap();
		        eoMap.put("EO_NUMBER", strEOName);
		        List<Map<String, String>> eoDataList = (List)sqlSession.selectList("EO_DATA", eoMap);
		        System.out.println("EO   DATA   MAP     :     "+eoDataList);
		        
		        String strEOType = DomainConstants.EMPTY_STRING;
		        String strEAR    = DomainConstants.EMPTY_STRING;
		        if(eoDataList.size() > 0){
		        	strEOType = eoDataList.get(0).get("EO_TYPE");
			        strEAR    = eoDataList.get(0).get("EO_EAR");	
		        }else{
		        	strEOType = "PRODUCT";
		        	strEAR    = "NP";
		        }
		        ////////////////////////////////////////// EO Date Value Setting  End !!!
		        
		        
		        
		        //////////////////////////////////////////  PreviousPart VW_BOM List  Start !!!
		        Map partSendMap = new HashMap();
	        	partSendMap.put("PARENTNUMBER", strPartObjectName);
	        	partSendMap.put("PARENTREV", strPrevRevision);
		        List<Map<String, String>> previousPartViewList = sqlSession.selectList("VW_BOM_PARTNUMBER", partSendMap);
		        System.out.println("PREVIOUS   PART   LIST     :     "+previousPartViewList);
		        //////////////////////////////////////////  PreviousPart VW_BOM List  End !!!
		        
		        
		        //////////////////////////////////////////  Now Part  Child Part  Start !!!
		        StringList slChildPartList = partDomObj.getInfoList(context, "from["+DomainConstants.RELATIONSHIP_EBOM+"].to."+DomainConstants.SELECT_ID);
		        System.out.println("Child Part List   :   "+slChildPartList);
		        //////////////////////////////////////////  Now Part  Child Part  End !!!
		        
		        
		        //////////////////////////////////////////  PreviousPart VW_BOM List No Nothing  [Revise] ChangePart  Start !!!
		        if(previousPartViewList.size() > 0){
		        	
		        	
		        	//////////////////////////////////////  PreviousPart VW_BOM  PrevChildList, QuantityMap  Setting  Start !!!
		        	StringList slPrevChildList = new StringList();
		        	Map quantityMap = new HashMap();
			        for(int k=0; k<previousPartViewList.size(); k++){
			        	
			        	Map sendBOMMap = (Map)previousPartViewList.get(k);
			        	String strViewChildNumber   = (String)sendBOMMap.get("CHILDNUMBER");
			        	String strViewChildRevision = (String)sendBOMMap.get("CHILDREV");
			        	String strViewChildQuantity = String.valueOf(sendBOMMap.get("QUANTITY"));
			        	
			        	slPrevChildList.add(strViewChildNumber); 
			        	quantityMap.put(strViewChildNumber, strViewChildQuantity);
			        	
			        }
			        //////////////////////////////////////PreviousPart VW_BOM  PrevChildList, QuantityMap  Setting  End !!!
			        
			        
			        SimpleDateFormat transFormat1 = new SimpleDateFormat("yyyy-MM-dd");
			    	Date dDate = transFormat1.parse(sDate);
			    	
			    	
			        StringList slPartNumberChildList = new StringList();
			        
			        for(int j=0; j<slChildPartList.size(); j++) {
		        		
			        	
			        	/////////////////////////////////// After Part Value Setting  Start !!!
		        		String strChildPartId = (String)slChildPartList.get(j);
			        	DomainObject partChildObj = new DomainObject(strChildPartId); 
			        	SelectList slPartChildBusList = new SelectList();
			        	slPartChildBusList.add(DomainConstants.SELECT_NAME);
			        	slPartChildBusList.add(DomainConstants.SELECT_REVISION);
			        	slPartChildBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_NAME);
			        	
			        	Map childPartDataMap = (Map)partChildObj.getInfo(context, slPartChildBusList);
			        	String strPartQuantity       = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {strChildPartId, "to["+DomainConstants.RELATIONSHIP_EBOM+"|from.id=='"+strPartObjectId+"']."+DomainConstants.SELECT_ATTRIBUTE_QUANTITY}));
			        	String strChildPartNumber    = (String) childPartDataMap.get(DomainConstants.SELECT_NAME);
				        String strChildPartRevision  = (String) childPartDataMap.get(DomainConstants.SELECT_REVISION);
				        String strChildPartName   	 = (String) childPartDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_NAME);
				        
				        /////////////////////////////////////////////// Delete Compare StringList Setting Start !!!
				        slPartNumberChildList.add(strChildPartNumber);
				        /////////////////////////////////////////////// Delete Compare StringList Setting End !!!
				        
				        StringList slPartRelVehicleIdList = partChildObj.getInfoList(context, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_VEHICLE+"].from."+displayTitle);
				    	StringBuffer strPartVehicleBuffer = new StringBuffer();
				    	if(slPartRelVehicleIdList.size()>0){
				    		for (int k=0; k<slPartRelVehicleIdList.size(); k++) {
				    			strPartVehicleBuffer.append(slPartRelVehicleIdList.get(k));
				    			if(slPartRelVehicleIdList.size()-1 != k){
				    				strPartVehicleBuffer.append(",");	
				    			}
				    		}	
				    	}
				        
				    	
				        //if(strPartQuantity.indexOf(".") > -1){
			        		//strPartQuantity = strPartQuantity.substring(0, strPartQuantity.indexOf("."));
			        	//}
				        
				        /////////////////////////////////// After Part Value Setting  End !!!
				        
				        
				        
				    	
				    	///////////////////////////////////  [Previous Part Value Setting...] [Start]
				    	String strChildPreviousPartId = StringUtils.trimToEmpty(partChildObj.getInfo(context, "previous.id"));
				    	
	        			String strChildPreviousPartNumber             = DomainConstants.EMPTY_STRING;
	        			String strChildPreviousPartRevision           = DomainConstants.EMPTY_STRING;
	        			String strChildPreviousPartName               = DomainConstants.EMPTY_STRING;
	        			String strChildPreviousPartQuantity           = DomainConstants.EMPTY_STRING;
	        			String strChildPreviousPartMaterial           = DomainConstants.EMPTY_STRING;
	        			String strChildPreviousPartTreatment          = DomainConstants.EMPTY_STRING;
	        			
	        			String strChildPreviousPartParentPartId       = DomainConstants.EMPTY_STRING;
	        			String strChildPreviousPartParentPartNumber   = DomainConstants.EMPTY_STRING;
	        			String strChildPreviousPartParentPartRevision = DomainConstants.EMPTY_STRING;
	        			
	        			StringBuffer strChildPreviousPartVehicleBuffer = new StringBuffer();
	        			
	        			strChildPreviousPartParentPartId = StringUtils.trimToEmpty(partDomObj.getInfo(context, "previous.id"));
		        		
	        			
	        			
		        		if(! DomainConstants.EMPTY_STRING.equals(strChildPreviousPartParentPartId)){
		        		
		        			DomainObject previousParentPartObj = new DomainObject(strChildPreviousPartParentPartId);
		        			strChildPreviousPartParentPartNumber    = StringUtils.trimToEmpty(previousParentPartObj.getInfo(context, "name"));
		        			strChildPreviousPartParentPartRevision  = StringUtils.trimToEmpty(previousParentPartObj.getInfo(context, "revision"));
		        			
		        		}
		        			
		        		if(! DomainConstants.EMPTY_STRING.equals(strChildPreviousPartId)){
		        				
		        			DomainObject childPrevDomObj = new DomainObject(strChildPreviousPartId);
		        			SelectList slChildPrevBusList = new SelectList();
		        			slChildPrevBusList.add(DomainConstants.SELECT_NAME);
		        			slChildPrevBusList.add(DomainConstants.SELECT_REVISION);
		        			slChildPrevBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_NAME);
		        			slChildPrevBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PLM_OBJECTID);
		        				
		        				
		        			Map childPrevDataMap = (Map) childPrevDomObj.getInfo(context, slChildPrevBusList);
		        				
		        			strChildPreviousPartNumber    = StringUtils.trimToEmpty((String) childPrevDataMap.get(DomainConstants.SELECT_NAME));
		        			strChildPreviousPartRevision  = StringUtils.trimToEmpty((String) childPrevDataMap.get(DomainConstants.SELECT_REVISION));
		        			strChildPreviousPartName      = StringUtils.trimToEmpty((String) childPrevDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_NAME)); 
		        			
		        			
		        			strChildPreviousPartQuantity  = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {strChildPreviousPartId, "to["+DomainConstants.RELATIONSHIP_EBOM+"|from.id=='"+strChildPreviousPartParentPartId+"']."+DomainConstants.SELECT_ATTRIBUTE_QUANTITY}));
		        				
		        				
		        			String strChildPreviousPartPLMObjectId = (String) childPrevDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PLM_OBJECTID);
		        				
		        			if(! DomainConstants.EMPTY_STRING.equals(strChildPreviousPartPLMObjectId)){
		        				String[] strChildPreviousPartPLMObjectIdArray = strChildPreviousPartPLMObjectId.split("\\|");
		        				strChildPreviousPartMaterial   = strChildPreviousPartPLMObjectIdArray[0];
		        				strChildPreviousPartTreatment  = strChildPreviousPartPLMObjectIdArray[1];
		        			}
		        				
		        				
//		        			if(strChildPreviousPartQuantity.indexOf(".") > -1){
//		        				strChildPreviousPartQuantity = strChildPreviousPartQuantity.substring(0, strChildPreviousPartQuantity.indexOf("."));
//					        }
		        						        		
		        				
		        			StringList slChildPreviousPartRelVehicleIdList = childPrevDomObj.getInfoList(context, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_VEHICLE+"].from."+displayTitle);
		        			if(slChildPreviousPartRelVehicleIdList.size()>0){
		        				for (int kk=0; kk<slChildPreviousPartRelVehicleIdList.size(); kk++) {
		        					strChildPreviousPartVehicleBuffer.append(slChildPreviousPartRelVehicleIdList.get(kk));
		        					if(slChildPreviousPartRelVehicleIdList.size()-1 != kk){
		        						strChildPreviousPartVehicleBuffer.append(",");	
		        					}
		        				}	
		        			}
		        				
		        		}
		        		///////////////////////////////////  [Previous Part Value Setting...] [End]
				    	
				    	
				    	//////////////////////////////////////  Previous Child Part List  Now Part Child Part Nothing !!!
		        		if(! slPrevChildList.contains(strChildPartNumber)){
		        			
		        			
		        			//add
		        			Map changePartMap = new HashMap();
		        			changePartMap.put("EO_NUMBER", 				strEOName);
		        			changePartMap.put("EO_TYPE", 				strEOType);
		        			changePartMap.put("EAR", 					strEAR);
		        			changePartMap.put("BEFORE_PARENT_PART_ID", 	strChildPreviousPartParentPartNumber);
		        			changePartMap.put("BEFORE_PARENT_PART_REV", strChildPreviousPartParentPartRevision);
		        			changePartMap.put("BEFORE_CHILD_PART_ID", 	strChildPreviousPartNumber);
		        			changePartMap.put("BEFORE_CHILD_PART_REV", 	strChildPreviousPartRevision);
		        			changePartMap.put("BEFORE_CHILD_PART_NAME", strChildPreviousPartName);
		        			changePartMap.put("BEFORE_QUANTITY", 		strChildPreviousPartQuantity);
		        			changePartMap.put("BEFORE_MATERIAL", 		strChildPreviousPartMaterial);
		        			changePartMap.put("BEFORE_TREATMENT", 		strChildPreviousPartTreatment);
		        			changePartMap.put("BEFORE_VEHICLE", 		strChildPreviousPartVehicleBuffer.toString());
		        			changePartMap.put("AFTER_CHANGE_TYPE", 		"A");
		        			changePartMap.put("AFTER_PARENT_PART_ID", 	strPartObjectName);
		        			changePartMap.put("AFTER_PARENT_PART_REV", 	strPartObjectRevision);
		        			changePartMap.put("AFTER_CHILD_PART_ID", 	strChildPartNumber);
		        			changePartMap.put("AFTER_CHILD_PART_REV", 	strChildPartRevision);
		        			changePartMap.put("AFTER_CHILD_PART_NAME", 	strChildPartName);
		        			changePartMap.put("AFTER_QUANTITY", 		strPartQuantity);
		        			changePartMap.put("AFTER_MATERIAL", 		strPartMaterial);
		        			changePartMap.put("AFTER_TREATMENT", 		strPartSurface);
		        			changePartMap.put("AFTER_VEHICLE", 			strPartVehicleBuffer.toString());
		        			changePartMap.put("EXPORT_DATETIME", 		dDate);
		        			changePartMap.put("INTERFACE_DATETIME", 	"");
		        			changePartMap.put("CLASSIFY", 				"N");
		        			
		        			System.out.println("[ADD] ChangePart PLM Insert Interface");
		        			System.out.println("changePartMap       "+changePartMap);
					        sqlSession.update("MERGE_CHANGE_PART_REQUEST_REPEAT", changePartMap);
					        
						    
		        			
		        		}else{
		        			
		        			String strPrevPartQuantity  = (String)quantityMap.get(strChildPartNumber);
		        			if(! strPartQuantity.equals(strPrevPartQuantity)){
		        				
		        				
		        				Map viewPartMap = new HashMap();
						        viewPartMap.put("PARTNUMBER", strChildPartNumber);
						        viewPartMap.put("REV", strChildPartRevision);
					        	String strViewECONumber = StringUtils.trimToEmpty((String)sqlSession.selectOne("VW_PART_ECONO", viewPartMap));
						        System.out.println("REQUEST   REPEAT   VIEW   ECO   NUMBER   :   " + strViewECONumber);
					        	
						        if(! "".equals(strViewECONumber)){
						        	
						        	//mod
			        				Map changePartMap = new HashMap();
				        			changePartMap.put("EO_NUMBER", 				strEOName);
				        			changePartMap.put("EO_TYPE", 				strEOType);
				        			changePartMap.put("EAR", 					strEAR);
				        			changePartMap.put("BEFORE_PARENT_PART_ID", 	strChildPreviousPartParentPartNumber);              
				        			changePartMap.put("BEFORE_PARENT_PART_REV", strChildPreviousPartParentPartRevision);            
				        			changePartMap.put("BEFORE_CHILD_PART_ID", 	strChildPreviousPartNumber);                
				        			changePartMap.put("BEFORE_CHILD_PART_REV", 	strChildPreviousPartRevision);            
				        			changePartMap.put("BEFORE_CHILD_PART_NAME", strChildPreviousPartName);            
				        			changePartMap.put("BEFORE_QUANTITY", 		strChildPreviousPartQuantity);           
				        			changePartMap.put("BEFORE_MATERIAL", 		strChildPreviousPartMaterial);    
				        			changePartMap.put("BEFORE_TREATMENT", 		strChildPreviousPartTreatment);  
				        			changePartMap.put("BEFORE_VEHICLE", 		strChildPreviousPartVehicleBuffer.toString());  
				        			changePartMap.put("AFTER_CHANGE_TYPE", 		"C");
				        			changePartMap.put("AFTER_PARENT_PART_ID", 	strPartObjectName);
				        			changePartMap.put("AFTER_PARENT_PART_REV", 	strPartObjectRevision);
				        			changePartMap.put("AFTER_CHILD_PART_ID", 	strChildPartNumber);
				        			changePartMap.put("AFTER_CHILD_PART_REV", 	strChildPartRevision);
				        			changePartMap.put("AFTER_CHILD_PART_NAME", 	strChildPartName);
				        			changePartMap.put("AFTER_QUANTITY", 		strPartQuantity);
				        			changePartMap.put("AFTER_MATERIAL", 		strPartMaterial);
				        			changePartMap.put("AFTER_TREATMENT", 		strPartSurface);
				        			changePartMap.put("AFTER_VEHICLE", 			strPartVehicleBuffer.toString());
				        			changePartMap.put("EXPORT_DATETIME", 		dDate);
				        			changePartMap.put("INTERFACE_DATETIME", 	"");
				        			changePartMap.put("CLASSIFY", 				"N");
			        				
				        			System.out.println("[MOD] ChangePart PLM Insert Interface");
				        			System.out.println("changePartMap       "+changePartMap);
							        sqlSession.update("MERGE_CHANGE_PART_REQUEST_REPEAT", changePartMap);
						        	
						        }
						        
		        			}
		        			
		        		}
		        		
		        	}
			        
			        
			        System.out.println("#################PartNumberChildList       "+slPartNumberChildList);
			        System.out.println("#################previousPartViewList      "+previousPartViewList);
			        
			        
			        for(int h=0; h<previousPartViewList.size(); h++){
			        	
			        	Map sendBOMMap = (Map)previousPartViewList.get(h);
			        	String strAfterPartNumber   		= (String)sendBOMMap.get("PARENTNUMBER");
				        String strAfterPartRevision 		= (String)sendBOMMap.get("PARENTREV");
				        String strAfterChildPartNumber   	= (String)sendBOMMap.get("CHILDNUMBER");
				        String strAfterChildPartRevision 	= (String)sendBOMMap.get("CHILDREV");
				        String strAfterPartQuantity 		= StringUtils.trimToEmpty(String.valueOf(sendBOMMap.get("QUANTITY")));
				        String strAfterChildPart = strAfterChildPartNumber;  
				    	
			        	//////////////////////////////////////////// Now AffectedItem List VW_BOM_TABLE Child Part Nothing
				        if(! slPartNumberChildList.contains(strAfterChildPart)){
			        		
				        		
				        	String strAfterParentPartObjectId = DomainConstants.EMPTY_STRING;
				        	String strTmpMqlValue = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "temp query bus $1 $2 $3 select $4 dump $5", new String[] {cdmConstantsUtil.TYPE_CDMPART, strAfterPartNumber, strAfterPartRevision, "id", "|"}));    
				        	if(! DomainConstants.EMPTY_STRING.equals(strTmpMqlValue)){
				        		String[] strTmpMqlValueArray = strTmpMqlValue.split("\\|");
				        		strAfterParentPartObjectId = strTmpMqlValueArray[3];
				        	}
				        	
				        	
				        	DomainObject afterParentPartObj = new DomainObject(strAfterParentPartObjectId);
				        	String strBeforeParentPartObjectId = StringUtils.trimToEmpty(afterParentPartObj.getInfo(context, "previous.id"));
				        	String strAfterParentPartEOName   = StringUtils.trimToEmpty(afterParentPartObj.getInfo(context, "to["+DomainConstants.RELATIONSHIP_AFFECTED_ITEM+"].from.name"));
				        	
				        		
				        	String strAfterChildPartObjectId = DomainConstants.EMPTY_STRING;
				        	String strMqlValue = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "temp query bus $1 $2 $3 select $4 dump $5", new String[] {cdmConstantsUtil.TYPE_CDMPART, strAfterChildPartNumber, strAfterChildPartRevision, "id", "|"}));    
				        	if(! DomainConstants.EMPTY_STRING.equals(strMqlValue)){
				        		String[] strMqlValueArray = strMqlValue.split("\\|");
				        		strAfterChildPartObjectId = strMqlValueArray[3];
				        	}
				        		
				        		
				        	DomainObject afterChildPartObj = new DomainObject(strAfterChildPartObjectId); 
				        	SelectList slAfterChildPartBusList = new SelectList();
				        	slAfterChildPartBusList.add("previous.id");
				        	slAfterChildPartBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_NAME);
				        	slAfterChildPartBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PLM_OBJECTID);
				        	
				        	Map afterChildDataMap = afterChildPartObj.getInfo(context, slAfterChildPartBusList);
				        		
				        	String strBeforeChildPartObjectId 	= StringUtils.trimToEmpty( (String)afterChildDataMap.get("previous.id") );
				        	String strAfterChildPartItemName    = (String)afterChildDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_NAME);
				        	String strAfterChildPartPLMObjectId = (String)afterChildDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PLM_OBJECTID);
				        	    
				        		
				        	String strAfterChildPartMaterial    = DomainConstants.EMPTY_STRING;
				        	String strAfterChildPartSurface     = DomainConstants.EMPTY_STRING;
				        	if(! DomainConstants.EMPTY_STRING.equals(strAfterChildPartPLMObjectId)){
				        	    	
				        	    String[] strPrevPartPLMObjectIdArray = strAfterChildPartPLMObjectId.split("\\|");
				        	    strAfterChildPartMaterial = strPrevPartPLMObjectIdArray[0];
				        	    strAfterChildPartSurface  = strPrevPartPLMObjectIdArray[1];
				        	        
				        	}
				        	        
				        	StringList slAfterVehicleRelIdList = afterChildPartObj.getInfoList(context, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_VEHICLE+"].from."+displayTitle);
				        	StringBuffer strAfterVehicleBuffer = new StringBuffer();
				        	if(slAfterVehicleRelIdList.size()>0){
				        	    for (int k=0; k<slAfterVehicleRelIdList.size(); k++) {
				        	    	strAfterVehicleBuffer.append(slAfterVehicleRelIdList.get(k));
				        	    	if(slAfterVehicleRelIdList.size()-1 != k){
				        	    		strAfterVehicleBuffer.append(";");	
				        	    	}
				        	    }	
				        	}
				        	
				        	
				        	
				        	String strBeforeChildPartNumber 	= "";
				        	String strBeforeChildPartRevision 	= "";
				        	String strBeforeChildPartName 		= "";
				        	String strBeforeChildPartMaterial 	= "";
				        	String strBeforeChildPartSurface 	= "";
				        	String strBeforePartQuantity 		= "";
				        	StringBuffer strBeforeVehicleBuffer = new StringBuffer();

				        	String strBeforeParentPartNumber 	= "";
				        	String strBeforeParentPartRevision 	= "";
				        	
				        	
				        	
				        	if(! "".equals(strBeforeParentPartObjectId)){
				        		
				        		DomainObject beforeParentPartObj = new DomainObject(strBeforeParentPartObjectId);
				        		strBeforeParentPartNumber 	= beforeParentPartObj.getInfo(context, "name");
				        		strBeforeParentPartRevision = beforeParentPartObj.getInfo(context, "revision");
				        		
				        		if(! "".equals(strBeforeChildPartObjectId) ){
				        			strBeforePartQuantity = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] {strBeforeParentPartObjectId, "from["+DomainConstants.RELATIONSHIP_EBOM+"|to.id=='"+strBeforeChildPartObjectId+"']."+DomainConstants.SELECT_ATTRIBUTE_QUANTITY}));
				        			if(strBeforePartQuantity.indexOf(".") > -1){
				        				strBeforePartQuantity = strBeforePartQuantity.substring(0, strBeforePartQuantity.indexOf("."));
							        }
				        		}
				        		
				        	}
				        	
				        	
				        	if(! "".equals(strBeforeChildPartObjectId) ){
				        		
				        		DomainObject beforChildObj = new DomainObject(strBeforeChildPartObjectId);	
				        	
				        		SelectList slBeforeChildPartBusList = new SelectList();
				        		slBeforeChildPartBusList.add(DomainConstants.SELECT_NAME);
				        		slBeforeChildPartBusList.add(DomainConstants.SELECT_REVISION);
					        	slBeforeChildPartBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_NAME);
					        	slBeforeChildPartBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PLM_OBJECTID);
					        	
					        	Map beforeChildDataMap = beforChildObj.getInfo(context, slBeforeChildPartBusList);
					        	
					        	
					        	strBeforeChildPartNumber    = (String)beforeChildDataMap.get(DomainConstants.SELECT_NAME);
					        	strBeforeChildPartRevision  = (String)beforeChildDataMap.get(DomainConstants.SELECT_REVISION);
					        	strBeforeChildPartName      = (String)beforeChildDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_NAME);
					        	String strBeforeChildPartPLMObjectId = (String)beforeChildDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PLM_OBJECTID);
					        	    
					        	
					        	if(! DomainConstants.EMPTY_STRING.equals(strBeforeChildPartPLMObjectId)){
				        	    	
					        	    String[] strAfterChildPartPLMObjectIdArray = strAfterChildPartPLMObjectId.split("\\|");
					        	    strBeforeChildPartMaterial = strAfterChildPartPLMObjectIdArray[0];
					        	    strBeforeChildPartSurface  = strAfterChildPartPLMObjectIdArray[1];
					        	        
					        	}
					        	
					        	StringList slBeforeVehicleRelIdList = beforChildObj.getInfoList(context, "to["+cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_VEHICLE+"].from."+displayTitle);
					        	if(slBeforeVehicleRelIdList.size()>0){
					        	    for (int k=0; k<slBeforeVehicleRelIdList.size(); k++) {
					        	    	strBeforeVehicleBuffer.append(slBeforeVehicleRelIdList.get(k));
					        	    	if(slBeforeVehicleRelIdList.size()-1 != k){
					        	    		strBeforeVehicleBuffer.append(";");	
					        	    	}
					        	    }	
					        	}
					        	
				        	}
				        	 
				        	    
				        	System.out.println("!!!!!!!!!AfterParentPartEOName       "+strAfterParentPartEOName);
				        	
				        	Map viewPartMap = new HashMap();
					        viewPartMap.put("PARTNUMBER", strAfterChildPartNumber);
					        viewPartMap.put("REV", strAfterChildPartRevision);
				        	String strViewECONumber = StringUtils.trimToEmpty((String)sqlSession.selectOne("VW_PART_ECONO", viewPartMap));
					        System.out.println("CHANGE   VIEW   ECO   NUMBER   :   " + strViewECONumber);
					        
				        	
					        if(! "".equals(strViewECONumber)){
					        	
					        	//del
				        		Map changePartMap = new HashMap();
				        		changePartMap.put("EO_NUMBER", 					strEOName);
				        		changePartMap.put("EO_TYPE", 					strEOType);
				        		changePartMap.put("EAR", 						strEAR);
				        		changePartMap.put("BEFORE_PARENT_PART_ID", 		strBeforeParentPartNumber);
				        		changePartMap.put("BEFORE_PARENT_PART_REV", 	strBeforeParentPartRevision);
				        		changePartMap.put("BEFORE_CHILD_PART_ID", 		strBeforeChildPartNumber);
				        		changePartMap.put("BEFORE_CHILD_PART_REV", 		strBeforeChildPartRevision);
				        		changePartMap.put("BEFORE_CHILD_PART_NAME", 	strBeforeChildPartName);
				        		changePartMap.put("BEFORE_QUANTITY", 			strBeforePartQuantity);
				        		changePartMap.put("BEFORE_MATERIAL", 			strBeforeChildPartMaterial);
				        		changePartMap.put("BEFORE_TREATMENT", 			strBeforeChildPartSurface);
				        		changePartMap.put("BEFORE_VEHICLE", 			strBeforeVehicleBuffer.toString());
				        		changePartMap.put("AFTER_CHANGE_TYPE", 			"D");
				        		changePartMap.put("AFTER_PARENT_PART_ID", 		strPartObjectName);//strAfterPartNumber
				        		changePartMap.put("AFTER_PARENT_PART_REV", 		strPartObjectRevision);//strAfterPartRevision
				        		changePartMap.put("AFTER_CHILD_PART_ID", 		strAfterChildPartNumber);
				        		changePartMap.put("AFTER_CHILD_PART_REV", 		strAfterChildPartRevision);
				        		changePartMap.put("AFTER_CHILD_PART_NAME", 		strAfterChildPartItemName);
				        		changePartMap.put("AFTER_QUANTITY", 			strAfterPartQuantity);
				        		changePartMap.put("AFTER_MATERIAL", 			strAfterChildPartMaterial);
				        		changePartMap.put("AFTER_TREATMENT", 			strAfterChildPartSurface);
				        		changePartMap.put("AFTER_VEHICLE", 				strAfterVehicleBuffer.toString());
				        		changePartMap.put("EXPORT_DATETIME", 			dDate);
				        		changePartMap.put("INTERFACE_DATETIME", 		"");
				        		changePartMap.put("CLASSIFY", 					"N");
				        			
				        		System.out.println("REVISE   [DEL] ChangePart PLM Insert Interface");
				        		System.out.println("REVISE   [DEL] ChangePartMap       "+changePartMap);
						        sqlSession.update("MERGE_CHANGE_PART_REQUEST_REPEAT", changePartMap);
					        	
					        }
						        
			        	}
			        	
			        }
			        
		        }
		        //////////////////////////////////////////PreviousPart VW_BOM List No Nothing  [Revise] ChangePart  End !!!
		        
		        
	        }
        
        }catch(Exception e){
        	e.printStackTrace();
        	throw e;
        }
        
	}

	public static String convertDateFormat(String date, String format1, String format2) throws Exception {
		if (UIUtil.isNullOrEmpty(date))
			return date;

		String strDate = date;
		try {
			SimpleDateFormat sd = new SimpleDateFormat(format1, Locale.US);
			Date d = sd.parse(date);
			sd.applyPattern(format2);
			strDate = sd.format(d);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return strDate;
	}
	

	
	/**
	 * Sends drawings(Auto CAD, NX Drawing, CAT Drawing with CGM and Auto CAD generated) which Affected Item has. 
	 * @param context
	 * @param args
	 * @throws Exception
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void sendDrawingFilesToPLM(Context context, String[] args) throws Exception {
		try {
			String strECObjectId = args[0];
			System.out.println("strECObjectId      " + strECObjectId);

			SelectList selectStmts = new SelectList();
			selectStmts.add(DomainObject.SELECT_ID);
			selectStmts.add(DomainObject.SELECT_TYPE);
			selectStmts.add(DomainObject.SELECT_NAME);
			selectStmts.add(DomainObject.SELECT_REVISION);
			selectStmts.add("attribute[cdmPartDrawingNo]");

			SelectList selectRel = new SelectList();
			Pattern typeDRWPattern = new Pattern("CATDrawing");
			typeDRWPattern.addPattern("cdmNXDrawing");
			typeDRWPattern.addPattern("cdmAutoCAD");
			typeDRWPattern.addPattern("cdmSTEP");
			typeDRWPattern.addPattern("cdmOrCADProduct");


			DomainObject ecDomObj = new DomainObject(strECObjectId);
			String strECONumber = ecDomObj.getInfo(context, DomainConstants.SELECT_NAME);
			MapList mlPartList = ecDomObj.getRelatedObjects(context, 
															DomainConstants.RELATIONSHIP_AFFECTED_ITEM, // relationship
															cdmConstantsUtil.TYPE_CDMPART, // type
															selectStmts, // objects
															selectRel, // relationships
															false, // to
															true, // from
															(short) 1, // recurse
															null, // where
															null, // relationship where
															(short) 0); // limit
			

			MapList mlCATDrawings = new MapList();
			MapList mlNONCATDrawings = new MapList();

			
			///////////////////////////////////////////  EO AffectedItem related Drawing List  Start !!!
			for (int i = 0; i < mlPartList.size(); i++) {

				Map partMap = (Map) mlPartList.get(i);
				String strPartObjType 	= (String) partMap.get(DomainObject.SELECT_TYPE);
				String strPartObjId 	= (String) partMap.get(DomainObject.SELECT_ID);
				String strPartNo 		= (String) partMap.get(DomainObject.SELECT_NAME);
				String strPartRevision 	= (String) partMap.get(DomainObject.SELECT_REVISION);
				String strPartDrawingNo = StringUtils.trimToEmpty((String) partMap.get("attribute[cdmPartDrawingNo]"));

				DomainObject doPartObj = new DomainObject(strPartObjId);
				
				String strParentPartType = doPartObj.getInfo(context, "to[EBOM].from.type");
				
				MapList ml2DList = new MapList();
				
				
				// if parent part is Phantom type, drawing is be in Phantom Part.
				// multi part in a drawing.
				if(cdmConstantsUtil.TYPE_CDMPHANTOMPART.equalsIgnoreCase(strParentPartType)) {
					
				//////////////////////////////////////// Phantom Part Start !!!	
					String strPhantomPartId = doPartObj.getInfo(context, "to[EBOM].from.id");
					
					DomainObject toGetDrwPartObj = new DomainObject(strPhantomPartId);
					ml2DList = toGetDrwPartObj.getRelatedObjects(context, 
																		DomainConstants.RELATIONSHIP_PART_SPECIFICATION, // relationship
																		typeDRWPattern.getPattern(), // type
																		selectStmts, // objects
																		selectRel, // relationships
																		false, // to
																		true, // from
																		(short) 1, // recurse
																		null, // where
																		null, // relationship where
																		(short) 0); // limit
				//////////////////////////////////////// Phantom Part End !!!
					
				} else {
				//////////////////////////////////////// Series Parts Start !!!	
					
					if(! DomainConstants.EMPTY_STRING.equals(strPartDrawingNo) ){
						
						MapList mlSeriesParts = DomainObject.findObjects(context,
															strPartObjType, 
															cdmConstantsUtil.QUERY_WILDCARD,
															strPartRevision,
															"*",
															"*",
															"attribute[cdmPartDrawingNo] == '"+strPartDrawingNo+ "'",
															true,
															new StringList(DomainObject.SELECT_ID) );
						
						
						for (Iterator iterator = mlSeriesParts.iterator(); iterator.hasNext();) {
							Map objMap = (Map) iterator.next();
							
							String strToGetDrwPartObjId = (String) objMap.get(DomainObject.SELECT_ID);
							DomainObject toGetDrwPartObj  = new DomainObject(strToGetDrwPartObjId);
							
							ml2DList = toGetDrwPartObj.getRelatedObjects(context, 
																		DomainConstants.RELATIONSHIP_PART_SPECIFICATION, // relationship
																		typeDRWPattern.getPattern(), // type
																		selectStmts, // objects
																		selectRel, // relationships
																		false, // to
																		true, // from
																		(short) 1, // recurse
																		null, // where
																		null, // relationship where
																		(short) 0); // limit
							
							if (ml2DList.size() > 0) {
								break;
							}
							
						}
					
					}
					
				}
				//////////////////////////////////////// Series Parts End !!!	
				

				////////////////////////////////////////  2D Drawing Start !!!    
				for (Iterator iterator = ml2DList.iterator(); iterator.hasNext();) {
					Map obj2DMap = (Map) iterator.next();
					String strDRWType = (String) obj2DMap.get(DomainObject.SELECT_TYPE);
					
					obj2DMap.put("PartNo", strPartNo);
					obj2DMap.put("PartRevision", strPartRevision);
					
					// if type is CATDrawing
					if ("CATDrawing".equals(strDRWType)) {
						mlCATDrawings.add(obj2DMap);

					// if type is not CATDrawing
					} else if("cdmNXDrawing".equals(strDRWType) || "cdmAutoCAD".equals(strDRWType) || "cdmSTEP".equals(strDRWType) || "cdmOrCADProduct".equals(strDRWType)) {
						mlNONCATDrawings.add(obj2DMap);
					}
				}
				////////////////////////////////////////  2D Drawing End !!!  
				
			}
			////////////////////////////////////////   EO AffectedItem related Drawing List  End !!!
			System.out.println("CAT Drawings MapList     :     " + mlCATDrawings);

			
			
			
			String strWorkspace = context.createWorkspace() ;

			///////////////////////////  [CATDrawings] File FTP Setting  Start !!!
			// if Drawing Type is CAT Drawing, file format that it will be passed is CGM & Auto DWG where is Derived Output.
			for (int i = 0; i < mlCATDrawings.size(); i++) {

				Map obj2DMap = (Map)mlCATDrawings.get(i);
				String strDrawingId = (String) obj2DMap.get(DomainObject.SELECT_ID);
				
				
				String strPartNo = (String) obj2DMap.get("PartNo");
				String strPartRevision = (String) obj2DMap.get("PartRevision");

				
				
				String strDerivedOutput = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", new String[] { strDrawingId, "from[" + cdmConstantsUtil.RELATIONSHIP_DERIVED_OUTPUT + "|to.format=='CGM'].to.id" }));

				if (StringUtils.isEmpty(strDerivedOutput))
					continue;

				DomainObject outputObj = new DomainObject(strDerivedOutput);
				FileList outFileList = outputObj.getFiles(context);
				

				outputObj.checkoutFiles(context, false, "CGM", outFileList, strWorkspace);
				
				
				// change file name to PartNo_PartRevision like "BC100A1100_A.cgm"
				for (Iterator iterator = outFileList.iterator(); iterator.hasNext();) {
					matrix.db.File objectFile = (matrix.db.File) iterator.next();

					String strFileName = objectFile.getName();
					String strNewFileName = strPartNo + "_" + strPartRevision + "." + strFileName.replaceAll(".+\\.", "");

					java.io.File physicalFile = new java.io.File(strWorkspace + "/" + strFileName);
					if (physicalFile.exists())
						physicalFile.renameTo(new java.io.File(strWorkspace + "/" + strNewFileName));
					
				}
				
				
				try{
					FileList fileList = outputObj.getFiles(context, "DWG");
					
					if(fileList.size() != 0){
						outputObj.checkoutFiles(context, false, "DWG", outFileList, strWorkspace);
						String strZipFileName = strPartNo+"_"+strPartRevision+ ".zip";
						this.compressDwgFile(strWorkspace, strZipFileName, outFileList);
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			///////////////////////////  [CATDrawings] File FTP Setting  End !!!
			
			
			
			// loop for Non CATDrawings
			///////////////////////////  Non CATDrawings File FTP Setting  Start !!!
			for (int i = 0; i < mlNONCATDrawings.size(); i++) {
				
				Map obj2DMap = (Map)mlNONCATDrawings.get(i);
				String strDrawingId = (String) obj2DMap.get(DomainObject.SELECT_ID);
				String strDrawingType = (String) obj2DMap.get(DomainObject.SELECT_TYPE);
				
				
				String strPartNo = (String) obj2DMap.get("PartNo");
				String strPartRevision = (String) obj2DMap.get("PartRevision");
				
				
				DomainObject domDrawing = new DomainObject(strDrawingId);
				FileList fileList = domDrawing.getFiles(context);
				
				String strFormat = null;
				if ("cdmOrCADProduct".equals(strDrawingType)) {
					strFormat = "DSN";
				} else {
					strFormat = "generic";
				}

				domDrawing.checkoutFiles(context, false, strFormat, fileList, strWorkspace);
				
				// change file name to PartNo_PartRevision like "BC100A1100_A.dwg"
				for (Iterator iterator = fileList.iterator(); iterator.hasNext();) {
					matrix.db.File objectFile = (matrix.db.File) iterator.next();

					String strFileName = objectFile.getName();
					String strNewFileName = strPartNo + "_" + strPartRevision + "." + strFileName.replaceAll(".+\\.", "");
					
//					String extensionsName = strFileName.substring(strFileName.lastIndexOf(".") + 1);
//					extensionsName = extensionsName.toLowerCase();
//					
//					StringBuffer strDrawingBuffer = new StringBuffer();
//					strDrawingBuffer.append(strDrawingRelPartName);
//					strDrawingBuffer.append("_");
//					strDrawingBuffer.append(strDrawingRelPartRevision);
//					strDrawingBuffer.append(".");
//					strDrawingBuffer.append(extensionsName);
					

					java.io.File physicalFile = new java.io.File(strWorkspace + "/" + strFileName);
					if (physicalFile.exists())
						physicalFile.renameTo(new java.io.File(strWorkspace + "/" + strNewFileName));
				}
				
			}
			///////////////////////////  Non CATDrawings File FTP Setting  End !!!
			
			
			String strSiteName = ecDomObj.getInfo(context, "attribute[cdmSiteName]");
			if(StringUtils.isEmpty(strSiteName)) {
				strSiteName = "MDK";
			}
			
			String KEY_PLM_FTP_SERVER 		= strSiteName + "_" + "FTP_SERVER";
			String KEY_PLM_FTP_ID 			= strSiteName + "_" + "FTP_ID";
			String KEY_PLM_FTP_PASSWORD 	= strSiteName + "_" + "FTP_PASSWORD";
			String KEY_PLM_FTP_HOME 		= strSiteName + "_" + "FTP_HOME";
			
			String PLM_FTP_SERVER 			= cdmPropertiesUtil.getPropValue("FTP.properties", KEY_PLM_FTP_SERVER);
			String PLM_FTP_ID 				= cdmPropertiesUtil.getPropValue("FTP.properties", KEY_PLM_FTP_ID);
			String PLM_FTP_PASSWORD 		= cdmPropertiesUtil.getPropValue("FTP.properties", KEY_PLM_FTP_PASSWORD);
			String PLM_FTP_DERECTORY 		= cdmPropertiesUtil.getPropValue("FTP.properties", KEY_PLM_FTP_HOME);
			
			
			System.out.println("PLM_FTP_SERVER      " + PLM_FTP_SERVER);
			System.out.println("PLM_FTP_ID          " + PLM_FTP_ID);
			System.out.println("PLM_FTP_PASSWORD    " + PLM_FTP_PASSWORD);
			System.out.println("PLM_FTP_DERECTORY   " + PLM_FTP_DERECTORY);

			cdmFTPUtil.FTPFileUpload(PLM_FTP_SERVER, PLM_FTP_ID, PLM_FTP_PASSWORD, strWorkspace, PLM_FTP_DERECTORY);//"/FILE/PLM_File/"
			System.out.println("================================== FTP FILE UPLOAD ==================================");
			
			
			//context.deleteWorkspace();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		
	}
	
	
	/**
	 * 
	 * @param strWorkspace
	 * @param strZipFileName
	 * @param fileNames
	 * @throws Exception
	 */
	public void compressDwgFile( String strWorkspace, String strZipFileName, matrix.db.FileList fileNames)  throws Exception {

		String zipFile = strWorkspace + File.separator + strZipFileName;

		try {

			byte[] buffer = new byte[1024];
			FileOutputStream fos = new FileOutputStream(zipFile);
			ZipOutputStream zos = new ZipOutputStream(fos, Charset.forName("UTF-8"));
			
			
			for (Iterator iterator = fileNames.iterator(); iterator.hasNext();) {
				matrix.db.File file = (matrix.db.File) iterator.next();
				
				String strFormat = file.getFormat();
				String strFileName = file.getName();
				
				File realFile = new File(strWorkspace + File.separator + strFileName);
				
				if(!"DWG".equals(strFormat))
					continue;
				if(!realFile.exists())
					continue;

				FileInputStream fis = new FileInputStream(realFile);
				
				String strEntry = realFile.getName().replaceAll("^.+\\" + File.separator, "");
				
				ZipEntry ze = new ZipEntry(strEntry);
				
				zos.putNextEntry(ze);
				int length;

				while ((length = fis.read(buffer)) > 0) {
					zos.write(buffer, 0, length);
				}

				zos.flush();
				if (fis != null)
					fis.close();
				zos.closeEntry();
			
				realFile.delete();
			}
			zos.finish(); 
			
			fos.flush();
			if (zos != null)
				zos.close();
			if(fos != null)
				fos.close();

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	

	/**
	 * exports parts to PLM System EC state Trigger
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings({ "deprecation", "rawtypes" })
	public int setExportDemote(Context context, String[] args) throws Exception {
		String strECObjectId = args[0];
		try {
			ContextUtil.startTransaction(context, true);
			DomainObject domECObj = new DomainObject(strECObjectId);

			StringList objectSelects = new StringList();
			objectSelects.add(DomainConstants.SELECT_ID);

			MapList mlECObjectList = domECObj.getRelatedObjects(context, DomainConstants.RELATIONSHIP_AFFECTED_ITEM, // Relationship
					cdmConstantsUtil.TYPE_CDMPART, // From Type name
					objectSelects, // objects Select
					null, // Rel selects
					false, // to Direction
					true, // from Direction
					(short) 1, // recusion level
					"", "", 0);

			DomainObject domPartSpecObjectId = new DomainObject();
			DomainObject domAssociatedObjectId = new DomainObject();

			String strPartSpecificationObject = "";
			String strAssociatedObject = "";

			if (strECObjectId != null) {
				for (int i = 0; i < mlECObjectList.size(); i++) {
					String id = "";
					Map tempMap = (Map) mlECObjectList.get(i);
					id = (String) tempMap.get("id");
					DomainObject domItemsid = new DomainObject(id);

					String strPartSpecificationObjectId = MqlUtil.mqlCommand(context, "print bus '" + id + "' select from[" + DomainConstants.RELATIONSHIP_PART_SPECIFICATION + "].to.id dump | ");
					String strAssociatedObjectId = MqlUtil.mqlCommand(context, "print bus '" + id + "' select from.to.to[" + cdmConstantsUtil.RELATIONSHIP_ASSOCIATED_DRAWING + "].from.id dump |");

					StringList strPartSpecificationObjectIds = FrameworkUtil.split(strPartSpecificationObjectId, "|");
					StringList strAssociatedObjectIds = FrameworkUtil.split(strAssociatedObjectId, "|");

					if (!"".equals(strPartSpecificationObjectId)) {
						for (int j = 0; j < strPartSpecificationObjectIds.size(); j++) {
							strPartSpecificationObject = (String) strPartSpecificationObjectIds.get(j);
							domPartSpecObjectId = new DomainObject(strPartSpecificationObject);
							ContextUtil.pushContext(context, null, null, null);
							MqlUtil.mqlCommand(context, "trigger off", new String[] {});

							domPartSpecObjectId.demote(context);

							MqlUtil.mqlCommand(context, "trigger on", new String[] {});
							ContextUtil.popContext(context);
						}
					}

					if (!"".equals(strAssociatedObjectId)) {
						for (int k = 0; k < strAssociatedObjectIds.size(); k++) {
							strAssociatedObject = (String) strAssociatedObjectIds.get(k);
							domAssociatedObjectId = new DomainObject(strAssociatedObject);

							ContextUtil.pushContext(context, null, null, null);
							MqlUtil.mqlCommand(context, "trigger off", new String[] {});

							domAssociatedObjectId.demote(context);

							MqlUtil.mqlCommand(context, "trigger on", new String[] {});
							ContextUtil.popContext(context);
						}
					}

					domItemsid.demote(context);
				}
			}
			ContextUtil.commitTransaction(context);
		} catch (Exception e) {
			ContextUtil.abortTransaction(context);
			e.printStackTrace();
			throw e;
		}
		return 0;
	}

	/**
	 * Code Search for Create Object
	 */
	@SuppressWarnings("deprecation")
	@com.matrixone.apps.framework.ui.ProgramCallable
	public String setEARCodeField(Context context, String[] args) throws Exception {
		Date date = new Date();
		SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy-");
		String strYear = sdfYear.format(date);

		StringBuffer sbReturnString = new StringBuffer();

		int strNumber = Integer.parseInt(MqlUtil.mqlCommand(context, "print bus 'eService Number Generator' 'cdmECNumberGenerator' 'cdmEC' select attribute.value dump;"));

		String strName = "X-" + strYear + String.format("%05d", strNumber);

		sbReturnString.append("<input type=\"text\"  name=\"Code\" id=\"Code\" width=\"30\" readOnly=\"true\" value =\"" + strName + "\" >");
		sbReturnString.append("</input>");
		sbReturnString.append("<input type=\"checkbox\" name=\"btnRelatedECR\" value=\"Auto name\" checked=\"checked\" onclick=\"return false\" > Auto Name");
		sbReturnString.append("</input>");

		return sbReturnString.toString();

	}

	/**
	 * Code Search for Create Object
	 */
	@SuppressWarnings({ "deprecation" })
	@com.matrixone.apps.framework.ui.ProgramCallable
	public String setECOCodeField(Context context, String[] args) throws Exception {
		Date date = new Date();
		SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy-");
		String strYear = sdfYear.format(date);

		StringBuffer sbReturnString = new StringBuffer();

		int strNumber = Integer.parseInt(MqlUtil.mqlCommand(context, "print bus 'eService Number Generator' 'cdmECNumberGenerator' 'cdmEC' select attribute.value dump;"));

		String strName = "X-" + strYear + String.format("%05d", strNumber);

		sbReturnString.append("<input type=\"text\"  name=\"Code\" id=\"Code\" width=\"30\" readOnly=\"true\" value =\"" + strName + "\" >");
		sbReturnString.append("</input>");
		sbReturnString.append("<input type=\"checkbox\" name=\"btnRelatedECR\" value=\"Auto name\" checked=\"checked\" onclick=\"return false\" > Auto Name");
		sbReturnString.append("</input>");

		return sbReturnString.toString();
	}

	/**
	 * Code Search for Create Object
	 */
	@SuppressWarnings("deprecation")
	@com.matrixone.apps.framework.ui.ProgramCallable
	public String setPEOCodeField(Context context, String[] args) throws Exception {
		Date date = new Date();
		SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy-");
		String strYear = sdfYear.format(date);

		StringBuffer sbReturnString = new StringBuffer();

		int strNumber = Integer.parseInt(MqlUtil.mqlCommand(context, "print bus 'eService Number Generator' 'cdmECNumberGenerator' 'cdmEC' select attribute.value dump;"));

		String strName = "X-" + strYear + String.format("%05d", strNumber);

		sbReturnString.append("<input type=\"text\"  name=\"Code\" id=\"Code\" width=\"30\" readOnly=\"true\" value =\"" + strName + "\" >");
		sbReturnString.append("</input>");
		sbReturnString.append("<input type=\"checkbox\" name=\"btnRelatedECR\" value=\"Auto name\" checked=\"checked\" onclick=\"return false\" > Auto Name");
		sbReturnString.append("</input>");

		return sbReturnString.toString();

	}

	/**
	 * Project Search for CreateForm
	 */
	@SuppressWarnings("rawtypes")
	@com.matrixone.apps.framework.ui.ProgramCallable
	public String setProjectField(Context context, String[] args) throws Exception {

		String strProjectId = "";
		String projectName = cdmOwnerRolesUtil.getDefaultProject(context, context.getUser());
		String organizationName = cdmOwnerRolesUtil.getDefaultOrganization(context, context.getUser());
		
		projectName = projectName.replaceAll("^.+?_", "");
		projectName = projectName.replaceAll("\\(.+\\)", "");

		String strProject = DomainConstants.EMPTY_STRING;
		if (!"".equals(projectName) && !"".equals(organizationName)) {
			strProject = projectName + " - " + organizationName;
			Map projectsMap = cdmOwnerRolesUtil.getProjectValues(context, projectName, organizationName);
			strProjectId = StringUtils.trimToEmpty((String) projectsMap.get(DomainConstants.SELECT_ID));
		}

		if (DomainConstants.EMPTY_STRING.equals(strProjectId)) {
			strProject = DomainConstants.EMPTY_STRING;
		}

		StringBuffer sbReturnString = new StringBuffer();
		sbReturnString.append("<input type=\"text\"  name=\"Project\" id=\"Project\" value=\"" + strProject + "\" width=\"30\" readOnly=\"true\">");
		sbReturnString.append("</input>");
		sbReturnString
				.append("<input type=\"button\" name=\"btnRelatedECR\" value=\"...\" onclick=\"javascript:window.open('../common/cdmPartLibraryChooser.jsp?fieldName=Project&amp;callbackFunction=showVehicleFocus&amp;header=emxEngineeringCentral.header.ProjectGroup&amp;multiSelect=false&amp;ShowIcons=true&amp;searchMode=ProjectGroup&amp;program=cdmPartLibrary:getProjectGroupFirstNode&amp;expandProgram=cdmPartLibrary:expandProjectGroupLowLankNode&amp;isFromSearch=false&amp;isNeededOId=false&amp;");
		sbReturnString.append("StringResourceFileId=emxEngineeringCentralStringResource&amp;displayKey=name&amp;rootNode=ProjectGroup&amp;fieldNameActual=ProjectGroup&amp;fieldNameDisplay=ProjectGroupDisplay&amp;fromPage=ProjectGroupForm&amp;firstLevelSelect=false&amp;secondLevel=false&amp;processURL=../engineeringcentral/cdmProjectSearchProcess.jsp', '', 'width=400', 'height=300')\">");
		sbReturnString.append("</input>");
		sbReturnString.append("<input type=\"hidden\" name=\"ProjectOID\" value=\"" + strProjectId + "\">");
		sbReturnString.append("</input>");

		return sbReturnString.toString();

	}

	/**
	 * Project Search for EditForm
	 */
	@SuppressWarnings({ "rawtypes", "deprecation" })
	@com.matrixone.apps.framework.ui.ProgramCallable
	public String setEditProjectField(Context context, String[] args) throws Exception {
		Map paramMap = (Map) JPO.unpackArgs(args);
		Map requestMap = (Map) paramMap.get("requestMap");
		String objectId = (String) requestMap.get("objectId");

		String strProjectName = MqlUtil.mqlCommand(context, "print bus " + objectId + " select to[" + cdmConstantsUtil.RELATIONSHIP_CDM_PROJECT_RELATIONSHIP_EC + "].from.attribute[" + cdmConstantsUtil.ATTRIBUTE_CDM_PROJECT_CODE + "].value dump");

		String stringResource = (String) requestMap.get("StringResourceFileId");

		StringBuffer sbReturnString = new StringBuffer();

		sbReturnString.append("<input type=\"text\"  name=\"Project\" id=\"Project\" value=\"" + strProjectName + "\" width=\"30\" readOnly=\"true\">");
		sbReturnString.append("</input>");
		sbReturnString.append(
				"<input type=\"button\" name=\"btnRelatedECR\" value=\"...\" onclick=\"javascript:window.open('../common/cdmPartLibraryChooser.jsp?fieldName=ProjectOID&amp;callbackFunction=showVehicleFocus&amp;header=emxEngineeringCentral.header.ProjectGroup&amp;multiSelect=false&amp;ShowIcons=true&amp;searchMode=ProjectGroup&amp;program=cdmPartLibrary:getProjectGroupFirstNode&amp;expandProgram=cdmPartLibrary:expandProjectGroupLowLankNode&amp;isFromSearch=false&amp;isNeededOId=false&amp;");
		sbReturnString.append("StringResourceFileId=" + stringResource + "&amp;displayKey=name&amp;rootNode=ProjectGroup&amp;fieldNameActual=ProjectGroup&amp;fieldNameDisplay=ProjectGroupDisplay&amp;fromPage=ProjectGroupForm&amp;firstLevelSelect=false&amp;secondLevel=false&amp;processURL=../engineeringcentral/cdmProjectSearchEditProcess.jsp', '', 'width=400', 'height=300')\">");
		sbReturnString.append("</input>");
		sbReturnString.append("<input type=\"hidden\" name=\"ProjectOID\" value=\"\">");
		sbReturnString.append("</input>");

		return sbReturnString.toString();
	}

	/**
	 * @desc cdmECEdit Form Project Field Update.
	 */
	@SuppressWarnings({ "rawtypes" })
	public Object updateEditForm(Context context, String[] args) throws Exception {
		HashMap hmProgramMap = (HashMap) JPO.unpackArgs(args);
		HashMap hmParamMap = (HashMap) hmProgramMap.get("paramMap");
		String strObjectId = (String) hmParamMap.get("objectId");
		String strNewOid = (String) hmParamMap.get("New OID");

		DomainObject domObj = new DomainObject(strObjectId);

		String relProjectId = domObj.getInfo(context, "to[" + cdmConstantsUtil.RELATIONSHIP_CDM_PROJECT_RELATIONSHIP_EC + "].id");
		if (relProjectId != null && !"".equals(relProjectId) && !"".equals(strNewOid)) {
			DomainRelationship.disconnect(context, relProjectId);
		}

		try {
			if (!"".equals(strNewOid)) {
				DomainRelationship.connect(context, new DomainObject(strNewOid), cdmConstantsUtil.RELATIONSHIP_CDM_PROJECT_RELATIONSHIP_EC, new DomainObject(strObjectId));
			}
		} catch (Exception e) {
			throw e;
		}
		return Boolean.TRUE;
	}

	/**
	 * @desc ECPart Form ProductType Field Value
	 */
	@SuppressWarnings({ "deprecation", "rawtypes" })
	public String getProjectNameForm(Context context, String[] args) throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap paramMap = (HashMap) programMap.get("paramMap");
		String objectId = cdmStringUtil.setEmpty((String) paramMap.get("objectId"));
		cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());
		String strProjectName = DomainConstants.EMPTY_STRING;

		try {
			if (!DomainConstants.EMPTY_STRING.equals(objectId)) {
				strProjectName = MqlUtil.mqlCommand(context, "print bus " + objectId + " select to[" + cdmConstantsUtil.RELATIONSHIP_CDM_PROJECT_RELATIONSHIP_EC + "].from.attribute[" + cdmConstantsUtil.ATTRIBUTE_CDM_PROJECT_CODE + "].value dump");
			}
		} catch (Exception e) {
			throw e;
		}
		return strProjectName;
	}

	/**
	 * PLM Export Table Column for Number Setting
	 * 
	 * @return vector.
	 * @throws Exception
	 */

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Vector setTableNumber(Context context, String[] args) throws Exception {
		Vector columnValues = new Vector();
		try {
			HashMap programMap = (HashMap) JPO.unpackArgs(args);
			MapList mlObjectList = (MapList) programMap.get("objectList");
			mlObjectList.sort("rowNumber", "ascending", "string");
			int iObjSize = mlObjectList.size();
			for (int i = 0; i < iObjSize; i++) {
				if (i % 2 == 0) {
					columnValues.addElement(String.valueOf((i + 2) / 2));
				} else {
					columnValues.addElement("");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return columnValues;
	}

	/**
	 * CodeSearch Process for authority
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */

	@SuppressWarnings({ "static-access", "deprecation", "rawtypes" })
	@com.matrixone.apps.framework.ui.IncludeOIDProgramCallable
	public StringList slCheckUser(Context context, String[] args) throws Exception {
		String strObjectId = "";
		StringList objectList = new StringList();
		try {
			HashMap paramMap = (HashMap) JPO.unpackArgs(args);
			String strUserObjectId = (String) paramMap.get("objectId");

			StringList objectSelects = new StringList();
			objectSelects.add(DomainConstants.SELECT_ID);

			DomainObject domUserObj = new DomainObject(strUserObjectId);

			String strType = domUserObj.getType(context);
			String strUser = context.getUser();
			String strWhere = "owner == " + strUser;

			MapList mlUserCheck = domUserObj.findObjects(context, strType, "*", strWhere, objectSelects);

			for (int i = 0; i < mlUserCheck.size(); i++) {
				Map tempMap = (Map) mlUserCheck.get(i);
				strObjectId = (String) tempMap.get("id");

				objectList.add(strObjectId);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return objectList;
	}

	
	/**
	 * return result of checking validation for export
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public MapList exportValidate(Context context, String[] args) throws Exception {
		
		MapList mlResult = new MapList();
		try {
			HashMap paramMap = (HashMap) JPO.unpackArgs(args);
			HashMap paramListMap = (HashMap) paramMap.get("paramList");
			
			String strECObjectId = (String) paramMap.get("objectId");
			int messageLimit = -1;
			mlResult = this.exportValidate(context, strECObjectId, messageLimit);
		} catch (Exception e) {
			throw e;
		}
			
		return mlResult;
	}
		
		
		
	/**
	 * 
	 * @param context
	 * @param strECObjectId
	 * @param nMessageLimit if this less than 0 then unlimited.
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
	public MapList exportValidate(Context context, String strECObjectId, final int nMessageLimit) throws Exception {
		
		MapList mlResult = new MapList();
		SqlSession sqlSession = null;
		try {
		
			SqlSessionUtil.reNew("plm");
			sqlSession = SqlSessionUtil.getSqlSession();
			
			
			DomainObject ecDomObj = new DomainObject();
			MapList mlPartList = new MapList();
		
			
			SelectList selectStmts = new SelectList();
			selectStmts.add(DomainObject.SELECT_ID);
			selectStmts.add(DomainObject.SELECT_TYPE);
			selectStmts.add(DomainObject.SELECT_NAME);
			selectStmts.add(DomainObject.SELECT_REVISION);
			
			selectStmts.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION1);
			selectStmts.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION2);
			selectStmts.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION3);
			selectStmts.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION4);
			selectStmts.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION5);
			selectStmts.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION6);
			selectStmts.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION7);
			selectStmts.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION8);
			selectStmts.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_OPTION9);
			selectStmts.add("attribute[cdmPartOption10]");
			selectStmts.add("attribute[cdmPartOption11]");
			selectStmts.add("attribute[cdmPartOption12]");
			selectStmts.add("attribute[cdmPartOption13]");
			selectStmts.add("attribute[cdmPartOption14]");
			selectStmts.add("attribute[cdmPartOption15]");
			selectStmts.add("attribute[cdmPartOption16]");
			selectStmts.add("attribute[cdmPartOption17]");
			selectStmts.add("attribute[cdmPartOption18]");
			selectStmts.add("attribute[cdmPartOption19]");
			selectStmts.add("attribute[cdmPartOption20]");
			
			SelectList selectRel = new SelectList();

			
			ecDomObj = new DomainObject(strECObjectId);
			mlPartList = ecDomObj.getRelatedObjects(context, DomainConstants.RELATIONSHIP_AFFECTED_ITEM, // relationship
																						cdmConstantsUtil.TYPE_CDMPART, // type
																						selectStmts, // objects
																						selectRel, // relationships
																						false, // to
																						true, // from
																						(short) 1, // recurse
																						null, // where
																						null, // relationship where
																						(short) 0); // limit
			
			if(mlPartList.size() == 0) {
    			
    			String strMessage = "No affected item found.";
    			Map messageMap = new HashMap();
				messageMap.put("part", "-");
				messageMap.put("message", strMessage);
				mlResult.add(messageMap);
				
				return mlResult;
			}
			
			
			
			
			String strEOType = ecDomObj.getInfo(context, DomainObject.SELECT_TYPE);
			
			
			if(cdmConstantsUtil.TYPE_CDMECO.equals(strEOType)) {
			
				//check if all items are first release 
				int nFirstReleseItemCnt = 0;
				for (int i = 0; i < mlPartList.size(); i++) {
					
					Map partMap = (Map) mlPartList.get(i);
					
					String strPartNo = (String) partMap.get(DomainObject.SELECT_NAME);
					
					Map paramMap = new HashMap();
					paramMap.put("PARTNUMBER", strPartNo);
					String strViewPartNumber = StringUtils.trimToEmpty((String)sqlSession.selectOne("VW_PART_PARTNUMBER", paramMap));
			        
			        if(DomainConstants.EMPTY_STRING.equals(strViewPartNumber)){
			        	nFirstReleseItemCnt++;
			        }
				}
				
				if(mlPartList.size() == nFirstReleseItemCnt ) {
	    			
	    			String strMessage = "All items are first production release. In this case, the items should be exported by EAR. ";
	    			Map messageMap = new HashMap();
					messageMap.put("part", "-");
					messageMap.put("message", strMessage);
					mlResult.add(messageMap);
					
					return mlResult;
				}
			}
			
			
			StringList busSelect = new StringList();
			busSelect.add(DomainObject.SELECT_TYPE);
			busSelect.add(DomainObject.SELECT_NAME);
			busSelect.add(DomainObject.SELECT_REVISION);
			busSelect.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_NAME);
			busSelect.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_TYPE);
			busSelect.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_APPROVAL_TYPE);
			busSelect.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_ITEM_TYPE);
			busSelect.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PHASE);
			busSelect.add("to[" + cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_VEHICLE + "]");
			busSelect.add("previous.id");
			busSelect.add("previous.revision");
			busSelect.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO);
			busSelect.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_MATERIAL);
			busSelect.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_SURFACE_TREATMENT);
			
			StringList slPartList = (StringList)ecDomObj.getInfoList(context, "from[Affected Item].to.id");	

			for (int i = 0; i < mlPartList.size(); i++) {
				
				if (nMessageLimit > 0 && mlResult.size() >= nMessageLimit) {
					break;
				}
				
				Map partMap = (Map) mlPartList.get(i);
				
				String strPartId = (String) partMap.get(DomainObject.SELECT_ID);
				String strPartNo = (String) partMap.get(DomainObject.SELECT_NAME);
				String strPartRev = (String) partMap.get(DomainObject.SELECT_REVISION);
				String strPartObjectTtype = (String) partMap.get(DomainObject.SELECT_TYPE);
				
				
				// if part type is Electronic or Electronic Assy then validation check will be skipped.
				if ("cdmElectronicAssemblyPart".equals(strPartObjectTtype) || "cdmElectronicPart".equals(strPartObjectTtype))
					continue;
				
				
				
				// will be used when displaying validation message.
				String strPartNoRev = strPartNo + " " + strPartRev;
				
				DomainObject doPart = new DomainObject(strPartId);
				
				Map partObjMap = doPart.getInfo(context, busSelect);
				
				String strPartName 			= (String) partObjMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_NAME);
				String strPartType			= (String) partObjMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_TYPE);
				String strPartApprovalType 	= (String) partObjMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_APPROVAL_TYPE);
				String strPartItemType 		= (String) partObjMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_ITEM_TYPE);
				String strParentPartPhase 	= (String) partObjMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PHASE);
				String strHasVehicle 		= (String) partObjMap.get("to[" + cdmConstantsUtil.RELATIONSHIP_CDM_PART_RELATION_VEHICLE + "]");
				String strPrevPartId 		= (String) partObjMap.get("previous.id");
				String strPrevPartRevision  = (String) partObjMap.get("previous.revision");
				String strPartRevision 		= (String) partObjMap.get(DomainConstants.SELECT_REVISION);
				String strPartDrawingNo 	= StringUtils.trimToEmpty((String) partObjMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO));
				String strPartMaterial  	= StringUtils.trimToEmpty((String) partObjMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_MATERIAL));
				String strPartSurfaceTreatment = StringUtils.trimToEmpty((String) partObjMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_SURFACE_TREATMENT));
				
				
				//cdmPhantomPart
				//cdmElectronicAssemblyPart
				//cdmElectronicPart
				//cdmMechanicalPart
				
				
				
				//check if CDM BOM equals PLM BOM...
				if(StringUtils.isNotEmpty(strPrevPartId)) {
					
					Map paramMap1 = new HashMap();
					paramMap1.put("PARENTNUMBER", strPartNo);
					paramMap1.put("PARENTREV", strPrevPartRevision);
					List<Map<String, String>> bomList = sqlSession.selectList("get_bom_info_with_parent", paramMap1);
	
					int nPLM_BOMChildCnt = bomList.size();
	
					StringList slPLMChildPart = new StringList();
					for (int j = 0; j < bomList.size(); j++) {
						Map pMap = (Map) bomList.get(j);
						String strChildNo = (String) pMap.get("CHILDNUMBER");
						String strChildRev = (String) pMap.get("CHILDREV");
						slPLMChildPart.add(strChildNo);
					}
	
					StringList prevBusSelect = new StringList();
					prevBusSelect.add(DomainObject.SELECT_NAME);
					prevBusSelect.add(DomainObject.SELECT_REVISION);
					
					DomainObject doPrevPart = new DomainObject(strPrevPartId);
					MapList mlPrevBOM = doPrevPart.getRelatedObjects(context, 
															DomainConstants.RELATIONSHIP_EBOM, // relationship
															cdmConstantsUtil.TYPE_CDMPART,     // type
															prevBusSelect,     				   // objects
															new StringList(),  				   // relationships
															false,                             // to
															true,          					   // from
															(short)1,                          // recurse
															"revision == last",                // where
															null,                              // relationship where
															(short)0);                         // limit
													
					
					
					int nCDM_BOMChildCnt = mlPrevBOM.size();
					StringList slCDMChildPart = new StringList();
					
					for (int j = 0; j < mlPrevBOM.size(); j++) {
						Map pMap = (Map) mlPrevBOM.get(j);
						String strChildNo = (String) pMap.get(DomainObject.SELECT_NAME);
						String strChildRev = (String) pMap.get(DomainObject.SELECT_REVISION);
						
						slCDMChildPart.add(strChildNo);
					}
					
					boolean isSameBOM = nCDM_BOMChildCnt == nCDM_BOMChildCnt;
	
					if (isSameBOM) {
						isSameBOM = slCDMChildPart.containsAll(slPLMChildPart) && slPLMChildPart.containsAll(slCDMChildPart);
					}
					
					if(!isSameBOM) {
						String strMessage = "CDM BOM is different from PLM BOM."+ slCDMChildPart +"/" + slPLMChildPart;
						Map messageMap = new HashMap();
						messageMap.put("part", strPartNoRev);
						messageMap.put("message", strMessage);
						mlResult.add(messageMap);
					}
				
				
				}
				
				if("cdmMechanicalPart".equals(strPartObjectTtype)) {
					
					//check if Part Option is null.
					Map paramOptionNameMap = new HashMap();
					paramOptionNameMap.put("BLOCKCODE", strPartNo.substring(0, 5));
					
					List<Map<String, String>> optionDetailsList = sqlSession.selectList("getPartBlockCodeOptionsMap", paramOptionNameMap);
					
					int iOptionSize = optionDetailsList.size();
					
					for (int k = 0; k < iOptionSize; k++) {
						Map map = (Map) optionDetailsList.get(k);
						String strLabelRequired = (String) map.get("LABELREQUIRED");
						String strLabelName = (String) map.get("LABELNAME");

						if ("Y".equals(strLabelRequired)) {

							String strOptionValue = StringUtils.trimToEmpty((String) partMap.get("attribute[cdmPartOption" + String.valueOf(k + 1) + "]"));

							if (DomainConstants.EMPTY_STRING.equals(strOptionValue)) {

								String strMessage = "Option is required.";
								Map messageMap = new HashMap();
								messageMap.put("part", strPartNoRev);
								messageMap.put("message", strMessage);
								mlResult.add(messageMap);
								
								break;

							}
						}
					}
					
					
					//check if Part Material & TreatmentSurface is null.getBlockCodeMaterialNecessary
					Map blockCodeParameterMap = new HashMap(); 
			    	blockCodeParameterMap.put("BLOCKCODE", strPartNo.substring(0, 5));
			    	List<Map<String, String>> blockCodeMaterialNecessaryList = sqlSession.selectList("getBlockCodeMaterialNecessary", blockCodeParameterMap);
			    	
			    	if(blockCodeMaterialNecessaryList.size() > 0){
			    		
			    		Map blockCodeMaterialNecessaryMap = (Map)blockCodeMaterialNecessaryList.get(0);
				    	String strMaterialYN = StringUtils.trimToEmpty( (String) blockCodeMaterialNecessaryMap.get("MATERIALYN") );
				    	String strFinishYN   = StringUtils.trimToEmpty( (String) blockCodeMaterialNecessaryMap.get("FINISHYN") );
				    	
				    	boolean isNotMaterial = false;
				    	
				    	if( "Y".equals(strMaterialYN) ){
				    		
				    		if (DomainConstants.EMPTY_STRING.equals(strPartMaterial)) {

								String strMessage = "Material is required.";
								Map messageMap = new HashMap();
								messageMap.put("part", strPartNoRev);
								messageMap.put("message", strMessage);
								mlResult.add(messageMap);
								
								isNotMaterial = true;
									
								if ( DomainConstants.EMPTY_STRING.equals(strPartSurfaceTreatment) && "Y".equals(strFinishYN) ) {
									mlResult.remove(messageMap);
								}

							}
				    		
				    	}
				    	
				    	if( "Y".equals(strFinishYN) ){
				    		
				    		if (DomainConstants.EMPTY_STRING.equals(strPartSurfaceTreatment)) {

				    			String strMessage = "";
				    			if(isNotMaterial){
				    				strMessage = "Material & SurfaceTreatment are required.";
				    			}else{
				    				strMessage = "SurfaceTreatment is required.";	
				    			}
				    			
								Map messageMap = new HashMap();
								messageMap.put("part", strPartNoRev);
								messageMap.put("message", strMessage);
								mlResult.add(messageMap);

							}
				    		
				    	}
			    		
			    	}
			    	
			    	
				}
				
				
				
				////////////////////  TitleBlock & EO Type same parameterRevision Validation  Start !!!
				String strPartRelationDrawingId = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump ", new String[] {strPartId, "from["+DomainConstants.RELATIONSHIP_PART_SPECIFICATION+"|to.type=='CATDrawing'].to.id"});
				
				if(! DomainConstants.EMPTY_STRING.equals(strPartRelationDrawingId) && ! strPartRelationDrawingId.contains(",") ){
					
					DomainObject cadObj = new DomainObject(strPartRelationDrawingId);
					String strCadParameterNumber = StringUtils.trimToEmpty((String) cadObj.getInfo(context, "attribute[cdmDrawing_Parameter_Part_Number]"));
					String strCadParameterRevision = StringUtils.trimToEmpty((String) cadObj.getInfo(context, "attribute[cdmDrawing_Parameter_Part_Revision]"));
					
					if (DomainConstants.EMPTY_STRING.equals(strCadParameterNumber) || DomainConstants.EMPTY_STRING.equals(strCadParameterRevision)) {
						
						String strMessage = "TitleBlock is Empty.";
						Map messageMap = new HashMap();
						messageMap.put("part", strPartNoRev);
						messageMap.put("message", strMessage);
						mlResult.add(messageMap);
						
					} else if ( ! (strPartNo.equals(strCadParameterNumber) && strPartRev.equals(strCadParameterRevision)) ) {
						
						String strMessage = "TitleBlock Name is not same with Part No.";
						Map messageMap = new HashMap();
						messageMap.put("part", strPartNoRev);
						messageMap.put("message", strMessage);
						mlResult.add(messageMap);
						
					} else if ( (strPartNo.equals(strCadParameterNumber) && ! strPartRev.equals(strCadParameterRevision)) ) {
						
						String strMessage = "TitleBlock Revision is not same with Part Revision.";
						Map messageMap = new HashMap();
						messageMap.put("part", strPartNoRev);
						messageMap.put("message", strMessage);
						mlResult.add(messageMap);
						
					}
																			
					
					if(! DomainConstants.EMPTY_STRING.equals(strCadParameterRevision)){
						
						String strCadParameterRevisionFirstName = StringUtils.trimToEmpty(strCadParameterRevision).substring(0, 1);
						strCadParameterRevisionFirstName = strCadParameterRevisionFirstName.toUpperCase();
									
						if( "cdmPEO".equals(strEOType) ){
										
							char ch = strCadParameterRevisionFirstName.charAt(0);
							if ( ! (ch >= '0' && ch <= '9') ){
									
								String strMessage = "Wrong drawing on EO Type.";
								Map messageMap = new HashMap();
								messageMap.put("part", strPartNoRev);
								messageMap.put("message", strMessage);
								mlResult.add(messageMap);
									
							}
										
						}else if( "cdmEAR".equals(strEOType) || "cdmECO".equals(strEOType)){
										
							char ch = strCadParameterRevisionFirstName.charAt(0);
							if ( ! (ch >= 'A' && ch <= 'Z') ){
									
								String strMessage = "Wrong drawing on EO Type.";
								Map messageMap = new HashMap();
								messageMap.put("part", strPartNoRev);
								messageMap.put("message", strMessage);
								mlResult.add(messageMap);
									
							}
								
						}
						
					}	
					
				}
				////////////////////TitleBlock & EO Type same parameterRevision Validation  End !!!
				
				
				
				/////////////////////////////////////////  PhantomPart-OneDrawing  Validation  Start !!!	
//				StringList slParentPartTypeList = (StringList)doPart.getInfoList(context, "to[EBOM].from.type");
//					
//				if( slParentPartTypeList.contains(cdmConstantsUtil.TYPE_CDMPHANTOMPART) ){
//			    		
//			    	StringBuffer strBufferWhere = new StringBuffer();
//			    	strBufferWhere.append(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO);
//			    	strBufferWhere.append(" == '");
//			    	strBufferWhere.append(strPartDrawingNo);
//			    	strBufferWhere.append("'");
//			    	strBufferWhere.append(" && ");
//					strBufferWhere.append("revision");
//					strBufferWhere.append(" == ");
//					strBufferWhere.append("last.revision");
//					
//					SelectList selectList = new SelectList();
//					selectList.addId();
//			    		
//			    	MapList mlPhantomPartDrawingList = DomainObject.findObjects(context, 
//			    				cdmConstantsUtil.TYPE_CDMPHANTOMPART,         	// type
//			    				DomainConstants.QUERY_WILDCARD,   				// name
//			    				DomainConstants.QUERY_WILDCARD,   				// revision
//			    				DomainConstants.QUERY_WILDCARD,   				// policy
//			    				cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION,     // vault
//			    				strBufferWhere.toString(),             			// where
//			    				DomainConstants.EMPTY_STRING,     				// query
//			    				true,							  				// expand
//			    				selectList,                      				// objects
//			    				(short)0);                        				// limits
//			    		
//			    		String strPhantomObjectId = "";
//			    		
//			    		if(mlPhantomPartDrawingList.size() > 0){
//			    			Map phantomPartDrawingMap = (Map)mlPhantomPartDrawingList.get(0);
//			    			strPhantomObjectId = (String) phantomPartDrawingMap.get("id");
//			    		}
//			    		
//			    		if(! slPartList.contains(strPhantomObjectId)){
//			    				
//			    			String strMessage = "Revise with phantom part";
//			    			Map messageMap = new HashMap();
//							messageMap.put("part", strPartNoRev);
//							messageMap.put("message", strMessage);
//							mlResult.add(messageMap);
//							
//			    		}
//			    		
//			    	}	
//					
//				}
				/////////////////////////////////////////  PhantomPart-OneDrawing  Validation  End !!!	
				
				
				/////////////////////////////////////////  ManyPart-OneDrawing  Validation  Start !!!
//				StringList slParentPartTypeList = (StringList) doPart.getInfoList(context, "to[EBOM].from.type");
//				if(! "".equals(strPartDrawingNo) && ! slParentPartTypeList.contains(cdmConstantsUtil.TYPE_CDMPHANTOMPART) && "cdmMechanicalPart".equals(strPartObjectTtype) ){
//				
//					StringBuffer strBufferWhere = new StringBuffer();
//					strBufferWhere.append(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO);
//					strBufferWhere.append(" == '");
//					strBufferWhere.append(strPartDrawingNo);
//					strBufferWhere.append("' && ");
//					strBufferWhere.append(DomainConstants.SELECT_REVISION);
//					strBufferWhere.append(" == ");
//					strBufferWhere.append(strPartRevision);
//					strBufferWhere.append(" && ");
//					strBufferWhere.append("revision");
//					strBufferWhere.append(" == ");
//					strBufferWhere.append("last.revision");
//					
//					
//					MapList mlManyPartOneDrawingList = DomainObject.findObjects(context, 
//					cdmConstantsUtil.TYPE_CDMMECHANICALPART,        // type
//					DomainConstants.QUERY_WILDCARD,   				// name
//					DomainConstants.QUERY_WILDCARD,   				// revision
//					DomainConstants.QUERY_WILDCARD,   				// policy
//					cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION,     // vault
//					strBufferWhere.toString(),             			// where
//					DomainConstants.EMPTY_STRING,     				// query
//					true,							  				// expand
//					selectStmts,                      				// objects
//					(short)0);                        				// limits
//					
//					
//					if(mlManyPartOneDrawingList.size() > 1){
//					
//						int manyPartOneDrawingCnt = 0;
//						for(int h=0; h<mlManyPartOneDrawingList.size(); h++){
//						
//							Map manyPartOneDrawingMap = (Map)mlManyPartOneDrawingList.get(h);
//							String strManyPartOneDrawingPartName 	 = StringUtils.trimToEmpty((String)manyPartOneDrawingMap.get(DomainConstants.SELECT_NAME));
//							String strManyPartOneDrawingPartRevision = StringUtils.trimToEmpty((String)manyPartOneDrawingMap.get(DomainConstants.SELECT_REVISION));
//							String strManyPartOneDrawingName 		 = StringUtils.trimToEmpty((String)manyPartOneDrawingMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO));
//						
//							if(! "".equals(strManyPartOneDrawingName) && strManyPartOneDrawingPartName.equals(strManyPartOneDrawingName)){
//								manyPartOneDrawingCnt ++;
//							}
//					
//						}
//					
//						if(manyPartOneDrawingCnt == 0){
//							Map messageMap = new HashMap();
//							messageMap.put("part", strPartNoRev);
//							messageMap.put("message", "Not representation part");
//							mlResult.add(messageMap);
//						}
//					
//					}
//				
//				}
	    		/////////////////////////////////////////  ManyPart-OneDrawing  Validation  End !!!
				

				
				//check if Part Name is null.
				if(StringUtils.isEmpty(strPartName)) {
					String strMessage = "Part Name is required.";
					Map messageMap = new HashMap();
					messageMap.put("part", strPartNoRev);
					messageMap.put("message", strMessage);
					mlResult.add(messageMap);
				}
				
				
				//check if Part Type is null.
				if(StringUtils.isEmpty(strPartType)) {
					String strMessage = "Part Type is required.";
					Map messageMap = new HashMap();
					messageMap.put("part", strPartNoRev);
					messageMap.put("message", strMessage);
					mlResult.add(messageMap);
				}
				
				
				// check if part has vehicle.
				if("false".equalsIgnoreCase(strHasVehicle)) {
					String strMessage = "Vehicle is required.";
					Map messageMap = new HashMap();
					messageMap.put("part", strPartNoRev);
					messageMap.put("message", strMessage);
					mlResult.add(messageMap);
				}
				
				
				
				
				
				String strParentPartEoNo = ecDomObj.getInfo(context, DomainObject.SELECT_NAME);
				
				StringList objSelect = new StringList();
				objSelect.add(DomainConstants.SELECT_NAME);
				objSelect.add(DomainObject.SELECT_CURRENT);
				objSelect.add("to[Affected Item].from.name");
				objSelect.add(cdmConstantsUtil.ATTRIBUTE_CDM_PART_PHASE);

				// check if eco with child part is same.
				MapList mlEbomPart = doPart.getRelatedObjects(context, 
																DomainConstants.RELATIONSHIP_EBOM, // relationship
																cdmConstantsUtil.TYPE_CDMPART,     								// type
																objSelect,     				// objects
																new StringList(),  	// relationships
																false,                             // to
																true,          					   // from
																(short)1,                          // recurse
																"revision == last",                // where
																null,                              // relationship where
																(short)0);                         // limit
				
				for (Iterator iterator = mlEbomPart.iterator(); iterator.hasNext();) {
					Map objMap = (Map) iterator.next();
					
					String strChildPartNo 		= (String) objMap.get(DomainConstants.SELECT_NAME);
					String strChildPartCurrent 	= (String) objMap.get(DomainConstants.SELECT_CURRENT);
					String strChildPartPhase 	= (String) objMap.get(cdmConstantsUtil.ATTRIBUTE_CDM_PART_PHASE);
					String strChildEoNo 		= StringUtils.trimToEmpty( (String) objMap.get("to[Affected Item].from.name") ); 
					
					
					if("Production".equals(strParentPartPhase)) {
						
						if("Proto".equals(strChildPartPhase)) {
							String strMessage = "Child Part ["+strChildPartNo+"] needs to be Production Phase.";
							Map messageMap = new HashMap();
							messageMap.put("part", strPartNoRev);
							messageMap.put("message", strMessage);
							mlResult.add(messageMap);
						}
						
					}
					
					
					// check if child part eco is same with parent part eco.
					if("Preliminary".equals(strChildPartCurrent)) {
						if(!strChildEoNo.equals(strParentPartEoNo)) {
							String strMessage = "[" + strChildPartNo + "] Child Parts' ECO is not same with parent's one. \r\n Please release child's EO first.";
							Map messageMap = new HashMap();
							messageMap.put("part", strPartNoRev);
							messageMap.put("message", strMessage);
							mlResult.add(messageMap);
						}
					}
				}
			
				
				
				//check if Drawing is revised.
				if(StringUtils.isNotEmpty(strPrevPartId)) {

					DomainObject doPrevPart = new DomainObject(strPrevPartId);
					
					StringList selectList = new StringList();
					selectList.add(DomainObject.SELECT_CURRENT);
					
					MapList mlPrevPartDrawings = doPrevPart.getRelatedObjects(context, 
																DomainConstants.RELATIONSHIP_PART_SPECIFICATION, 	// relationship
																"cdmAutoCAD,cdmNXDrawing,CATDrawing", 	// type
																selectList, 							// objects
																null,			 						// relationships
																false, 		// to
																true, 		// from
																(short) 1, 	// recurse
																null, 		// where
																null, 		// relationship where
																(short) 0); // limit

					
					// if previous revision parts has drawings, 
					if (mlPrevPartDrawings.size() > 0 ) {
						
						MapList ml2DList = doPart.getRelatedObjects(context, 
																	DomainConstants.RELATIONSHIP_PART_SPECIFICATION, 	// relationship
																	"cdmAutoCAD,cdmNXDrawing,CATDrawing", 	// type
																	selectList, 							// objects
																	null,			 					// relationships
																	false, 		// to
																	true, 		// from
																	(short) 1, 	// recurse
																	"current == PRIVATE || current == IN_WORK", // where
																	null, 		// relationship where
																	(short) 0); // limit
							
																	
						if (ml2DList.size() == 0) {
							String strMessage = "Drawing needs to be revised.";
							Map messageMap = new HashMap();
							messageMap.put("part", strPartNoRev);
							messageMap.put("message", strMessage);
							mlResult.add(messageMap);
						}
						
					}
					
				}
				
				
			
				
				//check if Drawing is locked.
				StringList selectList = new StringList();
				selectList.add(DomainObject.SELECT_CURRENT);
				selectList.add("locked");
				selectList.add(DomainObject.SELECT_TYPE);
				selectList.add(DomainObject.SELECT_REVISION);
				
				MapList ml2DList = doPart.getRelatedObjects(context, DomainConstants.RELATIONSHIP_PART_SPECIFICATION, 	// relationship
																				"cdmAutoCAD,cdmNXDrawing,CATDrawing", 	// type
																				selectList, 							// objects
																				null,			 					// relationships
																				false, 		// to
																				true, 		// from
																				(short) 1, 	// recurse
																				null, 		// where
																				null, 		// relationship where
																				(short) 0); // limit

				
				for (Iterator iterator = ml2DList.iterator(); iterator.hasNext();) {
					
					Map obj2DMap = (Map) iterator.next();

					String strLocked = (String) obj2DMap.get("locked");
					String strDrawingType = (String) obj2DMap.get(DomainObject.SELECT_TYPE);
					String strDrawingRevision = (String) obj2DMap.get(DomainObject.SELECT_REVISION);

					if ("true".equalsIgnoreCase(strLocked)) {
						String strMessage = "Drawing is locked. Please check in drawing.";
						Map messageMap = new HashMap();
						messageMap.put("part", strPartNoRev);
						messageMap.put("message", strMessage);
						mlResult.add(messageMap);
					}
					
				}
				
				
				// check if phantom has drawings.
				if (cdmConstantsUtil.TYPE_CDMPHANTOMPART.equals(strPartObjectTtype)) {

					if (ml2DList.size() == 0) {
						String strMessage = "Phantom Part does not have drawings.";
						Map messageMap = new HashMap();
						messageMap.put("part", strPartNoRev);
						messageMap.put("message", strMessage);
						mlResult.add(messageMap);
					}

				}
				
				
//				// check if drawing(not include CATDrawing) is revised.
//				for (Iterator iterator = ml2DList.iterator(); iterator.hasNext();) {
//					Map obj2DMap = (Map) iterator.next();
//
//					String strDrwCurrent = (String) obj2DMap.get(DomainObject.SELECT_CURRENT);
//
//					if (!"PRIVATE".equals(strDrwCurrent)) {
//						countOfInWorkDrawing++;
//					}
//				}
				
				
				
				MapList ml3DModelList = doPart.getRelatedObjects(context, 
														DomainConstants.RELATIONSHIP_PART_SPECIFICATION, 	// relationship
														"CATProduct,CATPart", 	// type
														selectList, 							// objects
														null,			 					// relationships
														false, 		// to
														true, 		// from
														(short) 1, 	// recurse
														null, 		// where
														null, 		// relationship where
														(short) 0); // limit
				
				
				for (Iterator iterator = ml3DModelList.iterator(); iterator.hasNext();) {
					Map obj3DModelMap = (Map) iterator.next();

					String strLocked = (String) obj3DModelMap.get("locked");

					if ("true".equalsIgnoreCase(strLocked)) {
						String strMessage = "3D model is locked. Please check in 3D model.";
						Map messageMap = new HashMap();
						messageMap.put("part", strPartNoRev);
						messageMap.put("message", strMessage);
						mlResult.add(messageMap);
					}
				}
				
			}
			
			
			
			
			
			if(mlResult.size() == 0){
				Map messageMap = new HashMap();
				messageMap.put("part", "");
				messageMap.put("message", "Validation is OK.  Please click Submit Button.");
				mlResult.add(messageMap);
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			sqlSession.close();
		}
		
		return mlResult;
	}

	/**
	 * CodeSearch Process for authority
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings({ "static-access", "rawtypes" })
	@com.matrixone.apps.framework.ui.IncludeOIDProgramCallable
	public StringList isCheckUser(Context context, String[] args) throws Exception {
		String strObjectId = "";
		StringList objectList = new StringList();
		try {
			HashMap paramMap = (HashMap) JPO.unpackArgs(args);
			String strUserObjectId = (String) paramMap.get("objectId");

			StringList objectSelects = new StringList();
			objectSelects.add(DomainConstants.SELECT_ID);
			DomainObject domUserObj = new DomainObject(strUserObjectId);

			StringBuffer strTypeBuffer = new StringBuffer();
			strTypeBuffer.append(cdmConstantsUtil.TYPE_CDMECO).append(",");
			strTypeBuffer.append(cdmConstantsUtil.TYPE_CDMEAR).append(",");
			strTypeBuffer.append(cdmConstantsUtil.TYPE_CDMPEO);

			// String strType = domUserObj.getType(context);
			String strUser = context.getUser();
			String strWhere = "owner == " + strUser;

			MapList mlUserList = domUserObj.findObjects(context, strTypeBuffer.toString(), "*", strWhere, objectSelects);

			for (int i = 0; i < mlUserList.size(); i++) {
				Map tempMap = (Map) mlUserList.get(i);
				strObjectId = (String) tempMap.get("id");

				objectList.add(strObjectId);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return objectList;
	}

	/**
	 * @desc EC Category
	 */
	@SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
	public HashMap getECCategory(Context context, String[] args) throws Exception {
		HashMap rangeMap = new HashMap();
		try {
			StringList fieldRangeValues = new StringList();
			StringList fieldDisplayRangeValues = new StringList();
			AttributeType attrECCategory = new AttributeType(cdmConstantsUtil.ATTRIBUTE_CDM_EC_CATEGORY);
			attrECCategory.open(context);
			StringList attrECCategoryList = attrECCategory.getChoices(context);
			attrECCategory.close(context);

			int iSize = attrECCategoryList.size() - 1;
			for (int i = 0; i < iSize; i++) {
				fieldRangeValues.addElement(attrECCategoryList.get(i));
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
	 * set Released Date
	 * 
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings({ "rawtypes", "deprecation" })
	public String setModified(Context context, String[] args) throws Exception {
		String strDate = "";
		String strCurrent = "";
		try {
			HashMap programMap = (HashMap) JPO.unpackArgs(args);
			HashMap paramMap = (HashMap) programMap.get("paramMap");
			String objectId = (String) paramMap.get("objectId");

			DomainObject domObj = new DomainObject(objectId);

			strCurrent = domObj.getCurrentState(context).getName();

			if ("Release".equals(strCurrent)) {
				strDate = domObj.getInfo(context, DomainConstants.SELECT_MODIFIED);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return strDate;

	}

	/*
	 * Revise.
	 */
	@SuppressWarnings("rawtypes")
	public String changeToPart(Context context, String[] args) throws Exception {
		HashMap programMap 		= (HashMap) JPO.unpackArgs(args);
		String strPartIds 		= (String) programMap.get("PartId");
		String strECObjectId 	= (String) programMap.get("ECobjectId");
		String strParentOID 	= (String) programMap.get("parentOID");
		String strModeType  	= (String) programMap.get("modeType");
		
		boolean isEAR = false;
		if("EAR".equals(strModeType)){
			isEAR = true;
		}
		
		String strReturnParentReviseObjectId = "SUCCESS";
		
		////////////  strPartIds Not Exists  Start !!!
		if (!DomainConstants.EMPTY_STRING.equals(strPartIds)) {
			
			String[] strObjectIdArray = strPartIds.split(",");
			
			StringList slList = new StringList();
			Map map = new HashMap();
			Map drawingMap = new HashMap();
			
			////////////  Checkbox Check PartIds Length  For  Start !!!
			for (int i = 0; i < strObjectIdArray.length; i++) {
				
				String strPartObjectId = strObjectIdArray[i];
				
				////////////  Part ObjectId Not Exists  Start !!!
				if (!DomainConstants.EMPTY_STRING.equals(strPartObjectId)) {
					
					DomainObject partDomObj = new DomainObject(strPartObjectId); 
					String strPartDrawingNo = StringUtils.trimToEmpty( partDomObj.getAttributeValue(context, "cdmPartDrawingNo") );
					
					if(isEAR){
						
						try{
							ContextUtil.pushContext(context, null, null, null);
							MqlUtil.mqlCommand(context, "trigger off", new String[]{});
							
							DomainObject domPartObj = new DomainObject (strPartObjectId);
							domPartObj.setDescription(context,  "CHANGE_EAR");
							//MqlUtil.mqlCommand(context, "mod bus $1 cdmPartPhase $2 cdmDescription $3", new String[] {strPartObjectId, "Production", "CHANGE_EAR" } );
							
						}catch(Exception e){
							throw e;
						}finally{
							MqlUtil.mqlCommand(context, "trigger on", new String[]{});
							ContextUtil.popContext(context);
						}
						
					}
					
					String strParentId = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump $3", new String[] { strPartObjectId, "to[" + DomainConstants.RELATIONSHIP_EBOM + "].from.id", "|" }));
					System.out.println("----------------strParentId----------------          "+strParentId);
					
					String strFindNumber = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump $3", new String[] { strPartObjectId, "to["+DomainConstants.RELATIONSHIP_EBOM+"]."+DomainConstants.SELECT_ATTRIBUTE_FIND_NUMBER, "|" }));
					if(DomainConstants.EMPTY_STRING.equals(strFindNumber)){
						strFindNumber = "1";
					}
					
					if(! slList.contains(strPartObjectId)){
						map.put(strPartObjectId, strParentId);
					}
					
					////////////  (BOM Tree 0 Level ObjectId) Not Equals (Part Parent Ids) Start !!! 
					if (!strParentOID.equals(strParentId)) {
						
						////////////  ParentPart Size > 1  Start !!!
						if (strParentId.contains("|")) {
							
							String[] strParentObjectIdArray = strParentId.split("\\|");
							
							String strRevisePartId = DomainConstants.EMPTY_STRING;
							
							////////////  Revise StringList (Contains)  Start !!!  
							if(slList.contains(strPartObjectId)){
								
								for (int k = 0; k < strParentObjectIdArray.length; k++) {
									
									if(! DomainConstants.EMPTY_STRING.equals(strRevisePartId)){
										String strReviseId = ${CLASS:cdmPartLibrary}.revisePartParentStructure(context, strRevisePartId, strPartObjectId, strParentId);
										System.out.println("CREATE Revise ObjectId     #=     " + strReviseId);	
										
										if("EAR".equals(strModeType)){
											new DomainObject(strRevisePartId).setAttributeValue(context, "cdmPartPhase", "Production");
										}
										
										if(strParentOID.equals(strPartObjectId)){
											strReturnParentReviseObjectId = strReviseId;
										}
										
									}
									
								}
							
							////////////  Revise StringList (Contains)  End !!!
							}else{
							////////////  Revise StringList (Not Contains)  Start !!!
								
								////////////  Parent Object Length For Start !!!  
								for (int k = 0; k < strParentObjectIdArray.length; k++) {
									
									//String strIsVersionObject = new DomainObject(strParentObjectIdArray[k]).getAttributeValue(context, DomainConstants.ATTRIBUTE_IS_VERSION_OBJECT);
									String strIsVersionObject = StringUtils.trimToEmpty( new DomainObject(strParentObjectIdArray[k]).getInfo(context, "next.revision") );
									
									////////////  Empty Next Revision  Start !!!   
									if ("".equals(strIsVersionObject)) {
											
										strParentId = strParentObjectIdArray[k];
											
										////////////  No Revise  Start !!!
										if(DomainConstants.EMPTY_STRING.equals(strRevisePartId)){
												
											strRevisePartId = ${CLASS:cdmPartLibrary}.revise(context, strPartObjectId, strParentId, strModeType);
											System.out.println("CREATE EO Revise Part ObjectId     =     " + strRevisePartId);
											${CLASS:cdmPartLibrary}.revisePartSetAttributeValueAndConnection(context, strPartObjectId, strECObjectId, strRevisePartId);
											
											
											if("EAR".equals(strModeType)){
												new DomainObject(strRevisePartId).setAttributeValue(context, "cdmPartPhase", "Production");
											}
											
											if(strParentOID.equals(strPartObjectId)){
												strReturnParentReviseObjectId = strRevisePartId;
											}
										
										////////////  No Revise  End !!!	
										}else{
								        ////////////  Yes Revise  Start !!!		
											
											strRevisePartId = ${CLASS:cdmPartLibrary}.revisePartParentStructure(context, strRevisePartId, strPartObjectId, strParentId);
											System.out.println("CREATE EO Revise Part ObjectId     ==     " + strRevisePartId);
												
											
											if("EAR".equals(strModeType)){
												new DomainObject(strRevisePartId).setAttributeValue(context, "cdmPartPhase", "Production");
											}
											
											if(strParentOID.equals(strPartObjectId)){
												strReturnParentReviseObjectId = strRevisePartId;
											}
											
										}
										////////////  Yes Revise  End !!!
									
									}
									////////////  Empty Next Revision  End !!!
									
								}
								////////////  Parent Object Length For End !!!  
								
							}
							////////////  Revise StringList (Not Contains)  End !!!
							
						////////////  ParentPart Size > 1  End !!!	
						} else {
						////////////  ParentPart Size = 1  Start !!!
							
							////////////  (Revise Complete Process StringList) [Exists] PartObjectId  Start !!!    	
							if(slList.contains(strPartObjectId)){
								
								////////////  Revise Complete ObjectId Not Exists  Start !!!
								String strRevisePartId = StringUtils.trimToEmpty(new DomainObject(strPartObjectId).getInfo(context, "next.id"));
								
								if(! "".equals(strRevisePartId)){
									
									//String strParentId = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump $3", new String[] { strPartObjectId, "to[" + DomainConstants.RELATIONSHIP_EBOM + "].from.id", "|" }));
									String strParentIds = (String)map.get(strPartObjectId);
									
									////////////  ParentId [Not Euals] PartId  Start !!!
									if (! strParentId.equals(strParentIds)) {
										
										if (strParentIds.contains("|")) {
											
											String[] strParentObjectIdsArray = strParentIds.split("\\|");
											
											////////////  Parent Part Length  [For]  Start !!!
											for (int l = 0; l < strParentObjectIdsArray.length; l++) {
												
												String strParentObjectId = strParentObjectIdsArray[l];
												
												if(! DomainConstants.EMPTY_STRING.equals(strParentObjectId)){
													
													//String strIsVersionObject = new DomainObject(strParentObjectId).getAttributeValue(context, DomainConstants.ATTRIBUTE_IS_VERSION_OBJECT);
													String strIsVersionObject = StringUtils.trimToEmpty( new DomainObject(strParentObjectId).getInfo(context, "next.revision") );
													
													////////////  PreviousRevision (Revise Complete ReviseObject Not Empty) is Start  !!!
													if (! "".equals(strIsVersionObject)) {
														
														try{
															ContextUtil.pushContext(context, null, null, null);
															MqlUtil.mqlCommand(context, "trigger off", new String[]{});
															
															String strParentPartId = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump $3", new String[] { strPartObjectId, "to[" + DomainConstants.RELATIONSHIP_EBOM + "].from.id", "|" }));
															
															if(! DomainConstants.EMPTY_STRING.equals(strParentPartId) && ! strParentPartId.contains(strParentObjectId) ){
																
																DomainRelationship EBOMNewRel = DomainRelationship.connect(context, new DomainObject(strParentObjectId), DomainConstants.RELATIONSHIP_EBOM, new DomainObject(strPartObjectId));
																EBOMNewRel.setAttributeValue(context, DomainConstants.ATTRIBUTE_FIND_NUMBER, strFindNumber);
															}
															
														}catch(Exception e){
															throw e;
														}finally{
															MqlUtil.mqlCommand(context, "trigger on", new String[]{});
															ContextUtil.popContext(context);
														}
														
													}
													////////////  PreviousRevision  (Revise Complete ReviseObject Not Empty) is End  !!!
													
												}
												
											}
											////////////  Parent Part Length  [For]  End !!!
											
										}
										
							        ////////////  ParentId [Not Equals] PartId  End !!!	
									}else{
									////////////  ParentId [Equals] PartId  Start !!!
										
										strRevisePartId = ${CLASS:cdmPartLibrary}.revisePartParentStructure(context, strRevisePartId, strPartObjectId, strParentId);
										System.out.println("CREATE EO Revise Part ObjectId     :     " + strRevisePartId);
										
										if("EAR".equals(strModeType)){
											new DomainObject(strRevisePartId).setAttributeValue(context, "cdmPartPhase", "Production");
										}
										
										if(strParentOID.equals(strPartObjectId)){
											strReturnParentReviseObjectId = strRevisePartId;
										}
										
									}
									////////////  ParentId [Equals] PartId  End !!!
									
								}
								////////////  Revise Complete ObjectId Not Exists  End !!!
								
							////////////  (Revise Complete Process StringList) [Exists] PartObjectId  End !!!    		
							}else{
							////////////  (Revise Complete Process StringList) [Not] Exists PartObjectId  Start !!!    	
								
								if(! DomainConstants.EMPTY_STRING.equals(strParentId)){
								
									//String strIsVersionObject = new DomainObject(strParentId).getAttributeValue(context, "Is Version Object");
									String strIsVersionObject = StringUtils.trimToEmpty( new DomainObject(strParentId).getInfo(context, "next.id") );
									
									if ("".equals(strIsVersionObject)) {
										
										String strRevisePartId = ${CLASS:cdmPartLibrary}.revise(context, strPartObjectId, strParentId, strModeType);
										System.out.println("CREATE EO Revise Part ObjectId     ::     " + strRevisePartId);
										${CLASS:cdmPartLibrary}.revisePartSetAttributeValueAndConnection(context, strPartObjectId, strECObjectId, strRevisePartId);
										
										if("EAR".equals(strModeType)){
											new DomainObject(strRevisePartId).setAttributeValue(context, "cdmPartPhase", "Production");
										}
										
										if(strParentOID.equals(strPartObjectId)){
											strReturnParentReviseObjectId = strRevisePartId;
										}
										
									////////////  [Modify] 2017.02.14 Start 
									}else{
										
										String strRevisePartId = ${CLASS:cdmPartLibrary}.revise(context, strPartObjectId, strIsVersionObject, strModeType);
										System.out.println("CREATE EO Revise Part ObjectId     -     " + strRevisePartId);
										${CLASS:cdmPartLibrary}.revisePartSetAttributeValueAndConnection(context, strPartObjectId, strECObjectId, strRevisePartId);
										
										if("EAR".equals(strModeType)){
											new DomainObject(strRevisePartId).setAttributeValue(context, "cdmPartPhase", "Production");
										}
										
										if(strParentOID.equals(strPartObjectId)){
											strReturnParentReviseObjectId = strRevisePartId;
										}
										
									}
									////////////  [Modify] 2017.02.14 End
									
								}else{
									
									String strRevisePartId = ${CLASS:cdmPartLibrary}.revise(context, strPartObjectId, strParentId, strModeType);
									System.out.println("CREATE EO Revise Part ObjectId     .....     " + strRevisePartId);
									${CLASS:cdmPartLibrary}.revisePartSetAttributeValueAndConnection(context, strPartObjectId, strECObjectId, strRevisePartId);
									
									
									if("EAR".equals(strModeType)){
										new DomainObject(strRevisePartId).setAttributeValue(context, "cdmPartPhase", "Production");
									}
									
									if(strParentOID.equals(strPartObjectId)){
										strReturnParentReviseObjectId = strRevisePartId;
									}
									
								}
								
							}
							////////////  (Revise Complete Process StringList) [Not] Exists PartObjectId  End !!!    	
							
						}
						////////////  ParentPart Size = 1  End !!!
					
					////////////  (BOM Tree 0 Level ObjectId) Not Equals (Part Parent Ids) End !!!
					} else {
					////////////  (BOM Tree 0 Level ObjectId) Equals (Part Parent Ids) Start !!!	
						
						
						////////////  (Revise Complete Process StringList) Exists PartObjectId  Start !!!    
						if(slList.contains(strPartObjectId)){
							
							////////////  Revise Complete ObjectId Start !!!	
							String strRevisePartId = StringUtils.trimToEmpty(new DomainObject(strPartObjectId).getInfo(context, "next.id"));
							
							
							if(! "".equals(strRevisePartId)){
								strRevisePartId = ${CLASS:cdmPartLibrary}.revisePartParentStructure(context, strRevisePartId, strPartObjectId, strParentId);
								System.out.println("CREATE EO Revise Part ObjectId     ..     " + strRevisePartId);
							}
							////////////  Revise Complete ObjectId End !!!
							
							
							if("EAR".equals(strModeType)){
								new DomainObject(strRevisePartId).setAttributeValue(context, "cdmPartPhase", "Production");
							}
							
							
							if(strParentOID.equals(strPartObjectId)){
								strReturnParentReviseObjectId = strRevisePartId;
							}
							
							
						////////////  (Revise Complete Process StringList) Exists PartObjectId  End !!!	
						}else{
						////////////  (Revise Complete Process StringList) Not Exists PartObjectId  Start !!!
							
							////////////  Revise & ReviseStructure  Start !!!  
							String strRevisePartId = ${CLASS:cdmPartLibrary}.revise(context, strPartObjectId, strParentId, strModeType);
							System.out.println("CREATE EO Revise Part ObjectId     ...     " + strRevisePartId);
							${CLASS:cdmPartLibrary}.revisePartSetAttributeValueAndConnection(context, strPartObjectId, strECObjectId, strRevisePartId);
							////////////  Revise & ReviseStructure  End !!!
							
							////////////  Revise EOType is EAR cdmPartPhase Set Attribute  Start !!!
							if("EAR".equals(strModeType)){
								new DomainObject(strRevisePartId).setAttributeValue(context, "cdmPartPhase", "Production");
							}
							////////////  Revise EOType is EAR cdmPartPhase Set Attribute  End !!!
							
							////////////  BOM Tree One Level equals Check PartObjectId Start !!!
							if(strParentOID.equals(strPartObjectId)){
								strReturnParentReviseObjectId = strRevisePartId;
							}
							////////////  BOM Tree One Level equals Check PartObjectId End !!!
							
						}
						////////////  (Revise Complete Process StringList) Not Exists PartObjectId  End !!!
						
					}
					////////////  (BOM Tree 0 Level ObjectId) Equals (Part Parent Ids) End !!!	

					
					////////////  Checkbox Check Revise Complete ObjectId  Add  Start !!!
					slList.add(strPartObjectId); 
					////////////  Checkbox Check Revise Complete ObjectId  Add  End !!!
					
					if(! DomainConstants.EMPTY_STRING.equals(strPartDrawingNo)){
					
						System.out.println( "drawingMap     :     " + drawingMap );
						if(! drawingMap.containsKey(strPartDrawingNo)){
						
							////////////  SeriesPart Revise  Start !!!
							reviseSeriesPart(context, strPartObjectId, strECObjectId);
							////////////  SeriesPart Revise  End !!!
							System.out.println("----------------");
						}
					
						drawingMap.put(strPartDrawingNo, strPartDrawingNo);	
						
					}
					
				}
				////////////  Part ObjectId Not Exists  End !!!
				
			}
			////////////  strPartId Length  For  End !!!
			
		}
		////////////  strPartIds Not Exists  End !!!

		return strReturnParentReviseObjectId;
	}
	
	
	@SuppressWarnings("rawtypes")
	public void reviseSeriesPart (Context context, String strPartObjectId, String strECobjectId) throws Exception {

		StringList slBusList = new StringList(); 
		slBusList.add(DomainConstants.SELECT_TYPE);
		slBusList.add(DomainConstants.SELECT_NAME);
		slBusList.add(DomainConstants.SELECT_REVISION);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PHASE);
		slBusList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO);
		
		DomainObject domObj = new DomainObject(strPartObjectId);
		
		Map partObjectDataMap = domObj.getInfo(context, slBusList);
		
		
		String strPartType         = (String) partObjectDataMap.get(DomainConstants.SELECT_TYPE);
		String strPartName         = (String) partObjectDataMap.get(DomainConstants.SELECT_NAME);
		String strPartRevision     = (String) partObjectDataMap.get(DomainConstants.SELECT_REVISION);
		
		String strPartPhase      = (String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_PHASE);
		String strDrawingNo 	 = StringUtils.trimToEmpty((String) partObjectDataMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO));
		String strHighLankType   = StringUtils.trimToEmpty(MqlUtil.mqlCommand(context, "print bus $1 select $2 dump $3", new String[] {strPartObjectId, "to["+DomainConstants.RELATIONSHIP_EBOM+"].from.type", "|"}));
		
		String strPartDescription 	= domObj.getAttributeValue(context, cdmConstantsUtil.ATTRIBUTE_CDM_PART_DESCRIPTION);
		
		Part part = new Part(strPartObjectId);
		BusinessObject partLastRevObj = part.getLastRevision(context);
		String strNextRevision = partLastRevObj.getRevision();
		
//		String strNextRevision = partLastRevObj.getNextSequence(context);
		
										
//		if(strPartDescription.contains("CHANGE_EAR")){
//				
//			strNextRevision = "A";
//				
//		}else{
//				
//			//////// MechanicalPart Revision Sequence Z -> ZA
//			if( "Z".equals(strPartRevision) && "cdmMechanicalPart".equals(strPartType) ){
//				strNextRevision = "ZA";	
//			}else{
//			//////// MechanicalPart Revision Sequence Z -> AA	
//				BusinessObject partlastRevObj = part.getLastRevision(context);
//				strNextRevision = partlastRevObj.getNextSequence(context);
//			}
//			
//		}
		
		
		if( cdmConstantsUtil.TYPE_CDMMECHANICALPART.equals(strPartType) && ! strHighLankType.contains(cdmConstantsUtil.TYPE_CDMPHANTOMPART) ){
			
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
			    strBufferWhere.append(strPartRevision);
			    strBufferWhere.append(" && ");
			    strBufferWhere.append(DomainConstants.SELECT_ID);
			    strBufferWhere.append(" != ");
			    strBufferWhere.append(strPartObjectId);
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
			            		
				        		
				for(int i=0; i<mlSeriesPartList.size(); i++){
					
					Map map = (Map)mlSeriesPartList.get(i);
					String id = (String)map.get(DomainConstants.SELECT_ID);
					
					try{
						
						String strSeriesRevisePartId = ${CLASS:cdmPartLibrary}.revise(context, id, strNextRevision, strPartPhase, DomainConstants.EMPTY_STRING);
					    
						if(! "".equals(strECobjectId)){
					    	${CLASS:cdmPartLibrary}.revisePartSetAttributeValueAndConnection(context, id, strECobjectId, strSeriesRevisePartId);
					    }
					    
					}catch(Exception e){
						e.printStackTrace();
					}
					
				}
					        	
			}
			
		}
		
	}
	
	

	/*
	 * Check. CDM Only Type Check.
	 * 
	 */
	@SuppressWarnings({ "deprecation", "rawtypes" })
	public boolean isTypeCheck(Context context, String[] args) throws Exception {

		try {
			HashMap paramMap = (HashMap) JPO.unpackArgs(args);
			String objectId = (String) paramMap.get("objectId");
			DomainObject domObj = new DomainObject(objectId);
			String strType = domObj.getType(context);
			if (!cdmConstantsUtil.TYPE_CDMDCR.equals(strType)) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return false;
	}

	
	/**
	 * 
	 * @param context
	 * @param args
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	public void removePart(Context context, String[] args) throws Exception {
		
		String propertyFile = "/config/mybatis/config.properties";
		String plmDelPartUrl = "PLM_DEL_PART_URL";
		
		try {
			ContextUtil.startTransaction(context, true);
			
			java.util.List mList = (java.util.List) JPO.unpackArgs(args);
			DomainObject domObject;
			String strObjectId = "";

			ContextUtil.pushContext(context, null, null, null);
			MqlUtil.mqlCommand(context, "trigger off", new String[] {});

			StringList busSelect = new StringList();
			busSelect.add(DomainObject.SELECT_ID);
			busSelect.add(DomainObject.SELECT_CURRENT);

			StringList relSelect = new StringList();
			relSelect.add(EngineeringConstants.SELECT_ATTRIBUTE_QUANTITY);
			relSelect.add(EngineeringConstants.SELECT_ATTRIBUTE_FIND_NUMBER);
			relSelect.add(EngineeringConstants.SELECT_ATTRIBUTE_REFERENCE_DESIGNATOR);

			for (int i = 0; i < mList.size(); i++) {
				
				strObjectId = (String) mList.get(i);
				StringTokenizer st = new StringTokenizer(strObjectId, "|");
				
				domObject = new DomainObject(st.nextToken());

				//
				String strPreviousId = domObject.getInfo(context, "previous.id");

				// if a part is revised one.
				if (StringUtils.isNotEmpty(strPreviousId)) {

					MapList mlChildBom = domObject.getRelatedObjects(context, 
																		DomainConstants.RELATIONSHIP_EBOM, // Relationship
																		cdmConstantsUtil.TYPE_CDMPART, // From Type name
																		busSelect, // objects Select
																		relSelect, // Rel selects
																		false, // to Direction
																		true, 				// from Direction
																		(short) 1, 		// recusion level
																		"current == Preliminary", 
																		"", 
																		0);

					for (int j = 0; j < mlChildBom.size(); j++) {
						Map objChildMap = (Map) mlChildBom.get(j);

						String strChildId = (String) objChildMap.get(DomainObject.SELECT_ID);
						String strQty = (String) objChildMap.get(EngineeringConstants.SELECT_ATTRIBUTE_QUANTITY);
						String strFindNumber = (String) objChildMap.get(EngineeringConstants.SELECT_ATTRIBUTE_FIND_NUMBER);
						String strRefDes = (String) objChildMap.get(EngineeringConstants.SELECT_ATTRIBUTE_REFERENCE_DESIGNATOR);

						DomainRelationship doEbom = DomainRelationship.connect(context, new DomainObject(strPreviousId), DomainConstants.RELATIONSHIP_EBOM, new DomainObject(strChildId));
						doEbom.setAttributeValue(context, EngineeringConstants.ATTRIBUTE_QUANTITY, strQty);
						doEbom.setAttributeValue(context, EngineeringConstants.ATTRIBUTE_FIND_NUMBER, strFindNumber);
						doEbom.setAttributeValue(context, EngineeringConstants.ATTRIBUTE_REFERENCE_DESIGNATOR, strRefDes);
					}
					
					
					MapList mlWhereUsed = domObject.getRelatedObjects(context, 
																	DomainConstants.RELATIONSHIP_EBOM, 	// Relationship
																	cdmConstantsUtil.TYPE_CDMPART, 		// From Type name
																	busSelect, 	// objects Select
																	relSelect, 	// Rel selects
																	true, 		// to Direction
																	false, 		// from Direction
																	(short) 1, 	// recusion level
																	"current == Preliminary", 
																	"", 
																	0);
					
					
					for (int j = 0; j < mlWhereUsed.size(); j++) {
						Map objParentMap = (Map) mlWhereUsed.get(j);
						
						String strParentPartId = (String) objParentMap.get(DomainObject.SELECT_ID);
						String strQty = (String) objParentMap.get(EngineeringConstants.SELECT_ATTRIBUTE_QUANTITY);
						String strFindNumber = (String) objParentMap.get(EngineeringConstants.SELECT_ATTRIBUTE_FIND_NUMBER);
						String strRefDes = (String) objParentMap.get(EngineeringConstants.SELECT_ATTRIBUTE_REFERENCE_DESIGNATOR);
						
						DomainRelationship doEbom = DomainRelationship.connect(context, new DomainObject(strParentPartId), DomainConstants.RELATIONSHIP_EBOM, new DomainObject(strPreviousId));
						doEbom.setAttributeValue(context, EngineeringConstants.ATTRIBUTE_QUANTITY, strQty);
						doEbom.setAttributeValue(context, EngineeringConstants.ATTRIBUTE_FIND_NUMBER, strFindNumber);
						doEbom.setAttributeValue(context, EngineeringConstants.ATTRIBUTE_REFERENCE_DESIGNATOR, strRefDes);
					}
				}
				
				
				
				String strPartOwner    = domObject.getInfo(context, "owner");
				String strPartName     = domObject.getInfo(context, "name");
				String strPartRevision = domObject.getInfo(context, "revision");
				String strPartDrawingName = StringUtils.trimToEmpty( domObject.getInfo(context, "attribute[cdmPartDrawingNo]") );
//				
//				if(! "".equals(strPartDrawingName) && strPartName.equals(strPartDrawingName)){
//					
//					StringBuffer strBufferWhere = new StringBuffer();
//		    		strBufferWhere.append(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_DRAWING_NO);
//		    		strBufferWhere.append(" == '");
//		    		strBufferWhere.append(strPartDrawingName);
//		    		strBufferWhere.append("'");
//		    		strBufferWhere.append(" && ");
//		    		strBufferWhere.append(DomainConstants.SELECT_REVISION);
//		    		strBufferWhere.append(" == '");
//		    		strBufferWhere.append(strPartRevision);
//		    		strBufferWhere.append("'");
//		    		strBufferWhere.append(" && ");
//					strBufferWhere.append("revision");
//					strBufferWhere.append(" == ");
//					strBufferWhere.append("last.revision");
//		    		
//		    		
//		    		MapList mlManyPartOneDrawingList = DomainObject.findObjects(context, 
//	    					cdmConstantsUtil.TYPE_CDMMECHANICALPART,        // type
//	    					DomainConstants.QUERY_WILDCARD,   				// name
//	    					DomainConstants.QUERY_WILDCARD,   				// revision
//	    					DomainConstants.QUERY_WILDCARD,   				// policy
//	    					cdmConstantsUtil.VAULT_ESERVICE_PRODUCTION,     // vault
//	    					strBufferWhere.toString(),             			// where
//	    					DomainConstants.EMPTY_STRING,     				// query
//	    					true,							  				// expand
//	    					busSelect,                      				// objects
//	    					(short)0);                        				// limits
//		    		
//		    		
//		    		for(int k=0; k<mlManyPartOneDrawingList.size(); k++){
//		    			
//		    			Map manyPartOneDrawingMap = (Map)mlManyPartOneDrawingList.get(k);
//		    			String manyPartOneDrawingPartObjectId = (String) manyPartOneDrawingMap.get(DomainConstants.SELECT_ID);
//		    			DomainObject manyPartOneDrawingObj = new DomainObject(manyPartOneDrawingPartObjectId);
//		    			manyPartOneDrawingObj.setAttributeValue(context, "cdmPartDrawingNo", "");
//		    			
//		    		}
//					
//				}
//				
				

				String strPartMasterObjectId = StringUtils.trimToEmpty( domObject.getInfo(context, "to[Part Revision].from.id") );
				if(! DomainConstants.EMPTY_STRING.equals(strPartMasterObjectId)){
					new DomainObject(strPartMasterObjectId).deleteObject(context);	
				}
				
				domObject.deleteObject(context);
				
				try {
					
					String strPlmDelUrl = cdmPropertiesUtil.getPropValue(propertyFile, plmDelPartUrl);
					//String strPartDeleteURL = strPlmDelUrl + "partNumber=" + strPartName + "&uid="+strPartOwner;
					String strPartDeleteURL = "http://gplm.halla.com/Windchill/cdmDeletePartNumber.jsp?partNumber=" + strPartName + "&uid="+strPartOwner;
					String strJsonObject = cdmJsonDataUtil.getJSON(strPartDeleteURL);
					
	        	} catch (Exception e) {
	    			
	        		String strErrorMessage = EnoviaResourceBundle.getProperty(context,"emxEngineeringCentralStringResource",context.getLocale(),"emxEngineeringCentral.Alert.CannotGetPartNoFromPLM");
	    			throw new FrameworkException(strErrorMessage);
	    			
	    		} 

			}
			MqlUtil.mqlCommand(context, "trigger on", new String[] {});
			ContextUtil.popContext(context);

			ContextUtil.commitTransaction(context);
		} catch (Exception e) {
			e.printStackTrace();
			ContextUtil.abortTransaction(context);
		}
	}

	/**
	 * Get ExportTable return MapList cdmPart Table
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public MapList getExportTable(Context context, String[] args) throws Exception {
		Map paramMap = JPO.unpackArgs(args);
		String strECObjectId = (String) paramMap.get("objectId");
		String displayTitle = cdmStringUtil.browserCommonCodeLanguage(context.getSession().getLanguage());

		MapList mlAffectedItems = new MapList();
		DomainObject domECObj = new DomainObject(strECObjectId);
		StringList objectSelects = new StringList();
		objectSelects.add(DomainConstants.SELECT_ID);

		mlAffectedItems = domECObj.getRelatedObjects(context, DomainConstants.RELATIONSHIP_AFFECTED_ITEM, // Relationship
				cdmConstantsUtil.TYPE_CDMPART, // From Type name
				objectSelects, // objects Select
				null, // Rel selects
				false, // to Direction
				true, // from Direction
				(short) 1, // recusion level
				"", "", 0);

		int rowNumber = 1;
		int index = 0;
		MapList mlObjectList = new MapList();
		for (int i = 0; i < mlAffectedItems.size(); i++) {

			Map affectedItemsMap = (Map) mlAffectedItems.get(i);
			String strPartId = (String) affectedItemsMap.get(DomainConstants.SELECT_ID);

			DomainObject doPart = new DomainObject(strPartId);
			String strFirstId = doPart.getInfo(context, "first.id");
			String strPrevPartId = doPart.getInfo(context, "previous.id");

			boolean isNewPart = strPartId.equals(strFirstId);
			String strChange = isNewPart ? "New" : "Revise";

			Map prevMap = new HashMap();
			prevMap.put("rowNumber", String.valueOf(rowNumber++));
			prevMap.put("Change", StringUtils.EMPTY);
			prevMap.put("parentId", StringUtils.EMPTY);
			prevMap.put("childId", strPrevPartId);
			prevMap.put("index", String.valueOf(index++));
			mlObjectList.add(prevMap);

			Map nextMap = new HashMap();
			nextMap.put("rowNumber", StringUtils.EMPTY);
			nextMap.put("Change", strChange);
			nextMap.put("parentId", StringUtils.EMPTY);
			nextMap.put("childId", strPartId);
			nextMap.put("index", String.valueOf(index++));
			mlObjectList.add(nextMap);
		}

		StringList busSelect = new StringList();
		busSelect.add(DomainObject.SELECT_ID);
		busSelect.add(DomainObject.SELECT_NAME);

		StringList relSelect = new StringList();
		relSelect.add(EngineeringConstants.SELECT_ATTRIBUTE_QUANTITY);

		// check add del.
		for (int i = 0; i < mlAffectedItems.size(); i++) {

			Map affectedItemsMap = (Map) mlAffectedItems.get(i);
			String strPartId = (String) affectedItemsMap.get(DomainConstants.SELECT_ID);

			DomainObject doPart = new DomainObject(strPartId);
			String strPrevPartId = doPart.getInfo(context, "previous.id");

			MapList mlCurrentBom = doPart.getRelatedObjects(context, DomainConstants.RELATIONSHIP_EBOM, // Relationship
					cdmConstantsUtil.TYPE_CDMPART, // From Type name
					busSelect, // objects Select
					relSelect, // Rel selects
					false, // to Direction
					true, // from Direction
					(short) 1, // recusion level
					"", "", 0);

			MapList mlPrevBom = new MapList();

			if (StringUtils.isNotEmpty(strPrevPartId)) {

				DomainObject doPrevPart = new DomainObject(strPrevPartId);
				mlPrevBom = doPrevPart.getRelatedObjects(context, DomainConstants.RELATIONSHIP_EBOM, // Relationship
						cdmConstantsUtil.TYPE_CDMPART, // From Type name
						busSelect, // objects Select
						relSelect, // Rel selects
						false, // to Direction
						true, // from Direction
						(short) 1, // recusion level
						"", "", 0);
			}

			// add
			curFor: for (int j = 0; j < mlCurrentBom.size(); j++) {
				Map currentBomMap = (Map) mlCurrentBom.get(j);

				String strCurrChildPartId = (String) currentBomMap.get(DomainObject.SELECT_ID);
				String strCurrChildPartNo = (String) currentBomMap.get(DomainObject.SELECT_NAME);
				String strCurrQty = (String) currentBomMap.get(EngineeringConstants.SELECT_ATTRIBUTE_QUANTITY);

				for (int k = 0; k < mlPrevBom.size(); k++) {
					Map prevBomMap = (Map) mlPrevBom.get(k);

					// strPrevChildPartId = (String)
					// prevBomMap.get(DomainObject.SELECT_ID);
					String strPrevChildPartNo = (String) prevBomMap.get(DomainObject.SELECT_NAME);

					if (strCurrChildPartNo.equals(strPrevChildPartNo)) {
						continue curFor;
					}
				}

				Map prevMap = new HashMap();
				prevMap.put("rowNumber", String.valueOf(rowNumber++));
				prevMap.put("Change", StringUtils.EMPTY);
				prevMap.put("parentId", strPrevPartId);
				prevMap.put("childId", StringUtils.EMPTY);
				prevMap.put("quantity", StringUtils.EMPTY);
				prevMap.put("index", String.valueOf(index++));
				mlObjectList.add(prevMap);

				Map nextMap = new HashMap();
				nextMap.put("rowNumber", StringUtils.EMPTY);
				nextMap.put("Change", "Add");
				nextMap.put("parentId", strPartId);
				nextMap.put("childId", strCurrChildPartId);
				nextMap.put("quantity", strCurrQty);
				nextMap.put("index", String.valueOf(index++));
				mlObjectList.add(nextMap);

			}

			if (StringUtils.isEmpty(strPrevPartId)) {
				continue;
			}

			// check Item Deleted
			prevFor: for (int j = 0; j < mlPrevBom.size(); j++) {

				Map prevBomMap = (Map) mlPrevBom.get(j);

				String strPrevChildPartId = (String) prevBomMap.get(DomainObject.SELECT_ID);
				String strPrevChildPartNo = (String) prevBomMap.get(DomainObject.SELECT_NAME);
				String strPrevQty = (String) prevBomMap.get(EngineeringConstants.SELECT_ATTRIBUTE_QUANTITY);
				for (int k = 0; k < mlCurrentBom.size(); k++) {
					Map currentBomMap = (Map) mlCurrentBom.get(k);
					// String strCurrChildPartId = (String)
					// currentBomMap.get(DomainObject.SELECT_ID);
					String strCurrChildPartNo = (String) currentBomMap.get(DomainObject.SELECT_NAME);

					if (strPrevChildPartNo.equals(strCurrChildPartNo)) {
						continue prevFor;
					}
				}

				Map prevMap = new HashMap();
				prevMap.put("rowNumber", String.valueOf(rowNumber++));
				prevMap.put("Change", StringUtils.EMPTY);
				prevMap.put("parentId", strPrevPartId);
				prevMap.put("childId", strPrevChildPartId);
				prevMap.put("quantity", strPrevQty);
				prevMap.put("index", String.valueOf(index++));
				mlObjectList.add(prevMap);

				Map nextMap = new HashMap();
				nextMap.put("rowNumber", StringUtils.EMPTY);
				nextMap.put("Change", "Del");
				nextMap.put("parentId", strPartId);
				nextMap.put("childId", StringUtils.EMPTY);
				nextMap.put("quantity", StringUtils.EMPTY);
				nextMap.put("index", String.valueOf(index++));
				mlObjectList.add(nextMap);

			}

			// check Quantity modified
			curFor: for (int j = 0; j < mlCurrentBom.size(); j++) {
				Map currentBomMap = (Map) mlCurrentBom.get(j);

				String strCurrChildPartId = (String) currentBomMap.get(DomainObject.SELECT_ID);
				String strCurrChildPartNo = (String) currentBomMap.get(DomainObject.SELECT_NAME);
				String strCurrQty = (String) currentBomMap.get(EngineeringConstants.SELECT_ATTRIBUTE_QUANTITY);

				boolean isContainedSamePart = false;
				String strPrevChildPartId = null;
				String strPrevQty = null;
				for (int k = 0; k < mlPrevBom.size(); k++) {
					Map prevBomMap = (Map) mlPrevBom.get(k);

					String strPrevChildPartNo = (String) prevBomMap.get(DomainObject.SELECT_NAME);
					strPrevChildPartId = (String) prevBomMap.get(DomainObject.SELECT_ID);
					strPrevQty = (String) prevBomMap.get(EngineeringConstants.SELECT_ATTRIBUTE_QUANTITY);

					if (strCurrChildPartNo.equals(strPrevChildPartNo)) {

						isContainedSamePart = true;
						if (strCurrQty.equals(strPrevQty)) {
							continue curFor;
						} else {
							break;
						}
					}
				}

				if (!isContainedSamePart)
					continue;

				Map prevMap = new HashMap();
				prevMap.put("rowNumber", String.valueOf(rowNumber++));
				prevMap.put("Change", StringUtils.EMPTY);
				prevMap.put("parentId", strPrevPartId);
				prevMap.put("childId", strPrevChildPartId);
				prevMap.put("quantity", strPrevQty);
				prevMap.put("index", String.valueOf(index++));
				mlObjectList.add(prevMap);

				Map nextMap = new HashMap();
				nextMap.put("rowNumber", StringUtils.EMPTY);
				nextMap.put("Change", "Mod");
				nextMap.put("parentId", strPartId);
				nextMap.put("childId", strCurrChildPartId);
				nextMap.put("quantity", strCurrQty);
				nextMap.put("index", String.valueOf(index++));
				mlObjectList.add(nextMap);

			}
		}


		SelectList selectList = new SelectList();

		selectList.add(DomainObject.SELECT_NAME);
		selectList.add(DomainObject.SELECT_REVISION);
		selectList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_NAME);
		selectList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_ITEM_TYPE);
		selectList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_UOM);
		selectList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_MATERIAL);
		selectList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_SURFACE_TREATMENT);
		selectList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_SIZE);
		selectList.add(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_MATERIAL);

		MapList mlResultList = new MapList();

		for (int i = 0; i < mlObjectList.size(); i++) {

			Map prevMap = (Map) mlObjectList.get(i);
			String strIndex = (String) prevMap.get("index");
			String strRowNumber = (String) prevMap.get("rowNumber");
			String strChange = (String) prevMap.get("Change");

			String strParentId = (String) prevMap.get("parentId");
			String strChildId = (String) prevMap.get("childId");

			String strQty = (String) prevMap.get("quantity");

			Map objMap = new HashMap();
			objMap.put("rowNumber", strRowNumber);
			objMap.put("Change", strChange);
			objMap.put("index", strIndex);
			objMap.put("Quantity", strQty);

			if (StringUtils.isNotEmpty(strParentId)) {
				DomainObject doParent = new DomainObject(strParentId);

				Map partMap = doParent.getInfo(context, selectList);
				String partNo = (String) partMap.get(DomainObject.SELECT_NAME);
				String partRev = (String) partMap.get(DomainObject.SELECT_REVISION);
				String partName = (String) partMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_NAME);
				String partUOM = (String) partMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_UOM);
				String partMaterial = (String) partMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_MATERIAL);
				String partTreatment = (String) partMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_SURFACE_TREATMENT);
				String partSize = (String) partMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_SIZE);
				String partItemType = (String) partMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_ITEM_TYPE);

				objMap.put("PartNo", partNo);
				objMap.put("Revision", partRev);
				objMap.put("PartName", partName);
				objMap.put("Each", partUOM);
				objMap.put("Material", partMaterial);
				objMap.put("Surface", partTreatment);
				objMap.put("Size", partSize);
				objMap.put("ItemType", partItemType);
			}

			if (StringUtils.isNotEmpty(strChildId)) {

				DomainObject doChild = new DomainObject(strChildId);

				Map partMap = doChild.getInfo(context, selectList);
				String partNo = (String) partMap.get(DomainObject.SELECT_NAME);
				String partRev = (String) partMap.get(DomainObject.SELECT_REVISION);
				String partName = (String) partMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_NAME);
				String partUOM = (String) partMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_UOM);
				String partMaterial = (String) partMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_MATERIAL);
				String partTreatment = (String) partMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_SURFACE_TREATMENT);
				String partSize = (String) partMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_SIZE);
				String partItemType = (String) partMap.get(cdmConstantsUtil.SELECT_ATTRIBUTE_CDM_PART_ITEM_TYPE);

				objMap.put("Child_No", partNo);
				objMap.put("Child_Rev", partRev);
				objMap.put("Child_PartName", partName);
				objMap.put("Each", partUOM);
				objMap.put("Material", partMaterial);
				objMap.put("Surface", partTreatment);
				objMap.put("Size", partSize);
				objMap.put("ItemType", partItemType);
			}

			mlResultList.add(objMap);
		}
		System.out.println("###########mlResultList      "+mlResultList);
		return mlResultList;
	}

	/**
	 * PLM Export Table Column for Number Setting
	 * 
	 * @return vector.
	 * @throws Exception
	 */

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Vector setReportTable(Context context, String[] args) throws Exception {
		Vector columnValues = new Vector();
		try {
			HashMap programMap = (HashMap) JPO.unpackArgs(args);
			HashMap columnMap = (HashMap) programMap.get("columnMap");
			HashMap columnSettings = (HashMap) columnMap.get("settings");
			MapList objectList = (MapList) programMap.get("objectList");
			String strFlag = (String) columnSettings.get("Flag");

			objectList.sort("index", "ascending", "integer");

			for (int i = 0; i < objectList.size(); i++) {
				Map map = (Map) objectList.get(i);

				String strValue = (String) map.get(strFlag);
				strValue = strValue == null ? StringUtils.EMPTY : strValue;

				columnValues.add(strValue);
			}
			return columnValues;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

	}

	/*
	 * Setting for Style Column Color
	 */
	public StringList setNumberColumnStyle(Context context, String[] args) throws Exception {
		StringList slColStyles = new StringList();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		MapList objectList = (MapList) programMap.get("objectList");
		objectList.sort("rowNumber", "ascending", "string");
		
		int iObjSize = objectList.size();
		
		try {
			
			int idx = 0;
			
			for (int i = 0; i < iObjSize; i++) {
				String colStyle = "";
				
				idx++;
				
				if(idx == 1 || idx == 2){
					colStyle = "EvenBackGroundColor";
					slColStyles.add(colStyle);
					
				}else if(idx == 3 || idx == 4){
					colStyle = "OddBackGroundColor";
					slColStyles.add(colStyle);
				}

				if (idx == 4)
					idx = 0;
				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return slColStyles;
	}
	
	

	/*
	 * Get Validate Column.
	 */
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Vector setValidateMessage(Context context, String[] args) throws Exception{
		Vector columnValues = new Vector();
		
		try {
			HashMap programMap = (HashMap) JPO.unpackArgs(args);
			MapList mlObjectList = (MapList) programMap.get("objectList");
			
			int iObjSize = mlObjectList.size();
			
			for (int i = 0; i < iObjSize; i++) {
				Map map = (Map) mlObjectList.get(i);
				String strMessage = (String) map.get("message");
				
				columnValues.add(strMessage);
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return columnValues;
	}
	
	/*
	 * Get Validate Values.
	 */
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Vector setValidatePartName(Context context, String[] args) throws Exception{
		Vector columnValues = new Vector();
		
		try {
			HashMap programMap = (HashMap) JPO.unpackArgs(args);
			MapList mlObjectList = (MapList) programMap.get("objectList");
			
			int iObjSize = mlObjectList.size();
			
			for (int i = 0; i < iObjSize; i++) {
				Map map = (Map) mlObjectList.get(i);
				String strName = (String) map.get("part");
				
				columnValues.add(strName);
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return columnValues;
	}
	
	
	/**
	 * check validation when Part is connected EC. trigger check method for REL Affected Item
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public int checkValidation(Context context, String[] args) throws Exception {
		
		
		try {
			
			String strECObjectId   = StringUtils.trimToEmpty(args[0]);    // ECO
			String strPartObjectId = StringUtils.trimToEmpty(args[1]);    // Part
			
			if(! DomainConstants.EMPTY_STRING.equals(strECObjectId) && ! DomainConstants.EMPTY_STRING.equals(strPartObjectId)){
				
				DomainObject doECO = new DomainObject(strECObjectId);
				String strCurrentECOCDMOnly = doECO.getInfo(context, "attribute[cdmECCategoryOnlyCDM]");
				
				DomainObject doPart = new DomainObject(strPartObjectId);
				String strPreviousPartId = StringUtils.trimToEmpty(doPart.getInfo(context, "previous.id"));
				
				if(! DomainConstants.EMPTY_STRING.equals(strPreviousPartId)){
					
					DomainObject doPreviousPart = new DomainObject(strPreviousPartId);
					String strPreviousItemECOCDMOnly = doPreviousPart.getInfo(context, "to[Affected Item].from.attribute[cdmECCategoryOnlyCDM]");
					
					if("NO".equalsIgnoreCase(strPreviousItemECOCDMOnly)) {
						
						if(!"NO".equalsIgnoreCase(strCurrentECOCDMOnly)) {
							String strErrorMessage = EnoviaResourceBundle.getProperty(context,"emxEngineeringCentralStringResource",context.getLocale(),"emxEngineeringCentral.Alert.notaddcdmonly");
							throw new FrameworkException(strErrorMessage);
						}
					}
					
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
	 * @return
	 * @throws Exception
	 */
	public int updateConversionFinishPLMFlag(Context context, String[] args) throws Exception {
		
		SqlSession sqlSession = null;
		try {
			
			
			String strECObjectId = args[0];
			String strECOType = args[1];
			String strECNumber = args[2];
			String strAttrName = args[3];
			String strAttrValue = args[4];
			String strAttrNewValue = args[5];
			
			/*
			 * 	'eService Program Argument 1' '${OBJECTID}'
				'eService Program Argument 2' '${TYPE}'
				'eService Program Argument 3' '${NAME}'
				'eService Program Argument 4' '${ATTRNAME}'
				'eService Program Argument 5' '${ATTRVALUE}'
				'eService Program Argument 6' '${NEWATTRVALUE}'
			 */
			
			
	
			
			if (cdmConstantsUtil.TYPE_CDMECO.equals(strECOType) || cdmConstantsUtil.TYPE_CDMEAR.equals(strECOType) || cdmConstantsUtil.TYPE_CDMPEO.equals(strECOType)) {
				
				
				if ("Yes".equalsIgnoreCase(strAttrNewValue)) {

					SqlSessionUtil.reNew("plm");
					sqlSession = SqlSessionUtil.getSqlSession();

					// update flag ec state
					Map updateEOMasterMap = new HashMap();
					updateEOMasterMap.put("CDM_FLAG", "M");
					updateEOMasterMap.put("EO_NUMBER", strECNumber);
					sqlSession.update("updateEOMaster", updateEOMasterMap);
					sqlSession.commit();
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlSession.close();
		}
		
		return 0;
		
	}

	
}






