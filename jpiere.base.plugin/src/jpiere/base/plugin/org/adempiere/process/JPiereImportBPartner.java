/******************************************************************************
 * Product: JPiere                                                            *
 * Copyright (C) Hideaki Hagiwara (h.hagiwara@oss-erp.co.jp)                  *
 *                                                                            *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY.                          *
 * See the GNU General Public License for more details.                       *
 *                                                                            *
 * JPiere is maintained by OSS ERP Solutions Co., Ltd.                        *
 * (http://www.oss-erp.co.jp)                                                 *
 *****************************************************************************/
package jpiere.base.plugin.org.adempiere.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.adempiere.model.ImportValidator;
import org.adempiere.process.ImportProcess;
import org.adempiere.util.IProcessUI;
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MColumn;
import org.compiere.model.MContactInterest;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.PO;
import org.compiere.model.X_I_BPartner;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

import jpiere.base.plugin.org.adempiere.model.X_I_BPartnerJP;
import jpiere.base.plugin.util.JPiereLocationUtil;

/**
 *	JPIERE-0092 : Import BPartners from I_BPartner
 *
 * 	@author 	Jorg Janke
 * 	@version 	$Id: ImportBPartner.java,v 1.2 2006/07/30 00:51:02 jjanke Exp $
 *
 * @author Teo Sarca, www.arhipac.ro
 * 			<li>FR [ 2788074 ] ImportBPartner: add IsValidateOnly option
 * 				https://sourceforge.net/tracker/?func=detail&aid=2788074&group_id=176962&atid=879335
 * 			<li>FR [ 2788278 ] Data Import Validator - migrate core processes
 * 				https://sourceforge.net/tracker/?func=detail&aid=2788278&group_id=176962&atid=879335
 *
 * @author Hideaki Hagiwara
 */
public class JPiereImportBPartner extends SvrProcess implements ImportProcess
{
	/**	Client to be imported to		*/
	private int				m_AD_Client_ID = 0;
	/**	Delete old Imported				*/
	private boolean			m_deleteOldImported = false;
	/**	Only validate, don't import		*/
	private boolean			p_IsValidateOnly = false;

	/** Effective						*/
	private Timestamp		m_DateValue = null;

