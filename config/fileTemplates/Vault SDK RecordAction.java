#set( $javaRecordActionName = ${RecordActionName} )
#set( $javaRecordActionName = $javaRecordActionName.replace("  "," ").replace(" ","_") )
#set( $javaRecordActionName = ${StringUtils.removeAndHump($javaRecordActionName)} )
#set( $javaRecordActionName = $javaRecordActionName.replace("Action","").replace("action","") )
#set( $javaRecordActionName = $javaRecordActionName + "Action" )
#set( $javaObjectName = ${ObjectName} )
#set( $javaObjectName = $javaObjectName.toLowerCase() )
#set( $javaObjectName = $javaObjectName.substring(0,$javaObjectName.indexOf("__")) )
/*
 * --------------------------------------------------------------------
 * RecordAction:	$javaRecordActionName
 * Object:			${ObjectName}
 * Author:			${USER}
 * Date:			${YEAR}-${MONTH}-${DAY}
 *---------------------------------------------------------------------
 * Description: ${Description}
 *---------------------------------------------------------------------
 * Copyright (c) ${YEAR} Veeva Systems Inc.  All Rights Reserved.
 *      This code is based on pre-existing content developed and
 *      owned by Veeva Systems Inc. and may only be used in connection
 *      with the deliverable with which it was provided to Customer.
 *---------------------------------------------------------------------
 */
package com.veeva.vault.custom.recordactions;

import com.veeva.vault.sdk.api.action.*;

@RecordActionInfo(object="${ObjectName}",
		label="${RecordActionLabel}",
		usages = Usage.UNSPECIFIED)
public class $javaRecordActionName implements RecordAction {

	public boolean isExecutable(RecordActionContext context) {
		return true;
	}

	public void execute(RecordActionContext context) {

	}
}