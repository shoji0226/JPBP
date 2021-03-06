/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2012 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package jpiere.base.plugin.org.adempiere.model;

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Model for JP_InvValProfile
 *  @author iDempiere (generated) 
 *  @version Release 6.2 - $Id$ */
public class X_JP_InvValProfile extends PO implements I_JP_InvValProfile, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20190427L;

    /** Standard Constructor */
    public X_JP_InvValProfile (Properties ctx, int JP_InvValProfile_ID, String trxName)
    {
      super (ctx, JP_InvValProfile_ID, trxName);
      /** if (JP_InvValProfile_ID == 0)
        {
			setC_ConversionType_ID (0);
			setC_Currency_ID (0);
			setCostingLevel (null);
// C
			setCostingMethod (null);
			setIsDefault (false);
// N
			setIsZeroStockInvValJP (false);
// N
			setJP_InvValProfile_ID (0);
			setName (null);
			setValue (null);
        } */
    }

    /** Load Constructor */
    public X_JP_InvValProfile (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 3 - Client - Org 
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuffer sb = new StringBuffer ("X_JP_InvValProfile[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_C_ElementValue getAccount() throws RuntimeException
    {
		return (org.compiere.model.I_C_ElementValue)MTable.get(getCtx(), org.compiere.model.I_C_ElementValue.Table_Name)
			.getPO(getAccount_ID(), get_TrxName());	}

	/** Set Account.
		@param Account_ID 
		Account used
	  */
	public void setAccount_ID (int Account_ID)
	{
		if (Account_ID < 1) 
			set_Value (COLUMNNAME_Account_ID, null);
		else 
			set_Value (COLUMNNAME_Account_ID, Integer.valueOf(Account_ID));
	}

	/** Get Account.
		@return Account used
	  */
	public int getAccount_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Account_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_AcctSchema getC_AcctSchema() throws RuntimeException
    {
		return (org.compiere.model.I_C_AcctSchema)MTable.get(getCtx(), org.compiere.model.I_C_AcctSchema.Table_Name)
			.getPO(getC_AcctSchema_ID(), get_TrxName());	}

	/** Set Accounting Schema.
		@param C_AcctSchema_ID 
		Rules for accounting
	  */
	public void setC_AcctSchema_ID (int C_AcctSchema_ID)
	{
		if (C_AcctSchema_ID < 1) 
			set_Value (COLUMNNAME_C_AcctSchema_ID, null);
		else 
			set_Value (COLUMNNAME_C_AcctSchema_ID, Integer.valueOf(C_AcctSchema_ID));
	}

	/** Get Accounting Schema.
		@return Rules for accounting
	  */
	public int getC_AcctSchema_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_AcctSchema_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_ConversionType getC_ConversionType() throws RuntimeException
    {
		return (org.compiere.model.I_C_ConversionType)MTable.get(getCtx(), org.compiere.model.I_C_ConversionType.Table_Name)
			.getPO(getC_ConversionType_ID(), get_TrxName());	}

	/** Set Currency Type.
		@param C_ConversionType_ID 
		Currency Conversion Rate Type
	  */
	public void setC_ConversionType_ID (int C_ConversionType_ID)
	{
		if (C_ConversionType_ID < 1) 
			set_Value (COLUMNNAME_C_ConversionType_ID, null);
		else 
			set_Value (COLUMNNAME_C_ConversionType_ID, Integer.valueOf(C_ConversionType_ID));
	}

	/** Get Currency Type.
		@return Currency Conversion Rate Type
	  */
	public int getC_ConversionType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_ConversionType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_Currency getC_Currency() throws RuntimeException
    {
		return (org.compiere.model.I_C_Currency)MTable.get(getCtx(), org.compiere.model.I_C_Currency.Table_Name)
			.getPO(getC_Currency_ID(), get_TrxName());	}

	/** Set Currency.
		@param C_Currency_ID 
		The Currency for this record
	  */
	public void setC_Currency_ID (int C_Currency_ID)
	{
		if (C_Currency_ID < 1) 
			set_Value (COLUMNNAME_C_Currency_ID, null);
		else 
			set_Value (COLUMNNAME_C_Currency_ID, Integer.valueOf(C_Currency_ID));
	}

	/** Get Currency.
		@return The Currency for this record
	  */
	public int getC_Currency_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Currency_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_DocType getC_DocType() throws RuntimeException
    {
		return (org.compiere.model.I_C_DocType)MTable.get(getCtx(), org.compiere.model.I_C_DocType.Table_Name)
			.getPO(getC_DocType_ID(), get_TrxName());	}

	/** Set Document Type.
		@param C_DocType_ID 
		Document type or rules
	  */
	public void setC_DocType_ID (int C_DocType_ID)
	{
		if (C_DocType_ID < 0) 
			set_Value (COLUMNNAME_C_DocType_ID, null);
		else 
			set_Value (COLUMNNAME_C_DocType_ID, Integer.valueOf(C_DocType_ID));
	}

	/** Get Document Type.
		@return Document type or rules
	  */
	public int getC_DocType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_DocType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** CostingLevel AD_Reference_ID=355 */
	public static final int COSTINGLEVEL_AD_Reference_ID=355;
	/** Client = C */
	public static final String COSTINGLEVEL_Client = "C";
	/** Organization = O */
	public static final String COSTINGLEVEL_Organization = "O";
	/** Batch/Lot = B */
	public static final String COSTINGLEVEL_BatchLot = "B";
	/** Set Costing Level.
		@param CostingLevel 
		The lowest level to accumulate Costing Information
	  */
	public void setCostingLevel (String CostingLevel)
	{

		set_Value (COLUMNNAME_CostingLevel, CostingLevel);
	}

	/** Get Costing Level.
		@return The lowest level to accumulate Costing Information
	  */
	public String getCostingLevel () 
	{
		return (String)get_Value(COLUMNNAME_CostingLevel);
	}

	/** Average PO = A */
	public static final String COSTINGMETHOD_AveragePO = "A";
	/** Fifo = F */
	public static final String COSTINGMETHOD_Fifo = "F";
	/** Average Invoice = I */
	public static final String COSTINGMETHOD_AverageInvoice = "I";
	/** Lifo = L */
	public static final String COSTINGMETHOD_Lifo = "L";
	/** Standard Costing = S */
	public static final String COSTINGMETHOD_StandardCosting = "S";
	/** User Defined = U */
	public static final String COSTINGMETHOD_UserDefined = "U";
	/** Last Invoice = i */
	public static final String COSTINGMETHOD_LastInvoice = "i";
	/** Last PO Price = p */
	public static final String COSTINGMETHOD_LastPOPrice = "p";
	/** _ = x */
	public static final String COSTINGMETHOD__ = "x";
	/** Retail Inventory method = R */
	public static final String COSTINGMETHOD_RetailInventoryMethod = "R";
	/** Set Costing Method.
		@param CostingMethod 
		Indicates how Costs will be calculated
	  */
	public void setCostingMethod (String CostingMethod)
	{

		set_Value (COLUMNNAME_CostingMethod, CostingMethod);
	}

	/** Get Costing Method.
		@return Indicates how Costs will be calculated
	  */
	public String getCostingMethod () 
	{
		return (String)get_Value(COLUMNNAME_CostingMethod);
	}

	/** Set Description.
		@param Description 
		Optional short description of the record
	  */
	public void setDescription (String Description)
	{
		set_Value (COLUMNNAME_Description, Description);
	}

	/** Get Description.
		@return Optional short description of the record
	  */
	public String getDescription () 
	{
		return (String)get_Value(COLUMNNAME_Description);
	}

	/** DocAction AD_Reference_ID=135 */
	public static final int DOCACTION_AD_Reference_ID=135;
	/** Complete = CO */
	public static final String DOCACTION_Complete = "CO";
	/** Approve = AP */
	public static final String DOCACTION_Approve = "AP";
	/** Reject = RJ */
	public static final String DOCACTION_Reject = "RJ";
	/** Post = PO */
	public static final String DOCACTION_Post = "PO";
	/** Void = VO */
	public static final String DOCACTION_Void = "VO";
	/** Close = CL */
	public static final String DOCACTION_Close = "CL";
	/** Reverse - Correct = RC */
	public static final String DOCACTION_Reverse_Correct = "RC";
	/** Reverse - Accrual = RA */
	public static final String DOCACTION_Reverse_Accrual = "RA";
	/** Invalidate = IN */
	public static final String DOCACTION_Invalidate = "IN";
	/** Re-activate = RE */
	public static final String DOCACTION_Re_Activate = "RE";
	/** <None> = -- */
	public static final String DOCACTION_None = "--";
	/** Prepare = PR */
	public static final String DOCACTION_Prepare = "PR";
	/** Unlock = XL */
	public static final String DOCACTION_Unlock = "XL";
	/** Wait Complete = WC */
	public static final String DOCACTION_WaitComplete = "WC";
	/** Set Document Action.
		@param DocAction 
		The targeted status of the document
	  */
	public void setDocAction (String DocAction)
	{

		set_Value (COLUMNNAME_DocAction, DocAction);
	}

	/** Get Document Action.
		@return The targeted status of the document
	  */
	public String getDocAction () 
	{
		return (String)get_Value(COLUMNNAME_DocAction);
	}

	/** Set Default.
		@param IsDefault 
		Default value
	  */
	public void setIsDefault (boolean IsDefault)
	{
		set_Value (COLUMNNAME_IsDefault, Boolean.valueOf(IsDefault));
	}

	/** Get Default.
		@return Default value
	  */
	public boolean isDefault () 
	{
		Object oo = get_Value(COLUMNNAME_IsDefault);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Do Inventory Valuation when 0 stock.
		@param IsZeroStockInvValJP Do Inventory Valuation when 0 stock	  */
	public void setIsZeroStockInvValJP (boolean IsZeroStockInvValJP)
	{
		set_Value (COLUMNNAME_IsZeroStockInvValJP, Boolean.valueOf(IsZeroStockInvValJP));
	}

	/** Get Do Inventory Valuation when 0 stock.
		@return Do Inventory Valuation when 0 stock	  */
	public boolean isZeroStockInvValJP () 
	{
		Object oo = get_Value(COLUMNNAME_IsZeroStockInvValJP);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Purchase Order = PO */
	public static final String JP_APPLYAMTLIST_PurchaseOrder = "PO";
	/** Invoice(Vendor) = PI */
	public static final String JP_APPLYAMTLIST_InvoiceVendor = "PI";
	/** Set Apply Amt List.
		@param JP_ApplyAmtList Apply Amt List	  */
	public void setJP_ApplyAmtList (String JP_ApplyAmtList)
	{

		set_Value (COLUMNNAME_JP_ApplyAmtList, JP_ApplyAmtList);
	}

	/** Get Apply Amt List.
		@return Apply Amt List	  */
	public String getJP_ApplyAmtList () 
	{
		return (String)get_Value(COLUMNNAME_JP_ApplyAmtList);
	}

	/** Set GL Journal Create Class.
		@param JP_GLJournalCreateClass GL Journal Create Class	  */
	public void setJP_GLJournalCreateClass (String JP_GLJournalCreateClass)
	{
		set_Value (COLUMNNAME_JP_GLJournalCreateClass, JP_GLJournalCreateClass);
	}

	/** Get GL Journal Create Class.
		@return GL Journal Create Class	  */
	public String getJP_GLJournalCreateClass () 
	{
		return (String)get_Value(COLUMNNAME_JP_GLJournalCreateClass);
	}

	/** Set Inv Val Adjust Calculation Class.
		@param JP_InvValAdjustCalClass Inv Val Adjust Calculation Class	  */
	public void setJP_InvValAdjustCalClass (String JP_InvValAdjustCalClass)
	{
		set_Value (COLUMNNAME_JP_InvValAdjustCalClass, JP_InvValAdjustCalClass);
	}

	/** Get Inv Val Adjust Calculation Class.
		@return Inv Val Adjust Calculation Class	  */
	public String getJP_InvValAdjustCalClass () 
	{
		return (String)get_Value(COLUMNNAME_JP_InvValAdjustCalClass);
	}

	/** Set Inv Val Adjust Line Create Class.
		@param JP_InvValAdjustLineClass Inv Val Adjust Line Create Class	  */
	public void setJP_InvValAdjustLineClass (String JP_InvValAdjustLineClass)
	{
		set_Value (COLUMNNAME_JP_InvValAdjustLineClass, JP_InvValAdjustLineClass);
	}

	/** Get Inv Val Adjust Line Create Class.
		@return Inv Val Adjust Line Create Class	  */
	public String getJP_InvValAdjustLineClass () 
	{
		return (String)get_Value(COLUMNNAME_JP_InvValAdjustLineClass);
	}

	/** Set Inventory Valuation Calculation Class.
		@param JP_InvValCalClass Inventory Valuation Calculation Class	  */
	public void setJP_InvValCalClass (String JP_InvValCalClass)
	{
		set_Value (COLUMNNAME_JP_InvValCalClass, JP_InvValCalClass);
	}

	/** Get Inventory Valuation Calculation Class.
		@return Inventory Valuation Calculation Class	  */
	public String getJP_InvValCalClass () 
	{
		return (String)get_Value(COLUMNNAME_JP_InvValCalClass);
	}

	/** Set Inv Val Cal Line Create Class.
		@param JP_InvValCalLineClass Inv Val Cal Line Create Class	  */
	public void setJP_InvValCalLineClass (String JP_InvValCalLineClass)
	{
		set_Value (COLUMNNAME_JP_InvValCalLineClass, JP_InvValCalLineClass);
	}

	/** Get Inv Val Cal Line Create Class.
		@return Inv Val Cal Line Create Class	  */
	public String getJP_InvValCalLineClass () 
	{
		return (String)get_Value(COLUMNNAME_JP_InvValCalLineClass);
	}

	/** Set Inventory Valuation Profile.
		@param JP_InvValProfile_ID Inventory Valuation Profile	  */
	public void setJP_InvValProfile_ID (int JP_InvValProfile_ID)
	{
		if (JP_InvValProfile_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_JP_InvValProfile_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_JP_InvValProfile_ID, Integer.valueOf(JP_InvValProfile_ID));
	}

	/** Get Inventory Valuation Profile.
		@return Inventory Valuation Profile	  */
	public int getJP_InvValProfile_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_JP_InvValProfile_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set JP_InvValProfile_UU.
		@param JP_InvValProfile_UU JP_InvValProfile_UU	  */
	public void setJP_InvValProfile_UU (String JP_InvValProfile_UU)
	{
		set_ValueNoCheck (COLUMNNAME_JP_InvValProfile_UU, JP_InvValProfile_UU);
	}

	/** Get JP_InvValProfile_UU.
		@return JP_InvValProfile_UU	  */
	public String getJP_InvValProfile_UU () 
	{
		return (String)get_Value(COLUMNNAME_JP_InvValProfile_UU);
	}

	/** Set Product Cost Update Class.
		@param JP_InvValUpdateCostClass Product Cost Update Class	  */
	public void setJP_InvValUpdateCostClass (String JP_InvValUpdateCostClass)
	{
		set_Value (COLUMNNAME_JP_InvValUpdateCostClass, JP_InvValUpdateCostClass);
	}

	/** Get Product Cost Update Class.
		@return Product Cost Update Class	  */
	public String getJP_InvValUpdateCostClass () 
	{
		return (String)get_Value(COLUMNNAME_JP_InvValUpdateCostClass);
	}

	/** Gross Average = GA */
	public static final String JP_TYPEOFAVERAGECOST_GrossAverage = "GA";
	/** Periodic Average = PA */
	public static final String JP_TYPEOFAVERAGECOST_PeriodicAverage = "PA";
	/** Set Type of Average Cost.
		@param JP_TypeOfAverageCost Type of Average Cost	  */
	public void setJP_TypeOfAverageCost (String JP_TypeOfAverageCost)
	{

		set_Value (COLUMNNAME_JP_TypeOfAverageCost, JP_TypeOfAverageCost);
	}

	/** Get Type of Average Cost.
		@return Type of Average Cost	  */
	public String getJP_TypeOfAverageCost () 
	{
		return (String)get_Value(COLUMNNAME_JP_TypeOfAverageCost);
	}

	/** Current Cost Price = CC */
	public static final String JP_UPDATECOST_CurrentCostPrice = "CC";
	/** Future Cost Price = FC */
	public static final String JP_UPDATECOST_FutureCostPrice = "FC";
	/** Both Current Cost and Future Cost = BT */
	public static final String JP_UPDATECOST_BothCurrentCostAndFutureCost = "BT";
	/** Set Update Cost.
		@param JP_UpdateCost Update Cost	  */
	public void setJP_UpdateCost (String JP_UpdateCost)
	{

		set_Value (COLUMNNAME_JP_UpdateCost, JP_UpdateCost);
	}

	/** Get Update Cost.
		@return Update Cost	  */
	public String getJP_UpdateCost () 
	{
		return (String)get_Value(COLUMNNAME_JP_UpdateCost);
	}

	public org.compiere.model.I_M_DiscountSchema getM_DiscountSchema() throws RuntimeException
    {
		return (org.compiere.model.I_M_DiscountSchema)MTable.get(getCtx(), org.compiere.model.I_M_DiscountSchema.Table_Name)
			.getPO(getM_DiscountSchema_ID(), get_TrxName());	}

	/** Set Discount Schema.
		@param M_DiscountSchema_ID 
		Schema to calculate the trade discount percentage
	  */
	public void setM_DiscountSchema_ID (int M_DiscountSchema_ID)
	{
		if (M_DiscountSchema_ID < 1) 
			set_Value (COLUMNNAME_M_DiscountSchema_ID, null);
		else 
			set_Value (COLUMNNAME_M_DiscountSchema_ID, Integer.valueOf(M_DiscountSchema_ID));
	}

	/** Get Discount Schema.
		@return Schema to calculate the trade discount percentage
	  */
	public int getM_DiscountSchema_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_DiscountSchema_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_M_PriceList getM_PriceList() throws RuntimeException
    {
		return (org.compiere.model.I_M_PriceList)MTable.get(getCtx(), org.compiere.model.I_M_PriceList.Table_Name)
			.getPO(getM_PriceList_ID(), get_TrxName());	}

	/** Set Price List.
		@param M_PriceList_ID 
		Unique identifier of a Price List
	  */
	public void setM_PriceList_ID (int M_PriceList_ID)
	{
		if (M_PriceList_ID < 1) 
			set_Value (COLUMNNAME_M_PriceList_ID, null);
		else 
			set_Value (COLUMNNAME_M_PriceList_ID, Integer.valueOf(M_PriceList_ID));
	}

	/** Get Price List.
		@return Unique identifier of a Price List
	  */
	public int getM_PriceList_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_PriceList_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_M_Product_Category getM_Product_Category() throws RuntimeException
    {
		return (org.compiere.model.I_M_Product_Category)MTable.get(getCtx(), org.compiere.model.I_M_Product_Category.Table_Name)
			.getPO(getM_Product_Category_ID(), get_TrxName());	}

	/** Set Product Category.
		@param M_Product_Category_ID 
		Category of a Product
	  */
	public void setM_Product_Category_ID (int M_Product_Category_ID)
	{
		if (M_Product_Category_ID < 1) 
			set_Value (COLUMNNAME_M_Product_Category_ID, null);
		else 
			set_Value (COLUMNNAME_M_Product_Category_ID, Integer.valueOf(M_Product_Category_ID));
	}

	/** Get Product Category.
		@return Category of a Product
	  */
	public int getM_Product_Category_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Product_Category_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Name.
		@param Name 
		Alphanumeric identifier of the entity
	  */
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName () 
	{
		return (String)get_Value(COLUMNNAME_Name);
	}

    /** Get Record ID/ColumnName
        @return ID/ColumnName pair
      */
    public KeyNamePair getKeyNamePair() 
    {
        return new KeyNamePair(get_ID(), getName());
    }

	/** Set Search Key.
		@param Value 
		Search key for the record in the format required - must be unique
	  */
	public void setValue (String Value)
	{
		set_Value (COLUMNNAME_Value, Value);
	}

	/** Get Search Key.
		@return Search key for the record in the format required - must be unique
	  */
	public String getValue () 
	{
		return (String)get_Value(COLUMNNAME_Value);
	}
}