#set( $javaUserDefinedTestName = ${UserDefinedTestName} )
#set( $javaUserDefinedTestName = $javaUserDefinedTestName.replace("  "," ").replace(" ","_") )
#set( $javaUserDefinedTestName = ${StringUtils.removeAndHump($javaUserDefinedTestName)} )
#set( $javaUserDefinedTestName = $javaUserDefinedTestName + "Test" )
/*
 * --------------------------------------------------------------------
 * UserDefinedTest:	    $javaUserDefinedTestName
 * Author:				${USER}
 * Date:				${YEAR}-${MONTH}-${DAY}
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

import com.veeva.vault.poc.annotations.UserDefinedTestInfo;
import com.veeva.vault.sdk.api.core.*;
import com.veeva.vault.sdk.api.data.*;

@UserDefinedTestInfo()
public class $javaUserDefinedTestName {

}