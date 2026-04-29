#set( $javaRecordWorkflowActionName = ${RecordWorkflowActionName} )
#set( $javaRecordWorkflowActionName = $javaRecordWorkflowActionName.replace("  "," ").replace(" ","_") )
#set( $javaRecordWorkflowActionName = ${StringUtils.removeAndHump($javaRecordWorkflowActionName)} )
#set( $javaRecordWorkflowActionName = $javaRecordWorkflowActionName.replace("Action","").replace("action","") )
#set( $javaRecordWorkflowActionName = $javaRecordWorkflowActionName.replace("Workflow","").replace("workflow","") )
#set( $javaRecordWorkflowActionName = $javaRecordWorkflowActionName + "WorkflowAction" )
/*
 * --------------------------------------------------------------------
 * RecordWorklowAction:	$javaRecordWorkflowActionName
 * Author:				${USER}
 * Date:				${YEAR}-${MONTH}-${DAY}
 *---------------------------------------------------------------------
 * Description: ${Description}
 *---------------------------------------------------------------------
 * Copyright (c) ${YEAR} Veeva Systems Inc.  All Rights Reserved.
 *      This code is based on pre-existing content developed and
 *      owned by Veeva Systems Inc. and may only be used in connection
 *      with the deliverable with which it was provided to Customer.
 *---------------------------------------------------------------------
 */
package com.veeva.vault.custom.recordworkflowactions;

import com.veeva.vault.sdk.api.workflow.*;

@RecordWorkflowActionInfo(label="${RecordWorkflowActionLabel}",
		stepTypes={WorkflowStepType.START})
public class $javaRecordWorkflowActionName implements RecordWorkflowAction {

	public void execute(RecordWorkflowActionContext context) {

	}
}