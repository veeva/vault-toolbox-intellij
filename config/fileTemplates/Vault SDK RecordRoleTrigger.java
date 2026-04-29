#set( $javaRecordRoleTriggerName = ${RecordRoleTriggerName} )
#set( $javaRecordRoleTriggerName = $javaRecordRoleTriggerName.replace("  "," ").replace(" ","_") )
#set( $javaRecordRoleTriggerName = ${StringUtils.removeAndHump($javaRecordRoleTriggerName)} )
#set( $javaRecordRoleTriggerName = $javaRecordRoleTriggerName.replace("Role","").replace("role","") )
#set( $javaRecordRoleTriggerName = $javaRecordRoleTriggerName.replace("Trigger","").replace("trigger","") )
#set( $javaRecordRoleTriggerName = $javaRecordRoleTriggerName + "RoleTrigger" )
#set( $javaObjectName = ${ObjectName} )
#set( $javaObjectName = $javaObjectName.toLowerCase() )
#set( $javaObjectName = $javaObjectName.substring(0,$javaObjectName.indexOf("__")) )
/*
 * --------------------------------------------------------------------
 * RecordRoleTrigger:	$javaRecordRoleTriggerName
 * Object:				${ObjectName}
 * Author:				${USER}
 * Date:				${YEAR}-${MONTH}-${DAY}
 *---------------------------------------------------------------------
 * Description:	${Description}
 *---------------------------------------------------------------------
 * Copyright (c) ${YEAR} Veeva Systems Inc.  All Rights Reserved.
 *		This code is based on pre-existing content developed and
 *		owned by Veeva Systems Inc. and may only be used in connection
 *		with the deliverable with which it was provided to Customer.
 *---------------------------------------------------------------------
 */
package com.veeva.vault.custom.recordroletriggers;

import com.veeva.vault.sdk.api.role.*;

@RecordRoleTriggerInfo(object="${ObjectName}",
		events = {RecordRoleEvent.BEFORE})
public class $javaRecordRoleTriggerName implements RecordRoleTrigger {

	public void execute(RecordRoleTriggerContext context) {


	}
}