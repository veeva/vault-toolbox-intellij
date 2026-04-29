#set( $javaMessageDeliveryEventHandlerName = ${MessageDeliveryEventHandlerName} )
#set( $javaMessageDeliveryEventHandlerName = $javaMessageDeliveryEventHandlerName.replace("  "," ").replace(" ","_") )
#set( $javaMessageDeliveryEventHandlerName = ${StringUtils.removeAndHump($javaMessageDeliveryEventHandlerName)} )
#set( $javaMessageDeliveryEventHandlerName = $javaMessageDeliveryEventHandlerName.replace("Handler","").replace("handler","") )
#set( $javaMessageDeliveryEventHandlerName = $javaMessageDeliveryEventHandlerName + "Handler" )
/*
 * --------------------------------------------------------------------
 * MessageDeliveryEventHandler:	$javaMessageDeliveryEventHandlerName
 * Author:				        ${USER}
 * Date:				        ${YEAR}-${MONTH}-${DAY}
 *---------------------------------------------------------------------
 * Description:	${Description}
 *---------------------------------------------------------------------
 * Copyright (c) ${YEAR} Veeva Systems Inc.  All Rights Reserved.
 *		This code is based on pre-existing content developed and
 * 		owned by Veeva Systems Inc. and may only be used in connection
 *		with the deliverable with which it was provided to Customer.
 *---------------------------------------------------------------------
 */
package com.veeva.vault.custom.classes;

import com.veeva.vault.sdk.api.queue.*;

@MessageDeliveryEventHandlerInfo()
public class $javaMessageDeliveryEventHandlerName implements MessageDeliveryEventHandler {

	public void onError(MessageDeliveryEventHandlerContext context) {

	}

	@Override
	public void onSend(MessageDeliveryEventHandlerSendContext context) {
		
	}
}