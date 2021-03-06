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
package jpiere.base.plugin.org.adempiere.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MRegion;
import org.compiere.util.Util;

/**
 * @author Hideaki Hagiwara
 *
 */
public class JPiereRegionCallout implements IColumnCallout {

	//JPIERE-0083
	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {

		if(value==null || Util.isEmpty(value.toString()))
			return "";

		Integer C_Region_ID = (Integer)value;
		MRegion region = null;

		if(C_Region_ID==142)//C_Region_ID==142 is Oragon at United States
		{
			Object obj =mTab.getValue("C_Country_ID");
			if(Integer.parseInt(obj.toString())!=100)
			{
				mTab.setValue("C_Region_ID", "");
				return null;
			}
		}

		region = MRegion.get(ctx, C_Region_ID);
		mTab.setValue("RegionName", region.getName());

		return null;
	}

}