	private IProcessUI processMonitor = null;

	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (name.equals("AD_Client_ID"))
				m_AD_Client_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("DeleteOldImported"))
				m_deleteOldImported = "Y".equals(para[i].getParameter());
			else if (name.equals("IsValidateOnly"))
				p_IsValidateOnly = para[i].getParameterAsBoolean();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
		if (m_DateValue == null)
			m_DateValue = new Timestamp (System.currentTimeMillis());
	}	//	prepare


	/**
	 *  Perform process.
	 *  @return Message
	 *  @throws Exception
	 */
	protected String doIt() throws java.lang.Exception
	{

		processMonitor = Env.getProcessUI(getCtx());

		StringBuilder sql = null;
		int no = 0;
		String clientCheck = getWhereClause();

		//	****	Prepare	****

		//	Delete Old Imported
		if (m_deleteOldImported)
		{
			sql = new StringBuilder ("DELETE I_BPartnerJP ")
					.append("WHERE I_IsImported='Y'").append(clientCheck);
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Delete Old Impored =" + no);
		}

		ModelValidationEngine.get().fireImportValidate(this, null, null, ImportValidator.TIMING_BEFORE_VALIDATE);

		//Reverse Lookup Surrogate Key
		reverseLookupC_BPartner_ID();
		reverseLookupAD_Org_ID();
		reverseLookupJP_Corporation_ID();
		reverseLookupC_BP_Group_ID();
		reverseLookupSalesRep_ID();
		reverseLookupC_InvoiceSchedule_ID();
		reverseLookupC_PaymentTerm_ID();
		reverseLookupPO_PaymentTerm_ID();
		reverseLookupJP_BillSchema_ID();
		reverseLookupJP_BillSchemaPO_ID();
		reverseLookupM_PriceList_ID();
		reverseLookupPO_PriceList_ID();
		reverseLookupM_DiscountSchema_ID();
		reverseLookupPO_DiscountSchema_ID();
		reverseLookupC_Dunning_ID();
		reverseLookupDefault1099Box_ID();
		reverseLookupC_Greeting_ID();
		reverseLookupJP_User_Greeting_ID();
		reverseLookupC_Location_ID();
		reverseLookupC_SalesRegion_ID();
		reverseLookupAD_User_ID();
		reverseLookupR_InterestArea_ID();
		reverseLookupInvoice_PrintFormat_ID();


		ModelValidationEngine.get().fireImportValidate(this, null, null, ImportValidator.TIMING_AFTER_VALIDATE);

		commitEx();
		if (p_IsValidateOnly)
		{
			return "Validated";
		}


		sql = new StringBuilder ("SELECT * FROM I_BPartnerJP WHERE I_IsImported='N' ")
				.append(clientCheck).append(" ORDER BY Value, ContactName, EMail ");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		int i = 0;
		try
		{
			pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
			rs = pstmt.executeQuery();
			String preValue = "";
			MBPartner bpartner = null;

			while (rs.next())
			{
				X_I_BPartnerJP imp = new X_I_BPartnerJP (getCtx (), rs, get_TrxName());

				if(Util.isEmpty(imp.getValue()))
				{
					Object[] objs = new Object[]{Msg.getElement(Env.getCtx(), "Value")};
					imp.setI_ErrorMsg(Msg.getMsg(getCtx(), "Error") + Msg.getMsg(Env.getCtx(),"JP_Mandatory",objs));
					imp.setI_IsImported(false);
					imp.setProcessed(false);
					imp.saveEx(get_TrxName());
					continue;
				}

				boolean isNew = true;
				if(imp.getC_BPartner_ID()!=0)
				{
					isNew =false;

				}else{

					if(preValue.equals(imp.getValue()))
					{
						isNew = false;

					}else {

						preValue = imp.getValue();

					}

				}

				if(isNew)
				{
					bpartner = new MBPartner(getCtx(), 0, get_TrxName());
					createNewBPartner(imp,bpartner);

				}else{

					updateBPartner(imp, bpartner);
				}

				commitEx();

				i++;
				if (processMonitor != null)	processMonitor.statusUpdate(String.valueOf(i));

			}//while

		}catch (Exception e) {

			log.log(Level.SEVERE, e.toString(), e);

		}finally{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return Msg.getMsg(getCtx(), "Success") + " : " + i ;
	}	//	doIt


	//@Override
	public String getWhereClause()
	{
		StringBuilder msgreturn = new StringBuilder(" AND AD_Client_ID=").append(m_AD_Client_ID);
		return msgreturn.toString();
	}


	//@Override
	public String getImportTableName()
	{
		return X_I_BPartner.Table_Name;
	}

	/**
	 * Reverse look up C_BPartner_ID From Value
	 *
	 * @throws Exception
	 */
	private void reverseLookupC_BPartner_ID() throws Exception
	{
		StringBuilder sql = new StringBuilder();
		String msg = new String();
		int no = 0;

		 msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "C_BPartner_ID");
		if (processMonitor != null)	processMonitor.statusUpdate(msg);

		//Reverse lookup C_BPartner_ID From Value
		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "C_BPartner_ID")
		+ " - " + Msg.getMsg(getCtx(), "MatchFrom") + " : " + Msg.getElement(getCtx(), "Value") ;
		sql = new StringBuilder ("UPDATE I_BPartnerJP i ")
			.append("SET C_BPartner_ID=(SELECT C_BPartner_ID FROM C_BPartner p")
			.append(" WHERE i.Value=p.Value AND i.AD_Client_ID=p.AD_Client_ID) ")
			.append("WHERE i.C_BPartner_ID IS NULL AND i.Value IS NOT NULL ")
			.append(" AND I_IsImported<>'Y'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Found Business Partner =" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

	}//reverseLookupC_BPartner_ID


	/**
	 * Reverse Look up Organization From JP_Org_Value
	 *
	 **/
	private void reverseLookupAD_Org_ID() throws Exception
	{

		StringBuilder sql = new StringBuilder();
		String msg = new String();
		int no = 0;

		 msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "AD_Org_ID");
		if (processMonitor != null)	processMonitor.statusUpdate(msg);

		//Look up AD_Org ID From JP_Org_Value
		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "AD_Org_ID")
		+ " - " + Msg.getMsg(getCtx(), "MatchFrom") + " : " + Msg.getElement(getCtx(), "JP_Org_Value") ;
		sql = new StringBuilder ("UPDATE I_BPartnerJP i ")
				.append("SET AD_Org_ID=(SELECT AD_Org_ID FROM AD_org p")
				.append(" WHERE i.JP_Org_Value=p.Value AND (p.AD_Client_ID=i.AD_Client_ID or p.AD_Client_ID=0) AND p.IsSummary='N') ")
				.append(" WHERE i.JP_Org_Value IS NOT NULL")
				.append(" AND i.I_IsImported='N'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + sql );
		}

		//Invalid JP_Org_Value
		msg = Msg.getMsg(getCtx(), "Invalid")+Msg.getElement(getCtx(), "JP_Org_Value");
		sql = new StringBuilder ("UPDATE I_BPartnerJP ")
			.append("SET I_ErrorMsg='"+ msg + "'")
			.append(" WHERE AD_Org_ID = 0 AND JP_Org_Value IS NOT NULL AND JP_Org_Value <> '0' ")
			.append(" AND I_IsImported<>'Y'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		if(no > 0)
		{
			commitEx();
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg );
		}

	}//reverseLookupAD_Org_ID


	/**
	 * Reverse Look up JP_Corporation_ID From JP_CorporationValue
	 *
	 *
	 */
	private void reverseLookupJP_Corporation_ID() throws Exception
	{
		StringBuilder sql = new StringBuilder();
		String msg = new String();
		int no = 0;

		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "JP_Corporation_ID");
		if (processMonitor != null)	processMonitor.statusUpdate(msg);

		//Look up JP_Corporation_ID From JP_CorporationValue
		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "JP_Corporation_ID")
		+ " - " + Msg.getMsg(getCtx(), "MatchFrom") + " : " + Msg.getElement(getCtx(), "JP_CorporationValue") ;
		sql = new StringBuilder ("UPDATE I_BPartnerJP i ")
				.append("SET JP_Corporation_ID=(SELECT JP_Corporation_ID FROM JP_Corporation p")
				.append(" WHERE i.JP_CorporationValue=p.Value AND p.AD_Client_ID=i.AD_Client_ID) ")
				.append(" WHERE i.JP_CorporationValue IS NOT NULL")
				.append(" AND i.I_IsImported='N'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		//Invalid JP_CorporationValue
		msg = Msg.getMsg(getCtx(), "Invalid")+Msg.getElement(getCtx(), "JP_CorporationValue");
		sql = new StringBuilder ("UPDATE I_BPartnerJP ")
			.append("SET I_ErrorMsg='"+ msg + "'")
			.append(" WHERE JP_CorporationValue IS NOT NULL AND JP_Corporation_ID IS NULL ")
			.append(" AND I_IsImported<>'Y'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		if(no > 0)
		{
			commitEx();
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg );
		}

	}

	/**
	 * Reverse Look up C_BP_Group_ID From GroupValue
	 *
	 * @throws Exception
	 */
	private void reverseLookupC_BP_Group_ID() throws Exception
	{
		StringBuilder sql = new StringBuilder();
		String msg = new String();
		int no = 0;

		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "C_BP_Group_ID");
		if (processMonitor != null)	processMonitor.statusUpdate(msg);


		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "C_BP_Group_ID")
		+ " - " + Msg.getMsg(getCtx(), "MatchFrom") + " : " + Msg.getElement(getCtx(), "GroupValue") ;

		//	Set Default C_BP_Group_ID
		sql = new StringBuilder ("UPDATE I_BPartnerJP i ")
				.append("SET GroupValue=(SELECT MAX(Value) FROM C_BP_Group g WHERE g.IsDefault='Y'")
				.append(" AND g.AD_Client_ID=i.AD_Client_ID) ");
		sql.append("WHERE GroupValue IS NULL AND C_BP_Group_ID IS NULL")
				.append(" AND I_IsImported<>'Y'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		//Look up C_BP_Group_ID From GroupValue
		sql = new StringBuilder ("UPDATE I_BPartnerJP i ")
				.append("SET C_BP_Group_ID=(SELECT C_BP_Group_ID FROM C_BP_Group g")
				.append(" WHERE i.GroupValue=g.Value AND g.AD_Client_ID=i.AD_Client_ID) ")
				.append("WHERE C_BP_Group_ID IS NULL")
				.append(" AND I_IsImported<>'Y'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		//Invalid GroupValue
		msg = Msg.getMsg(getCtx(), "Invalid")+Msg.getElement(getCtx(), "GroupValue");
		sql = new StringBuilder ("UPDATE I_BPartnerJP ")
			.append("SET I_ErrorMsg='"+ msg + "'")
			.append(" WHERE GroupValue IS NOT NULL AND C_BP_Group_ID IS NULL ")
			.append(" AND I_IsImported<>'Y'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		if(no > 0)
		{
			commitEx();
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg );
		}
	}

	/**
	 * Reverse Look up SalesRep_ID From JP_SalesRep_EMail
	 *
	 * @throws Exception
	 */
	private void reverseLookupSalesRep_ID() throws Exception
	{
		StringBuilder sql = new StringBuilder();
		String msg = new String();
		int no = 0;

		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "SalesRep_ID");
		if (processMonitor != null)	processMonitor.statusUpdate(msg);

		//Reverse Look up SalesRep_ID From JP_SalesRep_EMail
		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "SalesRep_ID")
		+ " - " + Msg.getMsg(getCtx(), "MatchFrom") + " : " + Msg.getElement(getCtx(), "JP_SalesRep_EMail") ;
		sql = new StringBuilder ("UPDATE I_BPartnerJP i ")
				.append("SET SalesRep_ID=(SELECT AD_User_ID FROM AD_User p")
				.append(" WHERE i.JP_SalesRep_EMail=p.EMail AND ( p.AD_Client_ID=i.AD_Client_ID OR p.AD_Client_ID=0 ) ) ")
				.append(" WHERE i.JP_SalesRep_EMail IS NOT NULL")
				.append(" AND i.I_IsImported='N'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no + ":" + sql);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		//Invalid JP_SalesRep_EMail
		msg = Msg.getMsg(getCtx(), "Invalid")+Msg.getElement(getCtx(), "JP_SalesRep_EMail");
		sql = new StringBuilder ("UPDATE I_BPartnerJP ")
			.append("SET I_ErrorMsg='"+ msg + "'")
			.append(" WHERE JP_SalesRep_EMail IS NOT NULL AND SalesRep_ID IS NULL ")
			.append(" AND I_IsImported<>'Y'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no + ":" + sql);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		if(no > 0)
		{
			commitEx();
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg );
		}

	}//reverseSalesRep_ID


	/**
	 * Reverse Look up C_InvoiceSchedule_ID From JP_InvoiceSchedule_Name
	 *
	 * @throws Exception
	 */
	private void reverseLookupC_InvoiceSchedule_ID() throws Exception
	{
		StringBuilder sql = new StringBuilder();
		String msg = new String();
		int no = 0;

		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "C_InvoiceSchedule_ID");
		if (processMonitor != null)	processMonitor.statusUpdate(msg);

		//Reverse Look up C_InvoiceSchedule_ID From JP_InvoiceSchedule_Name
		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "C_InvoiceSchedule_ID")
		+ " - " + Msg.getMsg(getCtx(), "MatchFrom") + " : " + Msg.getElement(getCtx(), "JP_InvoiceSchedule_Name") ;
		sql = new StringBuilder ("UPDATE I_BPartnerJP i ")
				.append("SET C_InvoiceSchedule_ID=(SELECT C_InvoiceSchedule_ID FROM C_InvoiceSchedule p")
				.append(" WHERE i.JP_InvoiceSchedule_Name=p.Name AND p.AD_Client_ID=i.AD_Client_ID ) ")
				.append(" WHERE i.JP_InvoiceSchedule_Name IS NOT NULL")
				.append(" AND i.I_IsImported='N'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		//Invalid JP_InvoiceSchedule_Name
		msg = Msg.getMsg(getCtx(), "Invalid")+Msg.getElement(getCtx(), "JP_InvoiceSchedule_Name");
		sql = new StringBuilder ("UPDATE I_BPartnerJP ")
			.append("SET I_ErrorMsg='"+ msg + "'")
			.append(" WHERE JP_InvoiceSchedule_Name IS NOT NULL AND C_InvoiceSchedule_ID IS NULL ")
			.append(" AND I_IsImported<>'Y'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		if(no > 0)
		{
			commitEx();
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg );
		}

	}//reverseLookupC_InvoiceSchedule_ID


	/**
	 * Reverse Look up C_PaymentTerm_ID From JP_PaymentTerm_Value
	 *
	 * @throws Exception
	 */
	private void reverseLookupC_PaymentTerm_ID() throws Exception
	{
		StringBuilder sql = new StringBuilder();
		String msg = new String();
		int no = 0;

		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "C_PaymentTerm_ID");
		if (processMonitor != null)	processMonitor.statusUpdate(msg);

		//Reverse Look up C_PaymentTerm_ID From JP_PaymentTerm_Value
		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "C_PaymentTerm_ID")
		+ " - " + Msg.getMsg(getCtx(), "MatchFrom") + " : " + Msg.getElement(getCtx(), "JP_PaymentTerm_Value") ;
		sql = new StringBuilder ("UPDATE I_BPartnerJP i ")
				.append("SET C_PaymentTerm_ID=(SELECT C_PaymentTerm_ID FROM C_PaymentTerm p")
				.append(" WHERE i.JP_PaymentTerm_Value=p.Value AND p.AD_Client_ID=i.AD_Client_ID ) ")
				.append(" WHERE i.JP_PaymentTerm_Value IS NOT NULL")
				.append(" AND i.I_IsImported='N'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		//Invalid JP_PaymentTerm_Value
		msg = Msg.getMsg(getCtx(), "Invalid")+Msg.getElement(getCtx(), "JP_PaymentTerm_Value");
		sql = new StringBuilder ("UPDATE I_BPartnerJP ")
			.append("SET I_ErrorMsg='"+ msg + "'")
			.append(" WHERE JP_PaymentTerm_Value IS NOT NULL AND C_PaymentTerm_ID IS NULL ")
			.append(" AND I_IsImported<>'Y'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		if(no > 0)
		{
			commitEx();
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg );
		}

	}//reverseLookupC_PaymentTerm_ID

	/**
	 *
	 * Reverse Look up PO_PaymentTerm_ID From JP_PO_PaymentTerm_Value
	 *
	 * @throws Exception
	 */
	private void reverseLookupPO_PaymentTerm_ID() throws Exception
	{
		StringBuilder sql = new StringBuilder();
		String msg = new String();
		int no = 0;

		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "PO_PaymentTerm_ID");
		if (processMonitor != null)	processMonitor.statusUpdate(msg);

		//Reverse Look up PO_PaymentTerm_ID From JP_PO_PaymentTerm_Value
		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "PO_PaymentTerm_ID")
		+ " - " + Msg.getMsg(getCtx(), "MatchFrom") + " : " + Msg.getElement(getCtx(), "JP_PO_PaymentTerm_Value") ;
		sql = new StringBuilder ("UPDATE I_BPartnerJP i ")
				.append("SET PO_PaymentTerm_ID=(SELECT C_PaymentTerm_ID FROM C_PaymentTerm p")
				.append(" WHERE i.JP_PO_PaymentTerm_Value=p.Value AND p.AD_Client_ID=i.AD_Client_ID ) ")
				.append(" WHERE i.JP_PO_PaymentTerm_Value IS NOT NULL")
				.append(" AND i.I_IsImported='N'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		//Invalid JP_PO_PaymentTerm_Value
		msg = Msg.getMsg(getCtx(), "Invalid")+Msg.getElement(getCtx(), "JP_PO_PaymentTerm_Value");
		sql = new StringBuilder ("UPDATE I_BPartnerJP ")
			.append("SET I_ErrorMsg='"+ msg + "'")
			.append(" WHERE JP_PO_PaymentTerm_Value IS NOT NULL AND PO_PaymentTerm_ID IS NULL ")
			.append(" AND I_IsImported<>'Y'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		if(no > 0)
		{
			commitEx();
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg );
		}

	}//reverseLookupPO_PaymentTerm_ID

	/**
	 * Reverse Look up JP_BillSchema_ID From JP_BillSchema_Value
	 *
	 * @throws Exception
	 */
	private void reverseLookupJP_BillSchema_ID() throws Exception
	{
		StringBuilder sql = new StringBuilder();
		String msg = new String();
		int no = 0;

		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "JP_BillSchema_ID");
		if (processMonitor != null)	processMonitor.statusUpdate(msg);

		//Reverse Look up JP_BillSchema_ID From JP_BillSchema_Value
		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "JP_BillSchema_ID")
		+ " - " + Msg.getMsg(getCtx(), "MatchFrom") + " : " + Msg.getElement(getCtx(), "JP_BillSchema_Value") ;
		sql = new StringBuilder ("UPDATE I_BPartnerJP i ")
				.append("SET JP_BillSchema_ID=(SELECT JP_BillSchema_ID FROM JP_BillSchema p")
				.append(" WHERE i.JP_BillSchema_Value=p.Value AND p.AD_Client_ID=i.AD_Client_ID ) ")
				.append(" WHERE i.JP_BillSchema_Value IS NOT NULL")
				.append(" AND i.I_IsImported='N'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		//Invalid JP_BillSchema_Value
		msg = Msg.getMsg(getCtx(), "Invalid")+Msg.getElement(getCtx(), "JP_BillSchema_Value");
		sql = new StringBuilder ("UPDATE I_BPartnerJP ")
			.append("SET I_ErrorMsg='"+ msg + "'")
			.append(" WHERE JP_BillSchema_Value IS NOT NULL AND JP_BillSchema_ID IS NULL ")
			.append(" AND I_IsImported<>'Y'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		if(no > 0)
		{
			commitEx();
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg );
		}

	}//reverseLookupJP_BillSchema_ID

	/**
	 * Reverse Look up JP_BillSchemaPO_ID From JP_BillSchemaPO_Value
	 *
	 * @throws Exception
	 */
	private void reverseLookupJP_BillSchemaPO_ID() throws Exception
	{
		StringBuilder sql = new StringBuilder();
		String msg = new String();
		int no = 0;

		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "JP_BillSchemaPO_ID");
		if (processMonitor != null)	processMonitor.statusUpdate(msg);

		//Reverse Look up JP_BillSchemaPO_ID From JP_BillSchemaPO_Value
		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "JP_BillSchemaPO_ID")
		+ " - " + Msg.getMsg(getCtx(), "MatchFrom") + " : " + Msg.getElement(getCtx(), "JP_BillSchemaPO_Value") ;
		sql = new StringBuilder ("UPDATE I_BPartnerJP i ")
				.append("SET JP_BillSchemaPO_ID=(SELECT JP_BillSchema_ID FROM JP_BillSchema p")
				.append(" WHERE i.JP_BillSchemaPO_Value=p.Value AND p.AD_Client_ID=i.AD_Client_ID ) ")
				.append(" WHERE i.JP_BillSchemaPO_Value IS NOT NULL")
				.append(" AND i.I_IsImported='N'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		//Invalid JP_BillSchemaPO_Value
		msg = Msg.getMsg(getCtx(), "Invalid")+Msg.getElement(getCtx(), "JP_BillSchemaPO_Value");
		sql = new StringBuilder ("UPDATE I_BPartnerJP ")
			.append("SET I_ErrorMsg='"+ msg + "'")
			.append(" WHERE JP_BillSchemaPO_Value IS NOT NULL AND JP_BillSchemaPO_ID IS NULL ")
			.append(" AND I_IsImported<>'Y'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		if(no > 0)
		{
			commitEx();
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg );
		}

	}//reverseLookupJP_BillSchemaPO_ID


	/**
	 * Reverse Look up M_PriceList_ID From JP_PriceList_Name
	 *
	 * @throws Exception
	 */
	private void reverseLookupM_PriceList_ID() throws Exception
	{
		StringBuilder sql = new StringBuilder();
		String msg = new String();
		int no = 0;

		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "M_PriceList_ID");
		if (processMonitor != null)	processMonitor.statusUpdate(msg);

		//Reverse Look up M_PriceList_ID From JP_PriceList_Name
		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "M_PriceList_ID")
		+ " - " + Msg.getMsg(getCtx(), "MatchFrom") + " : " + Msg.getElement(getCtx(), "JP_PriceList_Name") ;
		sql = new StringBuilder ("UPDATE I_BPartnerJP i ")
				.append("SET M_PriceList_ID=(SELECT M_PriceList_ID FROM M_PriceList p")
				.append(" WHERE i.JP_PriceList_Name=p.Name AND p.AD_Client_ID=i.AD_Client_ID ) ")
				.append(" WHERE i.JP_PriceList_Name IS NOT NULL")
				.append(" AND i.I_IsImported='N'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		//Invalid JP_PriceList_Name
		msg = Msg.getMsg(getCtx(), "Invalid")+Msg.getElement(getCtx(), "JP_PriceList_Name");
		sql = new StringBuilder ("UPDATE I_BPartnerJP ")
			.append("SET I_ErrorMsg='"+ msg + "'")
			.append(" WHERE JP_PriceList_Name IS NOT NULL AND M_PriceList_ID IS NULL ")
			.append(" AND I_IsImported<>'Y'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		if(no > 0)
		{
			commitEx();
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg );
		}

	}//reverseLookupM_PriceList_ID


	/**
	 * Reverse Look up PO_PriceList_ID From JP_PO_PriceList_Name
	 *
	 * @throws Exception
	 */
	private void reverseLookupPO_PriceList_ID() throws Exception
	{
		StringBuilder sql = new StringBuilder();
		String msg = new String();
		int no = 0;

		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "PO_PriceList_ID");
		if (processMonitor != null)	processMonitor.statusUpdate(msg);

		//Reverse Look up PO_PriceList_ID From JP_PO_PriceList_Name
		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "PO_PriceList_ID")
		+ " - " + Msg.getMsg(getCtx(), "MatchFrom") + " : " + Msg.getElement(getCtx(), "JP_PO_PriceList_Name") ;
		sql = new StringBuilder ("UPDATE I_BPartnerJP i ")
				.append("SET PO_PriceList_ID=(SELECT M_PriceList_ID FROM M_PriceList p")
				.append(" WHERE i.JP_PO_PriceList_Name=p.Name AND p.AD_Client_ID=i.AD_Client_ID ) ")
				.append(" WHERE i.JP_PO_PriceList_Name IS NOT NULL")
				.append(" AND i.I_IsImported='N'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		//Invalid JP_PO_PriceList_Name
		msg = Msg.getMsg(getCtx(), "Invalid")+Msg.getElement(getCtx(), "JP_PO_PriceList_Name");
		sql = new StringBuilder ("UPDATE I_BPartnerJP ")
			.append("SET I_ErrorMsg='"+ msg + "'")
			.append(" WHERE JP_PO_PriceList_Name IS NOT NULL AND PO_PriceList_ID IS NULL ")
			.append(" AND I_IsImported<>'Y'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		if(no > 0)
		{
			commitEx();
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg );
		}

	}//reverseLookupPO_PriceList_ID


	/**
	 * Reverse Look up M_DiscountSchema_ID From JP_DiscountSchema_Name
	 *
	 * @throws Exception
	 */
	private void reverseLookupM_DiscountSchema_ID() throws Exception
	{
		StringBuilder sql = new StringBuilder();
		String msg = new String();
		int no = 0;

		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "M_DiscountSchema_ID");
		if (processMonitor != null)	processMonitor.statusUpdate(msg);

		//Reverse Look up M_DiscountSchema_ID From JP_DiscountSchema_Name
		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "M_DiscountSchema_ID")
		+ " - " + Msg.getMsg(getCtx(), "MatchFrom") + " : " + Msg.getElement(getCtx(), "JP_DiscountSchema_Name") ;
		sql = new StringBuilder ("UPDATE I_BPartnerJP i ")
				.append("SET M_DiscountSchema_ID=(SELECT M_DiscountSchema_ID FROM M_DiscountSchema p")
				.append(" WHERE i.JP_DiscountSchema_Name=p.Name AND p.AD_Client_ID=i.AD_Client_ID ) ")
				.append(" WHERE i.JP_DiscountSchema_Name IS NOT NULL")
				.append(" AND i.I_IsImported='N'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		//Invalid JP_DiscountSchema_Name
		msg = Msg.getMsg(getCtx(), "Invalid")+Msg.getElement(getCtx(), "JP_DiscountSchema_Name");
		sql = new StringBuilder ("UPDATE I_BPartnerJP ")
			.append("SET I_ErrorMsg='"+ msg + "'")
			.append(" WHERE JP_DiscountSchema_Name IS NOT NULL AND M_DiscountSchema_ID IS NULL ")
			.append(" AND I_IsImported<>'Y'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		if(no > 0)
		{
			commitEx();
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg );
		}

	}//reverseLookupM_DiscountSchema_ID


	/**
	 * Reverse Look up PO_DiscountSchema_ID From JP_PO_DiscountSchema_Name
	 *
	 * @throws Exception
	 */
	private void reverseLookupPO_DiscountSchema_ID() throws Exception
	{
		StringBuilder sql = new StringBuilder();
		String msg = new String();
		int no = 0;

		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "PO_DiscountSchema_ID");
		if (processMonitor != null)	processMonitor.statusUpdate(msg);

		//Reverse Look up PO_DiscountSchema_ID From JP_PO_DiscountSchema_Name
		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "PO_DiscountSchema_ID")
		+ " - " + Msg.getMsg(getCtx(), "MatchFrom") + " : " + Msg.getElement(getCtx(), "JP_PO_DiscountSchema_Name") ;
		sql = new StringBuilder ("UPDATE I_BPartnerJP i ")
				.append("SET PO_DiscountSchema_ID=(SELECT M_DiscountSchema_ID FROM M_DiscountSchema p")
				.append(" WHERE i.JP_PO_DiscountSchema_Name=p.Name AND p.AD_Client_ID=i.AD_Client_ID ) ")
				.append(" WHERE i.JP_PO_DiscountSchema_Name IS NOT NULL")
				.append(" AND i.I_IsImported='N'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		//Invalid JP_DiscountSchema_Name
		msg = Msg.getMsg(getCtx(), "Invalid")+Msg.getElement(getCtx(), "JP_PO_DiscountSchema_Name");
		sql = new StringBuilder ("UPDATE I_BPartnerJP ")
			.append("SET I_ErrorMsg='"+ msg + "'")
			.append(" WHERE JP_PO_DiscountSchema_Name IS NOT NULL AND PO_DiscountSchema_ID IS NULL ")
			.append(" AND I_IsImported<>'Y'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		if(no > 0)
		{
			commitEx();
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg );
		}

	}//reverseLookupPO_DiscountSchema_ID


	/**
	 * Reverse Look up C_Dunning_ID From JP_Dunning_Name
	 *
	 * @throws Exception
	 */
	private void reverseLookupC_Dunning_ID() throws Exception
	{
		StringBuilder sql = new StringBuilder();
		String msg = new String();
		int no = 0;

		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "C_Dunning_ID");
		if (processMonitor != null)	processMonitor.statusUpdate(msg);

		//Reverse Look up C_Dunning_ID From JP_Dunning_Name
		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "C_Dunning_ID")
		+ " - " + Msg.getMsg(getCtx(), "MatchFrom") + " : " + Msg.getElement(getCtx(), "JP_Dunning_Name") ;
		sql = new StringBuilder ("UPDATE I_BPartnerJP i ")
				.append("SET C_Dunning_ID=(SELECT C_Dunning_ID FROM C_Dunning p")
				.append(" WHERE i.JP_Dunning_Name=p.Name AND p.AD_Client_ID=i.AD_Client_ID ) ")
				.append(" WHERE i.JP_Dunning_Name IS NOT NULL")
				.append(" AND i.I_IsImported='N'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		//Invalid JP_Dunning_Name
		msg = Msg.getMsg(getCtx(), "Invalid")+Msg.getElement(getCtx(), "JP_Dunning_Name");
		sql = new StringBuilder ("UPDATE I_BPartnerJP ")
			.append("SET I_ErrorMsg='"+ msg + "'")
			.append(" WHERE JP_Dunning_Name IS NOT NULL AND C_Dunning_ID IS NULL ")
			.append(" AND I_IsImported<>'Y'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		if(no > 0)
		{
			commitEx();
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg );
		}

	}//reverseLookupC_Dunning_ID


	/**
	 * Reverse Look up Default1099Box_ID From JP_Default1099Box_Value
	 *
	 * @throws Exception
	 */
	private void reverseLookupDefault1099Box_ID() throws Exception
	{
		StringBuilder sql = new StringBuilder();
		String msg = new String();
		int no = 0;

		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "Default1099Box_ID");
		if (processMonitor != null)	processMonitor.statusUpdate(msg);

		//Reverse Look up Default1099Box_ID From JP_Default1099Box_Value
		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "Default1099Box_ID")
		+ " - " + Msg.getMsg(getCtx(), "MatchFrom") + " : " + Msg.getElement(getCtx(), "JP_Default1099Box_Value") ;
		sql = new StringBuilder ("UPDATE I_BPartnerJP i ")
				.append("SET Default1099Box_ID=(SELECT C_1099Box_ID FROM C_1099Box p")
				.append(" WHERE i.JP_Default1099Box_Value=p.Value AND p.AD_Client_ID=i.AD_Client_ID ) ")
				.append(" WHERE i.JP_Default1099Box_Value IS NOT NULL")
				.append(" AND i.I_IsImported='N'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		//Invalid JP_Default1099Box_Value
		msg = Msg.getMsg(getCtx(), "Invalid")+Msg.getElement(getCtx(), "JP_Default1099Box_Value");
		sql = new StringBuilder ("UPDATE I_BPartnerJP ")
			.append("SET I_ErrorMsg='"+ msg + "'")
			.append(" WHERE JP_Default1099Box_Value IS NOT NULL AND Default1099Box_ID IS NULL ")
			.append(" AND I_IsImported<>'Y'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		if(no > 0)
		{
			commitEx();
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg );
		}

	}//reverseLookupDefault1099Box_ID


	/**
	 * Reverse Look up C_Greeting_ID From JP_Greeting_Name
	 *
	 * @throws Exception
	 */
	private void reverseLookupC_Greeting_ID() throws Exception
	{
		StringBuilder sql = new StringBuilder();
		String msg = new String();
		int no = 0;

		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "C_Greeting_ID");
		if (processMonitor != null)	processMonitor.statusUpdate(msg);

		//Reverse Look up C_Greeting_ID From JP_SalesRep_Email
		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "C_Greeting_ID")
		+ " - " + Msg.getMsg(getCtx(), "MatchFrom") + " : " + Msg.getElement(getCtx(), "JP_Greeting_Name") ;
		sql = new StringBuilder ("UPDATE I_BPartnerJP i ")
				.append("SET C_Greeting_ID=(SELECT C_Greeting_ID FROM C_Greeting p")
				.append(" WHERE i.JP_Greeting_Name=p.Name AND p.AD_Client_ID=i.AD_Client_ID ) ")
				.append(" WHERE i.JP_Greeting_Name IS NOT NULL")
				.append(" AND i.I_IsImported='N'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		//Invalid JP_Greeting_Name
		msg = Msg.getMsg(getCtx(), "Invalid")+Msg.getElement(getCtx(), "JP_Greeting_Name");
		sql = new StringBuilder ("UPDATE I_BPartnerJP ")
			.append("SET I_ErrorMsg='"+ msg + "'")
			.append(" WHERE JP_Greeting_Name IS NOT NULL AND C_Greeting_ID IS NULL ")
			.append(" AND I_IsImported<>'Y'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no );
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		if(no > 0)
		{
			commitEx();
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg + " : " + no  );
		}

	}//reverseLookupC_Greeting_ID


	/**
	 * Reverse Look up JP_User_Greeting_ID From BPContactGreeting
	 *
	 *
	 * @throws Exception
	 */
	private void reverseLookupJP_User_Greeting_ID() throws Exception
	{
		StringBuilder sql = new StringBuilder();
		String msg = new String();
		int no = 0;

		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "JP_User_Greeting_ID");
		if (processMonitor != null)	processMonitor.statusUpdate(msg);

		//Reverse Look up JP_User_Greeting_ID From BPContactGreeting
		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "JP_User_Greeting_ID")
		+ " - " + Msg.getMsg(getCtx(), "MatchFrom") + " : " + Msg.getElement(getCtx(), "BPContactGreeting") ;
		sql = new StringBuilder ("UPDATE I_BPartnerJP i ")
				.append("SET JP_User_Greeting_ID=(SELECT C_Greeting_ID FROM C_Greeting p")
				.append(" WHERE i.BPContactGreeting=p.Name AND p.AD_Client_ID=i.AD_Client_ID ) ")
				.append(" WHERE i.BPContactGreeting IS NOT NULL")
				.append(" AND i.I_IsImported='N'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		//Invalid BPContactGreeting
		msg = Msg.getMsg(getCtx(), "Invalid")+Msg.getElement(getCtx(), "BPContactGreeting");
		sql = new StringBuilder ("UPDATE I_BPartnerJP ")
			.append("SET I_ErrorMsg='"+ msg + "'")
			.append(" WHERE BPContactGreeting IS NOT NULL AND JP_User_Greeting_ID IS NULL ")
			.append(" AND I_IsImported<>'Y'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no );
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		if(no > 0)
		{
			commitEx();
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg + " : " + no  );
		}

	}//reverseLookupJP_User_Greeting_ID


	/**
	 * Reverse Look up C_Location_ID From JP_Location_Label
	 *
	 * @throws Exception
	 */
	private void reverseLookupC_Location_ID() throws Exception
	{
		StringBuilder sql = new StringBuilder();
		String msg = new String();
		int no = 0;

		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "C_Location_ID");
		if (processMonitor != null)	processMonitor.statusUpdate(msg);

		//Reverse Look up C_Location_ID From JP_Location_Label
		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "C_Location_ID")
		+ " - " + Msg.getMsg(getCtx(), "MatchFrom") + " : " + Msg.getElement(getCtx(), "JP_Location_Label") ;
		sql = new StringBuilder ("UPDATE I_BPartnerJP i ")
				.append("SET C_Location_ID=(SELECT C_Location_ID FROM C_Location p")
				.append(" WHERE i.JP_Location_Label=p.JP_Location_Label AND p.AD_Client_ID=i.AD_Client_ID ) ")
				.append(" WHERE i.JP_Location_Label IS NOT NULL")
				.append(" AND i.I_IsImported='N'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		//Invalid JP_Greeting_Name
		msg = Msg.getMsg(getCtx(), "Invalid")+Msg.getElement(getCtx(), "JP_Location_Label");
		sql = new StringBuilder ("UPDATE I_BPartnerJP ")
			.append("SET I_ErrorMsg='"+ msg + "'")
			.append(" WHERE JP_Location_Label IS NOT NULL AND C_Location_ID IS NULL ")
			.append(" AND I_IsImported<>'Y'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no );
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		if(no > 0)
		{
			commitEx();
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg + " : " + no  );
		}

	}//reverseLookupC_Location_ID

	/**
	 * Reverse Look up C_SalesRegion_ID From JP_SalesRegion_Value
	 *
	 * @throws Exception
	 */
	private void reverseLookupC_SalesRegion_ID() throws Exception
	{
		StringBuilder sql = new StringBuilder();
		String msg = new String();
		int no = 0;

		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "C_SalesRegion_ID");
		if (processMonitor != null)	processMonitor.statusUpdate(msg);

		//Reverse Look up C_SalesRegion_ID From JP_SalesRegion_Value
		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "C_SalesRegion_ID")
		+ " - " + Msg.getMsg(getCtx(), "MatchFrom") + " : " + Msg.getElement(getCtx(), "JP_SalesRegion_Value") ;
		sql = new StringBuilder ("UPDATE I_BPartnerJP i ")
				.append("SET C_SalesRegion_ID=(SELECT C_SalesRegion_ID FROM C_SalesRegion p")
				.append(" WHERE i.JP_SalesRegion_Value=p.Value AND p.AD_Client_ID=i.AD_Client_ID ) ")
				.append(" WHERE i.JP_SalesRegion_Value IS NOT NULL")
				.append(" AND i.I_IsImported='N'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		//Invalid JP_SalesRegion_Value
		msg = Msg.getMsg(getCtx(), "Invalid")+Msg.getElement(getCtx(), "JP_SalesRegion_Value");
		sql = new StringBuilder ("UPDATE I_BPartnerJP ")
			.append("SET I_ErrorMsg='"+ msg + "'")
			.append(" WHERE JP_SalesRegion_Value IS NOT NULL AND C_SalesRegion_ID IS NULL ")
			.append(" AND I_IsImported<>'Y'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no );
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		if(no > 0)
		{
			commitEx();
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg + " : " + no  );
		}

	}//reverseLookupC_SalesRegion_ID


	/**
	 * Reverse Look up User From E-Mail and (Value && Name)
	 * @throws Exception
	 *
	 */
	private void reverseLookupAD_User_ID() throws Exception
	{
		StringBuilder sql = new StringBuilder();
		String msg = new String();
		int no = 0;

		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "AD_User_ID");
		if (processMonitor != null)	processMonitor.statusUpdate(msg);

		//Reverse lookup AD_User_ID From Value && Name
		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "AD_User_ID")
		+ " - " + Msg.getMsg(getCtx(), "MatchFrom") + " : " + Msg.getElement(getCtx(), "JP_User_Value" + " : " + Msg.getElement(getCtx(), "ContactName") );
		sql = new StringBuilder ("UPDATE I_BPartnerJP i ")
				.append("SET AD_User_ID=(SELECT AD_User_ID FROM AD_User p")
				.append(" WHERE i.JP_User_Value=p.Value AND i.ContactName=p.Name AND p.AD_Client_ID=i.AD_Client_ID) ")
				.append(" WHERE i.AD_User_ID IS NULL AND i.ContactName IS NOT NULL")
				.append(" AND i.I_IsImported='N'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
//			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		//Reverse lookup AD_User_ID From E-Mail
		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "AD_User_ID")
		+ " - " + Msg.getMsg(getCtx(), "MatchFrom") + " : " + Msg.getElement(getCtx(), "EMail") ;
		sql = new StringBuilder ("UPDATE I_BPartnerJP i ")
			.append("SET AD_User_ID=(SELECT AD_User_ID FROM AD_User p")
			.append(" WHERE i.EMail=p.EMail AND i.AD_Client_ID=p.AD_Client_ID) ")
			.append(" WHERE i.EMail IS NOT NULL AND i.AD_User_ID IS NULL")
			.append(" AND i.I_IsImported='N'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
//			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}


		//Reverse lookup AD_User_ID From Name AND C_BPartner_ID
		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "AD_User_ID")
		+ " - " + Msg.getMsg(getCtx(), "MatchFrom") + " : " + Msg.getElement(getCtx(), "C_BPartner_ID" + " : " + Msg.getElement(getCtx(), "ContactName") );
		sql = new StringBuilder ("UPDATE I_BPartnerJP i ")
				.append("SET AD_User_ID=(SELECT AD_User_ID FROM AD_User p")
				.append(" WHERE i.ContactName=p.Name AND p.AD_Client_ID=i.AD_Client_ID AND p.C_BPartner_ID=i.C_BPartner_ID) ")
				.append(" WHERE i.AD_User_ID IS NULL AND i.ContactName IS NOT NULL AND i.C_BPartner_ID IS NOT NULL ")
				.append(" AND i.I_IsImported='N'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

	}//reverseLookupAD_User_ID


	/**
	 * Reverse Look up R_InterestArea_ID From InterestAreaName
	 *
	 * @throws Exception
	 */
	private void reverseLookupR_InterestArea_ID() throws Exception
	{
		StringBuilder sql = new StringBuilder();
		String msg = new String();
		int no = 0;

		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "R_InterestArea_ID");
		if (processMonitor != null)	processMonitor.statusUpdate(msg);

		//Reverse Look up R_InterestArea_ID From InterestAreaName
		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "R_InterestArea_ID")
		+ " - " + Msg.getMsg(getCtx(), "MatchFrom") + " : " + Msg.getElement(getCtx(), "InterestAreaName") ;
		sql = new StringBuilder ("UPDATE I_BPartnerJP i ")
				.append("SET R_InterestArea_ID=(SELECT R_InterestArea_ID FROM R_InterestArea p")
				.append(" WHERE i.InterestAreaName=p.Name AND p.AD_Client_ID=i.AD_Client_ID ) ")
				.append(" WHERE i.InterestAreaName IS NOT NULL")
				.append(" AND i.I_IsImported='N'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		//Invalid InterestAreaName
		msg = Msg.getMsg(getCtx(), "Invalid")+Msg.getElement(getCtx(), "InterestAreaName");
		sql = new StringBuilder ("UPDATE I_BPartnerJP ")
			.append("SET I_ErrorMsg='"+ msg + "'")
			.append(" WHERE InterestAreaName IS NOT NULL AND R_InterestArea_ID IS NULL ")
			.append(" AND I_IsImported<>'Y'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no );
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		if(no > 0)
		{
			commitEx();
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg + " : " + no  );
		}

	}//reverseLookupR_InterestArea_ID


	/**
	 * Reverse Look up Invoice_PrintFormat_ID From JP_Invoice_PrintFormat_Name
	 *
	 * @throws Exception
	 */
	private void reverseLookupInvoice_PrintFormat_ID() throws Exception
	{
		StringBuilder sql = new StringBuilder();
		String msg = new String();
		int no = 0;

		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "Invoice_PrintFormat_ID");
		if (processMonitor != null)	processMonitor.statusUpdate(msg);

		//Reverse Look up Invoice_PrintFormat_ID From JP_Invoice_PrintFormat_Name
		msg = Msg.getMsg(getCtx(), "Matching") + " : " + Msg.getElement(getCtx(), "Invoice_PrintFormat_ID")
		+ " - " + Msg.getMsg(getCtx(), "MatchFrom") + " : " + Msg.getElement(getCtx(), "JP_Invoice_PrintFormat_Name") ;
		sql = new StringBuilder ("UPDATE I_BPartnerJP i ")
				.append("SET Invoice_PrintFormat_ID=(SELECT AD_PrintFormat_ID FROM AD_PrintFormat p")
				.append(" WHERE i.JP_Invoice_PrintFormat_Name=p.Name AND (p.AD_Client_ID=i.AD_Client_ID OR p.AD_Client_ID= 0) )")
				.append(" WHERE i.JP_Invoice_PrintFormat_Name IS NOT NULL")
				.append(" AND i.I_IsImported='N'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no);
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		//Invalid InterestAreaName
		msg = Msg.getMsg(getCtx(), "Invalid")+Msg.getElement(getCtx(), "JP_Invoice_PrintFormat_Name");
		sql = new StringBuilder ("UPDATE I_BPartnerJP ")
			.append("SET I_ErrorMsg='"+ msg + "'")
			.append(" WHERE JP_Invoice_PrintFormat_Name IS NOT NULL AND Invoice_PrintFormat_ID IS NULL ")
			.append(" AND I_IsImported<>'Y'").append(getWhereClause());
		try {
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine(msg +"=" + no );
		}catch(Exception e) {
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg +" : " + sql );
		}

		if(no > 0)
		{
			commitEx();
			throw new Exception(Msg.getMsg(getCtx(), "Error") + msg + " : " + no  );
		}

	}//reverseLookupInvoice_PrintFormat_ID


	/**
	 * Create New Business Partner
	 *
	 * @param importBpartner
	 * @throws Exception
	 */
	private void createNewBPartner(X_I_BPartnerJP importBPartner, MBPartner newBPartner) throws Exception
	{
		ModelValidationEngine.get().fireImportValidate(this, importBPartner, newBPartner, ImportValidator.TIMING_BEFORE_IMPORT);

		if(Util.isEmpty(importBPartner.getName()))
		{
			Object[] objs = new Object[]{Msg.getElement(Env.getCtx(), "Name")};
			importBPartner.setI_ErrorMsg(Msg.getMsg(getCtx(), "Error") + Msg.getMsg(Env.getCtx(),"JP_Mandatory",objs));
			importBPartner.setI_IsImported(false);
			importBPartner.setProcessed(false);
			importBPartner.saveEx(get_TrxName());
			return ;
		}


		if(importBPartner.getC_BP_Group_ID() == 0)
		{
			Object[] objs = new Object[]{Msg.getElement(Env.getCtx(), "C_BP_Group_ID")};
			importBPartner.setI_ErrorMsg(Msg.getMsg(getCtx(), "Error") + Msg.getMsg(Env.getCtx(),"JP_Mandatory",objs));
			importBPartner.setI_IsImported(false);
			importBPartner.setProcessed(false);
			importBPartner.saveEx(get_TrxName());
			return ;
		}

		PO.copyValues(importBPartner, newBPartner);
		newBPartner.setIsActive(importBPartner.isI_IsActiveJP());

		ModelValidationEngine.get().fireImportValidate(this, importBPartner, newBPartner, ImportValidator.TIMING_AFTER_IMPORT);

		newBPartner.saveEx(get_TrxName());

		String processLocationMsg = null;
		if(!Util.isEmpty(importBPartner.getJP_BPartner_Location_Name()) || !Util.isEmpty(importBPartner.getJP_Location_Label()) )
		{
			int C_BPartner_Location_ID = createBPartnerLocation(importBPartner);
			if(C_BPartner_Location_ID == 0)
			{
				processLocationMsg = Msg.getMsg(getCtx(), "JP_CouldNotCreate") + " : " + Msg.getElement(getCtx(), "C_BPartner_Location_ID");

			}else {
				importBPartner.setC_BPartner_Location_ID(C_BPartner_Location_ID);
			}
		}

		String processUserMsg = null;
		if(importBPartner.getAD_User_ID() > 0 )
		{
			updateUser(importBPartner, newBPartner.getC_BPartner_ID());

		}else if(!Util.isEmpty(importBPartner.getContactName())) {

			int AD_User_ID = createNewUser(importBPartner, newBPartner.getC_BPartner_ID());
			if(AD_User_ID == 0)
			{
				processUserMsg = Msg.getMsg(getCtx(), "JP_CouldNotCreate") + " : " + Msg.getElement(getCtx(), "AD_User_ID");

			}else {
				importBPartner.setAD_User_ID(AD_User_ID);
			}

		}

		importBPartner.setC_BPartner_ID(newBPartner.getC_BPartner_ID());
		StringBuilder msg = new StringBuilder(Msg.getMsg(getCtx(), "NewRecord"));
		if(!Util.isEmpty(processLocationMsg))
			msg.append(" -> ").append(processLocationMsg);
		if(!Util.isEmpty(processUserMsg))
			msg.append(" -> ").append(processUserMsg);
		importBPartner.setI_ErrorMsg(msg.toString());
		importBPartner.setI_IsImported(true);
		importBPartner.setProcessed(true);
		importBPartner.saveEx(get_TrxName());
	}


	/**
	 * Update Business Partner
	 *
	 * @param importBPartner
	 * @param updateBPartner
	 * @throws Exception
	 */
	private void updateBPartner(X_I_BPartnerJP importBPartner, MBPartner updateBPartner) throws Exception
	{

		ModelValidationEngine.get().fireImportValidate(this, importBPartner, updateBPartner, ImportValidator.TIMING_BEFORE_IMPORT);

		//Update Business Partner
		MTable C_BPartner_Table = MTable.get(getCtx(), MBPartner.Table_ID, get_TrxName());
		MColumn[] C_BPartner_Columns = C_BPartner_Table.getColumns(true);

		MTable I_BPartnerJP_Table = MTable.get(getCtx(), X_I_BPartnerJP.Table_ID, get_TrxName());
		MColumn[] I_BPartnerJP_Columns = I_BPartnerJP_Table.getColumns(true);

		MColumn i_Column = null;
		for(int i = 0 ; i < C_BPartner_Columns.length; i++)
		{
			i_Column = C_BPartner_Columns[i];
			if(i_Column.isVirtualColumn() || i_Column.isKey() || i_Column.isUUIDColumn())
				continue;//i

			if(i_Column.getColumnName().equals("IsActive")
				|| i_Column.getColumnName().equals("AD_Client_ID")
				|| i_Column.getColumnName().equals("Value")
				|| i_Column.getColumnName().equals("Processing")
				|| i_Column.getColumnName().equals("Created")
				|| i_Column.getColumnName().equals("CreatedBy")
				|| i_Column.getColumnName().equals("Updated")
				|| i_Column.getColumnName().equals("UpdatedBy") )
				continue;//i

			MColumn j_Column = null;
			Object importValue = null;
			for(int j = 0 ; j < I_BPartnerJP_Columns.length; j++)
			{
				j_Column = I_BPartnerJP_Columns[j];

				if(i_Column.getColumnName().equals(j_Column.getColumnName()))
				{
					importValue = importBPartner.get_Value(j_Column.getColumnName());

					if(importValue == null )
					{
						break;//j

					}else if(importValue instanceof BigDecimal) {

						BigDecimal bigDecimal_Value = (BigDecimal)importValue;
						if(bigDecimal_Value.compareTo(Env.ZERO) == 0)
							break;

					}else if(j_Column.getAD_Reference_ID()==DisplayType.String) {

						String string_Value = (String)importValue;
						if(!Util.isEmpty(string_Value))
						{
							updateBPartner.set_ValueNoCheck(i_Column.getColumnName(), importValue);
						}

						break;

					}else if(j_Column.getColumnName().endsWith("_ID")) {

						Integer p_key = (Integer)importValue;
						if(p_key.intValue() <= 0)
							break;
					}

					if(importValue != null)
						updateBPartner.set_ValueNoCheck(i_Column.getColumnName(), importValue);

					break;
				}
			}//for j

		}//for i

		updateBPartner.setIsActive(importBPartner.isI_IsActiveJP());
		ModelValidationEngine.get().fireImportValidate(this, importBPartner, updateBPartner, ImportValidator.TIMING_AFTER_IMPORT);

		updateBPartner.saveEx(get_TrxName());

		//Business Partner Location
		String processLocationMsg = null;
		if(!Util.isEmpty(importBPartner.getJP_BPartner_Location_Name()))
		{
			MBPartnerLocation bpLocation = getMBPartnerLocation(importBPartner.getC_BPartner_ID(), importBPartner.getJP_BPartner_Location_Name());
			if(bpLocation == null)
			{
				int C_BPartner_Location_ID = createBPartnerLocation(importBPartner);
				if(C_BPartner_Location_ID == 0)
				{
					processLocationMsg = Msg.getMsg(getCtx(), "JP_CouldNotCreate") + " : " + Msg.getElement(getCtx(), "C_BPartner_Location_ID");

				}else {
					importBPartner.setC_BPartner_Location_ID(C_BPartner_Location_ID);
				}

			}else {
				updateBPartnerLocation(importBPartner,bpLocation);
			}
		}

		//User
		String processUserMsg = null;
		if(importBPartner.getAD_User_ID() > 0 )
		{
			updateUser(importBPartner, updateBPartner.getC_BPartner_ID());

		}else if(!Util.isEmpty(importBPartner.getContactName())) {

			MUser user = getMUser(importBPartner);
			if(user == null)
			{
				int AD_User_ID = createNewUser(importBPartner, updateBPartner.getC_BPartner_ID());
				if(AD_User_ID == 0)
				{
					processUserMsg = Msg.getMsg(getCtx(), "JP_CouldNotCreate") + " : " + Msg.getElement(getCtx(), "AD_User_ID");

				}else {
					importBPartner.setAD_User_ID(AD_User_ID);
				}
			}else {
				updateUser(importBPartner, updateBPartner.getC_BPartner_ID());
			}

		}

		StringBuilder msg = new StringBuilder(Msg.getMsg(getCtx(), "Update"));
		if(!Util.isEmpty(processLocationMsg))
			msg.append(" -> ").append(processLocationMsg);
		if(!Util.isEmpty(processUserMsg))
			msg.append(" -> ").append(processUserMsg);
		importBPartner.setI_ErrorMsg(msg.toString());
		importBPartner.setI_IsImported(true);
		importBPartner.setProcessed(true);
		importBPartner.saveEx(get_TrxName());
		commitEx();

	}


	/**
	 * Create Business Partner Location
	 *
	 * @param importBPartner
	 * @return
	 * @throws Exception
	 */
	private int createBPartnerLocation(X_I_BPartnerJP importBPartner) throws Exception
	{
		MBPartnerLocation newBPartnerLocation = new MBPartnerLocation(getCtx(), 0, get_TrxName());
		ModelValidationEngine.get().fireImportValidate(this, importBPartner, newBPartnerLocation, ImportValidator.TIMING_BEFORE_IMPORT);

		PO.copyValues(importBPartner, newBPartnerLocation);
		newBPartnerLocation.setC_BPartner_ID(importBPartner.getC_BPartner_ID());
		if(!Util.isEmpty(importBPartner.getJP_BPartner_Location_Name()))
			newBPartnerLocation.setName(importBPartner.getJP_BPartner_Location_Name());
		newBPartnerLocation.setIsActive(importBPartner.isI_IsActiveJP());

		//Location
		if(importBPartner.getC_Location_ID() > 0)
		{
			newBPartnerLocation.setC_Location_ID(importBPartner.getC_Location_ID());

		}else {
			int C_Location_ID = JPiereLocationUtil.createLocation(
					getCtx()
					,0
					,importBPartner.getJP_Location_Label()
					,importBPartner.getComments()
					,importBPartner.getCountryCode()
					,importBPartner.getPostal()
					,importBPartner.getPostal_Add()
					,importBPartner.getRegionName()
					,importBPartner.getCity()
					,importBPartner.getAddress1()
					,importBPartner.getAddress2()
					,importBPartner.getAddress3()
					,importBPartner.getAddress4()
					,importBPartner.getAddress5()
					,get_TrxName() );
			newBPartnerLocation.setC_Location_ID(C_Location_ID);
		}

		ModelValidationEngine.get().fireImportValidate(this, importBPartner, newBPartnerLocation, ImportValidator.TIMING_AFTER_IMPORT);

		newBPartnerLocation.saveEx(get_TrxName());

		return newBPartnerLocation.getC_BPartner_Location_ID();
	}

	/**
	 *
	 * Update Business Partner Location
	 *
	 * @param importBPartner
	 * @throws Exception
	 */
	private void updateBPartnerLocation(X_I_BPartnerJP importBPartner,MBPartnerLocation updateBPartnerLocation) throws Exception
	{
		ModelValidationEngine.get().fireImportValidate(this, importBPartner, updateBPartnerLocation, ImportValidator.TIMING_BEFORE_IMPORT);

		//Update Business Partner
		MTable C_BPartner_Location_Table = MTable.get(getCtx(), MBPartnerLocation.Table_ID, get_TrxName());
		MColumn[] C_BPartner_Location_Columns = C_BPartner_Location_Table.getColumns(true);

		MTable I_BPartnerJP_Table = MTable.get(getCtx(), X_I_BPartnerJP.Table_ID, get_TrxName());
		MColumn[] I_BPartnerJP_Columns = I_BPartnerJP_Table.getColumns(true);

		MColumn i_Column = null;
		for(int i = 0 ; i < C_BPartner_Location_Columns.length; i++)
		{
			i_Column = C_BPartner_Location_Columns[i];
			if(i_Column.isVirtualColumn() || i_Column.isKey() || i_Column.isUUIDColumn())
				continue;//i

			if(i_Column.getColumnName().equals("IsActive")
				|| i_Column.getColumnName().equals("AD_Client_ID")
				|| i_Column.getColumnName().equals("Value")
				|| i_Column.getColumnName().equals("Name")
				|| i_Column.getColumnName().equals("Processing")
				|| i_Column.getColumnName().equals("Created")
				|| i_Column.getColumnName().equals("CreatedBy")
				|| i_Column.getColumnName().equals("Updated")
				|| i_Column.getColumnName().equals("UpdatedBy") )
				continue;//i

			MColumn j_Column = null;
			Object importValue = null;
			for(int j = 0 ; j < I_BPartnerJP_Columns.length; j++)
			{
				j_Column = I_BPartnerJP_Columns[j];

				if(i_Column.getColumnName().equals(j_Column.getColumnName()))
				{
					importValue = importBPartner.get_Value(j_Column.getColumnName());

					if(importValue == null )
					{
						break;//j

					}else if(importValue instanceof BigDecimal) {

						BigDecimal bigDecimal_Value = (BigDecimal)importValue;
						if(bigDecimal_Value.compareTo(Env.ZERO) == 0)
							break;

					}else if(j_Column.getAD_Reference_ID()==DisplayType.String) {

						String string_Value = (String)importValue;
						if(!Util.isEmpty(string_Value))
						{
							updateBPartnerLocation.set_ValueNoCheck(i_Column.getColumnName(), importValue);
						}

						break;

					}else if(j_Column.getColumnName().endsWith("_ID")) {

						Integer p_key = (Integer)importValue;
						if(p_key.intValue() <= 0)
							break;
					}

					updateBPartnerLocation.set_ValueNoCheck(i_Column.getColumnName(), importValue);
					break;
				}
			}//for j

		}//for i

		updateBPartnerLocation.setIsActive(importBPartner.isI_IsActiveJP());
		ModelValidationEngine.get().fireImportValidate(this, importBPartner, updateBPartnerLocation, ImportValidator.TIMING_AFTER_IMPORT);

		updateBPartnerLocation.saveEx(get_TrxName());

	}

	/**
	 * Craete User
	 *
	 * @param importBPartner
	 * @param C_BPartner_ID
	 * @return
	 */
	private int createNewUser(X_I_BPartnerJP importBPartner, int C_BPartner_ID)
	{
		MUser user = new MUser(getCtx(), 0, get_TrxName());

		ModelValidationEngine.get().fireImportValidate(this, importBPartner, user, ImportValidator.TIMING_BEFORE_IMPORT);

		user.setName(importBPartner.getContactName());
		user.setC_BPartner_ID(C_BPartner_ID);
		if(importBPartner.getJP_Corporation_ID() > 0)
			user.set_ValueNoCheck("JP_Corporation_ID", importBPartner.getJP_Corporation_ID());

		if(!Util.isEmpty(importBPartner.getJP_User_Value()))
			user.setValue(importBPartner.getJP_User_Value());

		if(!Util.isEmpty(importBPartner.getEMail()))
			user.setEMail(importBPartner.getEMail());

		if(!Util.isEmpty(importBPartner.getContactDescription()))
			user.setDescription(importBPartner.getContactDescription());

		if(!Util.isEmpty(importBPartner.getComments()))
			user.setComments(importBPartner.getComments());

		if(!Util.isEmpty(importBPartner.getJP_User_Phone()))
			user.setPhone(importBPartner.getJP_User_Phone());

		if(!Util.isEmpty(importBPartner.getJP_User_Phone2()))
			user.setPhone2(importBPartner.getJP_User_Phone2());

		if(importBPartner.getJP_User_Greeting_ID() > 0)
			user.setC_Greeting_ID(importBPartner.getJP_User_Greeting_ID());

		if(!Util.isEmpty(importBPartner.getTitle()))
			user.setTitle(importBPartner.getTitle());

		if(importBPartner.getBirthday() != null)
			user.setBirthday(importBPartner.getBirthday() );

		if(importBPartner.getPassword() != null)
			user.setPassword(importBPartner.getPassword() );

		ModelValidationEngine.get().fireImportValidate(this, importBPartner, user, ImportValidator.TIMING_AFTER_IMPORT);

		user.saveEx(get_TrxName());

		if(importBPartner.getR_InterestArea_ID() > 0)
			createContactInterest(importBPartner,user.getAD_User_ID());

		return user.getAD_User_ID();
	}

	/**
	 * Update User
	 *
	 * @param importBPartner
	 * @param C_BPartner_ID
	 */
	private void updateUser(X_I_BPartnerJP importBPartner, int C_BPartner_ID)
	{
		MUser user = new MUser(getCtx(), importBPartner.getAD_User_ID(), get_TrxName());

		ModelValidationEngine.get().fireImportValidate(this, importBPartner, user, ImportValidator.TIMING_BEFORE_IMPORT);

		if(user.getC_BPartner_ID() ==  0)
		{
			user.setC_BPartner_ID(C_BPartner_ID);
			if(importBPartner.getJP_Corporation_ID() > 0)
			{
				user.set_ValueNoCheck("JP_Corporation_ID", importBPartner.getJP_Corporation_ID());
			}
		}

		if(!Util.isEmpty(importBPartner.getContactDescription()))
			user.setDescription(importBPartner.getContactDescription());

		if(!Util.isEmpty(importBPartner.getComments()))
			user.setComments(importBPartner.getComments());

		if(!Util.isEmpty(importBPartner.getJP_User_Phone()))
			user.setPhone(importBPartner.getJP_User_Phone());

		if(!Util.isEmpty(importBPartner.getJP_User_Phone2()))
			user.setPhone2(importBPartner.getJP_User_Phone2());

		if(importBPartner.getJP_User_Greeting_ID() > 0)
			user.setC_Greeting_ID(importBPartner.getJP_User_Greeting_ID());

		if(!Util.isEmpty(importBPartner.getTitle()))
			user.setTitle(importBPartner.getTitle());

		if(importBPartner.getBirthday() != null)
			user.setBirthday(importBPartner.getBirthday() );

		if(importBPartner.getPassword() != null)
			user.setPassword(importBPartner.getPassword() );

		ModelValidationEngine.get().fireImportValidate(this, importBPartner, user, ImportValidator.TIMING_AFTER_IMPORT);

		user.saveEx(get_TrxName());


		if(importBPartner.getR_InterestArea_ID() > 0)
			createContactInterest(importBPartner,user.getAD_User_ID());
	}

	/**
	 * Create Contact Interest
	 *
	 * @param importBPartner
	 * @param AD_User_ID
	 */
	private void createContactInterest(X_I_BPartnerJP importBPartner, int AD_User_ID)
	{
		MContactInterest  ci = MContactInterest.get(getCtx(), importBPartner.getR_InterestArea_ID(), AD_User_ID, true, get_TrxName());

		ModelValidationEngine.get().fireImportValidate(this, importBPartner, ci, ImportValidator.TIMING_BEFORE_IMPORT);

		ModelValidationEngine.get().fireImportValidate(this, importBPartner, ci, ImportValidator.TIMING_AFTER_IMPORT);

		ci.saveEx(get_TrxName());
	}

	/**
	 * Get MBPartnerLocation
	 *
	 * @param C_BPartner_ID
	 * @param JP_BPartner_Location_Name
	 * @return
	 * @throws Exception
	 */
	private MBPartnerLocation getMBPartnerLocation(int C_BPartner_ID, String JP_BPartner_Location_Name) throws Exception
	{

		if(C_BPartner_ID==0 || Util.isEmpty(JP_BPartner_Location_Name))
			return null;

		MBPartnerLocation[] bpLocations = MBPartnerLocation.getForBPartner(getCtx(),C_BPartner_ID,get_TrxName());
		for(int i = 0 ; i < bpLocations.length; i++)
		{
			if(bpLocations[i].getName().equals(JP_BPartner_Location_Name))
				return bpLocations[i];
		}

		return null;

	}//getMBPartnerLocation


	/**
	 * Get MUser
	 *
	 * @param importBPartner
	 * @return
	 */
	private MUser getMUser(X_I_BPartnerJP importBPartner)
	{
		int C_BPartner_ID = importBPartner.getC_BPartner_ID();
		if(C_BPartner_ID == 0)
			return null;

		MUser[] users = MUser.getOfBPartner(getCtx(), C_BPartner_ID, get_TrxName());
		for(int i = 0; i < users.length; i++)
		{
			if(importBPartner.getContactName().equals(users[i].getName()))
			{
				if(importBPartner.getEMail().equals(users[i].getEMail()))
				{
					return users[i];
				}

				if(importBPartner.getJP_User_Value().equals(users[i].getValue()))
				{
					return users[i];
				}

			}
		}//For

		return null;
	}


}	//	ImportBPartner
