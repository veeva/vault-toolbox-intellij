#set( $javaEmailProcessorName = ${EmailProcessorName} )
#set( $javaEmailProcessorName = $javaEmailProcessorName.replace("  "," ").replace(" ","_") )
#set( $javaEmailProcessorName = ${StringUtils.removeAndHump($javaEmailProcessorName)} )
#set( $javaEmailProcessorName = $javaEmailProcessorName.replace("Email","").replace("Email","") )
#set( $javaEmailProcessorName = $javaEmailProcessorName.replace("Processor","").replace("processor","") )
#set( $javaEmailProcessorName = $javaEmailProcessorName + "Processor" )
/*
 * --------------------------------------------------------------------
 * EmailProcessor:	$javaEmailProcessorName
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
package com.veeva.vault.custom.emailprocessors;
    
import com.veeva.vault.sdk.api.email.EmailProcessor;
import com.veeva.vault.sdk.api.email.EmailProcessorContext;
import com.veeva.vault.sdk.api.email.EmailProcessorInfo;

@EmailProcessorInfo(label = "${Description}")
public class $javaEmailProcessorName implements EmailProcessor {

	@Override
	public void execute(EmailProcessorContext emailProcessorContext) {
		
	}
}