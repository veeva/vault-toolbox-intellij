#set( $javaDocumentActionName = ${DocumentActionName} )
#set( $javaDocumentActionName = $javaDocumentActionName.replace("  "," ").replace(" ","_") )
#set( $javaDocumentActionName = ${StringUtils.removeAndHump($javaDocumentActionName)} )
#set( $javaDocumentActionName = $javaDocumentActionName.replace("Action","").replace("action","") )
#set( $javaDocumentActionName = $javaDocumentActionName + "Action" )
/*
 * --------------------------------------------------------------------
 * DocumentAction:	$javaDocumentActionName
 * Object:			document
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
package com.veeva.vault.custom.documentactions;

import com.veeva.vault.sdk.api.action.*;
import com.veeva.vault.sdk.api.core.*;
import com.veeva.vault.sdk.api.document.*;

@DocumentActionInfo(label="${DocumentActionLabel}",
		usages = Usage.UNSPECIFIED)
public class $javaDocumentActionName implements DocumentAction {

	public boolean isExecutable(DocumentActionContext context) {
		return true;
	}

	public void execute(DocumentActionContext context) {

	}
}