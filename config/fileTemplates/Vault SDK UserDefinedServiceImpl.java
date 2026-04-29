#set( $javaUserDefinedServiceName = ${UserDefinedServiceName} )
#set( $javaUserDefinedServiceName = $javaUserDefinedServiceName.replace("  "," ").replace(" ","_") )
#set( $javaUserDefinedServiceName = $javaUserDefinedServiceName.replace("Service","").replace("service","") )
#set( $javaUserDefinedServiceName = $javaUserDefinedServiceName + "Service" )
#set( $javaUserDefinedServiceNameImpl = $javaUserDefinedServiceName + "Impl" )
/*
 * --------------------------------------------------------------------
 * UserDefinedService:	$javaUserDefinedServiceName
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

import com.veeva.vault.sdk.api.core.*;
    
@UserDefinedServiceInfo
public class $javaUserDefinedServiceNameImpl implements $javaUserDefinedServiceName {
    
    public boolean exampleMethod() {
        return true;
    }
}