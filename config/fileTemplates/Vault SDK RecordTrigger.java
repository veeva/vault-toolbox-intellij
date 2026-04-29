#set( $javaRecordTriggerName = ${RecordTriggerName} )
#set( $javaRecordTriggerName = $javaRecordTriggerName.replace("  "," ").replace(" ","_") )
#set( $javaRecordTriggerName = ${StringUtils.removeAndHump($javaRecordTriggerName)} )
#set( $javaRecordTriggerName = $javaRecordTriggerName.replace("Trigger","").replace("trigger","") )
#set( $javaRecordTriggerName = $javaRecordTriggerName + "Trigger" )
#set( $javaObjectName = ${ObjectName} )
#set( $javaObjectName = $javaObjectName.toLowerCase() )
#set( $javaObjectName = $javaObjectName.substring(0,$javaObjectName.indexOf("__")) )
/*
 * --------------------------------------------------------------------
 * RecordTrigger:	$javaRecordTriggerName
 * Object:			${ObjectName}
 * Author:			${USER}
 * Date:			${YEAR}-${MONTH}-${DAY}
 *---------------------------------------------------------------------
 * Description:	${Description}
 *---------------------------------------------------------------------
 * Copyright (c) ${YEAR} Veeva Systems Inc.  All Rights Reserved.
 *		This code is based on pre-existing content developed and
 *		owned by Veeva Systems Inc. and may only be used in connection
 *		with the deliverable with which it was provided to Customer.
 *---------------------------------------------------------------------
 */
package com.veeva.vault.custom.recordtriggers;

import com.veeva.vault.sdk.api.data.*;

@RecordTriggerInfo(object="${ObjectName}",
		events = {RecordEvent.BEFORE_INSERT, RecordEvent.BEFORE_UPDATE})
public class $javaRecordTriggerName implements RecordTrigger {

	public void execute(RecordTriggerContext context) {


	}
}