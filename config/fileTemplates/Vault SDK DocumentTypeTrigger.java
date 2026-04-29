#set( $javaDocumentTypeTriggerName = ${DocumentTypeTriggerName} )
#set( $javaDocumentTypeTriggerName = $javaDocumentTypeTriggerName.replace("  "," ").replace(" ","_") )
#set( $javaDocumentTypeTriggerName = ${StringUtils.removeAndHump($javaDocumentTypeTriggerName)} )
#set( $javaDocumentTypeTriggerName = $javaDocumentTypeTriggerName.replace("Trigger","").replace("trigger","") )
#set( $javaDocumentTypeTriggerName = $javaDocumentTypeTriggerName + "Trigger" )
#set( $javaDoctypeName = ${DoctypeName} )
#set( $javaDoctypeName = $javaDoctypeName.toLowerCase() )
#set( $javaDoctypeName = $javaDoctypeName.substring(0,$javaDoctypeName.indexOf("__")) )
/*
 * --------------------------------------------------------------------
 * DocumentTypeTrigger:	$javaDocumentTypeTriggerName
 * Object:			${DoctypeName}
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
package com.veeva.vault.custom.doctypetriggers;

import com.veeva.vault.sdk.api.document.*;

@DocumentTypeTriggerInfo(
		documentType = "${DoctypeName}",
		events = {DocumentVersionEvent.BEFORE_UPDATE},
		level = DocumentVersionEventLevel.DOCUMENT_VERSION)
public class $javaDocumentTypeTriggerName implements DocumentTypeTrigger {

	public void execute(DocumentTypeTriggerContext context) {

	}
}