#set( $javaMessageProcessorName = ${MessageProcessorName} )
#set( $javaMessageProcessorName = $javaMessageProcessorName.replace("  "," ").replace(" ","_") )
#set( $javaMessageProcessorName = ${StringUtils.removeAndHump($javaMessageProcessorName)} )
#set( $javaMessageProcessorName = $javaMessageProcessorName.replace("Message","").replace("message","") )
#set( $javaMessageProcessorName = $javaMessageProcessorName.replace("Processor","").replace("processor","") )
#set( $javaMessageProcessorName = $javaMessageProcessorName + "Processor" )
/*
 * --------------------------------------------------------------------
 * MessageProcessor:	$javaMessageProcessorName
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
package com.veeva.vault.custom.messageprocessors;

import com.veeva.vault.sdk.api.queue.*;

@MessageProcessorInfo()
public class $javaMessageProcessorName implements MessageProcessor {
	
	public void execute(MessageContext context) {

	}
}