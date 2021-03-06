/******************************************************************************
 * Copyright (C) 2009 Low Heng Sin                                            *
 * Copyright (C) 2009 Idalica Corporation                                     *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
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

package jpiere.base.plugin.webui.apps.form;

import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.apps.form.WCreateFromWindow;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListModelTable;
import org.adempiere.webui.component.Listbox;
import org.adempiere.webui.component.ListboxFactory;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.editor.WEditor;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.event.ValueChangeListener;
import org.compiere.model.GridTab;
import org.compiere.model.MColumn;
import org.compiere.model.MLocator;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MWarehouse;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Vlayout;

import jpiere.base.plugin.org.adempiere.model.MPhysicalWarehouse;

/**
 * JPIERE-0234
 * @author Low Heng Sin
 * @author Hideaki Hagiwara
 *
 */
public class JPiereCreateFromRMAInOutUI extends JPiereCreateFromRMAInOut implements EventListener<Event>, ValueChangeListener
{

	private WCreateFromWindow window;

	public JPiereCreateFromRMAInOutUI(GridTab tab)
	{
		super(tab);
		log.info(getGridTab().toString());

		window = new WCreateFromWindow(this, getGridTab().getWindowNo());

		p_WindowNo = getGridTab().getWindowNo();

		try
		{
			if (!dynInit())
				return;
			zkInit();
			setInitOK(true);
		}
		catch(Exception e)
		{
			log.log(Level.SEVERE, "", e);
			setInitOK(false);
			throw new AdempiereException(e.getMessage());
		}
		AEnv.showWindow(window);
	}

	/** Window No               */
	private int p_WindowNo;

	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(getClass());

	protected Label bPartnerLabel = new Label();
	protected WEditor bPartnerField;

    /** Label for the rma selection */
    protected Label rmaLabel = new Label();
    /** Combo box for selecting RMA document */
    protected Listbox rmaField = ListboxFactory.newDropdownListbox();

	protected Label locatorLabel = new Label();
	// Locator
	protected WSearchEditor locatorField = null;

	/**
	 *  Dynamic Init
	 *  @throws Exception if Lookups cannot be initialized
	 *  @return true if initialized
	 */
	public boolean dynInit() throws Exception
	{
		log.config("");

		super.dynInit();

		window.setTitle(getTitle());

		//  load Locator
		int AD_Column_ID = MColumn.getColumn_ID("M_InOutLine", "M_Locator_ID");
		MLookup lookupLocator = MLookupFactory.get(Env.getCtx(), p_WindowNo, 0, AD_Column_ID, DisplayType.Search);
		locatorField = new WSearchEditor("M_Locator_ID", true, false, true, lookupLocator);
		Doc_PhysicalWarehouse_ID = Env.getContextAsInt(Env.getCtx(), p_WindowNo, "JP_PhysicalWarehouse_ID");
		if(Doc_PhysicalWarehouse_ID == 0)
		{
			MWarehouse wh = MWarehouse.get(Env.getCtx(), Env.getContextAsInt(Env.getCtx(),p_WindowNo, "M_Warehouse_ID"));
			if (wh != null)
			{
				MLocator locator = wh.getDefaultLocator();
				if(locator != null)
				{
					locatorField.setValue(locator.getM_Locator_ID());
					receiptLocator_ID = locator.getM_Locator_ID();
				}
			}
		}else{
			MPhysicalWarehouse phyWH = MPhysicalWarehouse.get(Env.getCtx(), Doc_PhysicalWarehouse_ID);
			MLocator locator = phyWH.getDefaultLocator(MWarehouse.get(Env.getCtx(), Env.getContextAsInt(Env.getCtx(),p_WindowNo, "M_Warehouse_ID")) );
			if(locator != null)
			{
				locatorField.setValue(locator.getM_Locator_ID());
				receiptLocator_ID = locator.getM_Locator_ID();
			}
		}
		locatorField.addValueChangeListener(this);

		initBPartner(false);
		bPartnerField.addValueChangeListener(this);
		locatorLabel.setMandatory(true);

		return true;
	}   //  dynInit

	protected void zkInit() throws Exception
	{
    	bPartnerLabel.setText(Msg.getElement(Env.getCtx(), "C_BPartner_ID"));
        rmaLabel.setText(Msg.getElement(Env.getCtx(), "M_RMA_ID",isSOTrx));
        if(isSOTrx)
        	locatorLabel.setText(Msg.getMsg(Env.getCtx(), "JP_ReceiptLocator"));
        else
        	locatorLabel.setText(Msg.getMsg(Env.getCtx(), "JP_ShipLocator"));

		Vlayout vlayout = new Vlayout();
		vlayout.setVflex("1");
		vlayout.setWidth("98%");
    	Panel parameterPanel = window.getParameterPanel();
		parameterPanel.appendChild(vlayout);

		Grid parameterStdLayout = GridFactory.newGridLayout();
    	vlayout.appendChild(parameterStdLayout);

		Rows rows = (Rows) parameterStdLayout.newRows();
		Row row = rows.newRow();
		row.appendChild(bPartnerLabel.rightAlign());
		if (bPartnerField != null) {
			row.appendChild(bPartnerField.getComponent());
			bPartnerField.fillHorizontal();
		}

        // Add RMA document selection to panel
		row = rows.newRow();
        row.appendChild(rmaLabel.rightAlign());
        row.appendChild(rmaField);
        rmaField.setHflex("1");

		row = rows.newRow();
		row.appendChild(locatorLabel.rightAlign());
		row.appendChild(locatorField.getComponent());

	}

