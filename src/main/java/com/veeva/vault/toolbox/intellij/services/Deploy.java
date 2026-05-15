package com.veeva.vault.toolbox.intellij.services;

import com.veeva.vault.toolbox.intellij.ui.Message;
import com.veeva.vault.vapil.api.model.response.MdlExecuteResponse;
import com.veeva.vault.vapil.api.model.response.VaultResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Renders the outcome of a Vault deploy or drop request into a {@link Message}
 * dialog and chooses the dialog severity based on the response contents.
 */
public final class Deploy {

	private static final Logger logger = LoggerFactory.getLogger(Deploy.class);

	private Deploy() {
	}

	/**
	 * Appends the contents of a Vault response to the supplied message and displays
	 * the resulting dialog. The dialog is shown as an error if any errors or
	 * exceptions were reported, as a warning if only warnings were reported, and
	 * as information otherwise.
	 *
	 * @param response the Vault API response to render; may be {@code null}
	 * @param message  the message builder used to construct and display the dialog
	 */
	public static void showResults(VaultResponse response, Message message) {
		try {
			int numErrors = 0;
			int numWarnings = 0;

			try {
				if (response == null) {
					message.append("response = null");
				}
				else {
					message.append(response.getResponseStatus(), true);
					if (response.getResponseMessage() != null) {
						message.appendSeparator();
						message.append(response.getResponseMessage(), true);
					}

					if (response.hasErrors()) {
						message.appendSeparator();
						for (VaultResponse.APIResponseError error : response.getErrors()) {
							message.append(error.getMessage(), true);
							numErrors++;
						}
					}

					if (response instanceof MdlExecuteResponse mdlResponse) {
						MdlExecuteResponse.ScriptExecution scriptExecution = mdlResponse.getScriptExecution();
						if (scriptExecution != null) {
							numWarnings = scriptExecution.getWarnings() != null ? scriptExecution.getWarnings() : 0;
							numErrors += (scriptExecution.getFailures() != null ? scriptExecution.getFailures() : 0)
									+ (scriptExecution.getExceptions() != null ? scriptExecution.getExceptions() : 0);

							message.appendSeparator();
							message.append("message = " + scriptExecution.getMessage(), true);
							message.append("warnings = " + scriptExecution.getWarnings(), true);
							message.append("failures = " + scriptExecution.getFailures(), true);
							message.append("exceptions = " + scriptExecution.getExceptions(), true);
							message.append("components_affected = " + scriptExecution.getComponentsAffected(), true);
						}

						List<MdlExecuteResponse.StatementExecution> statements = mdlResponse.getStatementExecution();
						if (statements != null) {
							for (MdlExecuteResponse.StatementExecution statement : statements) {
								message.appendSeparator();
								message.append("command = " + statement.getCommand(), true);
								message.append("component = " + statement.getComponent(), true);
								message.append("message = " + statement.getMessage(), true);
								message.append("response = " + statement.getResponse(), true);

								Object rawErrors = statement.get("errors");
								if (rawErrors instanceof List<?> errors) {
									message.appendSeparator();
									for (Object errorObj : errors) {
										if (errorObj instanceof Map<?, ?> errorMap) {
											message.append(String.valueOf(errorMap.get("message")), true);
										}
									}
								}
							}
						}
					}
				}
			}
			catch (Exception e) {
				numErrors = 1;
				message.append("exception = " + e.getMessage());
			}

			if (numErrors > 0) {
				message.showError();
			}
			else if (numWarnings > 0) {
				message.showWarning();
			}
			else {
				message.showInformation();
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}
