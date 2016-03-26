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
package jpiere.base.plugin.org.compiere.acct;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.logging.Level;

import jpiere.base.plugin.org.adempiere.model.MInvValCal;
import jpiere.base.plugin.org.adempiere.model.MInvValCalLine;

import org.compiere.acct.Doc;
import org.compiere.acct.DocLine;
import org.compiere.acct.Fact;
import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.ProductCost;
import org.compiere.util.Env;

/**
 * Post Inventory Valuation Calculate Documents.
 *
 *
 * JPIERE-0161:JPBP:InvValCal
 *
 * <pre>
 *   Table:              JP_InvValCal
 *   Document Types:     JPI
 * </pre>
 *
 * @author Jorg Janke
 * @author Hideaki Hagiwara(h.hagiwara@oss-erp.co.jp)
 * @version α
 */
public class Doc_JPInvValCal extends Doc
{
	/**
	 * Constructor
	 * 	@param as accounting schema
	 * 	@param rs record
	 * 	@param trxName trx
	 */
	public Doc_JPInvValCal (MAcctSchema as, ResultSet rs, String trxName)
	{
		super (as, MInvValCal.class, rs, DOCTYPE_PurchaseRequisition, trxName);
	}

	/**
	 *	Load Specific Document Details
	 *  @return error message or null
	 */
	protected String loadDocumentDetails ()
	{
		setC_Currency_ID(NO_CURRENCY);
		MInvValCal ivc = (MInvValCal)getPO();
		setDateDoc (ivc.getDateValue());
		setDateAcct (ivc.getDateValue());
		// Amounts
		setAmount(AMTTYPE_Gross, ivc.getTotalLines());
		setAmount(AMTTYPE_Net, ivc.getTotalLines());
		// Contained Objects
		p_lines = loadLines (ivc);
		// log.fine( "Lines=" + p_lines.length + ", Taxes=" + m_taxes.length);
		return null;
	}	// loadDocumentDetails

	/**
	 *	Load Requisition Lines
	 *	@param req requisition
	 *	@return DocLine Array
	 */
	private DocLine[] loadLines (MInvValCal ivc)
	{
		ArrayList<DocLine> list = new ArrayList<DocLine> ();
		MInvValCalLine[] lines = ivc.getLines();
		for (int i = 0; i < lines.length; i++)
		{
			MInvValCalLine line = lines[i];
			DocLine docLine = new DocLine (line, this);
			BigDecimal Qty = Env.ZERO;
			docLine.setQty (Qty, false);
			//BigDecimal PriceActual = line.getPriceActual();
			BigDecimal GrandTotal = line.getJP_InvValTotalAmt();
			docLine.setAmount (GrandTotal);	 // DR
			list.add (docLine);
		}
		// Return Array
		DocLine[] dls = new DocLine[list.size ()];
		list.toArray (dls);
		return dls;
	}	// loadLines

	/***************************************************************************
	 * Get Source Currency Balance - subtracts line and tax amounts from total -
	 * no rounding
	 *
	 * @return positive amount, if total invoice is bigger than lines
	 */
	public BigDecimal getBalance ()
	{
		BigDecimal retValue = Env.ZERO;
		return retValue;
	}	// getBalance

	/***************************************************************************
	 * Create Facts (the accounting logic) for POR.
	 * <pre>
	 * Reservation
	 * 	Expense		CR
	 * 	Offset			DR
	 * </pre>
	 * @param as accounting schema
	 * @return Fact
	 */
	public ArrayList<Fact> createFacts (MAcctSchema as)
	{
		ArrayList<Fact> facts = new ArrayList<Fact>();
		Fact fact = new Fact (this, as, Fact.POST_Reservation);
		setC_Currency_ID(as.getC_Currency_ID());
		//
		@SuppressWarnings("unused")
		BigDecimal grossAmt = getAmount (Doc.AMTTYPE_Gross);
		// Commitment
		if (as.isCreateReservation ())
		{
			BigDecimal total = Env.ZERO;
			for (int i = 0; i < p_lines.length; i++)
			{
				DocLine line = p_lines[i];
				BigDecimal cost = line.getAmtSource();
				total = total.add (cost);
				// Account
				MAccount expense = line.getAccount(ProductCost.ACCTTYPE_P_Expense, as);
				//
				fact.createLine (line, expense, as.getC_Currency_ID(), cost, null);
			}
			// Offset
			MAccount offset = getAccount (ACCTTYPE_CommitmentOffset, as);
			if (offset == null)
			{
				p_Error = "@NotFound@ @CommitmentOffset_Acct@";
				log.log (Level.SEVERE, p_Error);
				return null;
			}
			fact.createLine (null, offset, getC_Currency_ID(), null, total);
			facts.add(fact);
		}

		return facts;
	} // createFact
} //