	private boolean 	m_actionActive = false;

	/**
	 *  Action Listener
	 *  @param e event
	 * @throws Exception
	 */
	public void onEvent(Event e) throws Exception
	{
		if (m_actionActive)
			return;
		m_actionActive = true;

		if (e.getTarget().equals(rmaField))
        {
            KeyNamePair pp = rmaField.getSelectedItem().toKeyNamePair();
            if (pp == null || pp.getKey() == 0)
                ;
            else
            {
                int M_RMA_ID = pp.getKey();
                loadRMA(M_RMA_ID, locatorField.getValue()!=null?((Integer)locatorField.getValue()).intValue():0);
            }
        }

		m_actionActive = false;
	}


	/**
	 *  Change Listener
	 *  @param e event
	 */
	public void valueChange (ValueChangeEvent e)
	{
		if (log.isLoggable(Level.CONFIG)) log.config(e.getPropertyName() + "=" + e.getNewValue());

		//  BPartner - load Order/Invoice/Shipment
		if (e.getPropertyName().equals("C_BPartner_ID"))
		{
			int C_BPartner_ID = 0;
			if (e.getNewValue() != null){
				C_BPartner_ID = ((Integer)e.getNewValue()).intValue();
			}

			initBPRMADetails (C_BPartner_ID, true);
		}else if(e.getPropertyName().equals("M_Locator_ID")){
			if (e.getNewValue() != null){
				receiptLocator_ID = ((Integer)e.getNewValue()).intValue();
			}else{
				receiptLocator_ID = 0;
			}
		}
		window.tableChanged(null);
	}   //  vetoableChange

	/**************************************************************************
	 *  Load BPartner Field
	 *  @param forInvoice true if Invoices are to be created, false receipts
	 *  @throws Exception if Lookups cannot be initialized
	 */
	protected void initBPartner (boolean forInvoice) throws Exception
	{
		//  load BPartner
		int AD_Column_ID = 3499;        //  C_Invoice.C_BPartner_ID
		MLookup lookup = MLookupFactory.get (Env.getCtx(), p_WindowNo, 0, AD_Column_ID, DisplayType.Search);
		bPartnerField = new WSearchEditor ("C_BPartner_ID", true, true, true, lookup);
		//
		int C_BPartner_ID = Env.getContextAsInt(Env.getCtx(), p_WindowNo, "C_BPartner_ID");
		bPartnerField.setValue(Integer.valueOf(C_BPartner_ID));

		//  initial loading
		initBPRMADetails(C_BPartner_ID, forInvoice);
	}   //  initBPartner

	/**
	 *  Load PBartner dependent Order/Invoice/Shipment Field.
	 *  @param C_BPartner_ID BPartner
	 *  @param forInvoice for invoice
	 */
	protected void initBPRMADetails (int C_BPartner_ID, boolean forInvoice)
	{
	    rmaField.removeActionListener(this);
	    rmaField.removeAllItems();
	    //  None
	    KeyNamePair pp = new KeyNamePair(0,"");
	    rmaField.addItem(pp);

	    ArrayList<KeyNamePair> list = loadRMAData(C_BPartner_ID);
	    int M_RMA_ID = Env.getContextAsInt(Env.getCtx(), p_WindowNo, "M_RMA_ID");
	    rmaField.setSelectedIndex(0);
	    int i = 0;
		for(KeyNamePair knp : list)
		{
			i++;
			rmaField.addItem(knp);
			if(knp.getKey()==M_RMA_ID && M_RMA_ID > 0)
			{
				rmaField.setSelectedIndex(i);
				loadRMA(M_RMA_ID, locatorField.getValue()!=null?((Integer)locatorField.getValue()).intValue():0);
			}
		}
	    rmaField.addActionListener(this);
	}


	/**
	 *  Load Data - RMA
	 *  @param M_RMA_ID RMA
	 *  @param M_Locator_ID
	 */
	protected void loadRMA (int M_RMA_ID, int M_Locator_ID)
	{
		loadTableOIS(getRMAData(M_RMA_ID, M_Locator_ID));
	}

	/**
	 *  Load Order/Invoice/Shipment data into Table
	 *  @param data data
	 */
	protected void loadTableOIS (Vector<?> data)
	{
		window.getWListbox().clear();

		//  Remove previous listeners
		window.getWListbox().getModel().removeTableModelListener(window);
		//  Set Model
		ListModelTable model = new ListModelTable(data);
		model.addTableModelListener(window);
		window.getWListbox().setData(model, getOISColumnNames());
		//

		configureMiniTable(window.getWListbox());
	}   //  loadOrder

	public void showWindow()
	{
		window.setVisible(true);
	}

	public void closeWindow()
	{
		window.dispose();
	}

	@Override
	public Object getWindow() {
		return window;
	}
}
