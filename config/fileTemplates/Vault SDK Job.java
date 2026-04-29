#set( $javaJobName = ${JobName} )
#set( $javaJobName = $javaJobName.replace("  "," ").replace(" ","_") )
#set( $javaJobName = ${StringUtils.removeAndHump($javaJobName)} )
#set( $javaJobName = $javaJobName.replace("Job","").replace("job","") )
#set( $javaJobName = $javaJobName + "Job" )
/*
 * --------------------------------------------------------------------
 * Job Info:	$javaJobName 
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
package com.veeva.vault.custom.jobs;

import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.job.*;

@JobInfo(adminConfigurable = true,idempotent = true,isVisible = true)
public class $javaJobName implements Job {

	public JobInputSupplier init(JobInitContext context) {
        return context.newJobInput(VaultCollections.newList()); 
	}

	public void process(JobProcessContext context) {

	}

	public void completeWithSuccess(JobCompletionContext context) {

	}

	public void completeWithError(JobCompletionContext context) {

	}
}