#set( $javaUserDefinedModelName = ${UserDefinedModelName} )
#set( $javaUserDefinedModelName = $javaUserDefinedModelName.replace("  "," ").replace(" ","_") )
#set( $javaUserDefinedModelName = ${StringUtils.removeAndHump($javaUserDefinedModelName)} )
#set( $javaUserDefinedModelName = $javaUserDefinedModelName + "Model" )
/*1
 * --------------------------------------------------------------------
 * UserDefinedModel:	$javaUserDefinedModelName
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
package com.veeva.vault.custom.models;

import com.veeva.vault.sdk.api.core.*;

import java.math.BigDecimal;

@UserDefinedModelInfo(include = UserDefinedPropertyInclude.NON_NULL)
public interface $javaUserDefinedModelName extends UserDefinedModel {
//someField
	@UserDefinedProperty
	BigDecimal getId();

	@UserDefinedProperty(name = "other_field_here")
	BigDecimal getOtherField();
}